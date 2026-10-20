# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-20: reuse canonical Android JNI and page-size build contracts.
cmake_minimum_required(VERSION 3.20)
project(m3_arrays LANGUAGES C)

if(ANDROID)
  if(NOT CMAKE_ANDROID_NDK)
    message(FATAL_ERROR "Android JNI requires an installed NDK toolchain")
  endif()
  # NDK jni.h comes from the target sysroot, without host JVM/AWT discovery.
else()
  find_package(JNI REQUIRED)
endif()

add_library(m3_arrays SHARED
  m3_arrays.c
  m3_arrays_jni.c)

if(NOT ANDROID)
  target_include_directories(m3_arrays PRIVATE ${JNI_INCLUDE_DIRS})
endif()
target_include_directories(m3_arrays PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(m3_arrays PRIVATE c_std_11)
if(ANDROID)
  target_link_options(m3_arrays PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")
endif()
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
endif()
