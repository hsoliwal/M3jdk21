/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This file is subject to the
 * "Classpath" exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

#include "java_util_M3CollectionMasks.h"
#include <stdint.h>

/* Bounded stack scratch; no native allocation, critical region, or retained
 * Java heap pointer. Java callbacks never cross this primitive boundary. */
enum { M3_MASK_WORD_CHUNK = 128, M3_MASK_INDEX_CHUNK = 256 };

static void m3_mask_throw(JNIEnv *env, const char *class_name, const char *message) {
    jclass exception_class = (*env)->FindClass(env, class_name);
    if (exception_class != NULL) {
        (*env)->ThrowNew(env, exception_class, message);
        (*env)->DeleteLocalRef(env, exception_class);
    }
}

static jint m3_mask_population(uint64_t word) {
#if defined(__GNUC__) || defined(__clang__)
    return (jint)__builtin_popcountll((unsigned long long)word);
#else
    jint count = 0;
    while (word != UINT64_C(0)) {
        word &= word - UINT64_C(1);
        count++;
    }
    return count;
#endif
}

/* Called only with a nonzero word. */
static jint m3_mask_trailing_zeros(uint64_t word) {
#if defined(__GNUC__) || defined(__clang__)
    return (jint)__builtin_ctzll((unsigned long long)word);
#else
    jint count = 0;
    while ((word & UINT64_C(1)) == UINT64_C(0)) {
        word >>= 1;
        count++;
    }
    return count;
#endif
}

static uint64_t m3_mask_window(jlong bits, jint index, jint first_word,
                               jint last_word, jint from, jint to) {
    uint64_t word = (uint64_t)bits;
    if (index == first_word) {
        word &= UINT64_MAX << ((uint32_t)from & 63u);
    }
    if (index == last_word) {
        /* The nonempty validated window guarantees to > 0. All shift
         * distances lie in 0..63, including a word-aligned upper bound. */
        word &= UINT64_MAX >> (63u - (((uint32_t)to - 1u) & 63u));
    }
    return word;
}

JNIEXPORT jintArray JNICALL
Java_java_util_M3CollectionMasks_selected0(JNIEnv *env, jclass type,
                                          jlongArray words, jint from, jint to) {
    jlong input[M3_MASK_WORD_CHUNK];
    jint output[M3_MASK_INDEX_CHUNK];
    jint first_word;
    jint last_word;
    jint block;
    jint count = 0;
    jint written = 0;
    jint buffered = 0;
    jintArray result;
    jsize word_count;
    (void)type;

    if (words == NULL) {
        m3_mask_throw(env, "java/lang/NullPointerException", "words");
        return NULL;
    }
    word_count = (*env)->GetArrayLength(env, words);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    if (from < 0 || to < from || (jlong)to > ((jlong)word_count * 64)) {
        m3_mask_throw(env, "java/lang/IndexOutOfBoundsException",
                      "invalid selection range");
        return NULL;
    }
    if (from == to) {
        return (*env)->NewIntArray(env, 0);
    }
    first_word = (jint)((uint32_t)from >> 6);
    last_word = (jint)(((uint32_t)to - 1u) >> 6);

    for (block = first_word; block <= last_word;) {
        jint remaining = last_word - block + 1;
        jsize length = remaining < M3_MASK_WORD_CHUNK
                ? remaining : M3_MASK_WORD_CHUNK;
        jint offset;
        (*env)->GetLongArrayRegion(env, words, block, length, input);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        for (offset = 0; offset < length; offset++) {
            uint64_t word = m3_mask_window(input[offset], block + offset,
                    first_word, last_word, from, to);
            /* Each selected position lies in an int-sized half-open window,
             * so the count cannot overflow a jint. */
            count += m3_mask_population(word);
        }
        block += length;
    }

    result = (*env)->NewIntArray(env, count);
    if (result == NULL) {
        if (!(*env)->ExceptionCheck(env)) {
            m3_mask_throw(env, "java/lang/OutOfMemoryError", "selection result");
        }
        return NULL;
    }
    if (count == to - from) {
        jint observed = 0;
        /* A fully selected range has one exact consecutive answer. Re-read
         * all bounded word chunks before filling to retain drift detection. */
        for (block = first_word; block <= last_word;) {
            jint remaining = last_word - block + 1;
            jsize length = remaining < M3_MASK_WORD_CHUNK
                    ? remaining : M3_MASK_WORD_CHUNK;
            jint offset;
            (*env)->GetLongArrayRegion(env, words, block, length, input);
            if ((*env)->ExceptionCheck(env)) {
                return NULL;
            }
            for (offset = 0; offset < length; offset++) {
                observed += m3_mask_population(m3_mask_window(input[offset],
                        block + offset, first_word, last_word, from, to));
            }
            block += length;
        }
        if (observed != count) {
            m3_mask_throw(env, "java/util/ConcurrentModificationException",
                    "selection mask changed during extraction");
            return NULL;
        }
        while (written < count) {
            jint remaining = count - written;
            jsize length = remaining < M3_MASK_INDEX_CHUNK
                    ? remaining : M3_MASK_INDEX_CHUNK;
            jint offset;
            for (offset = 0; offset < length; offset++) {
                output[offset] = from + written + offset;
            }
            (*env)->SetIntArrayRegion(env, result, written, length, output);
            if ((*env)->ExceptionCheck(env)) {
                return NULL;
            }
            written += length;
        }
        return result;
    }
    for (block = first_word; block <= last_word;) {
        jint remaining = last_word - block + 1;
        jsize length = remaining < M3_MASK_WORD_CHUNK
                ? remaining : M3_MASK_WORD_CHUNK;
        jint offset;
        (*env)->GetLongArrayRegion(env, words, block, length, input);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        for (offset = 0; offset < length; offset++) {
            jint index = block + offset;
            uint64_t word = m3_mask_window(input[offset], index,
                    first_word, last_word, from, to);
            while (word != UINT64_C(0)) {
                if (written == count) {
                    m3_mask_throw(env, "java/util/ConcurrentModificationException",
                            "selection mask changed during extraction");
                    return NULL;
                }
                output[buffered++] = (jint)(((uint32_t)index << 6)
                        + (uint32_t)m3_mask_trailing_zeros(word));
                written++;
                if (buffered == M3_MASK_INDEX_CHUNK) {
                    (*env)->SetIntArrayRegion(env, result, written - buffered,
                            buffered, output);
                    if ((*env)->ExceptionCheck(env)) {
                        return NULL;
                    }
                    buffered = 0;
                }
                word &= word - UINT64_C(1);
            }
        }
        block += length;
    }
    if (written != count) {
        m3_mask_throw(env, "java/util/ConcurrentModificationException",
                "selection mask changed during extraction");
        return NULL;
    }
    if (buffered != 0) {
        (*env)->SetIntArrayRegion(env, result, written - buffered, buffered, output);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
    }
    return result;
}
