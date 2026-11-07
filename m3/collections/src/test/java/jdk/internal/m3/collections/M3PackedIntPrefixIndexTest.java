// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class M3PackedIntPrefixIndexTest {
    @Test
    void locatesLogicalOffsetsAcrossNonUniformAndEmptySegments() {
        int[] lengths = {0, 3, 0, 5, 1};
        M3PackedIntPrefixIndex index =
                M3PackedIntPrefixIndex.ofLengths(lengths.length, segment -> lengths[segment]);

        assertEquals(5, index.segmentCount());
        assertEquals(9, index.totalLength());
        assertArrayEquals(new int[] {0, 3, 3, 8, 9}, index.cumulativeEnds());

        assertEquals(1, index.segmentIndex(0));
        assertEquals(1, index.segmentIndex(2));
        assertEquals(3, index.segmentIndex(3));
        assertEquals(3, index.segmentIndex(7));
        assertEquals(4, index.segmentIndex(8));

        assertEquals(0, index.prefixLength(0));
        assertEquals(0, index.prefixLength(1));
        assertEquals(3, index.prefixLength(2));
        assertEquals(3, index.prefixLength(3));
        assertEquals(9, index.prefixLength(5));
        assertEquals(3, index.segmentStart(3));
        assertEquals(8, index.segmentEnd(3));
        assertEquals(5L * Integer.BYTES, index.retainedPrimitiveBytes());
    }

    @Test
    void cumulativeConstructionIsDefensiveAndRejectsDecreasingEnds() {
        int[] ends = {2, 2, 7};
        M3PackedIntPrefixIndex index = M3PackedIntPrefixIndex.ofCumulativeEnds(ends);
        ends[2] = 99;

        assertArrayEquals(new int[] {2, 2, 7}, index.cumulativeEnds());
        assertEquals(2, index.segmentIndex(2));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PackedIntPrefixIndex.ofCumulativeEnds(new int[] {2, 1}));
    }

    @Test
    void rejectsNegativeLengthsOverflowAndOutOfRangeCoordinates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PackedIntPrefixIndex.ofLengths(1, ignored -> -1));
        assertThrows(
                ArithmeticException.class,
                () ->
                        M3PackedIntPrefixIndex.ofLengths(
                                2, segment -> segment == 0 ? Integer.MAX_VALUE : 1));

        M3PackedIntPrefixIndex empty = M3PackedIntPrefixIndex.ofLengths(0, ignored -> 0);
        assertEquals(0, empty.totalLength());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.segmentIndex(0));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> M3PackedIntPrefixIndex.ofLengths(1, ignored -> 1).segmentIndex(1));
    }
}
