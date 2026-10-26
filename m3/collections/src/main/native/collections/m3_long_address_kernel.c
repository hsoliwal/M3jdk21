/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2026 Hitesh Soliwal and contributors.
 * Modified 2026-10-17: bounded index chunks and contiguous region reads.
 */
#include <jni.h>

/* Own scratch is fixed: 256 jint indexes and 256 jlong values (3072 bytes).
 * JVM-internal copying is implementation-dependent. Caller keeps input arrays stable.
 * No Java pointer survives a chunk. Prefix writes on an invalid address are preserved.
 */
enum { M3_GATHER_CHUNK = 256 };

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongAddressKernel_gatherNative(
        JNIEnv *env, jclass ignored, jlongArray payload, jintArray addresses,
        jint address_from, jint length, jlongArray output, jint output_from) {
    (void) ignored;
    if (payload == NULL || addresses == NULL || output == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null gather array");
        return;
    }

    jsize payload_length = (*env)->GetArrayLength(env, payload);
    jsize address_length = (*env)->GetArrayLength(env, addresses);
    jsize output_length = (*env)->GetArrayLength(env, output);
    if (address_from < 0 || length < 0 || output_from < 0
            || address_from > address_length - length
            || output_from > output_length - length) {
        jclass error = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
        if (error != NULL) (*env)->ThrowNew(env, error, "gather range");
        return;
    }
    if (length == 0) return;

    jint index[M3_GATHER_CHUNK];
    jlong result[M3_GATHER_CHUNK];
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_GATHER_CHUNK) count = M3_GATHER_CHUNK;
        (*env)->GetIntArrayRegion(env, addresses, address_from + done, count, index);
        if ((*env)->ExceptionCheck(env)) return;

        jint valid = 0;
        jboolean contiguous = JNI_TRUE;
        for (; valid < count; valid++) {
            jint physical = index[valid];
            if (physical < 0 || physical >= payload_length) break;
            if ((jlong) physical != (jlong) index[0] + valid) contiguous = JNI_FALSE;
        }
        if (valid != 0) {
            if (contiguous == JNI_TRUE) {
                /* Affine forward indexes need no critical pointer at all. */
                (*env)->GetLongArrayRegion(env, payload, index[0], valid, result);
                if ((*env)->ExceptionCheck(env)) return;
            } else {
                const jlong *values = (*env)->GetPrimitiveArrayCritical(env, payload, NULL);
                if (values == NULL) return;
                /* No callbacks, allocation, blocking, or unrelated JNI calls while pinned. */
                for (jint i = 0; i < valid; i++) result[i] = values[index[i]];
                (*env)->ReleasePrimitiveArrayCritical(env, payload, (void *) values, JNI_ABORT);
            }
            (*env)->SetLongArrayRegion(env, output, output_from + done, valid, result);
            if ((*env)->ExceptionCheck(env)) return;
        }
        if (valid != count) {
            /* Publish exactly the valid prefix, including the current partial chunk. */
            jclass error = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
            if (error != NULL) (*env)->ThrowNew(env, error, "physical address outside payload");
            return;
        }
        done += count;
    }
}

static jint m3_match_bits(jbyte flags, jint required, jint forbidden) {
    jint state = (jint) (unsigned char) flags;
    return (state & required) == required && (state & forbidden) == 0;
}

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass clazz = (*env)->FindClass(env, type);
    if (clazz != NULL) (*env)->ThrowNew(env, clazz, message);
}

/*
 * Stable masked compaction + physical gather in ONE JNI invocation.
 *   1) Verify all selected physical addresses before changing output.
 *   2) Visit input in logical encounter order, retaining duplicate physical IDs.
 *   3) Pin the payload only for a bounded 256-element read, release before
 *      any JNI region write. The output is caller-owned; no native pointer survives.
 * Inspired by algorithm families LeetCode 27/283 and indexed permutation 1920;
 * this code is original, not copied challenge code.
 */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3LongAddressKernel_gatherSelectedNative(
        JNIEnv *env, jclass ignored, jlongArray payload, jintArray addresses,
        jbyteArray row_states, jint address_from, jint length, jlongArray output,
        jint output_from, jint required, jint forbidden) {
    (void) ignored;
    if (payload == NULL || addresses == NULL || row_states == NULL || output == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null masked-gather array");
        return 0;
    }
    if ((required & ~0xff) != 0 || (forbidden & ~0xff) != 0
            || (required & forbidden) != 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "mask must be 8-bit and disjoint");
        return 0;
    }
    jsize payload_length = (*env)->GetArrayLength(env, payload);
    jsize address_length = (*env)->GetArrayLength(env, addresses);
    jsize state_length = (*env)->GetArrayLength(env, row_states);
    jsize output_length = (*env)->GetArrayLength(env, output);
    if (address_from < 0 || length < 0 || output_from < 0
            || length > address_length || address_from > address_length - length
            || length > state_length || address_from > state_length - length
            || length > output_length || output_from > output_length - length) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "masked-gather window");
        return 0;
    }
    if (length == 0) return 0;

    jint indices[M3_GATHER_CHUNK];
    jbyte states[M3_GATHER_CHUNK];
    jlong gathered[M3_GATHER_CHUNK];
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_GATHER_CHUNK) count = M3_GATHER_CHUNK;
        (*env)->GetIntArrayRegion(env, addresses, address_from + done, count, indices);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->GetByteArrayRegion(env, row_states, address_from + done, count, states);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jint i = 0; i < count; i++) {
            if (m3_match_bits(states[i], required, forbidden)
                    && (indices[i] < 0 || indices[i] >= payload_length)) {
                m3_throw(env, "java/lang/IndexOutOfBoundsException",
                        "masked physical address outside payload");
                return 0;
            }
        }
        done += count;
    }

    jint written = 0;
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_GATHER_CHUNK) count = M3_GATHER_CHUNK;
        (*env)->GetIntArrayRegion(env, addresses, address_from + done, count, indices);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->GetByteArrayRegion(env, row_states, address_from + done, count, states);
        if ((*env)->ExceptionCheck(env)) return 0;
        jint selected = 0;
        const jlong *values = (*env)->GetPrimitiveArrayCritical(env, payload, NULL);
        if (values == NULL) return 0;
        /* No callbacks, allocations or JNI calls while holding the GC-critical payload. */
        for (jint i = 0; i < count; i++) {
            if (m3_match_bits(states[i], required, forbidden)) {
                gathered[selected++] = values[indices[i]];
            }
        }
        (*env)->ReleasePrimitiveArrayCritical(env, payload, (void *) values, JNI_ABORT);
        if (selected > 0) {
            (*env)->SetLongArrayRegion(env, output, output_from + written, selected, gathered);
            if ((*env)->ExceptionCheck(env)) return 0;
            written += selected;
        }
        done += count;
    }
    return written;
}
