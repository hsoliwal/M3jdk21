/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3k_portability.h - "M3 kernel C" portability shim.
 *
 * M3 kernel C is a restricted OpenCL C 1.2 subset: __global buffers, 32/64-bit integers,
 * a 1-D NDRange, no fp64, no images, no local memory / barriers / atomics, no recursion,
 * no function pointers. One source feeds every target:
 *   OpenCL C (any vendor)   built at run time from the same bytes (header prepended)
 *   NVPTX (CUDA, P4)        clang -x cl -target nvptx64-nvidia-nvcl
 *   CPU                     compiled as C11 with M3K_TARGET_CPU defined, once per ISA level
 *
 * Kernel-author macros
 *   M3_KERNEL      kernel entry storage/qualifier      ("__kernel" | "static")
 *   M3_GLOBAL      global address space                ("__global" | "")
 *   M3_GID_PARAM   leading hidden parameter on CPU     ("" | "int m3k_gid0_,")
 *   M3_GID0        global id in dimension 0, as int
 *   M3_FN          helper-function storage             ("" | "static")
 *   m3_i32 m3_u32 m3_i64 m3_u64   fixed-width integers
 * Donor: the 4-macro CPU shim proven in gpu-survey-scratch/m3k_cpu_shim.c (same .cl as C);
 * the global-id builtin there was a process global, replaced here by a per-call parameter.
 */
#ifndef M3K_PORTABILITY_H
#define M3K_PORTABILITY_H

#if defined(M3K_TARGET_CPU)

#include <stdint.h>
typedef int32_t m3_i32;
typedef uint32_t m3_u32;
typedef int64_t m3_i64;
typedef uint64_t m3_u64;
#define M3_KERNEL static
#define M3_GLOBAL
#define M3_GID_PARAM int m3k_gid0_,
#define M3_GID0 (m3k_gid0_)
#define M3_FN static

#elif defined(__NVPTX__)

typedef int m3_i32;
typedef unsigned int m3_u32;
typedef long m3_i64;
typedef unsigned long m3_u64;
#define M3_KERNEL __kernel
#define M3_GLOBAL __global
#define M3_GID_PARAM
#define M3_GID0 ((int)(__nvvm_read_ptx_sreg_ctaid_x() * __nvvm_read_ptx_sreg_ntid_x() \
                       + __nvvm_read_ptx_sreg_tid_x()))
#define M3_FN

#else /* OpenCL C 1.2 */

typedef int m3_i32;
typedef uint m3_u32;
typedef long m3_i64;
typedef ulong m3_u64;
#define M3_KERNEL __kernel
#define M3_GLOBAL __global
#define M3_GID_PARAM
#define M3_GID0 ((int)get_global_id(0))
#define M3_FN

#endif

#endif /* M3K_PORTABILITY_H */
