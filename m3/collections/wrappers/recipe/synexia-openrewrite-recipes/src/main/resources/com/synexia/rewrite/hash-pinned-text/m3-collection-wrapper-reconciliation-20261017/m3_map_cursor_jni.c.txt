// SPDX-License-Identifier: Apache-2.0
// Verification-only bounded JNI region copy. Never store heap pointers or callbacks.
#include <jni.h>
#include <stdint.h>

JNIEXPORT jlong JNICALL Java_com_m3_collections_M3MapCursorNativeProof_fold(
        JNIEnv *env, jclass cls, jintArray input) {
    (void) cls;
    if (input == NULL) {
        jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (npe != NULL) {
            (*env)->ThrowNew(env, npe, "indices");
            (*env)->DeleteLocalRef(env, npe);
        }
        return (jlong) 0;
    }
    jsize count = (*env)->GetArrayLength(env, input);
    if ((*env)->ExceptionCheck(env)) return (jlong) 0;
    uint64_t state = UINT64_C(0xcbf29ce484222325);
    jint scratch[256];
    jsize offset = 0;
    while (offset < count) {
        jsize remaining = count - offset;
        jsize batch = remaining < (jsize) 256 ? remaining : (jsize) 256;
        (*env)->GetIntArrayRegion(env, input, offset, batch, scratch);
        if ((*env)->ExceptionCheck(env)) return (jlong) 0;
        for (jsize i = 0; i < batch; i++) {
            state ^= (uint64_t) (uint32_t) scratch[i];
            state *= UINT64_C(0x100000001b3);
        }
        offset += batch;
    }
    return (jlong) state;
}
