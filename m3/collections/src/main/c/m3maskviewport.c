/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

static void raise(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
    }
}

static int matches(jint state, jint required, jint forbidden) {
    /* Unsigned casts make bit 31 behavior explicit and avoid signed shifts. */
    uint32_t value = (uint32_t) state;
    uint32_t yes = (uint32_t) required;
    uint32_t no = (uint32_t) forbidden;
    return (value & yes) == yes && (value & no) == 0U;
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
    jint *src = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (src == NULL) {
        return 0; /* JVM has an exception pending. */
    }
    jint *states = (*env)->GetIntArrayElements(env, flags, NULL);
    if (states == NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
        return 0;
    }
    jint selected = 0;
    for (jint i = first; i < first + length; ++i) {
        if (src[i] < 0) {
            raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
            goto release;
        }
        if (matches(states[i], required, forbidden)) {
            ++selected;
        }
    }
    if (selected > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        goto release;
    }
    if (selected == 0) {
        goto release;
    }
    /* Never mutate the destination until every possible input validation
       has succeeded, including aliasing of target with either input array. */
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        goto release;
    }
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        goto release;
    }
    jint cursor = 0;
    for (jint i = first; i < first + length; ++i) {
        if (matches(states[i], required, forbidden)) {
            scratch[cursor++] = src[i];
        }
    }
    /* Pinned/copy source elements must be released before the writeback,
       so target-source aliasing does not let an abort undo output. */
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
    free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : selected;
release:
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    return 0;
}
