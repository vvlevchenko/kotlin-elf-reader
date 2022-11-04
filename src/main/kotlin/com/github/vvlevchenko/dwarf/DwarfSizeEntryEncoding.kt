package com.github.vvlevchenko.dwarf

import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfSectionHeader

/**
 * .debug_aranges, .debug_info, .debug_types, .debug_line, .debug_pubnames, and .debug_pubtypes
 */
abstract class DwarfEntry(val loader: ElfLoader, val offset: ULong) {
    enum class Format{
        Dwarf32,
        Dwarf64
    }
    val format by lazy {
        when(loader.readUInt(offset)) {
            0xffffffffu -> Format.Dwarf64
            else -> Format.Dwarf32
        }
    }
    val unitLength = when(format) {
        Format.Dwarf64 -> loader.readULong(offset + 4u)
        else -> loader.readUInt(offset).toULong()
    }
    abstract val size: ULong
    abstract val next: ULong
}