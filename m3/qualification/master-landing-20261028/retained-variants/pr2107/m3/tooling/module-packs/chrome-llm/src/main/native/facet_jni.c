/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from cpullm-java/facet-release/native/facet_jni.c
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>

static jint popcount64(uint64_t value) {
    /* Portable integer population count; no platform-specific ISA requirement. */
    value -= (value >> 1) & UINT64_C(0x5555555555555555);
    value = (value & UINT64_C(0x3333333333333333)) + ((value >> 2) & UINT64_C(0x3333333333333333));
    value = (value + (value >> 4)) & UINT64_C(0x0f0f0f0f0f0f0f0f);
    return (jint)((value * UINT64_C(0x0101010101010101)) >> 56);
}
JNIEXPORT jint JNICALL Java_jdk_m3_llm_facets_FacetKernels_abiNative(JNIEnv *env, jclass type) {
    (void)env; (void)type; return 1;
}
JNIEXPORT jintArray JNICALL Java_jdk_m3_llm_facets_FacetKernels_gainsNative(
        JNIEnv *env, jclass type, jlongArray input, jlong required) {
    (void)type;
    if (input == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null facet masks");
        return NULL;
    }
    jsize count = (*env)->GetArrayLength(env, input);
    if (count > 65536) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "native facet batch limit");
        return NULL;
    }
    jintArray output = (*env)->NewIntArray(env, count);
    if (output == NULL) return NULL;
    jlong masks[256]; jint results[256];
    for (jsize begin = 0; begin < count; begin += 256) {
        jsize size = count - begin < 256 ? count - begin : 256;
        (*env)->GetLongArrayRegion(env, input, begin, size, masks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < size; ++i) results[i] = popcount64((uint64_t)masks[i] & (uint64_t)required);
        (*env)->SetIntArrayRegion(env, output, begin, size, results);
        if ((*env)->ExceptionCheck(env)) return NULL;
    }
    return output;
}
