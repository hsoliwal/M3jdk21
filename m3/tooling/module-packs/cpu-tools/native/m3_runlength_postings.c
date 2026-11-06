/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>

/* Private C17 operator, identical signed comparison semantics to Java. */
#ifdef M3_RUNLENGTH_PROBES
static unsigned long long m3_runlength_comparisons;
#define M3_LESS(a,b) (++m3_runlength_comparisons, (a) < (b))
#define M3_EQUAL(a,b) (++m3_runlength_comparisons, (a) == (b))
#else
#define M3_LESS(a,b) ((a) < (b))
#define M3_EQUAL(a,b) ((a) == (b))
#endif
static jint m3_run_count(const jint *values, const jint *ends, jsize runs, jint target) {
    jsize low = 0;
    jsize high = runs;
    while (low < high) {
        const jsize middle = low + (high - low) / 2;
        if (M3_LESS(values[middle], target)) low = middle + 1;
        else high = middle;
    }
    if (low == runs || !M3_EQUAL(values[low], target)) return 0;
    return ends[low] - (low == 0 ? 0 : ends[low - 1]);
}

/* Sorted query batches amortize distinct-run lookup with one monotone cursor.
 * Unsuitable or unsorted batches preserve the original independent binary lookup.
 * No input is sorted, copied, or reordered by the kernel.
 */
static void m3_batch_counts(const jint *values, const jint *ends, jsize runs,
                            const jint *asked, jsize queryCount, jint *output) {
    int sorted = queryCount >= 64 && queryCount >= runs / 2;
    if (sorted) {
        for (jsize i = 1; i < queryCount; ++i) {
            if (asked[i] < asked[i - 1]) {
                sorted = 0;
                break;
            }
        }
    }
    if (sorted) {
        jsize run = 0;
        for (jsize i = 0; i < queryCount; ++i) {
            const jint target = asked[i];
            while (run < runs && M3_LESS(values[run], target)) ++run;
            if (run < runs && M3_EQUAL(values[run], target)) {
                output[i] = ends[run] - (run == 0 ? 0 : ends[run - 1]);
            } else output[i] = 0;
        }
    } else {
        for (jsize i = 0; i < queryCount; ++i) {
            output[i] = m3_run_count(values, ends, runs, asked[i]);
        }
    }
}

/* No Java array pointer escapes this invocation, and no input is mutated. */
JNIEXPORT jintArray JNICALL
Java_com_m3_tools_cpu_M3RunLengthPostingIndexNative_batchCounts0(
        JNIEnv *env, jclass unused, jintArray uniqueValues,
        jintArray runEnds, jintArray queries) {
    (void) unused;
    if (uniqueValues == NULL || runEnds == NULL || queries == NULL) return NULL;
    const jsize runs = (*env)->GetArrayLength(env, uniqueValues);
    const jsize endsCount = (*env)->GetArrayLength(env, runEnds);
    const jsize queryCount = (*env)->GetArrayLength(env, queries);
    if (runs != endsCount) {
        jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exception != NULL) (*env)->ThrowNew(env, exception, "inconsistent run lanes");
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, queryCount);
    if (result == NULL || queryCount == 0 || runs == 0) return result;

    jint *values = (*env)->GetIntArrayElements(env, uniqueValues, NULL);
    if (values == NULL) return NULL;
    jint *ends = (*env)->GetIntArrayElements(env, runEnds, NULL);
    if (ends == NULL) {
        (*env)->ReleaseIntArrayElements(env, uniqueValues, values, JNI_ABORT);
        return NULL;
    }
    jint *asked = (*env)->GetIntArrayElements(env, queries, NULL);
    if (asked == NULL) {
        (*env)->ReleaseIntArrayElements(env, runEnds, ends, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, uniqueValues, values, JNI_ABORT);
        return NULL;
    }
    jint *output = (*env)->GetIntArrayElements(env, result, NULL);
    if (output == NULL) {
        (*env)->ReleaseIntArrayElements(env, queries, asked, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, runEnds, ends, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, uniqueValues, values, JNI_ABORT);
        return NULL;
    }
    m3_batch_counts(values, ends, runs, asked, queryCount, output);
    (*env)->ReleaseIntArrayElements(env, result, output, 0);
    (*env)->ReleaseIntArrayElements(env, queries, asked, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, runEnds, ends, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, uniqueValues, values, JNI_ABORT);
    return result;
}
