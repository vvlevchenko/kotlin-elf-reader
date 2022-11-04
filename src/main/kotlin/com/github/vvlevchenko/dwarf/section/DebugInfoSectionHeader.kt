package com.github.vvlevchenko.dwarf.section

import com.github.vvlevchenko.dwarf.*
import com.github.vvlevchenko.elf.ElfLoader
import com.github.vvlevchenko.elf.ElfSectionHeader

class DebugInfoSection(val debugInfoSection: ElfSectionHeader, val debugAbbrevSection: DebugAbbrevSection) {
    private val loader = debugInfoSection.loader
    val entries by lazy {
        entries()
    }
    private fun entries(): List<DebugInfoEntry> {
        var offset = debugInfoSection.sectionOffset
        val debugInfoEntryEntries = mutableListOf <DebugInfoEntry> ()
        do {
            offset = CompilationUnitHeader(loader, offset).let { cu ->
                val entries = debugAbbrevSection.entries(cu.debugAbbrevOffset, cu.next.toUInt())
                debugAbbrevSection.entry(cu.debugAbbrevOffset).let {
                    var off = cu.offset + cu.size
                    do {
                        val diaOffset = off
                        val v = loader.readSleb128(off)
                        val number = v.value
                        off += v.size
                        val attributes = mutableListOf<Value>()
                        entries.find { it.number == number }?.let { abbrev ->
                            val tag = abbrev.tag
                            abbrev.encoding.forEach { (attribute, form) ->
                                val attribute = readEntry(attribute, form, loader, off, cu)
                                attributes.add(attribute)
                                off += attribute.size
                            }
                            debugInfoEntryEntries.add(DebugInfoEntry(diaOffset, tag, number, attributes.toList()))
                        }
                    } while (off < offset + cu.next)
                }
                offset + cu.next
            }
        } while((offset - debugInfoSection.sectionOffset) < debugInfoSection.sectionSize)
        return debugInfoEntryEntries.toList()
    }
    private fun readEntry(
        attribute: Attribute,
        form: Form,
        loader: ElfLoader,
        off: ULong,
        cu: CompilationUnitHeader
    ) = when (form) {
        Form.DW_FORM_flag -> FlagValue(attribute, off, loader.readUByte(off))
        Form.DW_FORM_ref1,
        Form.DW_FORM_data1 -> DataValue(attribute, form, off, loader.readUByte(off))
        Form.DW_FORM_ref2,
        Form.DW_FORM_data2 -> DataValue(attribute, form, off, loader.readShort(off))
        Form.DW_FORM_ref4,
        Form.DW_FORM_data4 -> DataValue(attribute, form, off, loader.readUInt(off))
        Form.DW_FORM_ref8,
        Form.DW_FORM_data8 -> DataValue(attribute, form, off, loader.readULong(off))
        Form.DW_FORM_udata,
        Form.DW_FORM_sdata -> LebDataValue(attribute, form, off, loader.readSleb128(off))
        Form.DW_FORM_strp,
        Form.DW_FORM_sec_offset,
        Form.DW_FORM_addr,
        Form.DW_FORM_ref_addr -> when (cu.format) {
            DwarfEntry.Format.Dwarf32 -> Dwarf32Data(attribute, form, off, loader.readUInt(off))
            DwarfEntry.Format.Dwarf64 -> Dwarf64Data(attribute, form, off, loader.readULong(off))
        }
        Form.DW_FORM_flag_present -> FlagValuePresent(attribute, off)
        Form.DW_FORM_exprloc -> {
            val v = loader.readSleb128(off)
            UByteArray(v.value.toInt()).let {
                for(i in it.indices) {
                    it[i] = loader.readUByte(off)
                }
                BlockData(attribute, form, off, (v.value + v.size).toUInt(), it)
            }
        }
        Form.DW_FORM_string -> {
            var v = 0.toUByte()
            var size = 0u
            val raw = mutableListOf<UByte>()
            do {
                raw.add(loader.readUByte(off + size))
                size++
            } while (v != 0.toUByte())
            StringData(attribute, form, off, raw.toUByteArray())
        }
        else -> TODO("$form")
    }
}

class DebugInfoEntry(val diaOffset: ULong, val tag: Tag, val number: ULong, val attributes: List<Value>)
open class Value(val attribute: Attribute, val form: Form, val offset:ULong, open val size: UInt)
class FlagValue(attribute: Attribute, offset: ULong, private val value: UByte): Value(attribute, Form.DW_FORM_flag, offset, 1u) {
    val isSet = value != 0.toUByte()
}
class FlagValuePresent(attribute: Attribute, offset: ULong): Value(attribute, Form.DW_FORM_flag_present, offset, 0u)
class DataValue<T>(attribute: Attribute, form: Form, offset: ULong, val value:T) : Value(attribute, form, offset, sizeof(form))
class LebDataValue(attribute: Attribute, form: Form, offset: ULong, rawValue: ElfLoader.Sleb128Entry):
    Value(attribute, form, offset, rawValue.size)  {
        val value = rawValue.value
}

class Dwarf32Data(attribute: Attribute, form: Form, offset: ULong, val value: UInt): Value(attribute, form, offset, 4u)
class Dwarf64Data(attribute: Attribute, form: Form, offset: ULong, val value: ULong): Value(attribute, form, offset, 8u)

class BlockData(attribute: Attribute, form: Form, offset: ULong, size:UInt, val value: UByteArray): Value(attribute, form, offset, size)
class StringData(attribute: Attribute, form: Form, offset: ULong, val value: UByteArray): Value(attribute, form, offset, value.size.toUInt())

fun sizeof(form: Form): UInt = when(form) {
    Form.DW_FORM_ref1,
    Form.DW_FORM_data1 -> 1u
    Form.DW_FORM_ref2,
    Form.DW_FORM_data2 -> 2u
    Form.DW_FORM_ref4,
    Form.DW_FORM_data4 -> 4u
    Form.DW_FORM_ref8,
    Form.DW_FORM_data8 -> 8u
    else -> TODO("$form")
}
