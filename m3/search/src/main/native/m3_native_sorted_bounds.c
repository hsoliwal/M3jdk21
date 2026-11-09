/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 * M3 review-only C17 JNI batch search: binary-search bounds over one copied primitive region.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include "jdk_m3_search_NativeSortedIntBounds.h"

#define M3_KEY_BATCH 256

static void m3_throw(JNIEnv *env, const char *type_name, const char *message) {
    jclass type = (*env)->FindClass(env, type_name);
    if (type != NULL && !(*env)->ExceptionCheck(env)) {
        (void)(*env)->ThrowNew(env, type, message);
    }
    if (type != NULL) {
        (*env)->DeleteLocalRef(env, type);
    }
}

static jint m3_lower(const jint *source, jint length, jint from, jint needle) {
    jint low = 0;
    jint high = length;
    while (low < high) {
        jint mid = low + (high - low) / 2;
        if (source[mid] < needle) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return from + low;
}

static jint m3_upper(const jint *source, jint length, jint from, jint needle) {
    jint low = 0;
    jint high = length;
    while (low < high) {
        jint mid = low + (high - low) / 2;
        if (source[mid] <= needle) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return from + low;
}

/* Same overflow-safe comparison budget as the Java owner; no new JNI entrypoint. */
static int m3_merge_within_budget(jint range_length, jsize key_count) {
    uint32_t remaining = (uint32_t)range_length;
    unsigned int depth = 0;
    while (remaining != 0) {
        depth++;
        remaining >>= 1;
    }
    return key_count >= 128
            && (uint64_t)range_length <= (uint64_t)key_count * depth;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_NativeSortedIntBounds_nativeBounds(
        JNIEnv *env, jclass class_unused, jintArray sorted, jint from, jint to, jintArray keys) {
    (void)class_unused;
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    if (sorted == NULL || keys == NULL) {
        m3_throw(env, "java/lang/NullPointerException", sorted == NULL ? "sorted" : "keys");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, sorted);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    jsize key_count = (*env)->GetArrayLength(env, keys);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    if (from < 0 || to < from || to > length) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid sorted-array range");
        return NULL;
    }
    if (key_count > INT32_MAX / 2) {
        m3_throw(env, "java/lang/IllegalArgumentException", "query batch exceeds result space");
        return NULL;
    }
    /*
     * An empty query batch has an empty result regardless of the source
     * range. Do not snapshot/copy an unobserved primitive lane or allocate
     * off-heap region storage merely to return a zero-length Java array.
     */
    if (key_count == 0) {
        return (*env)->NewIntArray(env, 0);
    }
    jint range_length = to - from;
    /*
     * An empty source interval has [from, from) bounds for every key.
     * The query values are unobservable: allocate only the required Java
     * result, then fill it in bounded JNI writes when 'from' is nonzero.
     */
    if (range_length == 0) {
        jintArray output = (*env)->NewIntArray(env, key_count * 2);
        if (output == NULL || (*env)->ExceptionCheck(env)) {
            return NULL;
        }
        if (from == 0) {
            return output;
        }
        jint fill_window[M3_KEY_BATCH * 2];
        for (jsize i = 0; i < M3_KEY_BATCH * 2; i++) {
            fill_window[i] = from;
        }
        for (jsize start = 0; start < key_count;) {
            jsize count = key_count - start < M3_KEY_BATCH ? key_count - start : M3_KEY_BATCH;
            (*env)->SetIntArrayRegion(env, output, start * 2, count * 2, fill_window);
            if ((*env)->ExceptionCheck(env)) {
                return NULL;
            }
            start += count;
        }
        return output;
    }
    if ((uintmax_t)range_length > SIZE_MAX / sizeof(jint)) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native search region exceeds size_t");
        return NULL;
    }
    /* Allocation is owned by this invocation; it is not a Java heap pointer. */
    jint *region = range_length == 0 ? NULL : malloc((size_t)range_length * sizeof(jint));
    if (range_length != 0 && region == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "cannot allocate native search snapshot");
        return NULL;
    }
    if (range_length != 0) {
        (*env)->GetIntArrayRegion(env, sorted, from, range_length, region);
        if ((*env)->ExceptionCheck(env)) {
            free(region);
            return NULL;
        }
    }
    jintArray output = (*env)->NewIntArray(env, key_count * 2);
    if (output == NULL || (*env)->ExceptionCheck(env)) {
        free(region);
        return NULL;
    }
    jint key_window[M3_KEY_BATCH];
    jint output_window[M3_KEY_BATCH * 2];
    /* Detect a monotone query batch without retaining any Java-heap reference. */
    int monotone_keys = m3_merge_within_budget(range_length, key_count);
    jint previous = 0;
    int has_previous = 0;
    for (jsize start = 0; monotone_keys && start < key_count;) {
        jsize count = key_count - start < M3_KEY_BATCH ? key_count - start : M3_KEY_BATCH;
        (*env)->GetIntArrayRegion(env, keys, start, count, key_window);
        if ((*env)->ExceptionCheck(env)) {
            free(region);
            return NULL;
        }
        for (jsize i = 0; i < count; i++) {
            if (has_previous && key_window[i] < previous) {
                monotone_keys = 0;
                break;
            }
            previous = key_window[i];
            has_previous = 1;
        }
        start += count;
    }
    jint lower_cursor = 0;
    jint upper_cursor = 0;
    /* Reuse the detection state as an invocation-local previous-key memo.
     * Reset after the monotonicity probe; keep it across 256-key windows.
     * Binary-mode cursors hold absolute bounds, not merge-mode offsets.
     */
    has_previous = 0;
    for (jsize start = 0; start < key_count;) {
        jsize count = key_count - start < M3_KEY_BATCH ? key_count - start : M3_KEY_BATCH;
        (*env)->GetIntArrayRegion(env, keys, start, count, key_window);
        if ((*env)->ExceptionCheck(env)) {
            free(region);
            return NULL;
        }
        for (jsize i = 0; i < count; i++) {
            jint needle = key_window[i];
            if (monotone_keys) {
                while (lower_cursor < range_length && region[lower_cursor] < needle) {
                    lower_cursor++;
                }
                while (upper_cursor < range_length && region[upper_cursor] <= needle) {
                    upper_cursor++;
                }
                output_window[2 * i] = from + lower_cursor;
                output_window[2 * i + 1] = from + upper_cursor;
            } else {
                if (!has_previous || needle != previous) {
                    /* Sorted input guarantees upper bound >= lower bound. */
                    lower_cursor = m3_lower(region, range_length, from, needle);
                    jint offset = lower_cursor - from;
                    upper_cursor = m3_upper(region + offset,
                            range_length - offset, lower_cursor, needle);
                    previous = needle;
                    has_previous = 1;
                }
                output_window[2 * i] = lower_cursor;
                output_window[2 * i + 1] = upper_cursor;
            }
        }
        (*env)->SetIntArrayRegion(env, output, start * 2, count * 2, output_window);
        if ((*env)->ExceptionCheck(env)) {
            free(region);
            return NULL;
        }
        start += count;
    }
    free(region);
    return output;
}
