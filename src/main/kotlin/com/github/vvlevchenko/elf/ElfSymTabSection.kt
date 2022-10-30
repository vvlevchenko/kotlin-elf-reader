package com.github.vvlevchenko.elf

import com.github.vvlevchenko.elf.ElfLoader.BitnessHeaderOffsets.BITNESS_32
import com.github.vvlevchenko.elf.ElfLoader.BitnessHeaderOffsets.BITNESS_64

class ElfSymTabSection(loader: ElfLoader, offset: ULong) : ElfSectionHeader(loader, offset) {
    fun symbol(index: Int):ElfSym {
        return loader.buffer.atOffset((sectionOffset + (sectionEntrySize * index.toUInt()))) {
            ElfSym(loader, position().toULong())
        }
    }
}

/**
 * typedef struct {
 * 	Elf32_Word	st_name; (4)
 * 	Elf32_Addr	st_value; (4)
 * 	Elf32_Word	st_size; (4)
 * 	unsigned char	st_info; (1)
 * 	unsigned char	st_other; (1)
 * 	Elf32_Half	st_shndx; (2)
 * } Elf32_Sym;
 *
 * typedef struct {
 * 	Elf64_Word	st_name; (4)
 * 	unsigned char	st_info; (1)
 * 	unsigned char	st_other; (1)
 * 	Elf64_Half	st_shndx; (2)
 * 	Elf64_Addr	st_value; (8)
 * 	Elf64_Xword	st_size; (8)
 * } Elf64_Sym;
 */

data class SymbolTypeAndBind(val type: UByte, val bind: UByte)
class ElfSym(private val loader: ElfLoader, private val offset: ULong) {
    fun name() = loader.readUInt(offset)
    fun value() = when(loader.bitness) {
        BITNESS_32 -> loader.readUInt(offset + 4u).toULong()
        BITNESS_64 -> loader.readULong(offset + 8u)
    }
    fun size() = when(loader.bitness) {
        BITNESS_32 -> loader.readUInt(offset + 8u).toULong()
        BITNESS_64 -> loader.readULong(offset + 16u)
    }
    fun info(): SymbolTypeAndBind {
        val value = when (loader.bitness) {
            BITNESS_32 -> loader.readUByte(offset + 12u)
            BITNESS_64 -> loader.readUByte(offset + 4u)
        }
        return SymbolTypeAndBind(value.toInt().and(0xf).toUByte(),
            value.toInt().shr(4).toUByte())
    }
    fun other() = when(loader.bitness) {
        BITNESS_32 -> loader.readUByte(offset + 13u)
        BITNESS_64 -> loader.readUByte(offset + 5u)
    }
    fun visibility() = other().toInt().and(0x3).toUByte()
    fun shindx() = when(loader.bitness) {
        BITNESS_32 -> loader.readShort(offset + 14u)
        BITNESS_64 -> loader.readShort(offset + 6u)
    }
}
