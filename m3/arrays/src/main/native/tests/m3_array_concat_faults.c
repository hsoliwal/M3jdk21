/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Adapted from the existing m3_compactor_spans.c JNI vtable fixture. This finite
 * injected-result proof complements the real JVM native-required array tests. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define SEGMENTS 4
#define SOURCE_CAPACITY 2049
#define OUTPUT_CAPACITY (SEGMENTS * SOURCE_CAPACITY)
#define CHUNK 1024

typedef struct { jsize length; jchar *units; int references; } fixture_chars;
static fixture_chars input[SEGMENTS], output;
static jchar source_units[SEGMENTS][SOURCE_CAPACITY], output_units[OUTPUT_CAPACITY];
static jsize segment_count;
static char segments_identity, exception_class;
enum { NONE, ORIGINAL_EXCEPTION, ILLEGAL_EXCEPTION };
static int pending, fail_at, fail_pending, object_reads, allocations, reads, writes;
static int class_references, lookups, throws, checks, cases;

static void require(int condition, const char *message) {
    checks++;
    if (!condition) {
        fprintf(stderr, "ARRAY_CONCAT_FAULT_ASSERT case=%d get=%d fail_at=%d: %s\n",
                cases, object_reads, fail_at, message);
        exit(1);
    }
}

static fixture_chars *chars(jarray array) {
    for (int i = 0; i < SEGMENTS; i++) if (array == (jarray)&input[i]) return &input[i];
    if (array == (jarray)&output) return &output;
    require(0, "unknown char-array identity");
    return NULL;
}

static jsize JNICALL length(JNIEnv *env, jarray array) {
    (void)env;
    require(pending == NONE && array != NULL, "length admission without pending exception");
    if (array == (jarray)&segments_identity) return segment_count;
    fixture_chars *value = chars(array);
    require(value->references > 0, "length needs an active local reference");
    return value->length;
}

static jboolean JNICALL exception(JNIEnv *env) {
    (void)env;
    return pending == NONE ? JNI_FALSE : JNI_TRUE;
}

static jobject JNICALL element(JNIEnv *env, jobjectArray array, jsize index) {
    (void)env;
    require(pending == NONE, "object lookup stops after a pending exception");
    require(array == (jobjectArray)&segments_identity && index >= 0 && index < segment_count,
            "object-array bounds");
    object_reads++;
    if (object_reads == fail_at) {
        if (fail_pending) pending = ORIGINAL_EXCEPTION;
        return NULL;
    }
    input[index].references++;
    return (jobject)&input[index];
}

static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;
    require(pending == NONE, "class lookup cannot replace a pending throwable");
    require(strcmp(name, "java/lang/IllegalArgumentException") == 0, "incumbent exception class");
    lookups++;
    class_references++;
    return (jclass)&exception_class;
}

static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    require(pending == NONE && type == (jclass)&exception_class, "existing throwable preserved");
    require(strcmp(message, "null UTF-16 segment") == 0, "incumbent refusal message");
    throws++;
    pending = ILLEGAL_EXCEPTION;
    return 0;
}

static void JNICALL delete_ref(JNIEnv *env, jobject object) {
    (void)env;
    if (object == (jobject)&exception_class) {
        require(class_references == 1, "balanced exception class reference");
        class_references--;
    } else {
        fixture_chars *value = chars((jarray)object);
        require(value->references > 0, "local reference released exactly once");
        value->references--;
    }
}

static jcharArray JNICALL new_chars(JNIEnv *env, jsize size) {
    (void)env;
    require(pending == NONE && size >= 0 && size <= OUTPUT_CAPACITY, "bounded output admission");
    require(output.references == 0, "one detached output per call");
    allocations++;
    output.length = size;
    output.references = 1;
    return (jcharArray)&output;
}

static void range(jarray array, jsize start, jsize count) {
    require(pending == NONE, "region work stops after pending throwable");
    fixture_chars *value = chars(array);
    require(value->references > 0 && start >= 0 && count >= 0
            && start <= value->length && count <= value->length - start, "exact JNI region bounds");
    require(count <= CHUNK, "existing bounded JNI transfer chunk");
}

static void JNICALL get_chars(JNIEnv *env, jcharArray array, jsize start, jsize count, jchar *out) {
    (void)env;
    range((jarray)array, start, count);
    reads++;
    memcpy(out, chars((jarray)array)->units + start, (size_t)count * sizeof(jchar));
}

static void JNICALL put_chars(JNIEnv *env, jcharArray array, jsize start, jsize count, const jchar *in) {
    (void)env;
    range((jarray)array, start, count);
    require(array == (jcharArray)&output, "only detached output is mutated");
    writes++;
    memcpy(output.units + start, in, (size_t)count * sizeof(jchar));
}

extern jcharArray Java_com_m3_arrays_M3ArrayNative_nativeConcatenateUtf16(
        JNIEnv *, jclass, jobjectArray);

static void configure(jsize count, int injected_call, int with_pending) {
    segment_count = count;
    fail_at = injected_call;
    fail_pending = with_pending;
    pending = NONE;
    object_reads = allocations = reads = writes = class_references = lookups = throws = 0;
    memset(output_units, 0, sizeof output_units);
    output = (fixture_chars){0, output_units, 0};
    const jsize lengths[SEGMENTS] = {0, 1, 1025, 2049};
    for (int i = 0; i < SEGMENTS; i++) {
        input[i] = (fixture_chars){lengths[i], source_units[i], 0};
        for (jsize at = 0; at < input[i].length; at++) {
            source_units[i][at] = (jchar)((uint32_t)at * UINT32_C(33013) + (uint32_t)i);
        }
        if (input[i].length > 1024) {
            source_units[i][1023] = (jchar)0xd83d;
            source_units[i][1024] = (jchar)0xde00;
        }
        if (input[i].length > 0) source_units[i][input[i].length - 1] = (jchar)0xd800;
    }
    cases++;
}

static void locals_balanced(void) {
    for (int i = 0; i < SEGMENTS; i++) require(input[i].references == 0, "source local references balanced");
    require(class_references == 0, "exception class local reference balanced");
}

int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = length;
    api.GetObjectArrayElement = element;
    api.ExceptionCheck = exception;
    api.FindClass = find_class;
    api.ThrowNew = throw_new;
    api.DeleteLocalRef = delete_ref;
    api.NewCharArray = new_chars;
    api.GetCharArrayRegion = get_chars;
    api.SetCharArrayRegion = put_chars;
    JNIEnv env = &api;

    for (jsize count = 0; count <= SEGMENTS; count++) {
        configure(count, 0, 0);
        jchar original[SEGMENTS][SOURCE_CAPACITY];
        memcpy(original, source_units, sizeof original);
        jcharArray value = Java_com_m3_arrays_M3ArrayNative_nativeConcatenateUtf16(
                &env, NULL, (jobjectArray)&segments_identity);
        require(value == (jcharArray)&output && pending == NONE, "normal work returns independent output");
        require(allocations == 1 && object_reads == 2 * count, "unchanged two-pass materialization work");
        jsize at = 0;
        int chunks = 0;
        for (jsize index = 0; index < count; index++) {
            chunks += (input[index].length + CHUNK - 1) / CHUNK;
            for (jsize unit = 0; unit < input[index].length; unit++) {
                require(output.units[at++] == source_units[index][unit], "exact raw UTF-16 output");
            }
        }
        require(at == output.length && reads == chunks && writes == chunks, "exact output size and chunks");
        require(memcmp(original, source_units, sizeof original) == 0, "source arrays stay unchanged");
        locals_balanced();
        delete_ref(&env, (jobject)value);
        require(output.references == 0, "caller can release returned result");
    }

    for (int with_pending = 0; with_pending <= 1; with_pending++) {
        for (int call = 1; call <= 2 * SEGMENTS; call++) {
            configure(SEGMENTS, call, with_pending);
            jcharArray value = Java_com_m3_arrays_M3ArrayNative_nativeConcatenateUtf16(
                    &env, NULL, (jobjectArray)&segments_identity);
            require(value == NULL, "failed lookup publishes no output");
            require(pending == (with_pending ? ORIGINAL_EXCEPTION : ILLEGAL_EXCEPTION),
                    "both passes preserve a pending throwable or refuse bare null");
            require(object_reads == call, "failure stops further object lookups");
            require(lookups == !with_pending && throws == !with_pending, "only bare null creates refusal");
            require(allocations == (call > SEGMENTS), "first-pass refusal allocates no output");
            require(output.references == 0, "second-pass refusal releases unpublished output");
            locals_balanced();
        }
    }
    printf("M3_ARRAY_CONCAT_FAULT_PASS cases=%d checks=%d two_pass_null=true pending_preserved=true\n",
            cases, checks);
    return 0;
}
