/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 */
#include <jni.h>
#include <stdlib.h>
#include <string.h>

JNIEXPORT jint JNICALL Java_M3StringNativeProjectionTest_projectionMask(
        JNIEnv *env, jclass ignored, jstring value, jcharArray expected) {
    jsize length = (*env)->GetArrayLength(env, expected);
    if ((*env)->GetStringLength(env, value) != length) return -1;
    size_t bytes = (size_t) length * sizeof(jchar);
    jchar *wanted = (jchar *) malloc(bytes == 0 ? 1 : bytes);
    jchar *region = (jchar *) malloc(bytes == 0 ? 1 : bytes);
    if (wanted == NULL || region == NULL) {
        free(wanted);
        free(region);
        return -2;
    }
    (*env)->GetCharArrayRegion(env, expected, 0, length, wanted);
    if ((*env)->ExceptionCheck(env)) {
        free(wanted);
        free(region);
        return -3;
    }

    jint result = 0;
    jboolean is_copy = JNI_FALSE;
    const jchar *critical = (*env)->GetStringCritical(env, value, &is_copy);
    if (critical != NULL) {
        /* No allocation, blocking, or JNI callback occurs in this critical interval. */
        if (memcmp(critical, wanted, bytes) == 0) result |= 1;
        if (is_copy == JNI_TRUE) result |= 8;
        (*env)->ReleaseStringCritical(env, value, critical);
    }
    if ((*env)->ExceptionCheck(env)) {
        free(wanted);
        free(region);
        return -4;
    }

    is_copy = JNI_FALSE;
    const jchar *chars = (*env)->GetStringChars(env, value, &is_copy);
    if (chars != NULL) {
        if (memcmp(chars, wanted, bytes) == 0) result |= 2;
        if (is_copy == JNI_TRUE) result |= 16;
        (*env)->ReleaseStringChars(env, value, chars);
    }
    if ((*env)->ExceptionCheck(env)) {
        free(wanted);
        free(region);
        return -5;
    }
    (*env)->GetStringRegion(env, value, 0, length, region);
    if (!(*env)->ExceptionCheck(env) && memcmp(region, wanted, bytes) == 0) result |= 4;
    free(wanted);
    free(region);
    return result;
}
