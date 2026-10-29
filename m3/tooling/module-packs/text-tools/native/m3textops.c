/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

#define REGION_UNITS 256

static void m3_throw(JNIEnv *env, const char *name, const char *message) {
    jclass kind = (*env)->FindClass(env, name);
    if (kind != NULL) {
        (*env)->ThrowNew(env, kind, message);
        (*env)->DeleteLocalRef(env, kind);
    }
}

/* Java String.compareTo order: unsigned UTF-16 code units, not locale or UTF-8 order.
 * Only bounded regions are copied; JNI never acquires a flattened full String. */
static int m3_compare(JNIEnv *env, jstring left, jstring right, int *out) {
    jsize a = (*env)->GetStringLength(env, left);
    jsize b = (*env)->GetStringLength(env, right);
    if ((*env)->ExceptionCheck(env)) return 0;
    jchar la[REGION_UNITS], rb[REGION_UNITS];
    jsize minimum = a < b ? a : b;
    for (jsize at = 0; at < minimum;) {
        jsize take = minimum - at < REGION_UNITS ? minimum - at : REGION_UNITS;
        (*env)->GetStringRegion(env, left, at, take, la);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->GetStringRegion(env, right, at, take, rb);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jsize i = 0; i < take; i++) {
            if (la[i] != rb[i]) {
                *out = la[i] < rb[i] ? -1 : 1;
                return 1;
            }
        }
        at += take;
    }
    *out = a < b ? -1 : a > b ? 1 : 0;
    return 1;
}

static int m3_compare_slots(JNIEnv *env, jobjectArray strings,
                            jint left, jint right, int *out) {
    jstring a = (jstring)(*env)->GetObjectArrayElement(env, strings, left);
    if ((*env)->ExceptionCheck(env)) return 0;
    jstring b = (jstring)(*env)->GetObjectArrayElement(env, strings, right);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->DeleteLocalRef(env, a);
        return 0;
    }
    if (a == NULL || b == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null text line");
        (*env)->DeleteLocalRef(env, a);
        (*env)->DeleteLocalRef(env, b);
        return 0;
    }
    int ok = m3_compare(env, a, b, out);
    (*env)->DeleteLocalRef(env, a);
    (*env)->DeleteLocalRef(env, b);
    return ok;
}

/* A compact manufactured lexical signal: the first eight UTF-16 units plus
 * the full length. Most comparisons never re-enter JNI to examine text. */
typedef struct {
    uint64_t high;
    uint64_t low;
    jsize length;
} M3Prefix;

static int m3_prefixes(JNIEnv *env, jobjectArray strings, M3Prefix *keys, size_t n) {
    for (size_t i = 0; i < n; i++) {
        jstring text = (jstring)(*env)->GetObjectArrayElement(env, strings, (jsize)i);
        if ((*env)->ExceptionCheck(env)) return 0;
        if (text == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null text line");
            return 0;
        }
        jsize length = (*env)->GetStringLength(env, text);
        if ((*env)->ExceptionCheck(env)) { (*env)->DeleteLocalRef(env, text); return 0; }
        jchar region[8] = {0};
        jsize take = length < 8 ? length : 8;
        if (take != 0) (*env)->GetStringRegion(env, text, 0, take, region);
        (*env)->DeleteLocalRef(env, text);
        if ((*env)->ExceptionCheck(env)) return 0;
        uint64_t high = 0, low = 0;
        for (int j = 0; j < 4; j++) high = (high << 16) | region[j];
        for (int j = 4; j < 8; j++) low = (low << 16) | region[j];
        keys[i].high = high;
        keys[i].low = low;
        keys[i].length = length;
    }
    return 1;
}

static int m3_key_compare(JNIEnv *env, jobjectArray strings,
                          M3Prefix *keys, jint a, jint b, int *out) {
    if (keys[a].high != keys[b].high) {
        *out = keys[a].high < keys[b].high ? -1 : 1;
        return 1;
    }
    if (keys[a].low != keys[b].low) {
        *out = keys[a].low < keys[b].low ? -1 : 1;
        return 1;
    }
    if (keys[a].length <= 8 || keys[b].length <= 8) {
        *out = keys[a].length < keys[b].length ? -1 :
               keys[a].length > keys[b].length ? 1 : 0;
        return 1;
    }
    return m3_compare_slots(env, strings, a, b, out);
}

/* Stable bottom-up mergesort of offsets. No Java payloads are copied or interned. */
JNIEXPORT void JNICALL Java_jdk_m3_text_NativeLineOps_sort0
  (JNIEnv *env, jclass owner, jobjectArray strings, jintArray result, jboolean reverse) {
    (void)owner;
    if (strings == NULL || result == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "sort arrays");
        return;
    }
    jsize length = (*env)->GetArrayLength(env, strings);
    if (length != (*env)->GetArrayLength(env, result)) {
        m3_throw(env, "java/lang/IllegalArgumentException", "sort lengths differ");
        return;
    }
    size_t n = (size_t)length;
    if (n == 0) return;
    if (n > SIZE_MAX / sizeof(jint) || n > SIZE_MAX / sizeof(M3Prefix)) {
        m3_throw(env, "java/lang/OutOfMemoryError", "sort index allocation overflow");
        return;
    }
    jint *order = (jint *)malloc(n * sizeof(jint));
    jint *scratch = (jint *)malloc(n * sizeof(jint));
    M3Prefix *keys = (M3Prefix *)malloc(n * sizeof(M3Prefix));
    if (order == NULL || scratch == NULL || keys == NULL) {
        free(order); free(scratch); free(keys);
        m3_throw(env, "java/lang/OutOfMemoryError", "native sort index allocation");
        return;
    }
    if (!m3_prefixes(env, strings, keys, n)) {
        free(order); free(scratch); free(keys);
        return;
    }
    for (size_t i = 0; i < n; i++) order[i] = (jint)i;
    for (size_t width = 1; width < n;) {
        for (size_t first = 0; first < n;) {
            size_t middle = n - first < width ? n : first + width;
            size_t last = n - middle < width ? n : middle + width;
            size_t i = first, j = middle, k = first;
            while (i < middle && j < last) {
                int comparison;
                if (!m3_key_compare(env, strings, keys, order[i], order[j], &comparison)) {
                    free(order); free(scratch); free(keys);
                    return;
                }
                if (reverse == JNI_TRUE) comparison = -comparison;
                scratch[k++] = comparison <= 0 ? order[i++] : order[j++];
            }
            while (i < middle) scratch[k++] = order[i++];
            while (j < last) scratch[k++] = order[j++];
            first = last;
        }
        jint *swap = order; order = scratch; scratch = swap;
        if (width > n / 2) break;
        width *= 2;
    }
    (*env)->SetIntArrayRegion(env, result, 0, length, order);
    free(order); free(scratch); free(keys);
}

/* One bit per retained line; adjacent equality only (not global deduplication). */
JNIEXPORT void JNICALL Java_jdk_m3_text_NativeLineOps_uniq0
  (JNIEnv *env, jclass owner, jobjectArray strings, jlongArray result) {
    (void)owner;
    if (strings == NULL || result == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "uniq arrays");
        return;
    }
    jsize n = (*env)->GetArrayLength(env, strings);
    size_t words = ((size_t)n + 63u) / 64u;
    if ((size_t)(*env)->GetArrayLength(env, result) != words) {
        m3_throw(env, "java/lang/IllegalArgumentException", "uniq mask length");
        return;
    }
    if (words == 0) return;
    jlong *bits = (jlong *)calloc(words, sizeof(jlong));
    if (bits == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native uniq mask allocation");
        return;
    }
    jstring previous = (jstring)(*env)->GetObjectArrayElement(env, strings, 0);
    if ((*env)->ExceptionCheck(env)) { free(bits); return; }
    if (previous == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null text line");
        free(bits); return;
    }
    bits[0] = (jlong)1;
    for (jsize i = 1; i < n; i++) {
        jstring current = (jstring)(*env)->GetObjectArrayElement(env, strings, i);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, previous); free(bits); return;
        }
        if (current == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null text line");
            (*env)->DeleteLocalRef(env, previous); free(bits); return;
        }
        int cmp;
        int ok = m3_compare(env, previous, current, &cmp);
        (*env)->DeleteLocalRef(env, previous);
        if (!ok) { (*env)->DeleteLocalRef(env, current); free(bits); return; }
        if (cmp != 0) {
            uint64_t bit = UINT64_C(1) << ((unsigned int)i & 63u);
            bits[(size_t)i >> 6] |= (jlong)bit;
        }
        previous = current;
    }
    (*env)->DeleteLocalRef(env, previous);
    (*env)->SetLongArrayRegion(env, result, 0, (jsize)words, bits);
    free(bits);
}
