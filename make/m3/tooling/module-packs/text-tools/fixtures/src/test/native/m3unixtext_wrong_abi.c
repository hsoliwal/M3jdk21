/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Deliberately incorrect ABI for an isolated fail-closed native-linkage test.
 */
#include <jni.h>
JNIEXPORT jint JNICALL
Java_jdk_m3_text_NativeLineBatch_abi0(JNIEnv *env, jclass owner) {
    (void)env;
    (void)owner;
    return 0x00020000;
}
