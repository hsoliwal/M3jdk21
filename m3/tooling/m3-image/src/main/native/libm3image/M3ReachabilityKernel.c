/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
#include "jdk_m3_tooling_image_M3JniReachabilityKernel.h"

#include <stdint.h>
#include <stdlib.h>

static void m3_throw(JNIEnv *env, const char *class_name, const char *message) {
    jclass exception_class = (*env)->FindClass(env, class_name);
    if (exception_class != NULL) {
        (void)(*env)->ThrowNew(env, exception_class, message);
        (*env)->DeleteLocalRef(env, exception_class);
    }
}

static int m3_mark(uint64_t *visited, jint vertex) {
    size_t word = ((size_t)(uint32_t)vertex) >> 6;
    uint64_t mask = UINT64_C(1) << (((uint32_t)vertex) & UINT32_C(63));
    uint64_t before = visited[word];
    visited[word] = before | mask;
    return (before & mask) == 0;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_tooling_image_M3JniReachabilityKernel_close0(
        JNIEnv *env, jclass type, jint vertex_count, jintArray offsets_array,
        jintArray edges_array, jintArray roots_array) {
    (void)type;
    jint *offsets = NULL;
    jint *edges = NULL;
    jint *roots = NULL;
    jint *queue = NULL;
    uint64_t *visited = NULL;
    jintArray result = NULL;

    if (vertex_count < 0 || vertex_count == INT32_MAX
            || offsets_array == NULL || edges_array == NULL || roots_array == NULL) {
        m3_throw(env, "java/lang/IllegalArgumentException", "Invalid JNI reachability arguments");
        return NULL;
    }

    jsize offsets_length = (*env)->GetArrayLength(env, offsets_array);
    jsize edges_length = (*env)->GetArrayLength(env, edges_array);
    jsize roots_length = (*env)->GetArrayLength(env, roots_array);
    if (offsets_length != vertex_count + 1) {
        m3_throw(env, "java/lang/IllegalArgumentException", "CSR offsets length mismatch");
        return NULL;
    }

    offsets = (*env)->GetIntArrayElements(env, offsets_array, NULL);
    if (edges_length > 0) {
        edges = (*env)->GetIntArrayElements(env, edges_array, NULL);
    }
    if (roots_length > 0) {
        roots = (*env)->GetIntArrayElements(env, roots_array, NULL);
    }
    if (offsets == NULL || (edges_length > 0 && edges == NULL)
            || (roots_length > 0 && roots == NULL) || (*env)->ExceptionCheck(env)) {
        if (!(*env)->ExceptionCheck(env)) {
            m3_throw(env, "java/lang/OutOfMemoryError", "Cannot access JNI reachability arrays");
        }
        goto cleanup;
    }

    if (offsets[0] != 0 || offsets[vertex_count] != edges_length) {
        m3_throw(env, "java/lang/IllegalArgumentException", "Invalid CSR boundary offsets");
        goto cleanup;
    }
    for (jint vertex = 0; vertex < vertex_count; vertex++) {
        if (offsets[vertex] > offsets[vertex + 1]) {
            m3_throw(env, "java/lang/IllegalArgumentException", "CSR offsets are not monotone");
            goto cleanup;
        }
        jint previous = -1;
        for (jint edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
            jint target = edges[edge];
            if (target < 0 || target >= vertex_count || target <= previous) {
                m3_throw(env, "java/lang/IllegalArgumentException",
                         "CSR edges must be in-range, sorted, and unique");
                goto cleanup;
            }
            previous = target;
        }
    }
    jint previous_root = -1;
    for (jsize index = 0; index < roots_length; index++) {
        jint root = roots[index];
        if (root < 0 || root >= vertex_count || root <= previous_root) {
            m3_throw(env, "java/lang/IllegalArgumentException",
                     "Roots must be in-range, sorted, and unique");
            goto cleanup;
        }
        previous_root = root;
    }

    size_t queue_length = vertex_count == 0 ? 1U : (size_t)(uint32_t)vertex_count;
    size_t visited_words = vertex_count == 0 ? 1U : (((size_t)(uint32_t)vertex_count + 63U) >> 6);
    queue = (jint *)malloc(queue_length * sizeof(jint));
    visited = (uint64_t *)calloc(visited_words, sizeof(uint64_t));
    if (queue == NULL || visited == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "Cannot allocate JNI reachability workspace");
        goto cleanup;
    }

    jsize head = 0;
    jsize tail = 0;
    for (jsize index = 0; index < roots_length; index++) {
        jint root = roots[index];
        if (m3_mark(visited, root)) {
            queue[tail++] = root;
        }
    }
    while (head < tail) {
        jint vertex = queue[head++];
        for (jint edge = offsets[vertex]; edge < offsets[vertex + 1]; edge++) {
            jint target = edges[edge];
            if (m3_mark(visited, target)) {
                queue[tail++] = target;
            }
        }
    }

    jsize output = 0;
    for (jint vertex = 0; vertex < vertex_count; vertex++) {
        size_t word = ((size_t)(uint32_t)vertex) >> 6;
        uint64_t mask = UINT64_C(1) << (((uint32_t)vertex) & UINT32_C(63));
        if ((visited[word] & mask) != 0) {
            queue[output++] = vertex;
        }
    }
    result = (*env)->NewIntArray(env, output);
    if (result != NULL && output > 0) {
        (*env)->SetIntArrayRegion(env, result, 0, output, queue);
    }

cleanup:
    free(visited);
    free(queue);
    if (roots != NULL) {
        (*env)->ReleaseIntArrayElements(env, roots_array, roots, JNI_ABORT);
    }
    if (edges != NULL) {
        (*env)->ReleaseIntArrayElements(env, edges_array, edges, JNI_ABORT);
    }
    if (offsets != NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets_array, offsets, JNI_ABORT);
    }
    return result;
}
