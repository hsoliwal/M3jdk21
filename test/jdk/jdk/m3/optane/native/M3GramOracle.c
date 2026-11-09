/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-index-20261102/proof/m3gramoracle.c blob ed55dfd8f21a; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
#include <jni.h>
#include <stddef.h>
#include <string.h>

/* Independent brute-force byte oracle, intentionally unlike production KMP. */
JNIEXPORT jlong JNICALL Java_jdk_m3_optane_NativeByteOracle_firstHit(
        JNIEnv *env, jclass type, jbyteArray haystack, jbyteArray pattern) {
    (void) type;
    if (haystack == NULL || pattern == NULL) {
        jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (npe != NULL) (*env)->ThrowNew(env, npe, "null native search array");
        return (jlong) -1;
    }
    const jsize n = (*env)->GetArrayLength(env, haystack);
    const jsize m = (*env)->GetArrayLength(env, pattern);
    if (m == 0) return 0;
    if (m > n) return (jlong) -1;
    jbyte *source = (*env)->GetByteArrayElements(env, haystack, NULL);
    if (source == NULL) return (jlong) -1;
    jbyte *needle = (*env)->GetByteArrayElements(env, pattern, NULL);
    if (needle == NULL) {
        (*env)->ReleaseByteArrayElements(env, haystack, source, JNI_ABORT);
        return (jlong) -1;
    }
    jlong found = -1;
    const size_t end = (size_t) n - (size_t) m;
    for (size_t i = 0; i <= end; ++i) {
        if (memcmp(source + i, needle, (size_t) m) == 0) {
            found = (jlong) i;
            break;
        }
    }
    (*env)->ReleaseByteArrayElements(env, pattern, needle, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, haystack, source, JNI_ABORT);
    return found;
}
