package com.github.vvlevchenko.dwarf

import com.github.vvlevchenko.dwarf.section.DebugAbbrevSection
import com.github.vvlevchenko.elf.ElfLoader

class CompilationUnitHeader(loader: ElfLoader, offset: ULong) : DwarfEntry(loader, offset) {
    val version: Short
        get() = when(format) {
            Format.Dwarf32 -> loader.readShort(offset + 4u)
            Format.Dwarf64 -> loader.readShort(offset + 12u)
        }
    val debugAbbrevOffset: ULong
        get() = when(format) {
            Format.Dwarf32 -> loader.readUInt(offset + 6u).toULong()
            Format.Dwarf64 -> loader.readULong(offset + 14u)
        }
    val addressSize: UByte
        get() = when(format) {
            Format.Dwarf32 -> loader.readUByte(10u)
            Format.Dwarf64 -> loader.readUByte(offset + 22u)
        }
    override val size: ULong
        get() = when(format) {
            Format.Dwarf32 -> 11u
            Format.Dwarf64 -> 23u
        }
    override val next: ULong
        get() = when(format) {
            Format.Dwarf32 -> unitLength + 4u
            Format.Dwarf64 -> unitLength + 12u
        }
}
