/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Non-negative int-key direct table with exact-width primitive values.
 *
 * <p>Key position is the array index; a bitset marks presence. This eliminates
 * hashing, stored key lanes and occupancy bytes when the key domain is dense.
 */
public final class PrimitiveDirectIntMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(int key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private Object values;
    private final LongBitSet present;
    private int size;

    public PrimitiveDirectIntMap(PrimitiveKind valueKind) {
        this(valueKind, 64);
    }

    public PrimitiveDirectIntMap(PrimitiveKind valueKind, int initialKeyCapacity) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (initialKeyCapacity < 0) throw new IllegalArgumentException("initialKeyCapacity < 0");
        int capacity = Math.max(1, initialKeyCapacity);
        values = valueKind.newArray(capacity);
        present = new LongBitSet(capacity);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return java.lang.reflect.Array.getLength(values); }
    public long payloadBytes() {
        return valueKind.payloadBytes(keyCapacity()) + present.payloadBytes();
    }

    public boolean containsKey(int key) {
        return key >= 0 && key < keyCapacity() && present.get(key);
    }

    public long getOrDefaultBits(int key, long defaultBits) {
        return containsKey(key) ? valueKind.readBits(values, key) : defaultBits;
    }

    public long putBits(int key, long valueBits, long absentBits) {
        if (key < 0) throw new IllegalArgumentException("key < 0");
        ensureKeyCapacity(key + 1);
        if (present.get(key)) {
            long old = valueKind.readBits(values, key);
            valueKind.writeBits(values, key, valueBits);
            return old;
        }
        valueKind.writeBits(values, key, valueBits);
        present.set(key);
        size++;
        return absentBits;
    }

    public long removeOrDefaultBits(int key, long defaultBits) {
        if (!containsKey(key)) return defaultBits;
        long old = valueKind.readBits(values, key);
        valueKind.clear(values, key, key + 1);
        present.clear(key);
        size--;
        return old;
    }

    public void clear() {
        valueKind.clear(values, 0, keyCapacity());
        present.clear();
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int key = present.nextSetBit(0); key >= 0; key = present.nextSetBit(key + 1)) {
            consumer.accept(key, valueKind.readBits(values, key));
        }
    }

    Object valueArray() { return values; }

    private void ensureKeyCapacity(int required) {
        if (required <= keyCapacity()) return;
        int next = keyCapacity() + (keyCapacity() >>> 1) + 1;
        if (next < required) next = required;
        values = valueKind.copyOf(values, next);
        // LongBitSet grows on set(), so there is no second resize step here.
    }
}
