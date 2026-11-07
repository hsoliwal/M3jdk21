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
 * Ordered range reduction over a caller-specified long monoid. Associativity, closure
 * and two-sided identity are binding obligations; commutativity is NOT required.
 * The operator must be stable and must not reenter this owner. Source is snapshotted.
 * Tree entries and reusable update-path scratch are primitive arrays, not node objects.
 * Thread-confined or externally synchronized. Callback side effects are not rolled back.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongMonoidSegmentTree.java}
 * blob {@code 74372bb5f67321cded61f7af39879e742a4112bf} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongMonoidSegmentTree {
    private final int size;
    private final int base;
    private final long identity;
    private final LongBinaryOperator operation;
    private final long[] tree;
    private final int[] stagedIndexes;
    private final long[] stagedValues;

    public M3LongMonoidSegmentTree(long[] values, long identity, LongBinaryOperator operation) {
        Objects.requireNonNull(values, "values");
        this.operation = Objects.requireNonNull(operation, "operation");
        size = values.length;
        if (size > (1 << 29)) throw new IllegalArgumentException("tree capacity");
        this.identity = identity;
        int power = 1;
        while (power < size) power <<= 1;
        base = power;
        tree = new long[base << 1];
        Arrays.fill(tree, identity);
        System.arraycopy(values, 0, tree, base, size);
        for (int i = base - 1; i > 0; i--) {
            tree[i] = operation.applyAsLong(tree[i << 1], tree[(i << 1) + 1]);
        }
        int depth = Integer.SIZE - Integer.numberOfLeadingZeros(base);
        stagedIndexes = new int[depth];
        stagedValues = new long[depth];
    }

    public int size() { return size; }
    public long get(int index) { return tree[base + Objects.checkIndex(index, size)]; }
    public long all() { return tree[1]; }

    /** Array payload only; excludes object/array headers and operator-owned state. */
    public long payloadBytes() {
        return 8L * tree.length + 4L * stagedIndexes.length + 8L * stagedValues.length;
    }

    /** O(log n) ordered fold; empty ranges return the identity without operator calls. */
    public long fold(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        if (fromInclusive == toExclusive) return identity;
        long left = identity;
        long right = identity;
        int lo = base + fromInclusive;
        int hi = base + toExclusive;
        while (lo < hi) {
            if ((lo & 1) != 0) left = operation.applyAsLong(left, tree[lo++]);
            if ((hi & 1) != 0) right = operation.applyAsLong(tree[--hi], right);
            lo >>>= 1;
            hi >>>= 1;
        }
        return operation.applyAsLong(left, right);
    }

    /**
     * Computes the entire replacement path before publishing any tree value.
     * An operator exception leaves the tree unchanged; scratch contents may change.
     * No array allocation occurs here. Reentrant/concurrent mutation is forbidden.
     */
    public void set(int index, long value) {
        int at = base + Objects.checkIndex(index, size);
        int count = 0;
        stagedIndexes[count] = at;
        stagedValues[count++] = value;
        while (at > 1) {
            value = (at & 1) == 0
                    ? operation.applyAsLong(value, tree[at + 1])
                    : operation.applyAsLong(tree[at - 1], value);
            at >>>= 1;
            stagedIndexes[count] = at;
            stagedValues[count++] = value;
        }
        for (int i = 0; i < count; i++) tree[stagedIndexes[i]] = stagedValues[i];
    }
}
