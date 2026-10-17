/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Primitive hash list-multimap backed by flat lanes.
 *
 * <p>A primitive key->int head map indexes chains stored as primitive key/value
 * arrays plus one int[] link lane. Duplicate pairs are optionally suppressed.
 * There is no Collection object per key and no entry/node object per value.
 */
public final class PrimitiveHashMultiMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final boolean uniquePairs;
    private final PrimitivePrimitiveHashMap heads;
    private Object keys;
    private Object values;
    private int[] links;
    private byte[] occupied;
    private int highWater;
    private int freeHead = -1;
    private int size;

    public PrimitiveHashMultiMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 16, false);
    }

    public PrimitiveHashMultiMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            int initialCapacity, boolean uniquePairs) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        keys = keyKind.newArray(capacity);
        values = valueKind.newArray(capacity);
        links = new int[capacity];
        occupied = new byte[capacity];
        heads = new PrimitivePrimitiveHashMap(keyKind, PrimitiveKind.INT);
        this.uniquePairs = uniquePairs;
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public boolean uniquePairs() { return uniquePairs; }
    public int size() { return size; }
    public int keyCount() { return heads.size(); }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return links.length; }
    public long payloadBytes() {
        return keyKind.payloadBytes(capacity())
                + valueKind.payloadBytes(capacity())
                + (long) links.length * Integer.BYTES
                + occupied.length
                + heads.payloadBytes();
    }

    /**
     * Returns a caller-owned live values view for one key.
     *
     * <p>The view retains only this multimap and one primitive key carrier. Missing
     * keys therefore return an empty live view whose add operation creates the
     * mapping. No collection object is retained per key. Traversal order is the
     * owner's existing newest-first chain order. The owner and view are not
     * thread-safe.</p>
     */
    public ValuesView valuesViewBits(long keyBits) {
        return new ValuesView(keyKind.storageBits(keyBits));
    }

    public final class ValuesView {
        private final long keyBits;

        private ValuesView(long keyBits) {
            this.keyBits = keyBits;
        }

        public int size() { return countKeyBits(keyBits); }
        public boolean isEmpty() { return size() == 0; }
        public boolean containsBits(long valueBits) {
            return containsEntryBits(keyBits, valueBits);
        }
        public boolean addBits(long valueBits) {
            return PrimitiveHashMultiMap.this.addBits(keyBits, valueBits);
        }
        public boolean removeBits(long valueBits) {
            return PrimitiveHashMultiMap.this.removeEntryBits(keyBits, valueBits);
        }
        public void clear() {
            PrimitiveHashMultiMap.this.removeAllKeyBits(keyBits);
        }
        public long getBits(int index) {
            Objects.checkIndex(index, size());
            int row = head(keyBits);
            for (int current = 0; current < index; current++) row = next(row);
            return valueKind.readBits(values, row);
        }
        public Object toArrayBits() {
            return PrimitiveHashMultiMap.this.valuesArrayBits(keyBits);
        }
        public void forEach(LongConsumer consumer) {
            PrimitiveHashMultiMap.this.forEachValueBits(
                    keyBits, Objects.requireNonNull(consumer, "consumer"));
        }
    }

    public boolean containsKeyBits(long keyBits) {
        return heads.containsKeyBits(keyBits);
    }

    public boolean containsEntryBits(long keyBits, long valueBits) {
        for (int row = head(keyBits); row >= 0; row = next(row)) {
            if (valueKind.equalBits(valueKind.readBits(values, row), valueBits)) return true;
        }
        return false;
    }

    public int countKeyBits(long keyBits) {
        int count = 0;
        for (int row = head(keyBits); row >= 0; row = next(row)) count++;
        return count;
    }

    public boolean addBits(long keyBits, long valueBits) {
        if (uniquePairs && containsEntryBits(keyBits, valueBits)) return false;
        int row = allocate();
        int previousHead = head(keyBits);
        keyKind.writeBits(keys, row, keyBits);
        valueKind.writeBits(values, row, valueBits);
        occupied[row] = 1;
        links[row] = previousHead + 1;
        heads.putBits(keyBits, row + 1L, 0L);
        size++;
        return true;
    }

    public boolean removeEntryBits(long keyBits, long valueBits) {
        int previous = -1;
        int row = head(keyBits);
        while (row >= 0) {
            int next = next(row);
            if (valueKind.equalBits(valueKind.readBits(values, row), valueBits)) {
                unlink(keyBits, previous, row, next);
                release(row);
                size--;
                return true;
            }
            previous = row;
            row = next;
        }
        return false;
    }

    public int removeAllKeyBits(long keyBits) {
        int row = head(keyBits);
        if (row < 0) return 0;
        heads.removeOrDefaultBits(keyBits, 0L);
        int removed = 0;
        while (row >= 0) {
            int next = next(row);
            release(row);
            removed++;
            row = next;
        }
        size -= removed;
        return removed;
    }

    public void forEachValueBits(long keyBits, LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int row = head(keyBits); row >= 0; row = next(row)) {
            consumer.accept(valueKind.readBits(values, row));
        }
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int row = 0, remaining = size; remaining > 0 && row < highWater; row++) {
            if (occupied[row] != 0) {
                consumer.accept(keyKind.readBits(keys, row), valueKind.readBits(values, row));
                remaining--;
            }
        }
    }

    /**
     * Copies a key's values into a caller-owned exact-width primitive array in
     * the same newest-first order as forEachValueBits. Missing keys yield an
     * empty array. Boolean lanes use byte[] as specified by PrimitiveKind.
     */
    public Object valuesArrayBits(long keyBits) {
        Object result = valueKind.newArray(countKeyBits(keyBits));
        int index = 0;
        for (int row = head(keyBits); row >= 0; row = next(row)) {
            valueKind.writeBits(result, index++, valueKind.readBits(values, row));
        }
        return result;
    }

    /**
     * Packs live rows and releases spare backing-array and head-index capacity.
     * Both global row traversal order and per-key chain order are preserved,
     * including after free-slot reuse. No canonical value is evicted.
     *
     * <p>Costs O(highWater + head-index capacity) time and an int[highWater]
     * relocation buffer plus replacement lanes. Old and new lanes coexist while
     * preparing the copy; callers must allow that temporary peak memory. This
     * collection is not thread-safe and must not be mutated during traversal.
     */
    public void compact() {
        int packedCapacity = Math.max(1, size);
        if (capacity() == packedCapacity && highWater == size) {
            heads.compact();
            return;
        }
        int[] relocation = new int[highWater];
        Object packedKeys = keyKind.newArray(packedCapacity);
        Object packedValues = valueKind.newArray(packedCapacity);
        int[] packedLinks = new int[packedCapacity];
        byte[] packedOccupied = new byte[packedCapacity];
        int target = 0;
        for (int row = 0; row < highWater; row++) {
            if (occupied[row] == 0) continue;
            relocation[row] = target;
            keyKind.writeBits(packedKeys, target, keyKind.readBits(keys, row));
            valueKind.writeBits(packedValues, target, valueKind.readBits(values, row));
            packedOccupied[target++] = 1;
        }
        for (int row = 0; row < highWater; row++) {
            if (occupied[row] == 0) continue;
            int successor = next(row);
            packedLinks[relocation[row]] = successor < 0 ? 0 : relocation[successor] + 1;
        }
        heads.replaceAllBits((key, encoded) -> relocation[(int) encoded - 1] + 1L);
        keys = packedKeys;
        values = packedValues;
        links = packedLinks;
        occupied = packedOccupied;
        highWater = size;
        freeHead = -1;
        heads.compact();
    }

    public void clear() {
        keyKind.clear(keys, 0, highWater);
        valueKind.clear(values, 0, highWater);
        Arrays.fill(links, 0, highWater, 0);
        Arrays.fill(occupied, 0, highWater, (byte) 0);
        heads.clear();
        highWater = 0;
        freeHead = -1;
        size = 0;
    }

    private int head(long keyBits) {
        long encoded = heads.getOrDefaultBits(keyBits, 0L);
        return encoded == 0L ? -1 : (int) encoded - 1;
    }

    private int next(int row) {
        int encoded = links[row];
        return encoded == 0 ? -1 : encoded - 1;
    }

    private void unlink(long keyBits, int previous, int row, int next) {
        if (previous < 0) {
            if (next < 0) heads.removeOrDefaultBits(keyBits, 0L);
            else heads.putBits(keyBits, next + 1L, 0L);
        } else {
            links[previous] = next + 1;
        }
    }

    private int allocate() {
        if (freeHead >= 0) {
            int row = freeHead;
            freeHead = links[row] == 0 ? -1 : links[row] - 1;
            links[row] = 0;
            return row;
        }
        ensure(highWater + 1);
        return highWater++;
    }

    private void release(int row) {
        keyKind.clear(keys, row, row + 1);
        valueKind.clear(values, row, row + 1);
        occupied[row] = 0;
        links[row] = freeHead + 1;
        freeHead = row;
    }

    private void ensure(int required) {
        if (required <= capacity()) return;
        int next = capacity() + (capacity() >>> 1) + 1;
        if (next < required) next = required;
        keys = keyKind.copyOf(keys, next);
        values = valueKind.copyOf(values, next);
        links = Arrays.copyOf(links, next);
        occupied = Arrays.copyOf(occupied, next);
    }
}
