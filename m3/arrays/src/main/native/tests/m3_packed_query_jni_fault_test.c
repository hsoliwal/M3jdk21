/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 */
/* Bounded JNI contract model. Real JVM parity is a separate Java test lane. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "m3_packed_codec.h"

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3ArrayNative_nativeQueryPacked(
    JNIEnv *, jclass, jbyteArray, jint, jint, jint, jlong, jlong, jboolean);
static unsigned long checks;
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "query JNI check failed at %d: %s\n", __LINE__, #x); exit(1); } } while (0)
static unsigned char source_token, exception_token;
static struct {
    jsize length;
    jint width, next_byte, match_index;
    int pending, length_failure, read_failure, find_failure, throw_failure;
    int lengths, reads, finds, throws, deletes;
    const char *exception_type;
} state;

static jboolean JNICALL pending(JNIEnv *env) { (void)env; return state.pending ? JNI_TRUE : JNI_FALSE; }
static jsize JNICALL length(JNIEnv *env, jarray source) {
    (void)env; CHECK(!state.pending); CHECK(source == (jarray)(void *)&source_token);
    ++state.lengths;
    if (state.length_failure) state.pending = 1;
    return state.length;
}
static void JNICALL read_bytes(JNIEnv *env, jbyteArray source, jsize start, jsize n, jbyte *dst) {
    (void)env; CHECK(!state.pending); CHECK(source == (jbyteArray)(void *)&source_token);
    CHECK(start == state.next_byte); CHECK(n > 0 && n <= (jsize)M3_PACKED_CODEC_MAX);
    CHECK(start >= 0 && start <= state.length && n <= state.length - start);
    CHECK(start % state.width == 0 && n % state.width == 0);
    ++state.reads;
    if (state.read_failure == state.reads) { state.pending = 1; return; }
    for (jsize i = 0; i < n; ++i) {
        jint element = (start + i) / state.width;
        jint lane = (start + i) % state.width;
        dst[i] = element == state.match_index && lane == 0 ? 7 : 0;
    }
    state.next_byte += n;
}
static jclass JNICALL find(JNIEnv *env, const char *name) {
    (void)env; CHECK(!state.pending); CHECK(state.exception_type && strcmp(name, state.exception_type) == 0);
    ++state.finds;
    if (state.find_failure) {
        if (state.find_failure == 1) state.pending = 1;
        return NULL;
    }
    return (jclass)(void *)&exception_token;
}
static jint JNICALL raise(JNIEnv *env, jclass type, const char *message) {
    (void)env; CHECK(!state.pending); CHECK(type == (jclass)(void *)&exception_token);
    CHECK(message && *message); ++state.throws;
    if (!state.throw_failure) state.pending = 1;
    return state.throw_failure ? JNI_ERR : JNI_OK;
}
static void JNICALL delete_local(JNIEnv *env, jobject type) {
    (void)env; CHECK(type == (jobject)(void *)&exception_token); ++state.deletes;
}
static void reset(jint bits, jint elements) {
    memset(&state, 0, sizeof state);
    state.width = bits / 8;
    state.length = elements * state.width;
    state.match_index = -1;
}
static jint query(JNIEnv *env, jint bits, jint from, jint to, jlong low, jlong high, jboolean first) {
    return Java_jdk_m3_arrays_M3ArrayNative_nativeQueryPacked(env, NULL,
        (jbyteArray)(void *)&source_token, bits, from, to, low, high, first);
}
static void error(JNIEnv *env, jint bits, jint from, jint to, jlong low, jlong high, const char *type) {
    state.exception_type = type;
    CHECK(query(env, bits, from, to, low, high, JNI_FALSE) == INT32_MIN);
    CHECK(state.pending == 1 && state.reads == 0 && state.finds == 1 && state.throws == 1);
    CHECK(state.deletes == 1);
}

int main(void) {
    const struct JNINativeInterface_ table = {
        .ExceptionCheck = pending, .GetArrayLength = length, .GetByteArrayRegion = read_bytes,
        .FindClass = find, .ThrowNew = raise, .DeleteLocalRef = delete_local
    };
    JNIEnv environment = &table;
    JNIEnv *env = &environment;
    const int widths[] = {40, 48, 56};
    for (size_t w = 0; w < 3; ++w) {
        jint bits = (jint)widths[w], chunk = (jint)M3_PACKED_CODEC_MAX / (bits / 8);
        reset(bits, chunk * 2 + 3);
        CHECK(query(env, bits, 0, chunk * 2 + 3, 0, 0, JNI_FALSE) == chunk * 2 + 3);
        CHECK(state.reads == 3 && state.lengths == 1 && !state.pending);
        reset(bits, chunk * 2 + 3); state.match_index = chunk;
        CHECK(query(env, bits, 0, chunk * 2 + 3, 7, 7, JNI_TRUE) == chunk);
        CHECK(state.reads == 2 && !state.pending);
        reset(bits, chunk * 2 + 3); state.match_index = 0; state.read_failure = 2;
        CHECK(query(env, bits, 0, chunk * 2 + 3, 7, 7, JNI_TRUE) == 0);
        CHECK(state.reads == 1 && !state.pending);
        reset(bits, chunk * 2 + 3); state.read_failure = 2;
        CHECK(query(env, bits, 0, chunk * 2 + 3, 0, 0, JNI_FALSE) == INT32_MIN);
        CHECK(state.reads == 2 && state.pending && !state.finds);
        reset(bits, chunk + 7); state.match_index = chunk + 6; state.next_byte = 3 * state.width;
        CHECK(query(env, bits, 3, chunk + 7, 7, 7, JNI_TRUE) == chunk + 6);
        CHECK(state.reads == 2 && !state.pending);
        reset(bits, 1);
        CHECK(query(env, bits, 1, 1, 0, 0, JNI_FALSE) == 0 && state.reads == 0);
        CHECK(query(env, bits, 1, 1, 0, 0, JNI_TRUE) == -1 && state.reads == 0);
        reset(bits, 1); state.read_failure = 1;
        CHECK(query(env, bits, 0, 1, 0, 0, JNI_FALSE) == INT32_MIN && state.pending);
        CHECK(state.reads == 1 && !state.finds);
    }
    reset(40, 1); state.pending = 1;
    CHECK(query(env, 40, 0, 1, 0, 0, JNI_FALSE) == INT32_MIN && !state.lengths && !state.reads);
    reset(40, 1); state.length_failure = 1;
    CHECK(query(env, 40, 0, 1, 0, 0, JNI_FALSE) == INT32_MIN && state.pending && !state.reads);
    reset(40, 1); state.exception_type = "java/lang/NullPointerException";
    CHECK(Java_jdk_m3_arrays_M3ArrayNative_nativeQueryPacked(env, NULL, NULL, 40, 0, 0, 0, 0, JNI_FALSE) == INT32_MIN);
    CHECK(state.pending && !state.lengths && !state.reads);
    reset(40, 1); error(env, 32, 0, 1, 0, 0, "java/lang/IllegalArgumentException");
    reset(40, 1); state.length = -1; error(env, 40, 0, 1, 0, 0, "java/lang/IllegalArgumentException");
    reset(40, 1); state.length = 6; error(env, 40, 0, 0, 0, 0, "java/lang/IllegalArgumentException");
    reset(40, 1); error(env, 40, -1, 0, 0, 0, "java/lang/IndexOutOfBoundsException");
    reset(40, 1); error(env, 40, 1, 0, 0, 0, "java/lang/IndexOutOfBoundsException");
    reset(40, 1); error(env, 40, 0, INT32_MAX, 0, 0, "java/lang/IndexOutOfBoundsException");
    reset(40, 0); error(env, 40, 0, 0, 1, 0, "java/lang/IllegalArgumentException");
    for (int failure = 1; failure <= 2; ++failure) {
        reset(40, 0); state.find_failure = failure; state.exception_type = "java/lang/IllegalArgumentException";
        CHECK(query(env, 40, 0, 0, 1, 0, JNI_FALSE) == INT32_MIN);
        CHECK(state.finds == 1 && !state.throws && !state.deletes && !state.reads);
        CHECK(state.pending == (failure == 1));
    }
    reset(40, 0); state.throw_failure = 1; state.exception_type = "java/lang/IllegalArgumentException";
    CHECK(query(env, 40, 0, 0, 1, 0, JNI_FALSE) == INT32_MIN);
    CHECK(state.finds == 1 && state.throws == 1 && !state.pending && !state.reads);
    printf("PACKED_QUERY_JNI_FAULT_PASS assertions=%lu actual_JVM=0\n", checks);
    return 0;
}
