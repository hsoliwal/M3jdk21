/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/* A transfer bound, never a bound on the caller's logical window. */
enum { REGION_SIZE = 1024 };

static void raise(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
    }
}

static int matches(jint state, jint required, jint forbidden) {
    uint32_t value = (uint32_t) state;
    uint32_t yes = (uint32_t) required;
    uint32_t no = (uint32_t) forbidden;
    return (value & yes) == yes && (value & no) == 0U;
}

/* Never retain a pin or copy of an entire parent-owned array. */
static int read_region(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    (*env)->GetIntArrayRegion(env, flags, first, length, states);
    return !(*env)->ExceptionCheck(env);
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_project(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jintArray target, jint targetOffset) {
    (void) cls;
    if (offsets == NULL || flags == NULL || target == NULL) {
        raise(env, "java/lang/NullPointerException", "null projection array");
        return 0;
    }
    const jsize sourceSize = (*env)->GetArrayLength(env, offsets);
    const jsize flagsSize = (*env)->GetArrayLength(env, flags);
    const jsize outputSize = (*env)->GetArrayLength(env, target);
    if (sourceSize != flagsSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if (first < 0 || length < 0 || first > sourceSize - length ||
            targetOffset < 0 || targetOffset > outputSize) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
    jint src[REGION_SIZE], states[REGION_SIZE], output[REGION_SIZE];
    jint selected = 0;
    const jint end = first + length;
    /* Finish every validation before writing, including invalid unselected
       offsets in later regions and capacity failures after a full scan. */
    for (jint start = first; start < end;) {
        jint n = end - start < REGION_SIZE ? end - start : REGION_SIZE;
        if (!read_region(env, offsets, flags, start, n, src, states)) return 0;
        for (jint i = 0; i < n; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (matches(states[i], required, forbidden)) ++selected;
        }
        start += n;
    }
    if (selected > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (selected == 0) return 0;

    /* All sums are bounded by validated array lengths. Disjoint windows on
       one array are as safe to stream as different destination arrays. */
    const int overlaps = targetOffset < end && first < targetOffset + selected;
    const int snapshot = overlaps &&
            ((*env)->IsSameObject(env, target, offsets) ||
             (*env)->IsSameObject(env, target, flags));
    jint *scratch = NULL;
    if (snapshot) {
        if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
            raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
            return 0;
        }
        scratch = (jint *) malloc((size_t) selected * sizeof(jint));
        if (scratch == NULL) {
            raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
            return 0;
        }
    }
    jint cursor = 0;
    for (jint start = first; start < end;) {
        jint n = end - start < REGION_SIZE ? end - start : REGION_SIZE;
        if (!read_region(env, offsets, flags, start, n, src, states)) {
            free(scratch);
            return 0;
        }
        jint used = 0;
        for (jint i = 0; i < n; ++i) {
            if (matches(states[i], required, forbidden)) {
                if (snapshot) scratch[cursor + used] = src[i];
                else output[used] = src[i];
                ++used;
            }
        }
        if (!snapshot && used != 0) {
            (*env)->SetIntArrayRegion(env, target, targetOffset + cursor, used, output);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        cursor += used;
        start += n;
    }
    if (snapshot) {
        (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
        free(scratch);
    }
    return (*env)->ExceptionCheck(env) ? 0 : selected;
}
