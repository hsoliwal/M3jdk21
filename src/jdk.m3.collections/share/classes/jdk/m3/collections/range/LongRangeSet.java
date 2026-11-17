/* *****************************************************************************
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
package jdk.m3.collections.range;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import jdk.internal.m3.collections.LaneSearch;
import jdk.m3.collections.IndexPathMasks;
import jdk.m3.collections.LongLongConsumer;

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
public final class LongRangeSet {
    private long[] starts;
    private long[] ends;
    private int size;
    private int revision;

    public LongRangeSet() {
        this(0);
    }

    public LongRangeSet(int expectedRanges) {
        if (expectedRanges < 0) throw new IllegalArgumentException("negative expectedRanges");
        starts = new long[expectedRanges];
        ends = new long[expectedRanges];
    }

    public int rangeCount() { return size; }

    public boolean isEmpty() { return size == 0; }

    public int capacity() { return starts.length; }

    /** Returns the payload size of both lanes in bytes (Synexia primitives LongRangeSet). */
    public long payloadBytes() {
        return (long) starts.length * Long.BYTES + (long) ends.length * Long.BYTES;
    }

    private static void valid(long from, long to) {
        if (from > to) throw new IllegalArgumentException("fromInclusive > toExclusive");
    }

    private void reserve(int required) {
        if (required <= starts.length) return;
        int capacity = grownCapacity(starts.length, required);
        long[] grownStarts = Arrays.copyOf(starts, capacity);
        long[] grownEnds = Arrays.copyOf(ends, capacity);
        starts = grownStarts;
        ends = grownEnds;
    }

    /** The capacity reserve() grows {@code old} to for {@code required} ranges. */
    private static int grownCapacity(int old, int required) {
        long proposed = Math.max((long) required, Math.max(8L, old + (long) (old >>> 1) + 1));
        if (proposed > Integer.MAX_VALUE - 8L) {
            if (required > Integer.MAX_VALUE - 8) throw new OutOfMemoryError("too many intervals");
            proposed = Integer.MAX_VALUE - 8L;
        }
        return (int) proposed;
    }

    /** Inserts a half-open range, merging overlaps and touching neighbors. */
    public void add(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return;
        // Ends are sorted; lowerBound (not upperBound) retains touching-range merges.
        int first = size == 0 || ends[0] >= fromInclusive
                ? 0 : LaneSearch.lowerBound(ends, 1, size, fromInclusive);
        // The intervals are sorted, disjoint and nonadjacent. Absorbing
        // a range beginning at/before toExclusive cannot bridge a later
        // gap: the next start is strictly greater than the absorbed end.
        // Therefore the last touching range is an upper-bound lookup.
        int last = LaneSearch.upperBound(starts, first, size, toExclusive);
        long start = fromInclusive;
        long end = toExclusive;
        if (last > first) {
            start = Math.min(start, starts[first]);
            end = Math.max(end, ends[last - 1]);
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
        // Prefix starts are sorted. This locates the first interval starting at/after
        // the exclusive removal bound without scanning every covered range.
        int last = LaneSearch.lowerBound(starts, first, size, toExclusive);
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
        return LaneSearch.upperBound(ends, lo, hi, point);
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
        // Intervals are disjoint subsets of the signed long coordinate space.
        // Hence the total fits in an unsigned 64-bit word (at most 2^64 - 1).
        // Unsigned modular addition is exact here even when a single interval
        // crosses the signed boundary; construct BigInteger only at the exit.
        long unsignedTotal = 0L;
        for (int i = 0; i < size; i++) {
            unsignedTotal += ends[i] - starts[i];
        }
        return unsignedTotal >= 0 ? BigInteger.valueOf(unsignedTotal)
                : BigInteger.valueOf(unsignedTotal & Long.MAX_VALUE).setBit(Long.SIZE - 1);
    }

    /** Defensive snapshot of the packed start coordinate lane. */
    public long[] startsToArray() { return Arrays.copyOf(starts, size); }

    /** Defensive snapshot of the packed exclusive end coordinate lane. */
    public long[] endsToArray() { return Arrays.copyOf(ends, size); }

    public void forEachRange(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedRevision = revision;
        for (int i = 0; i < size; i++) {
            consumer.accept(starts[i], ends[i]);
            if (expectedRevision != revision) throw new ConcurrentModificationException();
        }
    }

    /** Detached snapshot; no alias to caller-owned mutable lanes survives. */
    public LongRangeSet snapshot() {
        LongRangeSet copy = new LongRangeSet(size);
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
    public LongRangeSet selectedByState(int[] states, int requiredMask,
            int forbiddenMask, IndexPathMasks.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        if (states.length != size) throw new IllegalArgumentException("unaligned range states");
        if (backend == IndexPathMasks.Backend.JAVA) {
            // Direct parallel-lane compaction: the encounter path is the identity
            // sequence, so creating row IDs and selector arrays is unnecessary.
            // The JNI_REQUIRED path below deliberately preserves the existing
            // validated native selector and its unavailable-library refusal.
            int kept = 0;
            for (int state : states) {
                if ((state & requiredMask) == requiredMask
                        && (state & forbiddenMask) == 0) kept++;
            }
            LongRangeSet result = new LongRangeSet(kept);
            int output = 0;
            for (int i = 0; i < size; i++) {
                int state = states[i];
                if ((state & requiredMask) == requiredMask
                        && (state & forbiddenMask) == 0) {
                    if (output == kept) {
                        throw new ConcurrentModificationException("mask stages changed during selection");
                    }
                    result.starts[output] = starts[i];
                    result.ends[output++] = ends[i];
                }
            }
            if (output != kept) {
                throw new ConcurrentModificationException("mask stages changed during selection");
            }
            result.size = output;
            return result;
        }
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) rows[i] = i;
        int[] selected = IndexPathMasks.select(rows, rows, new int[][] {states},
                new int[] {requiredMask}, new int[] {forbiddenMask},
                0, size, 1, backend);
        LongRangeSet result = new LongRangeSet(selected.length);
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

    // API_UNION U6 (2026-11-09): members of Synexia primitives LongRangeSet, on the M3 lanes and conventions.

    /** Fewer source ranges than this are added one by one (a binary search each). */
    private static final int MERGE_THRESHOLD = 4;

    /**
     * Adds every range of {@code source}; {@code source} may be this set. From
     * {@value #MERGE_THRESHOLD} source ranges on, both sorted lanes are merged in one pass,
     * O(n + m) instead of one insertion per source range, with the ranges, capacity and payload
     * that adding them one by one gives; a source that adds nothing changes and allocates nothing.
     */
    //FLLDCIM: LEETCODIFY — two-pointer union of sorted disjoint lanes with capacity replay
    public void addAll(LongRangeSet source) {
        Objects.requireNonNull(source, "source");
        int count = source.size;
        if (source == this || count == 0) {
            return; // every range of this set is already in it
        }
        if (count < MERGE_THRESHOLD) {
            for (int i = 0; i < count; i++) {
                add(source.starts[i], source.ends[i]);
            }
            return;
        }
        mergeAll(source.starts, source.ends, count);
    }

    /**
     * One counting pass, then the union. Adding source range i one by one would absorb exactly
     * the target ranges it touches (a component formed by earlier source ranges counts once and
     * holds the one target range they share), so the sizes, the capacity growth and whether
     * anything changes follow from the touched target ranges alone.
     */
    private void mergeAll(long[] sourceStarts, long[] sourceEnds, int count) {
        int capacity = starts.length;
        int resulting = size;
        boolean changed = false;
        int first = 0;
        int last = 0;
        for (int i = 0; i < count; i++) {
            long from = sourceStarts[i];
            long to = sourceEnds[i];
            while (first < size && ends[first] < from) first++; // first range ending at/after from
            // ranges below first end before from <= to, so this also passes them
            while (last < size && starts[last] <= to) last++; // first range starting after to
            int touched = last - first;
            changed |= touched != 1 || starts[first] > from || ends[first] < to;
            if (touched == 0 && resulting == capacity) {
                capacity = grownCapacity(capacity, resulting + 1);
            }
            resulting += 1 - touched;
        }
        if (!changed) return;
        long[] mergedStarts = new long[capacity];
        long[] mergedEnds = new long[capacity];
        size = union(sourceStarts, sourceEnds, count, mergedStarts, mergedEnds);
        starts = mergedStarts;
        ends = mergedEnds;
        revision++;
    }

    /** Writes the union of both sets' ranges, touching ones coalesced; returns its size. */
    private int union(long[] sourceStarts, long[] sourceEnds, int count,
            long[] outStarts, long[] outEnds) {
        int t = 0;
        int s = 0;
        int w = 0;
        while (t < size || s < count) {
            boolean target = s == count || t < size && starts[t] <= sourceStarts[s];
            long from = target ? starts[t] : sourceStarts[s];
            long to = target ? ends[t++] : sourceEnds[s++];
            if (w > 0 && from <= outEnds[w - 1]) {
                outEnds[w - 1] = Math.max(outEnds[w - 1], to);
            } else {
                outStarts[w] = from;
                outEnds[w++] = to;
            }
        }
        return w;
    }

    /** Returns whether one stored range contains all of [fromInclusive, toExclusive); an empty query is covered. */
    public boolean covers(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return true;
        int index = firstEndingAfter(fromInclusive);
        return index < size && starts[index] <= fromInclusive && ends[index] >= toExclusive;
    }

    /**
     * Copies the range containing {@code point} into {@code output[offset]} (start) and {@code output[offset + 1]}
     * (exclusive end). Returns false on a miss and leaves the buffer unchanged.
     */
    public boolean copyRangeContaining(long point, long[] output, int offset) {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, 2, output.length);
        int index = firstEndingAfter(point);
        if (index >= size || starts[index] > point) return false;
        output[offset] = starts[index];
        output[offset + 1] = ends[index];
        return true;
    }

    /** Returns the start of the minimal enclosing span; throws NoSuchElementException when empty. */
    public long spanStart() {
        if (size == 0) throw new NoSuchElementException();
        return starts[0];
    }

    /** Returns the exclusive end of the minimal enclosing span; throws NoSuchElementException when empty. */
    public long spanEnd() {
        if (size == 0) throw new NoSuchElementException();
        return ends[size - 1];
    }

    /** Returns the start of range {@code index}. */
    public long startAt(int index) {
        return starts[Objects.checkIndex(index, size)];
    }

    /** Returns the exclusive end of range {@code index}. */
    public long endAt(int index) {
        return ends[Objects.checkIndex(index, size)];
    }

    /** Synexia name of {@link #startsToArray()}. */
    public long[] startsArray() { return startsToArray(); }

    /** Synexia name of {@link #endsToArray()}. */
    public long[] endsArray() { return endsToArray(); }

    /** Synexia name of {@link #forEachRange(LongLongConsumer)}. */
    public void forEach(LongLongConsumer consumer) { forEachRange(consumer); }

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
