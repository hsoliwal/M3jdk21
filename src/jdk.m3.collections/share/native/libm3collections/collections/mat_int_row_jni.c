/* SPDX-License-Identifier: Apache-2.0 */
/* Lineage: com.synexia 00bd2142b0ce synexia-mat-value/src/main/native/mat_int_row_jni.c blob 1fd42eeb33de (com.synexia.matvalue.MatIntRowKernel -> jdk.internal.m3.collections.mat.MatIntRowKernel, 2 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "jdk_internal_m3_collections_mat_MatIntRowKernel.h"
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jint) == 4, "jint must have 32 bits");
_Static_assert(sizeof(jlong) == 8, "jlong must have 64 bits");
_Static_assert(INT64_MIN == -INT64_MAX - INT64_C(1), "signed 64-bit two's complement required");

enum { MAT_ROW_CHUNK = 512 };

JNIEXPORT jlong JNICALL Java_jdk_internal_m3_collections_mat_MatIntRowKernel_hashNative0(
        JNIEnv *env, jclass owner, jintArray values) {
    (void) owner;
    if (values == NULL) return (jlong) 0; /* Java wrapper validates non-null */
    const jsize length = (*env)->GetArrayLength(env, values);
    if ((*env)->ExceptionCheck(env)) return (jlong) 0;
    uint64_t result = (UINT64_C(0xcbf29ce484222325) ^ (uint64_t) (uint32_t) length)
            * UINT64_C(0x100000001b3);
    jint buffer[MAT_ROW_CHUNK];
    for (jsize offset = 0; offset < length; ) {
        const jsize count = length - offset < MAT_ROW_CHUNK
                ? length - offset : MAT_ROW_CHUNK;
        (*env)->GetIntArrayRegion(env, values, offset, count, buffer);
        if ((*env)->ExceptionCheck(env)) return (jlong) 0;
        for (jsize index = 0; index < count; index++) {
            const uint32_t raw = (uint32_t) buffer[index];
            for (int shift = 0; shift < 32; shift += 8) {
                result = (result ^ (uint64_t) ((raw >> shift) & UINT32_C(0xff)))
                        * UINT64_C(0x100000001b3);
            }
        }
        offset += count;
    }
    /* Preserve all 64 FNV bits; out-of-range unsigned-to-signed casts are
     * implementation-defined in C17. Both widths are verified above. */
    int64_t signed_result;
    memcpy(&signed_result, &result, sizeof(signed_result));
    return (jlong) signed_result;
}

JNIEXPORT jboolean JNICALL Java_jdk_internal_m3_collections_mat_MatIntRowKernel_equalNative0(
        JNIEnv *env, jclass owner, jintArray left, jint left_offset,
        jintArray right, jint right_offset, jint length) {
    (void) owner;
    if (left == NULL || right == NULL || length < 0 || left_offset < 0
            || right_offset < 0) return JNI_FALSE;
    const jsize left_length = (*env)->GetArrayLength(env, left);
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    const jsize right_length = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    if (left_offset > left_length || length > left_length - left_offset
            || right_offset > right_length || length > right_length - right_offset) {
        return JNI_FALSE;
    }
    jint lhs[MAT_ROW_CHUNK];
    jint rhs[MAT_ROW_CHUNK];
    for (jsize offset = 0; offset < length; ) {
        const jsize count = length - offset < MAT_ROW_CHUNK
                ? length - offset : MAT_ROW_CHUNK;
        (*env)->GetIntArrayRegion(env, left, left_offset + offset, count, lhs);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        (*env)->GetIntArrayRegion(env, right, right_offset + offset, count, rhs);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        if (memcmp(lhs, rhs, (size_t) count * sizeof(jint)) != 0) return JNI_FALSE;
        offset += count;
    }
    return JNI_TRUE;
}
