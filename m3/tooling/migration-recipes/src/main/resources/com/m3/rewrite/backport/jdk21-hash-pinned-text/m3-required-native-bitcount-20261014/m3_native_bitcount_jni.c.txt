/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

_Static_assert(sizeof(jint) == sizeof(uint32_t), "JNI int must be 32 bits");

enum { M3_MAX_ITEMS = 1 << 18, M3_CHUNK_ITEMS = 2048 };

static void m3_throw(JNIEnv *env, const char *kind, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, kind);
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static jint m3_popcount(jint input) {
    uint32_t x = (uint32_t)input;
    x -= (x >> 1) & UINT32_C(0x55555555);
    x = (x & UINT32_C(0x33333333)) + ((x >> 2) & UINT32_C(0x33333333));
    x = (x + (x >> 4)) & UINT32_C(0x0f0f0f0f);
    x += x >> 8;
    x += x >> 16;
    return (jint)(x & UINT32_C(0x3f));
}

JNIEXPORT jint JNICALL
Java_com_m3_parallel_bulk_M3ParallelNativeBitCountProvider_nativeAbiVersion(
        JNIEnv *env, jclass owner) {
    (void)env;
    (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_parallel_bulk_M3ParallelNativeBitCountProvider_nativeBitCount(
        JNIEnv *env, jclass owner, jintArray input) {
    (void)owner;
    if (input == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "bitcount input");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, input);
    if (n < 1 || n > M3_MAX_ITEMS) {
        m3_throw(env, "java/lang/IllegalArgumentException", "native bitcount batch budget");
        return NULL;
    }
    jintArray output = (*env)->NewIntArray(env, n);
    if (output == NULL) return NULL;
    jint incoming[M3_CHUNK_ITEMS];
    jint outgoing[M3_CHUNK_ITEMS];
    for (jsize offset = 0; offset < n;) {
        jsize count = n - offset < M3_CHUNK_ITEMS ? n - offset : M3_CHUNK_ITEMS;
        (*env)->GetIntArrayRegion(env, input, offset, count, incoming);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < count; i++) outgoing[i] = m3_popcount(incoming[i]);
        (*env)->SetIntArrayRegion(env, output, offset, count, outgoing);
        if ((*env)->ExceptionCheck(env)) return NULL;
        offset += count;
    }
    return output;
}
