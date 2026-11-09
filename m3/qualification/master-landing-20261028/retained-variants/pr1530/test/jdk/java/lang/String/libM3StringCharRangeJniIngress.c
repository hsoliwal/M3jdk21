/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

#include "jni.h"

/*
 * Test-only caller for the package-private String JNI ingress. This crosses the
 * JNI boundary exactly as a native client does, resolving and invoking the
 * declared method rather than calling it reflectively from Java.
 */
JNIEXPORT jstring JNICALL
Java_M3StringCharRangeJniIngressTest_invokeJoinRanges(
        JNIEnv *env,
        jclass caller,
        jobjectArray sources,
        jintArray starts,
        jintArray lengths) {
    (void) caller;

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL) {
        return NULL; /* Preserve the pending exception. */
    }

    jmethodID join = (*env)->GetStaticMethodID(
            env, stringClass, "m3JoinCharRanges", "([[C[I[I)Ljava/lang/String;");
    if (join == NULL) {
        (*env)->DeleteLocalRef(env, stringClass);
        return NULL; /* Preserve the pending exception. */
    }

    jstring result = (jstring) (*env)->CallStaticObjectMethod(
            env, stringClass, join, sources, starts, lengths);
    (*env)->DeleteLocalRef(env, stringClass);
    return result;
}
