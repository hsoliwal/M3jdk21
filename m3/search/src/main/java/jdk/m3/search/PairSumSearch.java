// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/problem/PairSumSearch.java (blob aea33eb85270) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Primitive, deterministic pair-sum search with no challenge-site output convention.
 *
 * <p>Both methods return the lexicographically earliest distinct zero-based pair
 * {@code (i,j)} satisfying {@code i < j} and {@code a[i] + a[j] == target},
 * packed as two unsigned 32-bit fields in a {@code long}; or {@link #NOT_FOUND}.
 * The packed value is suitable for a future opt-in JNI implementation with
 * identical results and no object allocation at the algorithm boundary.
 * This class does not copy, sort or mutate caller arrays.
 */
public final class PairSumSearch {
    public static final long NOT_FOUND = -1L;
    private static final long MIN_PAIR_SUM = 2L * Integer.MIN_VALUE;
    private static final long MAX_PAIR_SUM = 2L * Integer.MAX_VALUE;

    private PairSumSearch() {}

    /**
     * Monotonically sorted input: O(n) worst-case time and O(1) extra memory.
     * Validates the sorted precondition even if a target cannot be represented
     * by two {@code int} elements.
     *
     * @throws NullPointerException if the array is null
     * @throws IllegalArgumentException if the array is not nondecreasing
     */
    public static long firstSorted(int[] sorted, long target) {
        Objects.requireNonNull(sorted, "sorted");
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i - 1] > sorted[i]) {
                throw new IllegalArgumentException("nondecreasing sorted array required");
            }
        }
        if (sorted.length < 2 || impossibleTarget(target)) return NOT_FOUND;

        int left = 0;
        int right = sorted.length - 1;
        while (left < right) {
            long sum = (long) sorted[left] + sorted[right];
            if (sum < target) {
                left++;
            } else if (sum > target) {
                right--;
            } else {
                // The first matching left is minimal. Select the earliest
                // duplicate right index as well, for a stable canonical pair.
                while (left + 1 < right && sorted[right - 1] == sorted[right]) {
                    right--;
                }
                return pack(left, right);
            }
        }
        return NOT_FOUND;
    }

    /**
     * Unsorted input: O(n) expected time and O(n) extra space. A suffix map
     * keeps the first later occurrence of each value so an earlier left index
     * always replaces a later candidate; duplicates never reuse one index.
     *
     * @throws NullPointerException if the array is null
     */
    public static long firstUnsorted(int[] values, long target) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2 || impossibleTarget(target)) return NOT_FOUND;

        Map<Integer, Integer> earliestSuffix = new HashMap<>();
        long first = NOT_FOUND;
        for (int left = values.length - 1; left >= 0; left--) {
            long needed = target - values[left];
            if (needed >= Integer.MIN_VALUE && needed <= Integer.MAX_VALUE) {
                Integer right = earliestSuffix.get((int) needed);
                if (right != null) first = pack(left, right);
            }
            earliestSuffix.put(values[left], left);
        }
        return first;
    }

    /** Decode the first zero-based index, or -1 when no pair was found. */
    public static int leftIndex(long packed) {
        return packed == NOT_FOUND ? -1 : (int) (packed >>> 32);
    }

    /** Decode the second zero-based index, or -1 when no pair was found. */
    public static int rightIndex(long packed) {
        return packed == NOT_FOUND ? -1 : (int) packed;
    }

    private static boolean impossibleTarget(long target) {
        return target < MIN_PAIR_SUM || target > MAX_PAIR_SUM;
    }

    private static long pack(int left, int right) {
        return ((long) left << 32) | (right & 0xffff_ffffL);
    }
}
