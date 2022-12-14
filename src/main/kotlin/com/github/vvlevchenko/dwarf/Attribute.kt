package com.github.vvlevchenko.dwarf

import java.util.EnumSet

enum class Attribute(val value: UShort, vararg val types: ElementType) {
    DW_AT_null(0u),
    DW_AT_sibling(0x01u, ElementType.Reference),
    DW_AT_location(0x02u, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_name(0x03u, ElementType.String),
    DW_AT_ordering(0x09u, ElementType.Constant),
    DW_AT_byte_size(0x0bu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_bit_offset(0x0cu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_bit_size(0x0du, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_stmt_list(0x10u, ElementType.LinePtr),
    DW_AT_low_pc(0x11u, ElementType.Address),
    DW_AT_high_pc(0x12u, ElementType.Address, ElementType.Constant),
    DW_AT_language(0x13u, ElementType.Constant),
    DW_AT_discr(0x15u, ElementType.Reference),
    DW_AT_discr_value(0x16u, ElementType.Constant),
    DW_AT_visibility(0x17u, ElementType.Constant),
    DW_AT_import(0x18u, ElementType.Reference),
    DW_AT_string_length(0x19u, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_common_reference(0x1au, ElementType.Reference),
    DW_AT_comp_dir(0x1bu, ElementType.String),
    DW_AT_const_value(0x1cu, ElementType.Block, ElementType.Constant, ElementType.String),
    DW_AT_containing_type(0x1du, ElementType.Reference),
    DW_AT_default_value(0x1eu, ElementType.Reference),
    DW_AT_inline(0x20u, ElementType.Constant),
    DW_AT_is_optional(0x21u, ElementType.Flag),
    DW_AT_lower_bound(0x22u, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_producer(0x25u, ElementType.String),
    DW_AT_prototyped(0x27u, ElementType.Flag),
    DW_AT_return_addr(0x2au, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_start_scope(0x2cu, ElementType.Constant, ElementType.RangeListPtr),
    DW_AT_bit_stride(0x2eu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_upper_bound(0x2fu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_abstract_origin(0x31u, ElementType.Reference),
    DW_AT_accessibility(0x32u, ElementType.Constant),
    DW_AT_address_class(0x33u, ElementType.Constant),
    DW_AT_artificial(0x34u, ElementType.Flag),
    DW_AT_base_types(0x35u, ElementType.Reference),
    DW_AT_calling_convention(0x36u, ElementType.Constant),
    DW_AT_count(0x37u, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_data_member_location(0x38u, ElementType.Constant, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_decl_column(0x39u, ElementType.Constant),
    DW_AT_decl_file(0x3au, ElementType.Constant),
    DW_AT_decl_line(0x3bu, ElementType.Constant),
    DW_AT_declaration(0x3cu, ElementType.Flag),
    DW_AT_discr_list(0x3du, ElementType.Block),
    DW_AT_encoding(0x3eu, ElementType.Constant),
    DW_AT_external(0x3fu, ElementType.Flag),
    DW_AT_frame_base(0x40u, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_friend(0x41u, ElementType.Reference),
    DW_AT_identifier_case(0x42u, ElementType.Constant),
    DW_AT_macro_info(0x43u, ElementType.MacPtr),
    DW_AT_namelist_item(0x44u, ElementType.Reference),
    DW_AT_priority(0x45u, ElementType.Reference),
    DW_AT_segment(0x46u, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_specification(0x47u, ElementType.Reference),
    DW_AT_static_link(0x48u, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_type(0x49u, ElementType.Reference),
    DW_AT_use_location(0x4au, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_variable_parameter(0x4bu, ElementType.Flag),
    DW_AT_virtuality(0x4cu, ElementType.Constant),
    DW_AT_vtable_elem_location(0x4du, ElementType.ExprLoc, ElementType.LocListPtr),
    DW_AT_allocated(0x4eu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_associated(0x4fu, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_data_location(0x50u, ElementType.ExprLoc),
    DW_AT_byte_stride(0x51u, ElementType.Constant, ElementType.ExprLoc, ElementType.Reference),
    DW_AT_entry_pc(0x52u, ElementType.Address),
    DW_AT_use_UTF8(0x53u, ElementType.Flag),
    DW_AT_extension(0x54u, ElementType.Reference),
    DW_AT_ranges(0x55u, ElementType.RangeListPtr),
    DW_AT_trampoline(0x56u, ElementType.Address, ElementType.Flag, ElementType.Reference, ElementType.String),
    DW_AT_call_column(0x57u, ElementType.Constant),
    DW_AT_call_file(0x58u, ElementType.Constant),
    DW_AT_call_line(0x59u, ElementType.Constant),
    DW_AT_description(0x5au, ElementType.String),
    DW_AT_binary_scale(0x5bu, ElementType.Constant),
    DW_AT_decimal_scale(0x5cu, ElementType.Constant),
    DW_AT_small(0x5du, ElementType.Reference),
    DW_AT_decimal_sign(0x5eu, ElementType.Constant),
    DW_AT_digit_count(0x5fu, ElementType.Constant),
    DW_AT_picture_string(0x60u, ElementType.String),
    DW_AT_mutable(0x61u, ElementType.Flag),
    DW_AT_threads_scaled(0x62u, ElementType.Flag),
    DW_AT_explicit(0x63u, ElementType.Flag),
    DW_AT_object_pointer(0x64u, ElementType.Reference),
    DW_AT_endianity(0x65u, ElementType.Constant),
    DW_AT_elemental(0x66u, ElementType.Flag),
    DW_AT_pure(0x67u, ElementType.Flag),
    DW_AT_recursive(0x68u, ElementType.Flag),
    DW_AT_signature(0x69u, ElementType.Reference),
    DW_AT_main_subprogram(0x6au, ElementType.Flag),
    DW_AT_data_bit_offset(0x6bu, ElementType.Constant),
    DW_AT_const_expr(0x6cu, ElementType.Flag),
    DW_AT_enum_class(0x6du, ElementType.Flag),
    DW_AT_linkage_name(0x6eu, ElementType.String),
    //DWARF 5
    DW_AT_string_lenght_bit_size(0x6fu),
    DW_AT_string_lenght_byte_size(0x70u),
    DW_AT_rank(0x71u),
    DW_AT_str_offsetes_base(0x72u),
    DW_AT_addr_base(0x73u),
    DW_AT_rnglists_base(0x74u),
    DW_AT_dwo_name(0x76u),
    DW_AT_reference(0x77u),
    DW_AT_rvalue_reference(0x78u),
    DW_AT_macros(0x79u),
    DW_AT_call_all_calls(0x7au),
    DW_AT_call_all_source_calls(0x7bu),
    DW_AT_call_all_tail_calls(0x7cu),
    DW_AT_call_return_pc(0x7du),
    DW_AT_call_value(0x7eu),
    DW_AT_call_orogon(0x7fu),
    DW_AT_call_parameter(0x80u),
    DW_AT_call_pc(0x81u),
    DW_AT_call_tail_call(0x82u),
    DW_AT_call_target(0x83u),
    DW_AT_call_target_clobbered(0x84u),
    DW_AT_call_data_location(0x85u),
    DW_AT_call_data_value(0x86u),
    DW_AT_noreturn(0x87u),
    DW_AT_alignment(0x88u),
    DW_AT_export_symbols(0x89u),
    DW_AT_deleted(0x8au),
    DW_AT_defaulted(0x8bu),
    DW_AT_loclists_base(0x8cu),
    DW_AT_lo_user(0x2000u),
    DW_AT_GNU_call_site_value(0x2111u),
    DW_AT_GNU_tail_call(0x2115u),
    DW_AT_GNU_all_call_sites(0x2117u),
    DW_AT_GNU_locviews(0x2137u),
    DW_AT_GNU_entry_view(0x2138u),
    DW_AT_hi_user(0x3fffu)
}

internal val attributes = EnumSet.allOf(Attribute::class.java)