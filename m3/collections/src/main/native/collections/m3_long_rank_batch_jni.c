/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <limits.h>
#include <stdint.h>
#include <stdlib.h>

/*
 * LeetCode 34/35 (two insertion boundaries) + LeetCode 88 (merge of sorted
 * streams), adapted independently to primitive JNI batch search.
 * A monotone query lane admits a merge-style scan O(n + q) instead of
 * q separate O(log n) bisections. The adaptive cost gate retains binary
 * search when scanning the owner would be more expensive. No Java array,
 * JNI export, callback, ownership, exception policy or input mutation changes.
 */
static int m3_queries_non_decreasing(const jlong *targets, jsize count) {
    for (jsize i = 1; i < count; i++) {
        if (targets[i] < targets[i - 1]) return 0;
    }
    return 1;
}

static int m3_use_monotone_sweep(const jlong *targets, jsize count,
        jsize size, unsigned int bounds_per_query) {
    if (count < 2 || !m3_queries_non_decreasing(targets, count)) return 0;
    unsigned int levels = 0;
    for (jsize remaining = size; remaining > 0; remaining >>= 1) levels++;
    /* Cast before multiply: even Integer.MAX_VALUE Java lanes cannot overflow. */
    uint64_t estimated_binary = (uint64_t) (unsigned int) count
            * (uint64_t) levels * (uint64_t) bounds_per_query;
    return (uint64_t) (unsigned int) size <= estimated_binary;
}

/*
 * One JNI transition per batch, signed jlong order matching Java Long.compare.
 * Get/ReleaseArrayElements respects moving GC: no object addresses are retained.
 * Input arrays are released JNI_ABORT; only the preallocated output array is written.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_lowerBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || results == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    const jsize size = (*env)->GetArrayLength(env, sorted);
    const jsize count = (*env)->GetArrayLength(env, queries);
    if (count != (*env)->GetArrayLength(env, results)) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "result length mismatch");
        return;
    }
    if (count == 0) return;
    /* LC #35 lower bound and LC #88 monotone merge: bounded JNI query pages.
     * Existing Java API and JNI symbol are unchanged. No whole-array query
     * GetArrayElements, no retained pointers, no JNI calls while source pinned.
     */
    enum { M3_LOWER_PAGE = 256 };
    jlong targets[M3_LOWER_PAGE];
    jint positions[M3_LOWER_PAGE];
    for (jsize done = 0; done < count;) {
        jsize batch = count - done;
        if (batch > M3_LOWER_PAGE) batch = M3_LOWER_PAGE;
        (*env)->GetLongArrayRegion(env, queries, done, batch, targets);
        if ((*env)->ExceptionCheck(env)) return;
        if (size == 0) {
            for (jsize i = 0; i < batch; i++) positions[i] = 0;
        } else {
            const jlong *values = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (values == NULL) return;
            if (m3_use_monotone_sweep(targets, batch, size, 1U)) {
                jsize cursor = 0;
                for (jsize i = 0; i < batch; i++) {
                    while (cursor < size && values[cursor] < targets[i]) cursor++;
                    positions[i] = (jint) cursor;
                }
            } else {
                for (jsize i = 0; i < batch; i++) {
                    jsize lo = 0, hi = size;
                    while (lo < hi) {
                        jsize mid = lo + ((hi - lo) >> 1);
                        if (values[mid] < targets[i]) lo = mid + 1;
                        else hi = mid;
                    }
                    positions[i] = (jint) lo;
                }
            }
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) values, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, results, done, batch, positions);
        if ((*env)->ExceptionCheck(env)) return;
        done += batch;
    }
}

/*
 * LeetCode 34 category, M3 contract: one sorted long[] plus a batch of
 * queries yields interleaved [lower, upper) insertion coordinates. Unlike
 * an inclusive "last occurrence" there is no target+1 overflow at LONG_MAX.
 * Inputs remain read-only under JNI_ABORT and no callback crosses JNI.
 */
/*
 * Legacy LC #34 batch entry point, retained ABI. Query and result pages stay
 * bounded (256 longs plus 512 ints); never pin or copy an entire query/result
 * vector just to process it once. Only the sorted payload is pinned, once per
 * page and with no JNI calls while pinned. Outputs are published page by page.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_equalRanges0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray ranges) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || ranges == NULL) return;
    const jsize size = (*env)->GetArrayLength(env, sorted);
    const jsize count = (*env)->GetArrayLength(env, queries);
    if (count > INT_MAX / 2 || (*env)->GetArrayLength(env, ranges) != count * 2) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "range output length mismatch");
        return;
    }
    enum { M3_EQUAL_RANGE_PAGE = 256 };
    jlong targets[M3_EQUAL_RANGE_PAGE];
    jint result[M3_EQUAL_RANGE_PAGE * 2];
    for (jsize done = 0; done < count;) {
        jsize batch = count - done;
        if (batch > M3_EQUAL_RANGE_PAGE) batch = M3_EQUAL_RANGE_PAGE;
        (*env)->GetLongArrayRegion(env, queries, done, batch, targets);
        if ((*env)->ExceptionCheck(env)) return;
        if (size == 0) {
            for (jsize i = 0; i < batch; ++i) {
                result[2 * i] = 0;
                result[2 * i + 1] = 0;
            }
        } else {
            const jlong *values = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (values == NULL) return;
            if (m3_use_monotone_sweep(targets, batch, size, 2U)) {
                jsize lower = 0, upper = 0;
                for (jsize i = 0; i < batch; ++i) {
                    const jlong needle = targets[i];
                    while (lower < size && values[lower] < needle) lower++;
                    if (upper < lower) upper = lower;
                    while (upper < size && values[upper] <= needle) upper++;
                    result[2 * i] = (jint) lower;
                    result[2 * i + 1] = (jint) upper;
                }
            } else {
                for (jsize i = 0; i < batch; ++i) {
                    const jlong needle = targets[i];
                    jsize lo = 0, hi = size;
                    while (lo < hi) {
                        jsize mid = lo + (hi - lo) / 2;
                        if (values[mid] < needle) lo = mid + 1;
                        else hi = mid;
                    }
                    result[2 * i] = (jint) lo;
                    hi = size;
                    while (lo < hi) {
                        jsize mid = lo + (hi - lo) / 2;
                        if (values[mid] <= needle) lo = mid + 1;
                        else hi = mid;
                    }
                    result[2 * i + 1] = (jint) lo;
                }
            }
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) values, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, ranges, done * 2, batch * 2, result);
        if ((*env)->ExceptionCheck(env)) return;
        done += batch;
    }
}

/* Original LeetCode #34/#35 primitive upper/count-in-window JNI atom. */
enum { M3_RANK_OUTPUT_CHUNK = 256, M3_RANK_UPPER = 1, M3_RANK_COUNT = 2 };

static jsize m3_bound(const jlong *sorted, jsize length, jlong key, int upper) {
    jsize lo = 0, hi = length;
    while (lo < hi) {
        jsize mid = lo + (hi - lo) / 2;
        if (upper ? sorted[mid] <= key : sorted[mid] < key) lo = mid + 1;
        else hi = mid;
    }
    return lo;
}

/*
 * LeetCode #34/#35 insertion bounds + #88 monotone two-stream scan:
 * if the query page is non-decreasing, advance two primitive coordinates
 * exactly once. The existing cost gate rejects large-owner scans and all
 * unordered query pages; normal binary rank remains the exact fallback.
 * Bounded 256-query stack lanes, no JNI calls inside critical sections.
 */
static void m3_rank_bound_chunk(const jlong *source, jsize source_size,
        const jlong *keys, jint batch, jint mode, jint *positions) {
    if (m3_use_monotone_sweep(keys, batch, source_size,
            mode == M3_RANK_COUNT ? 2U : 1U)) {
        jsize lower = 0;
        jsize upper = 0;
        for (jint i = 0; i < batch; i++) {
            const jlong key = keys[i];
            if (mode == 0) {
                while (lower < source_size && source[lower] < key) lower++;
                positions[i] = (jint) lower;
            } else if (mode == M3_RANK_COUNT) {
                while (lower < source_size && source[lower] < key) lower++;
                if (upper < lower) upper = lower;
                while (upper < source_size && source[upper] <= key) upper++;
                positions[i] = (jint) (upper - lower);
            } else {
                while (upper < source_size && source[upper] <= key) upper++;
                positions[i] = (jint) upper;
            }
        }
    } else {
        for (jint i = 0; i < batch; i++) {
            const jsize hi = m3_bound(source, source_size, keys[i], mode != 0);
            positions[i] = mode == M3_RANK_COUNT
                    ? (jint) (hi - m3_bound(source, source_size, keys[i], 0))
                    : (jint) hi;
        }
    }
}

static void m3_rank_throw(JNIEnv *env, const char *type, const char *message) {
    jclass error = (*env)->FindClass(env, type);
    if (error != NULL) (*env)->ThrowNew(env, error, message);
}

/*
 * One native transition per eligible batch. Native stack scratch is bounded
 * to 256 query longs and 256 result ints. No heap Entry/tuple objects and no
 * object addresses retained. GetPrimitiveArrayCritical has no JNI calls or
 * blocking operations between acquisition and release; read-only inputs.
 * A caller-owned output window remains untouched when validation fails.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_boundBatchInto0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jint query_from, jint count, jintArray output, jint output_from, jint mode) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || output == NULL) {
        m3_rank_throw(env, "java/lang/NullPointerException", "null rank batch array");
        return;
    }
    if (mode != 0 && mode != M3_RANK_UPPER && mode != M3_RANK_COUNT) {
        m3_rank_throw(env, "java/lang/IllegalArgumentException", "invalid rank output mode");
        return;
    }
    const jsize source_size = (*env)->GetArrayLength(env, sorted);
    const jsize query_size = (*env)->GetArrayLength(env, queries);
    const jsize result_size = (*env)->GetArrayLength(env, output);
    if (query_from < 0 || count < 0 || output_from < 0
            || query_from > query_size - count || output_from > result_size - count) {
        m3_rank_throw(env, "java/lang/IndexOutOfBoundsException", "rank window outside array");
        return;
    }
    if (count == 0) return;

    jlong keys[M3_RANK_OUTPUT_CHUNK];
    jint positions[M3_RANK_OUTPUT_CHUNK];
    for (jint completed = 0; completed < count;) {
        jint batch = count - completed;
        if (batch > M3_RANK_OUTPUT_CHUNK) batch = M3_RANK_OUTPUT_CHUNK;
        (*env)->GetLongArrayRegion(env, queries, query_from + completed, batch, keys);
        if ((*env)->ExceptionCheck(env)) return;
        if (source_size == 0) {
            for (jint i = 0; i < batch; ++i) positions[i] = 0;
        } else {
            const jlong *source = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (source == NULL) return;  /* JVM left OOME pending. */
            m3_rank_bound_chunk(source, source_size, keys, batch, mode, positions);
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) source, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, output, output_from + completed, batch, positions);
        if ((*env)->ExceptionCheck(env)) return;
        completed += batch;
    }
}

/*
 * LeetCode #986 interval-boundary category + #34/#35 insertion coordinates:
 * count sorted primitive values in parallel half-open [begin,end) intervals.
 * Independent M3 algorithm; no challenge code or Java callback is included.
 * The full input vector is checked before ANY caller-output mutation.
 * JNI critical-region work is bounded to 256 interval pairs, with no JNI
 * calls/blocking/allocation during the short-lived critical section.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_rangeCountInto0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray starts,
        jlongArray ends, jint query_from, jint count, jintArray output,
        jint output_from) {
    (void) ignored;
    if (sorted == NULL || starts == NULL || ends == NULL || output == NULL) {
        m3_rank_throw(env, "java/lang/NullPointerException", "null interval array");
        return;
    }
    const jsize source_size = (*env)->GetArrayLength(env, sorted);
    const jsize start_size = (*env)->GetArrayLength(env, starts);
    const jsize end_size = (*env)->GetArrayLength(env, ends);
    const jsize output_size = (*env)->GetArrayLength(env, output);
    if (start_size != end_size) {
        m3_rank_throw(env, "java/lang/IllegalArgumentException",
                "interval endpoint vector length mismatch");
        return;
    }
    if (query_from < 0 || count < 0 || output_from < 0
            || query_from > start_size - count
            || output_from > output_size - count) {
        m3_rank_throw(env, "java/lang/IndexOutOfBoundsException",
                "interval/output window outside array");
        return;
    }
    if (count == 0) return;

    jlong from[M3_RANK_OUTPUT_CHUNK];
    jlong to[M3_RANK_OUTPUT_CHUNK];
    jint result[M3_RANK_OUTPUT_CHUNK];

    /* Prevalidate ALL endpoints before ANY SetIntArrayRegion operation. */
    for (jint checked = 0; checked < count;) {
        jint batch = count - checked;
        if (batch > M3_RANK_OUTPUT_CHUNK) batch = M3_RANK_OUTPUT_CHUNK;
        (*env)->GetLongArrayRegion(env, starts, query_from + checked, batch, from);
        if ((*env)->ExceptionCheck(env)) return;
        (*env)->GetLongArrayRegion(env, ends, query_from + checked, batch, to);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < batch; i++) {
            if (from[i] > to[i]) {
                m3_rank_throw(env, "java/lang/IllegalArgumentException",
                        "inverted half-open interval");
                return;
            }
        }
        checked += batch;
    }

    for (jint completed = 0; completed < count;) {
        jint batch = count - completed;
        if (batch > M3_RANK_OUTPUT_CHUNK) batch = M3_RANK_OUTPUT_CHUNK;
        (*env)->GetLongArrayRegion(env, starts, query_from + completed, batch, from);
        if ((*env)->ExceptionCheck(env)) return;
        (*env)->GetLongArrayRegion(env, ends, query_from + completed, batch, to);
        if ((*env)->ExceptionCheck(env)) return;

        if (source_size == 0) {
            for (jint i = 0; i < batch; i++) result[i] = 0;
        } else {
            const jlong *source = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (source == NULL) return;  /* JVM left OOME pending. */
            for (jint i = 0; i < batch; i++) {
                /* Equal endpoints are empty; no target+1 arithmetic. */
                result[i] = (jint) (m3_bound(source, source_size, to[i], 0)
                        - m3_bound(source, source_size, from[i], 0));
            }
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) source, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, output, output_from + completed, batch, result);
        if ((*env)->ExceptionCheck(env)) return;
        completed += batch;
    }
}

/*
 * LeetCode #350 two-pointer intersection, counting min occurrence frequencies.
 * Existing M3 m3rankbatch JNI owner; one native transition. Two bounded
 * 256-long pages use 4096 bytes stack, never pin or materialize an entire
 * Java payload, retain object addresses, or cross into user callbacks.
 */
JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3LongRankBatchNative_intersectionCount0(
        JNIEnv *env, jclass ignored, jlongArray left, jlongArray right) {
    (void) ignored;
    if (left == NULL || right == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null sorted rank lane");
        return 0;
    }
    jsize left_size = (*env)->GetArrayLength(env, left);
    jsize right_size = (*env)->GetArrayLength(env, right);
    if (left_size == 0 || right_size == 0) return 0;

    enum { M3_INTERSECTION_PAGE = 256 };
    jlong left_page[M3_INTERSECTION_PAGE];
    jlong right_page[M3_INTERSECTION_PAGE];
    jsize left_at = 0, right_at = 0;
    jsize left_page_at = 0, right_page_at = 0;
    jsize left_page_len = 0, right_page_len = 0;
    jlong matches = 0;
    while (left_at < left_size && right_at < right_size) {
        if (left_page_at == left_page_len) {
            jsize count = left_size - left_at;
            if (count > M3_INTERSECTION_PAGE) count = M3_INTERSECTION_PAGE;
            (*env)->GetLongArrayRegion(env, left, left_at, count, left_page);
            if ((*env)->ExceptionCheck(env)) return 0;
            left_page_at = 0;
            left_page_len = count;
        }
        if (right_page_at == right_page_len) {
            jsize count = right_size - right_at;
            if (count > M3_INTERSECTION_PAGE) count = M3_INTERSECTION_PAGE;
            (*env)->GetLongArrayRegion(env, right, right_at, count, right_page);
            if ((*env)->ExceptionCheck(env)) return 0;
            right_page_at = 0;
            right_page_len = count;
        }
        jlong a = left_page[left_page_at];
        jlong b = right_page[right_page_at];
        if (a < b) {
            left_at++;
            left_page_at++;
        } else if (a > b) {
            right_at++;
            right_page_at++;
        } else {
            matches++;
            left_at++;
            right_at++;
            left_page_at++;
            right_page_at++;
        }
    }
    return matches;
}

/*
 * Additive LC #349/#350 sorted-window intersection JNI ABI. Both operands
 * remain Java-owned and read-only; only 256-value fixed stack pages cross
 * GetLongArrayRegion. This cannot retain moving-GC pointers or invoke Java
 * comparators, and it shares no mutable state with the original batch ranks.
 */
enum { M3_INTERSECTION_WINDOW_PAGE = 256 };

static void m3_window_throw(JNIEnv *env, const char *name, const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3LongRankBatchNative_intersectionWindowCount0(
        JNIEnv *env, jclass ignored, jlongArray left, jint left_from,
        jint left_length, jlongArray right, jint right_from,
        jint right_length, jboolean distinct) {
    (void) ignored;
    if (left == NULL || right == NULL) {
        m3_window_throw(env, "java/lang/NullPointerException", "null sorted window");
        return 0;
    }
    jsize left_size = (*env)->GetArrayLength(env, left);
    if ((*env)->ExceptionCheck(env)) return 0;
    jsize right_size = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return 0;
    /* Subtraction prevents overflow for an invalid from+length pair. */
    if (left_from < 0 || left_length < 0 || left_from > left_size
            || left_length > left_size - left_from
            || right_from < 0 || right_length < 0 || right_from > right_size
            || right_length > right_size - right_from) {
        m3_window_throw(env, "java/lang/IndexOutOfBoundsException",
                "sorted intersection window outside primitive lane");
        return 0;
    }
    if (left_length == 0 || right_length == 0) return 0;

    const jsize left_end = left_from + left_length;
    const jsize right_end = right_from + right_length;
    jlong left_page[M3_INTERSECTION_WINDOW_PAGE];
    jlong right_page[M3_INTERSECTION_WINDOW_PAGE];
    jsize left_at = left_from, right_at = right_from;
    jsize left_cursor = 0, right_cursor = 0;
    jsize left_loaded = 0, right_loaded = 0;
    jlong count = 0;
    jlong previous_match = 0;
    int matched_before = 0;

    while (left_at < left_end && right_at < right_end) {
        if (left_cursor == left_loaded) {
            jsize batch = left_end - left_at;
            if (batch > M3_INTERSECTION_WINDOW_PAGE) batch = M3_INTERSECTION_WINDOW_PAGE;
            (*env)->GetLongArrayRegion(env, left, left_at, batch, left_page);
            if ((*env)->ExceptionCheck(env)) return 0;
            left_cursor = 0;
            left_loaded = batch;
        }
        if (right_cursor == right_loaded) {
            jsize batch = right_end - right_at;
            if (batch > M3_INTERSECTION_WINDOW_PAGE) batch = M3_INTERSECTION_WINDOW_PAGE;
            (*env)->GetLongArrayRegion(env, right, right_at, batch, right_page);
            if ((*env)->ExceptionCheck(env)) return 0;
            right_cursor = 0;
            right_loaded = batch;
        }
        const jlong a = left_page[left_cursor];
        const jlong b = right_page[right_cursor];
        if (a < b) {
            left_cursor++;
            left_at++;
        } else if (a > b) {
            right_cursor++;
            right_at++;
        } else {
            /* #350 counts min(freqL, freqR); #349 counts each match once. */
            if (distinct != JNI_TRUE || !matched_before || previous_match != a) {
                count++;
                previous_match = a;
                matched_before = 1;
            }
            left_cursor++;
            right_cursor++;
            left_at++;
            right_at++;
        }
    }
    return count;
}

/*
 * LeetCode #349 / #350: exact sorted-set or multiset intersection values.
 * One JNI call, two linear passes for exact output size, and bounded primitive
 * windows. Moving-GC object pointers are never acquired or retained.
 */
enum { M3_INTERSECTION_VALUES_PAGE = 256 };

static jsize m3_intersection_values_scan(JNIEnv *env, jlongArray left, jsize left_size,
        jlongArray right, jsize right_size, jboolean distinct,
        jlongArray output, jsize output_capacity) {
    jlong left_page[M3_INTERSECTION_VALUES_PAGE];
    jlong right_page[M3_INTERSECTION_VALUES_PAGE];
    jlong result_page[M3_INTERSECTION_VALUES_PAGE];
    jsize left_at = 0, right_at = 0;
    jsize left_cursor = 0, right_cursor = 0;
    jsize left_loaded = 0, right_loaded = 0;
    jsize produced = 0;
    jlong previous = 0;
    int seen = 0;
    while (left_at < left_size && right_at < right_size) {
        if (left_cursor == left_loaded) {
            jsize batch = left_size - left_at;
            if (batch > M3_INTERSECTION_VALUES_PAGE) batch = M3_INTERSECTION_VALUES_PAGE;
            (*env)->GetLongArrayRegion(env, left, left_at, batch, left_page);
            if ((*env)->ExceptionCheck(env)) return -1;
            left_cursor = 0;
            left_loaded = batch;
        }
        if (right_cursor == right_loaded) {
            jsize batch = right_size - right_at;
            if (batch > M3_INTERSECTION_VALUES_PAGE) batch = M3_INTERSECTION_VALUES_PAGE;
            (*env)->GetLongArrayRegion(env, right, right_at, batch, right_page);
            if ((*env)->ExceptionCheck(env)) return -1;
            right_cursor = 0;
            right_loaded = batch;
        }
        jlong a = left_page[left_cursor];
        jlong b = right_page[right_cursor];
        if (a < b) {
            left_cursor++;
            left_at++;
        } else if (a > b) {
            right_cursor++;
            right_at++;
        } else {
            if (distinct != JNI_TRUE || !seen || previous != a) {
                if (output != NULL) {
                    if (produced >= output_capacity) {
                        m3_window_throw(env, "java/lang/IllegalStateException",
                                "sorted intersection grew between count and emission");
                        return -1;
                    }
                    result_page[produced % M3_INTERSECTION_VALUES_PAGE] = a;
                }
                produced++;
                previous = a;
                seen = 1;
                if (output != NULL && produced % M3_INTERSECTION_VALUES_PAGE == 0) {
                    (*env)->SetLongArrayRegion(env, output,
                            produced - M3_INTERSECTION_VALUES_PAGE,
                            M3_INTERSECTION_VALUES_PAGE, result_page);
                    if ((*env)->ExceptionCheck(env)) return -1;
                }
            }
            left_cursor++;
            right_cursor++;
            left_at++;
            right_at++;
        }
    }
    if (output != NULL) {
        jsize pending = produced % M3_INTERSECTION_VALUES_PAGE;
        if (pending != 0) {
            (*env)->SetLongArrayRegion(env, output, produced - pending, pending, result_page);
            if ((*env)->ExceptionCheck(env)) return -1;
        }
    }
    return produced;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3LongRankBatchNative_intersectionValues0(
        JNIEnv *env, jclass ignored, jlongArray left, jlongArray right,
        jboolean distinct) {
    (void) ignored;
    if (left == NULL || right == NULL) {
        m3_window_throw(env, "java/lang/NullPointerException", "null sorted intersection input");
        return NULL;
    }
    jsize left_size = (*env)->GetArrayLength(env, left);
    jsize right_size = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize count = m3_intersection_values_scan(
            env, left, left_size, right, right_size, distinct, NULL, 0);
    if (count < 0) return NULL;
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result == NULL) return NULL;
    jsize written = m3_intersection_values_scan(
            env, left, left_size, right, right_size, distinct, result, count);
    if (written < 0) return NULL;
    if (written != count) {
        m3_window_throw(env, "java/lang/IllegalStateException",
                "sorted intersection changed between count and emission");
        return NULL;
    }
    return result;
}

/*
 * LeetCode #88: two sorted primitive cursors, preserving the maximum
 * occurrence multiplicity of each value. Optional #26 mode emits one
 * value per distinct run. No Java array reference is ever pinned or retained.
 * Input and output move through bounded 256-element JNI region pages.
 */
enum { M3_UNION_VALUES_PAGE = 256 };

static jlong m3_union_values_scan(JNIEnv *env, jlongArray left, jsize left_size,
        jlongArray right, jsize right_size, jboolean distinct,
        jlongArray output, jsize output_capacity) {
    jlong left_page[M3_UNION_VALUES_PAGE];
    jlong right_page[M3_UNION_VALUES_PAGE];
    jlong result_page[M3_UNION_VALUES_PAGE];
    jsize li = 0, ri = 0;
    jsize lc = 0, rc = 0;
    jsize ll = 0, rl = 0;
    jlong written = 0;
    jlong previous = 0;
    int seen = 0;
    while (li < left_size || ri < right_size) {
        if (li < left_size && lc == ll) {
            jsize batch = left_size - li;
            if (batch > M3_UNION_VALUES_PAGE) batch = M3_UNION_VALUES_PAGE;
            (*env)->GetLongArrayRegion(env, left, li, batch, left_page);
            if ((*env)->ExceptionCheck(env)) return -1;
            lc = 0;
            ll = batch;
        }
        if (ri < right_size && rc == rl) {
            jsize batch = right_size - ri;
            if (batch > M3_UNION_VALUES_PAGE) batch = M3_UNION_VALUES_PAGE;
            (*env)->GetLongArrayRegion(env, right, ri, batch, right_page);
            if ((*env)->ExceptionCheck(env)) return -1;
            rc = 0;
            rl = batch;
        }

        jlong next;
        if (li == left_size) {
            next = right_page[rc++];
            ri++;
        } else if (ri == right_size) {
            next = left_page[lc++];
            li++;
        } else {
            jlong a = left_page[lc], b = right_page[rc];
            if (a < b) {
                next = a;
                lc++;
                li++;
            } else if (a > b) {
                next = b;
                rc++;
                ri++;
            } else {
                next = a;
                lc++;
                rc++;
                li++;
                ri++;
            }
        }
        if (distinct != JNI_TRUE || !seen || previous != next) {
            if (written >= (jlong) INT_MAX) {
                m3_window_throw(env, "java/lang/OutOfMemoryError",
                        "sorted union exceeds Java array size");
                return -1;
            }
            if (output != NULL) {
                if (written >= output_capacity) {
                    m3_window_throw(env, "java/lang/IllegalStateException",
                            "sorted union grew between count and emission");
                    return -1;
                }
                result_page[(jsize) (written % M3_UNION_VALUES_PAGE)] = next;
            }
            written++;
            previous = next;
            seen = 1;
            if (output != NULL && written % M3_UNION_VALUES_PAGE == 0) {
                (*env)->SetLongArrayRegion(env, output,
                        (jsize) (written - M3_UNION_VALUES_PAGE),
                        M3_UNION_VALUES_PAGE, result_page);
                if ((*env)->ExceptionCheck(env)) return -1;
            }
        }
    }
    if (output != NULL) {
        jsize pending = (jsize) (written % M3_UNION_VALUES_PAGE);
        if (pending != 0) {
            (*env)->SetLongArrayRegion(env, output,
                    (jsize) (written - pending), pending, result_page);
            if ((*env)->ExceptionCheck(env)) return -1;
        }
    }
    return written;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3LongRankBatchNative_unionValues0(
        JNIEnv *env, jclass ignored, jlongArray left, jlongArray right,
        jboolean distinct) {
    (void) ignored;
    if (left == NULL || right == NULL) {
        m3_window_throw(env, "java/lang/NullPointerException",
                "null sorted union input");
        return NULL;
    }
    jsize ln = (*env)->GetArrayLength(env, left);
    jsize rn = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jlong count = m3_union_values_scan(env, left, ln, right, rn, distinct, NULL, 0);
    if (count < 0) return NULL;
    jlongArray output = (*env)->NewLongArray(env, (jsize) count);
    if (output == NULL) return NULL;
    jlong written = m3_union_values_scan(env, left, ln, right, rn, distinct,
            output, (jsize) count);
    if (written < 0) return NULL;
    if (written != count) {
        m3_window_throw(env, "java/lang/IllegalStateException",
                "sorted union changed between count and emission");
        return NULL;
    }
    return output;
}

/*
 * M3 signed-long primitive difference (LeetCode #2215 / #350 categories).
 * Unmodified sorted snapshot owners; two bounded JNI page cursors, no
 * retained VM pointers, no user callbacks, two-pass exact output sizing.
 * distinct=true is SET algebra (left-only/XOR), whereas false uses
 * occurrence multiplicities max(left_count-right_count,0) / abs(delta).
 */
enum { M3_DIFFERENCE_PAGE = 256 };

typedef struct {
    jlongArray array;
    jsize size;
    jsize index;
    jsize page_start;
    jsize page_length;
    jlong page[M3_DIFFERENCE_PAGE];
} m3_diff_cursor;

static int m3_diff_peek(JNIEnv *env, m3_diff_cursor *cursor, jlong *value) {
    if (cursor->index >= cursor->size) return 0;
    if (cursor->page_length == 0 ||
            cursor->index < cursor->page_start ||
            cursor->index - cursor->page_start >= cursor->page_length) {
        jsize remaining = cursor->size - cursor->index;
        jsize amount = remaining < M3_DIFFERENCE_PAGE ? remaining : M3_DIFFERENCE_PAGE;
        (*env)->GetLongArrayRegion(env, cursor->array, cursor->index, amount, cursor->page);
        if ((*env)->ExceptionCheck(env)) return -1;
        cursor->page_start = cursor->index;
        cursor->page_length = amount;
    }
    *value = cursor->page[cursor->index - cursor->page_start];
    return 1;
}

static jlong m3_difference_scan(JNIEnv *env,
        jlongArray left, jsize left_size, jlongArray right, jsize right_size,
        jboolean distinct, jboolean symmetric, jlongArray output, jsize capacity) {
    m3_diff_cursor lhs = { .array = left, .size = left_size };
    m3_diff_cursor rhs = { .array = right, .size = right_size };
    jlong output_page[M3_DIFFERENCE_PAGE];
    jlong emitted = 0;
    while (lhs.index < left_size || rhs.index < right_size) {
        jlong a = 0, b = 0;
        int has_left = m3_diff_peek(env, &lhs, &a);
        int has_right = m3_diff_peek(env, &rhs, &b);
        if (has_left < 0 || has_right < 0) return -1;
        jlong key = !has_left ? b : !has_right ? a : a < b ? a : b;
        jlong left_count = 0, right_count = 0;
        while ((has_left = m3_diff_peek(env, &lhs, &a)) > 0 && a == key) {
            lhs.index++;
            left_count++;
        }
        if (has_left < 0) return -1;
        while ((has_right = m3_diff_peek(env, &rhs, &b)) > 0 && b == key) {
            rhs.index++;
            right_count++;
        }
        if (has_right < 0) return -1;
        jlong occurrences;
        if (distinct == JNI_TRUE) {
            occurrences = symmetric == JNI_TRUE
                    ? ((left_count > 0) != (right_count > 0))
                    : (left_count > 0 && right_count == 0);
        } else if (symmetric == JNI_TRUE) {
            occurrences = left_count > right_count
                    ? left_count - right_count : right_count - left_count;
        } else {
            occurrences = left_count > right_count ? left_count - right_count : 0;
        }
        if (emitted + occurrences > INT_MAX) {
            m3_window_throw(env, "java/lang/OutOfMemoryError", "sorted difference exceeds Java array limit");
            return -1;
        }
        for (jlong n = 0; n < occurrences && output != NULL; ++n) {
            if (emitted >= capacity) {
                m3_window_throw(env, "java/lang/IllegalStateException",
                        "sorted difference grew between counting and emission");
                return -1;
            }
            output_page[(jsize) (emitted % M3_DIFFERENCE_PAGE)] = key;
            emitted++;
            if (emitted % M3_DIFFERENCE_PAGE == 0) {
                (*env)->SetLongArrayRegion(env, output,
                        (jsize) (emitted - M3_DIFFERENCE_PAGE),
                        M3_DIFFERENCE_PAGE, output_page);
                if ((*env)->ExceptionCheck(env)) return -1;
            }
        }
        if (output == NULL) emitted += occurrences;
    }
    if (output != NULL) {
        jsize pending = (jsize) (emitted % M3_DIFFERENCE_PAGE);
        if (pending != 0) {
            (*env)->SetLongArrayRegion(env, output, (jsize) (emitted - pending),
                    pending, output_page);
            if ((*env)->ExceptionCheck(env)) return -1;
        }
    }
    return emitted;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3LongRankBatchNative_differenceValues0(
        JNIEnv *env, jclass ignored, jlongArray left, jlongArray right,
        jboolean distinct, jboolean symmetric) {
    (void) ignored;
    if (left == NULL || right == NULL) {
        m3_window_throw(env, "java/lang/NullPointerException", "null sorted difference input");
        return NULL;
    }
    jsize left_size = (*env)->GetArrayLength(env, left);
    jsize right_size = (*env)->GetArrayLength(env, right);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jlong expected = m3_difference_scan(env, left, left_size, right, right_size,
            distinct, symmetric, NULL, 0);
    if (expected < 0) return NULL;
    jlongArray result = (*env)->NewLongArray(env, (jsize) expected);
    if (result == NULL) return NULL;
    jlong written = m3_difference_scan(env, left, left_size, right, right_size,
            distinct, symmetric, result, (jsize) expected);
    if (written < 0) return NULL;
    if (written != expected) {
        m3_window_throw(env, "java/lang/IllegalStateException",
                "sorted difference changed between counting and emission");
        return NULL;
    }
    return result;
}

/*
 * LeetCode #26 and #128 categories: a single pass over already-sorted
 * signed-long physical payloads. Return (distinct_count << 32) | run_length.
 *
 * Bounded 256-long GetLongArrayRegion pages cross JNI; never retain moving
 * GC pointers, copy the entire owner, or invoke Java callbacks. The unsigned
 * subtraction tests consecutive values without signed MIN/MAX overflow.
 */
JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3LongRankBatchNative_runStatistics0(
        JNIEnv *env, jclass ignored, jlongArray sorted) {
    (void) ignored;
    if (sorted == NULL) {
        m3_rank_throw(env, "java/lang/NullPointerException", "null sorted rank input");
        return 0;
    }
    const jsize size = (*env)->GetArrayLength(env, sorted);
    if (size == 0) return 0;
    enum { M3_RUN_PAGE = 256 };
    jlong page[M3_RUN_PAGE];
    jlong previous = 0;
    jint distinct = 0, current = 0, best = 0;
    for (jsize offset = 0; offset < size;) {
        jsize count = size - offset;
        if (count > M3_RUN_PAGE) count = M3_RUN_PAGE;
        (*env)->GetLongArrayRegion(env, sorted, offset, count, page);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jsize i = 0; i < count; i++) {
            const jlong value = page[i];
            if (distinct == 0) {
                previous = value;
                distinct = current = best = 1;
                continue;
            }
            if (value == previous) continue;
            distinct++;
            /* C unsigned arithmetic cannot overflow, even at signed extremes. */
            current = value > previous
                    && (uint64_t) value - (uint64_t) previous == UINT64_C(1)
                    ? current + 1 : 1;
            if (current > best) best = current;
            previous = value;
        }
        offset += count;
    }
    return ((jlong) distinct << 32) | (jlong) (uint32_t) best;
}

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
Java_com_m3_collections_M3LongRankBatchNative_topKFrequent0(
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

/*
 * LC #532 first-party primitive distinct K-diff count. This is an ADDITIVE
 * entrypoint to the original m3rankbatch JNI owner, not another library.
 * Strict signed-long sorted input is supplied by immutable M3LongRankIndex.
 * Unsigned subtraction correctly recognizes mathematically unrepresentable
 * (>LONG_MAX) differences without UB or a false match. Two monotone cursor
 * pages are bounded to 256 long values each. GetLongArrayRegion does not pin
 * Java heap objects or retain addresses across JNI transitions.
 */
enum { M3_KDIFF_PAGE = 256 };
typedef struct {
    jlong values[M3_KDIFF_PAGE];
    jsize start;
    jsize length;
} m3_kdiff_page;

static int m3_kdiff_at(JNIEnv *env, jlongArray array, jsize size,
        jsize at, m3_kdiff_page *page, jlong *value) {
    if (at < page->start || at >= page->start + page->length) {
        jsize start = (at / M3_KDIFF_PAGE) * M3_KDIFF_PAGE;
        jsize count = size - start;
        if (count > M3_KDIFF_PAGE) count = M3_KDIFF_PAGE;
        (*env)->GetLongArrayRegion(env, array, start, count, page->values);
        if ((*env)->ExceptionCheck(env)) return 0;
        page->start = start;
        page->length = count;
    }
    *value = page->values[at - page->start];
    return 1;
}

static void m3_kdiff_throw(JNIEnv *env, const char *klass, const char *message) {
    jclass type = (*env)->FindClass(env, klass);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3LongRankBatchNative_distinctDifferencePairCount0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlong distance) {
    (void) ignored;
    if (sorted == NULL) {
        m3_kdiff_throw(env, "java/lang/NullPointerException", "null sorted input");
        return 0L;
    }
    if (distance < 0) {
        m3_kdiff_throw(env, "java/lang/IllegalArgumentException", "negative K-difference");
        return 0L;
    }
    jsize n = (*env)->GetArrayLength(env, sorted);
    if (n < 2) return 0L;
    m3_kdiff_page lp = {{0}, -1, 0};
    m3_kdiff_page rp = {{0}, -1, 0};
    jlong total = 0L;
    if (distance == 0L) {
        jlong previous;
        if (!m3_kdiff_at(env, sorted, n, 0, &lp, &previous)) return 0L;
        int counted = 0;
        for (jsize i = 1; i < n; i++) {
            jlong value;
            if (!m3_kdiff_at(env, sorted, n, i, &lp, &value)) return 0L;
            if (value == previous) {
                if (!counted) total++;
                counted = 1;
            } else {
                previous = value;
                counted = 0;
            }
        }
        return total;
    }
    jsize left = 0;
    jsize right = 1;
    while (left < n && right < n) {
        if (right <= left) {
            right = left + 1;
            continue;
        }
        jlong a, b;
        if (!m3_kdiff_at(env, sorted, n, left, &lp, &a)
                || !m3_kdiff_at(env, sorted, n, right, &rp, &b)) return 0L;
        /* Two signed-ordered values always satisfy b>=a; their mathematically
         * exact nonnegative difference fits in uint64_t, not always in jlong. */
        const uint64_t gap = (uint64_t) b - (uint64_t) a;
        const uint64_t k = (uint64_t) distance;
        if (gap < k) {
            right++;
        } else {
            if (gap == k) total++;
            do {
                left++;
                if (left == n) break;
                jlong candidate;
                if (!m3_kdiff_at(env, sorted, n, left, &lp, &candidate)) return 0L;
                if (candidate != a) break;
            } while (left < n);
        }
    }
    return total;
}

/* SPDX-License-Identifier: Apache-2.0 */
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
Java_com_m3_collections_M3LongRankBatchNative_pairDistanceCounts0(
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

/*
 * LeetCode #34/#35 boundary search and #88 monotone two-pointer query scan.
 * Original shared m3rankbatch JNI library: appended ABI, no new native owner.
 * A caller-owned int[] carries 2*length half-open [lower, upper) pairs at an
 * arbitrary int-slot offset. Each 256-query page has bounded stack scratch.
 */
static void m3_rank_equal_range_chunk(const jlong *source, jsize source_size,
        const jlong *keys, jint batch, jint *pairs) {
    if (m3_use_monotone_sweep(keys, batch, source_size, 2U)) {
        jsize lower = 0, upper = 0;
        for (jint i = 0; i < batch; ++i) {
            const jlong key = keys[i];
            while (lower < source_size && source[lower] < key) ++lower;
            if (upper < lower) upper = lower;
            while (upper < source_size && source[upper] <= key) ++upper;
            pairs[2 * i] = (jint) lower;
            pairs[2 * i + 1] = (jint) upper;
        }
    } else {
        for (jint i = 0; i < batch; ++i) {
            const jsize lo = m3_bound(source, source_size, keys[i], 0);
            pairs[2 * i] = (jint) lo;
            pairs[2 * i + 1] = (jint) m3_bound(source, source_size, keys[i], 1);
        }
    }
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_equalRangesWindow0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jint query_from, jint count, jintArray output, jint output_from) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || output == NULL) {
        m3_rank_throw(env, "java/lang/NullPointerException", "null equal-range batch array");
        return;
    }
    const jsize size = (*env)->GetArrayLength(env, sorted);
    const jsize query_size = (*env)->GetArrayLength(env, queries);
    const jsize result_size = (*env)->GetArrayLength(env, output);
    if (query_from < 0 || count < 0 || output_from < 0 || count > INT_MAX / 2
            || query_from > query_size - count
            || output_from > result_size - count * 2) {
        m3_rank_throw(env, "java/lang/IndexOutOfBoundsException",
                "equal-range window outside array");
        return;
    }
    if (count == 0) return;
    jlong keys[M3_RANK_OUTPUT_CHUNK];
    jint positions[2 * M3_RANK_OUTPUT_CHUNK];
    for (jint completed = 0; completed < count;) {
        jint batch = count - completed;
        if (batch > M3_RANK_OUTPUT_CHUNK) batch = M3_RANK_OUTPUT_CHUNK;
        (*env)->GetLongArrayRegion(env, queries, query_from + completed, batch, keys);
        if ((*env)->ExceptionCheck(env)) return;
        if (size == 0) {
            for (jint i = 0; i < batch * 2; ++i) positions[i] = 0;
        } else {
            const jlong *values = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (values == NULL) return;
            /* No JNI, blocking, allocation or callback while the owner is pinned. */
            m3_rank_equal_range_chunk(values, size, keys, batch, positions);
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) values, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, output, output_from + completed * 2,
                batch * 2, positions);
        if ((*env)->ExceptionCheck(env)) return;
        completed += batch;
    }
}

/*
 * LC #4 two-sorted-lane kth selection, source-owned M3RankBatch JNI atom.
 * Bounded 128-rank pages; no sorting/merge materialization or Java callbacks.
 * No JNI operations while Java long[] lanes are pinned.
 */
enum { M3_KTH_MERGED_PAGE = 128 };

static jlong m3_kth_merged_value(const jlong *left, jsize left_size,
                                const jlong *right, jsize right_size,
                                jlong rank) {
    jsize a = 0, b = 0;
    for (;;) {
        if (a == left_size) return right[b + (jsize) rank];
        if (b == right_size) return left[a + (jsize) rank];
        if (rank == 0) return left[a] <= right[b] ? left[a] : right[b];
        const jlong half = (rank + 1) / 2;
        const jlong remaining_a = (jlong) left_size - a;
        const jlong remaining_b = (jlong) right_size - b;
        const jsize step_a = (jsize) (remaining_a < half ? remaining_a : half);
        const jsize step_b = (jsize) (remaining_b < half ? remaining_b : half);
        if (left[a + step_a - 1] <= right[b + step_b - 1]) {
            a += step_a;
            rank -= step_a;
        } else {
            b += step_b;
            rank -= step_b;
        }
    }
}

static void m3_kth_merged_throw(JNIEnv *env, const char *class_name,
                                const char *message) {
    jclass cls = (*env)->FindClass(env, class_name);
    if (cls != NULL) (*env)->ThrowNew(env, cls, message);
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_selectMergedInto0(
        JNIEnv *env, jclass ignored, jlongArray left, jlongArray right,
        jlongArray ranks, jint rank_from, jint length,
        jlongArray output, jint output_from) {
    (void) ignored;
    if (left == NULL || right == NULL || ranks == NULL || output == NULL) {
        m3_kth_merged_throw(env, "java/lang/NullPointerException", "null merged selection array");
        return;
    }
    if (output == ranks || output == left || output == right) {
        m3_kth_merged_throw(env, "java/lang/IllegalArgumentException",
                            "merged selection output aliases input");
        return;
    }
    const jsize left_size = (*env)->GetArrayLength(env, left);
    const jsize right_size = (*env)->GetArrayLength(env, right);
    const jsize rank_size = (*env)->GetArrayLength(env, ranks);
    const jsize output_size = (*env)->GetArrayLength(env, output);
    const jlong total = (jlong) left_size + (jlong) right_size;
    if (rank_from < 0 || length < 0 || output_from < 0
            || rank_from > rank_size - length
            || output_from > output_size - length) {
        m3_kth_merged_throw(env, "java/lang/IndexOutOfBoundsException",
                            "merged selection window outside array");
        return;
    }
    if (length == 0) return;

    /* First pass refuses ANY invalid rank before modifying caller output. */
    jlong requested[M3_KTH_MERGED_PAGE];
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_KTH_MERGED_PAGE) count = M3_KTH_MERGED_PAGE;
        (*env)->GetLongArrayRegion(env, ranks, rank_from + done, count, requested);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < count; ++i) {
            if (requested[i] < 0 || requested[i] >= total) {
                m3_kth_merged_throw(env, "java/lang/IndexOutOfBoundsException",
                                    "merged rank outside combined source");
                return;
            }
        }
        done += count;
    }

    jlong answers[M3_KTH_MERGED_PAGE];
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_KTH_MERGED_PAGE) count = M3_KTH_MERGED_PAGE;
        (*env)->GetLongArrayRegion(env, ranks, rank_from + done, count, requested);
        if ((*env)->ExceptionCheck(env)) return;

        const jlong *a = NULL, *b = NULL;
        if (left_size != 0) {
            a = (*env)->GetPrimitiveArrayCritical(env, left, NULL);
            if (a == NULL) return;
        }
        if (right_size != 0) {
            if (left == right) {
                b = a;
            } else {
                b = (*env)->GetPrimitiveArrayCritical(env, right, NULL);
                if (b == NULL) {
                    if (a != NULL) (*env)->ReleasePrimitiveArrayCritical(env, left, (void *) a, JNI_ABORT);
                    return;
                }
            }
        }
        /* GC-sensitive critical region: pure bounded arithmetic and loads. */
        for (jint i = 0; i < count; ++i) {
            answers[i] = m3_kth_merged_value(a, left_size, b, right_size, requested[i]);
        }
        if (b != NULL && right != left) {
            (*env)->ReleasePrimitiveArrayCritical(env, right, (void *) b, JNI_ABORT);
        }
        if (a != NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, left, (void *) a, JNI_ABORT);
        }
        (*env)->SetLongArrayRegion(env, output, output_from + done, count, answers);
        if ((*env)->ExceptionCheck(env)) return;
        done += count;
    }
}


/*
 * LC #35 predecessor coordinate: last signed-long value <= query, -1
 * when no such coordinate exists. One JNI call for a validated batch,
 * fixed 256-slot scratch and no JNI calls while a heap array is pinned.
 * No overflow-prone query + 1 conversion (LONG_MAX is handled).
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_floorIndicesInto0(
        JNIEnv *env, jclass unused, jlongArray sorted, jlongArray queries,
        jint query_from, jint length, jintArray output, jint output_from) {
    (void) unused;
    if (sorted == NULL || queries == NULL || output == NULL) {
        m3_rank_throw(env, "java/lang/NullPointerException", "null predecessor array");
        return;
    }
    const jsize size = (*env)->GetArrayLength(env, sorted);
    const jsize query_size = (*env)->GetArrayLength(env, queries);
    const jsize output_size = (*env)->GetArrayLength(env, output);
    if (query_from < 0 || length < 0 || output_from < 0
            || query_from > query_size - length
            || output_from > output_size - length) {
        m3_rank_throw(env, "java/lang/IndexOutOfBoundsException", "predecessor window");
        return;
    }
    jlong keys[M3_RANK_OUTPUT_CHUNK];
    jint indexes[M3_RANK_OUTPUT_CHUNK];
    for (jint done = 0; done < length;) {
        jint count = length - done;
        if (count > M3_RANK_OUTPUT_CHUNK) count = M3_RANK_OUTPUT_CHUNK;
        (*env)->GetLongArrayRegion(env, queries, query_from + done, count, keys);
        if ((*env)->ExceptionCheck(env)) return;
        if (size == 0) {
            for (jint i = 0; i < count; ++i) indexes[i] = -1;
        } else {
            const jlong *values = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
            if (values == NULL) return;
            /* The existing bounded upper-bound kernel handles signed extremes. */
            for (jint i = 0; i < count; ++i) {
                indexes[i] = (jint) m3_bound(values, size, keys[i], 1) - 1;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) values, JNI_ABORT);
        }
        (*env)->SetIntArrayRegion(env, output, output_from + done, count, indexes);
        if ((*env)->ExceptionCheck(env)) return;
        done += count;
    }
}


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
Java_com_m3_collections_M3LongRankBatchNative_countSmallerRight0(
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
/*
 * LeetCode #658: selected-window start on sorted signed-long owners.
 * Unsigned positive differences handle Long.MIN_VALUE/Long.MAX_VALUE exactly;
 * repeated equal windows return the earliest physical start.
 */
static jint m3_closest_window_start(const jlong *sorted, jsize size, jint k, jlong target) {
    if (k == 0 || k == size) return 0;
    jsize low = 0;
    jsize high = size - k;
    while (low < high) {
        jsize middle = low + (high - low) / 2;
        jlong left = sorted[middle];
        jlong right = sorted[middle + k];
        if (target <= left) {
            high = middle;
        } else if (target >= right
                || (uint64_t) target - (uint64_t) left
                    > (uint64_t) right - (uint64_t) target) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    if (sorted[low] == sorted[low + k - 1]) {
        jlong value = sorted[low];
        jsize first = 0;
        jsize end = low;
        while (first < end) {
            jsize middle = first + (end - first) / 2;
            if (sorted[middle] < value) first = middle + 1;
            else end = middle;
        }
        return (jint) first;
    }
    return (jint) low;
}

/* Batches pure primitive #658 distance windows; no object ownership escapes JNI. */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_closestWindowStarts0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray targets,
        jint target_from, jint length, jint k, jintArray starts, jint start_from) {
    (void) ignored;
    if (sorted == NULL || targets == NULL || starts == NULL) {
        jclass type = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (type != NULL) (*env)->ThrowNew(env, type, "null nearest-window batch");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize queries = (*env)->GetArrayLength(env, targets);
    jsize capacity = (*env)->GetArrayLength(env, starts);
    if (k < 0 || k > size) {
        jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (type != NULL) (*env)->ThrowNew(env, type, "invalid k");
        return;
    }
    if (target_from < 0 || length < 0 || start_from < 0
            || length > queries || target_from > queries - length
            || length > capacity || start_from > capacity - length) {
        jclass type = (*env)->FindClass(env, "java/lang/IndexOutOfBoundsException");
        if (type != NULL) (*env)->ThrowNew(env, type, "invalid nearest-window bounds");
        return;
    }
    if (length == 0) return;
    /* Fixed 256-query pages: no copied query vector or full output array. */
    enum { M3_CLOSEST_PAGE = 256 };
    jlong requested[M3_CLOSEST_PAGE];
    jint output[M3_CLOSEST_PAGE];
    for (jint done = 0; done < length;) {
        jint chunk = length - done;
        if (chunk > M3_CLOSEST_PAGE) chunk = M3_CLOSEST_PAGE;
        (*env)->GetLongArrayRegion(env, targets, target_from + done, chunk, requested);
        if ((*env)->ExceptionCheck(env)) return;
        /* Bounded critical region: C-only binary searches, no JNI calls/pointers retained. */
        const jlong *values = (*env)->GetPrimitiveArrayCritical(env, sorted, NULL);
        if (values == NULL) return;
        for (jint i = 0; i < chunk; i++) {
            output[i] = m3_closest_window_start(values, size, k, requested[i]);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, sorted, (void *) values, JNI_ABORT);
        (*env)->SetIntArrayRegion(env, starts, start_from + done, chunk, output);
        if ((*env)->ExceptionCheck(env)) return;
        done += chunk;
    }
}

/* LeetCode #719: batched k-th pair distance over existing signed-long JNI rank owner.
 * Selects one-based ranks, returning exact representable distances only.
 * Reuses the existing fixed 256-entry array-region pages for pair counts;
 * no Java heap pointer, Java comparator or callback crosses the JNI boundary.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_kthPairDistancesInto0(
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

/* LeetCode #167 two-pointer sorted two-sum over signed long coordinates.
 * Reuse existing 2 x 256-long JNI array-region pages: no full-array copy,
 * critical pin, per-element object, VM pointer retention or callback bridge.
 * Query/output windows validated before any output write. */
static int m3_pair_sum_compare(jlong left, jlong right, jlong target) {
    if (right > 0 && left > INT64_MAX - right) return 1;
    if (right < 0 && left < INT64_MIN - right) return -1;
    jlong sum = left + right;
    return (sum > target) - (sum < target);
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_pairSumIndicesInto0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray targets,
        jint target_from, jint length, jintArray output, jint output_from) {
    (void) ignored;
    if (sorted == NULL || targets == NULL || output == NULL) {
        m3_pair_distance_throw(env, "java/lang/NullPointerException", "null pair-sum input");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize queries = (*env)->GetArrayLength(env, targets);
    jsize capacity = (*env)->GetArrayLength(env, output);
    if (target_from < 0 || length < 0 || output_from < 0
            || length > queries || target_from > queries - length
            || output_from > capacity || length > (capacity - output_from) / 2) {
        m3_pair_distance_throw(env, "java/lang/IndexOutOfBoundsException",
                "pair-sum query/output window outside array");
        return;
    }
    if (length == 0) return;

    m3_pair_distance_page left_page = {{0}, -1, 0};
    m3_pair_distance_page right_page = {{0}, -1, 0};
    jlong requests[128];
    jint answers[256];
    for (jint done = 0; done < length;) {
        jint amount = length - done;
        if (amount > 128) amount = 128;
        (*env)->GetLongArrayRegion(env, targets, target_from + done, amount, requests);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < amount; i++) {
            jsize lo = 0;
            jsize hi = size - 1;
            while (lo < hi) {
                jlong a, b;
                if (!m3_pair_distance_read(env, sorted, size, lo, &left_page, &a)
                        || !m3_pair_distance_read(env, sorted, size, hi, &right_page, &b)) return;
                int cmp = m3_pair_sum_compare(a, b, requests[i]);
                if (cmp == 0) break;
                if (cmp < 0) ++lo;
                else --hi;
            }
            answers[2 * i] = lo < hi ? lo : -1;
            answers[2 * i + 1] = lo < hi ? hi : -1;
        }
        (*env)->SetIntArrayRegion(env, output, output_from + 2 * done,
                                  2 * amount, answers);
        if ((*env)->ExceptionCheck(env)) return;
        done += amount;
    }
}
