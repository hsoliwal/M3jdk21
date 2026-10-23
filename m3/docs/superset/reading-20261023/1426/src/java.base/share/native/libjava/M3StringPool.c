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
#if defined(__linux__)
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#elif defined(_WIN32)
#include <windows.h>
#endif

static void mapping_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/io/IOException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
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
#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
    struct stat info;
    long page_size;
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
    mapped = mmap((void *)(uintptr_t)address, (size_t)length,
            PROT_READ, MAP_SHARED | MAP_FIXED_NOREPLACE, descriptor, 0);
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
#if defined(__linux__)
    return munmap((void *)(uintptr_t)address, (size_t)length) == 0 ? JNI_TRUE : JNI_FALSE;
#elif defined(_WIN32)
    return UnmapViewOfFile((void *)(uintptr_t)address) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca

/*
 * Fixed-array JNI concatenation of canonical owner/range views.
 * No joined byte[]/char[] payload and no persistent native pointers.
 * Incoming M3String[] keeps the OS-mapped and local owners reachable.
 */
// FLLDCIM: ATOM — native fixed-stack concatenation; M3StringPool owns canonical merging.
#define M3_CONCAT_LEVELS 32

static void m3_concat_throw(JNIEnv *env, const char *class_name, const char *message) {
    jclass error_class = (*env)->FindClass(env, class_name);
    if (error_class != NULL) {
        (void)(*env)->ThrowNew(env, error_class, message);
        (*env)->DeleteLocalRef(env, error_class);
    }
}

JNIEXPORT jobject JNICALL
Java_java_lang_M3StringPool_concatViewsNative0(JNIEnv *env, jclass pool, jobjectArray parts) {
    jobject levels[M3_CONCAT_LEVELS] = {NULL};
    jobject result = NULL;
    jclass string_class = NULL;
    jfieldID coordinate_field = NULL;
    jmethodID concat_method = NULL;
    jmethodID empty_method = NULL;
    jlong accumulated = 0;

    if (parts == NULL) {
        m3_concat_throw(env, "java/lang/NullPointerException", "M3String parts");
        return NULL;
    }
    string_class = (*env)->FindClass(env, "java/lang/M3String");
    if (string_class == NULL) goto cleanup;
    coordinate_field = (*env)->GetFieldID(env, string_class, "value", "J");
    if (coordinate_field == NULL) goto cleanup;
    concat_method = (*env)->GetStaticMethodID(env, pool, "concat",
            "(Ljava/lang/M3String;Ljava/lang/M3String;)Ljava/lang/M3String;");
    if (concat_method == NULL) goto cleanup;
    empty_method = (*env)->GetStaticMethodID(env, string_class, "empty", "()Ljava/lang/M3String;");
    if (empty_method == NULL) goto cleanup;

    const jsize count = (*env)->GetArrayLength(env, parts);
    for (jsize index = 0; index < count; index++) {
        jobject part = (*env)->GetObjectArrayElement(env, parts, index);
        if (part == NULL) {
            if (!(*env)->ExceptionCheck(env)) {
                m3_concat_throw(env, "java/lang/NullPointerException", "null interned M3String part");
            }
            goto cleanup;
        }
        const jlong coordinate = (*env)->GetLongField(env, part, coordinate_field);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, part);
            goto cleanup;
        }
        const uint64_t units = (uint64_t) coordinate & UINT64_C(0xffffffff);
        if (units > (uint64_t) INT_MAX || units > (uint64_t) (INT_MAX - accumulated)) {
            (*env)->DeleteLocalRef(env, part);
            m3_concat_throw(env, "java/lang/OutOfMemoryError", "Required length exceeds implementation limit");
            goto cleanup;
        }
        accumulated += (jlong) units;
        if (units == 0) {
            (*env)->DeleteLocalRef(env, part);
            continue;
        }

        int level;
        for (level = 0; level < M3_CONCAT_LEVELS; level++) {
            if (levels[level] == NULL) {
                levels[level] = part;
                part = NULL;
                break;
            }
            jobject merged = (*env)->CallStaticObjectMethod(env, pool, concat_method,
                    levels[level], part);
            (*env)->DeleteLocalRef(env, levels[level]);
            levels[level] = NULL;
            (*env)->DeleteLocalRef(env, part);
            part = merged;
            if ((*env)->ExceptionCheck(env) || part == NULL) {
                if (part != NULL) (*env)->DeleteLocalRef(env, part);
                goto cleanup;
            }
        }
        if (part != NULL) {
            (*env)->DeleteLocalRef(env, part);
            m3_concat_throw(env, "java/lang/OutOfMemoryError", "Too many M3 concatenation parts");
            goto cleanup;
        }
    }

    /* Higher occupied levels hold earlier pieces. Fold low-to-high for source order. */
    for (int level = 0; level < M3_CONCAT_LEVELS; level++) {
        if (levels[level] == NULL) continue;
        if (result == NULL) {
            result = levels[level];
            levels[level] = NULL;
        } else {
            jobject merged = (*env)->CallStaticObjectMethod(env, pool, concat_method,
                    levels[level], result);
            (*env)->DeleteLocalRef(env, levels[level]);
            levels[level] = NULL;
            (*env)->DeleteLocalRef(env, result);
            result = merged;
            if ((*env)->ExceptionCheck(env) || result == NULL) goto cleanup;
        }
    }
    if (result == NULL) result = (*env)->CallStaticObjectMethod(env, string_class, empty_method);

cleanup:
    for (int level = 0; level < M3_CONCAT_LEVELS; level++) {
        if (levels[level] != NULL) (*env)->DeleteLocalRef(env, levels[level]);
    }
    if (string_class != NULL) (*env)->DeleteLocalRef(env, string_class);
    if ((*env)->ExceptionCheck(env)) {
        if (result != NULL) (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}
=======
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
