/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 */
/* Test-only deliberately faulty JNI mask backend; never ship in a runtime library. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static jlong scan(JNIEnv *env, jintArray rows, jbyteArray states, jint first, jint end,
                  jint required, jint excluded, jintArray output, jint offset, jint limit) {
    // Two special masks exercise a broken JNI kernel without modifying the ordinary ABI.
    if (required == 253 || required == 254) {
        jint garbage = 999999;
        (*env)->SetIntArrayRegion(env, output, offset, 1, &garbage);
        if (required == 253) {
            jclass exceptionType = (*env)->FindClass(env, "java/lang/IllegalStateException");
            if (exceptionType != NULL) (*env)->ThrowNew(env, exceptionType, "injected JNI failure after partial output");
            return 0;
        }
        return ((jlong) (end + 1) << 32) | 1;
    }
    jint *rowData = (*env)->GetIntArrayElements(env, rows, NULL);
    if (rowData == NULL) return 0;
    jbyte *stateData = (*env)->GetByteArrayElements(env, states, NULL);
    if (stateData == NULL) {(*env)->ReleaseIntArrayElements(env, rows, rowData, JNI_ABORT);return 0;}
    jint *tmp = limit ? (jint *) malloc((size_t)limit * sizeof(jint)) : NULL;
    if (limit && tmp == NULL) {(*env)->ReleaseByteArrayElements(env, states, stateData, JNI_ABORT); (*env)->ReleaseIntArrayElements(env, rows, rowData, JNI_ABORT);return 0;}
    jint copied = 0, next = first;
    while (next < end && copied < limit) {
        unsigned int bits = (unsigned int) (unsigned char) stateData[next];
        if ((bits & (unsigned int) required) == (unsigned int)required &&
            (bits & (unsigned int) excluded) == 0U) tmp[copied++] = rowData[next];
        next++;
    }
    if (copied) (*env)->SetIntArrayRegion(env, output, offset, copied, tmp);
    free(tmp);
    (*env)->ReleaseByteArrayElements(env, states, stateData, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, rows, rowData, JNI_ABORT);
    return ((jlong) next << 32) | ((jlong) (uint32_t)copied);
}
JNIEXPORT jlong JNICALL Java_com_m3_collections_M3IndexMaskWindow_scanNative(JNIEnv *env, jclass cls,
        jintArray rows, jbyteArray states, jint first, jint required, jint excluded,
        jintArray output, jint offset, jint limit) {
    (void) cls;
    return scan(env, rows, states, first, (*env)->GetArrayLength(env, rows), required, excluded, output, offset, limit);
}
JNIEXPORT jlong JNICALL Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(JNIEnv *env, jclass cls,
        jintArray rows, jbyteArray states, jint first, jint end, jint required, jint excluded,
        jintArray output, jint offset, jint limit) {
    (void) cls;
    return scan(env, rows, states, first, end, required, excluded, output, offset, limit);
}
