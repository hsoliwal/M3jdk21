/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/** Read-dense sorted primitive map: binary search + parallel-array copy on mutation. */
public final class PrimitiveSortedArrayMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private Object keys;
    private Object values;
    private int size;

    public PrimitiveSortedArrayMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 8);
    }

    public PrimitiveSortedArrayMap(PrimitiveKind keyKind, PrimitiveKind valueKind, int initialCapacity) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        keys = keyKind.newArray(capacity);
        values = valueKind.newArray(capacity);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(keys); }
    public long payloadBytes() {
        return keyKind.payloadBytes(capacity()) + valueKind.payloadBytes(capacity());
    }

    public boolean containsKeyBits(long keyBits) { return search(keyBits) >= 0; }

    public long getOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        return index < 0 ? defaultValueBits : valueKind.readBits(values, index);
    }

    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        int result = search(keyBits);
        if (result >= 0) {
            long old = valueKind.readBits(values, result);
            valueKind.writeBits(values, result, valueBits);
            return old;
        }
        int insertion = -result - 1;
        ensure(size + 1);
        System.arraycopy(keys, insertion, keys, insertion + 1, size - insertion);
        System.arraycopy(values, insertion, values, insertion + 1, size - insertion);
        keyKind.writeBits(keys, insertion, keyBits);
        valueKind.writeBits(values, insertion, valueBits);
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        if (index < 0) return defaultValueBits;
        long old = valueKind.readBits(values, index);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        keyKind.clear(keys, size, size + 1);
        valueKind.clear(values, size, size + 1);
        return old;
    }

    public long firstKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyKind.readBits(keys, 0);
    }

    public long lastKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyKind.readBits(keys, size - 1);
    }

    public int binarySearchKeyBits(long keyBits) { return search(keyBits); }

    public void forEach(EntryConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) {
            action.accept(keyKind.readBits(keys, i), valueKind.readBits(values, i));
        }
    }

    public void clear() {
        keyKind.clear(keys, 0, size);
        valueKind.clear(values, 0, size);
        size = 0;
    }

    Object keyArray() { return keys; }
    Object valueArray() { return values; }

    private int search(long keyBits) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int comparison = keyKind.compareBits(keyKind.readBits(keys, mid), keyBits);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private void ensure(int required) {
        if (required <= capacity()) return;
        int next = capacity() + (capacity() >>> 1) + 1;
        if (next < required) next = required;
        keys = keyKind.copyOf(keys, next);
        values = valueKind.copyOf(values, next);
    }
}
