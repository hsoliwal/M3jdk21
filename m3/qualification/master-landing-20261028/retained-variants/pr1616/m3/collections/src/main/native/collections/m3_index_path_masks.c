/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-10-21: bounded chunked gather/filter/compact in the existing JNI owner.
 * Region APIs only; no retained heap address, callback, or per-row heap allocation.
 * Inputs must remain stable for the call. This is not a concurrent collection protocol.
 */
#include <jni.h>
#include <stddef.h>

/* Scratch capacity, not a limit on logical paths, state stages, or recipe atoms. */
#define M3_CHUNK 256

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (void)(*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}

/* Gather a valid signed-stride segment into call-local primitive scratch. */
static int m3_gather(JNIEnv *env, jintArray source, jlong first, jint stride,
                     jsize count, jint *output) {
    if (stride == 1 || stride == -1) {
        jlong low = stride == 1 ? first : first - (jlong)count + 1;
        (*env)->GetIntArrayRegion(env, source, (jsize)low, count, output);
        if ((*env)->ExceptionCheck(env)) return 0;
        if (stride == -1) {
            for (jsize i = 0; i < count / 2; ++i) {
                jint swap = output[i];
                output[i] = output[count - 1 - i];
                output[count - 1 - i] = swap;
            }
        }
    } else {
        for (jsize i = 0; i < count; ++i) {
            jlong at = first + (jlong)i * stride;
            (*env)->GetIntArrayRegion(env, source, (jsize)at, 1, output + i);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
    }
    return 1;
}

/* Preserve each row's left-to-right stage short circuit; filter primitive metadata only. */
static int m3_filter(JNIEnv *env, jobjectArray planes, jintArray needs, jintArray denies,
                     jsize stages, const jint *physical, jsize count, unsigned char *alive) {
    jint states[M3_CHUNK];
    int remaining = count;
    for (jsize i = 0; i < count; ++i) alive[i] = 1;
    for (jsize stage = 0; stage < stages && remaining != 0; ++stage) {
        jint required = 0, forbidden = 0;
        (*env)->GetIntArrayRegion(env, needs, stage, 1, &required);
        if ((*env)->ExceptionCheck(env)) return -1;
        (*env)->GetIntArrayRegion(env, denies, stage, 1, &forbidden);
        if ((*env)->ExceptionCheck(env)) return -1;
        jobject object = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (object == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return -1;
        }
        jintArray plane = (jintArray)object;
        for (jsize begin = 0; begin < count;) {
            if (!alive[begin]) { ++begin; continue; }
            jint direction = 1;
            jsize end = begin + 1;
            if (end < count && alive[end]
                    && (jlong)physical[end] == (jlong)physical[begin] - 1) direction = -1;
            while (end < count && alive[end]
                    && (jlong)physical[end] == (jlong)physical[begin]
                            + (jlong)(end - begin) * direction) ++end;
            if (!m3_gather(env, plane, physical[begin], direction, end - begin, states)) {
                (*env)->DeleteLocalRef(env, object);
                return -1;
            }
            for (jsize i = begin; i < end; ++i) {
                jint state = states[i - begin];
                if ((state & required) != required || (state & forbidden) != 0) {
                    alive[i] = 0;
                    --remaining;
                }
            }
            begin = end;
        }
        (*env)->DeleteLocalRef(env, object);
    }
    return remaining;
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride) {
    (void)owner;
    if (rows == NULL || offsets == NULL || planes == NULL || required == NULL || forbidden == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null native mask path argument");
        return NULL;
    }
    jsize size = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize position_count = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize stages = (*env)->GetArrayLength(env, planes);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_required = (*env)->GetArrayLength(env, required);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_forbidden = (*env)->GetArrayLength(env, forbidden);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (size != position_count || stages == 0 || stages != n_required || stages != n_forbidden) {
        m3_throw(env, "java/lang/IllegalArgumentException", "unaligned native mask planes");
        return NULL;
    }
    /* Defensive native admission also checks planes an empty/early-reject path never reads. */
    jsize capacity = 0;
    for (jsize stage = 0; stage < stages; ++stage) {
        jobject plane = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return NULL;
        if (plane == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return NULL;
        }
        jsize length = (*env)->GetArrayLength(env, plane);
        (*env)->DeleteLocalRef(env, plane);
        if ((*env)->ExceptionCheck(env)) return NULL;
        if (stage == 0 || length < capacity) capacity = length;
    }
    if (count < 0 || stride == 0 || first < 0 || first > size) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid native mask path");
        return NULL;
    }
    if (count > 0) {
        jlong last = (jlong)first + ((jlong)count - 1) * (jlong)stride;
        if (first == size || last < 0 || last >= size) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "native path out of bounds");
            return NULL;
        }
    }
    jint physical[M3_CHUNK], row_buffer[M3_CHUNK], selected[M3_CHUNK];
    unsigned char alive[M3_CHUNK];
    jsize matching = 0;
    jintArray result = NULL;
    for (int pass = 0; pass < 2; ++pass) {
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL || matching == 0) return result;
        }
        jsize written = 0;
        for (jlong ordinal = 0; ordinal < count;) {
            jsize chunk = (jsize)(((jlong)count - ordinal) < M3_CHUNK
                    ? (jlong)count - ordinal : M3_CHUNK);
            jlong at = (jlong)first + ordinal * stride;
            if (!m3_gather(env, offsets, at, stride, chunk, physical)) return NULL;
            for (jsize i = 0; i < chunk; ++i) {
                if (physical[i] < 0 || physical[i] >= capacity) {
                    m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
                    return NULL;
                }
            }
            int kept = m3_filter(env, planes, required, forbidden, stages, physical, chunk, alive);
            if (kept < 0) return NULL;
            if (pass == 0) {
                matching += kept;
            } else if (kept != 0) {
                if (kept > matching - written) {
                    m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                    return NULL;
                }
                if (!m3_gather(env, rows, at, stride, chunk, row_buffer)) return NULL;
                jsize output = 0;
                for (jsize i = 0; i < chunk; ++i) if (alive[i]) selected[output++] = row_buffer[i];
                (*env)->SetIntArrayRegion(env, result, written, output, selected);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += output;
            }
            ordinal += chunk;
        }
        if (pass == 1 && written != matching) {
            m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
            return NULL;
        }
    }
    return result;
}
