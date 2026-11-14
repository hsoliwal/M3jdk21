/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_webgpu_backend.c - "webgpu" backend: m3vcpu_backend_v1 over the standard webgpu.h C ABI
 * (m3-parallel-vcpu-design.md section 3.4, leaf P3w). Built against Dawn (Chrome's WebGPU engine,
 * statically linked); only webgpu.h entry points are used, so wgpu-native is an alternate provider.
 * Dawn loads D3D12 / Vulkan (and DXC, the Vulkan loader) at run time from the directory of this
 * module, so the import table holds OS and CRT DLLs only (hard rule 1).
 *
 * Devices: one entry per distinct (vendor id, device id, API) adapter returned for
 * {D3D12, Vulkan} x {high-performance, low-power} on Windows ({Metal} on Apple, {Vulkan}
 * elsewhere), in that order; M3VCPU_WEBGPU_API="d3d12,vulkan" narrows the APIs. Every context
 * gets its own WGPUDevice (instance feature MultipleDevicesPerAdapter, else a fresh adapter).
 * The probe refuses (available = 0) an in-process msvcp140.dll older than the linked Dawn needs
 * instead of crashing in it (M3VCPU_WEBGPU_MIN_MSVCP).
 * Mapping (design 3.4):
 *   buf_alloc   wgpuDeviceCreateBuffer(STORAGE | COPY_SRC | COPY_DST), size rounded up to 4;
 *               WebGPU zero-initializes; above min(maxBufferSize, maxStorageBufferBindingSize)
 *               the call fails with M3VCPU_E_OUT_OF_RANGE ("shard the batch")
 *   buf_write   wgpuQueueWriteBuffer in 64 MiB chunks; unaligned head / tail bytes are merged
 *               into their 32-bit words (read-modify-write)
 *   buf_read    copy to a MAP_READ staging buffer (64 MiB chunks) + wgpuBufferMapAsync
 *   kernel_load an M3K_WGSL blob, else the WGSL twin compiled into this backend for the entry,
 *               admitted only for the exact M3K_OPENCL_C blob it restates (m3vcpu_webgpu_kernels.h)
 *   launch_1d   one compute pass; pipeline per local size (override constant m3_local, cached);
 *               ceil(global / local) workgroups, as a 2-D grid above maxComputeWorkgroupsPerDimension
 *   finish      wgpuQueueOnSubmittedWorkDone, waited with wgpuInstanceWaitAny (TimedWaitAny) or
 *               wgpuInstanceProcessEvents polling when the instance lacks that feature
 * Kernel convention (M3 WGSL): launch argument i = @group(0) @binding(i) storage buffer; scalar
 * (M3VCPU_ARG_I32) arguments are rejected; index = (workgroup_id.y * num_workgroups.x +
 * workgroup_id.x) * m3_local + local_invocation_index; kernels guard their own item count.
 * Callbacks: futures use AllowProcessEvents (they run inside our waits); device loss uses
 * AllowSpontaneous and touches the context only for a real loss (not Destroyed / cancelled).
 */
#include "m3vcpu.h"
#include "m3vcpu_sys.h"
#include "m3vcpu_webgpu_kernels.h"

#include <webgpu/webgpu.h>

#include <stdlib.h>
#include <string.h>

#if defined(_WIN32)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#endif

/* Lowest msvcp140.dll (major * 100 + minor) the linked Dawn build runs with; 0 = no check.
 * MSVC 14.40 changed std::mutex (constexpr constructor): code built with >= 14.40 crashes with
 * an access violation on an older msvcp140.dll already resident in the process (for example the
 * 14.36 copy that the JDK 21 launcher preloads). Set from CMake (M3VCPU_WEBGPU_MIN_MSVCP). */
#ifndef M3VCPU_WEBGPU_MIN_MSVCP
#define M3VCPU_WEBGPU_MIN_MSVCP 1440
#endif

#define M3_WG_MAX_DEVICES 16
#define M3_WG_CTX_MAGIC 0x4d335743u
#define M3_WG_BUF_MAGIC 0x4d335742u
#define M3_WG_KER_MAGIC 0x4d33574bu
#define M3_WG_DEFAULT_LOCAL 64u
#define M3_WG_CHUNK ((uint64_t)64u << 20)
#define M3_WG_STAGING_MIN ((uint64_t)1u << 20)
#define M3_WG_PIPELINES 8
#define M3_WG_WAIT_MS 300000u
#define M3_WG_LOCAL_KEY "m3_local"

/* ---- shared state ------------------------------------------------------------------------- */
typedef struct m3_wg_link {
    struct m3_wg_link *prev;
    struct m3_wg_link *next;
} m3_wg_link;

typedef struct m3_wg_device {
    WGPUAdapter adapter;
    WGPUBackendType api;
    WGPUPowerPreference pref;
    uint32_t vendor_id;
    uint32_t device_id;
    WGPULimits limits;
    m3vcpu_device_info info;
} m3_wg_device;

static struct {
    m3_mutex lock;
    int ready;
    int timed_wait;   /* instance feature TimedWaitAny                                  */
    int multi_device; /* instance feature MultipleDevicesPerAdapter (else: fresh adapter) */
    int32_t status;
    char reason[192];
    WGPUInstance instance;
    m3_wg_device devices[M3_WG_MAX_DEVICES];
    int32_t count;
} m3_wg = {M3_MUTEX_INITIALIZER, 0, 0, 0, 0, {0}, NULL, {{0}}, 0};

typedef struct m3_wg_slot {
    uint32_t local;
    WGPUComputePipeline pipeline;
    WGPUBindGroupLayout layout;
    WGPUBindGroup group;
    uint32_t group_count;
    uint64_t group_ids[M3VCPU_MAX_ARGS];
} m3_wg_slot;

struct m3vcpu_ctx {
    uint32_t magic;
    int32_t device;
    WGPUDevice dev;
    WGPUQueue queue;
    WGPULimits limits;
    uint64_t max_alloc;
    uint32_t max_local;
    WGPUBuffer staging;
    uint64_t staging_size;
    uint64_t next_id;
    m3_wg_link bufs;
    m3_wg_link kernels;
    volatile int lost;
    volatile int async_error;
    char async_msg[512];
    char err[1024];
};

struct m3vcpu_buf {
    m3_wg_link link;
    uint32_t magic;
    m3vcpu_ctx *owner;
    size_t size;      /* bytes requested                     */
    uint64_t padded;  /* bytes allocated (multiple of 4)     */
    uint64_t id;      /* never reused: bind-group cache key  */
    WGPUBuffer buffer;
};

struct m3vcpu_kernel {
    m3_wg_link link;
    uint32_t magic;
    m3vcpu_ctx *owner;
    WGPUShaderModule module;
    uint32_t nargs; /* 0 = not declared (generic M3K_WGSL blob) */
    uint32_t victim;
    char entry[64];
    m3_wg_slot slots[M3_WG_PIPELINES];
};

/* One waiter type for every future: the callback fills it, the waiting thread reads it. */
typedef struct m3_wg_wait {
    volatile int done;
    int ok;
    WGPUErrorType type;
    WGPUAdapter adapter;
    WGPUDevice device;
    char msg[512];
} m3_wg_wait;

static M3_THREAD_LOCAL char m3_wg_error[512];

static void m3_wg_link_init(m3_wg_link *h) { h->prev = h->next = h; }

static void m3_wg_link_push(m3_wg_link *h, m3_wg_link *n)
{
    n->next = h->next;
    n->prev = h;
    h->next->prev = n;
    h->next = n;
}

static void m3_wg_link_remove(m3_wg_link *n)
{
    n->prev->next = n->next;
    n->next->prev = n->prev;
    n->next = n->prev = n;
}

static WGPUStringView m3_wg_view(const char *s)
{
    WGPUStringView v;
    v.data = s;
    v.length = strlen(s);
    return v;
}

static void m3_wg_copy_view(char *dst, size_t cap, WGPUStringView s)
{
    size_t n = s.data == NULL ? 0u : (s.length == WGPU_STRLEN ? strlen(s.data) : s.length);
    if (n >= cap) {
        n = cap - 1u;
    }
    if (n > 0u) {
        memcpy(dst, s.data, n);
    }
    dst[n] = '\0';
}

static int32_t m3_wg_fail(m3vcpu_ctx *ctx, int32_t status, const char *what)
{
    m3_strlcpy(ctx == NULL ? m3_wg_error : ctx->err, what, ctx == NULL ? sizeof m3_wg_error : sizeof ctx->err);
    return status;
}

static const char *m3_wg_api_name(WGPUBackendType api)
{
    switch (api) {
    case WGPUBackendType_D3D12: return "D3D12";
    case WGPUBackendType_Vulkan: return "Vulkan";
    case WGPUBackendType_Metal: return "Metal";
    case WGPUBackendType_D3D11: return "D3D11";
    case WGPUBackendType_OpenGL: return "OpenGL";
    case WGPUBackendType_OpenGLES: return "OpenGLES";
    default: return "other";
    }
}

static const char *m3_wg_error_name(WGPUErrorType type)
{
    switch (type) {
    case WGPUErrorType_Validation: return "validation error";
    case WGPUErrorType_OutOfMemory: return "out of memory";
    case WGPUErrorType_Internal: return "internal error";
    default: return "unknown error";
    }
}

/* ---- callbacks and waiting ------------------------------------------------------------------ */
static void m3_wg_on_adapter(WGPURequestAdapterStatus status, WGPUAdapter adapter, WGPUStringView message, void *u1,
                             void *u2)
{
    m3_wg_wait *w = (m3_wg_wait *)u1;
    (void)u2;
    w->ok = status == WGPURequestAdapterStatus_Success && adapter != NULL;
    w->adapter = adapter;
    m3_wg_copy_view(w->msg, sizeof w->msg, message);
    w->done = 1;
}

static void m3_wg_on_device(WGPURequestDeviceStatus status, WGPUDevice device, WGPUStringView message, void *u1,
                            void *u2)
{
    m3_wg_wait *w = (m3_wg_wait *)u1;
    (void)u2;
    w->ok = status == WGPURequestDeviceStatus_Success && device != NULL;
    w->device = device;
    m3_wg_copy_view(w->msg, sizeof w->msg, message);
    w->done = 1;
}

static void m3_wg_on_map(WGPUMapAsyncStatus status, WGPUStringView message, void *u1, void *u2)
{
    m3_wg_wait *w = (m3_wg_wait *)u1;
    (void)u2;
    w->ok = status == WGPUMapAsyncStatus_Success;
    m3_wg_copy_view(w->msg, sizeof w->msg, message);
    w->done = 1;
}

static void m3_wg_on_done(WGPUQueueWorkDoneStatus status, WGPUStringView message, void *u1, void *u2)
{
    m3_wg_wait *w = (m3_wg_wait *)u1;
    (void)u2;
    w->ok = status == WGPUQueueWorkDoneStatus_Success;
    m3_wg_copy_view(w->msg, sizeof w->msg, message);
    w->done = 1;
}

static void m3_wg_on_pop(WGPUPopErrorScopeStatus status, WGPUErrorType type, WGPUStringView message, void *u1,
                         void *u2)
{
    m3_wg_wait *w = (m3_wg_wait *)u1;
    (void)u2;
    w->ok = status == WGPUPopErrorScopeStatus_Success;
    w->type = w->ok ? type : WGPUErrorType_Unknown;
    m3_wg_copy_view(w->msg, sizeof w->msg, message);
    w->done = 1;
}

static void m3_wg_on_lost(WGPUDevice const *device, WGPUDeviceLostReason reason, WGPUStringView message, void *u1,
                          void *u2)
{
    m3vcpu_ctx *ctx = (m3vcpu_ctx *)u1;
    (void)device;
    (void)u2;
    if (ctx == NULL || reason == WGPUDeviceLostReason_Destroyed || reason == WGPUDeviceLostReason_CallbackCancelled
        || reason == WGPUDeviceLostReason_FailedCreation) {
        return; /* our own destroy / creation failure: the context may already be gone */
    }
    m3_wg_copy_view(ctx->async_msg, sizeof ctx->async_msg, message);
    ctx->lost = 1;
}

static void m3_wg_on_error(WGPUDevice const *device, WGPUErrorType type, WGPUStringView message, void *u1, void *u2)
{
    m3vcpu_ctx *ctx = (m3vcpu_ctx *)u1;
    char text[480];
    (void)device;
    (void)u2;
    if (ctx == NULL || ctx->async_error) {
        return; /* keep the first error */
    }
    m3_wg_copy_view(text, sizeof text, message);
    m3_format(ctx->async_msg, sizeof ctx->async_msg, "%s: %s", m3_wg_error_name(type), text);
    ctx->async_error = 1;
}

static void m3_wg_nap(void)
{
    m3_mutex m;
    m3_cond c;
    m3_mutex_init(&m);
    m3_cond_init(&c);
    m3_mutex_lock(&m);
    (void)m3_cond_timedwait(&c, &m, 1u);
    m3_mutex_unlock(&m);
    m3_cond_destroy(&c);
    m3_mutex_destroy(&m);
}

/* Blocks until w->done (set by the future's callback). Returns 0, or -1 on timeout. */
static int m3_wg_wait_for(WGPUFuture future, m3_wg_wait *w)
{
    uint64_t deadline = m3_now_ms() + M3_WG_WAIT_MS;
    if (m3_wg.timed_wait && !w->done) {
        WGPUFutureWaitInfo info;
        WGPUWaitStatus ws;
        info.future = future;
        info.completed = 0;
        ws = wgpuInstanceWaitAny(m3_wg.instance, 1u, &info, (uint64_t)M3_WG_WAIT_MS * 1000000u);
        if (ws == WGPUWaitStatus_TimedOut) {
            return -1;
        }
    }
    while (!w->done) {
        wgpuInstanceProcessEvents(m3_wg.instance);
        if (w->done) {
            break;
        }
        if (m3_now_ms() > deadline) {
            return -1;
        }
        m3_wg_nap();
    }
    return 0;
}

static WGPUCallbackMode m3_wg_mode(void) { return WGPUCallbackMode_AllowProcessEvents; }

/* ---- discovery ---------------------------------------------------------------------------- */
static void m3_wg_note(const char *api, const char *pref, const char *msg)
{
    size_t used = strlen(m3_wg.reason);
    m3_format(m3_wg.reason + used, sizeof m3_wg.reason - used, "%s%s/%s: %s", used == 0u ? "" : "; ", api, pref, msg);
}

static void m3_wg_fill_info(m3_wg_device *d, const WGPUAdapterInfo *ai)
{
    m3vcpu_device_info *info = &d->info;
    char device[96];
    char vendor[48];
    char desc[64];
    char arch[32];
    int secondary = d->api != m3_wg.devices[0].api;
    memset(info, 0, sizeof *info);
    info->struct_size = sizeof *info;
    switch (ai->adapterType) {
    case WGPUAdapterType_DiscreteGPU:
        info->kind = M3VCPU_DEVICE_GPU_DISCRETE;
        info->score = 900;
        break;
    case WGPUAdapterType_IntegratedGPU:
        info->kind = M3VCPU_DEVICE_GPU_INTEGRATED;
        info->score = 45;
        info->flags = M3VCPU_DEVICE_FLAG_UNIFIED;
        break;
    case WGPUAdapterType_CPU:
        info->kind = M3VCPU_DEVICE_CPU;
        info->score = 8;
        info->flags = M3VCPU_DEVICE_FLAG_UNIFIED;
        break;
    default:
        info->kind = M3VCPU_DEVICE_OTHER;
        info->score = 4;
        break;
    }
    info->score -= secondary; /* deterministic tie-break: the first API probed wins */
    info->compute_units = 0u;  /* not exposed by webgpu.h */
    info->max_local = d->limits.maxComputeInvocationsPerWorkgroup < d->limits.maxComputeWorkgroupSizeX
                          ? d->limits.maxComputeInvocationsPerWorkgroup
                          : d->limits.maxComputeWorkgroupSizeX;
    info->global_mem_bytes = 0u; /* not exposed by webgpu.h */
    info->max_alloc_bytes = (d->limits.maxBufferSize < d->limits.maxStorageBufferBindingSize
                                 ? d->limits.maxBufferSize
                                 : d->limits.maxStorageBufferBindingSize)
                            & ~(uint64_t)3u;
    m3_wg_copy_view(device, sizeof device, ai->device.length != 0u ? ai->device : ai->description);
    m3_wg_copy_view(vendor, sizeof vendor, ai->vendor);
    m3_wg_copy_view(desc, sizeof desc, ai->description);
    m3_wg_copy_view(arch, sizeof arch, ai->architecture);
    m3_format(info->name, sizeof info->name, "%s (%s)", device, m3_wg_api_name(d->api));
    m3_format(info->vendor, sizeof info->vendor, "%s 0x%04x", vendor, (unsigned)ai->vendorID);
    m3_format(info->version, sizeof info->version, "%s | %s | arch %s", m3_wg_api_name(d->api), desc, arch);
    m3_format(info->isa, sizeof info->isa, "WGSL/%s", m3_wg_api_name(d->api));
}

static void m3_wg_add_adapter(WGPUAdapter adapter, WGPUPowerPreference pref)
{
    WGPUAdapterInfo ai = WGPU_ADAPTER_INFO_INIT;
    m3_wg_device *d;
    int32_t i;
    if (wgpuAdapterGetInfo(adapter, &ai) != WGPUStatus_Success) {
        wgpuAdapterRelease(adapter);
        return;
    }
    for (i = 0; i < m3_wg.count; ++i) {
        if (m3_wg.devices[i].vendor_id == ai.vendorID && m3_wg.devices[i].device_id == ai.deviceID
            && m3_wg.devices[i].api == ai.backendType) {
            wgpuAdapterInfoFreeMembers(ai);
            wgpuAdapterRelease(adapter);
            return;
        }
    }
    if (m3_wg.count >= M3_WG_MAX_DEVICES) {
        wgpuAdapterInfoFreeMembers(ai);
        wgpuAdapterRelease(adapter);
        return;
    }
    d = &m3_wg.devices[m3_wg.count];
    memset(d, 0, sizeof *d);
    d->adapter = adapter;
    d->api = ai.backendType;
    d->pref = pref;
    d->vendor_id = ai.vendorID;
    d->device_id = ai.deviceID;
    d->limits.nextInChain = NULL;
    if (wgpuAdapterGetLimits(adapter, &d->limits) != WGPUStatus_Success) {
        wgpuAdapterInfoFreeMembers(ai);
        wgpuAdapterRelease(adapter);
        return;
    }
    m3_wg.count++;
    m3_wg_fill_info(d, &ai);
    wgpuAdapterInfoFreeMembers(ai);
}

/* Requests one adapter; NULL with a message in why[] when none is returned. */
static WGPUAdapter m3_wg_adapter(WGPUBackendType api, WGPUPowerPreference pref, char *why, size_t cap)
{
    WGPURequestAdapterOptions o = WGPU_REQUEST_ADAPTER_OPTIONS_INIT;
    WGPURequestAdapterCallbackInfo ci = WGPU_REQUEST_ADAPTER_CALLBACK_INFO_INIT;
    m3_wg_wait w;
    WGPUFuture f;
    memset(&w, 0, sizeof w);
    o.featureLevel = WGPUFeatureLevel_Core;
    o.powerPreference = pref;
    o.backendType = api;
    ci.mode = m3_wg_mode();
    ci.callback = m3_wg_on_adapter;
    ci.userdata1 = &w;
    f = wgpuInstanceRequestAdapter(m3_wg.instance, &o, ci);
    if (m3_wg_wait_for(f, &w) != 0) {
        m3_strlcpy(why, "timed out", cap);
        return NULL;
    }
    if (!w.ok) {
        m3_strlcpy(why, w.msg[0] != '\0' ? w.msg : "no adapter", cap);
        return NULL;
    }
    return w.adapter;
}

static void m3_wg_request(WGPUBackendType api, WGPUPowerPreference pref)
{
    char why[256];
    WGPUAdapter adapter = m3_wg_adapter(api, pref, why, sizeof why);
    if (adapter == NULL) {
        m3_wg_note(m3_wg_api_name(api), pref == WGPUPowerPreference_LowPower ? "low-power" : "high-performance", why);
    } else {
        m3_wg_add_adapter(adapter, pref);
    }
}

/* Creates the instance with TimedWaitAny + MultipleDevicesPerAdapter, dropping what is missing. */
static void m3_wg_instance(void)
{
    static const WGPUInstanceFeatureName features[] = {WGPUInstanceFeatureName_TimedWaitAny,
                                                       WGPUInstanceFeatureName_MultipleDevicesPerAdapter};
    size_t n;
    for (n = sizeof features / sizeof features[0]; n > 0u && m3_wg.instance == NULL; --n) {
        WGPUInstanceDescriptor desc = WGPU_INSTANCE_DESCRIPTOR_INIT;
        desc.requiredFeatureCount = n;
        desc.requiredFeatures = features;
        m3_wg.instance = wgpuCreateInstance(&desc);
        if (m3_wg.instance != NULL) {
            m3_wg.timed_wait = 1;
            m3_wg.multi_device = n > 1u;
        }
    }
    if (m3_wg.instance == NULL) {
        m3_wg.instance = wgpuCreateInstance(NULL);
    }
}

/* Hard rule 2 (probe never crashes): refuses an in-process C++ runtime older than the one the
 * linked Dawn needs, before any Dawn call that takes a std::mutex. Returns 1 when usable. */
static int m3_wg_runtime_ok(void)
{
#if defined(_WIN32) && M3VCPU_WEBGPU_MIN_MSVCP > 0
    HMODULE m = GetModuleHandleW(L"msvcp140.dll");
    HRSRC r = m == NULL ? NULL : FindResourceW(m, MAKEINTRESOURCEW(VS_VERSION_INFO), MAKEINTRESOURCEW(16));
    HGLOBAL g = r == NULL ? NULL : LoadResource(m, r);
    const uint8_t *p = g == NULL ? NULL : (const uint8_t *)LockResource(g);
    DWORD n = p == NULL ? 0u : SizeofResource(m, r);
    DWORD i;
    for (i = 0; i + sizeof(VS_FIXEDFILEINFO) <= n; i += 4u) {
        const VS_FIXEDFILEINFO *f = (const VS_FIXEDFILEINFO *)(const void *)(p + i);
        if (f->dwSignature == 0xFEEF04BDu) {
            unsigned major = HIWORD(f->dwFileVersionMS), minor = LOWORD(f->dwFileVersionMS);
            if (major * 100u + minor < (unsigned)M3VCPU_WEBGPU_MIN_MSVCP) {
                m3_format(m3_wg.reason, sizeof m3_wg.reason,
                          "in-process msvcp140.dll %u.%u.%u is older than %u.%u required by the linked Dawn (C++ "
                          "runtime ABI); load a newer msvcp140.dll first",
                          major, minor, (unsigned)HIWORD(f->dwFileVersionLS),
                          (unsigned)M3VCPU_WEBGPU_MIN_MSVCP / 100u, (unsigned)M3VCPU_WEBGPU_MIN_MSVCP % 100u);
                return 0;
            }
            return 1;
        }
    }
#endif
    return 1;
}

/* M3VCPU_WEBGPU_API="d3d12,vulkan" limits discovery to the listed APIs (unset = all). */
static int m3_wg_api_wanted(WGPUBackendType api)
{
    char list[128];
    char *cursor;
    if (m3_getenv("M3VCPU_WEBGPU_API", list, sizeof list) == NULL) {
        return 1;
    }
    for (cursor = list; cursor != NULL && *cursor != '\0';) {
        char *sep = strchr(cursor, ',');
        if (sep != NULL) {
            *sep = '\0';
        }
        while (*cursor == ' ') {
            ++cursor;
        }
        if (m3_strieq(cursor, m3_wg_api_name(api))) {
            return 1;
        }
        cursor = sep == NULL ? NULL : sep + 1;
    }
    return 0;
}

static void m3_wg_init_locked(void)
{
#if defined(_WIN32)
    static const WGPUBackendType apis[] = {WGPUBackendType_D3D12, WGPUBackendType_Vulkan};
#elif defined(__APPLE__)
    static const WGPUBackendType apis[] = {WGPUBackendType_Metal};
#else
    static const WGPUBackendType apis[] = {WGPUBackendType_Vulkan};
#endif
    static const WGPUPowerPreference prefs[] = {WGPUPowerPreference_HighPerformance, WGPUPowerPreference_LowPower};
    size_t a, p;
    if (m3_wg.ready) {
        return;
    }
    m3_wg.ready = 1;
    m3_wg.status = M3VCPU_E_UNAVAILABLE;
    if (!m3_wg_runtime_ok()) {
        return;
    }
    m3_wg_instance();
    if (m3_wg.instance == NULL) {
        m3_strlcpy(m3_wg.reason, "wgpuCreateInstance failed", sizeof m3_wg.reason);
        return;
    }
    for (a = 0; a < sizeof apis / sizeof apis[0]; ++a) {
        if (!m3_wg_api_wanted(apis[a])) {
            m3_wg_note(m3_wg_api_name(apis[a]), "*", "skipped by M3VCPU_WEBGPU_API");
            continue;
        }
        for (p = 0; p < sizeof prefs / sizeof prefs[0]; ++p) {
            m3_wg_request(apis[a], prefs[p]);
        }
    }
    if (m3_wg.count == 0) {
        if (m3_wg.reason[0] == '\0') {
            m3_strlcpy(m3_wg.reason, "no WebGPU adapter", sizeof m3_wg.reason);
        }
        return;
    }
    m3_wg.status = M3VCPU_OK;
}

static int32_t m3_wg_ensure(void)
{
    int32_t status;
    m3_mutex_lock(&m3_wg.lock);
    m3_wg_init_locked();
    status = m3_wg.status;
    m3_mutex_unlock(&m3_wg.lock);
    if (status != M3VCPU_OK) {
        m3_format(m3_wg_error, sizeof m3_wg_error, "webgpu unavailable: %s", m3_wg.reason);
    }
    return status;
}

/* ---- probe / devices ---------------------------------------------------------------------- */
static int32_t m3_wg_probe(m3vcpu_probe *out)
{
    int32_t i;
    size_t used;
    if (out == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    if (m3_wg_ensure() != M3VCPU_OK) {
        m3_strlcpy(out->reason, m3_wg.reason, sizeof out->reason);
        return M3VCPU_OK;
    }
    out->available = 1;
    out->device_count = m3_wg.count;
    m3_format(out->reason, sizeof out->reason, "ok: %d device(s):", (int)m3_wg.count);
    for (i = 0; i < m3_wg.count; ++i) {
        if (m3_wg.devices[i].info.score > out->score) {
            out->score = m3_wg.devices[i].info.score;
        }
        used = strlen(out->reason);
        m3_format(out->reason + used, sizeof out->reason - used, " [%d] %s;", (int)i, m3_wg.devices[i].info.name);
    }
    return M3VCPU_OK;
}

static int32_t m3_wg_device_count(void) { return m3_wg_ensure() == M3VCPU_OK ? m3_wg.count : 0; }

static int32_t m3_wg_device_info(int32_t index, m3vcpu_device_info *out)
{
    if (out == NULL) {
        return m3_wg_fail(NULL, M3VCPU_E_INVALID_ARG, "device_info: null out");
    }
    if (m3_wg_ensure() != M3VCPU_OK) {
        return M3VCPU_E_UNAVAILABLE;
    }
    if (index < 0 || index >= m3_wg.count) {
        return m3_wg_fail(NULL, M3VCPU_E_NO_DEVICE, "device_info: index out of range");
    }
    *out = m3_wg.devices[index].info;
    return M3VCPU_OK;
}

/* ---- error scopes --------------------------------------------------------------------------- */
/* Pops one error scope; on an error writes "<what>: <type>: <message>" to ctx->err. */
static int32_t m3_wg_pop(m3vcpu_ctx *ctx, const char *what, int32_t validation_status)
{
    WGPUPopErrorScopeCallbackInfo ci = WGPU_POP_ERROR_SCOPE_CALLBACK_INFO_INIT;
    m3_wg_wait w;
    WGPUFuture f;
    memset(&w, 0, sizeof w);
    ci.mode = m3_wg_mode();
    ci.callback = m3_wg_on_pop;
    ci.userdata1 = &w;
    f = wgpuDevicePopErrorScope(ctx->dev, ci);
    if (m3_wg_wait_for(f, &w) != 0) {
        m3_format(ctx->err, sizeof ctx->err, "%s: error scope wait timed out", what);
        return M3VCPU_E_DRIVER;
    }
    if (w.ok && w.type == WGPUErrorType_NoError) {
        return M3VCPU_OK;
    }
    m3_format(ctx->err, sizeof ctx->err, "%s: %s: %s", what, m3_wg_error_name(w.type), w.msg);
    if (w.type == WGPUErrorType_OutOfMemory) {
        return M3VCPU_E_OUT_OF_MEMORY;
    }
    return w.type == WGPUErrorType_Validation ? validation_status : M3VCPU_E_DRIVER;
}

/* Two nested scopes (out-of-memory outside, validation inside), popped in LIFO order. */
static void m3_wg_push2(m3vcpu_ctx *ctx)
{
    wgpuDevicePushErrorScope(ctx->dev, WGPUErrorFilter_OutOfMemory);
    wgpuDevicePushErrorScope(ctx->dev, WGPUErrorFilter_Validation);
}

static int32_t m3_wg_pop2(m3vcpu_ctx *ctx, const char *what, int32_t validation_status)
{
    int32_t inner = m3_wg_pop(ctx, what, validation_status);
    char first[1024];
    int32_t outer;
    m3_strlcpy(first, ctx->err, sizeof first);
    outer = m3_wg_pop(ctx, what, validation_status);
    if (inner != M3VCPU_OK) {
        m3_strlcpy(ctx->err, first, sizeof ctx->err);
        return inner;
    }
    return outer;
}

/* Converts a pending device loss or uncaptured error into a status (and clears the error). */
static int32_t m3_wg_health(m3vcpu_ctx *ctx, const char *what, int32_t status)
{
    if (ctx->lost) {
        m3_format(ctx->err, sizeof ctx->err, "%s: device lost: %s", what, ctx->async_msg);
        return M3VCPU_E_DRIVER;
    }
    if (ctx->async_error) {
        m3_format(ctx->err, sizeof ctx->err, "%s: %s", what, ctx->async_msg);
        ctx->async_error = 0;
        return status;
    }
    return M3VCPU_OK;
}

/* ---- context ------------------------------------------------------------------------------ */
static void m3_wg_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf);
static void m3_wg_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *kernel);
static int32_t m3_wg_finish(m3vcpu_ctx *ctx);

/* WebGPU adapters are single-use unless the instance has MultipleDevicesPerAdapter: then the
 * enumerated adapter is reused; otherwise a fresh adapter for the same (API, power preference)
 * is requested and must report the same vendor / device id. */
static WGPUAdapter m3_wg_device_adapter(const m3_wg_device *d)
{
    WGPUAdapterInfo ai = WGPU_ADAPTER_INFO_INIT;
    char why[256];
    WGPUAdapter fresh;
    int same;
    if (m3_wg.multi_device) {
        return d->adapter;
    }
    fresh = m3_wg_adapter(d->api, d->pref, why, sizeof why);
    if (fresh == NULL) {
        m3_format(m3_wg_error, sizeof m3_wg_error, "ctx_create: re-requesting the adapter of %s: %s", d->info.name, why);
        return NULL;
    }
    same = wgpuAdapterGetInfo(fresh, &ai) == WGPUStatus_Success && ai.vendorID == d->vendor_id
           && ai.deviceID == d->device_id && ai.backendType == d->api;
    if (same) {
        wgpuAdapterInfoFreeMembers(ai);
        return fresh;
    }
    wgpuAdapterInfoFreeMembers(ai);
    wgpuAdapterRelease(fresh);
    m3_format(m3_wg_error, sizeof m3_wg_error, "ctx_create: the adapter for %s changed since enumeration", d->info.name);
    return NULL;
}

static int32_t m3_wg_ctx_create(int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_ctx **out)
{
    const m3_wg_device *d;
    m3vcpu_ctx *ctx;
    WGPUAdapter adapter;
    WGPULimits want = WGPU_LIMITS_INIT;
    WGPUDeviceDescriptor dd = WGPU_DEVICE_DESCRIPTOR_INIT;
    WGPURequestDeviceCallbackInfo ci = WGPU_REQUEST_DEVICE_CALLBACK_INFO_INIT;
    m3_wg_wait w;
    WGPUFuture f;
    (void)opts;
    if (out == NULL) {
        return m3_wg_fail(NULL, M3VCPU_E_INVALID_ARG, "ctx_create: null out");
    }
    *out = NULL;
    if (m3_wg_ensure() != M3VCPU_OK) {
        return M3VCPU_E_UNAVAILABLE;
    }
    if (device < 0 || device >= m3_wg.count) {
        return m3_wg_fail(NULL, M3VCPU_E_NO_DEVICE, "ctx_create: device index out of range");
    }
    d = &m3_wg.devices[device];
    ctx = (m3vcpu_ctx *)calloc(1, sizeof *ctx);
    if (ctx == NULL) {
        return m3_wg_fail(NULL, M3VCPU_E_OUT_OF_MEMORY, "ctx_create: allocation");
    }
    ctx->magic = M3_WG_CTX_MAGIC;
    ctx->device = device;
    m3_wg_link_init(&ctx->bufs);
    m3_wg_link_init(&ctx->kernels);
    /* Ask for the adapter's limits on everything the vCPU depends on (defaults are 128 MiB
     * bindings and 256 MiB buffers). */
    want.maxStorageBufferBindingSize = d->limits.maxStorageBufferBindingSize;
    want.maxBufferSize = d->limits.maxBufferSize;
    want.maxStorageBuffersPerShaderStage = d->limits.maxStorageBuffersPerShaderStage;
    want.maxComputeWorkgroupsPerDimension = d->limits.maxComputeWorkgroupsPerDimension;
    want.maxComputeInvocationsPerWorkgroup = d->limits.maxComputeInvocationsPerWorkgroup;
    want.maxComputeWorkgroupSizeX = d->limits.maxComputeWorkgroupSizeX;
    dd.label = m3_wg_view("m3vcpu");
    dd.requiredLimits = &want;
    dd.deviceLostCallbackInfo.mode = WGPUCallbackMode_AllowSpontaneous;
    dd.deviceLostCallbackInfo.callback = m3_wg_on_lost;
    dd.deviceLostCallbackInfo.userdata1 = ctx;
    dd.uncapturedErrorCallbackInfo.callback = m3_wg_on_error;
    dd.uncapturedErrorCallbackInfo.userdata1 = ctx;
    memset(&w, 0, sizeof w);
    ci.mode = m3_wg_mode();
    ci.callback = m3_wg_on_device;
    ci.userdata1 = &w;
    adapter = m3_wg_device_adapter(d);
    if (adapter == NULL) {
        free(ctx);
        return M3VCPU_E_DRIVER;
    }
    f = wgpuAdapterRequestDevice(adapter, &dd, ci);
    if (m3_wg_wait_for(f, &w) != 0 || !w.ok) {
        m3_format(m3_wg_error, sizeof m3_wg_error, "wgpuAdapterRequestDevice(%s): %s", d->info.name,
                  w.done ? w.msg : "timed out");
        if (adapter != d->adapter) {
            wgpuAdapterRelease(adapter);
        }
        free(ctx);
        return M3VCPU_E_DRIVER;
    }
    if (adapter != d->adapter) {
        wgpuAdapterRelease(adapter); /* the device keeps what it needs */
    }
    ctx->dev = w.device;
    ctx->queue = wgpuDeviceGetQueue(ctx->dev);
    ctx->limits.nextInChain = NULL;
    if (wgpuDeviceGetLimits(ctx->dev, &ctx->limits) != WGPUStatus_Success) {
        ctx->limits = d->limits;
    }
    ctx->max_alloc = (ctx->limits.maxBufferSize < ctx->limits.maxStorageBufferBindingSize
                          ? ctx->limits.maxBufferSize
                          : ctx->limits.maxStorageBufferBindingSize)
                     & ~(uint64_t)3u;
    ctx->max_local = ctx->limits.maxComputeInvocationsPerWorkgroup < ctx->limits.maxComputeWorkgroupSizeX
                         ? ctx->limits.maxComputeInvocationsPerWorkgroup
                         : ctx->limits.maxComputeWorkgroupSizeX;
    *out = ctx;
    return M3VCPU_OK;
}

static void m3_wg_ctx_destroy(m3vcpu_ctx *ctx)
{
    if (ctx == NULL || ctx->magic != M3_WG_CTX_MAGIC) {
        return;
    }
    (void)m3_wg_finish(ctx);
    while (ctx->kernels.next != &ctx->kernels) {
        m3_wg_kernel_free(ctx, (m3vcpu_kernel *)(void *)ctx->kernels.next);
    }
    while (ctx->bufs.next != &ctx->bufs) {
        m3_wg_buf_free(ctx, (m3vcpu_buf *)(void *)ctx->bufs.next);
    }
    if (ctx->staging != NULL) {
        wgpuBufferDestroy(ctx->staging);
        wgpuBufferRelease(ctx->staging);
    }
    wgpuQueueRelease(ctx->queue);
    wgpuDeviceDestroy(ctx->dev);
    wgpuDeviceRelease(ctx->dev);
    ctx->magic = 0;
    free(ctx);
}

/* ---- buffers ------------------------------------------------------------------------------ */
static int m3_wg_owns(const m3vcpu_ctx *ctx, const m3vcpu_buf *buf)
{
    return ctx != NULL && buf != NULL && buf->magic == M3_WG_BUF_MAGIC && buf->owner == ctx;
}

static int m3_wg_ctx_ok(const m3vcpu_ctx *ctx) { return ctx != NULL && ctx->magic == M3_WG_CTX_MAGIC; }

static int32_t m3_wg_buf_alloc(m3vcpu_ctx *ctx, size_t bytes, uint32_t flags, m3vcpu_buf **out)
{
    WGPUBufferDescriptor bd = WGPU_BUFFER_DESCRIPTOR_INIT;
    m3vcpu_buf *buf;
    int32_t status;
    (void)flags; /* access is declared by the WGSL kernel */
    if (!m3_wg_ctx_ok(ctx) || out == NULL || bytes == 0u) {
        return m3_wg_fail(m3_wg_ctx_ok(ctx) ? ctx : NULL, M3VCPU_E_INVALID_ARG,
                          "buf_alloc: null argument or zero size");
    }
    *out = NULL;
    status = m3_wg_health(ctx, "buf_alloc", M3VCPU_E_DRIVER);
    if (status != M3VCPU_OK) {
        return status;
    }
    if ((uint64_t)bytes > ctx->max_alloc) {
        m3_format(ctx->err, sizeof ctx->err,
                  "buf_alloc: %llu bytes exceed the storage binding limit %llu (min of maxBufferSize, "
                  "maxStorageBufferBindingSize); shard the batch",
                  (unsigned long long)bytes, (unsigned long long)ctx->max_alloc);
        return M3VCPU_E_OUT_OF_RANGE;
    }
    buf = (m3vcpu_buf *)calloc(1, sizeof *buf);
    if (buf == NULL) {
        return m3_wg_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "buf_alloc: descriptor");
    }
    buf->size = bytes;
    buf->padded = ((uint64_t)bytes + 3u) & ~(uint64_t)3u;
    bd.label = m3_wg_view("m3vcpu buffer");
    bd.usage = WGPUBufferUsage_Storage | WGPUBufferUsage_CopySrc | WGPUBufferUsage_CopyDst;
    bd.size = buf->padded;
    m3_wg_push2(ctx);
    buf->buffer = wgpuDeviceCreateBuffer(ctx->dev, &bd);
    status = m3_wg_pop2(ctx, "wgpuDeviceCreateBuffer", M3VCPU_E_INVALID_ARG);
    if (status != M3VCPU_OK || buf->buffer == NULL) {
        if (buf->buffer != NULL) {
            wgpuBufferRelease(buf->buffer);
        }
        free(buf);
        return status != M3VCPU_OK ? status : m3_wg_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "wgpuDeviceCreateBuffer: null");
    }
    buf->magic = M3_WG_BUF_MAGIC;
    buf->owner = ctx;
    buf->id = ++ctx->next_id;
    m3_wg_link_push(&ctx->bufs, &buf->link);
    *out = buf;
    return M3VCPU_OK;
}

static int32_t m3_wg_buf_check(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, size_t offset, size_t bytes, const void *host)
{
    if (!m3_wg_ctx_ok(ctx)) {
        return m3_wg_fail(NULL, M3VCPU_E_INVALID_ARG, "buffer: foreign context");
    }
    if (!m3_wg_owns(ctx, buf) || (host == NULL && bytes != 0u)) {
        return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG, "buffer: foreign buffer or null host pointer");
    }
    if (offset > buf->size || bytes > buf->size - offset) {
        return m3_wg_fail(ctx, M3VCPU_E_OUT_OF_RANGE, "buffer: range outside the buffer");
    }
    return m3_wg_health(ctx, "buffer", M3VCPU_E_DRIVER);
}

static int32_t m3_wg_staging(m3vcpu_ctx *ctx, uint64_t bytes)
{
    WGPUBufferDescriptor bd = WGPU_BUFFER_DESCRIPTOR_INIT;
    int32_t status;
    if (ctx->staging != NULL && ctx->staging_size >= bytes) {
        return M3VCPU_OK;
    }
    if (ctx->staging != NULL) {
        wgpuBufferDestroy(ctx->staging);
        wgpuBufferRelease(ctx->staging);
        ctx->staging = NULL;
        ctx->staging_size = 0;
    }
    bd.label = m3_wg_view("m3vcpu readback");
    bd.usage = WGPUBufferUsage_MapRead | WGPUBufferUsage_CopyDst;
    bd.size = bytes < M3_WG_STAGING_MIN ? M3_WG_STAGING_MIN : bytes;
    m3_wg_push2(ctx);
    ctx->staging = wgpuDeviceCreateBuffer(ctx->dev, &bd);
    status = m3_wg_pop2(ctx, "staging buffer", M3VCPU_E_DRIVER);
    if (status != M3VCPU_OK || ctx->staging == NULL) {
        if (ctx->staging != NULL) {
            wgpuBufferRelease(ctx->staging);
            ctx->staging = NULL;
        }
        return status != M3VCPU_OK ? status : m3_wg_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "staging buffer: null");
    }
    ctx->staging_size = bd.size;
    return M3VCPU_OK;
}

/* Copies [offset, offset + bytes) to dst through the staging buffer; any alignment. */
static int32_t m3_wg_read_raw(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, uint64_t offset, uint8_t *dst, uint64_t bytes)
{
    uint64_t lo = offset & ~(uint64_t)3u;
    uint64_t hi = (offset + bytes + 3u) & ~(uint64_t)3u;
    uint64_t pos;
    for (pos = lo; pos < hi;) {
        uint64_t n = hi - pos < M3_WG_CHUNK ? hi - pos : M3_WG_CHUNK;
        uint64_t a = pos > offset ? pos : offset;
        uint64_t b = pos + n < offset + bytes ? pos + n : offset + bytes;
        WGPUBufferMapCallbackInfo ci = WGPU_BUFFER_MAP_CALLBACK_INFO_INIT;
        WGPUCommandEncoder enc;
        WGPUCommandBuffer cmd;
        const uint8_t *mapped;
        m3_wg_wait w;
        WGPUFuture f;
        int32_t status = m3_wg_staging(ctx, n);
        if (status != M3VCPU_OK) {
            return status;
        }
        enc = wgpuDeviceCreateCommandEncoder(ctx->dev, NULL);
        wgpuCommandEncoderCopyBufferToBuffer(enc, buf->buffer, pos, ctx->staging, 0u, n);
        cmd = wgpuCommandEncoderFinish(enc, NULL);
        wgpuQueueSubmit(ctx->queue, 1u, &cmd);
        wgpuCommandBufferRelease(cmd);
        wgpuCommandEncoderRelease(enc);
        memset(&w, 0, sizeof w);
        ci.mode = m3_wg_mode();
        ci.callback = m3_wg_on_map;
        ci.userdata1 = &w;
        f = wgpuBufferMapAsync(ctx->staging, WGPUMapMode_Read, 0u, (size_t)n, ci);
        if (m3_wg_wait_for(f, &w) != 0) {
            return m3_wg_fail(ctx, M3VCPU_E_DRIVER, "buf_read: map wait timed out");
        }
        if (!w.ok) {
            status = m3_wg_health(ctx, "buf_read", M3VCPU_E_DRIVER);
            if (status == M3VCPU_OK) {
                m3_format(ctx->err, sizeof ctx->err, "buf_read: wgpuBufferMapAsync: %s", w.msg);
            }
            return M3VCPU_E_DRIVER;
        }
        mapped = (const uint8_t *)wgpuBufferGetConstMappedRange(ctx->staging, 0u, (size_t)n);
        if (mapped == NULL) {
            wgpuBufferUnmap(ctx->staging);
            return m3_wg_fail(ctx, M3VCPU_E_DRIVER, "buf_read: wgpuBufferGetConstMappedRange returned null");
        }
        if (b > a) {
            memcpy(dst + (a - offset), mapped + (a - pos), (size_t)(b - a));
        }
        wgpuBufferUnmap(ctx->staging);
        pos += n;
    }
    return m3_wg_health(ctx, "buf_read", M3VCPU_E_DRIVER);
}

/* Queues an aligned write (offset and bytes multiples of 4) in bounded chunks. */
static void m3_wg_put(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, uint64_t offset, const uint8_t *src, uint64_t bytes)
{
    uint64_t pos;
    for (pos = 0; pos < bytes;) {
        uint64_t n = bytes - pos < M3_WG_CHUNK ? bytes - pos : M3_WG_CHUNK;
        wgpuQueueWriteBuffer(ctx->queue, buf->buffer, offset + pos, src + pos, (size_t)n);
        pos += n;
    }
}

/* Merges n bytes (1..3) at byte lane `lane` into the 32-bit word at word_offset. */
static int32_t m3_wg_patch(m3vcpu_ctx *ctx, const m3vcpu_buf *buf, uint64_t word_offset, uint64_t lane,
                           const uint8_t *src, uint64_t n)
{
    uint8_t word[4];
    int32_t status = m3_wg_read_raw(ctx, buf, word_offset, word, 4u);
    if (status != M3VCPU_OK) {
        return status;
    }
    memcpy(word + lane, src, (size_t)n);
    m3_wg_put(ctx, buf, word_offset, word, 4u);
    return M3VCPU_OK;
}

static int32_t m3_wg_buf_write(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, const void *src, size_t bytes)
{
    const uint8_t *p = (const uint8_t *)src;
    uint64_t off = offset, left = bytes, mid;
    int32_t status = m3_wg_buf_check(ctx, buf, offset, bytes, src);
    if (status != M3VCPU_OK || bytes == 0u) {
        return status;
    }
    if ((off & 3u) != 0u) {
        uint64_t lane = off & 3u;
        uint64_t n = 4u - lane < left ? 4u - lane : left;
        status = m3_wg_patch(ctx, buf, off - lane, lane, p, n);
        if (status != M3VCPU_OK) {
            return status;
        }
        off += n;
        p += n;
        left -= n;
    }
    mid = left & ~(uint64_t)3u;
    if (mid > 0u) {
        m3_wg_put(ctx, buf, off, p, mid);
        off += mid;
        p += mid;
        left -= mid;
    }
    if (left > 0u) {
        status = m3_wg_patch(ctx, buf, off, 0u, p, left);
        if (status != M3VCPU_OK) {
            return status;
        }
    }
    return m3_wg_health(ctx, "buf_write", M3VCPU_E_DRIVER);
}

static int32_t m3_wg_buf_read(m3vcpu_ctx *ctx, m3vcpu_buf *buf, size_t offset, void *dst, size_t bytes)
{
    int32_t status = m3_wg_buf_check(ctx, buf, offset, bytes, dst);
    if (status != M3VCPU_OK || bytes == 0u) {
        return status;
    }
    return m3_wg_read_raw(ctx, buf, offset, (uint8_t *)dst, bytes);
}

static void m3_wg_buf_free(m3vcpu_ctx *ctx, m3vcpu_buf *buf)
{
    if (!m3_wg_owns(ctx, buf)) {
        return;
    }
    m3_wg_link_remove(&buf->link);
    wgpuBufferDestroy(buf->buffer); /* storage is released once queued work using it completes */
    wgpuBufferRelease(buf->buffer);
    buf->magic = 0;
    free(buf);
}

/* ---- kernels ------------------------------------------------------------------------------ */
static void m3_wg_slot_clear(m3_wg_slot *s)
{
    if (s->group != NULL) {
        wgpuBindGroupRelease(s->group);
    }
    if (s->layout != NULL) {
        wgpuBindGroupLayoutRelease(s->layout);
    }
    if (s->pipeline != NULL) {
        wgpuComputePipelineRelease(s->pipeline);
    }
    memset(s, 0, sizeof *s);
}

/* Pipeline for one local size (override constant m3_local), cached per kernel. */
static int32_t m3_wg_pipeline(m3vcpu_ctx *ctx, m3vcpu_kernel *k, uint32_t local, m3_wg_slot **out)
{
    WGPUConstantEntry constant = WGPU_CONSTANT_ENTRY_INIT;
    WGPUComputePipelineDescriptor pd = WGPU_COMPUTE_PIPELINE_DESCRIPTOR_INIT;
    m3_wg_slot *s = NULL;
    WGPUComputePipeline pipeline;
    int32_t status;
    uint32_t i;
    for (i = 0; i < M3_WG_PIPELINES; ++i) {
        if (k->slots[i].pipeline != NULL && k->slots[i].local == local) {
            *out = &k->slots[i];
            return M3VCPU_OK;
        }
    }
    constant.key = m3_wg_view(M3_WG_LOCAL_KEY);
    constant.value = (double)local;
    pd.label = m3_wg_view(k->entry);
    pd.layout = NULL; /* auto layout: @group(0) bindings as the shader declares them */
    pd.compute.module = k->module;
    pd.compute.entryPoint = m3_wg_view(k->entry);
    pd.compute.constantCount = 1u;
    pd.compute.constants = &constant;
    m3_wg_push2(ctx);
    pipeline = wgpuDeviceCreateComputePipeline(ctx->dev, &pd);
    status = m3_wg_pop2(ctx, "wgpuDeviceCreateComputePipeline", M3VCPU_E_KERNEL_BUILD);
    if (status != M3VCPU_OK || pipeline == NULL) {
        if (pipeline != NULL) {
            wgpuComputePipelineRelease(pipeline);
        }
        return status != M3VCPU_OK ? status : m3_wg_fail(ctx, M3VCPU_E_KERNEL_BUILD, "compute pipeline: null");
    }
    for (i = 0; i < M3_WG_PIPELINES && s == NULL; ++i) {
        if (k->slots[i].pipeline == NULL) {
            s = &k->slots[i];
        }
    }
    if (s == NULL) {
        s = &k->slots[k->victim];
        k->victim = (k->victim + 1u) % M3_WG_PIPELINES;
        m3_wg_slot_clear(s);
    }
    s->local = local;
    s->pipeline = pipeline;
    s->layout = wgpuComputePipelineGetBindGroupLayout(pipeline, 0u);
    *out = s;
    return M3VCPU_OK;
}

static const m3_wg_twin *m3_wg_twin_for(const char *entry)
{
    size_t i;
    for (i = 0; i < m3_wg_twin_count; ++i) {
        if (strcmp(m3_wg_twins[i].entry, entry) == 0) {
            return &m3_wg_twins[i];
        }
    }
    return NULL;
}

/* First 8 bytes of a sha256 as 16 hex digits (cap >= 17). */
static void m3_wg_hex_prefix(const uint8_t *sha, char *out, size_t cap)
{
    static const char digits[] = "0123456789abcdef";
    size_t i;
    out[0] = '\0';
    for (i = 0; i < 8u && 2u * i + 2u < cap; ++i) {
        out[2u * i] = digits[sha[i] >> 4];
        out[2u * i + 1u] = digits[sha[i] & 15u];
        out[2u * i + 2u] = '\0';
    }
}

/* Picks the WGSL source: an M3K_WGSL blob, else the admitted twin of the offered OpenCL C blob. */
static int32_t m3_wg_source(m3vcpu_ctx *ctx, const m3k_blob *blobs, size_t count, const char *entry,
                            const uint8_t **code, size_t *size, uint32_t *nargs)
{
    const m3_wg_twin *twin;
    uint8_t digest[32];
    char want[20];
    size_t i;
    for (i = 0; i < count; ++i) {
        if (blobs[i].format == M3K_WGSL) {
            *code = blobs[i].bytes;
            *size = blobs[i].size;
            *nargs = 0u;
            return M3VCPU_OK;
        }
    }
    twin = m3_wg_twin_for(entry);
    if (twin == NULL) {
        return m3_wg_fail(ctx, M3VCPU_E_NO_KERNEL_FORMAT,
                          "kernel_load: no M3K_WGSL blob and no WGSL twin compiled in for this entry");
    }
    for (i = 0; i < count; ++i) {
        if (blobs[i].format == M3K_OPENCL_C && memcmp(blobs[i].sha256, twin->paired_sha256, 32u) == 0) {
            break;
        }
    }
    if (i == count) {
        m3_wg_hex_prefix(twin->paired_sha256, want, sizeof want);
        m3_format(ctx->err, sizeof ctx->err,
                  "kernel_load: the WGSL twin of %s restates the M3K_OPENCL_C blob sha256 %s..., which the "
                  "offered bundle does not contain (backend and core built from different sources?)",
                  entry, want);
        return M3VCPU_E_NO_KERNEL_FORMAT;
    }
    m3vcpu_sha256(twin->wgsl, twin->size, digest);
    if (memcmp(digest, twin->wgsl_sha256, sizeof digest) != 0) {
        return m3_wg_fail(ctx, M3VCPU_E_INTEGRITY, "kernel_load: embedded WGSL twin does not match its sha256");
    }
    *code = twin->wgsl;
    *size = twin->size;
    *nargs = twin->nargs;
    return M3VCPU_OK;
}

static int32_t m3_wg_kernel_load(m3vcpu_ctx *ctx, const m3k_blob *blobs, size_t count, const char *entry,
                                 m3vcpu_kernel **out)
{
    WGPUShaderSourceWGSL wgsl = WGPU_SHADER_SOURCE_WGSL_INIT;
    WGPUShaderModuleDescriptor md = WGPU_SHADER_MODULE_DESCRIPTOR_INIT;
    const uint8_t *code = NULL;
    size_t size = 0;
    uint32_t nargs = 0;
    m3vcpu_kernel *k;
    m3_wg_slot *slot;
    int32_t status;
    if (!m3_wg_ctx_ok(ctx) || out == NULL || entry == NULL || (blobs == NULL && count != 0u)) {
        return m3_wg_fail(m3_wg_ctx_ok(ctx) ? ctx : NULL, M3VCPU_E_INVALID_ARG, "kernel_load: null argument");
    }
    *out = NULL;
    if (strlen(entry) >= sizeof k->entry) {
        return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG, "kernel_load: entry name too long");
    }
    status = m3_wg_health(ctx, "kernel_load", M3VCPU_E_DRIVER);
    if (status == M3VCPU_OK) {
        status = m3_wg_source(ctx, blobs, count, entry, &code, &size, &nargs);
    }
    if (status != M3VCPU_OK) {
        return status;
    }
    k = (m3vcpu_kernel *)calloc(1, sizeof *k);
    if (k == NULL) {
        return m3_wg_fail(ctx, M3VCPU_E_OUT_OF_MEMORY, "kernel_load: descriptor");
    }
    m3_strlcpy(k->entry, entry, sizeof k->entry);
    k->nargs = nargs;
    wgsl.code.data = (const char *)code;
    wgsl.code.length = size;
    md.nextInChain = &wgsl.chain;
    md.label = m3_wg_view(k->entry);
    m3_wg_push2(ctx);
    k->module = wgpuDeviceCreateShaderModule(ctx->dev, &md);
    status = m3_wg_pop2(ctx, "wgpuDeviceCreateShaderModule", M3VCPU_E_KERNEL_BUILD);
    if (status == M3VCPU_OK) {
        uint32_t local = ctx->max_local < M3_WG_DEFAULT_LOCAL ? ctx->max_local : M3_WG_DEFAULT_LOCAL;
        status = m3_wg_pipeline(ctx, k, local, &slot); /* surfaces entry / layout errors at load time */
    }
    if (status != M3VCPU_OK) {
        uint32_t i;
        for (i = 0; i < M3_WG_PIPELINES; ++i) {
            m3_wg_slot_clear(&k->slots[i]);
        }
        if (k->module != NULL) {
            wgpuShaderModuleRelease(k->module);
        }
        free(k);
        return status;
    }
    k->magic = M3_WG_KER_MAGIC;
    k->owner = ctx;
    m3_wg_link_push(&ctx->kernels, &k->link);
    *out = k;
    return M3VCPU_OK;
}

static void m3_wg_kernel_free(m3vcpu_ctx *ctx, m3vcpu_kernel *k)
{
    uint32_t i;
    if (!m3_wg_ctx_ok(ctx) || k == NULL || k->magic != M3_WG_KER_MAGIC || k->owner != ctx) {
        return;
    }
    m3_wg_link_remove(&k->link);
    for (i = 0; i < M3_WG_PIPELINES; ++i) {
        m3_wg_slot_clear(&k->slots[i]);
    }
    wgpuShaderModuleRelease(k->module);
    k->magic = 0;
    free(k);
}

/* Bind group for the launch's buffers, cached per pipeline slot by buffer ids. */
static int32_t m3_wg_bind(m3vcpu_ctx *ctx, m3_wg_slot *s, const m3vcpu_arg *args, uint32_t count)
{
    WGPUBindGroupEntry entries[M3VCPU_MAX_ARGS];
    WGPUBindGroupDescriptor bd = WGPU_BIND_GROUP_DESCRIPTOR_INIT;
    WGPUBindGroup group;
    int32_t status;
    uint32_t i;
    int same = s->group != NULL && s->group_count == count;
    for (i = 0; i < count && same; ++i) {
        same = s->group_ids[i] == args[i].buf->id;
    }
    if (same) {
        return M3VCPU_OK;
    }
    for (i = 0; i < count; ++i) {
        WGPUBindGroupEntry e = WGPU_BIND_GROUP_ENTRY_INIT;
        e.binding = i;
        e.buffer = args[i].buf->buffer;
        e.offset = 0u;
        e.size = args[i].buf->padded;
        entries[i] = e;
    }
    bd.label = m3_wg_view("m3vcpu arguments");
    bd.layout = s->layout;
    bd.entryCount = count;
    bd.entries = entries;
    m3_wg_push2(ctx);
    group = wgpuDeviceCreateBindGroup(ctx->dev, &bd);
    status = m3_wg_pop2(ctx, "launch: wgpuDeviceCreateBindGroup", M3VCPU_E_INVALID_ARG);
    if (status != M3VCPU_OK || group == NULL) {
        if (group != NULL) {
            wgpuBindGroupRelease(group);
        }
        return status != M3VCPU_OK ? status : m3_wg_fail(ctx, M3VCPU_E_LAUNCH, "launch: bind group null");
    }
    if (s->group != NULL) {
        wgpuBindGroupRelease(s->group);
    }
    s->group = group;
    s->group_count = count;
    for (i = 0; i < count; ++i) {
        s->group_ids[i] = args[i].buf->id;
    }
    return M3VCPU_OK;
}

static int32_t m3_wg_launch_1d(m3vcpu_ctx *ctx, m3vcpu_kernel *k, uint64_t global, uint32_t local,
                               const m3vcpu_arg *args, uint32_t count)
{
    WGPUCommandEncoder enc;
    WGPUComputePassEncoder pass;
    WGPUCommandBuffer cmd;
    m3_wg_slot *slot;
    uint64_t groups, gx, gy, max_dim;
    uint32_t l, i;
    int32_t status;
    if (!m3_wg_ctx_ok(ctx) || k == NULL || k->magic != M3_WG_KER_MAGIC || k->owner != ctx) {
        return m3_wg_fail(m3_wg_ctx_ok(ctx) ? ctx : NULL, M3VCPU_E_INVALID_ARG, "launch: foreign kernel");
    }
    if (count > M3VCPU_MAX_ARGS || (count > 0u && args == NULL) || (k->nargs != 0u && count != k->nargs)) {
        return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: argument count does not match the kernel");
    }
    for (i = 0; i < count; ++i) {
        if (args[i].kind != M3VCPU_ARG_BUFFER) {
            return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG,
                              "launch: webgpu kernels take storage buffers only; pass scalars in a buffer");
        }
        if (!m3_wg_owns(ctx, args[i].buf)) {
            return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: foreign buffer argument");
        }
    }
    l = local == 0u ? (ctx->max_local < M3_WG_DEFAULT_LOCAL ? ctx->max_local : M3_WG_DEFAULT_LOCAL) : local;
    if (l > ctx->max_local) {
        return m3_wg_fail(ctx, M3VCPU_E_INVALID_ARG, "launch: local size above maxComputeInvocationsPerWorkgroup");
    }
    status = m3_wg_health(ctx, "launch", M3VCPU_E_LAUNCH);
    if (status != M3VCPU_OK || global == 0u) {
        return status;
    }
    groups = (global + l - 1u) / l;
    max_dim = ctx->limits.maxComputeWorkgroupsPerDimension;
    if (max_dim == 0u || max_dim == WGPU_LIMIT_U32_UNDEFINED) {
        max_dim = 65535u; /* WebGPU's guaranteed minimum */
    }
    gx = groups <= max_dim ? groups : max_dim;
    gy = (groups + gx - 1u) / gx;
    if (gy > max_dim) {
        return m3_wg_fail(ctx, M3VCPU_E_OUT_OF_RANGE,
                          "launch: workgroup grid exceeds maxComputeWorkgroupsPerDimension^2");
    }
    status = m3_wg_pipeline(ctx, k, l, &slot);
    if (status == M3VCPU_OK) {
        status = m3_wg_bind(ctx, slot, args, count);
    }
    if (status != M3VCPU_OK) {
        return status;
    }
    enc = wgpuDeviceCreateCommandEncoder(ctx->dev, NULL);
    pass = wgpuCommandEncoderBeginComputePass(enc, NULL);
    wgpuComputePassEncoderSetPipeline(pass, slot->pipeline);
    wgpuComputePassEncoderSetBindGroup(pass, 0u, slot->group, 0u, NULL);
    wgpuComputePassEncoderDispatchWorkgroups(pass, (uint32_t)gx, (uint32_t)gy, 1u);
    wgpuComputePassEncoderEnd(pass);
    cmd = wgpuCommandEncoderFinish(enc, NULL);
    wgpuQueueSubmit(ctx->queue, 1u, &cmd);
    wgpuCommandBufferRelease(cmd);
    wgpuComputePassEncoderRelease(pass);
    wgpuCommandEncoderRelease(enc);
    return m3_wg_health(ctx, "launch", M3VCPU_E_LAUNCH);
}

static int32_t m3_wg_finish(m3vcpu_ctx *ctx)
{
    WGPUQueueWorkDoneCallbackInfo ci = WGPU_QUEUE_WORK_DONE_CALLBACK_INFO_INIT;
    m3_wg_wait w;
    WGPUFuture f;
    int32_t status;
    if (!m3_wg_ctx_ok(ctx)) {
        return M3VCPU_E_INVALID_ARG;
    }
    status = m3_wg_health(ctx, "finish", M3VCPU_E_DRIVER);
    if (status != M3VCPU_OK) {
        return status;
    }
    wgpuQueueSubmit(ctx->queue, 0u, NULL); /* flush queued writes */
    memset(&w, 0, sizeof w);
    ci.mode = m3_wg_mode();
    ci.callback = m3_wg_on_done;
    ci.userdata1 = &w;
    f = wgpuQueueOnSubmittedWorkDone(ctx->queue, ci);
    if (m3_wg_wait_for(f, &w) != 0) {
        return m3_wg_fail(ctx, M3VCPU_E_DRIVER, "finish: wait timed out");
    }
    status = m3_wg_health(ctx, "finish", M3VCPU_E_DRIVER);
    if (status == M3VCPU_OK && !w.ok) {
        m3_format(ctx->err, sizeof ctx->err, "finish: wgpuQueueOnSubmittedWorkDone: %s", w.msg);
        status = M3VCPU_E_DRIVER;
    }
    return status;
}

static const char *m3_wg_last_error(const m3vcpu_ctx *ctx) { return ctx == NULL ? m3_wg_error : ctx->err; }

static const m3vcpu_backend_v1 m3_wg_table = {
    .abi = M3VCPU_ABI_V1,
    .struct_size = (uint32_t)sizeof(m3vcpu_backend_v1),
    .name = "webgpu",
    .probe = m3_wg_probe,
    .device_count = m3_wg_device_count,
    .device_info = m3_wg_device_info,
    .ctx_create = m3_wg_ctx_create,
    .ctx_destroy = m3_wg_ctx_destroy,
    .buf_alloc = m3_wg_buf_alloc,
    .buf_write = m3_wg_buf_write,
    .buf_read = m3_wg_buf_read,
    .buf_free = m3_wg_buf_free,
    .kernel_load = m3_wg_kernel_load,
    .kernel_free = m3_wg_kernel_free,
    .launch_1d = m3_wg_launch_1d,
    .finish = m3_wg_finish,
    .last_error = m3_wg_last_error,
};

M3VCPU_DLLEXPORT const m3vcpu_backend_v1 *m3vcpu_backend_entry_v1(void);
M3VCPU_DLLEXPORT const m3vcpu_backend_v1 *m3vcpu_backend_entry_v1(void) { return &m3_wg_table; }
