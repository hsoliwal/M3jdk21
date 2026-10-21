/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Adapted from the existing m3_mask_window_regions.c instrumentation fixture.
 * This counts transferred payload, not elapsed time or actual JVM behavior. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static int row_reads, row_elements, state_reads, writes, fault, pending;
static void require(int ok) { if (!ok) { fputs("span assertion failed\n", stderr); exit(1); } }
static void bounds(jarray a, jsize start, jsize n) {
    require(start >= 0 && n >= 0 && start <= ((fixture_array *)a)->size
            && n <= ((fixture_array *)a)->size - start);
}
static jsize JNICALL length(JNIEnv *env, jarray a) { (void)env; return ((fixture_array *)a)->size; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) { (void)env; return a == b; }
static void JNICALL bytes(JNIEnv *env, jbyteArray a, jsize start, jsize n, jbyte *out) {
    (void)env; bounds(a, start, n); state_reads++;
    if (fault == 1) { pending = 1; return; }
    memcpy(out, (jbyte *)((fixture_array *)a)->data + start, (size_t)n);
}
static void JNICALL ints(JNIEnv *env, jintArray a, jsize start, jsize n, jint *out) {
    (void)env; bounds(a, start, n); row_reads++; row_elements += n;
    if (fault == 2) { pending = 1; return; }
    memcpy(out, (jint *)((fixture_array *)a)->data + start, (size_t)n * sizeof(jint));
}
static void JNICALL put(JNIEnv *env, jintArray a, jsize start, jsize n, const jint *in) {
    (void)env; bounds(a, start, n); writes++;
    if (fault == 3) { pending = 1; return; }
    memcpy((jint *)((fixture_array *)a)->data + start, in, (size_t)n * sizeof(jint));
}
extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint, jint, jintArray, jint, jint);
static jlong run(JNIEnv *env, fixture_array *r, fixture_array *s, fixture_array *o, int cap) {
    row_reads = row_elements = state_reads = writes = pending = 0;
    return Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(env, NULL,
            (jintArray)r, (jbyteArray)s, 0, r->size, 128, 0, (jintArray)o, 2, cap);
}
int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception; api.IsSameObject = same;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.SetIntArrayRegion = put;
    JNIEnv env = &api;
    jint rows[300], output[304]; jbyte states[300] = {0};
    for (int i = 0; i < 300; i++) rows[i] = i - 150;
    fixture_array r = {300, rows}, s = {300, states}, o = {304, output};
    for (int pos = 0; pos < 300; pos++) {
        memset(states, 0, sizeof states); states[pos] = (jbyte)128;
        for (int i = 0; i < 304; i++) output[i] = 19;
        jlong result = run(&env, &r, &s, &o, 1);
        require(result == (((jlong)(pos + 1) << 32) | 1));
        require(row_reads == 1 && row_elements == 1 && output[2] == rows[pos]);
        require(output[0] == 19 && output[1] == 19 && output[3] == 19);
        result = run(&env, &r, &s, &o, 300);
        require(result == (((jlong)300 << 32) | 1) && row_elements == 1);
    }
    memset(states, 128, sizeof states);
    require(run(&env, &r, &s, &o, 1) == (((jlong)1 << 32) | 1));
    require(row_elements == 1); /* A dense source still transfers only the requested prefix. */
    require(run(&env, &r, &s, &o, 300) == (((jlong)300 << 32) | 300));
    require(row_elements == 300 && row_reads == 3);
    for (int i = 0; i < 300; i++) require(output[i + 2] == rows[i]);
    memset(states, 0, sizeof states);
    states[127] = states[128] = states[255] = states[256] = (jbyte)128;
    require(run(&env, &r, &s, &o, 300) == (((jlong)300 << 32) | 4));
    require(row_elements == 130 && row_reads == 3 && output[2] == rows[127]
            && output[3] == rows[128] && output[4] == rows[255] && output[5] == rows[256]);
    for (fault = 1; fault <= 3; fault++) {
        for (int i = 0; i < 304; i++) output[i] = 19;
        require(run(&env, &r, &s, &o, 1) == -1 && pending);
        for (int i = 0; i < 304; i++) require(output[i] == 19);
        require(fault != 1 || (row_reads == 0 && writes == 0));
        require(fault != 2 || writes == 0);
    }
    puts("JNI_SPANS_PASS single=1/128 dense_limit1=1/128 cross_chunk=130/300 faults=3");
    return 0;
}
