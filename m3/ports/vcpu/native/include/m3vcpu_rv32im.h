/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_rv32im.h - host-side ABI of the RV32IM slice kernel (built-in kernel "m3k_rv32im_slice").
 *
 * Flat structure-of-arrays state, identical to the donor cpu-gpu-rv32im (Rv32i.java @3bf82671):
 *   arg 0 memory     int32[cores * words_per_core]   little-endian bytes packed in words
 *   arg 1 registers  int32[cores * 32]                x0 reads as 0
 *   arg 2 state      int32[cores * 6]                 PC, STATUS, RETIRED, TRAP_CAUSE, TRAP_VALUE, EXIT_CODE
 *   arg 3 config     int32[3]                         CORE_COUNT, MEMORY_WORDS_PER_CORE, INSTRUCTION_BUDGET
 * One work-item = one core. The kernel-side copies of these constants live in
 * kernels/rv32im_slice.cl and are checked against this header at compile time.
 */
#ifndef M3VCPU_RV32IM_H
#define M3VCPU_RV32IM_H

#define M3VCPU_RV32_KERNEL_ENTRY "m3k_rv32im_slice"
#define M3VCPU_RV32_ARG_MEMORY    0
#define M3VCPU_RV32_ARG_REGISTERS 1
#define M3VCPU_RV32_ARG_STATE     2
#define M3VCPU_RV32_ARG_CONFIG    3
#define M3VCPU_RV32_ARG_COUNT     4

#define M3VCPU_RV32_REGISTER_COUNT 32

#define M3VCPU_RV32_STATE_PC         0
#define M3VCPU_RV32_STATE_STATUS     1
#define M3VCPU_RV32_STATE_RETIRED    2
#define M3VCPU_RV32_STATE_TRAP_CAUSE 3
#define M3VCPU_RV32_STATE_TRAP_VALUE 4
#define M3VCPU_RV32_STATE_EXIT_CODE  5
#define M3VCPU_RV32_STATE_STRIDE     6

#define M3VCPU_RV32_STATUS_READY   0
#define M3VCPU_RV32_STATUS_HALTED  1
#define M3VCPU_RV32_STATUS_TRAPPED 2
#define M3VCPU_RV32_STATUS_YIELDED 3

/* RISC-V exception cause numbers; TRAP_NONE = no active exception. */
#define M3VCPU_RV32_TRAP_NONE                        (-1)
#define M3VCPU_RV32_TRAP_INSTRUCTION_ADDRESS_MISALIGNED 0
#define M3VCPU_RV32_TRAP_INSTRUCTION_ACCESS_FAULT    1
#define M3VCPU_RV32_TRAP_ILLEGAL_INSTRUCTION         2
#define M3VCPU_RV32_TRAP_BREAKPOINT                  3
#define M3VCPU_RV32_TRAP_LOAD_ADDRESS_MISALIGNED     4
#define M3VCPU_RV32_TRAP_LOAD_ACCESS_FAULT           5
#define M3VCPU_RV32_TRAP_STORE_ADDRESS_MISALIGNED    6
#define M3VCPU_RV32_TRAP_STORE_ACCESS_FAULT          7
#define M3VCPU_RV32_TRAP_ENVIRONMENT_CALL            8

#define M3VCPU_RV32_CONFIG_CORE_COUNT            0
#define M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE 1
#define M3VCPU_RV32_CONFIG_INSTRUCTION_BUDGET    2
#define M3VCPU_RV32_CONFIG_STRIDE                3

/* custom-0 semihosting halt; exit code = x10 (a0). */
#define M3VCPU_RV32_XM3_HALT 0x0000000B

#endif /* M3VCPU_RV32IM_H */
