# SPDX-License-Identifier: Apache-2.0
file(REMOVE "${DB}" "${DB}-wal" "${DB}-shm")
execute_process(COMMAND "${TEST}" "${DB}" RESULT_VARIABLE status)
file(REMOVE "${DB}" "${DB}-wal" "${DB}-shm")
if(NOT status EQUAL 0)
  message(FATAL_ERROR "M3FS indexed test failed: ${status}")
endif()
