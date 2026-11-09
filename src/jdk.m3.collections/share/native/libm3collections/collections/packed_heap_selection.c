/* SPDX-License-Identifier: Apache-2.0
 * Ranged primitive heap algorithms; signed-zero/NaN order requires NO -ffast-math.
 * Existing Synexia hole-sifting pattern, generalized to the primitive/ranged contract.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_heap_selection.c blob 0e73a23f85b3 (com.synexia.common.collections.PackedSelectionNative -> jdk.internal.m3.collections.packed.PackedSelectionNative, 3 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "packed_selection_jni.h"
#include "jdk_internal_m3_collections_packed_PackedSelectionNative.h"

#define PS_HEAP(NAME, TYPE, ARRAY) \
static int ps_before_##NAME(TYPE a, TYPE b, jboolean minimum) { \
    int comparison = ps_compare_##NAME(a, b); return minimum ? comparison < 0 : comparison > 0; \
} \
static void ps_down_##NAME(TYPE *values, jint from, jint size, jint index, jboolean minimum) { \
    TYPE value = values[from + index]; \
    while (index < size / 2) { \
        jint child = index * 2 + 1, right = child + 1; \
        if (right < size && ps_before_##NAME(values[from + right], values[from + child], minimum)) { child = right; } \
        if (!ps_before_##NAME(values[from + child], value, minimum)) { break; } \
        values[from + index] = values[from + child]; index = child; \
    } \
    values[from + index] = value; \
} \
static void ps_heap_##NAME(TYPE *values, jint from, jint size, jboolean minimum) { \
    for (jint i = size / 2; i > 0; i--) { ps_down_##NAME(values, from, size, i - 1, minimum); } \
} \
JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_packed_PackedSelectionNative_heapify##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY array, jint from, jint to, jboolean minimum) { \
    (void)owner; \
    if (!ps_nonnull(env, array) || !ps_range(env, from, to, (*env)->GetArrayLength(env, array))) { return; } \
    if (to - from < 2) { return; } \
    TYPE *values = (*env)->Get##NAME##ArrayElements(env, array, NULL); \
    if (!ps_acquired(env, values)) { return; } \
    ps_heap_##NAME(values, from, to - from, minimum); \
    (*env)->Release##NAME##ArrayElements(env, array, values, 0); \
} \
JNIEXPORT void JNICALL Java_jdk_internal_m3_collections_packed_PackedSelectionNative_sort##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY array, jint from, jint to) { \
    (void)owner; \
    if (!ps_nonnull(env, array) || !ps_range(env, from, to, (*env)->GetArrayLength(env, array))) { return; } \
    jint size = to - from; if (size < 2) { return; } \
    TYPE *values = (*env)->Get##NAME##ArrayElements(env, array, NULL); \
    if (!ps_acquired(env, values)) { return; } \
    ps_heap_##NAME(values, from, size, JNI_FALSE); \
    while (size > 1) { \
        TYPE value = values[from]; values[from] = values[from + size - 1]; values[from + size - 1] = value; \
        size--; ps_down_##NAME(values, from, size, 0, JNI_FALSE); \
    } \
    (*env)->Release##NAME##ArrayElements(env, array, values, 0); \
} \
JNIEXPORT jint JNICALL Java_jdk_internal_m3_collections_packed_PackedSelectionNative_topK##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY source, jint from, jint to, jlongArray decisions, \
        ARRAY output, jint offset, jint limit, jboolean largest) { \
    (void)owner; \
    if (!ps_nonnull(env, source) || !ps_nonnull(env, output)) { return 0; } \
    if (!ps_range(env, from, to, (*env)->GetArrayLength(env, source)) || \
        !ps_space(env, offset, limit, (*env)->GetArrayLength(env, output))) { return 0; } \
    if (ps_same(env, source, output) || ps_same(env, source, decisions) || ps_same(env, output, decisions)) { \
        ps_fail(env, "java/lang/IllegalArgumentException", "unsafe alias"); return 0; \
    } \
    if (decisions != NULL && !ps_mask(env, decisions, to - from)) { return 0; } \
    if (limit == 0 || from == to) { return 0; } \
    jlong *bits = decisions == NULL ? NULL : (*env)->GetLongArrayElements(env, decisions, NULL); \
    if (decisions != NULL && !ps_acquired(env, bits)) { return 0; } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, source, NULL); \
    if (!ps_acquired(env, a)) { \
        if (bits != NULL) { (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); } return 0; \
    } \
    TYPE *out = (*env)->Get##NAME##ArrayElements(env, output, NULL); \
    if (!ps_acquired(env, out)) { \
        (*env)->Release##NAME##ArrayElements(env, source, a, JNI_ABORT); \
        if (bits != NULL) { (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); } return 0; \
    } \
    jint admitted = bits == NULL ? to - from : ps_count(bits, to - from); \
    int all = admitted == to - from, sparse = admitted <= (to - from) / 16; \
    jint size = 0; \
    for (jint i = from; i < to; i++) { \
        if (!all && !ps_selected(bits, i - from)) { \
            if (!sparse) { continue; } \
            jint bit = ps_next_selected(bits, i - from, to - from); \
            if (bit == to - from) { break; } \
            i = from + bit; \
        } \
        TYPE value = a[i]; \
        if (size < limit) { \
            out[offset + size] = value; size++; \
            if (size == limit) { ps_heap_##NAME(out, offset, size, largest); } \
        } else { \
            int comparison = ps_compare_##NAME(value, out[offset]); \
            if (largest ? comparison > 0 : comparison < 0) { \
                out[offset] = value; ps_down_##NAME(out, offset, size, 0, largest); \
            } \
        } \
    } \
    if (size < limit) { ps_heap_##NAME(out, offset, size, largest); } \
    (*env)->Release##NAME##ArrayElements(env, output, out, 0); \
    (*env)->Release##NAME##ArrayElements(env, source, a, JNI_ABORT); \
    if (bits != NULL) { (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); } \
    return size; \
}
PS_HEAP(Boolean, jboolean, jbooleanArray)
PS_HEAP(Byte, jbyte, jbyteArray)
PS_HEAP(Short, jshort, jshortArray)
PS_HEAP(Char, jchar, jcharArray)
PS_HEAP(Int, jint, jintArray)
PS_HEAP(Long, jlong, jlongArray)
PS_HEAP(Float, jfloat, jfloatArray)
PS_HEAP(Double, jdouble, jdoubleArray)
