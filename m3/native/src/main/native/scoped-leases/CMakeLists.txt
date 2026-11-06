# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/native/m3-scoped-leases/CMakeLists.txt blob 2d626edb6b90; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
cmake_minimum_required(VERSION 3.20)
project(m3_scoped_leases LANGUAGES C)
find_package(Java 21 REQUIRED COMPONENTS Development)
find_package(JNI REQUIRED)
set(owner "${CMAKE_CURRENT_SOURCE_DIR}/../../java/jdk/m3/jni")
set(generated "${CMAKE_CURRENT_BINARY_DIR}/generated")
set(header "${generated}/jdk_m3_jni_M3NativeLeaseScan.h")
set(concat_header "${generated}/jdk_m3_jni_M3JniByteConcat.h")
add_custom_command(OUTPUT "${header}" "${concat_header}"
  COMMAND "${CMAKE_COMMAND}" -E make_directory "${generated}"
  COMMAND "${Java_JAVAC_EXECUTABLE}" --release 21 -proc:none -Xlint:all -Werror
    -h "${generated}" -d "${generated}"
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../ports/collections/src/main/java/jdk/m3/util/M3RuntimeHeap.java"
    "${owner}/M3ScopedLeaseTable.java"
    "${owner}/M3NativeLeaseScan.java"
    "${owner}/M3JniArrayReferences.java" "${owner}/M3JniByteConcat.java"
  DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../ports/collections/src/main/java/jdk/m3/util/M3RuntimeHeap.java" "${owner}/M3ScopedLeaseTable.java"
          "${owner}/M3NativeLeaseScan.java"
          "${owner}/M3JniArrayReferences.java" "${owner}/M3JniByteConcat.java"
  VERBATIM)
add_library(m3_scoped_leases SHARED ../m3_scoped_leases.c "${header}" "${concat_header}")
target_include_directories(m3_scoped_leases PRIVATE ${JNI_INCLUDE_DIRS} "${generated}")
set_target_properties(m3_scoped_leases PROPERTIES C_STANDARD 17 C_STANDARD_REQUIRED YES C_EXTENSIONS NO)
if(MSVC)
  target_compile_options(m3_scoped_leases PRIVATE /W4 /WX)
else()
  target_compile_options(m3_scoped_leases PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow)
endif()
