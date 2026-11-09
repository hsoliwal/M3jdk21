/* SPDX-License-Identifier: Apache-2.0
 * Sorted set algebra; exact count before output acquisition. NaNs compare equal; -0 != +0.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_sorted_sets.c blob 0a25a1d20224 (com.synexia.common.collections.PackedSelectionNative -> jdk.internal.m3.collections.packed.PackedSelectionNative, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "packed_selection_jni.h"
#include "jdk_internal_m3_collections_packed_PackedSelectionNative.h"

#define PS_SET(NAME, TYPE, ARRAY) \
JNIEXPORT jint JNICALL Java_jdk_internal_m3_collections_packed_PackedSelectionNative_combine##NAME##0( \
        JNIEnv *env, jclass owner, ARRAY left, jint afrom, jint ato, ARRAY right, jint bfrom, jint bto, \
        ARRAY output, jint offset, jint operation) { \
    (void)owner; \
    if (!ps_nonnull(env, left) || !ps_nonnull(env, right) || !ps_nonnull(env, output)) { return 0; } \
    if (!ps_range(env, afrom, ato, (*env)->GetArrayLength(env, left)) || \
        !ps_range(env, bfrom, bto, (*env)->GetArrayLength(env, right))) { return 0; } \
    if (operation < 0 || operation > 3) { ps_fail(env, "java/lang/IllegalArgumentException", "operation"); return 0; } \
    if (ps_same(env, output, left) || ps_same(env, output, right)) { \
        ps_fail(env, "java/lang/IllegalArgumentException", "unsafe alias"); return 0; \
    } \
    TYPE *a = (*env)->Get##NAME##ArrayElements(env, left, NULL); \
    if (!ps_acquired(env, a)) { return 0; } \
    TYPE *b = (*env)->Get##NAME##ArrayElements(env, right, NULL); \
    if (!ps_acquired(env, b)) { (*env)->Release##NAME##ArrayElements(env, left, a, JNI_ABORT); return 0; } \
    jint result = 0; \
    if (!ps_sorted_##NAME(a, afrom, ato) || !ps_sorted_##NAME(b, bfrom, bto)) { \
        ps_fail(env, "java/lang/IllegalArgumentException", "unsorted input"); \
    } else { \
        int64_t count = ps_merge_##NAME(a, afrom, ato, b, bfrom, bto, NULL, 0, operation); \
        if (count > INT32_MAX) { ps_fail(env, "java/lang/IllegalArgumentException", "result too large"); } \
        else if (ps_space(env, offset, count, (*env)->GetArrayLength(env, output))) { \
            if (count != 0) { \
                TYPE *out = (*env)->Get##NAME##ArrayElements(env, output, NULL); \
                if (ps_acquired(env, out)) { \
                    result = (jint)ps_merge_##NAME(a, afrom, ato, b, bfrom, bto, out, offset, operation); \
                    (*env)->Release##NAME##ArrayElements(env, output, out, 0); \
                } \
            } \
        } \
    } \
    (*env)->Release##NAME##ArrayElements(env, right, b, JNI_ABORT); \
    (*env)->Release##NAME##ArrayElements(env, left, a, JNI_ABORT); return result; \
}
PS_SET(Boolean, jboolean, jbooleanArray)
PS_SET(Byte, jbyte, jbyteArray)
PS_SET(Short, jshort, jshortArray)
PS_SET(Char, jchar, jcharArray)
PS_SET(Int, jint, jintArray)
PS_SET(Long, jlong, jlongArray)
PS_SET(Float, jfloat, jfloatArray)
PS_SET(Double, jdouble, jdoubleArray)
