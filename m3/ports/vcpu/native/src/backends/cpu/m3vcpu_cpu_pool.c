/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_cpu_pool.c - worker pool with atomic dynamic chunking (see m3vcpu_cpu_pool.h). */
#include "m3vcpu_cpu_pool.h"
#include "m3vcpu_sys.h"

#include <stdlib.h>

#define M3_POOL_MAX_THREADS 1024u
#define M3_POOL_CHUNKS_PER_THREAD 16u
#define M3_POOL_MAX_CHUNK 256u

struct m3_cpu_pool {
    m3_mutex lock;
    m3_cond wake;
    m3_cond done;
    uint32_t total;   /* threads including the caller */
    uint32_t workers; /* started worker threads */
    m3_thread **threads;
    uint64_t generation;
    int shutdown;
    uint32_t pending; /* workers still inside the current generation */
    m3vcpu_cpu_range_fn fn;
    void *const *buffers;
    const int32_t *scalars;
    uint64_t items;
    uint64_t chunk;
    volatile int64_t next;
};

static void m3_pool_drain(m3_cpu_pool *p)
{
    for (;;) {
        int64_t begin = m3_atomic_fetch_add64(&p->next, (int64_t)p->chunk);
        uint64_t end;
        if ((uint64_t)begin >= p->items) {
            return;
        }
        end = (uint64_t)begin + p->chunk;
        if (end > p->items) {
            end = p->items;
        }
        p->fn(p->buffers, p->scalars, (uint64_t)begin, end);
    }
}

static void m3_pool_worker(void *arg)
{
    m3_cpu_pool *p = (m3_cpu_pool *)arg;
    uint64_t seen = 0; /* workers exist before the first job, whose generation is 1 */
    m3_mutex_lock(&p->lock);
    for (;;) {
        while (!p->shutdown && p->generation == seen) {
            m3_cond_wait(&p->wake, &p->lock);
        }
        if (p->shutdown) {
            break;
        }
        seen = p->generation;
        m3_mutex_unlock(&p->lock);
        m3_pool_drain(p);
        m3_mutex_lock(&p->lock);
        if (--p->pending == 0u) {
            m3_cond_signal(&p->done);
        }
    }
    m3_mutex_unlock(&p->lock);
}

int m3_cpu_pool_create(uint32_t threads, m3_cpu_pool **out)
{
    m3_cpu_pool *p = (m3_cpu_pool *)calloc(1, sizeof *p);
    uint32_t i;
    if (p == NULL) {
        return -1;
    }
    if (threads == 0u) {
        threads = 1u;
    }
    if (threads > M3_POOL_MAX_THREADS) {
        threads = M3_POOL_MAX_THREADS;
    }
    m3_mutex_init(&p->lock);
    m3_cond_init(&p->wake);
    m3_cond_init(&p->done);
    p->total = threads;
    if (threads > 1u) {
        p->threads = (m3_thread **)calloc(threads - 1u, sizeof *p->threads);
        if (p->threads == NULL) {
            m3_cpu_pool_destroy(p);
            return -1;
        }
        for (i = 0; i + 1u < threads; ++i) {
            if (m3_thread_start(&p->threads[i], m3_pool_worker, p) != 0) {
                break;
            }
            p->workers++;
        }
    }
    *out = p;
    return 0;
}

void m3_cpu_pool_destroy(m3_cpu_pool *p)
{
    uint32_t i;
    if (p == NULL) {
        return;
    }
    m3_mutex_lock(&p->lock);
    p->shutdown = 1;
    m3_cond_broadcast(&p->wake);
    m3_mutex_unlock(&p->lock);
    for (i = 0; i < p->workers; ++i) {
        m3_thread_join(p->threads[i]);
    }
    free(p->threads);
    m3_cond_destroy(&p->done);
    m3_cond_destroy(&p->wake);
    m3_mutex_destroy(&p->lock);
    free(p);
}

uint32_t m3_cpu_pool_threads(const m3_cpu_pool *p) { return p->workers + 1u; }

void m3_cpu_pool_run(m3_cpu_pool *p, m3vcpu_cpu_range_fn fn, void *const *buffers, const int32_t *scalars,
                     uint64_t items)
{
    uint64_t chunk;
    if (items == 0u) {
        return;
    }
    chunk = items / ((uint64_t)(p->workers + 1u) * M3_POOL_CHUNKS_PER_THREAD);
    if (chunk == 0u) {
        chunk = 1u;
    }
    if (chunk > M3_POOL_MAX_CHUNK) {
        chunk = M3_POOL_MAX_CHUNK;
    }
    if (p->workers == 0u || items <= chunk) {
        fn(buffers, scalars, 0u, items);
        return;
    }
    m3_mutex_lock(&p->lock);
    p->fn = fn;
    p->buffers = buffers;
    p->scalars = scalars;
    p->items = items;
    p->chunk = chunk;
    p->next = 0;
    p->pending = p->workers;
    p->generation++;
    m3_cond_broadcast(&p->wake);
    m3_mutex_unlock(&p->lock);
    m3_pool_drain(p);
    m3_mutex_lock(&p->lock);
    while (p->pending > 0u) {
        m3_cond_wait(&p->done, &p->lock);
    }
    m3_mutex_unlock(&p->lock);
}
