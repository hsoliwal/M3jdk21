# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Product gate regressions. These fixtures model tool text, not target platform execution.
cmake_minimum_required(VERSION 3.20)
foreach(required TOOL CHECKER OUT)
  if(NOT DEFINED ${required} OR "${${required}}" STREQUAL "")
    message(FATAL_ERROR "import regression requires ${required}")
  endif()
endforeach()
file(MAKE_DIRECTORY "${OUT}")

function(check_case format fixture expected_success diagnostic)
  set(input "${OUT}/${fixture}")
  file(WRITE "${input}" "synthetic inspector fixture; not a native binary\n")
  execute_process(COMMAND "${CMAKE_COMMAND}" "-DTOOL=${TOOL}" "-DFORMAT=${format}"
                  "-DFILES=${input}" -P "${CHECKER}"
                  OUTPUT_VARIABLE out ERROR_VARIABLE err RESULT_VARIABLE rc)
  # CMake wraps diagnostics according to message width; compare words, not display line breaks.
  string(REGEX REPLACE "[ \t\r\n]+" " " diagnostic_text "${out}\n${err}")
  if(expected_success)
    if(NOT rc STREQUAL "0" OR NOT out MATCHES "import-table gate: PASS")
      message(FATAL_ERROR "${format}/${fixture}: expected inspection success, got ${rc}\n${out}\n${err}")
    endif()
  else()
    if(rc STREQUAL "0" OR NOT diagnostic_text MATCHES "${diagnostic}")
      message(FATAL_ERROR "${format}/${fixture}: expected '${diagnostic}' refusal, got ${rc}\n${out}\n${err}")
    endif()
  endif()
endfunction()

foreach(format ELF COFF MACHO)
  check_case("${format}" "allowed ${format}.fixture" TRUE "")
  check_case("${format}" "vendor-${format}.fixture" FALSE "VENDOR RUNTIME IMPORTED")
  check_case("${format}" "unknown-${format}.fixture" FALSE "outside the platform OS/CRT allowlist")
  check_case("${format}" "empty-${format}.fixture" FALSE "empty or unrecognized tool output")
  check_case("${format}" "malformed-${format}.fixture" FALSE "empty or unrecognized tool output")
  check_case("${format}" "partial-${format}.fixture" FALSE "empty or unrecognized tool output")
  check_case("${format}" "tool-error-${format}.fixture" FALSE "fixture inspector failure")
endforeach()
foreach(command weak reexport upward lazy)
  check_case(MACHO "allowed-${command}.fixture" TRUE "")
  check_case(MACHO "vendor-${command}.fixture" FALSE "VENDOR RUNTIME IMPORTED")
endforeach()
check_case(MACHO missing-name.fixture FALSE "empty or unrecognized tool output")
check_case(COFF allowed-delay.fixture TRUE "")
check_case(COFF vendor-delay.fixture FALSE "VENDOR RUNTIME IMPORTED")
check_case(UNKNOWN allowed-unknown-format.fixture FALSE "unsupported FORMAT")

execute_process(COMMAND "${CMAKE_COMMAND}" "-DTOOL=${TOOL}" -DFORMAT=ELF "-DFILES=" -P "${CHECKER}"
                OUTPUT_VARIABLE out ERROR_VARIABLE err RESULT_VARIABLE rc)
if(rc STREQUAL "0" OR NOT err MATCHES "nonempty TOOL and FILES required")
  message(FATAL_ERROR "empty input list must refuse: ${rc}\n${out}\n${err}")
endif()
execute_process(COMMAND "${CMAKE_COMMAND}" "-DTOOL=${TOOL}" -DFORMAT=ELF
                "-DFILES=${OUT}/absent-input.fixture" -P "${CHECKER}"
                OUTPUT_VARIABLE out ERROR_VARIABLE err RESULT_VARIABLE rc)
if(rc STREQUAL "0" OR NOT err MATCHES "import input is not a file")
  message(FATAL_ERROR "absent input must refuse: ${rc}\n${out}\n${err}")
endif()
message(STATUS "import parser regression: PASS (35 positive/refusal cases; platform runtime NOT_RUN)")
