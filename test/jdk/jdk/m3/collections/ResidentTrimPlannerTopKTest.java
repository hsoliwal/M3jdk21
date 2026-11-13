// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LazyElementFlags;

/** Retains source module contract and runs the independent 499k-case oracle. */
//FLLDCIM: RELOCATE - donor blob 7cd338c319a8 at fd9a233680b4; recipe make/m3/collections/recipe/transient-facade-20261106/relocate.py v4
class ResidentTrimPlannerTopKTest {
    @Test
    void fullSortedOracleAndPinnedIdentity() throws Exception {
        ResidentTrimPlannerTopKSelfTest.main(new String[0]);
    }

    @Test
    void exactExistingOwnerContract() {
        short pin = LazyElementFlags.PINNED;
        assertArrayEquals(new int[] {2, 3, 0},
                ResidentTrimPlanner.oldestSlots(new short[] {0, pin, 0, 0, pin},
                        new long[] {5, 1, 2, 2, 0}, 5, 3));
        assertArrayEquals(new int[] {1},
                ResidentTrimPlanner.oldestSlots(new short[5],
                        new long[] {10, 1, 1, 5, 8}, 5, 1));
        assertEquals(0, ResidentTrimPlanner.oldestSlots(new short[] {pin},
                new long[] {Long.MIN_VALUE}, 1, Integer.MAX_VALUE).length);
        assertThrows(IllegalArgumentException.class,
                () -> ResidentTrimPlanner.oldestSlots(new short[0], new long[0], 1, 0));
    }
}
