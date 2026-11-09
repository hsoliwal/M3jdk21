cmake_minimum_required(VERSION 3.20)
project(m3_precompute LANGUAGES C)

find_package(JNI REQUIRED)

add_library(m3_precompute SHARED
  src/m3_precompute_signals.c
  src/m3_precompute_signals_jni.c)

target_include_directories(m3_precompute
  PRIVATE ${JNI_INCLUDE_DIRS}
  PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(m3_precompute PRIVATE c_std_11)

if(MSVC)
  target_compile_options(m3_precompute PRIVATE /W4 /WX)
else()
  target_compile_options(m3_precompute PRIVATE
    -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
endif()

file(GENERATE
  OUTPUT "${CMAKE_BINARY_DIR}/native-path-$<CONFIG>.txt"
  CONTENT "$<TARGET_FILE:m3_precompute>")

include(CTest)
if(BUILD_TESTING)
  add_executable(m3_precompute_signal_checks
    tests/m3_precompute_signals_checks.c
    src/m3_precompute_signals.c)
  target_include_directories(m3_precompute_signal_checks
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
  target_compile_features(m3_precompute_signal_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_precompute_signal_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_precompute_signal_checks PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_precompute_signal_checks COMMAND m3_precompute_signal_checks)
endif()
