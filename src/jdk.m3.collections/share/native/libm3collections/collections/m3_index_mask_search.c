/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_index_mask_compactor.c blob cd35a84cbc97 lines 1-2, 11 (M3_CHUNK) and 135-278 (the firstEqualBits0 section; lines 3-133 are the count/select kernels M3 already owns as nativeCount/nativeSelect) (com.synexia.primitives.M3IndexMaskCompactor -> jdk.m3.collections.IndexMaskCompactor, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>

#define M3_CHUNK 256
/*
 * Bounded first-occurrence search. The stable kind ABI is PrimitiveKind.ordinal():
 * BOOLEAN=0, BYTE=1, SHORT=2, CHAR=3, INT=4, LONG=5, FLOAT=6, DOUBLE=7.
 * Check the actual array class before GetArrayLength or any typed Region call.
 * Integral equality compares low-width unsigned representations, reproducing Java
 * narrowing without implementation-defined signed integer conversions.
 */
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jfloat) == sizeof(uint32_t), "JNI float must be 32 bits");
_Static_assert(sizeof(jdouble) == sizeof(uint64_t), "JNI double must be 64 bits");

static jint m3_search_fail(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
    return -1;
}

static uint32_t m3_float_key(uint32_t raw) {
    if ((raw & UINT32_C(0x7f800000)) == UINT32_C(0x7f800000)
            && (raw & UINT32_C(0x007fffff)) != 0) {
        return UINT32_C(0x7fc00000);
    }
    return raw;
}

static uint64_t m3_double_key(uint64_t raw) {
    if ((raw & UINT64_C(0x7ff0000000000000)) == UINT64_C(0x7ff0000000000000)
            && (raw & UINT64_C(0x000fffffffffffff)) != 0) {
        return UINT64_C(0x7ff8000000000000);
    }
    return raw;
}

static uint32_t m3_float_bits(jfloat value) {
    uint32_t raw;
    memcpy(&raw, &value, sizeof(raw));
    return raw;
}

static uint64_t m3_double_bits(jdouble value) {
    uint64_t raw;
    memcpy(&raw, &value, sizeof(raw));
    return raw;
}

/* Each invocation reserves at most 256 * sizeof(jdouble) bytes of scan scratch. */
#define M3_SEARCH_REGION(member, array_type, getter, equal) \
    do { \
        for (jint offset = from; offset < end;) { \
            jint chunk = end - offset < M3_CHUNK ? end - offset : M3_CHUNK; \
            (*env)->getter(env, (array_type) values, offset, chunk, scratch.member); \
            if ((*env)->ExceptionCheck(env)) return -1; \
            for (jint i = 0; i < chunk; i++) { \
                if (equal) return offset + i; \
            } \
            offset += chunk; \
        } \
        return -1; \
    } while (0)

JNIEXPORT jint JNICALL
Java_jdk_m3_collections_IndexMaskCompactor_firstEqualBits0(
        JNIEnv *env, jclass owner, jint kind, jobject values,
        jint from, jint count, jlong bits) {
    (void) owner;
    static const char *const types[] = {"[B", "[B", "[S", "[C", "[I", "[J", "[F", "[D"};
    if (kind < 0 || kind >= 8) {
        return m3_search_fail(env, "java/lang/IllegalArgumentException", "unknown primitive kind");
    }
    if (values == NULL) {
        return m3_search_fail(env, "java/lang/NullPointerException", "values");
    }
    jclass expected_type = (*env)->FindClass(env, types[kind]);
    if (expected_type == NULL) return -1;
    jboolean matches = (*env)->IsInstanceOf(env, values, expected_type);
    (*env)->DeleteLocalRef(env, expected_type);
    if ((*env)->ExceptionCheck(env)) return -1;
    if (!matches) {
        return m3_search_fail(env, "java/lang/IllegalArgumentException",
                "array does not match primitive kind");
    }
    jsize length = (*env)->GetArrayLength(env, (jarray) values);
    if ((*env)->ExceptionCheck(env)) return -1;
    if (from < 0 || count < 0 || from > length || count > length - from) {
        return m3_search_fail(env, "java/lang/IndexOutOfBoundsException", "invalid search range");
    }
    /* The subtraction-based bounds check proves that this addition cannot overflow. */
    jint end = from + count;
    union {
        jbyte bytes[M3_CHUNK];
        jshort shorts[M3_CHUNK];
        jchar chars[M3_CHUNK];
        jint ints[M3_CHUNK];
        jlong longs[M3_CHUNK];
        jfloat floats[M3_CHUNK];
        jdouble doubles[M3_CHUNK];
    } scratch;
    switch (kind) {
        case 0: {
            int target = bits != 0;
            M3_SEARCH_REGION(bytes, jbyteArray, GetByteArrayRegion,
                    (scratch.bytes[i] != 0) == target);
        }
        case 1: {
            uint8_t target = (uint8_t) (uint64_t) bits;
            M3_SEARCH_REGION(bytes, jbyteArray, GetByteArrayRegion,
                    (uint8_t) scratch.bytes[i] == target);
        }
        case 2: {
            uint16_t target = (uint16_t) (uint64_t) bits;
            M3_SEARCH_REGION(shorts, jshortArray, GetShortArrayRegion,
                    (uint16_t) scratch.shorts[i] == target);
        }
        case 3: {
            uint16_t target = (uint16_t) (uint64_t) bits;
            M3_SEARCH_REGION(chars, jcharArray, GetCharArrayRegion,
                    (uint16_t) scratch.chars[i] == target);
        }
        case 4: {
            uint32_t target = (uint32_t) (uint64_t) bits;
            M3_SEARCH_REGION(ints, jintArray, GetIntArrayRegion,
                    (uint32_t) scratch.ints[i] == target);
        }
        case 5:
            M3_SEARCH_REGION(longs, jlongArray, GetLongArrayRegion, scratch.longs[i] == bits);
        case 6: {
            uint32_t target = m3_float_key((uint32_t) (uint64_t) bits);
            M3_SEARCH_REGION(floats, jfloatArray, GetFloatArrayRegion,
                    m3_float_key(m3_float_bits(scratch.floats[i])) == target);
        }
        case 7: {
            uint64_t target = m3_double_key((uint64_t) bits);
            M3_SEARCH_REGION(doubles, jdoubleArray, GetDoubleArrayRegion,
                    m3_double_key(m3_double_bits(scratch.doubles[i])) == target);
        }
        default:
            return m3_search_fail(env, "java/lang/IllegalArgumentException", "unknown primitive kind");
    }
}
