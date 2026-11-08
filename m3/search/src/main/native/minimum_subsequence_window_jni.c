/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fast-search/src/main/native/minimum_subsequence_window_jni.c (blob 5db6498b1071) by M3_SEARCH_FORWARD v1.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

enum {
    M3_MAX_SOURCE = 1000000,
    M3_MAX_SEQUENCE = 4096,
    M3_MAX_WORK = 32000000
};

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

/* Private proof-only JNI export: no pointers or workspaces survive this call. */
JNIEXPORT jlong JNICALL
Java_jdk_m3_search_M3MinimumSubsequenceWindow_findNative(
        JNIEnv *env, jclass unused, jcharArray sourceArray, jcharArray sequenceArray) {
    (void) unused;
    jchar *source = NULL;
    jchar *sequence = NULL;
    jint *starts = NULL;
    jlong result = (jlong) -1;
    if (sourceArray == NULL || sequenceArray == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null minimum subsequence input");
        return (jlong) -1;
    }
    jsize n = (*env)->GetArrayLength(env, sourceArray);
    jsize m = (*env)->GetArrayLength(env, sequenceArray);
    if (n > M3_MAX_SOURCE || m > M3_MAX_SEQUENCE
            || (int64_t) n * (int64_t) m > M3_MAX_WORK) {
        m3_throw(env, "java/lang/IllegalArgumentException",
                 "minimum subsequence window budget exceeded");
        return (jlong) -1;
    }
    if (m == 0) return (jlong) 0;
    if (m > n) return (jlong) -1;

    starts = (jint *) malloc((size_t) m * sizeof(*starts));
    if (starts == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "subsequence DP allocation");
        goto done;
    }
    for (jsize j = 0; j < m; j++) starts[j] = -1;
    source = (*env)->GetCharArrayElements(env, sourceArray, NULL);
    if (source == NULL) goto done;
    sequence = (*env)->GetCharArrayElements(env, sequenceArray, NULL);
    if (sequence == NULL) goto done;

    /* A single jchar is not a byte or a Unicode scalar. Retain both acquisitions
     * and the existing allocation/failure ordering, including the absent case. */
    if (m == 1) {
        jchar wanted = sequence[0];
        for (jsize i = 0; i < n; i++) {
            if (source[i] == wanted) {
                result = ((jlong) i << 32) | (jlong) (uint32_t) (i + 1);
                break;
            }
        }
        goto done;
    }

    /* Reachable prefixes form one contiguous nonnegative state interval. */
    jsize reachable = 0;
    jsize end = 0;
    while (end < n && reachable < m) {
        jchar unit = source[end];
        /* j>0 has a reachable predecessor; j=0 must be refreshed last. */
        for (jsize j = reachable; j > 0; j--) {
            if (unit == sequence[j]) starts[j] = starts[j - 1];
        }
        if (unit == sequence[0]) starts[0] = end;
        if (starts[reachable] >= 0) reachable++;
        end++;
    }
    if (reachable < m) goto done;
    jint bestStart = starts[m - 1];
    jint bestEnd = end;
    jint bestLength = end - bestStart;

    /* A two-unit pattern needs only the previous first-unit index. Preserve
     * allocation/acquisition order; all exits still use the original cleanup. */
    if (bestLength != m && m == 2) {
        jchar first = sequence[0], terminal = sequence[1];
        jint latestFirst = starts[0];
        for (jsize i = end; i < n; i++) {
            jchar unit = source[i];
            if (unit == terminal) {
                jint length = i + 1 - latestFirst;
                if (length < bestLength) {
                    bestLength = length;
                    bestStart = latestFirst;
                    bestEnd = i + 1;
                    if (length == 2) break;
                }
            }
            /* Matching both units must not consume this source position twice. */
            if (unit == first) latestFirst = i;
        }
    } else if (bestLength != m) {
        for (jsize i = end; i < n; i++) {
            jchar unit = source[i];
            for (jsize j = m - 1; j > 0; j--) {
                if (unit == sequence[j]) starts[j] = starts[j - 1];
            }
            if (unit == sequence[0]) starts[0] = i;
            jint currentStart = starts[m - 1];
            jint length = i + 1 - currentStart;
            if (length < bestLength
                    || (length == bestLength && currentStart < bestStart)) {
                bestLength = length;
                bestStart = currentStart;
                bestEnd = i + 1;
                /* Exact minimum still exits through the original cleanup. */
                if (length == m) break;
            }
        }
    }
    result = ((jlong) bestStart << 32) | (jlong) (uint32_t) bestEnd;

done:
    if (sequence != NULL) {
        (*env)->ReleaseCharArrayElements(env, sequenceArray, sequence, JNI_ABORT);
    }
    if (source != NULL) {
        (*env)->ReleaseCharArrayElements(env, sourceArray, source, JNI_ABORT);
    }
    free(starts);
    return result;
}
