/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2026 Hitesh Soliwal and contributors.
 * Modified 2026-10-17: bounded index chunks and contiguous region reads.
 * Modified 2026-10-19: Java physical-bounds exception class and diagnostic parity.
 */
#include <jni.h>
#include <stdio.h>

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
            jclass error = (*env)->FindClass(env, "java/lang/ArrayIndexOutOfBoundsException");
            if (error != NULL) {
                char message[96];
                (void) snprintf(message, sizeof(message), "Index %d out of bounds for length %d",
                        (int) index[valid], (int) payload_length);
                (*env)->ThrowNew(env, error, message);
            }
            return;
        }
        done += count;
    }
}
