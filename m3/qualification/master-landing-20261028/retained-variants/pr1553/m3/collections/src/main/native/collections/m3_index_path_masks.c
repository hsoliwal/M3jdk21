/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Optional JNI parity backend for precomputed integer mask stages.
 * Only primitive Region APIs; no GetPrimitiveArrayCritical, retained Java pointers,
 * Java callbacks, persistent references, or per-row heap allocations.
 * This general gather kernel is not claimed faster than the Java reference path.
 */
#include <jni.h>
#include <stddef.h>
#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    m3_jni_throw_by_name(env, type, message);
}

/* -1 = pending JNI exception, 0 = rejected, 1 = accepted. */
static int m3_matches(JNIEnv *env, jobjectArray planes,
                      jintArray needs, jintArray denies,
                      jsize stages, jint slot) {
    for (jsize stage = 0; stage < stages; ++stage) {
        jint required = 0;
        jint forbidden = 0;
        jint state = 0;
        (*env)->GetIntArrayRegion(env, needs, stage, 1, &required);
        if ((*env)->ExceptionCheck(env)) return -1;
        (*env)->GetIntArrayRegion(env, denies, stage, 1, &forbidden);
        if ((*env)->ExceptionCheck(env)) return -1;
        jobject plane_object = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (plane_object == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return -1;
        }
        jintArray plane = (jintArray)plane_object;
        jsize length = (*env)->GetArrayLength(env, plane);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, plane_object);
            return -1;
        }
        if (slot < 0 || slot >= length) {
            (*env)->DeleteLocalRef(env, plane_object);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, plane, slot, 1, &state);
        (*env)->DeleteLocalRef(env, plane_object);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((state & required) != required || (state & forbidden) != 0) return 0;
    }
    return 1;
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride) {
    (void)owner;
    if (rows == NULL || offsets == NULL || planes == NULL || required == NULL || forbidden == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null native mask path argument");
        return NULL;
    }
    jsize size = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize position_count = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize stages = (*env)->GetArrayLength(env, planes);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_required = (*env)->GetArrayLength(env, required);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_forbidden = (*env)->GetArrayLength(env, forbidden);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (size != position_count || stages == 0 || stages != n_required || stages != n_forbidden) {
        m3_throw(env, "java/lang/IllegalArgumentException", "unaligned native mask planes");
        return NULL;
    }
    if (count < 0 || stride == 0 || first < 0 || first > size) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid native mask path");
        return NULL;
    }
    if (count > 0) {
        jlong last = (jlong)first + ((jlong)count - 1) * (jlong)stride;
        if (first == size || last < 0 || last >= size) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "native path out of bounds");
            return NULL;
        }
    }
    jsize matching = 0;
    for (jint pass = 0; pass < 2; ++pass) {
        /* Output allocation is deferred until after exact counting. */
        jintArray result = NULL;
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL) return NULL;
        }
        jsize written = 0;
        jlong at = (jlong)first;
        for (jint ordinal = 0; ordinal < count; ++ordinal, at += stride) {
            jint physical = -1;
            (*env)->GetIntArrayRegion(env, offsets, (jsize)at, 1, &physical);
            if ((*env)->ExceptionCheck(env)) return NULL;
            int keep = m3_matches(env, planes, required, forbidden, stages, physical);
            if (keep < 0) return NULL;
            if (keep == 0) continue;
            if (pass == 0) {
                ++matching;
            } else {
                if (written >= matching) {
                    m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                    return NULL;
                }
                jint row = 0;
                (*env)->GetIntArrayRegion(env, rows, (jsize)at, 1, &row);
                if ((*env)->ExceptionCheck(env)) return NULL;
                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);
                if ((*env)->ExceptionCheck(env)) return NULL;
            }
        }
        if (pass == 1) {
            if (written != matching) {
                m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                return NULL;
            }
            return result;
        }
    }
    return NULL;
}
