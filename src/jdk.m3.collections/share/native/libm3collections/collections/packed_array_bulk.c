/* SPDX-License-Identifier: Apache-2.0
 * Synexia packed-array companions. No critical arrays, global state, worker threads or native handles.
 * Never compile with -ffast-math: Java floating-point total ordering is part of the contract.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_array_bulk.c blob ec5f6abe8ba3 (com.synexia.common.collections.PackedArrayNative -> jdk.internal.m3.collections.packed.PackedArrayNative, 5 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "packed_selection_jni.h"
#include "jdk_internal_m3_collections_packed_PackedArrayNative.h"

static int alias(JNIEnv *env, jobject output, jobject a, jobject b) {
    if (ps_same(env, output, a) || ps_same(env, output, b)) {
        ps_fail(env, "java/lang/IllegalArgumentException", "output must not alias input"); return 1;
    }
    return 0;
}

#define BOUNDS(NAME, TYPE, ARRAY) \
JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_packed_PackedArrayNative_bounds##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY sorted, jint from, jint to, ARRAY queries, \
        jint qfrom, jint qto, jintArray result, jint offset, jboolean upper) { \
    (void)owner; \
    if (!ps_nonnull(env, sorted) || !ps_nonnull(env, queries) || !ps_nonnull(env, result)) { return; } \
    if (!ps_range(env, from, to, (*env)->GetArrayLength(env, sorted)) || \
        !ps_range(env, qfrom, qto, (*env)->GetArrayLength(env, queries)) || \
        !ps_space(env, offset, (int64_t)qto - qfrom, (*env)->GetArrayLength(env, result)) || \
        alias(env, result, sorted, queries)) { return; } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, sorted, NULL); \
    if (!ps_acquired(env, a)) { return; } \
    for (jint i = from + (from < to ? 1 : 0); i < to; i++) { \
        if (ps_compare_##NAME(a[i-1], a[i]) > 0) { \
            (*env)->Release##NAME##ArrayElements(env, sorted, a, JNI_ABORT); \
            ps_fail(env, "java/lang/IllegalArgumentException", "input is not sorted"); return; \
        } \
    } \
    int same = ps_same(env, sorted, queries); \
    /* Bounded transfers: no whole-query/output acquisition or retained JNI pointer. */ \
    TYPE queries_batch[256]; \
    jint result_batch[256]; \
    TYPE previous_query = (TYPE)0; \
    int coherent = 1, direction = 0; \
    jint previous = from, previous_advance = INT32_MAX; \
    for (jint base = qfrom; base < qto;) { \
        jint count = qto - base < 256 ? qto - base : 256; \
        const TYPE *q; \
        if (same) { q = a + base; } \
        else { \
            (*env)->Get##NAME##ArrayRegion(env, queries, base, count, queries_batch); \
            if ((*env)->ExceptionCheck(env)) { break; } \
            q = queries_batch; \
        } \
        if (base == qfrom) { \
            jint sample = count < 8 ? count : 8; \
            for (jint k = 1; k < sample; k++) { \
                int next = ps_compare_##NAME(q[k - 1], q[k]); \
                if (next != 0) { \
                    if (direction != 0 && direction != next) { coherent = 0; break; } \
                    direction = next; \
                } \
            } \
        } \
        for (jint k = 0; k < count; k++) { \
            if (!coherent || (base == qfrom && k == 0)) { \
                previous = ps_bound_##NAME(a, from, to, q[k], upper); \
            } else { \
                int next = ps_compare_##NAME(previous_query, q[k]); \
                if (next < 0) { \
                    jint start = previous; \
                    previous = previous_advance <= 16 \
                            ? ps_gallop_##NAME(a, previous, to, q[k], upper) \
                            : ps_bound_##NAME(a, previous, to, q[k], upper); \
                    previous_advance = previous - start; \
                } else if (next > 0) { \
                    previous = ps_bound_##NAME(a, from, previous, q[k], upper); \
                    previous_advance = INT32_MAX; \
                } \
            } \
            result_batch[k] = previous; \
            previous_query = q[k]; \
        } \
        (*env)->SetIntArrayRegion(env, result, offset + (base - qfrom), count, result_batch); \
        if ((*env)->ExceptionCheck(env)) { break; } \
        base += count; \
    } \
    (*env)->Release##NAME##ArrayElements(env, sorted, a, JNI_ABORT); \
}
BOUNDS(Byte, jbyte, jbyteArray)
BOUNDS(Short, jshort, jshortArray)
BOUNDS(Char, jchar, jcharArray)
BOUNDS(Int, jint, jintArray)
BOUNDS(Long, jlong, jlongArray)
BOUNDS(Float, jfloat, jfloatArray)
BOUNDS(Double, jdouble, jdoubleArray)
BOUNDS(Boolean, jboolean, jbooleanArray)

#define EQUAL_RANGE(NAME, TYPE, ARRAY) \
JNIEXPORT jlong JNICALL Java_jdk_internal_m3_collections_packed_PackedArrayNative_equalRange##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY sorted, jint from, jint to, TYPE value) { \
    (void)owner; \
    if (!ps_nonnull(env, sorted) || \
        !ps_range(env, from, to, (*env)->GetArrayLength(env, sorted))) { return 0; } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, sorted, NULL); \
    if (!ps_acquired(env, a)) { return 0; } \
    jint lower = ps_bound_##NAME(a, from, to, value, 0); \
    jint upper = lower < to && ps_compare_##NAME(a[lower], value) == 0 \
            ? ps_bound_##NAME(a, lower, to, value, 1) : lower; \
    (*env)->Release##NAME##ArrayElements(env, sorted, a, JNI_ABORT); \
    return ((jlong)lower << 32) | ((jlong)upper & 0xffffffffLL); \
}
EQUAL_RANGE(Byte, jbyte, jbyteArray)
EQUAL_RANGE(Short, jshort, jshortArray)
EQUAL_RANGE(Char, jchar, jcharArray)
EQUAL_RANGE(Int, jint, jintArray)
EQUAL_RANGE(Long, jlong, jlongArray)
EQUAL_RANGE(Float, jfloat, jfloatArray)
EQUAL_RANGE(Double, jdouble, jdoubleArray)
EQUAL_RANGE(Boolean, jboolean, jbooleanArray)

#define EQUAL_RANGES(NAME, TYPE, ARRAY) \
JNIEXPORT jint JNICALL Java_jdk_internal_m3_collections_packed_PackedArrayNative_equalRanges##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY sorted, jint from, jint to, ARRAY queries, \
        jint qfrom, jint qto, jlongArray result, jint offset) { \
    (void)owner; \
    if (!ps_nonnull(env, sorted) || !ps_nonnull(env, queries) || !ps_nonnull(env, result)) { return 0; } \
    if (!ps_range(env, from, to, (*env)->GetArrayLength(env, sorted)) || \
        !ps_range(env, qfrom, qto, (*env)->GetArrayLength(env, queries)) || \
        !ps_space(env, offset, (int64_t)qto - qfrom, (*env)->GetArrayLength(env, result)) || \
        alias(env, result, sorted, queries)) { return 0; } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, sorted, NULL); \
    if (!ps_acquired(env, a)) { return 0; } \
    for (jint i = from + (from < to ? 1 : 0); i < to; i++) { \
        if (ps_compare_##NAME(a[i - 1], a[i]) > 0) { \
            (*env)->Release##NAME##ArrayElements(env, sorted, a, JNI_ABORT); \
            ps_fail(env, "java/lang/IllegalArgumentException", "input is not sorted"); return 0; \
        } \
    } \
    int same = ps_same(env, sorted, queries); \
    TYPE queries_batch[256]; \
    jlong result_batch[256]; \
    jint written = 0; \
    for (jint base = qfrom; base < qto;) { \
        jint count = qto - base < 256 ? qto - base : 256; \
        const TYPE *q; \
        if (same) { q = a + base; } \
        else { \
            (*env)->Get##NAME##ArrayRegion(env, queries, base, count, queries_batch); \
            if ((*env)->ExceptionCheck(env)) { break; } \
            q = queries_batch; \
        } \
        for (jint k = 0; k < count; k++) { \
            jint lower = ps_bound_##NAME(a, from, to, q[k], 0); \
            jint upper = lower < to && ps_compare_##NAME(a[lower], q[k]) == 0 \
                    ? ps_bound_##NAME(a, lower, to, q[k], 1) : lower; \
            result_batch[k] = ((jlong)lower << 32) | ((jlong)upper & 0xffffffffLL); \
        } \
        (*env)->SetLongArrayRegion(env, result, offset + (base - qfrom), count, result_batch); \
        if ((*env)->ExceptionCheck(env)) { break; } \
        written += count; \
        base += count; \
    } \
    (*env)->Release##NAME##ArrayElements(env, sorted, a, JNI_ABORT); \
    return written; \
}
EQUAL_RANGES(Byte, jbyte, jbyteArray)
EQUAL_RANGES(Short, jshort, jshortArray)
EQUAL_RANGES(Char, jchar, jcharArray)
EQUAL_RANGES(Int, jint, jintArray)
EQUAL_RANGES(Long, jlong, jlongArray)
EQUAL_RANGES(Float, jfloat, jfloatArray)
EQUAL_RANGES(Double, jdouble, jdoubleArray)
EQUAL_RANGES(Boolean, jboolean, jbooleanArray)

JNIEXPORT jint JNICALL Java_jdk_internal_m3_collections_packed_PackedArrayNative_setOperation(
        JNIEnv *env, jclass owner, jlongArray left, jint afrom, jint ato,
        jlongArray right, jint bfrom, jint bto, jlongArray output, jint offset, jint operation) {
    (void)owner;
    if (!ps_nonnull(env, left) || !ps_nonnull(env, right) || !ps_nonnull(env, output)) { return 0; }
    if (!ps_range(env, afrom, ato, (*env)->GetArrayLength(env, left)) ||
        !ps_range(env, bfrom, bto, (*env)->GetArrayLength(env, right))) { return 0; }
    if (operation < 0 || operation > 2) {
        ps_fail(env, "java/lang/IllegalArgumentException", "operation"); return 0;
    }
    if (alias(env, output, left, right)) { return 0; }
    jlong *a = (*env)->GetLongArrayElements(env, left, NULL);
    if (!ps_acquired(env, a)) { return 0; }
    jlong *b = (*env)->GetLongArrayElements(env, right, NULL);
    if (!ps_acquired(env, b)) { (*env)->ReleaseLongArrayElements(env, left, a, JNI_ABORT); return 0; }
    jint answer = 0;
    if (!ps_sorted_Long(a, afrom, ato) || !ps_sorted_Long(b, bfrom, bto)) {
        ps_fail(env, "java/lang/IllegalArgumentException", "input is not sorted");
    } else {
        int64_t needed = ps_merge_Long(a, afrom, ato, b, bfrom, bto, NULL, 0, operation);
        if (needed > INT32_MAX) { ps_fail(env, "java/lang/IllegalArgumentException", "result exceeds array index limit"); }
        else if (ps_space(env, offset, needed, (*env)->GetArrayLength(env, output))) {
            jlong *out = (*env)->GetLongArrayElements(env, output, NULL);
            if (ps_acquired(env, out)) {
                answer = (jint)ps_merge_Long(a, afrom, ato, b, bfrom, bto, out, offset, operation);
                (*env)->ReleaseLongArrayElements(env, output, out, 0);
            }
        }
    }
    (*env)->ReleaseLongArrayElements(env, right, b, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, left, a, JNI_ABORT);
    return answer;
}
static void sift_down(jlong *heap, jint size, jint index, jboolean minimum) {
    jlong value = heap[index];
    jint half = size / 2;
    while (index < half) {
        jint child = index * 2 + 1, right = child + 1;
        if (right < size && (minimum ? heap[right] < heap[child] : heap[right] > heap[child])) { child = right; }
        if (!(minimum ? heap[child] < value : heap[child] > value)) { break; }
        heap[index] = heap[child]; index = child;
    }
    heap[index] = value;
}
JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_packed_PackedArrayNative_heapify(
        JNIEnv *env, jclass owner, jlongArray array, jint size, jboolean minimum) {
    (void)owner;
    if (!ps_nonnull(env, array) || !ps_space(env, 0, size, (*env)->GetArrayLength(env, array))) { return; }
    jlong *heap = (*env)->GetLongArrayElements(env, array, NULL);
    if (!ps_acquired(env, heap)) { return; }
    for (jint i = size / 2; i > 0; i--) { sift_down(heap, size, i - 1, minimum); }
    (*env)->ReleaseLongArrayElements(env, array, heap, 0);
}
