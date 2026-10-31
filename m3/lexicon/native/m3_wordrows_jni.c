/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Optional JNI projection over the existing immutable M3 word-row mapping.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

static void fail(JNIEnv *env, const char *class_name, const char *message) {
    jclass exception = (*env)->FindClass(env, class_name);
    if (exception != NULL) (*env)->ThrowNew(env, exception, message);
}

/* Keep native coordinates and array windows scoped to this call. */
JNIEXPORT void JNICALL
Java_com_m3_text_M3WordRowsNative_bulkFlags0(JNIEnv *env, jclass klass,
        jobject buffer, jint rows_base, jint count, jintArray ids, jintArray output) {
    (void) klass;
    if (buffer == NULL || ids == NULL || output == NULL) {
        fail(env, "java/lang/NullPointerException", "word-row JNI argument");
        return;
    }
    unsigned char *data = (unsigned char *) (*env)->GetDirectBufferAddress(env, buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (data == NULL || capacity < 0 || rows_base < 0 || count < 0
            || ((jlong) rows_base + (jlong) count * 128) > capacity) {
        fail(env, "java/lang/IllegalArgumentException", "invalid direct M3 row image");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, ids);
    if (size != (*env)->GetArrayLength(env, output)) {
        fail(env, "java/lang/IllegalArgumentException", "word-row JNI lengths differ");
        return;
    }
    for (jsize offset = 0; offset < size; offset += 128) {
        jsize take = size - offset < 128 ? size - offset : 128;
        jint input[128];
        jint result[128];
        (*env)->GetIntArrayRegion(env, ids, offset, take, input);
        if ((*env)->ExceptionCheck(env)) return;
        for (jsize i = 0; i < take; i++) {
            if (input[i] < 0 || input[i] >= count) {
                fail(env, "java/lang/IndexOutOfBoundsException", "invalid word-row local ID");
                return;
            }
            uint64_t address = (uint64_t) (uint32_t) rows_base + (uint64_t) (uint32_t) input[i] * 128;
            const unsigned char *p = data + (size_t) address;
            uint32_t bits = ((uint32_t) p[0] << 24) | ((uint32_t) p[1] << 16)
                    | ((uint32_t) p[2] << 8) | (uint32_t) p[3];
            result[i] = (jint) bits;
        }
        (*env)->SetIntArrayRegion(env, output, offset, take, result);
        if ((*env)->ExceptionCheck(env)) return;
    }
}
