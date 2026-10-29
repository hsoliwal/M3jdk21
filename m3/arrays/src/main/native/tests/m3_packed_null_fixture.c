/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 */
/* Test-only provider: exercise Java's last-resort allocation failure boundary. */
#include <jni.h>

JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodePacked(
        JNIEnv *env, jclass owner, jlongArray values, jint bits) {
    (void)env; (void)owner; (void)values; (void)bits;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodePacked(
        JNIEnv *env, jclass owner, jbyteArray values, jint bits) {
    (void)env; (void)owner; (void)values; (void)bits;
    return NULL;
}
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeUnsigned64(
        JNIEnv *env, jclass owner, jlongArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeUnsigned64(
        JNIEnv *env, jclass owner, jbyteArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeZigZag64(
        JNIEnv *env, jclass owner, jlongArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeZigZag64(
        JNIEnv *env, jclass owner, jbyteArray values) {
    (void)env; (void)owner; (void)values;
    return NULL;
}
