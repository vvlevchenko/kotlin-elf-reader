import com.github.vvlevchenko.dwarf.section.DebugAbbrevSection
import com.github.vvlevchenko.dwarf.section.DebugInfoSection
import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfProgBitsSection
import com.github.vvlevchenko.elf.ElfStrTabSection
import com.github.vvlevchenko.elf.ElfSymTabSection
import java.io.File
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
        debugInfoSectionSec.entries.forEach {
            tagsCount++
            println("[${it.number}] tag: ${it.tag} ... ${it.diaOffset.toString(16)}")
            it.attributes.forEach { attr ->
                attributeCount++
                println("\t[${attr.attribute}] ... ${attr.offset.toString(16)}")
            }
        }
    }
    println("dwarf(tags: $tagsCount, attributeCount: $attributeCount) load  $millis in millis")
    loader.section(".debug_types")?.let {

    }
    loader.section(".debug_str")?.let {
        println("${loader.sectionHeaderStringTable.string(it.nameIndex.toInt())}: ${it.type}")
    }
}