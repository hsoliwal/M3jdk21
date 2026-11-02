// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

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
public final class M3LongCoordinateIndex {
    private final long[] keys;
    private final long[] values;
    private final long[] prefixes;

    public M3LongCoordinateIndex(long[] sortedUniqueKeys, long[] amounts) {
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
    public static M3LongCoordinateIndex fromSparse(M3SparseLongIntArray source) {
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
        return new M3LongCoordinateIndex(coordinates, amounts);
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

    private static void requireNative() {
        if (!M3ArrayNative.isLoaded()) {
            String path = System.getProperty("m3.arrays.native.path");
            if (path == null || path.isBlank()) {
                throw new UnsatisfiedLinkError("M3 array JNI library is not loaded");
            }
            M3ArrayNative.load(Path.of(path));
        }
    }

    private static native int nativeBound(long[] values, long key, boolean upper);

    private static void checkedRange(long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("fromInclusive > toExclusive");
        }
    }
}
