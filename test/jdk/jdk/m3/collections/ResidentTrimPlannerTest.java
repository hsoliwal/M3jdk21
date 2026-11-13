// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LazyElementFlags;

//FLLDCIM: RELOCATE - donor blob 64273bba6a26 at fd9a233680b4; recipe make/m3/collections/recipe/transient-facade-20261106/relocate.py v4
class ResidentTrimPlannerTest {
    @Test void choosesOldestAndNeverPinned() {
        short pin = LazyElementFlags.PINNED;
        int[] selected = ResidentTrimPlanner.oldestSlots(
                new short[] {0, pin, 0, 0, pin},
                new long[] {5, 1, 2, 2, 0}, 5, 3);
        assertArrayEquals(new int[]{2, 3, 0}, selected);
    }

    @Test void tiesResolveByStableSlotAndBudgetClamps() {
        int[] selected = ResidentTrimPlanner.oldestSlots(
                new short[5], new long[] {10, 1, 1, 5, 8}, 5, 1);
        assertArrayEquals(new int[]{1}, selected);
        assertArrayEquals(new int[]{0, 1},
                ResidentTrimPlanner.oldestSlots(new short[2], new long[2], 2, 100));
    }

    @Test void rejectsBadGeometryAndPreservesCallerArrays() {
        short[] flags = {LazyElementFlags.PINNED, 0};
        long[] last = {1, 2};
        ResidentTrimPlanner.oldestSlots(flags, last, 2, 1);
        assertArrayEquals(new short[] {LazyElementFlags.PINNED, 0}, flags);
        assertArrayEquals(new long[] {1, 2}, last);
        assertThrows(IllegalArgumentException.class,
                () -> ResidentTrimPlanner.oldestSlots(flags, last, 3, 1));
    }
}
