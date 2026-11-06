/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fast-search/src/main/native/minimum_subsequence_window_jni.c (blob c6b4b8ab6b2d) by M3_SEARCH_RELOCATE v2.
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

    jint bestStart = -1;
    jint bestEnd = 0;
    jint bestLength = INT32_MAX;
    for (jsize i = 0; i < n; i++) {
        jchar unit = source[i];
        for (jsize j = m; j-- > 0;) {
            if (unit == sequence[j]) {
                if (j == 0) {
                    starts[0] = i;
                } else if (starts[j - 1] >= 0) {
                    starts[j] = starts[j - 1];
                }
            }
        }
        jint currentStart = starts[m - 1];
        if (currentStart >= 0) {
            jint length = i + 1 - currentStart;
            if (length < bestLength
                    || (length == bestLength && currentStart < bestStart)) {
                bestLength = length;
                bestStart = currentStart;
                bestEnd = i + 1;
            }
        }
    }
    if (bestStart >= 0) {
        result = ((jlong) bestStart << 32) | (jlong) (uint32_t) bestEnd;
    }
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
