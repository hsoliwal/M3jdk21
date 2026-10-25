/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <limits.h>
#include <stdint.h>

/*
 * LeetCode 34/35 (two insertion boundaries) + LeetCode 88 (merge of sorted
 * streams), adapted independently to primitive JNI batch search.
 * A monotone query lane admits a merge-style scan O(n + q) instead of
 * q separate O(log n) bisections. The adaptive cost gate retains binary
 * search when scanning the owner would be more expensive. No Java array,
 * JNI export, callback, ownership, exception policy or input mutation changes.
 */
static int m3_queries_non_decreasing(const jlong *targets, jsize count) {
    for (jsize i = 1; i < count; i++) {
        if (targets[i] < targets[i - 1]) return 0;
    }
    return 1;
}

static int m3_use_monotone_sweep(const jlong *targets, jsize count,
        jsize size, unsigned int bounds_per_query) {
    if (count < 2 || !m3_queries_non_decreasing(targets, count)) return 0;
    unsigned int levels = 0;
    for (jsize remaining = size; remaining > 0; remaining >>= 1) levels++;
    /* Cast before multiply: even Integer.MAX_VALUE Java lanes cannot overflow. */
    uint64_t estimated_binary = (uint64_t) (unsigned int) count
            * (uint64_t) levels * (uint64_t) bounds_per_query;
    return (uint64_t) (unsigned int) size <= estimated_binary;
}

/*
 * One JNI transition per batch, signed jlong order matching Java Long.compare.
 * Get/ReleaseArrayElements respects moving GC: no object addresses are retained.
 * Input arrays are released JNI_ABORT; only the preallocated output array is written.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_lowerBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
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

    if (m3_use_monotone_sweep(targets, count, size, 1U)) {
        jsize cursor = 0;
        for (jsize i = 0; i < count; i++) {
            while (cursor < size && values[cursor] < targets[i]) cursor++;
            positions[i] = (jint) cursor;
        }
    } else {
        for (jsize i = 0; i < count; i++) {
            jlong needle = targets[i];
            jsize lo = 0;
            jsize hi = size;
            while (lo < hi) {
                jsize mid = lo + ((hi - lo) >> 1);
                if (values[mid] < needle) lo = mid + 1;
                else hi = mid;
            }
            positions[i] = (jint) lo;
        }
    }
    (*env)->ReleaseIntArrayElements(env, results, positions, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}

/*
 * LeetCode 34 category, M3 contract: one sorted long[] plus a batch of
 * queries yields interleaved [lower, upper) insertion coordinates. Unlike
 * an inclusive "last occurrence" there is no target+1 overflow at LONG_MAX.
 * Inputs remain read-only under JNI_ABORT and no callback crosses JNI.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_equalRanges0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray ranges) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || ranges == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count > INT_MAX / 2 || (*env)->GetArrayLength(env, ranges) != count * 2) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "range output length mismatch");
        return;
    }
    if (count == 0) return;

    const jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) return;
    const jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
        return;
    }
    jint *result = (*env)->GetIntArrayElements(env, ranges, NULL);
    if (result == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, (jlong *) targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
        return;
    }
    if (m3_use_monotone_sweep(targets, count, size, 2U)) {
        jsize lower = 0;
        jsize upper = 0;
        for (jsize i = 0; i < count; i++) {
            const jlong needle = targets[i];
            while (lower < size && values[lower] < needle) lower++;
            if (upper < lower) upper = lower;
            /* Never use needle + 1: LONG_MAX remains valid without overflow. */
            while (upper < size && values[upper] <= needle) upper++;
            result[2 * i] = (jint) lower;
            result[2 * i + 1] = (jint) upper;
        }
    } else {
        for (jsize i = 0; i < count; ++i) {
            jlong needle = targets[i];
            jsize lo = 0;
            jsize hi = size;
            while (lo < hi) {
                jsize mid = lo + (hi - lo) / 2;
                if (values[mid] < needle) lo = mid + 1;
                else hi = mid;
            }
            jint first = (jint) lo;
            hi = size;
            while (lo < hi) {
                jsize mid = lo + (hi - lo) / 2;
                if (values[mid] <= needle) lo = mid + 1;
                else hi = mid;
            }
            result[2 * i] = first;
            result[2 * i + 1] = (jint) lo;
        }
    }
    (*env)->ReleaseIntArrayElements(env, ranges, result, 0);
    (*env)->ReleaseLongArrayElements(env, queries, (jlong *) targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
}
