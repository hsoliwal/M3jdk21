// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent endpoint-grid and callback oracles for the shared lower-bound add path. */
final class M3LongRangeBoundarySearchTest {
    private static final long[] COORDINATES = {
            Long.MIN_VALUE, Long.MIN_VALUE + 1, -100, -2, -1, 0,
            1, 7, 100, Long.MAX_VALUE - 1, Long.MAX_VALUE
    };

    @Test
    void touchingIntervalsCoalesceAcrossTheFullSignedDomain() {
        M3LongRangeSet set = new M3LongRangeSet(64);
        set.add(Long.MIN_VALUE, -5);
        set.add(0, 5);
        set.add(10, Long.MAX_VALUE);
        set.add(-5, 0);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 10}, set.startsToArray());
        assertArrayEquals(new long[] {5, Long.MAX_VALUE}, set.endsToArray());
        set.add(5, 10);
        assertArrayEquals(new long[] {Long.MIN_VALUE}, set.startsToArray());
        assertArrayEquals(new long[] {Long.MAX_VALUE}, set.endsToArray());
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), set.cardinality());
        assertFalse(set.contains(Long.MAX_VALUE));
        set.verifyInvariants();
    }

    @Test
    void searchUsesOccupiedPrefixAfterRemovalAndClear() {
        M3LongRangeSet set = new M3LongRangeSet(128);
        set.add(-100, -90);
        set.add(-70, -60);
        set.add(-40, -30);
        set.remove(-70, -30);
        set.add(-85, -80);
        assertArrayEquals(new long[] {-100, -85}, set.startsToArray());
        assertArrayEquals(new long[] {-90, -80}, set.endsToArray());
        set.add(-90, -85);
        assertArrayEquals(new long[] {-100}, set.startsToArray());
        assertArrayEquals(new long[] {-80}, set.endsToArray());
        set.clear();
        set.add(Long.MIN_VALUE, Long.MIN_VALUE + 1);
        set.add(Long.MAX_VALUE - 1, Long.MAX_VALUE);
        assertArrayEquals(new long[] {Long.MIN_VALUE, Long.MAX_VALUE - 1}, set.startsToArray());
        assertArrayEquals(new long[] {Long.MIN_VALUE + 1, Long.MAX_VALUE}, set.endsToArray());
        assertEquals(128, set.capacity());
        set.verifyInvariants();
    }

    @Test
    void idempotentAddsPreserveCallbackRevisionWhileChangesRemainFailFast() {
        M3LongRangeSet set = new M3LongRangeSet();
        set.add(-20, -10);
        set.add(0, 10);
        set.add(20, 30);
        assertDoesNotThrow(() -> set.forEachRange((from, to) -> {
            set.add(from, to);
            set.add(from, from);
        }));
        assertThrows(ConcurrentModificationException.class,
                () -> set.forEachRange((from, to) -> set.add(-10, 0)));
        assertArrayEquals(new long[] {-20, 20}, set.startsToArray());
        assertArrayEquals(new long[] {10, 30}, set.endsToArray());
        set.verifyInvariants();
    }

    @Test
    void emptyAndReversedInputsRetainTheReceiver() {
        M3LongRangeSet set = new M3LongRangeSet(32);
        set.add(-10, 10);
        set.add(Long.MIN_VALUE, Long.MIN_VALUE);
        set.add(Long.MAX_VALUE, Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> set.add(Long.MAX_VALUE, Long.MIN_VALUE));
        assertArrayEquals(new long[] {-10}, set.startsToArray());
        assertArrayEquals(new long[] {10}, set.endsToArray());
        assertEquals(32, set.capacity());
        set.verifyInvariants();
    }

    @Test
    void mixedEditsMatchIndependentNonuniformEndpointCells() {
        Random random = new Random(0x4d334c4f574552L);
        M3LongRangeSet set = new M3LongRangeSet(37);
        boolean[] cells = new boolean[COORDINATES.length - 1];
        for (int operation = 0; operation < 4_096; operation++) {
            int a = random.nextInt(COORDINATES.length);
            int b = random.nextInt(COORDINATES.length);
            int from = Math.min(a, b);
            int to = Math.max(a, b);
            boolean insert = random.nextBoolean();
            if (insert) set.add(COORDINATES[from], COORDINATES[to]);
            else set.remove(COORDINATES[from], COORDINATES[to]);
            Arrays.fill(cells, from, to, insert);
            assertGrid(set, cells);
        }
    }

    private static void assertGrid(M3LongRangeSet set, boolean[] cells) {
        long[] starts = new long[cells.length];
        long[] ends = new long[cells.length];
        int size = 0;
        BigInteger cardinality = BigInteger.ZERO;
        for (int cell = 0; cell < cells.length; cell++) {
            assertEquals(cells[cell], set.contains(COORDINATES[cell]), "endpoint membership");
            if (!cells[cell]) continue;
            cardinality = cardinality.add(BigInteger.valueOf(COORDINATES[cell + 1])
                    .subtract(BigInteger.valueOf(COORDINATES[cell])));
            if (cell == 0 || !cells[cell - 1]) starts[size] = COORDINATES[cell];
            if (cell + 1 == cells.length || !cells[cell + 1]) ends[size++] = COORDINATES[cell + 1];
        }
        assertArrayEquals(Arrays.copyOf(starts, size), set.startsToArray());
        assertArrayEquals(Arrays.copyOf(ends, size), set.endsToArray());
        assertEquals(size, set.rangeCount());
        assertEquals(cardinality, set.cardinality());
        assertFalse(set.contains(Long.MAX_VALUE));
        set.verifyInvariants();
    }
}
