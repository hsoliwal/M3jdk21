/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Original Synexia bulk CSR posting-list operations. No external runtime libraries.
 * Input owners stay Java int[] arrays. JNI takes one entire immutable query batch.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/m3/qualification/master-landing-20261028/retained-variants/pr15528/synexia-mat-collections/native/mat_posting_merge.c blob 9f301002dcbc; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
#include <jni.h>
#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    jintArray posting_array;
    jintArray offset_array;
    jintArray position_array;
    jint *postings;
    jint *offsets;
    jint *positions;
    jsize posting_count;
    jsize offset_count;
    jsize position_count;
    jint universe;
} PostingInput;

static void throw_java(JNIEnv *env, const char *klass, const char *message) {
    jclass exception = (*env)->FindClass(env, klass);
    if (exception != NULL) {
        (void) (*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}

static void release_input(JNIEnv *env, PostingInput *in) {
    if (in->positions != NULL) {
        (*env)->ReleaseIntArrayElements(env, in->position_array, in->positions, JNI_ABORT);
        in->positions = NULL;
    }
    if (in->offsets != NULL) {
        (*env)->ReleaseIntArrayElements(env, in->offset_array, in->offsets, JNI_ABORT);
        in->offsets = NULL;
    }
    if (in->postings != NULL) {
        (*env)->ReleaseIntArrayElements(env, in->posting_array, in->postings, JNI_ABORT);
        in->postings = NULL;
    }
}

static int read_input(JNIEnv *env, PostingInput *in,
        jintArray postings, jintArray offsets, jintArray positions, jint universe) {
    memset(in, 0, sizeof(*in));
    if (postings == NULL || offsets == NULL || positions == NULL || universe < 0) {
        throw_java(env, "java/lang/IllegalArgumentException", "invalid posting JNI input");
        return 0;
    }
    in->posting_array = postings;
    in->offset_array = offsets;
    in->position_array = positions;
    in->universe = universe;
    in->posting_count = (*env)->GetArrayLength(env, postings);
    in->offset_count = (*env)->GetArrayLength(env, offsets);
    in->position_count = (*env)->GetArrayLength(env, positions);
    if (in->offset_count < 1) {
        throw_java(env, "java/lang/IllegalArgumentException", "empty posting offsets");
        return 0;
    }
    if (in->posting_count != 0) {
        in->postings = (*env)->GetIntArrayElements(env, postings, NULL);
        if (in->postings == NULL) return 0;
    }
    in->offsets = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (in->offsets == NULL) {
        release_input(env, in);
        return 0;
    }
    if (in->position_count != 0) {
        in->positions = (*env)->GetIntArrayElements(env, positions, NULL);
        if (in->positions == NULL) {
            release_input(env, in);
            return 0;
        }
    }
    int valid = (in->offsets[0] == 0
            && in->offsets[in->offset_count - 1] == in->posting_count);
    for (jsize k = 0; valid && k + 1 < in->offset_count; ++k) {
        valid = in->offsets[k] <= in->offsets[k + 1]
                && in->offsets[k] >= 0 && in->offsets[k + 1] <= in->posting_count;
    }
    jint previous = -1;
    for (jsize k = 0; valid && k < in->position_count; ++k) {
        jint index = in->positions[k];
        valid = index > previous && index >= 0 && index < in->offset_count - 1;
        previous = index;
        if (!valid) break;
        jint begin = in->offsets[index];
        jint end = in->offsets[index + 1];
        for (jint cursor = begin; cursor < end; ++cursor) {
            jint row = in->postings[cursor];
            if (row < 0 || row >= universe ||
                    (cursor > begin && row <= in->postings[cursor - 1])) {
                valid = 0;
                break;
            }
        }
    }
    if (!valid) {
        release_input(env, in);
        throw_java(env, "java/lang/IllegalArgumentException", "invalid posting CSR batch");
        return 0;
    }
    return 1;
}

static uint32_t popcount64(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (uint32_t) __builtin_popcountll(value);
#else
    uint32_t count = 0;
    while (value != 0) { value &= value - 1; ++count; }
    return count;
#endif
}

static uint32_t trailing64(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (uint32_t) __builtin_ctzll(value);
#else
    uint32_t count = 0;
    while ((value & 1U) == 0U) { value >>= 1; ++count; }
    return count;
#endif
}

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_nativeAbi(
        JNIEnv *env, jclass owner) {
    (void) env;
    (void) owner;
    return 20261029;
}

/* Merge a sparse union using primitive heap/cursor/end lanes, without a
 * universe-sized bitmap. Each packed heap element stores (row, slot);
 * row is always nonnegative because read_input fully validates postings.
 * No JNI array pointer survives this call. */
static jintArray sparse_union(JNIEnv *env, PostingInput *in,
        size_t total, size_t active) {
    if (active == 0) {
        release_input(env, in);
        return (*env)->NewIntArray(env, 0);
    }
    if (active == 1) {
        /* The selected list is already sorted/unique and entirely validated. */
        for (jsize key = 0; key < in->position_count; ++key) {
            jint position = in->positions[key];
            jint begin = in->offsets[position];
            jint end = in->offsets[position + 1];
            if (begin == end) continue;
            jintArray result = (*env)->NewIntArray(env, end - begin);
            if (result != NULL) {
                (*env)->SetIntArrayRegion(env, result, 0, end - begin,
                        in->postings + begin);
            }
            release_input(env, in);
            return (*env)->ExceptionCheck(env) ? NULL : result;
        }
    }
    if (total > SIZE_MAX / sizeof(jint) || active > SIZE_MAX / sizeof(uint64_t)
            || active > SIZE_MAX / sizeof(jint)) {
        release_input(env, in);
        throw_java(env, "java/lang/OutOfMemoryError", "sparse union size overflow");
        return NULL;
    }
    uint64_t *heap = malloc(active * sizeof(uint64_t));
    jint *cursors = malloc(active * sizeof(jint));
    jint *ends = malloc(active * sizeof(jint));
    jint *scratch = malloc(total * sizeof(jint));
    if (heap == NULL || cursors == NULL || ends == NULL || scratch == NULL) {
        free(heap); free(cursors); free(ends); free(scratch);
        release_input(env, in);
        throw_java(env, "java/lang/OutOfMemoryError", "sparse union allocation");
        return NULL;
    }

    /* Heap insertion is deterministic; no per-record Node or object owner. */
    size_t heap_size = 0;
    for (jsize key = 0; key < in->position_count; ++key) {
        jint position = in->positions[key];
        jint begin = in->offsets[position];
        jint end = in->offsets[position + 1];
        if (begin == end) continue;
        size_t slot = heap_size++;
        cursors[slot] = begin;
        ends[slot] = end;
        uint64_t entry = ((uint64_t) (uint32_t) in->postings[begin] << 32)
                | (uint32_t) slot;
        size_t child = slot;
        while (child != 0) {
            size_t parent = (child - 1) / 2;
            if (heap[parent] <= entry) break;
            heap[child] = heap[parent];
            child = parent;
        }
        heap[child] = entry;
    }

    size_t written = 0;
    jint last = -1;
    while (heap_size > 0) {
        uint64_t entry = heap[0];
        jint row = (jint) (entry >> 32);
        if (row != last) {
            scratch[written++] = row;
            last = row;
        }
        uint32_t slot = (uint32_t) entry;
        ++cursors[slot];
        if (cursors[slot] < ends[slot]) {
            entry = ((uint64_t) (uint32_t) in->postings[cursors[slot]] << 32)
                    | slot;
        } else {
            entry = heap[--heap_size];
        }
        if (heap_size != 0) {
            size_t root = 0;
            while (root < heap_size / 2) {
                size_t child = root * 2 + 1;
                if (child >= heap_size) break;
                if (child + 1 < heap_size && heap[child + 1] < heap[child]) ++child;
                if (heap[child] >= entry) break;
                heap[root] = heap[child];
                root = child;
            }
            heap[root] = entry;
        }
    }

    free(heap); free(cursors); free(ends);
    release_input(env, in);
    jintArray result = (*env)->NewIntArray(env, (jsize) written);
    if (result != NULL && written != 0) {
        (*env)->SetIntArrayRegion(env, result, 0, (jsize) written, scratch);
    }
    free(scratch);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_unionNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe) {
    (void) owner;
    PostingInput in;
    if (!read_input(env, &in, postings, offsets, positions, universe)) return NULL;
    size_t word_count = ((size_t) universe + 63U) / 64U;
    size_t total = 0, active = 0;
    for (jsize key = 0; key < in.position_count; ++key) {
        jint position = in.positions[key];
        size_t length = (size_t) (in.offsets[position + 1] - in.offsets[position]);
        total += length;
        if (length != 0) ++active;
    }
    /* Under extreme sparsity, primitive heap+scratch is smaller than
     * a universe bitmap. Single/empty selections never require it. */
    if (active <= 1 || total <= word_count / 4U) {
        return sparse_union(env, &in, total, active);
    }
    uint64_t *bits = word_count == 0 ? NULL : calloc(word_count, sizeof(uint64_t));
    if (word_count != 0 && bits == NULL) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting union bitmap");
        return NULL;
    }
    for (jsize key = 0; key < in.position_count; ++key) {
        jint index = in.positions[key];
        for (jint cursor = in.offsets[index]; cursor < in.offsets[index + 1]; ++cursor) {
            uint32_t row = (uint32_t) in.postings[cursor];
            bits[row >> 6] |= UINT64_C(1) << (row & 63U);
        }
    }
    release_input(env, &in);
    jsize count = 0;
    for (size_t i = 0; i < word_count; ++i) count += (jsize) popcount64(bits[i]);
    jintArray result = (*env)->NewIntArray(env, count);
    if (result != NULL) {
        jint chunk[512];
        jsize cursor = 0;
        jsize pending = 0;
        for (size_t word = 0; word < word_count; ++word) {
            uint64_t value = bits[word];
            while (value != 0) {
                chunk[pending++] = (jint) (word * 64U + trailing64(value));
                value &= value - 1U;
                if (pending == 512) {
                    (*env)->SetIntArrayRegion(env, result, cursor, pending, chunk);
                    cursor += pending;
                    pending = 0;
                }
            }
        }
        if (pending != 0) (*env)->SetIntArrayRegion(env, result, cursor, pending, chunk);
    }
    free(bits);
    return result;
}

/* Cursor-local lower bound in [start,end). Bound before doubling, so neither
 * step*2 nor start+step can overflow jint. No allocation or retained JNI state. */
static jint posting_lower_bound(const jint *postings, jint start, jint end, jint value) {
    if (start == end || postings[start] >= value) return start;
    jint low = start + 1;
    const jint span = end - start;
    jint step = 1;
    while (step < span && postings[start + step] < value) {
        low = start + step + 1;
        step = step > span / 2 ? span : step * 2;
    }
    jint high = start + step;
    while (low < high) {
        const jint middle = low + (high - low) / 2;
        if (postings[middle] < value) low = middle + 1;
        else high = middle;
    }
    return low;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_intersectionNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe) {
    (void) owner;
    PostingInput in;
    if (!read_input(env, &in, postings, offsets, positions, universe)) return NULL;
    if (in.position_count == 0) {
        release_input(env, &in);
        jintArray full = (*env)->NewIntArray(env, universe);
        if (full != NULL) {
            jint chunk[512];
            for (jint cursor = 0; cursor < universe;) {
                jsize count = (universe - cursor) < 512 ? (universe - cursor) : 512;
                for (jsize i = 0; i < count; ++i) chunk[i] = cursor + i;
                (*env)->SetIntArrayRegion(env, full, cursor, count, chunk);
                cursor += count;
            }
        }
        return full;
    }
    jsize shortest = 0;
    for (jsize i = 1; i < in.position_count; ++i) {
        jint current = in.positions[i];
        jint prior = in.positions[shortest];
        if (in.offsets[current + 1] - in.offsets[current]
                < in.offsets[prior + 1] - in.offsets[prior]) shortest = i;
    }
    jint chosen = in.positions[shortest];
    jsize size = in.offsets[chosen + 1] - in.offsets[chosen];
    if ((size_t) size > SIZE_MAX / sizeof(jint)) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting intersection size overflow");
        return NULL;
    }
    jint *selected = size == 0 ? NULL : malloc((size_t) size * sizeof(jint));
    if (size != 0 && selected == NULL) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting intersection scratch");
        return NULL;
    }
    if (size > 0) memcpy(selected, in.postings + in.offsets[chosen], (size_t) size * sizeof(jint));
    for (jsize i = 0; i < in.position_count && size > 0; ++i) {
        if (i == shortest) continue;
        jint index = in.positions[i];
        jint read = 0, write = 0, cursor = in.offsets[index];
        const jint end = in.offsets[index + 1];
        /* Match the existing Java threshold: long peer => monotone lower_bound.
         * Read/input validation and shortest-first selection remain unchanged.
         * Wider arithmetic prevents size*16 overflow for large JNI arrays. */
        if ((int64_t) (end - cursor) >= (int64_t) size * INT64_C(16)) {
            for (; read < size && cursor < end; ++read) {
                const jint candidate = selected[read];
                const jint low = posting_lower_bound(in.postings, cursor, end, candidate);
                if (low < end && in.postings[low] == candidate) {
                    selected[write++] = candidate;
                    cursor = low + 1;
                } else {
                    cursor = low;
                }
            }
        } else {
            while (read < size && cursor < end) {
                if (selected[read] < in.postings[cursor]) ++read;
                else if (selected[read] > in.postings[cursor]) ++cursor;
                else {
                    selected[write++] = selected[read++];
                    ++cursor;
                }
            }
        }
        size = write;
    }
    release_input(env, &in);
    jintArray result = (*env)->NewIntArray(env, size);
    if (result != NULL && size > 0) (*env)->SetIntArrayRegion(env, result, 0, size, selected);
    free(selected);
    return result;
}

/* Count-only counterparts. Existing JNI entry points above are unchanged.
 * Arrays are validated by read_input; all scratch is O(selected postings),
 * never O(row universe) or O(number of result rows). */
static void sift_count_heap(uint64_t *heap, size_t size, size_t root, uint64_t entry) {
    while (root < size / 2U) {
        size_t child = root * 2U + 1U;
        if (child + 1U < size && heap[child + 1U] < heap[child]) ++child;
        if (heap[child] >= entry) break;
        heap[root] = heap[child];
        root = child;
    }
    heap[root] = entry;
}

static jint count_union_up_to(JNIEnv *env, jintArray postings, jintArray offsets,
        jintArray positions, jint universe, jint limit) {
    if (limit < 0) {
        throw_java(env, "java/lang/IllegalArgumentException", "negative posting count limit");
        return 0;
    }
    PostingInput in;
    if (!read_input(env, &in, postings, offsets, positions, universe)) return 0;
    /* Never let an early result hide a malformed late selected posting. */
    if (limit == 0) { release_input(env, &in); return 0; }
    if (in.position_count <= 1) {
        jint count = 0;
        if (in.position_count == 1) {
            jint position = in.positions[0];
            count = in.offsets[position + 1] - in.offsets[position];
        }
        release_input(env, &in);
        return count < limit ? count : limit;
    }
    size_t capacity = (size_t) in.position_count;
    if (capacity > SIZE_MAX / sizeof(uint64_t) || capacity > SIZE_MAX / sizeof(jint)) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting union count size overflow");
        return 0;
    }
    uint64_t *heap = malloc(capacity * sizeof(uint64_t));
    jint *cursors = malloc(capacity * sizeof(jint));
    jint *ends = malloc(capacity * sizeof(jint));
    if (heap == NULL || cursors == NULL || ends == NULL) {
        free(heap); free(cursors); free(ends);
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting union count scratch");
        return 0;
    }
    size_t active = 0U;
    for (jsize i = 0; i < in.position_count; ++i) {
        jint position = in.positions[i];
        jint begin = in.offsets[position], end = in.offsets[position + 1];
        if (begin == end) continue;
        cursors[active] = begin;
        ends[active] = end;
        heap[active] = ((uint64_t) (uint32_t) in.postings[begin] << 32) | (uint32_t) active;
        ++active;
    }
    for (size_t root = active / 2U; root > 0U;) {
        --root;
        sift_count_heap(heap, active, root, heap[root]);
    }
    jint count = 0, previous = -1;
    while (active != 0U) {
        uint64_t entry = heap[0];
        jint row = (jint) (entry >> 32);
        if (row != previous) {
            if (++count == limit) break;
            previous = row;
        }
        uint32_t slot = (uint32_t) entry;
        if (++cursors[slot] < ends[slot]) {
            entry = ((uint64_t) (uint32_t) in.postings[cursors[slot]] << 32) | slot;
        } else {
            entry = heap[--active];
        }
        if (active != 0U) sift_count_heap(heap, active, 0U, entry);
    }
    free(heap); free(cursors); free(ends);
    release_input(env, &in);
    return count;
}

static jint count_intersection_up_to(JNIEnv *env, jintArray postings, jintArray offsets,
        jintArray positions, jint universe, jint limit) {
    if (limit < 0) {
        throw_java(env, "java/lang/IllegalArgumentException", "negative posting count limit");
        return 0;
    }
    PostingInput in;
    if (!read_input(env, &in, postings, offsets, positions, universe)) return 0;
    if (limit == 0) { release_input(env, &in); return 0; }
    if (in.position_count == 0) {
        release_input(env, &in);
        return universe < limit ? universe : limit;
    }
    jsize shortest = 0;
    for (jsize i = 1; i < in.position_count; ++i) {
        jint current = in.positions[i], prior = in.positions[shortest];
        if (in.offsets[current + 1] - in.offsets[current]
                < in.offsets[prior + 1] - in.offsets[prior]) shortest = i;
    }
    jint first = in.positions[shortest];
    jint length = in.offsets[first + 1] - in.offsets[first];
    if (in.position_count == 1 || length == 0) {
        release_input(env, &in);
        return length < limit ? length : limit;
    }
    size_t capacity = (size_t) in.position_count;
    if (capacity > SIZE_MAX / sizeof(jint)) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting intersection count size overflow");
        return 0;
    }
    jint *cursors = malloc(capacity * sizeof(jint));
    if (cursors == NULL) {
        release_input(env, &in);
        throw_java(env, "java/lang/OutOfMemoryError", "posting intersection count scratch");
        return 0;
    }
    for (jsize i = 0; i < in.position_count; ++i) cursors[i] = in.offsets[in.positions[i]];
    jint count = 0;
    for (jint at = in.offsets[first]; at < in.offsets[first + 1]; ++at) {
        jint candidate = in.postings[at];
        int present = 1;
        for (jsize i = 0; i < in.position_count; ++i) {
            if (i == shortest) continue;
            jint end = in.offsets[in.positions[i] + 1];
            jint found = posting_lower_bound(in.postings, cursors[i], end, candidate);
            cursors[i] = found;
            if (found == end) goto finished;
            if (in.postings[found] != candidate) { present = 0; break; }
            cursors[i] = found + 1;
        }
        if (present && ++count == limit) break;
    }
finished:
    free(cursors);
    release_input(env, &in);
    return count;
}

/* Existing ABI remains available; exact counts use the shared engine without
 * an effective cap. Bounded variants add symbols, not a new library ABI. */
JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_countUnionNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe) {
    (void) owner;
    return count_union_up_to(env, postings, offsets, positions, universe, INT32_MAX);
}

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_countIntersectionNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe) {
    (void) owner;
    return count_intersection_up_to(env, postings, offsets, positions, universe, INT32_MAX);
}

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_countUnionUpToNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe, jint limit) {
    (void) owner;
    return count_union_up_to(env, postings, offsets, positions, universe, limit);
}

JNIEXPORT jint JNICALL Java_jdk_m3_arrays_M3IntPostingIndex_countIntersectionUpToNative(
        JNIEnv *env, jclass owner, jintArray postings, jintArray offsets,
        jintArray positions, jint universe, jint limit) {
    (void) owner;
    return count_intersection_up_to(env, postings, offsets, positions, universe, limit);
}
