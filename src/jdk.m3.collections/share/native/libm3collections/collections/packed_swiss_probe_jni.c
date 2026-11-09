/* SPDX-License-Identifier: Apache-2.0
 *
 * Batch JNI peer for Synexia PackedSwissProbeAtoms.
 *
 * The 8-byte portable control-word classification is mechanically adapted from
 * Abseil SwissTable GroupPortableImpl at
 * 33a35cf45087dffab22c0c9ae817021a45e93a47 (Apache-2.0).
 * Synexia retains its own table layout, hashing, growth, deletion and Java API.
 *
 * No critical arrays, native handles, global state, callbacks or retained JNI pointers.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/packed_swiss_probe_jni.c blob d40ae1f86041 (com.synexia.common.collections.PackedSwissNative -> jdk.internal.m3.collections.packed.PackedSwissNative, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>
#include <stdint.h>

#define PSW_GROUP_SIZE 8
#define PSW_MASKS_PER_GROUP 4
#define PSW_LSBS UINT64_C(0x0101010101010101)
#define PSW_MSBS UINT64_C(0x8080808080808080)

static void psw_fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (void)(*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static uint64_t psw_load64le(const jbyte *source) {
    const uint8_t *p = (const uint8_t *)source;
    return ((uint64_t)p[0])
            | ((uint64_t)p[1] << 8)
            | ((uint64_t)p[2] << 16)
            | ((uint64_t)p[3] << 24)
            | ((uint64_t)p[4] << 32)
            | ((uint64_t)p[5] << 40)
            | ((uint64_t)p[6] << 48)
            | ((uint64_t)p[7] << 56);
}

static uint64_t psw_match(uint64_t control, uint8_t fingerprint) {
    uint64_t x = control ^ (PSW_LSBS * (uint64_t)fingerprint);
    return (x - PSW_LSBS) & ~x & PSW_MSBS;
}

static uint64_t psw_empty(uint64_t control) {
    return (control & ~(control << 6)) & PSW_MSBS;
}

static uint64_t psw_empty_or_deleted(uint64_t control) {
    return (control & ~(control << 7)) & PSW_MSBS;
}

static uint64_t psw_full(uint64_t control) {
    return (control ^ PSW_MSBS) & PSW_MSBS;
}

JNIEXPORT void JNICALL
Java_jdk_internal_m3_collections_packed_PackedSwissNative_classifyGroups0(
        JNIEnv *env,
        jclass owner,
        jbyteArray controls,
        jint from_group,
        jint group_count,
        jbyte fingerprint,
        jlongArray output,
        jint output_from) {
    (void)owner;
    if (controls == NULL || output == NULL) {
        psw_fail(env, "java/lang/NullPointerException", "array");
        return;
    }
    if (from_group < 0 || group_count < 0 || output_from < 0) {
        psw_fail(env, "java/lang/IndexOutOfBoundsException", "negative range");
        return;
    }

    int64_t byte_from = (int64_t)from_group * PSW_GROUP_SIZE;
    int64_t byte_count = (int64_t)group_count * PSW_GROUP_SIZE;
    int64_t output_count = (int64_t)group_count * PSW_MASKS_PER_GROUP;
    jsize controls_length = (*env)->GetArrayLength(env, controls);
    jsize output_length = (*env)->GetArrayLength(env, output);
    if (byte_from > controls_length
            || byte_count > (int64_t)controls_length - byte_from
            || output_from > output_length
            || output_count > (int64_t)output_length - output_from) {
        psw_fail(env, "java/lang/IndexOutOfBoundsException", "range");
        return;
    }
    if (group_count == 0) {
        return;
    }

    jbyte *control = (*env)->GetByteArrayElements(env, controls, NULL);
    if (control == NULL) {
        if (!(*env)->ExceptionCheck(env)) {
            psw_fail(env, "java/lang/OutOfMemoryError", "control array acquisition");
        }
        return;
    }
    jlong *result = (*env)->GetLongArrayElements(env, output, NULL);
    if (result == NULL) {
        (*env)->ReleaseByteArrayElements(env, controls, control, JNI_ABORT);
        if (!(*env)->ExceptionCheck(env)) {
            psw_fail(env, "java/lang/OutOfMemoryError", "output array acquisition");
        }
        return;
    }

    uint8_t h2 = (uint8_t)fingerprint & UINT8_C(0x7f);
    for (jint group = 0; group < group_count; group++) {
        const jbyte *at = control + byte_from + (int64_t)group * PSW_GROUP_SIZE;
        uint64_t word = psw_load64le(at);
        int64_t out = (int64_t)output_from + (int64_t)group * PSW_MASKS_PER_GROUP;
        result[out] = (jlong)psw_match(word, h2);
        result[out + 1] = (jlong)psw_empty(word);
        result[out + 2] = (jlong)psw_empty_or_deleted(word);
        result[out + 3] = (jlong)psw_full(word);
    }

    (*env)->ReleaseLongArrayElements(env, output, result, 0);
    (*env)->ReleaseByteArrayElements(env, controls, control, JNI_ABORT);
}
