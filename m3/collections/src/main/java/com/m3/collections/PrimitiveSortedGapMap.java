/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/** Sorted exact-width primitive map with aligned key/value lanes and one movable sparse gap. */
public final class PrimitiveSortedGapMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final float loadFactor;
    private Object keys;
    private Object values;
    private int gapStart;
    private int gapEnd;
    private int size;

    public PrimitiveSortedGapMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 8, DEFAULT_LOAD_FACTOR);
    }

    public PrimitiveSortedGapMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int expected, float loadFactor) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (expected < 0) throw new IllegalArgumentException("expected < 0");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        this.loadFactor = loadFactor;
        int capacity = capacityFor(Math.max(1, expected));
        keys = keyKind.newArray(capacity);
        values = valueKind.newArray(capacity);
        gapStart = 0;
        gapEnd = capacity;
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(keys); }
    public int gapSize() { return gapEnd - gapStart; }
    public long payloadBytes() {
        return keyKind.payloadBytes(capacity()) + valueKind.payloadBytes(capacity());
    }

    public boolean containsKeyBits(long keyBits) { return search(keyBits) >= 0; }

    public long getOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        return index < 0 ? defaultValueBits : valueKind.readBits(values, physicalIndex(index));
    }

    /** Returns prior value carrier, or absentValueBits when a new key is inserted. */
    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        int result = search(keyBits);
        if (result >= 0) {
            int physical = physicalIndex(result);
            long old = valueKind.readBits(values, physical);
            valueKind.writeBits(values, physical, valueBits);
            return old;
        }
        int insertion = -result - 1;
        ensureGapAt(insertion);
        keyKind.writeBits(keys, gapStart, keyBits);
        valueKind.writeBits(values, gapStart, valueBits);
        gapStart++;
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        if (index < 0) return defaultValueBits;
        moveGapTo(index);
        if (gapEnd >= capacity()) throw new IllegalStateException("gap invariant");
        long old = valueKind.readBits(values, gapEnd);
        keyKind.clear(keys, gapEnd, gapEnd + 1);
        valueKind.clear(values, gapEnd, gapEnd + 1);
        gapEnd++;
        size--;
        return old;
    }

    public long firstKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyAt(0);
    }

    public long lastKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyAt(size - 1);
    }

    public void forEach(EntryConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) {
            int physical = physicalIndex(i);
            action.accept(keyKind.readBits(keys, physical), valueKind.readBits(values, physical));
        }
    }

    public void clear() {
        keyKind.clear(keys, 0, capacity());
        valueKind.clear(values, 0, capacity());
        size = 0;
        gapStart = 0;
        gapEnd = capacity();
    }

    Object keyArray() { return keys; }
    Object valueArray() { return values; }

    private long keyAt(int logicalIndex) {
        return keyKind.readBits(keys, physicalIndex(logicalIndex));
    }

    private int search(long keyBits) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int compare = keyKind.compareBits(keyAt(mid), keyBits);
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
            System.arraycopy(keys, logicalIndex, keys, gapEnd - count, count);
            System.arraycopy(values, logicalIndex, values, gapEnd - count, count);
            gapStart = logicalIndex;
            gapEnd -= count;
        } else {
            int count = logicalIndex - gapStart;
            System.arraycopy(keys, gapEnd, keys, gapStart, count);
            System.arraycopy(values, gapEnd, values, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
        keyKind.clear(keys, gapStart, gapEnd);
        valueKind.clear(values, gapStart, gapEnd);
    }

    private void growAt(int logicalIndex) {
        int newCapacity = capacityFor(size + 1);
        if (newCapacity <= capacity()) {
            long grown = (long) capacity() + (capacity() >>> 1) + 1L;
            if (grown > Integer.MAX_VALUE) throw new IllegalStateException("collection too large");
            newCapacity = (int) grown;
        }
        Object nextKeys = keyKind.newArray(newCapacity);
        Object nextValues = valueKind.newArray(newCapacity);
        int newGap = newCapacity - size;
        for (int i = 0; i < logicalIndex; i++) copyEntry(i, nextKeys, nextValues, i);
        for (int i = logicalIndex; i < size; i++) copyEntry(i, nextKeys, nextValues, i + newGap);
        keys = nextKeys;
        values = nextValues;
        gapStart = logicalIndex;
        gapEnd = logicalIndex + newGap;
    }

    private void copyEntry(int logicalSource, Object targetKeys, Object targetValues, int targetIndex) {
        int physical = physicalIndex(logicalSource);
        keyKind.writeBits(targetKeys, targetIndex, keyKind.readBits(keys, physical));
        valueKind.writeBits(targetValues, targetIndex, valueKind.readBits(values, physical));
    }

    private int capacityFor(int elementCount) {
        long required = Math.max(2L, (long) Math.ceil(elementCount / (double) loadFactor));
        if (required > Integer.MAX_VALUE) throw new IllegalArgumentException("collection too large");
        return (int) required;
    }
}
