/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Sorted primitive set with one movable sparse gap.
 *
 * <p>Binary search operates on logical indices. Inserts move the gap to the insertion point;
 * clustered edits therefore shift only the distance from the previous edit rather than the
 * entire tail. When the gap is exhausted the array is rebuilt to the configured load factor.
 */
public final class PrimitiveSortedGapSet {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final PrimitiveKind kind;
    private final float loadFactor;
    private Object elements;
    private int gapStart;
    private int gapEnd;
    private int size;

    public PrimitiveSortedGapSet(PrimitiveKind kind) {
        this(kind, 8, DEFAULT_LOAD_FACTOR);
    }

    public PrimitiveSortedGapSet(PrimitiveKind kind, int expected, float loadFactor) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (expected < 0) throw new IllegalArgumentException("expected < 0");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        this.loadFactor = loadFactor;
        int capacity = capacityFor(Math.max(1, expected));
        elements = kind.newArray(capacity);
        gapStart = 0;
        gapEnd = capacity;
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(elements); }
    public int gapSize() { return gapEnd - gapStart; }
    public float loadFactor() { return loadFactor; }
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    public boolean containsBits(long bits) { return search(bits) >= 0; }

    public boolean addBits(long bits) {
        int result = search(bits);
        if (result >= 0) return false;
        int insertion = -result - 1;
        ensureGapAt(insertion);
        kind.writeBits(elements, gapStart++, bits);
        size++;
        return true;
    }

    public boolean removeBits(long bits) {
        int index = search(bits);
        if (index < 0) return false;
        moveGapTo(index);
        if (gapEnd >= capacity()) throw new IllegalStateException("gap invariant");
        kind.clear(elements, gapEnd, gapEnd + 1);
        gapEnd++;
        size--;
        return true;
    }

    public long getBits(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= size) throw new IndexOutOfBoundsException(logicalIndex);
        return kind.readBits(elements, physicalIndex(logicalIndex));
    }

    public long firstBits() {
        if (size == 0) throw new NoSuchElementException();
        return getBits(0);
    }

    public long lastBits() {
        if (size == 0) throw new NoSuchElementException();
        return getBits(size - 1);
    }

    public int binarySearchBits(long bits) { return search(bits); }

    public void clear() {
        kind.clear(elements, 0, capacity());
        size = 0;
        gapStart = 0;
        gapEnd = capacity();
    }

    public Object toPrimitiveArray() {
        Object result = kind.newArray(size);
        if (gapStart > 0) System.arraycopy(elements, 0, result, 0, gapStart);
        int suffix = capacity() - gapEnd;
        if (suffix > 0) System.arraycopy(elements, gapEnd, result, gapStart, suffix);
        return result;
    }

    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(getBits(i));
    }

    Object backingArray() { return elements; }

    private int search(long bits) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int compare = kind.compareBits(getBits(mid), bits);
            if (compare < 0) low = mid + 1;
            else if (compare > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private int physicalIndex(int logicalIndex) {
        return logicalIndex < gapStart ? logicalIndex : logicalIndex + gapSize();
    }

    private void ensureGapAt(int logicalIndex) {
        if (gapSize() == 0) growAt(logicalIndex);
        else moveGapTo(logicalIndex);
    }

    private void moveGapTo(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex > size) throw new IndexOutOfBoundsException(logicalIndex);
        if (logicalIndex == gapStart) return;
        if (logicalIndex < gapStart) {
            int count = gapStart - logicalIndex;
            System.arraycopy(elements, logicalIndex, elements, gapEnd - count, count);
            gapStart = logicalIndex;
            gapEnd -= count;
        } else {
            int count = logicalIndex - gapStart;
            System.arraycopy(elements, gapEnd, elements, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
        kind.clear(elements, gapStart, gapEnd);
    }

    private void growAt(int logicalIndex) {
        int newCapacity = capacityFor(size + 1);
        if (newCapacity <= capacity()) {
            long grown = (long) capacity() + (capacity() >>> 1) + 1L;
            if (grown > Integer.MAX_VALUE) throw new IllegalStateException("collection too large");
            newCapacity = (int) grown;
        }
        Object next = kind.newArray(newCapacity);
        int newGap = newCapacity - size;
        for (int i = 0; i < logicalIndex; i++) {
            kind.writeBits(next, i, getBits(i));
        }
        for (int i = logicalIndex; i < size; i++) {
            kind.writeBits(next, i + newGap, getBits(i));
        }
        elements = next;
        gapStart = logicalIndex;
        gapEnd = logicalIndex + newGap;
    }

    private int capacityFor(int elementCount) {
        long required = Math.max(2L, (long) Math.ceil(elementCount / (double) loadFactor));
        if (required > Integer.MAX_VALUE) throw new IllegalArgumentException("collection too large");
        return (int) required;
    }
}
