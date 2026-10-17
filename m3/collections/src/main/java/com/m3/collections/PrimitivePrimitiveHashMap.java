/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/** Exact-width primitive-to-primitive map covering the full 8x8 primitive matrix. */
public final class PrimitivePrimitiveHashMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final float loadFactor;
    private Object keys;
    private Object values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZeroKey;
    private long zeroValueBits;

    public PrimitivePrimitiveHashMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 16, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public PrimitivePrimitiveHashMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int expected, float loadFactor) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.loadFactor = loadFactor;
        int capacity = DenseHash.capacityFor(expected, loadFactor);
        keys = keyKind.newArray(capacity);
        values = valueKind.newArray(capacity);
        mask = capacity - 1;
        threshold = DenseHash.threshold(capacity, loadFactor);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(keys); }
    public long payloadBytes() {
        return keyKind.payloadBytes(capacity()) + valueKind.payloadBytes(capacity());
    }

    public boolean containsKeyBits(long keyBits) {
        return isZeroKey(keyBits) ? hasZeroKey : findNonZero(keyBits) >= 0;
    }

    public long getOrDefaultBits(long keyBits, long defaultValueBits) {
        if (isZeroKey(keyBits)) return hasZeroKey ? zeroValueBits : defaultValueBits;
        int position = findNonZero(keyBits);
        return position < 0 ? defaultValueBits : valueKind.readBits(values, position);
    }

    /** Returns the previous value carrier, or {@code absentValueBits} if the key was absent. */
    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        if (isZeroKey(keyBits)) {
            long previous = hasZeroKey ? zeroValueBits : absentValueBits;
            if (!hasZeroKey) {
                hasZeroKey = true;
                size++;
                if (size > threshold) rehash(capacity() << 1);
            }
            zeroValueBits = valueKind.storageBits(valueBits);
            return previous;
        }

        int position = keyKind.hashBits(keyBits) & mask;
        long current;
        while (!isZeroStored(current = keyKind.readBits(keys, position))) {
            if (keyKind.equalBits(current, keyBits)) {
                long old = valueKind.readBits(values, position);
                valueKind.writeBits(values, position, valueBits);
                return old;
            }
            position = (position + 1) & mask;
        }
        if (size + 1 > threshold) {
            rehash(capacity() << 1);
            position = keyKind.hashBits(keyBits) & mask;
            while (!isZeroStored(keyKind.readBits(keys, position))) {
                position = (position + 1) & mask;
            }
        }
        keyKind.writeBits(keys, position, keyBits);
        valueKind.writeBits(values, position, valueBits);
        size++;
        return absentValueBits;
    }

    public boolean containsValueBits(long valueBits) {
        if (hasZeroKey && valueKind.equalBits(zeroValueBits, valueBits)) return true;
        int remaining = size - (hasZeroKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (!isZeroStored(key)) {
                if (valueKind.equalBits(valueKind.readBits(values, i), valueBits)) return true;
                remaining--;
            }
        }
        return false;
    }

    public long putIfAbsentBits(long keyBits, long valueBits, long absentValueBits) {
        if (isZeroKey(keyBits)) {
            if (hasZeroKey) return zeroValueBits;
            putBits(keyBits, valueBits, absentValueBits);
            return absentValueBits;
        }
        int position = findNonZero(keyBits);
        if (position >= 0) return valueKind.readBits(values, position);
        putBits(keyBits, valueBits, absentValueBits);
        return absentValueBits;
    }

    public long replaceOrDefaultBits(long keyBits, long valueBits, long absentValueBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey) return absentValueBits;
            long old = zeroValueBits;
            zeroValueBits = valueKind.storageBits(valueBits);
            return old;
        }
        int position = findNonZero(keyBits);
        if (position < 0) return absentValueBits;
        long old = valueKind.readBits(values, position);
        valueKind.writeBits(values, position, valueBits);
        return old;
    }

    public boolean replaceBits(long keyBits, long expectedValueBits, long newValueBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey || !valueKind.equalBits(zeroValueBits, expectedValueBits)) return false;
            zeroValueBits = valueKind.storageBits(newValueBits);
            return true;
        }
        int position = findNonZero(keyBits);
        if (position < 0
                || !valueKind.equalBits(
                        valueKind.readBits(values, position), expectedValueBits)) {
            return false;
        }
        valueKind.writeBits(values, position, newValueBits);
        return true;
    }

    public boolean removeBits(long keyBits, long expectedValueBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey || !valueKind.equalBits(zeroValueBits, expectedValueBits)) return false;
            hasZeroKey = false;
            zeroValueBits = 0L;
            size--;
            return true;
        }
        int position = findNonZero(keyBits);
        if (position < 0
                || !valueKind.equalBits(
                        valueKind.readBits(values, position), expectedValueBits)) {
            return false;
        }
        shiftKeys(position);
        size--;
        return true;
    }

    public long computeIfAbsentBits(
            long keyBits, LongUnaryOperator mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        if (containsKeyBits(keyBits)) return getOrDefaultBits(keyBits, 0L);
        long valueBits = mappingFunction.applyAsLong(keyBits);
        putBits(keyBits, valueBits, 0L);
        return valueBits;
    }

    public long computeIfPresentBits(
            long keyBits, long absentValueBits, LongBinaryOperator remappingFunction) {
        Objects.requireNonNull(remappingFunction, "remappingFunction");
        if (!containsKeyBits(keyBits)) return absentValueBits;
        long previous = getOrDefaultBits(keyBits, absentValueBits);
        long nextValue = remappingFunction.applyAsLong(keyBits, previous);
        replaceOrDefaultBits(keyBits, nextValue, absentValueBits);
        return nextValue;
    }

    public long computeBits(
            long keyBits, long absentValueBits, LongBinaryOperator remappingFunction) {
        Objects.requireNonNull(remappingFunction, "remappingFunction");
        long previous = getOrDefaultBits(keyBits, absentValueBits);
        long nextValue = remappingFunction.applyAsLong(keyBits, previous);
        putBits(keyBits, nextValue, absentValueBits);
        return nextValue;
    }

    public long mergeBits(
            long keyBits, long valueBits, LongBinaryOperator mergeFunction) {
        Objects.requireNonNull(mergeFunction, "mergeFunction");
        if (!containsKeyBits(keyBits)) {
            putBits(keyBits, valueBits, 0L);
            return valueBits;
        }
        long merged = mergeFunction.applyAsLong(
                getOrDefaultBits(keyBits, 0L), valueBits);
        replaceOrDefaultBits(keyBits, merged, 0L);
        return merged;
    }

    public void replaceAllBits(LongBinaryOperator function) {
        Objects.requireNonNull(function, "function");
        if (hasZeroKey) {
            zeroValueBits = valueKind.storageBits(
                    function.applyAsLong(0L, zeroValueBits));
        }
        int remaining = size - (hasZeroKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (!isZeroStored(key)) {
                long value = valueKind.readBits(values, i);
                valueKind.writeBits(values, i, function.applyAsLong(key, value));
                remaining--;
            }
        }
    }

    public void compact() {
        int ideal = DenseHash.capacityFor(size, loadFactor);
        if (ideal < capacity()) rehash(ideal);
    }

    public long removeOrDefaultBits(long keyBits, long defaultValueBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey) return defaultValueBits;
            long old = zeroValueBits;
            hasZeroKey = false;
            zeroValueBits = 0L;
            size--;
            return old;
        }
        int position = findNonZero(keyBits);
        if (position < 0) return defaultValueBits;
        long old = valueKind.readBits(values, position);
        shiftKeys(position);
        size--;
        return old;
    }

    public long minValueOrDefaultBits(long defaultBits) {
        if (size == 0) return defaultBits;
        boolean found = false;
        long best = 0L;
        if (hasZeroKey) {
            best = zeroValueBits;
            found = true;
        }
        for (int i = 0; i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (isZeroStored(key)) continue;
            long value = valueKind.readBits(values, i);
            if (!found || valueKind.compareBits(value, best) < 0) {
                best = value;
                found = true;
            }
        }
        return best;
    }

    public long maxValueOrDefaultBits(long defaultBits) {
        if (size == 0) return defaultBits;
        boolean found = false;
        long best = 0L;
        if (hasZeroKey) {
            best = zeroValueBits;
            found = true;
        }
        for (int i = 0; i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (isZeroStored(key)) continue;
            long value = valueKind.readBits(values, i);
            if (!found || valueKind.compareBits(value, best) > 0) {
                best = value;
                found = true;
            }
        }
        return best;
    }

    public void clear() {
        keyKind.clear(keys, 0, capacity());
        valueKind.clear(values, 0, capacity());
        size = 0;
        hasZeroKey = false;
        zeroValueBits = 0L;
    }

    public void forEach(EntryConsumer action) {
        Objects.requireNonNull(action, "action");
        if (hasZeroKey) action.accept(0L, zeroValueBits);
        int remaining = size - (hasZeroKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (!isZeroStored(key)) {
                action.accept(key, valueKind.readBits(values, i));
                remaining--;
            }
        }
    }

    Object keyArray() { return keys; }
    Object valueArray() { return values; }
    boolean hasZeroKey() { return hasZeroKey; }
    long zeroValueBits() { return zeroValueBits; }

    private int findNonZero(long keyBits) {
        int position = keyKind.hashBits(keyBits) & mask;
        long current;
        while (!isZeroStored(current = keyKind.readBits(keys, position))) {
            if (keyKind.equalBits(current, keyBits)) return position;
            position = (position + 1) & mask;
        }
        return -1;
    }

    private void shiftKeys(int position) {
        for (;;) {
            int last = position;
            position = (position + 1) & mask;
            long current;
            while (!isZeroStored(current = keyKind.readBits(keys, position))) {
                int slot = keyKind.hashBits(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (isZeroStored(current)) {
                keyKind.clear(keys, last, last + 1);
                valueKind.clear(values, last, last + 1);
                return;
            }
            keyKind.writeBits(keys, last, current);
            valueKind.writeBits(values, last, valueKind.readBits(values, position));
        }
    }

    private void rehash(int newCapacity) {
        Object oldKeys = keys;
        Object oldValues = values;
        int oldCapacity = capacity();
        keys = keyKind.newArray(newCapacity);
        values = valueKind.newArray(newCapacity);
        mask = newCapacity - 1;
        threshold = DenseHash.threshold(newCapacity, loadFactor);
        for (int i = 0; i < oldCapacity; i++) {
            long key = keyKind.readBits(oldKeys, i);
            if (isZeroStored(key)) continue;
            int position = keyKind.hashBits(key) & mask;
            while (!isZeroStored(keyKind.readBits(keys, position))) {
                position = (position + 1) & mask;
            }
            keyKind.writeBits(keys, position, key);
            valueKind.writeBits(values, position, valueKind.readBits(oldValues, i));
        }
    }

    private boolean isZeroKey(long bits) {
        return keyKind.normalizeBits(bits) == 0L;
    }

    private static boolean isZeroStored(long bits) {
        return bits == 0L;
    }
}
