// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable ascending signed-int set backed by one parent-owned primitive lane.
 * <p>A caller's source is validated and copied once. Reads never expose that lane;
 * returned arrays are always independent mutable compatibility snapshots. No per-element
 * entry or iterator objects are retained. The instance supports safe concurrent reads.
 */
public final class M3SortedIntSet {
    /** Maximum input cardinality and exact result cardinality for bounded operations. */
    public static final int MAX_ITEMS = 2_000_000;
    private static final int GALLOP_RATIO = 16;
    private final int[] values;

    private M3SortedIntSet(int[] ownedValues) {
        this.values = ownedValues;
    }

    /**
     * Freeze a strictly ascending signed-order input, rejecting duplicates and inversions.
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if source is not strictly ascending or exceeds the cap
     */
    public static M3SortedIntSet copyOfSorted(int[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length > MAX_ITEMS) throw new IllegalArgumentException("set too large");
        for (int i = 1; i < source.length; i++) {
            if (source[i - 1] >= source[i]) {
                throw new IllegalArgumentException("set must be strictly increasing");
            }
        }
        return new M3SortedIntSet(source.clone());
    }

    public int size() {
        return values.length;
    }

    /** Fresh mutable copy; never aliases the immutable owner. */
    public int[] toArray() {
        return values.clone();
    }

    /** First signed-order position whose value is at least {@code target}. */
    public int lowerBound(int target) {
        return lowerBound(values, 0, values.length, target);
    }

    public boolean contains(int value) {
        int i = lowerBound(value);
        return i < values.length && values[i] == value;
    }

    /** Sorted, distinct intersection with a detached mutable result. */
    public int[] intersection(M3SortedIntSet other) {
        Objects.requireNonNull(other, "other");
        int[] a = values;
        int[] b = other.values;
        if (a.length > b.length) {
            int[] swap = a;
            a = b;
            b = swap;
        }
        int[] output = new int[a.length];
        int used = 0;
        if ((long) a.length * GALLOP_RATIO < b.length) {
            int cursor = 0;
            for (int value : a) {
                cursor = gallop(b, cursor, value);
                if (cursor == b.length) break;
                if (b[cursor] == value) {
                    output[used++] = value;
                    cursor++;
                }
            }
        } else {
            int i = 0;
            int j = 0;
            while (i < a.length && j < b.length) {
                if (a[i] < b[j]) i++;
                else if (a[i] > b[j]) j++;
                else {
                    output[used++] = a[i++];
                    j++;
                }
            }
        }
        return Arrays.copyOf(output, used);
    }

    /**
     * Sorted, distinct union. Refuses any exact output larger than MAX_ITEMS;
     * never returns a silently truncated candidate.
     */
    public int[] union(M3SortedIntSet other) {
        Objects.requireNonNull(other, "other");
        int[] a = values;
        int[] b = other.values;
        int[] result = new int[(int) Math.min((long) MAX_ITEMS,
                (long) a.length + b.length)];
        int i = 0;
        int j = 0;
        int count = 0;
        while (i < a.length || j < b.length) {
            int next;
            if (j == b.length || (i < a.length && a[i] < b[j])) next = a[i++];
            else if (i == a.length || b[j] < a[i]) next = b[j++];
            else {
                next = a[i++];
                j++;
            }
            if (count == MAX_ITEMS) throw new IllegalArgumentException("result too large");
            result[count++] = next;
        }
        return Arrays.copyOf(result, count);
    }

    /** Sorted, distinct set subtraction: values in this set but not in other. */
    public int[] difference(M3SortedIntSet other) {
        Objects.requireNonNull(other, "other");
        int[] a = values;
        int[] b = other.values;
        int[] output = new int[a.length];
        int i = 0;
        int j = 0;
        int count = 0;
        while (i < a.length) {
            if (j == b.length || a[i] < b[j]) output[count++] = a[i++];
            else if (a[i] > b[j]) j++;
            else {
                i++;
                j++;
            }
        }
        return Arrays.copyOf(output, count);
    }

    /** Package-private access strictly for the opt-in native atom; never public API. */
    int[] ownedValues() {
        return values;
    }

    private static int lowerBound(int[] lane, int low, int high, int value) {
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (lane[middle] < value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /** Monotone exponential probe, followed by exact binary search. */
    private static int gallop(int[] lane, int cursor, int value) {
        int n = lane.length;
        if (cursor >= n || lane[cursor] >= value) return cursor;
        long span = 1;
        while ((long) cursor + span < n && lane[(int) (cursor + span)] < value) {
            span <<= 1;
        }
        int low = (int) Math.min(n, (long) cursor + (span >>> 1) + 1);
        int high = (int) Math.min(n, (long) cursor + span + 1);
        return lowerBound(lane, low, high, value);
    }
}
