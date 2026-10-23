/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Instrumented JNI table proves region work; separate real-JVM tests prove runtime parity. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static int row_reads, state_reads, writes;
static jsize JNICALL length(JNIEnv *env, jarray a) { (void)env; return ((fixture_array *)a)->size; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return JNI_FALSE; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) { (void)env; return a == b; }
static void JNICALL bytes(JNIEnv *env, jbyteArray a, jsize start, jsize n, jbyte *out) {
    (void)env; state_reads++; memcpy(out, (jbyte *)((fixture_array *)a)->data + start, (size_t)n);
}
static void JNICALL ints(JNIEnv *env, jintArray a, jsize start, jsize n, jint *out) {
    (void)env; row_reads++; memcpy(out, (jint *)((fixture_array *)a)->data + start, (size_t)n * sizeof(jint));
}
static void JNICALL put(JNIEnv *env, jintArray a, jsize start, jsize n, const jint *in) {
    (void)env; writes++; memcpy((jint *)((fixture_array *)a)->data + start, in, (size_t)n * sizeof(jint));
}
extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanNative(JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint, jintArray, jint, jint);
static void require(int ok) { if (!ok) { fputs("region assertion failed\n", stderr); exit(1); } }
int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception; api.IsSameObject = same;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.SetIntArrayRegion = put;
    JNIEnv env = &api;
    jint rows[1024], output[32]; jbyte states[1024] = {0};
    for (int i = 0; i < 1024; i++) rows[i] = i - 512;
    fixture_array r = {1024, rows}, s = {1024, states}, o = {32, output};
    jlong result = Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL, (jintArray)&r, (jbyteArray)&s, 0, 128, 0, (jintArray)&o, 0, 32);
    require(result == ((jlong)1024 << 32)); require(row_reads == 0 && state_reads == 8 && writes == 0);
    states[1000] = (jbyte)128; row_reads = state_reads = writes = 0;
    result = Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL, (jintArray)&r, (jbyteArray)&s, 0, 128, 0, (jintArray)&o, 0, 32);
    require(result == (((jlong)1024 << 32) | 1)); require(row_reads == 1 && state_reads == 8 && writes == 1 && output[0] == 488);
    row_reads = state_reads = writes = 0;
    result = Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL, (jintArray)&r, (jbyteArray)&s, 0, 128, 128, (jintArray)&o, 0, 32);
    require(result == ((jlong)1024 << 32)); require(row_reads == 0 && state_reads == 0 && writes == 0);
    result = Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL, (jintArray)&r, (jbyteArray)&s, 1000, 0, 0, (jintArray)&o, 0, 32);
    require(result == (((jlong)1024 << 32) | 24)); require(row_reads == 1 && state_reads == 0 && writes == 1);
    puts("PASS JNI region work: rejected=0/8 row reads; sparse=1/8; contradictory=0; all=1");
    return 0;
}
