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

/* Wide test oracle: scalar recurrence does not read the production epsilon-jump tables.
 * Fixed bounds follow the unchanged 256-atom admission, not caller-controlled allocation. */
JNIEXPORT jint JNICALL Java_M3PatternAsciiFrontierTest_wideOracle(
        JNIEnv *env, jclass type, jlongArray image, jbooleanArray repeats,
        jint atoms, jstring input) {
    (void)type;
    if (image == NULL || repeats == NULL || input == NULL) {
        bad(env, "java/lang/NullPointerException", "wide oracle image/repeats/input");
        return -1;
    }
    if (atoms < 1 || atoms > 256 || (*env)->GetArrayLength(env, repeats) != atoms
            || (*env)->GetArrayLength(env, image) != ((atoms >> 6) + 1) * 136) {
        bad(env, "java/lang/IllegalArgumentException", "wide oracle image shape");
        return -1;
    }
    jlong masks[680];
    jboolean stars[256];
    jsize words = (atoms >> 6) + 1;
    (*env)->GetLongArrayRegion(env, image, 0, words * 136, masks);
    if ((*env)->ExceptionCheck(env)) return -1;
    (*env)->GetBooleanArrayRegion(env, repeats, 0, atoms, stars);
    if ((*env)->ExceptionCheck(env)) return -1;
    for (int word = 0; word < words; word++) {
        int last = atoms - word * 64;
        uint64_t allowed = last >= 63 ? UINT64_MAX : (UINT64_C(1) << (last + 1)) - 1U;
        if (word == 0) allowed &= ~UINT64_C(1);
        for (int unit = 0; unit < 128; unit++) {
            if (((uint64_t)masks[word * 136 + unit] & ~allowed) != 0) {
                bad(env, "java/lang/IllegalArgumentException", "wide oracle predicate bits");
                return -1;
            }
        }
    }
    unsigned char row[257] = {1};
    for (int j = 1; j <= atoms; j++) row[j] = (unsigned char)(row[j - 1] && stars[j - 1]);
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
                unsigned char above = row[j];
                uint64_t predicate = (uint64_t)masks[(j >> 6) * 136 + block[at]];
                unsigned char matches = (unsigned char)((predicate >> (j & 63)) & 1U);
                row[j] = (unsigned char)(stars[j - 1]
                        ? row[j - 1] || (above && matches) : diagonal && matches);
                diagonal = above;
            }
        }
        first += count;
    }
    return row[atoms] ? 1 : 0;
}
