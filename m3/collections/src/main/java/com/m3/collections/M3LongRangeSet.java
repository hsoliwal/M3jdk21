/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Mutable union of half-open signed long intervals, backed by parallel dynamic arrays.
 * Each logical range is [startInclusive, endExclusive); empty intervals are ignored,
 * reversed intervals are rejected. Ranges remain sorted, nonoverlapping and nonadjacent.
 * No Range/Entry/Node object is retained for any interval.
 *
 * <p>Point membership and intersection are O(log r); add/remove are O(r) because
 * packed array lanes move together. This is an M3 primitive analogue, not Guava's
 * RangeSet or RangeMap, and not a replacement for JDK NavigableSet. No concurrency,
 * serialization, or universal unsigned 64-bit terminal interval is promised.</p>
 */
public final class M3LongRangeSet {
    private long[] starts;
    private long[] ends;
    private int size;
    private int revision;

    public M3LongRangeSet() {
        this(0);
    }

    public M3LongRangeSet(int expectedRanges) {
        if (expectedRanges < 0) throw new IllegalArgumentException("negative expectedRanges");
        starts = new long[expectedRanges];
        ends = new long[expectedRanges];
    }

    public int rangeCount() { return size; }

    public boolean isEmpty() { return size == 0; }

    public int capacity() { return starts.length; }

    private static void valid(long from, long to) {
        if (from > to) throw new IllegalArgumentException("fromInclusive > toExclusive");
    }

    private void reserve(int required) {
        if (required <= starts.length) return;
        int old = starts.length;
        long proposed = Math.max((long) required, Math.max(8L, old + (long) (old >>> 1) + 1));
        if (proposed > Integer.MAX_VALUE - 8L) {
            if (required > Integer.MAX_VALUE - 8) throw new OutOfMemoryError("too many intervals");
            proposed = Integer.MAX_VALUE - 8L;
        }
        int capacity = (int) proposed;
        long[] grownStarts = Arrays.copyOf(starts, capacity);
        long[] grownEnds = Arrays.copyOf(ends, capacity);
        starts = grownStarts;
        ends = grownEnds;
    }

    /** Inserts a half-open range, merging overlaps and touching neighbors. */
    public void add(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return;
        int first = 0;
        while (first < size && ends[first] < fromInclusive) first++;
        int last = first;
        long start = fromInclusive;
        long end = toExclusive;
        while (last < size && starts[last] <= end) {
            start = Math.min(start, starts[last]);
            end = Math.max(end, ends[last]);
            last++;
        }
        int removed = last - first;
        if (removed == 1 && start == starts[first] && end == ends[first]) return;
        int resulting = size - removed + 1;
        reserve(resulting);
        System.arraycopy(starts, last, starts, first + 1, size - last);
        System.arraycopy(ends, last, ends, first + 1, size - last);
        starts[first] = start;
        ends[first] = end;
        size = resulting;
        revision++;
    }

    /** Removes a half-open range, retaining exact remaining pieces. */
    public void remove(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive || size == 0) return;
        int first = firstEndingAfter(fromInclusive);
        int last = first;
        while (last < size && starts[last] < toExclusive) last++;
        if (first == last) return;
        long oldStart = starts[first];
        long oldEnd = ends[last - 1];
        boolean keepLeft = oldStart < fromInclusive;
        boolean keepRight = oldEnd > toExclusive;
        int kept = (keepLeft ? 1 : 0) + (keepRight ? 1 : 0);
        int resulting = size - (last - first) + kept;
        reserve(resulting);
        System.arraycopy(starts, last, starts, first + kept, size - last);
        System.arraycopy(ends, last, ends, first + kept, size - last);
        int slot = first;
        if (keepLeft) {
            starts[slot] = oldStart;
            ends[slot++] = fromInclusive;
        }
        if (keepRight) {
            starts[slot] = toExclusive;
            ends[slot] = oldEnd;
        }
        if (resulting < size) {
            Arrays.fill(starts, resulting, size, 0L);
            Arrays.fill(ends, resulting, size, 0L);
        }
        size = resulting;
        revision++;
    }

    /** First index with an end strictly greater than the coordinate. */
    private int firstEndingAfter(long point) {
        int lo = 0;
        int hi = size;
        return M3LaneSearch.upperBound(ends, lo, hi, point);
    }

    /** Exact membership for all signed-long input values. */
    public boolean contains(long point) {
        int index = firstEndingAfter(point);
        return index < size && starts[index] <= point;
    }

    public boolean intersects(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return false;
        int index = firstEndingAfter(fromInclusive);
        return index < size && starts[index] < toExclusive;
    }

    /** Exact count, including intervals wider than Long.MAX_VALUE. */
    public BigInteger cardinality() {
        BigInteger count = BigInteger.ZERO;
        for (int i = 0; i < size; i++) {
            count = count.add(BigInteger.valueOf(ends[i]).subtract(BigInteger.valueOf(starts[i])));
        }
        return count;
    }

    /** Defensive snapshot of the packed start coordinate lane. */
    public long[] startsToArray() { return Arrays.copyOf(starts, size); }

    /** Defensive snapshot of the packed exclusive end coordinate lane. */
    public long[] endsToArray() { return Arrays.copyOf(ends, size); }

    public void forEachRange(M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedRevision = revision;
        for (int i = 0; i < size; i++) {
            consumer.accept(starts[i], ends[i]);
            if (expectedRevision != revision) throw new ConcurrentModificationException();
        }
    }

    /** Detached snapshot; no alias to caller-owned mutable lanes survives. */
    public M3LongRangeSet snapshot() {
        M3LongRangeSet copy = new M3LongRangeSet(size);
        System.arraycopy(starts, 0, copy.starts, 0, size);
        System.arraycopy(ends, 0, copy.ends, 0, size);
        copy.size = size;
        return copy;
    }

    /**
     * Selects existing intervals using precomputed full-32-bit state flags.
     * The existing JNI mask engine receives only primitive row IDs and masks;
     * no Java comparator or interval object crosses native code. The result is
     * a detached range union and can merge adjacent selected ranges.
     */
    public M3LongRangeSet selectedByState(int[] states, int requiredMask,
            int forbiddenMask, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        if (states.length != size) throw new IllegalArgumentException("unaligned range states");
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) rows[i] = i;
        int[] selected = M3IndexPathMasks.select(rows, rows, new int[][] {states},
                new int[] {requiredMask}, new int[] {forbiddenMask},
                0, size, 1, backend);
        M3LongRangeSet result = new M3LongRangeSet(selected.length);
        // The source ranges are already sorted/disjoint. The selected path is
        // encounter ordered, so scatter endpoints directly in O(selected) time.
        for (int i = 0; i < selected.length; i++) {
            int row = selected[i];
            result.starts[i] = starts[row];
            result.ends[i] = ends[row];
        }
        result.size = selected.length;
        return result;
    }

    public void clear() {
        if (size == 0) return;
        Arrays.fill(starts, 0, size, 0L);
        Arrays.fill(ends, 0, size, 0L);
        size = 0;
        revision++;
    }

    /** Checks sorted/disjoint/nonadjacent invariants in debug and testing. */
    public void verifyInvariants() {
        if (size < 0 || size > starts.length || starts.length != ends.length) {
            throw new IllegalStateException("range lane length drift");
        }
        for (int i = 0; i < size; i++) {
            if (starts[i] >= ends[i]) throw new IllegalStateException("empty stored range");
            if (i > 0 && ends[i - 1] >= starts[i]) {
                throw new IllegalStateException("ranges overlap or touch");
            }
        }
    }
}
