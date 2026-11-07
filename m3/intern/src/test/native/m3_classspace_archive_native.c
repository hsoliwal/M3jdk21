/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-immutable-archive-20261030/m3_classspace_archive_native.c blob 26e0b4772a4e; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
#include "jdk_m3_intern_M3ClassSpaceArchiveNative.h"
#include <stdint.h>
#include <stddef.h>
#include <string.h>

static void reject(JNIEnv* env, const char* message) {
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_intern_M3ClassSpaceArchiveNative_fnv64Region(
    JNIEnv* env, jclass owner, jobject buffer, jint offset, jint length) {
    (void)owner;
    if (buffer == NULL) {
        reject(env, "Direct byte buffer required");
        return (jlong)0;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    const unsigned char* bytes = (const unsigned char*)(*env)->GetDirectBufferAddress(env, buffer);
    if (capacity < 0 || bytes == NULL || offset < 0 || length < 0
            || (jlong)offset > capacity || (jlong)length > capacity - (jlong)offset) {
        reject(env, "Invalid direct buffer region");
        return (jlong)0;
    }
    uint64_t hash = UINT64_C(0xcbf29ce484222325);
    for (jint i = 0; i < length; i++) {
        hash ^= (uint64_t)bytes[(size_t)offset + (size_t)i];
        hash *= UINT64_C(0x100000001b3);
    }
    /* Preserve every 64-bit bit pattern without an implementation-defined
     * out-of-range unsigned-to-signed conversion. JNI jlong is 64-bit. */
    _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI jlong size mismatch");
    jlong signed_bits;
    memcpy(&signed_bits, &hash, sizeof(signed_bits));
    return signed_bits;
}
