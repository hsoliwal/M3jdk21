// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * Index-only specialization of Synexia DenseArrayOrder's stable merge atom.
 * No Java callbacks, pinned arrays or retained native pointers.
 */
#include <jni.h>
#include <stdlib.h>
#include <stdint.h>
#include "com_m3_collections_M3SortedView.h"

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) { (*env)->ThrowNew(env, cls, message); (*env)->DeleteLocalRef(env, cls); }
}
JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3SortedView_nativeOrder(
        JNIEnv *env, jclass cls, jlongArray input) {
    (void)cls;
    if (input == NULL) { fail(env, "java/lang/NullPointerException", "keys"); return NULL; }
    jsize n = (*env)->GetArrayLength(env, input);
    if (n > 1048576) { fail(env, "java/lang/IllegalArgumentException", "native row budget"); return NULL; }
    jintArray result = (*env)->NewIntArray(env, n);
    if (result == NULL || n == 0) return result;
    jlong *keys = malloc((size_t)n * sizeof *keys);
    jint *a = malloc((size_t)n * sizeof *a);
    jint *b = NULL;
    if (keys == NULL || a == NULL) {
        free(keys); free(a); free(b);
        fail(env, "java/lang/OutOfMemoryError", "native index scratch"); return NULL;
    }
    for (jsize start = 0; start < n;) {
        jsize length = n - start < 4096 ? n - start : 4096;
        (*env)->GetLongArrayRegion(env, input, start, length, keys + start);
        if ((*env)->ExceptionCheck(env)) { free(keys); free(a); free(b); return NULL; }
        start += length;
    }
    int ordered = 1;
    for (jint i = 0; i < n; i++) {
        a[i] = i;
        if (ordered && i > 0 && keys[i - 1] > keys[i]) ordered = 0;
    }
    if (!ordered) {
        b = malloc((size_t)n * sizeof *b);
        if (b == NULL) {
            free(keys); free(a);
            fail(env, "java/lang/OutOfMemoryError", "native index scratch"); return NULL;
        }
    }
    /* An already ordered capture needs only identity indices, not merge passes. */
    for (jint width = 1; !ordered && width < n; width = width >= n - width ? n : width * 2) {
        for (jint start = 0; start < n;) {
            jint middle = n - start < width ? n : start + width;
            jint end = n - middle < width ? n : middle + width;
            jint left = start, right = middle;
            for (jint out = start; out < end; out++) {
                if (left < middle && (right == end || keys[a[left]] <= keys[a[right]])) b[out] = a[left++];
                else b[out] = a[right++];
            }
            start = end;
        }
        jint *swap = a; a = b; b = swap;
    }
    for (jsize start = 0; start < n;) {
        jsize length = n - start < 4096 ? n - start : 4096;
        (*env)->SetIntArrayRegion(env, result, start, length, a + start);
        if ((*env)->ExceptionCheck(env)) { free(keys); free(a); free(b); return NULL; }
        start += length;
    }
    free(keys); free(a); free(b); return result;
}
