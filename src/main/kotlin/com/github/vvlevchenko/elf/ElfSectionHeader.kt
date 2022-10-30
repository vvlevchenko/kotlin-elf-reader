package com.github.vvlevchenko.elf

open class ElfSectionHeader(
    internal val loader:ElfLoader, val offset: ULong
) {
    val nameIndex = loader.readUInt(offset)
    val type = loader.readUInt(offset + 4u)
    val sectionOffset = when(loader.bitness) {
        ElfLoader.BitnessHeaderOffsets.BITNESS_32 -> loader.readUInt(offset + 16u).toULong()
        ElfLoader.BitnessHeaderOffsets.BITNESS_64 -> loader.readULong(offset + 24u)
    }
    val sectionSize = when(loader.bitness) {
        ElfLoader.BitnessHeaderOffsets.BITNESS_32 -> loader.readUInt(offset + 20u).toULong()
        ElfLoader.BitnessHeaderOffsets.BITNESS_64 -> loader.readULong(offset + 32u)
    }
}

