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

import java.util.function.BiConsumer;
import jdk.internal.m3.collections.range.SegmentedIntLane28;

/**
 * Stable segmented payload slots indexed by open-addressed primitive slot IDs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SegmentedHashKeyStore.java}
 * blob {@code 9d91c542113e8d47ca0e83193cff26bb2aedeada} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SegmentedHashKeyStore<K, V> extends M3KeyStore<K, V> {
    private static final int[] EMPTY = new int[0];
    private int[] index = EMPTY;
    private SegmentedObjectLane28<K> keys;
    private SegmentedObjectLane28<V> values;
    private SegmentedIntLane28 hashes;
    private SegmentedIntLane28 freeNext;
    private int highWater;
    private int freeHead;
    private int used;
    SegmentedHashKeyStore(CollectionRequirements required, boolean values) { super(required, values); }
    @Override int find(K key, int hash) {
        if (index.length == 0) { return -1; }
        int mask = index.length - 1;
        for (int p = hash & mask; index[p] != 0; p = (p + 1) & mask) {
            int slot = index[p] - 1;
            if (slot >= 0 && hashes.get(slot) == hash && equivalent(key, keys.get(slot))) { return p; }
        }
        return -1;
    }
    @Override V value(int at) { return withValues ? values.get(index[at] - 1) : null; }
    @Override void value(int at, V value) { values.set(index[at] - 1, value); }
    private static int threshold(int capacity) { return capacity - capacity / 3; }
    @Override void insert(K key, V value, int hash) {
        int capacity = index.length;
        if (capacity == 0) { rehash(8); }
        else if (size + 1 > threshold(capacity)) { rehash(Math.multiplyExact(capacity, 2)); }
        else if (used + 1 > threshold(capacity)) { rehash(capacity); }
        if (keys == null) {
            keys = new SegmentedObjectLane28<>();
            if (withValues) { values = new SegmentedObjectLane28<>(); }
            hashes = new SegmentedIntLane28(); freeNext = new SegmentedIntLane28();
        }
        int slot = freeHead == 0 ? highWater : freeHead - 1;
        keys.set(slot, key);
        if (withValues) { values.set(slot, value); }
        hashes.set(slot, hash);
        if (freeHead == 0) { highWater++; }
        else { freeHead = freeNext.get(slot); freeNext.clear(slot); }
        int mask = index.length - 1;
        int p = hash & mask;
        while (index[p] > 0) { p = (p + 1) & mask; }
        if (index[p] == 0) { used++; }
        index[p] = slot + 1; size++;
    }
    private void rehash(int capacity) {
        int[] next = new int[capacity];
        int mask = capacity - 1;
        for (int encoded : index) {
            if (encoded <= 0) { continue; }
            int p = hashes.get(encoded - 1) & mask;
            while (next[p] != 0) { p = (p + 1) & mask; }
            next[p] = encoded;
        }
        index = next; used = size;
    }
    @Override void erase(int at) {
        int slot = index[at] - 1;
        freeNext.set(slot, freeHead);
        keys.set(slot, null);
        if (withValues) { values.set(slot, null); }
        hashes.clear(slot);
        freeHead = slot + 1; index[at] = -1; size--;
    }
    @Override void walk(BiConsumer<? super K, ? super V> action) {
        for (int encoded : index) {
            if (encoded > 0) { action.accept(keys.get(encoded - 1), withValues ? values.get(encoded - 1) : null); }
        }
    }
    @Override void reset() {
        index = EMPTY; keys = null; values = null; hashes = null; freeNext = null;
        highWater = 0; freeHead = 0; used = 0; size = 0;
    }
    @Override ShapeStorage storage() {
        long refs = keys == null ? 0 : keys.referenceSlots() + (withValues ? values.referenceSlots() : 0);
        long primitive = (long) index.length * Integer.BYTES;
        int blocks = 0;
        if (keys != null) {
            primitive += hashes.payloadBytes() + freeNext.payloadBytes();
            blocks = keys.blocks() + (withValues ? values.blocks() : 0)
                    + hashes.allocatedBlockCount() + freeNext.allocatedBlockCount();
        }
        return new ShapeStorage(refs, primitive, blocks, index.length);
    }
}
