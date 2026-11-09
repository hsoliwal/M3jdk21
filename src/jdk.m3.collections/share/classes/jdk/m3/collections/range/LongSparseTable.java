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
package jdk.m3.collections.range;

import java.util.Objects;
import java.util.function.LongBinaryOperator;

/**
 * Immutable idempotent range-query table.
 *
 * <p>Typical operations are min, max and gcd. The supplied operation must be
 * associative and idempotent. Each level is an exact-sized primitive long[].
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongSparseTable.java}
 * blob {@code 840ba7767f74a6997572b4385c8535f1d1382285} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongSparseTable {
    private final int size;
    private final LongBinaryOperator operation;
    private final long[][] levels;
    private final long payloadBytes;

    public LongSparseTable(long[] values, LongBinaryOperator operation) {
        Objects.requireNonNull(values, "values");
        this.operation = Objects.requireNonNull(operation, "operation");
        size = values.length;
        if (size == 0) {
            levels = new long[0][];
            payloadBytes = 0L;
            return;
        }

        int levelCount = 32 - Integer.numberOfLeadingZeros(size);
        levels = new long[levelCount][];
        levels[0] = values.clone();
        long bytes = (long) values.length * Long.BYTES;

        for (int level = 1; level < levelCount; level++) {
            int half = 1 << (level - 1);
            int span = half << 1;
            int count = size - span + 1;
            long[] current = new long[count];
            long[] previous = levels[level - 1];
            for (int i = 0; i < count; i++) {
                current[i] = operation.applyAsLong(previous[i], previous[i + half]);
            }
            levels[level] = current;
            bytes += (long) current.length * Long.BYTES;
        }
        payloadBytes = bytes;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int levels() { return levels.length; }
    public long payloadBytes() { return payloadBytes; }

    /** Queries non-empty [fromInclusive, toExclusive). */
    public long query(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive >= toExclusive || toExclusive > size) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
        int length = toExclusive - fromInclusive;
        int level = 31 - Integer.numberOfLeadingZeros(length);
        int span = 1 << level;
        long left = levels[level][fromInclusive];
        if (span == length) return left;
        long right = levels[level][toExclusive - span];
        return operation.applyAsLong(left, right);
    }
}
