# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# The two optional JNI libraries of jdk.m3.optane (module org.m3jdk.optane), Linux only:
#   m3optane   - madvise hints behind jdk.m3.optane.NativeSupport (m3optane.c)
#   m3pmemsdk  - the persistent-memory region family behind jdk.m3.optane.M3NativePmem (m3pmemsdk.c, relocated by
#                recipe/pmem-region-core-20261106 from hsoliwal/com.synexia m3-pmem-sdk): POSIX mmap/msync/fdatasync
#                regions; the libpmem2 (PMDK, BSD-3-Clause) backend is compiled only with -DM3_OPTANE_PMDK=ON, through
#                pkg-config libpmem2 of the build host. M3JDK ships no PMDK source and no PMDK binary; with the option
#                OFF (the default) PmemBackend.PMDK fails closed with UnsupportedOperationException and AUTO is POSIX.
# Every Java path of the module is exact without either library (FILE backend, Java planning).
# usage: cmake -S m3/optane/src/main/native -B <build> [-DM3_OPTANE_PMDK=ON] && cmake --build <build>
#        then -Djava.library.path=<build> (surefire: -Pm3-optane-native -Dm3.optane.library.path=<build>)
cmake_minimum_required(VERSION 3.20)
project(m3_optane_jni LANGUAGES C)
find_package(Java 21 REQUIRED COMPONENTS Development)
find_package(JNI REQUIRED)
option(M3_OPTANE_PMDK "Compile the libpmem2 (PMDK, BSD-3-Clause) backend of m3pmemsdk; never shipped by M3JDK" OFF)

set(owner "${CMAKE_CURRENT_SOURCE_DIR}/../java/jdk/m3/optane")
set(generated "${CMAKE_CURRENT_BINARY_DIR}/generated")
set(optane_header "${generated}/jdk_m3_optane_NativeSupport.h")
set(pmem_header "${generated}/jdk_m3_optane_M3NativePmem.h")
# javac -h of the two JNI owners; both are self-contained (java.base only), so no other owner is compiled here
add_custom_command(OUTPUT "${optane_header}" "${pmem_header}"
  COMMAND "${CMAKE_COMMAND}" -E make_directory "${generated}"
  COMMAND "${Java_JAVAC_EXECUTABLE}" --release 21 -proc:none -Xlint:all -Werror
    -h "${generated}" -d "${generated}"
    "${owner}/NativeSupport.java" "${owner}/M3NativePmem.java"
  DEPENDS "${owner}/NativeSupport.java" "${owner}/M3NativePmem.java"
  VERBATIM)

add_library(m3optane SHARED m3optane.c "${optane_header}")
add_library(m3pmemsdk SHARED m3pmemsdk.c "${pmem_header}")
foreach(library m3optane m3pmemsdk)
  target_include_directories(${library} PRIVATE ${JNI_INCLUDE_DIRS} "${generated}")
  set_target_properties(${library} PROPERTIES C_STANDARD 17 C_STANDARD_REQUIRED YES C_EXTENSIONS NO)
  if(MSVC)
    target_compile_options(${library} PRIVATE /W4 /WX)
  else()
    target_compile_options(${library} PRIVATE -Wall -Wextra -Wpedantic -Werror)
  endif()
endforeach()
# m3optane.c declares _DEFAULT_SOURCE and m3pmemsdk.c _GNU_SOURCE themselves (madvise, O_CLOEXEC, O_NOFOLLOW, fdatasync)

if(M3_OPTANE_PMDK)
  find_package(PkgConfig REQUIRED)
  pkg_check_modules(PMEM2 REQUIRED IMPORTED_TARGET libpmem2)
  target_compile_definitions(m3pmemsdk PRIVATE M3_HAVE_LIBPMEM2)
  target_link_libraries(m3pmemsdk PRIVATE PkgConfig::PMEM2)
  message(STATUS "m3pmemsdk: libpmem2 backend ON (PMDK, BSD-3-Clause, from the build host; not shipped)")
else()
  message(STATUS "m3pmemsdk: POSIX backend only (M3_OPTANE_PMDK=OFF)")
endif()
