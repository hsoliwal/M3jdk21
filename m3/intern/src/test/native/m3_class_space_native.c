/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/native/m3_class_space_native.c blob 8a249bc796db; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
#include <jni.h>
#include <string.h>

/*
 * Bounded, read-only JNI oracle. JNI arrays are never retained; each region is an ephemeral
 * stack copy. Unlike GetPrimitiveArrayCritical, this does not pin Java heap arrays.
 */
JNIEXPORT jboolean JNICALL
Java_jdk_m3_intern_ClassSpaceNative_equalBytes(
        JNIEnv *env, jclass owner, jbyteArray left, jbyteArray right) {
    (void) owner;
    if ((*env)->IsSameObject(env, left, right)) {
        return JNI_TRUE;
    }
    if (left == NULL || right == NULL) {
        return JNI_FALSE;
    }
    const jsize length = (*env)->GetArrayLength(env, left);
    if (length != (*env)->GetArrayLength(env, right)) {
        return JNI_FALSE;
    }
    jbyte lhs[4096];
    jbyte rhs[4096];
    for (jsize offset = 0; offset < length;) {
        const jsize remaining = length - offset;
        const jsize count = remaining < (jsize) sizeof lhs ? remaining : (jsize) sizeof lhs;
        (*env)->GetByteArrayRegion(env, left, offset, count, lhs);
        if ((*env)->ExceptionCheck(env)) {
            return JNI_FALSE; /* leave the original JVM exception pending */
        }
        (*env)->GetByteArrayRegion(env, right, offset, count, rhs);
        if ((*env)->ExceptionCheck(env)) {
            return JNI_FALSE;
        }
        if (memcmp(lhs, rhs, (size_t) count) != 0) {
            return JNI_FALSE;
        }
        offset += count;
    }
    return JNI_TRUE;
}
