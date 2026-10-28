/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Fault injection for the actual native filename atom. Real JVM/OS proof is separate.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>
#include <jni.h>

static void *observed_malloc(size_t count);
static void observed_free(void *value);
#define malloc observed_malloc
#define free observed_free
#include "../m3_fixed_address_mapping_jni.c"
#undef malloc
#undef free

enum Fault { NONE, CLASS_ABSENT, METHOD_ABSENT, ENCODER_THROWS,
    ENCODER_THROWS_WITH_ARRAY, ENCODER_NULL, ALLOCATION_FAIL, REGION_THROWS };
static enum Fault fault;
static int pending;
static int utility_deleted;
static int bytes_deleted;
static int allocations;
static int releases;
static int throws;
static int checks;
static size_t requested;
static jsize byte_count;
static const unsigned char *encoded;
static const char *error_type;
static const uintptr_t utility_id = 1, bytes_id = 2, exception_id = 3, method_id = 4;

static void require(int condition, const char *message) {
    checks++;
    if (!condition) {
        fprintf(stderr, "native filename fault check %d failed: %s\n", checks, message);
        exit(1);
    }
}

static void *observed_malloc(size_t count) {
    requested = count;
    if (fault == ALLOCATION_FAIL) return NULL;
    void *value = malloc(count);
    require(value != NULL, "test allocation");
    allocations++;
    return value;
}

static void observed_free(void *value) {
    require(value != NULL, "non-null owned release");
    releases++;
    free(value);
}

static jclass JNICALL class_lookup(JNIEnv *env, const char *name) {
    (void)env;
    if (strcmp(name, "sun/nio/fs/Util") == 0) {
        if (fault == CLASS_ABSENT) { pending = 1; return NULL; }
        return (jclass)utility_id;
    }
    error_type = name;
    return (jclass)exception_id;
}

static jmethodID JNICALL method_lookup(JNIEnv *env, jclass owner,
        const char *name, const char *signature) {
    (void)env;
    require((uintptr_t)owner == utility_id, "existing JDK charset owner");
    require(strcmp(name, "toBytes") == 0, "existing encoder name");
    require(strcmp(signature, "(Ljava/lang/String;)[B") == 0, "exact encoder descriptor");
    if (fault == METHOD_ABSENT) { pending = 1; return NULL; }
    return (jmethodID)method_id;
}

static jobject JNICALL encode_call(JNIEnv *env, jclass owner, jmethodID method, ...) {
    (void)env;
    require((uintptr_t)owner == utility_id && (uintptr_t)method == method_id,
            "exact encoder invocation");
    if (fault == ENCODER_THROWS || fault == ENCODER_THROWS_WITH_ARRAY) pending = 1;
    if (fault == ENCODER_THROWS || fault == ENCODER_NULL) return NULL;
    return (jobject)bytes_id;
}

static jboolean JNICALL exception_check(JNIEnv *env) { (void)env; return (jboolean)pending; }

static void JNICALL release_local(JNIEnv *env, jobject ref) {
    (void)env;
    if ((uintptr_t)ref == utility_id) utility_deleted++;
    else if ((uintptr_t)ref == bytes_id) bytes_deleted++;
    else require(0, "unexpected local reference");
}

static jsize JNICALL array_length(JNIEnv *env, jarray array) {
    (void)env;
    require((uintptr_t)array == bytes_id, "exact encoded array length");
    return byte_count;
}

static void JNICALL array_region(JNIEnv *env, jbyteArray array,
        jsize begin, jsize count, jbyte *destination) {
    (void)env;
    require((uintptr_t)array == bytes_id && begin == 0 && count == byte_count,
            "exact encoded byte region");
    if (fault == REGION_THROWS) { pending = 1; return; }
    memcpy(destination, encoded, (size_t)count);
}

static jint JNICALL throw_new(JNIEnv *env, jclass klass, const char *message) {
    (void)env;
    require((uintptr_t)klass == exception_id && message != NULL, "expected failure delivery");
    require(!pending, "do not replace a pending JNI exception");
    pending = 1;
    throws++;
    return 0;
}

static const struct JNINativeInterface_ calls = {
    .FindClass = class_lookup, .GetStaticMethodID = method_lookup,
    .CallStaticObjectMethod = encode_call, .ExceptionCheck = exception_check,
    .DeleteLocalRef = release_local, .GetArrayLength = array_length,
    .GetByteArrayRegion = array_region, .ThrowNew = throw_new
};

static void reset(enum Fault next) {
    fault = next;
    pending = utility_deleted = bytes_deleted = allocations = releases = throws = 0;
    requested = 0;
    byte_count = 1;
    encoded = (const unsigned char *)"x";
    error_type = NULL;
}

static void failure(JNIEnv *env, enum Fault kind, int utility_refs, int array_refs,
        int native_allocations, const char *expected_type) {
    reset(kind);
    require(platform_path(env, (jstring)(uintptr_t)8) == NULL, "failed path never escapes");
    require(pending, "failure remains pending");
    require(utility_deleted == utility_refs && bytes_deleted == array_refs,
            "all encoder local references released");
    require(allocations == native_allocations && releases == native_allocations,
            "all native temporary buffers released");
    if (expected_type == NULL) require(throws == 0, "preserve original exception");
    else require(throws == 1 && strcmp(error_type, expected_type) == 0, "exact failure type");
}

int main(void) {
    JNIEnv environment = &calls;
    JNIEnv *env = &environment;
    failure(env, CLASS_ABSENT, 0, 0, 0, NULL);
    failure(env, METHOD_ABSENT, 1, 0, 0, NULL);
    failure(env, ENCODER_THROWS, 1, 0, 0, NULL);
    failure(env, ENCODER_THROWS_WITH_ARRAY, 1, 1, 0, NULL);
    failure(env, ENCODER_NULL, 1, 0, 0, "java/lang/IllegalStateException");
    failure(env, ALLOCATION_FAIL, 1, 1, 0, "java/lang/OutOfMemoryError");
    failure(env, REGION_THROWS, 1, 1, 1, NULL);

    reset(ALLOCATION_FAIL);
    byte_count = INT_MAX;
    require(platform_path(env, (jstring)(uintptr_t)8) == NULL, "large allocation fails closed");
    require(requested == (size_t)INT_MAX + 1u, "terminator size does not overflow signed int");
    require(utility_deleted == 1 && bytes_deleted == 1 && allocations == releases,
            "large allocation cleanup");

    reset(NONE);
    static const unsigned char nul_path[] = {'a', 0, 'b'};
    encoded = nul_path;
    byte_count = 3;
    require(platform_path(env, (jstring)(uintptr_t)8) == NULL, "NUL path refuses prefix open");
    require(strcmp(error_type, "java/io/IOException") == 0 && pending, "NUL refusal type");
    require(allocations == 1 && releases == 1 && utility_deleted == 1 && bytes_deleted == 1,
            "NUL refusal cleanup");

    reset(NONE);
    static const unsigned char rocket_path[] = {'x', '/', 0xf0, 0x9f, 0x9a, 0x80};
    encoded = rocket_path;
    byte_count = (jsize)sizeof(rocket_path);
    char *filename = platform_path(env, (jstring)(uintptr_t)8);
    require(filename != NULL && !pending, "successful filename projection");
    require(memcmp(filename, rocket_path, sizeof(rocket_path)) == 0
            && filename[sizeof(rocket_path)] == 0, "preserve standard UTF-8 and add terminator");
    require(utility_deleted == 1 && bytes_deleted == 1, "success reference cleanup");
    observed_free(filename);
    require(allocations == 1 && releases == 1, "caller releases successful projection");
    printf("M3_GVAS_NATIVE_PATH_FAULT_PASS cases=10 checks=%d mocked_jni=true\n", checks);
    return 0;
}
