/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdbool.h>
#include <limits.h>
#include <stdint.h>
#include <string.h>
#include "jdk_m3_search_M3NativeSortedLongBounds.h"

#define M3_JNI_WINDOW 256
#define M3_JNI_CACHE_SLOTS 4
#define M3_JNI_CACHE_MASK (M3_JNI_CACHE_SLOTS - 1)
#define M3_JNI_PREFETCH_MIN_KEYS 128
#define M3_JNI_PREFETCH_SAMPLE 32
#define M3_JNI_PREFETCH_MIN_TRANSITIONS 8
#define M3_JNI_TOTAL_CACHE (M3_JNI_WINDOW * M3_JNI_CACHE_SLOTS)
#define M3_JNI_STREAM_MAX_SOURCE 8192
#define M3_JNI_STREAM_MAX_SOURCE_PER_KEY 4
#define M3_EQUAL_PROBE_LIMIT 8
#define M3_REPEAT_MEMO_SLOTS 64
#define M3_REPEAT_MEMO_MASK (M3_REPEAT_MEMO_SLOTS - 1)
#define M3_REPEAT_MEMO_WAYS 8
#define M3_REPEAT_MEMO_SETS (M3_REPEAT_MEMO_SLOTS / M3_REPEAT_MEMO_WAYS)
#define M3_REPEAT_MEMO_SET_MASK (M3_REPEAT_MEMO_SETS - 1)
#define M3_REPEAT_SAMPLE 64
#define M3_REPEAT_MIN_NONADJACENT 10

typedef struct M3LongWindow {
    JNIEnv *env;
    jlongArray sorted;
    jint from;
    jint to;
    jint first[M3_JNI_CACHE_SLOTS];
    jint last[M3_JNI_CACHE_SLOTS];
    jlong values[M3_JNI_TOTAL_CACHE];
    bool whole_slice_cached;
} M3LongWindow;

/* Eight-way set-associative primitive bounds, all invocation-local stack storage. */
typedef struct M3BoundMemo {
    jlong key;
    jint lower;
    jint upper;
    bool occupied;
} M3BoundMemo;

/* Deterministic unsigned mixing, not a semantic equality predicate. */
static unsigned memo_slot(jlong key) {
    uint64_t bits = (uint64_t)key;
    bits ^= bits >> 33;
    bits *= UINT64_C(0x9e3779b97f4a7c15);
    return (unsigned)(bits >> 58) & M3_REPEAT_MEMO_MASK;
}

/* The direct-mapped owner is retained unless sampled collisions fit at most
   eight keys per associative set. A ninth sampled key refuses this mode. */
static bool conflicting_memo_keys(const jlong *keys, jsize count) {
    jlong known_key[M3_REPEAT_MEMO_SLOTS];
    bool present[M3_REPEAT_MEMO_SLOTS] = {false};
    jlong set_keys[M3_REPEAT_MEMO_SETS][M3_REPEAT_MEMO_WAYS];
    unsigned char set_sizes[M3_REPEAT_MEMO_SETS] = {0};
    bool collision = false;
    jsize n = count < M3_REPEAT_SAMPLE ? count : M3_REPEAT_SAMPLE;
    for (jsize i = 0; i < n; i++) {
        unsigned slot = memo_slot(keys[i]);
        unsigned set = slot & M3_REPEAT_MEMO_SET_MASK;
        if (present[slot] && known_key[slot] != keys[i]) {
            collision = true;
        }
        known_key[slot] = keys[i];
        present[slot] = true;
        unsigned way = 0;
        for (; way < set_sizes[set]; way++) {
            if (set_keys[set][way] == keys[i]) {
                break;
            }
        }
        if (way == set_sizes[set]) {
            if (way == M3_REPEAT_MEMO_WAYS) {
                return false; /* Sampling cannot justify set associativity. */
            }
            set_keys[set][way] = keys[i];
            set_sizes[set]++;
        }
    }
    return collision;
}

/* Detect non-adjacent exact repeats in the already copied JNI query chunk. */
static bool repeated_nonadjacent(const jlong *keys, jsize count) {
    jsize sample = count < M3_REPEAT_SAMPLE ? count : M3_REPEAT_SAMPLE;
    jint repeated = 0;
    for (jsize i = 2; i < sample; ++i) {
        if (keys[i] == keys[i - 1]) {
            continue; /* Existing adjacent-key memo handles this shape. */
        }
        for (jsize j = 0; j + 1 < i; ++j) {
            if (keys[j] == keys[i]) {
                repeated++;
                break;
            }
        }
    }
    return repeated >= M3_REPEAT_MIN_NONADJACENT;
}

/* Four stack-owned, independently tagged JNI regions; no Java heap pointer is retained. */
static bool at(M3LongWindow *window, jint index, jlong *result) {
    jint page = (index - window->from) / M3_JNI_WINDOW;
    jint slot = page & M3_JNI_CACHE_MASK;
    if (index < window->first[slot] || index >= window->last[slot]) {
        jint start = window->from + page * M3_JNI_WINDOW;
        jint count = window->to - start;
        if (count > M3_JNI_WINDOW) {
            count = M3_JNI_WINDOW;
        }
        (*window->env)->GetLongArrayRegion(window->env, window->sorted,
                                             start, count, &window->values[slot * M3_JNI_WINDOW]);
        if ((*window->env)->ExceptionCheck(window->env)) {
            return false;
        }
        window->first[slot] = start;
        window->last[slot] = start + count;
    }
    *result = window->values[slot * M3_JNI_WINDOW + index - window->first[slot]];
    return true;
}

static bool bound(M3LongWindow *window, jlong needle, bool upper, jint *result) {
    jint low = window->from;
    jint high = window->to;
    while (low < high) {
        jint middle = low + ((high - low) >> 1);
        jlong value;
        if (!at(window, middle, &value)) {
            return false;
        }
        if (value < needle || (upper && value == needle)) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    *result = low;
    return true;
}

/* At most eight sequential equal-key probes; fall back to binary search for longer runs. */
static bool upper_after(M3LongWindow *window, jlong key, jint lower, jint *result) {
    jint next = lower;
    jint inspected = 0;
    while (next < window->to && inspected < M3_EQUAL_PROBE_LIMIT) {
        jlong value;
        if (!at(window, next, &value)) {
            return false;
        }
        if (value != key) {
            *result = next;
            return true;
        }
        next++;
        inspected++;
    }
    if (next == window->to) {
        *result = next;
        return true;
    }
    jint low = next;
    jint high = window->to;
    while (low < high) {
        jint middle = low + ((high - low) >> 1);
        jlong value;
        if (!at(window, middle, &value)) {
            return false;
        }
        if (value <= key) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    *result = low;
    return true;
}

/* A pure native binary-search atom for the wholly copied source slice.
   Only invocation-owned stack values are read; no JNI access or exception path. */
static jint bound_cached(const M3LongWindow *window, jlong needle, jint low, bool upper) {
    jint high = window->to;
    while (low < high) {
        jint middle = low + ((high - low) >> 1);
        jlong value = window->values[middle - window->from];
        if (value < needle || (upper && value == needle)) {
            low = middle + 1;
        } else {
            high = middle;
        }
    }
    return low;
}

/* Same first/last-occurrence contract as bound() + upper_after(), but JNI-free. */
static void bounds_cached(const M3LongWindow *window, jlong needle,
                          jint *lower_out, jint *upper_out) {
    jint lower = bound_cached(window, needle, window->from, false);
    jint upper = lower;
    jint inspected = 0;
    while (upper < window->to && inspected < M3_EQUAL_PROBE_LIMIT) {
        if (window->values[upper - window->from] != needle) {
            *lower_out = lower;
            *upper_out = upper;
            return;
        }
        upper++;
        inspected++;
    }
    if (upper < window->to && inspected == M3_EQUAL_PROBE_LIMIT) {
        upper = bound_cached(window, needle, upper, true);
    }
    *lower_out = lower;
    *upper_out = upper;
}

static void throw_illegal_argument(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL && !(*env)->ExceptionCheck(env)) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

/* Symbol name and descriptor are verified against javac -h generated header. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_search_M3NativeSortedLongBounds_nativeBounds(
        JNIEnv *env, jclass ignored, jlongArray sorted, jint from, jint to,
        jlongArray keys) {
    (void)ignored;
    if (sorted == NULL || keys == NULL) {
        throw_illegal_argument(env, "native bounds requires two non-null arrays");
        return NULL;
    }
    jsize source_length = (*env)->GetArrayLength(env, sorted);
    jsize key_count = (*env)->GetArrayLength(env, keys);
    if (from < 0 || to < from || to > source_length || key_count > INT_MAX / 2) {
        throw_illegal_argument(env, "invalid native bounds source range or result length");
        return NULL;
    }

    jintArray result = (*env)->NewIntArray(env, key_count * 2);
    if (result == NULL || (*env)->ExceptionCheck(env)) {
        return NULL;
    }
    M3LongWindow window = {.env = env, .sorted = sorted, .from = from, .to = to};
    jlong batch_keys[M3_JNI_WINDOW];
    jint pairs[2 * M3_JNI_WINDOW];
    /* One invocation-local memo; equality spans JNI query-region boundaries. */
    bool have_previous = false;
    jlong previous_key = 0;
    jint previous_lower = from;
    jint previous_upper = from;
    /* An ordered first query chunk is only a hint; each later key rechecks it. */
    bool ordered_merge = false;
    jint scan_lower = from;
    jint scan_upper = from;
    bool repeated_memo_enabled = false;
    bool repeated_associative = false;
    M3BoundMemo repeated_memo[M3_REPEAT_MEMO_SLOTS];
    unsigned char memo_victim[M3_REPEAT_MEMO_SETS];
    for (jsize offset = 0; offset < key_count;) {
        jsize count = key_count - offset;
        if (count > M3_JNI_WINDOW) {
            count = M3_JNI_WINDOW;
        }
        (*env)->GetLongArrayRegion(env, keys, offset, count, batch_keys);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        /* Qualify the optional complete-source copy using the first query region,
           which is already in stack storage. Long repeated prefixes retain the
           four demand-filled pages instead of copying unneeded source values. */
        if (offset == 0 && key_count >= M3_JNI_PREFETCH_MIN_KEYS && to > from) {
            jint source_span = to - from;
            bool whole_slice_candidate = source_span <= M3_JNI_TOTAL_CACHE;
            /* For larger sources, a forward scan is bounded by at most 4q
               source checks. Sparse queries keep binary-search demand pages. */
            bool stream_candidate = source_span > M3_JNI_TOTAL_CACHE &&
                    source_span <= M3_JNI_STREAM_MAX_SOURCE &&
                    key_count >= M3_JNI_WINDOW &&
                    (jlong)source_span <= (jlong)M3_JNI_STREAM_MAX_SOURCE_PER_KEY * key_count;
            if (whole_slice_candidate || stream_candidate) {
                jsize sample = count < M3_JNI_PREFETCH_SAMPLE ? count : M3_JNI_PREFETCH_SAMPLE;
                jint changes = 0;
                for (jsize j = 1; j < sample; j++) {
                    if (batch_keys[j] != batch_keys[j - 1]) {
                        changes++;
                    }
                }
                /* The whole first chunk, not just the short diversity sample, must
                   be ordered. A later descending key permanently drops to binary. */
                bool prefix_ordered = true;
                jint strict_increases = 0;
                /* An entirely constant first sample cannot establish a useful
                   ordered-merge win; preserve the historical demand cache. */
                for (jsize j = 1; changes > 0 && j < count; j++) {
                    if (batch_keys[j] < batch_keys[j - 1]) {
                        prefix_ordered = false;
                        break;
                    }
                    if (batch_keys[j] > batch_keys[j - 1]) {
                        strict_increases++;
                    }
                }
                ordered_merge = prefix_ordered &&
                        strict_increases >= M3_JNI_PREFETCH_MIN_TRANSITIONS;
                if (whole_slice_candidate &&
                        (changes >= M3_JNI_PREFETCH_MIN_TRANSITIONS || ordered_merge)) {
                    (*env)->GetLongArrayRegion(env, sorted, from, source_span, window.values);
                    if ((*env)->ExceptionCheck(env)) {
                        return NULL;
                    }
                    window.whole_slice_cached = true;
                }
            }
        }
        if (offset == 0 && !ordered_merge && !window.whole_slice_cached &&
                key_count >= M3_JNI_WINDOW && to - from > M3_JNI_TOTAL_CACHE &&
                repeated_nonadjacent(batch_keys, count)) {
            memset(repeated_memo, 0, sizeof(repeated_memo));
            repeated_associative = conflicting_memo_keys(batch_keys, count);
            if (repeated_associative) {
                memset(memo_victim, 0, sizeof(memo_victim));
            }
            repeated_memo_enabled = true;
        }
        jsize i = 0;
        /* Keep the original cached-source hot loop free of new per-key mode
           checks. A separate streaming loop accesses large-source JNI pages. */
        if (window.whole_slice_cached) {
            for (; i < count && ordered_merge; i++) {
                jlong key = batch_keys[i];
                if (have_previous && key == previous_key) {
                    pairs[2 * i] = previous_lower;
                    pairs[2 * i + 1] = previous_upper;
                    continue;
                }
                if (have_previous && key < previous_key) {
                    ordered_merge = false;
                    break;
                }
                while (scan_lower < to && window.values[scan_lower - from] < key) {
                    scan_lower++;
                }
                if (scan_upper < scan_lower) {
                    scan_upper = scan_lower;
                }
                while (scan_upper < to && window.values[scan_upper - from] <= key) {
                    scan_upper++;
                }
                pairs[2 * i] = scan_lower;
                pairs[2 * i + 1] = scan_upper;
                previous_key = key;
                previous_lower = scan_lower;
                previous_upper = scan_upper;
                have_previous = true;
            }
        } else {
            for (; i < count && ordered_merge; i++) {
                jlong key = batch_keys[i];
                if (have_previous && key == previous_key) {
                    pairs[2 * i] = previous_lower;
                    pairs[2 * i + 1] = previous_upper;
                    continue;
                }
                if (have_previous && key < previous_key) {
                    ordered_merge = false;
                    break;
                }
                /* Forward-only JNI region windows for larger source slices.
                   No full-array copy, JNI pointer retention or heap memory. */
                jlong value;
                while (scan_lower < to) {
                    if (!at(&window, scan_lower, &value)) {
                        return NULL;
                    }
                    if (value >= key) {
                        break;
                    }
                    scan_lower++;
                }
                if (scan_upper < scan_lower) {
                    scan_upper = scan_lower;
                }
                while (scan_upper < to) {
                    if (!at(&window, scan_upper, &value)) {
                        return NULL;
                    }
                    if (value > key) {
                        break;
                    }
                    scan_upper++;
                }
                pairs[2 * i] = scan_lower;
                pairs[2 * i + 1] = scan_upper;
                previous_key = key;
                previous_lower = scan_lower;
                previous_upper = scan_upper;
                have_previous = true;
            }
        }
        /* No detected collisions: execute the original 64-slot direct-map
           loop without adding a mode check to its per-query hot path. */
        if (repeated_memo_enabled && !repeated_associative) {
            for (; i < count; i++) {
                jlong key = batch_keys[i];
                if (have_previous && key == previous_key) {
                    pairs[2 * i] = previous_lower;
                    pairs[2 * i + 1] = previous_upper;
                } else {
                    unsigned slot = memo_slot(key);
                    M3BoundMemo *entry = &repeated_memo[slot];
                    if (entry->occupied && entry->key == key) {
                        pairs[2 * i] = entry->lower;
                        pairs[2 * i + 1] = entry->upper;
                    } else {
                        if (!bound(&window, key, false, &pairs[2 * i]) ||
                                !upper_after(&window, key, pairs[2 * i], &pairs[2 * i + 1])) {
                            return NULL;
                        }
                        entry->key = key;
                        entry->lower = pairs[2 * i];
                        entry->upper = pairs[2 * i + 1];
                        entry->occupied = true;
                    }
                    previous_key = key;
                    previous_lower = pairs[2 * i];
                    previous_upper = pairs[2 * i + 1];
                    have_previous = true;
                }
            }
        }
        /* Proven key collisions: eight ways per set, deterministic RR victim.
           Each hit compares exact signed 64-bit key, never hash alone. */
        if (repeated_memo_enabled && repeated_associative) {
            for (; i < count; ++i) {
                jlong key = batch_keys[i];
                if (have_previous && key == previous_key) {
                    pairs[2 * i] = previous_lower;
                    pairs[2 * i + 1] = previous_upper;
                } else {
                    unsigned set = memo_slot(key) & M3_REPEAT_MEMO_SET_MASK;
                    unsigned base = set * M3_REPEAT_MEMO_WAYS;
                    M3BoundMemo *hit = NULL;
                    unsigned vacant = M3_REPEAT_MEMO_SLOTS;
                    for (unsigned way = 0; way < M3_REPEAT_MEMO_WAYS; way++) {
                        M3BoundMemo *candidate = &repeated_memo[base + way];
                        if (candidate->occupied && candidate->key == key) {
                            hit = candidate;
                            break;
                        }
                        if (!candidate->occupied && vacant == M3_REPEAT_MEMO_SLOTS) {
                            vacant = base + way;
                        }
                    }
                    if (hit != NULL) {
                        pairs[2 * i] = hit->lower;
                        pairs[2 * i + 1] = hit->upper;
                    } else {
                        if (!bound(&window, key, false, &pairs[2 * i]) ||
                                !upper_after(&window, key, pairs[2 * i], &pairs[2 * i + 1])) {
                            return NULL;
                        }
                        unsigned slot = vacant;
                        if (slot == M3_REPEAT_MEMO_SLOTS) {
                            slot = base + memo_victim[set];
                            memo_victim[set] = (unsigned char)(
                                    (memo_victim[set] + 1U) & (M3_REPEAT_MEMO_WAYS - 1));
                        }
                        M3BoundMemo *entry = &repeated_memo[slot];
                        entry->key = key;
                        entry->lower = pairs[2 * i];
                        entry->upper = pairs[2 * i + 1];
                        entry->occupied = true;
                    }
                    previous_key = key;
                    previous_lower = pairs[2 * i];
                    previous_upper = pairs[2 * i + 1];
                    have_previous = true;
                }
            }
        }
        /* All other query batches retain the original per-query binary loop. */
        for (; i < count; i++) {
            jlong key = batch_keys[i];
            if (have_previous && key == previous_key) {
                pairs[2 * i] = previous_lower;
                pairs[2 * i + 1] = previous_upper;
            } else {
                if (window.whole_slice_cached) {
                    bounds_cached(&window, key, &pairs[2 * i], &pairs[2 * i + 1]);
                } else if (!bound(&window, key, false, &pairs[2 * i]) ||
                        !upper_after(&window, key, pairs[2 * i], &pairs[2 * i + 1])) {
                    return NULL;
                }
                previous_key = key;
                previous_lower = pairs[2 * i];
                previous_upper = pairs[2 * i + 1];
                have_previous = true;
            }
        }
        (*env)->SetIntArrayRegion(env, result, offset * 2, count * 2, pairs);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
        offset += count;
    }
    return result;
}
