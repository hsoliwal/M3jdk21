/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * Adapt the existing Synexia bag_counts test atom to runtime int count lanes.
 * No source from competitive-programming statements or submissions is included.
 */
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include "com_m3_collections_interop_M3CountAlgebra.h"

static void fail(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_interop_M3CountAlgebra_combine0(
        JNIEnv *env, jclass owner, jintArray left, jintArray right, jint operation) {
    (void) owner;
    if (left == NULL || right == NULL) {
        fail(env, "java/lang/NullPointerException", "count lane");
        return NULL;
    }
    if (operation < 0 || operation > 3) {
        fail(env, "java/lang/IllegalArgumentException", "operation");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, left);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize right_length = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (length != right_length) {
        fail(env, "java/lang/IllegalArgumentException", "unaligned lengths");
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, length);
    if (result == NULL || (*env)->ExceptionCheck(env)) return NULL;
    /* Fixed 12,288-byte input/output buffer payload, excluding frame/JVM costs. */
    jint a[1024], b[1024], out[1024];
    for (jsize offset = 0; offset < length;) {
        jsize count = length - offset;
        if (count > 1024) count = 1024;
        (*env)->GetIntArrayRegion(env, left, offset, count, a);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, right, offset, count, b);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < count; i++) {
            if (a[i] < 0 || b[i] < 0) {
                fail(env, "java/lang/IllegalArgumentException", "negative count");
                return NULL;
            }
            switch (operation) {
                case 0: {
                    int64_t sum = (int64_t) a[i] + b[i];
                    if (sum > INT32_MAX) {
                        fail(env, "java/lang/IllegalArgumentException", "count overflow");
                        return NULL;
                    }
                    out[i] = (jint) sum;
                    break;
                }
                case 1: out[i] = a[i] > b[i] ? a[i] : b[i]; break;
                case 2: out[i] = a[i] < b[i] ? a[i] : b[i]; break;
                default: out[i] = a[i] > b[i] ? a[i] - b[i] : 0; break;
            }
        }
        (*env)->SetIntArrayRegion(env, result, offset, count, out);
        if ((*env)->ExceptionCheck(env)) return NULL;
        offset += count;
    }
    return result;
}
