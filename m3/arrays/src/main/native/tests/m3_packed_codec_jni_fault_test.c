/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: target JNI names and shared exception-helper fault coverage.
 * Derived from synexia_cpu_codec_jni_fault_test.c at
 * 6f878f9e5d6c5044eda15efb9e743594e70ce7f7; legacy operations are excluded. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "m3_packed_codec.h"

static unsigned long m3_checks;
#define M3_CHECK(condition) do { \
    ++m3_checks; \
    if (!(condition)) { \
        fprintf(stderr, "M3_CHECK_FAILED %s:%d: %s\n", __FILE__, __LINE__, #condition); \
        exit(EXIT_FAILURE); \
    } \
} while (0)

JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodePacked(JNIEnv *, jclass, jlongArray, jint);
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodePacked(JNIEnv *, jclass, jbyteArray, jint);
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeUnsigned64(JNIEnv *, jclass, jlongArray);
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeUnsigned64(JNIEnv *, jclass, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeEncodeZigZag64(JNIEnv *, jclass, jlongArray);
JNIEXPORT jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeDecodeZigZag64(JNIEnv *, jclass, jbyteArray);

/* This documents a fault model. A real checked-JNI JVM is a separate gate. */
enum model_case {
    NULL_ALLOCATION, EXCEPTION_ALLOCATION, SUCCESS, READ_EXCEPTION,
    NONNULL_EXCEPTION_ALLOCATION, WRITE_EXCEPTION, LENGTH_EXCEPTION,
    ENTRY_EXCEPTION, NULL_INPUT, INPUT_BOUND, NEGATIVE_LENGTH,
    FIND_EXCEPTION, SILENT_FIND_FAILURE, SILENT_THROW_FAILURE,
    EMPTY_SUCCESS, EMPTY_ALLOCATION, BAD_WIDTH, BAD_GEOMETRY
};

struct model_state {
    enum model_case scenario;
    int pending, lengths, reads, allocations, writes, finds, throws, deletes;
    jsize input_count, output_count;
    jint bits;
    jbyte input_first, output_first;
    const char *exception_type;
    const char *exception_message;
};

struct expected_calls {
    int pending, lengths, reads, allocations, writes, finds, throws, deletes, returned;
};

/* Independently frozen observable JNI call counts, in model_case order. */
static const struct expected_calls expected[] = {
    {1, 1, 1, 1, 0, 1, 1, 1, 0}, /* silent allocation failure -> OOME */
    {1, 1, 1, 1, 0, 0, 0, 0, 0}, /* allocation exception retained */
    {0, 1, 1, 1, 1, 0, 0, 0, 1}, /* detached successful result */
    {1, 1, 1, 0, 0, 0, 0, 0, 0}, /* no allocation after failed read */
    {1, 1, 1, 1, 0, 0, 0, 0, 0}, /* nonnull does not hide exception */
    {1, 1, 1, 1, 1, 0, 0, 0, 0}, /* failed region write -> null */
    {1, 1, 0, 0, 0, 0, 0, 0, 0}, /* no read after failed length */
    {1, 0, 0, 0, 0, 0, 0, 0, 0}, /* no JNI array work on entry exception */
    {1, 0, 0, 0, 0, 1, 1, 1, 0}, /* null input -> NPE */
    {1, 1, 0, 0, 0, 1, 1, 1, 0}, /* oversized input -> IAE */
    {1, 1, 0, 0, 0, 1, 1, 1, 0}, /* defensive negative model length */
    {1, 1, 1, 1, 0, 1, 0, 0, 0}, /* failed class lookup preserves exception */
    {0, 1, 1, 1, 0, 1, 0, 0, 0}, /* Java facade must guard silent null */
    {0, 1, 1, 1, 0, 1, 1, 1, 0}, /* failed ThrowNew still cleans local ref */
    {0, 1, 0, 1, 0, 0, 0, 0, 1}, /* empty result has allocation, no regions */
    {1, 1, 0, 1, 0, 1, 1, 1, 0}, /* empty allocation failure -> OOME */
    {1, 1, 0, 0, 0, 1, 1, 1, 0}, /* packed width rejected before regions */
    {1, 1, 0, 0, 0, 1, 1, 1, 0}  /* packed geometry rejected before regions */
};

static struct model_state state;
static unsigned char source_fixture, result_fixture, class_fixture;

static jboolean JNICALL pending_exception(JNIEnv *env) {
    (void)env;
    return state.pending ? JNI_TRUE : JNI_FALSE;
}

static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void)env;
    M3_CHECK(!state.pending && array == (jarray)(void *)&source_fixture);
    ++state.lengths;
    if (state.scenario == LENGTH_EXCEPTION) state.pending = 1;
    return state.input_count;
}

static void JNICALL longs(JNIEnv *env, jlongArray array, jsize start, jsize size, jlong *out) {
    (void)env;
    M3_CHECK(!state.pending && array == (jlongArray)(void *)&source_fixture);
    M3_CHECK(start == 0 && size == state.input_count && size == 1);
    out[0] = 1;
    ++state.reads;
    if (state.scenario == READ_EXCEPTION) state.pending = 1;
}

static void JNICALL bytes(JNIEnv *env, jbyteArray array, jsize start, jsize size, jbyte *out) {
    (void)env;
    M3_CHECK(!state.pending && array == (jbyteArray)(void *)&source_fixture);
    M3_CHECK(start == 0 && size == state.input_count && size > 0 && size <= 7);
    for (jsize i = 0; i < size; ++i) out[i] = (jbyte)(i == 0 ? state.input_first : 0);
    ++state.reads;
    if (state.scenario == READ_EXCEPTION) state.pending = 1;
}

static jbyteArray JNICALL new_bytes(JNIEnv *env, jsize size) {
    (void)env;
    M3_CHECK(!state.pending && size == state.output_count);
    ++state.allocations;
    if (state.scenario == EXCEPTION_ALLOCATION || state.scenario == NONNULL_EXCEPTION_ALLOCATION)
        state.pending = 1;
    if (state.scenario == SUCCESS || state.scenario == NONNULL_EXCEPTION_ALLOCATION
            || state.scenario == WRITE_EXCEPTION || state.scenario == EMPTY_SUCCESS)
        return (jbyteArray)(void *)&result_fixture;
    return NULL;
}

static jlongArray JNICALL new_longs(JNIEnv *env, jsize size) {
    return (jlongArray)new_bytes(env, size);
}

static void JNICALL set_bytes(JNIEnv *env, jbyteArray array, jsize start, jsize size, const jbyte *in) {
    (void)env;
    M3_CHECK(!state.pending && array == (jbyteArray)(void *)&result_fixture);
    M3_CHECK(start == 0 && size == state.output_count && size > 0 && size <= 7);
    for (jsize i = 0; i < size; ++i) M3_CHECK(in[i] == (i == 0 ? state.output_first : 0));
    ++state.writes;
    if (state.scenario == WRITE_EXCEPTION) state.pending = 1;
}

static void JNICALL set_longs(JNIEnv *env, jlongArray array, jsize start, jsize size, const jlong *in) {
    (void)env;
    M3_CHECK(!state.pending && array == (jlongArray)(void *)&result_fixture);
    M3_CHECK(start == 0 && size == state.output_count && size == 1 && in[0] == 1);
    ++state.writes;
    if (state.scenario == WRITE_EXCEPTION) state.pending = 1;
}

static jclass JNICALL find(JNIEnv *env, const char *name) {
    (void)env;
    M3_CHECK(!state.pending && state.exception_type != NULL);
    M3_CHECK(strcmp(name, state.exception_type) == 0);
    ++state.finds;
    if (state.scenario == FIND_EXCEPTION) {
        state.pending = 1;
        return NULL;
    }
    if (state.scenario == SILENT_FIND_FAILURE) return NULL;
    return (jclass)(void *)&class_fixture;
}

static jint JNICALL raise_exception(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    M3_CHECK(!state.pending && type == (jclass)(void *)&class_fixture);
    M3_CHECK(state.exception_message != NULL && strcmp(message, state.exception_message) == 0);
    ++state.throws;
    if (state.scenario == SILENT_THROW_FAILURE) return JNI_ERR;
    state.pending = 1;
    return JNI_OK;
}

static void JNICALL delete_local(JNIEnv *env, jobject reference) {
    (void)env;
    /* DeleteLocalRef is permitted while an exception is pending. */
    M3_CHECK(reference == (jobject)(void *)&class_fixture);
    ++state.deletes;
}

static jobject invoke(JNIEnv *env, int operation) {
    jarray source = state.scenario == NULL_INPUT ? NULL : (jarray)(void *)&source_fixture;
    switch (operation) {
        case 0: return Java_com_m3_arrays_M3ArrayNative_nativeEncodeUnsigned64(env, NULL, (jlongArray)source);
        case 1: return Java_com_m3_arrays_M3ArrayNative_nativeDecodeUnsigned64(env, NULL, (jbyteArray)source);
        case 2: return Java_com_m3_arrays_M3ArrayNative_nativeEncodeZigZag64(env, NULL, (jlongArray)source);
        case 3: return Java_com_m3_arrays_M3ArrayNative_nativeDecodeZigZag64(env, NULL, (jbyteArray)source);
        default:
            M3_CHECK(operation >= 4 && operation < 10);
            if (operation % 2 == 0)
                return Java_com_m3_arrays_M3ArrayNative_nativeEncodePacked(env, NULL, (jlongArray)source, state.bits);
            return Java_com_m3_arrays_M3ArrayNative_nativeDecodePacked(env, NULL, (jbyteArray)source, state.bits);
    }
}

static void prepare(int operation, enum model_case scenario) {
    memset(&state, 0, sizeof state);
    state.scenario = scenario;
    state.input_count = state.output_count = 1;
    state.input_first = state.output_first = 1;
    state.exception_type = "java/lang/OutOfMemoryError";
    state.exception_message = "packed codec result allocation failed";
    if (operation == 2) state.output_first = 2;
    if (operation == 3) state.input_first = 2;
    if (operation >= 4) {
        jsize width = (jsize)(5 + (operation - 4) / 2);
        state.bits = (jint)(width * 8);
        if (operation % 2 == 0) state.output_count = width;
        else state.input_count = width;
    }
    switch (scenario) {
        case ENTRY_EXCEPTION:
            state.pending = 1;
            break;
        case NULL_INPUT:
            state.exception_type = "java/lang/NullPointerException";
            state.exception_message = "codec array";
            break;
        case INPUT_BOUND:
        case NEGATIVE_LENGTH:
            state.input_count = scenario == INPUT_BOUND ? (jsize)M3_PACKED_CODEC_MAX + 1 : -1;
            state.exception_type = "java/lang/IllegalArgumentException";
            state.exception_message = "codec array bound";
            break;
        case EMPTY_SUCCESS:
        case EMPTY_ALLOCATION:
            state.input_count = state.output_count = 0;
            break;
        case BAD_WIDTH:
        case BAD_GEOMETRY:
            M3_CHECK(operation >= 4 && operation < 10);
            state.exception_type = "java/lang/IllegalArgumentException";
            state.exception_message = "codec input/format/capacity contract";
            if (scenario == BAD_WIDTH) state.bits = 32;
            else if (operation % 2 == 0)
                state.input_count = (jsize)M3_PACKED_CODEC_MAX / (state.bits / 8) + 1;
            else state.input_count += 1;
            break;
        default:
            break;
    }
}

static void exercise(JNIEnv *env, int operation, enum model_case scenario) {
    prepare(operation, scenario);
    jobject result = invoke(env, operation);
    const struct expected_calls *want = &expected[(size_t)scenario];
    M3_CHECK(state.pending == want->pending);
    M3_CHECK(state.lengths == want->lengths);
    M3_CHECK(state.reads == want->reads);
    M3_CHECK(state.allocations == want->allocations);
    M3_CHECK(state.writes == want->writes);
    M3_CHECK(state.finds == want->finds);
    M3_CHECK(state.throws == want->throws);
    M3_CHECK(state.deletes == want->deletes);
    M3_CHECK(result == (want->returned ? (jobject)(void *)&result_fixture : NULL));
    M3_CHECK(result != (jobject)(void *)&source_fixture);
}

int main(void) {
    const struct JNINativeInterface_ table = {
        .FindClass = find, .ThrowNew = raise_exception, .ExceptionCheck = pending_exception,
        .DeleteLocalRef = delete_local,
        .GetArrayLength = length, .GetLongArrayRegion = longs, .GetByteArrayRegion = bytes,
        .NewByteArray = new_bytes, .NewLongArray = new_longs,
        .SetByteArrayRegion = set_bytes, .SetLongArrayRegion = set_longs
    };
    JNIEnv environment = &table;
    unsigned cases = 0;
    for (int operation = 0; operation < 10; ++operation) {
        for (int scenario = NULL_ALLOCATION; scenario <= EMPTY_ALLOCATION; ++scenario) {
            exercise(&environment, operation, (enum model_case)scenario);
            ++cases;
        }
        if (operation >= 4) {
            exercise(&environment, operation, BAD_WIDTH);
            exercise(&environment, operation, BAD_GEOMETRY);
            cases += 2;
        }
    }
    M3_CHECK(cases == 172);
    printf("M3_PACKED_JNI_DOCUMENTED_FAULT_MODEL_PASS assertions=%lu cases=%u not_a_real_JVM=1\n",
           m3_checks, cases);
    return 0;
}
