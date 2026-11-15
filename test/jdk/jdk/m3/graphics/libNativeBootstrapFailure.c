/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */

#include <jni.h>

/* A deliberately failing bootstrap fixture, used only by NativeLibraryFailure. */
JNIEXPORT void JNICALL Java_jdk_m3_graphics_impl_M3Library__1nAfterLoad
  (JNIEnv* env, jclass library) {
    jclass exception = (*env)->FindClass(env, "java/lang/UnsatisfiedLinkError");
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, "intentional bootstrap failure");
    }
}
