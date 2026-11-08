/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include "m3_int_codebook.h"

_Static_assert(sizeof(jint) == sizeof(int32_t), "JNI jint must be 32-bit");
_Static_assert(sizeof(jbyte) == sizeof(uint8_t), "JNI jbyte must be 8-bit");

static void throw_invalid(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static void throw_null(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, "java/lang/NullPointerException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, "null codebook input");
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_arrays_packed_IntCodebookNative_nativeDecode(
        JNIEnv *env, jclass owner, jbyteArray encoded,
        jintArray codebook, jint length, jint bits) {
    (void)owner;
    if (!encoded || !codebook) { throw_null(env); return NULL; }
    if (length < 0 || length > (jint)M3_INT_CODEBOOK_MAX || bits < 0 || bits > 12) {
        throw_invalid(env, "invalid codebook batch length/width");
        return NULL;
    }
    jsize bytes_count = (*env)->GetArrayLength(env, encoded);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize codes_count = (*env)->GetArrayLength(env, codebook);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (bytes_count > (jsize)M3_INT_CODEBOOK_MAX
            || codes_count <= 0 || codes_count > (jsize)M3_INT_CODEBOOK_MAX) {
        throw_invalid(env, "invalid codebook batch dimensions");
        return NULL;
    }
    jbyte encoded_copy[M3_INT_CODEBOOK_MAX];
    uint8_t ids[M3_INT_CODEBOOK_MAX];
    jint codes_copy[M3_INT_CODEBOOK_MAX];
    int32_t symbols[M3_INT_CODEBOOK_MAX];
    int32_t values[M3_INT_CODEBOOK_MAX];
    jint output[M3_INT_CODEBOOK_MAX];
    if (bytes_count > 0) (*env)->GetByteArrayRegion(env, encoded, 0, bytes_count, encoded_copy);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (codes_count > 0) (*env)->GetIntArrayRegion(env, codebook, 0, codes_count, codes_copy);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (bytes_count > 0) memcpy(ids, encoded_copy, (size_t)bytes_count);
    for (jsize i = 0; i < codes_count; i++) symbols[i] = (int32_t)codes_copy[i];
    if (m3_int_codebook_decode(ids, (size_t)bytes_count, symbols,
                               (size_t)codes_count, (size_t)length,
                               (unsigned)bits, values, M3_INT_CODEBOOK_MAX) != 0) {
        throw_invalid(env, "invalid codebook index or padding");
        return NULL;
    }
    for (jint i = 0; i < length; ++i) output[i] = (jint)values[i];
    jintArray result = (*env)->NewIntArray(env, length);
    if (!result) {
        if (!(*env)->ExceptionCheck(env)) {
            jclass type = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
            if (type != NULL) {
                (void)(*env)->ThrowNew(env, type, "codebook result allocation failed");
                (*env)->DeleteLocalRef(env, type);
            }
        }
        return NULL;
    }
    if (length > 0) (*env)->SetIntArrayRegion(env, result, 0, length, output);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
