package com.github.vvlevchenko.dwarf.section

import com.github.vvlevchenko.dwarf.DwarfEntry
import com.github.vvlevchenko.dwarf.DwarfEntry.Format.Dwarf32
import com.github.vvlevchenko.dwarf.DwarfEntry.Format.Dwarf64
import com.github.vvlevchenko.dwarf.Form
import com.github.vvlevchenko.dwarf.forms
import com.github.vvlevchenko.dwarf.section.DWLineTableStandardOperations.DW_LNS_copy
import com.github.vvlevchenko.elf.ElfLoader
import java.util.EnumSet

data class FileEntry(val fileName: String, val directoryIndex: ULong, val modificationTime: ULong, val sizeInBytes: ULong)
data class LineEntry(val address: ULong, val fileName: FileEntry, val line: Int, val collumn: Int)
class DebugLineHeader(val commonHeader: CommonDebugLineHeader, val lineTable: List<LineEntry>)

class CommonDebugLineHeader(loader: ElfLoader, offset: ULong) : DwarfEntry(loader, offset) {
    val version: UShort
        get() {
            val off = when (format) {
                Dwarf32 -> 4u
                Dwarf64 -> 12u
            }
            return loader.readShort(offset + off).toUShort()
        }

    val isDwarf5: Boolean
        get() = version.toInt() == 5
    override val size: ULong
        get() = unitLength
    override val next = 0UL
}


class DebugLineSection(val loader: ElfLoader) {
    private val rawElfSection = loader.section(".debug_line")
    fun header(offset: ULong): DebugLineHeader? {
        rawElfSection ?: return null
        val commonDebugLineHeader = CommonDebugLineHeader(loader, rawElfSection.sectionOffset + offset)
        var off = offset
        off += when (commonDebugLineHeader.format) {
            Dwarf32 -> 6UL
            Dwarf64 -> 14UL
        }

        fun incOffBy1(): ULong {
            return ++off
        }

        fun incOffBy2(): ULong {
            off += 2UL
            return off
        }

        fun incOffBy4(): ULong {
            off += 4UL; return off; }

        fun incOffby8(): ULong {
            off += 8UL; return off; }

        fun incOffBySize(inc: UInt): ULong {
            off += inc
            return off
        }

        // 3.
        val addressSizeV5 = if (commonDebugLineHeader.isDwarf5) {
            readerHelper(off, loader::readShort, ::incOffBy2)
        } else {
            0.toUShort()
        }
        // 4.
        val segmentSelectorSizeV5 = if (commonDebugLineHeader.isDwarf5) {
            readerHelper(off, loader::readUByte, ::incOffBy1)
        } else {
            0.toUByte()
        }
        // 5.
        val headerLength = when (commonDebugLineHeader.format) {
            Dwarf32 -> {
                readerHelper(off, loader::readUInt, ::incOffBy4).toULong()
            }

            Dwarf64 -> {
                readerHelper(off, loader::readULong, ::incOffby8)
            }
        }
        // 6.
        val minimumInstructionLenght = readerHelper(off, loader::readUByte, ::incOffBy1)
        // 7.
        val maximumOperationsPerInstruction = readerHelper(off, loader::readUByte, ::incOffBy1)
        // 8.
        val defaultIsStmt = readerHelper(off, loader::readUByte, ::incOffBy1)
        // 9.
        val lineBase = readerHelper(off, loader::readUByte, ::incOffBy1).toByte()
        // 10.
        val lineRange = readerHelper(off, loader::readUByte, ::incOffBy1)
        // 11.
        val opcodeBase = readerHelper(off, loader::readUByte, ::incOffBy1)
        // 12.
        val standardOpcodeLengths = (1 until opcodeBase.toInt()).readSleb128Array(off, ::incOffBySize).map {
            it.toUByte()
        }
        if (commonDebugLineHeader.isDwarf5) {
            // 13.
            val directoryEntryFormatCount = readerHelper(off, loader::readUByte, ::incOffBy1)
            // 14.
            val directoryEntryFormat =
                (0 until directoryEntryFormatCount.toInt()).readSequenceOfPairs(off, ::incOffBySize)
            // 15.
            val directoriesCount = loader.readSleb128(rawElfSection.sectionOffset + off)
            off += directoriesCount.size
            // 16.
            val directories = (0 until directoriesCount.size.toInt()).map {

            }
            // 17.
            val fileNameEntryFormatCount = readerHelper(off, loader::readUByte, ::incOffBy1)
            // 18.
            val fileNameEntryFormat =
                (0 until fileNameEntryFormatCount.toInt()).readSleb128Array(offset, ::incOffBySize)

            // 19.
            val fileNamesCount = loader.readSleb128(rawElfSection.sectionOffset + off)
            off += fileNamesCount.size
        } else {
            val subdirectories = mutableListOf<String>().also {
                while (true) {
                    val v = readerHelper(off, loader::readUByte, ::incOffBy1)
                    if (v == 0.toUByte()) {
                        break
                    }
                    val str = readString(v, off, ::incOffBy1)
                    it.add(str)
                }
            }.toList()
            val fileNames = mutableListOf<FileEntry>().also {
                while (true) {
                    val v = readerHelper(off, loader::readUByte, ::incOffBy1)
                    if (v == 0.toUByte()) {
                        break
                    }
                    val str = readString(v, off, ::incOffBy1)
                    val directory = readSleb128(off, ::incOffBySize)
                    val lastModification = readSleb128(off, ::incOffBySize)
                    val fileSize = readSleb128(off, ::incOffBySize)
                    it.add(FileEntry(str, directory, lastModification, fileSize))
                }

            }
            val stateMachine = LineNumberProgramStateMachine(defaultIsStmt != 0.toUByte(), fileNames.toList())
            while (true) {
                //if (stateMachine.isEndSequence)
                //    break
                if (off >= offset + commonDebugLineHeader.unitLength.toUInt() + 4u)
                    break
                val logOff = off
                val opcode = readerHelper(off, loader::readUByte, ::incOffBy1)

                when (opcode.toInt()) {
                    0 -> {
                        val extendedOpcodeLength = readSleb128(off, ::incOffBySize)
                        val extendedOpcode = readerHelper(off, loader::readUByte, ::incOffBy1)
                        println("${logOff.toString(16)} 0 ${extendedOperations.find { it.value == extendedOpcode.toInt() }}")
                        when (extendedOpcode.toInt()) {
                            DWLineTableExtendedOperations.DW_LNE_end_sequence.value -> {
                                stateMachine.isEndSequence = true
                                stateMachine.commit()
                                stateMachine.reset()
                            }

                            DWLineTableExtendedOperations.DW_LNE_set_address.value -> {
                                when (extendedOpcodeLength) {
                                    9uL -> { // x64
                                        stateMachine.address = readerHelper(off, loader::readULong, ::incOffby8)
                                    }
                                    5uL -> { // x32
                                        stateMachine.address = readerHelper(off, loader::readUInt, ::incOffBy4).toULong()
                                    }
                                    else -> {
                                        // unknown bitness
                                        off += (extendedOpcodeLength - 1u)
                                    }
                                }
                            }

                            DWLineTableExtendedOperations.DW_LNE_define_file.value -> {
                                val path = readString(null, off, ::incOffBy1)
                                val includeDir = readSleb128(off, ::incOffBySize)
                                // last modification
                                readSleb128(off, ::incOffBySize)
                                // file size
                                readSleb128(off, ::incOffBySize)
                            }

                            DWLineTableExtendedOperations.DW_LNE_set_discriminator.value -> {
                                val argument = loader.readSleb128(rawElfSection.sectionOffset + off)
                                off += argument.size
                                stateMachine.discriminator = argument.value.toUInt()
                            }
                            else -> {
                                // unknown extended opcode
                                off += (extendedOpcodeLength - 1u)
                            }
                        }
                    }

                    in 1 until opcodeBase.toInt() -> {
                        val argumentsCount = standardOpcodeLengths[opcode.toInt() - 1].toInt()
                        println("${logOff.toString(16)} ${opcode.toString(16)} ${standardOperations.find { it.value == opcode.toInt() }} ($argumentsCount)")
                        when (opcode.toInt()) {
                            DW_LNS_copy.value -> { // DW_LNS_copy

                                stateMachine.discriminator = 0u
                                stateMachine.isBasicBlock = false
                                stateMachine.isEpilogueBegin = false
                                stateMachine.isPrologEnd = false
                                assert(argumentsCount == 0)
                                stateMachine.commit()
                            }

                            DWLineTableStandardOperations.DW_LNS_advance_pc.value -> { // DW_LNS_advance_pc
                                val argument = readSleb128(off, ::incOffBySize)
                                stateMachine.address += (minimumInstructionLenght * argument)
                            }

                            DWLineTableStandardOperations.DW_LNS_advance_line.value -> { // DW_LNS_set_advance_line
                                stateMachine.line += readSleb128(off, ::incOffBySize).toInt()
                            }

                            DWLineTableStandardOperations.DW_LNS_set_file.value -> { // DW_LNS_set_file
                                stateMachine.file = readSleb128(off, ::incOffBySize).toInt()
                            }

                            DWLineTableStandardOperations.DW_LNS_set_column.value -> { // DW_LNS_set_column
                                stateMachine.column = readSleb128(off, ::incOffBySize).toInt()
                            }

                            DWLineTableStandardOperations.DW_LNS_negate_stmt.value -> { // DW_LNS_negate_stmt
                                stateMachine.isStmt = !stateMachine.isStmt
                            }

                            DWLineTableStandardOperations.DW_LNS_set_basic_block.value -> { // DW_LNS_set_basic_block
                                stateMachine.isBasicBlock = true
                            }

                            DWLineTableStandardOperations.DW_LNS_const_add_pc.value -> { // DW_LNS_const_add_pc
                                stateMachine.address += (minimumInstructionLenght * ((255.toUByte() - opcodeBase) / lineRange)).toULong()
                            }

                            DWLineTableStandardOperations.DW_LNS_fixed_advance_pc.value -> { // DW_LNS_fixed_advance_pc
                                stateMachine.address += readerHelper(off, loader::readShort, ::incOffBy2).toUInt()
                            }

                            DWLineTableStandardOperations.DW_LNS_set_prologue_end.value -> { // DW_LNS_set_prologue_end
                                stateMachine.isPrologEnd = true
                            }

                            DWLineTableStandardOperations.DW_LNS_set_epilogue_begin.value -> { // DW_LNS_set_epilogue_begin
                                stateMachine.isEpilogueBegin = true
                            }

                            DWLineTableStandardOperations.DW_LNS_set_isa.value -> { // DW_LNS_set_isa
                                stateMachine.isa = readSleb128(off, ::incOffBySize).toUInt()
                            }

                            else -> {
                                // ignore unknown standard operand.
                                (0 until standardOpcodeLengths[opcode.toInt()].toInt()).forEach {
                                    readSleb128(off, ::incOffBySize)
                                }
                            }
                        }
                    }

                    else -> {
                        println("${logOff.toString(16)} ${opcode.toString(16)}")
                        val adjustedOpcode = opcode - opcodeBase
                        val opcodeAdvance = adjustedOpcode/lineRange
                        stateMachine.address += minimumInstructionLenght * opcodeAdvance
                        stateMachine.line += lineBase + (adjustedOpcode % lineRange).toInt()
                        stateMachine.isBasicBlock = false
                        stateMachine.isPrologEnd = false
                        stateMachine.isEpilogueBegin = false
                        stateMachine.discriminator = 0u
                        stateMachine.commit()
                    }
                }
                stateMachine.opIndex++
            }
            return DebugLineHeader(commonDebugLineHeader, stateMachine.lineNumEntries.toList())
        }

        return null
    }

    private fun readString(v: UByte? = null, off: ULong, offsetAppender: () -> ULong): String {
        return buildString {
            v?.let {
                append(v.toInt().toChar())
            }
            var offset = off
            while (true) {
                val s = loader.readUByte(rawElfSection!!.sectionOffset + offset)
                offset = offsetAppender()
                if (s == 0.toUByte()) {
                    break
                }
                append(s.toInt().toChar())
            }
        }
    }


    private fun IntRange.readSleb128Array(offset: ULong, offsetIncrementer: (UInt) -> ULong): List<ULong> {
        var off = offset
        return map {
            val v = loader.readSleb128(rawElfSection!!.sectionOffset + off)
            off = offsetIncrementer(v.size)
            v.value
        }.toList()
    }

    private fun IntRange.readSequenceOfPairs(offset: ULong, offsetAppender: (UInt) -> ULong): List<Pair<ULong, Form>> {
        var off = offset
        return map {
            val first = loader.readSleb128(rawElfSection!!.sectionOffset + off)
            off = offsetAppender(first.size)
            val second = loader.readSleb128(rawElfSection.sectionOffset + off)
            off = offsetAppender(second.size)
            println(second.value.toString(16))
            first.value to (forms.find { it.value == second.value.toUShort() } ?: Form.DW_FORM_null.also {
                println("${first.value} to ${second.value.toString(16)}")
            }) //second.value
        }.toList()
    }

    private inline fun <reified T> readerHelper(
        off: ULong,
        readFunction: (ULong) -> T,
        noinline offsetAppender: () -> ULong
    ): T {
        return readFunction(rawElfSection!!.sectionOffset + off).also { offsetAppender() }
    }

    private inline fun readSleb128(off: ULong, offsetAppender: (UInt) -> ULong):ULong {
        val (value, size) = loader.readSleb128(rawElfSection!!.sectionOffset + off)
        offsetAppender(size)
        return value
    }

    private inner class LineNumberProgramStateMachine(var isStmt: Boolean, val files: List<FileEntry>) {
        val lineNumEntries = mutableListOf<LineEntry>()
        var address: ULong = 0UL
        var opIndex: Int = 0
        var file: Int = 1
        var line: Int = 1
        var column: Int = 0
        var isBasicBlock: Boolean = false
        var isEndSequence: Boolean = false
        var isPrologEnd: Boolean = false
        var isEpilogueBegin: Boolean = false
        var isa: UInt = 0u
        var discriminator: UInt = 0u
        fun commit() {
            lineNumEntries.add(LineEntry(address, files[file - 1], line, column))
        }
        fun reset () {
            address= 0UL
            opIndex = 0
            file = 1
            line = 1
            column = 0
            isBasicBlock = false
            isPrologEnd = false
            isEpilogueBegin = false
            isa = 0u
            discriminator = 0u
        }
    }
}

enum class DWLineTableStandardOperations(val value: Int) {
    DW_LNS_copy(1),
    DW_LNS_advance_pc(2),
    DW_LNS_advance_line(3),
    DW_LNS_set_file(4),
    DW_LNS_set_column(5),
    DW_LNS_negate_stmt(6),
    DW_LNS_set_basic_block(7),
    DW_LNS_const_add_pc(8),
    DW_LNS_fixed_advance_pc(9),
    DW_LNS_set_prologue_end(10),
    DW_LNS_set_epilogue_begin(11),
    DW_LNS_set_isa(12),
}
val standardOperations = EnumSet.allOf(DWLineTableStandardOperations::class.java)

enum class DWLineTableExtendedOperations(val value: Int) {
    DW_LNE_end_sequence(1),
    DW_LNE_set_address(2),
    DW_LNE_define_file(3),
    DW_LNE_set_discriminator(4)
}
val extendedOperations = EnumSet.allOf(DWLineTableExtendedOperations::class.java)



