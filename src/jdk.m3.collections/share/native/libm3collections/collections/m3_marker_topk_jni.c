/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Optional native exact-long-key top-K. Java owns direct buffers and their lifetime.
 * JNI boundary is one call, not one call per comparison or marker.
 */
/* Lineage: com.synexia 00bd2142b0ce synexia-primitives/native/m3_marker_topk_jni.c blob d4003b408e3b (com.synexia.primitives.M3MarkerTopKNative -> jdk.internal.m3.collections.sketch.MarkerTopKNative, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static void throw_exception(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) (*env)->ThrowNew(env, exception, message);
}
static int32_t read_id(const unsigned char *ptr, jint slot) {
    int32_t out;
    memcpy(&out, ptr + (size_t)slot * 4u, sizeof out);
    return out;
}
static int64_t read_key(const unsigned char *ptr, int32_t id) {
    int64_t out;
    memcpy(&out, ptr + (size_t)id * 8u, sizeof out);
    return out;
}
static int compare(const unsigned char *keys, int32_t a, int32_t b) {
    int64_t left = read_key(keys, a), right = read_key(keys, b);
    if (left < right) return -1;
    if (left > right) return 1;
    return (a > b) - (a < b);
}
static void sift_max(int32_t *heap, jint start, jint size, const unsigned char *keys) {
    int32_t value = heap[start];
    jint node = start;
    while (node < size / 2) {
        jint child = node * 2 + 1;
        if (child + 1 < size && compare(keys, heap[child], heap[child + 1]) < 0)
            child++;
        if (compare(keys, value, heap[child]) >= 0) break;
        heap[node] = heap[child];
        node = child;
    }
    heap[node] = value;
}
JNIEXPORT jintArray JNICALL
Java_jdk_internal_m3_collections_sketch_MarkerTopKNative_nativeSmallest(JNIEnv *env, jclass clazz,
        jobject idBuffer, jobject keyBuffer, jint count, jint k) {
    (void) clazz;
    jlong idCapacity = (*env)->GetDirectBufferCapacity(env, idBuffer);
    jlong keyCapacity = (*env)->GetDirectBufferCapacity(env, keyBuffer);
    const unsigned char *ids = (*env)->GetDirectBufferAddress(env, idBuffer);
    const unsigned char *keys = (*env)->GetDirectBufferAddress(env, keyBuffer);
    if (count < 0 || k < 0 || k > count || (jlong)count * 4 > idCapacity
            || ids == NULL || keys == NULL || keyCapacity < 0 || keyCapacity % 8 != 0) {
        throw_exception(env, "java/lang/IllegalArgumentException", "bad native Top-K buffer bounds");
        return NULL;
    }
    jintArray output = (*env)->NewIntArray(env, k);
    if (output == NULL || k == 0) return output;
    for (jint i = 0; i < count; i++) {
        int32_t id = read_id(ids, i);
        if (id < 0 || (jlong)id >= keyCapacity / 8) {
            throw_exception(env, "java/lang/IllegalArgumentException", "native Top-K ID outside key lane");
            return NULL;
        }
    }
    int32_t *heap = malloc((size_t) k * sizeof *heap);
    if (heap == NULL) {
        throw_exception(env, "java/lang/OutOfMemoryError", "native Top-K ID heap allocation");
        return NULL;
    }
    for (jint i = 0; i < k; i++) heap[i] = read_id(ids, i);
    for (jint i = k / 2; i > 0;) sift_max(heap, --i, k, keys);
    for (jint i = k; i < count; i++) {
        int32_t candidate = read_id(ids, i);
        if (compare(keys, candidate, heap[0]) < 0) {
            heap[0] = candidate;
            sift_max(heap, 0, k, keys);
        }
    }
    for (jint end = k - 1; end > 0; end--) {
        int32_t temp = heap[0]; heap[0] = heap[end]; heap[end] = temp;
        sift_max(heap, 0, end, keys);
    }
    (*env)->SetIntArrayRegion(env, output, 0, k, (jint *) heap);
    free(heap);
    return output;
}
