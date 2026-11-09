/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.search;

import java.util.Objects;

/** Deterministic absolute lower/upper search bounds for nondecreasing signed-long lanes. */
public final class SortedLongBounds {
    private static final int MAX_EQUAL_PROBE = 8;

    private SortedLongBounds() {
    }

    /**
     * Finds the first index greater than or equal to, and first strictly greater than, each key.
     * The source slice must be sorted in nondecreasing signed-long order and not concurrently modified.
     * No input array is changed or retained; the result always has independent ownership.
     *
     * @param sorted non-null, sorted-for-the-call signed-long source lane
     * @param from start inclusive
     * @param to end exclusive
     * @param keys non-null batch of arbitrary signed-long query keys
     * @return freshly allocated absolute lower/upper bounds interleaved by query
     * @throws NullPointerException when either source or keys is null
     * @throws IndexOutOfBoundsException when the source range is invalid
     * @throws IllegalArgumentException when twice the query count cannot fit a Java array
     */
    public static int[] bounds(long[] sorted, int from, int to, long[] keys) {
        validate(sorted, from, to, keys);
        int[] result = new int[keys.length * 2];
        if (keys.length >= 128 && nonDecreasing(keys)) {
            int lower = from;
            int upper = from;
            for (int i = 0; i < keys.length; i++) {
                long key = keys[i];
                while (lower < to && sorted[lower] < key) {
                    lower++;
                }
                while (upper < to && sorted[upper] <= key) {
                    upper++;
                }
                result[i * 2] = lower;
                result[i * 2 + 1] = upper;
            }
        } else {
            for (int i = 0; i < keys.length; i++) {
                long key = keys[i];
                if (i > 0 && key == keys[i - 1]) {
                    // Same signed-long value: reuse the preceding absolute pair without re-searching.
                    result[i * 2] = result[(i - 1) * 2];
                    result[i * 2 + 1] = result[(i - 1) * 2 + 1];
                } else {
                    int lower = lowerBound(sorted, from, to, key);
                    int upper = lower;
                    // Equal values form one contiguous run. Probe a bounded prefix before binary search.
                    while (upper < to && upper - lower < MAX_EQUAL_PROBE && sorted[upper] == key) {
                        upper++;
                    }
                    if (upper < to && upper - lower == MAX_EQUAL_PROBE) {
                        upper = upperBound(sorted, upper, to, key);
                    }
                    result[i * 2] = lower;
                    result[i * 2 + 1] = upper;
                }
            }
        }
        return result;
    }

    /**
     * Finds lower/upper bounds over the complete source lane.
     *
     * @param sorted non-null sorted source
     * @param keys non-null query batch
     * @return new interleaved absolute bound pairs
     * @throws NullPointerException if either argument is null
     */
    public static int[] bounds(long[] sorted, long[] keys) {
        Objects.requireNonNull(sorted, "sorted");
        return bounds(sorted, 0, sorted.length, keys);
    }

    /** Exact Java/JNI validation order for the long-lane contract. */
    static void validate(long[] sorted, int from, int to, long[] keys) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(keys, "keys");
        Objects.checkFromToIndex(from, to, sorted.length);
        if (keys.length > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("query batch too large to return two bounds per key");
        }
    }

    /** Returns true only for nondecreasing signed-long keys; no mutation or allocation. */
    private static boolean nonDecreasing(long[] keys) {
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] < keys[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /** First absolute index whose value is at least the key, or {@code to}. */
    static int lowerBound(long[] sorted, int from, int to, long key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (sorted[mid] < key) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /** First absolute index whose value is strictly greater than the key, or {@code to}. */
    static int upperBound(long[] sorted, int from, int to, long key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (sorted[mid] <= key) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
