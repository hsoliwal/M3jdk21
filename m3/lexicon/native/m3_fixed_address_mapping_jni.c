/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Fail-closed fixed-address, read-only canonical image mapping for consenting processes.
 */
#define _GNU_SOURCE
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <limits.h>
#include <stdlib.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <unistd.h>
#endif

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) (*env)->ThrowNew(env, klass, message);
}

#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
/* Admission-only filename projection through the JDK's filesystem charset owner.
 * JNI GetStringUTFChars uses modified UTF-8 and cannot name supplementary paths.
 * No returned JNI array or native filename buffer survives this native call.
 */
static char *platform_path(JNIEnv *env, jstring path) {
    jclass utility = (*env)->FindClass(env, "sun/nio/fs/Util");
    if (utility == NULL) return NULL;
    jmethodID encode = (*env)->GetStaticMethodID(env, utility, "toBytes", "(Ljava/lang/String;)[B");
    if (encode == NULL) {
        (*env)->DeleteLocalRef(env, utility);
        return NULL;
    }
    jbyteArray bytes = (jbyteArray)(*env)->CallStaticObjectMethod(env, utility, encode, path);
    jboolean failed = (*env)->ExceptionCheck(env);
    (*env)->DeleteLocalRef(env, utility);
    if (failed) {
        if (bytes != NULL) (*env)->DeleteLocalRef(env, bytes);
        return NULL;
    }
    if (bytes == NULL) {
        fail(env, "java/lang/IllegalStateException", "JDK filesystem encoder returned no path");
        return NULL;
    }
    jsize count = (*env)->GetArrayLength(env, bytes);
    char *filename = (char *)malloc((size_t)count + 1u);
    if (filename == NULL) {
        (*env)->DeleteLocalRef(env, bytes);
        fail(env, "java/lang/OutOfMemoryError", "canonical image file name");
        return NULL;
    }
    (*env)->GetByteArrayRegion(env, bytes, 0, count, (jbyte *)filename);
    failed = (*env)->ExceptionCheck(env);
    (*env)->DeleteLocalRef(env, bytes);
    if (failed) {
        free(filename);
        return NULL;
    }
    if (memchr(filename, '\0', (size_t)count) != NULL) {
        free(filename);
        fail(env, "java/io/IOException", "canonical image path contains NUL");
        return NULL;
    }
    filename[count] = '\0';
    return filename;
}
#endif

static int valid_range(jlong base, jlong length) {
    return base > 0 && length > 0 && length <= INT_MAX
            && (uint64_t)base % UINT64_C(65536) == 0
            && (uint64_t)base <= (uint64_t)INT64_MAX - (uint64_t)length;
}

JNIEXPORT jobject JNICALL
Java_com_m3_text_M3FixedAddressMapping_mapExact0(JNIEnv *env, jclass klass,
        jstring path, jlong base, jlong length) {
    (void)klass;
    if (path == NULL || !valid_range(base, length)) {
        fail(env, "java/lang/IllegalArgumentException", "invalid exact-address mapping arguments");
        return NULL;
    }
#ifdef __linux__
#ifndef MAP_FIXED_NOREPLACE
    fail(env, "java/lang/UnsupportedOperationException", "MAP_FIXED_NOREPLACE unavailable");
    return NULL;
#else
    long page = sysconf(_SC_PAGESIZE);
    if (page <= 0 || (uint64_t)base % (uint64_t)page != 0) {
        fail(env, "java/lang/IllegalArgumentException", "non-aligned mapping address");
        return NULL;
    }
    char *filename = platform_path(env, path);
    if (filename == NULL) return NULL;
    int fd = open(filename, O_RDONLY | O_CLOEXEC | O_NOFOLLOW);
    free(filename);
    if (fd < 0) {
        fail(env, "java/io/IOException", "cannot open canonical image read-only");
        return NULL;
    }
    struct stat st;
    if (fstat(fd, &st) != 0 || !S_ISREG(st.st_mode) || st.st_size != length) {
        close(fd);
        fail(env, "java/io/IOException", "canonical image identity/size changed");
        return NULL;
    }
    void *requested = (void *)(uintptr_t)(uint64_t)base;
    void *mapped = mmap(requested, (size_t)length, PROT_READ,
            MAP_SHARED | MAP_FIXED_NOREPLACE, fd, 0);
    int saved_errno = errno;
    close(fd);
    if (mapped == MAP_FAILED) {
        (void)saved_errno;
        fail(env, "java/io/IOException", "exact shared address unavailable (no relocation)");
        return NULL;
    }
    if (mapped != requested) {
        munmap(mapped, (size_t)length);
        fail(env, "java/io/IOException", "kernel did not honor exact-address contract");
        return NULL;
    }
    jobject buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) munmap(mapped, (size_t)length);
    return buffer;
#endif
#elif defined(_WIN32)
    SYSTEM_INFO info;
    GetSystemInfo(&info);
    if (info.dwAllocationGranularity == 0
            || (uint64_t)base % (uint64_t)info.dwAllocationGranularity != 0) {
        fail(env, "java/lang/IllegalArgumentException", "Windows mapping base alignment");
        return NULL;
    }
    jsize count = (*env)->GetStringLength(env, path);
    const jchar *chars = (*env)->GetStringChars(env, path, NULL);
    if (chars == NULL) return NULL;
    wchar_t *name = (wchar_t *)HeapAlloc(GetProcessHeap(), 0,
            ((size_t)count + 1u) * sizeof(wchar_t));
    if (name == NULL) {
        (*env)->ReleaseStringChars(env, path, chars);
        fail(env, "java/lang/OutOfMemoryError", "canonical image file name");
        return NULL;
    }
    for (jsize i = 0; i < count; i++) name[i] = (wchar_t)chars[i];
    name[count] = L'\0';
    (*env)->ReleaseStringChars(env, path, chars);
    HANDLE file = CreateFileW(name, GENERIC_READ, FILE_SHARE_READ, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    HeapFree(GetProcessHeap(), 0, name);
    if (file == INVALID_HANDLE_VALUE) {
        fail(env, "java/io/IOException", "cannot open canonical image read-only");
        return NULL;
    }
    LARGE_INTEGER size;
    if (!GetFileSizeEx(file, &size) || size.QuadPart != length) {
        CloseHandle(file);
        fail(env, "java/io/IOException", "canonical image length changed");
        return NULL;
    }
    HANDLE mapping = CreateFileMappingW(file, NULL, PAGE_READONLY, 0, 0, NULL);
    CloseHandle(file);
    if (mapping == NULL) {
        fail(env, "java/io/IOException", "cannot create shared image mapping");
        return NULL;
    }
    void *requested = (void *)(uintptr_t)(uint64_t)base;
    void *mapped = MapViewOfFileEx(mapping, FILE_MAP_READ, 0, 0, (SIZE_T)length, requested);
    CloseHandle(mapping);
    if (mapped == NULL || mapped != requested) {
        if (mapped != NULL) UnmapViewOfFile(mapped);
        fail(env, "java/io/IOException", "exact Windows address unavailable (no relocation)");
        return NULL;
    }
    jobject buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) UnmapViewOfFile(mapped);
    return buffer;
#else
    fail(env, "java/lang/UnsupportedOperationException", "no strict fixed mapping on this OS");
    return NULL;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_m3_text_M3FixedAddressMapping_directAddress0(JNIEnv *env, jclass klass,
        jobject buffer) {
    (void)klass;
    if (buffer == NULL) return 0;
    return (jlong)(uintptr_t)(*env)->GetDirectBufferAddress(env, buffer);
}

JNIEXPORT void JNICALL
Java_com_m3_text_M3FixedAddressMapping_unmapRejected0(JNIEnv *env, jclass klass,
        jlong base, jlong length) {
    (void)klass;
    if (!valid_range(base, length)) {
        fail(env, "java/lang/IllegalArgumentException", "invalid unmap range");
        return;
    }
#ifdef __linux__
    if (munmap((void *)(uintptr_t)(uint64_t)base, (size_t)length) != 0) {
        fail(env, "java/io/IOException", "cannot unmap rejected image");
    }
#elif defined(_WIN32)
    (void)length;
    if (!UnmapViewOfFile((void *)(uintptr_t)(uint64_t)base)) {
        fail(env, "java/io/IOException", "cannot unmap rejected image");
    }
#else
    (void)env;
#endif
}

JNIEXPORT void JNICALL
Java_com_m3_text_M3FixedAddressMapping_reserveCollision0(JNIEnv *env, jclass klass,
        jlong base, jlong length) {
    (void)klass;
    if (!valid_range(base, length)) {
        fail(env, "java/lang/IllegalArgumentException", "invalid reservation range");
        return;
    }
#ifdef __linux__
#ifndef MAP_FIXED_NOREPLACE
    fail(env, "java/lang/UnsupportedOperationException", "MAP_FIXED_NOREPLACE unavailable");
#else
    void *requested = (void *)(uintptr_t)(uint64_t)base;
    void *mapped = mmap(requested, (size_t)length, PROT_NONE,
            MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED_NOREPLACE, -1, 0);
    if (mapped == MAP_FAILED) {
        fail(env, "java/io/IOException", "cannot reserve target address for proof");
    } else if (mapped != requested) {
        munmap(mapped, (size_t)length);
        fail(env, "java/io/IOException", "unexpected reservation address");
    }
#endif
#elif defined(_WIN32)
    void *requested = (void *)(uintptr_t)(uint64_t)base;
    void *reserved = VirtualAlloc(requested, (SIZE_T)length, MEM_RESERVE, PAGE_NOACCESS);
    if (reserved != requested) {
        if (reserved != NULL) VirtualFree(reserved, 0, MEM_RELEASE);
        fail(env, "java/io/IOException", "cannot reserve target address for proof");
    }
#else
    fail(env, "java/lang/UnsupportedOperationException", "no fixed reservation on this OS");
#endif
}
