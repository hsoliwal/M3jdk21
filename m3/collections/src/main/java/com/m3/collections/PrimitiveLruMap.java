/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-capacity primitive LRU map with O(1) lookup/update/eviction.
 *
 * <p>A primitive hash index maps keys to stable slot numbers. Keys and values
 * live in exact-width lanes; access order uses int[] previous/next lanes.
 * There are no linked-node objects per mapping.
 */
public final class PrimitiveLruMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final int maxEntries;
    private final PrimitivePrimitiveHashMap index;
    private final Object keys;
    private final Object values;
    private final int[] previous;
    private final int[] next;
    private final int[] freeNext;
    private int size;
    private int eldest = -1;
    private int newest = -1;
    private int freeHead;

    public PrimitiveLruMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int maxEntries) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries <= 0");
        this.maxEntries = maxEntries;
        index = new PrimitivePrimitiveHashMap(
                keyKind, PrimitiveKind.INT, maxEntries, 0.65f);
        keys = keyKind.newArray(maxEntries);
        values = valueKind.newArray(maxEntries);
        previous = new int[maxEntries];
        next = new int[maxEntries];
        freeNext = new int[maxEntries];
        Arrays.fill(previous, -1);
        Arrays.fill(next, -1);
        for (int i = 0; i < maxEntries - 1; i++) freeNext[i] = i + 1;
        freeNext[maxEntries - 1] = -1;
        freeHead = 0;
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int maxEntries() { return maxEntries; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return keyKind.payloadBytes(maxEntries)
                + valueKind.payloadBytes(maxEntries)
                + (long) previous.length * Integer.BYTES
                + (long) next.length * Integer.BYTES
                + (long) freeNext.length * Integer.BYTES
                + index.payloadBytes();
    }

    public boolean containsKeyBits(long keyBits) {
        return index.containsKeyBits(keyBits);
    }

    /** Lookup that updates access order. */
    public long getOrDefaultBits(long keyBits, long defaultValueBits) {
        int slot = slotFor(keyBits);
        if (slot < 0) return defaultValueBits;
        touch(slot);
        return valueKind.readBits(values, slot);
    }

    /** Lookup that leaves access order unchanged. */
    public long peekOrDefaultBits(long keyBits, long defaultValueBits) {
        int slot = slotFor(keyBits);
        return slot < 0 ? defaultValueBits : valueKind.readBits(values, slot);
    }

    /**
     * Inserts/updates and marks the key most-recently used.
     * Returns the previous value or absentValueBits.
     */
    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        int slot = slotFor(keyBits);
        if (slot >= 0) {
            long old = valueKind.readBits(values, slot);
            valueKind.writeBits(values, slot, valueBits);
            touch(slot);
            return old;
        }

        if (size == maxEntries) {
            slot = eldest;
            long evictedKey = keyKind.readBits(keys, slot);
            index.removeOrDefaultBits(evictedKey, 0L);
            detach(slot);
        } else {
            slot = allocate();
            size++;
        }

        keyKind.writeBits(keys, slot, keyBits);
        valueKind.writeBits(values, slot, valueBits);
        index.putBits(keyBits, slot + 1L, 0L);
        appendNewest(slot);
        return absentValueBits;
    }

    public long removeOrDefaultBits(long keyBits, long defaultValueBits) {
        int slot = slotFor(keyBits);
        if (slot < 0) return defaultValueBits;
        long old = valueKind.readBits(values, slot);
        index.removeOrDefaultBits(keyBits, 0L);
        detach(slot);
        release(slot);
        size--;
        return old;
    }

    public boolean evictEldest(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (eldest < 0) return false;
        int slot = eldest;
        long key = keyKind.readBits(keys, slot);
        long value = valueKind.readBits(values, slot);
        index.removeOrDefaultBits(key, 0L);
        detach(slot);
        release(slot);
        size--;
        consumer.accept(key, value);
        return true;
    }

    public long eldestKeyBits() {
        if (eldest < 0) throw new java.util.NoSuchElementException();
        return keyKind.readBits(keys, eldest);
    }

    public long newestKeyBits() {
        if (newest < 0) throw new java.util.NoSuchElementException();
        return keyKind.readBits(keys, newest);
    }

    public void forEachLeastToMost(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int slot = eldest; slot >= 0; slot = next[slot]) {
            consumer.accept(
                    keyKind.readBits(keys, slot),
                    valueKind.readBits(values, slot));
        }
    }

    public void clear() {
        index.clear();
        keyKind.clear(keys, 0, maxEntries);
        valueKind.clear(values, 0, maxEntries);
        Arrays.fill(previous, -1);
        Arrays.fill(next, -1);
        for (int i = 0; i < maxEntries - 1; i++) freeNext[i] = i + 1;
        freeNext[maxEntries - 1] = -1;
        freeHead = 0;
        eldest = -1;
        newest = -1;
        size = 0;
    }

    private int slotFor(long keyBits) {
        long encoded = index.getOrDefaultBits(keyBits, 0L);
        return encoded == 0L ? -1 : (int) encoded - 1;
    }

    private int allocate() {
        if (freeHead < 0) throw new IllegalStateException("free-list invariant");
        int slot = freeHead;
        freeHead = freeNext[slot];
        freeNext[slot] = -1;
        return slot;
    }

    private void release(int slot) {
        keyKind.clear(keys, slot, slot + 1);
        valueKind.clear(values, slot, slot + 1);
        previous[slot] = -1;
        next[slot] = -1;
        freeNext[slot] = freeHead;
        freeHead = slot;
    }

    private void touch(int slot) {
        if (slot == newest) return;
        detach(slot);
        appendNewest(slot);
    }

    private void detach(int slot) {
        int before = previous[slot];
        int after = next[slot];
        if (before >= 0) next[before] = after;
        else eldest = after;
        if (after >= 0) previous[after] = before;
        else newest = before;
        previous[slot] = -1;
        next[slot] = -1;
    }

    private void appendNewest(int slot) {
        previous[slot] = newest;
        next[slot] = -1;
        if (newest >= 0) next[newest] = slot;
        else eldest = slot;
        newest = slot;
    }
}
