// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/native/compression/m3_lz_jni.c blob 36c60955c34e; recipe m3/compression/recipe/compression-20261106/relocate.py v3
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* Pure native kernel; caller must prove every requested range is within source. */
size_t m3_lz_prefix(const uint8_t *bytes, size_t left, size_t right, size_t limit) {
    size_t offset = 0;
    while (limit - offset >= sizeof(uint64_t)) {
        uint64_t a;
        uint64_t b;
        memcpy(&a, bytes + left + offset, sizeof(a));
        memcpy(&b, bytes + right + offset, sizeof(b));
        if (a != b) {
            break;
        }
        offset += sizeof(uint64_t);
    }
    while (offset < limit && bytes[left + offset] == bytes[right + offset]) {
        offset++;
    }
    return offset;
}

#ifndef M3_LZ_STANDALONE
#include <jni.h>

#define M3_MAX_BLOCK ((jsize) (16 * 1024 * 1024))
#define M3_MAX_LANES ((jsize) 65536)

static void m3_throw(JNIEnv *env, const char *klass, const char *message) {
    jclass exception_class = (*env)->FindClass(env, klass);
    if (exception_class != NULL) {
        (*env)->ThrowNew(env, exception_class, message);
    }
}

JNIEXPORT jint JNICALL Java_jdk_internal_m3_compression_LzNative_nativeAbi(
        JNIEnv *env, jclass type) {
    (void) env;
    (void) type;
    return 1;
}

JNIEXPORT jintArray JNICALL Java_jdk_internal_m3_compression_LzNative_nativeMatches(
        JNIEnv *env, jclass type, jbyteArray input, jintArray left, jintArray right,
        jintArray maximum) {
    (void) type;
    if (input == NULL || left == NULL || right == NULL || maximum == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null native batch");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, left);
    jsize input_size = (*env)->GetArrayLength(env, input);
    if (n != (*env)->GetArrayLength(env, right)
            || n != (*env)->GetArrayLength(env, maximum)
            || n > M3_MAX_LANES || input_size > M3_MAX_BLOCK) {
        m3_throw(env, "java/lang/IllegalArgumentException", "invalid native batch shape");
        return NULL;
    }

    size_t lanes = (size_t) n;
    size_t bytes = (size_t) input_size;
    /* One aligned arena replaces the five independent per-call allocations.
     * No Java array pointer is retained or shared with another invocation. */
    const size_t int_alignment = _Alignof(jint);
    const size_t rounded_bytes = bytes + (int_alignment - bytes % int_alignment)
            % int_alignment;
    const size_t lane_bytes = (lanes == 0 ? 1 : lanes) * sizeof(jint);
    if (lane_bytes > (SIZE_MAX - rounded_bytes) / 4) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native LZ batch allocation overflow");
        return NULL;
    }
    uint8_t *arena = malloc(rounded_bytes + lane_bytes * 4);
    if (arena == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native LZ batch allocation");
        return NULL;
    }
    uint8_t *source = arena;
    jint *starts = (jint *) (void *) (arena + rounded_bytes);
    jint *probes = (jint *) (void *) ((uint8_t *) starts + lane_bytes);
    jint *limits = (jint *) (void *) ((uint8_t *) probes + lane_bytes);
    jint *results = (jint *) (void *) ((uint8_t *) limits + lane_bytes);
    jintArray out = NULL;
    (*env)->GetByteArrayRegion(env, input, 0, input_size, (jbyte *) source);
    (*env)->GetIntArrayRegion(env, left, 0, n, starts);
    (*env)->GetIntArrayRegion(env, right, 0, n, probes);
    (*env)->GetIntArrayRegion(env, maximum, 0, n, limits);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }
    for (jsize i = 0; i < n; i++) {
        jint limit = limits[i];
        if (limit < 0 || starts[i] < 0 || probes[i] < 0 || limit > input_size
                || starts[i] > input_size - limit || probes[i] > input_size - limit) {
            m3_throw(env, "java/lang/IllegalArgumentException", "native LZ range out of bounds");
            goto cleanup;
        }
        results[i] = (jint) m3_lz_prefix(source, (size_t) starts[i],
                (size_t) probes[i], (size_t) limit);
    }
    out = (*env)->NewIntArray(env, n);
    if (out != NULL) {
        (*env)->SetIntArrayRegion(env, out, 0, n, results);
    }
cleanup:
    free(arena);
    return out;
}
#endif
