# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
cmake_minimum_required(VERSION 3.20)
project(m3_llm_ncm LANGUAGES C CXX)

find_package(JNI REQUIRED)

# Existing standalone bridge for the relocated JDK module source owner.
get_filename_component(M3_LLM_REPOSITORY_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../.." ABSOLUTE)
set(M3_NCM_SOURCE "${M3_LLM_REPOSITORY_ROOT}/src/jdk.m3.llm/share/native/libm3_ncm")
set(M3_NCM_TEST_SOURCE "${M3_LLM_REPOSITORY_ROOT}/test/jdk/jdk/m3/llm/native")

# The single M3JDK NCM native owner (library m3_ncm):
# - ncm_kernel.cpp: float nearest-code kernels, m3_ncm_nearest (binary64 ordered
#   differences; M3NcmNative via ncm_jni.cpp) and m3_ncm_nearest_expanded_f32 (FP32
#   expanded norm; M3NcmNativeOps via ncm_projection_jni.cpp, which also carries the
#   resize, Phi and tied-logit primitives translated from HazyResearch/ncm, MIT, see
#   NCM_DONOR_NOTICE.md);
# - m3_ncm_codebook.c: the binary64 offset-batch codebook for NativePrecomputeOps (below).
# Java remains the oracle. No weights, inference server or third-party runtime is added.
add_library(m3_ncm SHARED "${M3_NCM_SOURCE}/ncm_kernel.cpp" "${M3_NCM_SOURCE}/ncm_jni.cpp" "${M3_NCM_SOURCE}/ncm_projection_jni.cpp")
target_include_directories(m3_ncm PRIVATE ${JNI_INCLUDE_DIRS} "${M3_NCM_SOURCE}")
target_compile_features(m3_ncm PRIVATE cxx_std_20)
if(MSVC)
  target_compile_options(m3_ncm PRIVATE /W4 /WX /fp:strict /EHsc)
else()
  target_compile_options(m3_ncm PRIVATE
    -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic
    -fno-fast-math -ffp-contract=off)
endif()

option(M3_NCM_TESTS "Build the bounded standalone NCM C ABI oracle" OFF)
option(M3_NCM_SANITIZERS "Sanitize the standalone NCM C ABI oracle" OFF)
if(M3_NCM_TESTS)
  enable_testing()
  add_executable(m3_ncm_kernel_test "${M3_NCM_TEST_SOURCE}/ncm_kernel_test.cpp" "${M3_NCM_SOURCE}/ncm_kernel.cpp")
  target_include_directories(m3_ncm_kernel_test PRIVATE "${M3_NCM_SOURCE}")
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
target_sources(m3_ncm PRIVATE "${M3_NCM_SOURCE}/m3_ncm_codebook.c" "${M3_NCM_SOURCE}/m3_ncm_codebook_jni.c")
target_compile_features(m3_ncm PRIVATE c_std_17)
set_target_properties(m3_ncm PROPERTIES C_STANDARD 17 C_STANDARD_REQUIRED YES C_EXTENSIONS NO
  CXX_STANDARD 20 CXX_STANDARD_REQUIRED YES CXX_EXTENSIONS NO)
if(NOT MSVC)
  target_link_libraries(m3_ncm PRIVATE m)
endif()
file(GENERATE
  OUTPUT "${CMAKE_BINARY_DIR}/m3_ncm-path-$<CONFIG>.txt"
  CONTENT "$<TARGET_FILE:m3_ncm>")

if(M3_NCM_TESTS)
  add_executable(m3_ncm_codebook_checks "${M3_NCM_TEST_SOURCE}/m3_ncm_codebook_checks.c" "${M3_NCM_SOURCE}/m3_ncm_codebook.c")
  target_include_directories(m3_ncm_codebook_checks PRIVATE "${M3_NCM_SOURCE}")
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

# Qualification supplies javac -h output. Keep the existing no-header-directory
# standalone invocation working; JDK builds always use generated module headers.
set(M3_NCM_JNI_HEADER_DIR "" CACHE PATH "Optional directory containing generated jdk.m3.llm JNI headers")
if(M3_NCM_JNI_HEADER_DIR)
  set(M3_NCM_F64_JNI_HEADER "${M3_NCM_JNI_HEADER_DIR}/jdk_m3_llm_model_M3NcmNative.h")
  set(M3_NCM_FP32_JNI_HEADER "${M3_NCM_JNI_HEADER_DIR}/jdk_m3_llm_projection_M3NcmNativeOps.h")
  if(NOT EXISTS "${M3_NCM_F64_JNI_HEADER}" OR NOT EXISTS "${M3_NCM_FP32_JNI_HEADER}")
    message(FATAL_ERROR "M3_NCM_JNI_HEADER_DIR must contain both actual javac -h module headers")
  endif()
  if(MSVC)
    set_property(SOURCE "${M3_NCM_SOURCE}/ncm_jni.cpp" APPEND PROPERTY COMPILE_OPTIONS "/FI${M3_NCM_F64_JNI_HEADER}")
    set_property(SOURCE "${M3_NCM_SOURCE}/ncm_projection_jni.cpp" APPEND PROPERTY COMPILE_OPTIONS "/FI${M3_NCM_FP32_JNI_HEADER}")
  else()
    set_property(SOURCE "${M3_NCM_SOURCE}/ncm_jni.cpp" APPEND PROPERTY COMPILE_OPTIONS "-include" "${M3_NCM_F64_JNI_HEADER}")
    set_property(SOURCE "${M3_NCM_SOURCE}/ncm_projection_jni.cpp" APPEND PROPERTY COMPILE_OPTIONS "-include" "${M3_NCM_FP32_JNI_HEADER}")
  endif()
  set_property(SOURCE "${M3_NCM_SOURCE}/ncm_jni.cpp" APPEND PROPERTY OBJECT_DEPENDS "${M3_NCM_F64_JNI_HEADER}")
  set_property(SOURCE "${M3_NCM_SOURCE}/ncm_projection_jni.cpp" APPEND PROPERTY OBJECT_DEPENDS "${M3_NCM_FP32_JNI_HEADER}")
endif()

if(M3_NCM_TESTS)
  add_executable(ncm_controlled_kernel_test "${M3_NCM_TEST_SOURCE}/ncm_controlled_kernel_test.cpp" "${M3_NCM_SOURCE}/ncm_kernel.cpp")
  target_include_directories(ncm_controlled_kernel_test PRIVATE "${M3_NCM_SOURCE}")
  target_compile_features(ncm_controlled_kernel_test PRIVATE cxx_std_20)
  set_target_properties(ncm_controlled_kernel_test PROPERTIES CXX_STANDARD 20 CXX_STANDARD_REQUIRED YES CXX_EXTENSIONS NO)
  if(MSVC)
    target_compile_options(ncm_controlled_kernel_test PRIVATE /W4 /WX /fp:strict)
  else()
    target_compile_options(ncm_controlled_kernel_test PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic
      -fno-fast-math -ffp-contract=off)
    if(M3_NCM_SANITIZERS)
      target_compile_options(ncm_controlled_kernel_test PRIVATE
        -fsanitize=address,undefined -fno-omit-frame-pointer)
      target_link_options(ncm_controlled_kernel_test PRIVATE -fsanitize=address,undefined)
    endif()
  endif()
  add_test(NAME ncm_controlled_kernel COMMAND ncm_controlled_kernel_test)
endif()
