/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Host-only test registration; production uses the libjava symbol directly.
 */
#include <jni.h>
#include "jdk_internal_mindex_M3RX7.h"

static jlong calls;
static void counted(JNIEnv *env, jclass owner, jshortArray dfa, jbyteArray packed,
                    jintArray offsets, jintArray lengths, jshortArray output) {
    calls++;
    Java_jdk_internal_mindex_M3RX7_buildPacked0(env, owner, dfa, packed, offsets, lengths, output);
}

JNIEXPORT void JNICALL Java_M3RX7BoundaryContract_registerKernel(JNIEnv *env, jclass ignored) {
    (void) ignored;
    jclass owner = (*env)->FindClass(env, "jdk/internal/mindex/M3RX7");
    if (owner == NULL) return;
    JNINativeMethod method = {"buildPacked0", "([S[B[I[I[S)V", (void *) counted};
    if ((*env)->RegisterNatives(env, owner, &method, 1) != 0 && !(*env)->ExceptionCheck(env)) {
        jclass failure = (*env)->FindClass(env, "java/lang/AssertionError");
        if (failure != NULL) {
            (*env)->ThrowNew(env, failure, "native registration failed");
            (*env)->DeleteLocalRef(env, failure);
        }
    }
    (*env)->DeleteLocalRef(env, owner);
}

JNIEXPORT jlong JNICALL Java_M3RX7BoundaryContract_nativeCalls(JNIEnv *env, jclass ignored) {
    (void) env;
    (void) ignored;
    return calls;
}
