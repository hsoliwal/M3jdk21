/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * Independent JNI binding to Linux madvise; no PMDK or allocator implementation.
 */
#define _DEFAULT_SOURCE
#include <jni.h>
#include <errno.h>
#include <stdint.h>
#include <sys/mman.h>
#include <unistd.h>
#include "com_m3_optane_NativeSupport.h"

static jlong bad_argument(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) (*env)->ThrowNew(env, type, message);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_m3_optane_NativeSupport_abiVersion0
  (JNIEnv *env, jclass type) {
    (void)env; (void)type;
    return 1;
}

JNIEXPORT jlong JNICALL Java_com_m3_optane_NativeSupport_advise0
  (JNIEnv *env, jclass type, jobject buffer, jint offset, jint length, jint advice) {
    (void)type;
    if (buffer == NULL || offset < 0 || length < 0 || length > 64 * 1024 * 1024)
        return bad_argument(env, "invalid bounded mapping range");
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)offset > capacity - (jlong)length)
        return bad_argument(env, "direct-buffer capacity exceeded");
    jclass cls = (*env)->GetObjectClass(env, buffer);
    if (cls == NULL) return 0;
    jmethodID limit_method = (*env)->GetMethodID(env, cls, "limit", "()I");
    if (limit_method == NULL) return 0;
    jint limit = (*env)->CallIntMethod(env, buffer, limit_method);
    if ((*env)->ExceptionCheck(env)) return 0;
    jmethodID read_only = (*env)->GetMethodID(env, cls, "isReadOnly", "()Z");
    if (read_only == NULL) return 0;
    jboolean ro = (*env)->CallBooleanMethod(env, buffer, read_only);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (ro != JNI_TRUE || (jlong)offset + length > limit)
        return bad_argument(env, "read-only buffer/limit contract violated");
    int flag;
    switch (advice) {
        case 0: flag = MADV_NORMAL; break;
        case 1: flag = MADV_SEQUENTIAL; break;
        case 2: flag = MADV_RANDOM; break;
        case 3: flag = MADV_WILLNEED; break;
        default: return bad_argument(env, "unrecognized advice");
    }
    if (length == 0) return 0;
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    if (address == NULL) return bad_argument(env, "missing direct address");
    uintptr_t base = (uintptr_t)address;
    if (base > UINTPTR_MAX - (uintptr_t)offset)
        return bad_argument(env, "address overflow");
    uintptr_t start = base + (uintptr_t)offset;
    if (start > UINTPTR_MAX - (uintptr_t)length)
        return bad_argument(env, "range overflow");
    uintptr_t end = start + (uintptr_t)length;
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0) return -(jlong)EINVAL;
    uintptr_t page = (uintptr_t)page_size;
    uintptr_t remainder = start % page;
    if (remainder != 0) {
        uintptr_t skip = page - remainder;
        if (skip >= end - start) return 0;
        start += skip;
    }
    end -= end % page;
    if (end <= start) return 0;
    size_t count = (size_t)(end - start);
    if (madvise((void *)start, count, flag) != 0)
        return -(jlong)(errno == 0 ? EIO : errno);
    return (jlong)count;
}
