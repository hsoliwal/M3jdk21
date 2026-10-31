/* SPDX-License-Identifier: Apache-2.0 */
/*
 * test_webgpu.c - webgpu backend checks that the shared RV32IM suite (test_rv32im.c) does not
 * reach, on every webgpu device:
 *   - zero-filled allocation and byte-granular (unaligned) buffer writes / reads
 *   - over-limit allocation -> M3VCPU_E_OUT_OF_RANGE with a "shard the batch" message
 *   - the generic M3K_WGSL blob path: M3 WGSL kernel convention (override m3_local), 1-D and
 *     2-D workgroup grids, scalar-argument rejection, compile errors, missing m3_local override
 *   - the twin admission rule: an M3K_OPENCL_C blob other than the paired one is refused
 *   - a second concurrent session on the same device
 *   - RV32IM over a 2-D grid (70,000 cores, local 1) compared word for word with rv32_ref
 * plus the cross-backend state root: one batch run on cpu, every opencl device and every
 * webgpu device must give the same SHA-256 over memory || registers || state (little-endian
 * int32), equal to the C reference. Exit 77 = webgpu unavailable (ctest skip).
 */
#include "m3vcpu.h"
#include "m3vcpu_check.h"
#include "m3vcpu_rv32im.h"
#include "rv32_asm.h"
#include "rv32_ref.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define NREG M3VCPU_RV32_REGISTER_COUNT
#define NST M3VCPU_RV32_STATE_STRIDE

static m3vcpu_session *g_s;
static char g_label[192];
static uint64_t g_rng;

static uint64_t rnd(void)
{
    uint64_t z = (g_rng += 0x9E3779B97F4A7C15ull);
    z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9ull;
    z = (z ^ (z >> 27)) * 0x94D049BB133111EBull;
    return z ^ (z >> 31);
}

static uint32_t below(uint32_t n) { return (uint32_t)(rnd() % n); }

static void blob_of(m3k_blob *b, uint32_t format, const void *bytes, size_t size)
{
    memset(b, 0, sizeof *b);
    b->format = format;
    b->bytes = (const uint8_t *)bytes;
    b->size = size;
    m3vcpu_sha256(bytes, size, b->sha256);
}

/* ---- buffers ------------------------------------------------------------------------------- */
static void test_buffers(void)
{
    enum { N = 1031 };
    uint8_t mirror[N], back[N], src[64];
    m3vcpu_buf *buf = NULL;
    m3vcpu_device_info info;
    size_t i;
    int op, zero = 1;
    int32_t rc = m3vcpu_buffer_alloc(g_s, N, 0, &buf);
    CHECK(rc == M3VCPU_OK, "%s alloc %d bytes: %s", g_label, N, m3vcpu_last_error(g_s));
    if (rc != M3VCPU_OK) {
        return;
    }
    CHECK(m3vcpu_buffer_read(g_s, buf, 0, back, N) == M3VCPU_OK, "%s read fresh buffer", g_label);
    for (i = 0; i < N; ++i) {
        zero &= back[i] == 0u;
    }
    CHECK(zero, "%s a new buffer reads as zeros", g_label);
    memset(mirror, 0, sizeof mirror);
    for (op = 0; op < 300; ++op) {
        size_t len = 1u + below(64u);
        size_t off = below((uint32_t)(N - len + 1u));
        for (i = 0; i < len; ++i) {
            src[i] = (uint8_t)rnd();
        }
        if (below(3u) != 0u) {
            CHECK(m3vcpu_buffer_write(g_s, buf, off, src, len) == M3VCPU_OK, "%s write [%zu,+%zu): %s", g_label, off,
                  len, m3vcpu_last_error(g_s));
            memcpy(mirror + off, src, len);
        } else {
            CHECK(m3vcpu_buffer_read(g_s, buf, off, back, len) == M3VCPU_OK, "%s read [%zu,+%zu)", g_label, off, len);
            CHECK(memcmp(back, mirror + off, len) == 0, "%s read [%zu,+%zu) matches the host mirror", g_label, off,
                  len);
        }
    }
    CHECK(m3vcpu_buffer_read(g_s, buf, 0, back, N) == M3VCPU_OK, "%s read back all", g_label);
    CHECK(memcmp(back, mirror, N) == 0, "%s 300 unaligned writes/reads agree with the host mirror", g_label);
    m3vcpu_buffer_free(g_s, buf);

    (void)m3vcpu_session_device(g_s, NULL, &info);
    buf = NULL;
    rc = m3vcpu_buffer_alloc(g_s, (size_t)info.max_alloc_bytes + 1u, 0, &buf);
    CHECK(rc == M3VCPU_E_OUT_OF_RANGE && buf == NULL && strstr(m3vcpu_last_error(g_s), "shard the batch") != NULL,
          "%s alloc max_alloc_bytes + 1 (%llu) -> OUT_OF_RANGE: rc %d, %s", g_label,
          (unsigned long long)info.max_alloc_bytes + 1u, rc, m3vcpu_last_error(g_s));
}

/* ---- generic M3K_WGSL blobs ------------------------------------------------------------------ */
static const char k_iota[] =
    "// SPDX-License-Identifier: Apache-2.0\n"
    "@group(0) @binding(0) var<storage, read_write> dst: array<u32>;\n"
    "@group(0) @binding(1) var<storage, read> cfg: array<u32>;\n"
    "override m3_local: u32 = 64u;\n"
    "@compute @workgroup_size(m3_local)\n"
    "fn m3t_iota(@builtin(workgroup_id) wid: vec3<u32>, @builtin(num_workgroups) nwg: vec3<u32>,\n"
    "            @builtin(local_invocation_index) lidx: u32) {\n"
    "  let i = (wid.y * nwg.x + wid.x) * m3_local + lidx;\n"
    "  if (i >= cfg[0]) { return; }\n"
    "  dst[i] = (i * 2654435761u) ^ cfg[1];\n"
    "}\n";
static const char k_broken[] = "@compute @workgroup_size(1) fn m3t_bad() { let x: u32 = ; }\n";
static const char k_fixed[] = "@group(0) @binding(0) var<storage, read_write> d: array<u32>;\n"
                              "@compute @workgroup_size(1) fn m3t_fixed() { d[0] = 1u; }\n";

static void iota_case(m3vcpu_kernel *k, uint32_t n, uint32_t local, uint32_t salt)
{
    uint32_t cfg[2];
    uint32_t *out = (uint32_t *)calloc((size_t)n + 7u, sizeof(uint32_t));
    m3vcpu_buf *b[2] = {NULL, NULL};
    m3vcpu_arg args[2];
    uint32_t i;
    long bad = 0;
    cfg[0] = n;
    cfg[1] = salt;
    CHECK(m3vcpu_buffer_alloc(g_s, ((size_t)n + 7u) * 4u, 0, &b[0]) == M3VCPU_OK, "%s iota alloc", g_label);
    CHECK(m3vcpu_buffer_alloc(g_s, sizeof cfg, 0, &b[1]) == M3VCPU_OK, "%s iota cfg alloc", g_label);
    CHECK(m3vcpu_buffer_write(g_s, b[1], 0, cfg, sizeof cfg) == M3VCPU_OK, "%s iota cfg write", g_label);
    for (i = 0; i < 2u; ++i) {
        args[i].kind = M3VCPU_ARG_BUFFER;
        args[i].i32 = 0;
        args[i].buf = b[i];
    }
    CHECK(m3vcpu_launch_1d(g_s, k, n, local, args, 2) == M3VCPU_OK, "%s iota n=%u local=%u: %s", g_label,
          (unsigned)n, (unsigned)local, m3vcpu_last_error(g_s));
    CHECK(m3vcpu_finish(g_s) == M3VCPU_OK, "%s iota finish: %s", g_label, m3vcpu_last_error(g_s));
    CHECK(m3vcpu_buffer_read(g_s, b[0], 0, out, ((size_t)n + 7u) * 4u) == M3VCPU_OK, "%s iota read", g_label);
    for (i = 0; i < n + 7u; ++i) {
        uint32_t want = i < n ? (i * 2654435761u) ^ salt : 0u;
        bad += out[i] != want;
    }
    CHECK(bad == 0, "%s iota n=%u local=%u (%s grid): %ld wrong words (items past n must stay 0)", g_label,
          (unsigned)n, (unsigned)local, (n + (local == 0u ? 64u : local) - 1u) / (local == 0u ? 64u : local) > 65535u
                                            ? "2-D"
                                            : "1-D",
          bad);
    m3vcpu_buffer_free(g_s, b[0]);
    m3vcpu_buffer_free(g_s, b[1]);
    free(out);
}

static void test_wgsl_blobs(void)
{
    m3k_blob blob;
    m3vcpu_kernel *k = NULL;
    m3vcpu_buf *b = NULL;
    m3vcpu_arg args[2];
    int32_t rc;
    blob_of(&blob, M3K_WGSL, k_iota, sizeof k_iota - 1u);
    rc = m3vcpu_kernel_load(g_s, &blob, 1, "m3t_iota", &k);
    CHECK(rc == M3VCPU_OK, "%s load M3K_WGSL blob: %s", g_label, m3vcpu_last_error(g_s));
    if (rc == M3VCPU_OK) {
        iota_case(k, 1000u, 0u, 0x5A5A0001u);
        iota_case(k, 1000u, 7u, 0x5A5A0002u);
        iota_case(k, 4097u, 256u, 0x5A5A0003u);
        iota_case(k, 200003u, 1u, 0x5A5A0004u); /* 200,003 workgroups -> 65,535 x 4 grid */
        iota_case(k, 70001u, 1u, 0x5A5A0005u);  /* 65,535 x 2 grid, ragged tail */
        CHECK(m3vcpu_buffer_alloc(g_s, 64, 0, &b) == M3VCPU_OK, "%s alloc", g_label);
        args[0].kind = M3VCPU_ARG_BUFFER;
        args[0].i32 = 0;
        args[0].buf = b;
        args[1].kind = M3VCPU_ARG_I32;
        args[1].i32 = 5;
        args[1].buf = NULL;
        rc = m3vcpu_launch_1d(g_s, k, 4, 0, args, 2);
        CHECK(rc == M3VCPU_E_INVALID_ARG && strstr(m3vcpu_last_error(g_s), "scalars") != NULL,
              "%s scalar argument rejected: rc %d %s", g_label, rc, m3vcpu_last_error(g_s));
        rc = m3vcpu_launch_1d(g_s, k, 4, 100000u, args, 1);
        CHECK(rc == M3VCPU_E_INVALID_ARG, "%s local above the device limit rejected: rc %d", g_label, rc);
        m3vcpu_buffer_free(g_s, b);
        m3vcpu_kernel_free(g_s, k);
    }
    k = NULL;
    blob_of(&blob, M3K_WGSL, k_broken, sizeof k_broken - 1u);
    rc = m3vcpu_kernel_load(g_s, &blob, 1, "m3t_bad", &k);
    CHECK(rc == M3VCPU_E_KERNEL_BUILD && k == NULL && strlen(m3vcpu_last_error(g_s)) > 20u,
          "%s WGSL syntax error -> KERNEL_BUILD: rc %d", g_label, rc);
    blob_of(&blob, M3K_WGSL, k_fixed, sizeof k_fixed - 1u);
    rc = m3vcpu_kernel_load(g_s, &blob, 1, "m3t_fixed", &k);
    CHECK(rc == M3VCPU_E_KERNEL_BUILD && strstr(m3vcpu_last_error(g_s), "m3_local") != NULL,
          "%s kernel without the m3_local override -> KERNEL_BUILD: rc %d %.200s", g_label, rc,
          m3vcpu_last_error(g_s));
    blob_of(&blob, M3K_WGSL, k_iota, sizeof k_iota - 1u);
    rc = m3vcpu_kernel_load(g_s, &blob, 1, "no_such_entry", &k);
    CHECK(rc == M3VCPU_E_KERNEL_BUILD, "%s unknown entry point -> KERNEL_BUILD: rc %d", g_label, rc);
}

/* A second live session on the same device (WebGPU adapters are single-use by default). */
static void test_second_session(int32_t device)
{
    m3vcpu_session *first = g_s;
    m3k_blob blob;
    m3vcpu_kernel *k = NULL;
    int32_t rc = m3vcpu_open("webgpu", device, NULL, &g_s);
    CHECK(rc == M3VCPU_OK, "%s second concurrent session: %s", g_label, m3vcpu_last_error(NULL));
    if (rc != M3VCPU_OK) {
        g_s = first;
        return;
    }
    blob_of(&blob, M3K_WGSL, k_iota, sizeof k_iota - 1u);
    rc = m3vcpu_kernel_load(g_s, &blob, 1, "m3t_iota", &k);
    CHECK(rc == M3VCPU_OK, "%s second session kernel_load: %s", g_label, m3vcpu_last_error(g_s));
    if (rc == M3VCPU_OK) {
        iota_case(k, 5000u, 32u, 0x5A5A0006u);
    }
    m3vcpu_close(g_s);
    g_s = first;
}

static void test_twin_admission(void)
{
    const m3k_blob *blobs = NULL;
    size_t count = 0, i;
    m3k_blob changed;
    m3vcpu_kernel *k = NULL;
    uint8_t *bytes;
    int32_t rc;
    CHECK(m3vcpu_builtin_blobs(M3VCPU_RV32_KERNEL_ENTRY, &blobs, &count) == M3VCPU_OK, "builtin blobs");
    for (i = 0; i < count && blobs[i].format != M3K_OPENCL_C; ++i) {
    }
    CHECK(i < count, "builtin bundle carries an M3K_OPENCL_C blob");
    if (i == count) {
        return;
    }
    rc = m3vcpu_kernel_load(g_s, &blobs[i], 1, M3VCPU_RV32_KERNEL_ENTRY, &k);
    CHECK(rc == M3VCPU_OK, "%s paired OpenCL C blob alone admits the WGSL twin: %s", g_label, m3vcpu_last_error(g_s));
    if (rc == M3VCPU_OK) {
        m3vcpu_kernel_free(g_s, k);
    }
    bytes = (uint8_t *)malloc(blobs[i].size + 1u);
    memcpy(bytes, blobs[i].bytes, blobs[i].size);
    bytes[blobs[i].size] = (uint8_t)'\n';
    blob_of(&changed, M3K_OPENCL_C, bytes, blobs[i].size + 1u);
    changed.min_isa = blobs[i].min_isa;
    k = NULL;
    rc = m3vcpu_kernel_load(g_s, &changed, 1, M3VCPU_RV32_KERNEL_ENTRY, &k);
    CHECK(rc == M3VCPU_E_NO_KERNEL_FORMAT && k == NULL, "%s changed OpenCL C source refused for the twin: rc %d %s",
          g_label, rc, m3vcpu_last_error(g_s));
    free(bytes);
}

/* ---- RV32IM batches -------------------------------------------------------------------------- */
typedef struct rbatch {
    int32_t cores, words;
    int32_t *mem, *regs, *state;
} rbatch;

static rbatch *rb_new(int32_t cores, int32_t words)
{
    rbatch *b = (rbatch *)calloc(1, sizeof *b);
    int32_t c;
    b->cores = cores;
    b->words = words;
    b->mem = (int32_t *)calloc((size_t)cores * (size_t)words, 4u);
    b->regs = (int32_t *)calloc((size_t)cores * NREG, 4u);
    b->state = (int32_t *)calloc((size_t)cores * NST, 4u);
    for (c = 0; c < cores; ++c) {
        b->state[(size_t)c * NST + M3VCPU_RV32_STATE_TRAP_CAUSE] = M3VCPU_RV32_TRAP_NONE;
    }
    return b;
}

static rbatch *rb_copy(const rbatch *s)
{
    rbatch *b = rb_new(s->cores, s->words);
    memcpy(b->mem, s->mem, (size_t)s->cores * (size_t)s->words * 4u);
    memcpy(b->regs, s->regs, (size_t)s->cores * NREG * 4u);
    memcpy(b->state, s->state, (size_t)s->cores * NST * 4u);
    return b;
}

static void rb_free(rbatch *b)
{
    free(b->mem);
    free(b->regs);
    free(b->state);
    free(b);
}

/* SHA-256 over memory || registers || state as little-endian int32 (host order on x86/ARM). */
static void rb_root(const rbatch *b, char hex[65])
{
    size_t nm = (size_t)b->cores * (size_t)b->words * 4u, nr = (size_t)b->cores * NREG * 4u;
    size_t ns = (size_t)b->cores * NST * 4u;
    uint8_t *all = (uint8_t *)malloc(nm + nr + ns);
    uint8_t d[32];
    int i;
    memcpy(all, b->mem, nm);
    memcpy(all + nm, b->regs, nr);
    memcpy(all + nm + nr, b->state, ns);
    m3vcpu_sha256(all, nm + nr + ns, d);
    for (i = 0; i < 32; ++i) {
        m3_format(hex + 2 * i, 3u, "%02x", (unsigned)d[i]);
    }
    free(all);
}

/* Runs `slices` launches of the built-in kernel on the current session; 0 on success. */
static int rb_run(rbatch *b, int32_t slices, int32_t budget, uint32_t local)
{
    size_t sizes[4];
    m3vcpu_buf *buf[4] = {NULL, NULL, NULL, NULL};
    m3vcpu_kernel *k = NULL;
    m3vcpu_arg args[4];
    int32_t config[M3VCPU_RV32_CONFIG_STRIDE];
    int32_t s, ok = 1;
    int i;
    sizes[0] = (size_t)b->cores * (size_t)b->words * 4u;
    sizes[1] = (size_t)b->cores * NREG * 4u;
    sizes[2] = (size_t)b->cores * NST * 4u;
    sizes[3] = sizeof config;
    ok &= m3vcpu_kernel_builtin(g_s, M3VCPU_RV32_KERNEL_ENTRY, &k) == M3VCPU_OK;
    for (i = 0; i < 4 && ok; ++i) {
        ok &= m3vcpu_buffer_alloc(g_s, sizes[i], 0, &buf[i]) == M3VCPU_OK;
        args[i].kind = M3VCPU_ARG_BUFFER;
        args[i].i32 = 0;
        args[i].buf = buf[i];
    }
    ok = ok && m3vcpu_buffer_write(g_s, buf[0], 0, b->mem, sizes[0]) == M3VCPU_OK
         && m3vcpu_buffer_write(g_s, buf[1], 0, b->regs, sizes[1]) == M3VCPU_OK
         && m3vcpu_buffer_write(g_s, buf[2], 0, b->state, sizes[2]) == M3VCPU_OK;
    config[M3VCPU_RV32_CONFIG_CORE_COUNT] = b->cores;
    config[M3VCPU_RV32_CONFIG_MEMORY_WORDS_PER_CORE] = b->words;
    config[M3VCPU_RV32_CONFIG_INSTRUCTION_BUDGET] = budget;
    ok = ok && m3vcpu_buffer_write(g_s, buf[3], 0, config, sizeof config) == M3VCPU_OK;
    for (s = 0; s < slices && ok; ++s) {
        ok = m3vcpu_launch_1d(g_s, k, (uint64_t)b->cores, local, args, 4) == M3VCPU_OK
             && m3vcpu_finish(g_s) == M3VCPU_OK;
    }
    ok = ok && m3vcpu_buffer_read(g_s, buf[0], 0, b->mem, sizes[0]) == M3VCPU_OK
         && m3vcpu_buffer_read(g_s, buf[1], 0, b->regs, sizes[1]) == M3VCPU_OK
         && m3vcpu_buffer_read(g_s, buf[2], 0, b->state, sizes[2]) == M3VCPU_OK;
    CHECK(ok, "%s rv32 batch (%d cores, %d slices of %d, local %u): %s", g_label, (int)b->cores, (int)slices,
          (int)budget, (unsigned)local, m3vcpu_last_error(g_s));
    for (i = 0; i < 4; ++i) {
        m3vcpu_buffer_free(g_s, buf[i]);
    }
    return ok ? 0 : 1;
}

static void rb_reference(rbatch *b, int32_t slices, int32_t budget)
{
    rv32_coverage cov;
    int32_t s;
    memset(&cov, 0, sizeof cov);
    for (s = 0; s < slices; ++s) {
        rv32_ref_slice(b->mem, b->regs, b->state, b->cores, b->words, budget, &cov);
    }
}

static void rb_compare(const rbatch *dev, const rbatch *ref, const char *what)
{
    size_t nm = (size_t)ref->cores * (size_t)ref->words, nr = (size_t)ref->cores * NREG, ns = (size_t)ref->cores * NST;
    CHECK(memcmp(dev->mem, ref->mem, nm * 4u) == 0, "%s %s: memory equals rv32_ref", g_label, what);
    CHECK(memcmp(dev->regs, ref->regs, nr * 4u) == 0, "%s %s: registers equal rv32_ref", g_label, what);
    CHECK(memcmp(dev->state, ref->state, ns * 4u) == 0, "%s %s: state equals rv32_ref", g_label, what);
}

/* 70,000 cores at local 1: 70,000 workgroups -> a 65,535 x 2 grid. */
static void test_rv32_grid(void)
{
    rbatch *b = rb_new(70000, 8), *ref;
    int32_t c;
    for (c = 0; c < b->cores; ++c) {
        int32_t *m = b->mem + (size_t)c * 8u;
        m[0] = (int32_t)ADDI(10, 5, 7);
        m[1] = (int32_t)MUL(10, 10, 10);
        m[2] = (int32_t)SW(10, 0, 28);
        m[3] = (int32_t)XM3_HALT;
        b->regs[(size_t)c * NREG + 5] = c;
    }
    ref = rb_copy(b);
    if (rb_run(b, 1, 10, 1u) == 0) {
        rb_reference(ref, 1, 10);
        rb_compare(b, ref, "70,000 cores over a 2-D grid");
        int32_t want = (int32_t)(70006u * 70006u); /* MUL keeps the low 32 bits */
        CHECK(b->regs[(size_t)69999 * NREG + 10] == want && b->mem[(size_t)69999 * 8u + 7u] == want,
              "%s last core result low32(70006^2)", g_label);
    }
    rb_free(ref);
    rb_free(b);
}

/* Batch for the cross-backend root: random biased instruction streams + a halting loop mix. */
static uint32_t xinsn(void)
{
    static const uint32_t f7s[3] = {0u, 0x20u, 1u};
    /* Every random field is drawn in this fixed order (one per declaration) before encoding. */
    uint32_t k = below(20);
    uint32_t rd = below(32);
    uint32_t a = below(32);
    uint32_t b = below(32);
    uint32_t base = 1u + below(8);
    uint32_t f3 = below(8);
    uint32_t f7 = f7s[below(3)];
    uint32_t imm20 = below(1u << 20);
    int32_t jump = ((int32_t)below(17) - 8) * 4;
    int32_t imm6 = (int32_t)below(64) - 32;
    int32_t imm12 = (int32_t)below(4096) - 2048;
    uint32_t raw = (uint32_t)(rnd() >> 32);
    switch (k) {
    case 0: return LUI(rd, imm20);
    case 1: return AUIPC(rd, imm20);
    case 2: return JAL(rd, jump);
    case 3: return JALR(rd, base, imm6);
    case 4:
    case 5: return rv_b(jump, b, a, f3);
    case 6:
    case 7: return rv_i(imm6, base, f3 % 6u, rd, 0x03u);
    case 8:
    case 9: return rv_s(imm6, b, base, f3 & 3u);
    case 10:
    case 11:
    case 12: return rv_i(imm12, a, f3, rd, 0x13u);
    case 13:
    case 14:
    case 15:
    case 16: return rv_r(f7, b, a, f3, rd, 0x33u);
    case 17: return (raw & 1u) != 0u ? FENCE : ECALL;
    case 18: return XM3_HALT;
    default: return raw;
    }
}

static rbatch *x_batch(void)
{
    rbatch *b = rb_new(4099, 128);
    int32_t c, r;
    size_t w;
    g_rng = 0x5EED00A1ull;
    for (c = 0; c < b->cores; ++c) {
        int32_t *m = b->mem + (size_t)c * 128u;
        int32_t *x = b->regs + (size_t)c * NREG;
        if (c % 3 == 0) {
            int32_t n = 1 + c % 211;
            m[0] = (int32_t)ADDI(10, 0, 0);
            m[1] = (int32_t)ADDI(11, 0, 1);
            m[2] = (int32_t)ADDI(12, 0, n + 1);
            m[3] = (int32_t)ADD(10, 10, 11);
            m[4] = (int32_t)ADDI(11, 11, 1);
            m[5] = (int32_t)BLT(11, 12, -8);
            m[6] = (int32_t)XM3_HALT;
            continue;
        }
        for (w = 0; w < 128u; ++w) {
            m[w] = (int32_t)(w < 48u ? xinsn() : (uint32_t)(rnd() >> 32));
        }
        for (r = 1; r < NREG; ++r) {
            x[r] = r <= 8 ? (int32_t)(below(128u * 4u - 4u) & ~3u) : (int32_t)(uint32_t)(rnd() >> 32);
        }
    }
    return b;
}

typedef struct xroot {
    char label[96];
    char hex[65];
} xroot;

static void test_cross_backend(void)
{
    static const char *const names[] = {"cpu", "opencl", "webgpu"};
    xroot roots[24];
    int nroots = 0, i;
    size_t n;
    rbatch *seed = x_batch(), *ref = rb_copy(seed);
    char ref_hex[65];
    rb_reference(ref, 5, 173);
    rb_root(ref, ref_hex);
    printf("cross-backend batch: 4099 cores x 128 words, 5 slices x budget 173, local 64\n");
    printf("  %-56s %s\n", "rv32_ref (C reference)", ref_hex);
    for (n = 0; n < sizeof names / sizeof names[0]; ++n) {
        int32_t index = m3vcpu_backend_index(names[n]);
        m3vcpu_probe probe;
        int32_t d, count;
        if (index < 0 || m3vcpu_backend_probe(index, &probe) != M3VCPU_OK || !probe.available) {
            printf("  %-56s SKIP (not available)\n", names[n]);
            continue;
        }
        count = m3vcpu_backend_at(index)->device_count();
        for (d = 0; d < count && nroots < 24; ++d) {
            m3vcpu_session *saved = g_s;
            m3vcpu_device_info info;
            rbatch *b;
            uint64_t t0;
            if (m3vcpu_open(names[n], d, NULL, &g_s) != M3VCPU_OK) {
                CHECK(0, "open %s:%d: %s", names[n], (int)d, m3vcpu_last_error(NULL));
                g_s = saved;
                continue;
            }
            (void)m3vcpu_session_device(g_s, NULL, &info);
            m3_format(roots[nroots].label, sizeof roots[nroots].label, "%s:%d %s", names[n], (int)d, info.name);
            m3_format(g_label, sizeof g_label, "xbackend %s", roots[nroots].label);
            b = rb_copy(seed);
            t0 = m3_now_ms();
            if (rb_run(b, 5, 173, 64u) == 0) {
                rb_root(b, roots[nroots].hex);
                printf("  %-56s %s (%llu ms)\n", roots[nroots].label, roots[nroots].hex,
                       (unsigned long long)(m3_now_ms() - t0));
                CHECK(strcmp(roots[nroots].hex, ref_hex) == 0, "%s state root equals the C reference", g_label);
                ++nroots;
            }
            rb_free(b);
            m3vcpu_close(g_s);
            g_s = saved;
        }
    }
    for (i = 1; i < nroots; ++i) {
        CHECK(strcmp(roots[i].hex, roots[0].hex) == 0, "state root %s equals %s", roots[i].label, roots[0].label);
    }
    printf("cross-backend: %d session(s), %s\n", nroots,
           nroots > 0 && strcmp(roots[0].hex, ref_hex) == 0 ? "one root" : "MISMATCH");
    rb_free(ref);
    rb_free(seed);
}

int main(void)
{
    int32_t index = m3vcpu_backend_index("webgpu");
    m3vcpu_probe probe;
    int32_t d, count;
    if (index < 0) {
        printf("SKIP: backend 'webgpu' not registered\n");
        return 77;
    }
    if (m3vcpu_backend_probe(index, &probe) != M3VCPU_OK || !probe.available) {
        printf("SKIP: backend 'webgpu' unavailable: %s\n", probe.reason);
        return 77;
    }
    printf("backend webgpu (%s): %s\n", m3vcpu_backend_origin(index), probe.reason);
    count = m3vcpu_backend_at(index)->device_count();
    for (d = 0; d < count; ++d) {
        m3vcpu_device_info info;
        long before = m3t_checks, failed = m3t_failures;
        int32_t rc = m3vcpu_open("webgpu", d, NULL, &g_s);
        CHECK(rc == M3VCPU_OK, "open webgpu:%d: %s", (int)d, m3vcpu_last_error(NULL));
        if (rc != M3VCPU_OK) {
            continue;
        }
        (void)m3vcpu_session_device(g_s, NULL, &info);
        m3_format(g_label, sizeof g_label, "webgpu:%d %s", (int)d, info.name);
        printf("[%s] vendor %s | %s | %s | max_local %u | max_alloc %llu\n", g_label, info.vendor, info.version,
               info.isa, (unsigned)info.max_local, (unsigned long long)info.max_alloc_bytes);
        g_rng = 0xB0F0u + (uint64_t)d;
        test_buffers();
        test_wgsl_blobs();
        test_twin_admission();
        test_second_session(d);
        test_rv32_grid();
        printf("[%s] %ld checks, %ld failures\n", g_label, m3t_checks - before, m3t_failures - failed);
        fflush(stdout);
        m3vcpu_close(g_s);
        g_s = NULL;
    }
    test_cross_backend();
    return m3t_summary("m3vcpu_test_webgpu");
}
