/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 * JNI proof only: no native regex replacement and no retained string pointer.
 */
#include <jni.h>
#include <stdint.h>

#define OFFSET UINT64_C(14695981039346656037)
#define PRIME UINT64_C(1099511628211)

static uint64_t checksum(const jchar *units, jsize n) {
    uint64_t state = OFFSET;
    for (jsize i = 0; i < n; ++i) {
        state = (state ^ (uint64_t) units[i]) * PRIME;
    }
    return state;
}

JNIEXPORT jlongArray JNICALL
Java_M3StringRegexNativeBridgeTest_digests(JNIEnv *env, jclass unused, jstring text) {
    (void) unused;
    if (text == NULL) return NULL;
    const jsize length = (*env)->GetStringLength(env, text);
    /* Exercise critical access before any copied-string or region projection. */
    const jchar *critical = (*env)->GetStringCritical(env, text, NULL);
    if (critical == NULL) return NULL;
    const uint64_t pinned = checksum(critical, length);
    (*env)->ReleaseStringCritical(env, text, critical);
    const jchar *chars = (*env)->GetStringChars(env, text, NULL);
    if (chars == NULL) return NULL;
    const uint64_t copied = checksum(chars, length);
    (*env)->ReleaseStringChars(env, text, chars);
    uint64_t region = OFFSET;
    jchar local[128];
    for (jsize at = 0; at < length;) {
        jsize n = length - at;
        if (n > 128) n = 128;
        (*env)->GetStringRegion(env, text, at, n, local);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize j = 0; j < n; ++j) {
            region = (region ^ (uint64_t) local[j]) * PRIME;
        }
        at += n;
    }
    jlong digest[3] = {(jlong) copied, (jlong) pinned, (jlong) region};
    jlongArray output = (*env)->NewLongArray(env, 3);
    if (output == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, output, 0, 3, digest);
    return output;
}

JNIEXPORT jstring JNICALL
Java_M3StringRegexNativeBridgeTest_roundTrip(JNIEnv *env, jclass unused, jstring text) {
    (void) unused;
    if (text == NULL) return NULL;
    jsize length = (*env)->GetStringLength(env, text);
    const jchar *chars = (*env)->GetStringChars(env, text, NULL);
    if (chars == NULL) return NULL;
    jstring result = (*env)->NewString(env, chars, length);
    (*env)->ReleaseStringChars(env, text, chars);
    return result;
}
