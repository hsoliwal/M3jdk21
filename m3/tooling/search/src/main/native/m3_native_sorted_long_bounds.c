/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdbool.h>
#include <limits.h>
#include "jdk_m3_tooling_search_M3NativeSortedLongBounds.h"

#define M3_JNI_WINDOW 256
#define M3_JNI_CACHE_SLOTS 4
#define M3_JNI_CACHE_MASK (M3_JNI_CACHE_SLOTS - 1)
#define M3_EQUAL_PROBE_LIMIT 8

typedef struct M3LongWindow {
    JNIEnv *env;
    jlongArray sorted;
    jint from;
    jint to;
    jint first[M3_JNI_CACHE_SLOTS];
    jint last[M3_JNI_CACHE_SLOTS];
    jlong values[M3_JNI_CACHE_SLOTS][M3_JNI_WINDOW];
} M3LongWindow;

/* Four stack-owned, independently tagged JNI regions; no Java heap pointer is retained. */
static bool at(M3LongWindow *window, jint index, jlong *result) {
    jint page = (index - window->from) / M3_JNI_WINDOW;
    jint slot = page & M3_JNI_CACHE_MASK;
    if (index < window->first[slot] || index >= window->last[slot]) {
        jint start = window->from + page * M3_JNI_WINDOW;
        jint count = window->to - start;
        if (count > M3_JNI_WINDOW) {
            count = M3_JNI_WINDOW;
        }
        (*window->env)->GetLongArrayRegion(window->env, window->sorted,
                                             start, count, window->values[slot]);
        if ((*window->env)->ExceptionCheck(window->env)) {
            return false;
        }
        window->first[slot] = start;
        window->last[slot] = start + count;
    }
    *result = window->values[slot][index - window->first[slot]];
    return true;
}

static bool bound(M3LongWindow *window, jlong needle, bool upper, jint *result) {
    jint low = window->from;
    jint high = window->to;
    while (low < high) {
        jint middle = low + ((high - low) >> 1);
        jlong value;
        if (!at(window, middle, &value)) {
            return false;
        }
        if (value < needle || (upper && value == needle)) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    *result = low;
    return true;
}

/* At most eight sequential equal-key probes; fall back to binary search for longer runs. */
static bool upper_after(M3LongWindow *window, jlong key, jint lower, jint *result) {
    jint next = lower;
    jint inspected = 0;
    while (next < window->to && inspected < M3_EQUAL_PROBE_LIMIT) {
        jlong value;
        if (!at(window, next, &value)) {
            return false;
        }
        if (value != key) {
            *result = next;
            return true;
        }
        next++;
        inspected++;
    }
    if (next == window->to) {
        *result = next;
        return true;
    }
    jint low = next;
    jint high = window->to;
    while (low < high) {
        jint middle = low + ((high - low) >> 1);
        jlong value;
        if (!at(window, middle, &value)) {
            return false;
        }
        if (value <= key) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    *result = low;
    return true;
}

static void throw_illegal_argument(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL && !(*env)->ExceptionCheck(env)) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

/* Symbol name and descriptor are verified against javac -h generated header. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_tooling_search_M3NativeSortedLongBounds_nativeBounds(
        JNIEnv *env, jclass ignored, jlongArray sorted, jint from, jint to,
        jlongArray keys) {
    (void)ignored;
    if (sorted == NULL || keys == NULL) {
        throw_illegal_argument(env, "native bounds requires two non-null arrays");
        return NULL;
    }
    jsize source_length = (*env)->GetArrayLength(env, sorted);
    jsize key_count = (*env)->GetArrayLength(env, keys);
    if (from < 0 || to < from || to > source_length || key_count > INT_MAX / 2) {
        throw_illegal_argument(env, "invalid native bounds source range or result length");
        return NULL;
    }

    jintArray result = (*env)->NewIntArray(env, key_count * 2);
    if (result == NULL || (*env)->ExceptionCheck(env)) {
        return NULL;
    }
    M3LongWindow window = {.env = env, .sorted = sorted, .from = from, .to = to};
    jlong batch_keys[M3_JNI_WINDOW];
    jint pairs[2 * M3_JNI_WINDOW];
    /* One invocation-local memo; equality spans JNI query-region boundaries. */
    bool have_previous = false;
    jlong previous_key = 0;
    jint previous_lower = from;
    jint previous_upper = from;
    for (jsize offset = 0; offset < key_count;) {
        jsize count = key_count - offset;
        if (count > M3_JNI_WINDOW) {
            count = M3_JNI_WINDOW;
        }
        (*env)->GetLongArrayRegion(env, keys, offset, count, batch_keys);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        for (jsize i = 0; i < count; i++) {
            jlong key = batch_keys[i];
            if (have_previous && key == previous_key) {
                pairs[2 * i] = previous_lower;
                pairs[2 * i + 1] = previous_upper;
            } else {
                if (!bound(&window, key, false, &pairs[2 * i]) ||
                        !upper_after(&window, key, pairs[2 * i], &pairs[2 * i + 1])) {
                    return NULL;
                }
                previous_key = key;
                previous_lower = pairs[2 * i];
                previous_upper = pairs[2 * i + 1];
                have_previous = true;
            }
        }
        (*env)->SetIntArrayRegion(env, result, offset * 2, count * 2, pairs);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        offset += count;
    }
    return result;
}
