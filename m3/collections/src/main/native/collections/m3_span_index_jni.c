/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Pure primitive JNI batch: Java predicate/comparator/native handles never cross.
 */
#include <jni.h>
#include <stddef.h>

static jint earliest_covering(jint target, const jint *starts, const jlong *prefix, jsize count) {
    if (target <= 0) return target < 0 ? 0 : target;
    jsize low = 0;
    jsize high = count;
    const jint prior = target - 1;
    while (low < high) {
        jsize middle = low + (high - low) / 2;
        if (starts[middle] <= prior) low = middle + 1;
        else high = middle;
    }
    jsize last = low - 1;
    if (last < 0 || prefix[last] < (jlong) target) return target;
    low = 0;
    high = last;
    while (low < high) {
        jsize middle = low + (high - low) / 2;
        if (prefix[middle] >= (jlong) target) high = middle;
        else low = middle + 1;
    }
    return starts[low];
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3SpanIndex_batchNative(
        JNIEnv *env, jclass unused, jintArray sorted_starts, jlongArray prefix_max,
        jintArray targets) {
    (void) unused;
    if (sorted_starts == NULL || prefix_max == NULL || targets == NULL) return NULL;
    jsize count = (*env)->GetArrayLength(env, sorted_starts);
    if (count != (*env)->GetArrayLength(env, prefix_max)) return NULL;
    jsize queries = (*env)->GetArrayLength(env, targets);
    jintArray result = (*env)->NewIntArray(env, queries);
    if (result == NULL) return NULL;

    jint *starts = (*env)->GetIntArrayElements(env, sorted_starts, NULL);
    if (starts == NULL) return NULL;
    jlong *prefix = (*env)->GetLongArrayElements(env, prefix_max, NULL);
    if (prefix == NULL) {
        (*env)->ReleaseIntArrayElements(env, sorted_starts, starts, JNI_ABORT);
        return NULL;
    }
    jint *requested = (*env)->GetIntArrayElements(env, targets, NULL);
    if (requested == NULL) {
        (*env)->ReleaseLongArrayElements(env, prefix_max, prefix, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, sorted_starts, starts, JNI_ABORT);
        return NULL;
    }
    jint *answers = (*env)->GetIntArrayElements(env, result, NULL);
    if (answers == NULL) {
        (*env)->ReleaseIntArrayElements(env, targets, requested, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, prefix_max, prefix, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, sorted_starts, starts, JNI_ABORT);
        return NULL;
    }
    for (jsize i = 0; i < queries; ++i) {
        answers[i] = earliest_covering(requested[i], starts, prefix, count);
    }
    (*env)->ReleaseIntArrayElements(env, result, answers, 0);
    (*env)->ReleaseIntArrayElements(env, targets, requested, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, prefix_max, prefix, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, sorted_starts, starts, JNI_ABORT);
    return result;
}
