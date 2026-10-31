/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Reuses m3_mask_compactor_empty_regions.c instrumentation. Counts actual C-owner
 * JNI transfers; real-JVM parity is a separate gate, as is elapsed-time performance. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static int row_reads, row_elements, state_reads, writes, pending, fault, drift;
static int chunks, checks, observe, row_offset, state_offset;
static fixture_array result;

static void require(int ok, const char *label) {
    if (!ok) {
        fprintf(stderr, "FAIL %s rows=%d elements=%d states=%d writes=%d pending=%d\n",
                label, row_reads, row_elements, state_reads, writes, pending);
        exit(1);
    }
}
static fixture_array *bounds(jarray array, jsize start, jsize n) {
    fixture_array *a = (fixture_array *)array;
    require(!pending, "JNI operation after pending exception");
    require(start >= 0 && n >= 0 && start <= a->size && n <= a->size - start,
            "region bounds");
    return a;
}
static jsize JNICALL length(JNIEnv *env, jarray a) {
    (void)env; require(!pending, "length after exception"); return ((fixture_array *)a)->size;
}
static jboolean JNICALL exception(JNIEnv *env) {
    (void)env; return pending ? JNI_TRUE : JNI_FALSE;
}
static jclass JNICALL find(JNIEnv *env, const char *name) {
    (void)env; require(!pending, "find after exception");
    require(strcmp(name, "java/util/ConcurrentModificationException") == 0, "drift class");
    return (jclass)(uintptr_t)1;
}
static jint JNICALL thrown(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)type;
    require(strcmp(message, "aligned selection states changed") == 0, "drift message");
    pending = 1; return 0;
}
static void JNICALL release(JNIEnv *env, jobject value) { (void)env; (void)value; }
static void JNICALL bytes(JNIEnv *env, jbyteArray array, jsize start, jsize n, jbyte *out) {
    (void)env; fixture_array *a = bounds(array, start, n); state_reads++;
    if (fault == 1 || (fault == 5 && state_reads > chunks)) { pending = 1; return; }
    memcpy(out, (jbyte *)a->data + start, (size_t)n);
    if (drift && state_reads == chunks + 1 && n > 0) {
        out[0] = drift > 0 ? (jbyte)0x80 : 0;
    }
}
static void JNICALL ints(JNIEnv *env, jintArray array, jsize start, jsize n, jint *out) {
    (void)env; fixture_array *a = bounds(array, start, n); row_reads++; row_elements += n;
    if (fault == 2) { pending = 1; return; }
    memcpy(out, (jint *)a->data + start, (size_t)n * sizeof(jint));
}
static void JNICALL longs(JNIEnv *env, jlongArray array, jsize start, jsize n, jlong *out) {
    (void)env; fixture_array *a = bounds(array, start, n); row_reads++; row_elements += n;
    if (fault == 2) { pending = 1; return; }
    memcpy(out, (jlong *)a->data + start, (size_t)n * sizeof(jlong));
}
static jobject allocate(jsize n, size_t width) {
    require(!pending && n >= 0, "allocation geometry");
    if (fault == 4) { pending = 1; return NULL; }
    result.size = n; result.data = calloc(n == 0 ? 1 : (size_t)n, width);
    require(result.data != NULL, "fixture allocation"); return (jobject)&result;
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize n) {
    (void)env; return (jintArray)allocate(n, sizeof(jint));
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize n) {
    (void)env; return (jlongArray)allocate(n, sizeof(jlong));
}
static void JNICALL put_ints(JNIEnv *env, jintArray array, jsize start, jsize n, const jint *in) {
    (void)env; fixture_array *a = bounds(array, start, n); writes++;
    if (fault == 3) { pending = 1; return; }
    memcpy((jint *)a->data + start, in, (size_t)n * sizeof(jint));
}
static void JNICALL put_longs(JNIEnv *env, jlongArray array, jsize start, jsize n, const jlong *in) {
    (void)env; fixture_array *a = bounds(array, start, n); writes++;
    if (fault == 3) { pending = 1; return; }
    memcpy((jlong *)a->data + start, in, (size_t)n * sizeof(jlong));
}
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
        JNIEnv *, jclass, jintArray, jbyteArray, jint);
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
        JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint);
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
        JNIEnv *, jclass, jintArray, jint, jbyteArray, jint, jint, jint);
extern jlongArray Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
        JNIEnv *, jclass, jlongArray, jint, jbyteArray, jint, jint, jint);

static void run(JNIEnv *env, int family, int n, int mask, jint *rows,
        jlong *long_rows, jbyte *states, int expected_elements, int expected_reads) {
    fixture_array r = {family == 0 ? n : n + 5, rows};
    fixture_array l = {n + 5, long_rows};
    fixture_array s = {family < 2 ? n : n + 7, states};
    row_offset = family == 0 ? 0 : 2;
    state_offset = family < 2 ? 0 : 3;
    row_reads = row_elements = state_reads = writes = pending = 0;
    chunks = (n + 255) / 256;
    jobject output;
    if (family == 0) output = Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
            env, NULL, (jintArray)&r, (jbyteArray)&s, mask);
    else if (family == 1) output = Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
            env, NULL, (jintArray)&r, (jbyteArray)&s, row_offset, n, mask);
    else if (family == 2) output = Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
            env, NULL, (jintArray)&r, row_offset, (jbyteArray)&s, state_offset, n, mask);
    else output = Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
            env, NULL, (jlongArray)&l, row_offset, (jbyteArray)&s, state_offset, n, mask);
    if (fault || drift) {
        require(output == NULL && pending, "failure must propagate");
        require(fault != 1 || (row_reads == 0 && writes == 0), "count fault stops work");
        require(fault != 2 || writes == 0, "payload fault stops writes");
        require(fault != 4 || (row_reads == 0 && writes == 0), "allocation fault stops work");
        require(fault != 5 || (row_reads == 0 && writes == 0), "emit state fault stops work");
    } else {
        require(output != NULL && !pending, "successful result");
        int kept = 0;
        for (int i = 0; i < n; i++) {
            if (((unsigned char)states[state_offset + i] & mask) != mask) continue;
            require(kept < result.size, "result contains survivor");
            if (family == 3) require(((jlong *)result.data)[kept] == long_rows[row_offset + i], "long order");
            else require(((jint *)result.data)[kept] == rows[row_offset + i], "int order");
            kept++;
        }
        require(kept == result.size, "exact result size");
        require(state_reads == (mask == 0 ? 0 : 2 * chunks), "state passes retained");
        if (!observe) {
            require(row_elements == expected_elements, "only enclosing admitted spans transfer");
            require(row_reads == expected_reads, "rejected chunks skip payload JNI");
        }
    }
    checks++;
    free(result.data); result.data = NULL;
}

int main(int argc, char **argv) {
    observe = argc == 2 && strcmp(argv[1], "--observe-baseline") == 0;
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception;
    api.FindClass = find; api.ThrowNew = thrown; api.DeleteLocalRef = release;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.GetLongArrayRegion = longs;
    api.NewIntArray = new_ints; api.NewLongArray = new_longs;
    api.SetIntArrayRegion = put_ints; api.SetLongArrayRegion = put_longs;
    JNIEnv env = &api;
    jint rows[1030]; jlong long_rows[1030]; jbyte states[1032];
    for (int i = 0; i < 1030; i++) {
        rows[i] = i % 17 == 0 ? INT32_MIN : i % 17 - 8;
        long_rows[i] = i % 19 == 0 ? INT64_MIN : INT64_C(0x1234567890000000) + i % 23;
    }
    for (int family = 0; family < 4; family++) {
        int start = family < 2 ? 0 : 3;
        for (int pos = 0; pos < 1025; pos++) {
            memset(states, 0, sizeof states); states[start + pos] = (jbyte)0xff;
            run(&env, family, 1025, 0x80, rows, long_rows, states, 1, 1);
        }
        printf("family=%d single_survivor row_elements=%d row_regions=%d\n", family, row_elements, row_reads);
        memset(states, 0, sizeof states);
        run(&env, family, 1025, 0x80, rows, long_rows, states, 0, 0);
        run(&env, family, 1025, 0, rows, long_rows, states, 1025, 5);
        memset(states, 0xff, sizeof states);
        run(&env, family, 1025, 0xff, rows, long_rows, states, 1025, 5);
        run(&env, family, 0, 0x80, rows, long_rows, states, 0, 0);
        memset(states, 0, sizeof states);
        states[start + 127] = states[start + 255] = states[start + 256] = states[start + 1024] = (jbyte)0x80;
        run(&env, family, 1025, 0x80, rows, long_rows, states, 131, 3);
        for (fault = 1; fault <= 5; fault++) {
            run(&env, family, 1025, 0x80, rows, long_rows, states, 0, 0);
        }
        fault = 0;
        memset(states, 0, sizeof states); drift = 1;
        run(&env, family, 1025, 0x80, rows, long_rows, states, 0, 0);
        states[start] = (jbyte)0x80; drift = -1;
        run(&env, family, 1025, 0x80, rows, long_rows, states, 0, 0);
        drift = 0;
    }
    printf("COMPACTOR_SPANS_PASS cases=%d exports=4 faults=5 drift_directions=2 mode=%s\n",
            checks, observe ? "baseline-observation" : "span-contract");
    return 0;
}
