# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-29: retain existing arrays targets and add packed/full64 codecs.
cmake_minimum_required(VERSION 3.20)
project(m3_arrays LANGUAGES C)

find_package(JNI REQUIRED)

add_library(m3_arrays SHARED
  m3_arrays.c
  m3_arrays_jni.c
  m3_packed_codec.c
  m3_packed_codec_jni.c)

target_include_directories(
  m3_arrays
  PRIVATE ${JNI_INCLUDE_DIRS}
  PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(m3_arrays PRIVATE c_std_11)
if(MSVC)
  target_compile_options(m3_arrays PRIVATE /W4 /WX)
else()
  target_compile_options(m3_arrays PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
endif()

include(CTest)
if(BUILD_TESTING)
  add_executable(m3_arrays_checks tests/m3_arrays_checks.c m3_arrays.c)
  target_include_directories(m3_arrays_checks PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
  target_compile_features(m3_arrays_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_arrays_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_arrays_checks PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_arrays_checks COMMAND m3_arrays_checks)

  # Synexia packed/full64 kernels join the existing M3 arrays native owner.
  add_executable(m3_packed_codec_checks tests/m3_packed_codec_checks.c m3_packed_codec.c)
  target_include_directories(m3_packed_codec_checks PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
  add_executable(m3_packed_codec_jni_fault_test
    tests/m3_packed_codec_jni_fault_test.c m3_packed_codec.c m3_packed_codec_jni.c)
  target_include_directories(m3_packed_codec_jni_fault_test
    PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/include)
  add_library(m3_packed_null_fixture SHARED tests/m3_packed_null_fixture.c)
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
endif()

# All primitive allocation atoms reuse the actual array JNI owner above.
if(BUILD_TESTING)
  add_executable(m3_array_allocation_fault_test
    tests/m3_array_allocation_fault_test.c m3_arrays.c m3_arrays_jni.c)
  target_include_directories(m3_array_allocation_fault_test
    PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/include)
  add_library(m3_array_allocation_null_fixture SHARED tests/m3_array_allocation_null_fixture.c)
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
  m3_int_lane_search.c
  m3_int_lane_search_jni.c)

if(BUILD_TESTING)
  add_executable(m3_int_lane_search_checks
    tests/m3_int_lane_search_checks.c
    m3_int_lane_search.c)
  target_include_directories(m3_int_lane_search_checks
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
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
  m3_sparse_nonzero.c
  m3_sparse_nonzero_jni.c)

if(BUILD_TESTING)
  add_executable(m3_sparse_nonzero_checks
    tests/m3_sparse_nonzero_checks.c
    m3_sparse_nonzero.c)
  target_include_directories(m3_sparse_nonzero_checks
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
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
  m3_long_coordinate_search.c
  m3_long_coordinate_search_jni.c)

if(BUILD_TESTING)
  add_executable(m3_long_coordinate_search_checks
    tests/m3_long_coordinate_search_checks.c
    m3_long_coordinate_search.c)
  target_include_directories(m3_long_coordinate_search_checks
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
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
  m3_multiset_step.c
  m3_multiset_jni.c)

if(BUILD_TESTING)
  add_executable(m3_multiset_step_checks
    tests/m3_multiset_step_checks.c
    m3_multiset_step.c)
  target_include_directories(m3_multiset_step_checks
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
  target_compile_features(m3_multiset_step_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_multiset_step_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_multiset_step_checks
      PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_multiset_step_checks COMMAND m3_multiset_step_checks)
endif()
