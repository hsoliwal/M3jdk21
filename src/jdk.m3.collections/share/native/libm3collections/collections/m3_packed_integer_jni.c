/*
 * Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * First-party C17 translation of Synexia BOP's checked LSB-first spans.
 * Java PrimitiveBits remains the semantic owner. No JNI pointer survives a call.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_packed_integer_jni.c blob 951237483e0d (com.synexia.primitives.M3PackedNative -> jdk.internal.m3.collections.packed.PackedNative, 2 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "jdk_internal_m3_collections_packed_PackedNative.h"
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long must be 64 bits");

static void throw_error(JNIEnv *env, const char *type, const char *message) {
    jclass exception_class = (*env)->FindClass(env, type);
    if (exception_class != NULL) {
        (*env)->ThrowNew(env, exception_class, message);
        (*env)->DeleteLocalRef(env, exception_class);
    }
}

static int require_array(JNIEnv *env, jlongArray array, const char *name) {
    if (array == NULL) {
        throw_error(env, "java/lang/NullPointerException", name);
        return 0;
    }
    return 1;
}

static int require_width(JNIEnv *env, jint width) {
    if (width < 1 || width > 64) {
        throw_error(env, "java/lang/IllegalArgumentException", "width must be 1..64");
        return 0;
    }
    return 1;
}

static uint64_t width_mask(unsigned width) {
    return width == 64U ? UINT64_MAX : (UINT64_C(1) << width) - UINT64_C(1);
}

static jlong signed_bits(uint64_t bits) {
    jlong value;
    memcpy(&value, &bits, sizeof(value));
    return value;
}

static int word_count(JNIEnv *env, jint count, unsigned width, jsize *result) {
    if (count < 0) {
        throw_error(env, "java/lang/IllegalArgumentException", "negative value count");
        return 0;
    }
    uint64_t words = ((uint64_t) count * width + UINT64_C(63)) >> 6;
    if (words > (uint64_t) INT32_MAX - UINT64_C(8)) {
        throw_error(env, "java/lang/IllegalArgumentException", "packed word count exceeds array limit");
        return 0;
    }
    *result = (jsize) words;
    return 1;
}

static int value_fits(jlong value, unsigned width, jboolean is_signed) {
    if (width == 64U) {
        return 1;
    }
    if (is_signed == JNI_TRUE) {
        jlong boundary = (jlong) (UINT64_C(1) << (width - 1U));
        return value >= -boundary && value < boundary;
    }
    return value >= 0 && ((uint64_t) value & ~width_mask(width)) == 0;
}

static int valid_values(JNIEnv *env, const jlong *values, jsize count,
                        unsigned width, jboolean is_signed) {
    for (jsize index = 0; index < count; index++) {
        if (!value_fits(values[index], width, is_signed)) {
            throw_error(env, "java/lang/IllegalArgumentException",
                    is_signed == JNI_TRUE ? "value exceeds signed packed width"
                                          : "value exceeds unsigned packed width");
            return 0;
        }
    }
    return 1;
}

static void write_bits(jlong *words, uint64_t offset, unsigned width, uint64_t raw) {
    uint64_t word = offset >> 6;
    unsigned shift = (unsigned) (offset & UINT64_C(63));
    words[word] = signed_bits((uint64_t) words[word] | (raw << shift));
    if (shift != 0U && width > 64U - shift) {
        words[word + UINT64_C(1)] = signed_bits((uint64_t) words[word + UINT64_C(1)]
                | (raw >> (64U - shift)));
    }
}

static uint64_t read_bits(const jlong *words, uint64_t offset, unsigned width) {
    uint64_t word = offset >> 6;
    unsigned shift = (unsigned) (offset & UINT64_C(63));
    uint64_t raw = (uint64_t) words[word] >> shift;
    if (shift != 0U && width > 64U - shift) {
        raw |= (uint64_t) words[word + UINT64_C(1)] << (64U - shift);
    }
    return raw & width_mask(width);
}

static jlong decoded_value(uint64_t raw, unsigned width, jboolean is_signed) {
    if (is_signed == JNI_TRUE && width != 64U
            && (raw & (UINT64_C(1) << (width - 1U))) != 0) {
        raw |= ~width_mask(width);
    }
    return signed_bits(raw);
}

JNIEXPORT jlongArray JNICALL Java_jdk_internal_m3_collections_packed_PackedNative_packNative(
        JNIEnv *env, jclass owner, jlongArray input, jint bits_per_value, jboolean is_signed) {
    (void) owner;
    if (!require_array(env, input, "values") || !require_width(env, bits_per_value)) {
        return NULL;
    }
    unsigned width = (unsigned) bits_per_value;
    jsize count = (*env)->GetArrayLength(env, input);
    jsize words_count;
    if (!word_count(env, count, width, &words_count)) {
        return NULL;
    }
    if (count == 0) {
        return (*env)->NewLongArray(env, 0);
    }
    jlong *values = (*env)->GetLongArrayElements(env, input, NULL);
    if (values == NULL) {
        return NULL;
    }
    if (!valid_values(env, values, count, width, is_signed)) {
        (*env)->ReleaseLongArrayElements(env, input, values, JNI_ABORT);
        return NULL;
    }
    jlongArray output = (*env)->NewLongArray(env, words_count);
    if (output == NULL) {
        (*env)->ReleaseLongArrayElements(env, input, values, JNI_ABORT);
        return NULL;
    }
    jlong *words = (*env)->GetLongArrayElements(env, output, NULL);
    if (words == NULL) {
        (*env)->ReleaseLongArrayElements(env, input, values, JNI_ABORT);
        return NULL;
    }
    uint64_t mask = width_mask(width);
    for (jsize index = 0; index < count; index++) {
        write_bits(words, (uint64_t) index * width, width, (uint64_t) values[index] & mask);
    }
    (*env)->ReleaseLongArrayElements(env, output, words, 0);
    (*env)->ReleaseLongArrayElements(env, input, values, JNI_ABORT);
    return output;
}

JNIEXPORT jlongArray JNICALL Java_jdk_internal_m3_collections_packed_PackedNative_unpackNative(
        JNIEnv *env, jclass owner, jlongArray input, jint count,
        jint bits_per_value, jboolean is_signed) {
    (void) owner;
    if (!require_array(env, input, "words") || !require_width(env, bits_per_value)) {
        return NULL;
    }
    unsigned width = (unsigned) bits_per_value;
    jsize expected;
    if (!word_count(env, count, width, &expected)) {
        return NULL;
    }
    if ((*env)->GetArrayLength(env, input) != expected) {
        throw_error(env, "java/lang/IllegalArgumentException", "noncanonical packed word count");
        return NULL;
    }
    if (count == 0) {
        return (*env)->NewLongArray(env, 0);
    }
    jlong *words = (*env)->GetLongArrayElements(env, input, NULL);
    if (words == NULL) {
        return NULL;
    }
    unsigned tail_bits = (unsigned) (((uint64_t) count * width) & UINT64_C(63));
    if (tail_bits != 0U && ((uint64_t) words[expected - 1] & ~width_mask(tail_bits)) != 0) {
        throw_error(env, "java/lang/IllegalArgumentException", "nonzero packed tail bits");
        (*env)->ReleaseLongArrayElements(env, input, words, JNI_ABORT);
        return NULL;
    }
    jlongArray output = (*env)->NewLongArray(env, count);
    if (output == NULL) {
        (*env)->ReleaseLongArrayElements(env, input, words, JNI_ABORT);
        return NULL;
    }
    jlong *values = (*env)->GetLongArrayElements(env, output, NULL);
    if (values == NULL) {
        (*env)->ReleaseLongArrayElements(env, input, words, JNI_ABORT);
        return NULL;
    }
    for (jsize index = 0; index < count; index++) {
        values[index] = decoded_value(read_bits(words, (uint64_t) index * width, width),
                width, is_signed);
    }
    (*env)->ReleaseLongArrayElements(env, output, values, 0);
    (*env)->ReleaseLongArrayElements(env, input, words, JNI_ABORT);
    return output;
}
