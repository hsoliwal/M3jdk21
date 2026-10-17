/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Full signed-short-domain direct map with exact-width primitive values.
 *
 * <p>{@link Short#toUnsignedInt(short)} maps every signed short to one of 65,536
 * slots. Presence costs one bit per possible key. No key array or hash metadata
 * is retained.
 */
public final class PrimitiveDirectShortMap {
    private static final int DOMAIN_SIZE = 1 << Short.SIZE;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(short key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final Object values;
    private final long[] present = new long[DOMAIN_SIZE >>> 6];
    private int size;

    public PrimitiveDirectShortMap(PrimitiveKind valueKind) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        values = valueKind.newArray(DOMAIN_SIZE);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return DOMAIN_SIZE; }

    public long payloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE)
                + (long) present.length * Long.BYTES;
    }

    public boolean containsKey(short key) {
        int index = Short.toUnsignedInt(key);
        return (present[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    public long getOrDefaultBits(short key, long defaultBits) {
        int index = Short.toUnsignedInt(key);
        return containsKey(key) ? valueKind.readBits(values, index) : defaultBits;
    }

    public long putBits(short key, long valueBits, long absentBits) {
        int index = Short.toUnsignedInt(key);
        long bit = 1L << (index & 63);
        int word = index >>> 6;
        boolean contained = (present[word] & bit) != 0L;
        long old = contained ? valueKind.readBits(values, index) : absentBits;
        valueKind.writeBits(values, index, valueBits);
        if (!contained) {
            present[word] |= bit;
            size++;
        }
        return old;
    }

    public long removeOrDefaultBits(short key, long defaultBits) {
        int index = Short.toUnsignedInt(key);
        long bit = 1L << (index & 63);
        int word = index >>> 6;
        if ((present[word] & bit) == 0L) return defaultBits;
        long old = valueKind.readBits(values, index);
        valueKind.clear(values, index, index + 1);
        present[word] &= ~bit;
        size--;
        return old;
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, DOMAIN_SIZE);
        Arrays.fill(present, 0L);
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                int index = (wordIndex << 6) + bit;
                consumer.accept((short) index, valueKind.readBits(values, index));
                word &= word - 1L;
            }
        }
    }

    Object valueArray() { return values; }
    long[] presenceWords() { return present; }
}
