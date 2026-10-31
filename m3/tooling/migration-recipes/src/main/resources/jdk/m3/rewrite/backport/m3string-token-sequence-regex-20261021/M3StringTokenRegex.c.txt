/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
#include <jni.h>
#include <stdint.h>

#define M3_TOKEN_EXACT 0
#define M3_TOKEN_ANY 1

#define M3_Q_ONE 0
#define M3_Q_ZERO_OR_MORE 1
#define M3_Q_ONE_OR_MORE 2
#define M3_Q_OPTIONAL 3

#define M3_MAX_PATTERN_ATOMS 62

static uint32_t m3_ctz64(uint64_t value) {
    uint32_t count = 0U;
    while ((value & UINT64_C(1)) == UINT64_C(0)) {
        value >>= 1;
        count++;
    }
    return count;
}

static uint64_t m3_close(uint64_t bits, const jbyte *quantifiers, uint32_t count) {
    const uint64_t accept = UINT64_C(1) << count;
    uint64_t previous;
    do {
        previous = bits;
        uint64_t active = bits & (accept - UINT64_C(1));
        while (active != UINT64_C(0)) {
            uint32_t atom = m3_ctz64(active);
            active &= active - UINT64_C(1);
            uint8_t quantifier = (uint8_t) quantifiers[atom];
            if (quantifier == M3_Q_ZERO_OR_MORE || quantifier == M3_Q_OPTIONAL) {
                bits |= UINT64_C(1) << (atom + 1U);
            }
        }
    } while (bits != previous);
    return bits;
}

/*
 * Fingerprint execution is a rejection/candidate kernel only. Equal token text always has equal
 * (String.hashCode,length) fingerprints; collisions may create positives, never false negatives.
 * Java exact owner/range verification remains authoritative for every positive.
 */
JNIEXPORT jint JNICALL
Java_java_lang_M3StringTokenRegex_matches0(
        JNIEnv *env,
        jclass ignored,
        jlongArray source_array,
        jlongArray pattern_array,
        jbyteArray kind_array,
        jbyteArray quantifier_array) {
    (void) ignored;
    if (source_array == NULL || pattern_array == NULL
            || kind_array == NULL || quantifier_array == NULL) {
        return -1;
    }

    jsize source_count = (*env)->GetArrayLength(env, source_array);
    jsize pattern_count = (*env)->GetArrayLength(env, pattern_array);
    if (source_count < 0 || pattern_count < 1 || pattern_count > M3_MAX_PATTERN_ATOMS
            || (*env)->GetArrayLength(env, kind_array) != pattern_count
            || (*env)->GetArrayLength(env, quantifier_array) != pattern_count) {
        return -1;
    }

    jlong *source = (*env)->GetLongArrayElements(env, source_array, NULL);
    if (source == NULL) return -1;
    jlong *pattern = (*env)->GetLongArrayElements(env, pattern_array, NULL);
    if (pattern == NULL) {
        (*env)->ReleaseLongArrayElements(env, source_array, source, JNI_ABORT);
        return -1;
    }
    jbyte *kinds = (*env)->GetByteArrayElements(env, kind_array, NULL);
    if (kinds == NULL) {
        (*env)->ReleaseLongArrayElements(env, pattern_array, pattern, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, source_array, source, JNI_ABORT);
        return -1;
    }
    jbyte *quantifiers = (*env)->GetByteArrayElements(env, quantifier_array, NULL);
    if (quantifiers == NULL) {
        (*env)->ReleaseByteArrayElements(env, kind_array, kinds, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, pattern_array, pattern, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, source_array, source, JNI_ABORT);
        return -1;
    }

    int valid = 1;
    for (jsize atom = 0; atom < pattern_count; atom++) {
        uint8_t kind = (uint8_t) kinds[atom];
        uint8_t quantifier = (uint8_t) quantifiers[atom];
        if ((kind != M3_TOKEN_EXACT && kind != M3_TOKEN_ANY)
                || quantifier > M3_Q_OPTIONAL) {
            valid = 0;
            break;
        }
    }

    uint64_t state = valid
            ? m3_close(UINT64_C(1), quantifiers, (uint32_t) pattern_count)
            : UINT64_C(0);
    const uint64_t accept = UINT64_C(1) << (uint32_t) pattern_count;

    for (jsize token = 0; valid && token < source_count; token++) {
        uint64_t next = UINT64_C(0);
        uint64_t active = state & (accept - UINT64_C(1));
        while (active != UINT64_C(0)) {
            uint32_t atom = m3_ctz64(active);
            active &= active - UINT64_C(1);
            uint8_t kind = (uint8_t) kinds[atom];
            if (kind == M3_TOKEN_EXACT && pattern[atom] != source[token]) continue;

            switch ((uint8_t) quantifiers[atom]) {
                case M3_Q_ONE:
                case M3_Q_OPTIONAL:
                    next |= UINT64_C(1) << (atom + 1U);
                    break;
                case M3_Q_ZERO_OR_MORE:
                    next |= UINT64_C(1) << atom;
                    break;
                case M3_Q_ONE_OR_MORE:
                    next |= (UINT64_C(1) << atom) | (UINT64_C(1) << (atom + 1U));
                    break;
                default:
                    valid = 0;
                    break;
            }
        }
        state = valid ? m3_close(next, quantifiers, (uint32_t) pattern_count) : UINT64_C(0);
        if (state == UINT64_C(0)) break;
    }

    (*env)->ReleaseByteArrayElements(env, quantifier_array, quantifiers, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, kind_array, kinds, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, pattern_array, pattern, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, source_array, source, JNI_ABORT);

    if (!valid) return -1;
    return (state & accept) != UINT64_C(0) ? 1 : 0;
}
