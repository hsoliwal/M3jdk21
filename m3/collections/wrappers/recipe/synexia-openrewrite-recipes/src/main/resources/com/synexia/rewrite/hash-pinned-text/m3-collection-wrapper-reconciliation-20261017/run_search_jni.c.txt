/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * Derived from the existing Synexia verification-only RUN navigation peer.
 * Bounded region validation and answer selection in one pass. Not a production
 * map backend. Input must stay stable during the call; no atomic snapshot claim.
 */
#include "RunSearchJni.h"
#include <stdint.h>
#define RUNS_PER_CHUNK 512
static jint reject(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return -1;
}
JNIEXPORT jint JNICALL Java_RunSearchJni_first(JNIEnv *env, jclass type,
        jintArray array, jint runs, jint value) {
    (void)type;
    if (array == NULL) return reject(env, "null pairs");
    jsize length = (*env)->GetArrayLength(env, array);
    if (runs < 0 || runs > length / 2) return reject(env, "run count exceeds pairs");
    if (runs == 0) return 0;
    /* 4096 bytes of array scratch on this JNI ABI, independent of input length. */
    jint pairs[RUNS_PER_CHUNK * 2];
    jint answer = runs, previous_end = -1;
    for (jint offset = 0; offset < runs;) {
        jint n = runs - offset;
        if (n > RUNS_PER_CHUNK) n = RUNS_PER_CHUNK;
        (*env)->GetIntArrayRegion(env, array, offset * 2, n * 2, pairs);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jint i = 0; i < n; i++) {
            jint start = pairs[2 * i], end = pairs[2 * i + 1];
            if (start < 0 || start >= end || end > 65536 || previous_end >= start) {
                return reject(env, "unordered or invalid half-open runs");
            }
            /* Validate the entire admitted prefix, even after finding the answer. */
            if (answer == runs && end > value) answer = offset + i;
            previous_end = end;
        }
        offset += n;
    }
    return answer;
}
