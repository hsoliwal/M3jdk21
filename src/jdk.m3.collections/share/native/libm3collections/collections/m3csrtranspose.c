/* SPDX-License-Identifier: Apache-2.0
 * Original Synexia CSR row-major transpose, JNI bulk parity candidate.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-mat-collections/src/main/c/m3csrtranspose.c blob bc2135fedc57 (com.synexia.matcollections.MatCsrGraph -> jdk.internal.m3.collections.mat.MatCsrGraph, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "jdk_internal_m3_collections_mat_MatCsrGraph.h"

static void fail(JNIEnv *env, const char *name, const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type != NULL) {
        (void) (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jintArray JNICALL
Java_jdk_internal_m3_collections_mat_MatCsrGraph_transposeTargetsNative(
        JNIEnv *env, jclass ignored, jintArray offsets, jintArray targets,
        jintArray transposed) {
    (void) ignored;
    if (offsets == NULL || targets == NULL || transposed == NULL) {
        fail(env, "java/lang/NullPointerException", "MAT_CSR_NATIVE_NULL_INPUT");
        return NULL;
    }
    const jsize count = (*env)->GetArrayLength(env, offsets);
    const jsize transposeCount = (*env)->GetArrayLength(env, transposed);
    const jsize edges = (*env)->GetArrayLength(env, targets);
    if (count < 1 || count != transposeCount) {
        fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_SHAPE");
        return NULL;
    }
    const size_t nodes = (size_t) (count - 1);
    const size_t edgeCount = (size_t) edges;
    if (nodes > SIZE_MAX / sizeof(jint) - 1u
            || edgeCount > SIZE_MAX / sizeof(jint)) {
        fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_SIZE_OVERFLOW");
        return NULL;
    }
    jint *inOffsets = malloc(((size_t) count) * sizeof(jint));
    jint *outOffsets = malloc(((size_t) count) * sizeof(jint));
    jint *inTargets = malloc((edgeCount == 0 ? 1u : edgeCount) * sizeof(jint));
    jint *output = malloc((edgeCount == 0 ? 1u : edgeCount) * sizeof(jint));
    jint *cursor = malloc((nodes == 0 ? 1u : nodes) * sizeof(jint));
    if (inOffsets == NULL || outOffsets == NULL || inTargets == NULL
            || output == NULL || cursor == NULL) {
        free(inOffsets); free(outOffsets); free(inTargets); free(output); free(cursor);
        fail(env, "java/lang/OutOfMemoryError", "MAT_CSR_NATIVE_SCRATCH_ALLOCATION");
        return NULL;
    }
    (*env)->GetIntArrayRegion(env, offsets, 0, count, inOffsets);
    if (!(*env)->ExceptionCheck(env)) {
        (*env)->GetIntArrayRegion(env, transposed, 0, count, outOffsets);
    }
    if (!(*env)->ExceptionCheck(env) && edges != 0) {
        (*env)->GetIntArrayRegion(env, targets, 0, edges, inTargets);
    }
    if ((*env)->ExceptionCheck(env)) goto cleanup;

    if (inOffsets[0] != 0 || inOffsets[nodes] != edges
            || outOffsets[0] != 0 || outOffsets[nodes] != edges) {
        fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_OFFSET_ENDS");
        goto cleanup;
    }
    for (size_t i = 0; i < nodes; i++) {
        if (inOffsets[i] < 0 || inOffsets[i] > inOffsets[i + 1]
                || outOffsets[i] < 0 || outOffsets[i] > outOffsets[i + 1]
                || inOffsets[i + 1] > edges || outOffsets[i + 1] > edges) {
            fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_OFFSET_ORDER");
            goto cleanup;
        }
        cursor[i] = outOffsets[i];
    }
    for (size_t source = 0; source < nodes; source++) {
        for (jint edge = inOffsets[source]; edge < inOffsets[source + 1]; edge++) {
            jint target = inTargets[edge];
            if (target < 0 || (size_t) target >= nodes
                    || cursor[target] >= outOffsets[target + 1]) {
                fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_TARGET_BOUNDS");
                goto cleanup;
            }
            output[cursor[target]++] = (jint) source;
        }
    }
    for (size_t i = 0; i < nodes; i++) {
        if (cursor[i] != outOffsets[i + 1]) {
            fail(env, "java/lang/IllegalArgumentException", "MAT_CSR_NATIVE_TARGET_COUNTS");
            goto cleanup;
        }
    }
    {
        jintArray result = (*env)->NewIntArray(env, edges);
        if (result == NULL) goto cleanup;
        if (edges != 0) (*env)->SetIntArrayRegion(env, result, 0, edges, output);
        if (!(*env)->ExceptionCheck(env)) {
            free(inOffsets); free(outOffsets); free(inTargets); free(output); free(cursor);
            return result;
        }
        (*env)->DeleteLocalRef(env, result);
    }
cleanup:
    free(inOffsets); free(outOffsets); free(inTargets); free(output); free(cursor);
    return NULL;
}
