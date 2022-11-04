package com.github.vvlevchenko.dwarf.section

import com.github.vvlevchenko.dwarf.*
import com.github.vvlevchenko.dwarf.attributes
import com.github.vvlevchenko.dwarf.tags
import com.github.vvlevchenko.elf.ElfSectionHeader

/**
 * GNU specific dwarf tag/attr values please look at https://github.com/gcc-mirror/gcc/blob/master/include/dwarf2.def
 */
class DebugAbbrevSection(val section: ElfSectionHeader) {
    private val cacheEntries = mutableMapOf<Pair<ULong, UInt>, List<DebugAbbrevEntry>>()
    fun entries(headerOffset: ULong, size: UInt): List<DebugAbbrevEntry> {
        return cacheEntries.getOrPut(headerOffset to size) {
            var off = headerOffset
            val entries = mutableListOf<DebugAbbrevEntry>()
            do {
                val e = entry(off)
                entries.add(e)
                off += e.size
                if (section.loader.readSleb128(section.sectionOffset + off).value == 0uL)
                    break
            } while (off < size)
            entries.toList()
        }
    }
    fun entry(offset: ULong): DebugAbbrevEntry {
        var off = section.sectionOffset + offset
        var v = section.loader.readSleb128(off)
        val number = v.value
        off += v.size
        v = section.loader.readSleb128(off)
        off += v.size
        val tag = tags.find { it.value == v.value.toUShort() } ?: TODO("TAG: ${v.value.toString(16)}")
        val hasChildren = section.loader.readUByte(off)
        val entries = mutableListOf<Pair<Attribute, Form>>()
        off++
        while (true) {
            v = section.loader.readSleb128(off)
            off += v.size
            val attribute = attributes.find { it.value  == v.value.toUShort() } ?: Attribute.DW_AT_null//TODO("[$number][$tag](${offset.toString(16)})Attribute:${v.value.toString(16)}")
            v = section.loader.readSleb128(off)
            val type = forms.find { it.value == v.value.toUShort() } ?: TODO("FORM: ${v.value.toString(16)}")
            off += v.size
            if (attribute == Attribute.DW_AT_null && type == Form.DW_FORM_null)
                break
            entries.add(attribute to type)
        }
        return DebugAbbrevEntry(offset,
            number,
            tag,
            hasChildren.toUInt() != 0u,
            entries.toList(),
            off - (section.sectionOffset + offset))
    }
}

class DebugAbbrevEntry(
    val offset: ULong,
    val number: ULong,
    val tag: Tag,
    val hasChildren: Boolean,
    val encoding: List<Pair<Attribute, Form>>,
    val size: ULong
)
