/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_opencl_backend.c - "opencl" backend: the primary portable GPU path (Intel, AMD, NVIDIA).
 * OpenCL.dll / libOpenCL.so.1 is loaded at run time from the system directory only, so the
 * import table holds kernel32 + CRT and nothing vendor-specific. Kernels arrive as M3 kernel C
 * (OpenCL C 1.2) source blobs and are built per context with -cl-std=CL1.2.
 * Probe never crashes: a missing runtime, symbol or platform yields available = 0 + reason.
 * Host batching note (pattern from GPUOS gpu-rv32ima, MIT): one buffer cannot exceed
 * CL_DEVICE_MAX_MEM_ALLOC_SIZE, so callers shard large core batches into several launches.
 */
#include "m3vcpu.h"
#include "m3vcpu_cl_abi.h"
#include "m3vcpu_sys.h"

#include <stdlib.h>
#include <string.h>

#define M3_CL_MAX_PLATFORMS 8u
#define M3_CL_MAX_DEVICES 32
#define M3_CL_CTX_MAGIC 0x4d33434cu
#define M3_CL_BUF_MAGIC 0x4d33424cu
#define M3_CL_KER_MAGIC 0x4d334b4cu
#define M3_CL_DEFAULT_LOCAL 64u

typedef struct m3_cl_link {
    struct m3_cl_link *prev;
    struct m3_cl_link *next;
} m3_cl_link;

typedef struct m3_cl_device {
    m3cl_device id;
    m3vcpu_device_info info;
    uint32_t c_version; /* OpenCL C version x100 */
} m3_cl_device;

static struct {
    m3_mutex lock;
    int ready;
    int32_t status;
    char reason[192];
    m3_lib *lib;
    m3cl_api api;
    m3_cl_device devices[M3_CL_MAX_DEVICES];
    int32_t count;
} m3_cl = {M3_MUTEX_INITIALIZER, 0, 0, {0}, NULL, {0}, {{0}}, 0};

struct m3vcpu_ctx {
    uint32_t magic;
    int32_t device;
    m3cl_context context;
    m3cl_queue queue;
    m3_cl_link bufs;
    m3_cl_link kernels;
    char err[1024];
};

struct m3vcpu_buf {
    m3_cl_link link;
    uint32_t magic;
    m3vcpu_ctx *owner;
    size_t size;
    m3cl_mem mem;
};

struct m3vcpu_kernel {
    m3_cl_link link;
    uint32_t magic;
    m3vcpu_ctx *owner;
    m3cl_program program;
    m3cl_kernel kernel;
    m3cl_uint nargs;
    size_t max_local;
};

static M3_THREAD_LOCAL char m3_cl_error[512];

static const char *m3_cl_code(m3cl_int rc)
{
    switch (rc) {
    case 0: return "CL_SUCCESS";
    case -1: return "CL_DEVICE_NOT_FOUND";
    case -2: return "CL_DEVICE_NOT_AVAILABLE";
    case -3: return "CL_COMPILER_NOT_AVAILABLE";
    case -4: return "CL_MEM_OBJECT_ALLOCATION_FAILURE";
    case -5: return "CL_OUT_OF_RESOURCES";
    case -6: return "CL_OUT_OF_HOST_MEMORY";
    case -11: return "CL_BUILD_PROGRAM_FAILURE";
    case -30: return "CL_INVALID_VALUE";
    case -33: return "CL_INVALID_DEVICE";
    case -34: return "CL_INVALID_CONTEXT";
    case -36: return "CL_INVALID_COMMAND_QUEUE";
    case -38: return "CL_INVALID_MEM_OBJECT";
    case -44: return "CL_INVALID_PROGRAM";
    case -45: return "CL_INVALID_PROGRAM_EXECUTABLE";
    case -46: return "CL_INVALID_KERNEL_NAME";
    case -48: return "CL_INVALID_KERNEL";
    case -49: return "CL_INVALID_ARG_INDEX";
    case -50: return "CL_INVALID_ARG_VALUE";
    case -51: return "CL_INVALID_ARG_SIZE";
    case -52: return "CL_INVALID_KERNEL_ARGS";
    case -54: return "CL_INVALID_WORK_GROUP_SIZE";
    case -55: return "CL_INVALID_WORK_ITEM_SIZE";
    case -61: return "CL_INVALID_BUFFER_SIZE";
    case -63: return "CL_INVALID_GLOBAL_WORK_SIZE";
    case -1001: return "CL_PLATFORM_NOT_FOUND_KHR";
    default: return "CL_ERROR";
    }
}

static int32_t m3_cl_fail(m3vcpu_ctx *ctx, int32_t status, const char *what, m3cl_int rc)
{
    char *dst = ctx == NULL ? m3_cl_error : ctx->err;
    size_t cap = ctx == NULL ? sizeof m3_cl_error : sizeof ctx->err;
    if (rc != 0) {
        m3_format(dst, cap, "%s: %s (%d)", what, m3_cl_code(rc), (int)rc);
    } else {
        m3_strlcpy(dst, what, cap);
    }
    return status;
}

static void m3_cl_link_init(m3_cl_link *h) { h->prev = h->next = h; }

static void m3_cl_link_push(m3_cl_link *h, m3_cl_link *n)
{
    n->next = h->next;
    n->prev = h;
    h->next->prev = n;
    h->next = n;
}

static void m3_cl_link_remove(m3_cl_link *n)
{
    n->prev->next = n->next;
    n->next->prev = n->prev;
    n->next = n->prev = n;
}

/* ---- runtime discovery ------------------------------------------------------------------ */
static void m3_cl_string(m3cl_device d, m3cl_uint what, char *out, size_t cap)
{
    char buf[512];
    size_t got = 0;
    memset(buf, 0, sizeof buf);
    if (m3_cl.api.clGetDeviceInfo(d, what, sizeof buf - 1u, buf, &got) != M3CL_SUCCESS) {
        buf[0] = '\0';
    }
    buf[sizeof buf - 1u] = '\0';
    while (buf[0] == ' ') {
        memmove(buf, buf + 1, strlen(buf));
    }
    m3_strlcpy(out, buf, cap);
}

static uint64_t m3_cl_u64(m3cl_device d, m3cl_uint what, size_t width)
{
    uint64_t v = 0;
    if (width == 4u) {
        m3cl_uint u = 0;
        if (m3_cl.api.clGetDeviceInfo(d, what, sizeof u, &u, NULL) == M3CL_SUCCESS) {
            v = u;
        }
    } else if (m3_cl.api.clGetDeviceInfo(d, what, sizeof v, &v, NULL) != M3CL_SUCCESS) {
        v = 0;
    }
    return v;
}

static size_t m3_cl_size(m3cl_device d, m3cl_uint what)
{
    size_t v = 0;
    if (m3_cl.api.clGetDeviceInfo(d, what, sizeof v, &v, NULL) != M3CL_SUCCESS) {
        v = 0;
    }
    return v;
}

/* "OpenCL C 1.2 ..." -> 120 */
static uint32_t m3_cl_c_version(const char *s)
{
    const char *p = strstr(s, "OpenCL C ");
    if (p == NULL || p[9] < '0' || p[9] > '9' || p[10] != '.' || p[11] < '0' || p[11] > '9') {
        return 0;
    }
    return (uint32_t)(p[9] - '0') * 100u + (uint32_t)(p[11] - '0') * 10u;
}

static void m3_cl_add_device(m3cl_platform platform, m3cl_device id)
{
    m3_cl_device *d;
    m3vcpu_device_info *info;
    char pname[96];
    char cver[64];
    char driver[64];
    uint64_t type;
    if (m3_cl.count >= M3_CL_MAX_DEVICES || m3_cl_u64(id, M3CL_DEVICE_AVAILABLE, 4) == 0u
        || m3_cl_u64(id, M3CL_DEVICE_COMPILER_AVAILABLE, 4) == 0u) {
        return;
    }
    d = &m3_cl.devices[m3_cl.count++];
    memset(d, 0, sizeof *d);
    d->id = id;
    info = &d->info;
    info->struct_size = sizeof *info;
    type = m3_cl_u64(id, M3CL_DEVICE_TYPE, 8);
    if ((type & M3CL_DEVICE_TYPE_GPU) != 0u) {
        int unified = m3_cl_u64(id, M3CL_DEVICE_HOST_UNIFIED_MEMORY, 4) != 0u;
        info->kind = unified ? M3VCPU_DEVICE_GPU_INTEGRATED : M3VCPU_DEVICE_GPU_DISCRETE;
        info->score = unified ? 50 : 1000;
        info->flags |= unified ? M3VCPU_DEVICE_FLAG_UNIFIED : 0u;
    } else if ((type & M3CL_DEVICE_TYPE_ACCELERATOR) != 0u) {
        info->kind = M3VCPU_DEVICE_ACCELERATOR;
        info->score = 500;
    } else if ((type & M3CL_DEVICE_TYPE_CPU) != 0u) {
        info->kind = M3VCPU_DEVICE_CPU;
        info->score = 10;
        info->flags |= M3VCPU_DEVICE_FLAG_UNIFIED;
    } else {
        info->kind = M3VCPU_DEVICE_OTHER;
        info->score = 5;
    }
    if (m3_cl_u64(id, M3CL_DEVICE_DOUBLE_FP_CONFIG, 8) != 0u) {
        info->flags |= M3VCPU_DEVICE_FLAG_FP64;
    }
    info->compute_units = (uint32_t)m3_cl_u64(id, M3CL_DEVICE_MAX_COMPUTE_UNITS, 4);
    info->max_local = (uint32_t)m3_cl_size(id, M3CL_DEVICE_MAX_WORK_GROUP_SIZE);
    info->global_mem_bytes = m3_cl_u64(id, M3CL_DEVICE_GLOBAL_MEM_SIZE, 8);
    info->max_alloc_bytes = m3_cl_u64(id, M3CL_DEVICE_MAX_MEM_ALLOC_SIZE, 8);
    m3_cl_string(id, M3CL_DEVICE_NAME, info->name, sizeof info->name);
    m3_cl_string(id, M3CL_DEVICE_VENDOR, info->vendor, sizeof info->vendor);
    m3_cl_string(id, M3CL_DRIVER_VERSION, driver, sizeof driver);
    m3_cl_string(id, M3CL_DEVICE_OPENCL_C_VERSION, cver, sizeof cver);
    memset(pname, 0, sizeof pname);
    if (m3_cl.api.clGetPlatformInfo(platform, M3CL_PLATFORM_NAME, sizeof pname - 1u, pname, NULL) != M3CL_SUCCESS) {
        pname[0] = '\0';
    }
    m3_format(info->version, sizeof info->version, "driver %s | platform %s", driver, pname);
    m3_strlcpy(info->isa, cver, sizeof info->isa);
    d->c_version = m3_cl_c_version(cver);
}

static void m3_cl_init_locked(void)
{
#if defined(_WIN32)
    static const char *const names[] = {"OpenCL.dll"};
#elif defined(__APPLE__)
    static const char *const names[] = {"/System/Library/Frameworks/OpenCL.framework/OpenCL"};
#else
    static const char *const names[] = {"libOpenCL.so.1", "libOpenCL.so"};
#endif
    m3cl_platform platforms[M3_CL_MAX_PLATFORMS];
    m3cl_uint np = 0, p;
    m3cl_int rc;
    if (m3_cl.ready) {
        return;
    }
    m3_cl.ready = 1;
    m3_cl.status = M3VCPU_E_UNAVAILABLE;
    m3_cl.lib = m3_lib_open_system(names, sizeof names / sizeof names[0]);
    if (m3_cl.lib == NULL) {
        m3_format(m3_cl.reason, sizeof m3_cl.reason, "OpenCL runtime %s not found in the system directory", names[0]);
        return;
    }
#define M3CL_RESOLVE(ret, name, params, required)                                                     \
    m3_cl.api.name = (ret(M3CL_CALL *) params)m3_lib_sym(m3_cl.lib, #name);                           \
    if (m3_cl.api.name == NULL && (required)) {                                                       \
        m3_format(m3_cl.reason, sizeof m3_cl.reason, "OpenCL runtime lacks %s", #name);              \
        return;                                                                                       \
    }
    M3CL_FUNCS(M3CL_RESOLVE)
#undef M3CL_RESOLVE
    rc = m3_cl.api.clGetPlatformIDs(M3_CL_MAX_PLATFORMS, platforms, &np);
    if (rc != M3CL_SUCCESS || np == 0u) {
        m3_format(m3_cl.reason, sizeof m3_cl.reason, "no OpenCL platform (%s %d)", m3_cl_code(rc), (int)rc);
        return;
    }
    if (np > M3_CL_MAX_PLATFORMS) {
        np = M3_CL_MAX_PLATFORMS;
    }
    for (p = 0; p < np; ++p) {
        m3cl_device ids[16];
        m3cl_uint nd = 0, i;
        if (m3_cl.api.clGetDeviceIDs(platforms[p], M3CL_DEVICE_TYPE_ALL, 16u, ids, &nd) != M3CL_SUCCESS) {
            continue;
        }
        for (i = 0; i < nd && i < 16u; ++i) {
            m3_cl_add_device(platforms[p], ids[i]);
        }
    }
    if (m3_cl.count == 0) {
        m3_strlcpy(m3_cl.reason, "OpenCL platforms present but no available device with a compiler",
                   sizeof m3_cl.reason);
        return;
    }
    m3_cl.status = M3VCPU_OK;
}

static int32_t m3_cl_ensure(void)
{
    int32_t status;
    m3_mutex_lock(&m3_cl.lock);
    m3_cl_init_locked();
    status = m3_cl.status;
    m3_mutex_unlock(&m3_cl.lock);
    if (status != M3VCPU_OK) {
        m3_strlcpy(m3_cl_error, m3_cl.reason, sizeof m3_cl_error);
    }
    return status;
}

/* ---- probe / devices ---------------------------------------------------------------------- */
static int32_t m3_cl_probe(m3vcpu_probe *out)
{
    int32_t i;
    size_t used;
    if (out == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    if (m3_cl_ensure() != M3VCPU_OK) {
        m3_strlcpy(out->reason, m3_cl.reason, sizeof out->reason);
        return M3VCPU_OK;
    }
    out->available = 1;
    out->device_count = m3_cl.count;
    m3_format(out->reason, sizeof out->reason, "ok: %d device(s):", (int)m3_cl.count);
    for (i = 0; i < m3_cl.count; ++i) {
        if (m3_cl.devices[i].info.score > out->score) {
            out->score = m3_cl.devices[i].info.score;
        }
        used = strlen(out->reason);
        m3_format(out->reason + used, sizeof out->reason - used, " [%d] %s;", (int)i, m3_cl.devices[i].info.name);
    }
    return M3VCPU_OK;
}

static int32_t m3_cl_device_count(void) { return m3_cl_ensure() == M3VCPU_OK ? m3_cl.count : 0; }

static int32_t m3_cl_device_info(int32_t index, m3vcpu_device_info *out)
{
    if (out == NULL) {
        return m3_cl_fail(NULL, M3VCPU_E_INVALID_ARG, "device_info: null out", 0);
    }
    if (m3_cl_ensure() != M3VCPU_OK) {
        return M3VCPU_E_UNAVAILABLE;
    }
    if (index < 0 || index >= m3_cl.count) {
        return m3_cl_fail(NULL, M3VCPU_E_NO_DEVICE, "device_info: index out of range", 0);
    }
    *out = m3_cl.devices[index].info;
    return M3VCPU_OK;
}

/* ---- context ------------------------------------------------------------------------------ */
static void m3_cl_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf);
static void m3_cl_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel);

static int32_t m3_cl_ctx_create(int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_ctx **out)
{
    m3vcpu_ctx *ctx;
    m3cl_int rc = 0;
    (void)opts;
    if (out == NULL) {
        return m3_cl_fail(NULL, M3VCPU_E_INVALID_ARG, "ctx_create: null out", 0);
    }
    *out = NULL;
    if (m3_cl_ensure() != M3VCPU_OK) {
        return M3VCPU_E_UNAVAILABLE;
    }
    if (device < 0 || device >= m3_cl.count) {
        return m3_cl_fail(NULL, M3VCPU_E_NO_DEVICE, "ctx_create: device index out of range", 0);
    }
    ctx = (m3vcpu_ctx *)calloc(1, sizeof *ctx);
    if (ctx == NULL) {
        return m3_cl_fail(NULL, M3VCPU_E_OUT_OF_MEMORY, "ctx_create: allocation", 0);
    }
    ctx->magic = M3_CL_CTX_MAGIC;
    ctx->device = device;
    m3_cl_link_init(&ctx->bufs);
    m3_cl_link_init(&ctx->kernels);
    ctx->context = m3_cl.api.clCreateContext(NULL, 1u, &m3_cl.devices[device].id, NULL, NULL, &rc);
    if (ctx->context == NULL || rc != M3CL_SUCCESS) {
        free(ctx);
        return m3_cl_fail(NULL, M3VCPU_E_DRIVER, "clCreateContext", rc);
    }
    ctx->queue = m3_cl.api.clCreateCommandQueue(ctx->context, m3_cl.devices[device].id, 0u, &rc);
    if (ctx->queue == NULL || rc != M3CL_SUCCESS) {
        (void)m3_cl.api.clReleaseContext(ctx->context);
        free(ctx);
        return m3_cl_fail(NULL, M3VCPU_E_DRIVER, "clCreateCommandQueue", rc);
    }
    *out = ctx;
    return M3VCPU_OK;
}

static void m3_cl_ctx_destroy(m3vcpu_ctx *ctx)
{
    if (ctx == NULL || ctx->magic != M3_CL_CTX_MAGIC) {
        return;
    }
    (void)m3_cl.api.clFinish(ctx->queue);
    while (ctx->kernels.next != &ctx->kernels) {
        m3_cl_kernel_free(ctx, (m3vcpu_kernel *)(void *)ctx->kernels.next);
    }
    while (ctx->bufs.next != &ctx->bufs) {
        m3_cl_buf_free(ctx, (m3vcpu_buf *)(void *)ctx->bufs.next);
    }
    (void)m3_cl.api.clReleaseCommandQueue(ctx->queue);
    (void)m3_cl.api.clReleaseContext(ctx->context);
    ctx->magic = 0;
    free(ctx);
}

/* ---- buffers ------------------------------------------------------------------------------ */
static int m3_cl_owns(const m3vcpu_ctx *ctx, const m3vcpu_buf *buf)
{
    return ctx != NULL && buf != NULL && buf->magic == M3_CL_BUF_MAGIC && buf->owner == ctx;
}

static int32_t m3_cl_zero(m3vcpu_ctx *ctx, m3vcpu_buf *buf)
{
    static const uint8_t zero = 0;
    m3cl_int rc;
    if (m3_cl.api.clEnqueueFillBuffer != NULL) {
        rc = m3_cl.api.clEnqueueFillBuffer(ctx->queue, buf->mem, &zero, 1u, 0u, buf->size, 0u, NULL, NULL);
        return rc == M3CL_SUCCESS ? M3VCPU_OK : m3_cl_fail(ctx, M3VCPU_E_DRIVER, "clEnqueueFillBuffer", rc);
    } else {
        size_t chunk = buf->size < (1u << 20) ? buf->size : (size_t)(1u << 20);
        size_t off;
        uint8_t *zeros = (uint8_t *)calloc(1, chunk);
        if (zeros == NULL) {
            return m3_cl_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "buf_alloc: zero staging", 0);
        }
        for (off = 0; off < buf->size; off += chunk) {
            size_t n = buf->size - off < chunk ? buf->size - off : chunk;
            rc = m3_cl.api.clEnqueueWriteBuffer(ctx->queue, buf->mem, M3CL_TRUE, off, n, zeros, 0u, NULL, NULL);
            if (rc != M3CL_SUCCESS) {
                free(zeros);
                return m3_cl_fail(ctx, M3VCPU_E_DRIVER, "clEnqueueWriteBuffer(zero)", rc);
            }
        }
        free(zeros);
        return M3VCPU_OK;
    }
}

static int32_t m3_cl_buf_alloc(m3vcpu_ctx *ctx, size_t bytes, uint32_t flags, m3vcpu_buf **out)
{
    m3vcpu_buf *buf;
    m3cl_int rc = 0;
    m3cl_ulong mode = flags == M3VCPU_BUF_READ_ONLY    ? M3CL_MEM_READ_ONLY
                      : flags == M3VCPU_BUF_WRITE_ONLY ? M3CL_MEM_WRITE_ONLY
                                                       : M3CL_MEM_READ_WRITE;
    int32_t status;
    if (ctx == NULL || ctx->magic != M3_CL_CTX_MAGIC || out == NULL || bytes == 0u) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "buf_alloc: null argument or zero size", 0);
    }
    *out = NULL;
    if ((uint64_t)bytes > m3_cl.devices[ctx->device].info.max_alloc_bytes) {
        m3_format(ctx->err, sizeof ctx->err,
                  "buf_alloc: %llu bytes exceed CL_DEVICE_MAX_MEM_ALLOC_SIZE %llu; shard the batch",
                  (unsigned long long)bytes, (unsigned long long)m3_cl.devices[ctx->device].info.max_alloc_bytes);
        return M3VCPU_E_OUT_OF_RANGE;
    }
    buf = (m3vcpu_buf *)calloc(1, sizeof *buf);
    if (buf == NULL) {
        return m3_cl_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "buf_alloc: descriptor", 0);
    }
    buf->mem = m3_cl.api.clCreateBuffer(ctx->context, mode, bytes, NULL, &rc);
    if (buf->mem == NULL || rc != M3CL_SUCCESS) {
        free(buf);
        return m3_cl_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "clCreateBuffer", rc);
    }
    buf->magic = M3_CL_BUF_MAGIC;
    buf->owner = ctx;
    buf->size = bytes;
    status = m3_cl_zero(ctx, buf);
    if (status != M3VCPU_OK) {
        (void)m3_cl.api.clReleaseMemObject(buf->mem);
        free(buf);
        return status;
    }
    m3_cl_link_push(&ctx->bufs, &buf->link);
    *out = buf;
    return M3VCPU_OK;
}

static int32_t m3_cl_buf_check(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, size_t offset, size_t bytes, const void *host)
{
    if (!m3_cl_owns(ctx, buf) || (host == NULL && bytes != 0u)) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "buffer: foreign buffer or null host pointer", 0);
    }
    if (offset > buf->size || bytes > buf->size - offset) {
        return m3_cl_fail(ctx, M3VCPU_E_OUT_OF_RANGE, "buffer: range outside the buffer", 0);
    }
    return M3VCPU_OK;
}

static int32_t m3_cl_buf_write(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, const void *src, size_t bytes)
{
    int32_t status = m3_cl_buf_check(ctx, buf, offset, bytes, src);
    m3cl_int rc;
    if (status != M3VCPU_OK || bytes == 0u) {
        return status;
    }
    rc = m3_cl.api.clEnqueueWriteBuffer(ctx->queue, buf->mem, M3CL_TRUE, offset, bytes, src, 0u, NULL, NULL);
    return rc == M3CL_SUCCESS ? M3VCPU_OK : m3_cl_fail(ctx, M3VCPU_E_DRIVER, "clEnqueueWriteBuffer", rc);
}

static int32_t m3_cl_buf_read(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, void *dst, size_t bytes)
{
    int32_t status = m3_cl_buf_check(ctx, buf, offset, bytes, dst);
    m3cl_int rc;
    if (status != M3VCPU_OK || bytes == 0u) {
        return status;
    }
    rc = m3_cl.api.clEnqueueReadBuffer(ctx->queue, buf->mem, M3CL_TRUE, offset, bytes, dst, 0u, NULL, NULL);
    return rc == M3CL_SUCCESS ? M3VCPU_OK : m3_cl_fail(ctx, M3VCPU_E_DRIVER, "clEnqueueReadBuffer", rc);
}

static void m3_cl_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf)
{
    if (!m3_cl_owns(ctx, buf)) {
        return;
    }
    m3_cl_link_remove(&buf->link);
    (void)m3_cl.api.clReleaseMemObject(buf->mem);
    buf->magic = 0;
    free(buf);
}

/* ---- kernels ------------------------------------------------------------------------------ */
static int32_t m3_cl_build_failure(m3vcpu_ctx *ctx, m3cl_program program, m3cl_int rc)
{
    char log[768];
    size_t used;
    memset(log, 0, sizeof log);
    (void)m3_cl.api.clGetProgramBuildInfo(program, m3_cl.devices[ctx->device].id, M3CL_PROGRAM_BUILD_LOG,
                                          sizeof log - 1u, log, NULL);
    log[sizeof log - 1u] = '\0';
    m3_cl_fail(ctx, M3VCPU_E_KERNEL_BUILD, "clBuildProgram", rc);
    used = strlen(ctx->err);
    m3_format(ctx->err + used, sizeof ctx->err - used, ": %s", log);
    return M3VCPU_E_KERNEL_BUILD;
}

static int32_t m3_cl_kernel_load(m3vcpu_ctx *ctx, const m3k_blob *blobs, size_t count, const char *entry,
                                 m3vcpu_kernel **out)
{
    const m3k_blob *src = NULL;
    m3vcpu_kernel *k;
    m3cl_int rc = 0;
    const char *text;
    size_t length, i;
    if (ctx == NULL || ctx->magic != M3_CL_CTX_MAGIC || out == NULL || entry == NULL || (blobs == NULL && count != 0u)) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "kernel_load: null argument", 0);
    }
    *out = NULL;
    for (i = 0; i < count && src == NULL; ++i) {
        if (blobs[i].format == M3K_OPENCL_C && blobs[i].min_isa <= m3_cl.devices[ctx->device].c_version) {
            src = &blobs[i];
        }
    }
    if (src == NULL) {
        return m3_cl_fail(ctx, M3VCPU_E_NO_KERNEL_FORMAT,
                          "kernel_load: no M3K_OPENCL_C blob within the device's OpenCL C version", 0);
    }
    k = (m3vcpu_kernel *)calloc(1, sizeof *k);
    if (k == NULL) {
        return m3_cl_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "kernel_load: descriptor", 0);
    }
    text = (const char *)src->bytes;
    length = src->size;
    k->program = m3_cl.api.clCreateProgramWithSource(ctx->context, 1u, &text, &length, &rc);
    if (k->program == NULL || rc != M3CL_SUCCESS) {
        free(k);
        return m3_cl_fail(ctx, M3VCPU_E_KERNEL_BUILD, "clCreateProgramWithSource", rc);
    }
    rc = m3_cl.api.clBuildProgram(k->program, 1u, &m3_cl.devices[ctx->device].id, "-cl-std=CL1.2", NULL, NULL);
    if (rc != M3CL_SUCCESS) {
        int32_t status = m3_cl_build_failure(ctx, k->program, rc);
        (void)m3_cl.api.clReleaseProgram(k->program);
        free(k);
        return status;
    }
    k->kernel = m3_cl.api.clCreateKernel(k->program, entry, &rc);
    if (k->kernel == NULL || rc != M3CL_SUCCESS) {
        (void)m3_cl.api.clReleaseProgram(k->program);
        free(k);
        return m3_cl_fail(ctx, M3VCPU_E_KERNEL_BUILD, "clCreateKernel", rc);
    }
    if (m3_cl.api.clGetKernelInfo(k->kernel, M3CL_KERNEL_NUM_ARGS, sizeof k->nargs, &k->nargs, NULL) != M3CL_SUCCESS
        || m3_cl.api.clGetKernelWorkGroupInfo(k->kernel, m3_cl.devices[ctx->device].id, M3CL_KERNEL_WORK_GROUP_SIZE,
                                              sizeof k->max_local, &k->max_local, NULL)
               != M3CL_SUCCESS) {
        k->max_local = m3_cl.devices[ctx->device].info.max_local;
    }
    if (k->max_local == 0u) {
        k->max_local = 1u;
    }
    k->magic = M3_CL_KER_MAGIC;
    k->owner = ctx;
    m3_cl_link_push(&ctx->kernels, &k->link);
    *out = k;
    return M3VCPU_OK;
}

static void m3_cl_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *k)
{
    if (ctx == NULL || k == NULL || k->magic != M3_CL_KER_MAGIC || k->owner != ctx) {
        return;
    }
    m3_cl_link_remove(&k->link);
    (void)m3_cl.api.clReleaseKernel(k->kernel);
    (void)m3_cl.api.clReleaseProgram(k->program);
    k->magic = 0;
    free(k);
}

static int32_t m3_cl_launch_1d(m3vcpu_ctx *ctx, m3vcpu_kernel *k, uint64_t global, uint32_t local,
                               const m3vcpu_arg *args, uint32_t count)
{
    size_t g, l;
    uint32_t i;
    m3cl_int rc;
    if (ctx == NULL || ctx->magic != M3_CL_CTX_MAGIC || k == NULL || k->magic != M3_CL_KER_MAGIC || k->owner != ctx) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: foreign kernel", 0);
    }
    if (count != k->nargs || (count > 0u && args == NULL)) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: argument count does not match the kernel", 0);
    }
    l = local == 0u ? (k->max_local < M3_CL_DEFAULT_LOCAL ? k->max_local : M3_CL_DEFAULT_LOCAL) : (size_t)local;
    if (l > k->max_local) {
        return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: local size above CL_KERNEL_WORK_GROUP_SIZE", 0);
    }
    for (i = 0; i < count; ++i) {
        if (args[i].kind == M3VCPU_ARG_BUFFER) {
            if (!m3_cl_owns(ctx, args[i].buf)) {
                return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: foreign buffer argument", 0);
            }
            rc = m3_cl.api.clSetKernelArg(k->kernel, i, sizeof(m3cl_mem), &args[i].buf->mem);
        } else {
            rc = m3_cl.api.clSetKernelArg(k->kernel, i, sizeof(int32_t), &args[i].i32);
        }
        if (rc != M3CL_SUCCESS) {
            return m3_cl_fail(ctx, M3VCPU_E_INVALID_ARG, "clSetKernelArg", rc);
        }
    }
    g = (size_t)((global + l - 1u) / l * l);
    rc = m3_cl.api.clEnqueueNDRangeKernel(ctx->queue, k->kernel, 1u, NULL, &g, &l, 0u, NULL, NULL);
    return rc == M3CL_SUCCESS ? M3VCPU_OK : m3_cl_fail(ctx, M3VCPU_E_LAUNCH, "clEnqueueNDRangeKernel", rc);
}

static int32_t m3_cl_finish(m3vcpu_ctx *ctx)
{
    m3cl_int rc;
    if (ctx == NULL || ctx->magic != M3_CL_CTX_MAGIC) {
        return M3VCPU_E_INVALID_ARG;
    }
    rc = m3_cl.api.clFinish(ctx->queue);
    return rc == M3CL_SUCCESS ? M3VCPU_OK : m3_cl_fail(ctx, M3VCPU_E_DRIVER, "clFinish", rc);
}

static const char *m3_cl_last_error(const m3vcpu_ctx *ctx) { return ctx == NULL ? m3_cl_error : ctx->err; }

static const m3vcpu_backend_v1 m3_cl_table = {
    .abi = M3VCPU_ABI_V1,
    .struct_size = (uint32_t)sizeof(m3vcpu_backend_v1),
    .name = "opencl",
    .probe = m3_cl_probe,
    .device_count = m3_cl_device_count,
    .device_info = m3_cl_device_info,
    .ctx_create = m3_cl_ctx_create,
    .ctx_destroy = m3_cl_ctx_destroy,
    .buf_alloc = m3_cl_buf_alloc,
    .buf_write = m3_cl_buf_write,
    .buf_read = m3_cl_buf_read,
    .buf_free = m3_cl_buf_free,
    .kernel_load = m3_cl_kernel_load,
    .kernel_free = m3_cl_kernel_free,
    .launch_1d = m3_cl_launch_1d,
    .finish = m3_cl_finish,
    .last_error = m3_cl_last_error,
};

M3VCPU_DLLEXPORT const m3vcpu_backend_v1 *m3vcpu_backend_entry_v1(void);
M3VCPU_DLLEXPORT const m3vcpu_backend_v1 *m3vcpu_backend_entry_v1(void) { return &m3_cl_table; }
