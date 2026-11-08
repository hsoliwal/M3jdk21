# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-29: retain existing arrays targets and add packed/full64 codecs.
cmake_minimum_required(VERSION 3.20)
project(m3_arrays LANGUAGES C)
# Re-pointed by m3/tooling/jdk-naming/move_module_area.py (MOVE_TO_JDK_MODULE v2): the sources live in the
# JDK module src/jdk.m3.collections/share/native/libm3arrays and the C unit tests in test/jdk/jdk/m3/arrays/native.
set(M3_NATIVE ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/jdk.m3.collections/share/native/libm3arrays)
set(M3_NATIVE_TESTS ${CMAKE_CURRENT_SOURCE_DIR}/../../../test/jdk/jdk/m3/arrays/native)
# The shared JNI core header of m3/native (the moved sources include it as m3_jni_core.h).
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../m3/native/include)

find_package(JNI REQUIRED)

add_library(m3_arrays SHARED
  ${M3_NATIVE}/m3_arrays.c
  ${M3_NATIVE}/m3_arrays_jni.c
  ${M3_NATIVE}/m3_packed_codec.c
  ${M3_NATIVE}/m3_packed_codec_jni.c
  ${M3_NATIVE}/m3_int_codebook.c
  ${M3_NATIVE}/m3_int_codebook_jni.c)

target_include_directories(
  m3_arrays
  PRIVATE ${JNI_INCLUDE_DIRS}
  PUBLIC ${M3_NATIVE}/include)
target_compile_features(m3_arrays PRIVATE c_std_11)
if(MSVC)
  target_compile_options(m3_arrays PRIVATE /W4 /WX)
else()
  target_compile_options(m3_arrays PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
endif()

include(CTest)
if(BUILD_TESTING)
  add_executable(m3_arrays_checks ${M3_NATIVE_TESTS}/m3_arrays_checks.c ${M3_NATIVE}/m3_arrays.c)
  target_include_directories(m3_arrays_checks PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_arrays_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_arrays_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_arrays_checks PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_arrays_checks COMMAND m3_arrays_checks)

  # Synexia packed/full64 kernels join the existing M3 arrays native owner.
  add_executable(m3_packed_codec_checks ${M3_NATIVE_TESTS}/m3_packed_codec_checks.c ${M3_NATIVE}/m3_packed_codec.c)
  target_include_directories(m3_packed_codec_checks PRIVATE ${M3_NATIVE}/include)
  add_executable(m3_packed_codec_jni_fault_test
    ${M3_NATIVE_TESTS}/m3_packed_codec_jni_fault_test.c ${M3_NATIVE}/m3_packed_codec.c ${M3_NATIVE}/m3_packed_codec_jni.c)
  target_include_directories(m3_packed_codec_jni_fault_test
    PRIVATE ${JNI_INCLUDE_DIRS} ${M3_NATIVE}/include)
  add_library(m3_packed_null_fixture SHARED ${M3_NATIVE_TESTS}/m3_packed_null_fixture.c)
  target_include_directories(m3_packed_null_fixture PRIVATE ${JNI_INCLUDE_DIRS})
  foreach(codec_test IN ITEMS m3_packed_codec_checks m3_packed_codec_jni_fault_test m3_packed_null_fixture)
    target_compile_features(${codec_test} PRIVATE c_std_11)
    if(MSVC)
      target_compile_options(${codec_test} PRIVATE /W4 /WX)
    else()
      target_compile_options(${codec_test} PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
    endif()
  endforeach()
  add_test(NAME m3_packed_codec_checks COMMAND m3_packed_codec_checks)
  add_test(NAME m3_packed_codec_jni_fault_test COMMAND m3_packed_codec_jni_fault_test)

  # Exact integer codebook (weight-sharing analogue): strict C11 donor-neutral oracle.
  add_executable(m3_int_codebook_checks
    ${M3_NATIVE_TESTS}/m3_int_codebook_checks.c ${M3_NATIVE}/m3_int_codebook.c)
  target_include_directories(m3_int_codebook_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_int_codebook_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_int_codebook_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_int_codebook_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_int_codebook_checks COMMAND m3_int_codebook_checks)

endif()

# All primitive allocation atoms reuse the actual array JNI owner above.
if(BUILD_TESTING)
  add_executable(m3_array_allocation_fault_test
    ${M3_NATIVE_TESTS}/m3_array_allocation_fault_test.c ${M3_NATIVE}/m3_arrays.c ${M3_NATIVE}/m3_arrays_jni.c)
  target_include_directories(m3_array_allocation_fault_test
    PRIVATE ${JNI_INCLUDE_DIRS} ${M3_NATIVE}/include)
  add_library(m3_array_allocation_null_fixture SHARED ${M3_NATIVE_TESTS}/m3_array_allocation_null_fixture.c)
  target_include_directories(m3_array_allocation_null_fixture PRIVATE ${JNI_INCLUDE_DIRS})
  foreach(allocation_test IN ITEMS m3_array_allocation_fault_test m3_array_allocation_null_fixture)
    target_compile_features(${allocation_test} PRIVATE c_std_11)
    if(MSVC)
      target_compile_options(${allocation_test} PRIVATE /W4 /WX /UNDEBUG)
    else()
      target_compile_options(${allocation_test} PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic -UNDEBUG)
    endif()
  endforeach()
  add_test(NAME m3_array_allocation_fault_test COMMAND m3_array_allocation_fault_test)
endif()

# M3 LeetCodify signed-int search uses the existing array JNI library.
# Its Java-facing kernel uses GetIntArrayRegion, never a retained native borrow.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_int_lane_search.c
  ${M3_NATIVE}/m3_int_lane_search_jni.c)

if(BUILD_TESTING)
  add_executable(m3_int_lane_search_checks
    ${M3_NATIVE_TESTS}/m3_int_lane_search_checks.c
    ${M3_NATIVE}/m3_int_lane_search.c)
  target_include_directories(m3_int_lane_search_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_int_lane_search_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_int_lane_search_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_int_lane_search_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_int_lane_search_checks COMMAND m3_int_lane_search_checks)
endif()

# Sparse long-index integer primitive lane: required JNI occupancy audit.
# No GetPrimitiveArrayCritical or retained Java array pointers are used.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_sparse_nonzero.c
  ${M3_NATIVE}/m3_sparse_nonzero_jni.c)

if(BUILD_TESTING)
  add_executable(m3_sparse_nonzero_checks
    ${M3_NATIVE_TESTS}/m3_sparse_nonzero_checks.c
    ${M3_NATIVE}/m3_sparse_nonzero.c)
  target_include_directories(m3_sparse_nonzero_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_sparse_nonzero_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_sparse_nonzero_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_sparse_nonzero_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_sparse_nonzero_checks COMMAND m3_sparse_nonzero_checks)
endif()

# Long-coordinate binary search: reuse the existing bounded M3 arrays JNI owner.
# No permanent JNI array borrows; signed-long native oracle is independent.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_long_coordinate_search.c
  ${M3_NATIVE}/m3_long_coordinate_search_jni.c)

if(BUILD_TESTING)
  add_executable(m3_long_coordinate_search_checks
    ${M3_NATIVE_TESTS}/m3_long_coordinate_search_checks.c
    ${M3_NATIVE}/m3_long_coordinate_search.c)
  target_include_directories(m3_long_coordinate_search_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_long_coordinate_search_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_long_coordinate_search_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_long_coordinate_search_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_long_coordinate_search_checks COMMAND m3_long_coordinate_search_checks)
endif()

# Sorted primitive multiset algebra: one checked JNI owner, no borrowed Java pointers.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_multiset_step.c
  ${M3_NATIVE}/m3_multiset_jni.c)

if(BUILD_TESTING)
  add_executable(m3_multiset_step_checks
    ${M3_NATIVE_TESTS}/m3_multiset_step_checks.c
    ${M3_NATIVE}/m3_multiset_step.c)
  target_include_directories(m3_multiset_step_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_multiset_step_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_multiset_step_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_multiset_step_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_multiset_step_checks COMMAND m3_multiset_step_checks)
endif()

# Synexia CSR posting-list union/intersection/count kernels join the existing arrays JNI owner
# (recipe m3/arrays/recipe/posting-index-20261106): Java_jdk_m3_arrays_IntPostingIndex_* take one
# validated immutable query batch, use GetIntArrayElements with JNI_ABORT release only, and retain no pointer.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_posting_merge.c)

# Synexia dense bit-matrix transpose kernel (six-stage 64x64 broadword tile swaps, exact scalar fallback) joins the
# existing arrays JNI owner (recipe m3/arrays/recipe/bitmatrix-20261106): Java_jdk_m3_arrays_BitMatrix_{nativeAbi0,
# transposeNative0} take one detached immutable word snapshot, release every GetLongArrayElements borrow before
# returning (JNI_ABORT on the input), publish nothing partial and retain no pointer.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_bit_matrix.c)

# Exact queries scan packed storage in bounded JNI chunks without full expansion.
if(BUILD_TESTING)
  add_executable(m3_packed_query_checks ${M3_NATIVE_TESTS}/m3_packed_query_checks.c ${M3_NATIVE}/m3_packed_codec.c)
  add_executable(m3_packed_query_jni_fault_test
    ${M3_NATIVE_TESTS}/m3_packed_query_jni_fault_test.c ${M3_NATIVE}/m3_packed_codec.c ${M3_NATIVE}/m3_packed_codec_jni.c)
  foreach(query_test IN ITEMS m3_packed_query_checks m3_packed_query_jni_fault_test)
    target_include_directories(${query_test} PRIVATE ${JNI_INCLUDE_DIRS} ${M3_NATIVE}/include)
    target_compile_features(${query_test} PRIVATE c_std_17)
    if(MSVC)
      target_compile_options(${query_test} PRIVATE /W4 /WX)
    else()
      target_compile_options(${query_test} PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
    endif()
    add_test(NAME ${query_test} COMMAND ${query_test})
  endforeach()
endif()  # closes if(BUILD_TESTING) of the exact-query checks (missing on master since 83d44d514d0; fixed by the arrays move)
# Lossless int32 adaptive bitplanes share the existing M3 arrays JNI library.
# JNI performs bounded primitive-array region transfers; pure C atoms stay reusable.
target_sources(m3_arrays PRIVATE
  ${M3_NATIVE}/m3_adaptive_bitpack.c
  ${M3_NATIVE}/m3_adaptive_bitpack_jni.c)

if(BUILD_TESTING)
  add_executable(m3_adaptive_bitpack_checks
    ${M3_NATIVE_TESTS}/m3_adaptive_bitpack_checks.c
    ${M3_NATIVE}/m3_adaptive_bitpack.c)
  target_include_directories(m3_adaptive_bitpack_checks
    PRIVATE ${M3_NATIVE}/include)
  target_compile_features(m3_adaptive_bitpack_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_adaptive_bitpack_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_adaptive_bitpack_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_adaptive_bitpack_checks COMMAND m3_adaptive_bitpack_checks)
endif()
