// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

/* Optional symbol within the existing SHA-256-pinned LfCount JNI library.
 * No JVM heap pin or pointer survives the call, no shadow string store is created. */
JNIEXPORT jlongArray JNICALL
Java_com_m3_runtime_capability_ByteHistogram256_histogram0(
        JNIEnv *env, jclass owner, jobject buffer, jint start, jint length) {
    (void) owner;
    if (buffer == NULL || start < 0 || length < 0 || length > 16777216) {
        jclass err = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (err != NULL) (void)(*env)->ThrowNew(env, err, "histogram input bounds");
        return NULL;
    }
    const jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)start + (jlong)length > capacity) {
        jclass err = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (err != NULL) (void)(*env)->ThrowNew(env, err, "histogram direct buffer bounds");
        return NULL;
    }
    const uint8_t *bytes = (const uint8_t *)(*env)->GetDirectBufferAddress(env, buffer);
    if (bytes == NULL && length != 0) {
        jclass err = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (err != NULL) (void)(*env)->ThrowNew(env, err, "histogram direct buffer missing");
        return NULL;
    }
    jlong bins[256] = {0};
    for (jint i = 0; i < length; ++i) bins[bytes[(size_t)start + (size_t)i]]++;
    jlongArray result = (*env)->NewLongArray(env, 256);
    if (result == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, result, 0, 256, bins);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return result;
}
