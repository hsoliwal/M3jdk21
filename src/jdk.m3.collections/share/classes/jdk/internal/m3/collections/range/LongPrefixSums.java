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
package jdk.internal.m3.collections.range;

import java.util.Objects;

/**
 * Immutable prefix-sum index over a long sequence.
 *
 * <p>Retained state is one exact long[] prefix lane of length n+1. Range sums
 * are O(1); updates are deliberately unsupported.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongPrefixSums.java}
 * blob {@code 7251c563020e3239051201f809fa9a55d130452a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongPrefixSums {
    private final long[] prefix;

    public LongPrefixSums(long[] values) {
        Objects.requireNonNull(values, "values");
        prefix = new long[Math.addExact(values.length, 1)];
        long sum = 0L;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
            prefix[i + 1] = sum;
        }
    }

    public int size() {
        return prefix.length - 1;
    }

    public boolean isEmpty() {
        return prefix.length == 1;
    }

    public long payloadBytes() {
        return (long) prefix.length * Long.BYTES;
    }

    /** Sum of [0,endExclusive). */
    public long prefixSum(int endExclusive) {
        checkEnd(endExclusive);
        return prefix[endExclusive];
    }

    /** Sum of [fromInclusive,toExclusive). */
    public long rangeSum(int fromInclusive, int toExclusive) {
        checkRange(fromInclusive, toExclusive);
        return prefix[toExclusive] - prefix[fromInclusive];
    }

    public long get(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
        return prefix[index + 1] - prefix[index];
    }

    public long total() {
        return prefix[prefix.length - 1];
    }

    public long[] copyPrefixLane() {
        return prefix.clone();
    }

    private void checkEnd(int endExclusive) {
        if (endExclusive < 0 || endExclusive > size()) {
            throw new IndexOutOfBoundsException(endExclusive);
        }
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > size()) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }
}
