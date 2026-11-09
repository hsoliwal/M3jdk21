/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/primitive_order_jni.c blob 284c1d81169b (com.synexia.primitives.PrimitivePartialSort$LongOrder -> jdk.m3.collections.PrimitivePartialSort$LongOrder, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
/* Bounded direct-buffer execution of PrimitivePartialSort.LongOrder's exact state machine.
 * No retained native pointers, callbacks during work, primitive-array pinning or hidden loading.
 * Explicit little-endian loads avoid alignment, host-endian and strict-aliasing assumptions.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include "jdk_m3_collections_PrimitivePartialSort_LongOrder.h"

static uint32_t u32(const unsigned char *p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}
static uint64_t u64(const unsigned char *p) { return (uint64_t)u32(p) | ((uint64_t)u32(p + 4) << 32); }
static void p32(unsigned char *p, uint32_t v) {
    p[0] = (unsigned char)v; p[1] = (unsigned char)(v >> 8);
    p[2] = (unsigned char)(v >> 16); p[3] = (unsigned char)(v >> 24);
}
static void p64(unsigned char *p, uint64_t v) { p32(p, (uint32_t)v); p32(p + 4, (uint32_t)(v >> 32)); }
static jint fail(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) { (*env)->ThrowNew(env, exception, message); (*env)->DeleteLocalRef(env, exception); }
    return 0;
}
#define BAD(m) fail(env, "java/lang/IllegalArgumentException", m)
#define CORRUPT(m) fail(env, "java/lang/IllegalStateException", m)

JNIEXPORT jint JNICALL Java_jdk_m3_collections_PrimitivePartialSort_00024LongOrder_nativeAdvance
    (JNIEnv *env, jclass type, jobject buffer, jint requested, jint budget) {
    (void)type;
    if (buffer == NULL || requested < 0 || budget < 0 || budget > 256) return BAD("native arguments");
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 64 || capacity > INT32_MAX) return BAD("native direct capacity");
    jclass buffer_type = (*env)->GetObjectClass(env, buffer);
    if (buffer_type == NULL) return 0;
    jmethodID read_only = (*env)->GetMethodID(env, buffer_type, "isReadOnly", "()Z");
    if (read_only == NULL) { (*env)->DeleteLocalRef(env, buffer_type); return 0; }
    jboolean readonly = (*env)->CallBooleanMethod(env, buffer, read_only);
    (*env)->DeleteLocalRef(env, buffer_type);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (readonly) return BAD("read-only direct image");
    unsigned char *p = (*env)->GetDirectBufferAddress(env, buffer);
    if (p == NULL) return BAD("native direct address");
    uint32_t n = u32(p + 4), base = u32(p + 8), direction = u32(p + 12);
    uint32_t build = u32(p + 16), repair = u32(p + 20), ready = u32(p + 24);
    uint32_t count = u32(p + 28), limit = u32(p + 32), from = u32(p + 36);
    uint64_t tree64 = 64 + 8 * (uint64_t)n;
    uint64_t prefix64 = tree64 + 8 * (uint64_t)base;
    uint64_t tape64 = prefix64 + 4 * (uint64_t)n;
    uint64_t bytes = tape64 + 12 * (uint64_t)limit;
    if (u32(p) != UINT32_C(0x4d334f31) || base == 0 || (base & (base - 1)) != 0
            || n > base || (base > 1 && n <= base / 2) || direction > 1
            || build >= base || repair >= base || (build != 0 && repair != 0)
            || ready > n || count > limit || (uint64_t)from + n > INT32_MAX
            || (uint32_t)requested > n || bytes != (uint64_t)capacity) return BAD("native image geometry");
    size_t tree = (size_t)tree64, prefix = (size_t)prefix64, tape = (size_t)tape64;
    jint steps = 0;
    while (ready < (uint32_t)requested && steps < budget) {
        uint32_t node = build != 0 ? build : repair;
        if (node != 0) {
            uint32_t left = u32(p + tree + 8 * (size_t)node);
            uint32_t right = u32(p + tree + 8 * (size_t)node + 4), winner;
            if ((left != UINT32_MAX && left >= n) || (right != UINT32_MAX && right >= n))
                return CORRUPT("native child index");
            if (left == UINT32_MAX) winner = right;
            else if (right == UINT32_MAX) winner = left;
            else {
                if (count == limit) break; /* No comparison without reserved evidence space. */
                uint64_t a = u64(p + 64 + 8 * (size_t)left) ^ UINT64_C(0x8000000000000000);
                uint64_t b = u64(p + 64 + 8 * (size_t)right) ^ UINT64_C(0x8000000000000000);
                int order = (a > b) - (a < b);
                size_t row = tape + 12 * (size_t)count;
                p32(p + row, left); p32(p + row + 4, right); p32(p + row + 8, (uint32_t)order);
                p32(p + 28, ++count);
                winner = order == 0 ? (left < right ? left : right)
                    : ((direction == 0 ? order < 0 : order > 0) ? left : right);
            }
            p32(p + tree + 4 * (size_t)node, winner);
            if (build != 0) { --build; p32(p + 16, build); }
            else { repair >>= 1; p32(p + 20, repair); }
        } else {
            uint32_t winner = u32(p + tree + 4);
            if (winner >= n || u32(p + tree + 4 * ((size_t)base + winner)) != winner)
                return CORRUPT("native winner index");
            p32(p + prefix + 4 * (size_t)ready, winner);
            p32(p + tree + 4 * ((size_t)base + winner), UINT32_MAX);
            p32(p + 24, ++ready);
            repair = (base + winner) >> 1;
            p32(p + 20, repair);
        }
        ++steps;
        p64(p + 40, u64(p + 40) + 1);
    }
    return steps;
}
