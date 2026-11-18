/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

#include <jni.h>
#include <stdarg.h>

/* Test support only: no global cache, retained reference or runtime bridge. */

static void fail(JNIEnv *env, const char *type, const char *message)
{
    jclass exception_type;

    /* Never replace an exception that an earlier JNI operation already raised. */
    if ((*env)->ExceptionCheck(env)) {
        return;
    }
    exception_type = (*env)->FindClass(env, type);
    if (exception_type != NULL) {
        (void)(*env)->ThrowNew(env, exception_type, message);
        (*env)->DeleteLocalRef(env, exception_type);
    }
}

static void release_globals(JNIEnv *env, jobject strong, jweak weak)
{
    /* Both deletion functions are permitted while an exception is pending. */
    if (weak != NULL) {
        (*env)->DeleteWeakGlobalRef(env, weak);
    }
    if (strong != NULL) {
        (*env)->DeleteGlobalRef(env, strong);
    }
}

static jboolean require_reference(JNIEnv *env, jobject reference, jobject source)
{
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    if (source != NULL && reference == NULL) {
        fail(env, "java/lang/OutOfMemoryError", "JNI reference allocation returned NULL");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jboolean reference_identity(JNIEnv *env, jobject source, jobject local,
                                   jobject strong, jweak weak, jobject rooted)
{
    if (source == NULL) {
        if (local != NULL || strong != NULL || weak != NULL || rooted != NULL) {
            fail(env, "java/lang/AssertionError", "null reference changed identity");
            return JNI_FALSE;
        }
        return JNI_TRUE;
    }
    if ((*env)->GetObjectRefType(env, local) != JNILocalRefType
            || (*env)->GetObjectRefType(env, strong) != JNIGlobalRefType
            || (*env)->GetObjectRefType(env, weak) != JNIWeakGlobalRefType
            || (*env)->GetObjectRefType(env, rooted) != JNILocalRefType
            || !(*env)->IsSameObject(env, source, local)
            || !(*env)->IsSameObject(env, source, strong)
            || !(*env)->IsSameObject(env, source, weak)
            || !(*env)->IsSameObject(env, source, rooted)) {
        fail(env, "java/lang/AssertionError", "live JNI reference identity/type mismatch");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jobject call_with_varargs(JNIEnv *env, jobject receiver, jclass declaring_type,
                                  jmethodID method, ...)
{
    va_list arguments;
    jobject result;

    va_start(arguments, method);
    result = declaring_type == NULL
            ? (*env)->CallObjectMethodV(env, receiver, method, arguments)
            : (*env)->CallNonvirtualObjectMethodV(env, receiver, declaring_type, method, arguments);
    va_end(arguments);
    return result;
}

static jobject call_method(JNIEnv *env, jobject receiver, jclass declaring_type,
                            jmethodID method, jclass type, jint mode)
{
    jvalue arguments[1];

    switch (mode) {
        case 0:
            return (*env)->CallObjectMethod(env, receiver, method, type);
        case 1:
            arguments[0].l = type;
            return (*env)->CallObjectMethodA(env, receiver, method, arguments);
        case 2:
            return call_with_varargs(env, receiver, NULL, method, type);
        case 3:
            return (*env)->CallNonvirtualObjectMethod(env, receiver, declaring_type, method, type);
        case 4:
            arguments[0].l = type;
            return (*env)->CallNonvirtualObjectMethodA(env, receiver, declaring_type, method, arguments);
        case 5:
            return call_with_varargs(env, receiver, declaring_type, method, type);
        default:
            fail(env, "java/lang/IllegalArgumentException", "invalid JNI call mode");
            return NULL;
    }
}

static jobject invoke_checked(JNIEnv *env, jobject receiver, jclass type,
                               const char *owner, const char *name, jint mode,
                               jboolean expect_exception)
{
    jclass declaring_type;
    jmethodID method;
    jobject local = NULL;
    jobject strong = NULL;
    jweak weak = NULL;
    jobject rooted = NULL;
    jobject result = NULL;

    if (receiver == NULL) {
        fail(env, "java/lang/NullPointerException", "receiver");
        return NULL;
    }
    if (mode < 0 || mode > 5) {
        fail(env, "java/lang/IllegalArgumentException", "invalid JNI call mode");
        return NULL;
    }
    if ((*env)->PushLocalFrame(env, 16) < 0) {
        return NULL;
    }
    declaring_type = (*env)->FindClass(env, owner);
    if (declaring_type == NULL) {
        fail(env, "java/lang/AssertionError", "FindClass returned NULL without a pending exception");
        goto cleanup;
    }
    method = (*env)->GetMethodID(env, declaring_type, name,
                                 "(Ljava/lang/Class;)Ljava/lang/Object;");
    if (method == NULL) {
        fail(env, "java/lang/AssertionError", "GetMethodID returned NULL without a pending exception");
        goto cleanup;
    }
    local = (*env)->NewLocalRef(env, receiver);
    if (!require_reference(env, local, receiver)) {
        goto cleanup;
    }
    strong = (*env)->NewGlobalRef(env, local);
    if (!require_reference(env, strong, receiver)) {
        goto cleanup;
    }
    weak = (*env)->NewWeakGlobalRef(env, local);
    if (!require_reference(env, weak, receiver)) {
        goto cleanup;
    }
    rooted = (*env)->NewLocalRef(env, weak);
    if (!require_reference(env, rooted, receiver)
            || !reference_identity(env, receiver, local, strong, weak, rooted)) {
        goto cleanup;
    }
    result = call_method(env, rooted, declaring_type, method, type, mode);
    /* A NULL result is not an exception indicator. Check before inspecting it
     * or making any JNI call that is forbidden with a pending exception. */
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }
    if (expect_exception) {
        fail(env, "java/lang/AssertionError", "callback returned without expected pending exception");
    }

cleanup:
    release_globals(env, strong, weak);
    if ((*env)->ExceptionCheck(env)) {
        /* PopLocalFrame preserves the original Throwable; do not clear it. */
        (*env)->PopLocalFrame(env, NULL);
        return NULL;
    }
    return (*env)->PopLocalFrame(env, result);
}

static jobject copy_references(JNIEnv *env, jobject value, jint count, jthrowable pending)
{
    jobject result = NULL;
    jint index;

    if (count < 1 || count > 8192) {
        fail(env, "java/lang/IllegalArgumentException", "reference iteration bound");
        return NULL;
    }
    for (index = 0; index < count; index++) {
        jobject local = NULL;
        jobject strong = NULL;
        jweak weak = NULL;
        jobject rooted = NULL;

        (*env)->DeleteLocalRef(env, result);
        result = NULL;
        if ((*env)->PushLocalFrame(env, 8) < 0) {
            return NULL;
        }
        local = (*env)->NewLocalRef(env, value);
        if (!require_reference(env, local, value)) {
            goto cleanup;
        }
        strong = (*env)->NewGlobalRef(env, local);
        if (!require_reference(env, strong, value)) {
            goto cleanup;
        }
        weak = (*env)->NewWeakGlobalRef(env, local);
        if (!require_reference(env, weak, value)) {
            goto cleanup;
        }
        rooted = (*env)->NewLocalRef(env, weak);
        if (!require_reference(env, rooted, value)
                || !reference_identity(env, value, local, strong, weak, rooted)) {
            goto cleanup;
        }
        if (pending != NULL && index == count - 1) {
            if ((*env)->Throw(env, pending) != JNI_OK) {
                fail(env, "java/lang/AssertionError", "Throw failed without a pending exception");
            }
            if (!(*env)->ExceptionCheck(env)) {
                fail(env, "java/lang/AssertionError", "Throw did not install a pending exception");
            }
        }

cleanup:
        release_globals(env, strong, weak);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->PopLocalFrame(env, NULL);
            return NULL;
        }
        result = (*env)->PopLocalFrame(env, rooted);
        if (!require_reference(env, result, value)) {
            return NULL;
        }
    }
    return result;
}

JNIEXPORT jobject JNICALL
Java_ObjectAdapterInteropTest_invokeAdapter0(JNIEnv *env, jclass test_type,
        jobject receiver, jclass type, jint selector, jint mode, jboolean expect_exception)
{
    (void)test_type;
    if (selector < 0 || selector > 1) {
        fail(env, "java/lang/IllegalArgumentException", "invalid adapter selector");
        return NULL;
    }
    return invoke_checked(env, receiver, type, "java/lang/Object",
                           selector == 0 ? "toAdapter" : "toAdpater", mode, expect_exception);
}

JNIEXPORT jobject JNICALL
Java_ObjectAdapterInteropTest_copyReferences0(JNIEnv *env, jclass test_type,
        jobject value, jint count, jthrowable pending)
{
    (void)test_type;
    return copy_references(env, value, count, pending);
}

JNIEXPORT jobject JNICALL
Java_ObjectJniContractBaseline_invokeBaseline0(JNIEnv *env, jclass test_type,
        jobject receiver, jclass type, jint selector, jint mode, jboolean expect_exception)
{
    (void)test_type;
    if (selector < 0 || selector > 1) {
        fail(env, "java/lang/IllegalArgumentException", "invalid baseline selector");
        return NULL;
    }
    return invoke_checked(env, receiver, type, "ObjectJniContractBaseline$Callback",
                           selector == 0 ? "bounce" : "alias", mode, expect_exception);
}

JNIEXPORT jobject JNICALL
Java_ObjectJniContractBaseline_copyReferences0(JNIEnv *env, jclass test_type,
        jobject value, jint count, jthrowable pending)
{
    (void)test_type;
    return copy_references(env, value, count, pending);
}

JNIEXPORT void JNICALL
Java_ObjectJniContractBaseline_lookupMissing0(JNIEnv *env, jclass test_type, jobject receiver)
{
    (void)test_type;
    (void)invoke_checked(env, receiver, NULL, "ObjectJniContractBaseline$Callback",
                          "missingContractMethod", 0, JNI_TRUE);
}
