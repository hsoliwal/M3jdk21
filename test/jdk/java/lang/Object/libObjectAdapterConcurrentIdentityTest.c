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

/* One JNI virtual call; method and temporary class reference are thread-local. */
JNIEXPORT jobject JNICALL
Java_ObjectAdapterConcurrentIdentityTest_adaptNative(JNIEnv *env, jclass test_class,
                                                     jobject receiver, jclass requested)
{
    jclass object_class;
    jmethodID method;

    (void)test_class;
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
    /* JNI virtual dispatch and any pending exception belong to the VM. */
    return (*env)->CallObjectMethod(env, receiver, method, requested);
}
