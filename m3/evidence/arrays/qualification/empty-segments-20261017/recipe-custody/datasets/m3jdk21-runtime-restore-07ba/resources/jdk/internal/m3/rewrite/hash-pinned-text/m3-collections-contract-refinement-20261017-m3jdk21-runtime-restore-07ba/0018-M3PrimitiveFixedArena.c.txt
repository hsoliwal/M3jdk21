/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdint.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

static void io_error(JNIEnv *env, const char *message) {
    jclass exception = (*env)->FindClass(env, "java/io/IOException");
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
    }
}

JNIEXPORT jlong JNICALL
Java_jdk_internal_mindex_M3PrimitiveFixedArena_mapExact0(
        JNIEnv *env, jclass clazz, jstring path, jlong preferred, jlong length) {
    (void)clazz;
    if (path == NULL || preferred <= 0 || length <= 0 ||
            (uint64_t)length > (uint64_t)SIZE_MAX ||
            (uint64_t)preferred > (uint64_t)UINTPTR_MAX) {
        io_error(env, "invalid M3 fixed mapping arguments");
        return 0;
    }
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0 || ((uint64_t)preferred % (uint64_t)page_size) != 0) {
        io_error(env, "unaligned M3 fixed mapping address");
        return 0;
    }
#ifndef MAP_FIXED_NOREPLACE
    return 0; /* Never fall back to MAP_FIXED, which destroys existing mappings. */
#else
    const char *name = (*env)->GetStringUTFChars(env, path, NULL);
    if (name == NULL) return 0;
    int fd = open(name, O_RDONLY | O_CLOEXEC | O_NOFOLLOW);
    (*env)->ReleaseStringUTFChars(env, path, name);
    if (fd < 0) {
        io_error(env, "could not open M3 primitive image");
        return 0;
    }
    struct stat info;
    if (fstat(fd, &info) != 0 || !S_ISREG(info.st_mode) || info.st_size != length) {
        close(fd);
        io_error(env, "invalid/changed M3 primitive image file");
        return 0;
    }
    void *mapped = mmap((void *)(uintptr_t)preferred, (size_t)length,
            PROT_READ, MAP_SHARED | MAP_FIXED_NOREPLACE, fd, 0);
    int code = errno;
    close(fd);
    if (mapped == MAP_FAILED) {
        if (code == EEXIST || code == ENOMEM || code == EINVAL) return 0;
        io_error(env, "M3 fixed-address mmap failed");
        return 0;
    }
    if ((uintptr_t)mapped != (uintptr_t)preferred) {
        munmap(mapped, (size_t)length); /* Old kernels may ignore unknown flag. */
        return 0;
    }
    return (jlong)(uintptr_t)mapped;
#endif
}

JNIEXPORT jboolean JNICALL
Java_jdk_internal_mindex_M3PrimitiveFixedArena_unmap0(
        JNIEnv *env, jclass clazz, jlong address, jlong length) {
    (void)env;
    (void)clazz;
    if (address <= 0 || length <= 0 || (uint64_t)length > (uint64_t)SIZE_MAX) {
        return JNI_FALSE;
    }
    return munmap((void *)(uintptr_t)address, (size_t)length) == 0
            ? JNI_TRUE : JNI_FALSE;
}
