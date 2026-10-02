/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) { (*env)->ThrowNew(env, cls, message); (*env)->DeleteLocalRef(env, cls); }
}
static int input(JNIEnv *env, jstring value) {
    if (value == NULL) { fail(env, "java/lang/NullPointerException", "value"); return 0; }
    if ((*env)->GetStringLength(env, value) > INT_MAX / 3) {
        fail(env, "java/lang/OutOfMemoryError", "probe modified UTF-8 bound"); return 0;
    }
    return 1;
}
JNIEXPORT jstring JNICALL Java_com_m3_text_ProjectionProbe_roundTrip
  (JNIEnv *env, jclass cls, jstring value, jint mode) {
    (void) cls;
    if (!input(env, value)) return NULL;
    if (mode == 2) {
        jsize length = (*env)->GetStringUTFLength(env, value);
        char *copy = malloc((size_t) length + 1U);
        if (copy == NULL) { fail(env, "java/lang/OutOfMemoryError", "modified UTF-8"); return NULL; }
        const char *bytes = (*env)->GetStringUTFChars(env, value, NULL);
        if (bytes == NULL) { free(copy); return NULL; }
        // The JNI specification does not promise a terminator on acquired strings.
        memcpy(copy, bytes, (size_t) length);
        copy[length] = '\0';
        (*env)->ReleaseStringUTFChars(env, value, bytes);
        jstring result = (*env)->NewStringUTF(env, copy);
        free(copy);
        return result;
    }
    if (mode != 0 && mode != 1) {
        fail(env, "java/lang/IllegalArgumentException", "mode"); return NULL;
    }
    jsize length = (*env)->GetStringLength(env, value);
    if ((size_t) length > SIZE_MAX / sizeof(jchar)) {
        fail(env, "java/lang/OutOfMemoryError", "size overflow"); return NULL;
    }
    size_t size = (size_t) length * sizeof(jchar);
    jchar *copy = malloc(size == 0 ? sizeof(jchar) : size);
    if (copy == NULL) { fail(env, "java/lang/OutOfMemoryError", "projection"); return NULL; }
    const jchar *chars = mode == 0 ? (*env)->GetStringChars(env, value, NULL)
                                  : (*env)->GetStringCritical(env, value, NULL);
    if (chars == NULL) { free(copy); return NULL; }
    // No JNI calls, allocations, I/O or waits while the critical pointer is held.
    if (size != 0) memcpy(copy, chars, size);
    if (mode == 0) (*env)->ReleaseStringChars(env, value, chars);
    else (*env)->ReleaseStringCritical(env, value, chars);
    jstring result = (*env)->NewString(env, copy, length);
    free(copy);
    return result;
}
JNIEXPORT jbyteArray JNICALL Java_com_m3_text_ProjectionProbe_modifiedUtf8
  (JNIEnv *env, jclass cls, jstring value) {
    (void) cls;
    if (!input(env, value)) return NULL;
    jsize length = (*env)->GetStringUTFLength(env, value);
    const char *bytes = (*env)->GetStringUTFChars(env, value, NULL);
    if (bytes == NULL) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, length);
    if (result != NULL) (*env)->SetByteArrayRegion(env, result, 0, length, (const jbyte *) bytes);
    (*env)->ReleaseStringUTFChars(env, value, bytes);
    return result;
}
JNIEXPORT jstring JNICALL Java_com_m3_text_ProjectionProbe_region
  (JNIEnv *env, jclass cls, jstring value, jint start, jint length) {
    (void) cls;
    if (!input(env, value)) return NULL;
    jsize total = (*env)->GetStringLength(env, value);
    if (start < 0 || length < 0 || start > total || length > total - start) {
        fail(env, "java/lang/StringIndexOutOfBoundsException", "region"); return NULL;
    }
    if ((size_t) length > SIZE_MAX / sizeof(jchar)) {
        fail(env, "java/lang/OutOfMemoryError", "size overflow"); return NULL;
    }
    size_t size = (size_t) length * sizeof(jchar);
    jchar *copy = malloc(size == 0 ? sizeof(jchar) : size);
    if (copy == NULL) { fail(env, "java/lang/OutOfMemoryError", "region"); return NULL; }
    (*env)->GetStringRegion(env, value, start, length, copy);
    if ((*env)->ExceptionCheck(env)) { free(copy); return NULL; }
    jstring result = (*env)->NewString(env, copy, length);
    free(copy);
    return result;
}
