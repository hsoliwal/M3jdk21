/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Compiled as both C17 and C++20 against the actual shared JNI helper. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "m3_jni_util.h"

static int lookup_fails, throw_fails, pending, finds, throws, deletes;
static char seen_type[128], seen_message[128];
#define REQUIRE(test) do { if (!(test)) { \
    fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #test); exit(1); \
} } while (0)
static jclass JNICALL find_type(JNIEnv *env, const char *name) {
    (void)env; finds++;
    snprintf(seen_type, sizeof(seen_type), "%s", name);
    if (lookup_fails) { pending = 2; return NULL; }
    return (jclass)(uintptr_t)1;
}
static jint JNICALL throw_type(JNIEnv *env, jclass type, const char *message) {
    (void)env; REQUIRE(type == (jclass)(uintptr_t)1); throws++;
    snprintf(seen_message, sizeof(seen_message), "%s", message);
    pending = throw_fails ? 3 : 1;
    return throw_fails ? JNI_ERR : JNI_OK;
}
static void JNICALL delete_ref(JNIEnv *env, jobject ref) {
    (void)env; REQUIRE(ref == (jobject)(uintptr_t)1); deletes++;
}
static void reset(void) {
    lookup_fails = throw_fails = pending = finds = throws = deletes = 0;
    seen_type[0] = seen_message[0] = '\0';
}
int main(void) {
    struct JNINativeInterface_ functions;
    memset(&functions, 0, sizeof(functions));
    functions.FindClass = find_type;
    functions.ThrowNew = throw_type;
    functions.DeleteLocalRef = delete_ref;
#ifdef __cplusplus
    JNIEnv env_value;
    env_value.functions = &functions;
#else
    JNIEnv env_value = &functions;
#endif
    reset();
    m3_jni_throw_by_name(&env_value, "java/io/IOException", "fixed address unavailable");
    REQUIRE(finds == 1 && throws == 1 && deletes == 1 && pending == 1);
    REQUIRE(strcmp(seen_type, "java/io/IOException") == 0);
    REQUIRE(strcmp(seen_message, "fixed address unavailable") == 0);
    puts("PASS throw-type-message-local-reference");
    reset(); lookup_fails = 1;
    m3_jni_throw_by_name(&env_value, "java/io/IOException", "fixed address unavailable");
    REQUIRE(finds == 1 && throws == 0 && deletes == 0 && pending == 2);
    puts("PASS class-lookup-failure-preserved");
    reset(); throw_fails = 1;
    m3_jni_throw_by_name(&env_value, "java/io/IOException", "fixed address unavailable");
    REQUIRE(finds == 1 && throws == 1 && deletes == 1 && pending == 3);
    puts("PASS throw-failure-reference-released");
    puts("HELPER_CHECKS=3");
    return 0;
}
