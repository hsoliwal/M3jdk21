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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;

/** Mutable one-to-one long relation with parallel lanes and two primitive row indexes.
 * Expected O(1) lookup in either direction, insertion and swap-delete. Traversal order is
 * unspecified; deletion may move the final row. All long values, including zero and extrema,
 * are valid. put rejects duplicate values without changing the relation; forcePut evicts
 * a conflicting value owner explicitly. Not thread-safe; callbacks cannot mutate this map.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongBiMap.java}
 * blob {@code 770551cf71aa4f08891a9749f31214beb9e3518d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongBiMap {
    private long[] keys;
    private long[] values;
    private final PackedLongRowIndex byKey;
    private final PackedLongRowIndex byValue;
    private int size;
    private int callbacks;

    public PackedLongLongBiMap() { this(8); }
    public PackedLongLongBiMap(int expectedSize) {
        byKey = new PackedLongRowIndex(expectedSize);
        byValue = new PackedLongRowIndex(expectedSize);
        keys = new long[Math.max(1, expectedSize)]; values = new long[keys.length];
    }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private void reserve(int required) {
        byKey.ensure(required, keys); byValue.ensure(required, values);
        if (required <= keys.length) return;
        int n = PackedLongRowIndex.grownLength(keys.length, required);
        long[] k = Arrays.copyOf(keys, n), v = Arrays.copyOf(values, n);
        keys = k; values = v;
    }
    public boolean put(long key, long value) {
        writable();
        int k = byKey.find(key, keys), v = byValue.find(value, values);
        if (v >= 0 && v != k) throw new IllegalArgumentException("value already has an owner");
        if (k >= 0) {
            if (values[k] != value) {
                byValue.remove(values[k], values); values[k] = value; byValue.insert(k, values);
            }
            return false;
        }
        reserve(Math.addExact(size, 1));
        keys[size] = key; values[size] = value;
        byKey.insert(size, keys); byValue.insert(size, values); size++;
        return true;
    }
    /** Returns whether key was absent before the operation. */
    public boolean forcePut(long key, long value) {
        writable();
        int k = byKey.find(key, keys), v = byValue.find(value, values);
        boolean inserted = k < 0;
        if (k < 0 && v < 0) reserve(Math.addExact(size, 1));
        if (v >= 0 && v != k) removeRow(v);
        put(key, value);
        return inserted;
    }
    public boolean containsKey(long key) { return byKey.find(key, keys) >= 0; }
    public boolean containsValue(long value) { return byValue.find(value, values) >= 0; }
    public long getOrDefault(long key, long fallback) {
        int row = byKey.find(key, keys); return row < 0 ? fallback : values[row];
    }
    public long keyOrDefault(long value, long fallback) {
        int row = byValue.find(value, values); return row < 0 ? fallback : keys[row];
    }
    public boolean removeKey(long key) {
        writable(); int row = byKey.find(key, keys);
        if (row < 0) return false;
        removeRow(row); return true;
    }
    public boolean removeValue(long value) {
        writable(); int row = byValue.find(value, values);
        if (row < 0) return false;
        removeRow(row); return true;
    }
    private void removeRow(int row) {
        byKey.remove(keys[row], keys); byValue.remove(values[row], values);
        int tail = --size;
        if (row != tail) {
            byKey.relocate(tail, row, keys); byValue.relocate(tail, row, values);
            keys[row] = keys[tail]; values[row] = values[tail];
        }
    }
    public int filterInPlace(LongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] rejected = new long[(int) ((size + 63L) >>> 6)]; int count = 0;
        callbacks++;
        try {
            for (int i = 0; i < size; i++) if (!predicate.test(keys[i], values[i])) {
                rejected[i >>> 6] |= 1L << (i & 63); count++;
            }
        } finally { callbacks--; }
        // Descending deletion preserves the original row decisions despite swap-delete.
        for (int i = size - 1; i >= 0; i--)
            if ((rejected[i >>> 6] & (1L << (i & 63))) != 0) removeRow(i);
        return count;
    }
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) consumer.accept(keys[i], values[i]); }
        finally { callbacks--; }
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public void clear() { writable(); byKey.clear(); byValue.clear(); size = 0; }
    public long[] keysToArray() { return Arrays.copyOf(keys, size); }
    public long[] valuesToArray() { return Arrays.copyOf(values, size); }
    public long arrayPayloadBytes() {
        return 16L * keys.length + byKey.payloadBytes() + byValue.payloadBytes();
    }
    /** Live reversed facade, one object per requested view, never per mapping. */
    public Inverse inverse() { return new Inverse(); }
    public final class Inverse {
        private Inverse() { }
        public boolean put(long value, long key) {
            int owner = byValue.find(value, values), target = byKey.find(key, keys);
            writable();
            if (target >= 0 && target != owner)
                throw new IllegalArgumentException("inverse value already has an owner");
            if (owner >= 0) {
                if (keys[owner] != key) {
                    byKey.remove(keys[owner], keys); keys[owner] = key; byKey.insert(owner, keys);
                }
                return false;
            }
            PackedLongLongBiMap.this.put(key, value); return true;
        }
        public long getOrDefault(long value, long fallback) { return keyOrDefault(value, fallback); }
        public boolean containsKey(long value) { return containsValue(value); }
        public boolean remove(long value) { return removeValue(value); }
        public int size() { return PackedLongLongBiMap.this.size(); }
        public PackedLongLongBiMap inverse() { return PackedLongLongBiMap.this; }
    }
}
