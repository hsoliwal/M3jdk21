/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <stdlib.h>
#include <limits.h>

/* Immutable validated primitive lanes only. No persistent JNI references or state. */
static int m3_select_triples(const jint *ids, const jint *offsets,
        const jint *starts, jsize document_count, const jint *lcp,
        const jlong *cumulative, jsize suffix_count,
        const jlong *ranks, jsize query_count, jint *answer) {
    if (query_count > INT_MAX / 3) return -1;
    for (jsize q = 0; q < query_count; q++) {
        const jlong requested = ranks[q];
        if (suffix_count == 0 || requested < 1 || requested > cumulative[suffix_count-1])
            return -1;
        jsize low = 0, high = suffix_count;
        while (low < high) {
            const jsize middle = low + (high - low) / 2;
            if (cumulative[middle] < requested) low = middle + 1;
            else high = middle;
        }
        if (low == suffix_count || ids[low] < 0 || ids[low] >= document_count)
            return -1;
        const jlong preceding = low == 0 ? 0 : cumulative[low-1];
        const jlong length = (jlong) lcp[low] + requested - preceding;
        const jlong relative = (jlong) offsets[low] - starts[ids[low]];
        if (length < 1 || length > INT_MAX || relative < 0 || relative > INT_MAX)
            return -1;
        answer[(size_t)q*3] = ids[low];
        answer[(size_t)q*3+1] = (jint)relative;
        answer[(size_t)q*3+2] = (jint)length;
    }
    return 0;
}

static void m3_invalid(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) (*env)->ThrowNew(env, type, "invalid rank selection lanes");
}

/* Sole added JNI export; does not change existing ranking/suffix native symbols. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_tools_cpu_M3MultiRankedByteSubstringsNative_select0(
        JNIEnv *env, jclass owner,
        jintArray doc_ids, jintArray offsets, jintArray starts,
        jintArray lcps, jlongArray sums, jlongArray query_ranks) {
    (void)owner;
    if (doc_ids == NULL || offsets == NULL || starts == NULL ||
            lcps == NULL || sums == NULL || query_ranks == NULL) {
        m3_invalid(env);
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, sums);
    jsize qn = (*env)->GetArrayLength(env, query_ranks);
    jsize docs = (*env)->GetArrayLength(env, starts);
    if (n != (*env)->GetArrayLength(env, doc_ids) ||
            n != (*env)->GetArrayLength(env, offsets) ||
            n != (*env)->GetArrayLength(env, lcps) ||
            qn > INT_MAX/3 || (qn != 0 && (n == 0 || docs == 0))) {
        m3_invalid(env);
        return NULL;
    }
    if (qn == 0) return (*env)->NewIntArray(env, 0);
    jint *id = NULL, *pos = NULL, *start = NULL, *lcp = NULL;
    jlong *cum = NULL, *query = NULL;
    jint *output = NULL;
    int invalid = 0;
    id = (*env)->GetIntArrayElements(env, doc_ids, NULL);
    if (id == NULL) goto cleanup;
    pos = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (pos == NULL) goto cleanup;
    start = (*env)->GetIntArrayElements(env, starts, NULL);
    if (start == NULL) goto cleanup;
    lcp = (*env)->GetIntArrayElements(env, lcps, NULL);
    if (lcp == NULL) goto cleanup;
    cum = (*env)->GetLongArrayElements(env, sums, NULL);
    if (cum == NULL) goto cleanup;
    query = (*env)->GetLongArrayElements(env, query_ranks, NULL);
    if (query == NULL) goto cleanup;
    output = malloc((size_t)qn * 3u * sizeof(jint));
    if (output == NULL) {
        jclass failure = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (failure != NULL) (*env)->ThrowNew(env, failure, "native rank output allocation");
        goto cleanup;
    }
    invalid = m3_select_triples(id, pos, start, docs, lcp, cum, n, query, qn, output);
cleanup:
    if (query != NULL) (*env)->ReleaseLongArrayElements(env, query_ranks, query, JNI_ABORT);
    if (cum != NULL) (*env)->ReleaseLongArrayElements(env, sums, cum, JNI_ABORT);
    if (lcp != NULL) (*env)->ReleaseIntArrayElements(env, lcps, lcp, JNI_ABORT);
    if (start != NULL) (*env)->ReleaseIntArrayElements(env, starts, start, JNI_ABORT);
    if (pos != NULL) (*env)->ReleaseIntArrayElements(env, offsets, pos, JNI_ABORT);
    if (id != NULL) (*env)->ReleaseIntArrayElements(env, doc_ids, id, JNI_ABORT);
    if (invalid != 0) { free(output); m3_invalid(env); return NULL; }
    if (output == NULL) return NULL; /* Existing JVM allocation exception. */
    jintArray result = (*env)->NewIntArray(env, qn * 3);
    if (result != NULL) (*env)->SetIntArrayRegion(env, result, 0, qn * 3, output);
    free(output);
    return result;
}
