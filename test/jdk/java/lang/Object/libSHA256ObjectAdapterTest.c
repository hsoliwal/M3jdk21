/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

#include <jni.h>

/* Invoke the published Object adapter through JNI. Never pin/copy the
 * source array or replace the JDK cryptographic provider. */
JNIEXPORT jbyteArray JNICALL
Java_SHA256ObjectAdapterTest_digestFromNative(JNIEnv *env, jclass test_type,
                                              jobject receiver)
{
    jclass object_type = NULL;
    jclass adapter_type = NULL;
    jmethodID adapt_method;
    jmethodID digest_method;
    jobject adapter = NULL;
    jbyteArray result = NULL;

    (void)test_type;
    object_type = (*env)->FindClass(env, "java/lang/Object");
    if (object_type == NULL) {
        goto done;
    }
    adapter_type = (*env)->FindClass(env, "java/security/SHA256Adapter");
    if (adapter_type == NULL) {
        goto done;
    }
    adapt_method = (*env)->GetMethodID(env, object_type, "toAdapter",
                                      "(Ljava/lang/Class;)Ljava/lang/Object;");
    if (adapt_method == NULL) {
        goto done;
    }
    adapter = (*env)->CallObjectMethod(env, receiver, adapt_method, adapter_type);
    if ((*env)->ExceptionCheck(env) || adapter == NULL) {
        goto done;
    }
    digest_method = (*env)->GetMethodID(env, adapter_type, "digest", "()[B");
    if (digest_method == NULL) {
        goto done;
    }
    result = (jbyteArray)(*env)->CallObjectMethod(env, adapter, digest_method);
    if ((*env)->ExceptionCheck(env)) {
        result = NULL;
    }

done:
    if (adapter != NULL) {
        (*env)->DeleteLocalRef(env, adapter);
    }
    if (adapter_type != NULL) {
        (*env)->DeleteLocalRef(env, adapter_type);
    }
    if (object_type != NULL) {
        (*env)->DeleteLocalRef(env, object_type);
    }
    return result;
}
