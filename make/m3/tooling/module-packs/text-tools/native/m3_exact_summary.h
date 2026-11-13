/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Additive entry point in the existing m3textio translation unit. */
#ifndef M3_EXACT_SUMMARY_H
#define M3_EXACT_SUMMARY_H
#include <jni.h>
#include <stdlib.h>

static void m3_summary_throw(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (void)(*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static void *m3_summary_alloc(size_t size) {
#ifdef M3_SUMMARY_FORCE_OOM
    (void)size;
    return NULL; /* deterministic test-only allocation failure discriminator */
#else
    return malloc(size);
#endif
}

/* Verify the entire supplied prefix against the immutable copied pattern.
 * Checking only that fallback edges decrease is insufficient for exact results. */
static int m3_summary_prefix_valid(const jchar *key, const jint *prefix, jint length) {
    if (prefix[0] != 0) return 0;
    jint matched = 0;
    for (jint i = 1; i < length; i++) {
        while (matched > 0 && key[i] != key[matched]) matched = prefix[matched - 1];
        if (key[i] == key[matched]) matched++;
        if (prefix[i] != matched) return 0;
    }
    return 1;
}

JNIEXPORT jlongArray JNICALL Java_jdk_m3_tools_text_search_ExactSearch_summaryNative0
  (JNIEnv *env, jclass owner, jstring source, jint from, jint to,
   jstring pattern, jintArray failure) {
    (void)owner;
    if (source == NULL || pattern == NULL || failure == NULL) {
        m3_summary_throw(env, "java/lang/NullPointerException", "null summary input");
        return NULL;
    }
    jsize length = (*env)->GetStringLength(env, source);
    jsize size = (*env)->GetStringLength(env, pattern);
    jsize prefix_size = (*env)->GetArrayLength(env, failure);
    if (from < 0 || to < from || to > length) {
        m3_summary_throw(env, "java/lang/IndexOutOfBoundsException", "summary range");
        return NULL;
    }
    if (size > (1 << 20) || prefix_size != size) {
        m3_summary_throw(env, "java/lang/IllegalArgumentException", "summary prefix geometry");
        return NULL;
    }
    jlong facts[3] = {-1, -1, 0};
    if (size == 0) {
        facts[0] = from; facts[1] = to; facts[2] = (jlong)to - (jlong)from + 1;
    } else {
        /* ABI scratch only. No retained payload, GetStringChars, critical section,
         * Java heap pointer, handle registry, or global reference. */
        jchar *key = (jchar *)m3_summary_alloc((size_t)size * sizeof(jchar));
        jint *prefix = (jint *)m3_summary_alloc((size_t)size * sizeof(jint));
        if (key == NULL || prefix == NULL) {
            free(key); free(prefix);
            m3_summary_throw(env, "java/lang/OutOfMemoryError", "summary pattern scratch");
            return NULL;
        }
        (*env)->GetStringRegion(env, pattern, 0, size, key);
        if ((*env)->ExceptionCheck(env)) { free(key); free(prefix); return NULL; }
        (*env)->GetIntArrayRegion(env, failure, 0, size, prefix);
        if ((*env)->ExceptionCheck(env)) { free(key); free(prefix); return NULL; }
        if (!m3_summary_prefix_valid(key, prefix, size)) {
            free(key); free(prefix);
            m3_summary_throw(env, "java/lang/IllegalArgumentException", "inexact summary prefix");
            return NULL;
        }
        jchar page[1024];
        jint matched = 0;
        for (jint offset = from; offset < to;) {
            jint count = to - offset;
            if (count > 1024) count = 1024;
            (*env)->GetStringRegion(env, source, offset, count, page);
            if ((*env)->ExceptionCheck(env)) { free(key); free(prefix); return NULL; }
            for (jint i = 0; i < count; i++) {
                while (matched > 0 && page[i] != key[matched]) matched = prefix[matched - 1];
                if (page[i] == key[matched]) matched++;
                if (matched == size) {
                    jlong at = (jlong)offset + (jlong)i - (jlong)size + 1;
                    if (facts[2] == 0) facts[0] = at;
                    facts[1] = at;
                    facts[2]++;
                    matched = prefix[matched - 1];
                }
            }
            offset += count;
        }
        free(key); free(prefix);
    }
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, result, 0, 3, facts);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return result;
}

/* Batched independent ranges. Reuse the existing exact-prefix verifier, allocator
 * and error owner. All acquired scratch is invocation-local and freed on exit. */
JNIEXPORT jlongArray JNICALL Java_jdk_m3_tools_text_search_ExactSearch_summaryRangesNative0
  (JNIEnv *env, jclass owner, jstring source, jintArray starts, jintArray ends,
   jstring pattern, jintArray failure) {
    (void)owner;
    if (source == NULL || starts == NULL || ends == NULL || pattern == NULL || failure == NULL) {
        m3_summary_throw(env, "java/lang/NullPointerException", "null batch input");
        return NULL;
    }
    jsize length = (*env)->GetStringLength(env, source);
    jsize rows = (*env)->GetArrayLength(env, starts);
    jsize size = (*env)->GetStringLength(env, pattern);
    if (rows > 1000000 || rows != (*env)->GetArrayLength(env, ends)
            || size > (1 << 20) || size != (*env)->GetArrayLength(env, failure)) {
        m3_summary_throw(env, "java/lang/IllegalArgumentException", "batch geometry/budget");
        return NULL;
    }
    jint *bounds = NULL;
    jchar *key = NULL;
    jint *prefix = NULL;
    jlongArray result = NULL;
    if (rows != 0) {
        bounds = (jint *)m3_summary_alloc((size_t)rows * 2 * sizeof(jint));
        if (bounds == NULL) goto allocation_failed;
        (*env)->GetIntArrayRegion(env, starts, 0, rows, bounds);
        if ((*env)->ExceptionCheck(env)) goto done;
        (*env)->GetIntArrayRegion(env, ends, 0, rows, bounds + rows);
        if ((*env)->ExceptionCheck(env)) goto done;
    }
    jlong units = 0;
    for (jint row = 0; row < rows; row++) {
        jint from = bounds[row], to = bounds[rows + row];
        if (from < 0 || to < from || to > length) {
            m3_summary_throw(env, "java/lang/IndexOutOfBoundsException", "batch range");
            goto done;
        }
        units += (jlong)to - from;
        if (units > ((jlong)64 << 20)) {
            m3_summary_throw(env, "java/lang/IllegalArgumentException", "selected UTF-16 budget");
            goto done;
        }
    }
    if (size != 0) {
        key = (jchar *)m3_summary_alloc((size_t)size * sizeof(jchar));
        prefix = (jint *)m3_summary_alloc((size_t)size * sizeof(jint));
        if (key == NULL || prefix == NULL) goto allocation_failed;
        (*env)->GetStringRegion(env, pattern, 0, size, key);
        if ((*env)->ExceptionCheck(env)) goto done;
        (*env)->GetIntArrayRegion(env, failure, 0, size, prefix);
        if ((*env)->ExceptionCheck(env)) goto done;
        if (!m3_summary_prefix_valid(key, prefix, size)) {
            m3_summary_throw(env, "java/lang/IllegalArgumentException", "inexact batch prefix");
            goto done;
        }
    }
    result = (*env)->NewLongArray(env, 3 * rows);
    if (result == NULL) goto done;
    jchar page[1024];
    jlong facts[3 * 128];
    jint used = 0, written = 0;
    for (jint row = 0; row < rows; row++) {
        jint from = bounds[row], to = bounds[rows + row];
        jlong first = -1, last = -1, hits = 0;
        if (size == 0) {
            first = from; last = to; hits = (jlong)to - from + 1;
        } else if (size <= to - from) {
            jint matched = 0;
            for (jint offset = from; offset < to;) {
                jint count = to - offset;
                if (count > 1024) count = 1024;
                (*env)->GetStringRegion(env, source, offset, count, page);
                if ((*env)->ExceptionCheck(env)) { result = NULL; goto done; }
                for (jint i = 0; i < count; i++) {
                    while (matched > 0 && page[i] != key[matched]) matched = prefix[matched - 1];
                    if (page[i] == key[matched]) matched++;
                    if (matched == size) {
                        jlong at = (jlong)offset + i - size + 1;
                        if (hits == 0) first = at;
                        last = at; hits++;
                        matched = prefix[matched - 1];
                    }
                }
                offset += count;
            }
        }
        facts[used++] = first; facts[used++] = last; facts[used++] = hits;
        if (used == 3 * 128 || row + 1 == rows) {
            (*env)->SetLongArrayRegion(env, result, written, used, facts);
            if ((*env)->ExceptionCheck(env)) { result = NULL; goto done; }
            written += used; used = 0;
        }
    }
    goto done;
allocation_failed:
    m3_summary_throw(env, "java/lang/OutOfMemoryError", "batch summary scratch");
done:
    free(bounds); free(key); free(prefix);
    return result;
}
#endif
