/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Reuses the existing m3_mask_window_regions.c instrumented-JNI test pattern.
 * Real-JVM differential tests separately establish Java/native behavior. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int allocations, reads, writes, pending;
static size_t allocation_bytes;
static jint window_first, window_end;
typedef struct { jint size; jint *data; } array;
static void require(int ok) {
    if (!ok) { fputs("viewport region assertion failed\n", stderr); exit(1); }
}
static void *tracked_malloc(size_t n) {
    allocations++; allocation_bytes += n; return calloc(1, n);
}
#define malloc tracked_malloc
#include "../../main/c/m3maskviewport.c"
#undef malloc

static jsize JNICALL size(JNIEnv *env, jarray a) { (void)env; return ((array *)a)->size; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) { (void)env; return a == b; }
static jclass JNICALL find(JNIEnv *env, const char *name) { (void)env; (void)name; return (jclass)1; }
static jint JNICALL fail(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)type; (void)message; pending = 1; return 0;
}
static void JNICALL get(JNIEnv *env, jintArray a, jsize first, jsize n, jint *out) {
    (void)env;
    require(!pending && n > 0 && n <= REGION_SIZE);
    require(first >= window_first && first + n <= window_end);
    reads++; memcpy(out, ((array *)a)->data + first, (size_t)n * sizeof(jint));
}
static void JNICALL put(JNIEnv *env, jintArray a, jsize first, jsize n, const jint *in) {
    (void)env; require(!pending && first >= 0 && first + n <= ((array *)a)->size);
    writes++; memcpy(((array *)a)->data + first, in, (size_t)n * sizeof(jint));
}
static jint run(JNIEnv *env, array *src, array *flags, jint first, jint length,
        array *out, jint target) {
    allocations = reads = writes = pending = 0; allocation_bytes = 0;
    window_first = first; window_end = first + length;
    return Java_com_m3_collections_M3MaskViewportNative_project(env, NULL,
            (jintArray)src, (jintArray)flags, first, length, 1, 0, (jintArray)out, target);
}
int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = size; api.ExceptionCheck = exception; api.IsSameObject = same;
    api.GetIntArrayRegion = get; api.SetIntArrayRegion = put;
    api.FindClass = find; api.ThrowNew = fail;
    JNIEnv env = &api;
    jint source[6000], state[6000], output[6000];
    for (int i = 0; i < 6000; i++) { source[i] = i; state[i] = 1; output[i] = -1; }
    array src = {6000, source}, flags = {6000, state}, out = {6000, output};
    require(run(&env, &src, &flags, 7, 4097, &out, 2) == 4097);
    require(allocations == 0 && reads == 20 && writes == 5);
    require(output[1] == -1 && output[2] == 7 && output[4098] == 4103 && output[4099] == -1);
    require(run(&env, &src, &flags, 2000, 1024, &src, 0) == 1024);
    require(allocations == 0 && source[0] == 2000 && source[1023] == 3023);
    for (int i = 0; i < 6000; i++) source[i] = i;
    require(run(&env, &src, &flags, 0, 4097, &src, 1) == 4097);
    require(allocations == 1 && allocation_bytes == 4097 * sizeof(jint) && writes == 1);
    for (int i = 1; i <= 4097; i++) require(source[i] == i - 1);
    source[4096] = -1; state[4096] = 0;
    require(run(&env, &src, &flags, 0, 4097, &out, 0) == 0);
    require(pending && writes == 0 && allocations == 0);
    puts("PASS viewport JNI regions: bounded=1024 disjoint_allocations=0 overlap_snapshot=4097 late_invalid_writes=0");
    return 0;
}
