/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Adapted from the existing m3_mask_window_regions.c fixture. This instruments
 * JNI region work in the actual C owner; separate JVM tests exercise real JNI. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static int row_reads, state_reads, pending, drift, chunks;
static fixture_array result;

static void require(int ok, const char *label) {
    if (!ok) { fprintf(stderr, "FAIL %s rows=%d states=%d pending=%d\n", label,
                       row_reads, state_reads, pending); exit(1); }
}
static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void)env; return ((fixture_array *)array)->size;
}
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find(JNIEnv *env, const char *name) {
    (void)env;
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
    (void)env; state_reads++;
    fixture_array *a = (fixture_array *)array;
    require(start >= 0 && n >= 0 && start <= a->size - n, "byte range");
    memcpy(out, (jbyte *)a->data + start, (size_t)n);
    if (drift && state_reads == chunks + 1 && n > 0) out[0] = 1;
}
static void JNICALL ints(JNIEnv *env, jintArray array, jsize start, jsize n, jint *out) {
    (void)env; row_reads++;
    fixture_array *a = (fixture_array *)array;
    require(start >= 0 && n >= 0 && start <= a->size - n, "int range");
    memcpy(out, (jint *)a->data + start, (size_t)n * sizeof(jint));
}
static void JNICALL longs(JNIEnv *env, jlongArray array, jsize start, jsize n, jlong *out) {
    (void)env; row_reads++;
    fixture_array *a = (fixture_array *)array;
    require(start >= 0 && n >= 0 && start <= a->size - n, "long range");
    memcpy(out, (jlong *)a->data + start, (size_t)n * sizeof(jlong));
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize n) {
    (void)env; result.size = n; result.data = calloc(n == 0 ? 1 : (size_t)n, sizeof(jint));
    require(result.data != NULL, "fixture int allocation"); return (jintArray)&result;
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize n) {
    (void)env; result.size = n; result.data = calloc(n == 0 ? 1 : (size_t)n, sizeof(jlong));
    require(result.data != NULL, "fixture long allocation"); return (jlongArray)&result;
}
static void JNICALL put_ints(JNIEnv *env, jintArray array, jsize start, jsize n, const jint *in) {
    (void)env; fixture_array *a = (fixture_array *)array;
    require(start >= 0 && n >= 0 && start <= a->size - n, "int write");
    memcpy((jint *)a->data + start, in, (size_t)n * sizeof(jint));
}
static void JNICALL put_longs(JNIEnv *env, jlongArray array, jsize start, jsize n, const jlong *in) {
    (void)env; fixture_array *a = (fixture_array *)array;
    require(start >= 0 && n >= 0 && start <= a->size - n, "long write");
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

int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception;
    api.FindClass = find; api.ThrowNew = thrown; api.DeleteLocalRef = release;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.GetLongArrayRegion = longs;
    api.NewIntArray = new_ints; api.NewLongArray = new_longs;
    api.SetIntArrayRegion = put_ints; api.SetLongArrayRegion = put_longs;
    JNIEnv env = &api;
    jint payload[1030]; jlong payload_long[1030]; jbyte states[1031] = {0};
    for (int i = 0; i < 1030; i++) { payload[i] = i - 23; payload_long[i] = ((jlong)1 << 40) + i; }
    for (int family = 0; family < 4; family++) {
        for (int changed = 0; changed < 2; changed++) {
            int n = 1025;
            fixture_array rows = {family == 0 ? n : 1030, payload};
            fixture_array long_rows = {1030, payload_long};
            fixture_array flags = {family < 2 ? n : 1031, states};
            row_reads = state_reads = pending = 0; drift = changed; chunks = (n + 255) / 256;
            jobject output;
            if (family == 0) output = Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
                    &env, NULL, (jintArray)&rows, (jbyteArray)&flags, 1);
            else if (family == 1) output = Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
                    &env, NULL, (jintArray)&rows, (jbyteArray)&flags, 2, n, 1);
            else if (family == 2) output = Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
                    &env, NULL, (jintArray)&rows, 2, (jbyteArray)&flags, 3, n, 1);
            else output = Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
                    &env, NULL, (jlongArray)&long_rows, 2, (jbyteArray)&flags, 3, n, 1);
            require(row_reads == 0, "zero survivors never read row payload");
            require(changed ? output == NULL && pending : output != NULL && !pending && result.size == 0,
                    "state drift remains observable");
            require(state_reads == (changed ? chunks + 1 : 2 * chunks), "second state pass retained");
            free(result.data); result.data = NULL;
        }
    }
    puts("PASS empty compaction: 4 native exports, zero payload reads, second-pass drift retained");
    return 0;
}
