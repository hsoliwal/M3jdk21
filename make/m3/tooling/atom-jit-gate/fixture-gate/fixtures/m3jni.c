// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-20261031/fixtures/m3jni.c blob 91209081cc41; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
#include <jni.h>
#include <stdint.h>

JNIEXPORT jlong JNICALL
Java_jdk_m3_tooling_atomjit_fixture_AtomJitProbe_sum(JNIEnv *env, jclass owner, jintArray values) {
    (void) owner;
    if (values == NULL) return 0; /* fixture's declared behavior, not a JDK contract */
    jsize count = (*env)->GetArrayLength(env, values);
    jint *data = (*env)->GetIntArrayElements(env, values, NULL);
    if (data == NULL) return 0; /* pending OOME is propagated by JNI */
    jlong total = 0;
    for (jsize i = 0; i < count; i++) total += (jlong)data[i];
    (*env)->ReleaseIntArrayElements(env, values, data, JNI_ABORT);
    return total;
}
