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
import java.util.Objects;
import java.util.function.LongBinaryOperator;

/**
 * Mutable associative range index over one flat long[] tree.
 *
 * <p>No node objects are retained. Query order is preserved, so the operation
 * may be associative without being commutative.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongSegmentTree.java}
 * blob {@code b158ac13a1e0d4d78813a5ad0138d6a93fd5adbe} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongSegmentTree {
    private final int size;
    private final long identity;
    private final LongBinaryOperator operation;
    private final long[] tree;

    public M3LongSegmentTree(int size, long identity, LongBinaryOperator operation) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.size = size;
        this.identity = identity;
        this.operation = Objects.requireNonNull(operation, "operation");
        tree = new long[Math.max(1, Math.multiplyExact(size, 2))];
        Arrays.fill(tree, identity);
    }

    public M3LongSegmentTree(long[] values, long identity, LongBinaryOperator operation) {
        Objects.requireNonNull(values, "values");
        this.size = values.length;
        this.identity = identity;
        this.operation = Objects.requireNonNull(operation, "operation");
        tree = new long[Math.max(1, Math.multiplyExact(size, 2))];
        Arrays.fill(tree, identity);
        if (size != 0) {
            System.arraycopy(values, 0, tree, size, size);
            for (int i = size - 1; i > 0; i--) {
                tree[i] = operation.applyAsLong(tree[i << 1], tree[(i << 1) | 1]);
            }
        }
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long identity() { return identity; }
    public long payloadBytes() { return (long) tree.length * Long.BYTES; }

    public long get(int index) {
        checkIndex(index);
        return tree[size + index];
    }

    public void set(int index, long value) {
        checkIndex(index);
        int node = size + index;
        tree[node] = value;
        while ((node >>= 1) != 0) {
            tree[node] = operation.applyAsLong(tree[node << 1], tree[(node << 1) | 1]);
        }
    }

    /** Aggregates [fromInclusive, toExclusive). */
    public long query(int fromInclusive, int toExclusive) {
        checkRange(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return identity;

        int left = fromInclusive + size;
        int right = toExclusive + size;
        long leftResult = identity;
        long rightResult = identity;
        while (left < right) {
            if ((left & 1) != 0) {
                leftResult = operation.applyAsLong(leftResult, tree[left++]);
            }
            if ((right & 1) != 0) {
                rightResult = operation.applyAsLong(tree[--right], rightResult);
            }
            left >>= 1;
            right >>= 1;
        }
        return operation.applyAsLong(leftResult, rightResult);
    }

    public long all() {
        return size == 0 ? identity : tree[1];
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > size) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }
}
