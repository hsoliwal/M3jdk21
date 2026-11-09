// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Immutable sorted long-coordinate index with primitive value and prefix-sum lanes.
 *
 * <p>Construction copies caller-owned arrays and requires strictly increasing keys.
 * Prefix sums use checked long arithmetic: an unrepresentable prefix fails at
 * construction, and an unrepresentable range difference fails at query time.
 * Zero is a valid stored value, distinguishable from an absent coordinate.
 * All range intervals are half-open, even at Long.MIN_VALUE/Long.MAX_VALUE.
 *
 * <p>Algorithm categories: LeetCode 34 (two sorted bounds), HackerRank array
 * manipulation (prefix/difference accounting), GeeksforGeeks range-sum search,
 * fastutil's 64-bit primitive-coordinate design. No donor source is copied.
 */
public final class LongCoordinateIndex {
    private final long[] keys;
    private final long[] values;
    private final long[] prefixes;

    public LongCoordinateIndex(long[] sortedUniqueKeys, long[] amounts) {
        Objects.requireNonNull(sortedUniqueKeys, "sortedUniqueKeys");
        Objects.requireNonNull(amounts, "amounts");
        if (sortedUniqueKeys.length != amounts.length) {
            throw new IllegalArgumentException("coordinate/value length mismatch");
        }
        if (sortedUniqueKeys.length == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("prefix lane capacity overflow");
        }
        for (int i = 1; i < sortedUniqueKeys.length; i++) {
            if (sortedUniqueKeys[i - 1] >= sortedUniqueKeys[i]) {
                throw new IllegalArgumentException("coordinates must be strictly increasing");
            }
        }
        // Build all owned lanes before publishing an immutable instance.
        long[] newKeys = sortedUniqueKeys.clone();
        long[] newValues = amounts.clone();
        long[] newPrefix = new long[newValues.length + 1];
        for (int i = 0; i < newValues.length; i++) {
            newPrefix[i + 1] = Math.addExact(newPrefix[i], newValues[i]);
        }
        keys = newKeys;
        values = newValues;
        prefixes = newPrefix;
    }

    /** Materializes a sorted index of the sparse array's nonzero coordinates. */
    public static LongCoordinateIndex fromSparse(SparseLongIntArray source) {
        Objects.requireNonNull(source, "source");
        long size = source.nonZeroCount();
        if (size > Integer.MAX_VALUE - 8L) {
            throw new OutOfMemoryError("too many sparse coordinates to materialize");
        }
        long[] coordinates = new long[(int) size];
        int[] cursor = new int[1];
        source.forEachNonZero((index, value) -> {
            if (cursor[0] >= coordinates.length) {
                throw new ConcurrentModificationException("sparse count changed during index capture");
            }
            coordinates[cursor[0]++] = index;
        });
        if (cursor[0] != coordinates.length) {
            throw new ConcurrentModificationException("sparse count changed during index capture");
        }
        Arrays.sort(coordinates); // Sort primitive IDs, not boxed Entry objects.
        long[] amounts = new long[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            amounts[i] = source.get(coordinates[i]);
        }
        return new LongCoordinateIndex(coordinates, amounts);
    }

    public int size() { return keys.length; }

    /** First ordinal whose signed-long coordinate is not less than key. */
    public int lowerBound(long key) {
        return bound(key, false);
    }

    /** First ordinal whose signed-long coordinate exceeds key. */
    public int upperBound(long key) {
        return bound(key, true);
    }

    private int bound(long key, boolean upper) {
        int low = 0;
        int high = keys.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (upper ? keys[middle] <= key : keys[middle] < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /** Two detached boundary ordinals [lower(from), lower(to)]. */
    public int[] ordinalsInRange(long fromInclusive, long toExclusive) {
        checkedRange(fromInclusive, toExclusive);
        return new int[] {lowerBound(fromInclusive), lowerBound(toExclusive)};
    }

    /** Number of stored coordinates in [from,to), not a count of nonzero values. */
    public int countInRange(long fromInclusive, long toExclusive) {
        checkedRange(fromInclusive, toExclusive);
        return lowerBound(toExclusive) - lowerBound(fromInclusive);
    }

    public boolean contains(long coordinate) {
        int found = lowerBound(coordinate);
        return found < keys.length && keys[found] == coordinate;
    }

    /** Returns the stored zero value, when present, instead of the fallback. */
    public long valueOrDefault(long coordinate, long fallback) {
        int found = lowerBound(coordinate);
        return found < keys.length && keys[found] == coordinate ? values[found] : fallback;
    }

    public long keyAt(int ordinal) { return keys[Objects.checkIndex(ordinal, keys.length)]; }

    public long valueAt(int ordinal) { return values[Objects.checkIndex(ordinal, values.length)]; }

    public long[] keysToArray() { return keys.clone(); }

    public long[] valuesToArray() { return values.clone(); }

    /** Checked sum of values at ordinals [from,to); O(1) after construction. */
    public long sumByOrdinal(int from, int to) {
        Objects.checkFromToIndex(from, to, keys.length);
        return Math.subtractExact(prefixes[to], prefixes[from]);
    }

    /** Checked sum over coordinates [from,to); O(log n). */
    public long sumInRange(long fromInclusive, long toExclusive) {
        checkedRange(fromInclusive, toExclusive);
        return sumByOrdinal(lowerBound(fromInclusive), lowerBound(toExclusive));
    }

    public long sumAll() { return prefixes[keys.length]; }

    /** Independent native lookup; never fall back to Java if library is absent. */
    public int lowerBoundRequiredNative(long key) {
        requireNative();
        return nativeBound(keys, key, false);
    }

    public int upperBoundRequiredNative(long key) {
        requireNative();
        return nativeBound(keys, key, true);
    }

    /**
     * Bulk lower-bound lookup over a caller-owned query lane. The returned
     * ordinals are detached and preserve every query occurrence and order.
     * O(q log n) comparisons, O(q) output; no per-query Entry or boxing.
     */
    public int[] lowerBounds(long[] queries) {
        return boundsJava(queries, false);
    }

    /** Bulk upper-bound lookup, retaining duplicate queries in encounter order. */
    public int[] upperBounds(long[] queries) {
        return boundsJava(queries, true);
    }

    private int[] boundsJava(long[] queries, boolean upper) {
        Objects.requireNonNull(queries, "queries");
        int[] result = new int[queries.length];
        int hint = 0;
        int localStreak = 0;
        long previous = 0L;
        for (int i = 0; i < queries.length; i++) {
            long query = queries[i];
            // Descending or unproven batches use the original scalar bound.
            // Enable the nearby window only after two measured small ordinal
            // advances: sawtooth/random streams retain logarithmic search.
            if (i == 0 || query < previous) {
                hint = bound(query, upper);
                localStreak = 0;
            } else if (query != previous) {
                int oldHint = hint;
                hint = localStreak == 2
                        ? boundFromHint(query, upper, hint) : bound(query, upper);
                localStreak = hint - oldHint <= LOCAL_WINDOW
                        ? Math.min(2, localStreak + 1) : 0;
            }
            result[i] = hint;
            previous = query;
        }
        return result;
    }

    /**
     * Bounded local window for nondecreasing queries. An ordinary binary
     * search takes over for a distant jump, avoiding the extra logarithmic
     * probe chain that galloping imposes on random and sawtooth workloads.
     * A nearby hit costs O(log LOCAL_WINDOW), a far hit O(log n).
     */
    private static final int LOCAL_WINDOW = 8;

    private int boundFromHint(long query, boolean upper, int from) {
        int length = keys.length;
        if (from == length) return length;
        int end = from + Math.min(LOCAL_WINDOW, length - from);
        if (precedes(keys[end - 1], query, upper)) {
            return boundBetween(query, upper, end, length);
        }
        return boundBetween(query, upper, from, end);
    }

    private int boundBetween(long query, boolean upper, int low, int high) {
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (precedes(keys[middle], query, upper)) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static boolean precedes(long coordinate, long query, boolean upper) {
        return upper ? coordinate <= query : coordinate < query;
    }

    /**
     * Bounded, required-native batch of lower-bound queries. JNI never pins
     * or retains the immutable key lane or caller's query array, and it makes
     * one native entry for the batch rather than one entry per coordinate.
     * The Java result has the exact original query length; no native fallback.
     */
    public int[] lowerBoundsRequiredNative(long[] queries) {
        Objects.requireNonNull(queries, "queries");
        requireNative();
        int[] result = nativeBounds(keys, queries, false);
        if (result == null) throw new OutOfMemoryError("native lower-bound results unavailable");
        return result;
    }

    /** Required-native upper-bound batch; query duplicates are preserved. */
    public int[] upperBoundsRequiredNative(long[] queries) {
        Objects.requireNonNull(queries, "queries");
        requireNative();
        int[] result = nativeBounds(keys, queries, true);
        if (result == null) throw new OutOfMemoryError("native upper-bound results unavailable");
        return result;
    }

    private static void requireNative() {
        if (!ArrayNative.isLoaded()) {
            String path = System.getProperty("m3.arrays.native.path");
            if (path == null || path.isBlank()) {
                throw new UnsatisfiedLinkError("M3 array JNI library is not loaded");
            }
            ArrayNative.load(Path.of(path));
        }
    }

    private static native int nativeBound(long[] values, long key, boolean upper);

    private static native int[] nativeBounds(long[] values, long[] queries, boolean upper);

    private static void checkedRange(long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("fromInclusive > toExclusive");
        }
    }
}
