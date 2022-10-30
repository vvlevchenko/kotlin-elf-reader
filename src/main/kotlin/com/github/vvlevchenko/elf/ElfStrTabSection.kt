package com.github.vvlevchenko.elf

class ElfStrTabSection (loader: ElfLoader, offset: ULong) : ElfSectionHeader(loader, offset) {
    constructor(loader: ElfLoader, header: ElfSectionHeader) : this(loader, header.offset) {
        if (header !is ElfProgBitsSectionHeader && type != 0x3.toUInt())
            throw IllegalStateException("$type isn't string tab 0x3")
    }
    fun readString(index : Int): String {
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
            val str = readString(i)
            if (str.isNotEmpty())
                dump.add(str)
            i += str.length + 1
        }
        return dump.toList()
    }
}