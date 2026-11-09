/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
#include <jni.h>

/* Exercise the installed libjava bridge after indexed-owner churn.
 * JtregNativeJdk.gmk links this fixture to the target library; no bridge
 * implementation is compiled into this native test. */
#include "m3_object.h"

JNIEXPORT jboolean JNICALL
Java_java_lang_M3ObjectCacheNativeStressTest_notifyContent(JNIEnv *env, jclass ignored,
                                                  jobject source)
{
    (void)ignored;
    return M3Object_NotifyContentChanged(env, source);
}
