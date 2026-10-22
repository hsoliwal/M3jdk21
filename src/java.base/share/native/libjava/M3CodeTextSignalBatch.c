/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/* Target-owned implementation of java.lang.M3CodeTextSignalBatch.pack.
 * One batch entry, bounded region copies, no heap pins, native heap allocation,
 * retained references, application callbacks, or external precompute library.
 */
#include <stdint.h>
#include <string.h>

#include "java_lang_M3CodeTextSignalBatch.h"

#define M3_CODE_ROWS 128
#define M3_CODE_UNITS 1024

/* Low sixteen bits retain the existing M3CodeTextFacts schema. */
#define M3_CODE_BRACE       (UINT64_C(1) << 0)
#define M3_CODE_PAREN       (UINT64_C(1) << 1)
#define M3_CODE_SEMICOLON   (UINT64_C(1) << 2)
#define M3_CODE_BACKSLASH   (UINT64_C(1) << 3)
#define M3_CODE_REGEX_META  (UINT64_C(1) << 4)
#define M3_CODE_ARROW       (UINT64_C(1) << 5)
#define M3_CODE_METHOD_REF  (UINT64_C(1) << 6)
#define M3_CODE_ASSIGN      (UINT64_C(1) << 7)
#define M3_CODE_COMPARISON  (UINT64_C(1) << 8)
#define M3_CODE_QUANTIFIER  (UINT64_C(1) << 9)
#define M3_CODE_CLASS       (UINT64_C(1) << 10)
#define M3_CODE_DOT         (UINT64_C(1) << 11)
#define M3_CODE_COMMA       (UINT64_C(1) << 12)
#define M3_CODE_COLON       (UINT64_C(1) << 13)
#define M3_CODE_AMP_PIPE    (UINT64_C(1) << 14)
#define M3_CODE_WHITESPACE  (UINT64_C(1) << 15)

_Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long must carry 64 bits");

static void m3_code_throw(JNIEnv *env, const char *type_name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, type_name);
    if (type != NULL) {
        (void) (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static jboolean m3_code_range(jint offset, jint length, jsize capacity) {
    return offset >= 0 && length >= 0 && offset <= capacity
            && length <= capacity - offset ? JNI_TRUE : JNI_FALSE;
}

/* Adds one only inside an unsaturated byte lane; there is no cross-lane carry. */
static uint64_t m3_code_count(uint64_t packed, unsigned int shift) {
    return ((packed >> shift) & UINT64_C(255)) == UINT64_C(255)
            ? packed : packed + (UINT64_C(1) << shift);
}

/* Character.isWhitespace(char), Java 21 / Unicode 15.0. Surrogates are false. */
static jboolean m3_code_whitespace(jchar unit) {
    switch (unit) {
        case 0x0009: case 0x000a: case 0x000b: case 0x000c: case 0x000d:
        case 0x001c: case 0x001d: case 0x001e: case 0x001f: case 0x0020:
        case 0x1680: case 0x2028: case 0x2029: case 0x205f: case 0x3000:
            return JNI_TRUE;
        default:
            return (unit >= 0x2000 && unit <= 0x2006)
                    || (unit >= 0x2008 && unit <= 0x200a) ? JNI_TRUE : JNI_FALSE;
    }
}

static jboolean m3_code_pack(JNIEnv *env, jcharArray source, jint offset,
                            jint length, jlong *result) {
    jchar units[M3_CODE_UNITS];
    jchar previous = 0;
    uint64_t packed = 0;
    for (jint consumed = 0; consumed < length;) {
        jint take = length - consumed;
        if (take > M3_CODE_UNITS) take = M3_CODE_UNITS;
        (*env)->GetCharArrayRegion(env, source, offset + consumed, take, units);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        for (jint index = 0; index < take; index++) {
            jchar unit = units[index];
            /* Carry one UTF-16 unit across region seams, never across rows. */
            if (previous == ':' && unit == ':') packed |= M3_CODE_METHOD_REF;
            if (previous == '-' && unit == '>') packed |= M3_CODE_ARROW;
            previous = unit;
            switch (unit) {
                case '{': case '}':
                    packed |= M3_CODE_BRACE;
                    packed = m3_code_count(packed, 24);
                    packed = m3_code_count(packed, 48);
                    packed = m3_code_count(packed, 56);
                    break;
                case '(': case ')':
                    packed |= M3_CODE_PAREN;
                    packed = m3_code_count(packed, 32);
                    packed = m3_code_count(packed, 48);
                    packed = m3_code_count(packed, 56);
                    break;
                case ';':
                    packed |= M3_CODE_SEMICOLON;
                    packed = m3_code_count(packed, 16);
                    packed = m3_code_count(packed, 56);
                    break;
                case '\\':
                    packed |= M3_CODE_BACKSLASH;
                    packed = m3_code_count(packed, 40);
                    packed = m3_code_count(packed, 56);
                    break;
                case '.': case '*': case '+': case '?': case '|': case '^': case '$':
                    packed |= M3_CODE_REGEX_META;
                    if (unit == '*' || unit == '+' || unit == '?') packed |= M3_CODE_QUANTIFIER;
                    if (unit == '.') packed |= M3_CODE_DOT;
                    if (unit == '|') packed |= M3_CODE_AMP_PIPE;
                    packed = m3_code_count(packed, 48);
                    packed = m3_code_count(packed, 56);
                    break;
                case '[': case ']':
                    packed |= M3_CODE_REGEX_META | M3_CODE_CLASS;
                    packed = m3_code_count(packed, 48);
                    packed = m3_code_count(packed, 56);
                    break;
                case '=':
                    packed |= M3_CODE_ASSIGN;
                    packed = m3_code_count(packed, 56);
                    break;
                case '<': case '>': case '!':
                    packed |= M3_CODE_COMPARISON;
                    packed = m3_code_count(packed, 56);
                    break;
                case ',':
                    packed |= M3_CODE_COMMA;
                    packed = m3_code_count(packed, 56);
                    break;
                case ':':
                    packed |= M3_CODE_COLON;
                    packed = m3_code_count(packed, 56);
                    break;
                case '&':
                    packed |= M3_CODE_AMP_PIPE;
                    packed = m3_code_count(packed, 56);
                    break;
                case '-':
                    packed = m3_code_count(packed, 56);
                    break;
                default:
                    if (m3_code_whitespace(unit)) packed |= M3_CODE_WHITESPACE;
                    break;
            }
        }
        consumed += take;
    }
    /* Preserve all packed bits, including a saturated punctuation sign bit. */
    memcpy(result, &packed, sizeof(packed));
    return JNI_TRUE;
}

static jboolean m3_code_validate(JNIEnv *env, jcharArray units,
                                jintArray offsets, jintArray lengths,
                                jlong max_transfer, jlong max_units, jint max_rows,
                                jsize *unit_count, jsize *row_count) {
    if (units == NULL || offsets == NULL || lengths == NULL) {
        m3_code_throw(env, "java/lang/NullPointerException",
                units == NULL ? "units" : offsets == NULL ? "offsets" : "lengths");
        return JNI_FALSE;
    }
    if (max_transfer < 0 || max_units < 0 || max_rows < 1) {
        m3_code_throw(env, "java/lang/IllegalArgumentException", "invalid code-text signal limits");
        return JNI_FALSE;
    }
    *unit_count = (*env)->GetArrayLength(env, units);
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    *row_count = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    jsize length_count = (*env)->GetArrayLength(env, lengths);
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    if (*row_count != length_count || *row_count > max_rows) {
        m3_code_throw(env, "java/lang/IllegalArgumentException", "code-text row geometry");
        return JNI_FALSE;
    }
    if ((jlong) *unit_count > max_units) {
        m3_code_throw(env, "java/lang/IllegalArgumentException", "code-text UTF-16 budget exceeded");
        return JNI_FALSE;
    }
    /* Both array lengths are nonnegative signed 32-bit; this sum fits in 64 bits. */
    uint64_t transfer = UINT64_C(2) * (uint64_t) *unit_count
            + UINT64_C(16) * (uint64_t) *row_count;
    if (transfer > (uint64_t) max_transfer) {
        m3_code_throw(env, "java/lang/IllegalArgumentException", "code-text transfer budget exceeded");
        return JNI_FALSE;
    }
    jint row_offsets[M3_CODE_ROWS];
    jint row_lengths[M3_CODE_ROWS];
    for (jsize first = 0; first < *row_count;) {
        jsize take = *row_count - first;
        if (take > M3_CODE_ROWS) take = M3_CODE_ROWS;
        (*env)->GetIntArrayRegion(env, offsets, first, take, row_offsets);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        (*env)->GetIntArrayRegion(env, lengths, first, take, row_lengths);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        for (jsize row = 0; row < take; row++) {
            if (!m3_code_range(row_offsets[row], row_lengths[row], *unit_count)) {
                m3_code_throw(env, "java/lang/IllegalArgumentException", "code-text row outside packed image");
                return JNI_FALSE;
            }
        }
        first += take;
    }
    return JNI_TRUE;
}

JNIEXPORT jlongArray JNICALL
Java_java_lang_M3CodeTextSignalBatch_analyzeNative(
        JNIEnv *env, jclass owner, jcharArray units, jintArray offsets, jintArray lengths,
        jlong max_transfer, jlong max_units, jint max_rows) {
    (void) owner;
    jsize unit_count;
    jsize row_count;
    if (!m3_code_validate(env, units, offsets, lengths,
                         max_transfer, max_units, max_rows, &unit_count, &row_count)) {
        return NULL;
    }
    jlongArray result = (*env)->NewLongArray(env, row_count);
    if (result == NULL) {
        m3_code_throw(env, "java/lang/OutOfMemoryError", "code-text result allocation");
        return NULL;
    }
    jint row_offsets[M3_CODE_ROWS];
    jint row_lengths[M3_CODE_ROWS];
    jlong values[M3_CODE_ROWS];
    for (jsize first = 0; first < row_count;) {
        jsize take = row_count - first;
        if (take > M3_CODE_ROWS) take = M3_CODE_ROWS;
        (*env)->GetIntArrayRegion(env, offsets, first, take, row_offsets);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, lengths, first, take, row_lengths);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize row = 0; row < take; row++) {
            /* Recheck actual copied geometry even if a caller violates nonmutation. */
            if (!m3_code_range(row_offsets[row], row_lengths[row], unit_count)) {
                m3_code_throw(env, "java/lang/IllegalArgumentException", "code-text row outside packed image");
                return NULL;
            }
            if (!m3_code_pack(env, units, row_offsets[row], row_lengths[row], &values[row])) {
                return NULL;
            }
        }
        (*env)->SetLongArrayRegion(env, result, first, take, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        first += take;
    }
    return result;
}
