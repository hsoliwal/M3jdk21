/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_value_bits.c blob 2103edb20881 (com.synexia.primitives.PrimitiveFloat16 -> jdk.internal.m3.collections.PrimitiveFloat16, 2 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include "jdk_internal_m3_collections_PrimitiveFloat16.h"
#include <float.h>
#include <limits.h>
#include <stdint.h>
#include <string.h>

#ifndef __STDC_IEC_559__
#error "m3_value_bits requires IEC 60559 floating point"
#endif
_Static_assert(CHAR_BIT == 8, "eight-bit bytes required");
_Static_assert(sizeof(jfloat) == sizeof(uint32_t), "binary32 jfloat required");
_Static_assert(sizeof(float) == sizeof(uint32_t), "binary32 float required");
_Static_assert(sizeof(jshort) == sizeof(uint16_t), "16-bit jshort required");
_Static_assert(FLT_RADIX == 2 && FLT_MANT_DIG == 24 && FLT_MAX_EXP == 128,
               "IEEE binary32 representation required");

enum { M3_FLOAT16_CHUNK = 256 };

static uint32_t m3_float_bits(jfloat value) {
    uint32_t bits;
    memcpy(&bits, &value, sizeof(bits));
    return bits;
}

static jfloat m3_float_value(uint32_t bits) {
    jfloat value;
    memcpy(&value, &bits, sizeof(value));
    return value;
}

static uint16_t m3_to_binary16(uint32_t bits) {
    uint32_t sign = (bits >> 16) & UINT32_C(0x8000);
    uint32_t exponent = (bits >> 23) & UINT32_C(0xff);
    uint32_t fraction = bits & UINT32_C(0x7fffff);
    if (exponent == UINT32_C(255)) {
        return (uint16_t) (fraction != 0 ? UINT32_C(0x7e00) : sign | UINT32_C(0x7c00));
    }
    if (exponent > UINT32_C(142)) return (uint16_t) (sign | UINT32_C(0x7c00));
    if (exponent < UINT32_C(102)) return (uint16_t) sign;
    if (exponent < UINT32_C(113)) {
        uint32_t significand = fraction | UINT32_C(0x800000);
        uint32_t shift = UINT32_C(126) - exponent;
        uint32_t rounded = significand >> shift;
        uint32_t remainder = significand & ((UINT32_C(1) << shift) - UINT32_C(1));
        uint32_t halfway = UINT32_C(1) << (shift - UINT32_C(1));
        if (remainder > halfway || (remainder == halfway && (rounded & UINT32_C(1)) != 0)) {
            rounded++;
        }
        return (uint16_t) (sign | rounded);
    }
    uint32_t rounded = ((exponent - UINT32_C(112)) << 10) | (fraction >> 13);
    uint32_t remainder = fraction & UINT32_C(0x1fff);
    if (remainder > UINT32_C(0x1000)
            || (remainder == UINT32_C(0x1000) && (rounded & UINT32_C(1)) != 0)) {
        rounded++;
    }
    return (uint16_t) (sign | rounded);
}

static uint32_t m3_from_binary16(uint16_t code) {
    uint32_t sign = ((uint32_t) code & UINT32_C(0x8000)) << 16;
    uint32_t exponent = ((uint32_t) code >> 10) & UINT32_C(0x1f);
    uint32_t fraction = (uint32_t) code & UINT32_C(0x3ff);
    if (exponent == UINT32_C(31)) {
        return fraction != 0 ? UINT32_C(0x7fc00000) : sign | UINT32_C(0x7f800000);
    }
    if (exponent == 0) {
        if (fraction == 0) return sign;
        int unbiased = -14;
        while ((fraction & UINT32_C(0x400)) == 0) {
            fraction <<= 1;
            unbiased--;
        }
        return sign | ((uint32_t) (unbiased + 127) << 23)
                | ((fraction & UINT32_C(0x3ff)) << 13);
    }
    return sign | ((exponent + UINT32_C(112)) << 23) | (fraction << 13);
}

static uint16_t m3_to_bfloat16(uint32_t bits) {
    if ((bits & UINT32_C(0x7fffffff)) > UINT32_C(0x7f800000)) return UINT16_C(0x7fc0);
    return (uint16_t) ((bits + UINT32_C(0x7fff) + ((bits >> 16) & UINT32_C(1))) >> 16);
}

static uint32_t m3_from_bfloat16(uint16_t code) {
    if ((code & UINT16_C(0x7fff)) > UINT16_C(0x7f80)) return UINT32_C(0x7fc00000);
    return (uint32_t) code << 16;
}

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass exception = (*env)->FindClass(env, type);
    if (exception == NULL) return;
    if (!(*env)->ExceptionCheck(env)) {
        (void) (*env)->ThrowNew(env, exception, message);
    }
    (*env)->DeleteLocalRef(env, exception);
}

static int m3_validate(JNIEnv *env, jarray source, jint source_offset,
        jarray target, jint target_offset, jint length) {
    /* Preserve an existing JNI failure before any array or class operation. */
    if ((*env)->ExceptionCheck(env)) return 0;
    if (source == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "source");
        return 0;
    }
    if (target == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "target");
        return 0;
    }
    jsize source_length = (*env)->GetArrayLength(env, source);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize target_length = (*env)->GetArrayLength(env, target);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (length < 0 || source_offset < 0 || source_offset > source_length
            || length > source_length - source_offset
            || target_offset < 0 || target_offset > target_length
            || length > target_length - target_offset) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "float16 slice outside array");
        return 0;
    }
    return 1;
}

JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_PrimitiveFloat16_encode0(
        JNIEnv *env, jclass owner, jfloatArray source, jint source_offset,
        jshortArray target, jint target_offset, jint length, jboolean bfloat16) {
    (void) owner;
    if (!m3_validate(env, source, source_offset, target, target_offset, length)) return;
    jfloat values[M3_FLOAT16_CHUNK];
    jshort codes[M3_FLOAT16_CHUNK];
    jint offset = 0;
    while (offset < length) {
        jint count = length - offset;
        if (count > M3_FLOAT16_CHUNK) count = M3_FLOAT16_CHUNK;
        (*env)->GetFloatArrayRegion(env, source, source_offset + offset, count, values);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < count; i++) {
            uint32_t bits = m3_float_bits(values[i]);
            uint16_t code = bfloat16 != JNI_FALSE ? m3_to_bfloat16(bits) : m3_to_binary16(bits);
            memcpy(&codes[i], &code, sizeof(code));
        }
        (*env)->SetShortArrayRegion(env, target, target_offset + offset, count, codes);
        if ((*env)->ExceptionCheck(env)) return;
        offset += count;
    }
}

JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_PrimitiveFloat16_decode0(
        JNIEnv *env, jclass owner, jshortArray source, jint source_offset,
        jfloatArray target, jint target_offset, jint length, jboolean bfloat16) {
    (void) owner;
    if (!m3_validate(env, source, source_offset, target, target_offset, length)) return;
    jshort codes[M3_FLOAT16_CHUNK];
    jfloat values[M3_FLOAT16_CHUNK];
    jint offset = 0;
    while (offset < length) {
        jint count = length - offset;
        if (count > M3_FLOAT16_CHUNK) count = M3_FLOAT16_CHUNK;
        (*env)->GetShortArrayRegion(env, source, source_offset + offset, count, codes);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < count; i++) {
            uint16_t code;
            memcpy(&code, &codes[i], sizeof(code));
            uint32_t bits = bfloat16 != JNI_FALSE ? m3_from_bfloat16(code) : m3_from_binary16(code);
            values[i] = m3_float_value(bits);
        }
        (*env)->SetFloatArrayRegion(env, target, target_offset + offset, count, values);
        if ((*env)->ExceptionCheck(env)) return;
        offset += count;
    }
}
