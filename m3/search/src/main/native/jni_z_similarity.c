// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/native/jni_z_similarity.c (blob 8523d9a5aaf3) by M3_SEARCH_RELOCATE v2.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/*
 * Original C11 JNI realization of the canonical Java Z-array contract. Each jchar is
 * one UTF-16 code unit. No text normalization, Java callback, or retained JNI pointer.
 * All allocation and JNI failures return with a pending Java exception.
 */
JNIEXPORT jlong JNICALL Java_jdk_m3_search_M3ZSimilarity_sumNative(
        JNIEnv *env, jclass unused, jstring input) {
    (void)unused;
    if (input == NULL) {
        jclass type = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "input");
            (*env)->DeleteLocalRef(env, type);
        }
        return 0;
    }

    jsize n = (*env)->GetStringLength(env, input);
    if (n <= 1) return (jlong)n;
    if ((size_t)n > SIZE_MAX / sizeof(uint32_t)) {
        jclass type = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "Z-array capacity overflow");
            (*env)->DeleteLocalRef(env, type);
        }
        return 0;
    }
    uint32_t *z = (uint32_t *)calloc((size_t)n, sizeof(uint32_t));
    if (z == NULL) {
        jclass type = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "native Z-array allocation failed");
            (*env)->DeleteLocalRef(env, type);
        }
        return 0;
    }
    const jchar *chars = (*env)->GetStringChars(env, input, NULL);
    if (chars == NULL) {
        free(z);
        return 0;
    }

    /* A constant UTF-16 run has Z[i] = n-i for every suffix. Preserve the
     * existing allocation and String acquisition failure order, and release
     * both resources on this mathematically exact one-pass fast path. */
    jsize index = 1;
    while (index < n && chars[index] == chars[0]) ++index;
    if (index == n) {
        jlong length = (jlong)n;
        jlong uniform_sum = length * (length + 1) / 2;
        (*env)->ReleaseStringChars(env, input, chars);
        free(z);
        return uniform_sum;
    }

    /* A strict two-unit alternation has no odd-offset prefix matches. At
     * even offset 2*k the entire remaining suffix matches; use its exact
     * arithmetic sum. Preserve Z-array allocation and JNI pin failure order. */
    /* Rejection-only probes avoid an extra full scan on common near-periodic
     * tails/midpoints. A matching probe never admits the fast path by itself:
     * the complete ordered scan below remains the semantic authority. */
    if (index == 1 && n >= 64
            && chars[n - 1] == chars[(n - 1) & 1]
            && chars[n - 2] == chars[(n - 2) & 1]
            && chars[n / 2] == chars[(n / 2) & 1]
            && chars[n / 4] == chars[(n / 4) & 1]
            && chars[n - n / 4] == chars[(n - n / 4) & 1]) {
        jsize alternating = 2;
        while (alternating < n && chars[alternating] == chars[alternating & 1]) {
            ++alternating;
        }
        if (alternating == n) {
            jlong length = (jlong)n;
            jlong even_suffixes = (length - 1) / 2;
            jlong alternating_sum = length + even_suffixes * length
                    - even_suffixes * (even_suffixes + 1);
            (*env)->ReleaseStringChars(env, input, chars);
            free(z);
            return alternating_sum;
        }
    }

    jsize left = 0;
    jsize right = 0; /* Exclusive upper bound of the current Z-box. */
    jlong sum = (jlong)n; /* The full string is similar to itself by n units. */
    for (jsize i = 1; i < n; i++) {
        if (i < right) {
            uint32_t remaining = (uint32_t)(right - i);
            uint32_t mirrored = z[i - left];
            z[i] = remaining < mirrored ? remaining : mirrored;
        }
        while (i + (jsize)z[i] < n
                && chars[z[i]] == chars[i + (jsize)z[i]]) {
            z[i]++;
        }
        jsize end = i + (jsize)z[i];
        if (end > right) {
            left = i;
            right = end;
        }
        sum += (jlong)z[i];
    }

    (*env)->ReleaseStringChars(env, input, chars);
    free(z);
    return sum;
}
