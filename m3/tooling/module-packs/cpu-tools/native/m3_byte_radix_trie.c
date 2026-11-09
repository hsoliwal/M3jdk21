/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

#ifdef M3_RADIX_COUNT_PROBES
static unsigned long long m3_radix_compares;
#define M3_COMPARE() (m3_radix_compares++)
#else
#define M3_COMPARE() ((void)0)
#endif

/* Immutable CSR edge lanes: upper byte symbol, lower 24 bits child node. */
static int m3_radix_prefix_count(const jint *offset, jsize node_count,
                                 const jint *links, jsize edge_count,
                                 const jint *prefix, const jbyte *bytes,
                                 jsize bytes_count, jint start, jint length,
                                 jint *out) {
    if (start < 0 || length < 0 || start > bytes_count ||
            length > bytes_count - start || node_count < 1 || offset[0] != 0 ||
            offset[node_count] != edge_count) return -1;
    jsize node = 0;
    for (jint i = 0; i < length; i++) {
        jsize low = offset[node];
        jsize high = offset[node + 1];
        if (low < 0 || high < low || high > edge_count) return -1;
        const unsigned int target = (unsigned char)bytes[start + i];
        const jsize end = high;
        while (low < high) {
            const jsize mid = low + (high - low) / 2;
            M3_COMPARE();
            if (((uint32_t)links[mid] >> 24) < target) low = mid + 1;
            else high = mid;
        }
        if (low == end) { *out = 0; return 0; }
        M3_COMPARE();
        if (((uint32_t)links[low] >> 24) != target) { *out = 0; return 0; }
        const unsigned int child = (uint32_t)links[low] & UINT32_C(0x00ffffff);
        if (child >= (unsigned int)node_count) return -1;
        node = (jsize)child;
    }
    *out = prefix[node];
    return *out < 0 ? -1 : 0;
}

static void m3_invalid(JNIEnv *env) {
    jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (error != NULL) (*env)->ThrowNew(env, error, "invalid radix trie lanes");
}

/* Single new JNI export; existing M3JDK 21 and CPU tools ABI untouched. */
JNIEXPORT jintArray JNICALL Java_jdk_m3_tools_cpu_M3ByteRadixTrieNative_prefixCounts0(
        JNIEnv *env, jclass type, jintArray begins, jintArray links,
        jintArray totals, jbyteArray packed, jintArray starts, jintArray lengths) {
    (void)type;
    if (begins == NULL || links == NULL || totals == NULL ||
            packed == NULL || starts == NULL || lengths == NULL) {
        m3_invalid(env);
        return NULL;
    }
    const jsize nplus = (*env)->GetArrayLength(env, begins);
    const jsize nodes = (*env)->GetArrayLength(env, totals);
    const jsize links_count = (*env)->GetArrayLength(env, links);
    const jsize bytes_count = (*env)->GetArrayLength(env, packed);
    const jsize qn = (*env)->GetArrayLength(env, starts);
    if (nplus < 2 || nodes != nplus - 1 || qn != (*env)->GetArrayLength(env, lengths)) {
        m3_invalid(env);
        return NULL;
    }
    if (qn == 0) return (*env)->NewIntArray(env, 0);

    jint *node_starts = NULL, *edges = NULL, *counts = NULL;
    jint *query_starts = NULL, *query_lengths = NULL;
    jbyte *query_bytes = NULL;
    jint *output = NULL;
    jintArray result = NULL;
    int bad = 0;
    node_starts = (*env)->GetIntArrayElements(env, begins, NULL);
    if (node_starts == NULL) goto cleanup;
    edges = (*env)->GetIntArrayElements(env, links, NULL);
    if (edges == NULL) goto cleanup;
    counts = (*env)->GetIntArrayElements(env, totals, NULL);
    if (counts == NULL) goto cleanup;
    query_bytes = (*env)->GetByteArrayElements(env, packed, NULL);
    if (query_bytes == NULL) goto cleanup;
    query_starts = (*env)->GetIntArrayElements(env, starts, NULL);
    if (query_starts == NULL) goto cleanup;
    query_lengths = (*env)->GetIntArrayElements(env, lengths, NULL);
    if (query_lengths == NULL) goto cleanup;
    output = malloc((size_t)qn * sizeof(jint));
    if (output == NULL) {
        jclass memory_error = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (memory_error != NULL) (*env)->ThrowNew(env, memory_error, "native trie result");
        goto cleanup;
    }
    for (jsize i = 0; i < qn; i++) {
        if (m3_radix_prefix_count(node_starts, nodes, edges, links_count,
                counts, query_bytes, bytes_count, query_starts[i],
                query_lengths[i], &output[i]) != 0) {
            bad = 1;
            break;
        }
    }
cleanup:
    if (query_lengths != NULL) (*env)->ReleaseIntArrayElements(env, lengths, query_lengths, JNI_ABORT);
    if (query_starts != NULL) (*env)->ReleaseIntArrayElements(env, starts, query_starts, JNI_ABORT);
    if (query_bytes != NULL) (*env)->ReleaseByteArrayElements(env, packed, query_bytes, JNI_ABORT);
    if (counts != NULL) (*env)->ReleaseIntArrayElements(env, totals, counts, JNI_ABORT);
    if (edges != NULL) (*env)->ReleaseIntArrayElements(env, links, edges, JNI_ABORT);
    if (node_starts != NULL) (*env)->ReleaseIntArrayElements(env, begins, node_starts, JNI_ABORT);
    if (bad) {
        m3_invalid(env);
    } else if (output != NULL) {
        result = (*env)->NewIntArray(env, qn);
        if (result != NULL) (*env)->SetIntArrayRegion(env, result, 0, qn, output);
    }
    free(output);
    return result;
}
