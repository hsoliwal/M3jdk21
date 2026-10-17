/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Qualification fixture only: real production JNI exports against a counted JNI table.
 * Root must also run the actual JVM/JNI SelfTest; this fixture proves state-read bypass.
 */
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#ifndef M3_MASK_SOURCE
#define M3_MASK_SOURCE "../../src/main/native/collections/m3_index_mask_compactor.c"
#endif
#include M3_MASK_SOURCE

#define PROBE_CAPACITY 300
#define REQUIRE(test) do { if (!(test)) { \
    fprintf(stderr, "FAIL zero-mask JNI probe line=%d: %s\n", __LINE__, #test); \
    return 1; } } while (0)

typedef struct {
    jsize length;
    union {
        jint ints[PROBE_CAPACITY];
        jlong longs[PROBE_CAPACITY];
        jbyte bytes[PROBE_CAPACITY];
    } data;
} ProbeArray;

static ProbeArray output;
static int pending_exception;
static unsigned int state_reads;
static unsigned int row_reads;

static int probe_region(ProbeArray *array, jsize first, jsize length) {
    if (array == NULL || first < 0 || length < 0 || first > array->length
            || length > array->length - first) {
        pending_exception = 1;
        return 0;
    }
    return 1;
}

static jsize JNICALL probe_length(JNIEnv *env, jarray array) {
    (void)env;
    if (array == NULL) {
        pending_exception = 1;
        return 0;
    }
    return ((ProbeArray *)array)->length;
}
static jboolean JNICALL probe_exception(JNIEnv *env) {
    (void)env;
    return pending_exception ? JNI_TRUE : JNI_FALSE;
}
static jclass JNICALL probe_class(JNIEnv *env, const char *name) {
    (void)env; (void)name;
    return (jclass)(uintptr_t)1;
}
static jint JNICALL probe_throw(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)type; (void)message;
    pending_exception = 1;
    return 0;
}
static void JNICALL probe_delete(JNIEnv *env, jobject reference) {
    (void)env; (void)reference;
}
static jintArray JNICALL probe_new_int(JNIEnv *env, jsize length) {
    (void)env;
    if (length < 0 || length > PROBE_CAPACITY) {
        pending_exception = 1;
        return NULL;
    }
    memset(&output, 0, sizeof(output));
    output.length = length;
    return (jintArray)&output;
}
static jlongArray JNICALL probe_new_long(JNIEnv *env, jsize length) {
    return (jlongArray)probe_new_int(env, length);
}
static void JNICALL probe_get_bytes(JNIEnv *env, jbyteArray array,
        jsize first, jsize length, jbyte *values) {
    (void)env;
    state_reads++;
    if (!probe_region((ProbeArray *)array, first, length)) return;
    memcpy(values, ((ProbeArray *)array)->data.bytes + first, (size_t)length * sizeof(*values));
}
static void JNICALL probe_get_ints(JNIEnv *env, jintArray array,
        jsize first, jsize length, jint *values) {
    (void)env;
    row_reads++;
    if (!probe_region((ProbeArray *)array, first, length)) return;
    memcpy(values, ((ProbeArray *)array)->data.ints + first, (size_t)length * sizeof(*values));
}
static void JNICALL probe_get_longs(JNIEnv *env, jlongArray array,
        jsize first, jsize length, jlong *values) {
    (void)env;
    row_reads++;
    if (!probe_region((ProbeArray *)array, first, length)) return;
    memcpy(values, ((ProbeArray *)array)->data.longs + first, (size_t)length * sizeof(*values));
}
static void JNICALL probe_set_ints(JNIEnv *env, jintArray array,
        jsize first, jsize length, const jint *values) {
    (void)env;
    if (!probe_region((ProbeArray *)array, first, length)) return;
    memcpy(((ProbeArray *)array)->data.ints + first, values, (size_t)length * sizeof(*values));
}
static void JNICALL probe_set_longs(JNIEnv *env, jlongArray array,
        jsize first, jsize length, const jlong *values) {
    (void)env;
    if (!probe_region((ProbeArray *)array, first, length)) return;
    memcpy(((ProbeArray *)array)->data.longs + first, values, (size_t)length * sizeof(*values));
}

static const struct JNINativeInterface_ counted_jni = {
    .FindClass = probe_class,
    .ThrowNew = probe_throw,
    .DeleteLocalRef = probe_delete,
    .NewIntArray = probe_new_int,
    .NewLongArray = probe_new_long,
    .GetArrayLength = probe_length,
    .GetByteArrayRegion = probe_get_bytes,
    .GetIntArrayRegion = probe_get_ints,
    .GetLongArrayRegion = probe_get_longs,
    .SetIntArrayRegion = probe_set_ints,
    .SetLongArrayRegion = probe_set_longs,
    .ExceptionCheck = probe_exception
};

static void reset_calls(void) {
    pending_exception = 0;
    state_reads = 0;
    row_reads = 0;
}

static int prove_window(jsize length, jint row_offset, jint state_offset) {
    JNIEnv table = &counted_jni;
    JNIEnv *env = &table;
    ProbeArray rows = {0}, longs = {0}, states = {0};
    rows.length = longs.length = row_offset + length + 4;
    states.length = state_offset + length + 4;
    memset(states.data.bytes, 0xff, sizeof(states.data.bytes));
    memset(states.data.bytes + state_offset, 0, (size_t)length);
    for (jsize i = 0; i < PROBE_CAPACITY; i++) {
        rows.data.ints[i] = i % 3 == 0 ? INT32_MIN : i % 3 == 1 ? INT32_MAX : -7;
        longs.data.longs[i] = i % 3 == 0 ? INT64_MIN : i % 3 == 1 ? INT64_MAX : -7;
    }
    ProbeArray original_rows = rows, original_longs = longs, original_states = states;
    unsigned int chunks = (unsigned int)(length + M3_CHUNK - 1) / M3_CHUNK;
    reset_calls();
    jintArray selected = Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
            env, NULL, (jintArray)&rows, row_offset, (jbyteArray)&states, state_offset, length, 0);
    REQUIRE(selected != NULL && selected != (jintArray)&rows && !pending_exception);
    REQUIRE(((ProbeArray *)selected)->length == length);
    REQUIRE(state_reads == 0 && row_reads == chunks);
    REQUIRE(memcmp(output.data.ints, rows.data.ints + row_offset, (size_t)length * sizeof(jint)) == 0);
    reset_calls();
    jlongArray selected_long = Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
            env, NULL, (jlongArray)&longs, row_offset, (jbyteArray)&states, state_offset, length, 0);
    REQUIRE(selected_long != NULL && selected_long != (jlongArray)&longs && !pending_exception);
    REQUIRE(((ProbeArray *)selected_long)->length == length);
    REQUIRE(state_reads == 0 && row_reads == chunks);
    REQUIRE(memcmp(output.data.longs, longs.data.longs + row_offset, (size_t)length * sizeof(jlong)) == 0);
    REQUIRE(rows.length == original_rows.length
            && memcmp(rows.data.ints, original_rows.data.ints, sizeof(rows.data.ints)) == 0);
    REQUIRE(longs.length == original_longs.length
            && memcmp(longs.data.longs, original_longs.data.longs, sizeof(longs.data.longs)) == 0);
    REQUIRE(states.length == original_states.length
            && memcmp(states.data.bytes, original_states.data.bytes, sizeof(states.data.bytes)) == 0);
    reset_calls();
    selected = Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
            env, NULL, (jintArray)&rows, row_offset, (jbyteArray)&states, state_offset, length, 255);
    REQUIRE(selected != NULL && !pending_exception && ((ProbeArray *)selected)->length == 0);
    REQUIRE(state_reads == 2 * chunks);
    reset_calls();
    selected_long = Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
            env, NULL, (jlongArray)&longs, -1, (jbyteArray)&states, state_offset, length, 0);
    REQUIRE(selected_long == NULL && pending_exception && state_reads == 0 && row_reads == 0);
    reset_calls();
    selected = Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
            env, NULL, (jintArray)&rows, row_offset, NULL, state_offset, length, 0);
    REQUIRE(selected == NULL && pending_exception && state_reads == 0 && row_reads == 0);
    return 0;
}

int main(void) {
    const jsize lengths[] = {0, 1, 255, 256, 257};
    for (size_t i = 0; i < sizeof(lengths) / sizeof(lengths[0]); i++) {
        if (prove_window(lengths[i], 0, 0) || prove_window(lengths[i], 3, 5)
                || prove_window(lengths[i], 5, 3)) return 1;
    }
    puts("PASS zero-mask JNI state-read bypass windows=15 int+long");
    return 0;
}
