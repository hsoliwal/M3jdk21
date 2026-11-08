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
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable canonical disjoint half-open ranges over the full Java int domain.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntRangeSet.java}
 * blob {@code 1915e7b9cd0930249abe2a68db8093fbf9a0a7ff} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIntRangeSet {
    private static final long MIN = Integer.MIN_VALUE;
    private static final long MAX_EXCLUSIVE = (long) Integer.MAX_VALUE + 1L;
    private final long[] starts;
    private final long[] ends;
    private final long cardinality;

    private MatIntRangeSet(long[] starts, long[] ends) {
        this.starts = starts;
        this.ends = ends;
        long count = 0;
        for (int i = 0; i < starts.length; i++) count = Math.addExact(count, ends[i] - starts[i]);
        this.cardinality = count;
    }

    public static Builder builder() { return new Builder(); }

    public static MatIntRangeSet of(long startInclusive, long endExclusive) {
        return builder().add(startInclusive, endExclusive).build();
    }

    public int ranges() { return starts.length; }
    public long cardinality() { return cardinality; }
    public long primitivePayloadBytes() { return 16L * starts.length; }
    public long start(int rangeIndex) { return starts[Objects.checkIndex(rangeIndex, starts.length)]; }
    public long endExclusive(int rangeIndex) { return ends[Objects.checkIndex(rangeIndex, ends.length)]; }

    public boolean contains(int value) {
        int index = floorRange(value);
        return index >= 0 && (long) value < ends[index];
    }

    /** Append an already sorted interval into an internal, canonical output prefix. */
    private static void appendCanonical(Builder builder, long start, long end) {
        if (builder.size != 0 && start <= builder.ends[builder.size - 1]) {
            builder.ends[builder.size - 1] = Math.max(builder.ends[builder.size - 1], end);
        } else {
            builder.add(start, end);
        }
    }

    public MatIntRangeSet union(MatIntRangeSet other) {
        Objects.requireNonNull(other, "other");
        Builder builder = builder();
        int a = 0;
        int b = 0;
        // Both inputs are already canonical: merge in start order without re-sorting.
        while (a < ranges() || b < other.ranges()) {
            if (b >= other.ranges() || (a < ranges() && starts[a] <= other.starts[b])) {
                appendCanonical(builder, starts[a], ends[a]);
                a++;
            } else {
                appendCanonical(builder, other.starts[b], other.ends[b]);
                b++;
            }
        }
        return builder.buildSortedSnapshot();
    }

    public MatIntRangeSet intersect(MatIntRangeSet other) {
        Objects.requireNonNull(other, "other");
        // An empty intersection never needs staging arrays, even for large inputs.
        Builder builder = null;
        int a = 0;
        int b = 0;
        while (a < ranges() && b < other.ranges()) {
            long start = Math.max(starts[a], other.starts[b]);
            long end = Math.min(ends[a], other.ends[b]);
            if (start < end) {
                if (builder == null) builder = builder();
                appendCanonical(builder, start, end);
            }
            if (ends[a] < other.ends[b]) a++;
            else b++;
        }
        return builder == null ? new MatIntRangeSet(new long[0], new long[0])
                : builder.buildSortedSnapshot();
    }

    public MatIntRangeSet minus(MatIntRangeSet other) {
        Objects.requireNonNull(other, "other");
        Builder builder = builder();
        int b = 0;
        for (int a = 0; a < ranges(); a++) {
            long cursor = starts[a];
            long end = ends[a];
            while (b < other.ranges() && other.ends[b] <= cursor) b++;
            int probe = b;
            while (probe < other.ranges() && other.starts[probe] < end) {
                if (other.starts[probe] > cursor) appendCanonical(builder, cursor, Math.min(end, other.starts[probe]));
                cursor = Math.max(cursor, other.ends[probe]);
                if (cursor >= end) break;
                probe++;
            }
            if (cursor < end) appendCanonical(builder, cursor, end);
        }
        return builder.buildSortedSnapshot();
    }

    public OptionalInt next(int fromInclusive) {
        long from = fromInclusive;
        int index = floorRange(fromInclusive);
        if (index >= 0 && from < ends[index]) return OptionalInt.of(fromInclusive);
        int insertion = Arrays.binarySearch(starts, from);
        if (insertion < 0) insertion = -insertion - 1;
        if (insertion >= starts.length) return OptionalInt.empty();
        return OptionalInt.of((int) starts[insertion]);
    }

    public OptionalInt previous(int fromInclusive) {
        int index = floorRange(fromInclusive);
        if (index >= 0 && (long) fromInclusive < ends[index]) return OptionalInt.of(fromInclusive);
        if (index < 0) return OptionalInt.empty();
        return OptionalInt.of((int) (ends[index] - 1L));
    }

    private int floorRange(long value) {
        int index = Arrays.binarySearch(starts, value);
        if (index >= 0) return index;
        return -index - 2;
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
        private int size;

        public Builder add(long startInclusive, long endExclusive) {
            check(startInclusive, endExclusive);
            ensure(size + 1);
            starts[size] = startInclusive;
            ends[size] = endExclusive;
            size++;
            return this;
        }

        /** Private fast path for an already sorted, disjoint output; never aliases the builder. */
        private MatIntRangeSet buildSortedSnapshot() {
            if (size == 0) return new MatIntRangeSet(new long[0], new long[0]);
            return new MatIntRangeSet(Arrays.copyOf(starts, size), Arrays.copyOf(ends, size));
        }

        public MatIntRangeSet build() {
            if (size == 0) return new MatIntRangeSet(new long[0], new long[0]);
            sort(starts, ends, 0, size - 1);
            long[] outStarts = new long[size];
            long[] outEnds = new long[size];
            int out = 0;
            long start = starts[0];
            long end = ends[0];
            for (int i = 1; i < size; i++) {
                if (starts[i] <= end) {
                    end = Math.max(end, ends[i]);
                } else {
                    outStarts[out] = start;
                    outEnds[out++] = end;
                    start = starts[i];
                    end = ends[i];
                }
            }
            outStarts[out] = start;
            outEnds[out++] = end;
            return new MatIntRangeSet(Arrays.copyOf(outStarts, out), Arrays.copyOf(outEnds, out));
        }

        /** Package-private fault-injection seam; original public API unchanged. */
        @FunctionalInterface
        interface LongCopier { long[] copy(long[] source, int capacity); }

        private void ensure(int required) {
            ensure(required, Arrays::copyOf);
        }

        /** Stage and validate both replacement arrays before publishing either reference. */
        void ensure(int required, LongCopier copier) {
            if (required <= starts.length) return;
            Objects.requireNonNull(copier, "copier");
            int next = Math.max(required, starts.length * 2);
            long[] newStarts = Objects.requireNonNull(copier.copy(starts, next), "starts copy");
            long[] newEnds = Objects.requireNonNull(copier.copy(ends, next), "ends copy");
            if (newStarts.length != next || newEnds.length != next) {
                throw new IllegalStateException("invalid M3MatIntRangeSet backing capacity");
            }
            starts = newStarts;
            ends = newEnds;
        }

        private static void sort(long[] starts, long[] ends, int low, int high) {
            while (low < high) {
                int i = low;
                int j = high;
                long pivot = starts[(low + high) >>> 1];
                while (i <= j) {
                    while (starts[i] < pivot) i++;
                    while (starts[j] > pivot) j--;
                    if (i <= j) {
                        long value = starts[i]; starts[i] = starts[j]; starts[j] = value;
                        value = ends[i]; ends[i] = ends[j]; ends[j] = value;
                        i++; j--;
                    }
                }
                if (j - low < high - i) {
                    if (low < j) sort(starts, ends, low, j);
                    low = i;
                } else {
                    if (i < high) sort(starts, ends, i, high);
                    high = j;
                }
            }
        }
    }
}
