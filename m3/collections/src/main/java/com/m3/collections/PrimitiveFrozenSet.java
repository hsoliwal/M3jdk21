/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/** Exact-sized immutable sorted primitive set snapshot. */
public final class PrimitiveFrozenSet {
    private final PrimitiveKind kind;
    private final Object elements;

    private PrimitiveFrozenSet(PrimitiveKind kind, Object elements) {
        this.kind = kind;
        this.elements = elements;
    }

    public static PrimitiveFrozenSet copyOf(PrimitiveHashSet source) {
        Objects.requireNonNull(source, "source");
        Object values = source.toPrimitiveArray();
        source.kind().sort(values, 0, java.lang.reflect.Array.getLength(values));
        return new PrimitiveFrozenSet(source.kind(), values);
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return java.lang.reflect.Array.getLength(elements); }
    public boolean isEmpty() { return size() == 0; }
    public long payloadBytes() { return kind.payloadBytes(size()); }

    public boolean containsBits(long bits) {
        return binarySearch(bits) >= 0;
    }

    public long firstBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return kind.readBits(elements, 0);
    }

    public long lastBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return kind.readBits(elements, size() - 1);
    }

    public long lowerOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultBits : kind.readBits(elements, candidate);
    }

    public long floorOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index : -index - 2;
        return candidate < 0 ? defaultBits : kind.readBits(elements, candidate);
    }

    public long ceilingOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index : -index - 1;
        return candidate >= size() ? defaultBits : kind.readBits(elements, candidate);
    }

    public long higherOrDefaultBits(long bits, long defaultBits) {
        int index = binarySearch(bits);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size() ? defaultBits : kind.readBits(elements, candidate);
    }

    public long getBits(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size(); i++) consumer.accept(kind.readBits(elements, i));
    }

    public Object toPrimitiveArray() { return kind.copyOf(elements, size()); }

    private int binarySearch(long bits) {
        return kind.binarySearch(elements, 0, size(), bits);
    }
}
