/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>

static void raise(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) { (*env)->ThrowNew(env, klass, message); }
}

/* JNI arrays are copied into private native buffers. No Java heap address or pin survives. */
JNIEXPORT void JNICALL Java_com_m3_collections_M3SignalOrderNative_sort0(
        JNIEnv *env, jclass owner, jlongArray javaKeys, jintArray javaRows) {
    (void) owner;
    if (javaKeys == NULL || javaRows == NULL) {
        raise(env, "java/lang/NullPointerException", "key/row array is null"); return;
    }
    jsize n = (*env)->GetArrayLength(env, javaKeys);
    if ((*env)->GetArrayLength(env, javaRows) != n) {
        raise(env, "java/lang/IllegalArgumentException", "key/row length mismatch"); return;
    }
    if (n < 2) return;
    size_t count = (size_t) n;
    if (count > SIZE_MAX / sizeof(jlong) || count > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "radix workspace overflow"); return;
    }
    jlong *keys = (jlong *) malloc(count * sizeof(jlong));
    jlong *nextKeys = (jlong *) malloc(count * sizeof(jlong));
    jint *rows = (jint *) malloc(count * sizeof(jint));
    jint *nextRows = (jint *) malloc(count * sizeof(jint));
    if (keys == NULL || nextKeys == NULL || rows == NULL || nextRows == NULL) {
        free(keys); free(nextKeys); free(rows); free(nextRows);
        raise(env, "java/lang/OutOfMemoryError", "radix workspace unavailable"); return;
    }
    (*env)->GetLongArrayRegion(env, javaKeys, 0, n, keys);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    (*env)->GetIntArrayRegion(env, javaRows, 0, n, rows);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    for (unsigned int pass = 0; pass < 8U; pass++) {
        size_t positions[256] = {0};
        unsigned int shift = pass * 8U;
        for (size_t i = 0; i < count; i++) {
            unsigned int digit = (unsigned int) (((uint64_t) keys[i] >> shift) & UINT64_C(255));
            positions[digit]++;
        }
        size_t start = 0;
        for (size_t digit = 0; digit < 256U; digit++) {
            size_t occur = positions[digit];
            positions[digit] = start;
            start += occur;
        }
        for (size_t i = 0; i < count; i++) {
            unsigned int digit = (unsigned int) (((uint64_t) keys[i] >> shift) & UINT64_C(255));
            size_t target = positions[digit]++;
            nextKeys[target] = keys[i]; nextRows[target] = rows[i];
        }
        jlong *k = keys; keys = nextKeys; nextKeys = k;
        jint *r = rows; rows = nextRows; nextRows = r;
    }
    (*env)->SetLongArrayRegion(env, javaKeys, 0, n, keys);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    (*env)->SetIntArrayRegion(env, javaRows, 0, n, rows);
cleanup:
    free(keys); free(nextKeys); free(rows); free(nextRows);
}
