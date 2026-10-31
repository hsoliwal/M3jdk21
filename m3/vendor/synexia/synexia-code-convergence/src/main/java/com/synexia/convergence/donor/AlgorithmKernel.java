// SPDX-License-Identifier: Apache-2.0
package com.synexia.convergence.donor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Small primitive algorithm kernel shared by Java reference code and optional JNI acceleration.
 *
 * <p>All added methods are defaults so existing implementations remain source-compatible. Native
 * implementations may override only the operations that have a proven primitive hot path.</p>
 */
public interface AlgorithmKernel {
    int lowerBound(int[] sorted, int value);

    int[] intersectSortedUnique(int[] left, int[] right);

    int indexOf(byte[] haystack, byte[] needle);

    /**
     * Compiles a bounded byte multi-pattern automaton.
     *
     * <p>M3 atom: {@code M3_SEARCH_AHO_CORASICK_COMPILE_V1}. Pattern arrays are deep-copied by the
     * compiled program; caller mutation after compilation cannot change search semantics. The Java
     * implementation is the semantic oracle and JNI may override this method with a parity-checked
     * native handle.</p>
     *
     * @param patterns ordered non-empty byte patterns
     * @return compile-once multi-pattern program
     */
    default AhoCorasickProgram compileAhoCorasick(final byte[][] patterns) {
        return JavaAhoCorasickProgram.compile(patterns);
    }

    /** Compile-once Aho-Corasick search contract with fixed-size result vectors. */
    interface AhoCorasickProgram extends AutoCloseable {
        int patternCount();

        /**
         * First start position for each compiled pattern, or {@code -1} when absent.
         *
         * <p>Result index equals input pattern index. Searches begin from
         * {@code max(0, fromIndex)} and do not report a match that starts before that position.</p>
         */
        int[] firstPositions(byte[] haystack, int fromIndex);

        /**
         * Overlapping occurrence count for each compiled pattern from
         * {@code max(0, fromIndex)}.
         */
        int[] counts(byte[] haystack, int fromIndex);

        @Override
        void close();
    }

    default int upperBound(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            if (sorted[mid] <= value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    default int binarySearchExact(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        final int index = lowerBound(sorted, value);
        return index < sorted.length && sorted[index] == value ? index : -1;
    }

    /**
     * Canonical long-lane binary search used by {@code AlgorithmShape.BINARY_SEARCH}.
     *
     * <p>This deliberately matches the canonical donor contract rather than the older
     * {@link #binarySearchExact(int[], int)} convenience atom: a hit returns the probed index and
     * a miss returns {@code -(insertionPoint + 1)}. The method is a default to preserve every
     * existing implementation and caller.
     */
    default int binarySearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final long current = sorted[mid];
            if (current < value) {
                low = mid + 1;
            } else if (current > value) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    /**
     * Canonical long-lane exponential search used by {@code AlgorithmShape.EXPONENTIAL_SEARCH}.
     *
     * <p>The contract mirrors the canonical donor exactly: hits return the algorithm's probed
     * index; misses return {@code -(insertionPoint + 1)}.
     */
    default int exponentialSearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        if (sorted.length == 0) {
            return -1;
        }
        if (sorted[0] == value) {
            return 0;
        }
        long bound = 1L;
        while (bound < sorted.length && sorted[(int) bound] < value) {
            bound <<= 1;
        }
        int low = (int) (bound >>> 1);
        int high = (int) Math.min(bound, (long) sorted.length - 1L);
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final long current = sorted[mid];
            if (current < value) {
                low = mid + 1;
            } else if (current > value) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    /**
     * Canonical long-lane jump search used by {@code AlgorithmShape.JUMP_SEARCH}.
     *
     * <p>The bounded jump probes preserve the canonical donor mechanic. Result normalization uses
     * lower-bound geometry so duplicate hits return the first equal index and misses return
     * {@code -(insertionPoint + 1)}. Empty input therefore returns {@code -1}.</p>
     */
    default int jumpSearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        final int length = sorted.length;
        if (length == 0) {
            return -1;
        }

        final int block = Math.max(1, (int) Math.sqrt(length));
        int start = 0;
        int end = (int) Math.min((long) length, (long) block);
        while (end < length && sorted[end - 1] < value) {
            start = end;
            end = (int) Math.min((long) length, (long) start + block);
        }
        for (int index = start; index < end && sorted[index] <= value; index++) {
            if (sorted[index] == value) {
                break;
            }
        }

        int insertion = 0;
        int insertionEnd = length;
        while (insertion < insertionEnd) {
            final int mid = (insertion + insertionEnd) >>> 1;
            if (sorted[mid] < value) {
                insertion = mid + 1;
            } else {
                insertionEnd = mid;
            }
        }
        return insertion < length && sorted[insertion] == value
                ? insertion
                : -(insertion + 1);
    }

    /**
     * Canonical long-lane ternary search used by {@code AlgorithmShape.TERNARY_SEARCH}.
     *
     * <p>The ternary probes preserve the canonical donor mechanic. Result normalization uses
     * lower-bound geometry so duplicate hits return the first equal index and misses return
     * {@code -(insertionPoint + 1)}.</p>
     */
    default int ternarySearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length - 1;
        while (low <= high) {
            final int third = (high - low) / 3;
            final int leftProbe = low + third;
            final int rightProbe = high - third;
            final long leftValue = sorted[leftProbe];
            final long rightValue = sorted[rightProbe];
            if (leftValue == value || rightValue == value) {
                break;
            }
            if (value < leftValue) {
                high = leftProbe - 1;
            } else if (value > rightValue) {
                low = rightProbe + 1;
            } else {
                low = leftProbe + 1;
                high = rightProbe - 1;
            }
        }

        int insertion = 0;
        int end = sorted.length;
        while (insertion < end) {
            final int mid = (insertion + end) >>> 1;
            if (sorted[mid] < value) {
                insertion = mid + 1;
            } else {
                end = mid;
            }
        }
        return insertion < sorted.length && sorted[insertion] == value
                ? insertion
                : -(insertion + 1);
    }

    /**
     * Returns the duplicate/insertion range as one packed half-open pair.
     *
     * <p>High 32 bits are the lower bound and low 32 bits are the upper bound.
     * An absent key therefore yields an empty insertion range. Existing implementations
     * remain source-compatible because this is a default atom.</p>
     */
    default long equalRangePacked(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        final int lower = lowerBound(sorted, value);
        final int upper = lower < sorted.length && sorted[lower] == value
                ? boundRange(sorted, lower, sorted.length, value, true)
                : lower;
        return packRange(lower, upper);
    }

    /** Lower component of one packed half-open equal range. */
    static int equalRangeLower(final long packedRange) {
        return (int) (packedRange >>> 32);
    }

    /** Upper component of one packed half-open equal range. */
    static int equalRangeUpper(final long packedRange) {
        return (int) packedRange;
    }

    /**
     * Writes packed equal ranges for a query subrange into caller-owned output.
     * Only the requested destination window is modified.
     */
    default int equalRangesPacked(
            final int[] sorted,
            final int[] values,
            final int valueFrom,
            final int valueTo,
            final long[] destination,
            final int destinationFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(valueFrom, valueTo, values.length);
        final int count = valueTo - valueFrom;
        Objects.checkFromIndexSize(destinationFrom, count, destination.length);
        for (int index = 0; index < count; index++) {
            destination[destinationFrom + index] =
                    equalRangePacked(sorted, values[valueFrom + index]);
        }
        return count;
    }

    private static long packRange(final int lower, final int upper) {
        return ((long) lower << 32) | (upper & 0xffff_ffffL);
    }

    default int gallopLowerBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopBound(sorted, from, to, value, false);
    }

    default int gallopUpperBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopBound(sorted, from, to, value, true);
    }

    default int exponentialSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        final int index = gallopBound(sorted, from, to, value, false);
        return index < to && sorted[index] == value ? index : -1;
    }

    default int selectKthSmallestInPlace(
            final int[] values, final int from, final int to, final int rank) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        Objects.checkIndex(rank, to - from);
        return quickSelect(values, from, to, from + rank);
    }

    default int selectKthLargestInPlace(
            final int[] values, final int from, final int to, final int rank) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        Objects.checkIndex(rank, to - from);
        return selectKthSmallestInPlace(values, from, to, (to - from - 1) - rank);
    }

    private static int quickSelect(
            final int[] values, final int from, final int to, final int target) {
        int low = from;
        int high = to;
        while (high - low > 1) {
            final int middle = low + ((high - low) >>> 1);
            final int pivot = medianOfThree(values[low], values[middle], values[high - 1]);

            int less = low;
            int scan = low;
            int greater = high;
            while (scan < greater) {
                final int current = values[scan];
                if (current < pivot) {
                    swap(values, less++, scan++);
                } else if (current > pivot) {
                    swap(values, scan, --greater);
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

    private static int medianOfThree(final int first, final int second, final int third) {
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

    private static void swap(final int[] values, final int left, final int right) {
        if (left == right) {
            return;
        }
        final int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }

    /**
     * Returns the first equal value in the validated sorted subrange using bounded jump search.
     *
     * <p>M3 atom: {@code M3_SEARCH_JUMP_CONTRACT_V1}. This operation participates in the
     * AlgorithmKernel Strategy contract; Java is the semantic oracle and JNI is an optional
     * parity-checked ConcreteStrategy shadow.</p>
     *
     * @param sorted ascending caller-owned primitive lane
     * @param from inclusive range start
     * @param to exclusive range end
     * @param value target value
     * @return first equal index in {@code [from,to)}, or {@code -1}
     * @throws NullPointerException when {@code sorted} is null
     * @throws IndexOutOfBoundsException when {@code [from,to)} is invalid
     */
    default int jumpSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return jumpSearchExactRange(sorted, from, to, value);
    }

    /**
     * Returns the first equal value in the validated sorted subrange using interpolation probes.
     *
     * <p>M3 atom: {@code M3_SEARCH_INTERPOLATION_CONTRACT_V1}. Probe arithmetic uses signed
     * 64-bit intermediates, collapses constant-value ranges without division, and lowers an equal
     * probe to the first equal index. The method is a Strategy-contract operation with the same
     * Java-oracle/native-shadow boundary as the other ordered lookup atoms.</p>
     *
     * @param sorted ascending caller-owned primitive lane
     * @param from inclusive range start
     * @param to exclusive range end
     * @param value target value
     * @return first equal index in {@code [from,to)}, or {@code -1}
     * @throws NullPointerException when {@code sorted} is null
     * @throws IndexOutOfBoundsException when {@code [from,to)} is invalid
     */
    default int interpolationSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return interpolationSearchExactRange(sorted, from, to, value);
    }

    /**
     * Returns the first equal value in a validated sorted subrange using iterative ternary search.
     *
     * <p>M3 atom: {@code M3_SEARCH_TERNARY_CONTRACT_V1}. The range is split into three bounded
     * regions per probe; an equal candidate is lowered to the first equal index so duplicates have
     * the same semantics as binary/gallop/jump/interpolation search.</p>
     */
    default int ternarySearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return ternarySearchExactRange(sorted, from, to, value);
    }

    /**
     * Returns the first equal value in the validated sorted subrange using Fibonacci partitioning.
     *
     * <p>M3 atom: {@code M3_SEARCH_FIBONACCI_CONTRACT_V1}. Fibonacci state uses {@code long}
     * intermediates so sequence growth cannot overflow a Java {@code int} index. Equality lowers
     * to the same first-equal bound contract as the other ordered-search atoms.</p>
     */
    default int fibonacciSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return fibonacciSearchExactRange(sorted, from, to, value);
    }

    private static int ternarySearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        int low = from;
        int high = to - 1;
        while (low <= high) {
            final int third = (high - low) / 3;
            final int leftProbe = low + third;
            final int rightProbe = high - third;
            final int leftValue = sorted[leftProbe];
            final int rightValue = sorted[rightProbe];

            if (leftValue == value) {
                return boundRange(sorted, from, leftProbe + 1, value, false);
            }
            if (rightValue == value) {
                return boundRange(sorted, from, rightProbe + 1, value, false);
            }
            if (value < leftValue) {
                high = leftProbe - 1;
            } else if (value > rightValue) {
                low = rightProbe + 1;
            } else {
                low = leftProbe + 1;
                high = rightProbe - 1;
            }
        }
        return -1;
    }

    private static int jumpSearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        final int length = to - from;
        if (length == 0) {
            return -1;
        }
        final int block = Math.max(1, (int) Math.sqrt(length));
        int start = from;
        int end = (int) Math.min((long) to, (long) start + block);
        while (end < to && sorted[end - 1] < value) {
            start = end;
            end = (int) Math.min((long) to, (long) start + block);
        }
        for (int index = start; index < end && sorted[index] <= value; index++) {
            if (sorted[index] == value) {
                return index;
            }
        }
        return -1;
    }

    private static int interpolationSearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        if (from == to) {
            return -1;
        }
        int low = from;
        int high = to - 1;
        while (low <= high && value >= sorted[low] && value <= sorted[high]) {
            final int lowValue = sorted[low];
            final int highValue = sorted[high];
            if (lowValue == highValue) {
                return lowValue == value ? low : -1;
            }
            final long delta = (long) value - lowValue;
            final long denominator = (long) highValue - lowValue;
            final long width = (long) high - low;
            final long quotient = delta / denominator;
            final long remainder = delta % denominator;
            final long relative = quotient * width + (remainder * width) / denominator;
            final int probe = low + (int) relative;
            final int current = sorted[probe];
            if (current < value) {
                low = probe + 1;
            } else if (current > value) {
                high = probe - 1;
            } else {
                return boundRange(sorted, low, probe + 1, value, false);
            }
        }
        return -1;
    }

    private static int fibonacciSearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        final int length = to - from;
        if (length == 0) return -1;
        long fibMinus2 = 0L, fibMinus1 = 1L, fib = 1L;
        while (fib < length) {
            fibMinus2 = fibMinus1;
            fibMinus1 = fib;
            fib = Math.addExact(fibMinus2, fibMinus1);
        }
        int offset = -1;
        while (fib > 1L) {
            final long relativeCandidate = (long) offset + fibMinus2;
            final int relative = (int) Math.min(relativeCandidate, (long) length - 1L);
            final int probe = from + relative;
            final int current = sorted[probe];
            if (current < value) {
                fib = fibMinus1;
                fibMinus1 = fibMinus2;
                fibMinus2 = fib - fibMinus1;
                offset = relative;
            } else if (current > value) {
                fib = fibMinus2;
                fibMinus1 -= fibMinus2;
                fibMinus2 = fib - fibMinus1;
            } else {
                return boundRange(sorted, from, probe + 1, value, false);
            }
        }
        final int finalRelative = offset + 1;
        if (fibMinus1 == 1L && finalRelative < length
                && sorted[from + finalRelative] == value) {
            return boundRange(sorted, from, from + finalRelative + 1, value, false);
        }
        return -1;
    }

    private static int gallopBound(
            final int[] sorted,
            final int from,
            final int to,
            final int value,
            final boolean upper) {
        if (from == to) {
            return to;
        }
        int comparison = Integer.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) {
            return from;
        }
        int low = from + 1;
        for (long offset = 1L;
                (long) from + offset < to;
                offset = (offset << 1) + 1) {
            final int probe = (int) ((long) from + offset);
            comparison = Integer.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return boundRange(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return boundRange(sorted, low, to, value, upper);
    }

    private static int boundRange(
            final int[] sorted,
            final int from,
            final int to,
            final int value,
            final boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            final int mid = low + ((high - low) >>> 1);
            final int comparison = Integer.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    default int intersectCountSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return intersectSortedUnique(left, right).length;
    }

    default int[] unionSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final int[] out = new int[Math.addExact(left.length, right.length)];
        int i = 0;
        int j = 0;
        int size = 0;
        int previous = 0;
        boolean hasPrevious = false;
        while (i < left.length || j < right.length) {
            final int value;
            if (j >= right.length || (i < left.length && left[i] <= right[j])) {
                value = left[i++];
                while (i < left.length && left[i] == value) {
                    i++;
                }
                while (j < right.length && right[j] == value) {
                    j++;
                }
            } else {
                value = right[j++];
                while (j < right.length && right[j] == value) {
                    j++;
                }
            }
            if (!hasPrevious || previous != value) {
                out[size++] = value;
                previous = value;
                hasPrevious = true;
            }
        }
        return Arrays.copyOf(out, size);
    }

    default int indexOfKmp(final byte[] haystack, final byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0) {
            return 0;
        }
        if (needle.length > haystack.length) {
            return -1;
        }
        final int[] failure = new int[needle.length];
        for (int i = 1, prefix = 0; i < needle.length; ) {
            if (needle[i] == needle[prefix]) {
                failure[i++] = ++prefix;
            } else if (prefix > 0) {
                prefix = failure[prefix - 1];
            } else {
                failure[i++] = 0;
            }
        }
        for (int i = 0, matched = 0; i < haystack.length; ) {
            if (haystack[i] == needle[matched]) {
                i++;
                matched++;
                if (matched == needle.length) {
                    return i - matched;
                }
            } else if (matched > 0) {
                matched = failure[matched - 1];
            } else {
                i++;
            }
        }
        return -1;
    }

    default int longestCommonPrefix(final byte[] left, final byte[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final int limit = Math.min(left.length, right.length);
        int index = 0;
        while (index < limit && left[index] == right[index]) {
            index++;
        }
        return index;
    }
}
