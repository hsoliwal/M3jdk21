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

import java.util.Arrays;

/**
 * Dense IDs and lazily allocated sparse-index pages. Clear retains pages and is O(1).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntSparseSet.java}
 * blob {@code a2d5358e572c1944a627998d3a9f639c5441e38c} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedIntSparseSet implements M3IntUniverseSet {
    private final int universeSize;
    private final int blockSize;
    private final int[][] sparse;
    private int[] dense = new int[0];
    private int size;
    private int allocatedPages;
    public M3PackedIntSparseSet(int universeSize, int blockSize) {
        if (universeSize < 0 || universeSize > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("universeSize");
        if (blockSize < 1 || blockSize > 1 << 20) throw new IllegalArgumentException("blockSize");
        this.universeSize = universeSize; this.blockSize = blockSize;
        sparse = new int[(int) ((universeSize + (long) blockSize - 1) / blockSize)][];
    }
    public int size() { return size; }
    public int universeSize() { return universeSize; }
    public int blockSize() { return blockSize; }
    public int allocatedPages() { return allocatedPages; }
    public long arrayPayloadBytes() { return 4L * dense.length + 4L * allocatedPages * blockSize; }
    public boolean contains(int value) {
        check(value); int[] page = sparse[value / blockSize];
        if (page == null) return false;
        int at = page[value % blockSize];
        return at < size && dense[at] == value;
    }
    public boolean add(int value) {
        if (contains(value)) return false;
        int[] nextDense = dense;
        if (size == dense.length) {
            int capacity = (int) Math.min(universeSize, Math.max(16L, dense.length + (long) (dense.length >>> 1) + 1));
            nextDense = Arrays.copyOf(dense, capacity);
        }
        int pageAt = value / blockSize;
        int[] page = sparse[pageAt];
        if (page == null) { page = new int[blockSize]; sparse[pageAt] = page; allocatedPages++; }
        dense = nextDense; dense[size] = value; page[value % blockSize] = size++; return true;
    }
    public boolean remove(int value) {
        if (!contains(value)) return false;
        int at = sparse[value / blockSize][value % blockSize];
        int last = dense[--size]; dense[at] = last;
        sparse[last / blockSize][last % blockSize] = at; return true;
    }
    public void clear() { size = 0; }
    public int[] toArray() { return Arrays.copyOf(dense, size); }
    private void check(int value) {
        if (value < 0 || value >= universeSize) throw new IndexOutOfBoundsException(value);
    }
}
