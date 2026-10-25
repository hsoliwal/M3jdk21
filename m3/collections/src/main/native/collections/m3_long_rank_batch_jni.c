/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>
#include <limits.h>
#include <stdint.h>

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
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count != (*env)->GetArrayLength(env, results)) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "result length mismatch");
        return;
    }
    if (count == 0) return;

    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) return;
    jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }
    jint *positions = (*env)->GetIntArrayElements(env, results, NULL);
    if (positions == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }

    if (m3_use_monotone_sweep(targets, count, size, 1U)) {
        jsize cursor = 0;
        for (jsize i = 0; i < count; i++) {
            while (cursor < size && values[cursor] < targets[i]) cursor++;
            positions[i] = (jint) cursor;
        }
    } else {
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
    }
    (*env)->ReleaseIntArrayElements(env, results, positions, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}

/*
 * LeetCode 34 category, M3 contract: one sorted long[] plus a batch of
 * queries yields interleaved [lower, upper) insertion coordinates. Unlike
 * an inclusive "last occurrence" there is no target+1 overflow at LONG_MAX.
 * Inputs remain read-only under JNI_ABORT and no callback crosses JNI.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_equalRanges0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray ranges) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || ranges == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count > INT_MAX / 2 || (*env)->GetArrayLength(env, ranges) != count * 2) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "range output length mismatch");
        return;
    }
    if (count == 0) return;

    const jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) return;
    const jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
        return;
    }
    jint *result = (*env)->GetIntArrayElements(env, ranges, NULL);
    if (result == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, (jlong *) targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
        return;
    }
    if (m3_use_monotone_sweep(targets, count, size, 2U)) {
        jsize lower = 0;
        jsize upper = 0;
        for (jsize i = 0; i < count; i++) {
            const jlong needle = targets[i];
            while (lower < size && values[lower] < needle) lower++;
            if (upper < lower) upper = lower;
            /* Never use needle + 1: LONG_MAX remains valid without overflow. */
            while (upper < size && values[upper] <= needle) upper++;
            result[2 * i] = (jint) lower;
            result[2 * i + 1] = (jint) upper;
        }
    } else {
        for (jsize i = 0; i < count; ++i) {
            jlong needle = targets[i];
            jsize lo = 0;
            jsize hi = size;
            while (lo < hi) {
                jsize mid = lo + (hi - lo) / 2;
                if (values[mid] < needle) lo = mid + 1;
                else hi = mid;
            }
            jint first = (jint) lo;
            hi = size;
            while (lo < hi) {
                jsize mid = lo + (hi - lo) / 2;
                if (values[mid] <= needle) lo = mid + 1;
                else hi = mid;
            }
            result[2 * i] = first;
            result[2 * i + 1] = (jint) lo;
        }
    }
    (*env)->ReleaseIntArrayElements(env, ranges, result, 0);
    (*env)->ReleaseLongArrayElements(env, queries, (jlong *) targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, (jlong *) values, JNI_ABORT);
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
            if (mode == M3_RANK_COUNT) {
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
            const jsize hi = m3_bound(source, source_size, keys[i], 1);
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
    if (mode != M3_RANK_UPPER && mode != M3_RANK_COUNT) {
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
