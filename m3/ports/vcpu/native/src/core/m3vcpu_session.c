/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_session.c - sessions = backend + device + context. The core validates arguments,
 * tracks buffer sizes and kernel ownership, verifies blob sha256 before any backend sees the
 * bytes, and applies a geometry guard to the built-in RV32IM kernel so a malformed config can
 * never index past a buffer on any backend.
 */
#include "m3vcpu_internal.h"
#include "m3vcpu_rv32im.h"

#include <stdlib.h>
#include <string.h>

typedef int32_t (*m3_guard_fn)(m3vcpu_session *s, const m3vcpu_arg *args, uint32_t count);

typedef struct m3_buf_rec {
    m3vcpu_buf *buf;
    size_t size;
} m3_buf_rec;

typedef struct m3_kernel_rec {
    m3vcpu_kernel *kernel;
    m3_guard_fn guard;
    int builtin;
    char entry[64];
} m3_kernel_rec;

struct m3vcpu_session {
    const m3vcpu_backend_v1 *backend;
    m3vcpu_ctx *ctx;
    int32_t device;
    m3vcpu_device_info info;
    m3_buf_rec *bufs;
    size_t nbufs;
    size_t capbufs;
    m3_kernel_rec *kernels;
    size_t nkernels;
    size_t capkernels;
    char err[512];
};

static int32_t m3_fail(m3vcpu_session *s, int32_t status, const char *op, const char *detail)
{
    m3_format(s->err, sizeof s->err, "%s: %s: %s", op, m3vcpu_status_name(status), detail);
    return status;
}

static int32_t m3_fail_backend(m3vcpu_session *s, int32_t status, const char *op)
{
    const char *detail = s->backend->last_error(s->ctx);
    return m3_fail(s, status, op, detail == NULL ? "" : detail);
}

static m3_buf_rec *m3_find_buf(m3vcpu_session *s, const m3vcpu_buf *buf)
{
    size_t i;
    for (i = 0; i < s->nbufs; ++i) {
        if (s->bufs[i].buf == buf) {
            return &s->bufs[i];
        }
    }
    return NULL;
}

static m3_kernel_rec *m3_find_kernel(m3vcpu_session *s, const m3vcpu_kernel *kernel)
{
    size_t i;
    for (i = 0; i < s->nkernels; ++i) {
        if (s->kernels[i].kernel == kernel) {
            return &s->kernels[i];
        }
    }
    return NULL;
}

/* ---- RV32IM geometry guard ------------------------------------------------------------ */
static int32_t m3_rv32_guard(m3vcpu_session *s, const m3vcpu_arg *args, uint32_t count)
{
    const m3_buf_rec *rec[M3VCPU_RV32_ARG_COUNT];
    int32_t config[M3VCPU_RV32_CONFIG_STRIDE];
    uint64_t cores, words, need[3];
    uint32_t i, j;
    int32_t rc;
    if (count != M3VCPU_RV32_ARG_COUNT) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "m3k_rv32im_slice takes 4 buffer arguments");
    }
    for (i = 0; i < count; ++i) {
        if (args[i].kind != M3VCPU_ARG_BUFFER) {
            return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "m3k_rv32im_slice arguments must be buffers");
        }
        rec[i] = m3_find_buf(s, args[i].buf);
        for (j = 0; j < i; ++j) {
            if (args[j].buf == args[i].buf) {
                return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "m3k_rv32im_slice buffers must not alias");
            }
        }
    }
    if (rec[M3VCPU_RV32_ARG_CONFIG]->size < sizeof config) {
        return m3_fail(s, M3VCPU_E_OUT_OF_RANGE, "launch", "config buffer holds fewer than 3 ints");
    }
    rc = s->backend->buf_read(s->ctx, args[M3VCPU_RV32_ARG_CONFIG].buf, 0, config, sizeof config);
    if (rc != M3VCPU_OK) {
        return m3_fail_backend(s, rc, "launch(config read)");
    }
    if (config[M3VCPU_RV32_CONFIG_CORE_COUNT] <= 0) {
        return M3VCPU_OK; /* no work-item passes the kernel guard */
    }
    if (config[M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE] < 0) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "negative MEMORY_WORDS_PER_CORE");
    }
    cores = (uint64_t)config[M3VCPU_RV32_CONFIG_CORE_COUNT];
    words = (uint64_t)config[M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE];
    if (cores * words > 0xFFFFFFFFu) {
        return m3_fail(s, M3VCPU_E_OUT_OF_RANGE, "launch", "cores * words exceeds 2^32 words");
    }
    need[0] = cores * words * 4u;
    need[1] = cores * M3VCPU_RV32_REGISTER_COUNT * 4u;
    need[2] = cores * M3VCPU_RV32_STATE_STRIDE * 4u;
    for (i = 0; i < 3u; ++i) {
        if ((uint64_t)rec[i]->size < need[i]) {
            char detail[160];
            m3_format(detail, sizeof detail, "argument %u holds %llu bytes, geometry needs %llu", (unsigned)i,
                      (unsigned long long)rec[i]->size, (unsigned long long)need[i]);
            return m3_fail(s, M3VCPU_E_OUT_OF_RANGE, "launch", detail);
        }
    }
    return M3VCPU_OK;
}

static m3_guard_fn m3_guard_for(const char *entry)
{
    return strcmp(entry, M3VCPU_RV32_KERNEL_ENTRY) == 0 ? m3_rv32_guard : NULL;
}

/* ---- lifecycle ------------------------------------------------------------------------ */
int32_t m3vcpu_session_create(const m3vcpu_backend_v1 *backend, int32_t device, const m3vcpu_ctx_opts *opts,
                              m3vcpu_session **out)
{
    m3vcpu_session *s = (m3vcpu_session *)calloc(1, sizeof *s);
    int32_t rc;
    if (s == NULL) {
        m3vcpu_tls_error_set("session allocation failed");
        return M3VCPU_E_OUT_OF_MEMORY;
    }
    s->backend = backend;
    s->device = device;
    s->info.struct_size = sizeof s->info;
    rc = backend->device_info(device, &s->info);
    if (rc == M3VCPU_OK) {
        rc = backend->ctx_create(device, opts, &s->ctx);
    }
    if (rc != M3VCPU_OK) {
        m3vcpu_tls_error_set("%s", backend->last_error(NULL));
        free(s);
        return rc;
    }
    *out = s;
    return M3VCPU_OK;
}

void m3vcpu_close(m3vcpu_session *s)
{
    size_t i;
    if (s == NULL) {
        return;
    }
    for (i = 0; i < s->nkernels; ++i) {
        s->backend->kernel_free(s->ctx, s->kernels[i].kernel);
    }
    for (i = 0; i < s->nbufs; ++i) {
        s->backend->buf_free(s->ctx, s->bufs[i].buf);
    }
    s->backend->ctx_destroy(s->ctx);
    free(s->kernels);
    free(s->bufs);
    free(s);
}

const m3vcpu_backend_v1 *m3vcpu_session_backend(const m3vcpu_session *s) { return s == NULL ? NULL : s->backend; }

int32_t m3vcpu_session_device(const m3vcpu_session *s, int32_t *index, m3vcpu_device_info *info)
{
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    if (index != NULL) {
        *index = s->device;
    }
    if (info != NULL) {
        *info = s->info;
    }
    return M3VCPU_OK;
}

const char *m3vcpu_last_error(const m3vcpu_session *s) { return s == NULL ? m3vcpu_tls_error() : s->err; }

/* ---- buffers -------------------------------------------------------------------------- */
int32_t m3vcpu_buffer_alloc(m3vcpu_session *s, size_t bytes, uint32_t flags, m3vcpu_buf **out)
{
    m3vcpu_buf *buf = NULL;
    int32_t rc;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    if (out == NULL || bytes == 0u || flags > M3VCPU_BUF_WRITE_ONLY) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "buffer_alloc", "null out, zero size or unknown flags");
    }
    *out = NULL;
    if (s->nbufs == s->capbufs) {
        size_t cap = s->capbufs == 0 ? 8u : s->capbufs * 2u;
        m3_buf_rec *grown = (m3_buf_rec *)realloc(s->bufs, cap * sizeof *grown);
        if (grown == NULL) {
            return m3_fail(s, M3VCPU_E_OUT_OF_MEMORY, "buffer_alloc", "buffer table");
        }
        s->bufs = grown;
        s->capbufs = cap;
    }
    rc = s->backend->buf_alloc(s->ctx, bytes, flags, &buf);
    if (rc != M3VCPU_OK) {
        return m3_fail_backend(s, rc, "buffer_alloc");
    }
    s->bufs[s->nbufs].buf = buf;
    s->bufs[s->nbufs].size = bytes;
    s->nbufs++;
    *out = buf;
    return M3VCPU_OK;
}

static int32_t m3_check_range(m3vcpu_session *s, const m3vcpu_buf *buf, size_t offset, size_t bytes,
                              const void *host, const char *op)
{
    const m3_buf_rec *rec = m3_find_buf(s, buf);
    if (rec == NULL) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, op, "buffer does not belong to this session");
    }
    if (host == NULL && bytes != 0u) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, op, "null host pointer");
    }
    if (offset > rec->size || bytes > rec->size - offset) {
        char detail[128];
        m3_format(detail, sizeof detail, "[%llu, +%llu) outside %llu-byte buffer", (unsigned long long)offset,
                  (unsigned long long)bytes, (unsigned long long)rec->size);
        return m3_fail(s, M3VCPU_E_OUT_OF_RANGE, op, detail);
    }
    return M3VCPU_OK;
}

int32_t m3vcpu_buffer_write(m3vcpu_session *s, m3vcpu_buf *buf, size_t offset, const void *src, size_t bytes)
{
    int32_t rc;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    rc = m3_check_range(s, buf, offset, bytes, src, "buffer_write");
    if (rc != M3VCPU_OK || bytes == 0u) {
        return rc;
    }
    rc = s->backend->buf_write(s->ctx, buf, offset, src, bytes);
    return rc == M3VCPU_OK ? rc : m3_fail_backend(s, rc, "buffer_write");
}

int32_t m3vcpu_buffer_read(m3vcpu_session *s, m3vcpu_buf *buf, size_t offset, void *dst, size_t bytes)
{
    int32_t rc;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    rc = m3_check_range(s, buf, offset, bytes, dst, "buffer_read");
    if (rc != M3VCPU_OK || bytes == 0u) {
        return rc;
    }
    rc = s->backend->buf_read(s->ctx, buf, offset, dst, bytes);
    return rc == M3VCPU_OK ? rc : m3_fail_backend(s, rc, "buffer_read");
}

void m3vcpu_buffer_free(m3vcpu_session *s, m3vcpu_buf *buf)
{
    m3_buf_rec *rec;
    if (s == NULL || buf == NULL) {
        return;
    }
    rec = m3_find_buf(s, buf);
    if (rec == NULL) {
        return;
    }
    s->backend->buf_free(s->ctx, buf);
    *rec = s->bufs[s->nbufs - 1u];
    s->nbufs--;
}

/* ---- kernels -------------------------------------------------------------------------- */
int32_t m3vcpu_builtin_blobs(const char *entry, const m3k_blob **blobs, size_t *count)
{
    size_t i;
    if (entry == NULL || blobs == NULL || count == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    for (i = 0; i < m3vcpu_bundle_count; ++i) {
        if (strcmp(m3vcpu_bundle[i].entry, entry) == 0) {
            *blobs = m3vcpu_bundle[i].blobs;
            *count = m3vcpu_bundle[i].count;
            return M3VCPU_OK;
        }
    }
    return M3VCPU_E_NOT_FOUND;
}

static int32_t m3_verify_blobs(m3vcpu_session *s, const m3k_blob *blobs, size_t count)
{
    size_t i;
    for (i = 0; i < count; ++i) {
        uint8_t digest[32];
        if (blobs[i].bytes == NULL && blobs[i].size != 0u) {
            return m3_fail(s, M3VCPU_E_INVALID_ARG, "kernel_load", "blob with null bytes");
        }
        m3vcpu_sha256(blobs[i].bytes, blobs[i].size, digest);
        if (memcmp(digest, blobs[i].sha256, sizeof digest) != 0) {
            char detail[96];
            m3_format(detail, sizeof detail, "blob %u (format %u) sha256 does not match its bytes", (unsigned)i,
                      (unsigned)blobs[i].format);
            return m3_fail(s, M3VCPU_E_INTEGRITY, "kernel_load", detail);
        }
    }
    return M3VCPU_OK;
}

static int32_t m3_load(m3vcpu_session *s, const m3k_blob *blobs, size_t count, const char *entry, int builtin,
                       m3vcpu_kernel **out)
{
    m3vcpu_kernel *kernel = NULL;
    m3_kernel_rec *rec;
    int32_t rc;
    if (out == NULL || entry == NULL || strlen(entry) >= sizeof rec->entry || blobs == NULL || count == 0u) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "kernel_load", "null out/entry/blobs, or entry too long");
    }
    *out = NULL;
    rc = m3_verify_blobs(s, blobs, count);
    if (rc != M3VCPU_OK) {
        return rc;
    }
    if (s->nkernels == s->capkernels) {
        size_t cap = s->capkernels == 0 ? 4u : s->capkernels * 2u;
        m3_kernel_rec *grown = (m3_kernel_rec *)realloc(s->kernels, cap * sizeof *grown);
        if (grown == NULL) {
            return m3_fail(s, M3VCPU_E_OUT_OF_MEMORY, "kernel_load", "kernel table");
        }
        s->kernels = grown;
        s->capkernels = cap;
    }
    rc = s->backend->kernel_load(s->ctx, blobs, count, entry, &kernel);
    if (rc != M3VCPU_OK) {
        return m3_fail_backend(s, rc, "kernel_load");
    }
    rec = &s->kernels[s->nkernels++];
    memset(rec, 0, sizeof *rec);
    rec->kernel = kernel;
    rec->guard = m3_guard_for(entry);
    rec->builtin = builtin;
    m3_strlcpy(rec->entry, entry, sizeof rec->entry);
    *out = kernel;
    return M3VCPU_OK;
}

int32_t m3vcpu_kernel_load(m3vcpu_session *s, const m3k_blob *blobs, size_t count, const char *entry,
                           m3vcpu_kernel **out)
{
    return s == NULL ? M3VCPU_E_INVALID_ARG : m3_load(s, blobs, count, entry, 0, out);
}

int32_t m3vcpu_kernel_builtin(m3vcpu_session *s, const char *entry, m3vcpu_kernel **out)
{
    const m3k_blob *blobs = NULL;
    size_t count = 0;
    size_t i;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    if (entry == NULL || out == NULL) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "kernel_builtin", "null entry or out");
    }
    for (i = 0; i < s->nkernels; ++i) {
        if (s->kernels[i].builtin && strcmp(s->kernels[i].entry, entry) == 0) {
            *out = s->kernels[i].kernel;
            return M3VCPU_OK;
        }
    }
    if (m3vcpu_builtin_blobs(entry, &blobs, &count) != M3VCPU_OK) {
        return m3_fail(s, M3VCPU_E_NOT_FOUND, "kernel_builtin", entry);
    }
    return m3_load(s, blobs, count, entry, 1, out);
}

void m3vcpu_kernel_free(m3vcpu_session *s, m3vcpu_kernel *kernel)
{
    m3_kernel_rec *rec;
    if (s == NULL || kernel == NULL) {
        return;
    }
    rec = m3_find_kernel(s, kernel);
    if (rec == NULL || rec->builtin) {
        return; /* foreign, or owned by the session until close */
    }
    s->backend->kernel_free(s->ctx, kernel);
    *rec = s->kernels[s->nkernels - 1u];
    s->nkernels--;
}

/* ---- launch --------------------------------------------------------------------------- */
int32_t m3vcpu_launch_1d(m3vcpu_session *s, m3vcpu_kernel *kernel, uint64_t global, uint32_t local,
                         const m3vcpu_arg *args, uint32_t count)
{
    const m3_kernel_rec *rec;
    uint32_t i;
    int32_t rc;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    rec = m3_find_kernel(s, kernel);
    if (rec == NULL) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "kernel does not belong to this session");
    }
    if (count > M3VCPU_MAX_ARGS || (count > 0u && args == NULL)) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "too many arguments or null argument array");
    }
    if (global > 0x7FFFFFFFu) {
        return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "global work size exceeds 2^31-1");
    }
    for (i = 0; i < count; ++i) {
        if (args[i].kind == M3VCPU_ARG_BUFFER) {
            if (m3_find_buf(s, args[i].buf) == NULL) {
                return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "buffer argument does not belong to this session");
            }
        } else if (args[i].kind != M3VCPU_ARG_I32) {
            return m3_fail(s, M3VCPU_E_INVALID_ARG, "launch", "unknown argument kind");
        }
    }
    if (global == 0u) {
        return M3VCPU_OK;
    }
    if (rec->guard != NULL) {
        rc = rec->guard(s, args, count);
        if (rc != M3VCPU_OK) {
            return rc;
        }
    }
    rc = s->backend->launch_1d(s->ctx, kernel, global, local, args, count);
    return rc == M3VCPU_OK ? rc : m3_fail_backend(s, rc, "launch");
}

int32_t m3vcpu_finish(m3vcpu_session *s)
{
    int32_t rc;
    if (s == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    rc = s->backend->finish(s->ctx);
    return rc == M3VCPU_OK ? rc : m3_fail_backend(s, rc, "finish");
}
