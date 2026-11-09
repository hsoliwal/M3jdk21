/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 */
/* Test-only provider: exercise Java's last-resort allocation failure boundary. */
#include <jni.h>

JNIEXPORT jbyteArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeEncodePacked(
        JNIEnv *env, jclass owner, jlongArray values, jint bits) {
    (void)env; (void)owner; (void)values; (void)bits;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeDecodePacked(
        JNIEnv *env, jclass owner, jbyteArray values, jint bits) {
    (void)env; (void)owner; (void)values; (void)bits;
    return NULL;
}
JNIEXPORT jbyteArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeEncodeUnsigned64(
        JNIEnv *env, jclass owner, jlongArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeDecodeUnsigned64(
        JNIEnv *env, jclass owner, jbyteArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jbyteArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeEncodeZigZag64(
        JNIEnv *env, jclass owner, jlongArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_jdk_m3_arrays_ArrayNative_nativeDecodeZigZag64(
        JNIEnv *env, jclass owner, jbyteArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_ArrayNative_nativeQueryPacked(
        JNIEnv *env, jclass owner, jbyteArray bytes, jint bits,
        jint from, jint to, jlong lower, jlong upper, jboolean first) {
    (void)env; (void)owner; (void)bytes; (void)bits;
    (void)from; (void)to; (void)lower; (void)upper; (void)first;
    return (-2147483647 - 1);
}
