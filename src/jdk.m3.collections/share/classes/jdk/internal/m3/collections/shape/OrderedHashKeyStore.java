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
package jdk.internal.m3.collections.shape;

import java.util.Arrays;
import java.util.function.BiConsumer;
import jdk.internal.m3.collections.packed.CollectionHashes;

/**
 * Stable payload slots, primitive links and cached-hash index; insertion-order traversal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/OrderedHashKeyStore.java}
 * blob {@code 786554f78dc34d3438a6bcf563395620961dd68c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class OrderedHashKeyStore<K, V> extends M3KeyStore<K, V> {
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private Object[] keys = EMPTY_OBJECTS;
    private Object[] values = EMPTY_OBJECTS;
    private int[] hashes = EMPTY_INTS;
    private int[] previous = EMPTY_INTS;
    private int[] next = EMPTY_INTS;
    private int[] index = EMPTY_INTS;
    private int highWater;
    private int used;
    private int free = -1;
    private int first = -1;
    private int last = -1;

    OrderedHashKeyStore(CollectionRequirements required, boolean values) { super(required, values); }
    @Override int find(K key, int hash) {
        if (index.length == 0) { return -1; }
        int mask = index.length - 1;
        for (int at = CollectionHashes.mix(hash) & mask; index[at] != 0; at = (at + 1) & mask) {
            int row = index[at] - 1;
            if (row >= 0 && hashes[row] == hash && equivalent(key, keys[row])) { return at; }
        }
        return -1;
    }
    @Override V value(int at) { return withValues ? read(values, index[at] - 1) : null; }
    @Override void value(int at, V value) { values[index[at] - 1] = value; }
    private static int threshold(int capacity) { return capacity - capacity / 3; }
    private int vacant(int hash) {
        int at = CollectionHashes.mix(hash) & (index.length - 1);
        while (index[at] > 0) { at = (at + 1) & (index.length - 1); }
        return at;
    }
    @Override void insert(K key, V value, int hash) {
        int row = free < 0 ? highWater : free;
        if (row == keys.length) {
            int capacity = grown(keys.length, row + 1, required.maximumSize());
            Object[] newKeys = Arrays.copyOf(keys, capacity);
            Object[] newValues = withValues ? Arrays.copyOf(values, capacity) : EMPTY_OBJECTS;
            int[] newHashes = Arrays.copyOf(hashes, capacity);
            int[] newPrevious = Arrays.copyOf(previous, capacity);
            int[] newNext = Arrays.copyOf(next, capacity);
            keys = newKeys; values = newValues; hashes = newHashes; previous = newPrevious; next = newNext;
        }
        if (index.length == 0) { rehash(8); }
        int at = vacant(hash);
        if (index[at] == 0 && used >= threshold(index.length)) {
            rehash(size + 1 > threshold(index.length) ? Math.multiplyExact(index.length, 2) : index.length);
            at = vacant(hash);
        }
        if (free < 0) { highWater++; } else { free = next[row]; }
        keys[row] = key;
        if (withValues) { values[row] = value; }
        hashes[row] = hash;
        previous[row] = last; next[row] = -1;
        if (last < 0) { first = row; } else { next[last] = row; }
        last = row;
        if (index[at] == 0) { used++; }
        index[at] = row + 1; size++;
    }
    private void rehash(int capacity) {
        int[] replacement = new int[capacity];
        int mask = capacity - 1;
        for (int row = first; row >= 0; row = next[row]) {
            int at = CollectionHashes.mix(hashes[row]) & mask;
            while (replacement[at] != 0) { at = (at + 1) & mask; }
            replacement[at] = row + 1;
        }
        index = replacement; used = size;
    }
    @Override void erase(int at) {
        int row = index[at] - 1;
        int before = previous[row], after = next[row];
        if (before < 0) { first = after; } else { next[before] = after; }
        if (after < 0) { last = before; } else { previous[after] = before; }
        keys[row] = null;
        if (withValues) { values[row] = null; }
        hashes[row] = 0; previous[row] = -1;
        next[row] = free; free = row;
        index[at] = -1; size--;
    }
    @Override void walk(BiConsumer<? super K, ? super V> action) {
        for (int row = first; row >= 0; row = next[row]) {
            action.accept(read(keys, row), withValues ? read(values, row) : null);
        }
    }
    @Override void reset() {
        keys = EMPTY_OBJECTS; values = EMPTY_OBJECTS;
        hashes = EMPTY_INTS; previous = EMPTY_INTS; next = EMPTY_INTS; index = EMPTY_INTS;
        highWater = 0; used = 0; size = 0; free = -1; first = -1; last = -1;
    }
    @Override ShapeStorage storage() {
        return new ShapeStorage((long) keys.length + values.length,
                4L * (hashes.length + (long) previous.length + next.length + index.length),
                keys.length == 0 ? 0 : withValues ? 6 : 5, index.length);
    }
}
