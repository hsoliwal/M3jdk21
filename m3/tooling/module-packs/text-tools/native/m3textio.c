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

JNIEXPORT jint JNICALL Java_jdk_internal_m3_unix4jm3_io_FileTools_mkfifo0
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

JNIEXPORT jint JNICALL Java_jdk_internal_m3_unix4jm3_NativeUtf16Literal_abiVersion0
  (JNIEnv *env, jclass owner) { (void)env; (void)owner; return 1; }
JNIEXPORT jint JNICALL Java_jdk_m3_text_NativeUtf16Literal_abiVersion0
  (JNIEnv *env, jclass owner) { (void)env; (void)owner; return 1; }
JNIEXPORT jint JNICALL Java_jdk_internal_m3_unix4jm3_NativeUtf16Literal_feed0
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

/* M3 JNI native UTF-8 line kernel; generated from the source-sealed Synexia recipe.
 * Source payload is borrowed only within each call, never retained. */
#include <stdint.h>
#define M3_SCAN_TRIPLES 256
#define M3_SCAN_MAX_PATTERN 65536

static void m3_utf8_throw(JNIEnv *env, const char *kind, const char *message) {
    jclass type = (*env)->FindClass(env, kind);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static const unsigned char *m3_direct_bytes(JNIEnv *env, jobject buffer, jint from, jint limit) {
    if (buffer == NULL) {
        m3_utf8_throw(env, "java/lang/NullPointerException", "buffer");
        return NULL;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (from < 0 || limit < from || capacity < 0 || (jlong)limit > capacity) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid direct-buffer range");
        return NULL;
    }
    const unsigned char *p = (const unsigned char *)(*env)->GetDirectBufferAddress(env, buffer);
    if (p == NULL) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "direct buffer required");
        return NULL;
    }
    return p;
}

static int m3_valid_utf8(const unsigned char *p, jint start, jint end) {
    for (jint i = start; i < end;) {
        unsigned c = p[i++];
        if (c < 0x80) continue;
        int remaining;
        uint32_t cp;
        uint32_t min;
        if (c >= 0xc2 && c <= 0xdf) { remaining = 1; cp = c & 0x1f; min = 0x80; }
        else if (c >= 0xe0 && c <= 0xef) { remaining = 2; cp = c & 0x0f; min = 0x800; }
        else if (c >= 0xf0 && c <= 0xf4) { remaining = 3; cp = c & 0x07; min = 0x10000; }
        else return 0;
        if (end - i < remaining) return 0;
        for (int j = 0; j < remaining; j++) {
            unsigned next = p[i++];
            if ((next & 0xc0) != 0x80) return 0;
            cp = (cp << 6) | (next & 0x3f);
        }
        if (cp < min || cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) return 0;
    }
    return 1;
}

static jlong m3_validate_lines(JNIEnv *env, jobject buffer, jint from, jint limit) {
    const unsigned char *p = m3_direct_bytes(env, buffer, from, limit);
    if (p == NULL) return -1;
    if (!m3_valid_utf8(p, from, limit)) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "malformed UTF-8 input");
        return -1;
    }
    jlong rows = 0;
    for (jint pos = from; pos < limit;) {
        rows++;
        while (pos < limit && p[pos] != '\r' && p[pos] != '\n') pos++;
        if (pos < limit) {
            unsigned first = p[pos++];
            if (first == '\r' && pos < limit && p[pos] == '\n') pos++;
        }
    }
    return rows;
}

/*
 * The existing short-literal memchr path remains for small needles.
 * Long literals use a bounded KMP failure table: linear worst-case per scan
 * even for overlapping patterns such as aaaa...ab against aaaa...a.
 * All data are primitive lanes, freed before this JNI call returns.
 * A new scan call recalculates the table; native code retains no JVM pointers.
 */
static jint *m3_kmp_failure(JNIEnv *env, const unsigned char *needle, jint size) {
    if (size < 8) return NULL;
    jint *failure = (jint *)malloc((size_t)size * sizeof(jint));
    if (failure == NULL) {
        m3_utf8_throw(env, "java/lang/OutOfMemoryError", "native KMP failure table");
        return NULL;
    }
    failure[0] = 0;
    for (jint index = 1, prefix = 0; index < size; index++) {
        while (prefix > 0 && needle[index] != needle[prefix])
            prefix = failure[prefix - 1];
        if (needle[index] == needle[prefix]) prefix++;
        failure[index] = prefix;
    }
    return failure;
}

static int m3_line_contains(const unsigned char *p, jint begin, jint end,
                             const unsigned char *needle, jint size, const jint *failure) {
    if (size == 0) return 1;
    if (end - begin < size) return 0;
    if (failure != NULL) {
        for (jint index = begin, matched = 0; index < end; index++) {
            while (matched > 0 && p[index] != needle[matched])
                matched = failure[matched - 1];
            if (p[index] == needle[matched]) matched++;
            if (matched == size) return 1;
        }
        return 0;
    }
    const unsigned char *at = p + begin;
    const unsigned char *stop = p + end - size + 1;
    while (at < stop) {
        const unsigned char *found = (const unsigned char *)memchr(at, needle[0], (size_t)(stop - at));
        if (found == NULL) return 0;
        if (memcmp(found, needle, (size_t)size) == 0) return 1;
        at = found + 1;
    }
    return 0;
}

/* Scan body is common to the legacy JNI ABI and prepared-plan ABI.
 * Exactly one primitive int[3] triple per match, in original line order.
 * The direct source owner remains with the Java caller. */
static jlong m3_scan_rows(JNIEnv *env, const unsigned char *p, jint from, jint limit,
                          const unsigned char *needle, jint size,
                          const jint *failure, jintArray triples) {
    jint indices[3 * M3_SCAN_TRIPLES];
    jint used = 0;
    jint pos = from;
    while (pos < limit) {
        jint lineBegin = pos;
        while (pos < limit && p[pos] != '\r' && p[pos] != '\n') pos++;
        jint contentEnd = pos;
        if (pos < limit) {
            unsigned first = p[pos++];
            if (first == '\r' && pos < limit && p[pos] == '\n') pos++;
        }
        if (m3_line_contains(p, lineBegin, contentEnd, needle, size, failure)) {
            indices[3 * used] = lineBegin;
            indices[3 * used + 1] = contentEnd;
            indices[3 * used + 2] = pos;
            used++;
            if (used == M3_SCAN_TRIPLES) break;
        }
    }
    if (used > 0) {
        (*env)->SetIntArrayRegion(env, triples, 0, 3 * used, indices);
        if ((*env)->ExceptionCheck(env)) return -1;
    }
    return (jlong)(((uint64_t)(uint32_t)pos << 32) | (uint32_t)used);
}

static jlong m3_scan_literal(JNIEnv *env, jobject buffer, jint from, jint limit,
                              jbyteArray pattern, jintArray triples) {
    const unsigned char *p = m3_direct_bytes(env, buffer, from, limit);
    if (p == NULL) return -1;
    if (pattern == NULL || triples == NULL) {
        m3_utf8_throw(env, "java/lang/NullPointerException", "needle/offsets");
        return -1;
    }
    jsize size = (*env)->GetArrayLength(env, pattern);
    jsize available = (*env)->GetArrayLength(env, triples);
    if (size > M3_SCAN_MAX_PATTERN || available != 3 * M3_SCAN_TRIPLES) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid native scan lanes");
        return -1;
    }
    unsigned char *needle = NULL;
    if (size > 0) {
        needle = (unsigned char *)malloc((size_t)size);
        if (needle == NULL) {
            m3_utf8_throw(env, "java/lang/OutOfMemoryError", "native needle");
            return -1;
        }
        (*env)->GetByteArrayRegion(env, pattern, 0, size, (jbyte *)needle);
        if ((*env)->ExceptionCheck(env)) { free(needle); return -1; }
    }
    jint *failure = m3_kmp_failure(env, needle, size);
    if (size >= 8 && failure == NULL) {
        free(needle);
        return -1;
    }
    jlong packed = m3_scan_rows(env, p, from, limit, needle, size, failure, triples);
    free(failure);
    free(needle);
    return packed;
}

/* The caller owns both direct-buffer plans. No pointer or plan is retained,
 * so GC and JNI critical-section constraints do not depend on native handles. */
static jlong m3_scan_prepared(JNIEnv *env, jobject buffer, jint from, jint limit,
                             jobject needle_buffer, jobject failure_buffer, jintArray triples) {
    const unsigned char *p = m3_direct_bytes(env, buffer, from, limit);
    if (p == NULL) return -1;
    if (needle_buffer == NULL || failure_buffer == NULL || triples == NULL) {
        m3_utf8_throw(env, "java/lang/NullPointerException", "prepared scan lanes");
        return -1;
    }
    jlong size = (*env)->GetDirectBufferCapacity(env, needle_buffer);
    jlong cells = (*env)->GetDirectBufferCapacity(env, failure_buffer);
    jsize triples_size = (*env)->GetArrayLength(env, triples);
    if (size < 8 || size > M3_SCAN_MAX_PATTERN || cells != size
            || triples_size != 3 * M3_SCAN_TRIPLES) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid prepared scan geometry");
        return -1;
    }
    const unsigned char *needle = (const unsigned char *)
            (*env)->GetDirectBufferAddress(env, needle_buffer);
    const jint *failure = (const jint *)(*env)->GetDirectBufferAddress(env, failure_buffer);
    if (needle == NULL || failure == NULL || ((uintptr_t)failure % _Alignof(jint)) != 0) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid prepared scan address");
        return -1;
    }
    /* Fail closed on a corrupted/private-reflection-supplied KMP plan. */
    if (failure[0] != 0) {
        m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid KMP state");
        return -1;
    }
    for (jint i = 1; i < (jint)size; i++) {
        if (failure[i] < 0 || failure[i] > i) {
            m3_utf8_throw(env, "java/lang/IllegalArgumentException", "invalid KMP failure entry");
            return -1;
        }
    }
    return m3_scan_rows(env, p, from, limit, needle, (jint)size, failure, triples);
}

JNIEXPORT jlong JNICALL Java_jdk_m3_text_NativeUtf8Lines_validate0
 (JNIEnv *env, jclass ignored, jobject buffer, jint from, jint end) {
    (void)ignored;
    return m3_validate_lines(env, buffer, from, end);
}
JNIEXPORT jlong JNICALL Java_jdk_m3_text_NativeUtf8Lines_scan0
 (JNIEnv *env, jclass ignored, jobject buffer, jint from, jint end, jbyteArray needle, jintArray triples) {
    (void)ignored;
    return m3_scan_literal(env, buffer, from, end, needle, triples);
}

JNIEXPORT jlong JNICALL Java_jdk_m3_text_NativeUtf8Lines_scanPrepared0
 (JNIEnv *env, jclass ignored, jobject buffer, jint from, jint end,
  jobject needle, jobject failure, jintArray triples) {
    (void)ignored;
    return m3_scan_prepared(env, buffer, from, end, needle, failure, triples);
}
