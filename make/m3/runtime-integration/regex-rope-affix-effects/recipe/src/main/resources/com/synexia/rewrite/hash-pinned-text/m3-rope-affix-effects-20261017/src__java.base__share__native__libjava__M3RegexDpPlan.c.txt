/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
#include "jdk_internal_mindex_M3RegexDpPlan.h"
#include <stdint.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (void)(*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

/* The caller proves the input is nonzero before entering this replaceable atom. */
static unsigned first_bit(uint64_t word) {
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_REGEX_PORTABLE)
    return (unsigned)__builtin_ctzll(word);
#else
    unsigned bit = 0;
    while ((word & UINT64_C(1)) == 0) { word >>= 1; bit++; }
    return bit;
#endif
}

#ifndef M3_REGEX_CUDA_PROVIDER

/* Target-owned ordered Boolean relation composition. No canonical text crosses JNI.
 * All lanes are validated before output is written; the local result permits aliases. */
static int compose_rows(const uint64_t *a, const uint64_t *b, uint64_t *out, size_t n) {
    uint64_t result[64];
    if (a == NULL || b == NULL || out == NULL || n == 0 || n > 64) return -1;
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
    uint64_t ceiling = 0;
    for (size_t i = 0; i < n; i++) {
        if (((a[i] | b[i]) & ~mask) != 0) return -1;
        ceiling |= b[i];
    }
    /* Validation above precedes every shortcut, including zero output. */
    if (ceiling == 0) { memset(out, 0, n * sizeof(uint64_t)); return 0; }
    /* Only multi-state rows can repay proving a homogeneous left relation. */
    if ((a[0] & (a[0] - 1)) != 0) {
        size_t equal = 1;
        while (equal < n && a[equal] == a[0]) equal++;
        if (equal == n) {
            uint64_t frontier = a[0], value = frontier == mask ? ceiling : 0;
            while (frontier != 0 && value != ceiling) {
                value |= b[first_bit(frontier)];
                frontier &= frontier - 1;
            }
            for (size_t i = 0; i < n; i++) result[i] = value;
            memcpy(out, result, n * sizeof(uint64_t));
            return 0;
        }
    }
    for (size_t i = 0; i < n; i++) {
        uint64_t frontier = a[i], value = 0;
        while (frontier != 0 && value != ceiling) {
            unsigned bit = 0;
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_REGEX_PORTABLE)
            bit = (unsigned)__builtin_ctzll(frontier);
#else
            uint64_t scan = frontier;
            while ((scan & UINT64_C(1)) == 0) { scan >>= 1; bit++; }
#endif
            value |= b[bit];
            frontier &= frontier - 1;
        }
        result[i] = value;
    }
    memcpy(out, result, n * sizeof(uint64_t));
    return 0;
}

static int compose_structured_rows(const uint64_t *a, const uint64_t *b, uint64_t *out, size_t n) {
    uint64_t result[64];
    if (a == NULL || b == NULL || out == NULL || n == 0 || n > 64) return -1;
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
    uint64_t ceiling = 0;
    for (size_t i = 0; i < n; i++) {
        if (((a[i] | b[i]) & ~mask) != 0) return -1;
        ceiling |= b[i];
    }
    if (ceiling == 0) { memset(out, 0, n * sizeof(uint64_t)); return 0; }

    /* Stop this proof at the first counterexample; do not tax every generic row. */
    uint64_t productive = 0, previous = UINT64_MAX;
    int nested = 1;
    for (size_t i = 0; i < n; i++) {
        if (b[i] == 0) continue;
        if ((b[i] & ~previous) != 0) { nested = 0; break; }
        productive |= UINT64_C(1) << i;
        previous = b[i];
    }
    if (nested) {
        /* Nonempty rows descend by containment: the first selected row dominates. */
        for (size_t i = 0; i < n; i++) {
            uint64_t selected = a[i] & productive;
            result[i] = selected == 0 ? 0 : b[first_bit(selected)];
        }
    } else {
        /* The exact generic union traversal remains the fallback. */
        for (size_t i = 0; i < n; i++) {
            uint64_t frontier = a[i], value = 0;
            while (frontier != 0 && value != ceiling) {
                value |= b[first_bit(frontier)];
                frontier &= frontier - 1;
            }
            result[i] = value;
        }
    }
    memcpy(out, result, n * sizeof(uint64_t));
    return 0;
}

static jlongArray compose_jni(JNIEnv *env, jlongArray left, jlongArray right,
        int (*kernel)(const uint64_t*, const uint64_t*, uint64_t*, size_t)) {
    jlong transfer[64];
    uint64_t a[64], b[64];
    _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long width");
    if (left == NULL || right == NULL) {
        fail(env, "java/lang/NullPointerException", "null regex effect");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, left);
    if (n < 1 || n > 64 || n != (*env)->GetArrayLength(env, right)) {
        fail(env, "java/lang/IllegalArgumentException", "invalid regex effect dimensions");
        return NULL;
    }
    (*env)->GetLongArrayRegion(env, left, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    memcpy(a, transfer, (size_t)n * sizeof(uint64_t));
    (*env)->GetLongArrayRegion(env, right, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    memcpy(b, transfer, (size_t)n * sizeof(uint64_t));
    if (kernel(a, b, a, (size_t)n) != 0) {
        fail(env, "java/lang/IllegalArgumentException", "foreign regex effect state bits");
        return NULL;
    }
    memcpy(transfer, a, (size_t)n * sizeof(uint64_t));
    jlongArray output = (*env)->NewLongArray(env, n);
    if (output == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, output, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}
#endif /* !M3_REGEX_CUDA_PROVIDER */

/* Prepared effect batches own derived numeric copies only. Both backends consume the
 * same length/flags/ordinary/skip-leading-low layout. The optional provider compiles
 * this owner with distinct JNI symbols; it never replaces libjava's CPU functions. */
#ifdef M3_REGEX_CUDA_PROVIDER
#include "m3_regex_effects_cuda.h"
#define M3_BATCH_ABI_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchCudaAbi0
#define M3_BATCH_CREATE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchCudaCreate0
#define M3_BATCH_EVALUATE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchCudaEvaluate0
#define M3_BATCH_CLOSE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchCudaClose0
#define M3_BATCH_HANDLE_TAG UINT64_C(0x4d34000000000000)
#else
#define M3_BATCH_ABI_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchAbi0
#define M3_BATCH_CREATE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchCreate0
#define M3_BATCH_EVALUATE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchEvaluate0
#define M3_BATCH_CLOSE_FN Java_jdk_internal_mindex_M3RegexDpPlan_batchClose0
#define M3_BATCH_HANDLE_TAG UINT64_C(0x4d33000000000000)
#endif

#define M3_BATCH_MAX_IMAGE_BYTES (UINT64_C(1) << 30)
#define M3_BATCH_MAX_ROWS_PER_CALL 16384
#define M3_BATCH_MAX_EFFECTS_PER_CALL 65536
#define M3_BATCH_MAX_SEQUENCE UINT64_C(0x0000ffffffffffff)

typedef struct M3RegexBatch {
    uint64_t *lanes;
    uint64_t *dot_rows;
    jint *tuples;
    jint *offsets;
    size_t states, stride, effect_count;
    jint tuple_count, row_count;
    jint prefix, suffix;
    uint64_t initial, accepting;
    uint64_t seed, suffix_without_high, suffix_with_high, affix_length;
    int seed_high;
    uint64_t handle;
    size_t references;
    struct M3RegexBatch *next;
#ifdef M3_REGEX_CUDA_PROVIDER
    void *cuda;
#endif
} M3RegexBatch;

static void batch_prepare_context(M3RegexBatch *batch);

/* A class monitor protects only registry changes. No kernel, allocation, Java callback
 * or device synchronization runs under it. Opaque IDs are never native addresses and
 * are never reused. An in-flight lookup owns a lease even after raw JNI close retires it. */
static M3RegexBatch *batch_registry;
static uint64_t batch_next_sequence = 1;

static void batch_free(M3RegexBatch *batch) {
    if (batch == NULL) return;
#ifdef M3_REGEX_CUDA_PROVIDER
    if (batch->cuda != NULL) m3_regex_cuda_close(batch->cuda);
#endif
    free(batch->lanes);
    free(batch->dot_rows);
    free(batch->tuples);
    free(batch->offsets);
    free(batch);
}

static int batch_lock(JNIEnv *env, jclass cls) {
    return (*env)->MonitorEnter(env, cls) == JNI_OK;
}

static void batch_unlock(JNIEnv *env, jclass cls) {
    (void)(*env)->MonitorExit(env, cls);
}

static jlong batch_publish(JNIEnv *env, jclass cls, M3RegexBatch *batch) {
    if (!batch_lock(env, cls)) return 0;
    if (batch_next_sequence > M3_BATCH_MAX_SEQUENCE) {
        batch_unlock(env, cls);
        fail(env, "java/lang/IllegalStateException", "prepared regex handle space exhausted");
        return 0;
    }
    batch->handle = M3_BATCH_HANDLE_TAG | batch_next_sequence++;
    batch->references = 1;
    batch->next = batch_registry;
    batch_registry = batch;
    batch_unlock(env, cls);
    return (jlong)batch->handle;
}

static M3RegexBatch *batch_acquire(JNIEnv *env, jclass cls, jlong handle) {
    if (((uint64_t)handle & ~M3_BATCH_MAX_SEQUENCE) != M3_BATCH_HANDLE_TAG
            || ((uint64_t)handle & M3_BATCH_MAX_SEQUENCE) == 0) {
        fail(env, "java/lang/IllegalStateException", "invalid prepared regex handle");
        return NULL;
    }
    if (!batch_lock(env, cls)) return NULL;
    M3RegexBatch *batch = batch_registry;
    while (batch != NULL && batch->handle != (uint64_t)handle) batch = batch->next;
    if (batch != NULL && batch->references != SIZE_MAX) batch->references++;
    else batch = NULL;
    batch_unlock(env, cls);
    if (batch == NULL) fail(env, "java/lang/IllegalStateException", "closed prepared regex handle");
    return batch;
}

static void batch_release(JNIEnv *env, jclass cls, M3RegexBatch *batch) {
    if (!batch_lock(env, cls)) return;
    int dispose = --batch->references == 0;
    batch_unlock(env, cls);
    if (dispose) batch_free(batch);
}

/* Region copies never pin Java heap arrays or retain a JNI pointer. */
static int batch_copy_longs(JNIEnv *env, jlongArray source, uint64_t *target, jsize count) {
    jlong chunk[1024];
    _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long width");
    for (jsize at = 0; at < count;) {
        jsize n = count - at < 1024 ? count - at : 1024;
        (*env)->GetLongArrayRegion(env, source, at, n, chunk);
        if ((*env)->ExceptionCheck(env)) return 0;
        memcpy(target + at, chunk, (size_t)n * sizeof(uint64_t));
        at += n;
    }
    return 1;
}

static int batch_copy_ints(JNIEnv *env, jintArray source, jint *target, jsize count) {
    for (jsize at = 0; at < count;) {
        jsize n = count - at < 1024 ? count - at : 1024;
        (*env)->GetIntArrayRegion(env, source, at, n, target + at);
        if ((*env)->ExceptionCheck(env)) return 0;
        at += n;
    }
    return 1;
}

static int batch_validate(const M3RegexBatch *batch) {
    const uint64_t mask = batch->states == 64 ? UINT64_MAX
            : (UINT64_C(1) << batch->states) - 1;
    if (batch->initial == 0 || (batch->initial & ~mask) != 0
            || batch->accepting != (UINT64_C(1) << (batch->states - 1))) return 0;
    for (size_t state = 0; state < batch->states; state++) {
        if ((batch->dot_rows[state] & ~mask) != 0) return 0;
    }
    for (size_t atom = 0; atom < batch->effect_count; atom++) {
        const uint64_t *effect = batch->lanes + atom * batch->stride;
        uint64_t length = effect[0], flags = effect[1];
        if (length > INT64_MAX || flags > 3 || (length == 0 && flags != 0)
                || (length == 1 && flags == 3)) return 0;
        for (size_t state = 0; state < batch->states; state++) {
            uint64_t ordinary = effect[2 + state];
            uint64_t skipped = effect[2 + batch->states + state];
            if (((ordinary | skipped) & ~mask) != 0) return 0;
            if ((flags & 1) == 0 && ordinary != skipped) return 0;
            if (length == 0 && ordinary != (UINT64_C(1) << state)) return 0;
        }
    }
    if (batch->offsets[0] != 0 || batch->offsets[batch->row_count] != batch->tuple_count) return 0;
    uint64_t affix_length = 0;
    jint affixes[2] = { batch->prefix, batch->suffix };
    for (size_t i = 0; i < 2; i++) {
        jint atom = affixes[i];
        if (atom < -1 || (atom >= 0 && (size_t)atom >= batch->effect_count)) return 0;
        if (atom >= 0) {
            uint64_t length = batch->lanes[(size_t)atom * batch->stride];
            if (length > INT64_MAX - affix_length) return 0;
            affix_length += length;
        }
    }
    for (jint row = 0; row < batch->row_count; row++) {
        jint start = batch->offsets[row], end = batch->offsets[row + 1];
        if (start < 0 || end < start || end > batch->tuple_count
                || (int64_t)end - start > M3_BATCH_MAX_EFFECTS_PER_CALL) return 0;
        uint64_t length = affix_length;
        for (jint at = start; at < end; at++) {
            jint atom = batch->tuples[at];
            if (atom < 0 || (size_t)atom >= batch->effect_count) return 0;
            uint64_t add = batch->lanes[(size_t)atom * batch->stride];
            if (add > INT64_MAX - length) return 0;
            length += add;
        }
    }
    return 1;
}

JNIEXPORT jint JNICALL M3_BATCH_ABI_FN(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    return 1;
}

JNIEXPORT jlong JNICALL M3_BATCH_CREATE_FN(JNIEnv *env, jclass cls,
        jlongArray lanes, jlongArray dot_rows, jint states, jlong initial, jlong accepting,
        jintArray tuples, jintArray offsets, jint prefix, jint suffix, jlong max_image_bytes) {
    if (lanes == NULL || dot_rows == NULL || tuples == NULL || offsets == NULL) {
        fail(env, "java/lang/NullPointerException", "null prepared regex image");
        return 0;
    }
    if (states < 1 || states > 64 || max_image_bytes < 0
            || (uint64_t)max_image_bytes > M3_BATCH_MAX_IMAGE_BYTES) {
        fail(env, "java/lang/IllegalArgumentException", "invalid prepared regex image limit");
        return 0;
    }
    jsize lane_count = (*env)->GetArrayLength(env, lanes);
    jsize dot_count = (*env)->GetArrayLength(env, dot_rows);
    jsize tuple_count = (*env)->GetArrayLength(env, tuples);
    jsize offset_count = (*env)->GetArrayLength(env, offsets);
    size_t stride = 2 + 2 * (size_t)states;
    uint64_t image_bytes = ((uint64_t)lane_count + dot_count) * sizeof(uint64_t)
            + ((uint64_t)tuple_count + offset_count) * sizeof(jint);
    if (dot_count != states || (size_t)lane_count % stride != 0 || offset_count < 1
            || image_bytes > (uint64_t)max_image_bytes || image_bytes > SIZE_MAX) {
        fail(env, "java/lang/IllegalArgumentException", "invalid prepared regex image geometry");
        return 0;
    }
    M3RegexBatch *batch = (M3RegexBatch *)calloc(1, sizeof(M3RegexBatch));
    if (batch == NULL) {
        fail(env, "java/lang/OutOfMemoryError", "prepared regex owner allocation");
        return 0;
    }
    batch->states = (size_t)states;
    batch->stride = stride;
    batch->effect_count = (size_t)lane_count / stride;
    batch->tuple_count = tuple_count;
    batch->row_count = offset_count - 1;
    batch->prefix = prefix;
    batch->suffix = suffix;
    batch->initial = (uint64_t)initial;
    batch->accepting = (uint64_t)accepting;
    if (lane_count != 0) batch->lanes = (uint64_t *)malloc((size_t)lane_count * sizeof(uint64_t));
    batch->dot_rows = (uint64_t *)malloc((size_t)states * sizeof(uint64_t));
    if (tuple_count != 0) batch->tuples = (jint *)malloc((size_t)tuple_count * sizeof(jint));
    batch->offsets = (jint *)malloc((size_t)offset_count * sizeof(jint));
    if (batch->dot_rows == NULL || (lane_count != 0 && batch->lanes == NULL)
            || (tuple_count != 0 && batch->tuples == NULL)
            || batch->offsets == NULL) {
        batch_free(batch);
        fail(env, "java/lang/OutOfMemoryError", "prepared regex image allocation");
        return 0;
    }
    if (!batch_copy_longs(env, lanes, batch->lanes, lane_count)
            || !batch_copy_longs(env, dot_rows, batch->dot_rows, dot_count)
            || !batch_copy_ints(env, tuples, batch->tuples, tuple_count)
            || !batch_copy_ints(env, offsets, batch->offsets, offset_count)) {
        batch_free(batch);
        return 0;
    }
    if (!batch_validate(batch)) {
        batch_free(batch);
        fail(env, "java/lang/IllegalArgumentException", "invalid prepared regex effect or tuple");
        return 0;
    }
    batch_prepare_context(batch);
#ifdef M3_REGEX_CUDA_PROVIDER
    int status = m3_regex_cuda_prepare(batch->lanes, (size_t)lane_count,
            batch->dot_rows, batch->states, batch->seed, batch->seed_high,
            batch->suffix_without_high, batch->suffix_with_high, batch->affix_length,
            batch->tuples, (size_t)tuple_count, batch->offsets, (size_t)offset_count, &batch->cuda);
    if (status != M3_REGEX_CUDA_OK) {
        batch_free(batch);
        fail(env, status == M3_REGEX_CUDA_UNAVAILABLE ? "java/lang/UnsupportedOperationException"
                : status == M3_REGEX_CUDA_NOMEM ? "java/lang/OutOfMemoryError"
                : "java/lang/IllegalStateException", "prepared regex CUDA prepare failed");
        return 0;
    }
#endif
    jlong handle = batch_publish(env, cls, batch);
    if (handle == 0) batch_free(batch);
    return handle;
}

static uint64_t batch_apply(const uint64_t *rows, uint64_t frontier) {
    uint64_t result = 0;
    while (frontier != 0) {
        result |= rows[first_bit(frontier)];
        frontier &= frontier - 1;
    }
    return result;
}

static void batch_step(const M3RegexBatch *batch, jint atom,
        uint64_t *frontier, int *pending_high) {
    if (atom < 0) return;
    const uint64_t *effect = batch->lanes + (size_t)atom * batch->stride;
    if (effect[0] == 0) return;
    const uint64_t *relation = effect + 2;
    if (*pending_high) {
        *frontier = batch_apply(batch->dot_rows, *frontier);
        if ((effect[1] & 1) != 0) relation += batch->states;
    }
    *frontier = batch_apply(relation, *frontier);
    *pending_high = (effect[1] & 2) != 0;
}

static uint64_t batch_preimage(const uint64_t *rows, size_t states, uint64_t targets) {
    uint64_t result = 0;
    for (size_t state = 0; state < states; state++) {
        if ((rows[state] & targets) != 0) result |= UINT64_C(1) << state;
    }
    return result;
}

/* Factor fixed context once. Reverse existential observations leave the complete
 * immutable effect image intact and include both incoming surrogate-boundary lanes. */
static void batch_prepare_context(M3RegexBatch *batch) {
    batch->seed = batch->initial;
    batch->seed_high = 0;
    batch_step(batch, batch->prefix, &batch->seed, &batch->seed_high);
    if (batch->prefix >= 0) {
        batch->affix_length = batch->lanes[(size_t)batch->prefix * batch->stride];
    }
    const uint64_t *suffix = batch->suffix < 0 ? NULL
            : batch->lanes + (size_t)batch->suffix * batch->stride;
    if (suffix != NULL) batch->affix_length += suffix[0];
    if (suffix == NULL || suffix[0] == 0) {
        batch->suffix_without_high = batch->accepting;
        batch->suffix_with_high = batch_preimage(batch->dot_rows, batch->states, batch->accepting);
    } else {
        uint64_t targets = (suffix[1] & 2) == 0 ? batch->accepting
                : batch_preimage(batch->dot_rows, batch->states, batch->accepting);
        const uint64_t *ordinary = suffix + 2;
        const uint64_t *skipped = (suffix[1] & 1) == 0 ? ordinary : ordinary + batch->states;
        batch->suffix_without_high = batch_preimage(ordinary, batch->states, targets);
        batch->suffix_with_high = batch_preimage(batch->dot_rows, batch->states,
                batch_preimage(skipped, batch->states, targets));
    }
}

#ifndef M3_REGEX_CUDA_PROVIDER
static jbyte batch_match_row(const M3RegexBatch *batch, jint row) {
    uint64_t frontier = batch->seed;
    int pending_high = batch->seed_high;
    for (jint at = batch->offsets[row]; at < batch->offsets[row + 1]; at++) {
        batch_step(batch, batch->tuples[at], &frontier, &pending_high);
    }
    uint64_t witness = pending_high ? batch->suffix_with_high : batch->suffix_without_high;
    return (frontier & witness) != 0 ? 1 : 0;
}
#endif

JNIEXPORT void JNICALL M3_BATCH_EVALUATE_FN(JNIEnv *env, jclass cls, jlong handle,
        jint start, jint end, jbyteArray answers, jint answer_offset) {
    if (answers == NULL) {
        fail(env, "java/lang/NullPointerException", "null prepared regex answers");
        return;
    }
    jsize answer_count = (*env)->GetArrayLength(env, answers);
    if (start < 0 || end < start || (int64_t)end - start > M3_BATCH_MAX_ROWS_PER_CALL
            || answer_offset < 0 || (int64_t)answer_offset + end - start > answer_count) {
        fail(env, "java/lang/IllegalArgumentException", "invalid prepared regex output range");
        return;
    }
    M3RegexBatch *batch = batch_acquire(env, cls, handle);
    if (batch == NULL) return;
    int valid = end <= batch->row_count;
    if (valid) {
        int64_t work = (int64_t)batch->offsets[end] - batch->offsets[start];
        valid = work <= M3_BATCH_MAX_EFFECTS_PER_CALL;
    }
    if (!valid) {
        batch_release(env, cls, batch);
        if (!(*env)->ExceptionCheck(env)) {
            fail(env, "java/lang/IllegalArgumentException", "unbounded prepared regex row range");
        }
        return;
    }
    jbyte output[M3_BATCH_MAX_ROWS_PER_CALL];
    int status = 0;
#ifdef M3_REGEX_CUDA_PROVIDER
    if (end != start) status = m3_regex_cuda_evaluate(batch->cuda, start, end, (int8_t *)output);
#else
    for (jint row = start; row < end; row++) output[row - start] = batch_match_row(batch, row);
#endif
    /* Release before publishing output or throwing: no registry JNI call sees a pending exception. */
    batch_release(env, cls, batch);
    if ((*env)->ExceptionCheck(env)) return;
    if (status != 0) {
        fail(env, "java/lang/IllegalStateException", "prepared regex CUDA execution failed");
        return;
    }
    (*env)->SetByteArrayRegion(env, answers, answer_offset, end - start, output);
}

JNIEXPORT void JNICALL M3_BATCH_CLOSE_FN(JNIEnv *env, jclass cls, jlong handle) {
    if (!batch_lock(env, cls)) return;
    M3RegexBatch **slot = &batch_registry;
    while (*slot != NULL && (*slot)->handle != (uint64_t)handle) slot = &(*slot)->next;
    M3RegexBatch *batch = *slot;
    int dispose = 0;
    if (batch != NULL) {
        *slot = batch->next;
        dispose = --batch->references == 0;
    }
    batch_unlock(env, cls);
    if (batch == NULL) fail(env, "java/lang/IllegalStateException", "closed prepared regex handle");
    else if (dispose) batch_free(batch);
}

#ifndef M3_REGEX_CUDA_PROVIDER
JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3RegexDpPlan_compose0(
        JNIEnv *env, jclass cls, jlongArray left, jlongArray right) {
    (void)cls;
    return compose_jni(env, left, right, compose_rows);
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3RegexDpPlan_composeStructured0(
        JNIEnv *env, jclass cls, jlongArray left, jlongArray right) {
    (void)cls;
    return compose_jni(env, left, right, compose_structured_rows);
}

/* Bounded power of an existing relation, reusing the exact composition atoms above.
 * Validate all input before even the count-zero identity; output may overlap input.
 * No callbacks, retained pointers, text admission or allocation in this C atom.
 * C accepts an unsigned count; the JNI boundary admits only nonnegative Java longs. */
static int repeat_rows(const uint64_t *rows, uint64_t *out, size_t n,
        uint64_t count, int structured) {
    uint64_t result[64], power[64];
    if (rows == NULL || out == NULL || n == 0 || n > 64) return -1;
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
    for (size_t i = 0; i < n; i++) {
        if ((rows[i] & ~mask) != 0) return -1;
        power[i] = rows[i];
        result[i] = UINT64_C(1) << i;
    }
    int (*compose)(const uint64_t*, const uint64_t*, uint64_t*, size_t) =
            structured ? compose_structured_rows : compose_rows;
    for (uint64_t remaining = count; remaining != 0; remaining >>= 1) {
        if ((remaining & UINT64_C(1)) != 0 && compose(result, power, result, n) != 0) return -1;
        if (remaining > 1 && compose(power, power, power, n) != 0) return -1;
    }
    memcpy(out, result, n * sizeof(uint64_t));
    return 0;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3RegexDpPlan_repeat0(
        JNIEnv *env, jclass cls, jlongArray rows, jlong count, jboolean structured) {
    jlong transfer[64];
    uint64_t bits[64];
    (void)cls;
    _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long width");
    if (rows == NULL) {
        fail(env, "java/lang/NullPointerException", "null regex effect");
        return NULL;
    }
    if (count < 0) {
        fail(env, "java/lang/IllegalArgumentException", "negative repeat count");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if (n < 1 || n > 64) {
        fail(env, "java/lang/IllegalArgumentException", "invalid regex effect dimensions");
        return NULL;
    }
    (*env)->GetLongArrayRegion(env, rows, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    memcpy(bits, transfer, (size_t)n * sizeof(uint64_t));
    if (repeat_rows(bits, bits, (size_t)n, (uint64_t)count, structured != JNI_FALSE) != 0) {
        fail(env, "java/lang/IllegalArgumentException", "foreign regex effect state bits");
        return NULL;
    }
    memcpy(transfer, bits, (size_t)n * sizeof(uint64_t));
    jlongArray output = (*env)->NewLongArray(env, n);
    if (output == NULL) return NULL;
    (*env)->SetLongArrayRegion(env, output, 0, n, transfer);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}
#endif /* !M3_REGEX_CUDA_PROVIDER */
