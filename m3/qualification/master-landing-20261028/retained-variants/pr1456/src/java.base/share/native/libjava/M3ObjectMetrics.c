/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * Experimental M3JDK21 object sizing: direct JVMTI, no graph traversal.
 */
#include <jni.h>
#include <jvmti.h>
#include <stdatomic.h>

/* Copyright (c) 2026, Hitesh Soliwal and contributors.
 * JVMTI GetEnv creates an environment, unlike the thread-local JNI lookup.
 * libjava and this private, cross-thread JVMTI connection have VM lifetime.
 * Only the published environment is retained; cold-race losers are disposed.
 * No capabilities, events, tags, monitors or object references are installed.
 */
static _Atomic(jvmtiEnv *) object_metrics_jvmti = NULL;

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
    if ((*env)->GetJavaVM(env, &vm) != JNI_OK || vm == NULL) {
        throw_illegal_state(env, "JVMTI GetEnv is unavailable for M3 object size");
        return 0;
    }
    jvmti = atomic_load_explicit(&object_metrics_jvmti, memory_order_acquire);
    if (jvmti == NULL) {
        jvmtiEnv *created = NULL;
        jvmtiEnv *winner = NULL;
        if ((*vm)->GetEnv(vm, (void **) &created, JVMTI_VERSION_1_2) != JNI_OK) {
            throw_illegal_state(env, "JVMTI GetEnv is unavailable for M3 object size");
            return 0;
        }
        if (created == NULL) {
            throw_illegal_state(env, "JVMTI is unavailable for M3 object size");
            return 0;
        }
        if (atomic_compare_exchange_strong_explicit(&object_metrics_jvmti,
                &winner, created, memory_order_acq_rel, memory_order_acquire)) {
            jvmti = created;
        } else {
            result = (*created)->DisposeEnvironment(created);
            if (result != JVMTI_ERROR_NONE) {
                throw_illegal_state(env, "JVMTI DisposeEnvironment failed for M3 object size");
                return 0;
            }
            jvmti = winner;
        }
    }
    result = (*jvmti)->GetObjectSize(jvmti, object, &bytes);
    if (result != JVMTI_ERROR_NONE) {
        throw_illegal_state(env, "JVMTI GetObjectSize failed");
        return 0;
    }
    return bytes;
}
