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
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Immutable cumulative-end index for a logical sequence split across primitive segments.
 *
 * <p>The retained representation is one {@code int[]} containing cumulative exclusive ends.
 * Segment ownership lookup is O(log n), prefix length is O(1), and no payload values are copied
 * into the index. Zero-length segments are permitted and are skipped naturally by ownership
 * lookup.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntPrefixIndex.java}
 * blob {@code ab3ad8d337bb0a7348f883fb22d9f61bb41b7812} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedIntPrefixIndex {
    private static final PackedIntPrefixIndex EMPTY = new PackedIntPrefixIndex(new int[0]);

    private final int[] ends;

    private PackedIntPrefixIndex(int[] ends) {
        this.ends = ends;
    }

    /**
     * Builds cumulative ends from non-negative segment lengths.
     *
     * @param segmentCount number of segments
     * @param lengthAt supplies each segment's logical length
     * @return immutable prefix index
     */
    public static PackedIntPrefixIndex ofLengths(
            int segmentCount, IntUnaryOperator lengthAt) {
        if (segmentCount < 0) {
            throw new IllegalArgumentException("negative segment count");
        }
        Objects.requireNonNull(lengthAt, "lengthAt");
        if (segmentCount == 0) {
            return EMPTY;
        }

        int[] ends = new int[segmentCount];
        int total = 0;
        for (int segment = 0; segment < segmentCount; segment++) {
            int length = lengthAt.applyAsInt(segment);
            if (length < 0) {
                throw new IllegalArgumentException("negative segment length at " + segment);
            }
            total = Math.addExact(total, length);
            ends[segment] = total;
        }
        return new PackedIntPrefixIndex(ends);
    }

    /**
     * Builds from already cumulative non-decreasing exclusive ends.
     *
     * <p>The input is defensively copied.
     */
    public static PackedIntPrefixIndex ofCumulativeEnds(int[] cumulativeEnds) {
        Objects.requireNonNull(cumulativeEnds, "cumulativeEnds");
        if (cumulativeEnds.length == 0) {
            return EMPTY;
        }
        int previous = 0;
        int[] copy = cumulativeEnds.clone();
        for (int segment = 0; segment < copy.length; segment++) {
            int end = copy[segment];
            if (end < previous) {
                throw new IllegalArgumentException("decreasing cumulative end at " + segment);
            }
            previous = end;
        }
        return new PackedIntPrefixIndex(copy);
    }

    public int segmentCount() {
        return ends.length;
    }

    public int totalLength() {
        return ends.length == 0 ? 0 : ends[ends.length - 1];
    }

    /** Returns the cumulative length of the first {@code segmentCount} segments. */
    public int prefixLength(int segmentCount) {
        if (segmentCount < 0 || segmentCount > ends.length) {
            throw new IndexOutOfBoundsException(
                    "segmentCount=" + segmentCount + ", size=" + ends.length);
        }
        return segmentCount == 0 ? 0 : ends[segmentCount - 1];
    }

    public int segmentStart(int segment) {
        Objects.checkIndex(segment, ends.length);
        return segment == 0 ? 0 : ends[segment - 1];
    }

    public int segmentEnd(int segment) {
        return ends[Objects.checkIndex(segment, ends.length)];
    }

    /**
     * Locates the segment owning one logical element.
     *
     * <p>The first cumulative end strictly greater than {@code logicalIndex} owns the element.
     * This correctly skips any zero-length segments.
     */
    public int segmentIndex(int logicalIndex) {
        Objects.checkIndex(logicalIndex, totalLength());
        int low = 0;
        int high = ends.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ends[middle] <= logicalIndex) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** Returns a defensive snapshot of cumulative exclusive ends. */
    public int[] cumulativeEnds() {
        return ends.clone();
    }

    public long retainedPrimitiveBytes() {
        return (long) Integer.BYTES * ends.length;
    }

    @Override
    public String toString() {
        return "PackedIntPrefixIndex" + Arrays.toString(ends);
    }
}
