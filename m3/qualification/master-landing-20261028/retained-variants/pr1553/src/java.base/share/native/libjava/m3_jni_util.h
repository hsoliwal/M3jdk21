/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

#ifndef M3_JNI_UTIL_H
#define M3_JNI_UTIL_H

#include <jni.h>

/* Shared target-owned exception atom. Keep the incumbent JNI operation order:
 * FindClass, conditional ThrowNew, then release the temporary class reference.
 * The caller owns its return value and any pending-exception decision.
 * C and C++ JNI bindings expose these same operations through different syntax.
 */
#if defined(_MSC_VER) && !defined(__cplusplus)
static __inline void
#else
static inline void
#endif
m3_jni_throw_by_name(JNIEnv *env, const char *type, const char *message) {
#ifdef __cplusplus
    jclass exception = env->FindClass(type);
    if (exception != NULL) {
        (void)env->ThrowNew(exception, message);
        env->DeleteLocalRef(exception);
    }
#else
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (void)(*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
#endif
}

#endif /* M3_JNI_UTIL_H */
