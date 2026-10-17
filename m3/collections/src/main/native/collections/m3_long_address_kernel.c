/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>

/*
 * Explicit batch gather only. No Java heap pointer survives this call. PrimitiveArrayCritical is
 * held for one bounded non-blocking loop; no JNI calls are made until all critical arrays release.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongAddressKernel_gatherNative(
        JNIEnv *env, jclass ignored, jlongArray payload, jintArray addresses,
        jint address_from, jint length, jlongArray output, jint output_from) {
    (void) ignored;
    if (payload == NULL || addresses == NULL || output == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null gather array");
        return;
    }

    jsize payload_length = (*env)->GetArrayLength(env, payload);
    jsize address_length = (*env)->GetArrayLength(env, addresses);
    jsize output_length = (*env)->GetArrayLength(env, output);
    if (address_from < 0 || length < 0 || output_from < 0
            || address_from > address_length - length
            || output_from > output_length - length) {
        jclass error = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
        if (error != NULL) (*env)->ThrowNew(env, error, "gather range");
        return;
    }

    jlong *values = (*env)->GetPrimitiveArrayCritical(env, payload, NULL);
    if (values == NULL) return;
    jint *index = (*env)->GetPrimitiveArrayCritical(env, addresses, NULL);
    if (index == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, payload, values, JNI_ABORT);
        return;
    }
    jlong *result = (*env)->GetPrimitiveArrayCritical(env, output, NULL);
    if (result == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, addresses, index, JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env, payload, values, JNI_ABORT);
        return;
    }

    jboolean invalid = JNI_FALSE;
    for (jint i = 0; i < length; i++) {
        jint physical = index[address_from + i];
        if (physical < 0 || physical >= payload_length) {
            invalid = JNI_TRUE;
            break;
        }
        result[output_from + i] = values[physical];
    }

    (*env)->ReleasePrimitiveArrayCritical(env, output, result, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, addresses, index, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, payload, values, JNI_ABORT);

    if (invalid == JNI_TRUE) {
        jclass error = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
        if (error != NULL) (*env)->ThrowNew(env, error, "physical address outside payload");
    }
}
