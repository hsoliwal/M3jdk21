/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * Independent refinement JNI-table model for the actual exported compactor functions.
 * Template only: NOT COMPILED / NOT EXECUTED by its author.
 * Link this translation unit with the unchanged full baseline or generated full
 * candidate m3_index_mask_compactor.c. This is not a JVM or JNI conformance proof.
 * Real Java/JNI execution, generated headers and export comparison are separate gates.
 */
#include <jni.h>
#include <inttypes.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define PAGE 256
#define CAPACITY 4608
#define MAX_PAGES ((CAPACITY + PAGE - 1) / PAGE)

typedef enum { BYTE_LANE, INT_LANE, LONG_LANE } lane_kind;
typedef enum { BASELINE, SKIP_PREFIX } admission_mode;
typedef enum { NO_CHANGE, COUNT_INCREASE, COUNT_DECREASE, EQUAL_COUNT_MOVE,
               ZERO_COUNT_GROWTH } injected_change;
typedef enum { REJECT_ALL, ACCEPT_ALL, ALTERNATING, LAST_ONLY, PAGE_EDGES,
               EMPTY_MIDDLE, UNSIGNED_WORDS, DRIFT_THREE, SEEDED_WORDS } pattern_kind;

typedef struct {
    jsize size;
    lane_kind kind;
    void *data;
} fixture_array;

typedef struct {
    const char *name;
    jint length;
    jint row_offset;
    jint state_offset;
    jint required;
    jint excluded;
    pattern_kind pattern;
    uint32_t seed;
} case_spec;

typedef struct {
    jint ints[CAPACITY];
    jlong longs[CAPACITY];
    jbyte states[CAPACITY];
    jint saved_ints[CAPACITY];
    jlong saved_longs[CAPACITY];
    jbyte saved_states[CAPACITY];
    fixture_array rows;
    fixture_array marks;
} fixture_frame;

typedef struct {
    const case_spec *spec;
    fixture_frame *frame;
    lane_kind width;
    admission_mode mode;
    injected_change change;
    fixture_array output;
    jint expected_kept;
    jint written;
    int allocations;
    int length_calls;
    int count_calls;
    int emit_calls;
    uint64_t count_bytes;
    uint64_t emit_bytes;
    int payload_calls;
    uint64_t payload_elements;
    uint64_t payload_bytes;
    int rejected_payload_calls;
    int writes;
    int find_calls;
    int throw_calls;
    int delete_calls;
    int pending;
    int fail_allocation;
    int injection_seen;
    char exception_class[128];
    char exception_message[192];
    jbyte emitted_marks[PAGE];
    jint emitted_relative_start;
    jsize emitted_size;
    unsigned char count_visits[CAPACITY];
    unsigned char emit_visits[CAPACITY];
    unsigned char payload_page_calls[MAX_PAGES];
} fixture_context;

static fixture_context current;
static fixture_frame frame;
static admission_mode mode;
static uint64_t checks;
static uint64_t cases;
static uint64_t digest = UINT64_C(0xcbf29ce484222325);
static uint64_t total_count_bytes;
static uint64_t total_emit_bytes;
static uint64_t total_payload_bytes;
static uint64_t total_payload_calls;
static unsigned char exception_token;

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_windowMasksAbiNative(JNIEnv *, jclass);
JNIEXPORT jintArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectWindowMasksNative(
        JNIEnv *, jclass, jintArray, jint, jbyteArray, jint, jint, jint, jint);
JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowMasksNative(
        JNIEnv *, jclass, jlongArray, jint, jbyteArray, jint, jint, jint, jint);

static void need(int condition, const char *finding) {
    checks++;
    if (!condition) {
        fprintf(stderr, "FAIL JNI_TABLE_MODEL case=%s width=%s finding=%s check=%" PRIu64 "\n",
                current.spec == NULL ? "setup" : current.spec->name,
                current.width == LONG_LANE ? "long" : "int", finding, checks);
        exit(1);
    }
}

static void mix(uint64_t value) {
    for (unsigned int shift = 0; shift < 64; shift += 8) {
        digest ^= (value >> shift) & UINT64_C(255);
        digest *= UINT64_C(0x100000001b3);
    }
}

/* Check each required/forbidden bit as separate obligations, not the C owner's expression. */
static int oracle_admits(jbyte state, jint required, jint excluded) {
    unsigned int bits = (unsigned int)(unsigned char)state;
    for (unsigned int bit = 1; bit <= 128; bit <<= 1) {
        int present = (bits & bit) != 0U;
        if (((unsigned int)required & bit) != 0U && !present) return 0;
        if (((unsigned int)excluded & bit) != 0U && present) return 0;
    }
    return 1;
}

static jint oracle_count(const jbyte *states, const case_spec *spec) {
    jint count = 0;
    for (jint i = 0; i < spec->length; i++) {
        if (oracle_admits(states[spec->state_offset + i], spec->required, spec->excluded)) count++;
    }
    return count;
}

static uint32_t next_word(uint32_t *state) {
    *state = *state * UINT32_C(1664525) + UINT32_C(1013904223);
    return *state;
}

static void prepare(const case_spec *spec, lane_kind width, injected_change change) {
    memset(&current, 0, sizeof(current));
    current.spec = spec;
    current.frame = &frame;
    current.width = width;
    current.mode = mode;
    current.change = change;
    current.emitted_relative_start = -1;
    need(spec->row_offset + spec->length + 7 <= CAPACITY, "row fixture capacity");
    need(spec->state_offset + spec->length + 11 <= CAPACITY, "state fixture capacity");
    for (jint i = 0; i < CAPACITY; i++) {
        frame.ints[i] = INT32_C(1515870810);
        frame.longs[i] = INT64_C(6510615555426900570);
        frame.states[i] = (jbyte)0xa5;
    }
    uint32_t random = spec->seed;
    for (jint i = 0; i < spec->length; i++) {
        switch (i % 8) {
            case 0: case 3:
                frame.ints[spec->row_offset + i] = INT32_MIN;
                frame.longs[spec->row_offset + i] = INT64_MIN;
                break;
            case 1: case 6:
                frame.ints[spec->row_offset + i] = INT32_MAX;
                frame.longs[spec->row_offset + i] = INT64_MAX;
                break;
            case 2:
                frame.ints[spec->row_offset + i] = 0;
                frame.longs[spec->row_offset + i] = 0;
                break;
            case 4:
                frame.ints[spec->row_offset + i] = -7;
                frame.longs[spec->row_offset + i] = -7;
                break;
            case 5:
                frame.ints[spec->row_offset + i] = 7;
                frame.longs[spec->row_offset + i] = 7;
                break;
            default:
                frame.ints[spec->row_offset + i] = i % 13 - 6;
                frame.longs[spec->row_offset + i] = (INT64_C(1) << 40) + i % 13;
                break;
        }
        unsigned int word;
        switch (spec->pattern) {
            case REJECT_ALL: word = 0; break;
            case ACCEPT_ALL: word = 1; break;
            case ALTERNATING: word = i % 2 == 0 ? 1U : 129U; break;
            case LAST_ONLY: word = i == spec->length - 1 ? 1U : 0U; break;
            case PAGE_EDGES:
                word = i == 0 || i == spec->length - 1 || i % PAGE == 0 || i % PAGE == PAGE - 1
                        ? 1U : 128U;
                break;
            case EMPTY_MIDDLE:
                word = i >= PAGE && i < 2 * PAGE ? 128U : (i % 2 == 0 ? 1U : 129U);
                break;
            case UNSIGNED_WORDS: word = (unsigned int)i & 255U; break;
            case DRIFT_THREE: word = i == 17 || i == 270 || i == 590 ? 1U : 0U; break;
            case SEEDED_WORDS: word = next_word(&random) >> 24; break;
            default: need(0, "known state pattern"); word = 0; break;
        }
        frame.states[spec->state_offset + i] = (jbyte)(unsigned char)word;
    }
    memcpy(frame.saved_ints, frame.ints, sizeof(frame.ints));
    memcpy(frame.saved_longs, frame.longs, sizeof(frame.longs));
    memcpy(frame.saved_states, frame.states, sizeof(frame.states));
    frame.rows.size = spec->row_offset + spec->length + 7;
    frame.rows.kind = width;
    frame.rows.data = width == INT_LANE ? (void *)frame.ints : (void *)frame.longs;
    frame.marks.size = spec->state_offset + spec->length + 11;
    frame.marks.kind = BYTE_LANE;
    frame.marks.data = frame.states;
    current.expected_kept = oracle_count(frame.saved_states, spec);
}

static fixture_array *as_array(jarray array, lane_kind kind) {
    fixture_array *value = (fixture_array *)(void *)array;
    need(value != NULL, "non-null JNI array handle");
    need(value->kind == kind, "JNI primitive element width");
    return value;
}

static void checked_region(fixture_array *array, jsize start, jsize size) {
    need(current.pending == 0, "no region operation after a pending exception");
    need(start >= 0 && size >= 0 && start <= array->size && size <= array->size - start,
            "JNI region stays within array");
}

static jsize JNICALL array_length(JNIEnv *env, jarray array) {
    (void)env;
    need(current.pending == 0, "no length read after a pending exception");
    fixture_array *value = (fixture_array *)(void *)array;
    need(value == &frame.rows || value == &frame.marks, "length reads target borrowed input lanes");
    current.length_calls++;
    return value->size;
}

static jboolean JNICALL exception_check(JNIEnv *env) {
    (void)env;
    return current.pending ? JNI_TRUE : JNI_FALSE;
}

static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;
    need(current.pending == 0, "exception class resolution has no preceding pending exception");
    current.find_calls++;
    (void)snprintf(current.exception_class, sizeof(current.exception_class), "%s", name);
    return (jclass)(void *)&exception_token;
}

static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    need(type == (jclass)(void *)&exception_token, "ThrowNew uses the resolved class");
    current.throw_calls++;
    current.pending = 1;
    (void)snprintf(current.exception_message, sizeof(current.exception_message), "%s", message);
    return 0;
}

static void JNICALL delete_local_ref(JNIEnv *env, jobject reference) {
    (void)env;
    need(reference == (jobject)(void *)&exception_token, "delete exact exception local reference");
    current.delete_calls++;
}

static void inject_between_passes(void) {
    const case_spec *spec = current.spec;
    if (current.change == NO_CHANGE) return;
    need(spec->length == 600 && spec->required == 1 && spec->excluded == 128,
            "frozen mutation fixture geometry");
    current.injection_seen++;
    if (current.change == COUNT_INCREASE || current.change == ZERO_COUNT_GROWTH) {
        frame.states[spec->state_offset + 18] = 1;
    } else if (current.change == COUNT_DECREASE) {
        frame.states[spec->state_offset + 590] = 0;
    } else {
        frame.states[spec->state_offset + 17] = 0;
        frame.states[spec->state_offset + 18] = 1;
    }
}

static jobject new_result(lane_kind width, jsize size) {
    need(current.pending == 0, "no allocation after a pending exception");
    need(current.allocations == 0, "one detached result allocation");
    need(width == current.width, "result primitive width");
    need(size == current.expected_kept, "independent exact result size");
    current.allocations++;
    if (current.fail_allocation) {
        current.pending = 1;
        (void)snprintf(current.exception_class, sizeof(current.exception_class),
                "%s", "java/lang/OutOfMemoryError");
        (void)snprintf(current.exception_message, sizeof(current.exception_message),
                "%s", "fixture result allocation failure");
        return NULL;
    }
    size_t element = width == INT_LANE ? sizeof(jint) : sizeof(jlong);
    current.output.data = calloc(size == 0 ? 1U : (size_t)size, element);
    need(current.output.data != NULL, "test process has space for its result model");
    current.output.size = size;
    current.output.kind = width;
    inject_between_passes();
    return (jobject)(void *)&current.output;
}

static jintArray JNICALL new_int_array(JNIEnv *env, jsize size) {
    (void)env;
    return (jintArray)new_result(INT_LANE, size);
}

static jlongArray JNICALL new_long_array(JNIEnv *env, jsize size) {
    (void)env;
    return (jlongArray)new_result(LONG_LANE, size);
}

static void JNICALL get_bytes(JNIEnv *env, jbyteArray source, jsize start, jsize size, jbyte *out) {
    (void)env;
    fixture_array *array = as_array((jarray)source, BYTE_LANE);
    need(array == &frame.marks, "state region uses original state lane");
    checked_region(array, start, size);
    const case_spec *spec = current.spec;
    int emission = current.allocations != 0;
    int ordinal = emission ? current.emit_calls : current.count_calls;
    jint relative = (jint)ordinal * PAGE;
    jint remaining = spec->length - relative;
    jsize expected = remaining < PAGE ? remaining : PAGE;
    need(relative >= 0 && relative < spec->length, "state pass has no duplicate or excess page");
    need(start == spec->state_offset + relative && size == expected,
            "exact sequential state page geometry in each pass");
    memcpy(out, (jbyte *)array->data + start, (size_t)size);
    for (jsize i = 0; i < size; i++) {
        if (emission) current.emit_visits[relative + i]++;
        else current.count_visits[relative + i]++;
    }
    if (emission) {
        current.emit_calls++;
        current.emit_bytes += (uint64_t)size;
        current.emitted_relative_start = relative;
        current.emitted_size = size;
        memcpy(current.emitted_marks, out, (size_t)size);
    } else {
        current.count_calls++;
        current.count_bytes += (uint64_t)size;
    }
}

static void record_payload(fixture_array *array, jsize start, jsize size, lane_kind width) {
    need(array == &frame.rows, "payload reads original row lane");
    checked_region(array, start, size);
    need(current.allocations == 1 && current.emitted_relative_start >= 0,
            "payload follows result allocation and its second-pass state page");
    jsize first = 0;
    while (first < current.emitted_size && !oracle_admits(current.emitted_marks[first],
            current.spec->required, current.spec->excluded)) first++;
    int rejected = first == current.emitted_size;
    if (current.mode == SKIP_PREFIX) need(!rejected, "candidate reads no wholly rejected payload page");
    jsize prefix = current.mode == BASELINE ? 0 : first;
    need(start == current.spec->row_offset + current.emitted_relative_start + prefix,
            "payload starts at full page or first admitted ordinal according to admission mode");
    need(size == current.emitted_size - prefix,
            "payload includes exact suffix through page end; no last-match reverse scan");
    jint page = current.emitted_relative_start / PAGE;
    need(current.payload_page_calls[page] == 0, "at most one payload region per emission page");
    current.payload_page_calls[page]++;
    current.payload_calls++;
    current.payload_elements += (uint64_t)size;
    current.payload_bytes += (uint64_t)size * (width == INT_LANE ? sizeof(jint) : sizeof(jlong));
    if (rejected) current.rejected_payload_calls++;
}

static void JNICALL get_ints(JNIEnv *env, jintArray source, jsize start, jsize size, jint *out) {
    (void)env;
    fixture_array *array = as_array((jarray)source, INT_LANE);
    record_payload(array, start, size, INT_LANE);
    memcpy(out, (jint *)array->data + start, (size_t)size * sizeof(jint));
}

static void JNICALL get_longs(JNIEnv *env, jlongArray source, jsize start, jsize size, jlong *out) {
    (void)env;
    fixture_array *array = as_array((jarray)source, LONG_LANE);
    record_payload(array, start, size, LONG_LANE);
    memcpy(out, (jlong *)array->data + start, (size_t)size * sizeof(jlong));
}

static void record_output(fixture_array *array, jsize start, jsize size) {
    need(array == &current.output, "writes target only the detached result");
    checked_region(array, start, size);
    need(size > 0 && start == current.written, "positive contiguous stable output regions");
    current.writes++;
    current.written += size;
}

static void JNICALL put_ints(JNIEnv *env, jintArray target, jsize start, jsize size, const jint *in) {
    (void)env;
    fixture_array *array = as_array((jarray)target, INT_LANE);
    record_output(array, start, size);
    memcpy((jint *)array->data + start, in, (size_t)size * sizeof(jint));
}

static void JNICALL put_longs(JNIEnv *env, jlongArray target, jsize start, jsize size, const jlong *in) {
    (void)env;
    fixture_array *array = as_array((jarray)target, LONG_LANE);
    record_output(array, start, size);
    memcpy((jlong *)array->data + start, in, (size_t)size * sizeof(jlong));
}

static jobject invoke(JNIEnv *env, jobject rows, jbyteArray states, jint row_offset,
        jint state_offset, jint length, jint required, jint excluded) {
    if (current.width == INT_LANE) {
        return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectWindowMasksNative(
                env, NULL, (jintArray)rows, row_offset, states, state_offset, length, required, excluded);
    }
    return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowMasksNative(
            env, NULL, (jlongArray)rows, row_offset, states, state_offset, length, required, excluded);
}

static jobject call_current(JNIEnv *env) {
    const case_spec *spec = current.spec;
    return invoke(env, (jobject)(void *)&frame.rows, (jbyteArray)(void *)&frame.marks,
            spec->row_offset, spec->state_offset, spec->length, spec->required, spec->excluded);
}

static void inputs_unchanged(int states_unchanged) {
    need(memcmp(frame.ints, frame.saved_ints, sizeof(frame.ints)) == 0, "all int input/canary bytes unchanged");
    need(memcmp(frame.longs, frame.saved_longs, sizeof(frame.longs)) == 0, "all long input/canary bytes unchanged");
    if (states_unchanged) {
        need(memcmp(frame.states, frame.saved_states, sizeof(frame.states)) == 0,
                "all state input/canary bytes unchanged");
    }
}

static void state_schedule(int emission) {
    const case_spec *spec = current.spec;
    int count_pass = (spec->required & spec->excluded) == 0 && (spec->required | spec->excluded) != 0;
    int pages = (spec->length + PAGE - 1) / PAGE;
    need(current.count_calls == (count_pass ? pages : 0), "preserve count-pass state calls");
    need(current.emit_calls == (emission ? pages : 0), "preserve second-pass state calls");
    need(current.count_bytes == (count_pass ? (uint64_t)spec->length : 0), "preserve count-pass state bytes");
    need(current.emit_bytes == (emission ? (uint64_t)spec->length : 0), "preserve second-pass state bytes");
    for (jint i = 0; i < spec->length; i++) {
        need(current.count_visits[i] == (unsigned char)count_pass, "each count-pass state observed exactly once");
        need(current.emit_visits[i] == (unsigned char)emission, "each emission state observed exactly once");
    }
}

static void payload_schedule(const jbyte *observed_states, int emission, int check_writes) {
    const case_spec *spec = current.spec;
    int expected_calls = 0;
    int expected_writes = 0;
    uint64_t expected_elements = 0;
    for (jint relative = 0; relative < spec->length; relative += PAGE) {
        jint size = spec->length - relative;
        if (size > PAGE) size = PAGE;
        jint first = 0;
        while (first < size && !oracle_admits(observed_states[spec->state_offset + relative + first],
                spec->required, spec->excluded)) first++;
        int selected = first < size;
        int expected_page_call = emission && (current.mode == BASELINE || selected);
        need(current.payload_page_calls[relative / PAGE] == (unsigned char)expected_page_call,
                "per-page payload presence matches exact admission mode");
        if (expected_page_call) {
            expected_calls++;
            expected_elements += (uint64_t)(current.mode == BASELINE ? size : size - first);
        }
        if (emission && selected) expected_writes++;
    }
    need(current.payload_calls == expected_calls, "independent payload region count");
    need(current.payload_elements == expected_elements, "independent payload element count");
    need(current.payload_bytes == expected_elements
            * (current.width == INT_LANE ? sizeof(jint) : sizeof(jlong)), "independent payload byte count");
    if (current.mode == SKIP_PREFIX) need(current.rejected_payload_calls == 0, "zero rejected-page payload calls");
    if (check_writes) need(current.writes == expected_writes, "one output region per admitted page");
}

static void result_matches(jobject result, const jbyte *observed_states, int zero_count_return) {
    need(current.pending == 0, "successful call has no pending exception");
    need(result == (jobject)(void *)&current.output, "returned detached allocated handle");
    need(result != (jobject)(void *)&frame.rows, "result is not the borrowed input handle");
    jint kept = zero_count_return ? 0 : oracle_count(observed_states, current.spec);
    need(current.output.size == kept, "oracle result length");
    need(current.written == kept, "all and only selected output elements written");
    jint ordinal = 0;
    mix((uint64_t)current.width);
    mix((uint64_t)current.spec->length);
    mix((uint64_t)kept);
    if (zero_count_return) return;
    for (jint i = 0; i < current.spec->length; i++) {
        if (!oracle_admits(observed_states[current.spec->state_offset + i],
                current.spec->required, current.spec->excluded)) continue;
        if (current.width == INT_LANE) {
            jint actual = ((jint *)current.output.data)[ordinal];
            need(actual == frame.ints[current.spec->row_offset + i], "stable int values including duplicate occurrences");
            mix((uint64_t)(uint32_t)actual);
        } else {
            jlong actual = ((jlong *)current.output.data)[ordinal];
            need(actual == frame.longs[current.spec->row_offset + i], "stable long values including signed extremes");
            mix((uint64_t)actual);
        }
        ordinal++;
    }
    need(ordinal == kept, "oracle emits exact cardinality");
}

static void finish_case(int report) {
    cases++;
    total_count_bytes += current.count_bytes;
    total_emit_bytes += current.emit_bytes;
    total_payload_calls += (uint64_t)current.payload_calls;
    total_payload_bytes += current.payload_bytes;
    if (report) {
        printf("REGIONS mode=%s case=%s width=%s length=%d kept=%d count_calls=%d emit_calls=%d"
                " count_bytes=%" PRIu64 " emit_bytes=%" PRIu64 " payload_calls=%d payload_bytes=%" PRIu64
                " rejected_payload_calls=%d writes=%d\n",
                current.mode == BASELINE ? "baseline" : "skip-prefix", current.spec->name,
                current.width == INT_LANE ? "int" : "long", current.spec->length, current.expected_kept,
                current.count_calls, current.emit_calls, current.count_bytes, current.emit_bytes,
                current.payload_calls, current.payload_bytes, current.rejected_payload_calls, current.writes);
    }
    free(current.output.data);
    current.output.data = NULL;
}

static void stable_case(JNIEnv *env, const case_spec *spec, lane_kind width, int report) {
    prepare(spec, width, NO_CHANGE);
    jobject result = call_current(env);
    need(current.length_calls == 2 && current.allocations == 1, "unchanged preflight and result allocation");
    result_matches(result, frame.saved_states, 0);
    int emission = current.expected_kept != 0;
    state_schedule(emission);
    payload_schedule(frame.saved_states, emission, 1);
    inputs_unchanged(1);
    finish_case(report);
}

static void drift_case(JNIEnv *env, lane_kind width, injected_change change) {
    case_spec spec = {"count-drift", 600, 3, 5, 1, 128,
            change == ZERO_COUNT_GROWTH ? REJECT_ALL : DRIFT_THREE, 0};
    prepare(&spec, width, change);
    jobject result = call_current(env);
    need(current.injection_seen == 1, "single deterministic mutation between count and emission");
    need(current.length_calls == 2 && current.allocations == 1, "drift keeps preflight and exact allocation");
    int emission = current.expected_kept != 0;
    state_schedule(emission);
    payload_schedule(frame.states, emission, change != COUNT_INCREASE);
    inputs_unchanged(0);
    for (jint i = 0; i < CAPACITY; i++) {
        jbyte expected = frame.saved_states[i];
        if (i == spec.state_offset + 18
                && (change == COUNT_INCREASE || change == EQUAL_COUNT_MOVE || change == ZERO_COUNT_GROWTH)) expected = 1;
        if (i == spec.state_offset + 590 && change == COUNT_DECREASE) expected = 0;
        if (i == spec.state_offset + 17 && change == EQUAL_COUNT_MOVE) expected = 0;
        need(frame.states[i] == expected, "only the fixture's declared mutation changes states");
    }
    if (change == COUNT_INCREASE || change == COUNT_DECREASE) {
        need(result == NULL && current.pending, "count mismatch returns NULL with pending exception");
        need(strcmp(current.exception_class, "java/util/ConcurrentModificationException") == 0,
                "exact existing drift exception class");
        need(strcmp(current.exception_message, "aligned selection states changed") == 0,
                "exact existing drift exception message");
        need(current.find_calls == 1 && current.throw_calls == 1 && current.delete_calls == 1,
                "drift throws once and releases exception class reference");
        need(current.written == (change == COUNT_INCREASE ? 3 : 2),
                "overflow checks precede overflowing write; underflow checks follow complete scan");
        mix((uint64_t)change);
    } else {
        /* Existing count checks do not promise arbitrary race detection. In particular,
         * an equal-cardinality move succeeds, and an initial zero count returns early. */
        result_matches(result, frame.states, change == ZERO_COUNT_GROWTH);
        need(current.throw_calls == 0, "preserve existing equal-count/zero-count behavior");
    }
    finish_case(0);
}

static void invalid_case(JNIEnv *env, lane_kind width, int variant) {
    case_spec spec = {"invalid-input", 8, 3, 5, 1, 128, ACCEPT_ALL, 0};
    prepare(&spec, width, NO_CHANGE);
    jobject rows = (jobject)(void *)&frame.rows;
    jbyteArray states = (jbyteArray)(void *)&frame.marks;
    jint row_offset = spec.row_offset, state_offset = spec.state_offset, length = spec.length;
    jint required = spec.required, excluded = spec.excluded;
    int expected_length_calls = 0;
    switch (variant) {
        case 0: rows = NULL; break;
        case 1: states = NULL; break;
        case 2: row_offset = -1; break;
        case 3: state_offset = -1; break;
        case 4: length = -1; break;
        case 5: required = -1; break;
        case 6: required = 256; break;
        case 7: excluded = -1; break;
        case 8: excluded = 256; break;
        case 9: row_offset = frame.rows.size + 1; length = 0; expected_length_calls = 2; break;
        case 10: state_offset = frame.marks.size + 1; length = 0; expected_length_calls = 2; break;
        case 11: length = INT32_MAX; expected_length_calls = 2; break;
        case 12: row_offset = INT32_MAX; length = 1; expected_length_calls = 2; break;
        case 13: state_offset = INT32_MAX; length = 1; expected_length_calls = 2; break;
        default: need(0, "known invalid case"); break;
    }
    jobject result = invoke(env, rows, states, row_offset, state_offset, length, required, excluded);
    need(result == NULL && current.pending, "invalid native input refused");
    need(strcmp(current.exception_class, "java/lang/IllegalArgumentException") == 0,
            "native boundary retains IllegalArgumentException");
    need(strcmp(current.exception_message, "invalid indexed window or unsigned-byte mask") == 0,
            "native boundary retains exact invalid-window diagnostic");
    need(current.length_calls == expected_length_calls, "native validation order precedes input transfers");
    need(current.allocations == 0 && current.count_calls == 0 && current.emit_calls == 0
            && current.payload_calls == 0 && current.writes == 0, "invalid inputs have no allocation or transfers");
    need(current.find_calls == 1 && current.throw_calls == 1 && current.delete_calls == 1,
            "invalid inputs throw once and release the class reference");
    inputs_unchanged(1);
    mix((uint64_t)variant);
    finish_case(0);
}

static void allocation_failure(JNIEnv *env, lane_kind width) {
    case_spec spec = {"allocation-failure", 600, 3, 5, 1, 128, DRIFT_THREE, 0};
    prepare(&spec, width, NO_CHANGE);
    current.fail_allocation = 1;
    jobject result = call_current(env);
    need(result == NULL && current.pending, "preserve pending result-allocation failure");
    need(strcmp(current.exception_class, "java/lang/OutOfMemoryError") == 0
            && strcmp(current.exception_message, "fixture result allocation failure") == 0,
            "do not replace the model's allocation exception");
    need(current.allocations == 1 && current.output.data == NULL, "one failed result-allocation attempt");
    need(current.throw_calls == 0, "owner does not synthesize a different exception after allocation failure");
    state_schedule(0);
    payload_schedule(frame.saved_states, 0, 1);
    inputs_unchanged(1);
    mix(UINT64_C(0xa110c));
    finish_case(0);
}

int main(int argc, char **argv) {
    if (argc != 2 || (strcmp(argv[1], "--baseline") != 0 && strcmp(argv[1], "--skip-prefix") != 0)) {
        fputs("usage: region-model --baseline | --skip-prefix\n", stderr);
        return 2;
    }
    mode = strcmp(argv[1], "--baseline") == 0 ? BASELINE : SKIP_PREFIX;
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = array_length;
    api.ExceptionCheck = exception_check;
    api.FindClass = find_class;
    api.ThrowNew = throw_new;
    api.DeleteLocalRef = delete_local_ref;
    api.NewIntArray = new_int_array;
    api.NewLongArray = new_long_array;
    api.GetByteArrayRegion = get_bytes;
    api.GetIntArrayRegion = get_ints;
    api.GetLongArrayRegion = get_longs;
    api.SetIntArrayRegion = put_ints;
    api.SetLongArrayRegion = put_longs;
    JNIEnv environment = &api;
    JNIEnv *env = &environment;
    need(Java_com_m3_collections_M3IndexMaskCompactor_windowMasksAbiNative(env, NULL) == 1,
            "unchanged required/excluded window capability value");
    const jint lengths[] = {0, 1, 2, 127, 128, 129, 255, 256, 257, 511, 512, 513, 1023, 1024, 1025, 4097};
    const jint offsets[][2] = {{0, 0}, {3, 5}, {7, 1}, {1, 9}};
    const jint masks[][2] = {{0, 0}, {0, 128}, {1, 128}, {128, 1}, {127, 128},
            {255, 0}, {1, 1}, {255, 255}, {0, 255}};
    for (int width_index = 0; width_index < 2; width_index++) {
        lane_kind width = width_index == 0 ? INT_LANE : LONG_LANE;
        for (size_t n = 0; n < sizeof(lengths) / sizeof(lengths[0]); n++) {
            for (size_t offset = 0; offset < sizeof(offsets) / sizeof(offsets[0]); offset++) {
                for (int pattern = REJECT_ALL; pattern <= EMPTY_MIDDLE; pattern++) {
                    case_spec spec = {"boundary", lengths[n], offsets[offset][0], offsets[offset][1],
                            1, 128, (pattern_kind)pattern, 0};
                    stable_case(env, &spec, width, 0);
                }
            }
        }
        for (size_t mask = 0; mask < sizeof(masks) / sizeof(masks[0]); mask++) {
            case_spec spec = {"unsigned-mask-vector", 1025, 3, 5, masks[mask][0], masks[mask][1], UNSIGNED_WORDS, 0};
            stable_case(env, &spec, width, 0);
        }
        uint32_t random = UINT32_C(0x4d335750);
        for (int trial = 0; trial < 512; trial++) {
            case_spec spec;
            spec.name = "seeded-window";
            spec.length = (jint)(next_word(&random) % 2050U);
            spec.row_offset = (jint)(next_word(&random) % 13U);
            spec.state_offset = (jint)(next_word(&random) % 13U);
            spec.required = (jint)(next_word(&random) >> 24);
            spec.excluded = (jint)(next_word(&random) >> 24);
            spec.pattern = SEEDED_WORDS;
            spec.seed = next_word(&random);
            stable_case(env, &spec, width, 0);
        }
        const case_spec reports[] = {
            {"all-rejected", 1024, 3, 5, 1, 128, REJECT_ALL, 0},
            {"one-last-row", 1024, 3, 5, 1, 128, LAST_ONLY, 0},
            {"one-final-partial-page", 1025, 3, 5, 1, 128, LAST_ONLY, 0},
            {"rejected-middle-page", 1024, 3, 5, 1, 128, EMPTY_MIDDLE, 0},
            {"prefix-with-rejected-tail", 1024, 3, 5, 128, 1, UNSIGNED_WORDS, 0},
            {"dense", 1024, 3, 5, 1, 128, ACCEPT_ALL, 0},
            {"zero-mask-direct-export", 1024, 3, 5, 0, 0, REJECT_ALL, 0},
            {"contradictory", 1024, 3, 5, 1, 1, ACCEPT_ALL, 0}
        };
        for (size_t report = 0; report < sizeof(reports) / sizeof(reports[0]); report++) {
            stable_case(env, &reports[report], width, 1);
        }
        for (int change = COUNT_INCREASE; change <= ZERO_COUNT_GROWTH; change++) {
            drift_case(env, width, (injected_change)change);
        }
        for (int invalid = 0; invalid < 14; invalid++) invalid_case(env, width, invalid);
        allocation_failure(env, width);
    }
    printf("PASS JNI_TABLE_MODEL mode=%s cases=%" PRIu64 " checks=%" PRIu64 " digest=%016" PRIx64
            " count_bytes=%" PRIu64 " emit_bytes=%" PRIu64 " payload_calls=%" PRIu64 " payload_bytes=%" PRIu64 "\n",
            mode == BASELINE ? "baseline" : "skip-prefix", cases, checks, digest,
            total_count_bytes, total_emit_bytes, total_payload_calls, total_payload_bytes);
    puts("REAL_JVM_EXECUTION=NOT_PERFORMED_BY_THIS_FIXTURE");
    return 0;
}
