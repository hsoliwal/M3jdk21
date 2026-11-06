/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
//FLLDCIM: MRL - census P2-073, crates m3jdk21-lc719-pair-distance-jni-20261026 m3jdk21-lc315-count-smaller-native-20261026 m3jdk21-lc719-kth-pair-distance-jni-20261026: the crates' preimage include set (limits.h, stdint.h: INT_MAX, INT64_MAX, SIZE_MAX, uint64_t)
#include <limits.h>
#include <stdint.h>
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

//FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-pair-distance-jni-20261026 (donor blob 294447e6fe84): LC 719 pair-distance counts kernel
/* Original JNI m3rankbatch owner: LeetCode 719 sliding-window pair predicate.
 * A fixed two-page cache bounds scratch at 4096 bytes. No retained Java pointer.
 */
enum { M3_PAIR_DISTANCE_PAGE = 256 };

typedef struct m3_pair_distance_page {
    jlong data[M3_PAIR_DISTANCE_PAGE];
    jsize start;
    jsize length;
} m3_pair_distance_page;

static int m3_pair_distance_read(JNIEnv *env, jlongArray sorted, jsize size,
        jsize ordinal, m3_pair_distance_page *page, jlong *value) {
    if (ordinal < page->start || ordinal - page->start >= page->length) {
        jsize start = ordinal - ordinal % M3_PAIR_DISTANCE_PAGE;
        jsize length = size - start;
        if (length > M3_PAIR_DISTANCE_PAGE) length = M3_PAIR_DISTANCE_PAGE;
        (*env)->GetLongArrayRegion(env, sorted, start, length, page->data);
        if ((*env)->ExceptionCheck(env)) return 0;
        page->start = start;
        page->length = length;
    }
    *value = page->data[ordinal - page->start];
    return 1;
}

static void m3_pair_distance_throw(JNIEnv *env, const char *exception,
        const char *message) {
    jclass klass = (*env)->FindClass(env, exception);
    if (klass != NULL) (*env)->ThrowNew(env, klass, message);
}

static int m3_pair_distance_count(JNIEnv *env, jlongArray sorted, jsize size,
        jlong distance, m3_pair_distance_page *left_page,
        m3_pair_distance_page *right_page, jlong *result) {
    jsize left = 0;
    jlong total = 0;
    for (jsize right = 1; right < size; ++right) {
        jlong high;
        if (!m3_pair_distance_read(env, sorted, size, right, right_page, &high))
            return 0;
        while (left < right) {
            jlong low;
            if (!m3_pair_distance_read(env, sorted, size, left, left_page, &low))
                return 0;
            /* Unsigned subtraction represents full mathematical distance of
             * sorted signed longs, including Long.MIN_VALUE -> Long.MAX_VALUE.
             * Signed high-low would be undefined behavior in C on overflow.
             */
            if ((uint64_t) high - (uint64_t) low <= (uint64_t) distance) break;
            ++left;
        }
        /* n <= INT_MAX: n*(n-1)/2 always fits signed jlong. */
        total += (jlong) (right - left);
    }
    *result = total;
    return 1;
}

JNIEXPORT void JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_pairDistanceCounts0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray thresholds,
        jlongArray results) {
    (void) ignored;
    if (sorted == NULL || thresholds == NULL || results == NULL) {
        m3_pair_distance_throw(env, "java/lang/NullPointerException", "null pair distance input");
        return;
    }
    jsize count = (*env)->GetArrayLength(env, thresholds);
    if (count != (*env)->GetArrayLength(env, results)) {
        m3_pair_distance_throw(env, "java/lang/IllegalArgumentException",
                "pair distance output length mismatch");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jlong distances[M3_PAIR_DISTANCE_PAGE];
    jlong answers[M3_PAIR_DISTANCE_PAGE];

    /* Validate every threshold before any output write, even for empty owners.
     * All input and output arrays remain managed by the VM and never escape.
     */
    for (jsize base = 0; base < count;) {
        jsize length = count - base;
        if (length > M3_PAIR_DISTANCE_PAGE) length = M3_PAIR_DISTANCE_PAGE;
        (*env)->GetLongArrayRegion(env, thresholds, base, length, distances);
        if ((*env)->ExceptionCheck(env)) return;
        for (jsize i = 0; i < length; ++i) {
            if (distances[i] < 0) {
                m3_pair_distance_throw(env, "java/lang/IllegalArgumentException",
                        "negative pair distance threshold");
                return;
            }
        }
        base += length;
    }

    m3_pair_distance_page left_page = {{0}, -1, 0};
    m3_pair_distance_page right_page = {{0}, -1, 0};
    for (jsize base = 0; base < count;) {
        jsize length = count - base;
        if (length > M3_PAIR_DISTANCE_PAGE) length = M3_PAIR_DISTANCE_PAGE;
        (*env)->GetLongArrayRegion(env, thresholds, base, length, distances);
        if ((*env)->ExceptionCheck(env)) return;
        for (jsize i = 0; i < length; ++i) {
            jlong total = 0;
            if (size > 1 && !m3_pair_distance_count(env, sorted, size, distances[i],
                    &left_page, &right_page, &total)) return;
            answers[i] = total;
        }
        (*env)->SetLongArrayRegion(env, results, base, length, answers);
        if ((*env)->ExceptionCheck(env)) return;
        base += length;
    }
}

//FLLDCIM: MRL - census P2-073, crate m3jdk21-lc315-count-smaller-native-20261026 (donor blob 442630c291ce): LC 315 count-smaller kernel
/*
 * LC #315 Count of Smaller Numbers After Self: strictly smaller right-side
 * occurrence counts, indexed by original input coordinates. The existing
 * source-owned m3rankbatch JNI library receives this ABI append, not a new
 * loader, Java callback or per-element object representation.
 *
 * O(n log n) primitive index merge inversion count; O(n) temporary native
 * storage; 256-long JNI region reads. Never hold a critical JNI pointer
 * while sorting. All allocations and reads complete before a single output
 * publication; a failed native operation leaves caller output untouched.
 */
static void m3_count_smaller_throw(JNIEnv *env, const char *name,
        const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT void JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_countSmallerRight0(
        JNIEnv *env, jclass ignored, jlongArray input, jintArray output) {
    (void) ignored;
    if (input == NULL || output == NULL) {
        m3_count_smaller_throw(env, "java/lang/NullPointerException",
                "null count-smaller JNI input or output");
        return;
    }
    const jsize length = (*env)->GetArrayLength(env, input);
    if (length != (*env)->GetArrayLength(env, output)) {
        m3_count_smaller_throw(env, "java/lang/IllegalArgumentException",
                "count-smaller output length mismatch");
        return;
    }
    if (length < 2) return;
    const size_t n = (size_t) length;
    if (n > SIZE_MAX / sizeof(jlong) || n > SIZE_MAX / sizeof(jint)) {
        m3_count_smaller_throw(env, "java/lang/OutOfMemoryError",
                "count-smaller JNI array-size overflow");
        return;
    }
    jlong *values = malloc(n * sizeof(*values));
    jint *indices = malloc(n * sizeof(*indices));
    jint *scratch = malloc(n * sizeof(*scratch));
    jint *counts = calloc(n, sizeof(*counts));
    if (values == NULL || indices == NULL || scratch == NULL || counts == NULL) {
        free(values); free(indices); free(scratch); free(counts);
        m3_count_smaller_throw(env, "java/lang/OutOfMemoryError",
                "count-smaller JNI primitive work buffers");
        return;
    }
    enum { M3_LC315_PAGE = 256 };
    /* JNI region reads permit JVM object movement during O(n log n) work. */
    for (jsize start = 0; start < length;) {
        jsize count = length - start;
        if (count > M3_LC315_PAGE) count = M3_LC315_PAGE;
        (*env)->GetLongArrayRegion(env, input, start, count, values + start);
        if ((*env)->ExceptionCheck(env)) {
            free(values); free(indices); free(scratch); free(counts);
            return;
        }
        start += count;
    }
    for (size_t i = 0; i < n; ++i) indices[i] = (jint) i;

    jint *from = indices;
    jint *to = scratch;
    /* Avoid size_t overflow even on 32-bit native targets. */
    for (size_t width = 1; width < n; width = width > n / 2 ? n : width * 2) {
        for (size_t base = 0; base < n;) {
            const size_t middle = base + (width < n - base ? width : n - base);
            const size_t end = middle + (width < n - middle ? width : n - middle);
            size_t left = base, right = middle, write = base;
            jint right_moved = 0;
            while (left < middle && right < end) {
                const jint original_left = from[left];
                const jint original_right = from[right];
                if (values[original_left] <= values[original_right]) {
                    counts[original_left] += right_moved;
                    to[write++] = original_left;
                    ++left;
                } else {
                    to[write++] = original_right;
                    ++right;
                    ++right_moved;
                }
            }
            while (left < middle) {
                const jint original_left = from[left++];
                counts[original_left] += right_moved;
                to[write++] = original_left;
            }
            while (right < end) to[write++] = from[right++];
            base = end;
        }
        jint *swap = from; from = to; to = swap;
    }
    (*env)->SetIntArrayRegion(env, output, 0, length, counts);
    free(values); free(indices); free(scratch); free(counts);
}

//FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-kth-pair-distance-jni-20261026 (donor blob d52ddd76ccc2): LC 719 k-th pair-distance kernel
/* LeetCode #719: batched k-th pair distance over existing signed-long JNI rank owner.
 * Selects one-based ranks, returning exact representable distances only.
 * Reuses the existing fixed 256-entry array-region pages for pair counts;
 * no Java heap pointer, Java comparator or callback crosses the JNI boundary.
 */
JNIEXPORT void JNICALL
Java_jdk_m3_collections_M3LongRankBatchNative_kthPairDistancesInto0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray ranks,
        jint rank_from, jint length, jlongArray output, jint output_from) {
    (void) ignored;
    if (sorted == NULL || ranks == NULL || output == NULL) {
        m3_pair_distance_throw(env, "java/lang/NullPointerException", "null k-th pair batch");
        return;
    }
    if ((*env)->IsSameObject(env, sorted, output)
            || (*env)->IsSameObject(env, ranks, output)) {
        m3_pair_distance_throw(env, "java/lang/IllegalArgumentException",
                "pair-selection output aliases input");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize ranks_count = (*env)->GetArrayLength(env, ranks);
    jsize output_count = (*env)->GetArrayLength(env, output);
    if (rank_from < 0 || length < 0 || output_from < 0
            || length > ranks_count || rank_from > ranks_count - length
            || length > output_count || output_from > output_count - length) {
        m3_pair_distance_throw(env, "java/lang/IndexOutOfBoundsException",
                "k-th pair selection window outside array");
        return;
    }
    if (length == 0) return;

    /* The maximum pair count fits jlong even for the maximum Java array. */
    jlong total = (jlong) size * ((jlong) size - 1) / 2;
    jlong selected[128];
    /* Prevalidate EVERY rank before publishing ANY result prefix. */
    for (jint done = 0; done < length;) {
        jint chunk = length - done;
        if (chunk > 128) chunk = 128;
        (*env)->GetLongArrayRegion(env, ranks, rank_from + done, chunk, selected);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < chunk; i++) {
            if (selected[i] < 1 || selected[i] > total) {
                m3_pair_distance_throw(env, "java/lang/IndexOutOfBoundsException",
                        "k-th pair rank outside [1, n*(n-1)/2]");
                return;
            }
        }
        done += chunk;
    }
    m3_pair_distance_page left_page = {{0}, -1, 0};
    m3_pair_distance_page right_page = {{0}, -1, 0};
    jlong representable = 0;
    if (!m3_pair_distance_count(env, sorted, size, INT64_MAX,
            &left_page, &right_page, &representable)) return;
    for (jint done = 0; done < length;) {
        jint chunk = length - done;
        if (chunk > 128) chunk = 128;
        (*env)->GetLongArrayRegion(env, ranks, rank_from + done, chunk, selected);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < chunk; i++) {
            if (selected[i] > representable) {
                m3_pair_distance_throw(env, "java/lang/ArithmeticException",
                        "selected pair distance exceeds Long.MAX_VALUE");
                return;
            }
        }
        done += chunk;
    }

    jlong first, last;
    if (!m3_pair_distance_read(env, sorted, size, 0, &left_page, &first)
            || !m3_pair_distance_read(env, sorted, size, size - 1, &right_page, &last)) {
        return;
    }
    uint64_t extent = (uint64_t) last - (uint64_t) first;
    jlong bound = extent > (uint64_t) INT64_MAX ? INT64_MAX : (jlong) extent;
    jlong results[128];
    for (jint done = 0; done < length;) {
        jint chunk = length - done;
        if (chunk > 128) chunk = 128;
        (*env)->GetLongArrayRegion(env, ranks, rank_from + done, chunk, selected);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < chunk; i++) {
            jlong low = 0, high = bound;
            while (low < high) {
                jlong middle = low + (high - low) / 2;
                jlong at_most = 0;
                if (!m3_pair_distance_count(env, sorted, size, middle,
                        &left_page, &right_page, &at_most)) return;
                if (at_most >= selected[i]) high = middle;
                else low = middle + 1;
            }
            results[i] = low;
        }
        (*env)->SetLongArrayRegion(env, output, output_from + done, chunk, results);
        if ((*env)->ExceptionCheck(env)) return;
        done += chunk;
    }
}
