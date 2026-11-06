// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/native/jni_bitap_exact.c (blob e6b743f37515) by M3_SEARCH_RELOCATE v2.
// Candidate-only Shift-And JNI sidecar. Not part of the active native provider ABI.
#include <jni.h>
#include <stdint.h>

static void throw_java(JNIEnv *env, const char *type_name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, type_name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static jint find_exact(JNIEnv *env, jbyteArray needle_array, jbyteArray haystack_array,
        jint from_index, int reverse, int count_only) {
    if (needle_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "needle");
        return -1;
    }
    if (haystack_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "haystack");
        return -1;
    }
    const jsize m = (*env)->GetArrayLength(env, needle_array);
    const jsize n = (*env)->GetArrayLength(env, haystack_array);
    if (m < 1 || m > 63) {
        throw_java(env, "java/lang/IllegalArgumentException", "needle length must be between 1 and 63");
        return -1;
    }
    const jsize maximum = n - m;
    if (maximum < 0 || (reverse && from_index < 0)) return count_only ? 0 : -1;
    const jint start = reverse ? 0 : (from_index < 0 ? 0 : from_index);
    if (!reverse && start > maximum) return count_only ? 0 : -1;
    const jsize end = reverse ? (from_index < maximum ? from_index : maximum) + m : n;

    /* Accepted needle lengths are 1..63. Read exactly those bytes into a
     * local buffer; never pin/copy the whole Java needle array or hold a
     * pointer across subsequent haystack operations. */
    jbyte needle[63];
    (*env)->GetByteArrayRegion(env, needle_array, 0, m, needle);
    if ((*env)->ExceptionCheck(env)) return -1;

    uint64_t masks[256] = {0};
    for (jsize i = 0; i < m; ++i) {
        masks[(uint8_t)needle[i]] |= UINT64_C(1) << (unsigned)i;
    }

    const uint64_t accepting_bit = UINT64_C(1) << (unsigned)(m - 1);
    uint64_t state = 0;
    jint result = count_only ? 0 : -1;

    /* Small inputs retain the original fast direct-access JNI path. */
    if (n <= 65536) {
        jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
        if (haystack == NULL) return -1; /* pending JNI OOME */
        for (jsize i = start; i < end; ++i) {
            state = ((state << 1U) | UINT64_C(1)) & masks[(uint8_t)haystack[i]];
            if ((state & accepting_bit) != 0) {
                if (count_only) ++result;
                else {
                    result = i - m + 1;
                    if (!reverse) break;
                }
            }
        }
        (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
        return result;
    }

    /* Large inputs never borrow/pin/copy the whole Java array. The bit-parallel
     * state survives chunk boundaries, preserving overlapping matches. */
    jbyte window[8192];
    for (jsize offset = start; offset < end;) {
        const jsize amount = end - offset < (jsize)sizeof(window)
                ? end - offset : (jsize)sizeof(window);
        (*env)->GetByteArrayRegion(env, haystack_array, offset, amount, window);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize local = 0; local < amount; ++local) {
            state = ((state << 1U) | UINT64_C(1)) & masks[(uint8_t)window[local]];
            if ((state & accepting_bit) != 0) {
                if (count_only) ++result;
                else {
                    result = offset + local - m + 1;
                    if (!reverse) return result;
                }
            }
        }
        offset += amount;
    }
    return result;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3BitapExactSearch_indexOfNative(
        JNIEnv *env, jclass owner, jbyteArray needle, jbyteArray haystack, jint from_index) {
    (void)owner;
    return find_exact(env, needle, haystack, from_index, 0, 0);
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3BitapExactSearch_lastIndexOfNative(
        JNIEnv *env, jclass owner, jbyteArray needle, jbyteArray haystack, jint from_index) {
    (void)owner;
    return find_exact(env, needle, haystack, from_index, 1, 0);
}

/* Probe-only additive JNI symbol. All borrowed arrays are released via JNI_ABORT. */
JNIEXPORT jint JNICALL Java_jdk_m3_search_M3BitapExactSearch_countOverlappingNative(
        JNIEnv *env, jclass owner, jbyteArray needle, jbyteArray haystack, jint from_index) {
    (void)owner;
    return find_exact(env, needle, haystack, from_index, 0, 1);
}
