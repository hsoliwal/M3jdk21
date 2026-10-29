/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#ifndef _WIN32
#include <sys/types.h>
#include <sys/stat.h>
#endif

static jint m3_mkfifo(JNIEnv *env, jbyteArray input, jint mode) {
    if (input == NULL || (mode & ~0777) != 0) return EINVAL;
    jsize length = (*env)->GetArrayLength(env, input);
    if (length <= 0 || length > 65535) return EINVAL;
    char *path = (char *)malloc((size_t)length + 1);
    if (path == NULL) return ENOMEM;
    (*env)->GetByteArrayRegion(env, input, 0, length, (jbyte *)path);
    if ((*env)->ExceptionCheck(env)) { free(path); return EINVAL; }
    path[length] = '\0';
    if (memchr(path, '\0', (size_t)length) != NULL) { free(path); return EINVAL; }
#ifdef _WIN32
    jint result = ENOSYS;
#else
    jint result = mkfifo(path, (mode_t)mode) == 0 ? 0 : errno;
#endif
    free(path);
    return result;
}

JNIEXPORT jint JNICALL Java_com_synexia_unix4jm3_io_FileTools_mkfifo0
  (JNIEnv *env, jclass owner, jbyteArray path, jint mode) {
    (void)owner;
    return m3_mkfifo(env, path, mode);
}

JNIEXPORT jint JNICALL Java_jdk_m3_io_FileTools_mkfifo0
  (JNIEnv *env, jclass owner, jbyteArray path, jint mode) {
    (void)owner;
    return m3_mkfifo(env, path, mode);
}

/* JNI UTF-16 literal extension; sealed by the Synexia native recipe. */
/* The caller supplies a page of UTF-16 code units and an exact KMP prefix array.
 * -1 means a literal match. Nonnegative states carry across page boundaries. */
static jint m3_literal_feed(JNIEnv *env, jcharArray page, jint count,
                            jcharArray needle, jintArray prefix, jint state) {
    if (page == NULL || needle == NULL || prefix == NULL) {
        jclass type = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (type != NULL) (*env)->ThrowNew(env, type, "null native literal array");
        return -2;
    }
    jsize capacity = (*env)->GetArrayLength(env, page);
    jsize key_length = (*env)->GetArrayLength(env, needle);
    jsize prefix_length = (*env)->GetArrayLength(env, prefix);
    if (count < 0 || count > capacity || capacity > 8192 || key_length == 0 ||
        key_length != prefix_length || state < 0 || state >= key_length) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) (*env)->ThrowNew(env, type, "invalid native literal bounds");
        return -2;
    }
    jchar *text = (*env)->GetCharArrayElements(env, page, NULL);
    if (text == NULL) return -2; /* pending OutOfMemoryError */
    jchar *key = (*env)->GetCharArrayElements(env, needle, NULL);
    if (key == NULL) {
        (*env)->ReleaseCharArrayElements(env, page, text, JNI_ABORT);
        return -2;
    }
    jint *next = (*env)->GetIntArrayElements(env, prefix, NULL);
    if (next == NULL) {
        (*env)->ReleaseCharArrayElements(env, needle, key, JNI_ABORT);
        (*env)->ReleaseCharArrayElements(env, page, text, JNI_ABORT);
        return -2;
    }
    jint result = state;
    for (jint i = 0; i < count; i++) {
        while (result > 0 && text[i] != key[result]) {
            jint fallback = next[result - 1];
            if (fallback < 0 || fallback >= result) {
                result = -2; /* malformed private prefix table: fail closed */
                goto done;
            }
            result = fallback;
        }
        if (text[i] == key[result] && ++result == key_length) {
            result = -1;
            break;
        }
    }
done:
    (*env)->ReleaseIntArrayElements(env, prefix, next, JNI_ABORT);
    (*env)->ReleaseCharArrayElements(env, needle, key, JNI_ABORT);
    (*env)->ReleaseCharArrayElements(env, page, text, JNI_ABORT);
    if (result == -2 && !(*env)->ExceptionCheck(env)) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) (*env)->ThrowNew(env, type, "invalid KMP state table");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_com_synexia_unix4jm3_NativeUtf16Literal_abiVersion0
  (JNIEnv *env, jclass owner) { (void)env; (void)owner; return 1; }
JNIEXPORT jint JNICALL Java_jdk_m3_text_NativeUtf16Literal_abiVersion0
  (JNIEnv *env, jclass owner) { (void)env; (void)owner; return 1; }
JNIEXPORT jint JNICALL Java_com_synexia_unix4jm3_NativeUtf16Literal_feed0
  (JNIEnv *env, jclass owner, jcharArray page, jint count, jcharArray needle,
   jintArray prefix, jint state) {
    (void)owner;
    return m3_literal_feed(env, page, count, needle, prefix, state);
}
JNIEXPORT jint JNICALL Java_jdk_m3_text_NativeUtf16Literal_feed0
  (JNIEnv *env, jclass owner, jcharArray page, jint count, jcharArray needle,
   jintArray prefix, jint state) {
    (void)owner;
    return m3_literal_feed(env, page, count, needle, prefix, state);
}
