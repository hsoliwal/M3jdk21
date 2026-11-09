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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;
import jdk.m3.collections.LongLongSortedMap;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted long-to-long map backed by two primitive lanes and one movable vacant gap.
 *
 * <p>Binary search is over logical ranks; rank-to-physical translation is O(1). An insertion or
 * removal moves only the mappings between the current gap and the edit rank. Repeated nearby edits
 * therefore avoid whole-suffix shifts. Distant edits and growth remain O(n). No Entry, node,
 * bucket, page or per-mapping position object/lane is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongGapSortedMap.java}
 * blob {@code 91589a442f58c614994261bdac36c76f77138bd9} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongGapSortedMap implements LongLongSortedMap {
    private long[] keys;
    private long[] values;
    private final double targetLoad;
    private int gapStart;
    private int gapEnd;
    private int size;
    private int callbacks;
    private long movedMappings;

    public PackedLongLongGapSortedMap() { this(0, 0.75); }
    public PackedLongLongGapSortedMap(int expectedSize) { this(expectedSize, 0.75); }

    public PackedLongLongGapSortedMap(int expectedSize, double targetLoad) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (!(targetLoad >= 0.5 && targetLoad < 1.0)) {
            throw new IllegalArgumentException("targetLoad must be in [0.5, 1)");
        }
        this.targetLoad = targetLoad;
        int capacity = expectedSize == 0 ? 0 : capacityFor(expectedSize);
        keys = new long[capacity];
        values = new long[capacity];
        gapEnd = capacity;
    }

    @Override public boolean put(long key, long value) {
        writable();
        int rank = search(key);
        if (rank >= 0) {
            values[physical(rank)] = value;
            return false;
        }
        rank = -rank - 1;
        ensureGap(rank);
        moveGap(rank);
        keys[gapStart] = key;
        values[gapStart] = value;
        gapStart++;
        size++;
        return true;
    }

    /** Admission completes before any retained lane changes. */
    public boolean putIf(long key, long value, LongLongPredicate admission) {
        writable();
        Objects.requireNonNull(admission, "admission");
        boolean accepted;
        callbacks++;
        try { accepted = admission.test(key, value); }
        finally { callbacks--; }
        return accepted && put(key, value);
    }

    @Override public boolean containsKey(long key) { return search(key) >= 0; }

    @Override public long getOrDefault(long key, long defaultValue) {
        int rank = search(key);
        return rank < 0 ? defaultValue : values[physical(rank)];
    }

    @Override public long getOrThrow(long key) {
        int rank = search(key);
        if (rank < 0) throw new IllegalArgumentException("missing key: " + key);
        return values[physical(rank)];
    }

    @Override public boolean remove(long key) {
        writable();
        int rank = search(key);
        if (rank < 0) return false;
        moveGap(rank);
        gapEnd++;
        size--;
        return true;
    }

    @Override public long firstKey() {
        if (size == 0) throw new NoSuchElementException();
        return keyAt(0);
    }

    @Override public long lastKey() {
        if (size == 0) throw new NoSuchElementException();
        return keyAt(size - 1);
    }

    @Override public long floorKey(long key, long defaultValue) {
        int rank = upperBound(key) - 1;
        return rank < 0 ? defaultValue : keyAt(rank);
    }

    @Override public long ceilingKey(long key, long defaultValue) {
        int rank = lowerBound(key);
        return rank == size ? defaultValue : keyAt(rank);
    }

    @Override public long lowerKey(long key, long defaultValue) {
        int rank = lowerBound(key) - 1;
        return rank < 0 ? defaultValue : keyAt(rank);
    }

    @Override public long higherKey(long key, long defaultValue) {
        int rank = upperBound(key);
        return rank == size ? defaultValue : keyAt(rank);
    }

    /**
     * Evaluates every decision against private primitive snapshots before publishing a rebuild.
     * A throwing predicate leaves the map unchanged; callback mutation is rejected.
     */
    @Override public int filterInPlace(LongLongPredicate predicate) {
        writable();
        Objects.requireNonNull(predicate, "predicate");
        long[] keptKeys = keysToArray();
        long[] keptValues = valuesToArray();
        int write = 0;
        callbacks++;
        try {
            for (int read = 0; read < keptKeys.length; read++) {
                if (predicate.test(keptKeys[read], keptValues[read])) {
                    keptKeys[write] = keptKeys[read];
                    keptValues[write] = keptValues[read];
                    write++;
                }
            }
        } finally {
            callbacks--;
        }
        int removed = size - write;
        if (removed != 0) rebuild(keptKeys, keptValues, write);
        return removed;
    }

    @Override public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        callbacks++;
        try {
            for (int rank = 0; rank < size; rank++) {
                int index = physical(rank);
                consumer.accept(keys[index], values[index]);
            }
        } finally {
            callbacks--;
        }
    }

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override public void clear() {
        writable();
        size = 0;
        gapStart = 0;
        gapEnd = keys.length;
    }

    public int capacity() { return keys.length; }
    public int gapSize() { return gapEnd - gapStart; }
    public int gapRank() { return gapStart; }
    public double targetLoad() { return targetLoad; }
    public long movedMappings() { return movedMappings; }
    public long backingArrayPayloadBytes() { return 16L * keys.length; }

    public void trimToSize() {
        writable();
        long[] compactKeys = keysToArray();
        long[] compactValues = valuesToArray();
        keys = compactKeys;
        values = compactValues;
        gapStart = size;
        gapEnd = size;
    }

    @Override public long[] keysToArray() {
        long[] result = new long[size];
        if (gapStart != 0) System.arraycopy(keys, 0, result, 0, gapStart);
        int right = keys.length - gapEnd;
        if (right != 0) System.arraycopy(keys, gapEnd, result, gapStart, right);
        return result;
    }

    @Override public long[] valuesToArray() {
        long[] result = new long[size];
        if (gapStart != 0) System.arraycopy(values, 0, result, 0, gapStart);
        int right = values.length - gapEnd;
        if (right != 0) System.arraycopy(values, gapEnd, result, gapStart, right);
        return result;
    }

    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }

    private int search(long key) {
        int low = 0, high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long candidate = keyAt(middle);
            if (candidate < key) low = middle + 1;
            else if (candidate > key) high = middle - 1;
            else return middle;
        }
        return -low - 1;
    }

    private int lowerBound(long key) {
        int low = 0, high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (keyAt(middle) < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int upperBound(long key) {
        int low = 0, high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (keyAt(middle) <= key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private long keyAt(int logicalRank) { return keys[physical(logicalRank)]; }

    private int physical(int logicalRank) {
        return logicalRank < gapStart ? logicalRank : logicalRank + gapSize();
    }

    private void ensureGap(int rank) {
        if (gapStart != gapEnd) return;
        int required = PackedArrayCapacity.required(size, 1);
        int byLoad = capacityFor(required);
        int capacity = PackedSupport.growArrayCapacity(keys.length, byLoad);
        long[] newKeys = new long[capacity];
        long[] newValues = new long[capacity];
        int newGapEnd = capacity - (size - rank);
        if (rank != 0) {
            System.arraycopy(keys, 0, newKeys, 0, rank);
            System.arraycopy(values, 0, newValues, 0, rank);
        }
        int suffix = size - rank;
        if (suffix != 0) {
            System.arraycopy(keys, rank, newKeys, newGapEnd, suffix);
            System.arraycopy(values, rank, newValues, newGapEnd, suffix);
        }
        movedMappings += size;
        keys = newKeys;
        values = newValues;
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
            System.arraycopy(keys, targetRank, keys, destination, moved);
            System.arraycopy(values, targetRank, values, destination, moved);
            gapStart = targetRank;
            gapEnd = destination;
            movedMappings += moved;
        } else {
            int moved = targetRank - gapStart;
            System.arraycopy(keys, gapEnd, keys, gapStart, moved);
            System.arraycopy(values, gapEnd, values, gapStart, moved);
            gapStart += moved;
            gapEnd += moved;
            movedMappings += moved;
        }
    }

    private int capacityFor(int required) {
        if (required < 0 || required > PackedSupport.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("gap-sorted map capacity exceeded");
        }
        long capacity = (long) Math.ceil(required / targetLoad);
        if (capacity <= required && required < PackedSupport.MAX_ARRAY_SIZE) capacity = (long) required + 1;
        if (capacity > PackedSupport.MAX_ARRAY_SIZE) {
            if (required == PackedSupport.MAX_ARRAY_SIZE) return required;
            throw new OutOfMemoryError("gap-sorted map capacity exceeded");
        }
        return (int) capacity;
    }

    private void rebuild(long[] sortedKeys, long[] sortedValues, int length) {
        int capacity = length == 0 ? keys.length : Math.max(length, capacityFor(length));
        long[] newKeys = new long[capacity];
        long[] newValues = new long[capacity];
        if (length != 0) {
            System.arraycopy(sortedKeys, 0, newKeys, 0, length);
            System.arraycopy(sortedValues, 0, newValues, 0, length);
        }
        movedMappings += length;
        keys = newKeys;
        values = newValues;
        size = length;
        gapStart = length;
        gapEnd = capacity;
    }

    void checkInvariants() {
        if (keys.length != values.length || gapStart < 0 || gapStart > gapEnd
                || gapEnd > keys.length || size != keys.length - gapSize() || gapStart > size) {
            throw new IllegalStateException("invalid gap-sorted map lanes");
        }
        for (int rank = 1; rank < size; rank++) {
            if (keyAt(rank - 1) >= keyAt(rank)) throw new IllegalStateException("key order");
        }
    }
}
