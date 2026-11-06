/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

#ifdef M3_INTERVAL_PROBES
static unsigned long long m3_interval_comparisons;
#define M3_CHECK(expr) (++m3_interval_comparisons, (expr))
#else
#define M3_CHECK(expr) (expr)
#endif

/* Only normalized nonoverlapping alternating [start,end) pairs are admitted. */
static jbyte m3_interval_single(const jint *pairs, jsize count, jint value) {
    jsize low = 0, high = count;
    while (low < high) {
        const jsize mid = low + (high - low) / 2;
        if (M3_CHECK(pairs[2 * mid] <= value)) low = mid + 1;
        else high = mid;
    }
    return (jbyte)(low != 0 && M3_CHECK(value < pairs[2 * (low - 1) + 1]));
}

/* Preserve query order; monotone cursor only for a proven nondecreasing batch. */
static void m3_interval_batch(const jint *pairs, jsize intervals,
                              const jint *queries, jsize length, jbyte *output) {
    if (length < 64 || (jlong)length * 2 < intervals) {
        for (jsize q = 0; q < length; q++) {
            output[q] = m3_interval_single(pairs, intervals, queries[q]);
        }
        return;
    }
    /* The current prefix is proven nondecreasing as it is consumed. No pre-scan. */
    jsize cursor = 0;
    for (jsize q = 0; q < length; q++) {
        const jint value = queries[q];
        if (q != 0 && M3_CHECK(value < queries[q - 1])) {
            /* Prefix output is complete. The suffix has no ordering guarantee. */
            for (jsize tail = q; tail < length; tail++) {
                output[tail] = m3_interval_single(pairs, intervals, queries[tail]);
            }
            return;
        }
        while (cursor < intervals && M3_CHECK(pairs[2 * cursor + 1] <= value)) cursor++;
        output[q] = (jbyte)(cursor < intervals && M3_CHECK(pairs[2 * cursor] <= value)
                && M3_CHECK(value < pairs[2 * cursor + 1]));
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_m3_tools_cpu_M3IntIntervalSetNative_contains0(
        JNIEnv *env, jclass owner, jintArray intervals, jintArray queries) {
    (void)owner;
    if (intervals == NULL || queries == NULL) return NULL;
    const jsize endpointCount = (*env)->GetArrayLength(env, intervals);
    const jsize queryCount = (*env)->GetArrayLength(env, queries);
    jbyteArray result = (*env)->NewByteArray(env, queryCount);
    if (result == NULL || queryCount == 0) return result;
    jint *bounds = (*env)->GetIntArrayElements(env, intervals, NULL);
    if (bounds == NULL) return NULL;
    jint *values = (*env)->GetIntArrayElements(env, queries, NULL);
    if (values == NULL) {
        (*env)->ReleaseIntArrayElements(env, intervals, bounds, JNI_ABORT);
        return NULL;
    }
    jbyte *output = (*env)->GetByteArrayElements(env, result, NULL);
    if (output == NULL) {
        (*env)->ReleaseIntArrayElements(env, queries, values, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, intervals, bounds, JNI_ABORT);
        return NULL;
    }
    m3_interval_batch(bounds, endpointCount / 2, values, queryCount, output);
    (*env)->ReleaseByteArrayElements(env, result, output, 0);
    (*env)->ReleaseIntArrayElements(env, queries, values, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, intervals, bounds, JNI_ABORT);
    return result;
}
