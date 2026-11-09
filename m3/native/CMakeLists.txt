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

# Compile the actual existing SDK JNI owning sources on every supported CMake
# target, including Android cross-compiles. The generated-header residency owner
# continues to use its own javac -h / matched JNI integration workflow.
add_library(m3_jni_existing_owners OBJECT
  ${CMAKE_CURRENT_SOURCE_DIR}/../../src/jdk.m3.collections/share/native/libm3arrays/m3_arrays_jni.c
  ${CMAKE_CURRENT_SOURCE_DIR}/../../src/jdk.m3.collections/share/native/libm3collections/collections/m3_index_path_masks.c)
target_link_libraries(m3_jni_existing_owners PRIVATE m3_jni_core)
target_include_directories(m3_jni_existing_owners PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}/../../src/jdk.m3.collections/share/native/libm3arrays/include)
target_compile_features(m3_jni_existing_owners PRIVATE c_std_11)
if(MSVC)
  target_compile_options(m3_jni_existing_owners PRIVATE /W4 /WX)
else()
  target_compile_options(m3_jni_existing_owners PRIVATE -Wall -Wextra -Werror -Wpedantic)
endif()

# Optional jdk.m3.jni scoped-lease JNI library: Java owners under src/main/java/jdk/m3/jni, C17 source
# src/main/native/m3_scoped_leases.c, project src/main/native/scoped-leases/CMakeLists.txt (relocated by
# recipe/scoped-leases-20261106). It needs a JDK 21 javac for its generated headers, so it is off by default and
# never part of the Android/iOS cross-compiles; every Java path of jdk.m3.jni is exact without the library.
option(M3_SCOPED_LEASES_JNI "Build the jdk.m3.jni scoped-lease JNI library (needs a JDK 21 javac)" OFF)
if(M3_SCOPED_LEASES_JNI)
  add_subdirectory(src/main/native/scoped-leases)
endif()

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
