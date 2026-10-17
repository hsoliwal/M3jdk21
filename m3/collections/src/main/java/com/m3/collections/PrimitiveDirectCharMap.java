/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Dense char-domain direct map with exact-width primitive values.
 *
 * <p>The 16-bit char key is the value-array index. Presence costs one bit per
 * possible key. This representation is intended for sufficiently dense char
 * domains where retaining hash keys/probe slack costs more than the fixed table.
 */
public final class PrimitiveDirectCharMap {
    private static final int DOMAIN_SIZE = 1 << Character.SIZE;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(char key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final Object values;
    private final long[] present = new long[DOMAIN_SIZE >>> 6];
    private int size;

    public PrimitiveDirectCharMap(PrimitiveKind valueKind) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.values = valueKind.newArray(DOMAIN_SIZE);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return DOMAIN_SIZE; }

    /** Primitive payload only; JVM headers/alignment are intentionally excluded. */
    public long payloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE)
                + (long) present.length * Long.BYTES;
    }

    public boolean containsKey(char key) {
        return (present[key >>> 6] & (1L << (key & 63))) != 0L;
    }

    public long getOrDefaultBits(char key, long defaultBits) {
        return containsKey(key) ? valueKind.readBits(values, key) : defaultBits;
    }

    public long putBits(char key, long valueBits, long absentBits) {
        boolean contained = containsKey(key);
        long old = contained ? valueKind.readBits(values, key) : absentBits;
        valueKind.writeBits(values, key, valueBits);
        if (!contained) {
            present[key >>> 6] |= 1L << (key & 63);
            size++;
        }
        return old;
    }

    public long removeOrDefaultBits(char key, long defaultBits) {
        if (!containsKey(key)) return defaultBits;
        long old = valueKind.readBits(values, key);
        valueKind.clear(values, key, key + 1);
        present[key >>> 6] &= ~(1L << (key & 63));
        size--;
        return old;
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, DOMAIN_SIZE);
        java.util.Arrays.fill(present, 0L);
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                int key = (wordIndex << 6) + bit;
                consumer.accept((char) key, valueKind.readBits(values, key));
                word &= word - 1L;
            }
        }
    }

    Object valueArray() { return values; }
    long[] presenceWords() { return present; }
}
