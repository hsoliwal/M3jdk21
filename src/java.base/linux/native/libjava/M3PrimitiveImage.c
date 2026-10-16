/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */

/* Linux fixed-address mode is strict and never uses destructive MAP_FIXED. */
#define _GNU_SOURCE
#include <jni.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdint.h>
#include <limits.h>
#include <errno.h>
#include <stdio.h>

#ifndef MAP_FIXED_NOREPLACE
#define MAP_FIXED_NOREPLACE 0x100000
#endif

static void mapping_error(JNIEnv *env, const char *operation, int error) {
    char message[160];
    snprintf(message, sizeof(message), "%s (errno=%d)", operation, error);
    jclass klass = (*env)->FindClass(env, "java/io/IOException");
    if (klass != NULL) (*env)->ThrowNew(env, klass, message);
}

JNIEXPORT jobject JNICALL
Java_jdk_internal_mindex_M3PrimitiveImage_fixedMap0(JNIEnv *env, jclass klass,
        jobject descriptor, jlong base, jlong length) {
    (void)klass;
    long page = sysconf(_SC_PAGESIZE);
    if (descriptor == NULL || sizeof(uintptr_t) < sizeof(jlong) || base <= 0 || page <= 0
            || ((uint64_t)base % (uint64_t)page) != 0 || length <= 0 || length > INT_MAX
            || (uint64_t)base > UINTPTR_MAX - (uint64_t)length) {
        mapping_error(env, "invalid fixed mapping dimensions", EINVAL);
        return NULL;
    }
    jclass fd_class = (*env)->GetObjectClass(env, descriptor);
    if (fd_class == NULL) return NULL;
    jfieldID field = (*env)->GetFieldID(env, fd_class, "fd", "I");
    if (field == NULL) return NULL;
    jint fd = (*env)->GetIntField(env, descriptor, field);
    struct stat status;
    if (fd < 0 || fstat(fd, &status) != 0) {
        mapping_error(env, "primitive image descriptor", fd < 0 ? EBADF : errno);
        return NULL;
    }
    if (!S_ISREG(status.st_mode) || status.st_size != length) {
        mapping_error(env, "primitive image inode/length", EINVAL);
        return NULL;
    }
    void *requested = (void *)(uintptr_t)(uint64_t)base;
    void *mapped = mmap(requested, (size_t)length, PROT_READ,
                        MAP_SHARED | MAP_FIXED_NOREPLACE, fd, 0);
    if (mapped == MAP_FAILED) {
        mapping_error(env, "fixed primitive arena unavailable", errno);
        return NULL;
    }
    // Older kernels can ignore NOREPLACE: never accept a different returned address.
    if (mapped != requested) {
        munmap(mapped, (size_t)length);
        mapping_error(env, "kernel did not honor exact primitive arena", ENOTSUP);
        return NULL;
    }
    jobject buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) munmap(mapped, (size_t)length);
    return buffer;
}

JNIEXPORT void JNICALL
Java_jdk_internal_mindex_M3PrimitiveImage_fixedUnmap0(JNIEnv *env, jclass klass, jobject buffer) {
    (void)klass;
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    jlong length = (*env)->GetDirectBufferCapacity(env, buffer);
    if (address == NULL || length <= 0 || munmap(address, (size_t)length) != 0) {
        jclass failure = (*env)->FindClass(env, "java/lang/IllegalStateException");
        if (failure != NULL) (*env)->ThrowNew(env, failure, "primitive arena unmap failed");
    }
}
