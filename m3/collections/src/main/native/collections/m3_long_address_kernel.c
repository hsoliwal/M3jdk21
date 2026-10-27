/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2026 Hitesh Soliwal and contributors.
 * Modified 2026-10-17: bounded index chunks and contiguous region reads.
 */
#include <jni.h>
#include <stdlib.h>
#include <stdint.h>

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


/*
 * LeetCode #239 sliding maximum; canonical source M3LaneAlgorithms.
 * Exactly one JNI transition, fixed 256-long input/output pages and a
 * monotonically nonincreasing primitive circular deque of at most k entries.
 * The first equal maximum wins: evict strictly less values only.
 * No Java heap pointer is retained, and no callbacks cross the native ABI.
 */
typedef struct m3_max_candidate {
    jint physical;
    jlong value;
} m3_max_candidate;

enum { M3_MAX_PAGE = 256 };

static jsize m3_max_ring_slot(jsize head, jsize offset, jsize capacity) {
    const jsize remaining = capacity - head;
    return offset < remaining ? head + offset : offset - remaining;
}

static void m3_max_throw(JNIEnv *env, const char *kind, const char *message) {
    jclass type = (*env)->FindClass(env, kind);
    if (type != NULL) (*env)->ThrowNew(env, type, message);
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LaneAlgorithms_slidingMaximumIndices0(
        JNIEnv *env, jclass unused, jlongArray values,
        jint from, jint to, jint window, jintArray output, jint output_from) {
    (void) unused;
    if (values == NULL || output == NULL) {
        m3_max_throw(env, "java/lang/NullPointerException", "null sliding max array");
        return;
    }
    const jsize value_size = (*env)->GetArrayLength(env, values);
    const jsize output_size = (*env)->GetArrayLength(env, output);
    if (from < 0 || to < from || to > value_size) {
        m3_max_throw(env, "java/lang/IndexOutOfBoundsException", "sliding max range");
        return;
    }
    const jsize span = to - from;
    if (window < 1 || window > span) {
        m3_max_throw(env, "java/lang/IllegalArgumentException", "sliding max window");
        return;
    }
    const jsize expected = span - window + 1;
    if (output_from < 0 || expected > output_size
            || output_from > output_size - expected) {
        m3_max_throw(env, "java/lang/IndexOutOfBoundsException", "sliding max output range");
        return;
    }
    if ((size_t) window > SIZE_MAX / sizeof(m3_max_candidate)) {
        m3_max_throw(env, "java/lang/OutOfMemoryError", "sliding max deque too large");
        return;
    }
    m3_max_candidate *deque = malloc((size_t) window * sizeof(*deque));
    if (deque == NULL) {
        m3_max_throw(env, "java/lang/OutOfMemoryError", "sliding max deque allocation");
        return;
    }
    jsize head = 0, size = 0, produced = 0, chunk_size = 0;
    jlong input_page[M3_MAX_PAGE];
    jint output_page[M3_MAX_PAGE];
    for (jint read = from; read < to;) {
        jint count = to - read;
        if (count > M3_MAX_PAGE) count = M3_MAX_PAGE;
        (*env)->GetLongArrayRegion(env, values, read, count, input_page);
        if ((*env)->ExceptionCheck(env)) { free(deque); return; }
        for (jint step = 0; step < count; step++) {
            const jint index = read + step;
            const jlong value = input_page[step];
            if (size != 0 && deque[head].physical <= index - window) {
                head = (head + 1 == window) ? 0 : head + 1;
                size--;
            }
            while (size != 0) {
                jsize tail = m3_max_ring_slot(head, size - 1, window);
                if (deque[tail].value >= value) break;
                size--;
            }
            const jsize slot = m3_max_ring_slot(head, size, window);
            deque[slot].physical = index;
            deque[slot].value = value;
            size++;
            if (index - from >= window - 1) {
                output_page[chunk_size++] = deque[head].physical;
                if (chunk_size == M3_MAX_PAGE) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_from + produced, chunk_size, output_page);
                    if ((*env)->ExceptionCheck(env)) { free(deque); return; }
                    produced += chunk_size;
                    chunk_size = 0;
                }
            }
        }
        read += count;
    }
    if (chunk_size != 0) {
        (*env)->SetIntArrayRegion(env, output, output_from + produced,
                chunk_size, output_page);
    }
    free(deque);
}
