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
import java.util.Objects;

/**
 * Exact checked range-sum sqrt decomposition with point set/add updates.
 *
 * <p>Useful when the caller wants a simpler mutable block index than a Fenwick/segment tree.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongSqrtDecomposition.java}
 * blob {@code 774e7920a58275298b6b12801a91ad4be7acb37c} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongSqrtDecomposition {
    private final long[] values;
    private final long[] blockSums;
    private final int blockSize;

    public M3LongSqrtDecomposition(int length) {
        this(new long[checkedLength(length)]);
    }

    public M3LongSqrtDecomposition(long[] source) {
        Objects.requireNonNull(source, "source");
        values = source.clone();
        blockSize = Math.max(1, integerSqrtCeiling(Math.max(1, values.length)));
        blockSums = new long[divideCeiling(values.length, blockSize)];
        for (int i = 0; i < values.length; i++) {
            int block = i / blockSize;
            blockSums[block] = Math.addExact(blockSums[block], values[i]);
        }
    }

    public int length() {
        return values.length;
    }

    public int blockSize() {
        return blockSize;
    }

    public int blockCount() {
        return blockSums.length;
    }

    public long get(int index) {
        return values[Objects.checkIndex(index, values.length)];
    }

    public long set(int index, long value) {
        Objects.checkIndex(index, values.length);
        long previous = values[index];
        if (previous == value) {
            return previous;
        }
        int block = index / blockSize;
        long nextBlock = Math.addExact(
                Math.subtractExact(blockSums[block], previous),
                value);
        values[index] = value;
        blockSums[block] = nextBlock;
        return previous;
    }

    public long add(int index, long delta) {
        Objects.checkIndex(index, values.length);
        long nextValue = Math.addExact(values[index], delta);
        set(index, nextValue);
        return nextValue;
    }

    public long rangeSum(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, values.length);
        long result = 0L;
        int at = fromInclusive;
        while (at < toExclusive && at % blockSize != 0) {
            result = Math.addExact(result, values[at++]);
        }
        while (at + blockSize <= toExclusive) {
            result = Math.addExact(result, blockSums[at / blockSize]);
            at += blockSize;
        }
        while (at < toExclusive) {
            result = Math.addExact(result, values[at++]);
        }
        return result;
    }

    public long prefixSum(int toExclusive) {
        Objects.checkFromToIndex(0, toExclusive, values.length);
        return rangeSum(0, toExclusive);
    }

    public long[] values() {
        return values.clone();
    }

    public long payloadBytes() {
        return 8L * (values.length + blockSums.length);
    }

    private static int checkedLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length");
        }
        return length;
    }

    private static int divideCeiling(int value, int divisor) {
        return value == 0 ? 0 : 1 + (value - 1) / divisor;
    }

    private static int integerSqrtCeiling(int value) {
        int root = (int) Math.sqrt(value);
        if ((long) root * root < value) {
            root++;
        }
        return root;
    }
}
