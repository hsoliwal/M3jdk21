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
import java.util.function.Consumer;

/**
 * Flat reference-identity set: == equality and System.identityHashCode hashing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseIdentityHashSet.java}
 * blob {@code 45a60f3e63eae41beaf47813eb2da2459d020b4c} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DenseIdentityHashSet<E> {
    private final float loadFactor;
    private Object[] keys;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNull;

    public DenseIdentityHashSet() {
        this(16, DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public DenseIdentityHashSet(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int capacity = DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[capacity];
        mask = capacity - 1;
        threshold = DenseHash.threshold(capacity, loadFactor);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int referenceSlots() { return keys.length; }
    public long primitiveMetadataBytes() { return 0L; }

    public boolean contains(E key) {
        return key == null ? hasNull : findNonNull(key) >= 0;
    }

    public boolean add(E key) {
        if (key == null) {
            if (hasNull) return false;
            // Complete any required growth before publishing null membership.
            if (size + 1 > threshold) rehash(grownCapacity());
            hasNull = true;
            size++;
            return true;
        }
        int position = insertionPositionNonNull(key);
        if (position < 0) return false;
        if (size + 1 > threshold) {
            // Include the pending reference in the private image. A failure
            // must not publish partially migrated identity slots.
            rehash(grownCapacity(), key);
            return true;
        }
        keys[position] = key;
        size++;
        return true;
    }

    public boolean remove(E key) {
        if (key == null) {
            if (!hasNull) return false;
            hasNull = false;
            size--;
            return true;
        }
        int position = findNonNull(key);
        if (position < 0) return false;
        shift(position);
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

    private int findNonNull(E key) {
        int position = hash(key) & mask;
        while (keys[position] != null) {
            if (keys[position] == key) return position;
            position = (position + 1) & mask;
        }
        return -1;
    }

    private int insertionPositionNonNull(E key) {
        int position = hash(key) & mask;
        while (keys[position] != null) {
            if (keys[position] == key) return -position - 1;
            position = (position + 1) & mask;
        }
        return position;
    }

    private void shift(int position) {
        for (;;) {
            int last = position;
            position = (position + 1) & mask;
            Object current;
            while ((current = keys[position]) != null) {
                int slot = hash(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) break;
                position = (position + 1) & mask;
            }
            if (current == null) {
                keys[last] = null;
                return;
            }
            keys[last] = current;
        }
    }

    private void rehash(int capacity) {
        rehash(capacity, null);
    }

    /** Build and publish one complete identity-set image; never change a live lane mid-migration. */
    private void rehash(int capacity, Object pendingKey) {
        Object[] nextKeys = new Object[capacity];
        int nextMask = capacity - 1;
        int nextThreshold = DenseHash.threshold(capacity, loadFactor);
        for (Object key : keys) {
            if (key == null) continue;
            int position = hash(key) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = key;
        }
        if (pendingKey != null) {
            int position = hash(pendingKey) & nextMask;
            while (nextKeys[position] != null) position = (position + 1) & nextMask;
            nextKeys[position] = pendingKey;
        }
        keys = nextKeys;
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
