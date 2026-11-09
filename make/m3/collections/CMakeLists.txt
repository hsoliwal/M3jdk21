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

# m3_collections_jni(<library> SOURCES <C files under libm3collections> CLASSES <declaring class FQNs>)
function(m3_collections_jni library)
  cmake_parse_arguments(ARG "" "" "SOURCES;CLASSES" ${ARGN})
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
  foreach(class IN LISTS ARG_CLASSES)
    string(REPLACE "." "_" header ${class})
    string(REPLACE "." "/" path ${class})
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
m3_collections_jni(m3_collections_mask SOURCES collections/m3_index_mask_compactor.c collections/m3_index_mask_window.c
  CLASSES jdk.m3.collections.IndexMaskCompactor jdk.m3.collections.IndexMaskWindow)
m3_collections_jni(m3_collections_pathmask SOURCES collections/m3_index_path_masks.c
  CLASSES jdk.m3.collections.IndexPathMasks)
m3_collections_jni(m3_collections_span SOURCES collections/m3_span_index_jni.c
  CLASSES jdk.m3.collections.SpanIndex)
m3_collections_jni(m3bitlane SOURCES collections/segmented_bit_lane.c
  CLASSES jdk.m3.collections.BitLane28)

# Host-loaded owners: the host passes the built file to System.load (m3.counted.native, m3.totals.native,
# m3.native.path); the library is named after its C owner.
m3_collections_jni(m3_count_lanes SOURCES collections/m3_count_lanes.c
  CLASSES jdk.internal.m3.collections.CountLanes)
m3_collections_jni(m3_count_totals SOURCES collections/m3_count_totals.c
  CLASSES jdk.internal.m3.collections.CountTotals)
m3_collections_jni(m3maskviewport SOURCES m3maskviewport.c
  CLASSES jdk.internal.m3.collections.MaskViewportNative)

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
foreach(library IN ITEMS m3_disjoint_index m3_collections_address m3_collections_long_path_masks m3rankbatch
    m3signalorder m3_residency_state_mask m3_collections_statehist m3_collections_mask m3_collections_pathmask
    m3_collections_span m3bitlane m3_count_lanes m3_count_totals m3maskviewport)
  add_dependencies(${library} m3_collections_jni_headers)
endforeach()
