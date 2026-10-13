/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu.h - M3 vCPU hardware-abstraction layer: public C ABI (C99).
 *
 * Vendor-neutral by rule: no CUDA, OpenCL, Vulkan, Level Zero or HIP type appears here.
 * Two surfaces:
 *   1. m3vcpu_backend_v1   the versioned function table that every backend exports through
 *                          m3vcpu_backend_entry_v1() (one shared library per backend; the CPU
 *                          backend is built into the core).
 *   2. m3vcpu_* functions  the HAL core exported by the m3vcpu shared library: registry,
 *                          probe, scoring, selection (M3VCPU_BACKEND override), sessions.
 *
 * Design: m3-parallel-vcpu-design.md sections 3.2/3.3; GPU_BACKEND_SURVEY.md sections 3.1-3.3.
 *
 * Conventions
 *   - Every int32_t result is an M3VCPU_OK / M3VCPU_E_* status.
 *   - Input structs carry struct_size set by the caller; callees read only fields inside it.
 *   - Output structs are zero-filled by the callee, which then sets struct_size.
 *   - Members of m3vcpu_backend_v1 are append-only; abi changes only on incompatible change.
 *   - A context (and a session) is externally synchronized: one thread at a time.
 *   - Buffers are zero-filled on allocation. Offsets and sizes are in bytes.
 */
#ifndef M3VCPU_H
#define M3VCPU_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define M3VCPU_ABI_V1 1u

#if defined(_WIN32)
#define M3VCPU_DLLEXPORT __declspec(dllexport)
#define M3VCPU_DLLIMPORT __declspec(dllimport)
#else
#define M3VCPU_DLLEXPORT __attribute__((visibility("default")))
#define M3VCPU_DLLIMPORT
#endif

#if defined(M3VCPU_STATIC)
#define M3VCPU_API
#elif defined(M3VCPU_BUILDING_CORE)
#define M3VCPU_API M3VCPU_DLLEXPORT
#else
#define M3VCPU_API M3VCPU_DLLIMPORT
#endif

/* ---- status codes -------------------------------------------------------------------- */
#define M3VCPU_OK                   0
#define M3VCPU_E_INVALID_ARG      (-1)  /* null pointer, bad size, bad flag, foreign handle  */
#define M3VCPU_E_UNAVAILABLE      (-2)  /* backend / driver / ISA not present on this host   */
#define M3VCPU_E_NO_DEVICE        (-3)  /* device index out of range or no device matches    */
#define M3VCPU_E_OUT_OF_MEMORY    (-4)
#define M3VCPU_E_OUT_OF_RANGE     (-5)  /* buffer bounds or kernel geometry exceed storage   */
#define M3VCPU_E_NO_KERNEL_FORMAT (-6)  /* no blob in a format this backend accepts          */
#define M3VCPU_E_KERNEL_BUILD     (-7)  /* runtime compile / module load failed              */
#define M3VCPU_E_LAUNCH           (-8)
#define M3VCPU_E_DRIVER           (-9)  /* any other vendor-runtime failure                  */
#define M3VCPU_E_ABI              (-10) /* backend table rejected (abi / struct_size / null) */
#define M3VCPU_E_NOT_FOUND        (-11) /* no backend registered under that name             */
#define M3VCPU_E_INTEGRITY        (-12) /* kernel blob bytes do not match their sha256       */
#define M3VCPU_E_INTERNAL         (-13)

/* ---- kernel blobs (content-addressed; one per format) -------------------------------- */
#define M3K_PTX          1u /* min_isa = PTX ISA x10 (70 = 7.0)                              */
#define M3K_OPENCL_C     2u /* min_isa = OpenCL C version x100 (120 = 1.2); bytes = source   */
#define M3K_SPIRV_VK     3u
#define M3K_SPIRV_CL     4u
#define M3K_HSACO_FATBIN 5u
#define M3K_CPU_SYMBOL   6u /* bytes = UTF-8 name of a kernel compiled into the CPU backend  */
/* Reserved (owner decision 2026-10-12): WGSL source for a future "webgpu" backend that loads
 * webgpu_dawn / wgpu_native at run time through the standard webgpu.h C ABI. Value fixed now
 * so that backend fits m3vcpu_backend_v1 without an ABI bump; no backend implements it yet. */
#define M3K_WGSL         7u

typedef struct m3k_blob {
    uint32_t format;        /* M3K_*                                                  */
    uint32_t min_isa;       /* format-specific floor, 0 = none                        */
    const uint8_t *bytes;
    size_t size;
    uint8_t sha256[32];     /* content address of bytes[0, size); checked by the core */
} m3k_blob;

/* ---- probe / device / context -------------------------------------------------------- */
typedef struct m3vcpu_probe {
    uint32_t struct_size;
    int32_t available;      /* 1 usable, 0 not                                        */
    int32_t score;          /* higher preferred; max over devices                     */
    int32_t device_count;
    char reason[192];       /* "ok: ..." or why unavailable; NUL-terminated           */
} m3vcpu_probe;

#define M3VCPU_DEVICE_CPU           1u
#define M3VCPU_DEVICE_GPU_DISCRETE  2u
#define M3VCPU_DEVICE_GPU_INTEGRATED 3u
#define M3VCPU_DEVICE_ACCELERATOR   4u
#define M3VCPU_DEVICE_OTHER         5u

#define M3VCPU_DEVICE_FLAG_FP64     0x1u
#define M3VCPU_DEVICE_FLAG_UNIFIED  0x2u /* device shares host memory */

typedef struct m3vcpu_device_info {
    uint32_t struct_size;
    uint32_t kind;          /* M3VCPU_DEVICE_*                                        */
    uint32_t compute_units;
    uint32_t max_local;     /* largest accepted local size                            */
    uint64_t global_mem_bytes;
    uint64_t max_alloc_bytes;
    uint32_t flags;         /* M3VCPU_DEVICE_FLAG_*                                   */
    int32_t score;
    char name[128];
    char vendor[64];
    char version[96];       /* driver / runtime version                               */
    char isa[32];           /* cpu: "scalar" | "avx2" | "avx512"; opencl: "OpenCL C x.y" */
} m3vcpu_device_info;

#define M3VCPU_ISA_AUTO   0u
#define M3VCPU_ISA_SCALAR 1u
#define M3VCPU_ISA_AVX2   2u /* x86-64-v3: AVX2 + FMA + BMI1/2 + LZCNT + MOVBE + F16C */
#define M3VCPU_ISA_AVX512 3u /* x86-64-v4: v3 + AVX-512 F/BW/CD/DQ/VL                  */

typedef struct m3vcpu_ctx_opts {
    uint32_t struct_size;
    uint32_t threads;       /* cpu: threads incl. caller; 0 = all logical processors  */
    uint32_t isa;           /* cpu: M3VCPU_ISA_*; 0 = best supported                  */
    uint32_t flags;         /* reserved, 0                                            */
} m3vcpu_ctx_opts;

#define M3VCPU_BUF_READ_WRITE 0u
#define M3VCPU_BUF_READ_ONLY  1u /* kernel-side access hint */
#define M3VCPU_BUF_WRITE_ONLY 2u

typedef struct m3vcpu_ctx m3vcpu_ctx;
typedef struct m3vcpu_buf m3vcpu_buf;
typedef struct m3vcpu_kernel m3vcpu_kernel;

#define M3VCPU_ARG_BUFFER 1u
#define M3VCPU_ARG_I32    2u
#define M3VCPU_MAX_ARGS   16u

typedef struct m3vcpu_arg {
    uint32_t kind;          /* M3VCPU_ARG_*                                           */
    int32_t i32;
    m3vcpu_buf *buf;
} m3vcpu_arg;

/* ---- backend function table, v1 ------------------------------------------------------ */
typedef struct m3vcpu_backend_v1 {
    uint32_t abi;           /* M3VCPU_ABI_V1                                          */
    uint32_t struct_size;   /* sizeof(m3vcpu_backend_v1) as compiled by the backend   */
    const char *name;       /* "cpu" | "opencl" | "cuda" | "vulkan" | "ze" | "hip" | "webgpu" */
    /* Never crashes; a missing driver is available = 0 plus a reason. */
    int32_t (*probe)(m3vcpu_probe *out);
    int32_t (*device_count)(void);
    int32_t (*device_info)(int32_t index, m3vcpu_device_info *out);
    int32_t (*ctx_create)(int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_ctx **out);
    /* Releases every buffer and kernel still owned by the context. */
    void (*ctx_destroy)(m3vcpu_ctx *ctx);
    int32_t (*buf_alloc)(m3vcpu_ctx *ctx, size_t bytes, uint32_t flags, m3vcpu_buf **out);
    int32_t (*buf_write)(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, const void *src, size_t bytes);
    int32_t (*buf_read)(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, void *dst, size_t bytes);
    void (*buf_free)(m3vcpu_ctx *ctx, m3vcpu_buf *buf);
    /* Picks the first blob whose format the backend accepts; blobs are pre-verified by the core. */
    int32_t (*kernel_load)(m3vcpu_ctx *ctx, const m3k_blob *blobs, size_t count, const char *entry,
                           m3vcpu_kernel **out);
    void (*kernel_free)(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel);
    /* global work-items (rounded up to local); local 0 = backend choice. May return before
     * completion; finish() waits. Kernels guard index >= n themselves. */
    int32_t (*launch_1d)(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel, uint64_t global, uint32_t local,
                         const m3vcpu_arg *args, uint32_t count);
    int32_t (*finish)(m3vcpu_ctx *ctx);
    /* ctx == NULL returns the backend's last probe / ctx_create error. Never NULL. */
    const char *(*last_error)(const m3vcpu_ctx *ctx);
} m3vcpu_backend_v1;

typedef const m3vcpu_backend_v1 *(*m3vcpu_backend_entry_v1_fn)(void);
#define M3VCPU_BACKEND_ENTRY_V1_SYMBOL "m3vcpu_backend_entry_v1"

/* ---- HAL core: registry and selection ------------------------------------------------- */
/*
 * Registry: the built-in CPU backend, then every shared library next to the core (and in each
 * directory of M3VCPU_BACKEND_PATH) named m3vcpu_<name>.dll / libm3vcpu_<name>.so that exports
 * m3vcpu_backend_entry_v1. Probes are lazy, cached and time-bounded (M3VCPU_PROBE_TIMEOUT_MS,
 * default 15000).
 *
 * Selector grammar for m3vcpu_open:
 *   NULL | "" | "auto"   environment M3VCPU_BACKEND (same list grammar) if set, else the
 *                        available backend with the highest score
 *   "gpu"                the highest-scoring GPU device of any available backend
 *   "name[:dev],..."     ordered preference list, e.g. "opencl:1,cpu"
 * device >= 0 overrides any ":dev"; -1 picks the backend's best device.
 */
M3VCPU_API uint32_t m3vcpu_abi_version(void);
M3VCPU_API int32_t m3vcpu_backend_count(void);
M3VCPU_API const m3vcpu_backend_v1 *m3vcpu_backend_at(int32_t index);
M3VCPU_API int32_t m3vcpu_backend_index(const char *name); /* -1 when absent */
M3VCPU_API const char *m3vcpu_backend_origin(int32_t index); /* "builtin" or library path */
M3VCPU_API int32_t m3vcpu_backend_probe(int32_t index, m3vcpu_probe *out);
M3VCPU_API int32_t m3vcpu_register_backend(const m3vcpu_backend_v1 *backend);

/* ---- HAL core: sessions (backend + device + context) ------------------------------------ */
typedef struct m3vcpu_session m3vcpu_session;

M3VCPU_API int32_t m3vcpu_open(const char *selector, int32_t device, const m3vcpu_ctx_opts *opts,
                               m3vcpu_session **out);
M3VCPU_API void m3vcpu_close(m3vcpu_session *session);
M3VCPU_API const m3vcpu_backend_v1 *m3vcpu_session_backend(const m3vcpu_session *session);
M3VCPU_API int32_t m3vcpu_session_device(const m3vcpu_session *session, int32_t *index,
                                         m3vcpu_device_info *info);

M3VCPU_API int32_t m3vcpu_buffer_alloc(m3vcpu_session *session, size_t bytes, uint32_t flags,
                                       m3vcpu_buf **out);
M3VCPU_API int32_t m3vcpu_buffer_write(m3vcpu_session *session, m3vcpu_buf *buf, size_t offset,
                                       const void *src, size_t bytes);
M3VCPU_API int32_t m3vcpu_buffer_read(m3vcpu_session *session, m3vcpu_buf *buf, size_t offset,
                                      void *dst, size_t bytes);
M3VCPU_API void m3vcpu_buffer_free(m3vcpu_session *session, m3vcpu_buf *buf);

/* Verifies every blob's sha256, then loads. Caller frees with m3vcpu_kernel_free. */
M3VCPU_API int32_t m3vcpu_kernel_load(m3vcpu_session *session, const m3k_blob *blobs, size_t count,
                                      const char *entry, m3vcpu_kernel **out);
/* Loads a kernel from the bundle compiled into the core; cached and owned by the session. */
M3VCPU_API int32_t m3vcpu_kernel_builtin(m3vcpu_session *session, const char *entry,
                                         m3vcpu_kernel **out);
M3VCPU_API void m3vcpu_kernel_free(m3vcpu_session *session, m3vcpu_kernel *kernel);
M3VCPU_API int32_t m3vcpu_builtin_blobs(const char *entry, const m3k_blob **blobs, size_t *count);

/* Buffer arguments must belong to the session. Built-in kernels also get a geometry guard. */
M3VCPU_API int32_t m3vcpu_launch_1d(m3vcpu_session *session, m3vcpu_kernel *kernel, uint64_t global,
                                    uint32_t local, const m3vcpu_arg *args, uint32_t count);
M3VCPU_API int32_t m3vcpu_finish(m3vcpu_session *session);

/* session == NULL: this thread's last registry / open error. Never NULL. */
M3VCPU_API const char *m3vcpu_last_error(const m3vcpu_session *session);
M3VCPU_API const char *m3vcpu_status_name(int32_t status);
M3VCPU_API void m3vcpu_sha256(const void *data, size_t size, uint8_t out[32]);

#ifdef __cplusplus
}
#endif

#endif /* M3VCPU_H */
