/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

/*
 * One transition per batch; signed jlong order matches Java. The JVM can copy
 * the input arrays during GetArrayElements. No heap address escapes the call.
 * JNI_ABORT releases inputs; only the caller-allocated output is committed.
 */
static jsize bound(const jlong *values, jsize size, jlong needle, int upper) {
    jsize lo = 0;
    jsize hi = size;
    while (lo < hi) {
        jsize mid = lo + ((hi - lo) >> 1);
        if (upper ? values[mid] <= needle : values[mid] < needle) lo = mid + 1;
        else hi = mid;
    }
    return lo;
}

static void run_bounds(JNIEnv *env, jlongArray sorted, jlongArray queries,
                       jintArray results, int upper) {
    if (sorted == NULL || queries == NULL || results == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count != (*env)->GetArrayLength(env, results)) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "result length mismatch");
        return;
    }
    if (count == 0) return;

    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) return;
    jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }
    jint *positions = (*env)->GetIntArrayElements(env, results, NULL);
    if (positions == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }
    for (jsize i = 0; i < count; i++) {
        positions[i] = (jint) bound(values, size, targets[i], upper);
    }
    (*env)->ReleaseIntArrayElements(env, results, positions, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_lowerBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
    run_bounds(env, sorted, queries, results, 0);
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3LongRankBatchNative_upperBoundsAbi0(
        JNIEnv *env, jclass ignored) {
    (void) env;
    (void) ignored;
    return 1;
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_upperBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
    run_bounds(env, sorted, queries, results, 1);
}
