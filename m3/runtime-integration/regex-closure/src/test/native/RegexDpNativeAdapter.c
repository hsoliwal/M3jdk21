/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * Test-only binding/buffer lifetime. The complete production C file is compiled
 * as a separate translation unit; all regex execution and validation stay there.
 */
#include <jni.h>
#include <stdint.h>
#include "RegexDpNativeHarness.h"
#include "java_lang_M3StringRegexDp.h"

JNIEXPORT jlong JNICALL Java_RegexDpNativeHarness_run(
        JNIEnv *env, jclass ignored, jbyteArray bytes, jint capacity,
        jint start, jint length, jint width, jboolean big_endian,
        jlong incoming, jintArray atoms, jlongArray closure, jint address_mode) {
    jbyte *buffer = NULL;
    jlong address = address_mode == 2 ? (jlong) -1 : (jlong) 0;
    jlong result;
    (void) ignored;
    if (address_mode == 0 && bytes != NULL) {
        jsize size = (*env)->GetArrayLength(env, bytes);
        if (capacity >= 0 && (width == 1 || width == 2)
                && (uint64_t) (unsigned int) capacity * (uint64_t) (unsigned int) width
                        > (uint64_t) (unsigned int) size) {
            jclass exception = (*env)->FindClass(env, "java/lang/AssertionError");
            if (exception != NULL) {
                (void) (*env)->ThrowNew(env, exception, "test buffer smaller than declared capacity");
                (*env)->DeleteLocalRef(env, exception);
            }
            return (jlong) -1;
        }
        if (size != 0) {
            buffer = (*env)->GetByteArrayElements(env, bytes, NULL);
            if (buffer == NULL) return (jlong) -1;
            address = (jlong) (uintptr_t) buffer;
        }
    }
    result = Java_java_lang_M3StringRegexDp_advance0(
            env, NULL, address, capacity, start, length, width, big_endian,
            incoming, atoms, closure);
    if (buffer != NULL) (*env)->ReleaseByteArrayElements(env, bytes, buffer, JNI_ABORT);
    return result;
}
