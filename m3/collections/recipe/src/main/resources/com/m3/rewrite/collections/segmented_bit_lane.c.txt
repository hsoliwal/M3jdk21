/*
 * Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Optional, read-only peer of M3BitLane28.cardinality(from,to).
 */
#include <jni.h>
#include <stdint.h>

static void invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

/* Portable unsigned population count; no signed overflow or shift-by-64. */
static jint population(uint64_t word) {
    word -= (word >> 1) & UINT64_C(0x5555555555555555);
    word = (word & UINT64_C(0x3333333333333333))
            + ((word >> 2) & UINT64_C(0x3333333333333333));
    word = (word + (word >> 4)) & UINT64_C(0x0f0f0f0f0f0f0f0f);
    return (jint)((word * UINT64_C(0x0101010101010101)) >> 56);
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3BitLane28_cardinality0(
        JNIEnv *env, jclass type, jobjectArray regions, jint from, jint to) {
    (void)type;
    if (regions == NULL || from < 0 || to < from || to > (1 << 28)) {
        invalid(env, "invalid bitmap range/directory");
        return 0;
    }
    if ((*env)->GetArrayLength(env, regions) != 256) {
        invalid(env, "invalid bitmap region count");
        return 0;
    }
    jint count = 0;
    while (from < to) {
        jobjectArray region = (jobjectArray)(*env)->GetObjectArrayElement(env, regions, from >> 20);
        if ((*env)->ExceptionCheck(env)) return 0;
        jint region_end = ((from >> 20) + 1) << 20;
        if (region_end > to) region_end = to;
        if (region == NULL) { from = region_end; continue; }
        if ((*env)->GetArrayLength(env, region) != 256) {
            (*env)->DeleteLocalRef(env, region);
            invalid(env, "invalid bitmap page count");
            return 0;
        }
        while (from < region_end) {
            jlongArray page = (jlongArray)(*env)->GetObjectArrayElement(env, region, (from >> 12) & 255);
            if ((*env)->ExceptionCheck(env)) {
                (*env)->DeleteLocalRef(env, region);
                return 0;
            }
            jint end = ((from >> 12) + 1) << 12;
            if (end > region_end) end = region_end;
            if (page != NULL) {
                if ((*env)->GetArrayLength(env, page) != 64) {
                    (*env)->DeleteLocalRef(env, page);
                    (*env)->DeleteLocalRef(env, region);
                    invalid(env, "invalid bitmap word count");
                    return 0;
                }
                jlong words[64];
                jint first = (from & 4095) >> 6;
                jint last = ((end - 1) & 4095) >> 6;
                (*env)->GetLongArrayRegion(env, page, first, last - first + 1, words);
                (*env)->DeleteLocalRef(env, page);
                if ((*env)->ExceptionCheck(env)) {
                    (*env)->DeleteLocalRef(env, region);
                    return 0;
                }
                for (jint w = first; w <= last; w++) {
                    uint64_t word = (uint64_t)words[w - first];
                    if (w == first) word &= UINT64_MAX << (from & 63);
                    if (w == last) word &= UINT64_MAX >> (63 - ((end - 1) & 63));
                    count += population(word);
                }
            }
            from = end;
        }
        (*env)->DeleteLocalRef(env, region);
    }
    return count;
}
