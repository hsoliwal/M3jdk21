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
import java.util.function.IntConsumer;

/**
 * O(1) non-negative int sparse set using dense[] + sparse[] index lanes.
 *
 * <p>Removal swap-compacts the dense lane and repairs one sparse index. This is
 * useful for entity/state/behavior identifiers when the key domain is moderately
 * sparse and object hashing would be wasteful.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntSparseSet.java}
 * blob {@code 19c53bcc95e4af6cfc0ddf3b5b2a50c2e06553bd} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntSparseSet {
    private int[] dense;
    private int[] sparse;
    private int size;

    public M3IntSparseSet() {
        this(8, 8);
    }

    public M3IntSparseSet(int expectedSize, int expectedKeyRange) {
        if (expectedSize < 0 || expectedKeyRange < 0) throw new IllegalArgumentException();
        dense = new int[Math.max(1, expectedSize)];
        sparse = new int[Math.max(1, expectedKeyRange)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return (long) (dense.length + sparse.length) * Integer.BYTES;
    }

    public boolean contains(int value) {
        if (value < 0 || value >= sparse.length) return false;
        int index = sparse[value];
        return index < size && dense[index] == value;
    }

    public boolean add(int value) {
        if (value < 0) throw new IllegalArgumentException("value < 0");
        if (contains(value)) return false;
        ensureSparse(value + 1);
        ensureDense(size + 1);
        sparse[value] = size;
        dense[size++] = value;
        return true;
    }

    public boolean remove(int value) {
        if (!contains(value)) return false;
        int index = sparse[value];
        int lastIndex = --size;
        int moved = dense[lastIndex];
        dense[index] = moved;
        sparse[moved] = index;
        dense[lastIndex] = 0;
        sparse[value] = 0;
        return true;
    }

    public int get(int denseIndex) {
        if (denseIndex < 0 || denseIndex >= size) throw new IndexOutOfBoundsException(denseIndex);
        return dense[denseIndex];
    }

    public void clear() {
        for (int i = 0; i < size; i++) sparse[dense[i]] = 0;
        Arrays.fill(dense, 0, size, 0);
        size = 0;
    }

    public int[] toArray() { return Arrays.copyOf(dense, size); }

    public void forEach(IntConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept(dense[i]);
    }

    private void ensureDense(int required) {
        if (required <= dense.length) return;
        int next = dense.length + (dense.length >>> 1) + 1;
        dense = Arrays.copyOf(dense, Math.max(required, next));
    }

    private void ensureSparse(int required) {
        if (required <= sparse.length) return;
        int next = sparse.length + (sparse.length >>> 1) + 1;
        sparse = Arrays.copyOf(sparse, Math.max(required, next));
    }
}
