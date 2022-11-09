package com.github.vvlevchenko.dwarf

import java.util.*

enum class Form(val value: UShort, vararg val types: ElementType) {
    DW_FORM_null(0u),
    DW_FORM_addr(0x01u, ElementType.Address),
    DW_FORM_block2(0x03u, ElementType.Block),
    DW_FORM_block4(0x4u, ElementType.Block),
    DW_FORM_data2(0x5u, ElementType.Constant),
    DW_FORM_data4(0x06u, ElementType.Constant),
    DW_FORM_data8(0x07u, ElementType.Constant),
    DW_FORM_string(0x08u, ElementType.String),
    DW_FORM_block(0x09u, ElementType.Block),
    DW_FORM_block1(0x0au, ElementType.Block),
    DW_FORM_data1(0x0bu, ElementType.Constant),
    DW_FORM_flag(0x0cu, ElementType.Flag),
    DW_FORM_sdata(0x0du, ElementType.Constant),
    DW_FORM_strp(0x0eu, ElementType.String),
    DW_FORM_udata(0x0fu, ElementType.Constant),
    DW_FORM_ref_addr(0x10u, ElementType.Reference),
    DW_FORM_ref1(0x11u, ElementType.Reference),
    DW_FORM_ref2(0x12u, ElementType.Reference),
    DW_FORM_ref4(0x13u, ElementType.Reference),
    DW_FORM_ref8(0x14u, ElementType.Reference),
    DW_FORM_ref_udata(0x15u, ElementType.Reference),
    DW_FORM_indirect(0x16u),
    DW_FORM_sec_offset(0x17u, ElementType.LinePtr, ElementType.LocListPtr, ElementType.MacPtr, ElementType.RangeListPtr),
    DW_FORM_exprloc(0x18u, ElementType.ExprLoc),
    DW_FORM_flag_present(0x19u, ElementType.Flag),
    DW_FORM_ref_sig8(0x20u, ElementType.Reference),
    // DWARF 5
    DW_FORM_strx1(0x25u, ElementType.String),
    DW_FORM_strx2(0x26u, ElementType.String),
    DW_FORM_strx4(0x27u, ElementType.String),
    DW_FORM_strx8(0x28u, ElementType.String)
}
internal val forms = EnumSet.allOf(Form::class.java)
