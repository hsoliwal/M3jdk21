/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

/*
 * The Java owner validates every suffix index, rank-min tree and query offset.
 * This native module never constructs or retains owner state, and compares bytes
 * unsigned (0..255) exactly as the Java suffix search owner does.
 */
static int m3_prefix_cmp(const jbyte *text, jsize n, jsize pos,
                         const jbyte *pattern, jsize len) {
    for (jsize i = 0; i < len; i++) {
        if (pos + i == n) return -1;
        unsigned int a = (unsigned char)text[pos + i];
        unsigned int b = (unsigned char)pattern[i];
        if (a != b) return a < b ? -1 : 1;
    }
    return 0;
}

static jsize m3_suffix_bound(const jbyte *text, jsize n, const jint *suffixes,
                             const jbyte *pattern, jsize len, int upper) {
    jsize lo = 0;
    jsize hi = n;
    while (lo < hi) {
        jsize mid = lo + (hi - lo) / 2;
        int difference = m3_prefix_cmp(text, n, suffixes[mid], pattern, len);
        if (difference < 0 || (upper && difference == 0)) lo = mid + 1;
        else hi = mid;
    }
    return lo;
}

static jint m3_suffix_rank_min(const jint *tree, jsize base, jsize lo, jsize hi) {
    lo += base;
    hi += base;
    jint best = 2147483647;
    while (lo < hi) {
        if ((lo & 1) != 0) {
            if (tree[lo] < best) best = tree[lo];
            lo++;
        }
        if ((hi & 1) != 0) {
            hi--;
            if (tree[hi] < best) best = tree[hi];
        }
        lo >>= 1;
        hi >>= 1;
    }
    return best;
}

/* out has exactly 2*queries slots; no object allocation or shared state. */
static void m3_suffix_query_batch(const jbyte *text, jsize n, const jint *suffixes,
                                   const jint *tree, jsize base, const jbyte *packed,
                                   const jint *offsets, jsize queries, jint *out) {
    for (jsize i = 0; i < queries; i++) {
        jsize from = offsets[i];
        jsize len = offsets[i + 1] - from;
        if (len == 0) {
            out[i * 2] = n + 1;
            out[i * 2 + 1] = 0;
        } else {
            const jbyte *needle = packed + from;
            jsize lo = m3_suffix_bound(text, n, suffixes, needle, len, 0);
            // If the lower bound is not a prefix match, the upper search is
            // redundant. This branch is the same as Java's miss fast path.
            if (lo == n || m3_prefix_cmp(text, n, suffixes[lo], needle, len) != 0) {
                out[i * 2] = 0;
                out[i * 2 + 1] = -1;
            } else {
                jsize hi = m3_suffix_bound(text, n, suffixes, needle, len, 1);
                out[i * 2] = hi - lo;
                out[i * 2 + 1] = m3_suffix_rank_min(tree, base, lo, hi);
            }
        }
    }
}

/* Sole JNI symbol of the new opt-in suffix index native library. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_tools_cpu_M3ByteSuffixIndexNative_query0(JNIEnv *env, jclass owner,
        jbyteArray text, jintArray suffixes, jintArray rankMin, jint leafBase,
        jbyteArray packed, jintArray offsets) {
    (void)owner;
    jsize n = (*env)->GetArrayLength(env, text);
    jsize queries = (*env)->GetArrayLength(env, offsets) - 1;
    jintArray result = (*env)->NewIntArray(env, queries * 2);
    if (result == NULL || queries == 0) return result;

    jbyte *textBytes = NULL;
    jint *suffixValues = NULL;
    jint *minValues = NULL;
    jbyte *queryBytes = NULL;
    jint *queryOffsets = NULL;
    jint *answers = NULL;
    jintArray answer = NULL;

    textBytes = (*env)->GetByteArrayElements(env, text, NULL);
    if (textBytes == NULL) goto finish;
    suffixValues = (*env)->GetIntArrayElements(env, suffixes, NULL);
    if (suffixValues == NULL) goto finish;
    minValues = (*env)->GetIntArrayElements(env, rankMin, NULL);
    if (minValues == NULL) goto finish;
    queryBytes = (*env)->GetByteArrayElements(env, packed, NULL);
    if (queryBytes == NULL) goto finish;
    queryOffsets = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (queryOffsets == NULL) goto finish;
    answers = (*env)->GetIntArrayElements(env, result, NULL);
    if (answers == NULL) goto finish;
    m3_suffix_query_batch(textBytes, n, suffixValues, minValues, leafBase,
                          queryBytes, queryOffsets, queries, answers);
    (*env)->ReleaseIntArrayElements(env, result, answers, 0);
    answers = NULL;
    answer = result;

finish:
    if (answers != NULL) (*env)->ReleaseIntArrayElements(env, result, answers, JNI_ABORT);
    if (queryOffsets != NULL) (*env)->ReleaseIntArrayElements(env, offsets, queryOffsets, JNI_ABORT);
    if (queryBytes != NULL) (*env)->ReleaseByteArrayElements(env, packed, queryBytes, JNI_ABORT);
    if (minValues != NULL) (*env)->ReleaseIntArrayElements(env, rankMin, minValues, JNI_ABORT);
    if (suffixValues != NULL) (*env)->ReleaseIntArrayElements(env, suffixes, suffixValues, JNI_ABORT);
    if (textBytes != NULL) (*env)->ReleaseByteArrayElements(env, text, textBytes, JNI_ABORT);
    return answer;
}
