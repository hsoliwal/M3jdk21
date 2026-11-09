/* Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Deterministic JNI region-call fixture. Real JNI qualification is a separate Java run. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef M3_WINDOW_SOURCE
#error Define M3_WINDOW_SOURCE to the exact reviewed native source.
#endif
#include M3_WINDOW_SOURCE

typedef struct { jsize length; void *data; } TestArray;
static jint row_reads, mark_reads, written_values;
static int fault_at, calls;
static jboolean exception_pending;
static jsize test_length(JNIEnv *env, jarray array) { (void)env; return ((TestArray *) array)->length; }
static jboolean same(JNIEnv *env, jobject a, jobject b) { (void)env; return a == b ? JNI_TRUE : JNI_FALSE; }
static jboolean pending(JNIEnv *env) { (void)env; return exception_pending; }
static void byte_region(JNIEnv *env, jbyteArray array, jsize start, jsize size, jbyte *out) {
    (void)env;
    if (++calls == fault_at) { exception_pending = JNI_TRUE; return; }
    TestArray *a = (TestArray *) array;
    if (start < 0 || size < 0 || start > a->length - size) abort();
    mark_reads += size; memcpy(out, (jbyte *) a->data + start, (size_t) size);
}
static void int_region(JNIEnv *env, jintArray array, jsize start, jsize size, jint *out) {
    (void)env;
    if (++calls == fault_at) { exception_pending = JNI_TRUE; return; }
    TestArray *a = (TestArray *) array;
    if (start < 0 || size < 0 || start > a->length - size) abort();
    row_reads += size; memcpy(out, (jint *) a->data + start, (size_t) size * sizeof(jint));
}
static void put_region(JNIEnv *env, jintArray array, jsize start, jsize size, const jint *in) {
    (void)env;
    if (++calls == fault_at) { exception_pending = JNI_TRUE; return; }
    TestArray *a = (TestArray *) array;
    if (start < 0 || size < 0 || start > a->length - size) abort();
    written_values += size; memcpy((jint *) a->data + start, in, (size_t) size * sizeof(jint));
}
static const struct JNINativeInterface_ methods = {
    .GetArrayLength = test_length, .IsSameObject = same, .ExceptionCheck = pending,
    .GetByteArrayRegion = byte_region, .GetIntArrayRegion = int_region, .SetIntArrayRegion = put_region
};
static void run(int shape, int candidate, int fault) {
    jint rows[300], output[8]; jbyte flags[300];
    memset(flags, 0, sizeof(flags));
    for (int i = 0; i < 300; i++) rows[i] = i;
    for (int i = 0; i < 8; i++) output[i] = -777;
    int length = shape == 2 ? 300 : 128, limit = shape == 2 ? 4 : 1;
    if (shape == 0) flags[100] = 1;
    if (shape == 1) memset(flags, 1, sizeof(flags));
    if (shape == 2) flags[0] = flags[127] = flags[128] = flags[299] = 1;
    TestArray ra = {length, rows}, sa = {length, flags}, oa = {8, output};
    JNIEnv env = &methods;
    row_reads = mark_reads = written_values = calls = 0; exception_pending = JNI_FALSE; fault_at = fault;
    jlong result = m3_window_scan_range(&env, (jintArray) &ra, (jbyteArray) &sa,
            0, length, JNI_FALSE, 1, 0, (jintArray) &oa, 2, limit);
    if (fault) {
        if (result != -1 || !exception_pending) abort();
        for (int i = 0; i < 8; i++) if (output[i] != -777) abort();
        return;
    }
    if (result != m3_window_result(shape == 0 ? 101 : shape == 1 ? 1 : 300, limit)) abort();
    if (output[2] != (shape == 0 ? 100 : 0) || output[1] != -777 || output[2 + limit] != -777) abort();
    if (shape == 2 && (output[3] != 127 || output[4] != 128 || output[5] != 299)) abort();
    int expected = candidate ? (shape == 2 ? 130 : 1) : length;
    if (row_reads != expected || written_values != limit) abort();
    printf("%d,%d,%d,%d,%d\n", shape, row_reads, mark_reads, written_values, candidate);
}
int main(int argc, char **argv) {
    int candidate = argc == 2 && strcmp(argv[1], "candidate") == 0;
    puts("shape,row_values_read,state_values_read,output_values_written,candidate");
    for (int shape = 0; shape < 3; shape++) run(shape, candidate, 0);
    run(0, candidate, 1); run(0, candidate, 2); run(0, candidate, 3);
    puts("MASK_WINDOW_WORK_AND_FAULT_PASS");
    return 0;
}
