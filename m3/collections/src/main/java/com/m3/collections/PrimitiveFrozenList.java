/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/** Exact-sized immutable primitive list snapshot. */
public final class PrimitiveFrozenList {
    private final PrimitiveKind kind;
    private final Object elements;

    private PrimitiveFrozenList(PrimitiveKind kind, Object elements) {
        this.kind = kind;
        this.elements = elements;
    }

    public static PrimitiveFrozenList copyOf(PrimitiveArrayList source) {
        Objects.requireNonNull(source, "source");
        return new PrimitiveFrozenList(source.kind(), source.toPrimitiveArray());
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return java.lang.reflect.Array.getLength(elements); }
    public boolean isEmpty() { return size() == 0; }
    public long payloadBytes() { return kind.payloadBytes(size()); }

    public long getBits(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public int indexOfBits(long bits) {
        for (int i = 0; i < size(); i++) {
            if (kind.equalBits(kind.readBits(elements, i), bits)) return i;
        }
        return -1;
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size(); i++) consumer.accept(kind.readBits(elements, i));
    }

    public Object toPrimitiveArray() {
        return kind.copyOf(elements, size());
    }
}
