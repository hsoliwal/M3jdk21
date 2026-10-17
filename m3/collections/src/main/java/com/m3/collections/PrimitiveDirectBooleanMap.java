/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Two-key direct map for boolean keys and exact-width primitive values.
 *
 * <p>The key itself selects index 0/1. A two-bit collection-level mask records
 * presence, so there is no retained key lane, hash table, node or entry object.
 */
public final class PrimitiveDirectBooleanMap {
    private static final int DOMAIN_SIZE = 2;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(boolean key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final Object values;
    private byte presentMask;
    private int size;

    public PrimitiveDirectBooleanMap(PrimitiveKind valueKind) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        values = valueKind.newArray(DOMAIN_SIZE);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return DOMAIN_SIZE; }

    public long payloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE) + Byte.BYTES;
    }

    public boolean containsKey(boolean key) {
        return (presentMask & bit(key)) != 0;
    }

    public long getOrDefaultBits(boolean key, long defaultBits) {
        int index = index(key);
        return containsKey(key) ? valueKind.readBits(values, index) : defaultBits;
    }

    public long putBits(boolean key, long valueBits, long absentBits) {
        int index = index(key);
        byte bit = bit(key);
        boolean present = (presentMask & bit) != 0;
        long old = present ? valueKind.readBits(values, index) : absentBits;
        valueKind.writeBits(values, index, valueBits);
        if (!present) {
            presentMask |= bit;
            size++;
        }
        return old;
    }

    public long removeOrDefaultBits(boolean key, long defaultBits) {
        int index = index(key);
        byte bit = bit(key);
        if ((presentMask & bit) == 0) return defaultBits;
        long old = valueKind.readBits(values, index);
        valueKind.clear(values, index, index + 1);
        presentMask &= (byte) ~bit;
        size--;
        return old;
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, DOMAIN_SIZE);
        presentMask = 0;
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if ((presentMask & 1) != 0) consumer.accept(false, valueKind.readBits(values, 0));
        if ((presentMask & 2) != 0) consumer.accept(true, valueKind.readBits(values, 1));
    }

    Object valueArray() { return values; }

    private static int index(boolean key) { return key ? 1 : 0; }
    private static byte bit(boolean key) { return (byte) (key ? 2 : 1); }
}
