/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Test-only native consumer of production Java iterators. No global reference,
 * array copy, saved JNIEnv, native collection, OS resource or pointer escapes. */
#include <jni.h>
#include <limits.h>
#include "com_synexia_common_collections_RangeJniProof.h"

static void reject(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}
JNIEXPORT jint JNICALL
Java_com_synexia_common_collections_RangeJniProof_drain(
        JNIEnv *env, jclass owner, jobject iterator, jobject action, jint period) {
    (void)owner;
    if (iterator == NULL || action == NULL) {
        reject(env, "java/lang/NullPointerException", "test iterator/consumer"); return -1;
    }
    if (period < 0) {
        reject(env, "java/lang/IllegalArgumentException", "negative removal period"); return -1;
    }
    jint count = 0;
    jclass iterable = (*env)->FindClass(env, "java/util/Iterator");
    if (iterable == NULL) return -1;
    jclass consumer = (*env)->FindClass(env, "java/util/function/Consumer");
    if (consumer == NULL) { (*env)->DeleteLocalRef(env, iterable); return -1; }
    jmethodID has_next = (*env)->GetMethodID(env, iterable, "hasNext", "()Z");
    if (has_next == NULL) goto cleanup;
    jmethodID next = (*env)->GetMethodID(env, iterable, "next", "()Ljava/lang/Object;");
    if (next == NULL) goto cleanup;
    jmethodID remove = (*env)->GetMethodID(env, iterable, "remove", "()V");
    if (remove == NULL) goto cleanup;
    jmethodID accept = (*env)->GetMethodID(env, consumer, "accept", "(Ljava/lang/Object;)V");
    if (accept == NULL) goto cleanup;
    for (;;) {
        jboolean has = (*env)->CallBooleanMethod(env, iterator, has_next);
        if ((*env)->ExceptionCheck(env) || !has) break;
        if (count == INT_MAX) {
            reject(env, "java/lang/ArithmeticException", "fixture count overflow"); break;
        }
        jobject value = (*env)->CallObjectMethod(env, iterator, next);
        if ((*env)->ExceptionCheck(env)) break;
        (*env)->CallVoidMethod(env, action, accept, value);
        if (value != NULL) (*env)->DeleteLocalRef(env, value);
        if ((*env)->ExceptionCheck(env)) break;
        ++count;
        if (period != 0 && count % period == 0) {
            (*env)->CallVoidMethod(env, iterator, remove);
            if ((*env)->ExceptionCheck(env)) break;
        }
    }
cleanup:
    (*env)->DeleteLocalRef(env, consumer);
    (*env)->DeleteLocalRef(env, iterable);
    return (*env)->ExceptionCheck(env) ? -1 : count;
}
