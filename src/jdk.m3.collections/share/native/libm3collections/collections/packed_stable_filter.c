/* SPDX-License-Identifier: Apache-2.0
 * One mask bit per input. Preflight before acquisition/writes, safe left compaction only.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_stable_filter.c blob 5b2d9966a78c (com.synexia.common.collections.PackedSelectionNative -> jdk.internal.m3.collections.packed.PackedSelectionNative, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "packed_selection_jni.h"
#include <string.h>
#include "jdk_internal_m3_collections_packed_PackedSelectionNative.h"

#define PS_COMPACT(NAME, TYPE, ARRAY) \
JNIEXPORT jint JNICALL Java_jdk_internal_m3_collections_packed_PackedSelectionNative_compact##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY source, jint from, jint to, jlongArray decisions, ARRAY output, jint offset) { \
    (void)owner; \
    if (!ps_nonnull(env, source) || !ps_nonnull(env, output) || !ps_nonnull(env, decisions)) { return 0; } \
    if (!ps_range(env, from, to, (*env)->GetArrayLength(env, source)) || \
        !ps_space(env, offset, 0, (*env)->GetArrayLength(env, output))) { return 0; } \
    int same = ps_same(env, source, output); \
    if (ps_same(env, source, decisions) || ps_same(env, output, decisions) || (same && offset > from)) { \
        ps_fail(env, "java/lang/IllegalArgumentException", "unsafe alias"); return 0; \
    } \
    if (!ps_mask(env, decisions, to - from)) { return 0; } \
    if (from == to) { return 0; } \
    jlong *bits = (*env)->GetLongArrayElements(env, decisions, NULL); \
    if (!ps_acquired(env, bits)) { return 0; } \
    jint count = ps_count(bits, to - from); \
    if (!ps_space(env, offset, count, (*env)->GetArrayLength(env, output))) { \
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0; \
    } \
    if (count == 0) { (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0; } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, source, NULL); \
    if (!ps_acquired(env, a)) { (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0; } \
    TYPE *out = same ? a : (*env)->Get##NAME##ArrayElements(env, output, NULL); \
    if (!ps_acquired(env, out)) { \
        (*env)->Release##NAME##ArrayElements(env, source, a, JNI_ABORT); \
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0; \
    } \
    jint write = offset; \
    if (count == to - from) { \
        memmove(out + offset, a + from, (size_t)count * sizeof(TYPE)); \
    } else if (count > (to - from) / 16) { \
        for (jint i = from; i < to; i++) { if (ps_selected(bits, i - from)) { out[write++] = a[i]; } } \
    } else { \
        for (jint bit = ps_next_selected(bits, 0, to - from); bit < to - from; \
                bit = ps_next_selected(bits, bit + 1, to - from)) { out[write++] = a[from + bit]; } \
    } \
    if (same) { (*env)->Release##NAME##ArrayElements(env, source, a, 0); } \
    else { \
        (*env)->Release##NAME##ArrayElements(env, output, out, 0); \
        (*env)->Release##NAME##ArrayElements(env, source, a, JNI_ABORT); \
    } \
    (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return count; \
}
PS_COMPACT(Boolean, jboolean, jbooleanArray)
PS_COMPACT(Byte, jbyte, jbyteArray)
PS_COMPACT(Short, jshort, jshortArray)
PS_COMPACT(Char, jchar, jcharArray)
PS_COMPACT(Int, jint, jintArray)
PS_COMPACT(Long, jlong, jlongArray)
PS_COMPACT(Float, jfloat, jfloatArray)
PS_COMPACT(Double, jdouble, jdoubleArray)
