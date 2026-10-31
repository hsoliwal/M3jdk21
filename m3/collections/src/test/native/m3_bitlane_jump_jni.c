/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
/* Test-only C17 JNI consumer of the public indexed bitmap navigation contract. */
#include <jni.h>
#include <limits.h>

#define CHUNK 128

static jint refuse(JNIEnv *env, const char *message) {
    jclass error = (*env)->FindClass(env, "java/lang/AssertionError");
    if (error != NULL) {
        (*env)->ThrowNew(env, error, message);
        (*env)->DeleteLocalRef(env, error);
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3BitLaneJumpProof_verifyNative(
        JNIEnv *env, jclass unused, jobject lane,
        jintArray probes, jintArray expectedNext, jintArray expectedPrevious) {
    (void) unused;
    if (lane == NULL || probes == NULL || expectedNext == NULL || expectedPrevious == NULL) {
        return refuse(env, "null bitmap/query/expected lane");
    }
    jsize length = (*env)->GetArrayLength(env, probes);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (length != (*env)->GetArrayLength(env, expectedNext)
            || length != (*env)->GetArrayLength(env, expectedPrevious)
            || length > (jsize) (INT_MAX / 2)) {
        return refuse(env, "unaligned or oversized JNI navigation query lanes");
    }
    jclass type = (*env)->GetObjectClass(env, lane);
    if (type == NULL) return 0;
    jmethodID next = (*env)->GetMethodID(env, type, "nextSetBit", "(I)I");
    jmethodID previous = next == NULL ? NULL :
            (*env)->GetMethodID(env, type, "previousSetBit", "(I)I");
    (*env)->DeleteLocalRef(env, type);
    if (next == NULL || previous == NULL) return 0;

    jint queryBuffer[CHUNK];
    jint nextBuffer[CHUNK];
    jint previousBuffer[CHUNK];
    jint checked = 0;
    for (jsize offset = 0; offset < length;) {
        jsize remaining = length - offset;
        jsize count = remaining < CHUNK ? remaining : CHUNK;
        (*env)->GetIntArrayRegion(env, probes, offset, count, queryBuffer);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->GetIntArrayRegion(env, expectedNext, offset, count, nextBuffer);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->GetIntArrayRegion(env, expectedPrevious, offset, count, previousBuffer);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jsize i = 0; i < count; i++) {
            jint observedNext = (*env)->CallIntMethod(env, lane, next, queryBuffer[i]);
            if ((*env)->ExceptionCheck(env)) return 0;
            jint observedPrevious = (*env)->CallIntMethod(env, lane, previous, queryBuffer[i]);
            if ((*env)->ExceptionCheck(env)) return 0;
            if (observedNext != nextBuffer[i] || observedPrevious != previousBuffer[i]) {
                return refuse(env, "JNI bitmap navigation differs from independent sorted oracle");
            }
            checked += 2;
        }
        offset += count;
    }
    return checked;
}
