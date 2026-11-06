/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

#define M3_INTERVAL_MAX_PAIRS 1000000

/* Count with out==NULL, or emit normalized pair endpoints; -1 means bound exceeded. */
static jsize m3_intersect_half_open(const jint *a, jsize na, const jint *b, jsize nb,
                                   jint *out) {
    jsize i = 0, j = 0, used = 0;
    while (i < na && j < nb) {
        const jint start = a[i] > b[j] ? a[i] : b[j];
        const jint end = a[i + 1] < b[j + 1] ? a[i + 1] : b[j + 1];
        if (start < end) {
            if (used >= 2 * M3_INTERVAL_MAX_PAIRS) return -1;
            if (out != NULL) { out[used] = start; out[used + 1] = end; }
            used += 2;
        }
        if (a[i + 1] < b[j + 1]) i += 2;
        else if (a[i + 1] > b[j + 1]) j += 2;
        else { i += 2; j += 2; }
    }
    return used;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_tools_cpu_M3IntIntervalAlgebraNative_intersect0(
        JNIEnv *env, jclass owner, jintArray left, jintArray right) {
    (void)owner;
    if (left == NULL || right == NULL) return NULL;
    const jsize na = (*env)->GetArrayLength(env, left);
    const jsize nb = (*env)->GetArrayLength(env, right);
    if (na == 0 || nb == 0) return (*env)->NewIntArray(env, 0);

    jint *a = (*env)->GetIntArrayElements(env, left, NULL);
    if (a == NULL) return NULL;
    jint *b = (*env)->GetIntArrayElements(env, right, NULL);
    if (b == NULL) {
        (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
        return NULL;
    }
    const jsize count = m3_intersect_half_open(a, na, b, nb, NULL);
    jintArray result = NULL;
    if (count < 0) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) (*env)->ThrowNew(env, type, "interval result too large");
    } else {
        result = (*env)->NewIntArray(env, count);
        if (result != NULL && count != 0) {
            jint *output = (*env)->GetIntArrayElements(env, result, NULL);
            if (output == NULL) result = NULL; /* preserve pending allocation failure */
            else {
                const jsize written = m3_intersect_half_open(a, na, b, nb, output);
                (*env)->ReleaseIntArrayElements(env, result, output, 0);
                if (written != count) {
                    result = NULL;
                    jclass err = (*env)->FindClass(env, "java/lang/IllegalStateException");
                    if (err != NULL) (*env)->ThrowNew(env, err, "intersection count mismatch");
                }
            }
        }
    }
    (*env)->ReleaseIntArrayElements(env, right, b, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
    return result;
}
