/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * Test-only JVMTI observation. Every wrapper forwards the original JNI arguments
 * and return value exactly once. No text, array contents, Java references or JNI
 * pointers are retained. Agent-lifetime native code addresses are the only roots.
 */
#include <jni.h>
#include <jvmti.h>

#if defined(_WIN32)
/* The maintained jtreg is Linux-only. Keep other native-test builds compilable
 * without requiring C11 atomics/TLS from an unqualified Windows toolchain. */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    (void) vm; (void) options; (void) reserved;
    return JNI_ERR;
}
#else
#include <stdint.h>
#include <stdatomic.h>
#include <string.h>

typedef jlongArray (JNICALL *compose_function)(JNIEnv *, jclass, jlongArray, jlongArray);
typedef jlongArray (JNICALL *repeat_function)(JNIEnv *, jclass, jlongArray, jlong, jboolean);

_Static_assert(sizeof(compose_function) == sizeof(void *), "JVMTI function address width");
_Static_assert(sizeof(repeat_function) == sizeof(void *), "JVMTI function address width");

static _Atomic(compose_function) original_compose;
static _Atomic(compose_function) original_structured;
static _Atomic(repeat_function) original_repeat;
static atomic_int bound_mask;
static atomic_int binding_errors;
static int disable_forwarding;

#if defined(_MSC_VER)
#define M3_ROUTE_THREAD_LOCAL __declspec(thread)
#else
#define M3_ROUTE_THREAD_LOCAL _Thread_local
#endif
static M3_ROUTE_THREAD_LOCAL int armed;
static M3_ROUTE_THREAD_LOCAL jlong route_counts[3];

static void count_route(unsigned int index) {
    if (armed && route_counts[index] < INT64_MAX) ++route_counts[index];
}

/* This can only occur after an agent invariant failure; never call a null address. */
static jlongArray missing_original(JNIEnv *env) {
    jclass type = (*env)->FindClass(env, "java/lang/InternalError");
    if (type != NULL) {
        (void) (*env)->ThrowNew(env, type, "regex route probe lost original JNI binding");
        (*env)->DeleteLocalRef(env, type);
    }
    return NULL;
}

static jlongArray JNICALL counted_repeat(JNIEnv *env, jclass owner, jlongArray rows,
        jlong count, jboolean structured) {
    repeat_function original = atomic_load_explicit(&original_repeat, memory_order_acquire);
    if (original == NULL) return missing_original(env);
    count_route(0);
    return original(env, owner, rows, count, structured);
}

static jlongArray JNICALL counted_compose(JNIEnv *env, jclass owner,
        jlongArray left, jlongArray right) {
    compose_function original = atomic_load_explicit(&original_compose, memory_order_acquire);
    if (original == NULL) return missing_original(env);
    count_route(1);
    return original(env, owner, left, right);
}

static jlongArray JNICALL counted_structured(JNIEnv *env, jclass owner,
        jlongArray left, jlongArray right) {
    compose_function original = atomic_load_explicit(&original_structured, memory_order_acquire);
    if (original == NULL) return missing_original(env);
    count_route(2);
    return original(env, owner, left, right);
}

static void release_text(jvmtiEnv *jvmti, char *value) {
    if (value != NULL) (void) (*jvmti)->Deallocate(jvmti, (unsigned char *) value);
}

static void JNICALL native_bind(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
        jmethodID method, void *address, void **replacement) {
    char *name = NULL, *descriptor = NULL, *class_signature = NULL;
    jclass owner = NULL;
    jobject loader = NULL;
    int target = -1;
    (void) thread;
    if (env == NULL || address == NULL || replacement == NULL) return;
    if ((*jvmti)->GetMethodName(jvmti, method, &name, &descriptor, NULL) != JVMTI_ERROR_NONE) {
        goto cleanup;
    }
    if (strcmp(name, "repeat0") == 0 && strcmp(descriptor, "([JJZ)[J") == 0) target = 0;
    else if (strcmp(name, "compose0") == 0 && strcmp(descriptor, "([J[J)[J") == 0) target = 1;
    else if (strcmp(name, "composeStructured0") == 0
            && strcmp(descriptor, "([J[J)[J") == 0) target = 2;
    if (target < 0) goto cleanup;
    if ((*jvmti)->GetMethodDeclaringClass(jvmti, method, &owner) != JVMTI_ERROR_NONE
            || (*jvmti)->GetClassSignature(jvmti, owner, &class_signature, NULL)
                    != JVMTI_ERROR_NONE) {
        atomic_fetch_add_explicit(&binding_errors, 1, memory_order_relaxed);
        goto cleanup;
    }
    if (strcmp(class_signature, "Ljdk/internal/mindex/M3RegexDpPlan;") != 0) goto cleanup;
    if ((*jvmti)->GetClassLoader(jvmti, owner, &loader) != JVMTI_ERROR_NONE) {
        atomic_fetch_add_explicit(&binding_errors, 1, memory_order_relaxed);
        goto cleanup;
    }
    if (loader != NULL) goto cleanup;

    /* A disabled build is a probe-witness mutant, never a production fallback. */
    if (disable_forwarding) goto cleanup;
    if (target == 0) {
        repeat_function original, wrapper = counted_repeat;
        memcpy(&original, &address, sizeof(original));
        if (original == wrapper) goto cleanup;
        atomic_store_explicit(&original_repeat, original, memory_order_release);
        memcpy(replacement, &wrapper, sizeof(wrapper));
    } else {
        compose_function original;
        compose_function wrapper = target == 1 ? counted_compose : counted_structured;
        memcpy(&original, &address, sizeof(original));
        if (original == wrapper) goto cleanup;
        if (target == 1) atomic_store_explicit(&original_compose, original, memory_order_release);
        else atomic_store_explicit(&original_structured, original, memory_order_release);
        memcpy(replacement, &wrapper, sizeof(wrapper));
    }
    atomic_fetch_or_explicit(&bound_mask, 1 << target, memory_order_release);

cleanup:
    if (loader != NULL) (*env)->DeleteLocalRef(env, loader);
    if (owner != NULL) (*env)->DeleteLocalRef(env, owner);
    release_text(jvmti, name);
    release_text(jvmti, descriptor);
    release_text(jvmti, class_signature);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    jvmtiEnv *jvmti = NULL;
    jvmtiCapabilities capabilities;
    jvmtiEventCallbacks callbacks;
    (void) reserved;
    if (options != NULL && *options != '\0') {
        if (strcmp(options, "disable-forwarding") != 0) return JNI_ERR;
        disable_forwarding = 1;
    }
    if ((*vm)->GetEnv(vm, (void **) &jvmti, JVMTI_VERSION_1_2) != JNI_OK || jvmti == NULL) {
        return JNI_ERR;
    }
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_generate_native_method_bind_events = 1;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.NativeMethodBind = native_bind;
    if ((*jvmti)->AddCapabilities(jvmti, &capabilities) != JVMTI_ERROR_NONE
            || (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks))
                    != JVMTI_ERROR_NONE
            || (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                    JVMTI_EVENT_NATIVE_METHOD_BIND, NULL) != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL Java_M3RegexNativeRouteProbe_arm(JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    memset(route_counts, 0, sizeof(route_counts));
    armed = 1;
}

JNIEXPORT void JNICALL Java_M3RegexNativeRouteProbe_disarm(JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    armed = 0;
}

JNIEXPORT jlongArray JNICALL Java_M3RegexNativeRouteProbe_counts(JNIEnv *env, jclass owner) {
    jlongArray result;
    (void) owner;
    result = (*env)->NewLongArray(env, 3);
    if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 3, route_counts);
    return result;
}

JNIEXPORT jint JNICALL Java_M3RegexNativeRouteProbe_boundMask(JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    return atomic_load_explicit(&bound_mask, memory_order_acquire);
}

JNIEXPORT jint JNICALL Java_M3RegexNativeRouteProbe_bindingErrors(JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    return atomic_load_explicit(&binding_errors, memory_order_relaxed);
}
#endif
