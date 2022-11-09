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
                val e = entry(off) ?: break
                entries.add(e)
                off += e.size
            } while (true)
            entries.toList()
        }
    }

    inline fun find(headerOffset: ULong, size: UInt, noinline body: (DebugAbbrevEntry) -> Boolean): DebugAbbrevEntry? {
        entries(headerOffset, size)?.let { subEntries ->
            subEntries.forEach {
                val needle = it.find(body)
                if (needle != null)
                    return needle
            }
        }
        return null
    }

    fun DebugAbbrevEntry.find(body: (DebugAbbrevEntry) -> Boolean): DebugAbbrevEntry? {
        if (body(this))
            return this
        return child?.find(body) ?: sibling?.find(body)
    }
    fun entry(offset: ULong): DebugAbbrevEntry? {
        var off = section.sectionOffset + offset
        var v = section.loader.readSleb128(off)
        val number = v.value
            if (v.value == 0uL)
            return null
        off += v.size
        v = section.loader.readSleb128(off)
        off += v.size
        val tag = tags.find { it.value == v.value.toUShort() } ?: TODO("TAG: ${v.value.toString(16)}")
        val hasChildren = section.loader.readUByte(off).toUInt() != 0u
        val entries = mutableListOf<Pair<Attribute, Form>>()
        off++
        var child: DebugAbbrevEntry? = null
        var sibling: DebugAbbrevEntry? = null
        while (true) {
            v = section.loader.readSleb128(off)
            off += v.size
            if (v.value == 0uL) {
                if (hasChildren) {
                    child = entry(off - section.sectionOffset)
                    off += child?.size ?: section.loader.readSleb128(off).size.toULong()
                }
                sibling = entry(off - section.sectionOffset)
                off += sibling?.size ?: section.loader.readSleb128(off).size.toULong()
                break
            }
            val attribute = attributes.find { it.value  == v.value.toUShort() } ?: TODO("[$number][$tag](${offset.toString(16)})Attribute:${v.value.toString(16)}")
            v = section.loader.readSleb128(off)
            val type = forms.find { it.value == v.value.toUShort() } ?: TODO("[$number][$tag](${offset.toString(16)})FORM: ${v.value.toString(16)}")
            off += v.size
            entries.add(attribute to type)
        }
        return DebugAbbrevEntry(offset,
            number,
            tag,
            hasChildren,
            entries.toList(),
            off - (section.sectionOffset + offset),
            child,
            sibling)
    }
}

class DebugAbbrevEntry(
    val offset: ULong,
    val number: ULong,
    val tag: Tag,
    val hasChildren: Boolean,
    val encoding: List<Pair<Attribute, Form>>,
    val size: ULong,
    val child: DebugAbbrevEntry?,
    val sibling: DebugAbbrevEntry?
) {
    internal fun find(number: ULong): DebugAbbrevEntry? {
        if (this.number == number)
            return this
        return sibling?.find(number) ?: child?.find(number)
    }
}
