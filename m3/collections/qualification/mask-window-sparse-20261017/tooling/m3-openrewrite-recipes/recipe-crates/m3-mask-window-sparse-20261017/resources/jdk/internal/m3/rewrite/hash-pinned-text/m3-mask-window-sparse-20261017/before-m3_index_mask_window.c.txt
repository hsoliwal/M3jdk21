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

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint values[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < n && written < limit) {
        jint take = n - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        (*env)->GetIntArrayRegion(env, rows, next, take, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        for (jint i = 0; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
                selected[pending++] = values[i];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
                }
            }
            next++;
            if (written == limit) break;
        }
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);
}
