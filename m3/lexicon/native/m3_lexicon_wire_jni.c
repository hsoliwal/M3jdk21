// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>

static jint fail(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) (*env)->ThrowNew(env, type, message);
    return -1;
}

JNIEXPORT jint JNICALL Java_com_m3_lexicon_M3LexiconWireNative_expand(
        JNIEnv *env, jclass type, jobject arena, jintArray offsets,
        jintArray lengths, jintArray cwids, jobject output) {
    (void) type;
    if (arena == NULL || offsets == NULL || lengths == NULL || cwids == NULL || output == NULL) {
        return fail(env, "null JNI lexicon lane");
    }
    jlong source_length = (*env)->GetDirectBufferCapacity(env, arena);
    jlong destination_length = (*env)->GetDirectBufferCapacity(env, output);
    uint8_t *source = (uint8_t *)(*env)->GetDirectBufferAddress(env, arena);
    uint8_t *destination = (uint8_t *)(*env)->GetDirectBufferAddress(env, output);
    if (source_length < 0 || destination_length < 0 || source == NULL || destination == NULL) {
        return fail(env, "direct ByteBuffer required");
    }
    jclass buffer = (*env)->FindClass(env, "java/nio/ByteBuffer");
    if (buffer == NULL) return -1;
    jmethodID read_only = (*env)->GetMethodID(env, buffer, "isReadOnly", "()Z");
    if (read_only == NULL) return -1;
    jboolean readonly = (*env)->CallBooleanMethod(env, output, read_only);
    if ((*env)->ExceptionCheck(env)) return -1;
    if (readonly) return fail(env, "read-only output");
    jsize rows = (*env)->GetArrayLength(env, offsets);
    if (rows != (*env)->GetArrayLength(env, lengths)) {
        return fail(env, "row lane length mismatch");
    }
    jsize count = (*env)->GetArrayLength(env, cwids);
    jint *off = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (off == NULL) return -1;
    jint *len = (*env)->GetIntArrayElements(env, lengths, NULL);
    if (len == NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets, off, JNI_ABORT);
        return -1;
    }
    jint *ids = (*env)->GetIntArrayElements(env, cwids, NULL);
    if (ids == NULL) {
        (*env)->ReleaseIntArrayElements(env, lengths, len, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, offsets, off, JNI_ABORT);
        return -1;
    }
    const char *reason = NULL;
    jlong total = 0;
    for (jsize i = 0; i < count; i++) {
        jint id = ids[i];
        if (id <= 0 || id > rows) { reason = "invalid CWID"; break; }
        jint begin = off[id - 1];
        jint size = len[id - 1];
        if (begin < 0 || size <= 0 || (jlong) begin + size > source_length
                || size > destination_length - total || size > INT32_MAX - total) {
            reason = "invalid row bounds";
            break;
        }
        total += size;
    }
    // Overlapping direct-buffer views are unsafe. Check without address+capacity wraparound.
    if (reason == NULL && total != 0) {
        uintptr_t a = (uintptr_t) source;
        uintptr_t b = (uintptr_t) destination;
        uintptr_t span = (uintptr_t) source_length;
        uintptr_t target_span = (uintptr_t) total;
        if ((a <= b && (uintptr_t) (b - a) < span)
                || (b < a && (uintptr_t) (a - b) < target_span)) {
            reason = "overlapping direct buffers";
        }
    }
    if (reason == NULL) {
        jlong write = 0;
        for (jsize i = 0; i < count; i++) {
            jint row = ids[i] - 1;
            memcpy(destination + write, source + off[row], (size_t) len[row]);
            write += len[row];
        }
    }
    (*env)->ReleaseIntArrayElements(env, cwids, ids, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, lengths, len, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, off, JNI_ABORT);
    return reason == NULL ? (jint) total : fail(env, reason);
}
