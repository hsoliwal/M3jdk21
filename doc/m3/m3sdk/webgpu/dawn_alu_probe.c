// M3 vCPU WebGPU probe (Apache-2.0): runs rv32im_alu.wgsl through native Dawn via the standard webgpu.h C ABI
// and checks every result against a 64-bit C oracle. Inputs are identical to the Chrome probe (16x16 corner pairs,
// then 262144 xorshift32 pairs from seed 0x4D330001, every 4th divisor in [-4, 3]), so equal out_sha = equal results.
// usage: dawn_alu_probe <rv32im_alu.wgsl> [d3d12|vulkan|any] [high|low]
#define _CRT_SECURE_NO_WARNINGS
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include <bcrypt.h>
#include <webgpu/webgpu.h>

#define OPS 18
#define CORNERS 16
#define RANDOM_PAIRS 262144u
#define PAIRS (CORNERS * CORNERS + RANDOM_PAIRS)

static const uint32_t CORNER[CORNERS] = {0u, 1u, 2u, 3u, 0x7FFFFFFFu, 0x80000000u, 0x80000001u, 0xFFFFFFFFu,
    0xFFFFFFFEu, 0xFFFFu, 0x10000u, 0x1Fu, 0x20u, 0x21u, 0x55555555u, 0xAAAAAAAAu};
static const char *NAMES[OPS] = {"ADD", "SUB", "SLL", "SLT", "SLTU", "XOR", "SRL", "SRA", "OR", "AND", "MUL", "MULH",
    "MULHSU", "MULHU", "DIV", "DIVU", "REM", "REMU"};

typedef struct Wait { int done; int ok; WGPUAdapter adapter; WGPUDevice device; } Wait;
static int g_errors;

static void print_sv(const char *tag, WGPUStringView m) {
    size_t n = m.data == NULL ? 0 : (m.length == WGPU_STRLEN ? strlen(m.data) : m.length);
    fprintf(stderr, "%s: %.*s\n", tag, (int)n, m.data == NULL ? "" : m.data);
}

static void on_adapter(WGPURequestAdapterStatus s, WGPUAdapter a, WGPUStringView m, void *u1, void *u2) {
    Wait *w = u1; (void)u2;
    w->ok = s == WGPURequestAdapterStatus_Success; w->adapter = a; w->done = 1;
    if (!w->ok) print_sv("adapter", m);
}

static void on_device(WGPURequestDeviceStatus s, WGPUDevice d, WGPUStringView m, void *u1, void *u2) {
    Wait *w = u1; (void)u2;
    w->ok = s == WGPURequestDeviceStatus_Success; w->device = d; w->done = 1;
    if (!w->ok) print_sv("device", m);
}

static void on_map(WGPUMapAsyncStatus s, WGPUStringView m, void *u1, void *u2) {
    Wait *w = u1; (void)u2;
    w->ok = s == WGPUMapAsyncStatus_Success; w->done = 1;
    if (!w->ok) print_sv("map", m);
}

static void on_error(WGPUDevice const *d, WGPUErrorType t, WGPUStringView m, void *u1, void *u2) {
    (void)d; (void)t; (void)u1; (void)u2;
    g_errors++; print_sv("webgpu-error", m);
}

static void pump(WGPUInstance inst, Wait *w) {
    while (!w->done) { wgpuInstanceProcessEvents(inst); Sleep(0); }
}

static uint32_t g_xs = 0x4D330001u;
static uint32_t next_u32(void) { g_xs ^= g_xs << 13; g_xs ^= g_xs >> 17; g_xs ^= g_xs << 5; return g_xs; }

static void make_pairs(uint32_t *p) {
    size_t k = 0;
    for (int i = 0; i < CORNERS; i++) for (int j = 0; j < CORNERS; j++) { p[k++] = CORNER[i]; p[k++] = CORNER[j]; }
    for (uint32_t r = 0; r < RANDOM_PAIRS; r++) {
        uint32_t a = next_u32(), b = next_u32();
        if ((r & 3u) == 3u) b = (b & 7u) + 0xFFFFFFFCu;
        p[k++] = a; p[k++] = b;
    }
}

static void oracle(uint32_t a, uint32_t b, uint32_t *r) {
    int32_t sa = (int32_t)a, sb = (int32_t)b; uint32_t sh = b & 31u;
    int ovf = a == 0x80000000u && b == 0xFFFFFFFFu;
    r[0] = a + b; r[1] = a - b; r[2] = a << sh; r[3] = sa < sb; r[4] = a < b; r[5] = a ^ b; r[6] = a >> sh;
    r[7] = (uint32_t)(sa >> sh); r[8] = a | b; r[9] = a & b; r[10] = a * b;
    r[11] = (uint32_t)((uint64_t)((int64_t)sa * (int64_t)sb) >> 32);
    r[12] = (uint32_t)((uint64_t)((int64_t)sa * (int64_t)b) >> 32);
    r[13] = (uint32_t)(((uint64_t)a * b) >> 32);
    r[14] = b == 0 ? 0xFFFFFFFFu : ovf ? a : (uint32_t)(sa / sb);
    r[15] = b == 0 ? 0xFFFFFFFFu : a / b;
    r[16] = b == 0 ? a : ovf ? 0u : (uint32_t)(sa % sb);
    r[17] = b == 0 ? a : a % b;
}

static void sha256_hex(const void *data, size_t len, char out[65]) {
    BCRYPT_ALG_HANDLE alg; UCHAR h[32];
    BCryptOpenAlgorithmProvider(&alg, BCRYPT_SHA256_ALGORITHM, NULL, 0);
    BCryptHash(alg, NULL, 0, (PUCHAR)data, (ULONG)len, h, 32);
    BCryptCloseAlgorithmProvider(alg, 0);
    for (int i = 0; i < 32; i++) sprintf(out + 2 * i, "%02x", h[i]);
}

static char *read_file(const char *path, size_t *len) {
    FILE *f = fopen(path, "rb"); if (f == NULL) return NULL;
    fseek(f, 0, SEEK_END); *len = (size_t)ftell(f); fseek(f, 0, SEEK_SET);
    char *s = malloc(*len + 1); size_t got = fread(s, 1, *len, f); fclose(f);
    if (got != *len) { free(s); return NULL; }
    s[*len] = 0; return s;
}

static WGPUBuffer make_buffer(WGPUDevice dev, uint64_t size, WGPUBufferUsage usage) {
    WGPUBufferDescriptor d = WGPU_BUFFER_DESCRIPTOR_INIT; d.size = size; d.usage = usage;
    return wgpuDeviceCreateBuffer(dev, &d);
}

int main(int argc, char **argv) {
    if (argc < 2) { fprintf(stderr, "usage: dawn_alu_probe <rv32im_alu.wgsl> [d3d12|vulkan|any] [high|low]\n"); return 2; }
    size_t wlen; char *wgsl = read_file(argv[1], &wlen);
    if (wgsl == NULL) { fprintf(stderr, "cannot read %s\n", argv[1]); return 2; }
    WGPUBackendType backend = WGPUBackendType_Undefined;
    if (argc > 2 && strcmp(argv[2], "d3d12") == 0) backend = WGPUBackendType_D3D12;
    if (argc > 2 && strcmp(argv[2], "vulkan") == 0) backend = WGPUBackendType_Vulkan;

    WGPUInstance inst = wgpuCreateInstance(NULL);
    WGPURequestAdapterOptions ao = WGPU_REQUEST_ADAPTER_OPTIONS_INIT;
    ao.featureLevel = WGPUFeatureLevel_Core;
    ao.powerPreference = argc > 3 && strcmp(argv[3], "low") == 0 ? WGPUPowerPreference_LowPower : WGPUPowerPreference_HighPerformance;
    ao.backendType = backend;
    Wait wa = {0};
    WGPURequestAdapterCallbackInfo ac = WGPU_REQUEST_ADAPTER_CALLBACK_INFO_INIT;
    ac.mode = WGPUCallbackMode_AllowProcessEvents; ac.callback = on_adapter; ac.userdata1 = &wa;
    wgpuInstanceRequestAdapter(inst, &ao, ac); pump(inst, &wa);
    if (!wa.ok) return 3;
    WGPUAdapterInfo info = WGPU_ADAPTER_INFO_INIT; wgpuAdapterGetInfo(wa.adapter, &info);

    WGPUDeviceDescriptor dd = WGPU_DEVICE_DESCRIPTOR_INIT;
    dd.uncapturedErrorCallbackInfo.callback = on_error;
    Wait wd = {0};
    WGPURequestDeviceCallbackInfo dc = WGPU_REQUEST_DEVICE_CALLBACK_INFO_INIT;
    dc.mode = WGPUCallbackMode_AllowProcessEvents; dc.callback = on_device; dc.userdata1 = &wd;
    wgpuAdapterRequestDevice(wa.adapter, &dd, dc); pump(inst, &wd);
    if (!wd.ok) return 3;
    WGPUDevice dev = wd.device; WGPUQueue q = wgpuDeviceGetQueue(dev);

    WGPUShaderSourceWGSL src = WGPU_SHADER_SOURCE_WGSL_INIT;
    src.code.data = wgsl; src.code.length = wlen;
    WGPUShaderModuleDescriptor smd = WGPU_SHADER_MODULE_DESCRIPTOR_INIT; smd.nextInChain = &src.chain;
    WGPUShaderModule mod = wgpuDeviceCreateShaderModule(dev, &smd);
    WGPUComputePipelineDescriptor cpd = WGPU_COMPUTE_PIPELINE_DESCRIPTOR_INIT;
    cpd.compute.module = mod; cpd.compute.entryPoint.data = "main"; cpd.compute.entryPoint.length = 4;
    WGPUComputePipeline pipe = wgpuDeviceCreateComputePipeline(dev, &cpd);
    if (g_errors) return 4;

    static uint32_t pairs[2 * PAIRS];
    make_pairs(pairs);
    const uint64_t in_bytes = sizeof pairs, out_bytes = (uint64_t)PAIRS * OPS * 4u;
    WGPUBuffer b_in = make_buffer(dev, in_bytes, WGPUBufferUsage_Storage | WGPUBufferUsage_CopyDst);
    WGPUBuffer b_out = make_buffer(dev, out_bytes, WGPUBufferUsage_Storage | WGPUBufferUsage_CopySrc);
    WGPUBuffer b_par = make_buffer(dev, 16, WGPUBufferUsage_Uniform | WGPUBufferUsage_CopyDst);
    WGPUBuffer b_rd = make_buffer(dev, out_bytes, WGPUBufferUsage_MapRead | WGPUBufferUsage_CopyDst);
    const uint32_t par[4] = {PAIRS, 0, 0, 0};
    wgpuQueueWriteBuffer(q, b_in, 0, pairs, (size_t)in_bytes);
    wgpuQueueWriteBuffer(q, b_par, 0, par, sizeof par);

    WGPUBindGroupEntry e[3];
    WGPUBuffer bufs[3] = {b_in, b_out, b_par};
    for (uint32_t i = 0; i < 3; i++) {
        WGPUBindGroupEntry x = WGPU_BIND_GROUP_ENTRY_INIT; x.binding = i; x.buffer = bufs[i]; x.size = WGPU_WHOLE_SIZE;
        e[i] = x;
    }
    WGPUBindGroupDescriptor bgd = WGPU_BIND_GROUP_DESCRIPTOR_INIT;
    bgd.layout = wgpuComputePipelineGetBindGroupLayout(pipe, 0); bgd.entryCount = 3; bgd.entries = e;
    WGPUBindGroup bg = wgpuDeviceCreateBindGroup(dev, &bgd);

    LARGE_INTEGER f, t0, t1; QueryPerformanceFrequency(&f); QueryPerformanceCounter(&t0);
    WGPUCommandEncoder enc = wgpuDeviceCreateCommandEncoder(dev, NULL);
    WGPUComputePassEncoder pass = wgpuCommandEncoderBeginComputePass(enc, NULL);
    wgpuComputePassEncoderSetPipeline(pass, pipe);
    wgpuComputePassEncoderSetBindGroup(pass, 0, bg, 0, NULL);
    wgpuComputePassEncoderDispatchWorkgroups(pass, (PAIRS + 255u) / 256u, 1, 1);
    wgpuComputePassEncoderEnd(pass);
    wgpuCommandEncoderCopyBufferToBuffer(enc, b_out, 0, b_rd, 0, out_bytes);
    WGPUCommandBuffer cmd = wgpuCommandEncoderFinish(enc, NULL);
    wgpuQueueSubmit(q, 1, &cmd);
    Wait wm = {0};
    WGPUBufferMapCallbackInfo mc = WGPU_BUFFER_MAP_CALLBACK_INFO_INIT;
    mc.mode = WGPUCallbackMode_AllowProcessEvents; mc.callback = on_map; mc.userdata1 = &wm;
    wgpuBufferMapAsync(b_rd, WGPUMapMode_Read, 0, (size_t)out_bytes, mc); pump(inst, &wm);
    QueryPerformanceCounter(&t1);
    if (!wm.ok || g_errors) return 5;
    const uint32_t *gpu = wgpuBufferGetConstMappedRange(b_rd, 0, (size_t)out_bytes);

    long bad[OPS] = {0}, total = 0; uint32_t exp[OPS];
    for (uint32_t i = 0; i < PAIRS; i++) {
        oracle(pairs[2 * i], pairs[2 * i + 1], exp);
        for (int j = 0; j < OPS; j++) if (gpu[OPS * i + j] != exp[j]) { bad[j]++; total++; }
    }
    char wsha[65], isha[65], osha[65];
    sha256_hex(wgsl, wlen, wsha); sha256_hex(pairs, (size_t)in_bytes, isha); sha256_hex(gpu, (size_t)out_bytes, osha);
    printf("adapter=%.*s/%.*s backend=0x%x wgsl_sha=%s in_sha=%s out_sha=%s pairs=%u results=%u mismatches=%ld ms=%.2f\n",
        (int)info.vendor.length, info.vendor.data, (int)info.architecture.length, info.architecture.data,
        (unsigned)info.backendType, wsha, isha, osha, (unsigned)PAIRS, (unsigned)(PAIRS * OPS), total,
        1000.0 * (double)(t1.QuadPart - t0.QuadPart) / (double)f.QuadPart);
    for (int j = 0; j < OPS; j++) if (bad[j]) printf("  %s mismatches=%ld\n", NAMES[j], bad[j]);
    wgpuBufferUnmap(b_rd);
    wgpuAdapterInfoFreeMembers(info);
    free(wgsl);
    return total == 0 ? 0 : 1;
}
