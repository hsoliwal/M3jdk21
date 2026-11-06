/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: trim two-mask payload regions to admitted ordinals. */
/* JNI boundary: no callbacks, no pointer retention, no Java object graph walking.
 * Read fixed-size primitive chunks with Region API (no long-lived critical pins).
 * Two passes count then emit; input arrays are never mutated.
 */
#include <jni.h>
#include <stddef.h>

#define M3_CHUNK 256

static void m3_throw_state_drift(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/util/ConcurrentModificationException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, "aligned selection states changed");
        (*env)->DeleteLocalRef(env, type);
    }
}

static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* Additive count-only JNI ABI borrowed as a concept from the Synexia
 * M3IndexMaskCompactor donor. Reuse the target's existing bounded count kernel.
 * Preserve all existing exports and never allocate output row/index arrays.
 */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectedCountNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint required_mask) {
    (void)owner;
    if (states == NULL || required_mask < 0 || required_mask > 255) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "invalid primitive count inputs");
            (*env)->DeleteLocalRef(env, type);
        }
        return 0;
    }
    jsize length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return 0;
    jint result = m3_count_window(env, states, 0, length, required_mask);
    return result < 0 ? 0 : result;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */
static jintArray m3_select_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jsize first, jsize state_first, jsize n, jint required_mask) {
    jbyte marks[M3_CHUNK];
    jint row_ids[M3_CHUNK];
    jsize count = m3_count_window(env, states, state_first, n, required_mask);
    if (count < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL) return NULL;
    jsize written = 0;
    for (jsize offset = 0; offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        if (required_mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
            }
        }
        if (accepted > count - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetIntArrayRegion(env, result, written, accepted, row_ids);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += accepted;
        }
        offset += chunk;
    }
    if (written != count) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint required_mask) {
    (void)owner;
    if (rows == NULL || states == NULL || required_mask < 0 || required_mask > 255) {
        jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exception != NULL) {
            (*env)->ThrowNew(env, exception, "invalid primitive selection inputs");
            (*env)->DeleteLocalRef(env, exception);
        }
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if ((*env)->GetArrayLength(env, states) != n) {
        if ((*env)->ExceptionCheck(env)) return NULL;
        jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exception != NULL) {
            (*env)->ThrowNew(env, exception, "state/index length mismatch");
            (*env)->DeleteLocalRef(env, exception);
        }
        return NULL;
    }
    return m3_select_range(env, rows, states, 0, 0, n, required_mask);
}

/* Capability discovery keeps older binaries usable through their original native ABI. */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_rangeAbiNative(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectRangeNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states,
        jint first, jint length, jint required_mask) {
    (void)owner;
    if (rows == NULL || states == NULL || required_mask < 0 || required_mask > 255) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "invalid primitive selection inputs");
            (*env)->DeleteLocalRef(env, type);
        }
        return NULL;
    }
    jsize rows_length = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize states_length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return NULL;
    /* Subtraction-based bounds keep first+length from overflowing before validation. */
    if (first < 0 || length < 0 || first > rows_length || length > rows_length - first
            || states_length != length) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) {
            (*env)->ThrowNew(env, type, "invalid index window or aligned state length");
            (*env)->DeleteLocalRef(env, type);
        }
        return NULL;
    }
    return m3_select_range(env, rows, states, first, 0, length, required_mask);
}

/* Independent row/state offsets are checked before JNI region reads.
 * Reuse the original chunked mask/compactor ABI and state-drift diagnostic.
 */
static void m3_bad_window(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, "invalid indexed window or unsigned-byte mask");
        (*env)->DeleteLocalRef(env, type);
    }
}

static int m3_window_valid(JNIEnv *env, jarray rows, jbyteArray states,
        jint row_offset, jint state_offset, jint length, jint mask) {
    if (rows == NULL || states == NULL || row_offset < 0 || state_offset < 0
            || length < 0 || mask < 0 || mask > 255) {
        m3_bad_window(env);
        return 0;
    }
    jsize row_size = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize state_size = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (row_offset > row_size || state_offset > state_size
            || length > row_size - row_offset || length > state_size - state_offset) {
        m3_bad_window(env);
        return 0;
    }
    return 1;
}


/* ABI probe for direct logical-position selection from the state lane. */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_positionsAbiNative(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectPositionsNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint mask) {
    (void)owner;
    if (states == NULL || mask < 0 || mask > 255) {
        m3_bad_window(env);
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jint kept = m3_count_window(env, states, 0, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint positions[M3_CHUNK];
    jint written = 0;
    for (jint offset = 0; offset < length;) {
        jint remaining = length - offset;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize accepted = 0;
        if (mask == 0) {
            /* Identity positions reuse the Java owner's affine fill atom. */
            accepted = chunk;
            for (jsize i = 0; i < chunk; i++) positions[i] = offset + i;
        } else {
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) {
                    positions[accepted++] = offset + i;
                }
            }
        }
        if (accepted > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetIntArrayRegion(env, result, written, accepted, positions);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += accepted;
        }
        offset += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}


/* Additive exact-position ABI for required + excluded unsigned-byte state masks.
 * Existing required-only symbols remain unchanged for binary compatibility.
 */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_positionsMasksAbiNative(
        JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectPositionsMasksNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint required, jint excluded) {
    (void)owner;
    if (states == NULL || required < 0 || required > 255
            || excluded < 0 || excluded > 255) {
        m3_bad_window(env);
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if ((required & excluded) != 0) {
        return (*env)->NewIntArray(env, 0);
    }

    /* The overlap refusal above makes one combined-mask comparison exact. */
    unsigned int tested_mask = (unsigned int)(required | excluded);
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    if (tested_mask == 0) {
        kept = length;
    } else {
        for (jint offset = 0; offset < length;) {
            jint remaining = length - offset;
            jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & tested_mask) == (unsigned int)required) {
                    kept++;
                }
            }
            offset += chunk;
        }
    }

    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jint positions[M3_CHUNK];
    jint written = 0;
    for (jint offset = 0; offset < length;) {
        jint remaining = length - offset;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (tested_mask != 0) {
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize accepted = 0;
        if (tested_mask == 0) {
            accepted = chunk;
            for (jsize i = 0; i < chunk; i++) positions[i] = offset + i;
        } else {
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & tested_mask) == (unsigned int)required) {
                    positions[accepted++] = offset + i;
                }
            }
        }
        if (accepted > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetIntArrayRegion(env, result, written, accepted, positions);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += accepted;
        }
        offset += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectWindowNative(
        JNIEnv *env, jclass owner, jintArray rows, jint row_offset,
        jbyteArray states, jint state_offset, jint length, jint mask) {
    (void)owner;
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
        JNIEnv *env, jclass owner, jlongArray rows, jint row_offset,
        jbyteArray states, jint state_offset, jint length, jint mask) {
    (void)owner;
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jlongArray result = (*env)->NewLongArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jlong values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Keep the same detached, exact-sized copy contract as the int window. */
            (*env)->SetLongArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}

/* Additive ABI: required and excluded unsigned-byte state bits on independent
 * row/state offsets. Reuse the existing checked geometry, fixed 256-row
 * Region buffers and state-drift exception. Old JNI exports are unchanged.
 */
static int m3_window_accepts_masks(jbyte state, jint required, jint excluded) {
    unsigned int word = (unsigned int)(unsigned char) state;
    return ((word & (unsigned int) required) == (unsigned int) required)
            && ((word & (unsigned int) excluded) == 0U);
}

static int m3_window_valid_two_masks(JNIEnv *env, jarray rows, jbyteArray states,
        jint row_offset, jint state_offset, jint length,
        jint required, jint excluded) {
    if (excluded < 0 || excluded > 255) {
        m3_bad_window(env);
        return 0;
    }
    return m3_window_valid(env, rows, states, row_offset,
            state_offset, length, required);
}

/* -1 means a pending JNI exception, otherwise exact selected count. */
static jint m3_count_window_two_masks(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint required, jint excluded) {
    if ((required & excluded) != 0) return 0;
    if ((required | excluded) == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states,
                state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            if (m3_window_accepts_masks(marks[i], required, excluded)) kept++;
        }
        position += chunk;
    }
    return kept;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_windowMasksAbiNative(
        JNIEnv *env, jclass owner) {
    (void) env;
    (void) owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectWindowMasksNative(
        JNIEnv *env, jclass owner, jintArray rows, jint row_offset,
        jbyteArray states, jint state_offset, jint length,
        jint required, jint excluded) {
    (void) owner;
    if (!m3_window_valid_two_masks(env, rows, states, row_offset,
            state_offset, length, required, excluded)) return NULL;
    jint kept = m3_count_window_two_masks(env, states, state_offset,
            length, required, excluded);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL || kept == 0) return result;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        jsize begin = 0;
        jsize end = chunk;
        if ((required | excluded) != 0) {
            (*env)->GetByteArrayRegion(env, states,
                    state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            while (begin < chunk
                    && !m3_window_accepts_masks(marks[begin], required, excluded)) begin++;
            if (begin == chunk) {
                /* Preserve the state scan/drift check without copying rejected payload. */
                position += chunk;
                continue;
            }
            while (!m3_window_accepts_masks(marks[end - 1], required, excluded)) end--;
        }
        /* Reuse the bounded mask-window envelope: only admitted endpoint spans cross JNI. */
        (*env)->GetIntArrayRegion(env, rows,
                row_offset + position + begin, end - begin, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = end - begin;
        if ((required | excluded) != 0) {
            accepted = 0;
            for (jsize i = begin; i < end; i++) {
                if (m3_window_accepts_masks(marks[i], required, excluded)) {
                    /* accepted <= i - begin, so compaction cannot overwrite unread payload. */
                    values[accepted++] = values[i - begin];
                }
            }
        }
        if (accepted > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetIntArrayRegion(env, result, written, accepted, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += accepted;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectLongWindowMasksNative(
        JNIEnv *env, jclass owner, jlongArray rows, jint row_offset,
        jbyteArray states, jint state_offset, jint length,
        jint required, jint excluded) {
    (void) owner;
    if (!m3_window_valid_two_masks(env, rows, states, row_offset,
            state_offset, length, required, excluded)) return NULL;
    jint kept = m3_count_window_two_masks(env, states, state_offset,
            length, required, excluded);
    if (kept < 0) return NULL;
    jlongArray result = (*env)->NewLongArray(env, kept);
    if (result == NULL || kept == 0) return result;
    jbyte marks[M3_CHUNK];
    jlong values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        jsize begin = 0;
        jsize end = chunk;
        if ((required | excluded) != 0) {
            (*env)->GetByteArrayRegion(env, states,
                    state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            while (begin < chunk
                    && !m3_window_accepts_masks(marks[begin], required, excluded)) begin++;
            if (begin == chunk) {
                /* Preserve the state scan/drift check without copying rejected payload. */
                position += chunk;
                continue;
            }
            while (!m3_window_accepts_masks(marks[end - 1], required, excluded)) end--;
        }
        /* Reuse the bounded mask-window envelope: only admitted endpoint spans cross JNI. */
        (*env)->GetLongArrayRegion(env, rows,
                row_offset + position + begin, end - begin, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = end - begin;
        if ((required | excluded) != 0) {
            accepted = 0;
            for (jsize i = begin; i < end; i++) {
                if (m3_window_accepts_masks(marks[i], required, excluded)) {
                    /* accepted <= i - begin, so compaction cannot overwrite unread payload. */
                    values[accepted++] = values[i - begin];
                }
            }
        }
        if (accepted > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetLongArrayRegion(env, result, written, accepted, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += accepted;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
}

/* Additive, entryless logical-position paging ABI for reusable Java-owned int lanes.
 * All offsets are checked with subtraction before any JNI region access. No heap
 * pointers or global references escape; required native calls never fall back.
 */
static void m3_positions_page_invalid(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, "invalid position page bounds or masks");
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_positionsPageAbiNative(
        JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_selectPositionsPageNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint first, jint length,
        jint required, jint excluded, jintArray output, jint output_offset,
        jint capacity) {
    (void)owner;
    if (states == NULL || output == NULL || first < 0 || length < 0
            || output_offset < 0 || capacity < 0 || required < 0 || required > 255
            || excluded < 0 || excluded > 255 || (length != 0 && capacity == 0)) {
        m3_positions_page_invalid(env);
        return 0;
    }
    jsize state_length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize output_length = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (first > state_length || length > state_length - first
            || output_offset > output_length || capacity > output_length - output_offset) {
        m3_positions_page_invalid(env);
        return 0;
    }
    jint end = first + length;
    if ((required & excluded) != 0) return ((jlong)end) << 32;
    const unsigned int mask = (unsigned int)(required | excluded);
    jbyte marks[M3_CHUNK];
    jint indices[M3_CHUNK];
    jint written = 0;
    for (jint examined = 0; examined < length;) {
        jint remaining = length - examined;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0U) {
            (*env)->GetByteArrayRegion(env, states, first + examined, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        jsize buffered = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = mask == 0U ? 0U : (unsigned int)(unsigned char)marks[i];
            if ((flags & mask) == (unsigned int)required) {
                indices[buffered++] = first + examined + i;
                if (buffered == M3_CHUNK || written + buffered == capacity) {
                    (*env)->SetIntArrayRegion(env, output, output_offset + written,
                            buffered, indices);
                    if ((*env)->ExceptionCheck(env)) return 0;
                    written += buffered;
                    buffered = 0;
                    if (written == capacity) {
                        jint next = first + examined + i + 1;
                        return (((jlong)next) << 32) | (jlong)(unsigned int)written;
                    }
                }
            }
        }
        if (buffered != 0) {
            (*env)->SetIntArrayRegion(env, output, output_offset + written,
                    buffered, indices);
            if ((*env)->ExceptionCheck(env)) return 0;
            written += buffered;
        }
        examined += chunk;
    }
    return (((jlong)end) << 32) | (jlong)(unsigned int)written;
}

//FLLDCIM: RELOCATE-MERGE begin - donor blob b20d77f0f2d8 at ac5feec00a6d; recipe m3/collections/recipe/entryless-lane-kernel-20261106/relocate.py v3
/* Candidate-only batched primitive lane oracle. Does not allocate or pin Java object arrays. */
#include <jni.h>
#include <stdint.h>
#include <limits.h>

#define M3_BATCH 256
#define M3_LIVE 1

static int m3_throw(JNIEnv *env, const char *exception, const char *message) {
    jclass type = (*env)->FindClass(env, exception);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return 0;
}

/* JNI regions are bounded; no GetPrimitiveArrayCritical and no pointer survives the call. */
static int m3_scan(JNIEnv *env, jlongArray keys, jlongArray values, jbyteArray state,
        jint start, jint count, jlong key_min, jlong key_max, jlong value_min,
        jlong value_max, jintArray result, jint result_start, jint *matches, jint *first) {
    if (first != NULL) *first = -1;
    jlong key_batch[M3_BATCH], value_batch[M3_BATCH];
    jbyte state_batch[M3_BATCH];
    jint matched_slots[M3_BATCH];
    /* A full inclusive signed span cannot reject a key or value. JNI preflight still
     * validates all three array references and their windows before this helper. */
    const int filter_keys = key_min != INT64_MIN || key_max != INT64_MAX;
    const int filter_values = value_min != INT64_MIN || value_max != INT64_MAX;
    jint total = 0;
    for (jint offset = 0; offset < count; ) {
        const jint len = count - offset > M3_BATCH ? M3_BATCH : count - offset;
        const jsize base = start + offset;
        if (filter_keys) {
            (*env)->GetLongArrayRegion(env, keys, base, len, key_batch);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        if (filter_values) {
            (*env)->GetLongArrayRegion(env, values, base, len, value_batch);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        (*env)->GetByteArrayRegion(env, state, base, len, state_batch);
        if ((*env)->ExceptionCheck(env)) return 0;
        jint batch_matches = 0;
        /* Preserve the prior fully restricted loop byte-for-byte. Other shapes
         * avoid accessing any uninitialized, deliberately unborrowed payload. */
        if (filter_keys && filter_values) {
            for (jint i = 0; i < len; i++) {
                if ((state_batch[i] & M3_LIVE) == 0
                        || key_batch[i] < key_min || key_batch[i] > key_max
                        || value_batch[i] < value_min || value_batch[i] > value_max) continue;
                if (first != NULL) {
                    *first = base + i;
                    return 1;
                }
                matched_slots[batch_matches++] = base + i;
            }
        } else {
            for (jint i = 0; i < len; i++) {
                if ((state_batch[i] & M3_LIVE) == 0
                        || (filter_keys && (key_batch[i] < key_min || key_batch[i] > key_max))
                        || (filter_values && (value_batch[i] < value_min || value_batch[i] > value_max))) continue;
                if (first != NULL) {
                    *first = base + i;
                    return 1;
                }
                matched_slots[batch_matches++] = base + i;
            }
        }
        if (result != NULL && batch_matches != 0) {
            (*env)->SetIntArrayRegion(env, result, result_start + total, batch_matches, matched_slots);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        total += batch_matches;
        offset += len;
    }
    if (matches != NULL) *matches = total;
    return 1;
}

/* The native entrypoints defend their own JNI boundary, independently of Java validation. */
static int m3_validate(JNIEnv *env, jlongArray keys, jlongArray values, jbyteArray state,
        jint start, jint count, jlong key_min, jlong key_max, jlong value_min, jlong value_max) {
    if (keys == NULL || values == NULL || state == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null lane");
        return 0;
    }
    if (key_min > key_max || value_min > value_max) {
        m3_throw(env, "java/lang/IllegalArgumentException", "inverted lane filter bounds");
        return 0;
    }
    const jsize klen = (*env)->GetArrayLength(env, keys);
    const jsize vlen = (*env)->GetArrayLength(env, values);
    const jsize slen = (*env)->GetArrayLength(env, state);
    if (start < 0 || count < 0 || start > klen || count > klen - start
            || start > vlen || count > vlen - start
            || start > slen || count > slen - start) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "lane window exceeds backing arrays");
        return 0;
    }
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_nativeSelect(JNIEnv *env, jclass clazz,
        jlongArray keys, jlongArray values, jbyteArray state, jint start, jint count,
        jlong key_min, jlong key_max, jlong value_min, jlong value_max) {
    (void) clazz;
    if (!m3_validate(env, keys, values, state, start, count, key_min, key_max, value_min, value_max))
        return NULL;
    jint total = 0;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, NULL, 0, &total, NULL)) return NULL;
    jintArray output = (*env)->NewIntArray(env, total);
    if (output == NULL) return NULL;
    jint verified = 0;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, output, 0, &verified, NULL)) return NULL;
    if (verified != total) {
        m3_throw(env, "java/util/ConcurrentModificationException",
                "lanes changed during native two-pass selection");
        return NULL;
    }
    return output;
}

/* Destination-array selection: bounded region writes into caller-owned primitive storage.
 * With stable input lanes, capacity and offset rejection occurs before first write.
 * No JVM array pointer or native reference survives this call. */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_nativeSelectInto(JNIEnv *env, jclass clazz,
        jlongArray keys, jlongArray values, jbyteArray state, jint start, jint count,
        jlong key_min, jlong key_max, jlong value_min, jlong value_max,
        jintArray output, jint output_start) {
    (void) clazz;
    if (!m3_validate(env, keys, values, state, start, count, key_min, key_max, value_min, value_max))
        return -1;
    if (output == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "output");
        return -1;
    }
    const jsize capacity = (*env)->GetArrayLength(env, output);
    if (output_start < 0 || output_start > capacity) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid output start");
        return -1;
    }
    jint required = 0;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, NULL, 0, &required, NULL)) return -1;
    if (required > capacity - output_start) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "insufficient output capacity");
        return -1;
    }
    if (required == 0) return 0;
    jint written = 0;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, output, output_start, &written, NULL)) return -1;
    if (written != required) {
        m3_throw(env, "java/util/ConcurrentModificationException",
                "lane selection changed during scan");
        return -1;
    }
    return written;
}

/* One-pass no-allocation queries. No Java object comparisons or retained JNI pointers. */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_nativeCount(JNIEnv *env, jclass clazz,
        jlongArray keys, jlongArray values, jbyteArray state, jint start, jint count,
        jlong key_min, jlong key_max, jlong value_min, jlong value_max) {
    (void) clazz;
    if (!m3_validate(env, keys, values, state, start, count, key_min, key_max, value_min, value_max))
        return -1;
    jint total = 0;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, NULL, 0, &total, NULL)) return -1;
    return total;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_nativeFirst(JNIEnv *env, jclass clazz,
        jlongArray keys, jlongArray values, jbyteArray state, jint start, jint count,
        jlong key_min, jlong key_max, jlong value_min, jlong value_max) {
    (void) clazz;
    if (!m3_validate(env, keys, values, state, start, count, key_min, key_max, value_min, value_max))
        return -1;
    jint first = -1;
    if (!m3_scan(env, keys, values, state, start, count,
            key_min, key_max, value_min, value_max, NULL, 0, NULL, &first)) return -1;
    return first;
}

/* One-pass bounded page, preserving physical slot order, using only primitive JNI regions.
 * No Java array address is retained. Next is absolute; count is packed in high 32 bits. */
JNIEXPORT jlong JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_nativeScanPageInto(JNIEnv *env, jclass clazz,
        jlongArray keys, jlongArray values, jbyteArray state, jint start, jint count,
        jlong key_min, jlong key_max, jlong value_min, jlong value_max,
        jintArray output, jint output_start) {
    (void) clazz;
    if (!m3_validate(env, keys, values, state, start, count, key_min, key_max, value_min, value_max))
        return (jlong) -1;
    if (output == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "output");
        return (jlong) -1;
    }
    const jsize capacity = (*env)->GetArrayLength(env, output);
    if (output_start < 0 || output_start > capacity) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid output start");
        return (jlong) -1;
    }
    if (count != 0 && output_start == capacity) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "nonempty page requires output capacity");
        return (jlong) -1;
    }
    jlong key_batch[M3_BATCH], value_batch[M3_BATCH];
    jbyte state_batch[M3_BATCH];
    jint slot_batch[M3_BATCH];
    /* A full inclusive signed range cannot reject any key/value payload.
     * Do not copy payload lanes that have no observable influence on selection. */
    const int filter_keys = key_min != INT64_MIN || key_max != INT64_MAX;
    const int filter_values = value_min != INT64_MIN || value_max != INT64_MAX;
    const jint limit = capacity - output_start;
    jint written = 0;
    for (jint offset = 0; offset < count; ) {
        const jint batch_len = count - offset > M3_BATCH ? M3_BATCH : count - offset;
        const jint base = start + offset;
        if (filter_keys) {
            (*env)->GetLongArrayRegion(env, keys, base, batch_len, key_batch);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        }
        if (filter_values) {
            (*env)->GetLongArrayRegion(env, values, base, batch_len, value_batch);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        }
        (*env)->GetByteArrayRegion(env, state, base, batch_len, state_batch);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        jint batch_matches = 0;
        /* Preserve the original fully-filtered loop as the hot restricted case. */
        if (filter_keys && filter_values) {
            for (jint i = 0; i < batch_len; i++) {
                if ((state_batch[i] & M3_LIVE) == 0
                        || key_batch[i] < key_min || key_batch[i] > key_max
                        || value_batch[i] < value_min || value_batch[i] > value_max) continue;
                slot_batch[batch_matches++] = base + i;
                written++;
                if (written == limit) {
                    (*env)->SetIntArrayRegion(env, output, output_start + written - batch_matches,
                            batch_matches, slot_batch);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    return ((jlong) written << 32) | (uint32_t) (base + i + 1);
                }
            }
        } else {
            for (jint i = 0; i < batch_len; i++) {
                if ((state_batch[i] & M3_LIVE) == 0
                        || (filter_keys && (key_batch[i] < key_min || key_batch[i] > key_max))
                        || (filter_values && (value_batch[i] < value_min || value_batch[i] > value_max))) continue;
                slot_batch[batch_matches++] = base + i;
                written++;
                if (written == limit) {
                    (*env)->SetIntArrayRegion(env, output, output_start + written - batch_matches,
                            batch_matches, slot_batch);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    return ((jlong) written << 32) | (uint32_t) (base + i + 1);
                }
            }
        }
        if (batch_matches != 0) {
            (*env)->SetIntArrayRegion(env, output, output_start + written - batch_matches,
                    batch_matches, slot_batch);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        }
        offset += batch_len;
    }
    return ((jlong) written << 32) | (uint32_t) (start + count);
}

/* ABI probe for the additive key/value signed-range lane kernel (recipe entryless-lane-kernel-20261106);
 * older m3_collections_mask binaries keep their existing exports and the Java lane paths stay exact.
 */
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3IndexMaskCompactor_laneKernelAbiNative(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}
//FLLDCIM: RELOCATE-MERGE end - donor blob b20d77f0f2d8
