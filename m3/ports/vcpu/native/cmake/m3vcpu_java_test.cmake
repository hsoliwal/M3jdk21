# SPDX-License-Identifier: Apache-2.0
# m3vcpu_java_test.cmake - compiles Java test sources and runs MAIN against the built JNI library.
#   cmake -DJAVAC= -DJAVA= -DOUT=<dir> -DSOURCES="a|b" [-DDONOR_SOURCES="c|d"] -DMAIN=<class>
#         -DPATH_FILE=<native-path file> -P this
# Donor sources (read in place, never copied) are compiled first without -Werror; our sources are
# compiled with -Xlint:all -Werror against them.
cmake_minimum_required(VERSION 3.20)
foreach(var JAVAC JAVA OUT SOURCES MAIN PATH_FILE)
  if(NOT DEFINED ${var})
    message(FATAL_ERROR "m3vcpu_java_test: ${var} not set")
  endif()
endforeach()
file(REMOVE_RECURSE "${OUT}")
file(MAKE_DIRECTORY "${OUT}")
if(DEFINED DONOR_SOURCES AND NOT DONOR_SOURCES STREQUAL "")
  string(REPLACE "|" ";" donor "${DONOR_SOURCES}")
  execute_process(COMMAND "${JAVAC}" --release 21 -nowarn -d "${OUT}" ${donor} RESULT_VARIABLE rc)
  if(NOT rc EQUAL 0)
    message(FATAL_ERROR "javac (donor oracle) failed: ${rc}")
  endif()
endif()
string(REPLACE "|" ";" ours "${SOURCES}")
execute_process(COMMAND "${JAVAC}" --release 21 -Xlint:all -Werror -cp "${OUT}" -d "${OUT}" ${ours} RESULT_VARIABLE rc)
if(NOT rc EQUAL 0)
  message(FATAL_ERROR "javac failed: ${rc}")
endif()
execute_process(COMMAND "${JAVA}" -Xmx256m "-Dm3.vcpu.native.pathFile=${PATH_FILE}" -cp "${OUT}" ${MAIN}
                RESULT_VARIABLE rc)
if(NOT rc EQUAL 0)
  message(FATAL_ERROR "${MAIN} failed: ${rc}")
endif()
