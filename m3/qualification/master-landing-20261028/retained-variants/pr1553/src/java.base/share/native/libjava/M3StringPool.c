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
#include <stdint.h>
#include <limits.h>
#include "java_lang_M3StringPool.h"
#include "m3_jni_util.h"
#if defined(__linux__) || defined(__APPLE__)
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#elif defined(_WIN32)
#include <windows.h>
#endif

static void mapping_error(JNIEnv *env, const char *message) {
    m3_jni_throw_by_name(env, "java/io/IOException", message);
}

static int valid_range(jlong address, jlong length) {
    if (address <= 0 || length <= 0 || length > INT_MAX
            || ((uint64_t)address & UINT64_C(65535)) != 0
            || (uint64_t)address > (uint64_t)UINTPTR_MAX
            || (uint64_t)length > (uint64_t)SIZE_MAX) return 0;
    return (uint64_t)(length - 1) <= (uint64_t)UINTPTR_MAX - (uint64_t)address;
}

JNIEXPORT jobject JNICALL
Java_java_lang_M3StringPool_mapLexicon0(JNIEnv *env, jclass ignored,
        jint descriptor, jlong handle, jlong address, jlong length) {
    void *mapped;
    jobject buffer;
    (void)ignored;
    if (!valid_range(address, length)) {
        mapping_error(env, "Invalid fixed M3 lexicon address or size");
        return NULL;
    }
#if defined(__linux__) || defined(__APPLE__)
    struct stat info;
    long page_size;
    int mapping_flags = MAP_SHARED;
    (void)handle;
    if (descriptor < 0 || fstat(descriptor, &info) != 0
            || !S_ISREG(info.st_mode) || info.st_size != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon descriptor");
        return NULL;
    }
    page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0 || (uint64_t)address % (uint64_t)page_size != 0) {
        mapping_error(env, "Unaligned fixed M3 lexicon address");
        return NULL;
    }
#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
    mapping_flags |= MAP_FIXED_NOREPLACE;
#endif
    /* A non-fixed address hint never replaces an occupied mapping. Accept only
     * the exact requested address below; release any alternate mapping. This
     * preserves the existing no-replace path on Linux and supports Apple or
     * Linux headers without MAP_FIXED_NOREPLACE through the same contract.
     */
    mapped = mmap((void *)(uintptr_t)address, (size_t)length,
            PROT_READ, mapping_flags, descriptor, 0);
    if (mapped == MAP_FAILED) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)munmap(mapped, (size_t)length);
        mapping_error(env, "Kernel did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)munmap(mapped, (size_t)length);
    return buffer;
#elif defined(_WIN32)
    HANDLE file = (HANDLE)(uintptr_t)handle;
    HANDLE section;
    LARGE_INTEGER size;
    (void)descriptor;
    if (file == NULL || file == INVALID_HANDLE_VALUE || GetFileType(file) != FILE_TYPE_DISK
            || !GetFileSizeEx(file, &size) || size.QuadPart != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon handle");
        return NULL;
    }
    section = CreateFileMappingW(file, NULL, PAGE_READONLY, 0, 0, NULL);
    if (section == NULL) {
        mapping_error(env, "Cannot create fixed M3 lexicon file mapping");
        return NULL;
    }
    mapped = MapViewOfFileEx(section, FILE_MAP_READ, 0, 0, (SIZE_T)length,
            (LPVOID)(uintptr_t)address);
    (void)CloseHandle(section);
    if (mapped == NULL) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)UnmapViewOfFile(mapped);
        mapping_error(env, "System did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)UnmapViewOfFile(mapped);
    return buffer;
#else
    (void)descriptor;
    (void)handle;
    (void)mapped;
    (void)buffer;
    mapping_error(env, "Fixed M3 lexicon mapping is unsupported on this platform");
    return NULL;
#endif
}

JNIEXPORT jboolean JNICALL
Java_java_lang_M3StringPool_unmapLexicon0(JNIEnv *env, jclass ignored,
        jlong address, jlong length) {
    (void)env;
    (void)ignored;
    if (!valid_range(address, length)) return JNI_FALSE;
#if defined(__linux__) || defined(__APPLE__)
    return munmap((void *)(uintptr_t)address, (size_t)length) == 0 ? JNI_TRUE : JNI_FALSE;
#elif defined(_WIN32)
    return UnmapViewOfFile((void *)(uintptr_t)address) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}
