/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.search;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic absolute lower/upper search bounds for sorted signed integer lanes. */
public final class M3SortedIntBounds {
    private M3SortedIntBounds() {
    }

    /**
     * Computes the first index greater-or-equal and first index strictly greater for each query.
     * The input array must be sorted in nondecreasing order; concurrent mutation is unsupported.
     *
     * @param sorted immutable-for-the-call sorted signed integer array
     * @param from lower inclusive index
     * @param to upper exclusive index
     * @param keys batch of search queries
     * @return new interleaved lower/upper absolute bounds of length twice the query count
     * @throws NullPointerException if either array is null
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if query count cannot be doubled as an array length
     */
    public static int[] bounds(int[] sorted, int from, int to, int[] keys) {
        validate(sorted, from, to, keys);
        int[] result = new int[keys.length * 2];
        /*
         * No source element is in an empty range, so both insertion
         * boundaries equal 'from' for every key. Preserve a fresh result
         * array without examining the caller's query values.
         */
        if (from == to) {
            if (from != 0) {
                Arrays.fill(result, from);
            }
            return result;
        }
        if (keys.length >= 128 && nonDecreasing(keys)
                && mergeWithinBudget(to - from, keys.length)) {
            int lower = from;
            int upper = from;
            for (int i = 0; i < keys.length; i++) {
                int needle = keys[i];
                while (lower < to && sorted[lower] < needle) {
                    lower++;
                }
                while (upper < to && sorted[upper] <= needle) {
                    upper++;
                }
                result[i * 2] = lower;
                result[i * 2 + 1] = upper;
            }
        } else {
            for (int i = 0; i < keys.length; i++) {
                int needle = keys[i];
                if (i > 0 && needle == keys[i - 1]) {
                    // Exact adjacent duplicate: the preceding bounds have identical search semantics.
                    result[i * 2] = result[(i - 1) * 2];
                    result[i * 2 + 1] = result[(i - 1) * 2 + 1];
                } else {
                    // Sorted-order invariant: upperBound cannot precede lowerBound.
                    // Reuse the first bound to avoid searching the excluded prefix.
                    int lower = lowerBound(sorted, from, to, needle);
                    result[i * 2] = lower;
                    result[i * 2 + 1] = upperBound(sorted, lower, to, needle);
                }
            }
        }
        return result;
    }

    /**
     * Convenience full-range operation with the same contract as the range overload.
     *
     * @param sorted non-null sorted integer array
     * @param keys non-null integer query batch
     * @return newly allocated interleaved lower/upper bounds
     */
    public static int[] bounds(int[] sorted, int[] keys) {
        Objects.requireNonNull(sorted, "sorted");
        return bounds(sorted, 0, sorted.length, keys);
    }

    /**
     * Validates identical Java/native boundary conditions in a fixed exception order.
     *
     * @param sorted source array
     * @param from range start
     * @param to range end
     * @param keys query array
     */
    static void validate(int[] sorted, int from, int to, int[] keys) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(keys, "keys");
        Objects.checkFromToIndex(from, to, sorted.length);
        if (keys.length > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("query batch too large to return two bounds per key");
        }
    }

    /** Admit a full range scan only within the binary-search comparison budget. */
    private static boolean mergeWithinBudget(int rangeLength, int queryCount) {
        // bitLength(N) bounds each binary search for N >= 0. Widen before multiplying;
        // a sparse ordered batch must not force a scan over an arbitrarily large range.
        return queryCount >= 128 && (long) rangeLength <= (long) queryCount
                * (Integer.SIZE - Integer.numberOfLeadingZeros(rangeLength));
    }

    /** Check monotonic order once, without sorting or mutating callers' query values. */
    private static boolean nonDecreasing(int[] keys) {
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] < keys[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /** Returns first absolute index with value at least needle, or {@code to}. */
    static int lowerBound(int[] sorted, int from, int to, int needle) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (sorted[mid] < needle) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /** Returns first absolute index with value greater than needle, or {@code to}. */
    static int upperBound(int[] sorted, int from, int to, int needle) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (sorted[mid] <= needle) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
