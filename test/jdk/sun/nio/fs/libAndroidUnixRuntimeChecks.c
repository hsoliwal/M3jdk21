/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 */
#include "jni.h"

#if defined(__linux__)
#include <dlfcn.h>
#include <stddef.h>
#endif

JNIEXPORT jboolean JNICALL
Java_AndroidUnixRuntimeChecks_hasDescriptorRelativeCalls(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
#if defined(__linux__)
    const char* required[] = {"openat64", "fstatat64", "unlinkat", "renameat", "fdopendir"};
    for (size_t i = 0; i < sizeof(required) / sizeof(required[0]); i++) {
        if (dlsym(RTLD_DEFAULT, required[i]) == NULL) return JNI_FALSE;
    }
    if (dlsym(RTLD_DEFAULT, "futimesat") != NULL) return JNI_TRUE;
#if defined(__ANDROID__)
    if (dlsym(RTLD_DEFAULT, "futimens") != NULL) return JNI_TRUE;
#endif
#endif
    return JNI_FALSE;
}
