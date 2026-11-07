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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Overflow-safe binary search over a monotone false..true predicate on a signed-long interval.
 *
 * <p>No retained search state or wrapper allocation is required by the kernel.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongPartitionSearch.java}
 * blob {@code 0758d70c3594eefb5df307c3d387c8501215a673} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongPartitionSearch {

    private M3LongPartitionSearch() {
    }

    /**
     * Returns the smallest value in [lowInclusive, highInclusive] for which predicate is true.
     *
     * @throws NoSuchElementException when the predicate is false at highInclusive
     */
    public static long firstTrue(
            long lowInclusive,
            long highInclusive,
            LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        checkRange(lowInclusive, highInclusive);
        if (!predicate.test(highInclusive)) {
            throw new NoSuchElementException("predicate is false across the supplied upper bound");
        }

        long low = lowInclusive;
        long high = highInclusive;
        while (low < high) {
            long mid = floorAverage(low, high);
            if (predicate.test(mid)) {
                high = mid;
            } else {
                low = mid + 1L;
            }
        }
        return low;
    }

    /**
     * Returns the largest value in [lowInclusive, highInclusive] for which predicate is false.
     *
     * @throws NoSuchElementException when the predicate is true at lowInclusive
     */
    public static long lastFalse(
            long lowInclusive,
            long highInclusive,
            LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        checkRange(lowInclusive, highInclusive);
        if (predicate.test(lowInclusive)) {
            throw new NoSuchElementException("predicate is true across the supplied lower bound");
        }

        long low = lowInclusive;
        long high = highInclusive;
        while (low < high) {
            long mid = ceilAverage(low, high);
            if (predicate.test(mid)) {
                high = mid - 1L;
            } else {
                low = mid;
            }
        }
        return low;
    }

    /** Exact floor((left + right) / 2) without signed overflow. */
    static long floorAverage(long left, long right) {
        return (left & right) + ((left ^ right) >> 1);
    }

    /** Exact ceil((left + right) / 2) without signed overflow. */
    static long ceilAverage(long left, long right) {
        long floor = floorAverage(left, right);
        return floor + ((left ^ right) & 1L);
    }

    private static void checkRange(long lowInclusive, long highInclusive) {
        if (lowInclusive > highInclusive) {
            throw new IllegalArgumentException(
                    "lowInclusive > highInclusive: "
                            + lowInclusive + " > " + highInclusive);
        }
    }
}
