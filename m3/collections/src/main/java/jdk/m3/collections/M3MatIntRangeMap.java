/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable non-overlapping half-open int range map.
 * Adjacent equal-valued ranges are merged at build time.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntRangeMap.java}
 * blob {@code a0c01a9fbbec641774dbbe617d53b2399b6df6b6} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIntRangeMap {
    private static final long MIN = Integer.MIN_VALUE;
    private static final long MAX_EXCLUSIVE = (long) Integer.MAX_VALUE + 1L;
    private final long[] starts;
    private final long[] ends;
    private final int[] values;

    private M3MatIntRangeMap(long[] starts, long[] ends, int[] values) {
        this.starts = starts;
        this.ends = ends;
        this.values = values;
    }

    public static Builder builder() { return new Builder(); }
    public int ranges() { return starts.length; }
    public long primitivePayloadBytes() { return 20L * ranges(); }
    public long start(int index) { return starts[Objects.checkIndex(index, ranges())]; }
    public long endExclusive(int index) { return ends[Objects.checkIndex(index, ranges())]; }
    public int valueAtRange(int index) { return values[Objects.checkIndex(index, ranges())]; }

    public boolean containsKey(int key) { return rangeIndex(key) >= 0; }

    public int get(int key) {
        int index = rangeIndex(key);
        if (index < 0) throw new IllegalArgumentException("no range for key: " + key);
        return values[index];
    }

    public int getOrDefault(int key, int fallback) {
        int index = rangeIndex(key);
        return index < 0 ? fallback : values[index];
    }

    public int rangeIndex(int key) {
        int index = Arrays.binarySearch(starts, (long) key);
        if (index < 0) index = -index - 2;
        return index >= 0 && (long) key < ends[index] ? index : -1;
    }

    /** Returns only overlapping source range indexes; retained payload is exactly four bytes per match. */
    public M3MatIntList overlappingRangeIndexes(long fromInclusive, long toExclusive) {
        check(fromInclusive, toExclusive);
        int first = firstEndAfter(fromInclusive);
        int limit = firstStartAtOrAfter(toExclusive);
        int count = Math.max(0, limit - first);
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = first + i;
        }
        return M3MatIntList.trustedView(indexes, 0, count);
    }

    private int firstEndAfter(long key) {
        int low = 0;
        int high = ends.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (ends[middle] <= key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int firstStartAtOrAfter(long key) {
        int low = 0;
        int high = starts.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (starts[middle] < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static void check(long start, long end) {
        if (start < MIN || end > MAX_EXCLUSIVE || start >= end) {
            throw new IllegalArgumentException("invalid int range [" + start + "," + end + ")");
        }
    }

    public static final class Builder {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Builder() {
        }

        private long[] starts = new long[8];
        private long[] ends = new long[8];
        private int[] values = new int[8];
        private int size;

        public Builder put(long startInclusive, long endExclusive, int value) {
            check(startInclusive, endExclusive);
            ensure(size + 1);
            starts[size] = startInclusive;
            ends[size] = endExclusive;
            values[size] = value;
            size++;
            return this;
        }

        public M3MatIntRangeMap build() {
            if (size == 0) return new M3MatIntRangeMap(new long[0], new long[0], new int[0]);
            sort(starts, ends, values, 0, size - 1);
            long[] outStarts = new long[size];
            long[] outEnds = new long[size];
            int[] outValues = new int[size];
            int out = 0;
            for (int i = 0; i < size; i++) {
                if (i > 0 && starts[i] < ends[i - 1]) {
                    throw new IllegalArgumentException("overlapping range map entries");
                }
                if (out > 0 && outEnds[out - 1] == starts[i] && outValues[out - 1] == values[i]) {
                    outEnds[out - 1] = ends[i];
                } else {
                    outStarts[out] = starts[i];
                    outEnds[out] = ends[i];
                    outValues[out] = values[i];
                    out++;
                }
            }
            return new M3MatIntRangeMap(
                    Arrays.copyOf(outStarts, out),
                    Arrays.copyOf(outEnds, out),
                    Arrays.copyOf(outValues, out));
        }

        /** Test seam only: the original public put contract remains unchanged. */
        @FunctionalInterface
        interface LongCopier { long[] copy(long[] source, int capacity); }
        @FunctionalInterface
        interface IntCopier { int[] copy(int[] source, int capacity); }

        private void ensure(int required) {
            if (required <= starts.length) return;
            ensure(required, Arrays::copyOf, Arrays::copyOf);
        }

        /** Stage all three copies before changing the observable backing of a builder. */
        void ensure(int required, LongCopier longs, IntCopier ints) {
            if (required <= starts.length) return;
            Objects.requireNonNull(longs, "long copier");
            Objects.requireNonNull(ints, "int copier");
            int next = Math.max(required, starts.length * 2);
            long[] newStarts = Objects.requireNonNull(longs.copy(starts, next), "starts copy");
            long[] newEnds = Objects.requireNonNull(longs.copy(ends, next), "ends copy");
            int[] newValues = Objects.requireNonNull(ints.copy(values, next), "values copy");
            if (newStarts.length != next || newEnds.length != next || newValues.length != next) {
                throw new IllegalStateException("invalid M3MatIntRangeMap backing capacity");
            }
            starts = newStarts;
            ends = newEnds;
            values = newValues;
        }

        private static void sort(long[] starts, long[] ends, int[] values, int low, int high) {
            while (low < high) {
                int i = low;
                int j = high;
                long pivot = starts[(low + high) >>> 1];
                while (i <= j) {
                    while (starts[i] < pivot) i++;
                    while (starts[j] > pivot) j--;
                    if (i <= j) {
                        long l = starts[i]; starts[i] = starts[j]; starts[j] = l;
                        l = ends[i]; ends[i] = ends[j]; ends[j] = l;
                        int v = values[i]; values[i] = values[j]; values[j] = v;
                        i++; j--;
                    }
                }
                if (j - low < high - i) {
                    if (low < j) sort(starts, ends, values, low, j);
                    low = i;
                } else {
                    if (i < high) sort(starts, ends, values, i, high);
                    high = j;
                }
            }
        }
    }
}
