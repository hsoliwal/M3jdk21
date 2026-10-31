cmake_minimum_required(VERSION 3.20)
project(m3_arrays LANGUAGES C)

find_package(JNI REQUIRED)

add_library(m3_arrays SHARED
  m3_arrays.c
  m3_arrays_jni.c)

target_include_directories(
  m3_arrays
  PRIVATE ${JNI_INCLUDE_DIRS}
  PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(m3_arrays PRIVATE c_std_11)
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
