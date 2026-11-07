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

/**
 * Immutable exact coordinate compression for signed long values.
 *
 * <p>Dense ranks preserve signed numeric order. No map or boxed key is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongCoordinateCompression.java}
 * blob {@code fbfd110f2867989f1f49e6038d09f899706fc8e6} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongCoordinateCompression {
    private final long[] values;

    public M3LongCoordinateCompression(long[] source) {
        Objects.requireNonNull(source, "source");
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        if (sorted.length == 0) {
            values = sorted;
            return;
        }
        int unique = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] != sorted[unique - 1]) {
                sorted[unique++] = sorted[i];
            }
        }
        values = Arrays.copyOf(sorted, unique);
    }

    public int size() {
        return values.length;
    }

    public boolean isEmpty() {
        return values.length == 0;
    }

    public long valueAt(int rank) {
        return values[Objects.checkIndex(rank, values.length)];
    }

    public int rankOf(long value) {
        int rank = Arrays.binarySearch(values, value);
        return rank >= 0 ? rank : -1;
    }

    public int requireRank(long value) {
        int rank = rankOf(value);
        if (rank < 0) {
            throw new IllegalArgumentException("coordinate absent: " + value);
        }
        return rank;
    }

    public int lowerBound(long value) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (values[middle] < value) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public int upperBound(long value) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (values[middle] <= value) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public int[] compressExact(long[] source) {
        Objects.requireNonNull(source, "source");
        int[] ranks = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            ranks[i] = requireRank(source[i]);
        }
        return ranks;
    }

    public long[] decompress(int[] ranks) {
        Objects.requireNonNull(ranks, "ranks");
        long[] decoded = new long[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            decoded[i] = valueAt(ranks[i]);
        }
        return decoded;
    }

    public long[] values() {
        return values.clone();
    }

    public long payloadBytes() {
        return 8L * values.length;
    }
}
