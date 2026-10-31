// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Primitive mask-based uniqueness, missingness and foreign-key quality checks.
 * These are exact integer-key checks; not a SQL constraint engine or schema validator.
 */
public final class M3DataQuality {
    private M3DataQuality() { }

    /** Flags all rows whose key occurs at least twice, including first occurrences. */
    public static long[] duplicates(int[] keys) {
        Objects.requireNonNull(keys, "keys");
        if (keys.length > 1_000_000) throw new IllegalArgumentException("row budget");
        long[] ordered = new long[keys.length];
        for (int i = 0; i < keys.length; i++) ordered[i] = ((long) keys[i] << 32) | (i & 0xffffffffL);
        Arrays.sort(ordered);
        long[] mask = new long[(keys.length + 63) >>> 6];
        for (int first = 0; first < ordered.length;) {
            int last = first + 1;
            while (last < ordered.length && (int) (ordered[last] >> 32) == (int) (ordered[first] >> 32))
                last++;
            if (last - first > 1) for (int j = first; j < last; j++) {
                int at = (int) ordered[j];
                mask[at >>> 6] |= 1L << (at & 63);
            }
            first = last;
        }
        return mask;
    }

    /** FK violations. The target dictionary is sorted unique and must be admitted as such. */
    public static long[] missingReferences(int[] references, int[] sortedKeys) {
        Objects.requireNonNull(references); Objects.requireNonNull(sortedKeys);
        for (int i = 1; i < sortedKeys.length; i++)
            if (sortedKeys[i] <= sortedKeys[i - 1]) throw new IllegalArgumentException("dictionary order");
        long[] mask = new long[(references.length + 63) >>> 6];
        for (int i = 0; i < references.length; i++) if (Arrays.binarySearch(sortedKeys, references[i]) < 0)
            mask[i >>> 6] |= 1L << (i & 63);
        return mask;
    }

    /** Row-major missing-cell bitmap; returns missing count for each column. */
    public static long[] missingCounts(long[] missing, int rows, int columns) {
        Objects.requireNonNull(missing);
        if (rows < 0 || columns < 0 || (long) rows * columns > 50_000_000L)
            throw new IllegalArgumentException("missingness geometry");
        long slots = (long) rows * columns;
        if (missing.length != (slots + 63) / 64) throw new IllegalArgumentException("bitmap length");
        if ((slots & 63) != 0 && missing.length > 0
                && (missing[missing.length - 1] >>> (slots & 63)) != 0)
            throw new IllegalArgumentException("bits outside missingness domain");
        long[] count = new long[columns];
        for (long at = 0; at < slots; at++)
            if ((missing[(int) (at >>> 6)] & (1L << ((int) at & 63))) != 0)
                count[(int) (at % columns)]++;
        return count;
    }
}
