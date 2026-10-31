/* SPDX-License-Identifier: Apache-2.0 */
/*
 * test_core.c - HAL core: SHA-256 vectors, bundle integrity, registry, probe, selector grammar,
 * M3VCPU_BACKEND override, ABI validation, buffer bounds, kernel blob checks, launch guards.
 */
#include "m3vcpu.h"
#include "m3vcpu_check.h"
#include "m3vcpu_rv32im.h"

#include <stdio.h>
#include <string.h>

static void hex(const uint8_t *d, char *out)
{
    static const char digits[] = "0123456789abcdef";
    int i;
    for (i = 0; i < 32; ++i) {
        out[i * 2] = digits[d[i] >> 4];
        out[i * 2 + 1] = digits[d[i] & 15];
    }
    out[64] = '\0';
}

static void sha_vector(const char *msg, size_t n, const char *want)
{
    uint8_t d[32];
    char got[65];
    m3vcpu_sha256(msg, n, d);
    hex(d, got);
    CHECK(strcmp(got, want) == 0, "sha256(%zu bytes) = %s, want %s", n, got, want);
}

static void test_sha256(void)
{
    static char million[1000000];
    memset(million, 'a', sizeof million);
    sha_vector("", 0, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    sha_vector("abc", 3, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    sha_vector("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", 56,
               "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1");
    sha_vector(million, sizeof million, "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0");
}

static void test_bundle(void)
{
    const m3k_blob *blobs = NULL;
    size_t count = 0;
    size_t i;
    int saw_cl = 0, saw_cpu = 0;
    CHECK(m3vcpu_builtin_blobs(M3VCPU_RV32_KERNEL_ENTRY, &blobs, &count) == M3VCPU_OK && count == 2u,
          "built-in bundle has 2 blobs for m3k_rv32im_slice (got %zu)", count);
    for (i = 0; i < count; ++i) {
        uint8_t d[32];
        m3vcpu_sha256(blobs[i].bytes, blobs[i].size, d);
        CHECK(memcmp(d, blobs[i].sha256, 32) == 0, "blob %zu: CMake sha256 == core sha256", i);
        saw_cl |= blobs[i].format == M3K_OPENCL_C && blobs[i].min_isa == 120u;
        saw_cpu |= blobs[i].format == M3K_CPU_SYMBOL && blobs[i].size == strlen(M3VCPU_RV32_KERNEL_ENTRY);
    }
    CHECK(saw_cl && saw_cpu, "bundle carries OpenCL C 1.2 source and the CPU symbol");
    CHECK(m3vcpu_builtin_blobs("nope", &blobs, &count) == M3VCPU_E_NOT_FOUND, "unknown kernel id");
}

/* ---- a registered fake backend (always unavailable) ----------------------------------- */
static int32_t fake_probe(m3vcpu_probe *out)
{
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    m3_strlcpy(out->reason, "fake driver not installed", sizeof out->reason);
    return M3VCPU_OK;
}
static int32_t fake_count(void) { return 0; }
static int32_t fake_info(int32_t i, m3vcpu_device_info *o) { (void)i; (void)o; return M3VCPU_E_NO_DEVICE; }
static int32_t fake_ctx(int32_t d, const m3vcpu_ctx_opts *o, m3vcpu_ctx **c) { (void)d; (void)o; (void)c; return M3VCPU_E_UNAVAILABLE; }
static void fake_ctx_destroy(m3vcpu_ctx *c) { (void)c; }
static int32_t fake_alloc(m3vcpu_ctx *c, size_t n, uint32_t f, m3vcpu_buf **o) { (void)c; (void)n; (void)f; (void)o; return M3VCPU_E_UNAVAILABLE; }
static int32_t fake_write(m3vcpu_ctx *c, m3vcpu_buf *b, size_t o, const void *s, size_t n) { (void)c; (void)b; (void)o; (void)s; (void)n; return M3VCPU_E_UNAVAILABLE; }
static int32_t fake_read(m3vcpu_ctx *c, m3vcpu_buf *b, size_t o, void *d, size_t n) { (void)c; (void)b; (void)o; (void)d; (void)n; return M3VCPU_E_UNAVAILABLE; }
static void fake_free(m3vcpu_ctx *c, m3vcpu_buf *b) { (void)c; (void)b; }
static int32_t fake_load(m3vcpu_ctx *c, const m3k_blob *b, size_t n, const char *e, m3vcpu_kernel **o) { (void)c; (void)b; (void)n; (void)e; (void)o; return M3VCPU_E_UNAVAILABLE; }
static void fake_kfree(m3vcpu_ctx *c, m3vcpu_kernel *k) { (void)c; (void)k; }
static int32_t fake_launch(m3vcpu_ctx *c, m3vcpu_kernel *k, uint64_t g, uint32_t l, const m3vcpu_arg *a, uint32_t n) { (void)c; (void)k; (void)g; (void)l; (void)a; (void)n; return M3VCPU_E_UNAVAILABLE; }
static int32_t fake_finish(m3vcpu_ctx *c) { (void)c; return M3VCPU_OK; }
static const char *fake_err(const m3vcpu_ctx *c) { (void)c; return "fake"; }

static m3vcpu_backend_v1 fake = {M3VCPU_ABI_V1, (uint32_t)sizeof(m3vcpu_backend_v1), "fake", fake_probe, fake_count,
                                 fake_info, fake_ctx, fake_ctx_destroy, fake_alloc, fake_write, fake_read, fake_free,
                                 fake_load, fake_kfree, fake_launch, fake_finish, fake_err};

static void test_registry(void)
{
    m3vcpu_probe probe;
    m3vcpu_session *s = NULL;
    m3vcpu_device_info info;
    m3vcpu_backend_v1 bad;
    int32_t n = m3vcpu_backend_count();
    int32_t i;
    CHECK(m3vcpu_abi_version() == M3VCPU_ABI_V1, "abi version");
    CHECK(n >= 1, "at least the built-in backend (%d)", n);
    CHECK(strcmp(m3vcpu_backend_at(0)->name, "cpu") == 0 && strcmp(m3vcpu_backend_origin(0), "builtin") == 0,
          "backend 0 is the built-in cpu");
    CHECK(m3vcpu_backend_index("CPU") == 0, "names are case-insensitive");
    for (i = 0; i < n; ++i) {
        CHECK(m3vcpu_backend_probe(i, &probe) == M3VCPU_OK, "probe %d", i);
        printf("backend %d: %-8s origin=%s available=%d score=%d devices=%d reason=%s\n", i,
               m3vcpu_backend_at(i)->name, m3vcpu_backend_origin(i), probe.available, probe.score,
               probe.device_count, probe.reason);
    }
    CHECK(m3vcpu_backend_probe(0, &probe) == M3VCPU_OK && probe.available == 1 && probe.score == 100
              && probe.device_count == 1,
          "cpu probe: available, score 100, one device");
    CHECK(m3vcpu_backend_probe(n, &probe) == M3VCPU_E_NOT_FOUND, "probe out of range");

    bad = fake;
    bad.abi = 99u;
    CHECK(m3vcpu_register_backend(&bad) == M3VCPU_E_ABI, "abi 99 rejected");
    bad = fake;
    bad.struct_size = 8u;
    CHECK(m3vcpu_register_backend(&bad) == M3VCPU_E_ABI, "short struct_size rejected");
    bad = fake;
    bad.finish = NULL;
    CHECK(m3vcpu_register_backend(&bad) == M3VCPU_E_ABI, "null entry rejected");
    bad = fake;
    bad.name = "cpu";
    CHECK(m3vcpu_register_backend(&bad) == M3VCPU_E_INVALID_ARG, "duplicate name rejected");
    CHECK(m3vcpu_register_backend(&fake) == M3VCPU_OK, "fake backend registered");
    CHECK(m3vcpu_open("fake", -1, NULL, &s) == M3VCPU_E_UNAVAILABLE && s == NULL, "unavailable backend");
    CHECK(strstr(m3vcpu_last_error(NULL), "fake driver not installed") != NULL, "reason surfaces: %s",
          m3vcpu_last_error(NULL));

    CHECK(m3vcpu_open("cpu", -1, NULL, &s) == M3VCPU_OK, "open cpu: %s", m3vcpu_last_error(NULL));
    CHECK(m3vcpu_session_device(s, &i, &info) == M3VCPU_OK && i == 0 && info.kind == M3VCPU_DEVICE_CPU,
          "cpu device 0 kind CPU");
    printf("cpu device: %s | %s | isa %s | %u logical | %s\n", info.name, info.vendor, info.isa,
           (unsigned)info.compute_units, m3vcpu_last_error(s));
    m3vcpu_close(s);
    s = NULL;
    CHECK(m3vcpu_open("nope", -1, NULL, &s) == M3VCPU_E_NOT_FOUND, "unknown backend: %s", m3vcpu_last_error(NULL));
    CHECK(strstr(m3vcpu_last_error(NULL), "nope") != NULL, "error names the backend");
    CHECK(m3vcpu_open("nope, fake ,cpu", -1, NULL, &s) == M3VCPU_OK, "preference list falls through to cpu");
    m3vcpu_close(s);
    s = NULL;
    CHECK(m3vcpu_open("cpu:1", -1, NULL, &s) == M3VCPU_E_NO_DEVICE, "cpu:1 has no device");
    CHECK(m3vcpu_open("cpu:x", -1, NULL, &s) == M3VCPU_E_INVALID_ARG, "malformed selector");
    CHECK(m3vcpu_open("cpu", 0, NULL, NULL) == M3VCPU_E_INVALID_ARG, "null out");

    CHECK(m3_setenv("M3VCPU_BACKEND", "cpu") == 0, "setenv");
    CHECK(m3vcpu_open(NULL, -1, NULL, &s) == M3VCPU_OK && strcmp(m3vcpu_session_backend(s)->name, "cpu") == 0,
          "M3VCPU_BACKEND=cpu selects cpu");
    m3vcpu_close(s);
    s = NULL;
    CHECK(m3_setenv("M3VCPU_BACKEND", "nope") == 0, "setenv");
    CHECK(m3vcpu_open("auto", -1, NULL, &s) == M3VCPU_E_NOT_FOUND, "M3VCPU_BACKEND=nope overrides auto");
    CHECK(m3vcpu_open("cpu", -1, NULL, &s) == M3VCPU_OK, "explicit selector ignores M3VCPU_BACKEND");
    m3vcpu_close(s);
    s = NULL;
    CHECK(m3_setenv("M3VCPU_BACKEND", NULL) == 0, "unsetenv");
    CHECK(m3vcpu_open(NULL, -1, NULL, &s) == M3VCPU_OK, "auto opens the best available backend");
    printf("auto picked: %s\n", s == NULL ? "-" : m3vcpu_session_backend(s)->name);
    m3vcpu_close(s);
    s = NULL;
    {
        int32_t rc = m3vcpu_open("gpu", -1, NULL, &s);
        printf("gpu policy: %s %s\n", m3vcpu_status_name(rc), rc == M3VCPU_OK ? m3vcpu_session_backend(s)->name
                                                                              : m3vcpu_last_error(NULL));
        CHECK(rc == M3VCPU_OK || rc == M3VCPU_E_NO_DEVICE || rc == M3VCPU_E_UNAVAILABLE, "gpu policy status");
        m3vcpu_close(s);
        s = NULL;
    }
}

static void test_session(void)
{
    m3vcpu_session *s = NULL, *other = NULL;
    m3vcpu_buf *b = NULL, *foreign = NULL, *bufs[4];
    m3vcpu_kernel *k = NULL, *k2 = NULL;
    const m3k_blob *blobs = NULL;
    m3k_blob copy[2];
    size_t count = 0;
    uint8_t data[64], back[64];
    m3vcpu_arg args[4];
    int32_t config[3] = {4, 16, 10};
    m3vcpu_ctx_opts opts;
    m3vcpu_device_info info;
    int i;
    CHECK(m3vcpu_open("cpu", 0, NULL, &s) == M3VCPU_OK, "open");
    CHECK(m3vcpu_open("cpu", 0, NULL, &other) == M3VCPU_OK, "open second");
    CHECK(m3vcpu_buffer_alloc(s, 0, 0, &b) == M3VCPU_E_INVALID_ARG, "zero-size buffer rejected");
    CHECK(m3vcpu_buffer_alloc(s, 64, 9u, &b) == M3VCPU_E_INVALID_ARG, "unknown flags rejected");
    CHECK(m3vcpu_buffer_alloc(s, 64, 0, &b) == M3VCPU_OK, "alloc 64");
    CHECK(m3vcpu_buffer_read(s, b, 0, back, 64) == M3VCPU_OK, "read");
    for (i = 0; i < 64; ++i) {
        data[i] = (uint8_t)(i * 7 + 1);
        CHECK(back[i] == 0u, "buffers are zero-filled");
    }
    CHECK(m3vcpu_buffer_write(s, b, 0, data, 64) == M3VCPU_OK, "write");
    CHECK(m3vcpu_buffer_read(s, b, 0, back, 64) == M3VCPU_OK && memcmp(data, back, 64) == 0, "round trip");
    CHECK(m3vcpu_buffer_write(s, b, 60, data, 8) == M3VCPU_E_OUT_OF_RANGE, "write past end: %s",
          m3vcpu_last_error(s));
    CHECK(m3vcpu_buffer_read(s, b, 65, back, 0) == M3VCPU_E_OUT_OF_RANGE, "offset past end");
    CHECK(m3vcpu_buffer_write(s, b, (size_t)-1, data, 2) == M3VCPU_E_OUT_OF_RANGE, "offset overflow");
    CHECK(m3vcpu_buffer_alloc(other, 64, 0, &foreign) == M3VCPU_OK, "alloc in other session");
    CHECK(m3vcpu_buffer_write(s, foreign, 0, data, 4) == M3VCPU_E_INVALID_ARG, "foreign buffer rejected");

    CHECK(m3vcpu_kernel_builtin(s, "nope", &k) == M3VCPU_E_NOT_FOUND, "unknown builtin");
    CHECK(m3vcpu_kernel_builtin(s, M3VCPU_RV32_KERNEL_ENTRY, &k) == M3VCPU_OK, "builtin kernel");
    CHECK(m3vcpu_kernel_builtin(s, M3VCPU_RV32_KERNEL_ENTRY, &k2) == M3VCPU_OK && k2 == k, "builtin cached");
    CHECK(m3vcpu_builtin_blobs(M3VCPU_RV32_KERNEL_ENTRY, &blobs, &count) == M3VCPU_OK, "blobs");
    memcpy(copy, blobs, sizeof copy);
    copy[1].sha256[5] ^= 0x40u;
    CHECK(m3vcpu_kernel_load(s, copy, 2, M3VCPU_RV32_KERNEL_ENTRY, &k2) == M3VCPU_E_INTEGRITY,
          "tampered blob rejected: %s", m3vcpu_last_error(s));
    memcpy(copy, blobs, sizeof copy);
    CHECK(m3vcpu_kernel_load(s, &copy[0], 1, M3VCPU_RV32_KERNEL_ENTRY, &k2) == M3VCPU_E_NO_KERNEL_FORMAT,
          "cpu backend needs a CPU symbol blob");
    CHECK(m3vcpu_kernel_load(s, copy, 2, M3VCPU_RV32_KERNEL_ENTRY, &k2) == M3VCPU_OK && k2 != k, "explicit load");
    m3vcpu_kernel_free(s, k2);

    for (i = 0; i < 4; ++i) {
        size_t sizes[4] = {4u * 16u * 4u, 4u * 32u * 4u, 4u * 6u * 4u, 12u};
        if (i == 0) {
            sizes[0] = 4u * 16u * 4u - 4u; /* one word short */
        }
        CHECK(m3vcpu_buffer_alloc(s, sizes[i], 0, &bufs[i]) == M3VCPU_OK, "alloc arg %d", i);
        args[i].kind = M3VCPU_ARG_BUFFER;
        args[i].i32 = 0;
        args[i].buf = bufs[i];
    }
    CHECK(m3vcpu_buffer_write(s, bufs[3], 0, config, sizeof config) == M3VCPU_OK, "config");
    CHECK(m3vcpu_launch_1d(s, k, 4, 0, args, 4) == M3VCPU_E_OUT_OF_RANGE, "guard: memory too small: %s",
          m3vcpu_last_error(s));
    config[1] = 15;
    CHECK(m3vcpu_buffer_write(s, bufs[3], 0, config, sizeof config) == M3VCPU_OK, "config");
    CHECK(m3vcpu_launch_1d(s, k, 4, 0, args, 4) == M3VCPU_OK, "guard passes when geometry fits: %s",
          m3vcpu_last_error(s));
    CHECK(m3vcpu_launch_1d(s, k, 4, 0, args, 3) == M3VCPU_E_INVALID_ARG, "wrong argument count");
    args[1].buf = bufs[0];
    CHECK(m3vcpu_launch_1d(s, k, 4, 0, args, 4) == M3VCPU_E_INVALID_ARG, "aliased buffers rejected");
    args[1].buf = foreign;
    CHECK(m3vcpu_launch_1d(s, k, 4, 0, args, 4) == M3VCPU_E_INVALID_ARG, "foreign buffer argument rejected");
    args[1].buf = bufs[1];
    CHECK(m3vcpu_launch_1d(s, k, 0x80000000ull, 0, args, 4) == M3VCPU_E_INVALID_ARG, "global > 2^31-1");
    CHECK(m3vcpu_launch_1d(s, k, 4, 4096, args, 4) == M3VCPU_E_INVALID_ARG, "cpu local > 1024");
    CHECK(m3vcpu_launch_1d(other, k, 4, 0, args, 4) == M3VCPU_E_INVALID_ARG, "kernel of another session");
    CHECK(m3vcpu_finish(s) == M3VCPU_OK, "finish");
    m3vcpu_close(other);
    m3vcpu_close(s);

    CHECK(m3vcpu_open("cpu", 0, NULL, &s) == M3VCPU_OK, "open");
    (void)m3vcpu_session_device(s, NULL, &info);
    m3vcpu_close(s);
    memset(&opts, 0, sizeof opts);
    opts.struct_size = sizeof opts;
    opts.isa = M3VCPU_ISA_AVX512;
    s = NULL;
    i = m3vcpu_open("cpu", 0, &opts, &s);
    if (strcmp(info.isa, "avx512") == 0) {
        CHECK(i == M3VCPU_OK, "avx512 host accepts avx512");
    } else {
        CHECK(i == M3VCPU_E_UNAVAILABLE, "isa avx512 refused on a host without it: %s", m3vcpu_last_error(NULL));
    }
    m3vcpu_close(s);
    CHECK(strcmp(m3vcpu_status_name(M3VCPU_E_INTEGRITY), "M3VCPU_E_INTEGRITY") == 0, "status names");
    CHECK(strcmp(m3vcpu_status_name(-999), "M3VCPU_E_UNKNOWN") == 0, "unknown status name");
}

int main(void)
{
    test_sha256();
    test_bundle();
    test_registry();
    test_session();
    return m3t_summary("m3vcpu_test_core");
}
