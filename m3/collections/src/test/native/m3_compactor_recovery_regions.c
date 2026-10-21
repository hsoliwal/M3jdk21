/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Fresh 2026-10-19 instrumentation using the existing JNI-table fixture form.
 * It includes the actual production C translation unit. Region counts measure
 * transferred primitive payload; an actual JVM is qualified separately.
 */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef M3_COMPACTOR_SOURCE
#error M3_COMPACTOR_SOURCE must name the exact production source
#endif
#include M3_COMPACTOR_SOURCE

enum { BYTE = 1, INT = 2, LONG = 3, REGION_FAILURE = 1, DRIFT = 2, BAD_INPUT = 3, ALLOCATION_FAILURE = 4 };
typedef struct { jsize length; void *data; int kind; } Array;
static long checks;
static int pending, regions, fail_region, fail_allocation, allocations;
static int byte_calls, row_calls, write_calls, drift_index, drift_value;
static long byte_lanes, row_lanes, write_lanes;
static jint result_ints[600];
static jlong result_longs[600];
static jbyte states[600];
static Array result;

static void require(int condition, const char *label) {
    checks++;
    if (!condition) { fprintf(stderr, "COMPACTOR_RECOVERY_ASSERT:%s\n", label); exit(1); }
}

static Array *array(jarray value) { return (Array *)(void *)value; }
static jsize JNICALL length(JNIEnv *env, jarray value) {
    (void)env; require(!pending, "length after pending"); return array(value)->length;
}
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending != 0; }
static jclass JNICALL find(JNIEnv *env, const char *name) { (void)env; return (jclass)(void *)name; }
static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)message;
    require(!pending, "exception identity replaced");
    pending = strcmp((const char *)(void *)type, "java/util/ConcurrentModificationException") == 0
            ? DRIFT : BAD_INPUT;
    return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject value) { (void)env; (void)value; }
static int region(Array *value, jsize begin, jsize count, int kind) {
    require(!pending, "region after pending exception");
    require(value->kind == kind && begin >= 0 && count >= 0 && begin <= value->length
            && count <= value->length - begin, "region geometry and lane type");
    require(count <= 256, "fixed native chunk bound");
    regions++;
    if (regions == fail_region) { pending = REGION_FAILURE; return 0; }
    return 1;
}
static void JNICALL get_bytes(JNIEnv *env, jbyteArray value, jsize begin, jsize count, jbyte *out) {
    (void)env; byte_calls++; byte_lanes += count;
    if (region(array(value), begin, count, BYTE))
        memcpy(out, (jbyte *)array(value)->data + begin, (size_t)count);
}
static void JNICALL get_ints(JNIEnv *env, jintArray value, jsize begin, jsize count, jint *out) {
    (void)env; row_calls++; row_lanes += count;
    if (region(array(value), begin, count, INT))
        memcpy(out, (jint *)array(value)->data + begin, (size_t)count * sizeof(jint));
}
static void JNICALL get_longs(JNIEnv *env, jlongArray value, jsize begin, jsize count, jlong *out) {
    (void)env; row_calls++; row_lanes += count;
    if (region(array(value), begin, count, LONG))
        memcpy(out, (jlong *)array(value)->data + begin, (size_t)count * sizeof(jlong));
}
static void JNICALL put_ints(JNIEnv *env, jintArray value, jsize begin, jsize count, const jint *in) {
    (void)env; write_calls++; write_lanes += count;
    if (region(array(value), begin, count, INT))
        memcpy((jint *)array(value)->data + begin, in, (size_t)count * sizeof(jint));
}
static void JNICALL put_longs(JNIEnv *env, jlongArray value, jsize begin, jsize count, const jlong *in) {
    (void)env; write_calls++; write_lanes += count;
    if (region(array(value), begin, count, LONG))
        memcpy((jlong *)array(value)->data + begin, in, (size_t)count * sizeof(jlong));
}
static jarray allocate(jsize count, int kind) {
    require(!pending && count >= 0 && count <= 600, "exact result allocation geometry");
    allocations++;
    if (fail_allocation) { pending = ALLOCATION_FAILURE; return NULL; }
    if (drift_index >= 0) states[drift_index] = (jbyte)drift_value;
    result.length = count; result.kind = kind;
    result.data = kind == INT ? (void *)result_ints : (void *)result_longs;
    memset(result_ints, 0x6d, sizeof result_ints);
    memset(result_longs, 0x6d, sizeof result_longs);
    return (jarray)(void *)&result;
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize count) { (void)env; return (jintArray)allocate(count, INT); }
static jlongArray JNICALL new_longs(JNIEnv *env, jsize count) { (void)env; return (jlongArray)allocate(count, LONG); }
static const struct JNINativeInterface_ table = {
    .GetArrayLength = length, .ExceptionCheck = exception, .FindClass = find,
    .ThrowNew = throw_new, .DeleteLocalRef = delete_ref,
    .GetByteArrayRegion = get_bytes, .GetIntArrayRegion = get_ints,
    .GetLongArrayRegion = get_longs, .SetIntArrayRegion = put_ints,
    .SetLongArrayRegion = put_longs, .NewIntArray = new_ints, .NewLongArray = new_longs
};

static void reset(void) {
    pending = regions = fail_region = fail_allocation = allocations = 0;
    byte_calls = row_calls = write_calls = 0;
    byte_lanes = row_lanes = write_lanes = 0;
    drift_index = -1; drift_value = 0;
}

static jarray call(int owner, Array *rows, Array *marks, int ro, int so, int count, int mask) {
    JNIEnv env = &table;
    if (owner == 0)
        return Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env, NULL,
                (jintArray)(void *)rows, (jbyteArray)(void *)marks, mask);
    if (owner == 1)
        return Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(&env, NULL,
                (jintArray)(void *)rows, (jbyteArray)(void *)marks, ro, count, mask);
    if (owner == 2)
        return Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env, NULL,
                (jintArray)(void *)rows, ro, (jbyteArray)(void *)marks, so, count, mask);
    if (owner == 3)
        return Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(&env, NULL,
                (jlongArray)(void *)rows, ro, (jbyteArray)(void *)marks, so, count, mask);
    return Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative(&env, NULL,
            (jbyteArray)(void *)marks, mask);
}

int main(int argc, char **argv) {
    int enforce = argc == 1 || strcmp(argv[1], "observe") != 0;
    jint ints[600], before_ints[600];
    jlong longs[600], before_longs[600];
    for (int i = 0; i < 600; i++) {
        ints[i] = i % 11 == 0 ? INT32_MIN : i % 13 == 0 ? INT32_MAX : i % 17 - 8;
        longs[i] = i % 11 == 0 ? INT64_MIN : i % 13 == 0 ? INT64_MAX : (jlong)(i % 17 - 8);
    }
    memcpy(before_ints, ints, sizeof ints); memcpy(before_longs, longs, sizeof longs);
    const int lengths[] = {0, 1, 255, 256, 257, 513};
    const int masks[] = {0, 1, 128, 255};
    for (int owner = 0; owner < 5; owner++) {
        for (unsigned int li = 0; li < sizeof lengths / sizeof lengths[0]; li++) {
            int count = lengths[li];
            int ro = owner == 0 || owner == 4 ? 0 : 17;
            int so = owner < 2 || owner == 4 ? 0 : 11;
            Array rows = {owner == 0 ? count : 600, owner == 3 ? (void *)longs : (void *)ints,
                          owner == 3 ? LONG : INT};
            Array marks = {owner < 2 || owner == 4 ? count : 600, states, BYTE};
            for (unsigned int mi = 0; mi < sizeof masks / sizeof masks[0]; mi++) {
                int mask = masks[mi];
                for (int pattern = 0; pattern < 3; pattern++) {
                    for (int i = 0; i < 600; i++)
                        states[i] = pattern == 0 ? 0 : pattern == 1 ? (jbyte)255 : (jbyte)(i * 37);
                    jbyte before_states[600]; memcpy(before_states, states, sizeof states);
                    int expected_slots[600], accepted = 0;
                    for (int i = 0; i < count; i++)
                        if (((unsigned char)states[so + i] & mask) == mask) expected_slots[accepted++] = i;
                    reset();
                    jarray output = call(owner, &rows, &marks, ro, so, count, mask);
                    require(output != NULL && !pending && allocations == 1, "result and single allocation");
                    require(array(output)->length == accepted, "exact-sized accepted result");
                    for (int i = 0; i < accepted; i++) {
                        int slot = expected_slots[i];
                        if (owner == 3) require(result_longs[i] == longs[ro + slot], "stable signed long occurrence");
                        else require(result_ints[i] == (owner == 4 ? slot : ints[ro + slot]), "stable signed int/position occurrence");
                    }
                    require(memcmp(states, before_states, sizeof states) == 0, "state input not mutated");
                    require(memcmp(ints, before_ints, sizeof ints) == 0
                            && memcmp(longs, before_longs, sizeof longs) == 0, "row inputs not mutated");
                    if (mask == 0) require(byte_calls == 0, "identity mask avoids state reads");
                    if (accepted == 0) {
                        require(write_calls == 0, "empty result writes no rows");
                        if (enforce) require(row_calls == 0, "empty result reads no rows");
                        if (mask != 0) require(byte_calls == 2 * ((count + 255) / 256), "empty keeps two state passes");
                    }
                    if (owner < 4 && count == 257 && pattern == 0 && mask == 128)
                        printf("EMPTY_WORK owner=%d rows=%ld row_calls=%d states=%ld state_calls=%d writes=%ld\n",
                                owner, row_lanes, row_calls, byte_lanes, byte_calls, write_lanes);
                    int successful_regions = regions;
                    for (int fault = 1; fault <= successful_regions; fault++) {
                        reset(); fail_region = fault;
                        output = call(owner, &rows, &marks, ro, so, count, mask);
                        require(output == NULL && pending == REGION_FAILURE && regions == fault,
                                "pending region failure stops immediately");
                    }
                    reset(); fail_allocation = 1;
                    output = call(owner, &rows, &marks, ro, so, count, mask);
                    require(output == NULL && pending == ALLOCATION_FAILURE && row_calls == 0
                            && write_calls == 0, "allocation failure precedes rows and writes");
                }
            }
            if (count != 0) {
                for (int increase = 0; increase < 2; increase++) {
                    memset(states, 0, sizeof states);
                    if (!increase) states[so] = (jbyte)128;
                    reset(); drift_index = so; drift_value = increase ? 128 : 0;
                    jarray output = call(owner, &rows, &marks, ro, so, count, 128);
                    require(output == NULL && pending == DRIFT, "count/emit cardinality drift diagnostic");
                }
            }
        }
        Array rows = {600, owner == 3 ? (void *)longs : (void *)ints, owner == 3 ? LONG : INT};
        Array marks = {600, states, BYTE};
        reset();
        require(call(owner, &rows, &marks, 0, 0, 600, 256) == NULL
                && pending == BAD_INPUT && regions == 0, "invalid unsigned-byte mask before regions");
    }
    printf("COMPACTOR_RECOVERY_PASS checks=%ld enforce_empty_rows=%d\n", checks, enforce);
    return 0;
}
