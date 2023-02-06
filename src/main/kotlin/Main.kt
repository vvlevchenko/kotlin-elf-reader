import com.github.vvlevchenko.dwarf.Attribute
import com.github.vvlevchenko.dwarf.Form
import com.github.vvlevchenko.dwarf.Tag
import com.github.vvlevchenko.dwarf.section.*
import com.github.vvlevchenko.elf.*
import java.io.File
import java.lang.Exception
import kotlin.system.measureTimeMillis

fun main() {
    val file = File("javagdb")
    val loader = ElfLoader.elfLoader(file)
    loader?.section(".shstrtab")?.let {
        val dump = (it as? ElfStrTabSection)?.dumpTable()
    }
    loader?.section(".debug.svm.imagebuild.arguments")?.let {
        ElfStrTabSection(loader, it as ElfProgBitsSection).dumpTable().forEach {
            println(it)
        }
    }
    loader?.section(".debug.svm.imagebuild.java.properties")?.let {
        ElfStrTabSection(loader, it as ElfProgBitsSection).dumpTable().forEach {
            println(it)
        }
    }
    val debugStr = loader?.section(".debug_str")?.let {
        ElfStrTabSection(loader, it)
    }
    val strSection = loader?.section(".strtab") as? ElfStrTabSection
    loader?.section(".symtab")?.let {
        for (i in 0 until (it.sectionSize/it.sectionEntrySize).toInt()) {
            (it as? ElfSymTabSection)?.symbol(i)?.let { sym ->
                val info = sym.info()
                println("${info.bind}: ${strSection?.string(sym.name().toInt())}:${info.type}:${sym.visibility()}...0x${sym.value().toString(16)}")
            }
        }
    }

    val debugAbbrevSection = loader?.section(".debug_abbrev")?.let {
        DebugAbbrevSection(it)
    }
    val debugInfoSection = loader?.section(".debug_info")
    val  debugInfoSectionSec = DebugInfoSection(debugInfoSection!!, debugAbbrevSection!!)
    var tagsCount = 0L
    var attributeCount = 0L
    val millis = measureTimeMillis {
        debugInfoSectionSec.entries.forEach { die ->
            tagsCount++
            println("[${die.number}] tag: ${die.tag} ... ${(die.diaOffset).toString(16)}")
            die.attributes.forEach { attr ->
                attributeCount++
                println("\t[${attr.attribute}]/${attr.form} ... ${attr.offset.toString(16)}")
                when (attr.form) {
                    Form.DW_FORM_sec_offset -> {
                        val value = when (attr) {
                            is Dwarf32Data -> attr.value.toULong()
                            is Dwarf64Data -> attr.value
                            else -> TODO()
                        }
                        println("\t\t${attr.attribute} ${value.toString(16)}")
                    }
                    Form.DW_FORM_strp -> {
                        debugStr?.let {
                            val off = when (attr) {
                                is Dwarf64Data -> attr.value.toInt()
                                is Dwarf32Data -> attr.value.toInt()
                                else -> TODO()
                            }
                            println(
                                "\t\t${off.toString(16)}: attribute: $attr/${
                                    (attr as? Dwarf32Data)?.value?.mod(
                                        debugStr.sectionSize
                                    )?.toString(16)
                                }"
                            )
                            try {
                                println("\t\t${debugStr.string(off)}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    println("dwarf(tags: $tagsCount, attributeCount: $attributeCount) load  $millis in millis")

    val diClassEnum = debugInfoSectionSec.findClassByName("java.lang.Enum")
    println("java.lang.Enum(${diClassEnum?.die?.diaOffset?.toString(16)}")
    val diClassA = debugInfoSectionSec.findClassByName("javagdb.A")
    diClassA?.run {
        fields
        methods
    }

    val debugLineSection = DebugLineSection(loader)
    val v = debugInfoSectionSec.findSourceFileByFileName(debugLineSection, "App.java")
    //val header0 = debugLineSection.header(0xbf1f4U)
    val header1 = debugLineSection.header(0x4cec8U)
    assert(header1!!.commonHeader.version == 4.toUShort())
}

fun DebugInfoSection.findSourceFileByFileName(debugLineSection: DebugLineSection, path: String): List<LineEntry> {
    val fileName = File(path).name
    return entries.filter {
        it.attributes.find { att ->
            att.attribute == Attribute.DW_AT_name || return@find false
            val index = when(att) {
                is Dwarf32Data -> att.value.toInt()
                is Dwarf64Data -> att.value.toInt()
                else -> return@find false
            }
            debugStr(debugInfoSection.loader)?.string(index) == fileName
        } != null
    }.mapNotNull {
        val att = it.attributes.find { it.attribute == Attribute.DW_AT_stmt_list }
        when (att) {
            is Dwarf32Data -> att.value.toULong()
            is Dwarf64Data -> att.value
            else -> null
        }
    }.fold(mutableListOf<LineEntry>()) { acc, it ->
        debugLineSection.header(it)?.lineTable?.let { it1 -> acc.addAll(it1) }
        acc
    }.toList()
}
fun DebugInfoSection.findClassByName(name: String):DiClass? {
    val die = find { die ->
        die.tag == Tag.DW_TAG_class_type
                && die.attributes.find { it.attribute == Attribute.DW_AT_name }.let {
            val index = when (it) {
                is Dwarf32Data -> it.value.toInt()
                is Dwarf64Data -> it.value.toInt()
                else -> TODO()
            }
            debugStr(this.debugInfoSection.loader)?.string(index) == name
        }
    } ?: return null
    return DiClass(this, die)
}
fun debugStr(loader: ElfLoader) = loader.section(".debug_str")?.let {
    ElfStrTabSection(loader, it)
}

fun DebugInfoEntry.attibuteByTag(tag: Tag) = children.find { it.tag == tag }
fun DebugInfoEntry.attributeDwarfData(attribute: Attribute):Int? {
    return attributes.find {
        it.attribute == attribute
    }?.let {
        when (it) {
            is Dwarf32Data -> it.value.toInt()
            is Dwarf64Data -> it.value.toInt()
            else -> TODO()
        }
    }
}
fun DebugInfoSection.findDieByIndex(index: Int) = find { it.diaOffset == index.toULong() }

open class DiType(val debugInfoSection: DebugInfoSection, val die: DebugInfoEntry) {
    val name:String? by lazy {
        val index = die.attributeDwarfData(Attribute.DW_AT_name) ?: return@lazy null
        debugStr(debugInfoSection.debugInfoSection.loader)?.string(index)
    }

}
class DiArrayType(debugInfoSection: DebugInfoSection, die: DebugInfoEntry): DiType(debugInfoSection, die)
class DiBaseType(debugInfoSection: DebugInfoSection, die: DebugInfoEntry): DiType(debugInfoSection, die)
class DiField(val name: String, val type: DiType)
class DiParameter(val name: String, val type: DiType)
class DiMethod(val returnType: DiType, val name: String, vararg val parameters:DiParameter)

class DiClass(debugInfoSection: DebugInfoSection, die: DebugInfoEntry): DiType(debugInfoSection, die) {
    val superType:DiClass? by lazy {
        die.attibuteByTag(Tag.DW_TAG_inheritance)?.let { entry ->
            val index = entry.attributeDwarfData(Attribute.DW_AT_type) ?: return@let null
            val die = debugInfoSection.findDieByIndex(index) ?: return@let null
            DiClass(debugInfoSection, die)
        }
    }

    val fields by lazy {
        die.children.filter {
            it.tag == Tag.DW_TAG_member
        }.mapNotNull {
            DiField(nameOfDie(it)!!, typeOfDie(it)!!)
        }
    }

    private fun typeOfDie(die: DebugInfoEntry): DiType? {
        val typeOff = die.attributeDwarfData(Attribute.DW_AT_type) ?: return null
        val die = debugInfoSection.findDieByIndex(typeOff) ?: return null
        return when(die.tag) {
            Tag.DW_TAG_class_type -> DiClass(debugInfoSection, die)
            Tag.DW_TAG_pointer_type,
            Tag.DW_TAG_unspecified_type,
            Tag.DW_TAG_base_type -> DiBaseType(debugInfoSection, die)
            Tag.DW_TAG_array_type -> DiArrayType(debugInfoSection, die)
            else -> TODO("${die.tag.name}")
        }
    }

    private fun nameOfDie(die: DebugInfoEntry): String? {
        val nameOff = die.attributeDwarfData(Attribute.DW_AT_name) ?: return null
        return debugStr(debugInfoSection.debugInfoSection.loader)?.string(nameOff) ?: return null

    }

    val methods by lazy {
        die.children.filter {
            it.tag == Tag.DW_TAG_subprogram
        }.mapNotNull {
            val name = nameOfDie(it)!!
            val type = typeOfDie(it)!!
            val params = it.children.filter {
                it.tag == Tag.DW_TAG_formal_parameter
            }.map {
                DiParameter(nameOfDie(it)!!, typeOfDie(it)!!)
            }
            DiMethod(type, name, *params.toTypedArray())
        }
    }

}

