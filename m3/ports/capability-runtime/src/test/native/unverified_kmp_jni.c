// SPDX-License-Identifier: Apache-2.0
// Deliberately incomplete test fixture: no LF installation/ABI or SHA identity.
// The Java native routing must refuse this independently loaded KMP symbol.
#include <jni.h>

JNIEXPORT jintArray JNICALL
Java_com_m3_runtime_capability_BytePatternSearch_findKmp0(
        JNIEnv *env, jclass owner, jobject buffer, jint start, jint length,
        jbyteArray needle, jint maxMatches) {
    (void) owner; (void) buffer; (void) start; (void) length;
    (void) needle; (void) maxMatches;
    jint forged = 0;
    jintArray result = (*env)->NewIntArray(env, 1);
    if (result != NULL) (*env)->SetIntArrayRegion(env, result, 0, 1, &forged);
    return result;
}
