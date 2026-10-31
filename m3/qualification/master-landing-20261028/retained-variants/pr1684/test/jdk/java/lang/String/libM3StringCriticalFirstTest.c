/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

#include <jni.h>
#include <stdint.h>

JNIEXPORT jlongArray JNICALL Java_M3StringCriticalFirstTest_criticalFirst(
        JNIEnv *env, jclass unused, jstring text) {
    (void) unused;
    if (text == NULL) return NULL;
    jsize length = (*env)->GetStringLength(env, text);
    jboolean copied = JNI_FALSE;
    const jchar *units = (*env)->GetStringCritical(env, text, &copied);
    if (units == NULL) return NULL;
    uint64_t digest = UINT64_C(0xcbf29ce484222325);
    for (jsize index = 0; index < length; index++) {
        digest = (digest ^ units[index]) * UINT64_C(0x100000001b3);
    }
    // No allocation, blocking, or JNI call occurs in the critical region.
    (*env)->ReleaseStringCritical(env, text, units);
    jlong values[3] = {(jlong) digest, (jlong) length, (jlong) copied};
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 3, values);
    return result;
}
