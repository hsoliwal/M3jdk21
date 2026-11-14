# SPDX-License-Identifier: Apache-2.0
cmake_minimum_required(VERSION 3.20)
project(m3_llm_ncm LANGUAGES CXX)

find_package(JNI REQUIRED)

# Independently implemented residual-VQ nearest-code acceleration. Java remains the oracle.
# No weights, inference server, third-party runtime or automatic library loading is added.
add_library(m3_ncm SHARED src/ncm_kernel.cpp src/ncm_jni.cpp)
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
