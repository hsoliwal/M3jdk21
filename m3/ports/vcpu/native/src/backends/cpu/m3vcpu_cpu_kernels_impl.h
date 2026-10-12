/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cpu_kernels_impl.h - body of one per-ISA translation unit. The including file defines
 * M3VCPU_CPU_VARIANT (scalar | avx2 | avx512); CMake sets the matching ISA flags on that file.
 * The kernel source is the unmodified kernels/rv32im_slice.cl compiled as C through the
 * m3k_portability.h shim. Every kernel helper is static, so no inline function compiled with
 * wider ISA flags can be merged into the scalar path by the linker.
 */
#ifndef M3VCPU_CPU_VARIANT
#error "define M3VCPU_CPU_VARIANT before including m3vcpu_cpu_kernels_impl.h"
#endif

#define M3K_TARGET_CPU 1
#include "m3k_portability.h"
#include "rv32im_slice.cl"

#include "m3vcpu_cpu_kernels.h"
#include "m3vcpu_rv32im.h"

/* Kernel-side constants must equal the host ABI header. */
_Static_assert(M3K_RV_REGS == M3VCPU_RV32_REGISTER_COUNT, "register count");
_Static_assert(M3K_RV_STATE_PC == M3VCPU_RV32_STATE_PC, "STATE_PC");
_Static_assert(M3K_RV_STATE_STATUS == M3VCPU_RV32_STATE_STATUS, "STATE_STATUS");
_Static_assert(M3K_RV_STATE_RETIRED == M3VCPU_RV32_STATE_RETIRED, "STATE_RETIRED");
_Static_assert(M3K_RV_STATE_TRAP_CAUSE == M3VCPU_RV32_STATE_TRAP_CAUSE, "STATE_TRAP_CAUSE");
_Static_assert(M3K_RV_STATE_TRAP_VALUE == M3VCPU_RV32_STATE_TRAP_VALUE, "STATE_TRAP_VALUE");
_Static_assert(M3K_RV_STATE_EXIT_CODE == M3VCPU_RV32_STATE_EXIT_CODE, "STATE_EXIT_CODE");
_Static_assert(M3K_RV_STATE_STRIDE == M3VCPU_RV32_STATE_STRIDE, "STATE_STRIDE");
_Static_assert(M3K_RV_READY == M3VCPU_RV32_STATUS_READY, "READY");
_Static_assert(M3K_RV_HALTED == M3VCPU_RV32_STATUS_HALTED, "HALTED");
_Static_assert(M3K_RV_TRAPPED == M3VCPU_RV32_STATUS_TRAPPED, "TRAPPED");
_Static_assert(M3K_RV_YIELDED == M3VCPU_RV32_STATUS_YIELDED, "YIELDED");
_Static_assert(M3K_RV_TRAP_NONE == M3VCPU_RV32_TRAP_NONE, "TRAP_NONE");
_Static_assert(M3K_RV_TRAP_FETCH_MISALIGNED == M3VCPU_RV32_TRAP_INSTRUCTION_ADDRESS_MISALIGNED, "cause 0");
_Static_assert(M3K_RV_TRAP_FETCH_FAULT == M3VCPU_RV32_TRAP_INSTRUCTION_ACCESS_FAULT, "cause 1");
_Static_assert(M3K_RV_TRAP_ILLEGAL == M3VCPU_RV32_TRAP_ILLEGAL_INSTRUCTION, "cause 2");
_Static_assert(M3K_RV_TRAP_BREAKPOINT == M3VCPU_RV32_TRAP_BREAKPOINT, "cause 3");
_Static_assert(M3K_RV_TRAP_LOAD_MISALIGNED == M3VCPU_RV32_TRAP_LOAD_ADDRESS_MISALIGNED, "cause 4");
_Static_assert(M3K_RV_TRAP_LOAD_FAULT == M3VCPU_RV32_TRAP_LOAD_ACCESS_FAULT, "cause 5");
_Static_assert(M3K_RV_TRAP_STORE_MISALIGNED == M3VCPU_RV32_TRAP_STORE_ADDRESS_MISALIGNED, "cause 6");
_Static_assert(M3K_RV_TRAP_STORE_FAULT == M3VCPU_RV32_TRAP_STORE_ACCESS_FAULT, "cause 7");
_Static_assert(M3K_RV_TRAP_ECALL == M3VCPU_RV32_TRAP_ENVIRONMENT_CALL, "cause 8");
_Static_assert(M3K_RV_CONFIG_CORE_COUNT == M3VCPU_RV32_CONFIG_CORE_COUNT, "CONFIG_CORE_COUNT");
_Static_assert(M3K_RV_CONFIG_WORDS == M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE, "CONFIG_WORDS");
_Static_assert(M3K_RV_CONFIG_BUDGET == M3VCPU_RV32_CONFIG_INSTRUCTION_BUDGET, "CONFIG_BUDGET");
_Static_assert(M3K_RV_XM3_HALT == (m3_u32)M3VCPU_RV32_XM3_HALT, "XM3_HALT");

#define M3VCPU_CPU_CAT2(a, b) a##b
#define M3VCPU_CPU_CAT(a, b) M3VCPU_CPU_CAT2(a, b)

void M3VCPU_CPU_CAT(m3vcpu_cpu_rv32im_slice_, M3VCPU_CPU_VARIANT)(void *const *buffers, const int32_t *scalars,
                                                                  uint64_t begin, uint64_t end)
{
    uint64_t gid;
    (void)scalars;
    for (gid = begin; gid < end; ++gid) {
        m3k_rv32im_slice((int)gid, (m3_i32 *)buffers[0], (m3_i32 *)buffers[1], (m3_i32 *)buffers[2],
                         (const m3_i32 *)buffers[3]);
    }
}
