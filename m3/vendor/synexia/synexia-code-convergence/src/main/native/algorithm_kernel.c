/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdlib.h>

#ifndef SYNEXIA_MALLOC
#define SYNEXIA_MALLOC malloc
#endif

#ifndef SYNEXIA_CALLOC
#define SYNEXIA_CALLOC calloc
#endif

static void throw_out_of_memory(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
    }
}

static jsize lower_bound_native(const jint *values, jsize length, jint value) {
    jsize low = 0;
    jsize high = length;
    while (low < high) {
        const jsize mid = low + ((high - low) >> 1);
        if (values[mid] < value) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return low;
}

/*
 * M3_SEARCH_INSERTION_POINT_V1: encode a nonnegative jsize insertion point.
 * Widen before addition/negation, including INT_MAX -> INT_MIN, without signed C overflow.
 * This is the canonical Java binary/exponential miss contract, not the -1-only exact contract.
 */
static jint encode_insertion_point_native(jsize insertion) {
    return (jint) (-((jlong) insertion + 1));
}

static jint canonical_binary_search_long_native(
        const jlong *values, jsize length, jlong value) {
    jsize low = 0;
    jsize high = length - 1;
    while (low <= high) {
        const jsize mid = low + ((high - low) >> 1);
        const jlong current = values[mid];
        if (current < value) {
            low = mid + 1;
        } else if (current > value) {
            high = mid - 1;
        } else {
            return (jint) mid;
        }
    }
    return encode_insertion_point_native(low);
}

static jint canonical_exponential_search_long_native(
        const jlong *values, jsize length, jlong value) {
    if (length == 0) {
        return -1;
    }
    if (values[0] == value) {
        return 0;
    }
    jlong bound = 1;
    while (bound < (jlong) length && values[(jsize) bound] < value) {
        bound <<= 1;
    }
    jsize low = (jsize) (bound >> 1);
    jsize high =
            bound < (jlong) length ? (jsize) bound : length - 1;
    while (low <= high) {
        const jsize mid = low + ((high - low) >> 1);
        const jlong current = values[mid];
        if (current < value) {
            low = mid + 1;
        } else if (current > value) {
            high = mid - 1;
        } else {
            return (jint) mid;
        }
    }
    return encode_insertion_point_native(low);
}

static jsize integer_sqrt_native(jsize length);

static jint canonical_jump_search_long_native(
        const jlong *values, jsize length, jlong value) {
    if (length == 0) {
        return -1;
    }

    jsize block = integer_sqrt_native(length);
    if (block < 1) {
        block = 1;
    }
    jsize start = 0;
    jlong next_end = (jlong) block;
    jsize end = next_end < (jlong) length ? (jsize) next_end : length;
    while (end < length && values[end - 1] < value) {
        start = end;
        next_end = (jlong) start + (jlong) block;
        end = next_end < (jlong) length ? (jsize) next_end : length;
    }
    for (jsize index = start; index < end && values[index] <= value; index++) {
        if (values[index] == value) {
            break;
        }
    }

    jsize insertion = 0;
    jsize insertion_end = length;
    while (insertion < insertion_end) {
        const jsize mid = insertion + ((insertion_end - insertion) >> 1);
        if (values[mid] < value) {
            insertion = mid + 1;
        } else {
            insertion_end = mid;
        }
    }
    return insertion < length && values[insertion] == value
            ? (jint) insertion
            : encode_insertion_point_native(insertion);
}

static jint canonical_ternary_search_long_native(
        const jlong *values, jsize length, jlong value) {
    jlong low = 0;
    jlong high = (jlong) length - 1;
    while (low <= high) {
        const jlong third = (high - low) / 3;
        const jsize left_probe = (jsize) (low + third);
        const jsize right_probe = (jsize) (high - third);
        const jlong left_value = values[left_probe];
        const jlong right_value = values[right_probe];
        if (left_value == value || right_value == value) {
            break;
        }
        if (value < left_value) {
            high = (jlong) left_probe - 1;
        } else if (value > right_value) {
            low = (jlong) right_probe + 1;
        } else {
            low = (jlong) left_probe + 1;
            high = (jlong) right_probe - 1;
        }
    }

    jsize insertion = 0;
    jsize end = length;
    while (insertion < end) {
        const jsize mid = insertion + ((end - insertion) >> 1);
        if (values[mid] < value) {
            insertion = mid + 1;
        } else {
            end = mid;
        }
    }
    return insertion < length && values[insertion] == value
            ? (jint) insertion
            : encode_insertion_point_native(insertion);
}

static jsize upper_bound_native(const jint *values, jsize length, jint value) {
    jsize low = 0;
    jsize high = length;
    while (low < high) {
        const jsize mid = low + ((high - low) >> 1);
        if (values[mid] <= value) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return low;
}

static jsize bound_range_native(
        const jint *values,
        jsize from,
        jsize to,
        jint value,
        int upper) {
    jsize low = from;
    jsize high = to;
    while (low < high) {
        const jsize mid = low + ((high - low) >> 1);
        const jint current = values[mid];
        if (current < value || (upper && current == value)) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return low;
}

static jsize gallop_bound_native(
        const jint *values,
        jsize from,
        jsize to,
        jint value,
        int upper) {
    if (from == to) {
        return to;
    }
    const jint first = values[from];
    if (first > value || (!upper && first == value)) {
        return from;
    }
    jsize low = from + 1;
    for (jlong offset = 1;
            (jlong) from + offset < (jlong) to;
            offset = (offset << 1) + 1) {
        const jsize probe = (jsize) ((jlong) from + offset);
        const jint current = values[probe];
        if (current > value || (!upper && current == value)) {
            return bound_range_native(values, low, probe, value, upper);
        }
        low = probe + 1;
    }
    return bound_range_native(values, low, to, value, upper);
}
static jint median_of_three(jint first, jint second, jint third) {
    if (first < second) {
        if (second < third) {
            return second;
        }
        return first < third ? third : first;
    }
    if (first < third) {
        return first;
    }
    return second < third ? third : second;
}

static void swap_int(jint *values, jsize left, jsize right) {
    if (left == right) {
        return;
    }
    const jint value = values[left];
    values[left] = values[right];
    values[right] = value;
}

static jint select_kth_smallest_native(
        jint *values,
        jsize from,
        jsize to,
        jsize target) {
    jsize low = from;
    jsize high = to;
    while (high - low > 1) {
        const jsize middle = low + ((high - low) >> 1);
        const jint pivot =
                median_of_three(values[low], values[middle], values[high - 1]);

        jsize less = low;
        jsize scan = low;
        jsize greater = high;
        while (scan < greater) {
            const jint current = values[scan];
            if (current < pivot) {
                swap_int(values, less++, scan++);
            } else if (current > pivot) {
                swap_int(values, scan, --greater);
            } else {
                scan++;
            }
        }

        if (target < less) {
            high = less;
        } else if (target >= greater) {
            low = greater;
        } else {
            return values[target];
        }
    }
    return values[target];
}


static jsize integer_sqrt_native(jsize length) {
    if (length <= 1) {
        return length;
    }
    jsize low = 1;
    jsize high = length < 46340 ? length : 46340;
    jsize result = 1;
    while (low <= high) {
        const jsize mid = low + ((high - low) >> 1);
        const jlong square = (jlong) mid * (jlong) mid;
        if (square <= (jlong) length) {
            result = mid;
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }
    return result;
}

static jsize fibonacci_search_exact_native(
        const jint *values, jsize from, jsize to, jint value) {
    const jsize length = to - from;
    if (length == 0) {
        return -1;
    }

    jlong fib_minus_2 = 0;
    jlong fib_minus_1 = 1;
    jlong fib = 1;
    while (fib < (jlong) length) {
        fib_minus_2 = fib_minus_1;
        fib_minus_1 = fib;
        fib = fib_minus_1 + fib_minus_2;
    }

    jlong offset = -1;
    while (fib > 1) {
        const jlong candidate = offset + fib_minus_2;
        const jsize relative = (jsize) (
                candidate < (jlong) length - 1 ? candidate : (jlong) length - 1);
        const jint current = values[from + relative];
        if (current < value) {
            fib = fib_minus_1;
            fib_minus_1 = fib_minus_2;
            fib_minus_2 = fib - fib_minus_1;
            offset = relative;
        } else if (current > value) {
            fib = fib_minus_2;
            fib_minus_1 = fib_minus_1 - fib_minus_2;
            fib_minus_2 = fib - fib_minus_1;
        } else {
            return bound_range_native(values, from, from + relative + 1, value, 0);
        }
    }

    const jlong relative = offset + 1;
    if (fib_minus_1 == 1
            && relative >= 0
            && relative < (jlong) length
            && values[from + (jsize) relative] == value) {
        return bound_range_native(
                values, from, from + (jsize) relative + 1, value, 0);
    }
    return -1;
}

static jsize ternary_search_exact_native(
        const jint *values, jsize from, jsize to, jint value) {
    jlong low = from;
    jlong high = (jlong) to - 1;
    while (low <= high) {
        const jlong third = (high - low) / 3;
        const jsize left_probe = (jsize) (low + third);
        const jsize right_probe = (jsize) (high - third);
        const jint left_value = values[left_probe];
        const jint right_value = values[right_probe];

        if (left_value == value) {
            return bound_range_native(values, from, left_probe + 1, value, 0);
        }
        if (right_value == value) {
            return bound_range_native(values, from, right_probe + 1, value, 0);
        }
        if (value < left_value) {
            high = (jlong) left_probe - 1;
        } else if (value > right_value) {
            low = (jlong) right_probe + 1;
        } else {
            low = (jlong) left_probe + 1;
            high = (jlong) right_probe - 1;
        }
    }
    return -1;
}

static jsize jump_search_exact_native(
        const jint *values, jsize from, jsize to, jint value) {
    const jsize length = to - from;
    if (length == 0) {
        return -1;
    }
    jsize block = integer_sqrt_native(length);
    if (block < 1) {
        block = 1;
    }
    jsize start = from;
    jlong next_end = (jlong) start + (jlong) block;
    jsize end = next_end < (jlong) to ? (jsize) next_end : to;
    while (end < to && values[end - 1] < value) {
        start = end;
        next_end = (jlong) start + (jlong) block;
        end = next_end < (jlong) to ? (jsize) next_end : to;
    }
    for (jsize index = start; index < end && values[index] <= value; index++) {
        if (values[index] == value) {
            return index;
        }
    }
    return -1;
}

static jsize interpolation_search_exact_native(
        const jint *values, jsize from, jsize to, jint value) {
    if (from == to) {
        return -1;
    }
    jsize low = from;
    jsize high = to - 1;
    while (low <= high && value >= values[low] && value <= values[high]) {
        const jint low_value = values[low];
        const jint high_value = values[high];
        if (low_value == high_value) {
            return low_value == value ? low : -1;
        }
        const jlong delta = (jlong) value - (jlong) low_value;
        const jlong denominator = (jlong) high_value - (jlong) low_value;
        const jlong width = (jlong) high - (jlong) low;
        const jlong quotient = delta / denominator;
        const jlong remainder = delta % denominator;
        const jlong relative = quotient * width + (remainder * width) / denominator;
        const jsize probe = low + (jsize) relative;
        const jint current = values[probe];
        if (current < value) {
            low = probe + 1;
        } else if (current > value) {
            if (probe == 0) {
                return -1;
            }
            high = probe - 1;
        } else {
            return bound_range_native(values, low, probe + 1, value, 0);
        }
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_lowerBound0(
        JNIEnv *env, jclass type, jintArray sorted, jint value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize result = lower_bound_native(values, length, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_upperBound0(
        JNIEnv *env, jclass type, jintArray sorted, jint value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize result = upper_bound_native(values, length, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_binarySearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize index = lower_bound_native(values, length, value);
    const jint result =
            index < length && values[index] == value ? (jint) index : (jint) -1;
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_binarySearchCanonical0(
        JNIEnv *env, jclass type, jlongArray sorted, jlong value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jint result = canonical_binary_search_long_native(values, length, value);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_exponentialSearchCanonical0(
        JNIEnv *env, jclass type, jlongArray sorted, jlong value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jint result =
            canonical_exponential_search_long_native(values, length, value);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_jumpSearchCanonical0(
        JNIEnv *env, jclass type, jlongArray sorted, jlong value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jint result = canonical_jump_search_long_native(values, length, value);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_ternarySearchCanonical0(
        JNIEnv *env, jclass type, jlongArray sorted, jlong value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jint result =
            canonical_ternary_search_long_native(values, length, value);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_equalRangePacked0(
        JNIEnv *env, jclass type, jintArray sorted, jint value) {
    (void) type;
    const jsize length = (*env)->GetArrayLength(env, sorted);
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize lower = lower_bound_native(values, length, value);
    const jsize upper =
            lower < length && values[lower] == value
                    ? bound_range_native(values, lower, length, value, 1)
                    : lower;
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return ((jlong) lower << 32) | ((jlong) upper & 0xffffffffLL);
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_equalRangesPacked0(
        JNIEnv *env, jclass type, jintArray sorted, jintArray queries,
        jint value_from, jint value_to, jlongArray destination, jint destination_from) {
    (void) type;
    const jsize sorted_length = (*env)->GetArrayLength(env, sorted);
    jint *sorted_values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (sorted_values == NULL) {
        return 0;
    }
    jint *query_values = (*env)->GetIntArrayElements(env, queries, NULL);
    if (query_values == NULL) {
        (*env)->ReleaseIntArrayElements(env, sorted, sorted_values, JNI_ABORT);
        return 0;
    }
    jlong *output = (*env)->GetLongArrayElements(env, destination, NULL);
    if (output == NULL) {
        (*env)->ReleaseIntArrayElements(env, queries, query_values, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, sorted, sorted_values, JNI_ABORT);
        return 0;
    }

    const jsize count = (jsize) (value_to - value_from);
    for (jsize index = 0; index < count; index++) {
        const jint value = query_values[(jsize) value_from + index];
        const jsize lower = lower_bound_native(sorted_values, sorted_length, value);
        const jsize upper =
                lower < sorted_length && sorted_values[lower] == value
                        ? bound_range_native(
                                sorted_values, lower, sorted_length, value, 1)
                        : lower;
        output[(jsize) destination_from + index] =
                ((jlong) lower << 32) | ((jlong) upper & 0xffffffffLL);
    }

    (*env)->ReleaseLongArrayElements(env, destination, output, 0);
    (*env)->ReleaseIntArrayElements(env, queries, query_values, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, sorted, sorted_values, JNI_ABORT);
    return (jint) count;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_gallopLowerBound0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize result =
            gallop_bound_native(values, (jsize) from, (jsize) to, value, 0);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_gallopUpperBound0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize result =
            gallop_bound_native(values, (jsize) from, (jsize) to, value, 1);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_exponentialSearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize index =
            gallop_bound_native(values, (jsize) from, (jsize) to, value, 0);
    const jint result =
            index < (jsize) to && values[index] == value ? (jint) index : (jint) -1;
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_selectKthSmallestInPlace0(
        JNIEnv *env, jclass type, jintArray array, jint from, jint to, jint rank) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, array, NULL);
    if (values == NULL) {
        return 0;
    }
    const jsize target = (jsize) from + (jsize) rank;
    const jint result = select_kth_smallest_native(
            values, (jsize) from, (jsize) to, target);
    (*env)->ReleaseIntArrayElements(env, array, values, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_fibonacciSearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize result =
            fibonacci_search_exact_native(values, (jsize) from, (jsize) to, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_ternarySearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize result =
            ternary_search_exact_native(values, (jsize) from, (jsize) to, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_jumpSearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize result =
            jump_search_exact_native(values, (jsize) from, (jsize) to, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_interpolationSearchExact0(
        JNIEnv *env, jclass type, jintArray sorted, jint from, jint to, jint value) {
    (void) type;
    jint *values = (*env)->GetIntArrayElements(env, sorted, NULL);
    if (values == NULL) {
        return -1;
    }
    const jsize result =
            interpolation_search_exact_native(values, (jsize) from, (jsize) to, value);
    (*env)->ReleaseIntArrayElements(env, sorted, values, JNI_ABORT);
    return (jint) result;
}


JNIEXPORT jintArray JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_intersectSortedUnique0(
        JNIEnv *env, jclass type, jintArray left, jintArray right) {
    (void) type;
    const jsize ln = (*env)->GetArrayLength(env, left);
    const jsize rn = (*env)->GetArrayLength(env, right);
    jint *l = (*env)->GetIntArrayElements(env, left, NULL);
    jint *r = (*env)->GetIntArrayElements(env, right, NULL);
    if (l == NULL || r == NULL) {
        if (l != NULL) {
            (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        }
        if (r != NULL) {
            (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        }
        return NULL;
    }

    const jsize cap = ln < rn ? ln : rn;
    jint *tmp =
            cap == 0 ? NULL : (jint *) SYNEXIA_MALLOC((size_t) cap * sizeof(jint));
    if (cap > 0 && tmp == NULL) {
        (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        throw_out_of_memory(env, "native intersection allocation failed");
        return NULL;
    }

    jsize i = 0;
    jsize j = 0;
    jsize size = 0;
    jint previous = 0;
    int has_previous = 0;
    while (i < ln && j < rn) {
        const jint a = l[i];
        const jint b = r[j];
        if (a < b) {
            i++;
        } else if (a > b) {
            j++;
        } else {
            if (!has_previous || previous != a) {
                tmp[size++] = a;
                previous = a;
                has_previous = 1;
            }
            while (i < ln && l[i] == a) {
                i++;
            }
            while (j < rn && r[j] == b) {
                j++;
            }
        }
    }

    jintArray out = (*env)->NewIntArray(env, size);
    if (out != NULL && size > 0) {
        (*env)->SetIntArrayRegion(env, out, 0, size, tmp);
    }
    free(tmp);
    (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
    return out;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_intersectCountSortedUnique0(
        JNIEnv *env, jclass type, jintArray left, jintArray right) {
    (void) type;
    const jsize ln = (*env)->GetArrayLength(env, left);
    const jsize rn = (*env)->GetArrayLength(env, right);
    jint *l = (*env)->GetIntArrayElements(env, left, NULL);
    jint *r = (*env)->GetIntArrayElements(env, right, NULL);
    if (l == NULL || r == NULL) {
        if (l != NULL) {
            (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        }
        if (r != NULL) {
            (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        }
        return 0;
    }

    jsize i = 0;
    jsize j = 0;
    jint count = 0;
    jint previous = 0;
    int has_previous = 0;
    while (i < ln && j < rn) {
        const jint a = l[i];
        const jint b = r[j];
        if (a < b) {
            i++;
        } else if (a > b) {
            j++;
        } else {
            if (!has_previous || previous != a) {
                count++;
                previous = a;
                has_previous = 1;
            }
            while (i < ln && l[i] == a) {
                i++;
            }
            while (j < rn && r[j] == b) {
                j++;
            }
        }
    }

    (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
    return count;
}

JNIEXPORT jintArray JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_unionSortedUnique0(
        JNIEnv *env, jclass type, jintArray left, jintArray right) {
    (void) type;
    const jsize ln = (*env)->GetArrayLength(env, left);
    const jsize rn = (*env)->GetArrayLength(env, right);
    jint *l = (*env)->GetIntArrayElements(env, left, NULL);
    jint *r = (*env)->GetIntArrayElements(env, right, NULL);
    if (l == NULL || r == NULL) {
        if (l != NULL) {
            (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        }
        if (r != NULL) {
            (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        }
        return NULL;
    }

    const jlong cap64 = (jlong) ln + (jlong) rn;
    if (cap64 > 2147483647LL) {
        (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        throw_out_of_memory(env, "native union capacity exceeds JNI array limit");
        return NULL;
    }
    const jsize cap = (jsize) cap64;
    jint *tmp =
            cap == 0 ? NULL : (jint *) SYNEXIA_MALLOC((size_t) cap * sizeof(jint));
    if (cap > 0 && tmp == NULL) {
        (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
        throw_out_of_memory(env, "native union allocation failed");
        return NULL;
    }

    jsize i = 0;
    jsize j = 0;
    jsize size = 0;
    jint previous = 0;
    int has_previous = 0;
    while (i < ln || j < rn) {
        jint value;
        if (j >= rn || (i < ln && l[i] <= r[j])) {
            value = l[i++];
            while (i < ln && l[i] == value) {
                i++;
            }
            while (j < rn && r[j] == value) {
                j++;
            }
        } else {
            value = r[j++];
            while (j < rn && r[j] == value) {
                j++;
            }
        }
        if (!has_previous || previous != value) {
            tmp[size++] = value;
            previous = value;
            has_previous = 1;
        }
    }

    jintArray out = (*env)->NewIntArray(env, size);
    if (out != NULL && size > 0) {
        (*env)->SetIntArrayRegion(env, out, 0, size, tmp);
    }
    free(tmp);
    (*env)->ReleaseIntArrayElements(env, left, l, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, right, r, JNI_ABORT);
    return out;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_indexOf0(
        JNIEnv *env, jclass type, jbyteArray haystack, jbyteArray needle) {
    (void) type;
    const jsize hn = (*env)->GetArrayLength(env, haystack);
    const jsize nn = (*env)->GetArrayLength(env, needle);
    if (nn == 0) {
        return 0;
    }
    if (nn > hn) {
        return -1;
    }

    jbyte *h = (*env)->GetByteArrayElements(env, haystack, NULL);
    jbyte *n = (*env)->GetByteArrayElements(env, needle, NULL);
    if (h == NULL || n == NULL) {
        if (h != NULL) {
            (*env)->ReleaseByteArrayElements(env, haystack, h, JNI_ABORT);
        }
        if (n != NULL) {
            (*env)->ReleaseByteArrayElements(env, needle, n, JNI_ABORT);
        }
        return -1;
    }

    jint result = -1;
    for (jsize start = 0; start <= hn - nn; start++) {
        jsize offset = 0;
        while (offset < nn && h[start + offset] == n[offset]) {
            offset++;
        }
        if (offset == nn) {
            result = (jint) start;
            break;
        }
    }

    (*env)->ReleaseByteArrayElements(env, haystack, h, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, needle, n, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_indexOfKmp0(
        JNIEnv *env, jclass type, jbyteArray haystack, jbyteArray needle) {
    (void) type;
    const jsize hn = (*env)->GetArrayLength(env, haystack);
    const jsize nn = (*env)->GetArrayLength(env, needle);
    if (nn == 0) {
        return 0;
    }
    if (nn > hn) {
        return -1;
    }

    jbyte *h = (*env)->GetByteArrayElements(env, haystack, NULL);
    jbyte *n = (*env)->GetByteArrayElements(env, needle, NULL);
    if (h == NULL || n == NULL) {
        if (h != NULL) {
            (*env)->ReleaseByteArrayElements(env, haystack, h, JNI_ABORT);
        }
        if (n != NULL) {
            (*env)->ReleaseByteArrayElements(env, needle, n, JNI_ABORT);
        }
        return -1;
    }

    jsize *failure = (jsize *) SYNEXIA_CALLOC((size_t) nn, sizeof(jsize));
    if (failure == NULL) {
        (*env)->ReleaseByteArrayElements(env, haystack, h, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, needle, n, JNI_ABORT);
        throw_out_of_memory(env, "native KMP failure-table allocation failed");
        return -1;
    }

    for (jsize i = 1, prefix = 0; i < nn; ) {
        if (n[i] == n[prefix]) {
            failure[i++] = ++prefix;
        } else if (prefix > 0) {
            prefix = failure[prefix - 1];
        } else {
            failure[i++] = 0;
        }
    }

    jint result = -1;
    for (jsize i = 0, matched = 0; i < hn; ) {
        if (h[i] == n[matched]) {
            i++;
            matched++;
            if (matched == nn) {
                result = (jint) (i - matched);
                break;
            }
        } else if (matched > 0) {
            matched = failure[matched - 1];
        } else {
            i++;
        }
    }

    free(failure);
    (*env)->ReleaseByteArrayElements(env, haystack, h, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, needle, n, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_synexia_convergence_donor_JniAlgorithmKernel_longestCommonPrefix0(
        JNIEnv *env, jclass type, jbyteArray left, jbyteArray right) {
    (void) type;
    const jsize ln = (*env)->GetArrayLength(env, left);
    const jsize rn = (*env)->GetArrayLength(env, right);
    const jsize limit = ln < rn ? ln : rn;
    jbyte *l = (*env)->GetByteArrayElements(env, left, NULL);
    jbyte *r = (*env)->GetByteArrayElements(env, right, NULL);
    if (l == NULL || r == NULL) {
        if (l != NULL) {
            (*env)->ReleaseByteArrayElements(env, left, l, JNI_ABORT);
        }
        if (r != NULL) {
            (*env)->ReleaseByteArrayElements(env, right, r, JNI_ABORT);
        }
        return 0;
    }

    jsize index = 0;
    while (index < limit && l[index] == r[index]) {
        index++;
    }

    (*env)->ReleaseByteArrayElements(env, left, l, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, right, r, JNI_ABORT);
    return (jint) index;
}
