/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Adapted from the existing m3_mask_window_regions.c / m3_mask_window_spans.c
 * fixture pattern. Counts finite JNI-table work; real-JVM parity is separate. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CAPACITY 4096
#define STAGES 9

typedef struct { jsize size; void *data; } fixture_array;
enum failure_kind { NONE, METADATA, OFFSET, STATE, ROW, WRITE, ALLOCATION,
                    ALLOCATION_NULL, OBJECT };

static jint row_values[CAPACITY], offset_values[CAPACITY], result_values[CAPACITY];
static jint state_values[STAGES][CAPACITY], need_values[STAGES], deny_values[STAGES];
static jobject plane_values[STAGES];
static fixture_array rows, offsets, planes, needs, denies, result, state[STAGES];
static int pending_exception, local_refs, peak_refs, fault, fault_at, drift;
static int row_reads, offset_reads, state_reads, metadata_reads, object_reads;
static int writes, written_elements, allocations, max_write, cases;
static jint active_first, active_count, active_stride, active_stages;
static uint64_t checks, trace, combined_trace = UINT64_C(1469598103934665603);
static char exception_class[128], exception_message[128], class_handle;

static void require(int ok, const char *detail) {
    checks++;
    if (!ok) {
        fprintf(stderr, "path-mask region assertion failed: %s\n", detail);
        exit(1);
    }
}

static void ready(void) { require(!pending_exception, "JNI work with pending exception"); }

static void bounds(jarray array, jsize start, jsize count) {
    require(array != NULL, "null array reached Region API");
    fixture_array *a = (fixture_array *)array;
    require(start >= 0 && count >= 0 && start <= a->size
            && count <= a->size - start, "Region outside array");
}

static int fail(int kind, int ordinal) {
    if (fault == kind && fault_at == ordinal) {
        pending_exception = 1;
        return 1;
    }
    return 0;
}

static void mix(uint64_t value) {
    trace ^= value;
    trace *= UINT64_C(1099511628211);
}

static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void)env;
    ready();
    require(array != NULL, "null array length");
    return ((fixture_array *)array)->size;
}

static jboolean JNICALL exception(JNIEnv *env) {
    (void)env;
    return pending_exception ? JNI_TRUE : JNI_FALSE;
}

static jobject JNICALL object(JNIEnv *env, jobjectArray array, jsize index) {
    (void)env;
    ready();
    bounds((jarray)array, index, 1);
    object_reads++;
    if (fail(OBJECT, object_reads)) return NULL;
    jobject value = ((jobject *)((fixture_array *)array)->data)[index];
    if (value != NULL) {
        local_refs++;
        if (local_refs > peak_refs) peak_refs = local_refs;
    }
    return value;
}

static void JNICALL release(JNIEnv *env, jobject reference) {
    (void)env;
    /* DeleteLocalRef is permitted during exceptional cleanup. */
    require(reference != NULL && local_refs > 0, "unbalanced local reference");
    local_refs--;
}

static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;
    ready();
    require(strlen(name) < sizeof exception_class, "exception class capacity");
    strcpy(exception_class, name);
    local_refs++;
    return (jclass)&class_handle;
}

static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    ready();
    require(type == (jclass)&class_handle, "exception class identity");
    require(strlen(message) < sizeof exception_message, "exception message capacity");
    strcpy(exception_message, message);
    pending_exception = 1;
    return 0;
}

static void JNICALL get_ints(JNIEnv *env, jintArray array, jsize start,
                              jsize count, jint *output) {
    (void)env;
    ready();
    bounds((jarray)array, start, count);
    require(count == 1, "source reads must retain original scalar Region demand");
    int kind;
    int ordinal;
    int identity;
    if (array == (jintArray)&rows) {
        kind = ROW; ordinal = ++row_reads; identity = 1;
    } else if (array == (jintArray)&offsets) {
        kind = OFFSET; ordinal = ++offset_reads; identity = 2;
    } else if (array == (jintArray)&needs || array == (jintArray)&denies) {
        kind = METADATA; ordinal = ++metadata_reads;
        identity = array == (jintArray)&needs ? 3 : 4;
    } else {
        kind = STATE; ordinal = ++state_reads; identity = -1;
        for (int i = 0; i < STAGES; i++) {
            if (array == (jintArray)&state[i]) identity = 5 + i;
        }
        require(identity >= 5, "unregistered state plane");
    }
    if (fail(kind, ordinal)) return;
    const jint *input = (jint *)((fixture_array *)array)->data + start;
    mix((uint64_t)(unsigned int)identity);
    mix((uint64_t)(uint32_t)start);
    mix((uint64_t)(uint32_t)count);
    mix((uint64_t)(uint32_t)input[0]);
    memcpy(output, input, (size_t)count * sizeof(jint));
}

static jintArray JNICALL new_ints(JNIEnv *env, jsize count) {
    (void)env;
    ready();
    require(count >= 0 && count <= CAPACITY, "result allocation bound");
    allocations++;
    if (fail(ALLOCATION, allocations)) return NULL;
    if (fault == ALLOCATION_NULL && fault_at == allocations) return NULL;
    result.size = count;
    result.data = result_values;
    memset(result_values, 0x39, sizeof result_values);
    if (drift != 0) {
        state_values[0][offset_values[active_first]] = drift > 0 ? 1 : 0;
    }
    return (jintArray)&result;
}

static void JNICALL put_ints(JNIEnv *env, jintArray array, jsize start,
                              jsize count, const jint *input) {
    (void)env;
    ready();
    require(array == (jintArray)&result, "source array mutation");
    bounds((jarray)array, start, count);
    require(count > 0 && count <= 128, "bounded nonempty output Region");
    require(start == written_elements, "output Regions must be adjacent and ordered");
    writes++;
    if (fail(WRITE, writes)) return;
    if (count > max_write) max_write = count;
    memcpy(result_values + start, input, (size_t)count * sizeof(jint));
    written_elements += count;
}

extern jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *, jclass, jintArray, jintArray, jobjectArray,
        jintArray, jintArray, jint, jint, jint);

static void setup(jint count, jint stage_count, jint stride, int mode) {
    require(count >= 0 && count <= 1025 && stage_count >= 1
            && stage_count <= STAGES, "fixture geometry");
    int step = stride < 0 ? -stride : stride;
    jint size = count == 0 ? 4 : (count - 1) * step + 5;
    jint capacity = count + 7;
    if (capacity < 4) capacity = 4;
    active_first = count == 0 ? size : stride > 0 ? 2 : size - 3;
    active_count = count;
    active_stride = stride;
    active_stages = stage_count;
    rows = (fixture_array){size, row_values};
    offsets = (fixture_array){size, offset_values};
    planes = (fixture_array){stage_count, plane_values};
    needs = (fixture_array){stage_count, need_values};
    denies = (fixture_array){stage_count, deny_values};
    for (int i = 0; i < size; i++) {
        row_values[i] = i % 4 == 0 ? INT32_MIN : i % 4 == 1
                ? INT32_MAX : (jint)(i % 13 - 6);
        offset_values[i] = (i * 17 + 3) % capacity;
    }
    for (int s = 0; s < stage_count; s++) {
        state[s] = (fixture_array){capacity + s % 3, state_values[s]};
        plane_values[s] = (jobject)&state[s];
        need_values[s] = deny_values[s] = 0;
        for (int i = 0; i < state[s].size; i++) state_values[s][i] = (jint)(i * 73);
    }
    if (mode == 1) {
        need_values[0] = deny_values[0] = INT32_MIN;
    } else if (mode == 2) {
        need_values[stage_count - 1] = INT32_MIN;
        deny_values[stage_count - 1] = 64;
        for (int i = 0; i < state[stage_count - 1].size; i++) {
            state_values[stage_count - 1][i] = i % 2 == 0 ? INT32_MIN : -1;
        }
    } else if (mode == 3) {
        need_values[stage_count - 1] = -1;
        for (int i = 0; i < state[stage_count - 1].size; i++) {
            state_values[stage_count - 1][i] = i % 127 == 0 || i % 128 == 1 ? -1 : 0;
        }
    }
    pending_exception = local_refs = peak_refs = fault = fault_at = drift = 0;
    row_reads = offset_reads = state_reads = metadata_reads = object_reads = 0;
    writes = written_elements = allocations = max_write = 0;
    exception_class[0] = exception_message[0] = '\0';
    trace = UINT64_C(1469598103934665603);
}

static jintArray run(JNIEnv *env) {
    return Java_com_m3_collections_M3IndexPathMasks_selectNative(env, NULL,
            (jintArray)&rows, (jintArray)&offsets, (jobjectArray)&planes,
            (jintArray)&needs, (jintArray)&denies,
            active_first, active_count, active_stride);
}

static void normal(JNIEnv *env, int baseline) {
    jint expected[1025];
    jint selected = 0;
    int visited_stages = 0;
    jlong at = active_first;
    for (jint i = 0; i < active_count; i++, at += active_stride) {
        jint slot = offset_values[(jsize)at];
        int accepts = 1;
        for (int s = 0; s < active_stages; s++) {
            visited_stages++;
            uint32_t value = (uint32_t)state_values[s][slot];
            uint32_t need = (uint32_t)need_values[s];
            uint32_t deny = (uint32_t)deny_values[s];
            if ((value & need) != need || (value & deny) != 0) {
                accepts = 0;
                break;
            }
        }
        if (accepts) expected[selected++] = row_values[(jsize)at];
    }
    int cached = active_count >= 8 && active_stages <= 8;
    require(run(env) == (jintArray)&result, "successful result identity");
    require(!pending_exception && local_refs == 0, "normal reference cleanup");
    require(result.size == selected && written_elements == selected, "exact result length");
    require(allocations == 1, "one exact-sized Java array");
    require(row_reads == selected && offset_reads == active_count * 2,
            "no rejected row reads or offset lookahead");
    require(state_reads == visited_stages * 2, "unchanged two-pass stage reads");
    require(metadata_reads == (cached ? active_stages * 2 : visited_stages * 4),
            "unchanged metadata reads");
    require(object_reads == (cached ? active_stages : visited_stages * 2),
            "unchanged local plane acquisition");
    require(peak_refs <= (cached ? active_stages : 1), "bounded local references");
    int expected_writes = baseline || !cached ? selected : (selected + 127) / 128;
    require(writes == expected_writes, "output Region count");
    for (jint i = 0; i < selected; i++) {
        require(result_values[i] == expected[i], "stable signed duplicate row IDs");
    }
    if (cached && !baseline && selected >= 128) require(max_write == 128, "full batch");
    combined_trace ^= trace;
    combined_trace *= UINT64_C(1099511628211);
    cases++;
}

static void injected_failure(JNIEnv *env, int kind, int ordinal) {
    setup(257, 8, 1, 0);
    fault = kind;
    fault_at = ordinal;
    require(run(env) == NULL, "injected failure must not publish a result");
    require(local_refs == 0, "failure reference cleanup");
    require(kind == ALLOCATION_NULL ? !pending_exception : pending_exception,
            "pending exception preservation");
}

static void drift_failure(JNIEnv *env, int direction) {
    setup(257, 8, 1, 0);
    need_values[0] = 1;
    for (int i = 0; i < state[0].size; i++) state_values[0][i] = 1;
    if (direction > 0) state_values[0][offset_values[active_first]] = 0;
    drift = direction;
    require(run(env) == NULL && pending_exception, "count drift refusal");
    require(local_refs == 0, "count drift cleanup");
    require(strcmp(exception_class, "java/util/ConcurrentModificationException") == 0,
            "count drift exception type");
    require(strcmp(exception_message, "mask stages changed during JNI selection") == 0,
            "count drift exception message");
}

int main(int argc, char **argv) {
    if (argc != 2 || (strcmp(argv[1], "--baseline") != 0
            && strcmp(argv[1], "--candidate") != 0)) {
        fputs("use --baseline or --candidate\n", stderr);
        return 2;
    }
    int baseline = strcmp(argv[1], "--baseline") == 0;
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length;
    api.ExceptionCheck = exception;
    api.GetObjectArrayElement = object;
    api.DeleteLocalRef = release;
    api.FindClass = find_class;
    api.ThrowNew = throw_new;
    api.GetIntArrayRegion = get_ints;
    api.NewIntArray = new_ints;
    api.SetIntArrayRegion = put_ints;
    JNIEnv env = &api;
    const jint counts[] = {0, 1, 7, 8, 9, 127, 128, 129, 255, 256, 257, 513, 1025};
    const jint stages[] = {1, 8, 9};
    const jint strides[] = {1, -1, 3, -2};
    for (size_t n = 0; n < sizeof counts / sizeof counts[0]; n++) {
        for (size_t s = 0; s < sizeof stages / sizeof stages[0]; s++) {
            for (size_t step = 0; step < sizeof strides / sizeof strides[0]; step++) {
                for (int mode = 0; mode < 4; mode++) {
                    setup(counts[n], stages[s], strides[step], mode);
                    normal(&env, baseline);
                }
            }
        }
    }
    injected_failure(&env, METADATA, 1);
    injected_failure(&env, METADATA, 16);
    injected_failure(&env, OBJECT, 1);
    injected_failure(&env, OBJECT, 8);
    injected_failure(&env, OFFSET, 1);
    injected_failure(&env, OFFSET, 258);
    injected_failure(&env, STATE, 1);
    injected_failure(&env, STATE, 2057);
    injected_failure(&env, ROW, 1);
    injected_failure(&env, ROW, 129);
    injected_failure(&env, ROW, 257);
    injected_failure(&env, WRITE, 1);
    injected_failure(&env, WRITE, 2);
    injected_failure(&env, WRITE, 3);
    injected_failure(&env, ALLOCATION, 1);
    injected_failure(&env, ALLOCATION_NULL, 1);
    drift_failure(&env, -1);
    drift_failure(&env, 1);
    printf("M3_PATH_OUTPUT_REGIONS_PASS mode=%s cases=%d checks=%llu input_trace=%016llx faults=16 drift=2\n",
            baseline ? "baseline" : "candidate", cases,
            (unsigned long long)checks, (unsigned long long)combined_trace);
    return 0;
}
