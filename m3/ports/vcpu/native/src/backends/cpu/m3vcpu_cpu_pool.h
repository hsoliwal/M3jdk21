/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cpu_pool.h - fixed worker pool with dynamic chunking. A launch publishes one job; the
 * caller and every worker claim chunks of work-items with an atomic counter until the range
 * is exhausted, so fast (P) and slow (E) cores self-balance. One job at a time per pool.
 */
#ifndef M3VCPU_CPU_POOL_H
#define M3VCPU_CPU_POOL_H

#include <stdint.h>

#include "m3vcpu_cpu_kernels.h"

typedef struct m3_cpu_pool m3_cpu_pool;

/* threads counts the calling thread; threads - 1 workers are started immediately. */
int m3_cpu_pool_create(uint32_t threads, m3_cpu_pool **out);
void m3_cpu_pool_destroy(m3_cpu_pool *pool);
uint32_t m3_cpu_pool_threads(const m3_cpu_pool *pool);
/* Runs fn over work-items [0, items) and returns when all of them finished. */
void m3_cpu_pool_run(m3_cpu_pool *pool, m3vcpu_cpu_range_fn fn, void *const *buffers, const int32_t *scalars,
                     uint64_t items);

#endif /* M3VCPU_CPU_POOL_H */
