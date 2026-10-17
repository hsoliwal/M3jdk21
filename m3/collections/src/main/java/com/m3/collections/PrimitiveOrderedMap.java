/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Insertion-ordered primitive map without linked entry objects.
 *
 * <p>The hash map owns key/value lanes; a second primitive key lane retains
 * insertion order. Removal shifts only that dense key lane.
 */
public final class PrimitiveOrderedMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitivePrimitiveHashMap values;
    private final PrimitiveArrayList order;

    public PrimitiveOrderedMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        Objects.requireNonNull(valueKind, "valueKind");
        values = new PrimitivePrimitiveHashMap(keyKind, valueKind);
        order = new PrimitiveArrayList(keyKind);
    }

    public int size() { return order.size(); }
    public boolean isEmpty() { return order.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes() + order.payloadBytes(); }

    public boolean containsKeyBits(long keyBits) { return values.containsKeyBits(keyBits); }

    public long getOrDefaultBits(long keyBits, long defaultBits) {
        return values.getOrDefaultBits(keyBits, defaultBits);
    }

    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        boolean present = values.containsKeyBits(keyBits);
        long old = values.putBits(keyBits, valueBits, absentValueBits);
        if (!present) order.addBits(keyBits);
        return old;
    }

    public long removeOrDefaultBits(long keyBits, long defaultBits) {
        if (!values.containsKeyBits(keyBits)) return defaultBits;
        long old = values.removeOrDefaultBits(keyBits, defaultBits);
        if (!order.removeBits(keyBits)) throw new IllegalStateException("order/map invariant");
        return old;
    }

    public long keyBitsAt(int index) { return order.getBits(index); }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < order.size(); i++) {
            long keyBits = order.getBits(i);
            consumer.accept(keyBits, values.getOrDefaultBits(keyBits, 0L));
        }
    }

    public void clear() {
        values.clear();
        order.clear();
    }
}
