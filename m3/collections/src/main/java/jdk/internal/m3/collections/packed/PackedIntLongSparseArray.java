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

import java.util.Arrays;
import jdk.internal.m3.collections.IntLongSparseMap;

/**
 * Sparse primitive int-index to long-value table using sorted parallel arrays.
 *
 * <p>Useful when the logical index space is huge but only a small fraction of positions
 * are materialized. Lookup is binary search; insert/delete shift primitive lanes only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntLongSparseArray.java}
 * blob {@code 23e08b3b98ade8986256ceaf0ee13c2166d7998b} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedIntLongSparseArray implements IntLongSparseMap {
    private int[] indices;
    private long[] values;
    private int size;

    public PackedIntLongSparseArray() {
        this(8);
    }

    public PackedIntLongSparseArray(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        int capacity = Math.max(1, expectedSize);
        indices = new int[capacity];
        values = new long[capacity];
    }

    /**
     * @return true for a new index, false for replacement
     */
    public boolean put(int index, long value) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        int found = Arrays.binarySearch(indices, 0, size, index);
        if (found >= 0) {
            values[found] = value;
            return false;
        }
        int insertion = -found - 1;
        ensureCapacity(size + 1);
        int moved = size - insertion;
        if (moved > 0) {
            System.arraycopy(indices, insertion, indices, insertion + 1, moved);
            System.arraycopy(values, insertion, values, insertion + 1, moved);
        }
        indices[insertion] = index;
        values[insertion] = value;
        size++;
        return true;
    }

    public boolean containsIndex(int index) {
        return Arrays.binarySearch(indices, 0, size, index) >= 0;
    }

    public long getOrDefault(int index, long defaultValue) {
        int found = Arrays.binarySearch(indices, 0, size, index);
        return found < 0 ? defaultValue : values[found];
    }

    public long getOrThrow(int index) {
        int found = Arrays.binarySearch(indices, 0, size, index);
        if (found < 0) {
            throw new IllegalArgumentException("missing index: " + index);
        }
        return values[found];
    }

    public boolean remove(int index) {
        int found = Arrays.binarySearch(indices, 0, size, index);
        if (found < 0) {
            return false;
        }
        int moved = size - found - 1;
        if (moved > 0) {
            System.arraycopy(indices, found + 1, indices, found, moved);
            System.arraycopy(values, found + 1, values, found, moved);
        }
        size--;
        return true;
    }

    public int indexAt(int ordinal) {
        PackedSupport.checkIndex(ordinal, size);
        return indices[ordinal];
    }

    public long valueAt(int ordinal) {
        PackedSupport.checkIndex(ordinal, size);
        return values[ordinal];
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public int[] indicesToArray() {
        return Arrays.copyOf(indices, size);
    }

    public long[] valuesToArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensureCapacity(int minimum) {
        if (minimum <= indices.length) {
            return;
        }
        int capacity = PackedSupport.growArrayCapacity(indices.length, minimum);
        indices = Arrays.copyOf(indices, capacity);
        values = Arrays.copyOf(values, capacity);
    }
}
