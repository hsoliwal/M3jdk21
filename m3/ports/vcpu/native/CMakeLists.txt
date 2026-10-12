# SPDX-License-Identifier: Apache-2.0
# M3 vCPU native layer: HAL core + built-in CPU backend (m3vcpu), OpenCL backend (m3vcpu_opencl,
# OpenCL.dll loaded at run time), JNI glue (m3vcpu_jni), ctest gates.
# Template: M3jdk21 m3/ports/precompute/src/main/native (C11 core + _jni.c glue, -Werror, path file).
cmake_minimum_required(VERSION 3.20)
project(m3vcpu VERSION 0.1.0 LANGUAGES C)

option(M3VCPU_BUILD_JNI "Build the JNI glue (needs JNI headers)" ON)
option(M3VCPU_BUILD_OPENCL "Build the OpenCL backend (no SDK needed)" ON)
option(M3VCPU_ENABLE_AVX512 "Build the AVX-512 (x86-64-v4) CPU kernel variant" ON)
set(M3VCPU_DONOR_ORACLE_DIR "" CACHE PATH
    "Optional: cpu-gpu-rv32im src/main/java/com/synexia/m3/cpugpu (Java oracle for the JNI parity test)")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

set(M3VCPU_X86_64 OFF)
if(CMAKE_SYSTEM_PROCESSOR MATCHES "^(AMD64|amd64|x86_64|X86_64|x64)$")
  set(M3VCPU_X86_64 ON)
endif()

function(m3vcpu_strict target)
  if(MSVC)
    target_compile_options(${target} PRIVATE /W4 /WX /utf-8)
    target_compile_definitions(${target} PRIVATE _CRT_SECURE_NO_WARNINGS)
  else()
    target_compile_options(${target} PRIVATE -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic)
  endif()
endfunction()

set(M3VCPU_INC ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(M3VCPU_KERNELS ${CMAKE_CURRENT_SOURCE_DIR}/kernels)
set(M3VCPU_COMMON ${CMAKE_CURRENT_SOURCE_DIR}/src/common)
set(M3VCPU_GEN ${CMAKE_BINARY_DIR}/gen)

# ---- kernel bundle (content-addressed blobs compiled into the core) ------------------------
add_custom_command(
  OUTPUT ${M3VCPU_GEN}/m3vcpu_bundle_data.c ${M3VCPU_GEN}/m3k_rv32im_slice.opencl.cl ${M3VCPU_GEN}/kernel-bundle.tsv
  COMMAND ${CMAKE_COMMAND} -DHEADER=${M3VCPU_KERNELS}/m3k_portability.h -DKERNEL=${M3VCPU_KERNELS}/rv32im_slice.cl
          -DENTRY=m3k_rv32im_slice -DOUT_DIR=${M3VCPU_GEN} -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/m3vcpu_embed_kernel.cmake
  DEPENDS ${M3VCPU_KERNELS}/m3k_portability.h ${M3VCPU_KERNELS}/rv32im_slice.cl
          ${CMAKE_CURRENT_SOURCE_DIR}/cmake/m3vcpu_embed_kernel.cmake
  COMMENT "Embedding kernel bundle (m3k_rv32im_slice)"
  VERBATIM)

# ---- CPU kernels: same .cl compiled once per ISA level -----------------------------------
set(M3VCPU_CPU_KERNEL_SOURCES src/backends/cpu/m3vcpu_cpu_kernels_scalar.c)
set(M3VCPU_CPU_DEFS "")
if(M3VCPU_X86_64)
  list(APPEND M3VCPU_CPU_KERNEL_SOURCES src/backends/cpu/m3vcpu_cpu_kernels_avx2.c)
  list(APPEND M3VCPU_CPU_DEFS M3VCPU_HAVE_AVX2=1)
  if(MSVC)
    set_source_files_properties(src/backends/cpu/m3vcpu_cpu_kernels_avx2.c PROPERTIES COMPILE_OPTIONS "/arch:AVX2")
  else()
    set_source_files_properties(src/backends/cpu/m3vcpu_cpu_kernels_avx2.c PROPERTIES COMPILE_OPTIONS "-march=x86-64-v3")
  endif()
  if(M3VCPU_ENABLE_AVX512)
    list(APPEND M3VCPU_CPU_KERNEL_SOURCES src/backends/cpu/m3vcpu_cpu_kernels_avx512.c)
    list(APPEND M3VCPU_CPU_DEFS M3VCPU_HAVE_AVX512=1)
    if(MSVC)
      set_source_files_properties(src/backends/cpu/m3vcpu_cpu_kernels_avx512.c PROPERTIES COMPILE_OPTIONS "/arch:AVX512")
    else()
      set_source_files_properties(src/backends/cpu/m3vcpu_cpu_kernels_avx512.c PROPERTIES COMPILE_OPTIONS "-march=x86-64-v4")
    endif()
  endif()
endif()

# ---- m3vcpu: HAL core + built-in CPU backend ---------------------------------------------
add_library(m3vcpu SHARED
  src/common/m3vcpu_sys.c
  src/core/m3vcpu_status.c
  src/core/m3vcpu_sha256.c
  src/core/m3vcpu_registry.c
  src/core/m3vcpu_select.c
  src/core/m3vcpu_session.c
  ${M3VCPU_GEN}/m3vcpu_bundle_data.c
  src/backends/cpu/m3vcpu_cpu_isa.c
  src/backends/cpu/m3vcpu_cpu_pool.c
  src/backends/cpu/m3vcpu_cpu_backend.c
  ${M3VCPU_CPU_KERNEL_SOURCES})
target_include_directories(m3vcpu
  PUBLIC ${M3VCPU_INC}
  PRIVATE ${M3VCPU_COMMON} ${CMAKE_CURRENT_SOURCE_DIR}/src/core ${CMAKE_CURRENT_SOURCE_DIR}/src/backends/cpu
          ${M3VCPU_KERNELS})
target_compile_definitions(m3vcpu PRIVATE M3VCPU_BUILDING_CORE ${M3VCPU_CPU_DEFS})
if(NOT WIN32)
  find_package(Threads REQUIRED)
  target_link_libraries(m3vcpu PRIVATE Threads::Threads ${CMAKE_DL_LIBS})
endif()
m3vcpu_strict(m3vcpu)

# ---- m3vcpu_opencl: OpenCL backend (vendor runtime loaded at run time) -------------------
if(M3VCPU_BUILD_OPENCL AND EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/src/backends/opencl/m3vcpu_opencl_backend.c)
  add_library(m3vcpu_opencl MODULE
    src/common/m3vcpu_sys.c
    src/backends/opencl/m3vcpu_opencl_backend.c)
  target_include_directories(m3vcpu_opencl PRIVATE ${M3VCPU_INC} ${M3VCPU_COMMON})
  set_target_properties(m3vcpu_opencl PROPERTIES PREFIX "${CMAKE_SHARED_LIBRARY_PREFIX}")
  if(NOT WIN32)
    target_link_libraries(m3vcpu_opencl PRIVATE Threads::Threads ${CMAKE_DL_LIBS})
  endif()
  m3vcpu_strict(m3vcpu_opencl)
  add_dependencies(m3vcpu m3vcpu_opencl)
endif()

# ---- m3vcpu_jni: thin JNI glue ------------------------------------------------------------
set(M3VCPU_JNI_TARGET "")
if(M3VCPU_BUILD_JNI)
  find_package(JNI)
  set(M3VCPU_JNI_INCLUDES ${JNI_INCLUDE_DIRS})
  if(NOT M3VCPU_JNI_INCLUDES AND DEFINED ENV{JAVA_HOME})
    set(M3VCPU_JNI_INCLUDES "$ENV{JAVA_HOME}/include" "$ENV{JAVA_HOME}/include/win32" "$ENV{JAVA_HOME}/include/linux")
  endif()
  add_library(m3vcpu_jni SHARED src/common/m3vcpu_sys.c src/jni/m3vcpu_jni.c)
  target_include_directories(m3vcpu_jni SYSTEM PRIVATE ${M3VCPU_JNI_INCLUDES})
  target_include_directories(m3vcpu_jni PRIVATE ${M3VCPU_COMMON})
  target_link_libraries(m3vcpu_jni PRIVATE m3vcpu)
  if(NOT WIN32)
    target_link_libraries(m3vcpu_jni PRIVATE Threads::Threads)
    set_target_properties(m3vcpu_jni PROPERTIES BUILD_RPATH "$ORIGIN" INSTALL_RPATH "$ORIGIN")
  endif()
  m3vcpu_strict(m3vcpu_jni)
  set(M3VCPU_JNI_TARGET m3vcpu_jni)
  file(GENERATE OUTPUT "${CMAKE_BINARY_DIR}/native-path-$<CONFIG>.txt" CONTENT "$<TARGET_FILE:m3vcpu_jni>")
endif()

# ---- tests ---------------------------------------------------------------------------------
include(CTest)
if(BUILD_TESTING)
  add_library(m3vcpu_testlib STATIC tests/rv32_ref.c tests/m3vcpu_check.c src/common/m3vcpu_sys.c)
  target_include_directories(m3vcpu_testlib PUBLIC ${M3VCPU_INC} ${M3VCPU_COMMON} ${CMAKE_CURRENT_SOURCE_DIR}/tests)
  m3vcpu_strict(m3vcpu_testlib)

  foreach(t core rv32im)
    add_executable(m3vcpu_test_${t} tests/test_${t}.c)
    target_link_libraries(m3vcpu_test_${t} PRIVATE m3vcpu m3vcpu_testlib)
    m3vcpu_strict(m3vcpu_test_${t})
  endforeach()

  add_test(NAME m3vcpu_core COMMAND m3vcpu_test_core)
  add_test(NAME m3vcpu_rv32im_cpu COMMAND m3vcpu_test_rv32im cpu)
  add_test(NAME m3vcpu_rv32im_opencl COMMAND m3vcpu_test_rv32im opencl)
  set_tests_properties(m3vcpu_rv32im_opencl PROPERTIES SKIP_RETURN_CODE 77)
  set_tests_properties(m3vcpu_core m3vcpu_rv32im_cpu m3vcpu_rv32im_opencl PROPERTIES TIMEOUT 600)

  # Hard rule 1: no vendor runtime in any import table.
  set(M3VCPU_GATE_FILES "$<TARGET_FILE:m3vcpu>")
  if(TARGET m3vcpu_opencl)
    string(APPEND M3VCPU_GATE_FILES "|$<TARGET_FILE:m3vcpu_opencl>")
  endif()
  if(TARGET m3vcpu_jni)
    string(APPEND M3VCPU_GATE_FILES "|$<TARGET_FILE:m3vcpu_jni>")
  endif()
  if(WIN32)
    find_program(M3VCPU_READOBJ NAMES llvm-readobj)
  else()
    find_program(M3VCPU_READOBJ NAMES readelf llvm-readelf)
  endif()
  if(M3VCPU_READOBJ)
    add_test(NAME m3vcpu_import_table
             COMMAND ${CMAKE_COMMAND} -DTOOL=${M3VCPU_READOBJ} "-DFILES=${M3VCPU_GATE_FILES}"
                     -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/m3vcpu_check_imports.cmake)
  else()
    message(WARNING "llvm-readobj/readelf not found: import-table gate not registered")
  endif()

  # JNI end to end: compile NativeVcpu.java + tests, run against the built library.
  if(TARGET m3vcpu_jni)
    find_package(Java 21 COMPONENTS Development Runtime)
    if(Java_JAVAC_EXECUTABLE AND Java_JAVA_EXECUTABLE)
      set(M3VCPU_JAVA_SRC ${CMAKE_CURRENT_SOURCE_DIR}/../src/main/java/com/m3/vcpu/internal/NativeVcpu.java)
      add_test(NAME m3vcpu_jni_smoke
               COMMAND ${CMAKE_COMMAND} -DJAVAC=${Java_JAVAC_EXECUTABLE} -DJAVA=${Java_JAVA_EXECUTABLE}
                       -DOUT=${CMAKE_BINARY_DIR}/java-smoke "-DSOURCES=${M3VCPU_JAVA_SRC}|${CMAKE_CURRENT_SOURCE_DIR}/tests/java/NativeVcpuSmoke.java"
                       -DMAIN=NativeVcpuSmoke "-DPATH_FILE=${CMAKE_BINARY_DIR}/native-path-$<CONFIG>.txt"
                       -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/m3vcpu_java_test.cmake)
      set_tests_properties(m3vcpu_jni_smoke PROPERTIES TIMEOUT 300)
      if(M3VCPU_DONOR_ORACLE_DIR AND EXISTS "${M3VCPU_DONOR_ORACLE_DIR}/CpuRv32iEngine.java")
        set(d ${M3VCPU_DONOR_ORACLE_DIR})
        add_test(NAME m3vcpu_jni_oracle_parity
                 COMMAND ${CMAKE_COMMAND} -DJAVAC=${Java_JAVAC_EXECUTABLE} -DJAVA=${Java_JAVA_EXECUTABLE}
                         -DOUT=${CMAKE_BINARY_DIR}/java-oracle
                         "-DDONOR_SOURCES=${d}/Rv32i.java|${d}/Rv32iBatch.java|${d}/CpuRv32iEngine.java|${d}/Rv32iAssembler.java"
                         "-DSOURCES=${M3VCPU_JAVA_SRC}|${CMAKE_CURRENT_SOURCE_DIR}/tests/java/NativeVcpuOracleParity.java"
                         -DMAIN=NativeVcpuOracleParity "-DPATH_FILE=${CMAKE_BINARY_DIR}/native-path-$<CONFIG>.txt"
                         -P ${CMAKE_CURRENT_SOURCE_DIR}/cmake/m3vcpu_java_test.cmake)
        set_tests_properties(m3vcpu_jni_oracle_parity PROPERTIES TIMEOUT 600)
      endif()
    else()
      message(WARNING "JDK 21 javac/java not found: JNI ctests not registered")
    endif()
  endif()
endif()
