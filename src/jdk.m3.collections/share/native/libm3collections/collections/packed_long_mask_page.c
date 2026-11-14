/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * One bounded page from a caller-owned decision mask. Java remains the oracle.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_long_mask_page.c blob b65f8a0aaae4 (com.synexia.common.collections.PackedLongMaskPageNative -> jdk.internal.m3.collections.packed.PackedLongMaskPageNative, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include "packed_selection_jni.h"
#include "jdk_internal_m3_collections_packed_PackedLongMaskPageNative.h"

JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_packed_PackedLongMaskPageNative_copyPage0(
        JNIEnv *env, jclass owner, jlongArray source, jint from, jint to,
        jlongArray decisions, jint skip, jint limit, jlongArray output, jint offset) {
    (void)owner;
    if (!ps_nonnull(env, source) || !ps_nonnull(env, output)
            || !ps_nonnull(env, decisions)) { return 0; }
    if (!ps_range(env, from, to, (*env)->GetArrayLength(env, source))
            || !ps_space(env, offset, 0, (*env)->GetArrayLength(env, output))) { return 0; }
    if (skip < 0 || limit < 0) {
        ps_fail(env, "java/lang/IndexOutOfBoundsException", "negative skip or limit"); return 0;
    }
    const int same = ps_same(env, source, output);
    if (ps_same(env, source, decisions) || ps_same(env, output, decisions)
            || (same && offset > from)) {
        ps_fail(env, "java/lang/IllegalArgumentException", "unsafe alias"); return 0;
    }
    jint range = to - from;
    if (!ps_mask(env, decisions, range)) { return 0; }
    if (range == 0) { return 0; }
    jlong *bits = (*env)->GetLongArrayElements(env, decisions, NULL);
    if (!ps_acquired(env, bits)) { return 0; }
    jint accepted = ps_count(bits, range);
    jint needed = skip >= accepted ? 0 : (accepted - skip < limit ? accepted - skip : limit);
    if (!ps_space(env, offset, needed, (*env)->GetArrayLength(env, output))) {
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0;
    }
    if (needed == 0) {
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0;
    }
    jlong *a = (*env)->GetLongArrayElements(env, source, NULL);
    if (!ps_acquired(env, a)) {
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0;
    }
    jlong *out = same ? a : (*env)->GetLongArrayElements(env, output, NULL);
    if (!ps_acquired(env, out)) {
        (*env)->ReleaseLongArrayElements(env, source, a, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT); return 0;
    }
    jint remaining = skip, written = 0;
    jint words = (jint)(((int64_t)range + 63) / 64);
    for (jint word = 0; word < words && written < needed; word++) {
        uint64_t lane = (uint64_t)bits[word];
        jint width = range - (word << 6);
        if (width < 64) { lane &= (UINT64_C(1) << width) - UINT64_C(1); }
        jint available = ps_popcount(lane);
        if (remaining >= available) { remaining -= available; continue; }
        while (remaining > 0) { lane &= lane - UINT64_C(1); remaining--; }
        while (lane != 0 && written < needed) {
            jint bit = (word << 6) + ps_trailing_zeros(lane);
            out[offset + written++] = a[from + bit];
            lane &= lane - UINT64_C(1);
        }
    }
    if (same) { (*env)->ReleaseLongArrayElements(env, source, a, 0); }
    else {
        (*env)->ReleaseLongArrayElements(env, output, out, 0);
        (*env)->ReleaseLongArrayElements(env, source, a, JNI_ABORT);
    }
    (*env)->ReleaseLongArrayElements(env, decisions, bits, JNI_ABORT);
    return written;
}
