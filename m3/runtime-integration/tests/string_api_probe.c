/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

JNIEXPORT jstring JNICALL Java_StringApiProbe_roundTrip(
        JNIEnv *env, jclass cls, jstring s) {
    jsize n;
    const jchar *chars;
    jstring result;
    const char *utf;
    jstring utfCopy;
    (void) cls;

    n = (*env)->GetStringLength(env, s);
    chars = (*env)->GetStringChars(env, s, NULL);
    if (chars == NULL) return NULL;
    result = (*env)->NewString(env, chars, n);
    (*env)->ReleaseStringChars(env, s, chars);

    utf = (*env)->GetStringUTFChars(env, s, NULL);
    if (utf == NULL) return NULL;
    utfCopy = (*env)->NewStringUTF(env, utf);
    (*env)->ReleaseStringUTFChars(env, s, utf);
    if (utfCopy == NULL) return NULL;
    (*env)->DeleteLocalRef(env, utfCopy);
    return result;
}

JNIEXPORT jstring JNICALL Java_StringApiProbe_regionRoundTrip(
        JNIEnv *env, jclass cls, jstring s, jint start, jint length) {
    jchar empty = 0;
    jchar *chars = NULL;
    jstring result;
    (void) cls;

    if (length < 0 || (size_t) length > SIZE_MAX / sizeof(jchar)) return NULL;
    if (length != 0) {
        chars = (jchar *) malloc((size_t) length * sizeof(jchar));
        if (chars == NULL) return NULL;
        (*env)->GetStringRegion(env, s, start, length, chars);
        if ((*env)->ExceptionCheck(env)) {
            free(chars);
            return NULL;
        }
    }

    result = (*env)->NewString(env, length == 0 ? &empty : chars, length);
    free(chars);
    return result;
}
