/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include "com_m3_collections_M3IndexCountKernel.h"

/* Numeric count-span mechanics only. No retained pointers/references or callbacks. */
#define COUNT_CHUNK 256
_Static_assert(sizeof(jint) == 4, "32-bit JNI counts required");
_Static_assert(sizeof(jlong) == 8, "64-bit JNI ordinal required");
_Static_assert(INT32_MAX == 2147483647, "JNI positive count bound");

static void raise(JNIEnv *env, const char *name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, name);
    if (type == NULL) return;
    (void) (*env)->ThrowNew(env, type, message);
    (*env)->DeleteLocalRef(env, type);
}

static int bounds(JNIEnv *env, jintArray counts, jint from, jint to) {
    if (counts == NULL) {
        raise(env, "java/lang/NullPointerException", "counts"); return 0;
    }
    jsize length = (*env)->GetArrayLength(env, counts);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (from < 0 || to < from || to > length) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid count span"); return 0;
    }
    return 1;
}

JNIEXPORT jlong JNICALL Java_com_m3_collections_M3IndexCountKernel_sum0(
        JNIEnv *env, jclass owner, jintArray counts, jint from, jint to) {
    (void) owner;
    if (!bounds(env, counts, from, to)) return 0;
    jint buffer[COUNT_CHUNK];
    jlong total = 0;
    for (jint base = from; base < to;) {
        jint take = to - base < COUNT_CHUNK ? to - base : COUNT_CHUNK;
        (*env)->GetIntArrayRegion(env, counts, base, take, buffer);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jint i = 0; i < take; i++) {
            if (buffer[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative count"); return 0;
            }
            total += (jlong) buffer[i];
        }
        base += take;
    }
    return total;
}

JNIEXPORT jlong JNICALL Java_com_m3_collections_M3IndexCountKernel_locate0(
        JNIEnv *env, jclass owner, jintArray counts, jint from, jint to, jlong ordinal,
        jboolean descending) {
    (void) owner;
    if (!bounds(env, counts, from, to)) return 0;
    if (ordinal < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "negative occurrence"); return 0;
    }
    jint buffer[COUNT_CHUNK];
    jlong remaining = ordinal;
    jlong result = -1;
    jint next = descending ? to : from;
    while (descending ? next > from : next < to) {
        jint available = descending ? next - from : to - next;
        jint take = available < COUNT_CHUNK ? available : COUNT_CHUNK;
        jint base = descending ? next - take : next;
        (*env)->GetIntArrayRegion(env, counts, base, take, buffer);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jint j = 0; j < take; j++) {
            jint i = descending ? take - 1 - j : j;
            jint count = buffer[i];
            if (count < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative count"); return 0;
            }
            if (result < 0) {
                if (remaining < (jlong) count) {
                    /* row >= 0 and <= INT32_MAX-1: shifted result fits signed jlong. */
                    result = ((jlong) (base + i) << 32) | remaining;
                } else remaining -= (jlong) count;
            }
        }
        next = descending ? base : base + take;
    }
    if (result < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "occurrence outside count span"); return 0;
    }
    return result;
}

/* Adapted Synexia M3RowSelectionKernel atom; reuse this library's exception owner. */
static uint64_t selected_bits(uint64_t bits, jsize word, jsize words, jint rows) {
    unsigned final_bits = (unsigned) (rows & 63);
    if (word == words - 1 && final_bits != 0U)
        bits &= (UINT64_C(1) << final_bits) - UINT64_C(1);
    return bits;
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexCountKernel_selectedRows0(
        JNIEnv *env, jclass owner, jlongArray input, jint rows) {
    (void) owner;
    if (input == NULL) { raise(env, "java/lang/NullPointerException", "words"); return NULL; }
    if (rows < 0) { raise(env, "java/lang/IllegalArgumentException", "logicalRows must be nonnegative"); return NULL; }
    jsize required = (rows >> 6) + ((rows & 63) != 0);
    jsize length = (*env)->GetArrayLength(env, input);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (length != required) { raise(env, "java/lang/IllegalArgumentException", "mask word count mismatch"); return NULL; }
    jlong words[COUNT_CHUNK];
    jsize count = 0;
    for (jsize base = 0; base < required;) {
        jsize take = required - base < COUNT_CHUNK ? required - base : COUNT_CHUNK;
        (*env)->GetLongArrayRegion(env, input, base, take, words);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < take; i++) {
            uint64_t bits = selected_bits((uint64_t) words[i], base + i, required, rows);
            while (bits != 0U) { ++count; bits &= bits - UINT64_C(1); }
        }
        base += take;
    }
    jintArray selected = (*env)->NewIntArray(env, count);
    if (selected == NULL || (*env)->ExceptionCheck(env)) return NULL;
    jint buffer[COUNT_CHUNK];
    jint filled = 0, written = 0;
    for (jsize base = 0; base < required;) {
        jsize take = required - base < COUNT_CHUNK ? required - base : COUNT_CHUNK;
        (*env)->GetLongArrayRegion(env, input, base, take, words);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < take; i++) {
            uint64_t bits = selected_bits((uint64_t) words[i], base + i, required, rows);
            while (bits != 0U) {
                if ((int64_t) written + filled >= count) {
                    raise(env, "java/lang/IllegalStateException", "mask changed during selection"); return NULL;
                }
                jint bit = 0; uint64_t probe = bits;
                while ((probe & UINT64_C(1)) == 0U) { ++bit; probe >>= 1; }
                buffer[filled++] = ((base + i) << 6) + bit;
                bits &= bits - UINT64_C(1);
                if (filled == COUNT_CHUNK) {
                    (*env)->SetIntArrayRegion(env, selected, written, filled, buffer);
                    if ((*env)->ExceptionCheck(env)) return NULL;
                    written += filled; filled = 0;
                }
            }
        }
        base += take;
    }
    if ((int64_t) written + filled != count) {
        raise(env, "java/lang/IllegalStateException", "mask changed during selection"); return NULL;
    }
    if (filled != 0) {
        (*env)->SetIntArrayRegion(env, selected, written, filled, buffer);
        if ((*env)->ExceptionCheck(env)) return NULL;
    }
    return selected;
}
