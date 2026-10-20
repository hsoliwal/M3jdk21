cmake_minimum_required(VERSION 3.20)
project(m3_jni_core LANGUAGES C)

# Android NDK provides jni.h through its sysroot. iOS/AOT ports must provide
# their selected Java runtime's compatible JNI headers; do not bundle a JVM.
if(ANDROID)
  set(M3_JNI_HEADERS "")
elseif(M3_JNI_INCLUDE_DIRS)
  set(M3_JNI_HEADERS ${M3_JNI_INCLUDE_DIRS})
else()
  find_package(JNI REQUIRED)
  set(M3_JNI_HEADERS ${JNI_INCLUDE_DIRS})
endif()

add_library(m3_jni_core INTERFACE)
target_include_directories(m3_jni_core INTERFACE
  ${CMAKE_CURRENT_SOURCE_DIR}/include ${M3_JNI_HEADERS})
target_compile_features(m3_jni_core INTERFACE c_std_11)

include(CTest)
if(BUILD_TESTING)
  add_executable(m3_jni_core_contract tests/m3_jni_core_contract.c)
  target_link_libraries(m3_jni_core_contract PRIVATE m3_jni_core)
  if(MSVC)
    target_compile_options(m3_jni_core_contract PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_jni_core_contract PRIVATE -Wall -Wextra -Werror -Wpedantic)
  endif()
  if(NOT CMAKE_CROSSCOMPILING)
    add_test(NAME m3_jni_core_contract COMMAND m3_jni_core_contract)
  endif()
endif()
