// SPDX-License-Identifier: Apache-2.0
// Test-only ABI forwarder to the existing, unchanged M3 primitive region kernel.
// Does not implement native predicates, comparators, or collection storage.
#include <jni.h>

JNIEXPORT jlong JNICALL Java_com_m3_collections_M3MapCursorNativeProof_fold(
        JNIEnv *env, jclass cls, jintArray input);

JNIEXPORT jlong JNICALL Java_com_m3_collections_M3DeepFilterNativeProof_fold(
        JNIEnv *env, jclass cls, jintArray input) {
    return Java_com_m3_collections_M3MapCursorNativeProof_fold(env, cls, input);
}
