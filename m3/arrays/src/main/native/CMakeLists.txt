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
