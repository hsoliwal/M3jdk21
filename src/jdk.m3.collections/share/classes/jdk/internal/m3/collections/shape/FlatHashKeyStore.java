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
package jdk.internal.m3.collections.shape;

import java.util.function.BiConsumer;

/**
 * Lazy open-addressed parallel arrays, cached hashes and tombstones; no collision bucket objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/FlatHashKeyStore.java}
 * blob {@code 5bb2917290b83829c33a86ebea45e52bdcf58ac0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class FlatHashKeyStore<K, V> extends M3KeyStore<K, V> {
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final int[] EMPTY_INTS = new int[0];
    private Object[] keys = EMPTY_OBJECTS;
    private Object[] values = EMPTY_OBJECTS;
    private byte[] states = EMPTY_BYTES;
    private int[] hashes = EMPTY_INTS;
    private int used;
    FlatHashKeyStore(CollectionRequirements required, boolean values) { super(required, values); }
    @Override int find(K key, int hash) {
        if (keys.length == 0) { return -1; }
        int mask = keys.length - 1;
        for (int p = hash & mask; states[p] != 0; p = (p + 1) & mask) {
            if (states[p] == 1 && hashes[p] == hash && equivalent(key, keys[p])) { return p; }
        }
        return -1;
    }
    @Override V value(int at) { return withValues ? read(values, at) : null; }
    @Override void value(int at, V value) { values[at] = value; }
    private static int threshold(int capacity) { return capacity - capacity / 3; }
    @Override void insert(K key, V value, int hash) {
        int capacity = keys.length;
        if (capacity == 0) { rehash(8); }
        else if (size + 1 > threshold(capacity)) { rehash(Math.multiplyExact(capacity, 2)); }
        else if (used + 1 > threshold(capacity)) { rehash(capacity); }
        int mask = keys.length - 1;
        int p = hash & mask;
        while (states[p] == 1) { p = (p + 1) & mask; }
        if (states[p] == 0) { used++; }
        keys[p] = key;
        if (withValues) { values[p] = value; }
        hashes[p] = hash; states[p] = 1; size++;
    }
    private void rehash(int capacity) {
        Object[] newKeys = new Object[capacity];
        Object[] newValues = withValues ? new Object[capacity] : EMPTY_OBJECTS;
        byte[] newStates = new byte[capacity];
        int[] newHashes = new int[capacity];
        int mask = capacity - 1;
        for (int i = 0; i < keys.length; i++) {
            if (states[i] != 1) { continue; }
            int p = hashes[i] & mask;
            while (newStates[p] != 0) { p = (p + 1) & mask; }
            newKeys[p] = keys[i];
            if (withValues) { newValues[p] = values[i]; }
            newHashes[p] = hashes[i]; newStates[p] = 1;
        }
        keys = newKeys; values = newValues; states = newStates; hashes = newHashes; used = size;
    }
    @Override void erase(int at) {
        keys[at] = null;
        if (withValues) { values[at] = null; }
        hashes[at] = 0; states[at] = 2; size--;
    }
    @Override void walk(BiConsumer<? super K, ? super V> action) {
        for (int i = 0; i < keys.length; i++) {
            if (states[i] == 1) { action.accept(read(keys, i), value(i)); }
        }
    }
    @Override void reset() {
        keys = EMPTY_OBJECTS; values = EMPTY_OBJECTS; states = EMPTY_BYTES; hashes = EMPTY_INTS;
        size = 0; used = 0;
    }
    @Override ShapeStorage storage() {
        return new ShapeStorage((long) keys.length + values.length,
                (long) states.length + (long) hashes.length * Integer.BYTES,
                keys.length == 0 ? 0 : (withValues ? 4 : 3), keys.length);
    }
}
