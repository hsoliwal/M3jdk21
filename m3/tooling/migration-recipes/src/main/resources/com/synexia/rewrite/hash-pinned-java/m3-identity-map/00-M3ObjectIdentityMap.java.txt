/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Flat reference-identity map with parallel key and value reference lanes and no entry objects.
 *
 * <p>Object-space owner for the {@link java.util.IdentityHashMap} shape: keys compare with
 * {@code ==} and hash with {@link System#identityHashCode}; {@code null} is the empty-slot marker
 * and a present {@code null} key lives in one collection-level sidecar, so {@code null} keys and
 * {@code null} values are both legal as in the JDK. Removal back-shifts the probe run instead of
 * leaving tombstones. {@link #forEach} is fail-fast after the traversal, as the JDK's is. Adapted
 * from the first-party Synexia donor {@code com.synexia.primitives.DenseIdentityHashMap}. This is
 * an M3 proving-ground backend; {@link java.util.IdentityHashMap} remains the public owner.</p>
 */
public final class M3ObjectIdentityMap<K, V> {
    private static final int MIN_CAPACITY = 8;
    private static final float LOAD_FACTOR = 0.65f;

    private Object[] keys;
    private Object[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNullKey;
    private V nullValue;
    private int modCount;

    public M3ObjectIdentityMap() {
        this(0);
    }

    public M3ObjectIdentityMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new Object[capacity];
        values = new Object[capacity];
        mask = capacity - 1;
        threshold = thresholdFor(capacity);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return keys.length;
    }

    /** Retained reference slots: key lane plus value lane (the null-key sidecar is one field). */
    public int referenceSlots() {
        return keys.length + values.length;
    }

    public boolean containsKey(Object key) {
        return key == null ? hasNullKey : find(key) >= 0;
    }

    /** Identity comparison of values, as {@link java.util.IdentityHashMap#containsValue}. */
    public boolean containsValue(Object value) {
        if (hasNullKey && nullValue == value) {
            return true;
        }
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && values[i] == value) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (key == null) {
            return hasNullKey ? nullValue : null;
        }
        int index = find(key);
        return index < 0 ? null : (V) values[index];
    }

    /** Returns {@code defaultValue} only when the key is absent, as {@link java.util.Map}. */
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null) {
            return hasNullKey ? nullValue : defaultValue;
        }
        int index = find(key);
        return index < 0 ? defaultValue : (V) values[index];
    }

    /** Associates {@code value}; returns the previous value, or {@code null} if none. */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key == null) {
            V previous = hasNullKey ? nullValue : null;
            if (!hasNullKey) {
                hasNullKey = true;
                size++;
                modCount++;
                if (size > threshold) {
                    rehash(keys.length << 1);
                }
            }
            nullValue = value;
            return previous;
        }
        int index = hash(key) & mask;
        Object current;
        while ((current = keys[index]) != null) {
            if (current == key) {
                V previous = (V) values[index];
                values[index] = value;
                return previous;
            }
            index = (index + 1) & mask;
        }
        keys[index] = key;
        values[index] = value;
        size++;
        modCount++;
        if (size > threshold) {
            rehash(keys.length << 1);
        }
        return null;
    }

    /** {@link java.util.Map#putIfAbsent} semantics: a {@code null}-valued mapping is replaced. */
    public V putIfAbsent(K key, V value) {
        V existing = get(key);
        return existing == null ? put(key, value) : existing;
    }

    /** Removes the mapping; returns the previous value, or {@code null} if none. */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (key == null) {
            if (!hasNullKey) {
                return null;
            }
            V previous = nullValue;
            nullValue = null;
            hasNullKey = false;
            size--;
            modCount++;
            return previous;
        }
        int index = find(key);
        if (index < 0) {
            return null;
        }
        V previous = (V) values[index];
        shift(index);
        size--;
        modCount++;
        return previous;
    }

    /** Removes only when the key maps (by identity) to {@code value}, as the JDK does. */
    public boolean remove(Object key, Object value) {
        if (!containsKey(key) || get(key) != value) {
            return false;
        }
        remove(key);
        return true;
    }

    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        hasNullKey = false;
        nullValue = null;
        size = 0;
        modCount++;
    }

    /**
     * Visits mappings (the {@code null} key first, then slot order). A structural modification
     * during the traversal is reported with {@link ConcurrentModificationException} afterwards.
     */
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedModCount = modCount;
        if (hasNullKey) {
            consumer.accept(null, nullValue);
        }
        int remaining = size - (hasNullKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                consumer.accept((K) key, (V) values[i]);
                remaining--;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /** Snapshot of the keys in traversal order ({@code null} first when present). */
    public Object[] keysToArray() {
        Object[] result = new Object[size];
        int cursor = 0;
        if (hasNullKey) {
            result[cursor++] = null;
        }
        for (Object key : keys) {
            if (key != null) {
                result[cursor++] = key;
            }
        }
        return result;
    }

    private int find(Object key) {
        int index = hash(key) & mask;
        Object current;
        while ((current = keys[index]) != null) {
            if (current == key) {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void shift(int index) {
        for (;;) {
            int last = index;
            index = (index + 1) & mask;
            Object current;
            while ((current = keys[index]) != null) {
                int slot = hash(current) & mask;
                if (last <= index ? last >= slot || slot > index : last >= slot && slot > index) {
                    break;
                }
                index = (index + 1) & mask;
            }
            if (current == null) {
                keys[last] = null;
                values[last] = null;
                return;
            }
            keys[last] = current;
            values[last] = values[index];
        }
    }

    private void rehash(int newCapacity) {
        if (keys.length >= (1 << 30)) {
            throw new IllegalStateException("maximum hash-table capacity reached");
        }
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        keys = new Object[newCapacity];
        values = new Object[newCapacity];
        mask = newCapacity - 1;
        threshold = thresholdFor(newCapacity);
        for (int i = 0; i < oldKeys.length; i++) {
            Object key = oldKeys[i];
            if (key != null) {
                int index = hash(key) & mask;
                while (keys[index] != null) {
                    index = (index + 1) & mask;
                }
                keys[index] = key;
                values[index] = oldValues[i];
            }
        }
    }

    private static int capacityFor(int expectedSize) {
        long needed = Math.max(MIN_CAPACITY, (long) Math.ceil(expectedSize / (double) LOAD_FACTOR));
        if (needed > (1L << 30)) {
            throw new IllegalArgumentException("expectedSize too large");
        }
        int capacity = 1;
        while (capacity < needed) {
            capacity <<= 1;
        }
        return capacity;
    }

    private static int thresholdFor(int capacity) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * LOAD_FACTOR)));
    }

    private static int hash(Object key) {
        int x = System.identityHashCode(key);
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }
}
