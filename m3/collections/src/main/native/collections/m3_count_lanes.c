/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * Derived from Synexia m3-four-library-bags bag_counts.c at
 * 6c33a5772a12ca99c8bff6a02403b28be4202bb3. Modified for M3CountLanes' explicit
 * production JNI entry point; original checked arithmetic and bounded regions retained.
 * Fixed scratch is three 1024-element jlong arrays (24 KiB on this JNI ABI).
 */
#include <jni.h>
#include <stdint.h>
#define CHUNK 1024
_Static_assert(sizeof(jlong) == sizeof(int64_t), "64-bit JNI counts required");

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) {
        (void)(*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3CountLanes_combineNative(
        JNIEnv *env, jclass owner, jlongArray left, jint left_offset,
        jlongArray right, jint right_offset, jint length, jint operation) {
    (void)owner;
    if (left == NULL || right == NULL) {
        fail(env, "java/lang/NullPointerException", "count lane"); return NULL;
    }
    jsize left_length = (*env)->GetArrayLength(env, left);
    jsize right_length = (*env)->GetArrayLength(env, right);
    if (left_offset < 0 || length < 0 || left_offset > left_length - length
            || right_offset < 0 || right_offset > right_length - length) {
        fail(env, "java/lang/IndexOutOfBoundsException", "count slice"); return NULL;
    }
    if (operation < 0 || operation > 3) {
        fail(env, "java/lang/IllegalArgumentException", "operation"); return NULL;
    }
    jlongArray result = (*env)->NewLongArray(env, length);
    if (result == NULL) return NULL;
    jlong a[CHUNK], b[CHUNK], output[CHUNK], total = 0;
    for (jint offset = 0; offset < length; ) {
        jint n = length - offset; if (n > CHUNK) n = CHUNK;
        (*env)->GetLongArrayRegion(env, left, left_offset + offset, n, a);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetLongArrayRegion(env, right, right_offset + offset, n, b);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jint i = 0; i < n; i++) {
            if (a[i] < 0 || b[i] < 0) {
                fail(env, "java/lang/IllegalArgumentException", "negative count"); return NULL;
            }
            jlong value;
            switch (operation) {
                case 0:
                    if (a[i] > INT64_MAX - b[i]) {
                        fail(env, "java/lang/ArithmeticException", "count overflow"); return NULL;
                    }
                    value = a[i] + b[i]; break;
                case 1: value = a[i] > b[i] ? a[i] : b[i]; break;
                case 2: value = a[i] < b[i] ? a[i] : b[i]; break;
                default: value = a[i] > b[i] ? a[i] - b[i] : 0; break;
            }
            if (value > INT64_MAX - total) {
                fail(env, "java/lang/ArithmeticException", "total overflow"); return NULL;
            }
            total += value; output[i] = value;
        }
        (*env)->SetLongArrayRegion(env, result, offset, n, output);
        if ((*env)->ExceptionCheck(env)) return NULL;
        offset += n;
    }
    return result;
}
