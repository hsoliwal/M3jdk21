// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Retains source module contract and runs the independent 499k-case oracle. */
//FLLDCIM: RELOCATE - donor blob 7cd338c319a8 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class M3ResidentTrimPlannerTopKTest {
    @Test
    void fullSortedOracleAndPinnedIdentity() throws Exception {
        M3ResidentTrimPlannerTopKSelfTest.main(new String[0]);
    }

    @Test
    void exactExistingOwnerContract() {
        short pin = M3LazyElementFlags.PINNED;
        assertArrayEquals(new int[] {2, 3, 0},
                M3ResidentTrimPlanner.oldestSlots(new short[] {0, pin, 0, 0, pin},
                        new long[] {5, 1, 2, 2, 0}, 5, 3));
        assertArrayEquals(new int[] {1},
                M3ResidentTrimPlanner.oldestSlots(new short[5],
                        new long[] {10, 1, 1, 5, 8}, 5, 1));
        assertEquals(0, M3ResidentTrimPlanner.oldestSlots(new short[] {pin},
                new long[] {Long.MIN_VALUE}, 1, Integer.MAX_VALUE).length);
        assertThrows(IllegalArgumentException.class,
                () -> M3ResidentTrimPlanner.oldestSlots(new short[0], new long[0], 1, 0));
    }
}
