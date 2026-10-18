/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * Exact UTF-16 facts; borrowed direct memory, no retained JNI addresses or payload copies.
 */
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jint) == sizeof(uint32_t), "32-bit Java int required");

static jintArray metrics_fail(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
    return NULL;
}

static uint16_t metrics_unit(const unsigned char *bytes, size_t index, jboolean little) {
    const unsigned char *p = bytes + index * 2;
    return little ? (uint16_t) ((uint16_t) p[0] | (uint16_t) p[1] << 8)
                  : (uint16_t) ((uint16_t) p[0] << 8 | (uint16_t) p[1]);
}

JNIEXPORT jintArray JNICALL
Java_com_m3_file_M3TextMetrics_00024Native_directMetrics0(
        JNIEnv *env, jclass owner, jobject buffer, jint position, jint length,
        jboolean little, jint replacement) {
    (void) owner;
    if (buffer == NULL) return metrics_fail(env, "java/lang/NullPointerException", "buffer");
    if (position < 0 || length < 0 || replacement <= 0) {
        return metrics_fail(env, "java/lang/IllegalArgumentException", "invalid text metrics range");
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0) return NULL;
    if ((jlong) position + length > capacity
            || (uint64_t) position + (uint64_t) length > SIZE_MAX / 2) {
        return metrics_fail(env, "java/lang/IndexOutOfBoundsException", "direct UTF-16 range");
    }
    const unsigned char *base = (*env)->GetDirectBufferAddress(env, buffer);
    if (base == NULL) return NULL;
    const unsigned char *bytes = base + (size_t) position * 2;
    uint32_t hash = 0, power = 1;
    jlong utf8 = 0, points = 0, unpaired = 0;
    int pending_high = 0;
    for (jint i = 0; i < length; i++) {
        uint16_t current = metrics_unit(bytes, (size_t) i, little);
        hash = hash * UINT32_C(31) + current;
        power *= UINT32_C(31);
        if (pending_high) {
            if (current >= 0xdc00 && current <= 0xdfff) {
                utf8 += 4;
                points++;
                pending_high = 0;
                continue;
            }
            utf8 += replacement;
            points++;
            unpaired++;
            pending_high = 0;
        }
        if (current >= 0xd800 && current <= 0xdbff) {
            pending_high = 1;
        } else {
            points++;
            if (current >= 0xdc00 && current <= 0xdfff) {
                utf8 += replacement;
                unpaired++;
            } else {
                utf8 += current <= 0x7f ? 1 : current <= 0x7ff ? 2 : 3;
            }
        }
    }
    if (pending_high) { utf8 += replacement; points++; unpaired++; }
    if (utf8 > INT_MAX || points > INT_MAX || unpaired > INT_MAX) {
        return metrics_fail(env, "java/lang/ArithmeticException", "text metrics overflow");
    }
    jint values[8] = {length, (jint) utf8, (jint) points, (jint) unpaired, 0, 0,
        length == 0 ? -1 : (jint) metrics_unit(bytes, 0, little),
        length == 0 ? -1 : (jint) metrics_unit(bytes, (size_t) length - 1, little)};
    /* Preserve the exact overflowed Java int bits without signed C overflow. */
    memcpy(&values[4], &hash, sizeof(hash));
    memcpy(&values[5], &power, sizeof(power));
    jintArray result = (*env)->NewIntArray(env, 8);
    if (result != NULL) (*env)->SetIntArrayRegion(env, result, 0, 8, values);
    return result;
}
