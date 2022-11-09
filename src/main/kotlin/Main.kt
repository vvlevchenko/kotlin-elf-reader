import com.github.vvlevchenko.dwarf.Form
import com.github.vvlevchenko.dwarf.section.DebugAbbrevSection
import com.github.vvlevchenko.dwarf.section.DebugInfoSection
import com.github.vvlevchenko.dwarf.section.Dwarf32Data
import com.github.vvlevchenko.dwarf.section.Dwarf64Data
import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfProgBitsSection
import com.github.vvlevchenko.elf.ElfStrTabSection
import com.github.vvlevchenko.elf.ElfSymTabSection
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
    loader.section(".debug_types")?.let {

    }
    debugStr?.let {
        println("${loader.sectionHeaderStringTable.string(it.nameIndex.toInt())}: ${it.type}")
    }
}