/* SPDX-License-Identifier: Apache-2.0
 * Checked JNI helpers and ordered/bitmap atoms shared by the existing packed companions.
 * JNI_ABORT is NOT rollback for pinned arrays. Complete validation/acquisition before writes.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_selection_jni.h blob ea029ad19580. JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#ifndef SYNEXIA_PACKED_SELECTION_JNI_H
#define SYNEXIA_PACKED_SELECTION_JNI_H
#include <jni.h>
#include <stdint.h>
#include <math.h>

static inline void ps_fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) { (void)(*env)->ThrowNew(env, cls, message); (*env)->DeleteLocalRef(env, cls); }
}
static inline int ps_nonnull(JNIEnv *env, jobject value) {
    if (value != NULL) { return 1; }
    ps_fail(env, "java/lang/NullPointerException", "array"); return 0;
}
static inline int ps_acquired(JNIEnv *env, const void *value) {
    if (value != NULL) { return 1; }
    if (!(*env)->ExceptionCheck(env)) { ps_fail(env, "java/lang/OutOfMemoryError", "JNI array acquisition"); }
    return 0;
}
static inline int ps_range(JNIEnv *env, jint from, jint to, jsize size) {
    if (from >= 0 && from <= to && to <= size) { return 1; }
    ps_fail(env, "java/lang/IndexOutOfBoundsException", "range"); return 0;
}
static inline int ps_space(JNIEnv *env, jint from, int64_t count, jsize size) {
    if (from >= 0 && from <= size && count >= 0 && count <= (int64_t)size - from) { return 1; }
    ps_fail(env, "java/lang/IndexOutOfBoundsException", "output capacity"); return 0;
}
static inline int ps_same(JNIEnv *env, jobject a, jobject b) {
    return b != NULL && (*env)->IsSameObject(env, a, b);
}
static inline int ps_mask(JNIEnv *env, jlongArray mask, jint count) {
    return ps_space(env, 0, ((int64_t)count + 63) / 64, (*env)->GetArrayLength(env, mask));
}
static inline int ps_selected(const jlong *bits, jint bit) {
    return (((uint64_t)bits[bit / 64] >> (bit % 64)) & UINT64_C(1)) != 0;
}
static inline jint ps_popcount(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (jint)__builtin_popcountll((unsigned long long)value);
#else
    jint result = 0;
    while (value != 0) { value &= value - UINT64_C(1); result++; }
    return result;
#endif
}
/* Nonzero input only; the caller checks before invoking compiler bit intrinsics. */
static inline jint ps_trailing_zeros(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (jint)__builtin_ctzll((unsigned long long)value);
#else
    jint result = 0;
    while ((value & UINT64_C(1)) == 0) { value >>= 1; result++; }
    return result;
#endif
}
static inline jint ps_count(const jlong *bits, jint count) {
    jint result = 0, whole = count / 64, tail = count % 64;
    for (jint word = 0; word < whole; word++) { result += ps_popcount((uint64_t)bits[word]); }
    if (tail != 0) { result += ps_popcount((uint64_t)bits[whole] & ((UINT64_C(1) << tail) - UINT64_C(1))); }
    return result;
}
static inline jint ps_next_selected(const jlong *bits, jint from, jint count) {
    if (from >= count) { return count; }
    jint word = from / 64, words = (jint)(((int64_t)count + 63) / 64);
    uint64_t value = (uint64_t)bits[word] & (UINT64_MAX << (from % 64));
    for (;;) {
        if (value != 0) {
            jint selected = word * 64 + ps_trailing_zeros(value);
            return selected < count ? selected : count;
        }
        if (++word == words) { return count; }
        value = (uint64_t)bits[word];
    }
}
#define PS_INTEGER(NAME, TYPE) \
static inline int ps_compare_##NAME(TYPE a, TYPE b) { return (a > b) - (a < b); }
PS_INTEGER(Boolean, jboolean)
PS_INTEGER(Byte, jbyte)
PS_INTEGER(Short, jshort)
PS_INTEGER(Char, jchar)
PS_INTEGER(Int, jint)
PS_INTEGER(Long, jlong)
static inline int ps_compare_Float(jfloat a, jfloat b) {
    if (a < b) { return -1; } if (a > b) { return 1; }
    if (isnan(a)) { return isnan(b) ? 0 : 1; } if (isnan(b)) { return -1; }
    return a == 0.0f && b == 0.0f ? (!!signbit(b)) - (!!signbit(a)) : 0;
}
static inline int ps_compare_Double(jdouble a, jdouble b) {
    if (a < b) { return -1; } if (a > b) { return 1; }
    if (isnan(a)) { return isnan(b) ? 0 : 1; } if (isnan(b)) { return -1; }
    return a == 0.0 && b == 0.0 ? (!!signbit(b)) - (!!signbit(a)) : 0;
}

/* Trusted sorted ranges. Query/merge owners complete validation before using these atoms. */
#define PS_ORDERED(NAME, TYPE) \
static inline jint ps_bound_##NAME(const TYPE *a, jint from, jint to, TYPE value, jboolean upper) { \
    while (from < to) { \
        jint mid = from + (to - from) / 2; \
        int c = ps_compare_##NAME(a[mid], value); \
        if (c < 0 || (upper && c == 0)) { from = mid + 1; } else { to = mid; } \
    } \
    return from; \
} \
static inline jint ps_gallop_##NAME(const TYPE *a, jint from, jint to, TYPE value, jboolean upper) { \
    if (from == to) { return to; } \
    int c = ps_compare_##NAME(a[from], value); \
    if (c > 0 || (!upper && c == 0)) { return from; } \
    jint low = from + 1; \
    for (int64_t step = 1; (int64_t)from + step < to; step = step * 2 + 1) { \
        jint probe = (jint)((int64_t)from + step); \
        c = ps_compare_##NAME(a[probe], value); \
        if (c > 0 || (!upper && c == 0)) { return ps_bound_##NAME(a, low, probe, value, upper); } \
        low = probe + 1; \
    } \
    return ps_bound_##NAME(a, low, to, value, upper); \
} \
static inline jint ps_after_equal_##NAME(const TYPE *a, jint index, jint to, TYPE value) { \
    jint next = index + 1; \
    if (next == to || ps_compare_##NAME(a[next], value) != 0) { return next; } \
    return ps_gallop_##NAME(a, next, to, value, JNI_TRUE); \
} \
static inline int ps_sorted_##NAME(const TYPE *a, jint from, jint to) { \
    for (jint i = from; i < to && i + 1 < to; i++) { \
        if (ps_compare_##NAME(a[i], a[i + 1]) > 0) { return 0; } \
    } \
    return 1; \
} \
static inline int64_t ps_merge_##NAME(const TYPE *a, jint i, jint ato, const TYPE *b, jint j, jint bto, \
        TYPE *out, jint offset, jint operation) { \
    int64_t count = 0; \
    while (i < ato || j < bto) { \
        if ((operation == 1 && (i == ato || j == bto)) || (operation == 2 && i == ato)) { break; } \
        TYPE value; int emit; \
        if (j == bto || (i < ato && ps_compare_##NAME(a[i], b[j]) < 0)) { \
            if (operation == 1) { i = ps_gallop_##NAME(a, i + 1, ato, b[j], JNI_FALSE); continue; } \
            value = a[i]; emit = operation != 1; \
            i = ps_after_equal_##NAME(a, i, ato, value); \
        } else if (i == ato || ps_compare_##NAME(b[j], a[i]) < 0) { \
            if (operation == 1 || operation == 2) { j = ps_gallop_##NAME(b, j + 1, bto, a[i], JNI_FALSE); continue; } \
            value = b[j]; emit = operation == 0 || operation == 3; \
            j = ps_after_equal_##NAME(b, j, bto, value); \
        } else { \
            value = a[i]; emit = operation == 0 || operation == 1; \
            i = ps_after_equal_##NAME(a, i, ato, value); \
            j = ps_after_equal_##NAME(b, j, bto, value); \
        } \
        if (emit) { if (out != NULL) { out[offset + (jint)count] = value; } count++; } \
    } \
    return count; \
}
PS_ORDERED(Boolean, jboolean)
PS_ORDERED(Byte, jbyte)
PS_ORDERED(Short, jshort)
PS_ORDERED(Char, jchar)
PS_ORDERED(Int, jint)
PS_ORDERED(Long, jlong)
PS_ORDERED(Float, jfloat)
PS_ORDERED(Double, jdouble)
#undef PS_ORDERED
#endif
