# SPDX-FileCopyrightText: 2026 Synexia, Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# JNI libraries of src/jdk.m3.collections/share/native/libm3collections (NATIVE_ABSORB N1, 2026-11-09).
# Before this file, nothing compiled these sources. There is one shared library per concern, named exactly as its
# Java owner loads it: System.loadLibrary("<name>") for the self-loading owners, and the file the host passes to
# System.load for the host-loaded ones. Each library is compiled with the javac -h header of every class it serves
# force-included, so a C definition whose signature drifts from its Java `native` declaration fails the build.
cmake_minimum_required(VERSION 3.24)
project(m3_collections LANGUAGES C)

set(M3_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../..)
set(M3_CLASSES ${M3_ROOT}/src/jdk.m3.collections/share/classes)
set(M3_NATIVE ${M3_ROOT}/src/jdk.m3.collections/share/native/libm3collections)
set(M3_JNI_OUT ${CMAKE_CURRENT_BINARY_DIR}/jni)
set(M3_JNI_HEADERS ${M3_JNI_OUT}/jdk.m3.collections)

find_package(Java 21 REQUIRED COMPONENTS Development)
# Headers only: no AWT, and no libjvm is linked (the hosting JVM provides the JNI functions).
find_package(JNI REQUIRED COMPONENTS JVM)

set(M3_JNI_DECLARATIONS "")
set(M3_JNI_HEADER_FILES "")

# Some donor C states a platform requirement with #error (m3_value_bits: IEC 60559 floats). Such a library is built
# only where the requirement holds; its classes are listed in m3_skipped_jni_classes.txt for the export gate.
include(CheckCSourceCompiles)
check_c_source_compiles("#ifndef __STDC_IEC_559__\n#error no IEC 60559\n#endif\nint main(void) { return 0; }"
  M3_C_IEC_559)
file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/m3_skipped_jni_classes.txt "")

# m3_collections_jni(<library> [REQUIRES <variable>] SOURCES <C files under libm3collections>
#                    CLASSES <declaring class binary names>)
function(m3_collections_jni library)
  cmake_parse_arguments(ARG "" "REQUIRES" "SOURCES;CLASSES" ${ARGN})
  if(ARG_REQUIRES AND NOT ${ARG_REQUIRES})
    message(STATUS "m3_collections_jni: ${library} skipped (${ARG_REQUIRES} is false)")
    foreach(class IN LISTS ARG_CLASSES)
      file(APPEND ${CMAKE_CURRENT_BINARY_DIR}/m3_skipped_jni_classes.txt "${class}\n")
    endforeach()
    return()
  endif()
  set(sources "")
  foreach(source IN LISTS ARG_SOURCES)
    list(APPEND sources ${M3_NATIVE}/${source})
  endforeach()
  add_library(${library} SHARED ${sources})
  # loadLibrary("x") looks for x.dll on Windows and libx.so on Linux; MinGW would otherwise name it libx.dll.
  if(WIN32)
    set_target_properties(${library} PROPERTIES PREFIX "")
  endif()
  set_target_properties(${library} PROPERTIES C_EXTENSIONS NO C_VISIBILITY_PRESET hidden)
  target_compile_features(${library} PRIVATE c_std_11)
  target_include_directories(${library} PRIVATE ${JNI_INCLUDE_DIRS} ${M3_JNI_HEADERS} ${M3_ROOT}/m3/native/include)
  set(includes "")
  set_property(GLOBAL APPEND PROPERTY M3_COLLECTIONS_JNI_LIBRARIES ${library})
  # A nested class is written Outer$Inner: javac -h names its header Outer_Inner.h; the source is Outer.java.
  foreach(class IN LISTS ARG_CLASSES)
    string(REGEX REPLACE "[.$]" "_" header ${class})
    string(REGEX REPLACE "[$].*" "" outer ${class})
    string(REPLACE "." "/" path ${outer})
    set(header ${M3_JNI_HEADERS}/${header}.h)
    if(MSVC)
      list(APPEND includes "/FI${header}")
    else()
      # SHELL: keeps each "-include <header>" pair together; plain options would be de-duplicated.
      list(APPEND includes "SHELL:-include ${header}")
    endif()
    list(APPEND M3_JNI_DECLARATIONS_LOCAL ${M3_CLASSES}/${path}.java)
    list(APPEND M3_JNI_HEADER_FILES_LOCAL ${header})
  endforeach()
  set(M3_JNI_DECLARATIONS ${M3_JNI_DECLARATIONS} ${M3_JNI_DECLARATIONS_LOCAL} PARENT_SCOPE)
  set(M3_JNI_HEADER_FILES ${M3_JNI_HEADER_FILES} ${M3_JNI_HEADER_FILES_LOCAL} PARENT_SCOPE)
  if(MSVC)
    target_compile_options(${library} PRIVATE /W4 /WX ${includes})
  else()
    target_compile_options(${library} PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic ${includes})
  endif()
endfunction()

# Self-loading owners: System.loadLibrary("<library>").
m3_collections_jni(m3_disjoint_index SOURCES collections/m3_disjoint_index.c
  CLASSES jdk.internal.m3.collections.DisjointIndex)
m3_collections_jni(m3_collections_address SOURCES collections/m3_long_address_kernel.c
  CLASSES jdk.internal.m3.collections.LaneAlgorithms jdk.m3.collections.LongAddressKernel)
m3_collections_jni(m3_collections_long_path_masks SOURCES collections/m3_long_index_path_masks_jni.c
  CLASSES jdk.internal.m3.collections.LongIndexPathMasksJNI)
m3_collections_jni(m3rankbatch SOURCES collections/m3_long_rank_batch_jni.c
  CLASSES jdk.internal.m3.collections.LongRankBatchNative)
m3_collections_jni(m3signalorder SOURCES collections/m3_object_signal_jni.c collections/m3_signal_order_jni.c
  CLASSES jdk.internal.m3.collections.ObjectSignalOps jdk.m3.collections.SignalOrderNative)
m3_collections_jni(m3_residency_state_mask SOURCES collections/m3_residency_state_mask.c
  CLASSES jdk.internal.m3.collections.ResidencyStateMaskIndex)
m3_collections_jni(m3_collections_statehist SOURCES collections/m3_state_mask_histogram.c
  CLASSES jdk.internal.m3.collections.sketch.StateMaskHistogram)
# IndexMaskWindow loads nothing itself: it calls its natives once IndexMaskCompactor reports m3_collections_mask
# loaded, so its C joins that library.
m3_collections_jni(m3_collections_mask
  SOURCES collections/m3_index_mask_compactor.c collections/m3_index_mask_window.c collections/m3_index_mask_search.c
  CLASSES jdk.m3.collections.IndexMaskCompactor jdk.m3.collections.IndexMaskWindow)
m3_collections_jni(m3_collections_pathmask SOURCES collections/m3_index_path_masks.c
  CLASSES jdk.m3.collections.IndexPathMasks)
m3_collections_jni(m3_collections_span SOURCES collections/m3_span_index_jni.c
  CLASSES jdk.m3.collections.SpanIndex)
m3_collections_jni(m3bitlane SOURCES collections/segmented_bit_lane.c
  CLASSES jdk.m3.collections.BitLane28)
# NATIVE_ABSORB N2 (synexia-primitives C, relocated by NATIVE_SYMBOL_RELOCATE v1).
m3_collections_jni(m3_packed_integer SOURCES collections/m3_packed_integer_jni.c
  CLASSES jdk.internal.m3.collections.packed.PackedNative)
m3_collections_jni(m3_big_longs SOURCES collections/m3_big_longs.c
  CLASSES jdk.internal.m3.collections.packed.PrimitiveBigNative)
m3_collections_jni(m3_doop_frozen_mask SOURCES collections/m3_doop_frozen_mask.c
  CLASSES jdk.internal.m3.collections.DoopFrozenMaskedViewport)
m3_collections_jni(m3_doop_mask_window SOURCES collections/m3_doop_mask_window.c
  CLASSES jdk.internal.m3.collections.DoopMaskedWindowScan)

# Host-loaded owners: the host passes the built file to System.load (m3.counted.native, m3.totals.native,
# m3.native.path); the library is named after its C owner.
m3_collections_jni(m3_count_lanes SOURCES collections/m3_count_lanes.c
  CLASSES jdk.internal.m3.collections.CountLanes)
m3_collections_jni(m3_count_totals SOURCES collections/m3_count_totals.c
  CLASSES jdk.internal.m3.collections.CountTotals)
m3_collections_jni(m3maskviewport SOURCES m3maskviewport.c
  CLASSES jdk.internal.m3.collections.MaskViewportNative)
m3_collections_jni(m3_value_bits REQUIRES M3_C_IEC_559 SOURCES collections/m3_value_bits.c
  CLASSES jdk.internal.m3.collections.PrimitiveFloat16)
m3_collections_jni(m3_primitive_order SOURCES collections/primitive_order_jni.c
  CLASSES jdk.m3.collections.PrimitivePartialSort$LongOrder)
# NATIVE_ABSORB N3 (synexia-common packed C). The host loads one library for the packed owners and the bit lane
# (ArrayStorageNativeContractTest: m3.native.path), as the donor packaged them; segmented_bit_lane.c is therefore
# linked here and into m3bitlane.
m3_collections_jni(m3_collections_packed
  SOURCES collections/packed_array_bulk.c collections/packed_heap_selection.c collections/packed_sorted_sets.c
    collections/packed_stable_filter.c collections/packed_long_mask_page.c collections/packed_swiss_probe_jni.c
    collections/segmented_short_lane.c collections/segmented_bit_lane.c
  CLASSES jdk.internal.m3.collections.packed.PackedArrayNative jdk.internal.m3.collections.packed.PackedSelectionNative
    jdk.internal.m3.collections.packed.PackedLongMaskPageNative jdk.internal.m3.collections.packed.PackedSwissNative
    jdk.internal.m3.collections.range.SegmentedShortLane28 jdk.m3.collections.BitLane28)
if(MINGW)
  # MinGW's signbit() macro instantiates its float branch for double arguments (packed_selection_jni.h
  # ps_compare_Double), which -Wfloat-conversion rejects; glibc and clang use a type-generic builtin.
  target_compile_options(m3_collections_packed PRIVATE -Wno-float-conversion)
endif()
# NATIVE_ABSORB N5 (synexia-mat-collections C): MatCsrGraph loads m3_mat_csr (or m3.matcsr.native.path),
# MatIntFilterKernel loads m3_matfilter after the m3.matcollections.jni opt-in.
m3_collections_jni(m3_mat_csr SOURCES collections/m3csrtranspose.c
  CLASSES jdk.internal.m3.collections.mat.MatCsrGraph)
m3_collections_jni(m3_matfilter SOURCES collections/mat_int_filter_jni.c
  CLASSES jdk.internal.m3.collections.mat.MatIntFilterKernel)

# One javac -h run writes the headers of every declaring class; javac compiles their dependencies implicitly.
list(REMOVE_DUPLICATES M3_JNI_DECLARATIONS)
list(REMOVE_DUPLICATES M3_JNI_HEADER_FILES)
add_custom_command(OUTPUT ${M3_JNI_HEADER_FILES}
  COMMAND ${CMAKE_COMMAND} -E make_directory ${M3_JNI_OUT} ${CMAKE_CURRENT_BINARY_DIR}/jni-classes
  COMMAND ${Java_JAVAC_EXECUTABLE} -proc:none -nowarn -encoding UTF-8
    -h ${M3_JNI_OUT} -d ${CMAKE_CURRENT_BINARY_DIR}/jni-classes
    --module-source-path jdk.m3.collections=${M3_CLASSES} ${M3_JNI_DECLARATIONS}
  DEPENDS ${M3_JNI_DECLARATIONS} VERBATIM)
add_custom_target(m3_collections_jni_headers DEPENDS ${M3_JNI_HEADER_FILES})
get_property(M3_COLLECTIONS_JNI_LIBRARIES GLOBAL PROPERTY M3_COLLECTIONS_JNI_LIBRARIES)
foreach(library IN LISTS M3_COLLECTIONS_JNI_LIBRARIES)
  add_dependencies(${library} m3_collections_jni_headers)
endforeach()
