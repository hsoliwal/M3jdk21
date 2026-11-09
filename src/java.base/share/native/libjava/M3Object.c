/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "m3_object.h"

/* Called only when no exception is pending. Local references never escape. */
static jboolean
m3_object_throw(JNIEnv *env, const char *type, const char *message)
{
    jclass exception_type = (*env)->FindClass(env, type);
    if (exception_type != NULL) {
        (*env)->ThrowNew(env, exception_type, message);
        (*env)->DeleteLocalRef(env, exception_type);
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
M3Object_NotifyContentChanged(JNIEnv *env, jobject source)
{
    jclass runtime;
    jmethodID notify;
    jboolean failed;

    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    if (source == NULL) {
        return m3_object_throw(env, "java/lang/NullPointerException",
                               "M3Object source is null");
    }

    runtime = (*env)->FindClass(env, "java/lang/M3ObjectRuntime");
    if (runtime == NULL) {
        return JNI_FALSE;
    }
    notify = (*env)->GetStaticMethodID(env, runtime, "notifyContentChanged",
                                       "(Ljava/lang/Object;)V");
    if (notify == NULL) {
        (*env)->DeleteLocalRef(env, runtime);
        return JNI_FALSE;
    }
    (*env)->CallStaticVoidMethod(env, runtime, notify, source);
    failed = (*env)->ExceptionCheck(env);
    (*env)->DeleteLocalRef(env, runtime);
    return failed ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
M3Object_NotifyFieldChanged(JNIEnv *env, jobject source,
                          jclass declaring_type, jstring field_name,
                          jclass field_type, jint mask)
{
    jclass runtime;
    jmethodID notify;
    jboolean failed;

    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    if (source == NULL || declaring_type == NULL || field_name == NULL ||
            field_type == NULL) {
        return m3_object_throw(env, "java/lang/NullPointerException",
                               "M3Object field notification argument is null");
    }
    if (mask != M3_OBJECT_REFERENCE && mask != M3_OBJECT_VALUE) {
        return m3_object_throw(env, "java/lang/IllegalArgumentException",
                               "M3Object field mask must be REFERENCE or VALUE");
    }

    runtime = (*env)->FindClass(env, "java/lang/M3ObjectRuntime");
    if (runtime == NULL) {
        return JNI_FALSE;
    }
    notify = (*env)->GetStaticMethodID(env, runtime, "notifyFieldChanged",
            "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I)V");
    if (notify == NULL) {
        (*env)->DeleteLocalRef(env, runtime);
        return JNI_FALSE;
    }
    (*env)->CallStaticVoidMethod(env, runtime, notify, source,
                                declaring_type, field_name, field_type, mask);
    failed = (*env)->ExceptionCheck(env);
    (*env)->DeleteLocalRef(env, runtime);
    return failed ? JNI_FALSE : JNI_TRUE;
}
