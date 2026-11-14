# SPDX-License-Identifier: Apache-2.0
cmake_minimum_required(VERSION 3.20)
project(m3_llm_ncm LANGUAGES C CXX)

find_package(JNI REQUIRED)

# The single M3JDK NCM native owner (library m3_ncm):
# - ncm_kernel.cpp: float nearest-code kernels, m3_ncm_nearest (binary64 ordered
#   differences; M3NcmNative via ncm_jni.cpp) and m3_ncm_nearest_expanded_f32 (FP32
#   expanded norm; M3NcmNativeOps via ncm_projection_jni.cpp, which also carries the
#   resize, Phi and tied-logit primitives translated from HazyResearch/ncm, MIT, see
#   NCM_DONOR_NOTICE.md);
# - m3_ncm_codebook.c: the binary64 offset-batch codebook for NativePrecomputeOps (below).
# Java remains the oracle. No weights, inference server or third-party runtime is added.
add_library(m3_ncm SHARED src/ncm_kernel.cpp src/ncm_jni.cpp src/ncm_projection_jni.cpp)
target_include_directories(m3_ncm PRIVATE ${JNI_INCLUDE_DIRS} include)
target_compile_features(m3_ncm PRIVATE cxx_std_20)
if(MSVC)
  target_compile_options(m3_ncm PRIVATE /W4 /WX /fp:strict)
else()
  target_compile_options(m3_ncm PRIVATE
    -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic
    -fno-fast-math -ffp-contract=off)
endif()

option(M3_NCM_TESTS "Build the bounded standalone NCM C ABI oracle" OFF)
option(M3_NCM_SANITIZERS "Sanitize the standalone NCM C ABI oracle" OFF)
if(M3_NCM_TESTS)
  enable_testing()
  add_executable(m3_ncm_kernel_test tests/ncm_kernel_test.cpp src/ncm_kernel.cpp)
  target_include_directories(m3_ncm_kernel_test PRIVATE include)
  target_compile_features(m3_ncm_kernel_test PRIVATE cxx_std_20)
  if(MSVC)
    target_compile_options(m3_ncm_kernel_test PRIVATE /W4 /WX /fp:strict)
  else()
    target_compile_options(m3_ncm_kernel_test PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic
      -fno-fast-math -ffp-contract=off)
    if(M3_NCM_SANITIZERS)
      target_compile_options(m3_ncm_kernel_test PRIVATE
        -fsanitize=address,undefined -fno-omit-frame-pointer)
      target_link_options(m3_ncm_kernel_test PRIVATE -fsanitize=address,undefined)
    endif()
  endif()
  add_test(NAME m3_ncm_kernel COMMAND m3_ncm_kernel_test)
endif()

# Binary64 offset-batch codebook entry for jdk.m3.precompute.NativePrecomputeOps
# (ncmCodebookAbiVersionNative, nearestNcmCodesNative), moved from the m3_precompute
# library so that m3_ncm is the only NCM native library.
target_sources(m3_ncm PRIVATE src/m3_ncm_codebook.c src/m3_ncm_codebook_jni.c)
target_compile_features(m3_ncm PRIVATE c_std_17)
if(NOT MSVC)
  target_link_libraries(m3_ncm PRIVATE m)
endif()
file(GENERATE
  OUTPUT "${CMAKE_BINARY_DIR}/m3_ncm-path-$<CONFIG>.txt"
  CONTENT "$<TARGET_FILE:m3_ncm>")

if(M3_NCM_TESTS)
  add_executable(m3_ncm_codebook_checks tests/m3_ncm_codebook_checks.c src/m3_ncm_codebook.c)
  target_include_directories(m3_ncm_codebook_checks PRIVATE include)
  target_compile_features(m3_ncm_codebook_checks PRIVATE c_std_17)
  if(MSVC)
    target_compile_options(m3_ncm_codebook_checks PRIVATE /W4 /WX /fp:strict)
  else()
    target_compile_options(m3_ncm_codebook_checks PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic
      -fno-fast-math -ffp-contract=off)
    target_link_libraries(m3_ncm_codebook_checks PRIVATE m)
  endif()
  add_test(NAME m3_ncm_codebook_checks COMMAND m3_ncm_codebook_checks)
endif()
