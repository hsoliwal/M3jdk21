/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <stdlib.h>

/* Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted from the existing Synexia packed-selection JNI acquisition guard.
 * A NULL acquisition may have no pending exception; report it after cleanup.
 */
static void m3_rank_acquisition_failed(JNIEnv *env) {
    if (!(*env)->ExceptionCheck(env)) {
        jclass error = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (error != NULL) {
            (void) (*env)->ThrowNew(env, error, "JNI array acquisition");
            (*env)->DeleteLocalRef(env, error);
        }
    }
}

/*
 * One JNI transition per batch, signed jlong order matching Java Long.compare.
 * Get/ReleaseArrayElements respects moving GC: no object addresses are retained.
 * Input arrays are released JNI_ABORT; only the preallocated output array is written.
 */
JNIEXPORT void JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_lowerBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || results == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count != (*env)->GetArrayLength(env, results)) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "result length mismatch");
        return;
    }
    if (count == 0) return;

    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        m3_rank_acquisition_failed(env);
        return;
    }
    jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        m3_rank_acquisition_failed(env);
        return;
    }
    jint *positions = (*env)->GetIntArrayElements(env, results, NULL);
    if (positions == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        m3_rank_acquisition_failed(env);
        return;
    }

    for (jsize i = 0; i < count; i++) {
        jlong needle = targets[i];
        jsize lo = 0;
        jsize hi = size;
        while (lo < hi) {
            jsize mid = lo + ((hi - lo) >> 1);
            if (values[mid] < needle) lo = mid + 1;
            else hi = mid;
        }
        positions[i] = (jint) lo;
    }
    (*env)->ReleaseIntArrayElements(env, results, positions, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}

/*
 * LeetCode 34 / 35 batch primitive: [lower0,upper0,...] in one JNI transition.
 * The upper-bound search starts at the lower bound (never left of it).
 * No JNI callbacks or Java object allocations in the search loop.
 * Moving-GC-safe Get/ReleaseArrayElements; inputs always JNI_ABORT.
 */
/*
 * The test-only legacy build deliberately omits new symbols while preserving
 * the original lowerBounds0 export. It verifies safe Java fallback against
 * already-installed older libm3rankbatch binaries, without a duplicate C owner.
 */
#if !defined(M3_RANK_LEGACY_ABI_TEST)
JNIEXPORT jint JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_capabilities0(
        JNIEnv *env, jclass ignored) {
    (void) env;
    (void) ignored;
    return 1; /* Fused lower/upper-bound pairs supported. */
}

JNIEXPORT void JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_lowerUpperBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray pairs) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || pairs == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null pair argument");
        return;
    }
    const jsize size = (*env)->GetArrayLength(env, sorted);
    const jsize count = (*env)->GetArrayLength(env, queries);
    const jsize output_size = (*env)->GetArrayLength(env, pairs);
    if ((jlong) count * 2 != (jlong) output_size) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "pair lane length mismatch");
        return;
    }
    if (count == 0) return;

    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        m3_rank_acquisition_failed(env);
        return;
    }
    jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        m3_rank_acquisition_failed(env);
        return;
    }
    jint *out = (*env)->GetIntArrayElements(env, pairs, NULL);
    if (out == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        m3_rank_acquisition_failed(env);
        return;
    }

    for (jsize i = 0; i < count; ++i) {
        const jlong target = targets[i];
        jsize lo = 0, hi = size;
        while (lo < hi) {
            const jsize mid = lo + ((hi - lo) >> 1);
            if (values[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        const jsize lower = lo;
        hi = size;
        while (lo < hi) {
            const jsize mid = lo + ((hi - lo) >> 1);
            if (values[mid] <= target) lo = mid + 1;
            else hi = mid;
        }
        const size_t offset = (size_t) i * 2;
        out[offset] = (jint) lower;
        out[offset + 1] = (jint) lo;
    }

    (*env)->ReleaseIntArrayElements(env, pairs, out, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}
#endif /* !M3_RANK_LEGACY_ABI_TEST */

/*
 * LC #347: frequency-top-K from a signed-long sorted snapshot. Follows the
 * existing M3SignalTopK worst-first heap pattern, but counts consecutive
 * primitive value runs. No Java callbacks, pinned heap addresses, per-entry
 * objects, or copied full payloads. One JNI call, bounded 256-long pages.
 */
static int m3_topk_worse(jint count_a, jlong key_a, jint count_b, jlong key_b) {
    return count_a < count_b || (count_a == count_b && key_a > key_b);
}

static void m3_topk_swap(jlong *keys, jint *frequencies, jsize a, jsize b) {
    jlong key = keys[a]; keys[a] = keys[b]; keys[b] = key;
    jint count = frequencies[a]; frequencies[a] = frequencies[b]; frequencies[b] = count;
}

static void m3_topk_sift_down(jlong *keys, jint *frequencies, jsize at, jsize size) {
    while (at < size / 2) {
        jsize child = at * 2 + 1;
        if (child >= size) break;
        if (child + 1 < size && m3_topk_worse(
                frequencies[child + 1], keys[child + 1],
                frequencies[child], keys[child])) child++;
        if (!m3_topk_worse(frequencies[child], keys[child],
                frequencies[at], keys[at])) break;
        m3_topk_swap(keys, frequencies, child, at);
        at = child;
    }
}

static void m3_topk_offer(jlong *keys, jint *frequencies, jsize capacity,
        jsize *size, jlong key, jint count) {
    if (*size < capacity) {
        jsize at = (*size)++;
        keys[at] = key;
        frequencies[at] = count;
        while (at > 0) {
            jsize parent = (at - 1) / 2;
            if (!m3_topk_worse(frequencies[at], keys[at],
                    frequencies[parent], keys[parent])) break;
            m3_topk_swap(keys, frequencies, at, parent);
            at = parent;
        }
    } else if (m3_topk_worse(frequencies[0], keys[0], count, key)) {
        keys[0] = key;
        frequencies[0] = count;
        m3_topk_sift_down(keys, frequencies, 0, *size);
    }
}

static void m3_topk_throw(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_topKFrequent0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jint k) {
    (void) ignored;
    if (sorted == NULL) {
        m3_topk_throw(env, "java/lang/NullPointerException", "null sorted rank input");
        return NULL;
    }
    if (k < 0) {
        m3_topk_throw(env, "java/lang/IllegalArgumentException",
                "negative frequency result limit");
        return NULL;
    }
    const jsize length = (*env)->GetArrayLength(env, sorted);
    if (length == 0 || k == 0) return (*env)->NewLongArray(env, 0);
    const jsize capacity = k < length ? k : length;
    if ((size_t) capacity > SIZE_MAX / sizeof(jlong)
            || (size_t) capacity > SIZE_MAX / sizeof(jint)) {
        m3_topk_throw(env, "java/lang/OutOfMemoryError",
                "top-K primitive heap size overflow");
        return NULL;
    }
    jlong *keys = malloc((size_t) capacity * sizeof(jlong));
    jint *frequencies = malloc((size_t) capacity * sizeof(jint));
    if (keys == NULL || frequencies == NULL) {
        free(keys);
        free(frequencies);
        m3_topk_throw(env, "java/lang/OutOfMemoryError",
                "top-K primitive heap allocation");
        return NULL;
    }
    enum { M3_TOPK_PAGE = 256 };
    jlong page[M3_TOPK_PAGE];
    jlong previous = 0;
    jint run_count = 0;
    jsize heap_size = 0;
    for (jsize offset = 0; offset < length;) {
        jsize count = length - offset;
        if (count > M3_TOPK_PAGE) count = M3_TOPK_PAGE;
        (*env)->GetLongArrayRegion(env, sorted, offset, count, page);
        if ((*env)->ExceptionCheck(env)) {
            free(keys);
            free(frequencies);
            return NULL;
        }
        for (jsize i = 0; i < count; i++) {
            jlong value = page[i];
            if (run_count == 0) {
                previous = value;
                run_count = 1;
            } else if (value == previous) {
                run_count++;
            } else {
                m3_topk_offer(keys, frequencies, capacity, &heap_size,
                        previous, run_count);
                previous = value;
                run_count = 1;
            }
        }
        offset += count;
    }
    m3_topk_offer(keys, frequencies, capacity, &heap_size, previous, run_count);
    /* Heap sort: extract worst to the tail, leaving the best keys first. */
    for (jsize end = heap_size - 1; end > 0; end--) {
        m3_topk_swap(keys, frequencies, 0, end);
        m3_topk_sift_down(keys, frequencies, 0, end);
    }
    jlongArray output = (*env)->NewLongArray(env, heap_size);
    if (output != NULL) (*env)->SetLongArrayRegion(env, output, 0, heap_size, keys);
    free(keys);
    free(frequencies);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}
