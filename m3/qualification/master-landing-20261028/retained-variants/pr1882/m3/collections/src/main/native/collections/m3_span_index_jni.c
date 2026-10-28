/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Pure primitive JNI batch: Java predicates/comparators/native handles never cross.
 */
#include <jni.h>
#include <stddef.h>
#include <stdint.h>

static jint earliest_covering(jint target, const jint *starts, const jlong *prefix, jsize count) {
    if (target <= 0) return target < 0 ? 0 : target;
    if (count == 0 || starts[0] >= target || prefix[count - 1] < (jlong) target) return target;
    /* Same first-qualifying-prefix proof as the Java owner; start remains exclusive. */
    jsize low = 0;
    jsize high = count - 1;
    while (low < high) {
        jsize middle = low + (high - low) / 2;
        if (prefix[middle] >= (jlong) target) high = middle;
        else low = middle + 1;
    }
    return starts[low] < target ? starts[low] : target;
}

/* LeetCode #35 boundary search over M3's *existing* prefix-maximum span plan.
 * Chunked source/output query regions eliminate unbounded query/result JNI pins.
 * The two immutable snapshot lanes are critically accessed only for 256 queries;
 * NO JNI calls, allocations, blocking operations or Java callbacks occur while pinned.
 * JNI may internally copy critical arrays, so no zero-copy guarantee is claimed.
 */
enum { M3_SPAN_QUERY_PAGE = 256 };

/*
 * LeetCode #34: lower bound of monotone prefix ends.
 * LeetCode #88: two forward cursors for an already-sorted query page.
 *
 * Scanning the entire snapshot can be worse than q binary searches, so
 * select the sweep only when the primitive query page is nondecreasing
 * and the owner scan estimate does not exceed q * floor(log2(n) + 1).
 * This is a comparison-cost gate, not a wall-clock speedup assertion.
 */
static int span_use_monotone_sweep(const jint *requested, jsize queries, jsize count) {
    if (queries < 2 || count == 0) return 0;
    for (jsize i = 1; i < queries; i++) {
        if (requested[i] < requested[i - 1]) return 0;
    }
    unsigned int levels = 0;
    for (jsize remaining = count; remaining > 0; remaining >>= 1) levels++;
    /* uint64_t avoids overflow at the largest legal Java array lengths. */
    return (uint64_t) (uint32_t) count
            <= (uint64_t) (uint32_t) queries * (uint64_t) levels;
}
/*
 * Preserve the primitive prefix cursor across contiguous nondecreasing
 * 256-query pages. Restart when a page is unordered or its first query
 * precedes the previously processed page's last query.
 *
 * LeetCode #35 lower-bound and #88 ordered two-stream merge patterns.
 * No JNI calls, allocation, handles, or mutation of caller arrays here.
 * Query-count cost gating remains unchanged for first/standalone pages.
 */
static jboolean span_answer_page(const jint *requests, jsize page,
        const jint *starts, const jlong *prefix, jsize count, jint *answers,
        jsize *cursor, jboolean previous_sweep, jint previous_last) {
    const jboolean sweep = span_use_monotone_sweep(requests, page, count)
            ? JNI_TRUE : JNI_FALSE;
    if (sweep == JNI_TRUE) {
        if (previous_sweep == JNI_FALSE || requests[0] < previous_last)
            *cursor = 0;
        for (jsize i = 0; i < page; i++) {
            const jint row = requests[i];
            if (row <= 0) {
                answers[i] = row < 0 ? 0 : row;
            } else if (starts[0] >= row || prefix[count - 1] < (jlong) row) {
                answers[i] = row;
            } else {
                /* Sorted query pages never lower an accepted prefix bound. */
                while (*cursor < count && prefix[*cursor] < (jlong) row)
                    ++*cursor;
                answers[i] = starts[*cursor] < row ? starts[*cursor] : row;
            }
        }
    } else {
        for (jsize i = 0; i < page; i++)
            answers[i] = earliest_covering(requests[i], starts, prefix, count);
    }
    return sweep;
}

static jboolean span_into(JNIEnv *env, jintArray sorted_starts, jlongArray prefix_max,
        jintArray targets, jsize query_from, jsize length,
        jintArray output, jsize output_from, jsize count) {
    jint requests[M3_SPAN_QUERY_PAGE];
    jint answers[M3_SPAN_QUERY_PAGE];
    jsize cursor = 0;
    jint previous_last = 0;
    jboolean previous_sweep = JNI_FALSE;
    for (jsize done = 0; done < length;) {
        jsize page = length - done;
        if (page > M3_SPAN_QUERY_PAGE) page = M3_SPAN_QUERY_PAGE;
        (*env)->GetIntArrayRegion(env, targets, query_from + done, page, requests);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        const jint *starts = (*env)->GetPrimitiveArrayCritical(env, sorted_starts, NULL);
        if (starts == NULL) return JNI_FALSE;
        const jlong *prefix = (*env)->GetPrimitiveArrayCritical(env, prefix_max, NULL);
        if (prefix == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, sorted_starts, (void *) starts, JNI_ABORT);
            return JNI_FALSE;
        }
        previous_sweep = span_answer_page(requests, page, starts, prefix,
                count, answers, &cursor, previous_sweep, previous_last);
        previous_last = requests[page - 1];
        (*env)->ReleasePrimitiveArrayCritical(env, prefix_max, (void *) prefix, JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env, sorted_starts, (void *) starts, JNI_ABORT);
        (*env)->SetIntArrayRegion(env, output, output_from + done, page, answers);
        if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
        done += page;
    }
    return JNI_TRUE;
}

static void span_error(JNIEnv *env, const char *klass, const char *message) {
    jclass error = (*env)->FindClass(env, klass);
    if (error != NULL) (*env)->ThrowNew(env, error, message);
}

/* Existing ABI preserved: returns a detached output array. */
JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3SpanIndex_batchNative(
        JNIEnv *env, jclass unused, jintArray sorted_starts, jlongArray prefix_max,
        jintArray targets) {
    (void) unused;
    if (sorted_starts == NULL || prefix_max == NULL || targets == NULL) return NULL;
    jsize count = (*env)->GetArrayLength(env, sorted_starts);
    if (count != (*env)->GetArrayLength(env, prefix_max)) return NULL;
    jsize queries = (*env)->GetArrayLength(env, targets);
    jintArray result = (*env)->NewIntArray(env, queries);
    if (result == NULL) return NULL;
    if (span_into(env, sorted_starts, prefix_max, targets, 0, queries,
                  result, 0, count) == JNI_FALSE) return NULL;
    return result;
}

/* Additive ABI: caller-owned output window, no result allocation or pointer lifetime. */
JNIEXPORT void JNICALL Java_com_m3_collections_M3SpanIndex_batchIntoNative(
        JNIEnv *env, jclass unused, jintArray sorted_starts, jlongArray prefix_max,
        jintArray targets, jint query_from, jint length,
        jintArray output, jint output_from) {
    (void) unused;
    if (sorted_starts == NULL || prefix_max == NULL || targets == NULL || output == NULL) {
        span_error(env, "java/lang/NullPointerException", "null span batch array");
        return;
    }
    jsize count = (*env)->GetArrayLength(env, sorted_starts);
    if (count != (*env)->GetArrayLength(env, prefix_max)) {
        span_error(env, "java/lang/IllegalArgumentException", "mismatched span lanes");
        return;
    }
    jsize n = (*env)->GetArrayLength(env, targets);
    jsize capacity = (*env)->GetArrayLength(env, output);
    if (query_from < 0 || length < 0 || output_from < 0
            || query_from > n - length || output_from > capacity - length) {
        span_error(env, "java/lang/IndexOutOfBoundsException", "span batch window");
        return;
    }
    if ((*env)->IsSameObject(env, targets, output)) {
        span_error(env, "java/lang/IllegalArgumentException", "output must not alias queries");
        return;
    }
    (void) span_into(env, sorted_starts, prefix_max, targets,
                     query_from, length, output, output_from, count);
}
