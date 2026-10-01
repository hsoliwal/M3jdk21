/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdlib.h>
#include <string.h>

JNIEXPORT jstring JNICALL
Java_M3JoinedStringJni_roundTrip(JNIEnv *env, jclass type, jstring value, jint mode) {
    (void)type;
    jsize length = (*env)->GetStringLength(env, value);
    if ((*env)->ExceptionCheck(env)) return NULL;

    if (mode == 5) {
        jchar unused;
        (*env)->GetStringRegion(env, value, -1, 1, &unused);
        return NULL;
    }

    if (mode == 2 || mode == 4) {
        jsize size = (*env)->GetStringUTFLength(env, value);
        if ((*env)->ExceptionCheck(env)) return NULL;
        char *copy = malloc((size_t)size + 1U);
        if (copy == NULL) return NULL;
        if (mode == 2) {
            const char *bytes = (*env)->GetStringUTFChars(env, value, NULL);
            if (bytes == NULL) { free(copy); return NULL; }
            memcpy(copy, bytes, (size_t)size);
            (*env)->ReleaseStringUTFChars(env, value, bytes);
        } else {
            (*env)->GetStringUTFRegion(env, value, 0, length, copy);
            if ((*env)->ExceptionCheck(env)) { free(copy); return NULL; }
        }
        copy[size] = '\0';
        jstring result = (*env)->NewStringUTF(env, copy);
        free(copy);
        return result;
    }

    jchar *copy = malloc(((size_t)length + 1U) * sizeof(jchar));
    if (copy == NULL) return NULL;
    if (mode == 0) {
        const jchar *chars = (*env)->GetStringChars(env, value, NULL);
        if (chars == NULL) { free(copy); return NULL; }
        memcpy(copy, chars, (size_t)length * sizeof(jchar));
        (*env)->ReleaseStringChars(env, value, chars);
    } else if (mode == 1) {
        const jchar *chars = (*env)->GetStringCritical(env, value, NULL);
        if (chars == NULL) { free(copy); return NULL; }
        memcpy(copy, chars, (size_t)length * sizeof(jchar));
        (*env)->ReleaseStringCritical(env, value, chars);
    } else {
        (*env)->GetStringRegion(env, value, 0, length, copy);
        if ((*env)->ExceptionCheck(env)) { free(copy); return NULL; }
    }
    jstring result = (*env)->NewString(env, copy, length);
    free(copy);
    return result;
}
