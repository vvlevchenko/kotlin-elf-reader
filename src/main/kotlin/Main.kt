import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfProgBitsSection
import com.github.vvlevchenko.elf.ElfStrTabSection
import com.github.vvlevchenko.elf.ElfSymTabSection
import java.io.File

fun main() {
    val file = File("javagdb")
    val loader = ElfLoader.elfLoader(file)
    loader?.section(".shstrtab")?.let {
        val dump = (it as? ElfStrTabSection)?.dumpTable()
        dump?.forEach {
            println(it)
        }
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
}