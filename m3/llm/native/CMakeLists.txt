# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
cmake_minimum_required(VERSION 3.20)
project(m3_llm_ncm C)
find_package(JNI REQUIRED)
add_library(m3_llm_ncm SHARED ncm_jni.c)
target_include_directories(m3_llm_ncm PRIVATE ${JNI_INCLUDE_DIRS})
target_compile_features(m3_llm_ncm PRIVATE c_std_17)
if(MSVC)
  target_compile_options(m3_llm_ncm PRIVATE /W4 /WX /fp:strict)
else()
  target_compile_options(m3_llm_ncm PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic -fno-fast-math -ffp-contract=off)
  target_link_libraries(m3_llm_ncm PRIVATE m)
endif()
