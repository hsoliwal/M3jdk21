/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/** Mutable sorted primitive set: binary search + arraycopy, no nodes or wrappers. */
public final class PrimitiveSortedArraySet {
    private final PrimitiveKind kind;
    private Object elements;
    private int size;

    public PrimitiveSortedArraySet(PrimitiveKind kind) {
        this(kind, 8);
    }

    public PrimitiveSortedArraySet(PrimitiveKind kind, int initialCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        elements = kind.newArray(Math.max(1, initialCapacity));
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(elements); }
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    public boolean containsBits(long bits) { return search(bits) >= 0; }

    public boolean addBits(long bits) {
        int result = search(bits);
        if (result >= 0) return false;
        int insertion = -result - 1;
        ensure(size + 1);
        System.arraycopy(elements, insertion, elements, insertion + 1, size - insertion);
        kind.writeBits(elements, insertion, bits);
        size++;
        return true;
    }

    public boolean removeBits(long bits) {
        int index = search(bits);
        if (index < 0) return false;
        int moved = size - index - 1;
        if (moved > 0) System.arraycopy(elements, index + 1, elements, index, moved);
        size--;
        kind.clear(elements, size, size + 1);
        return true;
    }

    public long firstBits() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return kind.readBits(elements, 0);
    }

    public long lastBits() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return kind.readBits(elements, size - 1);
    }

    public long getBits(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public int binarySearchBits(long bits) { return search(bits); }

    public void clear() {
        kind.clear(elements, 0, size);
        size = 0;
    }

    public Object toPrimitiveArray() { return kind.copyOf(elements, size); }

    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(kind.readBits(elements, i));
    }

    Object backingArray() { return elements; }

    private int search(long bits) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int compare = kind.compareBits(kind.readBits(elements, mid), bits);
            if (compare < 0) low = mid + 1;
            else if (compare > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private void ensure(int required) {
        int capacity = capacity();
        if (required <= capacity) return;
        int next = capacity + (capacity >>> 1) + 1;
        if (next < required) next = required;
        elements = kind.copyOf(elements, next);
    }
}
