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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Capacity policy of {@link SlotArrays#grow(int)}: 8 minimum, then 1.5x + 1, capped at
 * {@code Integer.MAX_VALUE - 8}, and strictly increasing below the cap. Owners call it in
 * {@code while (cap < required) cap = grow(cap)} loops, so a result that does not increase is a hang.
 */
final class SlotArraysGrowTest {
    private static final int CAP = Integer.MAX_VALUE - 8;

    @Test
    void smallCapacitiesFollowTheGeometricPolicy() {
        assertEquals(8, SlotArrays.grow(0));
        assertEquals(8, SlotArrays.grow(4));
        assertEquals(13, SlotArrays.grow(8));
        assertEquals(151, SlotArrays.grow(100));
    }

    /** {@code old + (old >> 1)} exceeds int range here; the result must still grow, not wrap to the minimum. */
    @Test
    void largeCapacitiesDoNotWrapAround() {
        assertEquals(CAP, SlotArrays.grow(1_500_000_000));
        assertEquals(CAP, SlotArrays.grow(1_431_655_766));
        assertEquals(CAP, SlotArrays.grow(CAP - 1));
        assertEquals(2_147_483_473, SlotArrays.grow(1_431_655_648));
    }

    @Test
    void growthIsStrictlyIncreasingUpToTheCap() {
        for (long old = 0; old < CAP; old = old < 64 ? old + 1 : old + old / 7 + 1) {
            int next = SlotArrays.grow((int) old);
            assertTrue(next > old && next <= CAP, "grow(" + old + ") = " + next);
        }
        assertThrows(OutOfMemoryError.class, () -> SlotArrays.grow(CAP));
        assertThrows(OutOfMemoryError.class, () -> SlotArrays.grow(Integer.MAX_VALUE));
    }

    /** The owners' sizing loop must reach any legal requirement in a bounded number of steps. */
    @Test
    void sizingLoopTerminatesForTheLargestRequirement() {
        int cap = 0;
        int steps = 0;
        while (cap < CAP && steps < 64) {
            cap = SlotArrays.grow(cap);
            steps++;
        }
        assertEquals(CAP, cap, "sizing loop stalled after " + steps + " steps");
        assertTrue(steps < 64);
    }

    @Test
    void indexDoublingStopsAtTheLargestPowerOfTwo() {
        assertEquals(1 << 30, SlotArrays.doubled(1 << 29));
        assertThrows(OutOfMemoryError.class, () -> SlotArrays.doubled(1 << 30));
    }
}
