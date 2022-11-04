package com.github.vvlevchenko.elf

import com.github.vvlevchenko.elf.ElfSectionHeader.SectionType.*
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files

/**
 * Elf32_Addr	4	4	Unsigned program address
 * Elf32_Off	4	4	Unsigned file offset
 * Elf32_Half	2	2	Unsigned medium integer
 * Elf32_Word	4	4	Unsigned integer
 * Elf32_Sword	4	4	Signed integer
 * unsigned char	1	1	Unsigned small integer
 *
 * Name	Size	Alignment	Purpose
 * Elf64_Addr	8	8	Unsigned program address
 * Elf64_Off	8	8	Unsigned file offset
 * Elf64_Half	2	2	Unsigned medium integer
 * Elf64_Word	4	4	Unsigned integer
 * Elf64_Sword	4	4	Signed integer
 * Elf64_Xword	8	8	Unsigned long integer
 * Elf64_Sxword	8	8	Signed long integer
 * unsigned char	1	1	Unsigned small integer
 */

class ElfLoader(val bitness: BitnessHeaderOffsets, val buffer: MappedByteBuffer) {

    companion object {
        fun elfLoader(file: File): ElfLoader? {
            return FileInputStream(file).use {
                val elfHeader = ByteArray(16)
                val byteBuffer = it.channel.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(file.toPath()))
                byteBuffer.get(elfHeader)
                if (elfHeader[0] == 0x7f.toByte() &&
                    elfHeader[1] == 'E'.code.toByte() &&
                    elfHeader[2] == 'L'.code.toByte() &&
                    elfHeader[3] == 'F'.code.toByte()
                ) {
                    byteBuffer.position(0)
                    return@use when (elfHeader[BitnessHeaderOffsets.BITNESS_32.bitnessOffset].toInt()) {
                        1 -> ElfLoader(BitnessHeaderOffsets.BITNESS_32, byteBuffer)
                        2 -> ElfLoader(BitnessHeaderOffsets.BITNESS_64, byteBuffer)
                        else -> TODO()
                    }
                }
                null
            }
        }
    }

    enum class BitnessHeaderOffsets(
        val offsetEntrySize: Int,
        val bitnessOffset: Int,
        val endianess: Int,
        val type: Int,
        val machine: Int,
        val entry: Int,
        val programHeaderOffset: Int,
        val sectionHeaderOffset: Int,
        val elfHeaderSize: Int,
        val programHeaderEntrySize: Int,
        val programHeaderEntryNum: Int,
        val sectionHeaderEntrySize: Int,
        val sectionHeaderNumber: Int,
        val sectionHeaderStringIndex:Int
        ){
        BITNESS_32(4, 0x4, 0x5, 0x10, 0x12, 0x18,
            0x1c, 0x20, 0x20, 0x2a,
            0x2c, 0x2e, 0x30, 0x32),
        BITNESS_64(8, 0x4, 0x5, 0x10, 0x12, 0x18,
            0x20, 0x28, 0x28, 0x36,
            0x38, 0x3a, 0x3c, 0x3e)

    }
    val fullElfHeader: ByteArray by lazy {
        buffer.position(0)
        ByteArray(readShort(bitness.elfHeaderSize.toULong()).toInt()).also {
            buffer.position(0)
            buffer.get(it)
        }
    }

    private val sectionHeaderOffset = readULong(bitness.sectionHeaderOffset.toULong())
    private val sectionHeaderEntrySize = readShort(bitness.sectionHeaderEntrySize.toULong())
    private val sectionHeaderStringIndex = readShort(bitness.sectionHeaderStringIndex.toULong())

    val sectionHeaderStringTable by lazy {
        section(sectionHeaderStringIndex.toInt()) as ElfStrTabSection
    }

    fun readUByte(offset: ULong): UByte {
        return buffer.atOffset(offset) {
            buffer.get().toUByte()
        }
    }
    fun readShort(offset: ULong): Short {
        return ByteArray(2).let {
            buffer.atOffset(offset) {
                get(it)
                it[1].toUByte().toInt().shl(8).or(it[0].toUByte().toInt())
            }
        }.toShort()
    }

    fun readUInt(offset: ULong): UInt {
        return ByteArray(4).let {
            buffer.atOffset(offset) {
                buffer.get(it)
                var value = 0U
                for (i in 0 until 4) {
                    value = value.or(it[i].toUByte().toUInt().shl(i * 8))
                }
                value
            }
        }
    }

    fun readULong(offset: ULong): ULong{
        return ByteArray(8).let {
            buffer.atOffset(offset) {
                buffer.get(it)
                var value = 0UL
                for (i in 0 until 8) {
                    value = value.or(it[i].toUByte().toULong().shl(i * 8))
                }
                value
            }
        }
    }

    data class Sleb128Entry(val value: ULong, val size: UInt)
    fun readSleb128(offset: ULong): Sleb128Entry {
        return buffer.atOffset(offset) {
            var result = 0UL
            var size = 0U
            var v = 0UL
            do {
                v = get().toUByte().toULong()
                result = result.or(v.and(0x7fu).shl(7 * size.toInt()))
                size += 1U
            } while(v.and(0x80u) != 0UL)
            Sleb128Entry(result, size)
        }
    }

    fun section(sectionName: String): ElfSectionHeader? {
        val sectionNumber = readShort(bitness.sectionHeaderNumber.toULong())
        for (i in 0 until sectionNumber) {
            val section = section(i)
            if (sectionHeaderStringTable.string(section.nameIndex.toInt()) == sectionName) {
                return section
            }
        }
        return null
    }

    fun section(index: Int): ElfSectionHeader {
        val offset = sectionHeaderOffset + (sectionHeaderEntrySize * index).toUInt()
        val typeOffset = offset + 4u;
        return when(readUInt(typeOffset)) {
            shtProgBits.type -> ElfProgBitsSection(this, offset)
            shtSymTab.type -> ElfSymTabSection(this, offset)
            shtStrTab.type -> ElfStrTabSection(this,offset)
            else -> ElfSectionHeader(this, offset)
        }
    }
}

internal inline fun <reified T> ByteBuffer.atOffset(offset: ULong, action: ByteBuffer.() -> T): T {
    position(offset.toInt())
    val v = action()
    position(0)
    return v
}