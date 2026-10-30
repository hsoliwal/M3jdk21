/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project:
 * derive exact JNI region traces from independently selected source ordinals. */
/* Instrumented JNI work and failure protocol; real-JVM tests prove JNI parity. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
typedef struct { char kind; jint start, count; } transfer;
enum { FULL_CHUNK, SUFFIX_ONLY, ADMITTED_SPAN };
enum { CHUNK = 128, MAX_SELECTED = 4096, MAX_TRANSFERS = 2048, GUARD = 0x74ac013d };
static int transfer_mode = ADMITTED_SPAN;
static int row_reads, state_reads, writes;
static long long row_elements, state_elements, written_elements;
static const char *case_name = "initialization";
static transfer expected_transfers[MAX_TRANSFERS];
static jint ordinals[MAX_SELECTED], selected_values[MAX_SELECTED];
static int transfer_count, observed_transfers, fault_at, pending_exception;
static jint expected_next, selected_count, completed_output;
static fixture_array *expected_rows, *expected_states, *expected_output;
static unsigned int successful_cases, failure_cases;

static void require(int valid, const char *message) {
    if (!valid) { fprintf(stderr, "%s: %s\n", case_name, message); exit(1); }
}

static void region(fixture_array *array, jsize start, jsize count) {
    require(start >= 0 && start <= array->size && count > 0 && count <= CHUNK
            && count <= array->size - start, "native region outside fixed source/output bounds");
}

static void expect(char kind, jint start, jint count) {
    require(transfer_count < MAX_TRANSFERS, "reference trace capacity");
    expected_transfers[transfer_count++] = (transfer) {kind, start, count};
}

/* Select globally using the scalar all-bits oracle first. Then group that immutable
 * ordinal list into the documented fixed source chunks and output batches. This
 * does not copy the owner's offset-buffer/consumed-state implementation. */
static void plan(fixture_array *rows, fixture_array *states, jint first, jint end,
        jint required, jint excluded, jint limit) {
    transfer_count = 0;
    selected_count = 0;
    expected_next = first;
    while (expected_next < end && selected_count < limit) {
        unsigned int flags = (unsigned int) (unsigned char) ((jbyte *) states->data)[expected_next];
        if ((flags | (unsigned int) required) == flags && (flags & (unsigned int) excluded) == 0U) {
            ordinals[selected_count] = expected_next;
            selected_values[selected_count++] = ((jint *) rows->data)[expected_next];
        }
        expected_next++;
    }
    if (limit == 0 || first == end || (required & excluded) != 0) return;
    if ((required | excluded) == 0) {
        for (jint i = 0; i < selected_count; i += CHUNK) {
            jint count = selected_count - i < CHUNK ? selected_count - i : CHUNK;
            expect('R', first + i, count);
            expect('W', 2 + i, count);
        }
        return;
    }
    jint selection = 0, batch = 0, emitted = 0;
    for (jint base = first; base < expected_next; base += CHUNK) {
        jint count = end - base < CHUNK ? end - base : CHUNK;
        expect('S', base, count);
        jint begin = selection;
        while (selection < selected_count && ordinals[selection] < base + count) selection++;
        if (begin == selection) continue;
        jint low = transfer_mode == FULL_CHUNK ? base : ordinals[begin];
        jint high = transfer_mode == ADMITTED_SPAN ? ordinals[selection - 1] + 1 : base + count;
        expect('R', low, high - low);
        for (jint i = begin; i < selection; i++) {
            batch++;
            emitted++;
            if (batch == CHUNK || emitted == limit) {
                expect('W', 2 + emitted - batch, batch);
                batch = 0;
            }
        }
    }
    if (batch != 0) expect('W', 2 + emitted - batch, batch);
}

static int observe(char kind, jint start, jint count) {
    require(!pending_exception, "JNI work continued after pending exception");
    require(observed_transfers < transfer_count, "unexpected additional JNI transfer");
    transfer wanted = expected_transfers[observed_transfers++];
    require(kind == wanted.kind, "JNI transfer order changed");
    require(start == wanted.start && count == wanted.count, "JNI transfer span differs from admitted ordinal oracle");
    if (observed_transfers == fault_at) {
        pending_exception = 1;
        return 0;
    }
    return 1;
}

static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void) env;
    require(!pending_exception, "array length read after pending exception");
    return ((fixture_array *) array)->size;
}
static jboolean JNICALL exception(JNIEnv *env) { (void) env; return pending_exception ? JNI_TRUE : JNI_FALSE; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) {
    (void) env;
    require(!pending_exception, "alias lookup after pending exception");
    return a == b;
}
static void JNICALL bytes(JNIEnv *env, jbyteArray array, jsize start, jsize count, jbyte *out) {
    (void) env;
    fixture_array *source = (fixture_array *) array;
    require(source == expected_states, "state read used another array");
    region(source, start, count);
    state_reads++; state_elements += count;
    if (observe('S', start, count)) memcpy(out, (jbyte *) source->data + start, (size_t) count);
}
static void JNICALL ints(JNIEnv *env, jintArray array, jsize start, jsize count, jint *out) {
    (void) env;
    fixture_array *source = (fixture_array *) array;
    require(source == expected_rows, "row read used another array");
    region(source, start, count);
    row_reads++; row_elements += count;
    if (observe('R', start, count)) memcpy(out, (jint *) source->data + start, (size_t) count * sizeof(jint));
}
static void JNICALL put(JNIEnv *env, jintArray array, jsize start, jsize count, const jint *in) {
    (void) env;
    fixture_array *target = (fixture_array *) array;
    require(target == expected_output, "output write used another array");
    region(target, start, count);
    writes++; written_elements += count;
    if (observe('W', start, count)) {
        memcpy((jint *) target->data + start, in, (size_t) count * sizeof(jint));
        completed_output += count;
    }
}

static jint * JNICALL pin_ints(JNIEnv *env, jintArray array, jboolean *copy) {
    (void) env; (void) array; (void) copy;
    require(0, "whole-array row pin/copy is outside the region contract"); return NULL;
}
static jbyte * JNICALL pin_bytes(JNIEnv *env, jbyteArray array, jboolean *copy) {
    (void) env; (void) array; (void) copy;
    require(0, "whole-array state pin/copy is outside the region contract"); return NULL;
}
static void * JNICALL pin_any(JNIEnv *env, jarray array, jboolean *copy) {
    (void) env; (void) array; (void) copy;
    require(0, "critical array pin is outside the region contract"); return NULL;
}
static jobject JNICALL retain(JNIEnv *env, jobject value) {
    (void) env; (void) value;
    require(0, "global retention is outside the region contract"); return NULL;
}

extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jintArray, jint, jint);
extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jint, jintArray, jint, jint);

static void execute(JNIEnv *env, const char *name, fixture_array *rows, fixture_array *states,
        fixture_array *output, jint first, jint end, int bounded, jint required, jint excluded,
        jint limit, int fail_transfer) {
    case_name = name;
    if (!bounded) end = rows->size;
    plan(rows, states, first, end, required, excluded, limit);
    row_reads = state_reads = writes = 0;
    row_elements = state_elements = written_elements = 0;
    observed_transfers = pending_exception = completed_output = 0;
    fault_at = fail_transfer;
    expected_rows = rows; expected_states = states; expected_output = output;
    jint *out = (jint *) output->data;
    for (jsize i = 0; i < output->size; i++) out[i] = GUARD;
    jlong actual = bounded
            ? Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(env, NULL,
                (jintArray) rows, (jbyteArray) states, first, end, required, excluded,
                (jintArray) output, 2, limit)
            : Java_com_m3_collections_M3IndexMaskWindow_scanNative(env, NULL,
                (jintArray) rows, (jbyteArray) states, first, required, excluded,
                (jintArray) output, 2, limit);
    if (fail_transfer != 0) {
        require(actual == -1 && pending_exception, "JNI failure did not preserve pending exception/result");
        require(observed_transfers == fail_transfer, "JNI failure did not stop at the injected transfer");
        failure_cases++;
    } else {
        jlong expected = ((jlong) expected_next << 32) | (jlong) selected_count;
        require(actual == expected, "exclusive cursor/count mismatch");
        require(observed_transfers == transfer_count && !pending_exception, "JNI region trace incomplete");
        require(completed_output == selected_count, "selected output occurrence count changed");
        successful_cases++;
    }
    for (jsize i = 0; i < output->size; i++) {
        if (i >= 2 && i < 2 + completed_output) {
            require(out[i] == selected_values[i - 2], "selected row occurrence mismatch");
        } else require(out[i] == GUARD, "output prefix/suffix guard changed");
    }
}

/* Preserve all original hard-coded call, byte-transfer and batching witnesses.
 * The current mode derives its exact spans from selected ordinals. Historical
 * modes retain their old byte counts for explicit predecessor/control replay. */
static void run(JNIEnv *env, const char *name, fixture_array *rows, fixture_array *states,
        fixture_array *output, jint first, jint end, int bounded, jint required, jint excluded,
        jint limit, int want_row_calls, int want_state_calls, int want_writes,
        long long baseline_rows, long long suffix_rows, long long want_states) {
    execute(env, name, rows, states, output, first, end, bounded, required, excluded, limit, 0);
    require(row_reads == want_row_calls, "row-region call count changed");
    require(state_reads == want_state_calls, "state-region call count changed");
    require(writes == want_writes, "output-region batching changed");
    if (transfer_mode != ADMITTED_SPAN) {
        require(row_elements == (transfer_mode == SUFFIX_ONLY ? suffix_rows : baseline_rows),
                "historical row element transfer count");
    }
    require(state_elements == want_states, "state transfer count changed");
    require(written_elements == selected_count, "output transfer count changed");
    printf("REGION_CASE name=%s mode=%s rowCalls=%d stateCalls=%d outputCalls=%d rowElements=%lld stateElements=%lld outputElements=%lld\n",
            name, transfer_mode == ADMITTED_SPAN ? "admitted-span" : transfer_mode == SUFFIX_ONLY ? "suffix" : "baseline",
            row_reads, state_reads, writes, row_elements, state_elements, written_elements);
}

static uint32_t random_bits(uint32_t *state) {
    *state = *state * UINT32_C(1664525) + UINT32_C(1013904223); return *state;
}

static void matrices(JNIEnv *env, jint *rows, jbyte *states, jint *output) {
    fixture_array r = {1024, rows}, s = {1024, states}, o = {4100, output};
    static const jint limits[] = {0, 1, 2, 31, 127, 128, 129, 256, 4096};
    uint32_t seed = UINT32_C(0x73a2bd19);
    for (int trial = 0; trial < 2048; trial++) {
        r.size = s.size = (jsize) (random_bits(&seed) % 1025U);
        for (jsize i = 0; i < r.size; i++) {
            /* Reuse the same array identities with fresh masks and duplicate payloads. */
            states[i] = (jbyte) (random_bits(&seed) >> 24);
            rows[i] = (jint) (random_bits(&seed) % 31U) - 15;
        }
        jint first = (jint) (random_bits(&seed) % ((uint32_t) r.size + 1U));
        jint end = first + (jint) (random_bits(&seed) % ((uint32_t) (r.size - first) + 1U));
        jint required = (jint) (random_bits(&seed) & 255U);
        jint excluded = (jint) (random_bits(&seed) & 255U);
        if ((trial & 3) != 0) excluded &= ~required;
        execute(env, "seeded-mask-window", &r, &s, &o, first, end, trial & 1, required, excluded,
                limits[trial % (int) (sizeof limits / sizeof limits[0])], 0);
    }
    r.size = s.size = 513;
    for (int i = 0; i < 513; i++) { rows[i] = i % 9 - 4; states[i] = (jbyte) 128; }
    for (int bounded = 0; bounded <= 1; bounded++) {
        for (int all = 0; all <= 1; all++) {
            execute(env, "fault-baseline", &r, &s, &o, 7, 507, bounded, all ? 0 : 128, 0, 300, 0);
            int events = transfer_count;
            for (int fail = 1; fail <= events; fail++) {
                execute(env, "every-transfer-failure", &r, &s, &o, 7, 507, bounded,
                        all ? 0 : 128, 0, 300, fail);
            }
        }
    }
    /* Exhaust every single-hit position, including first/last and chunk edges. */
    for (int hit = 0; hit < 513; hit++) {
        memset(states, 0, 513);
        states[hit] = (jbyte) 131;
        rows[hit] = hit - 256;
        for (int bounded = 0; bounded <= 1; bounded++) {
            execute(env, "one-admitted-ordinal", &r, &s, &o, 0, 513, bounded, 129, 4, 1, 0);
        }
    }
    /* Distinguish all-bit admission, exclusion and mutations between invocations. */
    for (int i = 0; i < 513; i++) { rows[i] = i - 256; states[i] = (jbyte) (i & 7); }
    execute(env, "required-all-bits", &r, &s, &o, 3, 509, 1, 3, 4, 256, 0);
    for (int i = 0; i < 513; i++) { rows[i] = 256 - i; states[i] ^= (jbyte) 7; }
    execute(env, "same-array-mutated-content", &r, &s, &o, 3, 509, 1, 3, 4, 256, 0);
}

int main(int argc, char **argv) {
    if (argc == 2 && strcmp(argv[1], "--baseline") == 0) transfer_mode = FULL_CHUNK;
    else if (argc == 2 && strcmp(argv[1], "--suffix") == 0) transfer_mode = SUFFIX_ONLY;
    else if (argc > 2 || (argc == 2 && strcmp(argv[1], "--candidate") != 0)) {
        fputs("usage: regions [--baseline|--suffix|--candidate]\n", stderr); return 2;
    }
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception; api.IsSameObject = same;
    api.GetByteArrayRegion = bytes; api.GetIntArrayRegion = ints; api.SetIntArrayRegion = put;
    api.GetIntArrayElements = pin_ints; api.GetByteArrayElements = pin_bytes;
    api.GetPrimitiveArrayCritical = pin_any; api.NewGlobalRef = retain;
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
    matrices(&env, rows, states, output);
    printf("PASS JNI region traces successful=%u injectedFailures=%u; real JVM/JNI parity is separate\n", successful_cases, failure_cases);
    return 0;
}
