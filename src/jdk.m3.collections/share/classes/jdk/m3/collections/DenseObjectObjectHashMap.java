/* *****************************************************************************
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
 * Flat Object-to-Object hash map with parallel reference lanes.
 *
 * <p>Null is the empty key sentinel. A real null key is carried by one
 * collection-level sidecar, eliminating the former occupancy byte per slot.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseObjectObjectHashMap.java}
 * blob {@code 9f03ef28e406719fe7451e1238d5a0bde2f03f35} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DenseObjectObjectHashMap<K, V> {
    private final float loadFactor;
    private Object[] keys;
    private Object[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNullKey;
    private V nullValue;

    public DenseObjectObjectHashMap() {
        this(16, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public DenseObjectObjectHashMap(int expected) {
        this(expected, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public DenseObjectObjectHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int capacity = DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[capacity];
        values = new Object[capacity];
        mask = capacity - 1;
        threshold = DenseHash.threshold(capacity, loadFactor);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public int referenceSlots() { return keys.length + values.length; }
    public long primitiveMetadataBytes() { return 0L; }

    public boolean containsKey(Object key) {
        return key == null ? hasNullKey : findNonNull(key) >= 0;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (key == null) return hasNullKey ? nullValue : null;
        int index = findNonNull(key);
        return index < 0 ? null : (V) values[index];
    }

    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null) return hasNullKey ? nullValue : defaultValue;
        int index = findNonNull(key);
        return index < 0 ? defaultValue : (V) values[index];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key == null) {
            V old = hasNullKey ? nullValue : null;
            if (!hasNullKey) {
                // Do not publish null membership/value before a potentially failing resize.
                if (size + 1 > threshold) rehash(grownCapacity());
                hasNullKey = true;
                size++;
            }
            nullValue = value;
            return old;
        }

        int index = hash(key) & mask;
        while (keys[index] != null) {
            if (Objects.equals(keys[index], key)) {
                V old = (V) values[index];
                values[index] = value;
                return old;
            }
            index = (index + 1) & mask;
        }
        if (size + 1 > threshold) {
            // Stage both arrays AND the pending insertion before publishing either.
            rehash(grownCapacity(), key, value);
            return null;
        }
        keys[index] = key;
        values[index] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
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
        shiftKeys(index);
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

    private int findNonNull(Object key) {
        int index = hash(key) & mask;
        while (keys[index] != null) {
            if (Objects.equals(keys[index], key)) return index;
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void shiftKeys(int index) {
        for (;;) {
            int last = index;
            index = (index + 1) & mask;
            Object current;
            while ((current = keys[index]) != null) {
                int slot = hash(current) & mask;
                if (last <= index ? last >= slot || slot > index
                        : last >= slot && slot > index) {
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
        rehash(newCapacity, null, null);
    }

    /** Private, failure-atomic array image construction; no per-entry objects. */
    private void rehash(int newCapacity, Object pendingKey, Object pendingValue) {
        Object[] nextKeys = new Object[newCapacity];
        Object[] nextValues = new Object[newCapacity];
        int nextMask = newCapacity - 1;
        int nextThreshold = DenseHash.threshold(newCapacity, loadFactor);
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (key == null) continue;
            int position = hash(key) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = key;
            nextValues[position] = values[i];
        }
        if (pendingKey != null) {
            // Preserve the existing post-grow hash probe, but before publication.
            int position = hash(pendingKey) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = pendingKey;
            nextValues[position] = pendingValue;
        }
        // All potentially throwing hashing and allocations have completed.
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = nextThreshold;
        if (pendingKey != null) size++;
    }

    private static int hash(Object key) {
        return DenseHash.mix(Objects.hashCode(key));
    }

    private int grownCapacity() {
        if (capacity() >= (1 << 30)) throw new IllegalStateException("collection too large");
        return capacity() << 1;
    }
}
