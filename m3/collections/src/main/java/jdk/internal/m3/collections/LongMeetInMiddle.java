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
 * Deterministic meet-in-the-middle subset-sum kernels over long values.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongMeetInMiddle.java}
 * blob {@code f4ee1b48faf3e9dc507f31cb08cc210d5604be25} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongMeetInMiddle {
    private static final int MAX_HALF_BITS = 25;

    private LongMeetInMiddle() {}

    /**
     * Maximum subset sum <= limit.
     *
     * <p>Supports negative values. Arithmetic overflow throws.
     */
    public static long maxSubsetSumAtMost(long[] values, long limit) {
        Objects.requireNonNull(values, "values");
        int leftCount = values.length >>> 1;
        int rightCount = values.length - leftCount;
        if (leftCount > MAX_HALF_BITS || rightCount > MAX_HALF_BITS) {
            throw new IllegalArgumentException("at most " + (MAX_HALF_BITS * 2) + " values supported");
        }

        long[] left = enumerate(values, 0, leftCount);
        long[] right = enumerate(values, leftCount, values.length);
        Arrays.sort(right);

        boolean found = false;
        long best = Long.MIN_VALUE;
        for (long leftSum : left) {
            long remaining;
            try {
                remaining = Math.subtractExact(limit, leftSum);
            } catch (ArithmeticException overflow) {
                remaining = leftSum < 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            int index = upperBound(right, remaining) - 1;
            if (index < 0) continue;
            long total = Math.addExact(leftSum, right[index]);
            if (total <= limit && (!found || total > best)) {
                best = total;
                found = true;
            }
        }
        if (!found) throw new IllegalStateException("no subset sum <= limit");
        return best;
    }

    private static long[] enumerate(long[] values, int from, int to) {
        int count = to - from;
        int size = 1 << count;
        long[] sums = new long[size];
        for (int mask = 1; mask < size; mask++) {
            int bit = Integer.numberOfTrailingZeros(mask);
            int previous = mask & (mask - 1);
            sums[mask] = Math.addExact(sums[previous], values[from + bit]);
        }
        return sums;
    }

    private static int upperBound(long[] values, long target) {
        int lo = 0;
        int hi = values.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (values[mid] <= target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }
}
