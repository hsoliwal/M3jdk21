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
    jbyte marks[M3_CHUNK];
    jint row_ids[M3_CHUNK];
    jint selected[M3_CHUNK];
    jsize count = 0;
    for (jsize offset = 0; offset < n;) {
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
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, rows, offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                selected[accepted++] = row_ids[i];
            }
        }
        if (accepted > count - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetIntArrayRegion(env, result, written, accepted, selected);
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

/* Opt-in long-coordinate kernel, identical mask-order contract to selectNative.
 * A detached batch result is not a live Java collection view.
 */
JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectLongNative(
        JNIEnv *env, jclass owner, jlongArray rows, jbyteArray states, jint required_mask) {
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
    jsize marks_length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (marks_length != n) {
        jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exception != NULL) {
            (*env)->ThrowNew(env, exception, "state/index length mismatch");
            (*env)->DeleteLocalRef(env, exception);
        }
        return NULL;
    }
    jbyte marks[M3_CHUNK];
    jlong row_ids[M3_CHUNK];
    jlong selected[M3_CHUNK];
    jsize count = 0;
    for (jsize offset = 0; offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) count++;
        }
        offset += chunk;
    }
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result == NULL) return NULL;
    jsize written = 0;
    for (jsize offset = 0; offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetLongArrayRegion(env, rows, offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                selected[accepted++] = row_ids[i];
            }
        }
        if (accepted > count - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (accepted > 0) {
            (*env)->SetLongArrayRegion(env, result, written, accepted, selected);
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
