// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-08: bound single/reverse BMH verification work with the existing lazy KMP table.
#include <jni.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define SNX_JNI_ABI 1
#define SNX_BULK_POLL_WORK 4096U

typedef struct {
    uint32_t length;
    uint32_t shift[256];
    uint32_t reverse_shift[256];
    uint8_t *needle;
    /* Built on first KMP fallback or bulk scan; the Java pattern monitor serializes access. */
    uint32_t *prefix;
} snx_pattern;

/* Stack-local state; the Java caller keeps the pattern borrowed across callbacks. */
typedef struct {
    JNIEnv *env;
    jobject callback;
    jmethodID check_canceled;
    uint32_t remaining;
} snx_bulk_monitor;

static void throw_java(JNIEnv *env, const char *class_name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, class_name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

/* Native lease registry: never turn a Java long into a raw pointer. */
#define SNX_BUCKETS 1024U
typedef struct snx_entry {
    uint64_t id;
    snx_pattern *pattern;
    struct snx_entry *next;
    int borrowed;
    int retired;
} snx_entry;
static snx_entry *snx_table[SNX_BUCKETS];
static atomic_flag snx_guard = ATOMIC_FLAG_INIT;
static _Atomic uint64_t snx_next_id = 1U;

static void snx_lock(void) {
    while (atomic_flag_test_and_set_explicit(&snx_guard, memory_order_acquire)) { }
}
static void snx_unlock(void) {
    atomic_flag_clear_explicit(&snx_guard, memory_order_release);
}
static size_t snx_bucket(uint64_t id) { return (size_t)(id & (SNX_BUCKETS - 1U)); }

static void snx_entry_free(snx_entry *entry) {
    if (entry == NULL) return;
    free(entry->pattern->prefix);
    free(entry->pattern->needle);
    free(entry->pattern);
    free(entry);
}

static jlong snx_register(JNIEnv *env, snx_pattern *pattern) {
    snx_entry *entry = (snx_entry *)calloc(1U, sizeof(*entry));
    if (entry == NULL) {
        free(pattern->needle);
        free(pattern->prefix);
        free(pattern);
        throw_java(env, "java/lang/OutOfMemoryError", "native handle allocation failed");
        return 0;
    }
    /* Saturating CAS: IDs are never reused, even after theoretical exhaustion. */
    uint64_t id = atomic_load_explicit(&snx_next_id, memory_order_relaxed);
    for (;;) {
        if (id == 0U || id > INT64_MAX) {
            free(pattern->needle);
            free(pattern->prefix);
            free(pattern);
            free(entry);
            throw_java(env, "java/lang/IllegalStateException", "native handle IDs exhausted");
            return 0;
        }
        if (atomic_compare_exchange_weak_explicit(
                &snx_next_id, &id, id + 1U,
                memory_order_relaxed, memory_order_relaxed)) break;
    }
    entry->id = id;
    entry->pattern = pattern;
    snx_lock();
    size_t bucket = snx_bucket(id);
    entry->next = snx_table[bucket];
    snx_table[bucket] = entry;
    snx_unlock();
    return (jlong)id;
}

static snx_entry *snx_borrow(JNIEnv *env, jlong handle) {
    if (handle <= 0) {
        throw_java(env, "java/lang/IllegalStateException", "invalid or closed native search handle");
        return NULL;
    }
    uint64_t id = (uint64_t)handle;
    snx_lock();
    snx_entry *entry = snx_table[snx_bucket(id)];
    while (entry != NULL && entry->id != id) entry = entry->next;
    int busy = entry != NULL && entry->borrowed;
    if (entry != NULL && !busy) entry->borrowed = 1;
    snx_unlock();
    if (entry == NULL || busy) {
        throw_java(env, "java/lang/IllegalStateException",
                busy ? "native search pattern already borrowed"
                     : "invalid or closed native search handle");
        return NULL;
    }
    return entry;
}

static void snx_end(snx_entry *entry) {
    snx_lock();
    entry->borrowed = 0;
    int reclaim = entry->retired;
    snx_unlock();
    if (reclaim) snx_entry_free(entry);
}

static void snx_retire(JNIEnv *env, jlong handle) {
    if (handle == 0) return;
    if (handle < 0) {
        throw_java(env, "java/lang/IllegalStateException", "invalid native search handle");
        return;
    }
    uint64_t id = (uint64_t)handle;
    snx_lock();
    snx_entry **link = &snx_table[snx_bucket(id)];
    while (*link != NULL && (*link)->id != id) link = &(*link)->next;
    snx_entry *entry = *link;
    int reclaim = 0;
    if (entry != NULL) {
        *link = entry->next;
        entry->next = NULL;
        entry->retired = 1;
        reclaim = !entry->borrowed;
    }
    snx_unlock();
    if (entry == NULL) {
        throw_java(env, "java/lang/IllegalStateException", "invalid or closed native search handle");
    } else if (reclaim) {
        snx_entry_free(entry);
    }
}

static int bulk_monitor_init(
        JNIEnv *env, jobject callback, snx_bulk_monitor *monitor) {
    if (callback == NULL) {
        throw_java(env, "java/lang/NullPointerException", "monitor");
        return 0;
    }
    jclass type = (*env)->GetObjectClass(env, callback);
    if (type == NULL) return 0;
    jmethodID check_canceled = (*env)->GetMethodID(env, type, "checkCanceled", "()V");
    (*env)->DeleteLocalRef(env, type);
    if (check_canceled == NULL) return 0;
    monitor->env = env;
    monitor->callback = callback;
    monitor->check_canceled = check_canceled;
    monitor->remaining = SNX_BULK_POLL_WORK;
    return 1;
}

static int bulk_poll(snx_bulk_monitor *monitor) {
    if (monitor == NULL) return 1;
    JNIEnv *env = monitor->env;
    (*env)->CallVoidMethod(env, monitor->callback, monitor->check_canceled);
    if ((*env)->ExceptionCheck(env)) return 0;
    monitor->remaining = SNX_BULK_POLL_WORK;
    return 1;
}

/* Charge each outer iteration and each extra KMP fallback-loop iteration. */
static int bulk_step(snx_bulk_monitor *monitor) {
    return monitor == NULL || --monitor->remaining != 0U || bulk_poll(monitor);
}

/* One prefix table is shared by the count and bounded-position operations. */
static int ensure_prefix(
        JNIEnv *env, snx_pattern *pattern, snx_bulk_monitor *monitor) {
    if (pattern->prefix != NULL) return 1;
#if SIZE_MAX <= UINT32_MAX
    if (pattern->length > SIZE_MAX / sizeof(uint32_t)) {
        throw_java(env, "java/lang/OutOfMemoryError", "native prefix table is too large");
        return 0;
    }
#endif
    uint32_t *prefix = (uint32_t *)malloc((size_t)pattern->length * sizeof(uint32_t));
    if (prefix == NULL) {
        throw_java(env, "java/lang/OutOfMemoryError", "native prefix allocation failed");
        return 0;
    }
    prefix[0] = 0U;
    for (uint32_t index = 1U, matched = 0U; index < pattern->length; ++index) {
        if (!bulk_step(monitor)) {
            free(prefix);
            return 0;
        }
        while (matched > 0U && pattern->needle[index] != pattern->needle[matched]) {
            if (!bulk_step(monitor)) {
                free(prefix);
                return 0;
            }
            matched = prefix[matched - 1U];
        }
        if (pattern->needle[index] == pattern->needle[matched]) ++matched;
        prefix[index] = matched;
    }
    if (!bulk_poll(monitor)) {
        free(prefix);
        return 0;
    }
    pattern->prefix = prefix;
    return 1;
}

/* Monitored bulk operations read 4 KiB JNI regions, never retaining an
 * array-elements pointer while invoking the caller's cancellation callback.
 * Smaller unmonitored inputs retain their original contiguous fast scan. */
#define SNX_MONITORED_CHUNK 4096U
/* Reuse the existing 4 KiB GetByteArrayRegion/KMP owner for large
 * unmonitored scans. Smaller inputs retain the original contiguous
 * fast search to avoid repeated JNI region transitions. */
#define SNX_UNMONITORED_REGION_CHUNKS 64U
#define SNX_UNMONITORED_REGION_MIN_BYTES \
        (SNX_MONITORED_CHUNK * SNX_UNMONITORED_REGION_CHUNKS)

typedef struct {
    jint *positions;
    size_t count;
    size_t capacity;
    size_t limit;
    int collect;
} snx_monitored_hits;

static int snx_monitored_hit(
        JNIEnv *env, snx_monitored_hits *hits, uint32_t offset) {
    if (hits->count >= hits->limit) return 1;
    if (hits->collect) {
        if (hits->count == hits->capacity) {
            size_t next = hits->capacity == 0U ? 16U
                    : hits->capacity + hits->capacity / 2U + 1U;
            if (next > hits->limit) next = hits->limit;
            if (next > SIZE_MAX / sizeof(jint)) {
                throw_java(env, "java/lang/OutOfMemoryError", "monitored positions overflow");
                return 0;
            }
            jint *expanded = (jint *)realloc(hits->positions, next * sizeof(jint));
            if (expanded == NULL) {
                throw_java(env, "java/lang/OutOfMemoryError", "monitored positions allocation");
                return 0;
            }
            hits->positions = expanded;
            hits->capacity = next;
        }
        hits->positions[hits->count] = (jint)offset;
    }
    ++hits->count;
    return 1;
}

static int snx_scan_monitored(
        JNIEnv *env, const snx_pattern *pattern, jbyteArray haystack,
        uint32_t begin, uint32_t end, snx_bulk_monitor *monitor,
        snx_monitored_hits *hits) {
    uint8_t chunk[SNX_MONITORED_CHUNK];
    uint32_t matched = 0U;
    for (uint32_t offset = begin; offset < end;) {
        uint32_t available = end - offset;
        jsize width = (jsize)(available < SNX_MONITORED_CHUNK
                ? available : SNX_MONITORED_CHUNK);
        (*env)->GetByteArrayRegion(env, haystack, (jsize)offset, width,
                (jbyte *)chunk);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jsize i = 0; i < width; ++i) {
            if (!bulk_step(monitor)) return 0;
            uint8_t value = chunk[i];
            while (matched > 0U && value != pattern->needle[matched]) {
                if (!bulk_step(monitor)) return 0;
                matched = pattern->prefix[matched - 1U];
            }
            if (value == pattern->needle[matched]) ++matched;
            if (matched == pattern->length) {
                uint32_t position = offset + (uint32_t)i + 1U - pattern->length;
                if (!snx_monitored_hit(env, hits, position)) return 0;
                if (hits->count == hits->limit) return bulk_poll(monitor);
                matched = pattern->prefix[matched - 1U];
            }
        }
        offset += (uint32_t)width;
    }
    return bulk_poll(monitor);
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_nativeAbi(
        JNIEnv *env, jclass owner) {
    (void)env;
    (void)owner;
    return SNX_JNI_ABI;
}

/* Callback-free discovery: ABI-1 libraries predating monitored bulk scans omit this symbol. */
JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_monitoredBulkNativeAbi(
        JNIEnv *env, jclass owner) {
    (void)env;
    (void)owner;
    return 1;
}

JNIEXPORT jlong JNICALL Java_jdk_m3_search_M3JniSearchEngine_compileNative(
        JNIEnv *env, jclass owner, jbyteArray needle_array) {
    (void)owner;
    if (needle_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "needle");
        return 0;
    }
    jsize length = (*env)->GetArrayLength(env, needle_array);
    if (length <= 0) {
        throw_java(env, "java/lang/IllegalArgumentException", "needle must not be empty");
        return 0;
    }

    snx_pattern *pattern = (snx_pattern *)calloc(1U, sizeof(*pattern));
    if (pattern == NULL) {
        throw_java(env, "java/lang/OutOfMemoryError", "native pattern allocation failed");
        return 0;
    }

    pattern->length = (uint32_t)length;
    pattern->needle = (uint8_t *)malloc((size_t)length);
    if (pattern->needle == NULL) {
        free(pattern);
        throw_java(env, "java/lang/OutOfMemoryError", "native needle allocation failed");
        return 0;
    }

    (*env)->GetByteArrayRegion(
            env, needle_array, 0, length, (jbyte *)pattern->needle);
    if ((*env)->ExceptionCheck(env)) {
        free(pattern->needle);
        free(pattern);
        return 0;
    }

    for (uint32_t index = 0U; index < 256U; ++index) {
        pattern->shift[index] = (uint32_t)length;
        pattern->reverse_shift[index] = (uint32_t)length;
    }
    for (uint32_t index = 0U; index + 1U < (uint32_t)length; ++index) {
        pattern->shift[pattern->needle[index]] =
                (uint32_t)length - 1U - index;
    }
    for (uint32_t index = (uint32_t)length - 1U; index > 0U; --index) {
        pattern->reverse_shift[pattern->needle[index]] = index;
    }
    return snx_register(env, pattern);
}

/* Linear fallback over a bounded range; retain the existing prefix owner and lifetime. */
static jint search_kmp(
        const snx_pattern *pattern, const uint8_t *haystack,
        uint32_t from_index, uint32_t limit, int retain_last) {
    uint32_t matched = 0U;
    jint result = -1;
    for (uint32_t index = from_index; index < limit; ++index) {
        uint8_t value = haystack[index];
        while (matched > 0U && value != pattern->needle[matched]) {
            matched = pattern->prefix[matched - 1U];
        }
        if (value == pattern->needle[matched]) ++matched;
        if (matched == pattern->length) {
            result = (jint)(index + 1U - pattern->length);
            if (!retain_last) return result;
            matched = pattern->prefix[matched - 1U];
        }
    }
    return result;
}

static jint search_forward(
        JNIEnv *env, snx_pattern *pattern, const uint8_t *haystack,
        uint32_t haystack_length, jint from_index) {
    if (from_index < 0) from_index = 0;
    if ((uint32_t)from_index > haystack_length
            || pattern->length > haystack_length - (uint32_t)from_index) return -1;
    /* C11 single-byte fast path: preserve the same clamped first-hit offset.
     * No new JNI symbol, table, heap owner or platform-specific memrchr required. */
    if (pattern->length == 1U) {
        const uint8_t *found = (const uint8_t *)memchr(
                haystack + (uint32_t)from_index, pattern->needle[0],
                (size_t)(haystack_length - (uint32_t)from_index));
        return found == NULL ? -1 : (jint)(found - haystack);
    }
    /* Two-byte sparse first-hit lane: after repeated nearby failed first-byte
     * candidates, resume the existing BMH/KMP search at the next unchecked
     * start. All JNI signatures, memory ownership and match offsets remain
     * unchanged. Sampling remains bounded to 64 bytes. */
    uint32_t position = (uint32_t)from_index;
    if (pattern->length == 2U
            && haystack_length - (uint32_t)from_index >= 4096U
            && pattern->needle[0] != pattern->needle[1]) {
        uint32_t first_hits = 0U;
        const uint32_t start = (uint32_t)from_index;
        for (uint32_t index = 0U; index < 64U; ++index) {
            first_hits += (uint32_t)(haystack[start + index] == pattern->needle[0]);
            if (first_hits > 4U) break;
        }
        if (first_hits <= 4U) {
            const uint8_t *cursor = haystack + start;
            const uint8_t *end = haystack + haystack_length - 1U;
            const uint8_t *window_start = cursor;
            uint32_t window_hits = 0U;
            while (cursor < end) {
                const uint8_t *found = (const uint8_t *)memchr(
                        cursor, pattern->needle[0], (size_t)(end - cursor));
                if (found == NULL) return -1;
                if (found[1] == pattern->needle[1]) return (jint)(found - haystack);
                cursor = found + 1;
                if (++window_hits == 8U) {
                    if ((size_t)(cursor - window_start) < 512U) {
                        position = (uint32_t)(cursor - haystack);
                        break;
                    }
                    window_start = cursor;
                    window_hits = 0U;
                }
            }
            if (cursor >= end) return -1;
        }
    }
    uint32_t maximum = haystack_length - pattern->length;
    uint32_t last = pattern->length - 1U;
    uint64_t compared = 0U;
    uint64_t budget = 4U * (uint64_t)(haystack_length - position);
    while (position <= maximum) {
        uint8_t tail = haystack[position + last];
        if (tail == pattern->needle[last]) {
            /* A conservative work charge avoids changing the optimized memcmp fast path. */
            compared += pattern->length;
            if (memcmp(haystack + position, pattern->needle, (size_t)last) == 0) {
                return (jint)position;
            }
        }
        uint32_t step = pattern->shift[tail];
        position += step == 0U ? 1U : step;
        if (compared > budget && position <= maximum) {
            if (!ensure_prefix(env, pattern, NULL)) return -1;
            return search_kmp(pattern, haystack, position, haystack_length, 0);
        }
    }
    return -1;
}

static jint search_reverse(
        JNIEnv *env, snx_pattern *pattern, const uint8_t *haystack,
        uint32_t haystack_length, jint from_index) {
    if (from_index < 0 || pattern->length > haystack_length) return -1;
    uint32_t maximum = haystack_length - pattern->length;
    uint32_t position =
            (uint32_t)from_index > maximum ? maximum : (uint32_t)from_index;
    /* Portable C11 one-octet reverse search. The original guards bound position.
     * No JNI symbol, native handle owner, direct-buffer range or BMH multi-byte
     * behavior is changed; no non-standard memrchr dependency is introduced. */
    if (pattern->length == 1U) {
        const uint8_t wanted = pattern->needle[0];
        for (;;) {
            if (haystack[position] == wanted) return (jint)position;
            if (position == 0U) return -1;
            --position;
        }
    }
    uint64_t compared = 0U;
    uint64_t budget = 4U * ((uint64_t)position + pattern->length);
    for (;;) {
        compared += pattern->length;
        if (memcmp(haystack + position, pattern->needle,
                (size_t)pattern->length) == 0) return (jint)position;
        uint32_t step = pattern->reverse_shift[haystack[position]];
        if (position < step) break;
        position -= step;
        if (compared > budget) {
            if (!ensure_prefix(env, pattern, NULL)) return -1;
            /* All later starts were excluded by reverse BMH. A forward KMP pass
             * finds the final remaining occurrence without a second prefix table. */
            return search_kmp(pattern, haystack, 0U, position + pattern->length, 1);
        }
    }
    return -1;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_indexOfNative(
        JNIEnv *env, jclass owner, jlong handle, jbyteArray haystack_array, jint from_index) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return -1;
    jint result = -1;
    if (haystack_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "haystack");
    } else {
        jsize length = (*env)->GetArrayLength(env, haystack_array);
        jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
        if (haystack != NULL) {
            result = search_forward(env, lease->pattern,
                    (const uint8_t *)haystack, (uint32_t)length, from_index);
            (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
        }
    }
    snx_end(lease);
    return result;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_lastIndexOfNative(
        JNIEnv *env, jclass owner, jlong handle, jbyteArray haystack_array, jint from_index) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return -1;
    jint result = -1;
    if (haystack_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "haystack");
    } else {
        jsize length = (*env)->GetArrayLength(env, haystack_array);
        jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
        if (haystack != NULL) {
            result = search_reverse(env, lease->pattern,
                    (const uint8_t *)haystack, (uint32_t)length, from_index);
            (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
        }
    }
    snx_end(lease);
    return result;
}

static const uint8_t *direct_range(
        JNIEnv *env, jobject buffer, jint offset, jint length) {
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (address == NULL || capacity < 0 || offset < 0 || length < 0
            || (jlong)offset + (jlong)length > capacity) return NULL;
    return (const uint8_t *)address + offset;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_indexOfDirectNative(
        JNIEnv *env, jclass owner, jlong handle, jobject haystack_buffer,
        jint offset, jint length, jint from_index) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return INT32_MIN;
    jint result = INT32_MIN;
    if (haystack_buffer != NULL) {
        const uint8_t *haystack = direct_range(env, haystack_buffer, offset, length);
        if (haystack != NULL) {
            result = search_forward(env, lease->pattern, haystack,
                    (uint32_t)length, from_index);
        }
    }
    snx_end(lease);
    return result;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_lastIndexOfDirectNative(
        JNIEnv *env, jclass owner, jlong handle, jobject haystack_buffer,
        jint offset, jint length, jint from_index) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return INT32_MIN;
    jint result = INT32_MIN;
    if (haystack_buffer != NULL) {
        const uint8_t *haystack = direct_range(env, haystack_buffer, offset, length);
        if (haystack != NULL) {
            result = search_reverse(env, lease->pattern, haystack,
                    (uint32_t)length, from_index);
        }
    }
    snx_end(lease);
    return result;
}

static jint count_overlapping(
        JNIEnv *env, snx_pattern *pattern, jbyteArray haystack_array,
        jint from_index, snx_bulk_monitor *monitor) {
    if (haystack_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "haystack");
        return -1;
    }
    jsize haystack_length = (*env)->GetArrayLength(env, haystack_array);
    if (from_index < 0) from_index = 0;
    if (from_index > haystack_length
            || (jsize)pattern->length > haystack_length - from_index) return 0;

    if (!bulk_poll(monitor) || !ensure_prefix(env, pattern, monitor)) return -1;
    if (monitor != NULL
            || (uint32_t)(haystack_length - from_index)
                    >= SNX_UNMONITORED_REGION_MIN_BYTES) {
        /* The same region scanner is valid with a NULL monitor. This avoids
         * pinning or copying the entire large Java byte[] in bulk counting. */
        snx_monitored_hits hits = {NULL, 0U, 0U, (size_t)INT32_MAX, 0};
        if (!snx_scan_monitored(env, pattern, haystack_array,
                (uint32_t)from_index, (uint32_t)haystack_length, monitor, &hits)) {
            return -1;
        }
        return (jint)hits.count;
    }

    if (monitor != NULL
            || (uint32_t)(haystack_length - from_index)
                    >= SNX_UNMONITORED_REGION_MIN_BYTES) {
        /* The same region scanner is valid with a NULL monitor. This avoids
         * pinning or copying the entire large Java byte[] in bulk counting. */
        snx_monitored_hits hits = {NULL, 0U, 0U, (size_t)INT32_MAX, 0};
        if (!snx_scan_monitored(env, pattern, haystack_array,
                (uint32_t)from_index, (uint32_t)haystack_length, monitor, &hits)) {
            return -1;
        }
        return (jint)hits.count;
    }
    /* Retain prefix setup and monitored cancellation semantics. For long
     * unmonitored one-byte scans, find sparse hits using bounded libc memchr.
     * Switch to scalar counting after eight dense hits in a 512-byte window. */
    if (monitor == NULL && pattern->length == 1U
            && (uint32_t)(haystack_length - from_index) >= 4096U) {
        jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
        if (haystack == NULL) return -1;
        const uint8_t *cursor = (const uint8_t *)haystack + (uint32_t)from_index;
        const uint8_t *end = (const uint8_t *)haystack + (uint32_t)haystack_length;
        const uint8_t *window_start = cursor;
        uint32_t window_hits = 0U;
        uint32_t count = 0U;
        while (cursor < end) {
            const uint8_t *match = (const uint8_t *)memchr(
                    cursor, pattern->needle[0], (size_t)(end - cursor));
            if (match == NULL) break;
            ++count;
            cursor = match + 1;
            if (++window_hits == 8U) {
                if ((size_t)(cursor - window_start) < 512U) {
                    for (; cursor < end; ++cursor) {
                        count += (uint32_t)(*cursor == pattern->needle[0]);
                    }
                    break;
                }
                window_start = cursor;
                window_hits = 0U;
            }
        }
        (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
        return (jint)count;
    }

    jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
    if (haystack == NULL) return -1;
    uint32_t matched = 0U;
    uint32_t count = 0U;
    for (uint32_t index = (uint32_t)from_index;
            index < (uint32_t)haystack_length; ++index) {
        if (!bulk_step(monitor)) goto count_done;
        uint8_t value = (uint8_t)haystack[index];
        while (matched > 0U && value != pattern->needle[matched]) {
            if (!bulk_step(monitor)) goto count_done;
            matched = pattern->prefix[matched - 1U];
        }
        if (value == pattern->needle[matched]) ++matched;
        if (matched == pattern->length) {
            ++count;
            matched = pattern->prefix[matched - 1U];
        }
    }
count_done:
    (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
    if ((*env)->ExceptionCheck(env) || !bulk_poll(monitor)) return -1;
    return (jint)count;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_countOverlappingNative(
        JNIEnv *env, jclass owner, jlong handle,
        jbyteArray haystack_array, jint from_index) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return -1;
    jint result = count_overlapping(env, lease->pattern, haystack_array, from_index, NULL);
    snx_end(lease);
    return result;
}

JNIEXPORT jint JNICALL Java_jdk_m3_search_M3JniSearchEngine_countOverlappingMonitoredNative(
        JNIEnv *env, jclass owner, jlong handle,
        jbyteArray haystack_array, jint from_index, jobject callback) {
    (void)owner;
    snx_bulk_monitor monitor;
    if (!bulk_monitor_init(env, callback, &monitor)) return -1;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return -1;
    jint result = count_overlapping(env, lease->pattern, haystack_array, from_index, &monitor);
    snx_end(lease);
    return result;
}

static jintArray positions_overlapping(
        JNIEnv *env, snx_pattern *pattern, jbyteArray haystack_array,
        jint from_index, jint max_matches, snx_bulk_monitor *monitor) {
    if (haystack_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "haystack");
        return NULL;
    }
    if (max_matches < 0) {
        throw_java(env, "java/lang/IllegalArgumentException", "maxMatches must be nonnegative");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, haystack_array);
    if (from_index < 0) from_index = 0;
    if (max_matches == 0 || from_index > length
            || (jsize)pattern->length > length - from_index) {
        return (*env)->NewIntArray(env, 0);
    }
    if (!bulk_poll(monitor) || !ensure_prefix(env, pattern, monitor)) return NULL;
    if (monitor != NULL
            || (uint32_t)(length - from_index)
                    >= SNX_UNMONITORED_REGION_MIN_BYTES) {
        /* Reuse the tested monitored scan with no callback: one 4 KiB JNI
         * region per step and only the bounded result-position allocation. */
        snx_monitored_hits hits = {
                NULL, 0U, 0U, (size_t)max_matches, 1};
        if (!snx_scan_monitored(env, pattern, haystack_array,
                (uint32_t)from_index, (uint32_t)length, monitor, &hits)) {
            free(hits.positions);
            return NULL;
        }
        jintArray result = (*env)->NewIntArray(env, (jsize)hits.count);
        if (result != NULL && hits.count != 0U) {
            (*env)->SetIntArrayRegion(env, result, 0,
                    (jsize)hits.count, hits.positions);
        }
        free(hits.positions);
        return result;
    }
    jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
    if (haystack == NULL) return NULL;

    jint *positions = NULL;
    size_t capacity = 0U;
    size_t count = 0U;
    uint32_t matched = 0U;
    uint32_t scalar_from = (uint32_t)from_index;
    /* Count and positions share a sparse single-byte threshold. Keep monitored
     * scans on the existing checkpointed KMP loop. Hand dense scans back to
     * that unchanged scalar loop immediately after eight hits within 512 bytes. */
    if (monitor == NULL && pattern->length == 1U
            && (uint32_t)(length - from_index) >= 4096U) {
        const uint8_t *cursor = (const uint8_t *)haystack + scalar_from;
        const uint8_t *end = (const uint8_t *)haystack + (uint32_t)length;
        const uint8_t *window_start = cursor;
        uint32_t window_hits = 0U;
        for (;;) {
            const uint8_t *found = (const uint8_t *)memchr(
                    cursor, pattern->needle[0], (size_t)(end - cursor));
            if (found == NULL) goto positions_done;
            if (count == capacity) {
                size_t ceiling = (size_t)max_matches;
                size_t next = capacity == 0U ? (ceiling < 16U ? ceiling : 16U)
                        : capacity + capacity / 2U + 1U;
                if (next > ceiling) next = ceiling;
                if (next > SIZE_MAX / sizeof(jint)) {
                    throw_java(env, "java/lang/OutOfMemoryError", "native positions too large");
                    goto positions_done;
                }
                jint *expanded = (jint *)realloc(positions, next * sizeof(jint));
                if (expanded == NULL) {
                    throw_java(env, "java/lang/OutOfMemoryError", "native positions allocation failed");
                    goto positions_done;
                }
                positions = expanded;
                capacity = next;
            }
            positions[count++] = (jint)(found - (const uint8_t *)haystack);
            if (count == (size_t)max_matches) goto positions_done;
            cursor = found + 1;
            if (++window_hits == 8U) {
                if ((size_t)(cursor - window_start) < 512U) {
                    scalar_from = (uint32_t)(cursor - (const uint8_t *)haystack);
                    break;
                }
                window_start = cursor;
                window_hits = 0U;
            }
        }
    }
    for (uint32_t index = scalar_from; index < (uint32_t)length; ++index) {
        if (!bulk_step(monitor)) goto positions_done;
        uint8_t value = (uint8_t)haystack[index];
        while (matched > 0U && value != pattern->needle[matched]) {
            if (!bulk_step(monitor)) goto positions_done;
            matched = pattern->prefix[matched - 1U];
        }
        if (value == pattern->needle[matched]) ++matched;
        if (matched == pattern->length) {
            if (count == capacity) {
                size_t ceiling = (size_t)max_matches;
                size_t next = capacity == 0U ? (ceiling < 16U ? ceiling : 16U)
                        : capacity + capacity / 2U + 1U;
                if (next > ceiling) next = ceiling;
                if (next > SIZE_MAX / sizeof(jint)) {
                    throw_java(env, "java/lang/OutOfMemoryError", "native positions too large");
                    break;
                }
                jint *expanded = (jint *)realloc(positions, next * sizeof(jint));
                if (expanded == NULL) {
                    throw_java(env, "java/lang/OutOfMemoryError", "native positions allocation failed");
                    break;
                }
                positions = expanded;
                capacity = next;
            }
            positions[count++] = (jint)(index + 1U - pattern->length);
            if (count == (size_t)max_matches) break;
            matched = pattern->prefix[matched - 1U];
        }
    }
positions_done:
    (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
    if ((*env)->ExceptionCheck(env) || !bulk_poll(monitor)) {
        free(positions);
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, (jsize)count);
    if (result != NULL && count > 0U) {
        (*env)->SetIntArrayRegion(env, result, 0, (jsize)count, positions);
    }
    free(positions);
    return result;
}

/* ABI-1 additive symbol: return only the requested prefix of overlapping byte offsets. */
JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3JniSearchEngine_positionsOverlappingNative(
        JNIEnv *env, jclass owner, jlong handle, jbyteArray haystack_array,
        jint from_index, jint max_matches) {
    (void)owner;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return NULL;
    jintArray result = positions_overlapping(env, lease->pattern,
            haystack_array, from_index, max_matches, NULL);
    snx_end(lease);
    return result;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3JniSearchEngine_positionsOverlappingMonitoredNative(
        JNIEnv *env, jclass owner, jlong handle, jbyteArray haystack_array,
        jint from_index, jint max_matches, jobject callback) {
    (void)owner;
    snx_bulk_monitor monitor;
    if (!bulk_monitor_init(env, callback, &monitor)) return NULL;
    snx_entry *lease = snx_borrow(env, handle);
    if (lease == NULL) return NULL;
    jintArray result = positions_overlapping(env, lease->pattern,
            haystack_array, from_index, max_matches, &monitor);
    snx_end(lease);
    return result;
}

JNIEXPORT void JNICALL Java_jdk_m3_search_M3JniSearchEngine_releaseNative(
        JNIEnv *env, jclass owner, jlong handle) {
    (void)owner;
    snx_retire(env, handle);
}

/* M3 batch-first occurrence superset: source-sealed C11/JNI atom. */
/* Separate additive native atom; no change to existing pattern ABI or exported JNI symbols. */
typedef struct {
    jint start;
    jint ordinal;
} m3_batch_request;

static void m3_throw(JNIEnv *env, const char *class_name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass cls = (*env)->FindClass(env, class_name);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static int m3_compare_request(const void *left, const void *right) {
    const m3_batch_request *a = (const m3_batch_request *)left;
    const m3_batch_request *b = (const m3_batch_request *)right;
    if (a->start != b->start) return a->start < b->start ? -1 : 1;
    if (a->ordinal != b->ordinal) return a->ordinal < b->ordinal ? -1 : 1;
    return 0;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3BatchOffsetSearch_indexOfManyNative(
        JNIEnv *env, jclass owner, jbyteArray haystack_array,
        jbyteArray needle_array, jintArray from_array) {
    (void)owner;
    if (haystack_array == NULL || needle_array == NULL || from_array == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "batch search input");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, haystack_array);
    jsize m = (*env)->GetArrayLength(env, needle_array);
    jsize q = (*env)->GetArrayLength(env, from_array);
    if (m == 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "needle must not be empty");
        return NULL;
    }
    if (q == 0) return (*env)->NewIntArray(env, 0);
    if ((size_t)q > SIZE_MAX / sizeof(jint)) {
        m3_throw(env, "java/lang/OutOfMemoryError", "batch results exceed address space");
        return NULL;
    }

    jint *results = (jint *)malloc((size_t)q * sizeof(jint));
    if (results == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "batch results allocation failed");
        return NULL;
    }
    for (jsize i = 0; i < q; ++i) results[i] = -1;
    m3_batch_request *requests = NULL;
    uint32_t *prefix = NULL;
    jint *from = NULL;
    jbyte *needle = NULL;
    jbyte *haystack = NULL;
    jintArray output = NULL;

    if (m <= n) {
        if ((size_t)q > SIZE_MAX / sizeof(*requests)
                || (size_t)m > SIZE_MAX / sizeof(*prefix)) {
            m3_throw(env, "java/lang/OutOfMemoryError", "batch scratch exceeds address space");
            goto cleanup;
        }
        requests = (m3_batch_request *)malloc((size_t)q * sizeof(*requests));
        prefix = (uint32_t *)malloc((size_t)m * sizeof(*prefix));
        if (requests == NULL || prefix == NULL) {
            m3_throw(env, "java/lang/OutOfMemoryError", "batch scratch allocation failed");
            goto cleanup;
        }
        from = (*env)->GetIntArrayElements(env, from_array, NULL);
        if (from == NULL) goto cleanup;
        for (jsize i = 0; i < q; ++i) {
            requests[i].start = from[i] < 0 ? 0 : from[i];
            requests[i].ordinal = i;
        }
        (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
        from = NULL;
        qsort(requests, (size_t)q, sizeof(*requests), m3_compare_request);

        needle = (*env)->GetByteArrayElements(env, needle_array, NULL);
        if (needle == NULL) goto cleanup;
        haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
        if (haystack == NULL) goto cleanup;

        prefix[0] = 0U;
        uint32_t prefixed = 0U;
        for (jsize i = 1; i < m; ++i) {
            uint8_t value = (uint8_t)needle[i];
            while (prefixed > 0U && value != (uint8_t)needle[prefixed]) {
                prefixed = prefix[prefixed - 1U];
            }
            if (value == (uint8_t)needle[prefixed]) ++prefixed;
            prefix[i] = prefixed;
        }

        uint32_t matched = 0U;
        jsize next = 0;
        for (jsize i = 0; i < n && next < q; ++i) {
            uint8_t value = (uint8_t)haystack[i];
            while (matched > 0U && value != (uint8_t)needle[matched]) {
                matched = prefix[matched - 1U];
            }
            if (value == (uint8_t)needle[matched]) ++matched;
            if (matched == (uint32_t)m) {
                jint at = i + 1 - m;
                while (next < q && requests[next].start <= at) {
                    results[requests[next].ordinal] = at;
                    ++next;
                }
                matched = prefix[matched - 1U];
            }
        }
    }

    output = (*env)->NewIntArray(env, q);
    if (output != NULL) {
        (*env)->SetIntArrayRegion(env, output, 0, q, results);
        if ((*env)->ExceptionCheck(env)) output = NULL;
    }
cleanup:
    if (haystack != NULL) (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
    if (needle != NULL) (*env)->ReleaseByteArrayElements(env, needle_array, needle, JNI_ABORT);
    if (from != NULL) (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
    free(prefix);
    free(requests);
    free(results);
    return output;
}

/* Additive direct-buffer batch path. Existing array JNI symbol and ABI remain unchanged. */
typedef struct {
    jint start;
    jint ordinal;
} m3_direct_query;

static void m3_direct_throw(JNIEnv *env, const char *klass, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, klass);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static int m3_direct_query_cmp(const void *left, const void *right) {
    const m3_direct_query *a = (const m3_direct_query *)left;
    const m3_direct_query *b = (const m3_direct_query *)right;
    if (a->start != b->start) return a->start < b->start ? -1 : 1;
    if (a->ordinal != b->ordinal) return a->ordinal < b->ordinal ? -1 : 1;
    return 0;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3BatchOffsetSearch_indexOfManyDirectNative(
        JNIEnv *env, jclass owner, jobject haystack_buffer, jint position, jint limit,
        jbyteArray needle_array, jintArray from_array) {
    (void)owner;
    if (haystack_buffer == NULL || needle_array == NULL || from_array == NULL) {
        m3_direct_throw(env, "java/lang/NullPointerException", "direct batch input");
        return NULL;
    }
    void *buffer = (*env)->GetDirectBufferAddress(env, haystack_buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, haystack_buffer);
    /* Null with no pending exception is an explicit unsupported-buffer signal to Java AUTO. */
    if (buffer == NULL || capacity < 0) return NULL;
    if (position < 0 || limit < position || (jlong)limit > capacity) {
        m3_direct_throw(env, "java/lang/IllegalArgumentException", "invalid direct byte window");
        return NULL;
    }
    jsize n = limit - position;
    jsize m = (*env)->GetArrayLength(env, needle_array);
    jsize q = (*env)->GetArrayLength(env, from_array);
    if (m == 0) {
        m3_direct_throw(env, "java/lang/IllegalArgumentException", "needle must not be empty");
        return NULL;
    }
    if (q == 0) return (*env)->NewIntArray(env, 0);
    if ((size_t)q > SIZE_MAX / sizeof(jint)) {
        m3_direct_throw(env, "java/lang/OutOfMemoryError", "batch output exceeds address space");
        return NULL;
    }
    jint *result = (jint *)malloc((size_t)q * sizeof(jint));
    if (result == NULL) {
        m3_direct_throw(env, "java/lang/OutOfMemoryError", "batch output allocation failed");
        return NULL;
    }
    for (jsize i = 0; i < q; ++i) result[i] = -1;
    m3_direct_query *requests = NULL;
    uint32_t *prefix = NULL;
    jint *from = NULL;
    jbyte *needle = NULL;
    jintArray output = NULL;
    if (m <= n) {
        if ((size_t)q > SIZE_MAX / sizeof(*requests)
                || (size_t)m > SIZE_MAX / sizeof(*prefix)) {
            m3_direct_throw(env, "java/lang/OutOfMemoryError", "batch scratch exceeds address space");
            goto cleanup;
        }
        requests = (m3_direct_query *)malloc((size_t)q * sizeof(*requests));
        prefix = (uint32_t *)malloc((size_t)m * sizeof(*prefix));
        if (requests == NULL || prefix == NULL) {
            m3_direct_throw(env, "java/lang/OutOfMemoryError", "batch scratch allocation failed");
            goto cleanup;
        }
        from = (*env)->GetIntArrayElements(env, from_array, NULL);
        if (from == NULL) goto cleanup;
        for (jsize i = 0; i < q; ++i) {
            requests[i].start = from[i] < 0 ? 0 : from[i];
            requests[i].ordinal = i;
        }
        (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
        from = NULL;
        qsort(requests, (size_t)q, sizeof(*requests), m3_direct_query_cmp);

        needle = (*env)->GetByteArrayElements(env, needle_array, NULL);
        if (needle == NULL) goto cleanup;
        prefix[0] = 0U;
        uint32_t prepared = 0U;
        for (jsize i = 1; i < m; ++i) {
            uint8_t value = (uint8_t)needle[i];
            while (prepared > 0U && value != (uint8_t)needle[prepared]) {
                prepared = prefix[prepared - 1U];
            }
            if (value == (uint8_t)needle[prepared]) ++prepared;
            prefix[i] = prepared;
        }

        const uint8_t *bytes = (const uint8_t *)buffer + (size_t)position;
        uint32_t matched = 0U;
        jsize next = 0;
        for (jsize i = 0; i < n && next < q; ++i) {
            uint8_t value = bytes[i];
            while (matched > 0U && value != (uint8_t)needle[matched]) {
                matched = prefix[matched - 1U];
            }
            if (value == (uint8_t)needle[matched]) ++matched;
            if (matched == (uint32_t)m) {
                jint at = i + 1 - m;
                while (next < q && requests[next].start <= at) {
                    result[requests[next].ordinal] = at;
                    ++next;
                }
                matched = prefix[matched - 1U];
            }
        }
    }

    output = (*env)->NewIntArray(env, q);
    if (output != NULL) {
        (*env)->SetIntArrayRegion(env, output, 0, q, result);
        if ((*env)->ExceptionCheck(env)) output = NULL;
    }
cleanup:
    if (needle != NULL) (*env)->ReleaseByteArrayElements(env, needle_array, needle, JNI_ABORT);
    if (from != NULL) (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
    free(prefix);
    free(requests);
    free(result);
    return output;
}

// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/native/jni_search.c (blob e1e23542974e) by M3_SEARCH_RELOCATE v2.
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>

/* Opt-in reversed KMP batch atom. No existing native function or handle is modified. */
typedef struct {
    jint bound;
    jint ordinal;
} m3_reverse_query;

static void m3_reverse_throw(JNIEnv *env, const char *type, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static int m3_reverse_compare(const void *left, const void *right) {
    const m3_reverse_query *a = (const m3_reverse_query *)left;
    const m3_reverse_query *b = (const m3_reverse_query *)right;
    if (a->bound != b->bound) return a->bound < b->bound ? -1 : 1;
    if (a->ordinal != b->ordinal) return a->ordinal < b->ordinal ? -1 : 1;
    return 0;
}

/* Input byte window is borrowed for this call only; no Java pointer escapes. */
static jintArray m3_reverse_core(JNIEnv *env, const uint8_t *bytes, jsize n,
        jbyteArray needle_array, jintArray from_array) {
    jsize m = (*env)->GetArrayLength(env, needle_array);
    jsize q = (*env)->GetArrayLength(env, from_array);
    if (m == 0) {
        m3_reverse_throw(env, "java/lang/IllegalArgumentException", "needle must not be empty");
        return NULL;
    }
    if (q == 0) return (*env)->NewIntArray(env, 0);
    if ((size_t)q > SIZE_MAX / sizeof(jint)) {
        m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse results too large");
        return NULL;
    }
    jintArray output = NULL;
    jint *result = (jint *)malloc((size_t)q * sizeof(*result));
    if (result == NULL) {
        m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse result allocation failed");
        return NULL;
    }
    for (jsize i = 0; i < q; ++i) result[i] = -1;
    m3_reverse_query *requests = NULL;
    uint32_t *prefix = NULL;
    jint *from = NULL;
    jbyte *needle = NULL;

    if (m <= n) {
        if ((size_t)q > SIZE_MAX / sizeof(*requests)
                || (size_t)m > SIZE_MAX / sizeof(*prefix)) {
            m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse scratch too large");
            goto cleanup;
        }
        requests = (m3_reverse_query *)malloc((size_t)q * sizeof(*requests));
        prefix = (uint32_t *)malloc((size_t)m * sizeof(*prefix));
        if (requests == NULL || prefix == NULL) {
            m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse scratch allocation failed");
            goto cleanup;
        }
        from = (*env)->GetIntArrayElements(env, from_array, NULL);
        if (from == NULL) {
            m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse offsets unavailable");
            goto cleanup;
        }
        int already_ordered = 1;
        for (jsize i = 0; i < q; ++i) {
            jint bound = from[i];
            jint maximum = n - m;
            requests[i].bound = bound < maximum ? bound : maximum;
            requests[i].ordinal = i;
            if (i > 0 && requests[i].bound < requests[i - 1].bound) {
                already_ordered = 0;
            }
        }
        (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
        from = NULL;
        /* Already ordered bounds (including duplicates) need no qsort. */
        if (!already_ordered) qsort(requests, (size_t)q, sizeof(*requests), m3_reverse_compare);
        if (requests[q - 1].bound >= 0) {
            needle = (*env)->GetByteArrayElements(env, needle_array, NULL);
            if (needle == NULL) {
                m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse needle unavailable");
                goto cleanup;
            }
            prefix[0] = 0U;
            uint32_t built = 0U;
            for (jsize i = 1; i < m; ++i) {
                uint8_t value = (uint8_t)needle[m - 1 - i];
                while (built > 0U && value != (uint8_t)needle[m - 1 - (jsize)built]) {
                    built = prefix[built - 1U];
                }
                if (value == (uint8_t)needle[m - 1 - (jsize)built]) ++built;
                prefix[i] = built;
            }
            uint32_t matched = 0U;
            jsize next = q - 1;
            for (jsize i = n - 1; i >= 0 && next >= 0 && requests[next].bound >= 0; --i) {
                uint8_t value = bytes[i];
                while (matched > 0U && value != (uint8_t)needle[m - 1 - (jsize)matched]) {
                    matched = prefix[matched - 1U];
                }
                if (value == (uint8_t)needle[m - 1 - (jsize)matched]) ++matched;
                if (matched == (uint32_t)m) {
                    while (next >= 0 && requests[next].bound >= i) {
                        result[requests[next].ordinal] = i;
                        --next;
                    }
                    matched = prefix[matched - 1U];
                }
            }
        }
    }
    output = (*env)->NewIntArray(env, q);
    if (output != NULL) {
        (*env)->SetIntArrayRegion(env, output, 0, q, result);
        if ((*env)->ExceptionCheck(env)) output = NULL;
    }
cleanup:
    if (needle != NULL) (*env)->ReleaseByteArrayElements(env, needle_array, needle, JNI_ABORT);
    if (from != NULL) (*env)->ReleaseIntArrayElements(env, from_array, from, JNI_ABORT);
    free(prefix);
    free(requests);
    free(result);
    return output;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3BatchReverseSearch_lastIndexOfManyNative(
        JNIEnv *env, jclass owner, jbyteArray haystack_array, jbyteArray needle_array,
        jintArray from_array) {
    (void)owner;
    if (haystack_array == NULL || needle_array == NULL || from_array == NULL) {
        m3_reverse_throw(env, "java/lang/NullPointerException", "batch reverse input");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, haystack_array);
    jsize m = (*env)->GetArrayLength(env, needle_array);
    jsize q = (*env)->GetArrayLength(env, from_array);
    if (m == 0 || m > n || q == 0) return m3_reverse_core(env, NULL, n, needle_array, from_array);
    jbyte *haystack = (*env)->GetByteArrayElements(env, haystack_array, NULL);
    if (haystack == NULL) {
        m3_reverse_throw(env, "java/lang/OutOfMemoryError", "batch reverse haystack unavailable");
        return NULL;
    }
    jintArray result = m3_reverse_core(env, (const uint8_t *)haystack, n, needle_array, from_array);
    (*env)->ReleaseByteArrayElements(env, haystack_array, haystack, JNI_ABORT);
    return result;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_search_M3BatchReverseSearch_lastIndexOfManyDirectNative(
        JNIEnv *env, jclass owner, jobject haystack_buffer, jint position, jint limit,
        jbyteArray needle_array, jintArray from_array) {
    (void)owner;
    if (haystack_buffer == NULL || needle_array == NULL || from_array == NULL) {
        m3_reverse_throw(env, "java/lang/NullPointerException", "direct reverse input");
        return NULL;
    }
    uint8_t *base = (uint8_t *)(*env)->GetDirectBufferAddress(env, haystack_buffer);
    jlong capacity = (*env)->GetDirectBufferCapacity(env, haystack_buffer);
    if (base == NULL || capacity < 0) return NULL;
    if (position < 0 || limit < position || (jlong)limit > capacity) {
        m3_reverse_throw(env, "java/lang/IllegalArgumentException", "invalid direct reverse window");
        return NULL;
    }
    return m3_reverse_core(env, base + (size_t)position,
            limit - position, needle_array, from_array);
}
