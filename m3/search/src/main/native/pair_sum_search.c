/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/pair_sum_search.c (blob 83409f7dd632) by M3_SEARCH_RELOCATE v3.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/* The packed two-index result is stable across the Java/C17 ABI boundary. */
_Static_assert(sizeof(jint) == sizeof(int32_t), "JNI jint must be 32 bits");
_Static_assert(sizeof(jlong) == sizeof(int64_t), "JNI jlong must be 64 bits");

enum { MAX_BATCH_QUERIES = 8192 };
enum { MAX_NATIVE_UNSORTED_ITEMS = 65536 };

typedef struct {
    jint key;
    jsize earliest_suffix;
    uint32_t generation;
} SuffixEntry;

static void throw_java(JNIEnv *env, const char *class_name, const char *message) {
    jclass type = (*env)->FindClass(env, class_name);
    if (type != NULL) (*env)->ThrowNew(env, type, message);
}

static uint32_t mix_int32(jint key) {
    uint32_t mixed = (uint32_t) key;
    mixed ^= mixed >> 16;
    mixed *= UINT32_C(0x7feb352d);
    mixed ^= mixed >> 15;
    mixed *= UINT32_C(0x846ca68b);
    return mixed ^ (mixed >> 16);
}

/* Twice the number of keys gives an occupancy ceiling of 1/2. */
static size_t suffix_capacity(jsize length) {
    size_t capacity = 2;
    while (capacity < (size_t) length * 2) capacity <<= 1;
    return capacity;
}

/* Right-to-left suffix map preserves the earliest (left,right) index pair. */
static jlong pair_sum_unsorted(const jint *values, jsize length, jlong target,
                               SuffixEntry *table, size_t capacity, uint32_t generation) {
    const jlong minimum = (jlong) INT32_MIN * 2;
    const jlong maximum = (jlong) INT32_MAX * 2;
    if (length < 2 || target < minimum || target > maximum) return (jlong) -1;

    jlong first = (jlong) -1;
    const size_t mask = capacity - 1;
    for (jsize left = length; left-- > 0;) {
        /* The global pair-sum bound above makes this subtraction overflow-safe. */
        const jlong needed = target - (jlong) values[left];
        if (needed >= INT32_MIN && needed <= INT32_MAX) {
            size_t slot = (size_t) mix_int32((jint) needed) & mask;
            while (table[slot].generation == generation) {
                if (table[slot].key == (jint) needed) {
                    first = ((jlong) left << 32)
                            | (jlong) (uint32_t) table[slot].earliest_suffix;
                    break;
                }
                slot = (slot + 1) & mask;
            }
        }
        size_t slot = (size_t) mix_int32(values[left]) & mask;
        while (table[slot].generation == generation
                && table[slot].key != values[left]) {
            slot = (slot + 1) & mask;
        }
        table[slot].generation = generation;
        table[slot].key = values[left];
        table[slot].earliest_suffix = left;
    }
    return first;
}

static jlong pair_sum_sorted(const jint *values, jsize length, jlong target) {
    const jlong minimum = (jlong) INT32_MIN * 2;
    const jlong maximum = (jlong) INT32_MAX * 2;
    if (length < 2 || target < minimum || target > maximum) return (jlong) -1;

    jsize left = 0;
    jsize right = length - 1;
    while (left < right) {
        const jlong sum = (jlong) values[left] + (jlong) values[right];
        if (sum < target) {
            left++;
        } else if (sum > target) {
            right--;
        } else {
            while (left + 1 < right && values[right - 1] == values[right]) right--;
            return ((jlong) left << 32) | (jlong) (uint32_t) right;
        }
    }
    return (jlong) -1;
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_search_M3PairSumJni_sortedPairNative(
        JNIEnv *env, jclass owner, jintArray sorted, jlong target) {
    (void) owner;
    if (sorted == NULL) return (jlong) -1;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) return (jlong) -1; /* JVM already has pending exception. */
    const jlong result = pair_sum_sorted(values, length, target);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_search_M3PairSumJni_sortedPairsNative(
        JNIEnv *env, jclass owner, jintArray sorted, jlongArray targets) {
    (void) owner;
    if (sorted == NULL || targets == NULL) return NULL;
    const jsize count = (*env)->GetArrayLength(env, targets);
    if (count > MAX_BATCH_QUERIES) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "bounded JNI query batch exceeded");
        return NULL;
    }
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result == NULL || count == 0) return result;

    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) return NULL;
    jlong *queries = (*env)->GetLongArrayElements(env, targets, NULL);
    if (queries == NULL) {
        (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
        return NULL;
    }

    jlong *packed = malloc((size_t) count * sizeof(jlong));
    if (packed == NULL) {
        (*env)->ReleaseLongArrayElements(env, targets, queries, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
        jclass error = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (error != NULL) (*env)->ThrowNew(env, error, "pair-sum JNI batch allocation");
        return NULL;
    }
    for (jsize index = 0; index < count; index++) {
        packed[index] = pair_sum_sorted(values, length, queries[index]);
    }
    (*env)->ReleaseLongArrayElements(env, targets, queries, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    (*env)->SetLongArrayRegion(env, result, 0, count, packed);
    free(packed);
    return result;
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_search_M3PairSumJni_unsortedPairNative(
        JNIEnv *env, jclass owner, jintArray values_array, jlong target) {
    (void) owner;
    if (values_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "values");
        return (jlong) -1;
    }
    const jsize length = (*env)->GetArrayLength(env, values_array);
    if (length > MAX_NATIVE_UNSORTED_ITEMS) {
        throw_java(env, "java/lang/IllegalArgumentException", "native suffix bound exceeded");
        return (jlong) -1;
    }
    const size_t capacity = suffix_capacity(length);
    SuffixEntry *table = calloc(capacity, sizeof(*table));
    if (table == NULL) {
        throw_java(env, "java/lang/OutOfMemoryError", "native suffix allocation");
        return (jlong) -1;
    }
    jint *values = (*env)->GetIntArrayElements(env, values_array, NULL);
    if (values == NULL) {
        free(table);
        return (jlong) -1; /* VM has a pending exception. */
    }
    const jlong result = pair_sum_unsorted(values, length, target, table, capacity, 1);
    (*env)->ReleaseIntArrayElements(env, values_array, values, JNI_ABORT);
    free(table);
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_search_M3PairSumJni_unsortedPairsNative(
        JNIEnv *env, jclass owner, jintArray values_array, jlongArray targets_array) {
    (void) owner;
    if (values_array == NULL || targets_array == NULL) {
        throw_java(env, "java/lang/NullPointerException", "values or targets");
        return NULL;
    }
    const jsize count = (*env)->GetArrayLength(env, targets_array);
    const jsize length = (*env)->GetArrayLength(env, values_array);
    if (count > MAX_BATCH_QUERIES || length > MAX_NATIVE_UNSORTED_ITEMS) {
        throw_java(env, "java/lang/IllegalArgumentException", "native suffix/query bound exceeded");
        return NULL;
    }
    jlongArray result = (*env)->NewLongArray(env, count);
    if (result == NULL || count == 0) return result;

    const size_t capacity = suffix_capacity(length);
    SuffixEntry *table = calloc(capacity, sizeof(*table));
    jlong *packed = malloc((size_t) count * sizeof(*packed));
    if (table == NULL || packed == NULL) {
        free(packed);
        free(table);
        throw_java(env, "java/lang/OutOfMemoryError", "native unsorted batch allocation");
        return NULL;
    }
    jint *values = (*env)->GetIntArrayElements(env, values_array, NULL);
    if (values == NULL) {
        free(packed);
        free(table);
        return NULL;
    }
    jlong *targets = (*env)->GetLongArrayElements(env, targets_array, NULL);
    if (targets == NULL) {
        (*env)->ReleaseIntArrayElements(env, values_array, values, JNI_ABORT);
        free(packed);
        free(table);
        return NULL;
    }
    for (jsize index = 0; index < count; index++) {
        /* One stamped table per JNI call, with no per-target clearing or allocation. */
        packed[index] = pair_sum_unsorted(values, length, targets[index],
                table, capacity, (uint32_t) index + 1U);
    }
    (*env)->ReleaseLongArrayElements(env, targets_array, targets, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, values_array, values, JNI_ABORT);
    (*env)->SetLongArrayRegion(env, result, 0, count, packed);
    free(packed);
    free(table);
    return result;
}
