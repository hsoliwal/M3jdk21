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
import java.util.function.Consumer;

/**
 * Flat open-addressed object-reference set with no per-entry node or occupancy objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseObjectHashSet.java}
 * blob {@code 885b01d85cb0aa782e80183908fc5fa309bedf6d} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3DenseObjectHashSet<E> {
    private final float loadFactor;
    private Object[] keys;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNull;

    public M3DenseObjectHashSet() {
        this(16, M3DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public M3DenseObjectHashSet(int expected) {
        this(expected, M3DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public M3DenseObjectHashSet(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int capacity = M3DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[capacity];
        mask = capacity - 1;
        threshold = M3DenseHash.threshold(capacity, loadFactor);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public int referenceSlots() { return keys.length; }
    public long primitiveMetadataBytes() { return 0L; }

    public boolean contains(Object key) {
        return key == null ? hasNull : findNonNull(key) >= 0;
    }

    public boolean add(E key) {
        if (key == null) {
            if (hasNull) return false;
            // Reserve a larger table before publishing the null member.
            // Failed allocation or user hashCode leaves size/null unchanged.
            if (size + 1 > threshold) rehash(grownCapacity());
            hasNull = true;
            size++;
            return true;
        }
        int position = insertionPositionNonNull(key);
        if (position < 0) return false;
        if (size + 1 > threshold) {
            // Stage the new member too: a late second hash/equals failure
            // must not publish a partially resized table.
            rehash(grownCapacity(), key);
            return true;
        }
        keys[position] = key;
        size++;
        return true;
    }

    public boolean remove(Object key) {
        if (key == null) {
            if (!hasNull) return false;
            hasNull = false;
            size--;
            return true;
        }
        int position = findNonNull(key);
        if (position < 0) return false;
        shiftKeys(position);
        size--;
        return true;
    }

    public void clear() {
        Arrays.fill(keys, null);
        hasNull = false;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasNull) consumer.accept(null);
        int remaining = size - (hasNull ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                consumer.accept((E) key);
                remaining--;
            }
        }
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        int out = 0;
        if (hasNull) result[out++] = null;
        for (Object key : keys) if (key != null) result[out++] = key;
        return result;
    }

    private int findNonNull(Object key) {
        int position = hash(key) & mask;
        while (keys[position] != null) {
            if (Objects.equals(keys[position], key)) return position;
            position = (position + 1) & mask;
        }
        return -1;
    }

    /** Returns insertion slot or -(existingSlot+1). */
    private int insertionPositionNonNull(Object key) {
        return insertionPositionNonNull(keys, mask, key);
    }

    /** Probe against either the live or still-unpublished staged image. */
    private static int insertionPositionNonNull(Object[] table, int tableMask, Object key) {
        int position = hash(key) & tableMask;
        while (table[position] != null) {
            if (Objects.equals(table[position], key)) return -position - 1;
            position = (position + 1) & tableMask;
        }
        return position;
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
                return;
            }
            keys[last] = current;
        }
    }

    private void rehash(int newCapacity) {
        rehash(newCapacity, null);
    }

    /** Construct then publish: no live field changes until every user hash/equals succeeds. */
    private void rehash(int newCapacity, Object pendingKey) {
        Object[] nextKeys = new Object[newCapacity];
        int nextMask = newCapacity - 1;
        int nextThreshold = M3DenseHash.threshold(newCapacity, loadFactor);
        for (Object key : keys) {
            if (key == null) continue;
            int position = hash(key) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = key;
        }
        if (pendingKey != null) {
            // Preserve the original post-growth hash/equals probe, including
            // its exception behavior, but keep the live table untouched.
            int position = insertionPositionNonNull(nextKeys, nextMask, pendingKey);
            nextKeys[position] = pendingKey;
        }
        // Publish the complete new image only after all potentially throwing work.
        keys = nextKeys;
        mask = nextMask;
        threshold = nextThreshold;
        if (pendingKey != null) size++;
    }

    private static int hash(Object key) {
        return M3DenseHash.mix(Objects.hashCode(key));
    }

    private int grownCapacity() {
        if (capacity() >= (1 << 30)) throw new IllegalStateException("collection too large");
        return capacity() << 1;
    }
}
