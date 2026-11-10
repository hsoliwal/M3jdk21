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

/**
 * Object-key to exact-width primitive-value map with parallel lanes and no per-mapping wrapper.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ObjectPrimitiveHashMap.java}
 * blob {@code f1b7707194dccd7c29b82f96478cb5b39b37bec6} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class ObjectPrimitiveHashMap<K> {
    @FunctionalInterface
    public interface EntryConsumer<K> {
        void accept(K key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final float loadFactor;
    private Object[] keys;
    private Object values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNullKey;
    private long nullValueBits;

    public ObjectPrimitiveHashMap(PrimitiveKind valueKind) {
        this(valueKind, 16, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public ObjectPrimitiveHashMap(PrimitiveKind valueKind, int expected, float loadFactor) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        this.loadFactor = loadFactor;
        int capacity = DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[capacity];
        values = valueKind.newArray(capacity);
        mask = capacity - 1;
        threshold = DenseHash.threshold(capacity, loadFactor);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public long primitivePayloadBytes() {
        return valueKind.payloadBytes(capacity());
    }
    public int referenceSlots() { return keys.length; }

    public boolean containsKey(Object key) {
        return key == null ? hasNullKey : findNonNull(key) >= 0;
    }

    public long getOrDefaultBits(Object key, long defaultValueBits) {
        if (key == null) return hasNullKey ? nullValueBits : defaultValueBits;
        int position = findNonNull(key);
        return position < 0 ? defaultValueBits : valueKind.readBits(values, position);
    }

    /** Returns previous value carrier, or absentValueBits if the key was absent. */
    public long putBits(K key, long valueBits, long absentValueBits) {
        if (key == null) {
            long old = hasNullKey ? nullValueBits : absentValueBits;
            if (!hasNullKey) {
                // Grow before publishing the null-key sidecar or its value.
                if (size + 1 > threshold) rehash(grownCapacity());
                hasNullKey = true;
                size++;
            }
            nullValueBits = valueKind.storageBits(valueBits);
            return old;
        }

        int position = hash(key) & mask;
        while (keys[position] != null) {
            if (Objects.equals(keys[position], key)) {
                long old = valueKind.readBits(values, position);
                valueKind.writeBits(values, position, valueBits);
                return old;
            }
            position = (position + 1) & mask;
        }
        if (size + 1 > threshold) {
            // Stage the pending key and exact-width value in the unpublished image.
            // A second Java hashCode() failure cannot leave visible partial growth.
            rehash(grownCapacity(), key, valueBits);
            return absentValueBits;
        }
        keys[position] = key;
        valueKind.writeBits(values, position, valueBits);
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(Object key, long defaultValueBits) {
        if (key == null) {
            if (!hasNullKey) return defaultValueBits;
            long old = nullValueBits;
            nullValueBits = 0L;
            hasNullKey = false;
            size--;
            return old;
        }
        int position = findNonNull(key);
        if (position < 0) return defaultValueBits;
        long old = valueKind.readBits(values, position);
        shiftKeys(position);
        size--;
        return old;
    }

    public void clear() {
        Arrays.fill(keys, null);
        valueKind.clear(values, 0, capacity());
        hasNullKey = false;
        nullValueBits = 0L;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(EntryConsumer<? super K> action) {
        Objects.requireNonNull(action, "action");
        if (hasNullKey) action.accept(null, nullValueBits);
        int remaining = size - (hasNullKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                action.accept((K) key, valueKind.readBits(values, i));
                remaining--;
            }
        }
    }

    Object[] keyArray() { return keys; }
    Object valueArray() { return values; }
    boolean hasNullKey() { return hasNullKey; }
    long nullValueBits() { return nullValueBits; }

    private int findNonNull(Object key) {
        int position = hash(key) & mask;
        while (keys[position] != null) {
            if (Objects.equals(keys[position], key)) return position;
            position = (position + 1) & mask;
        }
        return -1;
    }

    private void shiftKeys(int position) {
        for (;;) {
            int last = position;
            position = (position + 1) & mask;
            Object current;
            while ((current = keys[position]) != null) {
                int slot = hash(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (current == null) {
                keys[last] = null;
                valueKind.clear(values, last, last + 1);
                return;
            }
            keys[last] = current;
            valueKind.writeBits(values, last, valueKind.readBits(values, position));
        }
    }

    private void rehash(int newCapacity) {
        rehash(newCapacity, null, 0L);
    }

    /** Build all eight possible primitive lanes privately before publishing a new map image. */
    private void rehash(int newCapacity, Object pendingKey, long pendingBits) {
        Object[] nextKeys = new Object[newCapacity];
        Object nextValues = valueKind.newArray(newCapacity);
        int nextMask = newCapacity - 1;
        int nextThreshold = DenseHash.threshold(newCapacity, loadFactor);
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (key == null) continue;
            int position = hash(key) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = key;
            valueKind.writeBits(nextValues, position, valueKind.readBits(values, i));
        }
        if (pendingKey != null) {
            // Preserve the original post-growth second hash callback, but precommit.
            int position = hash(pendingKey) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = pendingKey;
            valueKind.writeBits(nextValues, position, pendingBits);
        }
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = nextThreshold;
        if (pendingKey != null) size++;
    }

    private int hash(Object key) {
        return DenseHash.mix(Objects.hashCode(key));
    }

    private int grownCapacity() {
        if (capacity() >= (1 << 30)) throw new IllegalStateException("collection too large");
        return capacity() << 1;
    }
}
