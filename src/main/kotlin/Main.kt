import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfProgBitsSectionHeader
import com.github.vvlevchenko.elf.ElfStrTabSection
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
        val dump = ElfStrTabSection(loader,it as ElfProgBitsSectionHeader).dumpTable()
        dump.forEach {
            println(it)
        }
    }
    loader?.section(".debug.svm.imagebuild.java.properties")?.let {
        val dump = ElfStrTabSection(loader,it as ElfProgBitsSectionHeader).dumpTable()
        dump.forEach {
            println(it)
        }
    }
}