/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cpu_kernels.h - CPU builds of the M3 kernel C sources. Each kernel is compiled once
 * per ISA level (scalar, AVX2 = x86-64-v3, AVX-512 = x86-64-v4) from the same .cl bytes and
 * exposed as a range function that runs work-items [begin, end).
 */
#ifndef M3VCPU_CPU_KERNELS_H
#define M3VCPU_CPU_KERNELS_H

#include <stdint.h>

typedef void (*m3vcpu_cpu_range_fn)(void *const *buffers, const int32_t *scalars, uint64_t begin, uint64_t end);

void m3vcpu_cpu_rv32im_slice_scalar(void *const *buffers, const int32_t *scalars, uint64_t begin, uint64_t end);
void m3vcpu_cpu_rv32im_slice_avx2(void *const *buffers, const int32_t *scalars, uint64_t begin, uint64_t end);
void m3vcpu_cpu_rv32im_slice_avx512(void *const *buffers, const int32_t *scalars, uint64_t begin, uint64_t end);

#endif /* M3VCPU_CPU_KERNELS_H */
