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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Small-set representation with one lazy dense long lane, no index or state bytes.
 * O(n) membership; swap-delete does not preserve encounter order. Every long is valid.
 * Callback failure leaves filter decisions unapplied; callbacks cannot mutate the set.
 * Iterators detect structural changes on a best-effort basis. Not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLinearSet.java}
 * blob {@code 67a9c2bc85edcb333b9902ce85c3142d5bdb8ab0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLinearSet implements PackedMutableLongSet {
    private static final long[] EMPTY = new long[0];
    private long[] values = EMPTY;
    private int reserved;
    private int size;
    private int changes;
    private int callbacks;

    public PackedLongLinearSet() { this(4); }
    public PackedLongLinearSet(int expectedSize) {
        if (expectedSize < 0 || expectedSize > PackedSupport.MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("expectedSize");
        }
        reserved = Math.max(1, expectedSize);
    }
    @Override public boolean add(long value) {
        writable(); if (contains(value)) { return false; }
        int minimum = Math.addExact(size, 1);
        if (minimum > values.length) {
            int capacity = minimum <= reserved ? reserved : PackedSupport.growArrayCapacity(reserved, minimum);
            values = Arrays.copyOf(values, capacity); reserved = capacity;
        }
        values[size++] = value; changes++; return true;
    }
    @Override public boolean contains(long value) { return find(value) >= 0; }
    @Override public boolean remove(long value) {
        writable(); int found = find(value);
        if (found < 0) { return false; }
        values[found] = values[--size]; changes++; return true;
    }
    @Override public int filterInPlace(LongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] keep = new long[(int) ((size + 63L) >>> 6)];
        callbacks++;
        try {
            for (int i = 0; i < size; i++) {
                if (predicate.test(values[i])) { keep[i >>> 6] |= 1L << (i & 63); }
            }
        } finally { callbacks--; }
        int write = 0;
        for (int i = 0; i < size; i++) {
            if ((keep[i >>> 6] & (1L << (i & 63))) != 0) { values[write++] = values[i]; }
        }
        int removed = size - write; size = write;
        if (removed != 0) { changes++; }
        return removed;
    }
    @Override public int size() { return size; }
    @Override public int capacity() { return reserved; }
    @Override public void clear() { writable(); if (size != 0) { size = 0; changes++; } }
    @Override public void trimToSize() {
        writable(); values = size == 0 ? EMPTY : Arrays.copyOf(values, size); reserved = Math.max(1, size);
    }
    @Override public long[] toArray() { return Arrays.copyOf(values, size); }
    public long arrayPayloadBytes() { return 8L * values.length; }
    @Override public LongIterator iterator() {
        return new LongIterator() {
            private final int expected = changes;
            private int at;
            private void check() {
                if (expected != changes) { throw new ConcurrentModificationException(); }
            }
            @Override public boolean hasNext() { check(); return at < size; }
            @Override public long nextLong() {
                check(); if (at == size) { throw new NoSuchElementException(); }
                return values[at++];
            }
        };
    }
    private int find(long value) {
        for (int i = 0; i < size; i++) { if (values[i] == value) { return i; } }
        return -1;
    }
    private void writable() {
        if (callbacks != 0) { throw new IllegalStateException("mutation during callback"); }
    }
}
