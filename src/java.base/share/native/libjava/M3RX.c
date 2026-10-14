/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 * Target-owned bounded Boolean relation composition; no text/native lifetime ownership.
 */
#include "jdk_internal_mindex_M3RX.h"
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long must be 64 bits");

static void m3rx_throw(JNIEnv *env, const char *name, const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jlongArray JNICALL Java_jdk_internal_mindex_M3RX_composeRows0(
        JNIEnv *env, jclass type, jlongArray left, jlongArray right, jint states) {
    (void) type;
    if (left == NULL || right == NULL) {
        m3rx_throw(env, "java/lang/NullPointerException", "M3 regex relations");
        return NULL;
    }
    if (states < 1 || states > 64 || (*env)->GetArrayLength(env, left) != states
            || (*env)->GetArrayLength(env, right) != states) {
        m3rx_throw(env, "java/lang/IllegalArgumentException", "M3 regex relation geometry");
        return NULL;
    }
    jlong a[64], b[64], result[64];
    (*env)->GetLongArrayRegion(env, left, 0, states, a);
    if ((*env)->ExceptionCheck(env)) return NULL;
    (*env)->GetLongArrayRegion(env, right, 0, states, b);
    if ((*env)->ExceptionCheck(env)) return NULL;
    uint64_t valid = states == 64 ? UINT64_MAX : (UINT64_C(1) << (unsigned) states) - 1;
    /* Check every row, including rows unreachable from the first relation. */
    for (jint i = 0; i < states; i++) {
        if ((((uint64_t) a[i] | (uint64_t) b[i]) & ~valid) != 0) {
            m3rx_throw(env, "java/lang/IllegalArgumentException", "M3 regex relation row");
            return NULL;
        }
    }
    for (jint row = 0; row < states; row++) {
        uint64_t incoming = (uint64_t) a[row], outgoing = 0;
        for (jint bit = 0; bit < states; bit++) {
            if ((incoming & (UINT64_C(1) << (unsigned) bit)) != 0) outgoing |= (uint64_t) b[bit];
        }
        /* Preserve the high bit without an out-of-range unsigned-to-signed conversion. */
        memcpy(&result[row], &outgoing, sizeof(outgoing));
    }
    jlongArray output = (*env)->NewLongArray(env, states);
    if (output == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, output, 0, states, result);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}
