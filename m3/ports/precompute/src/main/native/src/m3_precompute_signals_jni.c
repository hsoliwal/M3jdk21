/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

#include "m3_precompute_signals.h"

static void throw_bad(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_precompute_M3CodeTextSignalBatchNative_nativeAnalyzeRange(
        JNIEnv *env,
        jclass type,
        jcharArray units_array,
        jintArray offsets_array,
        jintArray lengths_array,
        jint from_row,
        jint to_row) {
    (void)type;
    if (units_array == NULL || offsets_array == NULL || lengths_array == NULL) {
        throw_bad(env, "null M3 precompute input");
        return NULL;
    }

    jsize rows = (*env)->GetArrayLength(env, offsets_array);
    if ((*env)->GetArrayLength(env, lengths_array) != rows
            || from_row < 0
            || to_row < from_row
            || to_row > rows) {
        throw_bad(env, "invalid M3 precompute row range");
        return NULL;
    }

    jsize count = to_row - from_row;
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result == NULL || count == 0) return result;

    jint *offsets = (jint *)malloc((size_t)count * sizeof(jint));
    jint *lengths = (jint *)malloc((size_t)count * sizeof(jint));
    jlong *values = (jlong *)malloc((size_t)count * sizeof(jlong));
    if (offsets == NULL || lengths == NULL || values == NULL) {
        free(offsets);
        free(lengths);
        free(values);
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) {
            (void)(*env)->ThrowNew(env, oom, "M3 precompute JNI scratch");
            (*env)->DeleteLocalRef(env, oom);
        }
        return NULL;
    }

    (*env)->GetIntArrayRegion(env, offsets_array, from_row, count, offsets);
    if ((*env)->ExceptionCheck(env)) goto failure;
    (*env)->GetIntArrayRegion(env, lengths_array, from_row, count, lengths);
    if ((*env)->ExceptionCheck(env)) goto failure;

    jsize unit_count = (*env)->GetArrayLength(env, units_array);
    jint maximum = 0;
    for (jsize row = 0; row < count; row++) {
        if (offsets[row] < 0
                || lengths[row] < 0
                || (int64_t)offsets[row] + (int64_t)lengths[row] > unit_count) {
            throw_bad(env, "M3 precompute row outside packed image");
            goto failure;
        }
        if (lengths[row] > maximum) maximum = lengths[row];
    }

    jchar *scratch =
            maximum == 0 ? NULL : (jchar *)malloc((size_t)maximum * sizeof(jchar));
    if (maximum != 0 && scratch == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) {
            (void)(*env)->ThrowNew(env, oom, "M3 precompute JNI row scratch");
            (*env)->DeleteLocalRef(env, oom);
        }
        goto failure;
    }

    for (jsize row = 0; row < count; row++) {
        if (lengths[row] != 0) {
            (*env)->GetCharArrayRegion(
                    env, units_array, offsets[row], lengths[row], scratch);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                goto failure;
            }
        }
        values[row] =
                (jlong)m3_precompute_code_text_signal(
                        (const uint16_t *)scratch, (size_t)lengths[row]);
    }
    free(scratch);

    (*env)->SetLongArrayRegion(env, result, 0, count, values);

failure:
    free(offsets);
    free(lengths);
    free(values);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_precompute_M3SimilarityBatchNative_nativeScores(
        JNIEnv *env,
        jclass type,
        jint query_length,
        jlong query_hash,
        jintArray lengths_array,
        jlongArray hashes_array) {
    (void)type;
    if (query_length < 0 || lengths_array == NULL || hashes_array == NULL) {
        throw_bad(env, "invalid M3 similarity input");
        return NULL;
    }
    jsize count = (*env)->GetArrayLength(env, lengths_array);
    if ((*env)->GetArrayLength(env, hashes_array) != count) {
        throw_bad(env, "invalid M3 similarity geometry");
        return NULL;
    }

    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL || count == 0) return result;

    jint *lengths = (jint *)malloc((size_t)count * sizeof(jint));
    jlong *hashes = (jlong *)malloc((size_t)count * sizeof(jlong));
    jint *scores = (jint *)malloc((size_t)count * sizeof(jint));
    if (lengths == NULL || hashes == NULL || scores == NULL) {
        free(lengths);
        free(hashes);
        free(scores);
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) {
            (void)(*env)->ThrowNew(env, oom, "M3 similarity JNI scratch");
            (*env)->DeleteLocalRef(env, oom);
        }
        return NULL;
    }

    (*env)->GetIntArrayRegion(env, lengths_array, 0, count, lengths);
    if ((*env)->ExceptionCheck(env)) goto score_failure;
    (*env)->GetLongArrayRegion(env, hashes_array, 0, count, hashes);
    if ((*env)->ExceptionCheck(env)) goto score_failure;

    for (jsize row = 0; row < count; row++) {
        if (lengths[row] < 0) {
            throw_bad(env, "negative M3 similarity candidate length");
            goto score_failure;
        }
        scores[row] =
                (jint)m3_precompute_similarity_score(
                        (int)query_length,
                        (uint64_t)query_hash,
                        (int)lengths[row],
                        (uint64_t)hashes[row]);
    }

    (*env)->SetIntArrayRegion(env, result, 0, count, scores);

score_failure:
    free(lengths);
    free(hashes);
    free(scores);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
