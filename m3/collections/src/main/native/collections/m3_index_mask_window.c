/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Additional ABI in the existing m3_collections_mask shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include <jni.h>
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (first < 0 || first > n || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    if ((required & excluded) != 0) return m3_window_result(n, 0);

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint values[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    while (next < n && written < limit) {
        jint take = n - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        if ((required | excluded) == 0) {
            // Every ordinal qualifies: copy only the caller's remaining window, no states.
            if (take > limit - written) take = limit - written;
            (*env)->GetIntArrayRegion(env, rows, next, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + written, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            next += take;
            written += take;
            continue;
        }
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        // A transient two-word selection bitmap owns decisions, never payload or object state.
        uint64_t selected[2] = {UINT64_C(0), UINT64_C(0)};
        for (jint i = 0; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
                selected[i >> 6] |= UINT64_C(1) << (i & 63);
            }
        }
        if ((selected[0] | selected[1]) == 0) {
            // Metadata rejects the whole block: do not fetch row IDs that cannot be emitted.
            next += take;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, next, take, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        jint copied = 0;
        jint consumed = 0;
        while (consumed < take && copied < limit - written) {
            if ((selected[consumed >> 6] & (UINT64_C(1) << (consumed & 63))) != 0) {
                // Stable prefix compaction: copied never exceeds the unconsumed local slot.
                values[copied++] = values[consumed];
            }
            consumed++;
        }
        (*env)->SetIntArrayRegion(env, output, output_offset + written, copied, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        next += consumed;
        written += copied;
    }
    return m3_window_result(next, written);
}
