// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent observable-output regression for one-pass primitive snapshot indexing. */
public final class M3ListDiffSingleProbeTest {
    private static final long[] UNIVERSE = universe();

    private static long[] universe() {
        long[] values = new long[48];
        values[0] = Long.MIN_VALUE;
        values[1] = Long.MAX_VALUE;
        values[2] = 0;
        values[3] = -1;
        for (int i = 4; i < values.length; i++) {
            values[i] = (i & 1) == 0 ? i : -i;
        }
        return values;
    }

    private static long[] sample(Random random) {
        long[] values = UNIVERSE.clone();
        for (int i = values.length - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            long old = values[i];
            values[i] = values[other];
            values[other] = old;
        }
        return Arrays.copyOf(values, random.nextInt(values.length + 1));
    }

    @Test
    void signedExtremaZeroAndReordersRoundTrip() {
        long[] before = {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE};
        long[] after = {0, Long.MAX_VALUE, Long.MIN_VALUE, -1};
        M3Patch patch = M3ListDiff.diff(before, after);
        assertArrayEquals(after, patch.apply(before, after));
        assertEquals(before.length, patch.keptCount() + patch.removedCount());
        assertEquals(after.length, patch.keptCount() + patch.insertedCount());
        assertArrayEquals(new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE}, before);
    }

    @Test
    void randomUniqueSnapshotsPreservePatchContract() {
        Random random = new Random(0x6d335eedL);
        for (int caseNumber = 0; caseNumber < 5000; caseNumber++) {
            long[] before = sample(random);
            long[] after = sample(random);
            long[] beforeCopy = before.clone();
            long[] afterCopy = after.clone();
            M3Patch patch = M3ListDiff.diff(before, after);
            assertArrayEquals(after, patch.apply(before, after), "case " + caseNumber);
            assertEquals(before.length, patch.keptCount() + patch.removedCount());
            assertEquals(after.length, patch.keptCount() + patch.insertedCount());
            assertTrue(patch.moveCount() <= patch.removedCount());
            assertTrue(patch.moveCount() <= patch.insertedCount());
            assertArrayEquals(beforeCopy, before);
            assertArrayEquals(afterCopy, after);
            for (int move = 0; move < patch.moveCount(); move++) {
                assertEquals(before[patch.moveFrom(move)], after[patch.moveTo(move)]);
            }
        }
    }

    @Test
    void afterDuplicatePrecedesBeforeDuplicate() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> M3ListDiff.diff(new long[] {7, 7}, new long[] {3, 3}));
        assertEquals("duplicate key in after: 3", exception.getMessage());
    }

    @Test
    void firstRepeatedValueAndSideRemainExact() {
        IllegalArgumentException after = assertThrows(IllegalArgumentException.class,
                () -> M3ListDiff.diff(new long[] {1}, new long[] {0, Long.MIN_VALUE, 0}));
        assertEquals("duplicate key in after: 0", after.getMessage());
        IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                () -> M3ListDiff.diff(new long[] {Long.MAX_VALUE, 5, Long.MAX_VALUE},
                        new long[] {5}));
        assertEquals("duplicate key in before: " + Long.MAX_VALUE, before.getMessage());
    }

    @Test
    void identicalAndEmptySnapshotsRequireNoEdits() {
        for (long[] values : new long[][] {{}, {0}, {Long.MIN_VALUE, 3, Long.MAX_VALUE}}) {
            M3Patch patch = M3ListDiff.diff(values, values.clone());
            assertTrue(patch.isEmpty());
            assertArrayEquals(values, patch.apply(values, values));
        }
    }
}
