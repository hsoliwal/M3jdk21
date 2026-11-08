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
import java.util.function.BiConsumer;

/**
 * Flat identity-key object map.
 *
 * <p>Keys use == and System.identityHashCode; null is the empty-slot sentinel
 * with one collection-level null-key sidecar.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseIdentityHashMap.java}
 * blob {@code 69a1029547159e3bc7c4f39531283d03b0c2901d} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DenseIdentityHashMap<K, V> {
    private final float loadFactor;
    private Object[] keys;
    private Object[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNullKey;
    private V nullValue;

    public DenseIdentityHashMap() {
        this(16, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public DenseIdentityHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int capacity = DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[capacity];
        values = new Object[capacity];
        mask = capacity - 1;
        threshold = DenseHash.threshold(capacity, loadFactor);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int referenceSlots() { return keys.length + values.length; }
    public long primitiveMetadataBytes() { return 0L; }

    public boolean containsKey(K key) {
        return key == null ? hasNullKey : findNonNull(key) >= 0;
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        if (key == null) return hasNullKey ? nullValue : null;
        int index = findNonNull(key);
        return index < 0 ? null : (V) values[index];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key == null) {
            V old = hasNullKey ? nullValue : null;
            if (!hasNullKey) {
                // A failed growth must not publish null membership or its value.
                if (size + 1 > threshold) rehash(grownCapacity());
                hasNullKey = true;
                size++;
            }
            nullValue = value;
            return old;
        }

        int index = hash(key) & mask;
        while (keys[index] != null) {
            if (keys[index] == key) {
                V old = (V) values[index];
                values[index] = value;
                return old;
            }
            index = (index + 1) & mask;
        }
        if (size + 1 > threshold) {
            // Stage both reference lanes and the pending entry before publication.
            rehash(grownCapacity(), key, value);
            return null;
        }
        keys[index] = key;
        values[index] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(K key) {
        if (key == null) {
            if (!hasNullKey) return null;
            V old = nullValue;
            nullValue = null;
            hasNullKey = false;
            size--;
            return old;
        }
        int index = findNonNull(key);
        if (index < 0) return null;
        V old = (V) values[index];
        shift(index);
        size--;
        return old;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        hasNullKey = false;
        nullValue = null;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasNullKey) consumer.accept(null, nullValue);
        int remaining = size - (hasNullKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                consumer.accept((K) key, (V) values[i]);
                remaining--;
            }
        }
    }

    private int findNonNull(K key) {
        int index = hash(key) & mask;
        while (keys[index] != null) {
            if (keys[index] == key) return index;
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
                if (last <= index ? last >= slot || slot > index
                        : last >= slot && slot > index) break;
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

    private void rehash(int capacity) {
        rehash(capacity, null, null);
    }

    /** Build the complete identity-key/value image before modifying live owner state. */
    private void rehash(int capacity, Object pendingKey, Object pendingValue) {
        Object[] nextKeys = new Object[capacity];
        Object[] nextValues = new Object[capacity];
        int nextMask = capacity - 1;
        int nextThreshold = DenseHash.threshold(capacity, loadFactor);
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (key == null) continue;
            int position = hash(key) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = key;
            nextValues[position] = values[i];
        }
        if (pendingKey != null) {
            // Keep identity hashing and pending value reference in the private image.
            int position = hash(pendingKey) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = pendingKey;
            nextValues[position] = pendingValue;
        }
        // Publication is the final operation; no per-entry wrapper is allocated.
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = nextThreshold;
        if (pendingKey != null) size++;
    }

    private static int hash(Object key) {
        return DenseHash.mix(System.identityHashCode(key));
    }

    private int grownCapacity() {
        if (keys.length >= (1 << 30)) throw new IllegalStateException("collection too large");
        return keys.length << 1;
    }
}
