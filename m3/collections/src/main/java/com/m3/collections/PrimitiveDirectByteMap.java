/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Full byte-domain direct map with exact-width primitive values.
 *
 * <p>The byte key itself is the value-array index. Four primitive long fields
 * hold the 256 presence bits, so no key array, hash table, occupancy array,
 * node, entry, or wrapper is retained per mapping.
 */
public final class PrimitiveDirectByteMap {
    private static final int DOMAIN_SIZE = 1 << Byte.SIZE;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(byte key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final Object values;
    private long present0;
    private long present1;
    private long present2;
    private long present3;
    private int size;

    public PrimitiveDirectByteMap(PrimitiveKind valueKind) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.values = valueKind.newArray(DOMAIN_SIZE);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return DOMAIN_SIZE; }

    /** Primitive payload only; JVM headers/alignment are intentionally excluded. */
    public long payloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE) + 4L * Long.BYTES;
    }

    public boolean containsKey(byte key) {
        return containsIndex(Byte.toUnsignedInt(key));
    }

    public boolean containsUnsignedKey(int key) {
        return containsIndex(checkUnsignedKey(key));
    }

    public long getOrDefaultBits(byte key, long defaultBits) {
        int index = Byte.toUnsignedInt(key);
        return containsIndex(index) ? valueKind.readBits(values, index) : defaultBits;
    }

    public long getUnsignedOrDefaultBits(int key, long defaultBits) {
        int index = checkUnsignedKey(key);
        return containsIndex(index) ? valueKind.readBits(values, index) : defaultBits;
    }

    public long putBits(byte key, long valueBits, long absentBits) {
        return putIndex(Byte.toUnsignedInt(key), valueBits, absentBits);
    }

    public long putUnsignedBits(int key, long valueBits, long absentBits) {
        return putIndex(checkUnsignedKey(key), valueBits, absentBits);
    }

    public long removeOrDefaultBits(byte key, long defaultBits) {
        return removeIndex(Byte.toUnsignedInt(key), defaultBits);
    }

    public long removeUnsignedOrDefaultBits(int key, long defaultBits) {
        return removeIndex(checkUnsignedKey(key), defaultBits);
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, DOMAIN_SIZE);
        present0 = 0L;
        present1 = 0L;
        present2 = 0L;
        present3 = 0L;
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        emitWord(consumer, present0, 0);
        emitWord(consumer, present1, 64);
        emitWord(consumer, present2, 128);
        emitWord(consumer, present3, 192);
    }

    Object valueArray() { return values; }

    private long putIndex(int index, long valueBits, long absentBits) {
        boolean present = containsIndex(index);
        long old = present ? valueKind.readBits(values, index) : absentBits;
        valueKind.writeBits(values, index, valueBits);
        if (!present) {
            setPresent(index);
            size++;
        }
        return old;
    }

    private long removeIndex(int index, long defaultBits) {
        if (!containsIndex(index)) return defaultBits;
        long old = valueKind.readBits(values, index);
        valueKind.clear(values, index, index + 1);
        clearPresent(index);
        size--;
        return old;
    }

    private boolean containsIndex(int index) {
        long bit = 1L << (index & 63);
        return switch (index >>> 6) {
            case 0 -> (present0 & bit) != 0L;
            case 1 -> (present1 & bit) != 0L;
            case 2 -> (present2 & bit) != 0L;
            case 3 -> (present3 & bit) != 0L;
            default -> throw new AssertionError(index);
        };
    }

    private void setPresent(int index) {
        long bit = 1L << (index & 63);
        switch (index >>> 6) {
            case 0 -> present0 |= bit;
            case 1 -> present1 |= bit;
            case 2 -> present2 |= bit;
            case 3 -> present3 |= bit;
            default -> throw new AssertionError(index);
        }
    }

    private void clearPresent(int index) {
        long mask = ~(1L << (index & 63));
        switch (index >>> 6) {
            case 0 -> present0 &= mask;
            case 1 -> present1 &= mask;
            case 2 -> present2 &= mask;
            case 3 -> present3 &= mask;
            default -> throw new AssertionError(index);
        }
    }

    private void emitWord(EntryConsumer consumer, long word, int base) {
        while (word != 0L) {
            int bit = Long.numberOfTrailingZeros(word);
            int index = base + bit;
            consumer.accept((byte) index, valueKind.readBits(values, index));
            word &= word - 1L;
        }
    }

    private static int checkUnsignedKey(int key) {
        if ((key & ~0xff) != 0) throw new IllegalArgumentException("key must be 0..255");
        return key;
    }
}
