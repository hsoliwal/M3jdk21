/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
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

/* Reuse the state lane to bound each row fetch. Empty selection chunks never
 * cross JNI for row values. Bounds are local offsets; no heap scratch or new ABI.
 */
static jsize m3_selected_span(const jbyte *marks, jsize length, jint mask, jsize *end) {
    jsize first = 0;
    *end = length;
    if (mask == 0) return first;
    while (first < length && (((unsigned char) marks[first] & (unsigned int) mask) != (unsigned int) mask)) first++;
    if (first == length) { *end = first; return first; }
    while (*end > first && (((unsigned char) marks[*end - 1] & (unsigned int) mask) != (unsigned int) mask)) (*end)--;
    return first;
}

/* One core for legacy whole-index and offset windows. All bounds validated by exports.
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */
static jintArray m3_select_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jsize first, jsize n, jint required_mask) {
    jbyte marks[M3_CHUNK];
    jint row_ids[M3_CHUNK];
    jsize count = required_mask == 0 ? n : 0;
    for (jsize offset = 0; required_mask != 0 && offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) count++;
        }
        offset += chunk;
    }
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL) return NULL;
    jsize written = 0;
    for (jsize offset = 0; offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        if (required_mask != 0) {
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, required_mask, &stop);
        if (begin == stop) { offset += chunk; continue; }
        (*env)->GetIntArrayRegion(env, rows, first + offset + begin, stop - begin, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = begin; i < stop; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i - begin];
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
Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
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
    return m3_select_range(env, rows, states, 0, n, required_mask);
}

/* Capability discovery keeps older binaries usable through their original native ABI. */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_rangeAbiNative(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
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
    return m3_select_range(env, rows, states, first, length, required_mask);
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

/* ABI probe for direct logical-position selection from the state lane. */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_positionsAbiNative(JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative(
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
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) positions[accepted++] = offset + i;
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
Java_com_m3_collections_M3IndexMaskCompactor_positionsMasksAbiNative(
        JNIEnv *env, jclass owner) {
    (void)env; (void)owner;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsMasksNative(
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

    jbyte marks[M3_CHUNK];
    jint kept = 0;
    if ((required | excluded) == 0) {
        kept = length;
    } else {
        for (jint offset = 0; offset < length;) {
            jint remaining = length - offset;
            jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)required) == (unsigned int)required
                        && (flags & (unsigned int)excluded) == 0U) {
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
        if ((required | excluded) != 0) {
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (required | excluded) == 0
                    ? 0U : (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)required) == (unsigned int)required
                    && (flags & (unsigned int)excluded) == 0U) {
                positions[accepted++] = offset + i;
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
Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
        JNIEnv *env, jclass owner, jintArray rows, jint row_offset,
        jbyteArray states, jint state_offset, jint length, jint mask) {
    (void)owner;
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, mask, &stop);
        if (begin == stop) { position += chunk; continue; }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position + begin, stop - begin, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = stop - begin;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i - begin, so unread values stay intact. */
            batch = 0;
            for (jsize i = begin; i < stop; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i - begin];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
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

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
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
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, mask, &stop);
        if (begin == stop) { position += chunk; continue; }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position + begin, stop - begin, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = stop - begin;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i - begin, so unread values stay intact. */
            batch = 0;
            for (jsize i = begin; i < stop; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i - begin];
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
