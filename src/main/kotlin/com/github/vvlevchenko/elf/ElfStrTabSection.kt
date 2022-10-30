package com.github.vvlevchenko.elf

import com.github.vvlevchenko.elf.ElfSectionHeader.SectionType.shtStrTab

class ElfStrTabSection (loader: ElfLoader, offset: ULong) : ElfSectionHeader(loader, offset) {
    constructor(loader: ElfLoader, header: ElfSectionHeader) : this(loader, header.offset) {
        if (header !is ElfProgBitsSection && type != shtStrTab.type)
            throw IllegalStateException("$type isn't string tab ${shtStrTab.type}")
    }
    fun string(index : Int): String {
        return loader.buffer.atOffset((sectionOffset + index.toUInt())) {
            val bytes = mutableListOf<Byte>()
            var b: Byte
            while(true){
                b = get()
                if (b == 0.toByte())
                    break
                bytes.add(b)
            }
            String(bytes.toByteArray())
        }
    }
    fun dumpTable(): List<String> {
        val dump = mutableListOf<String>()
        var i = 0
        while (i < sectionSize.toInt()) {
            val str = string(i)
            if (str.isNotEmpty())
                dump.add(str)
            i += str.length + 1
        }
        return dump.toList()
    }
}