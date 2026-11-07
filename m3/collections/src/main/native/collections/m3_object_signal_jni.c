/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor blob 4c3bcedc7980 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

static unsigned trailing_zero_u64(uint64_t value) {
    unsigned count = 0;
    while ((value & UINT64_C(1)) == 0) {
        value >>= 1;
        count++;
    }
    return count;
}

JNIEXPORT void JNICALL
Java_jdk_internal_m3_collections_M3ObjectSignalOps_applyFeatureMasksNative(
        JNIEnv *env,
        jclass type,
        jlongArray planes_array,
        jint word_count,
        jlong required_bits,
        jlong forbidden_bits,
        jlongArray selected_array) {
    (void)type;
    if (planes_array == NULL || selected_array == NULL || word_count < 0) return;

    const jsize plane_length = (*env)->GetArrayLength(env, planes_array);
    const jsize selected_length = (*env)->GetArrayLength(env, selected_array);
    const int64_t expected_planes = (int64_t)word_count * 64;
    if (expected_planes > INT32_MAX
            || plane_length != (jsize)expected_planes
            || selected_length != word_count) return;

    jlong *planes = (*env)->GetPrimitiveArrayCritical(env, planes_array, NULL);
    if (planes == NULL) return;
    jlong *selected = (*env)->GetPrimitiveArrayCritical(env, selected_array, NULL);
    if (selected == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, planes_array, planes, JNI_ABORT);
        return;
    }

    uint64_t required = (uint64_t)required_bits;
    while (required != 0) {
        const unsigned feature = trailing_zero_u64(required);
        const size_t base = (size_t)feature * (size_t)word_count;
        for (jint word = 0; word < word_count; word++) {
            selected[word] = (jlong)(
                (uint64_t)selected[word] & (uint64_t)planes[base + (size_t)word]);
        }
        required &= required - 1;
    }

    uint64_t forbidden = (uint64_t)forbidden_bits;
    while (forbidden != 0) {
        const unsigned feature = trailing_zero_u64(forbidden);
        const size_t base = (size_t)feature * (size_t)word_count;
        for (jint word = 0; word < word_count; word++) {
            selected[word] = (jlong)(
                (uint64_t)selected[word] & ~(uint64_t)planes[base + (size_t)word]);
        }
        forbidden &= forbidden - 1;
    }

    (*env)->ReleasePrimitiveArrayCritical(env, selected_array, selected, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, planes_array, planes, JNI_ABORT);
}
