/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */

#include <jni.h>
#include <stdarg.h>

enum { CALL_OBJECT = 0, CALL_OBJECT_A = 1, CALL_OBJECT_V = 2 };

static jobject call_adapter_v(JNIEnv *env, jobject receiver, jmethodID method, ...)
{
    va_list arguments;
    jobject result;

    va_start(arguments, method);
    result = (*env)->CallObjectMethodV(env, receiver, method, arguments);
    va_end(arguments);
    return result;
}

/* Each JNI thread resolves the original virtual method with local refs only. */
JNIEXPORT jobject JNICALL
Java_ObjectAdapterConcurrentIdentityTest_adaptNative(JNIEnv *env, jclass test_class,
                                                     jobject receiver, jclass requested,
                                                     jint mode)
{
    jclass object_class;
    jmethodID method;
    jvalue arguments[1];

    (void)test_class;
    if (receiver == NULL) {
        jclass null_type = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (null_type != NULL) {
            (*env)->ThrowNew(env, null_type, "receiver");
            (*env)->DeleteLocalRef(env, null_type);
        }
        return NULL;
    }
    object_class = (*env)->FindClass(env, "java/lang/Object");
    if (object_class == NULL) {
        return NULL;
    }
    method = (*env)->GetMethodID(env, object_class, "toAdapter",
                                 "(Ljava/lang/Class;)Ljava/lang/Object;");
    (*env)->DeleteLocalRef(env, object_class);
    if (method == NULL) {
        return NULL;
    }
    switch (mode) {
        case CALL_OBJECT:
            return (*env)->CallObjectMethod(env, receiver, method, requested);
        case CALL_OBJECT_A:
            arguments[0].l = requested;
            return (*env)->CallObjectMethodA(env, receiver, method, arguments);
        case CALL_OBJECT_V:
            return call_adapter_v(env, receiver, method, requested);
        default: {
            jclass invalid = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
            if (invalid != NULL) {
                (*env)->ThrowNew(env, invalid, "unknown concurrent adapter JNI mode");
                (*env)->DeleteLocalRef(env, invalid);
            }
            return NULL;
        }
    }
}
