# SPDX-License-Identifier: Apache-2.0
cmake_minimum_required(VERSION 3.18)
project(m3_mindex_jni VERSION 1.0.0 LANGUAGES C CXX)

# Desktop defaults preserve the existing shared JNI target and its tests.
set(_mindex_desktop_default ON)
if(CMAKE_SYSTEM_NAME MATCHES "^(iOS|tvOS|watchOS|visionOS)$")
  set(_mindex_desktop_default OFF)
endif()
option(M3_MINDEX_JNI "Build the existing Java native bindings" ${_mindex_desktop_default})
option(M3_MINDEX_SHARED "Build a shared rather than static C ABI library" ${_mindex_desktop_default})
set(_mindex_test_default ${_mindex_desktop_default})
if(ANDROID)
  set(_mindex_test_default OFF)
endif()
option(BUILD_TESTING "Build native contract tests" ${_mindex_test_default})
include(CTest)

if(M3_MINDEX_JNI)
  if(CMAKE_SYSTEM_NAME MATCHES "^(iOS|tvOS|watchOS|visionOS)$")
    message(FATAL_ERROR "Apple device SDKs use M3_MINDEX_JNI=OFF; no target JVM adapter is supplied")
  elseif(ANDROID)
    if(NOT CMAKE_ANDROID_NDK)
      message(FATAL_ERROR "Android JNI requires an installed NDK toolchain")
    endif()
    # NDK jni.h is in the target sysroot. Never discover host JVM/AWT libraries.
  else()
    find_package(JNI REQUIRED)
  endif()
endif()

set(_mindex_library_kind STATIC)
if(M3_MINDEX_SHARED)
  set(_mindex_library_kind SHARED)
endif()
add_library(
  m3_mindex
  ${_mindex_library_kind}
  src/mindex_native.c
  src/mindex_tensor.c
  src/mindex_decision.c
  src/mindex_ast_candidate.c
  src/mindex_ast_partial.c
  src/mindex_systemone.c)
if(M3_MINDEX_JNI)
  target_sources(
    m3_mindex PRIVATE
    src/mindex_jni.c
    src/mindex_transition_power_jni.c
    src/mindex_segment_search_jni.c
    src/mindex_string_jni.c
    src/mindex_prepared_kmp_jni.c
    src/mindex_prepared_horspool_jni.c
    src/mindex_tensor_jni.c
    src/mindex_decision_jni.cpp
    src/mindex_ast_candidate_jni.cpp
    src/mindex_ast_partial_jni.cpp
    src/mindex_systemone_jni.c
    src/mindex_pmem2_jni.c)
  if(NOT ANDROID)
    target_include_directories(m3_mindex PRIVATE ${JNI_INCLUDE_DIRS})
  endif()
endif()
target_include_directories(
  m3_mindex PUBLIC
  "$<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>"
  "$<INSTALL_INTERFACE:include>")
target_compile_features(m3_mindex PRIVATE c_std_11 cxx_std_17)
if(M3_MINDEX_SHARED)
  target_compile_definitions(m3_mindex PRIVATE M3_MINDEX_BUILD_SHARED=1)
else()
  target_compile_definitions(m3_mindex PUBLIC M3_MINDEX_STATIC=1)
endif()
set_target_properties(m3_mindex PROPERTIES OUTPUT_NAME "m3_mindex" POSITION_INDEPENDENT_CODE ON)
if(ANDROID AND M3_MINDEX_SHARED)
  target_link_options(m3_mindex PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")
endif()


if(NOT MSVC)
  target_link_libraries(m3_mindex PRIVATE m)
endif()

option(M3_MINDEX_PMEM2 "Build optional Linux PMDK libpmem2 region JNI" OFF)
if(M3_MINDEX_PMEM2)
  if(NOT M3_MINDEX_JNI)
    message(FATAL_ERROR "M3_MINDEX_PMEM2 requires M3_MINDEX_JNI=ON")
  endif()
  if(NOT CMAKE_SYSTEM_NAME STREQUAL "Linux")
    message(FATAL_ERROR "M3_MINDEX_PMEM2 currently supports Linux only")
  endif()
  find_package(PkgConfig REQUIRED)
  pkg_check_modules(PMEM2 REQUIRED IMPORTED_TARGET libpmem2)
  target_link_libraries(m3_mindex PRIVATE PkgConfig::PMEM2)
  target_compile_definitions(m3_mindex PRIVATE M3_MINDEX_PMEM2=1)
endif()

option(
  M3_MINDEX_AST_CUDA
  "Build optional CUDA/CCCL AST candidate compaction backend"
  OFF)
if(M3_MINDEX_AST_CUDA)
  enable_language(CUDA)
  find_package(CUDAToolkit REQUIRED)
  target_sources(
    m3_mindex
    PRIVATE
    src/mindex_ast_candidate_cuda.cu
    src/mindex_ast_partial_cuda.cu)
  target_compile_definitions(
    m3_mindex
    PRIVATE
    M3_MINDEX_AST_CUDA=1)
  target_link_libraries(
    m3_mindex
    PRIVATE
    CUDA::cudart)
  set_target_properties(
    m3_mindex
    PROPERTIES
    CUDA_STANDARD 17
    CUDA_STANDARD_REQUIRED YES)

  set(M3_CCCL_NOTICE_DIR
      "${CMAKE_CURRENT_SOURCE_DIR}/third_party/cccl-0fd92952")
  add_custom_command(
    TARGET m3_mindex POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory
      "$<TARGET_FILE_DIR:m3_mindex>/licenses/nvidia-cccl"
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
      "${M3_CCCL_NOTICE_DIR}/LICENSE"
      "${M3_CCCL_NOTICE_DIR}/ATTRIBUTION.txt"
      "$<TARGET_FILE_DIR:m3_mindex>/licenses/nvidia-cccl")
endif()

option(
  M3_MINDEX_SYSTEMONE_CPP
  "Build independent C++17 System One probability differential kernel"
  ON)
if(M3_MINDEX_SYSTEMONE_CPP)
  enable_language(CXX)
  target_sources(
    m3_mindex
    PRIVATE
    src/mindex_systemone_cpp.cpp)
  target_compile_features(m3_mindex PRIVATE cxx_std_17)
endif()

option(M3_MINDEX_LITERT "Build optional Google LiteRT NPU precompute JNI adapter" OFF)
set(M3_LITERT_INCLUDE_DIR "" CACHE PATH "LiteRT include/source root containing litert/c")
set(M3_LITERT_LIBRARY "" CACHE FILEPATH "LiteRT runtime library (for example libLiteRt.so)")
if(M3_MINDEX_LITERT)
  if(NOT M3_MINDEX_JNI)
    message(FATAL_ERROR "M3_MINDEX_LITERT requires M3_MINDEX_JNI=ON")
  endif()
  if(NOT M3_LITERT_INCLUDE_DIR)
    message(FATAL_ERROR "M3_LITERT_INCLUDE_DIR is required when M3_MINDEX_LITERT=ON")
  endif()
  if(NOT EXISTS "${M3_LITERT_INCLUDE_DIR}/litert/c/litert_compiled_model.h")
    message(FATAL_ERROR "M3_LITERT_INCLUDE_DIR does not contain LiteRT public C headers")
  endif()
  if(NOT M3_LITERT_LIBRARY OR NOT EXISTS "${M3_LITERT_LIBRARY}")
    message(FATAL_ERROR "M3_LITERT_LIBRARY must name an existing LiteRT runtime library")
  endif()

  enable_language(CXX)
  target_sources(
    m3_mindex
    PRIVATE
    src/mindex_litert_precompute_jni.cpp)
  target_include_directories(
    m3_mindex
    PRIVATE
    "${M3_LITERT_INCLUDE_DIR}")
  target_link_libraries(
    m3_mindex
    PRIVATE
    "${M3_LITERT_LIBRARY}")
  target_compile_features(m3_mindex PRIVATE cxx_std_17)
  target_compile_definitions(m3_mindex PRIVATE M3_MINDEX_LITERT=1)
endif()


option(M3_MINDEX_CORAL "Build optional Google Coral Edge TPU precompute JNI adapter" OFF)
set(M3_CORAL_INCLUDE_DIR "" CACHE PATH "libedgetpu source/include root containing tflite/public")
set(M3_TFLITE_INCLUDE_DIR "" CACHE PATH "TensorFlow source root containing tensorflow/lite/core/c")
set(M3_EDGETPU_LIBRARY "" CACHE FILEPATH "Google libedgetpu runtime library")
set(M3_TFLITE_LIBRARY "" CACHE FILEPATH "TensorFlow Lite C runtime library")
if(M3_MINDEX_CORAL)
  if(NOT M3_MINDEX_JNI)
    message(FATAL_ERROR "M3_MINDEX_CORAL requires M3_MINDEX_JNI=ON")
  endif()
  if(NOT EXISTS "${M3_CORAL_INCLUDE_DIR}/tflite/public/edgetpu_c.h")
    message(FATAL_ERROR "M3_CORAL_INCLUDE_DIR does not contain libedgetpu public headers")
  endif()
  if(NOT EXISTS "${M3_TFLITE_INCLUDE_DIR}/tensorflow/lite/core/c/c_api.h")
    message(FATAL_ERROR "M3_TFLITE_INCLUDE_DIR does not contain TensorFlow Lite C headers")
  endif()
  if(NOT M3_EDGETPU_LIBRARY OR NOT EXISTS "${M3_EDGETPU_LIBRARY}")
    message(FATAL_ERROR "M3_EDGETPU_LIBRARY must name an existing libedgetpu runtime")
  endif()
  if(NOT M3_TFLITE_LIBRARY OR NOT EXISTS "${M3_TFLITE_LIBRARY}")
    message(FATAL_ERROR "M3_TFLITE_LIBRARY must name an existing TensorFlow Lite C runtime")
  endif()

  enable_language(CXX)
  target_sources(
    m3_mindex
    PRIVATE
    src/mindex_coral_precompute_jni.cpp)
  target_include_directories(
    m3_mindex
    PRIVATE
    "${M3_CORAL_INCLUDE_DIR}"
    "${M3_TFLITE_INCLUDE_DIR}")
  target_link_libraries(
    m3_mindex
    PRIVATE
    "${M3_EDGETPU_LIBRARY}"
    "${M3_TFLITE_LIBRARY}")
  target_compile_features(m3_mindex PRIVATE cxx_std_17)
  target_compile_definitions(m3_mindex PRIVATE M3_MINDEX_CORAL=1)
endif()

if(MSVC)
  target_compile_options(
    m3_mindex
    PRIVATE
    "$<$<COMPILE_LANGUAGE:C,CXX>:/W4>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:/WX>")
else()
  target_compile_options(
    m3_mindex
    PRIVATE
    "$<$<COMPILE_LANGUAGE:C,CXX>:-Wall>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-Wextra>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-Wpedantic>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-Werror>")
endif()

if(BUILD_TESTING)
enable_testing()
add_executable(mindex_native_test test/mindex_native_test.c)
target_link_libraries(mindex_native_test PRIVATE m3_mindex)
target_compile_features(mindex_native_test PRIVATE c_std_11)
if(MSVC)
  target_compile_options(mindex_native_test PRIVATE /W4 /WX)
else()
  target_compile_options(mindex_native_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_native_c_abi COMMAND mindex_native_test)

# Stored-hash scalar-copy and C++ C-ABI linkage contracts.
add_executable(mindex_string_hash_test test/mindex_string_hash_test.c)
target_link_libraries(mindex_string_hash_test PRIVATE m3_mindex)
target_compile_features(mindex_string_hash_test PRIVATE c_std_11)
add_executable(mindex_string_hash_cpp_test test/mindex_string_hash_cpp_test.cpp)
target_link_libraries(mindex_string_hash_cpp_test PRIVATE m3_mindex)
target_compile_features(mindex_string_hash_cpp_test PRIVATE cxx_std_17)
if(MSVC)
  target_compile_options(mindex_string_hash_test PRIVATE /W4 /WX)
  target_compile_options(mindex_string_hash_cpp_test PRIVATE /W4 /WX)
else()
  target_compile_options(mindex_string_hash_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
  target_compile_options(mindex_string_hash_cpp_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_string_hash_c COMMAND mindex_string_hash_test)
add_test(NAME mindex_string_hash_cpp COMMAND mindex_string_hash_cpp_test)

add_executable(mindex_string_storage_test test/mindex_string_storage_test.c)
target_link_libraries(mindex_string_storage_test PRIVATE m3_mindex)
target_compile_features(mindex_string_storage_test PRIVATE c_std_11)
if(MSVC)
  target_compile_options(mindex_string_storage_test PRIVATE /W4 /WX)
else()
  target_compile_options(
    mindex_string_storage_test
    PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_string_storage_c COMMAND mindex_string_storage_test)

add_executable(mindex_ast_candidate_test test/mindex_ast_candidate_test.c)
target_link_libraries(mindex_ast_candidate_test PRIVATE m3_mindex)
target_compile_features(mindex_ast_candidate_test PRIVATE c_std_11)
if(MSVC)
  target_compile_options(mindex_ast_candidate_test PRIVATE /W4 /WX)
else()
  target_compile_options(
    mindex_ast_candidate_test
    PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_ast_candidate_c_abi COMMAND mindex_ast_candidate_test)

add_executable(mindex_ast_partial_test test/mindex_ast_partial_test.c)
target_link_libraries(mindex_ast_partial_test PRIVATE m3_mindex)
target_compile_features(mindex_ast_partial_test PRIVATE c_std_11)
if(MSVC)
  target_compile_options(mindex_ast_partial_test PRIVATE /W4 /WX)
else()
  target_compile_options(mindex_ast_partial_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_ast_partial_c_abi COMMAND mindex_ast_partial_test)

add_executable(mindex_ast_schedule_admission_test test/mindex_ast_schedule_admission_test.c)
target_link_libraries(mindex_ast_schedule_admission_test PRIVATE m3_mindex)
target_compile_features(mindex_ast_schedule_admission_test PRIVATE c_std_11)
if(MSVC)
  target_compile_options(mindex_ast_schedule_admission_test PRIVATE /W4 /WX)
else()
  target_compile_options(mindex_ast_schedule_admission_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_ast_schedule_admission_c_abi COMMAND mindex_ast_schedule_admission_test)

add_executable(mindex_systemone_test test/mindex_systemone_test.c)
target_link_libraries(mindex_systemone_test PRIVATE m3_mindex)
target_compile_features(mindex_systemone_test PRIVATE c_std_11)
if(NOT MSVC)
  target_link_libraries(mindex_systemone_test PRIVATE m)
endif()
if(MSVC)
  target_compile_options(mindex_systemone_test PRIVATE /W4 /WX)
else()
  target_compile_options(mindex_systemone_test PRIVATE -Wall -Wextra -Wpedantic -Werror)
endif()
add_test(NAME mindex_systemone_c COMMAND mindex_systemone_test)

if(M3_MINDEX_SYSTEMONE_CPP)
  add_executable(
    mindex_systemone_cpp_test
    test/mindex_systemone_cpp_test.cpp)
  target_link_libraries(mindex_systemone_cpp_test PRIVATE m3_mindex)
  target_compile_features(mindex_systemone_cpp_test PRIVATE cxx_std_17)
  if(NOT MSVC)
    target_link_libraries(mindex_systemone_cpp_test PRIVATE m)
  endif()
  if(MSVC)
    target_compile_options(mindex_systemone_cpp_test PRIVATE /W4 /WX)
  else()
    target_compile_options(
      mindex_systemone_cpp_test
      PRIVATE
      -Wall
      -Wextra
      -Wpedantic
      -Werror)
  endif()
  add_test(NAME mindex_systemone_cpp COMMAND mindex_systemone_cpp_test)
endif()

# Contract executables use assert; keep assertions active for Release SDK tests.
foreach(_mindex_test IN ITEMS
    mindex_native_test mindex_string_hash_test mindex_string_hash_cpp_test
    mindex_string_storage_test mindex_ast_candidate_test mindex_ast_partial_test
    mindex_ast_schedule_admission_test mindex_systemone_test mindex_systemone_cpp_test)
  if(TARGET ${_mindex_test})
    if(MSVC)
      target_compile_options(${_mindex_test} PRIVATE /UNDEBUG)
    else()
      target_compile_options(${_mindex_test} PRIVATE -UNDEBUG)
    endif()
  endif()
endforeach()
endif()

install(TARGETS m3_mindex EXPORT M3MIndexSDKTargets
  ARCHIVE DESTINATION lib LIBRARY DESTINATION lib RUNTIME DESTINATION bin)
install(DIRECTORY include/ DESTINATION include FILES_MATCHING PATTERN "*.h")
