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
