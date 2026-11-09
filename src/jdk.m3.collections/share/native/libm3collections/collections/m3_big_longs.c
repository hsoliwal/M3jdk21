/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Bounded primitive-array operations. No retained Java pointers, references,
 * native heap allocations or independent payload ownership.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_big_longs.c blob 00435ac947b5 (com.synexia.primitives.PrimitiveBigNative -> jdk.internal.m3.collections.packed.PrimitiveBigNative, 4 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>
#include <stddef.h>

#define M3_BIG_LONG_CHUNK 256

static int m3_throw(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return 0;
    jclass error = (*env)->FindClass(env, type);
    if (error != NULL) {
        (*env)->ThrowNew(env, error, message);
        (*env)->DeleteLocalRef(env, error);
    }
    return 0;
}

static int m3_check_slice(JNIEnv *env, jlongArray array, jint offset, jint count) {
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return 0;
    if (array == NULL) {
        return m3_throw(env, "java/lang/NullPointerException", "null LONG array");
    }
    jsize length = (*env)->GetArrayLength(env, array);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return 0;
    if (offset < 0 || count < 0 || offset > length || count > length - offset) {
        return m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid LONG array slice");
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_packed_PrimitiveBigNative_abiNative(JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    return 1;
}

JNIEXPORT void JNICALL
Java_jdk_internal_m3_collections_packed_PrimitiveBigNative_copyChunkNative(
        JNIEnv *env, jclass type, jlongArray source, jint sourceOffset,
        jlongArray target, jint targetOffset, jint count) {
    (void)type;
    if (!m3_check_slice(env, source, sourceOffset, count)
            || !m3_check_slice(env, target, targetOffset, count)) return;
    if (count == 0) return;
    jboolean same = (*env)->IsSameObject(env, source, target);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
    if (same == JNI_TRUE && sourceOffset == targetOffset) return;
    int backward = same == JNI_TRUE && targetOffset > sourceOffset
            && targetOffset - sourceOffset < count;
    jlong buffer[M3_BIG_LONG_CHUNK];
    jint remaining = count;
    while (remaining > 0) {
        jint take = remaining < M3_BIG_LONG_CHUNK ? remaining : M3_BIG_LONG_CHUNK;
        jint relative = backward ? remaining - take : count - remaining;
        (*env)->GetLongArrayRegion(env, source, sourceOffset + relative, take, buffer);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
        (*env)->SetLongArrayRegion(env, target, targetOffset + relative, take, buffer);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
        remaining -= take;
    }
}

JNIEXPORT void JNICALL
Java_jdk_internal_m3_collections_packed_PrimitiveBigNative_fillChunkNative(
        JNIEnv *env, jclass type, jlongArray target, jint offset, jint count, jlong value) {
    (void)type;
    if (!m3_check_slice(env, target, offset, count)) return;
    if (count == 0) return;
    jlong buffer[M3_BIG_LONG_CHUNK];
    for (jint i = 0; i < M3_BIG_LONG_CHUNK; i++) buffer[i] = value;
    jint written = 0;
    while (written < count) {
        jint take = count - written;
        if (take > M3_BIG_LONG_CHUNK) take = M3_BIG_LONG_CHUNK;
        (*env)->SetLongArrayRegion(env, target, offset + written, take, buffer);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
        written += take;
    }
}

JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_packed_PrimitiveBigNative_indexOfChunkNative(
        JNIEnv *env, jclass type, jlongArray source, jint offset, jint count, jlong value) {
    (void)type;
    if (!m3_check_slice(env, source, offset, count)) return -1;
    jlong buffer[M3_BIG_LONG_CHUNK];
    jint read = 0;
    while (read < count) {
        jint take = count - read;
        if (take > M3_BIG_LONG_CHUNK) take = M3_BIG_LONG_CHUNK;
        (*env)->GetLongArrayRegion(env, source, offset + read, take, buffer);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return -1;
        for (jint i = 0; i < take; i++) {
            if (buffer[i] == value) return offset + read + i;
        }
        read += take;
    }
    return -1;
}
