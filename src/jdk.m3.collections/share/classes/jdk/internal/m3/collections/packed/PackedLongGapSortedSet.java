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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Sorted primitive-long set in one array with a movable vacant gap.
 *
 * <p>Logical-rank binary search remains O(log n); local insert/remove movement is proportional to
 * the distance from the current gap. There is no dummy value lane and no per-element position
 * index, node or wrapper.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongGapSortedSet.java}
 * blob {@code cb659a0033d17bad836971db441505d119c2a8b5} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongGapSortedSet implements LongSortedSet {
    private long[] values;
    private final double targetLoad;
    private int gapStart;
    private int gapEnd;
    private int size;
    private int callbacks;
    private int modifications;
    private long movedValues;

    public PackedLongGapSortedSet() { this(0, 0.75); }
    public PackedLongGapSortedSet(int expectedSize) { this(expectedSize, 0.75); }

    public PackedLongGapSortedSet(int expectedSize, double targetLoad) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (!(targetLoad >= 0.5 && targetLoad < 1.0)) {
            throw new IllegalArgumentException("targetLoad must be in [0.5, 1)");
        }
        this.targetLoad = targetLoad;
        int capacity = expectedSize == 0 ? 0 : capacityFor(expectedSize);
        values = new long[capacity];
        gapEnd = capacity;
    }

    @Override public boolean add(long value) {
        writable();
        int rank = search(value);
        if (rank >= 0) return false;
        rank = -rank - 1;
        ensureGap(rank);
        moveGap(rank);
        values[gapStart++] = value;
        size++;
        modifications++;
        return true;
    }

    @Override public boolean contains(long value) { return search(value) >= 0; }

    @Override public boolean remove(long value) {
        writable();
        int rank = search(value);
        if (rank < 0) return false;
        moveGap(rank);
        gapEnd++;
        size--;
        modifications++;
        return true;
    }

    @Override public long first() {
        if (size == 0) throw new NoSuchElementException();
        return valueAt(0);
    }

    @Override public long last() {
        if (size == 0) throw new NoSuchElementException();
        return valueAt(size - 1);
    }

    @Override public long floor(long value, long defaultValue) {
        int rank = upperBound(value) - 1;
        return rank < 0 ? defaultValue : valueAt(rank);
    }

    @Override public long ceiling(long value, long defaultValue) {
        int rank = lowerBound(value);
        return rank == size ? defaultValue : valueAt(rank);
    }

    @Override public long lower(long value, long defaultValue) {
        int rank = lowerBound(value) - 1;
        return rank < 0 ? defaultValue : valueAt(rank);
    }

    @Override public long higher(long value, long defaultValue) {
        int rank = upperBound(value);
        return rank == size ? defaultValue : valueAt(rank);
    }

    @Override public int filterInPlace(LongPredicate predicate) {
        writable();
        Objects.requireNonNull(predicate, "predicate");
        long[] kept = toArray();
        int write = 0;
        callbacks++;
        try {
            for (long value : kept) {
                if (predicate.test(value)) kept[write++] = value;
            }
        } finally {
            callbacks--;
        }
        int removed = size - write;
        if (removed != 0) {
            rebuild(kept, write);
            modifications++;
        }
        return removed;
    }

    @Override public void clear() {
        writable();
        if (size != 0) {
            size = 0;
            gapStart = 0;
            gapEnd = values.length;
            modifications++;
        }
    }

    @Override public LongIterator iterator() {
        int expected = modifications;
        return new LongIterator() {
            private int rank;

            @Override public boolean hasNext() {
                check();
                return rank < size;
            }

            @Override public long nextLong() {
                check();
                if (rank >= size) throw new NoSuchElementException();
                return valueAt(rank++);
            }

            private void check() {
                if (expected != modifications) throw new ConcurrentModificationException();
            }
        };
    }

    @Override public void forEachLong(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        callbacks++;
        try {
            for (int rank = 0; rank < size; rank++) consumer.accept(valueAt(rank));
        } finally {
            callbacks--;
        }
    }

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override public long[] toArray() {
        long[] result = new long[size];
        if (gapStart != 0) System.arraycopy(values, 0, result, 0, gapStart);
        int right = values.length - gapEnd;
        if (right != 0) System.arraycopy(values, gapEnd, result, gapStart, right);
        return result;
    }

    public int capacity() { return values.length; }
    public int gapSize() { return gapEnd - gapStart; }
    public int gapRank() { return gapStart; }
    public double targetLoad() { return targetLoad; }
    public long movedValues() { return movedValues; }
    public long backingArrayPayloadBytes() { return 8L * values.length; }

    public void trimToSize() {
        writable();
        values = toArray();
        gapStart = size;
        gapEnd = size;
        modifications++;
    }

    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }

    private int search(long value) {
        int low = 0, high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long candidate = valueAt(middle);
            if (candidate < value) low = middle + 1;
            else if (candidate > value) high = middle - 1;
            else return middle;
        }
        return -low - 1;
    }

    private int lowerBound(long value) {
        int low = 0, high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (valueAt(middle) < value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int upperBound(long value) {
        int low = 0, high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (valueAt(middle) <= value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private long valueAt(int logicalRank) { return values[physical(logicalRank)]; }

    private int physical(int logicalRank) {
        return logicalRank < gapStart ? logicalRank : logicalRank + gapSize();
    }

    private void ensureGap(int rank) {
        if (gapStart != gapEnd) return;
        int required = PackedArrayCapacity.required(size, 1);
        int byLoad = capacityFor(required);
        int capacity = PackedSupport.growArrayCapacity(values.length, byLoad);
        long[] replacement = new long[capacity];
        int newGapEnd = capacity - (size - rank);
        if (rank != 0) System.arraycopy(values, 0, replacement, 0, rank);
        int suffix = size - rank;
        if (suffix != 0) System.arraycopy(values, rank, replacement, newGapEnd, suffix);
        movedValues += size;
        values = replacement;
        gapStart = rank;
        gapEnd = newGapEnd;
    }

    private void moveGap(int targetRank) {
        if (targetRank == gapStart) return;
        if (targetRank < 0 || targetRank > size) {
            throw new IndexOutOfBoundsException("targetRank=" + targetRank + ", size=" + size);
        }
        if (targetRank < gapStart) {
            int moved = gapStart - targetRank;
            int destination = gapEnd - moved;
            System.arraycopy(values, targetRank, values, destination, moved);
            gapStart = targetRank;
            gapEnd = destination;
            movedValues += moved;
        } else {
            int moved = targetRank - gapStart;
            System.arraycopy(values, gapEnd, values, gapStart, moved);
            gapStart += moved;
            gapEnd += moved;
            movedValues += moved;
        }
    }

    private int capacityFor(int required) {
        if (required < 0 || required > PackedSupport.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("gap-sorted set capacity exceeded");
        }
        long capacity = (long) Math.ceil(required / targetLoad);
        if (capacity <= required && required < PackedSupport.MAX_ARRAY_SIZE) capacity = (long) required + 1;
        if (capacity > PackedSupport.MAX_ARRAY_SIZE) {
            if (required == PackedSupport.MAX_ARRAY_SIZE) return required;
            throw new OutOfMemoryError("gap-sorted set capacity exceeded");
        }
        return (int) capacity;
    }

    private void rebuild(long[] sortedValues, int length) {
        int capacity = length == 0 ? values.length : Math.max(length, capacityFor(length));
        long[] replacement = new long[capacity];
        if (length != 0) System.arraycopy(sortedValues, 0, replacement, 0, length);
        movedValues += length;
        values = replacement;
        size = length;
        gapStart = length;
        gapEnd = capacity;
    }

    void checkInvariants() {
        if (gapStart < 0 || gapStart > gapEnd || gapEnd > values.length
                || size != values.length - gapSize() || gapStart > size) {
            throw new IllegalStateException("invalid gap-sorted set lane");
        }
        for (int rank = 1; rank < size; rank++) {
            if (valueAt(rank - 1) >= valueAt(rank)) throw new IllegalStateException("value order");
        }
    }
}
