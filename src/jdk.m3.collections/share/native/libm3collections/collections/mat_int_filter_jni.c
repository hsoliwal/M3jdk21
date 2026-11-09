/* SPDX-License-Identifier: Apache-2.0 */
/* Lineage: com.synexia 537b10a9de1f synexia-mat-collections/src/main/native/mat_int_filter_jni.c blob 5bb50efc6ed0 (com.synexia.matcollections.MatIntFilterKernel -> jdk.internal.m3.collections.mat.MatIntFilterKernel, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>

/* Java: stable signed-int selection; no heap object handles or unsafe GC pointers cross JNI. */
JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_mat_MatIntFilterKernel_selectGreaterOrEqualNative(
        JNIEnv *env, jclass owner, jintArray values, jint threshold, jintArray result) {
    (void) owner;
    if (values == NULL || result == NULL) {
        jclass klass = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (klass != NULL) (*env)->ThrowNew(env, klass, "input or result array is null");
        return -1;
    }
    jsize count = (*env)->GetArrayLength(env, values);
    if ((*env)->GetArrayLength(env, result) < count) {
        jclass klass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (klass != NULL) (*env)->ThrowNew(env, klass, "result array shorter than input");
        return -1;
    }
    jint *input = (*env)->GetIntArrayElements(env, values, NULL);
    if (input == NULL) return -1; /* JNI has a pending allocation exception. */
    jint *positions = (*env)->GetIntArrayElements(env, result, NULL);
    if (positions == NULL) {
        (*env)->ReleaseIntArrayElements(env, values, input, JNI_ABORT);
        return -1;
    }
    jint selected = 0;
    for (jsize index = 0; index < count; index++) {
        if (input[index] >= threshold) positions[selected++] = (jint) index;
    }
    (*env)->ReleaseIntArrayElements(env, result, positions, 0);
    (*env)->ReleaseIntArrayElements(env, values, input, JNI_ABORT);
    return selected;
}
