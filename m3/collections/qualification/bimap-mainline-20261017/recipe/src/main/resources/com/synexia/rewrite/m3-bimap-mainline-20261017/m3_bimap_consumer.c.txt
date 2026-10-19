/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * Test-only JNI BiMap client. Java remains the collection owner. No retained references.
 */
#include <jni.h>
#include "com_m3_collections_interop_M3BiMapNativeTest.h"

static void fail(JNIEnv *env, const char *name, const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type == NULL) return;
    (void)(*env)->ThrowNew(env, type, message);
    (*env)->DeleteLocalRef(env, type);
}
JNIEXPORT jobject JNICALL Java_com_m3_collections_interop_M3BiMapNativeTest_call(
        JNIEnv *env, jclass unused, jobject map, jobject key, jobject value, jint operation) {
    (void)unused;
    if (map == NULL) { fail(env, "java/lang/NullPointerException", "map"); return NULL; }
    if (operation < 0 || operation > 3) {
        fail(env, "java/lang/IllegalArgumentException", "operation"); return NULL;
    }
    jclass type = (*env)->FindClass(env, "com/google/common/collect/BiMap");
    if (type == NULL) return NULL;
    jobject result = NULL;
    if (operation < 2) {
        jmethodID method = (*env)->GetMethodID(env, type, operation == 0 ? "put" : "forcePut",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        if (method != NULL) result = (*env)->CallObjectMethod(env, map, method, key, value);
        if ((*env)->ExceptionCheck(env)) result = NULL;
    } else if (operation == 2) {
        jmethodID method = (*env)->GetMethodID(env, type, "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
        if (method != NULL) result = (*env)->CallObjectMethod(env, map, method, key);
        if ((*env)->ExceptionCheck(env)) result = NULL;
    } else {
        jmethodID inverse_method = (*env)->GetMethodID(env, type, "inverse", "()Lcom/google/common/collect/BiMap;");
        if (inverse_method != NULL) {
            jobject inverse = (*env)->CallObjectMethod(env, map, inverse_method);
            if (!(*env)->ExceptionCheck(env) && inverse != NULL) {
                jmethodID get = (*env)->GetMethodID(env, type, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
                if (get != NULL) result = (*env)->CallObjectMethod(env, inverse, get, key);
                if ((*env)->ExceptionCheck(env)) result = NULL;
            }
            if (inverse != NULL) (*env)->DeleteLocalRef(env, inverse);
        }
    }
    (*env)->DeleteLocalRef(env, type);
    return result;
}
