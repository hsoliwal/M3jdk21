/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
#include <jni.h>
#include <stdint.h>
#include <string.h>

/* Called synchronously only while M3PrimitiveImage.withAddress holds the mapping lease. */
JNIEXPORT jlong JNICALL Java_PrimitiveIdentityTest_readKey(JNIEnv *env, jclass klass, jlong address) {
    (void)env; (void)klass;
    const unsigned char *p = (const unsigned char *)(uintptr_t)address;
    uint64_t bits = 0;
    for (int i = 0; i < 8; i++) bits = (bits << 8) | p[i];
    jlong value;
    memcpy(&value, &bits, sizeof(value));
    return value;
}


/* Stock-JDK patch test only: register the exact product functions without modifying libjava.
 * Matched M3 images resolve these exported symbols from their own libjava instead. */
#ifdef M3_PATCHED_TEST
extern jobject Java_jdk_internal_mindex_M3PrimitiveImage_fixedMap0(JNIEnv *, jclass, jobject, jlong, jlong);
extern void Java_jdk_internal_mindex_M3PrimitiveImage_fixedUnmap0(JNIEnv *, jclass, jobject);
#endif
JNIEXPORT void JNICALL Java_PrimitiveIdentityFixedTest_registerMapping(JNIEnv *env, jclass ignored) {
    (void)ignored;
#ifdef M3_PATCHED_TEST
    jclass image = (*env)->FindClass(env, "jdk/internal/mindex/M3PrimitiveImage");
    if (image == NULL) return;
    JNINativeMethod methods[] = {
        {"fixedMap0", "(Ljava/io/FileDescriptor;JJ)Ljava/nio/ByteBuffer;",
         (void *)Java_jdk_internal_mindex_M3PrimitiveImage_fixedMap0},
        {"fixedUnmap0", "(Ljava/nio/ByteBuffer;)V",
         (void *)Java_jdk_internal_mindex_M3PrimitiveImage_fixedUnmap0}
    };
    (*env)->RegisterNatives(env, image, methods, 2);
#else
    (void)env; // Matched-image tests resolve product methods from libjava normally.
#endif
}
JNIEXPORT jlong JNICALL Java_PrimitiveIdentityFixedTest_readKey(JNIEnv *env, jclass klass, jlong address) {
    return Java_PrimitiveIdentityTest_readKey(env, klass, address);
}
