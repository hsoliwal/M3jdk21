/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact-width primitive-key to object-value map with no per-mapping wrapper object.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveObjectHashMap.java}
 * blob {@code 4555eabce0d1e779a39f1f544acd6341778ec13e} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveObjectHashMap<V> {
    @FunctionalInterface
    public interface EntryConsumer<V> {
        void accept(long keyBits, V value);
    }

    private final PrimitiveKind keyKind;
    private final float loadFactor;
    private Object keys;
    private Object[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZeroKey;
    private V zeroValue;

    public M3PrimitiveObjectHashMap(PrimitiveKind keyKind) {
        this(keyKind, 16, M3DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public M3PrimitiveObjectHashMap(PrimitiveKind keyKind, int expected, float loadFactor) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.loadFactor = loadFactor;
        int capacity = M3DenseHash.capacityFor(expected, loadFactor);
        keys = keyKind.newArray(capacity);
        values = new Object[capacity];
        mask = capacity - 1;
        threshold = M3DenseHash.threshold(capacity, loadFactor);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return values.length; }
    public long primitivePayloadBytes() {
        return keyKind.payloadBytes(capacity());
    }
    public int referenceSlots() { return values.length; }

    public boolean containsKeyBits(long keyBits) {
        return isZeroKey(keyBits) ? hasZeroKey : findNonZero(keyBits) >= 0;
    }

    @SuppressWarnings("unchecked")
    public V getOrDefault(long keyBits, V defaultValue) {
        if (isZeroKey(keyBits)) return hasZeroKey ? zeroValue : defaultValue;
        int position = findNonZero(keyBits);
        return position < 0 ? defaultValue : (V) values[position];
    }

    @SuppressWarnings("unchecked")
    public V put(long keyBits, V value, V absentValue) {
        if (isZeroKey(keyBits)) {
            V old = hasZeroKey ? zeroValue : absentValue;
            if (!hasZeroKey) {
                hasZeroKey = true;
                size++;
                if (size > threshold) rehash(grownCapacity());
            }
            zeroValue = value;
            return old;
        }

        int position = keyKind.hashBits(keyBits) & mask;
        long current;
        while ((current = keyKind.readBits(keys, position)) != 0L) {
            if (keyKind.equalBits(current, keyBits)) {
                V old = (V) values[position];
                values[position] = value;
                return old;
            }
            position = (position + 1) & mask;
        }
        if (size + 1 > threshold) {
            rehash(grownCapacity());
            position = keyKind.hashBits(keyBits) & mask;
            while (keyKind.readBits(keys, position) != 0L) position = (position + 1) & mask;
        }
        keyKind.writeBits(keys, position, keyBits);
        values[position] = value;
        size++;
        return absentValue;
    }

    @SuppressWarnings("unchecked")
    public V removeOrDefault(long keyBits, V defaultValue) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey) return defaultValue;
            V old = zeroValue;
            zeroValue = null;
            hasZeroKey = false;
            size--;
            return old;
        }
        int position = findNonZero(keyBits);
        if (position < 0) return defaultValue;
        V old = (V) values[position];
        shiftKeys(position);
        size--;
        return old;
    }

    public void clear() {
        keyKind.clear(keys, 0, capacity());
        Arrays.fill(values, null);
        zeroValue = null;
        hasZeroKey = false;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(EntryConsumer<? super V> action) {
        Objects.requireNonNull(action, "action");
        if (hasZeroKey) action.accept(0L, zeroValue);
        int remaining = size - (hasZeroKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < capacity(); i++) {
            long key = keyKind.readBits(keys, i);
            if (key != 0L) {
                action.accept(key, (V) values[i]);
                remaining--;
            }
        }
    }

    Object keyArray() { return keys; }
    Object[] valueArray() { return values; }
    boolean hasZeroKey() { return hasZeroKey; }
    V zeroValue() { return zeroValue; }

    private int findNonZero(long keyBits) {
        int position = keyKind.hashBits(keyBits) & mask;
        long current;
        while ((current = keyKind.readBits(keys, position)) != 0L) {
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
            while ((current = keyKind.readBits(keys, position)) != 0L) {
                int slot = keyKind.hashBits(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (current == 0L) {
                keyKind.clear(keys, last, last + 1);
                values[last] = null;
                return;
            }
            keyKind.writeBits(keys, last, current);
            values[last] = values[position];
        }
    }

    private void rehash(int newCapacity) {
        Object oldKeys = keys;
        Object[] oldValues = values;
        int oldCapacity = capacity();
        keys = keyKind.newArray(newCapacity);
        values = new Object[newCapacity];
        mask = newCapacity - 1;
        threshold = M3DenseHash.threshold(newCapacity, loadFactor);
        for (int i = 0; i < oldCapacity; i++) {
            long key = keyKind.readBits(oldKeys, i);
            if (key == 0L) continue;
            int position = keyKind.hashBits(key) & mask;
            while (keyKind.readBits(keys, position) != 0L) position = (position + 1) & mask;
            keyKind.writeBits(keys, position, key);
            values[position] = oldValues[i];
        }
    }

    private boolean isZeroKey(long bits) {
        return keyKind.normalizeBits(bits) == 0L;
    }

    private int grownCapacity() {
        if (capacity() >= (1 << 30)) throw new IllegalStateException("collection too large");
        return capacity() << 1;
    }
}
