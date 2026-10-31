/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * Experimental M3JDK21 object sizing: direct JVMTI, no graph traversal.
 */
#include <jni.h>
#include <jvmti.h>

static void throw_illegal_state(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
    }
}

JNIEXPORT jlong JNICALL Java_jdk_internal_m3_M3ObjectMetrics_shallowSize0(
        JNIEnv *env, jclass type, jobject object) {
    jlong bytes = 0;
    jvmtiEnv *jvmti = NULL;
    JavaVM *vm = NULL;
    jvmtiError result;
    (void) type;

    if (object == NULL) {
        jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (npe != NULL) {
            (*env)->ThrowNew(env, npe, "object");
        }
        return 0;
    }
    if ((*env)->GetJavaVM(env, &vm) != JNI_OK || vm == NULL ||
            (*vm)->GetEnv(vm, (void **) &jvmti, JVMTI_VERSION_1_2) != JNI_OK) {
        throw_illegal_state(env, "JVMTI GetEnv is unavailable for M3 object size");
        return 0;
    }
    if (jvmti == NULL) {
        throw_illegal_state(env, "JVMTI is unavailable for M3 object size");
        return 0;
    }
    result = (*jvmti)->GetObjectSize(jvmti, object, &bytes);
    if (result != JVMTI_ERROR_NONE) {
        throw_illegal_state(env, "JVMTI GetObjectSize failed");
        return 0;
    }
    return bytes;
}
