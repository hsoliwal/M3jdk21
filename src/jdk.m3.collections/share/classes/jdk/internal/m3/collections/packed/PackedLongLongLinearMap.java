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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.LongLongPredicate;

/**
 * Small-map representation: two dense long lanes, no hash index or state lane.
 * Lookup is O(n); deletion swaps the last row. All long keys/values are valid.
 * Arrays allocate on first insertion. Capacity is a logical reservation until then.
 * Predicates evaluate before compaction; callbacks cannot mutate this map.
 * Not thread-safe. Iteration order is unspecified and can change after deletion.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongLinearMap.java}
 * blob {@code c53251233d9630c9f0b3206e617deb250ae79d00} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongLinearMap implements PackedLongLongMap {
    private static final long[] EMPTY = new long[0];
    private long[] keys = EMPTY;
    private long[] values = EMPTY;
    private int reserved;
    private int size;
    private int callbacks;

    public PackedLongLongLinearMap() { this(4); }
    public PackedLongLongLinearMap(int expectedSize) {
        if (expectedSize < 0 || expectedSize > PackedSupport.MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("expectedSize");
        }
        reserved = Math.max(1, expectedSize);
    }

    @Override public boolean put(long key, long value) {
        writable();
        int found = find(key);
        if (found >= 0) { values[found] = value; return false; }
        reserve(Math.addExact(size, 1));
        keys[size] = key; values[size] = value; size++;
        return true;
    }

    @Override public boolean containsKey(long key) { return find(key) >= 0; }
    @Override public long getOrDefault(long key, long fallback) {
        int found = find(key); return found < 0 ? fallback : values[found];
    }
    @Override public long getOrThrow(long key) {
        int found = find(key);
        if (found < 0) { throw new IllegalArgumentException("missing key: " + key); }
        return values[found];
    }
    @Override public boolean remove(long key) {
        writable(); int found = find(key);
        if (found < 0) { return false; }
        int last = --size; keys[found] = keys[last]; values[found] = values[last];
        return true;
    }
    @Override public int filterInPlace(LongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] keep = new long[(int) ((size + 63L) >>> 6)];
        callbacks++;
        try {
            for (int i = 0; i < size; i++) {
                if (predicate.test(keys[i], values[i])) { keep[i >>> 6] |= 1L << (i & 63); }
            }
        } finally { callbacks--; }
        int write = 0;
        for (int i = 0; i < size; i++) {
            if ((keep[i >>> 6] & (1L << (i & 63))) != 0) {
                keys[write] = keys[i]; values[write++] = values[i];
            }
        }
        int removed = size - write; size = write; return removed;
    }
    @Override public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) { consumer.accept(keys[i], values[i]); } }
        finally { callbacks--; }
    }
    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public int capacity() { return reserved; }
    @Override public void clear() { writable(); size = 0; }
    @Override public void trimToSize() {
        writable();
        if (size == 0) { keys = EMPTY; values = EMPTY; reserved = 1; return; }
        long[] nextKeys = Arrays.copyOf(keys, size);
        long[] nextValues = Arrays.copyOf(values, size);
        keys = nextKeys; values = nextValues; reserved = size;
    }
    @Override public long[] keysToArray() { return Arrays.copyOf(keys, size); }
    @Override public long[] valuesToArray() { return Arrays.copyOf(values, size); }
    public long arrayPayloadBytes() { return 16L * keys.length; }

    private int find(long key) {
        for (int i = 0; i < size; i++) { if (keys[i] == key) { return i; } }
        return -1;
    }
    private void writable() {
        if (callbacks != 0) { throw new IllegalStateException("mutation during callback"); }
    }
    private void reserve(int minimum) {
        if (minimum <= keys.length) { return; }
        int capacity = minimum <= reserved ? reserved : PackedSupport.growArrayCapacity(reserved, minimum);
        long[] nextKeys = Arrays.copyOf(keys, capacity);
        long[] nextValues = Arrays.copyOf(values, capacity);
        keys = nextKeys; values = nextValues; reserved = capacity;
    }
}
