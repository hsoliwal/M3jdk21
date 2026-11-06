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
 * Immutable sorted-unique int dictionary for coordinate/value compression.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/IntCoordinateCompressor.java}
 * blob {@code 8d00ada0cdf023c181da4730335eb308bc616f81} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntCoordinateCompressor {
    private final int[] values;

    public M3IntCoordinateCompressor(int[] source) {
        Objects.requireNonNull(source, "source");
        int[] sorted = source.clone();
        Arrays.sort(sorted);
        int unique = 0;
        for (int value : sorted) {
            if (unique == 0 || sorted[unique - 1] != value) sorted[unique++] = value;
        }
        // The sorted clone is already the exact immutable dictionary when all
        // entries are distinct. Avoid allocating and copying a second int[].
        values = unique == sorted.length ? sorted : Arrays.copyOf(sorted, unique);
    }

    public int size() { return values.length; }
    public boolean isEmpty() { return values.length == 0; }
    public long payloadBytes() { return (long) values.length * Integer.BYTES; }

    public int valueAt(int rank) {
        return values[rank];
    }

    /** Exact rank, or -1 when absent. */
    public int rankOf(int value) {
        int index = Arrays.binarySearch(values, value);
        return index >= 0 ? index : -1;
    }

    /** First rank whose value is >= target; may equal size(). */
    public int lowerBound(int target) {
        int lo = 0;
        int hi = values.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (values[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** First rank whose value is > target; may equal size(). */
    public int upperBound(int target) {
        int lo = 0;
        int hi = values.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (values[mid] <= target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Fail-closed exact encoding. */
    public int[] encode(int[] source) {
        Objects.requireNonNull(source, "source");
        int[] encoded = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            int rank = rankOf(source[i]);
            if (rank < 0) throw new IllegalArgumentException("value absent from compressor: " + source[i]);
            encoded[i] = rank;
        }
        return encoded;
    }

    public int[] dictionary() {
        return values.clone();
    }
}
