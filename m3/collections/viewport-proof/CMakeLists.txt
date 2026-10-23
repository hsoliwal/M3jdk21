# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
# Adapted from the Synexia keyed JNI build; compile the existing viewport owner.
cmake_minimum_required(VERSION 3.24)
if(CMAKE_SOURCE_DIR STREQUAL CMAKE_BINARY_DIR)
  message(FATAL_ERROR "Use an explicit out-of-source build directory")
endif()
project(m3maskviewport VERSION 1.0.0 LANGUAGES C)
if(CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT OR
   NOT IS_ABSOLUTE "${CMAKE_INSTALL_PREFIX}" OR CMAKE_INSTALL_PREFIX STREQUAL "/")
  message(FATAL_ERROR "Set CMAKE_INSTALL_PREFIX to an approved explicit destination")
endif()
find_package(Java 21 REQUIRED COMPONENTS Development)
find_package(JNI REQUIRED COMPONENTS JVM)
set(owner "${CMAKE_CURRENT_LIST_DIR}/../src/main/c/m3maskviewport.c")
set(declaration "${CMAKE_CURRENT_LIST_DIR}/../src/main/java/com/m3/collections/M3MaskViewportNative.java")
set(header "${CMAKE_CURRENT_BINARY_DIR}/jni/com_m3_collections_M3MaskViewportNative.h")
add_custom_command(OUTPUT "${header}"
  COMMAND "${CMAKE_COMMAND}" -E make_directory
    "${CMAKE_CURRENT_BINARY_DIR}/jni" "${CMAKE_CURRENT_BINARY_DIR}/jni-classes"
  COMMAND "${Java_JAVAC_EXECUTABLE}" --release 21 -encoding UTF-8 -Xlint:all -Werror
    -h "${CMAKE_CURRENT_BINARY_DIR}/jni" -d "${CMAKE_CURRENT_BINARY_DIR}/jni-classes"
    "${declaration}"
  DEPENDS "${declaration}" VERBATIM)
add_library(m3maskviewport SHARED "${owner}" "${header}")
set_target_properties(m3maskviewport PROPERTIES
  C_STANDARD 17 C_STANDARD_REQUIRED YES C_EXTENSIONS NO C_VISIBILITY_PRESET hidden)
# Header-only JNI interface: do not introduce libjvm into the hosting process.
target_link_libraries(m3maskviewport PRIVATE JNI::JNI)
if(MSVC)
  target_compile_options(m3maskviewport PRIVATE /W4 /WX "/FI${header}")
else()
  target_compile_options(m3maskviewport PRIVATE -Wall -Wextra -Wpedantic -Werror
    -Wconversion -Wshadow -include "${header}")
endif()
install(TARGETS m3maskviewport LIBRARY DESTINATION lib RUNTIME DESTINATION bin)
