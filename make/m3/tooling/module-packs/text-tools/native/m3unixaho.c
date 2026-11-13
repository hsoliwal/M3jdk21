/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Bounded JNI Aho-Corasick prefilter. No native retained pointers, references,
 * thread-local state, Java callbacks, regex captures or semantic acceptance.
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <stddef.h>

#define M3_MAX_STATES 16384
#define M3_MAX_UNITS (1 << 20)
#define M3_MAX_ROWS 1000000

static void m3_aho_error(JNIEnv *env, const char *clazz, const char *message) {
    jclass kind = (*env)->FindClass(env, clazz);
    if (kind != NULL) {
        (*env)->ThrowNew(env, kind, message);
        (*env)->DeleteLocalRef(env, kind);
    }
}

static jint m3_aho_step(jint state, jchar unit, const jint *heads,
                        const jchar *chars, const jint *targets, const jint *next) {
    for (jint edge = heads[state]; edge >= 0; edge = next[edge]) {
        if (chars[edge] == unit) return targets[edge];
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_text_M3NativeAho_abi0(JNIEnv *env, jclass owner) {
    (void)env;
    (void)owner;
    return 0x00010001;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_text_M3NativeAho_select0(
        JNIEnv *env, jclass owner,
        jcharArray packed_array, jintArray offsets_array,
        jintArray heads_array, jcharArray chars_array,
        jintArray targets_array, jintArray next_array,
        jintArray failures_array, jlongArray outputs_array,
        jlong required_mask, jlong empty_mask) {
    (void)owner;
    jchar *packed = NULL, *chars = NULL;
    jint *offsets = NULL, *heads = NULL, *targets = NULL, *next = NULL, *failures = NULL;
    jlong *outputs = NULL;
    uint64_t *results = NULL;
    jlongArray result = NULL;
    jsize units, row_count, state_count, edge_count, words;

    if (packed_array == NULL || offsets_array == NULL || heads_array == NULL
            || chars_array == NULL || targets_array == NULL || next_array == NULL
            || failures_array == NULL || outputs_array == NULL) {
        m3_aho_error(env, "java/lang/NullPointerException", "Aho primitive lanes");
        return NULL;
    }
    units = (*env)->GetArrayLength(env, packed_array);
    row_count = (*env)->GetArrayLength(env, offsets_array) - 1;
    state_count = (*env)->GetArrayLength(env, heads_array);
    edge_count = (*env)->GetArrayLength(env, chars_array);
    if (units > M3_MAX_UNITS || row_count < 0 || row_count > M3_MAX_ROWS
            || state_count <= 0 || state_count > M3_MAX_STATES
            || edge_count >= state_count
            || (*env)->GetArrayLength(env, targets_array) != edge_count
            || (*env)->GetArrayLength(env, next_array) != edge_count
            || (*env)->GetArrayLength(env, failures_array) != state_count
            || (*env)->GetArrayLength(env, outputs_array) != state_count
            || required_mask == 0 || (((uint64_t)empty_mask) & ~((uint64_t)required_mask)) != 0) {
        m3_aho_error(env, "java/lang/IllegalArgumentException", "invalid native Aho geometry");
        return NULL;
    }
    words = (row_count + 63) / 64;
    packed = (*env)->GetCharArrayElements(env, packed_array, NULL);
    if (packed == NULL) goto done;
    offsets = (*env)->GetIntArrayElements(env, offsets_array, NULL);
    if (offsets == NULL) goto done;
    heads = (*env)->GetIntArrayElements(env, heads_array, NULL);
    if (heads == NULL) goto done;
    chars = (*env)->GetCharArrayElements(env, chars_array, NULL);
    if (chars == NULL) goto done;
    targets = (*env)->GetIntArrayElements(env, targets_array, NULL);
    if (targets == NULL) goto done;
    next = (*env)->GetIntArrayElements(env, next_array, NULL);
    if (next == NULL) goto done;
    failures = (*env)->GetIntArrayElements(env, failures_array, NULL);
    if (failures == NULL) goto done;
    outputs = (*env)->GetLongArrayElements(env, outputs_array, NULL);
    if (outputs == NULL) goto done;

    if (offsets[0] != 0 || offsets[row_count] != units) {
        m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho offsets boundary");
        goto done;
    }
    for (jsize row = 0; row < row_count; row++) {
        if (offsets[row] < 0 || offsets[row + 1] < offsets[row] || offsets[row + 1] > units) {
            m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho offsets order");
            goto done;
        }
    }
    for (jsize state = 0; state < state_count; state++) {
        if (heads[state] < -1 || heads[state] >= edge_count
                || failures[state] < 0 || failures[state] >= state_count
                || (((uint64_t)outputs[state]) & ~((uint64_t)required_mask)) != 0) {
            m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho state geometry");
            goto done;
        }
    }
    if (failures[0] != 0) {
        m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho root failure");
        goto done;
    }
    for (jsize edge = 0; edge < edge_count; edge++) {
        if (targets[edge] <= 0 || targets[edge] >= state_count
                || next[edge] < -1 || next[edge] >= edge) {
            m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho edge geometry");
            goto done;
        }
    }
    results = (uint64_t *)calloc(words == 0 ? 1u : (size_t)words, sizeof(uint64_t));
    if (results == NULL) {
        m3_aho_error(env, "java/lang/OutOfMemoryError", "Aho output bitmap");
        goto done;
    }
    for (jsize row = 0; row < row_count; row++) {
        jint state = 0;
        uint64_t observed = (uint64_t)empty_mask;
        const uint64_t desired = (uint64_t)required_mask;
        for (jint at = offsets[row]; at < offsets[row + 1] && observed != desired; at++) {
            jint found = m3_aho_step(state, packed[at], heads, chars, targets, next);
            jsize hops = 0;
            while (found < 0 && state != 0 && hops++ < state_count) {
                state = failures[state];
                found = m3_aho_step(state, packed[at], heads, chars, targets, next);
            }
            if (hops > state_count) {
                m3_aho_error(env, "java/lang/IllegalArgumentException", "Aho failure cycle");
                goto done;
            }
            state = found < 0 ? 0 : found;
            observed |= (uint64_t)outputs[state];
        }
        if ((observed & desired) == desired) {
            results[row >> 6] |= UINT64_C(1) << (row & 63);
        }
    }
    result = (*env)->NewLongArray(env, words);
    if (result != NULL && words > 0) {
        /* Both jlong and uint64_t are 64-bit by JNI and stdint contracts. */
        (*env)->SetLongArrayRegion(env, result, 0, words, (const jlong *)results);
    }

done:
    free(results);
    if (outputs != NULL) (*env)->ReleaseLongArrayElements(env, outputs_array, outputs, JNI_ABORT);
    if (failures != NULL) (*env)->ReleaseIntArrayElements(env, failures_array, failures, JNI_ABORT);
    if (next != NULL) (*env)->ReleaseIntArrayElements(env, next_array, next, JNI_ABORT);
    if (targets != NULL) (*env)->ReleaseIntArrayElements(env, targets_array, targets, JNI_ABORT);
    if (chars != NULL) (*env)->ReleaseCharArrayElements(env, chars_array, chars, JNI_ABORT);
    if (heads != NULL) (*env)->ReleaseIntArrayElements(env, heads_array, heads, JNI_ABORT);
    if (offsets != NULL) (*env)->ReleaseIntArrayElements(env, offsets_array, offsets, JNI_ABORT);
    if (packed != NULL) (*env)->ReleaseCharArrayElements(env, packed_array, packed, JNI_ABORT);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return result;
}
