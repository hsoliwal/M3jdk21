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
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
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
    jint accepted[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            (*env)->SetIntArrayRegion(env, result, written, batch, accepted);
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
    jlong accepted[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            (*env)->SetLongArrayRegion(env, result, written, batch, accepted);
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


/* SPDX-License-Identifier: Apache-2.0
 * 2026 M3JDK21 indexed mask-window native repair.
 * The original one-shot compactor JNI entrypoints above remain unchanged.
 */
#include <stdint.h>
#define M3_WINDOW_CHUNK 256

static jlong m3_window_pack(jint next, jint written) {
    return ((jlong)(uint32_t)next << 32) | (jlong)(uint32_t)written;
}
static void m3_window_invalid(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, "invalid indexed window geometry, masks or alias");
        (*env)->DeleteLocalRef(env, type);
    }
}
static int m3_window_inputs(JNIEnv *env, jarray rows, jbyteArray states,
        jarray output, jint first, jint required, jint excluded,
        jint outputOffset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL
            || first < 0 || required < 0 || required > 255
            || excluded < 0 || excluded > 255
            || outputOffset < 0 || limit < 0 || limit > 4096) {
        m3_window_invalid(env);
        return 0;
    }
    if ((*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        m3_window_invalid(env);
        return 0;
    }
    jsize length = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize flagsLength = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (flagsLength != length || first > length
            || outputOffset > capacity || limit > capacity - outputOffset) {
        m3_window_invalid(env);
        return 0;
    }
    return 1;
}

/* Exact same primitive region protocol for int[] and long[]; output is
 * caller-owned, alias forbidden, maximum stack chunk 256 elements.
 * Every inspected source index advances the returned resume cursor.
 */
#define M3_DEFINE_WINDOW_JNI(SUFFIX, ARR, VALUE, GET, SET) \
JNIEXPORT jlong JNICALL Java_com_m3_collections_M3IndexMaskWindow_##SUFFIX( \
        JNIEnv *env, jclass owner, ARR rows, jbyteArray states, jint first, \
        jint required, jint excluded, ARR output, jint outputOffset, jint limit) { \
    (void)owner; \
    if (!m3_window_inputs(env, rows, states, output, first, required, \
            excluded, outputOffset, limit)) return (jlong)0; \
    jsize length = (*env)->GetArrayLength(env, rows); \
    if ((*env)->ExceptionCheck(env)) return (jlong)0; \
    if (limit == 0) return m3_window_pack(first, 0); \
    if ((required & excluded) != 0) return m3_window_pack(length, 0); \
    VALUE readBuffer[M3_WINDOW_CHUNK]; \
    VALUE selected[M3_WINDOW_CHUNK]; \
    jbyte flags[M3_WINDOW_CHUNK]; \
    jint next = first, kept = 0; \
    while (next < length && kept < limit) { \
        jint remaining = length - next; \
        jsize amount = remaining < M3_WINDOW_CHUNK ? remaining : M3_WINDOW_CHUNK; \
        if (amount > limit - kept && required == 0 && excluded == 0) \
            amount = limit - kept; \
        (*env)->GET(env, rows, next, amount, readBuffer); \
        if ((*env)->ExceptionCheck(env)) return (jlong)0; \
        if (required != 0 || excluded != 0) { \
            (*env)->GetByteArrayRegion(env, states, next, amount, flags); \
            if ((*env)->ExceptionCheck(env)) return (jlong)0; \
        } \
        jint consumed = 0, accepted = 0; \
        while (consumed < amount && kept + accepted < limit) { \
            unsigned int bits = (required | excluded) == 0 ? 0u \
                    : (unsigned int)(unsigned char)flags[consumed]; \
            if ((bits & (unsigned int)required) == (unsigned int)required \
                    && (bits & (unsigned int)excluded) == 0u) { \
                selected[accepted++] = readBuffer[consumed]; \
            } \
            consumed++; \
        } \
        if (accepted > 0) { \
            (*env)->SET(env, output, outputOffset + kept, accepted, selected); \
            if ((*env)->ExceptionCheck(env)) return (jlong)0; \
        } \
        kept += accepted; \
        next += consumed; \
    } \
    return m3_window_pack(next, kept); \
}
M3_DEFINE_WINDOW_JNI(scanNative, jintArray, jint, GetIntArrayRegion, SetIntArrayRegion)
M3_DEFINE_WINDOW_JNI(scanLongNative, jlongArray, jlong, GetLongArrayRegion, SetLongArrayRegion)
#undef M3_DEFINE_WINDOW_JNI
