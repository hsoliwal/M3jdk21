// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Current-receiver regression for original scaffold identity and shared lower-bound kernel. */
final class M3FrozenLongSetCurrentContractTest {
    @Test
    void keepsTheOriginalScaffoldDescriptorWhileReceivingFunctionalBehavior() {
        assertEquals("M3FrozenLongSet|FROZEN_LONG_SET|LONG|FROZEN|long[] valueLane",
                M3FrozenLongSet.scaffoldDescriptor());
        M3FrozenLongSet empty = new M3FrozenLongSet();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertThrows(NoSuchElementException.class, empty::first);
        assertThrows(NoSuchElementException.class, empty::last);
        assertThrows(UnsupportedOperationException.class, () -> empty.add(7L));
        assertThrows(UnsupportedOperationException.class, () -> empty.remove(7L));
        assertArrayEquals(new long[0], empty.toArray());
    }

    @Test
    void sharedLowerBoundMatchesTheSignedUniqueSetContract() {
        SplittableRandom random = new SplittableRandom(0xF20E_2026L);
        for (int round = 0; round < 800; round++) {
            int length = random.nextInt(0, 64);
            long[] input = new long[length];
            for (int i = 0; i < length; i++) {
                int lane = random.nextInt(6);
                input[i] = lane == 0 ? Long.MIN_VALUE
                        : lane == 1 ? Long.MAX_VALUE
                        : lane == 2 ? 0L : lane == 3 ? -1L
                        : lane == 4 ? random.nextLong(-64, 65)
                        : random.nextLong();
            }
            long[] saved = input.clone();
            M3FrozenLongSet set = M3FrozenLongSet.copyOf(input);
            assertArrayEquals(saved, input, "caller lane must remain unchanged");
            TreeSet<Long> oracle = new TreeSet<>();
            for (long value : input) oracle.add(value);
            assertEquals(oracle.size(), set.size());
            long[] selected = set.toArray();
            for (int step = 0; step < 96; step++) {
                long probe = step == 0 ? Long.MIN_VALUE
                        : step == 1 ? Long.MAX_VALUE
                        : step == 2 ? 0L : step == 3 ? -1L
                        : step % 4 == 0 && selected.length > 0
                        ? selected[random.nextInt(selected.length)] : random.nextLong(-70, 71);
                int lower = M3LaneSearch.lowerBound(selected, 0, selected.length, probe);
                int expectedRank = lower < selected.length && selected[lower] == probe ? lower : -1;
                assertEquals(expectedRank, set.rank(probe));
                assertEquals(oracle.contains(probe), set.contains(probe));
                assertEquals(oracle.headSet(probe).size(), lower);
                if (expectedRank >= 0) assertEquals(probe, set.get(expectedRank));
            }
            if (selected.length > 0) {
                assertEquals(selected[0], set.first());
                assertEquals(selected[selected.length - 1], set.last());
            }
            long[] detached = set.toArray();
            if (detached.length > 0) {
                detached[0] = ~detached[0];
                assertArrayEquals(selected, set.toArray());
            }
            assertEquals(Arrays.toString(selected), set.toString());
            assertFalse(set.iterator().hasNext() && selected.length == 0);
        }
    }
}
