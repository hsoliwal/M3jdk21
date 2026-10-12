/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cpu_backend.c - the always-present CPU backend ("cpu"). Host memory buffers, kernels
 * compiled from the M3 kernel C sources once per ISA level, runtime ISA dispatch (CPUID),
 * and a worker pool with dynamic chunking. Built into the m3vcpu core library.
 */
#include "m3vcpu.h"
#include "m3vcpu_cpu_isa.h"
#include "m3vcpu_cpu_kernels.h"
#include "m3vcpu_cpu_pool.h"
#include "m3vcpu_sys.h"

#include <stdlib.h>
#include <string.h>

#define M3_CPU_CTX_MAGIC 0x4d334358u /* "M3CX" */
#define M3_CPU_BUF_MAGIC 0x4d334342u /* "M3CB" */
#define M3_CPU_KER_MAGIC 0x4d33434bu /* "M3CK" */
#define M3_CPU_MAX_LOCAL 1024u
#define M3_CPU_DEFAULT_LOCAL 64u
#define M3_CPU_SCORE 100

typedef struct m3_cpu_link {
    struct m3_cpu_link *prev;
    struct m3_cpu_link *next;
} m3_cpu_link;

typedef struct m3_cpu_kernel_desc {
    const char *entry;
    uint32_t arg_count;
    uint32_t arg_kinds[M3VCPU_MAX_ARGS];
    m3vcpu_cpu_range_fn variants[4]; /* indexed by M3VCPU_ISA_*; [0] unused */
} m3_cpu_kernel_desc;

#if defined(M3VCPU_HAVE_AVX2)
#define M3_RV32_AVX2 m3vcpu_cpu_rv32im_slice_avx2
#else
#define M3_RV32_AVX2 NULL
#endif
#if defined(M3VCPU_HAVE_AVX512)
#define M3_RV32_AVX512 m3vcpu_cpu_rv32im_slice_avx512
#else
#define M3_RV32_AVX512 NULL
#endif

static const m3_cpu_kernel_desc m3_cpu_kernels[] = {
    {"m3k_rv32im_slice",
     4u,
     {M3VCPU_ARG_BUFFER, M3VCPU_ARG_BUFFER, M3VCPU_ARG_BUFFER, M3VCPU_ARG_BUFFER},
     {NULL, m3vcpu_cpu_rv32im_slice_scalar, M3_RV32_AVX2, M3_RV32_AVX512}},
};

struct m3vcpu_buf {
    m3_cpu_link link; /* first member: list node */
    uint32_t magic;
    m3vcpu_ctx *owner;
    size_t size;
    uint8_t *data;
};

struct m3vcpu_kernel {
    m3_cpu_link link;
    uint32_t magic;
    m3vcpu_ctx *owner;
    const m3_cpu_kernel_desc *desc;
    m3vcpu_cpu_range_fn fn;
};

struct m3vcpu_ctx {
    uint32_t magic;
    uint32_t isa;
    m3_cpu_pool *pool;
    m3_cpu_link bufs;
    m3_cpu_link kernels;
    char err[256];
};

static M3_THREAD_LOCAL char m3_cpu_error[256];

static void m3_link_init(m3_cpu_link *head) { head->prev = head->next = head; }

static void m3_link_push(m3_cpu_link *head, m3_cpu_link *node)
{
    node->next = head->next;
    node->prev = head;
    head->next->prev = node;
    head->next = node;
}

static void m3_link_remove(m3_cpu_link *node)
{
    node->prev->next = node->next;
    node->next->prev = node->prev;
    node->next = node->prev = node;
}

static int32_t m3_cpu_fail(m3vcpu_ctx *ctx, int32_t status, const char *msg)
{
    m3_strlcpy(ctx == NULL ? m3_cpu_error : ctx->err, msg, sizeof m3_cpu_error);
    return status;
}

static int m3_cpu_isa_supported(const m3_cpu_isa_info *info, uint32_t isa)
{
    switch (isa) {
    case M3VCPU_ISA_SCALAR:
        return 1;
    case M3VCPU_ISA_AVX2:
        return info->v3 && m3_cpu_kernels[0].variants[M3VCPU_ISA_AVX2] != NULL;
    case M3VCPU_ISA_AVX512:
        return info->v4 && m3_cpu_kernels[0].variants[M3VCPU_ISA_AVX512] != NULL;
    default:
        return 0;
    }
}

static uint32_t m3_cpu_best_isa(const m3_cpu_isa_info *info)
{
    if (m3_cpu_isa_supported(info, M3VCPU_ISA_AVX512)) {
        return M3VCPU_ISA_AVX512;
    }
    return m3_cpu_isa_supported(info, M3VCPU_ISA_AVX2) ? M3VCPU_ISA_AVX2 : M3VCPU_ISA_SCALAR;
}

/* ---- probe / device ------------------------------------------------------------------- */
static int32_t m3_cpu_probe(m3vcpu_probe *out)
{
    m3_cpu_isa_info info;
    if (out == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    m3_cpu_isa_detect(&info);
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    out->available = 1;
    out->score = M3_CPU_SCORE;
    out->device_count = 1;
    m3_format(out->reason, sizeof out->reason, "ok: %s, %u logical processors, isa %s", info.brand,
              (unsigned)m3_cpu_count(), m3_cpu_isa_name(m3_cpu_best_isa(&info)));
    return M3VCPU_OK;
}

static int32_t m3_cpu_device_count(void) { return 1; }

static int32_t m3_cpu_device_info(int32_t index, m3vcpu_device_info *out)
{
    m3_cpu_isa_info info;
    uint64_t mem = m3_physical_memory();
    if (out == NULL) {
        return m3_cpu_fail(NULL, M3VCPU_E_INVALID_ARG, "device_info: null out");
    }
    if (index != 0) {
        return m3_cpu_fail(NULL, M3VCPU_E_NO_DEVICE, "device_info: the cpu backend has one device (0)");
    }
    m3_cpu_isa_detect(&info);
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    out->kind = M3VCPU_DEVICE_CPU;
    out->compute_units = m3_cpu_count();
    out->max_local = M3_CPU_MAX_LOCAL;
    out->global_mem_bytes = mem;
    out->max_alloc_bytes = mem;
    out->flags = M3VCPU_DEVICE_FLAG_FP64 | M3VCPU_DEVICE_FLAG_UNIFIED;
    out->score = M3_CPU_SCORE;
    m3_strlcpy(out->name, info.brand[0] != '\0' ? info.brand : "host CPU", sizeof out->name);
    m3_strlcpy(out->vendor, info.vendor, sizeof out->vendor);
    m3_strlcpy(out->version, "m3vcpu-cpu 1 (threads + runtime ISA dispatch)", sizeof out->version);
    m3_strlcpy(out->isa, m3_cpu_isa_name(m3_cpu_best_isa(&info)), sizeof out->isa);
    return M3VCPU_OK;
}

/* ---- context -------------------------------------------------------------------------- */
static uint32_t m3_cpu_isa_from_env(void)
{
    char buf[32];
    if (m3_getenv("M3VCPU_CPU_ISA", buf, sizeof buf) == NULL) {
        return M3VCPU_ISA_AUTO;
    }
    if (m3_strieq(buf, "scalar")) {
        return M3VCPU_ISA_SCALAR;
    }
    if (m3_strieq(buf, "avx2")) {
        return M3VCPU_ISA_AVX2;
    }
    return m3_strieq(buf, "avx512") ? M3VCPU_ISA_AVX512 : M3VCPU_ISA_AUTO;
}

static int32_t m3_cpu_ctx_create(int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_ctx **out)
{
    m3_cpu_isa_info info;
    uint32_t isa = M3VCPU_ISA_AUTO;
    uint32_t threads = 0;
    m3vcpu_ctx *ctx;
    if (out == NULL) {
        return m3_cpu_fail(NULL, M3VCPU_E_INVALID_ARG, "ctx_create: null out");
    }
    *out = NULL;
    if (device != 0) {
        return m3_cpu_fail(NULL, M3VCPU_E_NO_DEVICE, "ctx_create: the cpu backend has one device (0)");
    }
    if (opts != NULL && opts->struct_size >= sizeof *opts) {
        isa = opts->isa;
        threads = opts->threads;
    }
    if (isa == M3VCPU_ISA_AUTO) {
        isa = m3_cpu_isa_from_env();
    }
    m3_cpu_isa_detect(&info);
    if (isa == M3VCPU_ISA_AUTO) {
        isa = m3_cpu_best_isa(&info);
    }
    if (!m3_cpu_isa_supported(&info, isa)) {
        char msg[128];
        m3_format(msg, sizeof msg, "ctx_create: isa %s not supported by this CPU or build", m3_cpu_isa_name(isa));
        return m3_cpu_fail(NULL, M3VCPU_E_UNAVAILABLE, msg);
    }
    ctx = (m3vcpu_ctx *)calloc(1, sizeof *ctx);
    if (ctx == NULL) {
        return m3_cpu_fail(NULL, M3VCPU_E_OUT_OF_MEMORY, "ctx_create: allocation failed");
    }
    ctx->magic = M3_CPU_CTX_MAGIC;
    ctx->isa = isa;
    m3_link_init(&ctx->bufs);
    m3_link_init(&ctx->kernels);
    if (m3_cpu_pool_create(threads == 0u ? m3_cpu_count() : threads, &ctx->pool) != 0) {
        free(ctx);
        return m3_cpu_fail(NULL, M3VCPU_E_OUT_OF_MEMORY, "ctx_create: thread pool");
    }
    m3_format(ctx->err, sizeof ctx->err, "ok: isa %s, %u threads", m3_cpu_isa_name(isa),
              (unsigned)m3_cpu_pool_threads(ctx->pool));
    *out = ctx;
    return M3VCPU_OK;
}

static void m3_cpu_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf);
static void m3_cpu_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel);

static void m3_cpu_ctx_destroy(m3vcpu_ctx *ctx)
{
    if (ctx == NULL || ctx->magic != M3_CPU_CTX_MAGIC) {
        return;
    }
    while (ctx->bufs.next != &ctx->bufs) {
        m3_cpu_buf_free(ctx, (m3vcpu_buf *)(void *)ctx->bufs.next);
    }
    while (ctx->kernels.next != &ctx->kernels) {
        m3_cpu_kernel_free(ctx, (m3vcpu_kernel *)(void *)ctx->kernels.next);
    }
    m3_cpu_pool_destroy(ctx->pool);
    ctx->magic = 0;
    free(ctx);
}

/* ---- buffers -------------------------------------------------------------------------- */
static int m3_cpu_owns(const m3vcpu_ctx *ctx, const m3vcpu_buf *buf)
{
    return buf != NULL && buf->magic == M3_CPU_BUF_MAGIC && buf->owner == ctx;
}

static int32_t m3_cpu_buf_alloc(m3vcpu_ctx *ctx, size_t bytes, uint32_t flags, m3vcpu_buf **out)
{
    m3vcpu_buf *buf;
    (void)flags;
    if (ctx == NULL || out == NULL || bytes == 0u) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "buf_alloc: null argument or zero size");
    }
    buf = (m3vcpu_buf *)calloc(1, sizeof *buf);
    if (buf == NULL) {
        return m3_cpu_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "buf_alloc: descriptor");
    }
    buf->data = (uint8_t *)m3_aligned_alloc(64u, bytes);
    if (buf->data == NULL) {
        free(buf);
        return m3_cpu_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "buf_alloc: host memory");
    }
    buf->magic = M3_CPU_BUF_MAGIC;
    buf->owner = ctx;
    buf->size = bytes;
    m3_link_push(&ctx->bufs, &buf->link);
    *out = buf;
    return M3VCPU_OK;
}

static int32_t m3_cpu_buf_check(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, size_t offset, size_t bytes,
                                const void *host)
{
    if (ctx == NULL || !m3_cpu_owns(ctx, buf) || (host == NULL && bytes != 0u)) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "buffer: foreign buffer or null host pointer");
    }
    if (offset > buf->size || bytes > buf->size - offset) {
        return m3_cpu_fail(ctx, M3VCPU_E_OUT_OF_RANGE, "buffer: range outside the buffer");
    }
    return M3VCPU_OK;
}

static int32_t m3_cpu_buf_write(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, const void *src, size_t bytes)
{
    int32_t rc = m3_cpu_buf_check(ctx, buf, offset, bytes, src);
    if (rc == M3VCPU_OK && bytes != 0u) {
        memcpy(buf->data + offset, src, bytes);
    }
    return rc;
}

static int32_t m3_cpu_buf_read(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, void *dst, size_t bytes)
{
    int32_t rc = m3_cpu_buf_check(ctx, buf, offset, bytes, dst);
    if (rc == M3VCPU_OK && bytes != 0u) {
        memcpy(dst, buf->data + offset, bytes);
    }
    return rc;
}

static void m3_cpu_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf)
{
    if (ctx == NULL || !m3_cpu_owns(ctx, buf)) {
        return;
    }
    m3_link_remove(&buf->link);
    buf->magic = 0;
    m3_aligned_free(buf->data);
    free(buf);
}

/* ---- kernels -------------------------------------------------------------------------- */
static int32_t m3_cpu_kernel_load(m3vcpu_ctx *ctx, const m3k_blob *blobs, size_t count, const char *entry,
                                  m3vcpu_kernel **out)
{
    size_t i, k;
    if (ctx == NULL || out == NULL || entry == NULL || (blobs == NULL && count != 0u)) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "kernel_load: null argument");
    }
    *out = NULL;
    for (i = 0; i < count; ++i) {
        if (blobs[i].format != M3K_CPU_SYMBOL || blobs[i].size != strlen(entry)
            || memcmp(blobs[i].bytes, entry, blobs[i].size) != 0) {
            continue;
        }
        for (k = 0; k < sizeof m3_cpu_kernels / sizeof m3_cpu_kernels[0]; ++k) {
            if (strcmp(m3_cpu_kernels[k].entry, entry) == 0) {
                m3vcpu_kernel *kernel = (m3vcpu_kernel *)calloc(1, sizeof *kernel);
                if (kernel == NULL) {
                    return m3_cpu_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "kernel_load: descriptor");
                }
                kernel->magic = M3_CPU_KER_MAGIC;
                kernel->owner = ctx;
                kernel->desc = &m3_cpu_kernels[k];
                kernel->fn = m3_cpu_kernels[k].variants[ctx->isa];
                if (kernel->fn == NULL) {
                    kernel->fn = m3_cpu_kernels[k].variants[M3VCPU_ISA_SCALAR];
                }
                m3_link_push(&ctx->kernels, &kernel->link);
                *out = kernel;
                return M3VCPU_OK;
            }
        }
        return m3_cpu_fail(ctx, M3VCPU_E_NOT_FOUND, "kernel_load: no CPU build of that kernel symbol");
    }
    return m3_cpu_fail(ctx, M3VCPU_E_NO_KERNEL_FORMAT, "kernel_load: no M3K_CPU_SYMBOL blob naming the entry");
}

static void m3_cpu_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel)
{
    if (ctx == NULL || kernel == NULL || kernel->magic != M3_CPU_KER_MAGIC || kernel->owner != ctx) {
        return;
    }
    m3_link_remove(&kernel->link);
    kernel->magic = 0;
    free(kernel);
}

static int32_t m3_cpu_launch_1d(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel, uint64_t global, uint32_t local,
                                const m3vcpu_arg *args, uint32_t count)
{
    void *buffers[M3VCPU_MAX_ARGS];
    int32_t scalars[M3VCPU_MAX_ARGS];
    uint64_t items;
    uint32_t i;
    if (ctx == NULL || kernel == NULL || kernel->magic != M3_CPU_KER_MAGIC || kernel->owner != ctx) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: foreign kernel");
    }
    if (count != kernel->desc->arg_count || (count > 0u && args == NULL)) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: argument count does not match the kernel");
    }
    if (local > M3_CPU_MAX_LOCAL) {
        return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: local size above 1024");
    }
    for (i = 0; i < count; ++i) {
        buffers[i] = NULL;
        scalars[i] = 0;
        if (args[i].kind != kernel->desc->arg_kinds[i]) {
            return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: argument kind does not match the kernel");
        }
        if (args[i].kind == M3VCPU_ARG_BUFFER) {
            if (!m3_cpu_owns(ctx, args[i].buf)) {
                return m3_cpu_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: foreign buffer argument");
            }
            buffers[i] = args[i].buf->data;
        } else {
            scalars[i] = args[i].i32;
        }
    }
    if (local == 0u) {
        local = M3_CPU_DEFAULT_LOCAL;
    }
    items = (global + local - 1u) / local * local;
    m3_cpu_pool_run(ctx->pool, kernel->fn, buffers, scalars, items);
    return M3VCPU_OK;
}

static int32_t m3_cpu_finish(m3vcpu_ctx *ctx)
{
    return ctx == NULL ? M3VCPU_E_INVALID_ARG : M3VCPU_OK; /* launches are synchronous */
}

static const char *m3_cpu_last_error(const m3vcpu_ctx *ctx) { return ctx == NULL ? m3_cpu_error : ctx->err; }

static const m3vcpu_backend_v1 m3_cpu_table = {
    .abi = M3VCPU_ABI_V1,
    .struct_size = (uint32_t)sizeof(m3vcpu_backend_v1),
    .name = "cpu",
    .probe = m3_cpu_probe,
    .device_count = m3_cpu_device_count,
    .device_info = m3_cpu_device_info,
    .ctx_create = m3_cpu_ctx_create,
    .ctx_destroy = m3_cpu_ctx_destroy,
    .buf_alloc = m3_cpu_buf_alloc,
    .buf_write = m3_cpu_buf_write,
    .buf_read = m3_cpu_buf_read,
    .buf_free = m3_cpu_buf_free,
    .kernel_load = m3_cpu_kernel_load,
    .kernel_free = m3_cpu_kernel_free,
    .launch_1d = m3_cpu_launch_1d,
    .finish = m3_cpu_finish,
    .last_error = m3_cpu_last_error,
};

const m3vcpu_backend_v1 *m3vcpu_cpu_backend_v1(void);
const m3vcpu_backend_v1 *m3vcpu_cpu_backend_v1(void) { return &m3_cpu_table; }
