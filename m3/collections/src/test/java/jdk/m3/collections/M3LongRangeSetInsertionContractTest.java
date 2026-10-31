/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Public-contract oracles independent of the insertion-position implementation. */
final class M3LongRangeSetInsertionContractTest {
    @Test
    void emptyFrontTailGapAndTwoSidedTouchesRetainExactUnion() {
        M3LongRangeSet set = new M3LongRangeSet();
        assertRanges(set);
        set.add(10, 20);
        assertRanges(set, 10, 20);
        set.add(30, 40);
        assertRanges(set, 10, 20, 30, 40);
        set.add(-10, 0);
        assertRanges(set, -10, 0, 10, 20, 30, 40);
        set.add(5, 7);
        assertRanges(set, -10, 0, 5, 7, 10, 20, 30, 40);
        set.add(0, 5);
        assertRanges(set, -10, 7, 10, 20, 30, 40);
        set.add(7, 10);
        assertRanges(set, -10, 20, 30, 40);
        set.add(20, 30);
        assertRanges(set, -10, 40);
        set.add(-5, 35);
        assertRanges(set, -10, 40);
    }

    @Test
    void oneSidedTouchAndOverlapDoNotAbsorbAnUncoveredGap() {
        M3LongRangeSet left = ranges(10, 20, 30, 40);
        left.add(0, 10);
        assertRanges(left, 0, 20, 30, 40);

        M3LongRangeSet right = ranges(10, 20, 30, 40);
        right.add(40, 50);
        assertRanges(right, 10, 20, 30, 50);

        M3LongRangeSet overlap = ranges(10, 20, 30, 40, 50, 60);
        overlap.add(15, 35);
        assertRanges(overlap, 10, 40, 50, 60);
        assertFalse(overlap.contains(40));
        assertFalse(overlap.intersects(40, 50));
        assertTrue(overlap.contains(50));
    }

    @Test
    void allSmallDomainInsertionsMatchIndependentCoverageModel() {
        final int offset = 4;
        final int cells = 8;
        for (int mask = 0; mask < (1 << cells); mask++) {
            M3LongRangeSet original = new M3LongRangeSet();
            BitSet coverage = new BitSet(cells);
            for (int bit = 0; bit < cells; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    original.add(bit - offset, bit + 1 - offset);
                    coverage.set(bit);
                }
            }
            assertCoverage(original, coverage, offset, cells);
            for (int from = -offset; from <= offset; from++) {
                for (int to = from; to <= offset; to++) {
                    M3LongRangeSet candidate = original.snapshot();
                    BitSet expected = (BitSet) coverage.clone();
                    candidate.add(from, to);
                    expected.set(from + offset, to + offset);
                    assertCoverage(candidate, expected, offset, cells);
                }
            }
            assertCoverage(original, coverage, offset, cells);
        }
    }

    @Test
    void containedIdenticalEmptyAndRejectedAddsPreserveStateAndCapacity() {
        M3LongRangeSet set = ranges(10, 20, 30, 40);
        int capacity = set.capacity();
        set.add(11, 19);
        set.add(30, 40);
        set.add(Long.MIN_VALUE, Long.MIN_VALUE);
        set.add(Long.MAX_VALUE, Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> set.add(21, 20));
        assertThrows(IllegalArgumentException.class,
                () -> set.add(Long.MAX_VALUE, Long.MIN_VALUE));
        assertRanges(set, 10, 20, 30, 40);
        assertEquals(capacity, set.capacity());
    }

    @Test
    void nonstructuralAddsInsideRangeCallbackDoNotInvalidateTraversal() {
        M3LongRangeSet set = ranges(10, 20, 30, 40);
        List<Long> visited = new ArrayList<>();
        set.forEachRange((from, to) -> {
            visited.add(from);
            visited.add(to);
            set.add(from, to);
            set.add(from + 1, to - 1);
            set.add(from, from);
        });
        assertEquals(List.of(10L, 20L, 30L, 40L), visited);
        assertRanges(set, 10, 20, 30, 40);
    }

    @Test
    void structuralAddInsideCallbackFailsFastAfterPreservingMutation() {
        M3LongRangeSet set = ranges(10, 20, 30, 40);
        List<Long> visited = new ArrayList<>();
        assertThrows(ConcurrentModificationException.class, () ->
                set.forEachRange((from, to) -> {
                    visited.add(from);
                    visited.add(to);
                    set.add(50, 60);
                }));
        assertEquals(List.of(10L, 20L), visited);
        assertRanges(set, 10, 20, 30, 40, 50, 60);
    }

    @Test
    void callbackExceptionIdentityAndPriorityArePreserved() {
        M3LongRangeSet set = ranges(10, 20, 30, 40);
        IllegalStateException marker = new IllegalStateException("callback marker");
        int[] visits = {0};
        IllegalStateException actual = assertThrows(IllegalStateException.class, () ->
                set.forEachRange((from, to) -> {
                    visits[0]++;
                    set.add(50, 60);
                    throw marker;
                }));
        assertSame(marker, actual);
        assertEquals(1, visits[0]);
        assertRanges(set, 10, 20, 30, 40, 50, 60);
        assertThrows(NullPointerException.class, () -> set.forEachRange(null));
        assertThrows(NullPointerException.class,
                () -> new M3LongRangeSet().forEachRange(null));
    }

    @Test
    void snapshotsAndReturnedLanesRemainDetachedAcrossInsertions() {
        M3LongRangeSet set = ranges(-10, -2, 4, 9);
        M3LongRangeSet snapshot = set.snapshot();
        long[] exposedStarts = set.startsToArray();
        long[] exposedEnds = set.endsToArray();
        long[] snapshotStarts = snapshot.startsToArray();
        long[] snapshotEnds = snapshot.endsToArray();
        exposedStarts[0] = Long.MIN_VALUE;
        exposedEnds[0] = Long.MAX_VALUE;
        snapshotStarts[1] = Long.MIN_VALUE;
        snapshotEnds[1] = Long.MAX_VALUE;
        assertRanges(set, -10, -2, 4, 9);
        assertRanges(snapshot, -10, -2, 4, 9);

        set.add(-2, 4);
        assertRanges(set, -10, 9);
        assertRanges(snapshot, -10, -2, 4, 9);
        snapshot.remove(-8, 6);
        assertRanges(snapshot, -10, -8, 6, 9);
        assertRanges(set, -10, 9);
        set.clear();
        assertRanges(set);
        assertRanges(snapshot, -10, -8, 6, 9);
    }

    @Test
    void signedExtremeTouchesPreserveExclusiveMaximumAndExactCardinality() {
        M3LongRangeSet set = ranges(Long.MIN_VALUE, Long.MIN_VALUE + 1,
                -1, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE);
        set.add(Long.MIN_VALUE + 1, -1);
        assertRanges(set, Long.MIN_VALUE, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE);
        set.add(1, Long.MAX_VALUE - 1);
        assertRanges(set, Long.MIN_VALUE, Long.MAX_VALUE);
        assertTrue(set.contains(Long.MIN_VALUE));
        assertTrue(set.contains(Long.MAX_VALUE - 1));
        assertFalse(set.contains(Long.MAX_VALUE));
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), set.cardinality());
        assertFalse(set.intersects(Long.MAX_VALUE, Long.MAX_VALUE));
        set.remove(-1, 1);
        assertRanges(set, Long.MIN_VALUE, -1, 1, Long.MAX_VALUE);
        assertFalse(set.contains(0));
        set.add(-1, 1);
        assertRanges(set, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static M3LongRangeSet ranges(long... coordinates) {
        M3LongRangeSet set = new M3LongRangeSet(coordinates.length / 2);
        for (int i = 0; i < coordinates.length; i += 2) {
            set.add(coordinates[i], coordinates[i + 1]);
        }
        return set;
    }

    private static void assertRanges(M3LongRangeSet set, long... coordinates) {
        int count = coordinates.length / 2;
        long[] starts = new long[count];
        long[] ends = new long[count];
        BigInteger cardinality = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            starts[i] = coordinates[2 * i];
            ends[i] = coordinates[2 * i + 1];
            cardinality = cardinality.add(BigInteger.valueOf(ends[i])
                    .subtract(BigInteger.valueOf(starts[i])));
        }
        set.verifyInvariants();
        assertEquals(count, set.rangeCount());
        assertEquals(count == 0, set.isEmpty());
        assertTrue(set.capacity() >= count);
        assertArrayEquals(starts, set.startsToArray());
        assertArrayEquals(ends, set.endsToArray());
        assertEquals(cardinality, set.cardinality());
    }

    private static void assertCoverage(M3LongRangeSet set, BitSet expected,
            int offset, int cells) {
        List<Long> endpoints = new ArrayList<>();
        int cursor = 0;
        while (cursor < cells) {
            if (!expected.get(cursor)) {
                cursor++;
                continue;
            }
            endpoints.add((long) cursor - offset);
            do {
                cursor++;
            } while (cursor < cells && expected.get(cursor));
            endpoints.add((long) cursor - offset);
        }
        long[] coordinates = new long[endpoints.size()];
        for (int i = 0; i < coordinates.length; i++) coordinates[i] = endpoints.get(i);
        assertRanges(set, coordinates);
        for (int point = -offset; point <= cells - offset; point++) {
            assertEquals(expected.get(point + offset), set.contains(point));
        }
    }
}
