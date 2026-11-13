/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>

/*
 * JNI array elements are released in every path with JNI_ABORT; no heap pointer
 * escapes. Input lanes were validated and frozen by M3SortedIntMultiset.
 * Equal values consume one occurrence from each lane, retaining min multiplicity.
 */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_tools_cpu_M3SortedIntMultisetNative_intersection0(
        JNIEnv *env, jclass owner, jintArray left, jintArray right) {
    (void)owner;
    if (left == NULL || right == NULL) return NULL;
    jsize n = (*env)->GetArrayLength(env, left);
    jsize m = (*env)->GetArrayLength(env, right);
    if (n == 0 || m == 0) return (*env)->NewIntArray(env, 0);

    jint *a = (*env)->GetIntArrayElements(env, left, NULL);
    if (a == NULL) return NULL;
    jint *b = (*env)->GetIntArrayElements(env, right, NULL);
    if (b == NULL) {
        (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
        return NULL;
    }

    jsize i = 0;
    jsize j = 0;
    jsize matched = 0;
    while (i < n && j < m) {
        if (a[i] < b[j]) i++;
        else if (a[i] > b[j]) j++;
        else { matched++; i++; j++; }
    }

    jintArray output = (*env)->NewIntArray(env, matched);
    if (output != NULL && matched != 0) {
        jint *dst = (*env)->GetIntArrayElements(env, output, NULL);
        if (dst == NULL) output = NULL;
        else {
            i = 0;
            j = 0;
            jsize at = 0;
            while (i < n && j < m) {
                if (a[i] < b[j]) i++;
                else if (a[i] > b[j]) j++;
                else { dst[at++] = a[i]; i++; j++; }
            }
            (*env)->ReleaseIntArrayElements(env, output, dst, 0);
        }
    }
    (*env)->ReleaseIntArrayElements(env, right, b, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
    return output;
}
