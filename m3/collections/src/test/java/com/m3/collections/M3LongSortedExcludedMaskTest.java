// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Differential assertions over the existing M3 dynamic-array/offset/mask owners. */
public final class M3LongSortedExcludedMaskTest {
    @Test void requiredExcludedMasksPreserveSortedSnapshotContracts() {
        long[] values = {Long.MIN_VALUE, -1L, 0L, 1L, 5L, Long.MAX_VALUE};
        byte[] flags = {(byte) 0x80, 0x01, 0x03, (byte) 0x81, (byte) 0xff, 0x00};
        M3LongSortedSnapshot admitted = M3LongSortedSnapshot.fromAlignedMasks(values, flags, 1, 2);
        assertArrayEquals(new long[]{-1L, 1L}, admitted.toArray());
        assertEquals(2, admitted.size());
        assertEquals(1, admitted.rank(1L));
        assertEquals(1L, admitted.select(1));
        assertEquals(0, admitted.indexOf(-1));
        assertEquals(-1, admitted.indexOf(0));
        assertEquals(-1L, admitted.first());
        assertEquals(1L, admitted.last());
        assertArrayEquals(new long[]{1}, admitted.tailSet(0).toArray());
        assertArrayEquals(new long[]{-1}, admitted.headSet(1).toArray());
        assertArrayEquals(new long[]{-1}, admitted.subSet(-1, 1).toArray());
        values[1] = 777;
        flags[3] = 0;
        assertArrayEquals(new long[]{-1L, 1L}, admitted.toArray(), "snapshot is detached");
        assertThrows(UnsupportedOperationException.class, () -> admitted.add(17L));
        assertThrows(UnsupportedOperationException.class, () -> admitted.remove(-1L));
    }

    @Test void invalidMasksAndUnsuitableInputsAreRefused() {
        assertArrayEquals(new long[0], M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1, 2}, new byte[]{1, 1}, 1, 1).toArray());
        assertThrows(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1, 2}, new byte[]{1}, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{2, 1}, new byte[]{1, 1}, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1, 1}, new byte[]{1, 1}, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1}, new byte[]{1}, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1}, new byte[]{1}, 0, -1));
        assertArrayEquals(new long[]{1, 2}, M3LongSortedSnapshot.fromAlignedMasks(
                new long[]{1, 2}, new byte[]{1, 1}, 1, 0).toArray(),
                "zero excluded mask retains original required-only behavior");
    }

    @Test void randomizedOracleAgainstSimplePerRowMaskPredicate() {
        Random random = new Random(0x20261021L);
        for (int round = 0; round < 300; round++) {
            int length = random.nextInt(300);
            long[] values = new long[length];
            byte[] states = new byte[length];
            long value = -500L;
            for (int i = 0; i < length; i++) {
                value += 1L + random.nextInt(10);
                values[i] = value;
                states[i] = (byte) random.nextInt(256);
            }
            int required = random.nextInt(256), excluded = random.nextInt(256);
            long[] expected = new long[length];
            int count = 0;
            for (int i = 0; i < length; i++) {
                int bits = states[i] & 0xff;
                if ((bits & required) == required && (bits & excluded) == 0) {
                    expected[count++] = values[i];
                }
            }
            expected = Arrays.copyOf(expected, count);
            M3LongSortedSnapshot selected =
                    M3LongSortedSnapshot.fromAlignedMasks(values, states, required, excluded);
            assertArrayEquals(expected, selected.toArray(), "round " + round);
            assertEquals(count, selected.size(), "round " + round);
            for (int i = 0; i < count; i++) {
                assertEquals(expected[i], selected.select(i), "round " + round + " rank " + i);
                assertEquals(i, selected.indexOf(expected[i]), "round " + round + " index " + i);
            }
        }
    }
}
