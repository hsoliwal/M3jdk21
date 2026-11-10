/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test-only fault double for jdk.m3.text.M3FixedAddressMapping. It binds the same
 * four natives as lexicon/native/m3_fixed_address_mapping_jni.c but never maps a
 * file: each requested length selects one way a broken native mapper can answer,
 * so the Java owner's fail-closed admission is exercised without relying on a
 * kernel failure. Never ship or load this library outside the coverage runner.
 *
 *   length 1: NULL without a pending exception
 *   length 2: a Java heap (non-direct) buffer
 *   length 3: a direct buffer whose capacity differs from the request
 *   other   : a direct buffer of the right capacity at a foreign address
 */
#include <jni.h>
#include <stdint.h>

static unsigned char fault_storage[64];

JNIEXPORT jobject JNICALL
Java_jdk_m3_text_M3FixedAddressMapping_mapExact0(JNIEnv *env, jclass klass,
        jstring path, jlong base, jlong length) {
    (void)klass;
    (void)path;
    (void)base;
    if (length == 1) return NULL;
    if (length == 2) {
        jclass buffers = (*env)->FindClass(env, "java/nio/ByteBuffer");
        if (buffers == NULL) return NULL;
        jmethodID allocate = (*env)->GetStaticMethodID(env, buffers, "allocate",
                "(I)Ljava/nio/ByteBuffer;");
        if (allocate == NULL) return NULL;
        return (*env)->CallStaticObjectMethod(env, buffers, allocate, (jint)length);
    }
    if (length == 3) return (*env)->NewDirectByteBuffer(env, fault_storage, length - 1);
    if (length < 0 || length > (jlong)sizeof(fault_storage)) return NULL;
    return (*env)->NewDirectByteBuffer(env, fault_storage, length);
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_text_M3FixedAddressMapping_directAddress0(JNIEnv *env, jclass klass,
        jobject buffer) {
    (void)klass;
    if (buffer == NULL) return 0;
    return (jlong)(uintptr_t)(*env)->GetDirectBufferAddress(env, buffer);
}

JNIEXPORT void JNICALL
Java_jdk_m3_text_M3FixedAddressMapping_unmapRejected0(JNIEnv *env, jclass klass,
        jlong base, jlong length) {
    (void)env;
    (void)klass;
    (void)base;
    (void)length;
}

JNIEXPORT void JNICALL
Java_jdk_m3_text_M3FixedAddressMapping_reserveCollision0(JNIEnv *env, jclass klass,
        jlong base, jlong length) {
    (void)env;
    (void)klass;
    (void)base;
    (void)length;
}
