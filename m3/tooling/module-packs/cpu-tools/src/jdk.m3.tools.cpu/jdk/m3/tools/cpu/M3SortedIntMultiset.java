// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable ascending signed-int multiset with one parent-owned primitive lane.
 * Duplicates are retained, unlike {@link M3SortedIntSet}. Input is copied on admission;
 * every public array export is a fresh caller-owned copy. No per-value structural objects.
 * The instance is safe for concurrent reads after construction.
 */
public final class M3SortedIntMultiset {
    /** Input and exact-output size bound; never silently truncate results. */
    public static final int MAX_ITEMS = 2_000_000;
    private static final int INTERSECTION = 0;
    private static final int DIFFERENCE = 1;
    private static final int UNION = 2;

    private final int[] values;

    private M3SortedIntMultiset(int[] owned) {
        values = owned;
    }

    /** Freeze an ascending signed multiset; equal adjacent values are valid. */
    public static M3SortedIntMultiset copyOfSorted(int[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length > MAX_ITEMS) throw new IllegalArgumentException("multiset too large");
        for (int i = 1; i < source.length; ++i) {
            if (source[i - 1] > source[i]) {
                throw new IllegalArgumentException("multiset must be nondecreasing");
            }
        }
        return new M3SortedIntMultiset(source.clone());
    }

    public int size() { return values.length; }

    /** Detached array; mutating it never changes this multiset. */
    public int[] toArray() { return values.clone(); }

    /** Signed-order first position whose value is not less than target. */
    public int lowerBound(int target) { return bound(target, false); }

    /** Exact duplicate count of target. */
    public int countOf(int target) { return bound(target, true) - bound(target, false); }

    /** Sorted multiset intersection, retaining min(leftCount,rightCount) copies. */
    public int[] intersection(M3SortedIntMultiset other) {
        return merge(Objects.requireNonNull(other, "other"), INTERSECTION);
    }

    /** Sorted positive subtraction, retaining max(leftCount-rightCount,0) copies. */
    public int[] difference(M3SortedIntMultiset other) {
        return merge(Objects.requireNonNull(other, "other"), DIFFERENCE);
    }

    /** Sorted multiset union, retaining max(leftCount,rightCount) copies. */
    public int[] union(M3SortedIntMultiset other) {
        return merge(Objects.requireNonNull(other, "other"), UNION);
    }

    /**
     * For each value in original, return it ONCE if original has more copies than
     * this observed multiset. This is not ordinary multiset difference.
     */
    public int[] missingDistinctFrom(M3SortedIntMultiset original) {
        Objects.requireNonNull(original, "original");
        int[] answer = null;
        int[] observed = values;
        int[] expected = original.values;
        for (int pass = 0; pass < 2; pass++) {
            int i = 0;
            int j = 0;
            int used = 0;
            while (j < expected.length) {
                int value = expected[j];
                int originalStart = j;
                while (j < expected.length && expected[j] == value) j++;
                while (i < observed.length && observed[i] < value) i++;
                int observedStart = i;
                while (i < observed.length && observed[i] == value) i++;
                if (j - originalStart > i - observedStart) {
                    if (answer != null) answer[used] = value;
                    used++;
                }
            }
            if (pass == 0) answer = new int[used];
            else if (used != answer.length) throw new AssertionError("immutable snapshot drift");
        }
        return answer;
    }

    /** Returns the exact multiset result without temporary per-element wrappers. */
    private int[] merge(M3SortedIntMultiset other, int operation) {
        int[] left = values;
        int[] right = other.values;
        int[] result = null;
        for (int pass = 0; pass < 2; pass++) {
            int i = 0;
            int j = 0;
            int used = 0;
            while (i < left.length || j < right.length) {
                final int value = j == right.length
                        || (i < left.length && left[i] <= right[j])
                        ? left[i] : right[j];
                int a = 0;
                int b = 0;
                while (i < left.length && left[i] == value) { i++; a++; }
                while (j < right.length && right[j] == value) { j++; b++; }
                int count = switch (operation) {
                    case INTERSECTION -> Math.min(a, b);
                    case DIFFERENCE -> Math.max(a - b, 0);
                    case UNION -> Math.max(a, b);
                    default -> throw new AssertionError("unreachable operation");
                };
                if (count > MAX_ITEMS - used) {
                    throw new IllegalArgumentException("multiset result too large");
                }
                if (result != null && count != 0) {
                    Arrays.fill(result, used, used + count, value);
                }
                used += count;
            }
            if (pass == 0) result = new int[used];
            else if (used != result.length) throw new AssertionError("immutable snapshot drift");
        }
        return result;
    }

    private int bound(int target, boolean upper) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (values[mid] < target || (upper && values[mid] == target)) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** Package-private ingress for the optional JNI atom; never part of public API. */
    int[] ownedValues() { return values; }
}
