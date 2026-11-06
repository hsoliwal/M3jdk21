// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

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

    /**
     * Exact UTF-16 grouping on the target's String facade. Group ids follow first
     * occurrence, not lexical order. Input references are borrowed only for this
     * call and must not change during it. At most one million non-null rows.
     * No spelling, String reference, or per-row structural object is retained.
     */
    public static StringGroups groupStrings(String[] values) {
        Objects.requireNonNull(values, "values");
        int n = values.length;
        if (n > 1_000_000) throw new IllegalArgumentException("string row budget");
        for (String value : values) Objects.requireNonNull(value, "value");
        int[] order = new int[n], scratch = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        // Stable bottom-up sorting of coordinates, never sorting/copying spelling.
        for (int width = 1; width < n; width *= 2) {
            for (int low = 0; low < n; low += 2 * width) {
                int middle = Math.min(n, low + width), high = Math.min(n, low + 2 * width);
                int left = low, right = middle;
                for (int out = low; out < high; out++) {
                    boolean takeLeft = right == high || (left < middle
                            && values[order[left]].compareTo(values[order[right]]) <= 0);
                    scratch[out] = takeLeft ? order[left++] : order[right++];
                }
            }
            int[] swap = order; order = scratch; scratch = swap;
        }
        int[] rowGroup = new int[n];
        int distinct = 0;
        for (int rank = 0; rank < n; rank++) {
            if (rank == 0 || !values[order[rank]].equals(values[order[rank - 1]])) distinct++;
            rowGroup[order[rank]] = distinct - 1;
        }
        int[] remap = new int[distinct], first = new int[distinct], count = new int[distinct];
        Arrays.fill(remap, -1);
        int used = 0;
        for (int row = 0; row < n; row++) {
            int lexicalGroup = rowGroup[row], group = remap[lexicalGroup];
            if (group < 0) {
                group = used++; remap[lexicalGroup] = group; first[group] = row;
            }
            rowGroup[row] = group; count[group]++;
        }
        return new StringGroups(rowGroup, first, count);
    }

    /** Immutable metadata over original row coordinates; no retained input payload. */
    public static final class StringGroups {
        private final int[] group, first, count;
        private StringGroups(int[] group, int[] first, int[] count) {
            this.group = group; this.first = first; this.count = count;
        }
        public int rows() { return group.length; }
        public int groups() { return first.length; }
        public int groupOf(int row) { return group[Objects.checkIndex(row, group.length)]; }
        public int firstRow(int id) { return first[Objects.checkIndex(id, first.length)]; }
        public int count(int id) { return count[Objects.checkIndex(id, count.length)]; }
        public int[] groupIds() { return group.clone(); }
        /** Includes every member of a repeated group, including its first row. */
        public long[] duplicateMask() {
            long[] mask = new long[(group.length + 63) >>> 6];
            for (int i = 0; i < group.length; i++) if (count[group[i]] > 1)
                mask[i >>> 6] |= 1L << (i & 63);
            return mask;
        }
    }

    /**
     * Exact UTF-16 grouping of immutable M3 views and other CharSequences.
     * Group ids follow first occurrence. Sources and their array must be stable
     * throughout this call. Every non-null row and length is admitted before
     * comparison; at most one million rows. Object equals/hash and toString are
     * never used as text authority. Only primitive metadata survives the call.
     * Work is coordinate merge-sort comparisons plus the compared code units;
     * source immutability and inexpensive charAt are caller contracts, not guesses.
     */
    public static StringGroups groupViews(CharSequence[] values) {
        Objects.requireNonNull(values, "values");
        int n = values.length;
        if (n > 1_000_000) throw new IllegalArgumentException("view row budget");
        int[] lengths = new int[n];
        for (int row = 0; row < n; row++) {
            lengths[row] = Objects.requireNonNull(values[row], "value").length();
            if (lengths[row] < 0) throw new IllegalArgumentException("negative view length");
        }
        int[] order = new int[n], scratch = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        // Reuse the owner's stable primitive coordinate-sort/group/remap pattern.
        for (int width = 1; width < n; width *= 2) {
            for (int low = 0; low < n; low += 2 * width) {
                int middle = Math.min(n, low + width), high = Math.min(n, low + 2 * width);
                int left = low, right = middle;
                for (int out = low; out < high; out++) {
                    boolean takeLeft = right == high || (left < middle
                            && compareViews(values[order[left]], lengths[order[left]],
                                    values[order[right]], lengths[order[right]]) <= 0);
                    scratch[out] = takeLeft ? order[left++] : order[right++];
                }
            }
            int[] swap = order; order = scratch; scratch = swap;
        }
        int[] rowGroup = new int[n];
        int distinct = 0;
        for (int rank = 0; rank < n; rank++) {
            if (rank == 0 || compareViews(values[order[rank]], lengths[order[rank]],
                    values[order[rank - 1]], lengths[order[rank - 1]]) != 0) distinct++;
            rowGroup[order[rank]] = distinct - 1;
        }
        int[] remap = new int[distinct], first = new int[distinct], count = new int[distinct];
        Arrays.fill(remap, -1);
        int used = 0;
        for (int row = 0; row < n; row++) {
            int lexicalGroup = rowGroup[row], group = remap[lexicalGroup];
            if (group < 0) {
                group = used++; remap[lexicalGroup] = group; first[group] = row;
            }
            rowGroup[row] = group; count[group]++;
        }
        return new StringGroups(rowGroup, first, count);
    }

    /** Exact comparator; prepared lengths are geometry, never equality evidence. */
    private static int compareViews(CharSequence left, int leftLength,
                                    CharSequence right, int rightLength) {
        if (left == right) return 0;
        if (left instanceof String a && right instanceof String b) return a.compareTo(b);
        int common = Math.min(leftLength, rightLength);
        for (int i = 0; i < common; i++) {
            int difference = left.charAt(i) - right.charAt(i);
            if (difference != 0) return difference;
        }
        return leftLength - rightLength;
    }
}
