/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-m3-jmods/native/accelerator-pmem/m3pmem.c blob 6a49c7787c76; recipe m3/optane/recipe/pmem-accelerator-20261107/relocate.py v4
#define _GNU_SOURCE
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <sched.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>
#ifdef __linux__
#include <linux/stat.h>
#endif
#if defined(__x86_64__) || defined(__i386__)
#include <immintrin.h>
#endif

/* JNI handles remain private: Java synchronizes every operation with close. */
typedef struct { const int8_t *base; size_t bytes; int dax; } region;
#define JNI_NAME(name) Java_jdk_internal_m3_optane_M3PmemAccelerator_##name
static void fail(JNIEnv *env, const char *clazz, const char *message) {
    jclass type = (*env)->FindClass(env, clazz);
    if (type) { (*env)->ThrowNew(env, type, message); (*env)->DeleteLocalRef(env, type); }
}
static void invalid(JNIEnv *env, const char *m) { fail(env, "java/lang/IllegalArgumentException", m); }
static void bounds(JNIEnv *env) { fail(env, "java/lang/IndexOutOfBoundsException", "mapped region exceeded"); }
static void io(JNIEnv *env, const char *op) {
    char msg[240];
    snprintf(msg, sizeof(msg), "%s: %s", op, strerror(errno));
    fail(env, "java/io/IOException", msg);
}
static region *as_region(jlong h) { return (region *)(intptr_t)h; }
static int within(region *r, jlong offset, uint64_t length) {
    return r && offset >= 0 && (uint64_t)offset <= (uint64_t)r->bytes
        && length <= (uint64_t)r->bytes - (uint64_t)offset;
}
static int fsdax(int fd) {
#if defined(SYS_statx) && defined(STATX_ATTR_DAX) && defined(AT_EMPTY_PATH)
    struct statx s = {0};
    if (syscall(SYS_statx, fd, "", AT_EMPTY_PATH, STATX_BASIC_STATS, &s) == 0
            && (s.stx_attributes_mask & STATX_ATTR_DAX)) {
        return (s.stx_attributes & STATX_ATTR_DAX) ? 1 : 0;
    }
#else
    (void)fd;
#endif
    return -1;
}

JNIEXPORT jlong JNICALL JNI_NAME(nativeOpen)(JNIEnv *env, jclass owner, jbyteArray path_bytes) {
    (void)owner;
    if (!path_bytes) { invalid(env, "null path"); return 0; }
    jsize n = (*env)->GetArrayLength(env, path_bytes);
    if (n < 1 || n > 65535) { invalid(env, "path length"); return 0; }
    char *path = malloc((size_t)n + 1U);
    if (!path) { fail(env, "java/lang/OutOfMemoryError", "path allocation"); return 0; }
    (*env)->GetByteArrayRegion(env, path_bytes, 0, n, (jbyte *)path);
    if ((*env)->ExceptionCheck(env)) { free(path); return 0; }
    if (memchr(path, '\0', (size_t)n)) { free(path); invalid(env, "path contains NUL"); return 0; }
    path[n] = 0;
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    int saved = errno;
    free(path);
    if (fd < 0) { errno = saved; io(env, "open"); return 0; }
    struct stat st;
    if (fstat(fd, &st) != 0) { saved = errno; close(fd); errno = saved; io(env, "fstat"); return 0; }
    if (!S_ISREG(st.st_mode) || st.st_size <= 0 || (uint64_t)st.st_size > SIZE_MAX) {
        close(fd); invalid(env, "nonempty regular file required"); return 0;
    }
    size_t size = (size_t)st.st_size;
    int dax = fsdax(fd);
    void *base = mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);
    saved = errno;
    close(fd);
    if (base == MAP_FAILED) { errno = saved; io(env, "mmap"); return 0; }
    region *r = malloc(sizeof(*r));
    if (!r) { munmap(base, size); fail(env, "java/lang/OutOfMemoryError", "region allocation"); return 0; }
    *r = (region){(const int8_t *)base, size, dax};
    return (jlong)(intptr_t)r;
}
JNIEXPORT void JNICALL JNI_NAME(nativeClose)(JNIEnv *env, jclass owner, jlong handle) {
    (void)owner;
    region *r = as_region(handle);
    if (!r) return;
    int rc = munmap((void *)r->base, r->bytes);
    int saved = errno;
    free(r);
    if (rc) { errno = saved; io(env, "munmap"); }
}
JNIEXPORT jlong JNICALL JNI_NAME(nativeLength)(JNIEnv *env, jclass owner, jlong handle) {
    (void)owner;
    region *r = as_region(handle);
    if (!r) { invalid(env, "closed mapping"); return 0; }
    return (jlong)r->bytes;
}
JNIEXPORT jint JNICALL JNI_NAME(nativeDaxState)(JNIEnv *env, jclass owner, jlong handle) {
    (void)owner;
    region *r = as_region(handle);
    if (!r) { invalid(env, "closed mapping"); return -1; }
    return r->dax;
}
JNIEXPORT void JNICALL JNI_NAME(nativeRead)(JNIEnv *env, jclass owner, jlong handle,
        jlong offset, jbyteArray dst) {
    (void)owner;
    region *r = as_region(handle);
    if (!r || !dst) { invalid(env, "null or closed mapping"); return; }
    jsize n = (*env)->GetArrayLength(env, dst);
    if (!within(r, offset, (uint64_t)n)) { bounds(env); return; }
    (*env)->SetByteArrayRegion(env, dst, 0, n, (const jbyte *)(r->base + (size_t)offset));
}
static float dot_scalar(const int8_t *w, const float *x, int n) {
    float sum = 0.0f;
    for (int i = 0; i < n; i++) sum += (float)w[i] * x[i];
    return sum;
}
#if defined(__x86_64__) || defined(__i386__)
/* SIMD products, strictly ordered FP32 fold: same column order as Java
 * and the reference native scalar implementation; no FMA horizontal reduction.
 * Tested against cancellation-heavy signed Q8 x FP32 vectors. */
__attribute__((target("avx2")))
static float dot_avx2(const int8_t *w, const float *x, int n) {
    float sum = 0.0f;
    int i = 0;
    for (; i <= n - 8; i += 8) {
        __m128i packed = _mm_loadl_epi64((const __m128i *)(w + i));
        __m256 weights = _mm256_cvtepi32_ps(_mm256_cvtepi8_epi32(packed));
        __m256 products = _mm256_mul_ps(weights, _mm256_loadu_ps(x + i));
        float lanes[8];
        _mm256_storeu_ps(lanes, products);
        for (int lane = 0; lane < 8; lane++) sum += lanes[lane];
    }
    for (; i < n; i++) sum += (float)w[i] * x[i];
    return sum;
}
#endif
JNIEXPORT void JNICALL JNI_NAME(nativeMatVec)(JNIEnv *env, jclass owner, jlong handle,
        jlong offset, jint rows, jint cols, jfloat scale, jfloatArray input, jfloatArray output) {
    (void)owner;
    region *r = as_region(handle);
    if (!r || !input || !output || rows <= 0 || cols <= 0 || !isfinite(scale)) {
        invalid(env, "invalid matvec arguments"); return;
    }
    if ((*env)->GetArrayLength(env, input) != cols || (*env)->GetArrayLength(env, output) != rows) {
        invalid(env, "matrix/vector dimensions mismatch"); return;
    }
    uint64_t bytes = (uint64_t)(uint32_t)rows * (uint64_t)(uint32_t)cols;
    if (!within(r, offset, bytes)) { bounds(env); return; }
    if ((size_t)rows > SIZE_MAX/sizeof(float) || (size_t)cols > SIZE_MAX/sizeof(float)) {
        invalid(env, "matrix workspace overflow"); return;
    }
    /* Java owns float[rows]. Keep native output scratch to 64 floats and
     * avoid a second O(rows) allocation. Arithmetic, scale, and FP32
     * column order remain unchanged. Caller holds the Mapping lifetime lock. */
    float *x = malloc((size_t)cols * sizeof(float));
    if (!x) { fail(env, "java/lang/OutOfMemoryError", "matvec workspace"); return; }
    (*env)->GetFloatArrayRegion(env, input, 0, cols, x);
    if ((*env)->ExceptionCheck(env)) { free(x); return; }
    int avx2 = 0;
#if defined(__x86_64__) || defined(__i386__)
    __builtin_cpu_init();
    avx2 = __builtin_cpu_supports("avx2") && __builtin_cpu_supports("fma");
#endif
    float tile[64];
    int row = 0;
    while (row < rows) {
        int count = rows - row;
        if (count > 64) count = 64;
        for (int lane = 0; lane < count; lane++) {
            int index = row + lane;
            const int8_t *w = r->base + (size_t)offset + (size_t)index * (size_t)cols;
#if defined(__x86_64__) || defined(__i386__)
            float v = avx2 ? dot_avx2(w, x, cols) : dot_scalar(w, x, cols);
#else
            float v = dot_scalar(w, x, cols);
#endif
            tile[lane] = v * scale;
        }
        (*env)->SetFloatArrayRegion(env, output, row, count, tile);
        if ((*env)->ExceptionCheck(env)) break;
        row += count; /* Last partial tile also handles INT_MAX rows safely. */
    }
    free(x);
}
JNIEXPORT jint JNICALL JNI_NAME(nativeCpuFeatures)(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    int mask = 0;
#if defined(__x86_64__) || defined(__i386__)
    __builtin_cpu_init();
    if (__builtin_cpu_supports("avx2")) mask |= 1;
    if (__builtin_cpu_supports("fma")) mask |= 2;
    if (__builtin_cpu_supports("avx512f")) mask |= 4;
    if (__builtin_cpu_supports("avx512vnni")) mask |= 8;
#endif
    return mask;
}
JNIEXPORT void JNICALL JNI_NAME(nativePinNode)(JNIEnv *env, jclass owner, jint node) {
    (void)owner;
    if (node < 0 || node > 4096) { invalid(env, "invalid NUMA node"); return; }
    char file[128]; snprintf(file, sizeof(file), "/sys/devices/system/node/node%d/cpulist", node);
    FILE *f = fopen(file, "r");
    if (!f) { io(env, "node cpulist"); return; }
    char cpus[8192];
    if (!fgets(cpus, sizeof(cpus), f)) { int e = ferror(f) ? errno : EINVAL; fclose(f); errno=e; io(env, "read cpulist"); return; }
    fclose(f);
    cpu_set_t allowed, target;
    CPU_ZERO(&allowed); CPU_ZERO(&target);
    if (sched_getaffinity(0, sizeof(allowed), &allowed)) { io(env, "getaffinity"); return; }
    const char *p = cpus;
    while (*p) {
        while (*p == ',' || *p == ' ' || *p == '\n') p++;
        if (!*p) break;
        char *end;
        errno = 0;
        long lo = strtol(p, &end, 10);
        if (end == p || errno) { invalid(env, "invalid CPU list"); return; }
        p = end; long hi = lo;
        if (*p == '-') {
            p++;
            hi = strtol(p, &end, 10);
            if (end == p || errno) { invalid(env, "invalid CPU range"); return; }
            p = end;
        }
        if (lo < 0 || hi < lo || hi > 1048576 || (*p && *p != ',' && *p != '\n' && *p != ' ')) {
            invalid(env, "invalid CPU interval"); return;
        }
        for (long cpu=lo; cpu<=hi && cpu<CPU_SETSIZE; cpu++)
            if (CPU_ISSET((int)cpu, &allowed)) CPU_SET((int)cpu, &target);
    }
    if (!CPU_COUNT(&target)) { errno = ENODEV; io(env, "node has no permitted CPUs"); return; }
    if (sched_setaffinity(0, sizeof(target), &target)) io(env, "setaffinity");
}
