/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Exact-sized immutable primitive map snapshot.
 *
 * <p>Keys are sorted with paired values using in-place heap sort. Retained
 * state is exactly two primitive arrays and no hash slack or entry objects.
 */
public final class PrimitiveFrozenMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final Object keys;
    private final Object values;

    private PrimitiveFrozenMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, Object keys, Object values) {
        this.keyKind = keyKind;
        this.valueKind = valueKind;
        this.keys = keys;
        this.values = values;
    }

    public static PrimitiveFrozenMap copyOf(PrimitivePrimitiveHashMap source) {
        Objects.requireNonNull(source, "source");
        int size = source.size();
        PrimitiveKind keyKind = source.keyKind();
        PrimitiveKind valueKind = source.valueKind();
        Object keys = keyKind.newArray(size);
        Object values = valueKind.newArray(size);
        int[] cursor = new int[1];
        source.forEach((keyBits, valueBits) -> {
            int index = cursor[0]++;
            keyKind.writeBits(keys, index, keyBits);
            valueKind.writeBits(values, index, valueBits);
        });
        heapSortPairs(keyKind, valueKind, keys, values, size);
        return new PrimitiveFrozenMap(keyKind, valueKind, keys, values);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return java.lang.reflect.Array.getLength(keys); }
    public boolean isEmpty() { return size() == 0; }
    public long payloadBytes() {
        return keyKind.payloadBytes(size()) + valueKind.payloadBytes(size());
    }

    public boolean containsKeyBits(long keyBits) {
        return binarySearch(keyBits) >= 0;
    }

    public long getOrDefaultBits(long keyBits, long defaultBits) {
        int index = binarySearch(keyBits);
        return index < 0 ? defaultBits : valueKind.readBits(values, index);
    }

    public boolean containsValueBits(long valueBits) {
        for (int index = 0; index < size(); index++) {
            if (valueKind.equalBits(valueKind.readBits(values, index), valueBits)) return true;
        }
        return false;
    }

    public long firstKeyBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return keyKind.readBits(keys, 0);
    }

    public long lastKeyBits() {
        if (isEmpty()) throw new NoSuchElementException();
        return keyKind.readBits(keys, size() - 1);
    }

    public long lowerKeyOrDefaultBits(long keyBits, long defaultBits) {
        int index = binarySearch(keyBits);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultBits : keyKind.readBits(keys, candidate);
    }

    public long floorKeyOrDefaultBits(long keyBits, long defaultBits) {
        int index = binarySearch(keyBits);
        int candidate = index >= 0 ? index : -index - 2;
        return candidate < 0 ? defaultBits : keyKind.readBits(keys, candidate);
    }

    public long ceilingKeyOrDefaultBits(long keyBits, long defaultBits) {
        int index = binarySearch(keyBits);
        int candidate = index >= 0 ? index : -index - 1;
        return candidate >= size() ? defaultBits : keyKind.readBits(keys, candidate);
    }

    public long higherKeyOrDefaultBits(long keyBits, long defaultBits) {
        int index = binarySearch(keyBits);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size() ? defaultBits : keyKind.readBits(keys, candidate);
    }

    public long keyBitsAt(int index) {
        check(index);
        return keyKind.readBits(keys, index);
    }

    public long valueBitsAt(int index) {
        check(index);
        return valueKind.readBits(values, index);
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size(); i++) {
            consumer.accept(keyKind.readBits(keys, i), valueKind.readBits(values, i));
        }
    }

    private int binarySearch(long keyBits) {
        int low = 0;
        int high = size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int comparison = keyKind.compareBits(keyKind.readBits(keys, mid), keyBits);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private void check(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
    }

    private static void heapSortPairs(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            Object keys, Object values, int size) {
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            siftDown(keyKind, valueKind, keys, values, parent, size);
        }
        for (int end = size - 1; end > 0; end--) {
            swap(keyKind, valueKind, keys, values, 0, end);
            siftDown(keyKind, valueKind, keys, values, 0, end);
        }
    }

    private static void siftDown(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            Object keys, Object values, int root, int size) {
        int index = root;
        while (true) {
            int child = (index << 1) + 1;
            if (child >= size) return;
            int right = child + 1;
            if (right < size
                    && keyKind.compareBits(
                            keyKind.readBits(keys, right),
                            keyKind.readBits(keys, child)) > 0) {
                child = right;
            }
            if (keyKind.compareBits(
                    keyKind.readBits(keys, index),
                    keyKind.readBits(keys, child)) >= 0) {
                return;
            }
            swap(keyKind, valueKind, keys, values, index, child);
            index = child;
        }
    }

    private static void swap(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            Object keys, Object values, int left, int right) {
        long key = keyKind.readBits(keys, left);
        long value = valueKind.readBits(values, left);
        keyKind.writeBits(keys, left, keyKind.readBits(keys, right));
        valueKind.writeBits(values, left, valueKind.readBits(values, right));
        keyKind.writeBits(keys, right, key);
        valueKind.writeBits(values, right, value);
    }
}
