/* SPDX-License-Identifier: Apache-2.0 */
/* Original Synexia/M3JDK21 JNI owner regression: exact-current-master region/span test. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jsize size; void *data; } fixture_array;
static const char *case_name;
static int checks, row_calls, state_calls, output_calls, thrown;
static long long row_elements, state_elements, output_elements;

static void check(int condition, const char *message) {
    checks++;
    if (!condition) {
        fprintf(stderr, "FAIL %s: %s\n", case_name, message);
        exit(1);
    }
}

static void bounds(fixture_array *array, jsize first, jsize count) {
    check(first >= 0 && first <= array->size && count >= 0
          && count <= array->size - first, "JNI region out of bounds");
}

static jsize JNICALL get_length(JNIEnv *env, jarray src) {
    (void) env;
    return ((fixture_array *) src)->size;
}

static jboolean JNICALL has_exception(JNIEnv *env) {
    (void) env;
    return JNI_FALSE;
}

static jboolean JNICALL same_object(JNIEnv *env, jobject left, jobject right) {
    (void) env;
    return left == right ? JNI_TRUE : JNI_FALSE;
}

static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void) env;
    check(strcmp(name, "java/lang/IllegalArgumentException") == 0, "invalid exception class");
    return (jclass) &checks;
}

static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void) env;
    check(type == (jclass) &checks && message != NULL, "invalid native exception");
    thrown++;
    return 0;
}

static void JNICALL delete_ref(JNIEnv *env, jobject ref) {
    (void) env;
    check(ref == (jobject) &checks, "invalid local ref");
}

static void JNICALL read_bytes(JNIEnv *env, jbyteArray array, jsize first,
                               jsize count, jbyte *target) {
    (void) env;
    fixture_array *a = (fixture_array *) array;
    bounds(a, first, count);
    state_calls++;
    state_elements += count;
    memcpy(target, (jbyte *) a->data + first, (size_t) count);
}

static void JNICALL read_ints(JNIEnv *env, jintArray array, jsize first,
                              jsize count, jint *target) {
    (void) env;
    fixture_array *a = (fixture_array *) array;
    bounds(a, first, count);
    row_calls++;
    row_elements += count;
    memcpy(target, (jint *) a->data + first, (size_t) count * sizeof(jint));
}

static void JNICALL write_ints(JNIEnv *env, jintArray array, jsize first,
                               jsize count, const jint *source) {
    (void) env;
    fixture_array *a = (fixture_array *) array;
    bounds(a, first, count);
    output_calls++;
    output_elements += count;
    memcpy((jint *) a->data + first, source, (size_t) count * sizeof(jint));
}

extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jintArray, jint, jint);
extern jlong Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(JNIEnv *, jclass,
        jintArray, jbyteArray, jint, jint, jint, jint, jintArray, jint, jint);

static void reset(const char *name, jint *output, jsize size) {
    case_name = name;
    row_calls = state_calls = output_calls = thrown = 0;
    row_elements = state_elements = output_elements = 0;
    for (jsize i = 0; i < size; i++) output[i] = (jint) 0x74ac013d;
}

static void cursor(jlong result, jint next, jint written) {
    check(result == (((jlong) next << 32) | (jlong) written), "cursor or selected count");
}

static void guards(const jint *output, jsize size, jint offset, jint written) {
    for (jsize i = 0; i < size; i++) {
        if (i < offset || i >= offset + written)
            check(output[i] == (jint) 0x74ac013d, "caller output guard changed");
    }
}

int main(void) {
    struct JNINativeInterface_ api = {0};
    api.GetArrayLength = get_length;
    api.ExceptionCheck = has_exception;
    api.IsSameObject = same_object;
    api.GetByteArrayRegion = read_bytes;
    api.GetIntArrayRegion = read_ints;
    api.SetIntArrayRegion = write_ints;
    api.FindClass = find_class;
    api.ThrowNew = throw_new;
    api.DeleteLocalRef = delete_ref;
    JNIEnv env = &api;

    jint rows[1024], output[64];
    jbyte states[1024] = {0};
    for (int i = 0; i < 1024; ++i) rows[i] = i - 512;
    fixture_array r = {1024, rows}, s = {1024, states}, o = {64, output};

    reset("reject all", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 0, (jintArray) &o, 2, 32), 1024, 0);
    check(row_calls == 0 && state_calls == 8 && output_calls == 0,
          "mask admission before row transfer");
    check(state_elements == 1024 && row_elements == 0, "mask-only work budget");
    guards(output, 64, 2, 0);

    reset("one sparse occurrence", output, 64);
    states[1000] = (jbyte) 128;
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 0, (jintArray) &o, 2, 32), 1024, 1);
    check(output[2] == rows[1000], "sparse value");
    check(row_calls == 1 && row_elements == 1 && output_elements == 1,
          "one element transferred, not rejected prefix or suffix");
    guards(output, 64, 2, 1);

    reset("contradictory masks", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 128, (jintArray) &o, 2, 32), 1024, 0);
    check(row_calls == 0 && state_calls == 0 && output_calls == 0, "constant mask shortcut");

    reset("all pass tail", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 1000, 0, 0, (jintArray) &o, 2, 32), 1024, 24);
    check(row_elements == 24 && state_calls == 0 && output_elements == 24, "bulk tail transfer");
    for (int i = 0; i < 24; ++i) check(output[i + 2] == rows[i + 1000], "bulk order");
    guards(output, 64, 2, 24);

    memset(states, 0, sizeof(states));
    states[124] = (jbyte) 128;
    reset("bounded rejected prefix", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 128, 0, (jintArray) &o, 2, 16), 128, 1);
    check(output[2] == rows[124] && row_elements == 1 && state_calls == 1,
          "minimum admitted single row region");
    guards(output, 64, 2, 1);

    memset(states, 0, sizeof(states));
    states[20] = states[120] = (jbyte) 128;
    reset("two matches one span", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 128, 0, (jintArray) &o, 2, 16), 128, 2);
    check(output[2] == rows[20] && output[3] == rows[120], "stable sparse values");
    check(row_calls == 1 && row_elements == 101 && state_calls == 1,
          "minimal enclosing row span");

    memset(states, 0, sizeof(states));
    states[10] = states[20] = states[30] = (jbyte) 128;
    reset("early limit", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 128, 0, (jintArray) &o, 2, 2), 21, 2);
    check(output[2] == rows[10] && output[3] == rows[20], "early two values");
    check(row_elements == 11 && output_calls == 1 && output_elements == 2,
          "early stop excludes unexamined suffix");

    memset(states, 0, sizeof(states));
    states[7] = states[129] = states[255] = (jbyte) 128;
    reset("two chunks", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 256, 128, 0, (jintArray) &o, 2, 3), 256, 3);
    check(output[2] == rows[7] && output[3] == rows[129] && output[4] == rows[255],
          "cross-chunk occurrence order");
    check(state_calls == 2 && row_calls == 2 && row_elements == 128,
          "two bounded JNI regions with only admitted enclosing spans");

    reset("zero limit", output, 64);
    cursor(Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 47, 120, 128, 0, (jintArray) &o, 2, 0), 47, 0);
    check(state_calls == 0 && row_calls == 0 && output_calls == 0, "zero limit no region traffic");

    reset("aliased source/output rejected", output, 64);
    check(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 128, 0, (jintArray) &r, 2, 1) == -1,
          "alias refusal code");
    check(thrown == 1 && row_calls == 0 && output_calls == 0, "alias refusal before transfer");

    reset("invalid mask rejected", output, 64);
    check(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 256, 0, (jintArray) &o, 2, 1) == -1,
          "unsigned byte admission limit");
    check(thrown == 1 && state_calls == 0 && output_calls == 0, "invalid mask cannot mutate");

    reset("invalid output rejected", output, 64);
    check(Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env, NULL,
            (jintArray) &r, (jbyteArray) &s, 0, 0, 0, (jintArray) &o, 2, 64) == -1,
          "bounded output capacity check");
    check(thrown == 1 && row_calls == 0, "invalid output no transfer");
    guards(output, 64, 2, 0);

    printf("M3 JNI CURRENT REGION PASS checks=%d\n", checks);
    return 0;
}
