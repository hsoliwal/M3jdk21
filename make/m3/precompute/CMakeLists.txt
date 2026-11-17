cmake_minimum_required(VERSION 3.20)
project(m3_precompute LANGUAGES C)

# The sources are part of java.base and the C checks live with the Java tests (OpenJDK layout).
set(M3_PRECOMPUTE_NATIVE ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/java.base/share/native/libm3precompute)
set(M3_PRECOMPUTE_NATIVE_TESTS ${CMAKE_CURRENT_SOURCE_DIR}/../../../test/jdk/jdk/m3/precompute/native)

find_package(JNI REQUIRED)

add_library(m3_precompute SHARED
  ${M3_PRECOMPUTE_NATIVE}/m3_precompute_signals.c
  ${M3_PRECOMPUTE_NATIVE}/m3_precompute_signals_jni.c
  ${M3_PRECOMPUTE_NATIVE}/m3_derivative_regex.c
  ${M3_PRECOMPUTE_NATIVE}/m3_derivative_regex_jni.c)

target_include_directories(m3_precompute
  PRIVATE ${JNI_INCLUDE_DIRS}
  PUBLIC ${M3_PRECOMPUTE_NATIVE})
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
    ${M3_PRECOMPUTE_NATIVE_TESTS}/m3_precompute_signals_checks.c
    ${M3_PRECOMPUTE_NATIVE}/m3_precompute_signals.c)
  target_include_directories(m3_precompute_signal_checks
    PRIVATE ${M3_PRECOMPUTE_NATIVE})
  target_compile_features(m3_precompute_signal_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_precompute_signal_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_precompute_signal_checks PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_precompute_signal_checks COMMAND m3_precompute_signal_checks)
  add_executable(m3_derivative_regex_checks
    ${M3_PRECOMPUTE_NATIVE_TESTS}/m3_derivative_regex_checks.c
    ${M3_PRECOMPUTE_NATIVE}/m3_derivative_regex.c)
  target_include_directories(m3_derivative_regex_checks PRIVATE ${M3_PRECOMPUTE_NATIVE})
  target_compile_features(m3_derivative_regex_checks PRIVATE c_std_11)
  if(MSVC)
    target_compile_options(m3_derivative_regex_checks PRIVATE /W4 /WX)
  else()
    target_compile_options(m3_derivative_regex_checks PRIVATE
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
  add_test(NAME m3_derivative_regex_checks COMMAND m3_derivative_regex_checks)

endif()

# Synexia precompute planner kernels join the module's JNI library (recipe m3/ports/precompute/recipe/planner-native-20261107):
# Java_jdk_m3_precompute_M3NativePrecomputeOps_{nativeAbiVersion,lookupPairsNative,tupleLookupAbiVersionNative,
# lookupTuplesNative}: coarse batch lookups over one frozen tuple index, every borrowed array released before returning,
# nothing published on a refused shape; M3NativePrecomputeOps keeps the Java path as the semantic oracle.
target_sources(m3_precompute PRIVATE
  ${M3_PRECOMPUTE_NATIVE}/m3_precompute_pair_jni.c
  ${M3_PRECOMPUTE_NATIVE}/m3_precompute_tuple_jni.c)

# NCM codebook batches (NativePrecomputeOps.ncmCodebookAbiVersionNative and
# nearestNcmCodesNative) live in the single jdk.m3.llm NCM native owner,
# m3/llm/src/main/native (library m3_ncm); this library carries no NCM code.
target_compile_features(m3_precompute PRIVATE c_std_17)
