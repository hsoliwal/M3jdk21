/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
/* Test-only fault library. Never install or package this as the actual array provider. */
#include <jni.h>

#define NULL_ALLOCATION(type, plural) \
JNIEXPORT type JNICALL Java_jdk_m3_arrays_M3ArrayNative_nativeAllocate##plural( \
    JNIEnv *env, jclass owner, jint length) { \
    (void)env; (void)owner; (void)length; return NULL; \
}
NULL_ALLOCATION(jobjectArray, Objects)
NULL_ALLOCATION(jbooleanArray, Booleans)
NULL_ALLOCATION(jbyteArray, Bytes)
NULL_ALLOCATION(jcharArray, Chars)
NULL_ALLOCATION(jshortArray, Shorts)
NULL_ALLOCATION(jintArray, Ints)
NULL_ALLOCATION(jlongArray, Longs)
NULL_ALLOCATION(jfloatArray, Floats)
NULL_ALLOCATION(jdoubleArray, Doubles)
