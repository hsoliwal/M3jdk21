// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-20261031/fixtures/m3jni.cpp blob 2a857279c571; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
#include <jni.h>
#include <bit>
#include <cstdint>

extern "C" JNIEXPORT jlong JNICALL
Java_jdk_m3_tooling_atomjit_fixture_AtomJitProbe_cppFold(JNIEnv *env, jclass owner, jintArray values) {
    (void) owner;
    if (values == nullptr) return 0;
    const jsize count = env->GetArrayLength(values);
    jint *data = env->GetIntArrayElements(values, nullptr);
    if (data == nullptr) return 0;
    std::uint64_t total = 0;
    for (jsize i = 0; i < count; ++i) {
        const std::uint64_t element = static_cast<std::uint64_t>(static_cast<std::int64_t>(data[i]));
        total += element * element;
    }
    env->ReleaseIntArrayElements(values, data, JNI_ABORT);
    return std::bit_cast<jlong>(total);
}
