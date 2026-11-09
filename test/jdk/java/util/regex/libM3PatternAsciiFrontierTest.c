/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
/* Independent TEST-ONLY scalar DP over exported JDK predicate masks.
 * Does not use or trust the production six-jump closure. No JNI pointers retained. */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

static void bad(JNIEnv *env, const char *kind, const char *message) {
    jclass type = (*env)->FindClass(env, kind);
    if (type != NULL) (void)(*env)->ThrowNew(env, type, message);
}

JNIEXPORT jint JNICALL Java_M3PatternAsciiFrontierTest_scalarOracle(
        JNIEnv *env, jclass type, jlongArray image, jlong repeated, jint atoms, jstring input) {
    (void)type;
    if (image == NULL || input == NULL) {
        bad(env, "java/lang/NullPointerException", "oracle image/input");
        return -1;
    }
    if (atoms < 0 || atoms > 63 || (*env)->GetArrayLength(env, image) != 136) {
        bad(env, "java/lang/IllegalArgumentException", "oracle image shape");
        return -1;
    }
    uint64_t allowed = atoms == 63 ? UINT64_MAX : (UINT64_C(1) << (atoms + 1)) - UINT64_C(1);
    uint64_t stars = (uint64_t)repeated;
    if ((stars & (~allowed | UINT64_C(1))) != 0) {
        bad(env, "java/lang/IllegalArgumentException", "oracle repeated bits");
        return -1;
    }
    jlong masks[136];
    (*env)->GetLongArrayRegion(env, image, 0, 136, masks);
    if ((*env)->ExceptionCheck(env)) return -1;
    for (int unit = 0; unit < 128; unit++) {
        if (((uint64_t)masks[unit] & (~allowed | UINT64_C(1))) != 0) {
            bad(env, "java/lang/IllegalArgumentException", "oracle predicate bits");
            return -1;
        }
    }
    unsigned char row[64] = {1};
    for (int j = 1; j <= atoms; j++) row[j] = (unsigned char)(row[j - 1] && ((stars >> j) & 1U));
    jsize length = (*env)->GetStringLength(env, input);
    jchar block[64];
    for (jsize first = 0; first < length;) {
        jsize count = length - first < 64 ? length - first : 64;
        (*env)->GetStringRegion(env, input, first, count, block);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize at = 0; at < count; at++) {
            if (block[at] >= 128) return -1;
            uint64_t predicate = (uint64_t)masks[block[at]];
            unsigned char diagonal = row[0];
            row[0] = 0;
            for (int j = 1; j <= atoms; j++) {
                unsigned char above = row[j];
                unsigned char matches = (unsigned char)((predicate >> j) & 1U);
                row[j] = (unsigned char)(((stars >> j) & 1U)
                        ? row[j - 1] || (above && matches) : diagonal && matches);
                diagonal = above;
            }
        }
        first += count;
    }
    return row[atoms] ? 1 : 0;
}

/* Independent wide TEST oracle. Closure and initial/accepting image planes are intentionally
 * ignored: only predicate/repetition planes feed the scalar recurrence. Fixed scratch is
 * <=685 jlongs, 257 Boolean bytes and 64 UTF-16 units, excluding compiler stack overhead. */
JNIEXPORT jint JNICALL Java_M3PatternAsciiFrontierTest_wideScalarOracle(
        JNIEnv *env, jclass type, jlongArray image, jint atoms, jstring input) {
    (void)type;
    if (image == NULL || input == NULL) {
        bad(env, "java/lang/NullPointerException", "wide oracle image/input");
        return -1;
    }
    if (atoms < 64 || atoms > 256) {
        bad(env, "java/lang/IllegalArgumentException", "wide oracle atom count");
        return -1;
    }
    int words = atoms / 64 + 1;
    int image_size = 137 * words;
    if ((*env)->GetArrayLength(env, image) != image_size) {
        bad(env, "java/lang/IllegalArgumentException", "wide oracle image shape");
        return -1;
    }
    jlong masks[137 * 5];
    (*env)->GetLongArrayRegion(env, image, 0, image_size, masks);
    if ((*env)->ExceptionCheck(env)) return -1;
    for (int word = 0; word < words; word++) {
        unsigned last = (unsigned)atoms & 63U;
        uint64_t allowed = word != words - 1 || last == 63U ? UINT64_MAX
                : (UINT64_C(1) << (last + 1U)) - UINT64_C(1);
        if (word == 0) allowed &= ~UINT64_C(1);
        if (((uint64_t)masks[136 * words + word] & ~allowed) != 0) {
            bad(env, "java/lang/IllegalArgumentException", "wide oracle repeated bits");
            return -1;
        }
        for (int unit = 0; unit < 128; unit++) {
            if (((uint64_t)masks[unit * words + word] & ~allowed) != 0) {
                bad(env, "java/lang/IllegalArgumentException", "wide oracle predicate bits");
                return -1;
            }
        }
    }
    unsigned char row[257] = {1};
    for (int j = 1; j <= atoms; j++) {
        uint64_t stars = (uint64_t)masks[136 * words + j / 64];
        row[j] = (unsigned char)(row[j - 1] && ((stars >> ((unsigned)j & 63U)) & 1U));
    }
    jsize length = (*env)->GetStringLength(env, input);
    jchar block[64];
    for (jsize first = 0; first < length;) {
        jsize count = length - first < 64 ? length - first : 64;
        (*env)->GetStringRegion(env, input, first, count, block);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize at = 0; at < count; at++) {
            if (block[at] >= 128) return -1;
            unsigned char diagonal = row[0];
            row[0] = 0;
            for (int j = 1; j <= atoms; j++) {
                int word = j / 64;
                unsigned shift = (unsigned)j & 63U;
                unsigned char above = row[j];
                uint64_t predicate = (uint64_t)masks[(int)block[at] * words + word];
                uint64_t stars = (uint64_t)masks[136 * words + word];
                unsigned char accepts = (unsigned char)((predicate >> shift) & 1U);
                row[j] = (unsigned char)(((stars >> shift) & 1U)
                        ? row[j - 1] || (above && accepts) : diagonal && accepts);
                diagonal = above;
            }
        }
        first += count;
    }
    return row[atoms] ? 1 : 0;
}
