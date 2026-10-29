/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent pair-level oracle for a stable aligned-prefix compaction refinement. */
final class M3StateMaskArraysPrefixCompactionTest {
    private record Pair(int slot, byte state) { }

    private static void verify(int[] slots, byte[] states, int from, int to,
                               int required, int forbidden) {
        int[] expectedSlots = slots.clone();
        byte[] expectedStates = states.clone();
        List<Pair> kept = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int value = states[i] & 0xff;
            if ((value & required) == required && (value & forbidden) == 0) {
                kept.add(new Pair(slots[i], states[i]));
            }
        }
        int expectedEnd = from + kept.size();
        for (int i = from; i < to; i++) {
            if (i < expectedEnd) {
                Pair pair = kept.get(i - from);
                expectedSlots[i] = pair.slot();
                expectedStates[i] = pair.state();
            } else {
                expectedSlots[i] = 0;
                expectedStates[i] = 0;
            }
        }
        int actualEnd = M3StateMaskArrays.compactWindow(
                slots, states, from, to, required, forbidden);
        assertEquals(expectedEnd, actualEnd);
        assertArrayEquals(expectedSlots, slots);
        assertArrayEquals(expectedStates, states);
    }

    @Test
    void retainsAlreadyCompactedPrefixAndUnchangedWindow() {
        verify(new int[] {91, 0, -7, 8, 99}, new byte[] {9, 1, 5, 1, 7},
                1, 4, 1, 2);
        verify(new int[] {8, 0, 8, -3, 0}, new byte[] {1, 1, 1, 1, 1},
                0, 5, 1, 0);
        verify(new int[] {8, 7, 6}, new byte[] {1, 1, 1}, 0, 3, 0, 0);
        verify(new int[0], new byte[0], 0, 0, 0, 0);
    }

    @Test
    void preservesOrderAndClearsOnlyTheRejectedWindowTail() {
        verify(new int[] {100, 0, -3, 0, 10, -7, 100},
                new byte[] {1, 1, 1, 0, 1, 1, 1}, 1, 6, 1, 2);
        verify(new int[] {2, 2, 2, 2, 2},
                new byte[] {0, 0, 0, 0, 0}, 0, 5, 1, 0);
        verify(new int[] {42, 41, 40, 39, 38, 37},
                new byte[] {1, 1, 1, 1, 1, 0}, 0, 6, 1, 0);
        verify(new int[] {0, Integer.MIN_VALUE, Integer.MAX_VALUE},
                new byte[] {(byte) 0xff, (byte) 0x80, 0}, 0, 3, 0x80, 0x01);
    }

    @Test
    void validationIsBeforeMutation() {
        int[] slots = {7, 8, 9};
        byte[] states = {1, 2, 3};
        assertThrows(IllegalArgumentException.class, () ->
                M3StateMaskArrays.compactWindow(slots, new byte[2], 0, 2, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () ->
                M3StateMaskArrays.compactWindow(slots, states, -1, 3, 1, 0));
        assertThrows(IllegalArgumentException.class, () ->
                M3StateMaskArrays.compactWindow(slots, states, 0, 3, 1, 1));
        assertThrows(IllegalArgumentException.class, () ->
                M3StateMaskArrays.compactWindow(slots, states, 0, 3, 256, 0));
        assertArrayEquals(new int[] {7, 8, 9}, slots);
        assertArrayEquals(new byte[] {1, 2, 3}, states);
    }

    @Test
    void independentRandomizedWindowOracle() {
        Random random = new Random(0x4D33505245464958L);
        for (int trial = 0; trial < 10_000; trial++) {
            int length = random.nextInt(257);
            int[] slots = new int[length];
            byte[] states = new byte[length];
            for (int i = 0; i < length; i++) {
                slots[i] = random.nextInt(37) - 18;
                states[i] = (byte) random.nextInt(256);
            }
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int required = random.nextInt(256);
            int forbidden = random.nextInt(256) & ~required;
            verify(slots, states, from, to, required, forbidden);
        }
    }
}
