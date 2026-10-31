/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: target-owned M3 arrays codec adaptation from Synexia
 * 6f878f9e5d6c5044eda15efb9e743594e70ce7f7; six packed/full64 operations only. */
#include <jni.h>
#include <stdint.h>
#include "m3_packed_codec.h"
#include "../../../../native/include/m3_jni_core.h"

_Static_assert(sizeof(jlong) == 8, "JNI jlong must be 64-bit");

/* JNI permits a null allocation without a pending exception. */
static int allocated(JNIEnv *env, jarray array) {
    if (array) return 1;
    m3_jni_throw_new(env, "java/lang/OutOfMemoryError", "packed codec result allocation failed");
    return 0;
}

static int count(JNIEnv *env, jarray array, jsize *n) {
    if ((*env)->ExceptionCheck(env)) return 0;
    if (!array) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "codec array");
        return 0;
    }
    *n = (*env)->GetArrayLength(env, array);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (*n < 0 || *n > (jsize)M3_PACKED_CODEC_MAX) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "codec array bound");
        return 0;
    }
    return 1;
}

static int status_ok(JNIEnv *env, int status) {
    if (status == M3_CODEC_OK) return 1;
    m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "codec input/format/capacity contract");
    return 0;
}

/* Refuse impossible geometry before copying input into bounded scratch. */
static int packed_geometry(JNIEnv *env, jsize n, jint bits, int encode) {
    if (bits != 40 && bits != 48 && bits != 56) return status_ok(env, M3_CODEC_FORMAT);
    size_t width = (size_t)(bits / 8);
    if (encode) {
        if ((size_t)n > M3_PACKED_CODEC_MAX / width) return status_ok(env, M3_CODEC_BOUND);
    } else if ((size_t)n % width != 0) {
        return status_ok(env, M3_CODEC_FORMAT);
    }
    return 1;
}

enum m3_codec_mode { M3_MODE_PACKED, M3_MODE_UNSIGNED64, M3_MODE_ZIGZAG64 };

static jbyteArray encode_extended(JNIEnv *env, jlongArray source,
                                  enum m3_codec_mode mode, jint bits) {
    jsize n;
    if (!count(env, source, &n) || (*env)->ExceptionCheck(env)) return NULL;
    if (mode == M3_MODE_PACKED && !packed_geometry(env, n, bits, 1)) return NULL;
    jlong raw[M3_PACKED_CODEC_MAX]; int64_t input[M3_PACKED_CODEC_MAX];
    uint8_t output[M3_PACKED_CODEC_MAX]; jbyte bytes[M3_PACKED_CODEC_MAX];
    if (n) (*env)->GetLongArrayRegion(env, source, 0, n, raw);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < n; ++i) input[i] = (int64_t)raw[i];
    size_t written = 0;
    int status;
    switch (mode) {
        case M3_MODE_PACKED:
            status = m3_codec_encode_packed(input, (size_t)n, (int)bits,
                                               output, M3_PACKED_CODEC_MAX, &written);
            break;
        case M3_MODE_UNSIGNED64:
            status = m3_codec_encode_unsigned64(input, (size_t)n,
                                                   output, M3_PACKED_CODEC_MAX, &written);
            break;
        case M3_MODE_ZIGZAG64:
            status = m3_codec_encode_zigzag64(input, (size_t)n,
                                                 output, M3_PACKED_CODEC_MAX, &written);
            break;
        default:
            status = M3_CODEC_ARGUMENT;
    }
    if (!status_ok(env, status)) return NULL;
    for (size_t i = 0; i < written; ++i)
        bytes[i] = output[i] < 128 ? (jbyte)output[i] : (jbyte)((int)output[i] - 256);
    jbyteArray result = (*env)->NewByteArray(env, (jsize)written);
    if (!allocated(env, result) || (*env)->ExceptionCheck(env)) return NULL;
    if (written) (*env)->SetByteArrayRegion(env, result, 0, (jsize)written, bytes);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

static jlongArray decode_extended(JNIEnv *env, jbyteArray source,
                                  enum m3_codec_mode mode, jint bits) {
    jsize n;
    if (!count(env, source, &n) || (*env)->ExceptionCheck(env)) return NULL;
    if (mode == M3_MODE_PACKED && !packed_geometry(env, n, bits, 0)) return NULL;
    jbyte bytes[M3_PACKED_CODEC_MAX]; uint8_t input[M3_PACKED_CODEC_MAX];
    int64_t output[M3_PACKED_CODEC_MAX]; jlong raw[M3_PACKED_CODEC_MAX];
    if (n) (*env)->GetByteArrayRegion(env, source, 0, n, bytes);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < n; ++i) input[i] = (uint8_t)bytes[i];
    size_t written = 0;
    int status;
    switch (mode) {
        case M3_MODE_PACKED:
            status = m3_codec_decode_packed(input, (size_t)n, (int)bits,
                                               output, M3_PACKED_CODEC_MAX, &written);
            break;
        case M3_MODE_UNSIGNED64:
            status = m3_codec_decode_unsigned64(input, (size_t)n,
                                                   output, M3_PACKED_CODEC_MAX, &written);
            break;
        case M3_MODE_ZIGZAG64:
            status = m3_codec_decode_zigzag64(input, (size_t)n,
                                                 output, M3_PACKED_CODEC_MAX, &written);
            break;
        default:
            status = M3_CODEC_ARGUMENT;
    }
    if (!status_ok(env, status)) return NULL;
    for (size_t i = 0; i < written; ++i) raw[i] = (jlong)output[i];
    jlongArray result = (*env)->NewLongArray(env, (jsize)written);
    if (!allocated(env, result) || (*env)->ExceptionCheck(env)) return NULL;
    if (written) (*env)->SetLongArrayRegion(env, result, 0, (jsize)written, raw);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodePacked(
        JNIEnv *env, jclass clazz, jlongArray source, jint bits) {
    (void)clazz;
    return encode_extended(env, source, M3_MODE_PACKED, bits);
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodePacked(
        JNIEnv *env, jclass clazz, jbyteArray source, jint bits) {
    (void)clazz;
    return decode_extended(env, source, M3_MODE_PACKED, bits);
}
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeUnsigned64(
        JNIEnv *env, jclass clazz, jlongArray source) {
    (void)clazz;
    return encode_extended(env, source, M3_MODE_UNSIGNED64, 0);
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeUnsigned64(
        JNIEnv *env, jclass clazz, jbyteArray source) {
    (void)clazz;
    return decode_extended(env, source, M3_MODE_UNSIGNED64, 0);
}
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeZigZag64(
        JNIEnv *env, jclass clazz, jlongArray source) {
    (void)clazz;
    return encode_extended(env, source, M3_MODE_ZIGZAG64, 0);
}
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeZigZag64(
        JNIEnv *env, jclass clazz, jbyteArray source) {
    (void)clazz;
    return decode_extended(env, source, M3_MODE_ZIGZAG64, 0);
}
