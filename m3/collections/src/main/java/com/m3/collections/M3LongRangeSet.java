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
    // Lazily derived unsigned 64-bit cumulative end ranks; invalidated by mutation.
    // A single long[] plane, never a per-range boxed prefix object.
    private long[] cumulative;

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
        cumulative = null;
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
        cumulative = null;
        revision++;
    }

    /** First index with an end strictly greater than the coordinate. */
    private int firstEndingAfter(long point) {
        int lo = 0;
        int hi = size;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (ends[mid] <= point) lo = mid + 1;
            else hi = mid;
        }
        return lo;
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

    /** Build one ephemeral/reusable primitive index plane for rank/select. */
    private void prepareRanks() {
        if (cumulative != null) return;
        long[] prepared = new long[size];
        long count = 0L;
        for (int i = 0; i < size; i++) {
            // Native signed subtraction is deliberately interpreted as unsigned.
            // The whole representable half-open signed long domain has at most
            // 2^64 - 1 elements, so the accumulated unsigned count never wraps.
            count += ends[i] - starts[i];
            prepared[i] = count;
        }
        cumulative = prepared;
    }

    private static BigInteger unsigned(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }

    /**
     * O(1) after O(r) lazy rank preparation. No per-interval BigInteger storage.
     * Exact counts through spans wider than signed long range.
     */
    public BigInteger cardinality() {
        if (size == 0) return BigInteger.ZERO;
        prepareRanks();
        return unsigned(cumulative[size - 1]);
    }

    /**
     * Count elements strictly less than {@code point} in signed natural order.
     * O(log r) after one O(r) lazy preparation, with unsigned 64-bit indexes.
     */
    public BigInteger rank(long point) {
        if (size == 0) return BigInteger.ZERO;
        prepareRanks();
        int slot = firstEndingAfter(point);
        if (slot == size) return unsigned(cumulative[size - 1]);
        long before = slot == 0 ? 0L : cumulative[slot - 1];
        return unsigned(starts[slot] >= point ? before : before + point - starts[slot]);
    }

    /**
     * Select zero-based ordinal from the entire signed-sorted union.
     * O(log r) after one O(r) lazy preparation; no boxing of each point.
     * @throws IndexOutOfBoundsException when ordinal is outside [0, cardinality)
     */
    public long select(BigInteger ordinal) {
        Objects.requireNonNull(ordinal, "ordinal");
        if (ordinal.signum() < 0 || ordinal.compareTo(cardinality()) >= 0) {
            throw new IndexOutOfBoundsException("range ordinal outside cardinality");
        }
        long unsignedOrdinal = ordinal.longValue(); // exact low 64-bit bit pattern
        int lo = 0, hi = size;
        while (lo < hi) {
            int middle = lo + ((hi - lo) >>> 1);
            if (Long.compareUnsigned(cumulative[middle], unsignedOrdinal) <= 0) {
                lo = middle + 1;
            } else {
                hi = middle;
            }
        }
        long preceding = lo == 0 ? 0L : cumulative[lo - 1];
        return starts[lo] + (unsignedOrdinal - preceding); // exact signed coordinate, modular arithmetic
    }

    /**
     * Append already-sorted interval to a result under construction.
     * Never allocates an entry object; merges touching bounds in O(1).
     */
    private void appendSorted(long start, long end) {
        if (start >= end) return;
        if (size != 0 && ends[size - 1] >= start) {
            if (end > ends[size - 1]) ends[size - 1] = end;
            return;
        }
        reserve(size + 1);
        starts[size] = start;
        ends[size] = end;
        size++;
    }

    /** Linear-time immutable union of two sorted array/index-backed owners. */
    public M3LongRangeSet union(M3LongRangeSet other) {
        Objects.requireNonNull(other, "other");
        if (other == this) return snapshot();
        M3LongRangeSet result = new M3LongRangeSet();
        int a = 0, b = 0;
        while (a < size || b < other.size) {
            if (b == other.size || (a < size && starts[a] <= other.starts[b])) {
                result.appendSorted(starts[a], ends[a]);
                a++;
            } else {
                result.appendSorted(other.starts[b], other.ends[b]);
                b++;
            }
        }
        return result;
    }

    /** Linear-time intersection without a per-range object or third array payload. */
    public M3LongRangeSet intersection(M3LongRangeSet other) {
        Objects.requireNonNull(other, "other");
        if (other == this) return snapshot();
        M3LongRangeSet result = new M3LongRangeSet();
        int a = 0, b = 0;
        while (a < size && b < other.size) {
            result.appendSorted(Math.max(starts[a], other.starts[b]),
                    Math.min(ends[a], other.ends[b]));
            if (ends[a] <= other.ends[b]) a++;
            else b++;
        }
        return result;
    }

    /**
     * Linear-time difference {@code this \ other}; source owners are never mutated.
     */
    public M3LongRangeSet difference(M3LongRangeSet other) {
        Objects.requireNonNull(other, "other");
        if (other == this) return new M3LongRangeSet();
        M3LongRangeSet result = new M3LongRangeSet();
        int b = 0;
        for (int a = 0; a < size; a++) {
            long cursor = starts[a], end = ends[a];
            while (b < other.size && other.ends[b] <= cursor) b++;
            int probe = b;
            while (probe < other.size && other.starts[probe] < end) {
                if (other.starts[probe] > cursor) {
                    result.appendSorted(cursor, other.starts[probe]);
                }
                if (other.ends[probe] >= end) {
                    cursor = end;
                    break;
                }
                cursor = Math.max(cursor, other.ends[probe]);
                probe++;
            }
            if (cursor < end) result.appendSorted(cursor, end);
            b = probe;
        }
        return result;
    }

    /** Difference of unions in linear time; unlike generic range APIs, bounds are half-open. */
    public M3LongRangeSet symmetricDifference(M3LongRangeSet other) {
        Objects.requireNonNull(other, "other");
        return difference(other).union(other.difference(this));
    }

    /** Finite signed-long complement clipped to the caller's half-open domain. */
    public M3LongRangeSet complement(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        M3LongRangeSet scope = new M3LongRangeSet();
        scope.add(fromInclusive, toExclusive);
        return scope.difference(this);
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
        if (cumulative != null) copy.cumulative = Arrays.copyOf(cumulative, size);
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
        cumulative = null;
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
