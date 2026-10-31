/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Instrumented JNI table measures actual owner region work; real-JVM tests prove JNI parity. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static int row_reads, state_reads, writes;
static long long row_elements, state_elements, written_elements;
static const char *case_name = "initialization";
static int suffix_mode = 1;

static void require(int valid, const char *message) {
    if (!valid) { fprintf(stderr, "%s: %s\n", case_name, message); exit(1); }
}

static void region(fixture_array *array, jsize start, jsize count) {
    require(start >= 0 && start <= array->size && count >= 0
            && count <= array->size - start, "native region outside source/output bounds");
}

static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void) env; return ((fixture_array *) array)->size;
}

static jboolean JNICALL exception(JNIEnv *env) { (void) env; return JNI_FALSE; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) { (void) env; return a == b; }

static void JNICALL bytes(JNIEnv *env, jbyteArray array, jsize start, jsize count, jbyte *out) {
    (void) env;
    fixture_array *source = (fixture_array *) array; region(source, start, count);
    state_reads++; state_elements += count;
    memcpy(out, (jbyte *) source->data + start, (size_t) count);
}

static void JNICALL ints(JNIEnv *env, jintArray array, jsize start, jsize count, jint *out) {
    (void) env;
    fixture_array *source = (fixture_array *) array; region(source, start, count);
    row_reads++; row_elements += count;
    memcpy(out, (jint *) source->data + start, (size_t) count * sizeof(jint));
}

static void JNICALL put(JNIEnv *env, jintArray array, jsize start, jsize count, const jint *in) {
    (void) env;
    fixture_array *target = (fixture_array *) array; region(target, start, count);
    writes++; written_elements += count;
    memcpy((jint *) target->data + start, in, (size_t) count * sizeof(jint));
}

extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jintArray, jint, jint);
extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jint, jintArray, jint, jint);

static void run(JNIEnv *env, const char *name, fixture_array *rows, fixture_array *states,
        fixture_array *output, jint first, jint end, int bounded, jint required, jint excluded,
        jint limit, int want_row_calls, int want_state_calls, int want_writes,
        long long baseline_rows, long long suffix_rows, long long want_states) {
    case_name = name;
    row_reads = state_reads = writes = 0;
    row_elements = state_elements = written_elements = 0;
    jint *out = (jint *) output->data;
    for (jsize i = 0; i < output->size; i++) out[i] = 0x74ac013d;
    jlong actual;
    if (bounded) {
        actual = Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(env, NULL,
                (jintArray) rows, (jbyteArray) states, first, end, required, excluded,
                (jintArray) output, 2, limit);
    } else {
        end = rows->size;
        actual = Java_com_m3_collections_M3IndexMaskWindow_scanNative(env, NULL,
                (jintArray) rows, (jbyteArray) states, first, required, excluded,
                (jintArray) output, 2, limit);
    }
    jint next = first, copied = 0;
    while (next < end && copied < limit) {
        unsigned int flags = (unsigned int) (unsigned char) ((jbyte *) states->data)[next];
        if ((flags | (unsigned int) required) == flags && (flags & (unsigned int) excluded) == 0U) {
            require(out[2 + copied] == ((jint *) rows->data)[next], "selected row occurrence mismatch");
            copied++;
        }
        next++;
    }
    jlong expected = ((jlong) next << 32) | (jlong) copied;
    require(actual == expected, "exclusive cursor/count mismatch");
    for (jsize i = 0; i < output->size; i++) {
        if (i < 2 || i >= 2 + copied) require(out[i] == 0x74ac013d, "output guard changed");
    }
    require(row_reads == want_row_calls, "row-region call count changed");
    require(state_reads == want_state_calls, "state-region call count changed");
    require(writes == want_writes, "output-region batching changed");
    require(row_elements == (suffix_mode ? suffix_rows : baseline_rows), "row element transfer count");
    require(state_elements == want_states, "state transfer count changed");
    require(written_elements == copied, "output transfer count changed");
    printf("REGION_CASE name=%s mode=%s rowCalls=%d stateCalls=%d outputCalls=%d rowElements=%lld stateElements=%lld outputElements=%lld\n",
            name, suffix_mode ? "candidate" : "baseline", row_reads, state_reads, writes,
            row_elements, state_elements, written_elements);
}

int main(int argc, char **argv) {
    if (argc == 2 && strcmp(argv[1], "--baseline") == 0) suffix_mode = 0;
    else if (argc > 2 || (argc == 2 && strcmp(argv[1], "--candidate") != 0)) {
        fputs("usage: regions [--baseline|--candidate]\n", stderr); return 2;
    }
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception; api.IsSameObject = same;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.SetIntArrayRegion = put;
    JNIEnv env = &api;
    static jint rows[32768], output[4100];
    static jbyte states[32768];
    for (int i = 0; i < 32768; i++) rows[i] = i - 512;
    fixture_array r = {1024, rows}, s = {1024, states}, o = {4100, output};

    /* Preserve the original rejected/single-hit/contradictory/all-pass witnesses. */
    run(&env, "rejected", &r, &s, &o, 0, 1024, 0, 128, 0, 32, 0, 8, 0, 0, 0, 1024);
    states[1000] = (jbyte) 128;
    run(&env, "single-hit", &r, &s, &o, 0, 1024, 0, 128, 0, 32, 1, 8, 1, 128, 24, 1024);
    run(&env, "contradictory", &r, &s, &o, 0, 1024, 0, 128, 128, 32, 0, 0, 0, 0, 0, 0);
    run(&env, "all-pass", &r, &s, &o, 1000, 1024, 0, 0, 0, 32, 1, 0, 1, 24, 24, 0);

    r.size = s.size = 32768;
    memset(states, 0, sizeof(states));
    for (int i = 127; i < 32768; i += 128) states[i] = (jbyte) 128;
    run(&env, "one-tail-hit-per-chunk", &r, &s, &o, 0, 32768, 0, 128, 0, 256,
            256, 256, 2, 32768, 256, 32768);
    run(&env, "sparse-early-limit", &r, &s, &o, 0, 32768, 0, 128, 0, 1,
            1, 1, 1, 128, 1, 128);

    memset(states, 128, sizeof(states));
    run(&env, "dense-window", &r, &s, &o, 0, 32768, 0, 128, 0, 4096,
            32, 32, 32, 4096, 4096, 4096);
    run(&env, "dense-early-limit", &r, &s, &o, 0, 32768, 0, 128, 0, 1,
            1, 1, 1, 128, 128, 128);

    memset(states, 0, sizeof(states));
    states[134] = states[262] = states[269] = (jbyte) 128;
    run(&env, "bounded-unaligned-three-tails", &r, &s, &o, 7, 270, 1, 128, 0, 32,
            3, 3, 1, 263, 3, 263);
    run(&env, "bounded-tail-early-limit", &r, &s, &o, 7, 270, 1, 128, 0, 1,
            1, 1, 1, 128, 1, 128);
    memset(states, 0, sizeof(states)); states[64] = (jbyte) 128;
    run(&env, "bounded-partial-chunk", &r, &s, &o, 3, 65, 1, 128, 0, 32,
            1, 1, 1, 62, 1, 62);
    memset(states, 0, sizeof(states));
    run(&env, "bounded-rejected", &r, &s, &o, 7, 270, 1, 128, 0, 32,
            0, 3, 0, 0, 0, 263);
    run(&env, "bounded-zero-work", &r, &s, &o, 7, 7, 1, 128, 0, 32,
            0, 0, 0, 0, 0, 0);
    run(&env, "bounded-zero-limit", &r, &s, &o, 7, 270, 1, 128, 0, 0,
            0, 0, 0, 0, 0, 0);
    run(&env, "bounded-all-pass", &r, &s, &o, 7, 270, 1, 0, 0, 2,
            1, 0, 1, 2, 2, 0);
    run(&env, "bounded-contradictory", &r, &s, &o, 7, 270, 1, 128, 128, 32,
            0, 0, 0, 0, 0, 0);
    puts("PASS JNI region transfer and batching; actual JVM/JNI parity is a separate gate");
    return 0;
}
