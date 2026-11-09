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
/* Copyright 2026 Synexia <hsoliwal@gmail.com> */
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import jdk.m3.collections.LongLongPredicate;

/**
 * Primitive insertion/access-ordered map: two long lanes, two integer link lanes,
 * and an open-addressed integer row index. No per-entry nodes, pairs or boxing.
 * Deletion reuses stable slots rather than shifting a dense order list.
 * Default constructors preserve insertion order; access order is explicit.
 * Expected O(1) lookup/update/remove; hash-collision worst case is linear.
 * Not thread-safe. User callbacks may inspect but may not mutate this map.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongOrderedMap.java}
 * blob {@code 8e9af86abc2cb28e2944c23018af5ecf19abe0b3} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongOrderedMap implements LongLongOrderedMap {
    private long[] keys;
    private long[] values;
    private int[] previous;
    private int[] next;
    private final PackedLongRowIndex index;
    private final boolean accessOrder;
    private int highWater;
    private int free = -1;
    private int first = -1;
    private int last = -1;
    private int callbacks;

    public PackedLongLongOrderedMap() { this(8); }
    public PackedLongLongOrderedMap(int expectedSize) { this(expectedSize, false); }
    public PackedLongLongOrderedMap(int expectedSize, boolean accessOrder) {
        index = new PackedLongRowIndex(expectedSize);
        int capacity = Math.max(1, expectedSize);
        keys = new long[capacity]; values = new long[capacity];
        previous = new int[capacity]; next = new int[capacity];
        this.accessOrder = accessOrder;
    }

    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }

    private void reserve() {
        index.ensure(Math.addExact(size(), 1), keys);
        if (free >= 0 || highWater < keys.length) return;
        int n = PackedLongRowIndex.grownLength(keys.length, highWater + 1);
        long[] k = Arrays.copyOf(keys, n), v = Arrays.copyOf(values, n);
        int[] p = Arrays.copyOf(previous, n), x = Arrays.copyOf(next, n);
        keys = k; values = v; previous = p; next = x;
    }

    @Override public boolean put(long key, long value) {
        writable();
        int row = index.find(key, keys);
        if (row >= 0) {
            values[row] = value;
            if (accessOrder) moveLast(row);
            return false;
        }
        reserve();
        if (free >= 0) { row = free; free = next[row]; }
        else row = highWater++;
        keys[row] = key; values[row] = value;
        index.insert(row, keys);
        linkLast(row);
        return true;
    }

    @Override public boolean containsKey(long key) { return index.find(key, keys) >= 0; }
    /** Read without changing access order, including from a callback. */
    public long peekOrDefault(long key, long fallback) {
        int row = index.find(key, keys);
        return row < 0 ? fallback : values[row];
    }
    @Override public long getOrDefault(long key, long fallback) {
        int row = index.find(key, keys);
        if (row < 0) return fallback;
        if (accessOrder) { writable(); moveLast(row); }
        return values[row];
    }
    @Override public long getOrThrow(long key) {
        int row = index.find(key, keys);
        if (row < 0) throw new IllegalArgumentException("missing key: " + key);
        if (accessOrder) { writable(); moveLast(row); }
        return values[row];
    }
    /** Explicit promotion, useful when insertion order is otherwise required. */
    public boolean moveToLast(long key) {
        writable(); int row = index.find(key, keys);
        if (row < 0) return false;
        moveLast(row); return true;
    }
    public boolean moveToFirst(long key) {
        writable(); int row = index.find(key, keys);
        if (row < 0) return false;
        if (row != first) {
            long endpoints = PackedSlotLinkAtoms.unlink(previous, next, first, last, row);
            endpoints = PackedSlotLinkAtoms.linkFirst(
                    previous, next,
                    PackedSlotLinkAtoms.first(endpoints),
                    PackedSlotLinkAtoms.last(endpoints),
                    row);
            first = PackedSlotLinkAtoms.first(endpoints);
            last = PackedSlotLinkAtoms.last(endpoints);
        }
        return true;
    }

    private void linkLast(int row) {
        long endpoints = PackedSlotLinkAtoms.linkLast(previous, next, first, last, row);
        first = PackedSlotLinkAtoms.first(endpoints);
        last = PackedSlotLinkAtoms.last(endpoints);
    }
    private void unlink(int row) {
        long endpoints = PackedSlotLinkAtoms.unlink(previous, next, first, last, row);
        first = PackedSlotLinkAtoms.first(endpoints);
        last = PackedSlotLinkAtoms.last(endpoints);
    }
    private void moveLast(int row) {
        long endpoints = PackedSlotLinkAtoms.moveLast(previous, next, first, last, row);
        first = PackedSlotLinkAtoms.first(endpoints);
        last = PackedSlotLinkAtoms.last(endpoints);
    }
    private void removeRow(int row) {
        index.remove(keys[row], keys); unlink(row);
        previous[row] = -1; next[row] = free; free = row;
    }
    @Override public boolean remove(long key) {
        writable(); int row = index.find(key, keys);
        if (row < 0) return false;
        removeRow(row); return true;
    }
    public long firstKey() {
        if (first < 0) throw new NoSuchElementException("empty ordered map");
        return keys[first];
    }
    public long lastKey() {
        if (last < 0) throw new NoSuchElementException("empty ordered map");
        return keys[last];
    }
    public long removeFirstKey() {
        writable(); long key = firstKey(); removeRow(first); return key;
    }
    public long removeLastKey() {
        writable(); long key = lastKey(); removeRow(last); return key;
    }

    /** Retains matching mappings. A throwing predicate leaves every mapping/order unchanged.
     * Decisions consume one temporary bit per allocated row; callback side effects outside
     * the collection are not rolled back. Reentrant mutation is rejected before it occurs.
     */
    @Override public int filterInPlace(LongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] reject = new long[(int) ((highWater + 63L) >>> 6)];
        int removed = 0;
        callbacks++;
        try {
            for (int row = first; row >= 0; row = next[row]) {
                if (!predicate.test(keys[row], values[row])) {
                    reject[row >>> 6] |= 1L << (row & 63); removed++;
                }
            }
        } finally { callbacks--; }
        for (int row = first; row >= 0;) {
            int following = next[row];
            if ((reject[row >>> 6] & (1L << (row & 63))) != 0) removeRow(row);
            row = following;
        }
        return removed;
    }

    @Override public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try {
            for (int row = first; row >= 0; row = next[row]) consumer.accept(keys[row], values[row]);
        } finally { callbacks--; }
    }
    @Override public int size() { return index.size(); }
    @Override public boolean isEmpty() { return size() == 0; }
    public int capacity() { return keys.length; }
    public boolean isAccessOrder() { return accessOrder; }
    /** Primitive array payload only: excludes headers, alignment, this object and helper. */
    public long arrayPayloadBytes() { return 24L * keys.length + index.payloadBytes(); }
    @Override public void clear() {
        writable(); index.clear(); highWater = 0; free = -1; first = -1; last = -1;
    }
    @Override public long[] keysToArray() {
        long[] result = new long[size()]; int i = 0;
        for (int row = first; row >= 0; row = next[row]) result[i++] = keys[row];
        return result;
    }
    @Override public long[] valuesToArray() {
        long[] result = new long[size()]; int i = 0;
        for (int row = first; row >= 0; row = next[row]) result[i++] = values[row];
        return result;
    }
}
