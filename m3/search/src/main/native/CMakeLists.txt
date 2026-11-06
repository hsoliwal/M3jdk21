# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# m3/search native kernels. m3search = M3JniSearchEngine + M3BatchOffsetSearch/M3BatchReverseSearch atoms
# (donor m3-fast-search CMake target, renamed). The sidecars back the property-gated native tests of the window,
# concatenation, subsequence and bitap kernels.
cmake_minimum_required(VERSION 3.16)
project(m3_search C)
find_package(JNI REQUIRED)
function(m3_search_jni name source)
  add_library(${name} SHARED ${source})
  target_include_directories(${name} PRIVATE ${JNI_INCLUDE_DIRS})
  target_compile_features(${name} PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(${name} PRIVATE /W4 /WX)
  else()
    target_compile_options(${name} PRIVATE -Wall -Wextra -Werror -O3 -fPIC)
  endif()
endfunction()
m3_search_jni(m3search jni_search.c)
m3_search_jni(m3coverwindow minimum_cover_window_jni.c)
m3_search_jni(m3subseqwindow minimum_subsequence_window_jni.c)
m3_search_jni(m3wordconcat fixed_word_concat_jni.c)
m3_search_jni(m3batchsubseq batch_subsequence_match_jni.c)
m3_search_jni(m3bitap jni_bitap_exact.c)
