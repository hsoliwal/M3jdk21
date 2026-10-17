/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Mutable signed-long half-open range map, stored as three parallel dynamic arrays.
 * Intervals are sorted, disjoint, and nonempty; adjacent intervals may remain
 * distinct even when values compare equal. No per-range Entry/Node is retained.
 *
 * <p>{@code put(a,b,v)} replaces mappings for exactly the coordinates in
 * {@code [a,b)}, splitting old spans as needed. Null values are supported;
 * use {@link #containsPoint(long)} to distinguish null from absent. The exclusive
 * upper bound cannot express a mapped {@code Long.MAX_VALUE} coordinate.
 * This is not Guava RangeMap nor a java.util.Map and makes no provider ABI claim.
 *
 * <p>Membership is O(log ranges), mutation O(ranges) with primitive array
 * shifts, range visit O(ranges), and state selection O(ranges). Copies occur
 * only at explicit detached-snapshot and growth boundaries. Not thread-safe;
 * callbacks must not mutate the owner. Reentrant structural modification is
 * detected after callback return, not rolled back. Optional JNI receives only
 * ordered integer rows and 32-bit precomputed flags, never references or Java
 * predicate/comparator callbacks.
 */
public final class M3LongRangeMap<V> {
    private long[] starts;
    private long[] ends;
    private Object[] values;
    private int size;
    private int revision;

    public M3LongRangeMap() { this(0); }

    public M3LongRangeMap(int expectedRanges) {
        if (expectedRanges < 0) throw new IllegalArgumentException("negative expectedRanges");
        starts = new long[expectedRanges];
        ends = new long[expectedRanges];
        values = new Object[expectedRanges];
    }

    public int rangeCount() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return starts.length; }

    private static void valid(long from, long to) {
        if (from > to) throw new IllegalArgumentException("fromInclusive > toExclusive");
    }

    private int firstEndingAfter(long coordinate) {
        int low = 0, high = size;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (ends[middle] <= coordinate) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int containing(long point) {
        int i = firstEndingAfter(point);
        return i < size && starts[i] <= point ? i : -1;
    }

    public boolean containsPoint(long point) { return containing(point) >= 0; }

    @SuppressWarnings("unchecked")
    public V get(long point) {
        int index = containing(point);
        return index < 0 ? null : (V) values[index];
    }

    public boolean intersects(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return false;
        int i = firstEndingAfter(fromInclusive);
        return i < size && starts[i] < toExclusive;
    }

    /** Replace one half-open interval without touching nonoverlapping neighbors. */
    public void put(long fromInclusive, long toExclusive, V value) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return;
        int first = firstEndingAfter(fromInclusive);
        int last = first;
        while (last < size && starts[last] < toExclusive) last++;
        if (last == first + 1 && starts[first] == fromInclusive
                && ends[first] == toExclusive && values[first] == value) return;
        boolean keepLeft = first < last && starts[first] < fromInclusive;
        boolean keepRight = first < last && ends[last - 1] > toExclusive;
        long leftStart = keepLeft ? starts[first] : 0;
        Object leftValue = keepLeft ? values[first] : null;
        long rightEnd = keepRight ? ends[last - 1] : 0;
        Object rightValue = keepRight ? values[last - 1] : null;
        int newSlots = 1 + (keepLeft ? 1 : 0) + (keepRight ? 1 : 0);
        int resulting = resulting(last - first, newSlots);
        reserve(resulting);
        shiftTail(last, first + newSlots);
        int i = first;
        if (keepLeft) write(i++, leftStart, fromInclusive, leftValue);
        write(i++, fromInclusive, toExclusive, value);
        if (keepRight) write(i, toExclusive, rightEnd, rightValue);
        truncate(resulting);
    }

    /** Remove exactly the half-open interval, preserving split fragments. */
    public void remove(long fromInclusive, long toExclusive) {
        valid(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive || size == 0) return;
        int first = firstEndingAfter(fromInclusive);
        int last = first;
        while (last < size && starts[last] < toExclusive) last++;
        if (last == first) return;
        boolean keepLeft = starts[first] < fromInclusive;
        boolean keepRight = ends[last - 1] > toExclusive;
        long leftStart = keepLeft ? starts[first] : 0;
        Object leftValue = keepLeft ? values[first] : null;
        long rightEnd = keepRight ? ends[last - 1] : 0;
        Object rightValue = keepRight ? values[last - 1] : null;
        int kept = (keepLeft ? 1 : 0) + (keepRight ? 1 : 0);
        int resulting = resulting(last - first, kept);
        reserve(resulting);
        shiftTail(last, first + kept);
        int i = first;
        if (keepLeft) write(i++, leftStart, fromInclusive, leftValue);
        if (keepRight) write(i, toExclusive, rightEnd, rightValue);
        truncate(resulting);
    }

    private int resulting(int removed, int inserted) {
        long next = (long) size - removed + inserted;
        if (next > Integer.MAX_VALUE - 8L) throw new OutOfMemoryError("too many ranges");
        return (int) next;
    }

    private void reserve(int required) {
        if (required <= starts.length) return;
        int old = starts.length;
        long capacity = Math.max((long) required, Math.max(8L, old + (long) (old >>> 1) + 1));
        if (capacity > Integer.MAX_VALUE - 8L) capacity = Integer.MAX_VALUE - 8L;
        // Stage all three allocations before publishing any replacement lane.
        long[] newStarts = Arrays.copyOf(starts, (int) capacity);
        long[] newEnds = Arrays.copyOf(ends, (int) capacity);
        Object[] newValues = Arrays.copyOf(values, (int) capacity);
        starts = newStarts;
        ends = newEnds;
        values = newValues;
    }

    private void shiftTail(int oldFrom, int newFrom) {
        int length = size - oldFrom;
        System.arraycopy(starts, oldFrom, starts, newFrom, length);
        System.arraycopy(ends, oldFrom, ends, newFrom, length);
        System.arraycopy(values, oldFrom, values, newFrom, length);
    }

    private void write(int index, long from, long to, Object value) {
        starts[index] = from;
        ends[index] = to;
        values[index] = value;
    }

    private void truncate(int resulting) {
        if (resulting < size) {
            Arrays.fill(starts, resulting, size, 0L);
            Arrays.fill(ends, resulting, size, 0L);
            Arrays.fill(values, resulting, size, null); // no accidentally retained old values
        }
        size = resulting;
        revision++;
    }

    public long[] startsToArray() { return Arrays.copyOf(starts, size); }
    public long[] endsToArray() { return Arrays.copyOf(ends, size); }
    public Object[] valuesToArray() { return Arrays.copyOf(values, size); }

    @FunctionalInterface
    public interface LongRangeConsumer<T> {
        void accept(long startInclusive, long endExclusive, T value);
    }

    @SuppressWarnings("unchecked")
    public void forEachRange(LongRangeConsumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        int expected = revision;
        for (int i = 0; i < size; i++) {
            action.accept(starts[i], ends[i], (V) values[i]);
            if (expected != revision) throw new ConcurrentModificationException();
        }
    }

    /** Detached parallel arrays, no retained alias of mutable owner storage. */
    public M3LongRangeMap<V> snapshot() {
        M3LongRangeMap<V> copy = new M3LongRangeMap<>(size);
        System.arraycopy(starts, 0, copy.starts, 0, size);
        System.arraycopy(ends, 0, copy.ends, 0, size);
        System.arraycopy(values, 0, copy.values, 0, size);
        copy.size = size;
        return copy;
    }

    /**
     * Detached subset by interval-ordinal 32-bit flags. Backend.JAVA uses the
     * incumbent M3IndexPathMasks reference method, NATIVE_REQUIRED demands its
     * real C17 JNI binary; no implicit native fallback or new ABI is introduced.
     */
    public M3LongRangeMap<V> selectedByState(int[] alignedStates,
            int requiredMask, int excludedMask, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(alignedStates, "alignedStates");
        Objects.requireNonNull(backend, "backend");
        if (alignedStates.length != size) {
            throw new IllegalArgumentException("unaligned range-state coordinates");
        }
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) rows[i] = i;
        int[] selected = M3IndexPathMasks.select(rows, rows,
                new int[][] {alignedStates}, new int[] {requiredMask},
                new int[] {excludedMask}, 0, size, 1, backend);
        M3LongRangeMap<V> copy = new M3LongRangeMap<>(selected.length);
        for (int i = 0; i < selected.length; i++) {
            int index = selected[i];
            copy.write(i, starts[index], ends[index], values[index]);
        }
        copy.size = selected.length;
        return copy;
    }

    public void clear() {
        if (size == 0) return;
        Arrays.fill(starts, 0, size, 0L);
        Arrays.fill(ends, 0, size, 0L);
        Arrays.fill(values, 0, size, null);
        size = 0;
        revision++;
    }

    /** Call in test and diagnostic passes; never silently repair broken ordering. */
    public void verifyInvariants() {
        if (size < 0 || starts.length != ends.length || starts.length != values.length
                || size > starts.length) throw new IllegalStateException("misaligned range planes");
        for (int i = 0; i < size; i++) {
            if (starts[i] >= ends[i]) throw new IllegalStateException("empty range stored");
            if (i > 0 && ends[i - 1] > starts[i]) {
                throw new IllegalStateException("overlapping range map storage");
            }
        }
    }
}
