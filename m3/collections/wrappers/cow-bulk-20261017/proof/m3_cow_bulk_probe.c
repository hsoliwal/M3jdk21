/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test-only JNI consumer of Java Collection.iterator(). Does not own a collection,
 * invoke arbitrary native predicates, or introduce a product java.base JNI ABI.
 */
#include <jni.h>
#include <stdint.h>

JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3CowBulkReceiverProof_nativeCountAndSum(
        JNIEnv *env, jclass caller, jobject source) {
    (void) caller;
    if (source == NULL) {
        jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (npe != NULL) {
            (*env)->ThrowNew(env, npe, "source");
            (*env)->DeleteLocalRef(env, npe);
        }
        return (jlong) 0;
    }
    jclass collectionClass = (*env)->GetObjectClass(env, source);
    if (collectionClass == NULL) return (jlong) 0;
    jmethodID iteratorMethod = (*env)->GetMethodID(env, collectionClass,
            "iterator", "()Ljava/util/Iterator;");
    (*env)->DeleteLocalRef(env, collectionClass);
    if (iteratorMethod == NULL) return (jlong) 0;
    jobject iterator = (*env)->CallObjectMethod(env, source, iteratorMethod);
    if ((*env)->ExceptionCheck(env) || iterator == NULL) return (jlong) 0;

    jclass iteratorClass = (*env)->GetObjectClass(env, iterator);
    if (iteratorClass == NULL) {
        (*env)->DeleteLocalRef(env, iterator);
        return (jlong) 0;
    }
    jmethodID hasNext = (*env)->GetMethodID(env, iteratorClass, "hasNext", "()Z");
    jmethodID next = hasNext == NULL ? NULL : (*env)->GetMethodID(
            env, iteratorClass, "next", "()Ljava/lang/Object;");
    (*env)->DeleteLocalRef(env, iteratorClass);
    if (hasNext == NULL || next == NULL) {
        (*env)->DeleteLocalRef(env, iterator);
        return (jlong) 0;
    }
    jclass numberClass = (*env)->FindClass(env, "java/lang/Number");
    if (numberClass == NULL) {
        (*env)->DeleteLocalRef(env, iterator);
        return (jlong) 0;
    }
    jmethodID longValue = (*env)->GetMethodID(env, numberClass, "longValue", "()J");
    (*env)->DeleteLocalRef(env, numberClass);
    if (longValue == NULL) {
        (*env)->DeleteLocalRef(env, iterator);
        return (jlong) 0;
    }

    uint32_t count = 0;
    int64_t sum = 0;
    for (;;) {
        jboolean more = (*env)->CallBooleanMethod(env, iterator, hasNext);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, iterator);
            return (jlong) 0;
        }
        if (more == JNI_FALSE) break;
        jobject element = (*env)->CallObjectMethod(env, iterator, next);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, iterator);
            return (jlong) 0;
        }
        if (element != NULL) {
            jlong value = (*env)->CallLongMethod(env, element, longValue);
            (*env)->DeleteLocalRef(env, element);
            if ((*env)->ExceptionCheck(env)) {
                (*env)->DeleteLocalRef(env, iterator);
                return (jlong) 0;
            }
            sum += (int64_t) value;
        }
        count++;
    }
    (*env)->DeleteLocalRef(env, iterator);
    return (jlong) (((uint64_t) count << 32) | ((uint64_t) sum & UINT64_C(0xffffffff)));
}
