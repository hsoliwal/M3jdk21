/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_doop_frozen_mask.c blob 5fd0ea73d91c (com.synexia.primitives.M3DoopFrozenMaskedViewport -> jdk.internal.m3.collections.DoopFrozenMaskedViewport, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
/* Optional one-time immutable DOOP mask preparation: bounded JNI regions, no retained pointers. */
#include <jni.h>
#include <stdint.h>
#include <string.h>

#define M3_FLAGS_CHUNK 128
#define M3_WORDS_CHUNK 128

static void m3_argument_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static int m3_selected(jshort flags, jshort required, jshort excluded) {
    uint16_t bits = (uint16_t) flags;
    uint16_t yes = (uint16_t) required;
    uint16_t no = (uint16_t) excluded;
    return (bits & yes) == yes && (bits & no) == 0;
}

static void m3_append_word(jlong *buffer, jint *used, uint64_t bits) {
    jlong value;
    /* Preserve the high sign bit without an implementation-defined unsigned conversion. */
    memcpy(&value, &bits, sizeof(value));
    buffer[(*used)++] = value;
}

JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_DoopFrozenMaskedViewport_nativeBuildMask(
        JNIEnv *env, jclass type, jshortArray flagsArray, jint count,
        jshort required, jshort excluded, jlongArray outputWords) {
    (void)type;
    if (flagsArray == NULL || outputWords == NULL || count < 0) {
        m3_argument_error(env, "invalid JNI frozen-mask arguments");
        return;
    }
    jint words = count == 0 ? 0 : 1 + ((count - 1) / 64);
    if ((*env)->GetArrayLength(env, flagsArray) < count
            || (*env)->GetArrayLength(env, outputWords) != words) {
        m3_argument_error(env, "invalid JNI frozen-mask array lengths");
        return;
    }
    jshort flags[M3_FLAGS_CHUNK];
    jlong buffered[M3_WORDS_CHUNK];
    jint used = 0;
    jint committed = 0;
    uint64_t pending = UINT64_C(0);
    jint within = 0;
    jint base = 0;
    while (base < count) {
        jint take = count - base;
        if (take > M3_FLAGS_CHUNK) take = M3_FLAGS_CHUNK;
        (*env)->GetShortArrayRegion(env, flagsArray, base, take, flags);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
        for (jint i = 0; i < take; i++) {
            if (m3_selected(flags[i], required, excluded)) {
                pending |= UINT64_C(1) << within;
            }
            within++;
            if (within == 64) {
                m3_append_word(buffered, &used, pending);
                pending = UINT64_C(0);
                within = 0;
                if (used == M3_WORDS_CHUNK) {
                    (*env)->SetLongArrayRegion(env, outputWords, committed, used, buffered);
                    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return;
                    committed += used;
                    used = 0;
                }
            }
        }
        base += take;
    }
    if (within != 0) m3_append_word(buffered, &used, pending);
    if (used != 0) {
        (*env)->SetLongArrayRegion(env, outputWords, committed, used, buffered);
    }
}
