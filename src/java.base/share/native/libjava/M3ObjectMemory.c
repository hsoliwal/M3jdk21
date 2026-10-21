/* Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 */
/* java.base's libjava JVMTI diagnostic: no oop pointer escapes; no JVMTI callbacks issue JNI calls. */
#include <jni.h>
#include <jvmti.h>
#include <stdint.h>
#include <string.h>

#define M3_SIZE_CAPABLE 1
#define M3_TAG_CAPABLE 2

static jvmtiEnv *m3_ti;
static jlong next_id = 1;

static void fail(JNIEnv *env, const char *klass, const char *message) {
    jclass error = (*env)->FindClass(env, klass);
    if (error != NULL) {
        (*env)->ThrowNew(env, error, message);
        (*env)->DeleteLocalRef(env, error);
    }
}

/* Called once by class initialization. No JNI_OnLoad collision with libjava. */
JNIEXPORT jint JNICALL Java_jdk_internal_misc_M3ObjectMemory_initialize0(
        JNIEnv *env, jclass ignored) {
    (void)ignored;
    JavaVM *vm = NULL;
    if ((*env)->GetJavaVM(env, &vm) != JNI_OK) return 0;
    if ((*vm)->GetEnv(vm, (void **)&m3_ti, JVMTI_VERSION_1_2) != JNI_OK
            || m3_ti == NULL) return 0;
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_tag_objects = 1;
    jint result = M3_SIZE_CAPABLE;
    if ((*m3_ti)->AddCapabilities(m3_ti, &capabilities) == JVMTI_ERROR_NONE)
        result |= M3_TAG_CAPABLE;
    return result;
}

JNIEXPORT jlong JNICALL Java_jdk_internal_misc_M3ObjectMemory_size0(
        JNIEnv *env, jclass ignored, jobject object) {
    (void)ignored;
    if (m3_ti == NULL || object == NULL) {
        fail(env, "java/lang/IllegalStateException", "unavailable M3 JVM object size");
        return -1;
    }
    jlong bytes = -1;
    if ((*m3_ti)->GetObjectSize(m3_ti, object, &bytes) != JVMTI_ERROR_NONE
            || bytes < 0) {
        fail(env, "java/lang/IllegalStateException", "JVMTI GetObjectSize failed");
        return -1;
    }
    return bytes;
}

/* Caller holds the M3ObjectMemory class monitor; never wrap a live tag identity. */
static jlong tagged(JNIEnv *env, jobject object) {
    jlong tag = 0;
    if ((*m3_ti)->GetTag(m3_ti, object, &tag) != JVMTI_ERROR_NONE) {
        fail(env, "java/lang/IllegalStateException", "JVMTI GetTag failed");
        return 0;
    }
    if (tag > 0) return tag;
    if (tag < 0 || next_id <= 0 || next_id == INT64_MAX) {
        fail(env, "java/lang/IllegalStateException", "M3 object tag exhausted");
        return 0;
    }
    tag = next_id++;
    if ((*m3_ti)->SetTag(m3_ti, object, tag) != JVMTI_ERROR_NONE) {
        fail(env, "java/lang/IllegalStateException", "JVMTI SetTag failed");
        return 0;
    }
    return tag;
}

JNIEXPORT jlong JNICALL Java_jdk_internal_misc_M3ObjectMemory_id0(
        JNIEnv *env, jclass ignored, jobject object) {
    (void)ignored;
    return tagged(env, object);
}

typedef struct {
    jlong identity;
    jlong all;
    jlong heap;
    jlong roots;
    int overflow;
} m3_edges;

static jint JNICALL count_ref(jvmtiHeapReferenceKind kind,
        const jvmtiHeapReferenceInfo *info, jlong class_tag,
        jlong referrer_class_tag, jlong size, jlong *tag,
        jlong *referrer_tag, jint array_length, void *user_data) {
    (void)info;
    (void)class_tag;
    (void)referrer_class_tag;
    (void)size;
    (void)referrer_tag;
    (void)array_length;
    m3_edges *counts = (m3_edges *)user_data;
    if (tag != NULL && *tag == counts->identity) {
        if (counts->all == INT64_MAX) {
            counts->overflow = 1;
            return JVMTI_VISIT_ABORT;
        }
        counts->all++;
        if (kind >= JVMTI_HEAP_REFERENCE_CLASS
                && kind <= JVMTI_HEAP_REFERENCE_SUPERCLASS)
            counts->heap++;
        else counts->roots++;
    }
    return JVMTI_VISIT_OBJECTS;
}

JNIEXPORT jlongArray JNICALL Java_jdk_internal_misc_M3ObjectMemory_references0(
        JNIEnv *env, jclass ignored, jobject object) {
    (void)ignored;
    if (m3_ti == NULL || object == NULL) {
        fail(env, "java/lang/IllegalStateException", "JVMTI traversal unavailable");
        return NULL;
    }
    m3_edges counts = {0};
    counts.identity = tagged(env, object);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jvmtiHeapCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.heap_reference_callback = count_ref;
    if ((*m3_ti)->FollowReferences(m3_ti, 0, NULL, NULL,
                    &callbacks, &counts) != JVMTI_ERROR_NONE || counts.overflow) {
        fail(env, "java/lang/IllegalStateException", "JVMTI FollowReferences failed");
        return NULL;
    }
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result == NULL) return NULL;
    jlong values[3] = {counts.all, counts.heap, counts.roots};
    (*env)->SetLongArrayRegion(env, result, 0, 3, values);
    return result;
}
