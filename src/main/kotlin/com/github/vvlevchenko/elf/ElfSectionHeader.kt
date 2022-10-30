package com.github.vvlevchenko.elf

import com.github.vvlevchenko.elf.ElfLoader.BitnessHeaderOffsets.BITNESS_32
import com.github.vvlevchenko.elf.ElfLoader.BitnessHeaderOffsets.BITNESS_64

open class ElfSectionHeader(
    internal val loader:ElfLoader, val offset: ULong
) {
    enum class SectionType(val type: UInt) {
        shtNull(0u),
        shtProgBits(1u),
        shtSymTab(2u),
        shtStrTab(3u),
        shtRela(4u),
        shtHash(5u),
        shtNote(7u),
        shtNoBits(8u),
        shtRel(9u),
        shtShLib(10u),
        shtDynSym(11u),
        shtLoProc(0x70000000u),
        shtHiProc(0x7fffffffu),
        shtLoUser(0x80000000u),
        shtHiUser(0x8fffffffu)
    }
    val nameIndex = loader.readUInt(offset)
    val type = loader.readUInt(offset + 4u)
    val sectionOffset = when(loader.bitness) {
        BITNESS_32 -> loader.readUInt(offset + 16u).toULong()
        BITNESS_64 -> loader.readULong(offset + 24u)
    }
    val sectionSize = when(loader.bitness) {
        BITNESS_32 -> loader.readUInt(offset + 20u).toULong()
        BITNESS_64 -> loader.readULong(offset + 32u)
    }

    val sectionEntrySize = when(loader.bitness) {
        BITNESS_32 -> loader.readUInt(offset + 0x24u).toULong()
        BITNESS_64 -> loader.readULong(offset + 0x38u)
    }
}

