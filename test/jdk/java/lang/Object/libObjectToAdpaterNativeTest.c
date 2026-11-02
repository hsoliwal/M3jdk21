/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */

#include <jni.h>
#include <stdarg.h>

enum {
    CALL_OBJECT = 0,
    CALL_OBJECT_A = 1,
    CALL_OBJECT_V = 2,
    LOCAL_FRAME_CAPACITY = 16
};

static jobject call_adapter_v(JNIEnv *env, jobject receiver, jmethodID method, ...)
{
    va_list arguments;
    jobject result;

    va_start(arguments, method);
    result = (*env)->CallObjectMethodV(env, receiver, method, arguments);
    va_end(arguments);
    return result;
}

static jobject call_adapter(JNIEnv *env, jobject receiver, jmethodID method,
                            jclass type, jint mode)
{
    jvalue arguments[1];

    switch (mode) {
        case CALL_OBJECT:
            return (*env)->CallObjectMethod(env, receiver, method, type);
        case CALL_OBJECT_A:
            arguments[0].l = type;
            return (*env)->CallObjectMethodA(env, receiver, method, arguments);
        case CALL_OBJECT_V:
            return call_adapter_v(env, receiver, method, type);
        default:
            /* Preserve the existing private fixture's unknown-mode result. */
            return NULL;
    }
}

JNIEXPORT jobject JNICALL
Java_ObjectToAdpaterNativeTest_invoke0(JNIEnv *env, jclass test_type,
                                     jobject receiver, jclass type, jint mode, jboolean alias)
{
    jclass object_type;
    jmethodID method;
    jobject result;

    (void)test_type;
    /* The Java wrapper checks receiver before crossing the JNI boundary. */
    object_type = (*env)->FindClass(env, "java/lang/Object");
    if (object_type == NULL) {
        return NULL;
    }
    method = (*env)->GetMethodID(env, object_type, alias ? "toAdpater" : "toAdapter",
                                 "(Ljava/lang/Class;)Ljava/lang/Object;");
    if (method == NULL) {
        (*env)->DeleteLocalRef(env, object_type);
        return NULL;
    }
    result = call_adapter(env, receiver, method, type, mode);
    if ((*env)->ExceptionCheck(env)) {
        /* DeleteLocalRef is permitted while the original exception is pending. */
        (*env)->DeleteLocalRef(env, object_type);
        return NULL;
    }
    (*env)->DeleteLocalRef(env, object_type);
    /* No Class.cast or replacement runtime implementation is inserted here. */
    return result;
}

JNIEXPORT jint JNICALL
Java_ObjectToAdpaterNativeTest_repeat0(JNIEnv *env, jclass test_type,
                                     jobject receiver, jclass type, jobject expected,
                                     jint count, jint mode, jboolean alias)
{
    jclass object_type;
    jmethodID method;
    jobject result;
    jint completed = 0;

    (void)test_type;
    /* One bounded frame spans the whole loop. Each successful result is deleted
     * before the next call, including capability and overridden return values. */
    if ((*env)->PushLocalFrame(env, LOCAL_FRAME_CAPACITY) < 0) {
        return 0;
    }
    object_type = (*env)->FindClass(env, "java/lang/Object");
    if (object_type == NULL) {
        (*env)->PopLocalFrame(env, NULL);
        return 0;
    }
    method = (*env)->GetMethodID(env, object_type, alias ? "toAdpater" : "toAdapter",
                                 "(Ljava/lang/Class;)Ljava/lang/Object;");
    if (method == NULL) {
        (*env)->PopLocalFrame(env, NULL);
        return 0;
    }
    for (; completed < count; completed++) {
        result = call_adapter(env, receiver, method, type, mode);
        if ((*env)->ExceptionCheck(env)) {
            /* PopLocalFrame is allowed with a pending exception and preserves
             * its identity. Do not inspect a failed Java call's return value. */
            (*env)->PopLocalFrame(env, NULL);
            return completed;
        }
        if (!(*env)->IsSameObject(env, result, expected)) {
            (*env)->DeleteLocalRef(env, result);
            (*env)->PopLocalFrame(env, NULL);
            return completed;
        }
        (*env)->DeleteLocalRef(env, result);
    }
    (*env)->PopLocalFrame(env, NULL);
    return completed;
}
