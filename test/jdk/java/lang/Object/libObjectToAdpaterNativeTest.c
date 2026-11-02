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

static jobject call_v(JNIEnv *env, jobject receiver, jmethodID method, ...)
{
    jobject result;
    va_list args;
    va_start(args, method);
    result = (*env)->CallObjectMethodV(env, receiver, method, args);
    va_end(args);
    return result;
}

JNIEXPORT jobject JNICALL
Java_ObjectToAdpaterNativeTest_invoke0(JNIEnv *env, jclass caller,
                                      jobject receiver, jclass requested, jint mode)
{
    jclass object_class;
    jmethodID method;
    jobject result = NULL;
    jvalue args[1];

    (void)caller;
    object_class = (*env)->FindClass(env, "java/lang/Object");
    if (object_class == NULL) {
        return NULL;
    }
    method = (*env)->GetMethodID(env, object_class, "toAdpater",
                                 "(Ljava/lang/Class;)Ljava/lang/Object;");
    if (method == NULL) {
        (*env)->DeleteLocalRef(env, object_class);
        return NULL;
    }
    switch (mode) {
        case 0:
            result = (*env)->CallObjectMethod(env, receiver, method, requested);
            break;
        case 1:
            args[0].l = requested;
            result = (*env)->CallObjectMethodA(env, receiver, method, args);
            break;
        case 2:
            result = call_v(env, receiver, method, requested);
            break;
        default:
            (*env)->DeleteLocalRef(env, object_class);
            return NULL;
    }
    /* Deleting local references is permitted with a pending exception.
     * Do not inspect result or invoke arbitrary JNI operations until it clears. */
    (*env)->DeleteLocalRef(env, object_class);
    return result;
}
