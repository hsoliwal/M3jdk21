/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * M3JDK21: one JNI contract for primitive owner offsets, windows and masks.
 * Header-only C11 atoms: no retained Java handles, native heap or JVM pins.
 * JNIEnv must belong to the invoking thread and is never stored.
 */
#ifndef M3_JNI_CORE_H
#define M3_JNI_CORE_H

#include <jni.h>
#include <stdint.h>

/* Subtraction-based validation avoids first+count overflow, including INT_MAX. */
static inline int m3_jni_valid_window(jsize capacity, jint first, jint count) {
    return capacity >= 0 && first >= 0 && count >= 0
            && first <= capacity && count <= capacity - first;
}

/* M3 object-state masks use unsigned-byte semantics despite signed jbyte. */
static inline int m3_jni_matches_u8(jbyte bits, jint required, jint excluded) {
    if (required < 0 || required > 255 || excluded < 0 || excluded > 255) {
        return 0;
    }
    unsigned int state = (unsigned int)(uint8_t)bits;
    unsigned int needs = (unsigned int)required;
    unsigned int denies = (unsigned int)excluded;
    return (state & needs) == needs && (state & denies) == 0u;
}

/* Bitwise semantics preserve the full Java signed-int and signed-long bit patterns. */
static inline int m3_jni_matches_u32(jint bits, jint required, jint excluded) {
    uint32_t state = (uint32_t)bits;
    uint32_t needs = (uint32_t)required;
    uint32_t denies = (uint32_t)excluded;
    return (state & needs) == needs && (state & denies) == UINT32_C(0);
}

static inline int m3_jni_matches_u64(jlong bits, jlong required, jlong excluded) {
    uint64_t state = (uint64_t)bits;
    uint64_t needs = (uint64_t)required;
    uint64_t denies = (uint64_t)excluded;
    return (state & needs) == needs && (state & denies) == UINT64_C(0);
}

/* Shared pending-exception discipline; no JNI work after a pending exception. */
static inline void m3_jni_throw_new(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass exception_type = (*env)->FindClass(env, type);
    if (exception_type != NULL) {
        (void)(*env)->ThrowNew(env, exception_type, message);
        (*env)->DeleteLocalRef(env, exception_type);
    }
}

#endif /* M3_JNI_CORE_H */
