/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

/* Stateless primitive lower_bound and span selection; validated immutable Java input. */
static void m3_ranked_span(const jint *suffixes, const jint *lcp, const jlong *cumulative,
                           jsize size, jlong rank, jint *out) {
    jsize lo = 0, hi = size;
    while (lo < hi) {
        jsize mid = lo + (hi - lo) / 2;
        if (cumulative[mid] < rank) lo = mid + 1;
        else hi = mid;
    }
    jlong previous = lo == 0 ? 0 : cumulative[lo - 1];
    out[0] = suffixes[lo];
    out[1] = lcp[lo] + (jint)(rank - previous);
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_tools_cpu_M3RankedByteSubstringsNative_select0(
        JNIEnv *env, jclass owner, jintArray suffixes, jintArray lcp,
        jlongArray cumulative, jlongArray ranks) {
    (void)owner;
    if (suffixes == NULL || lcp == NULL || cumulative == NULL || ranks == NULL) return NULL;
    jsize count = (*env)->GetArrayLength(env, ranks);
    jsize n = (*env)->GetArrayLength(env, suffixes);
    if ((*env)->GetArrayLength(env, lcp) != n ||
        (*env)->GetArrayLength(env, cumulative) != n || count > 32768) {
        jclass err = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (err != NULL) (*env)->ThrowNew(env, err, "invalid rank arrays");
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, count * 2);
    if (result == NULL || count == 0) return result;
    jint *s = NULL, *l = NULL, *out = NULL;
    jlong *p = NULL, *r = NULL;
    jintArray answer = NULL;
    s = (*env)->GetIntArrayElements(env, suffixes, NULL);
    if (s == NULL) goto finish;
    l = (*env)->GetIntArrayElements(env, lcp, NULL);
    if (l == NULL) goto finish;
    p = (*env)->GetLongArrayElements(env, cumulative, NULL);
    if (p == NULL) goto finish;
    r = (*env)->GetLongArrayElements(env, ranks, NULL);
    if (r == NULL) goto finish;
    out = (*env)->GetIntArrayElements(env, result, NULL);
    if (out == NULL) goto finish;
    for (jsize i = 0; i < count; i++) {
        if (n == 0 || r[i] < 1 || r[i] > p[n-1]) {
            jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
            if (error != NULL) (*env)->ThrowNew(env, error, "rank out of range");
            goto finish;
        }
        m3_ranked_span(s, l, p, n, r[i], out + i*2);
    }
    (*env)->ReleaseIntArrayElements(env, result, out, 0);
    out = NULL;
    answer = result;
finish:
    if (out != NULL) (*env)->ReleaseIntArrayElements(env, result, out, JNI_ABORT);
    if (r != NULL) (*env)->ReleaseLongArrayElements(env, ranks, r, JNI_ABORT);
    if (p != NULL) (*env)->ReleaseLongArrayElements(env, cumulative, p, JNI_ABORT);
    if (l != NULL) (*env)->ReleaseIntArrayElements(env, lcp, l, JNI_ABORT);
    if (s != NULL) (*env)->ReleaseIntArrayElements(env, suffixes, s, JNI_ABORT);
    return answer;
}
