/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
#include "jdk_internal_mindex_M3RegexDpPlan.h"
#include <stdint.h>
#include <string.h>

static unsigned first_bit(uint64_t word);

/* Target-owned ordered Boolean relation composition. No canonical text crosses JNI.
 * All lanes are validated before output is written; the local result permits aliases. */
static int compose_rows(const uint64_t *a, const uint64_t *b, uint64_t *out, size_t n) {
    uint64_t result[64];
    if (a == NULL || b == NULL || out == NULL || n == 0 || n > 64) return -1;
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
    uint64_t ceiling = 0;
    for (size_t i = 0; i < n; i++) {
        if (((a[i] | b[i]) & ~mask) != 0) return -1;
        ceiling |= b[i];
    }
    /* Validation above precedes every shortcut, including zero output. */
    if (ceiling == 0) { memset(out, 0, n * sizeof(uint64_t)); return 0; }
    /* Only multi-state rows can repay proving a homogeneous left relation. */
    if ((a[0] & (a[0] - 1)) != 0) {
        size_t equal = 1;
        while (equal < n && a[equal] == a[0]) equal++;
        if (equal == n) {
            uint64_t frontier = a[0], value = frontier == mask ? ceiling : 0;
            while (frontier != 0 && value != ceiling) {
                value |= b[first_bit(frontier)];
                frontier &= frontier - 1;
            }
            for (size_t i = 0; i < n; i++) result[i] = value;
            memcpy(out, result, n * sizeof(uint64_t));
            return 0;
        }
    }
    for (size_t i = 0; i < n; i++) {
        uint64_t frontier = a[i], value = 0;
        while (frontier != 0 && value != ceiling) {
            unsigned bit = 0;
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_REGEX_PORTABLE)
            bit = (unsigned)__builtin_ctzll(frontier);
#else
            uint64_t scan = frontier;
            while ((scan & UINT64_C(1)) == 0) { scan >>= 1; bit++; }
#endif
            value |= b[bit];
            frontier &= frontier - 1;
        }
        result[i] = value;
    }
    memcpy(out, result, n * sizeof(uint64_t));
    return 0;
}

/* The caller proves the input is nonzero before entering this replaceable atom. */
static unsigned first_bit(uint64_t word) {
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_REGEX_PORTABLE)
    return (unsigned)__builtin_ctzll(word);
#else
    unsigned bit = 0;
    while ((word & UINT64_C(1)) == 0) { word >>= 1; bit++; }
    return bit;
#endif
}

static int compose_structured_rows(const uint64_t *a, const uint64_t *b, uint64_t *out, size_t n) {
    uint64_t result[64];
    if (a == NULL || b == NULL || out == NULL || n == 0 || n > 64) return -1;
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
    uint64_t ceiling = 0;
    for (size_t i = 0; i < n; i++) {
        if (((a[i] | b[i]) & ~mask) != 0) return -1;
        ceiling |= b[i];
    }
    if (ceiling == 0) { memset(out, 0, n * sizeof(uint64_t)); return 0; }

    /* Stop this proof at the first counterexample; do not tax every generic row. */
    uint64_t productive = 0, previous = UINT64_MAX;
    int nested = 1;
    for (size_t i = 0; i < n; i++) {
        if (b[i] == 0) continue;
        if ((b[i] & ~previous) != 0) { nested = 0; break; }
        productive |= UINT64_C(1) << i;
        previous = b[i];
    }
    if (nested) {
        /* Nonempty rows descend by containment: the first selected row dominates. */
        for (size_t i = 0; i < n; i++) {
            uint64_t selected = a[i] & productive;
            result[i] = selected == 0 ? 0 : b[first_bit(selected)];
        }
    } else {
        /* The exact generic union traversal remains the fallback. */
        for (size_t i = 0; i < n; i++) {
            uint64_t frontier = a[i], value = 0;
            while (frontier != 0 && value != ceiling) {
                value |= b[first_bit(frontier)];
                frontier &= frontier - 1;
            }
            result[i] = value;
        }
    }
    memcpy(out, result, n * sizeof(uint64_t));
    return 0;
}

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (void)(*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static jlongArray compose_jni(JNIEnv *env, jlongArray left, jlongArray right,
        int (*kernel)(const uint64_t*, const uint64_t*, uint64_t*, size_t)) {
    jlong transfer[64];
    uint64_t a[64], b[64];
    _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long width");
    if (left == NULL || right == NULL) {
        fail(env, "java/lang/NullPointerException", "null regex effect");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, left);
    if (n < 1 || n > 64 || n != (*env)->GetArrayLength(env, right)) {
        fail(env, "java/lang/IllegalArgumentException", "invalid regex effect dimensions");
        return NULL;
    }
    (*env)->GetLongArrayRegion(env, left, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    memcpy(a, transfer, (size_t)n * sizeof(uint64_t));
    (*env)->GetLongArrayRegion(env, right, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    memcpy(b, transfer, (size_t)n * sizeof(uint64_t));
    if (kernel(a, b, a, (size_t)n) != 0) {
        fail(env, "java/lang/IllegalArgumentException", "foreign regex effect state bits");
        return NULL;
    }
    memcpy(transfer, a, (size_t)n * sizeof(uint64_t));
    jlongArray output = (*env)->NewLongArray(env, n);
    if (output == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, output, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3RegexDpPlan_compose0(
        JNIEnv *env, jclass cls, jlongArray left, jlongArray right) {
    (void)cls;
    return compose_jni(env, left, right, compose_rows);
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3RegexDpPlan_composeStructured0(
        JNIEnv *env, jclass cls, jlongArray left, jlongArray right) {
    (void)cls;
    return compose_jni(env, left, right, compose_structured_rows);
}
