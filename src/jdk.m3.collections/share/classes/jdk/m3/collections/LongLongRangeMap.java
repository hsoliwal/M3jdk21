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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Non-overlapping half-open long range map backed by three parallel long[] lanes.
 *
 * <p>Putting a range overwrites overlaps, removal can split one existing range,
 * and adjacent equal-valued ranges are coalesced. No Range or Entry object is
 * retained per interval.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongLongRangeMap.java}
 * blob {@code 44c6fd1f06060e193347dbb3d6ced9d5219065a2} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class LongLongRangeMap {
    @FunctionalInterface
    public interface RangeValueConsumer {
        void accept(long startInclusive, long endExclusive, long value);
    }

    private long[] starts;
    private long[] ends;
    private long[] values;
    private int size;

    public LongLongRangeMap() {
        this(8);
    }

    public LongLongRangeMap(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        starts = new long[capacity];
        ends = new long[capacity];
        values = new long[capacity];
    }

    public int rangeCount() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return starts.length; }
    public long payloadBytes() {
        return (long) starts.length * Long.BYTES * 3L;
    }

    public long getOrDefault(long point, long defaultValue) {
        int index = floorStart(point);
        return index >= 0 && point < ends[index] ? values[index] : defaultValue;
    }

    public boolean containsPoint(long point) {
        int index = floorStart(point);
        return index >= 0 && point < ends[index];
    }

    /**
     * Copies the coalesced entry containing {@code point} into {@code output}
     * as [startInclusive, endExclusive, value]. Returns false on a miss and
     * leaves the caller buffer unchanged.
     */
    public boolean copyEntryContaining(long point, long[] output, int offset) {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, 3, output.length);
        int index = floorStart(point);
        if (index < 0 || point >= ends[index]) return false;
        output[offset] = starts[index];
        output[offset + 1] = ends[index];
        output[offset + 2] = values[index];
        return true;
    }

    /** Returns the lower endpoint of the minimal enclosing span. */
    public long spanStart() {
        if (size == 0) throw new NoSuchElementException();
        return starts[0];
    }

    /** Returns the upper endpoint of the minimal enclosing span. */
    public long spanEnd() {
        if (size == 0) throw new NoSuchElementException();
        return ends[size - 1];
    }

    public void put(long startInclusive, long endExclusive, long value) {
        put(startInclusive, endExclusive, value, Arrays::copyOf);
    }

    /** Internal fault-injection seam: the actual public API is unchanged. */
    void put(long startInclusive, long endExclusive, long value, BackingCopier copier) {
        checkRange(startInclusive, endExclusive);
        // An overwrite can create at most two additional rows. Avoid the
        // boundary binary searches entirely while there is sufficient slack.
        if (starts.length - size < 2) {
            ensure(requiredCapacityForPut(startInclusive, endExclusive), copier);
        }
        remove(startInclusive, endExclusive);

        int insertion = lowerBoundStart(startInclusive);
        int moved = size - insertion;
        if (moved > 0) {
            System.arraycopy(starts, insertion, starts, insertion + 1, moved);
            System.arraycopy(ends, insertion, ends, insertion + 1, moved);
            System.arraycopy(values, insertion, values, insertion + 1, moved);
        }
        starts[insertion] = startInclusive;
        ends[insertion] = endExclusive;
        values[insertion] = value;
        size++;
        coalesceAround(insertion);
    }

    public void remove(long startInclusive, long endExclusive) {
        checkRange(startInclusive, endExclusive);
        int index = firstEndingAfter(startInclusive);
        while (index < size && starts[index] < endExclusive) {
            long currentStart = starts[index];
            long currentEnd = ends[index];
            long currentValue = values[index];

            if (startInclusive <= currentStart && endExclusive >= currentEnd) {
                int next = index + 1;
                if (next == size || ends[next] > endExclusive) {
                    removeAt(index);
                } else {
                    removeRange(index, firstEndingAfter(endExclusive));
                }
                continue;
            }

            if (startInclusive <= currentStart) {
                starts[index] = endExclusive;
                if (starts[index] >= ends[index]) {
                    removeAt(index);
                }
                return;
            }

            if (endExclusive >= currentEnd) {
                ends[index] = startInclusive;
                index++;
                continue;
            }

            // Strictly inside: split into two ranges retaining the old value.
            ensure(size + 1);
            int tail = size - index - 1;
            if (tail > 0) {
                System.arraycopy(starts, index + 1, starts, index + 2, tail);
                System.arraycopy(ends, index + 1, ends, index + 2, tail);
                System.arraycopy(values, index + 1, values, index + 2, tail);
            }
            ends[index] = startInclusive;
            starts[index + 1] = endExclusive;
            ends[index + 1] = currentEnd;
            values[index + 1] = currentValue;
            size++;
            return;
        }
    }

    public long startAt(int index) {
        checkIndex(index);
        return starts[index];
    }

    public long endAt(int index) {
        checkIndex(index);
        return ends[index];
    }

    public long valueAt(int index) {
        checkIndex(index);
        return values[index];
    }

    public void forEach(RangeValueConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(starts[i], ends[i], values[i]);
        }
    }

    public void clear() {
        Arrays.fill(starts, 0, size, 0L);
        Arrays.fill(ends, 0, size, 0L);
        Arrays.fill(values, 0, size, 0L);
        size = 0;
    }

    private void coalesceAround(int index) {
        int current = index;
        if (current > 0
                && ends[current - 1] == starts[current]
                && values[current - 1] == values[current]) {
            ends[current - 1] = ends[current];
            removeAt(current);
            current--;
        }
        if (current + 1 < size
                && ends[current] == starts[current + 1]
                && values[current] == values[current + 1]) {
            ends[current] = ends[current + 1];
            removeAt(current + 1);
        }
    }

    /**
     * Compute the exact number of rows needed before coalescing, without
     * mutating any existing interval. Two binary searches identify all
     * overlapping rows; at most two outer fragments survive the overwrite.
     */
    private int requiredCapacityForPut(long from, long to) {
        int first = firstEndingAfter(from);
        int afterLast = lowerBoundStart(to);
        if (first >= afterLast) return Math.addExact(size, 1);
        int survivors = size - (afterLast - first);
        if (starts[first] < from) survivors++;
        if (ends[afterLast - 1] > to) survivors++;
        return Math.addExact(survivors, 1);
    }

    private int lowerBoundStart(long value) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (starts[mid] < value) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private int floorStart(long value) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (starts[mid] <= value) low = mid + 1;
            else high = mid;
        }
        return low - 1;
    }

    private int firstEndingAfter(long value) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (ends[mid] <= value) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private void removeAt(int index) {
        int tail = size - index - 1;
        if (tail > 0) {
            System.arraycopy(starts, index + 1, starts, index, tail);
            System.arraycopy(ends, index + 1, ends, index, tail);
            System.arraycopy(values, index + 1, values, index, tail);
        }
        int last = --size;
        starts[last] = 0L;
        ends[last] = 0L;
        values[last] = 0L;
    }

    // The caller admits a nonempty block; compaction preserves lane order and capacity.
    private void removeRange(int from, int to) {
        int tail = size - to;
        if (tail > 0) {
            System.arraycopy(starts, to, starts, from, tail);
            System.arraycopy(ends, to, ends, from, tail);
            System.arraycopy(values, to, values, from, tail);
        }
        int newSize = size - (to - from);
        Arrays.fill(starts, newSize, size, 0L);
        Arrays.fill(ends, newSize, size, 0L);
        Arrays.fill(values, newSize, size, 0L);
        size = newSize;
    }

    /** Explicit test seam for atomic growth of the three parent-owned long lanes. */
    @FunctionalInterface
    interface BackingCopier {
        long[] copy(long[] source, int capacity);
    }

    private void ensure(int required) {
        ensure(required, Arrays::copyOf);
    }

    /** Stage all three copies before publishing any backing reference. */
    void ensure(int required, BackingCopier copier) {
        if (required <= starts.length) return;
        Objects.requireNonNull(copier, "copier");
        int next = starts.length + (starts.length >>> 1) + 1;
        if (next < required) next = required;
        long[] nextStarts = Objects.requireNonNull(copier.copy(starts, next), "starts copy");
        long[] nextEnds = Objects.requireNonNull(copier.copy(ends, next), "ends copy");
        long[] nextValues = Objects.requireNonNull(copier.copy(values, next), "values copy");
        if (nextStarts.length != next || nextEnds.length != next
                || nextValues.length != next) {
            throw new IllegalStateException("incomplete M3LongLongRangeMap backing growth");
        }
        starts = nextStarts;
        ends = nextEnds;
        values = nextValues;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private static void checkRange(long startInclusive, long endExclusive) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("range must satisfy start < end");
        }
    }
}
