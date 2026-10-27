/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass kind = (*env)->FindClass(env, type);
    if (kind != NULL) {
        (void)(*env)->ThrowNew(env, kind, message);
        (*env)->DeleteLocalRef(env, kind);
    }
}

/* All traversal uses primitive scalar regions; no array pin or retained address.
 * Return -1 only when a Java exception is pending; valid roots are >= 0.
 */
static jint m3_root(JNIEnv *env, jintArray parents, jint size, jint index) {
    if (index < 0 || index >= size) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "query index outside logical size");
        return -1;
    }
    jint at = index;
    for (jint hops = 0; hops < size; ++hops) {
        jint parent;
        (*env)->GetIntArrayRegion(env, parents, at, 1, &parent);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (parent < 0) return at;
        if (parent >= size) {
            m3_throw(env, "java/lang/IllegalStateException", "invalid parent index");
            return -1;
        }
        at = parent;
    }
    m3_throw(env, "java/lang/IllegalStateException", "cyclic disjoint index");
    return -1;
}

JNIEXPORT jbooleanArray JNICALL Java_com_m3_collections_M3DisjointIndex_connectedNative(
        JNIEnv *env, jclass owner, jintArray parents, jint logicalSize,
        jintArray left, jintArray right) {
    (void)owner;
    if (parents == NULL || left == NULL || right == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null primitive JNI query lane");
        return NULL;
    }
    jsize capacity = (*env)->GetArrayLength(env, parents);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize nleft = (*env)->GetArrayLength(env, left);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize nright = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (logicalSize < 0 || logicalSize > capacity || nleft != nright) {
        m3_throw(env, "java/lang/IllegalArgumentException", "invalid JNI index/query geometry");
        return NULL;
    }
    jbooleanArray result = (*env)->NewBooleanArray(env, nleft);
    if (result == NULL) return NULL;
    for (jsize i = 0; i < nleft; ++i) {
        jint lhs, rhs;
        (*env)->GetIntArrayRegion(env, left, i, 1, &lhs);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, right, i, 1, &rhs);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jint rootL = m3_root(env, parents, logicalSize, lhs);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jint rootR = m3_root(env, parents, logicalSize, rhs);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jboolean same = rootL == rootR ? JNI_TRUE : JNI_FALSE;
        (*env)->SetBooleanArrayRegion(env, result, i, 1, &same);
        if ((*env)->ExceptionCheck(env)) return NULL;
    }
    return result;
}
