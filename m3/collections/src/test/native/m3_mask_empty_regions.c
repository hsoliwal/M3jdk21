// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * Instrumented JNI FUNCTION TABLE, not a JVM. Link this same fixture separately
 * with the authoritative baseline and the empty-selection candidate, then run
 * the binaries with "baseline" and "candidate", respectively. Real JVM JNI
 * validation, including -Xcheck:jni, remains a separate gate.
 *
 * Baseline owner: m3/collections/src/main/native/collections/m3_index_mask_compactor.c
 * SHA-256: d1c300832aa9bf44cd7b7199cff593beb2a9464fbb52c9c8448da7773eaf0aef
 * First-party table pattern: src/test/native/m3_mask_window_regions.c
 * SHA-256: ac09456844268b592475d9da1e6eee6f154e741c893167cf21d675010580deab
 *
 * At 513 rejected states, all four row selectors must make six state-region
 * reads. The baseline makes three row-region reads; the candidate makes zero.
 * NewArray is a deterministic injection point between counting and emission:
 * zero -> one and one -> zero selected states must still produce the incumbent
 * ConcurrentModificationException. Every mock region checks lane, bounds and
 * sequence before copying; writes must fit the EXACT allocated result length.
 * Matching calls also check exception propagation, independent window offsets,
 * fresh empty arrays, unsigned masks, stable int/long payloads, and all nine
 * current native exports (including required/excluded logical positions).
 *
 * No timing or throughput conclusion follows from these region-call counts.
 */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CHUNK 256
#define LIMIT 1024
#define NATIVE(name) Java_com_m3_collections_M3IndexMaskCompactor_##name

extern jintArray JNICALL NATIVE(selectNative)(JNIEnv *, jclass, jintArray, jbyteArray, jint);
extern jint JNICALL NATIVE(rangeAbiNative)(JNIEnv *, jclass);
extern jintArray JNICALL NATIVE(selectRangeNative)(JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint);
extern jint JNICALL NATIVE(positionsAbiNative)(JNIEnv *, jclass);
extern jintArray JNICALL NATIVE(selectPositionsNative)(JNIEnv *, jclass, jbyteArray, jint);
extern jint JNICALL NATIVE(positionsMasksAbiNative)(JNIEnv *, jclass);
extern jintArray JNICALL NATIVE(selectPositionsMasksNative)(JNIEnv *, jclass, jbyteArray, jint, jint);
extern jintArray JNICALL NATIVE(selectWindowNative)(JNIEnv *, jclass, jintArray, jint, jbyteArray, jint, jint, jint);
extern jlongArray JNICALL NATIVE(selectLongWindowNative)(JNIEnv *, jclass, jlongArray, jint, jbyteArray, jint, jint, jint);

enum lane { BYTE_LANE, INT_LANE, LONG_LANE };
enum selector { WHOLE, RANGE, INT_WINDOW, LONG_WINDOW };
typedef struct {
    enum lane lane;
    jsize size, written;
    void *data;
    int owned;
} fixture_array;
typedef struct {
    int state_reads, int_reads, long_reads, writes, allocations;
    int finds, throws, deletes;
    jsize allocated_length;
    int fail_allocation, fail_state_read;
    jbyte *mutation;
    jbyte mutation_value;
    jboolean pending;
    const char *exception_class, *exception_message;
} observation;

static const char *const selector_names[] = {"whole", "range", "int-window", "long-window"};
static const char *const drift_class = "java/util/ConcurrentModificationException";
static const char *const drift_message = "aligned selection states changed";
static observation observed;
static fixture_array *results[8];
static size_t result_count;
static fixture_array rows, states;
static jint int_values[LIMIT];
static jlong long_values[LIMIT];
static jbyte state_values[LIMIT];
static jsize row_offset, state_offset, span;
static const char *found_class;
static char label[96];
static unsigned long checks, cases;
static int candidate;

static void check(int condition, const char *expression, int line) {
    checks++;
    if (!condition) {
        fprintf(stderr, "FAIL %s line=%d: %s\n", label, line, expression);
        exit(EXIT_FAILURE);
    }
}
#define REQUIRE(expression) check((expression), #expression, __LINE__)

static int chunks(jsize length) { return length == 0 ? 0 : 1 + (length - 1) / CHUNK; }
static int row_reads(void) { return observed.int_reads + observed.long_reads; }

static void release_results(void) {
    for (size_t i = 0; i < result_count; i++) {
        free(results[i]->data);
        free(results[i]);
    }
    result_count = 0;
}

static void setup(const char *name, enum selector which, jsize length) {
    release_results();
    memset(&observed, 0, sizeof(observed));
    found_class = NULL;
    (void)snprintf(label, sizeof(label), "%s/%s/%d", name, selector_names[which], (int)length);
    cases++;
    REQUIRE(length >= 0 && length <= LIMIT - 16);
    row_offset = which == WHOLE ? 0 : 7;
    state_offset = which == INT_WINDOW || which == LONG_WINDOW ? 11 : 0;
    span = length;
    rows = (fixture_array){which == LONG_WINDOW ? LONG_LANE : INT_LANE,
        which == WHOLE ? length : row_offset + length + 5, 0,
        which == LONG_WINDOW ? (void *)long_values : (void *)int_values, 0};
    states = (fixture_array){BYTE_LANE,
        state_offset == 0 ? length : state_offset + length + 5, 0, state_values, 0};
    for (int i = 0; i < LIMIT; i++) {
        int_values[i] = i % 23 - 11; /* Preserve duplicates and negative row IDs. */
        long_values[i] = (jlong)(i - 512) * (jlong)4294967297LL;
        state_values[i] = (jbyte)128; /* Matching guards outside a rejected window. */
    }
    for (jsize i = 0; i < length; i++) state_values[state_offset + i] = 0;
}

static fixture_array *array(jarray handle, enum lane lane) {
    REQUIRE(observed.pending == JNI_FALSE);
    REQUIRE(handle != NULL);
    fixture_array *a = (fixture_array *)handle;
    REQUIRE(a->lane == lane);
    return a;
}

static void bounds(fixture_array *a, jsize start, jsize length) {
    REQUIRE(start >= 0 && length >= 0 && start <= a->size);
    REQUIRE(length <= a->size - start);
}

/* Assert every region covers the next chunk of the intended window, including
 * the second state scan and calls repeated without releasing the first result. */
static void read_sequence(jsize start, jsize length, jsize first, int calls) {
    int count = chunks(span);
    REQUIRE(count > 0);
    jsize position = ((calls - 1) % count) * CHUNK;
    jsize expected = span - position < CHUNK ? span - position : CHUNK;
    REQUIRE(start == first + position && length == expected);
}

static jsize JNICALL get_length(JNIEnv *env, jarray handle) {
    (void)env;
    REQUIRE(observed.pending == JNI_FALSE && handle != NULL);
    return ((fixture_array *)handle)->size;
}
static jboolean JNICALL has_exception(JNIEnv *env) { (void)env; return observed.pending; }

static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;
    REQUIRE(observed.pending == JNI_FALSE && name != NULL);
    observed.finds++;
    found_class = name;
    return (jclass)&found_class;
}
static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    REQUIRE(observed.pending == JNI_FALSE && type == (jclass)&found_class);
    REQUIRE(found_class != NULL && message != NULL);
    observed.throws++;
    observed.pending = JNI_TRUE;
    observed.exception_class = found_class;
    observed.exception_message = message;
    return JNI_OK;
}
static void JNICALL delete_ref(JNIEnv *env, jobject reference) {
    (void)env;
    REQUIRE(reference == (jobject)&found_class);
    observed.deletes++;
}

static fixture_array *new_array(enum lane lane, jsize length) {
    REQUIRE(observed.pending == JNI_FALSE && length >= 0 && length <= LIMIT);
    observed.allocations++;
    observed.allocated_length = length;
    if (observed.fail_allocation) {
        observed.pending = JNI_TRUE;
        observed.exception_class = "java/lang/OutOfMemoryError";
        observed.exception_message = "fixture allocation failure";
        return NULL;
    }
    REQUIRE(result_count < sizeof(results) / sizeof(results[0]));
    fixture_array *a = calloc(1, sizeof(*a));
    REQUIRE(a != NULL);
    a->lane = lane;
    a->size = length;
    a->owned = 1;
    if (length != 0) {
        a->data = calloc((size_t)length, lane == LONG_LANE ? sizeof(jlong) : sizeof(jint));
        REQUIRE(a->data != NULL);
    }
    results[result_count++] = a;
    if (observed.mutation != NULL) {
        *observed.mutation = observed.mutation_value;
        observed.mutation = NULL;
    }
    return a;
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize length) {
    (void)env; return (jintArray)new_array(INT_LANE, length);
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize length) {
    (void)env; return (jlongArray)new_array(LONG_LANE, length);
}

static void JNICALL get_bytes(JNIEnv *env, jbyteArray handle, jsize start, jsize length, jbyte *out) {
    (void)env;
    fixture_array *a = array((jarray)handle, BYTE_LANE);
    REQUIRE(a == &states && out != NULL);
    bounds(a, start, length);
    read_sequence(start, length, state_offset, ++observed.state_reads);
    if (observed.fail_state_read == observed.state_reads) {
        observed.pending = JNI_TRUE;
        observed.exception_class = "java/lang/ArrayIndexOutOfBoundsException";
        observed.exception_message = "fixture state-region failure";
        return;
    }
    memcpy(out, (jbyte *)a->data + start, (size_t)length * sizeof(jbyte));
}
static void JNICALL get_ints(JNIEnv *env, jintArray handle, jsize start, jsize length, jint *out) {
    (void)env;
    fixture_array *a = array((jarray)handle, INT_LANE);
    REQUIRE(a == &rows && out != NULL);
    bounds(a, start, length);
    read_sequence(start, length, row_offset, ++observed.int_reads);
    memcpy(out, (jint *)a->data + start, (size_t)length * sizeof(jint));
}
static void JNICALL get_longs(JNIEnv *env, jlongArray handle, jsize start, jsize length, jlong *out) {
    (void)env;
    fixture_array *a = array((jarray)handle, LONG_LANE);
    REQUIRE(a == &rows && out != NULL);
    bounds(a, start, length);
    read_sequence(start, length, row_offset, ++observed.long_reads);
    memcpy(out, (jlong *)a->data + start, (size_t)length * sizeof(jlong));
}
static void put_region(jarray handle, enum lane lane, jsize start, jsize length, const void *in) {
    fixture_array *a = array(handle, lane);
    REQUIRE(a->owned && in != NULL && length > 0 && start == a->written);
    bounds(a, start, length);
    size_t width = lane == LONG_LANE ? sizeof(jlong) : sizeof(jint);
    memcpy((unsigned char *)a->data + (size_t)start * width, in, (size_t)length * width);
    a->written += length;
    observed.writes++;
}
static void JNICALL put_ints(JNIEnv *env, jintArray handle, jsize start, jsize length, const jint *in) {
    (void)env; put_region((jarray)handle, INT_LANE, start, length, in);
}
static void JNICALL put_longs(JNIEnv *env, jlongArray handle, jsize start, jsize length, const jlong *in) {
    (void)env; put_region((jarray)handle, LONG_LANE, start, length, in);
}

static jarray select_rows(JNIEnv *env, enum selector which, jint mask) {
    switch (which) {
        case WHOLE: return (jarray)NATIVE(selectNative)(env, NULL, (jintArray)&rows, (jbyteArray)&states, mask);
        case RANGE: return (jarray)NATIVE(selectRangeNative)(env, NULL, (jintArray)&rows, (jbyteArray)&states, row_offset, span, mask);
        case INT_WINDOW: return (jarray)NATIVE(selectWindowNative)(env, NULL, (jintArray)&rows, row_offset, (jbyteArray)&states, state_offset, span, mask);
        case LONG_WINDOW: return (jarray)NATIVE(selectLongWindowNative)(env, NULL, (jlongArray)&rows, row_offset, (jbyteArray)&states, state_offset, span, mask);
    }
    REQUIRE(0);
    return NULL;
}

static fixture_array *result(jarray handle, enum lane lane, jsize length) {
    fixture_array *a = array(handle, lane);
    REQUIRE(a->owned && a != &rows && a != &states);
    REQUIRE(a->size == length && a->written == length);
    if (length == 0) REQUIRE(a->data == NULL);
    return a;
}
static void exception_is(jarray returned, const char *type, const char *message, int throws) {
    REQUIRE(returned == NULL && observed.pending == JNI_TRUE);
    REQUIRE(observed.exception_class != NULL && observed.exception_message != NULL);
    REQUIRE(strcmp(observed.exception_class, type) == 0);
    REQUIRE(strcmp(observed.exception_message, message) == 0);
    REQUIRE(observed.finds == throws && observed.throws == throws && observed.deletes == throws);
}
static void empty_work(int count, int calls) {
    REQUIRE(observed.state_reads == 2 * count * calls);
    REQUIRE(row_reads() == (candidate ? 0 : count * calls));
    REQUIRE(observed.writes == 0 && observed.allocations == calls);
    REQUIRE(observed.allocated_length == 0);
}
static void report(enum selector which, const char *name) {
    printf("OBS mode=%s api=%s case=%s length=%d state_reads=%d row_reads=%d writes=%d allocated=%d exception=%s\n",
        candidate ? "candidate" : "baseline", selector_names[which], name, (int)span,
        observed.state_reads, row_reads(), observed.writes, (int)observed.allocated_length,
        observed.pending ? observed.exception_class : "none");
}

static void empty_cases(JNIEnv *env, enum selector which) {
    const jsize lengths[] = {0, 1, 255, 256, 257, 513};
    for (size_t i = 0; i < sizeof(lengths) / sizeof(lengths[0]); i++) {
        setup("empty", which, lengths[i]);
        jarray first = select_rows(env, which, 128);
        (void)result(first, rows.lane, 0);
        empty_work(chunks(span), 1);
        if (span == 513) report(which, "empty");
        jarray second = select_rows(env, which, 128);
        (void)result(second, rows.lane, 0);
        REQUIRE(first != second); /* The first allocation remains live here. */
        empty_work(chunks(span), 2);
    }
    if (which != WHOLE) {
        setup("empty-at-end", which, 0);
        row_offset = rows.size;
        if (which != RANGE) state_offset = states.size;
        (void)result(select_rows(env, which, 128), rows.lane, 0);
        empty_work(0, 1);
    }
}

static void drift_cases(JNIEnv *env, enum selector which) {
    setup("zero-to-one", which, 513);
    observed.mutation = &state_values[state_offset + span - 1];
    observed.mutation_value = (jbyte)128;
    exception_is(select_rows(env, which, 128), drift_class, drift_message, 1);
    REQUIRE(observed.mutation == NULL);
    empty_work(chunks(span), 1);
    report(which, "zero-to-one-drift");

    setup("one-to-zero", which, 513);
    state_values[state_offset + span - 1] = (jbyte)128;
    observed.mutation = &state_values[state_offset + span - 1];
    observed.mutation_value = 0;
    exception_is(select_rows(env, which, 128), drift_class, drift_message, 1);
    REQUIRE(observed.mutation == NULL);
    REQUIRE(observed.allocations == 1 && observed.allocated_length == 1);
    REQUIRE(observed.state_reads == 2 * chunks(span) && row_reads() == chunks(span));
    REQUIRE(observed.writes == 0);
    report(which, "one-to-zero-drift");
}

static void failure_cases(JNIEnv *env, enum selector which) {
    setup("allocation-failure", which, 513);
    observed.fail_allocation = 1;
    exception_is(select_rows(env, which, 128), "java/lang/OutOfMemoryError", "fixture allocation failure", 0);
    REQUIRE(observed.state_reads == chunks(span) && row_reads() == 0 && observed.writes == 0);
    REQUIRE(observed.allocations == 1 && observed.allocated_length == 0 && result_count == 0);

    const int failed_reads[] = {1, 4}; /* First count read; first verification read. */
    for (size_t i = 0; i < sizeof(failed_reads) / sizeof(failed_reads[0]); i++) {
        setup("state-read-failure", which, 513);
        observed.fail_state_read = failed_reads[i];
        exception_is(select_rows(env, which, 128), "java/lang/ArrayIndexOutOfBoundsException", "fixture state-region failure", 0);
        REQUIRE(observed.state_reads == failed_reads[i] && row_reads() == 0 && observed.writes == 0);
        REQUIRE(observed.allocations == (failed_reads[i] == 1 ? 0 : 1));
    }
}

static void copy_cases(JNIEnv *env, enum selector which) {
    setup("zero-mask", which, 513);
    fixture_array *all = result(select_rows(env, which, 0), rows.lane, span);
    REQUIRE(observed.state_reads == 0 && row_reads() == chunks(span));
    REQUIRE(observed.writes == chunks(span));
    for (jsize i = 0; i < span; i++) {
        if (which == LONG_WINDOW) REQUIRE(((jlong *)all->data)[i] == long_values[row_offset + i]);
        else REQUIRE(((jint *)all->data)[i] == int_values[row_offset + i]);
    }
    /* A detached output must keep the copied value after caller storage changes. */
    if (which == LONG_WINDOW) {
        jlong copy = ((jlong *)all->data)[0];
        long_values[row_offset]++;
        REQUIRE(((jlong *)all->data)[0] == copy);
    } else {
        jint copy = ((jint *)all->data)[0];
        int_values[row_offset]++;
        REQUIRE(((jint *)all->data)[0] == copy);
    }

    setup("nonempty-prefix-control", which, 513);
    const jsize selected[] = {0, 255, 256, 512};
    const jbyte flags[] = {(jbyte)128, (jbyte)129, (jbyte)255, (jbyte)128};
    for (size_t i = 0; i < 4; i++) state_values[state_offset + selected[i]] = flags[i];
    fixture_array *some = result(select_rows(env, which, 128), rows.lane, 4);
    REQUIRE(observed.state_reads == 2 * chunks(span) && row_reads() == chunks(span));
    REQUIRE(observed.writes == 3);
    for (size_t i = 0; i < 4; i++) {
        if (which == LONG_WINDOW) REQUIRE(((jlong *)some->data)[i] == long_values[row_offset + selected[i]]);
        else REQUIRE(((jint *)some->data)[i] == int_values[row_offset + selected[i]]);
    }
}

static void invalid_cases(JNIEnv *env, enum selector which) {
    const jint masks[] = {-1, 256};
    for (size_t i = 0; i < sizeof(masks) / sizeof(masks[0]); i++) {
        setup("invalid-mask", which, 513);
        exception_is(select_rows(env, which, masks[i]), "java/lang/IllegalArgumentException",
            which == WHOLE || which == RANGE ? "invalid primitive selection inputs"
                : "invalid indexed window or unsigned-byte mask", 1);
        REQUIRE(observed.state_reads == 0 && row_reads() == 0 && observed.allocations == 0);
    }
    setup("invalid-bounds", which, 513);
    if (which == WHOLE || which == RANGE) states.size--;
    else row_offset = rows.size; /* Positive length cannot start at the end. */
    exception_is(select_rows(env, which, 128), "java/lang/IllegalArgumentException",
        which == WHOLE ? "state/index length mismatch"
            : which == RANGE ? "invalid index window or aligned state length"
                : "invalid indexed window or unsigned-byte mask", 1);
    REQUIRE(observed.state_reads == 0 && row_reads() == 0 && observed.allocations == 0);
    if (which == INT_WINDOW || which == LONG_WINDOW) {
        setup("invalid-state-offset", which, 513);
        state_offset = states.size;
        exception_is(select_rows(env, which, 128), "java/lang/IllegalArgumentException",
            "invalid indexed window or unsigned-byte mask", 1);
        REQUIRE(observed.state_reads == 0 && row_reads() == 0 && observed.allocations == 0);
    }
}

static void position_cases(JNIEnv *env) {
    setup("abi-probes", WHOLE, 0);
    REQUIRE(NATIVE(rangeAbiNative)(env, NULL) == 1);
    REQUIRE(NATIVE(positionsAbiNative)(env, NULL) == 1);
    REQUIRE(NATIVE(positionsMasksAbiNative)(env, NULL) == 1);
    const jbyte flags[] = {0, (jbyte)128, (jbyte)129, 1, 64};

    setup("required-positions", WHOLE, 5);
    memcpy(state_values, flags, sizeof(flags));
    fixture_array *required = result((jarray)NATIVE(selectPositionsNative)(env, NULL, (jbyteArray)&states, 128), INT_LANE, 2);
    REQUIRE(((jint *)required->data)[0] == 1 && ((jint *)required->data)[1] == 2);
    REQUIRE(observed.state_reads == 2 && row_reads() == 0);

    setup("required-excluded-positions", WHOLE, 5);
    memcpy(state_values, flags, sizeof(flags));
    fixture_array *excluded = result((jarray)NATIVE(selectPositionsMasksNative)(env, NULL, (jbyteArray)&states, 128, 1), INT_LANE, 1);
    REQUIRE(((jint *)excluded->data)[0] == 1);
    REQUIRE(observed.state_reads == 2 && row_reads() == 0);

    setup("contradictory-positions", WHOLE, 5);
    jarray first = (jarray)NATIVE(selectPositionsMasksNative)(env, NULL, (jbyteArray)&states, 128, 128);
    jarray second = (jarray)NATIVE(selectPositionsMasksNative)(env, NULL, (jbyteArray)&states, 128, 128);
    (void)result(first, INT_LANE, 0);
    (void)result(second, INT_LANE, 0);
    REQUIRE(first != second && observed.allocations == 2);
    REQUIRE(observed.state_reads == 0 && row_reads() == 0 && observed.writes == 0);

    setup("unrestricted-positions", WHOLE, 5);
    fixture_array *all = result((jarray)NATIVE(selectPositionsMasksNative)(env, NULL, (jbyteArray)&states, 0, 0), INT_LANE, 5);
    for (jint i = 0; i < 5; i++) REQUIRE(((jint *)all->data)[i] == i);
    REQUIRE(observed.state_reads == 0 && row_reads() == 0);

    setup("invalid-excluded-mask", WHOLE, 5);
    exception_is((jarray)NATIVE(selectPositionsMasksNative)(env, NULL, (jbyteArray)&states, 0, 256),
        "java/lang/IllegalArgumentException", "invalid indexed window or unsigned-byte mask", 1);
    REQUIRE(observed.state_reads == 0 && row_reads() == 0 && observed.allocations == 0);
}

int main(int argc, char **argv) {
    if (argc != 2 || (strcmp(argv[1], "baseline") != 0 && strcmp(argv[1], "candidate") != 0)) {
        fputs("usage: m3_mask_empty_regions baseline|candidate\n", stderr);
        return 2;
    }
    candidate = strcmp(argv[1], "candidate") == 0;
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = get_length;
    api.ExceptionCheck = has_exception;
    api.FindClass = find_class;
    api.ThrowNew = throw_new;
    api.DeleteLocalRef = delete_ref;
    api.NewIntArray = new_ints;
    api.NewLongArray = new_longs;
    api.GetByteArrayRegion = get_bytes;
    api.GetIntArrayRegion = get_ints;
    api.GetLongArrayRegion = get_longs;
    api.SetIntArrayRegion = put_ints;
    api.SetLongArrayRegion = put_longs;
    JNIEnv env = &api;
    for (int i = WHOLE; i <= LONG_WINDOW; i++) {
        enum selector which = (enum selector)i;
        empty_cases(&env, which);
        drift_cases(&env, which);
        failure_cases(&env, which);
        copy_cases(&env, which);
        invalid_cases(&env, which);
    }
    position_cases(&env);
    release_results();
    printf("PASS JNI function-table mode=%s exports=9 cases=%lu assertions=%lu (not a JVM execution)\n",
        candidate ? "candidate" : "baseline", cases, checks);
    return 0;
}
