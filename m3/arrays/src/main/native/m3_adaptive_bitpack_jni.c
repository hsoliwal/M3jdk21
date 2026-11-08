/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include "include/m3_adaptive_bitpack.h"
#include "../../../../native/include/m3_jni_core.h"

_Static_assert(sizeof(jint) == sizeof(int32_t), "JNI int must be 32-bit");
_Static_assert(sizeof(jbyte) == sizeof(uint8_t), "JNI byte must be 8-bit");

static int invalid(JNIEnv *env, const char *message) {
    m3_jni_throw_new(env, "java/lang/IllegalArgumentException", message);
    return 0;
}

static int checked_geometry(JNIEnv *env, jint length, jint bits, size_t *bytes) {
    if (length < 0 || length > (jint)M3_ADAPTIVE_MAX || bits < 0 || bits > 32)
        return invalid(env, "native adaptive bitplane length/width");
    *bytes = ((size_t)length * (size_t)bits + 7u) / 8u;
    if (*bytes > M3_ADAPTIVE_MAX)
        return invalid(env, "native adaptive bitplane payload bound");
    return 1;
}

JNIEXPORT jbyteArray JNICALL Java_jdk_m3_arrays_packed_AdaptivePackedIntNative_nativeEncode(
        JNIEnv *env, jclass clazz, jintArray values, jint base, jint bits) {
    (void)clazz;
    if (!values) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "values");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, values);
    if ((*env)->ExceptionCheck(env)) return NULL;
    size_t bytes;
    if (!checked_geometry(env, length, bits, &bytes)) return NULL;
    jint raw[M3_ADAPTIVE_MAX];
    int32_t input[M3_ADAPTIVE_MAX];
    uint8_t output[M3_ADAPTIVE_MAX];
    if (length != 0) (*env)->GetIntArrayRegion(env, values, 0, length, raw);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < length; ++i) input[i] = (int32_t)raw[i];
    if (m3_adaptive_encode(input, (size_t)length, (int32_t)base,
                           (unsigned)bits, output, sizeof(output)) != 0) {
        invalid(env, "adaptive packed value outside base/width");
        return NULL;
    }
    jbyteArray result = (*env)->NewByteArray(env, (jsize)bytes);
    if (!result || (*env)->ExceptionCheck(env)) return NULL;
    if (bytes != 0u) (*env)->SetByteArrayRegion(env, result, 0, (jsize)bytes, (const jbyte *)output);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_arrays_packed_AdaptivePackedIntNative_nativeDecode(
        JNIEnv *env, jclass clazz, jbyteArray bytes, jint length, jint base, jint bits) {
    (void)clazz;
    if (!bytes) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "bytes");
        return NULL;
    }
    size_t expected;
    if (!checked_geometry(env, length, bits, &expected)) return NULL;
    jsize nbytes = (*env)->GetArrayLength(env, bytes);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if ((size_t)nbytes != expected) {
        invalid(env, "native adaptive packed byte count mismatch");
        return NULL;
    }
    jbyte raw[M3_ADAPTIVE_MAX];
    uint8_t input[M3_ADAPTIVE_MAX];
    int32_t output[M3_ADAPTIVE_MAX];
    jint resultData[M3_ADAPTIVE_MAX];
    if (nbytes != 0) (*env)->GetByteArrayRegion(env, bytes, 0, nbytes, raw);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (nbytes != 0) memcpy(input, raw, (size_t)nbytes);
    if (m3_adaptive_decode(input, (size_t)nbytes, (size_t)length,
                           (int32_t)base, (unsigned)bits, output, M3_ADAPTIVE_MAX) != 0) {
        invalid(env, "adaptive packed padding or signed range");
        return NULL;
    }
    for (jint i = 0; i < length; ++i) resultData[i] = (jint)output[i];
    jintArray result = (*env)->NewIntArray(env, length);
    if (!result || (*env)->ExceptionCheck(env)) return NULL;
    if (length != 0) (*env)->SetIntArrayRegion(env, result, 0, length, resultData);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
