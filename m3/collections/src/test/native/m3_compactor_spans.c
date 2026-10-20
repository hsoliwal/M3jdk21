/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Adapted from m3_mask_window_spans.c. Fake JNI counts transferred payload;
 * actual JVM behavior is qualified separately by the owning Java/JNI gate. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CAPACITY 704
#define CHUNK 256

typedef struct { jsize size; void *data; } fixture_array;
enum { NONE, STATE_FAULT, ROW_FAULT, WRITE_FAULT, ALLOC_FAULT, LENGTH_FAULT };
enum { NO_EXCEPTION, REGION_EXCEPTION, ALLOCATION_EXCEPTION, INVALID_EXCEPTION, DRIFT_EXCEPTION };
static int row_reads, row_elements, state_reads, writes, allocations, length_reads;
static int fault, fault_at, pending, mode, drift_at = -1;
static jbyte drift_value;
static jint row_first, state_first, extent;
static jint row_ints[CAPACITY], out_ints[CAPACITY];
static jlong row_longs[CAPACITY], out_longs[CAPACITY];
static jbyte marks[CAPACITY];
static fixture_array int_rows, long_rows, states, result;
static char invalid_class, drift_class;

static void require(int ok, const char *message) {
    if (!ok) {
        fprintf(stderr, "COMPACTOR_SPAN_ASSERT mode=%d: %s\n", mode, message);
        exit(1);
    }
}
static void bounds(jarray array, jsize first, jsize count) {
    require(pending == NO_EXCEPTION, "JNI work after pending exception");
    require(array != NULL, "null JNI array access");
    fixture_array *a = (fixture_array *)array;
    require(first >= 0 && count >= 0 && first <= a->size && count <= a->size - first,
            "JNI region bounds");
}
static int fail(int kind, int call) {
    if (fault == kind && call == fault_at) {
        pending = kind == ALLOC_FAULT ? ALLOCATION_EXCEPTION : REGION_EXCEPTION;
        return 1;
    }
    return 0;
}
static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void)env;
    require(pending == NO_EXCEPTION && array != NULL, "array length admission");
    if (fail(LENGTH_FAULT, ++length_reads)) return 0;
    return ((fixture_array *)array)->size;
}
static jboolean JNICALL exception(JNIEnv *env) {
    (void)env;
    return pending == NO_EXCEPTION ? JNI_FALSE : JNI_TRUE;
}
static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;
    require(pending == NO_EXCEPTION, "class lookup after pending exception");
    if (strcmp(name, "java/lang/IllegalArgumentException") == 0) return (jclass)&invalid_class;
    require(strcmp(name, "java/util/ConcurrentModificationException") == 0, "unexpected exception class");
    return (jclass)&drift_class;
}
static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)message;
    require(pending == NO_EXCEPTION, "exception overwritten");
    pending = type == (jclass)&invalid_class ? INVALID_EXCEPTION : DRIFT_EXCEPTION;
    return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject value) { (void)env; (void)value; }
static jobject new_result(jsize count, int longs) {
    require(pending == NO_EXCEPTION && count >= 0 && count <= CAPACITY, "result allocation bounds");
    if (fail(ALLOC_FAULT, ++allocations)) return NULL;
    result.size = count;
    result.data = longs ? (void *)out_longs : (void *)out_ints;
    if (drift_at >= 0) marks[drift_at] = drift_value;
    return (jobject)&result;
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize count) {
    (void)env; return (jintArray)new_result(count, 0);
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize count) {
    (void)env; return (jlongArray)new_result(count, 1);
}
static void JNICALL get_bytes(JNIEnv *env, jbyteArray array, jsize first, jsize count, jbyte *out) {
    (void)env; bounds(array, first, count);
    require(count <= CHUNK, "state chunk bound");
    if (fail(STATE_FAULT, ++state_reads)) return;
    memcpy(out, (jbyte *)((fixture_array *)array)->data + first, (size_t)count);
}
static void JNICALL get_ints(JNIEnv *env, jintArray array, jsize first, jsize count, jint *out) {
    (void)env; bounds(array, first, count);
    require(count <= CHUNK, "int chunk bound");
    row_elements += count;
    if (fail(ROW_FAULT, ++row_reads)) return;
    memcpy(out, (jint *)((fixture_array *)array)->data + first, (size_t)count * sizeof(jint));
}
static void JNICALL get_longs(JNIEnv *env, jlongArray array, jsize first, jsize count, jlong *out) {
    (void)env; bounds(array, first, count);
    require(count <= CHUNK, "long chunk bound");
    row_elements += count;
    if (fail(ROW_FAULT, ++row_reads)) return;
    memcpy(out, (jlong *)((fixture_array *)array)->data + first, (size_t)count * sizeof(jlong));
}
static void JNICALL put_ints(JNIEnv *env, jintArray array, jsize first, jsize count, const jint *in) {
    (void)env; bounds(array, first, count);
    if (fail(WRITE_FAULT, ++writes)) return;
    memcpy((jint *)((fixture_array *)array)->data + first, in, (size_t)count * sizeof(jint));
}
static void JNICALL put_longs(JNIEnv *env, jlongArray array, jsize first, jsize count, const jlong *in) {
    (void)env; bounds(array, first, count);
    if (fail(WRITE_FAULT, ++writes)) return;
    memcpy((jlong *)((fixture_array *)array)->data + first, in, (size_t)count * sizeof(jlong));
}

extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
        JNIEnv *, jclass, jintArray, jbyteArray, jint);
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
        JNIEnv *, jclass, jintArray, jbyteArray, jint, jint, jint);
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
        JNIEnv *, jclass, jintArray, jint, jbyteArray, jint, jint, jint);
extern jlongArray Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
        JNIEnv *, jclass, jlongArray, jint, jbyteArray, jint, jint, jint);

static void configure(int selected_mode, jint count) {
    mode = selected_mode; extent = count;
    row_first = mode == 0 ? 0 : 17;
    state_first = mode < 2 ? 0 : 23;
    int_rows = (fixture_array){count + row_first + (mode == 0 ? 0 : 9), row_ints};
    long_rows = (fixture_array){int_rows.size, row_longs};
    states = (fixture_array){count + state_first + (mode < 2 ? 0 : 11), marks};
    memset(marks, 255, sizeof marks);
    memset(marks + state_first, 0, (size_t)count);
    for (int i = 0; i < CAPACITY; i++) {
        row_ints[i] = i % 5 == 0 ? INT32_MIN : i % 7 == 0 ? INT32_MAX : i % 13 - 6;
        row_longs[i] = i % 5 == 0 ? INT64_MIN : i % 7 == 0 ? INT64_MAX
                : (jlong)i * INT64_C(4294967297) - INT64_C(1000000000000);
    }
    fault = NONE; fault_at = 1; drift_at = -1;
}
static jobject invoke(JNIEnv *env, jint mask, int null_rows) {
    row_reads = row_elements = state_reads = writes = allocations = length_reads = 0;
    pending = NO_EXCEPTION; result.size = -1;
    memset(out_ints, 0, sizeof out_ints);
    memset(out_longs, 0, sizeof out_longs);
    jintArray ints = null_rows ? NULL : (jintArray)&int_rows;
    jlongArray longs = null_rows ? NULL : (jlongArray)&long_rows;
    switch (mode) {
        case 0: return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
                env, NULL, ints, (jbyteArray)&states, mask);
        case 1: return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative(
                env, NULL, ints, (jbyteArray)&states, row_first, extent, mask);
        case 2: return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
                env, NULL, ints, row_first, (jbyteArray)&states, state_first, extent, mask);
        default: return (jobject)Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
                env, NULL, longs, row_first, (jbyteArray)&states, state_first, extent, mask);
    }
}
static int matches(jbyte state, jint mask) { return (((int)state & 255) & mask) == mask; }
static void selection(JNIEnv *env, jint mask) {
    jbyte original_states[CAPACITY];
    jint original_ints[CAPACITY];
    jlong original_longs[CAPACITY];
    memcpy(original_states, marks, sizeof marks);
    memcpy(original_ints, row_ints, sizeof row_ints);
    memcpy(original_longs, row_longs, sizeof row_longs);
    int expected = 0, expected_reads = 0, expected_elements = 0;
    for (int start = 0; start < extent; start += CHUNK) {
        int end = start + CHUNK < extent ? start + CHUNK : extent;
        int first = end, last = -1;
        for (int i = start; i < end; i++) {
            if (matches(original_states[state_first + i], mask)) {
                if (first == end) first = i;
                last = i;
                expected++;
            }
        }
        if (last >= 0) { expected_reads++; expected_elements += last - first + 1; }
    }
    require(invoke(env, mask, 0) != NULL && pending == NO_EXCEPTION, "successful selection");
    require(allocations == 1 && result.size == expected, "exact detached output size");
    require(row_reads == expected_reads && row_elements == expected_elements, "minimal enclosing row spans");
    require(state_reads == (mask == 0 ? 0 : 2 * ((extent + CHUNK - 1) / CHUNK)), "two state passes or zero-mask bypass");
    int output = 0;
    for (int i = 0; i < extent; i++) {
        if (!matches(original_states[state_first + i], mask)) continue;
        if (mode == 3) require(out_longs[output] == original_longs[row_first + i], "long result oracle");
        else require(out_ints[output] == original_ints[row_first + i], "int result oracle");
        output++;
    }
    require(memcmp(original_states, marks, sizeof marks) == 0, "states unchanged");
    require(memcmp(original_ints, row_ints, sizeof row_ints) == 0, "int rows unchanged");
    require(memcmp(original_longs, row_longs, sizeof row_longs) == 0, "long rows unchanged");
}

int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length; api.ExceptionCheck = exception;
    api.FindClass = find_class; api.ThrowNew = throw_new; api.DeleteLocalRef = delete_ref;
    api.NewIntArray = new_ints; api.NewLongArray = new_longs;
    api.GetByteArrayRegion = get_bytes; api.GetIntArrayRegion = get_ints; api.GetLongArrayRegion = get_longs;
    api.SetIntArrayRegion = put_ints; api.SetLongArrayRegion = put_longs;
    JNIEnv env = &api;
    const jint lengths[] = {0, 1, 255, 256, 257, 512, 513, 600};
    for (int variant = 0; variant < 4; variant++) {
        for (int position = 0; position < 600; position++) {
            configure(variant, 600);
            marks[state_first + position] = (jbyte)128;
            selection(&env, 128);
            require(row_elements == 1, "singleton transfers one row");
        }
        for (size_t i = 0; i < sizeof lengths / sizeof lengths[0]; i++) {
            configure(variant, lengths[i]);
            selection(&env, 128);
            require(row_elements == 0, "rejected chunks transfer no rows");
            memset(marks + state_first, 255, (size_t)extent);
            selection(&env, 165);
            selection(&env, 255);
            fault = STATE_FAULT;
            selection(&env, 0);
            require(row_elements == extent, "zero mask copies every row");
        }
        configure(variant, 600);
        marks[state_first + 255] = marks[state_first + 256] =
                marks[state_first + 511] = marks[state_first + 512] = (jbyte)128;
        selection(&env, 128);
        require(row_elements == 258 && row_reads == 3, "cross-chunk enclosing span accounting");

        const int kinds[] = {STATE_FAULT, STATE_FAULT, STATE_FAULT, ROW_FAULT,
                            ROW_FAULT, WRITE_FAULT, WRITE_FAULT, ALLOC_FAULT,
                            LENGTH_FAULT, LENGTH_FAULT};
        const int calls[] = {1, 4, 5, 1, 2, 1, 2, 1, 1, 2};
        for (size_t i = 0; i < sizeof kinds / sizeof kinds[0]; i++) {
            configure(variant, 600);
            memset(marks + state_first, 128, (size_t)extent);
            fault = kinds[i]; fault_at = calls[i];
            require(invoke(&env, 128, 0) == NULL, "fault returns no result");
            require(pending == (fault == ALLOC_FAULT ? ALLOCATION_EXCEPTION : REGION_EXCEPTION),
                    "original region/allocation exception retained");
            if (fault == STATE_FAULT && fault_at <= 4) require(row_reads == 0 && writes == 0, "state failure stops rows");
            if (fault == ROW_FAULT && fault_at == 1) require(writes == 0, "row failure stops writes");
            if (fault == WRITE_FAULT && fault_at == 1) {
                for (int j = 0; j < CAPACITY; j++) require(out_ints[j] == 0 && out_longs[j] == 0, "failed write stays unpublished");
            }
            if (fault == ALLOC_FAULT || fault == LENGTH_FAULT) require(row_reads == 0 && writes == 0, "early failure stops emission");
        }
        for (int mask = -1; mask <= 256; mask += 257) {
            configure(variant, 600);
            require(invoke(&env, mask, 0) == NULL && pending == INVALID_EXCEPTION, "invalid unsigned mask refused");
            require(allocations == 0 && row_reads == 0, "invalid mask before allocation/read");
        }
        configure(variant, 600);
        require(invoke(&env, 128, 1) == NULL && pending == INVALID_EXCEPTION, "null rows refused");
        if (variant > 0) {
            configure(variant, 600); row_first = -1;
            require(invoke(&env, 128, 0) == NULL && pending == INVALID_EXCEPTION, "invalid row window refused");
        }
        if (variant > 1) {
            configure(variant, 600); state_first = 1000;
            require(invoke(&env, 128, 0) == NULL && pending == INVALID_EXCEPTION, "invalid state window refused");
        }
        configure(variant, 600);
        drift_at = state_first + 255; drift_value = (jbyte)128;
        require(invoke(&env, 128, 0) == NULL && pending == DRIFT_EXCEPTION, "count increase refused");
        require(writes == 0 && row_elements == 1, "increase guard remains after row read before output");
        configure(variant, 600);
        marks[state_first + 599] = (jbyte)128;
        drift_at = state_first + 599; drift_value = 0;
        require(invoke(&env, 128, 0) == NULL && pending == DRIFT_EXCEPTION, "count decrease refused");
        require(writes == 0 && row_elements == 0, "final count guard survives rejected chunks");
        configure(variant, 600);
        marks[state_first] = (jbyte)128;
        drift_at = state_first + 599; drift_value = (jbyte)128;
        require(invoke(&env, 128, 0) == NULL && pending == DRIFT_EXCEPTION, "late count increase refused");
        require(writes == 1 && row_elements == 2, "late increase preserves prefix and stops publication");
    }
    puts("JNI_COMPACTOR_SPANS_PASS singleton_cases=2400 singleton_rows=1 rejected_rows=0 cross_chunk_rows=258/600 modes=4 region_allocation_faults=40 count_drift_cases=12");
    return 0;
}
