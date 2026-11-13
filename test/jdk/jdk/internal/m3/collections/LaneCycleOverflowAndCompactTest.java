// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Additional original-owner Java21 differential for LeetCode 189 and 27 atoms. */
//FLLDCIM: RELOCATE - donor blob 32e1b1ec051c at d5ad5090d87e; recipe make/m3/collections/qualification/census2-c3-20261106/relocate.py v1
final class LaneCycleOverflowAndCompactTest {
    @Test void wrapSafeCycleStepForHugeLogicalLengths() {
        assertEquals(Integer.MAX_VALUE - 3, LaneAlgorithms.nextCyclicIndex(
                Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        Random random = new Random(0x4d334c414e454a44L);
        for (int trial = 0; trial < 30_000; trial++) {
            int n = 2 + random.nextInt(Integer.MAX_VALUE - 2);
            int shift = 1 + random.nextInt(n - 1), index = random.nextInt(n);
            int expected = (int) (((long) index + shift) % n);
            assertEquals(expected, LaneAlgorithms.nextCyclicIndex(index, shift, n));
        }
    }

    @Test void bothRotationsMatchCollectionsOnExactWindows() {
        Random random = new Random(0x4d334c414e45524fL);
        for (int trial = 0; trial < 1_024; trial++) {
            int length = random.nextInt(171);
            int[] ints = new int[length];
            long[] longs = new long[length];
            for (int i = 0; i < length; i++) {
                ints[i] = random.nextInt(39) - 19;
                longs[i] = i % 11 == 0 ? Long.MIN_VALUE : random.nextLong();
            }
            int[] expectedInts = ints.clone();
            long[] expectedLongs = longs.clone();
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int distance = switch (trial % 7) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> 0;
                default -> random.nextInt();
            };
            List<Integer> a = new ArrayList<>();
            List<Long> b = new ArrayList<>();
            for (int i = from; i < to; i++) {
                a.add(ints[i]);
                b.add(longs[i]);
            }
            Collections.rotate(a, distance);
            Collections.rotate(b, distance);
            for (int i = from; i < to; i++) {
                expectedInts[i] = a.get(i - from);
                expectedLongs[i] = b.get(i - from);
            }
            LaneAlgorithms.rotateByCycles(ints, from, to, distance);
            LaneAlgorithms.rotateByCycles(longs, from, to, distance);
            assertArrayEquals(expectedInts, ints, "int rotation " + trial);
            assertArrayEquals(expectedLongs, longs, "long rotation " + trial);
        }
    }

    @Test void stableCompactionPreservesValuesOutsideWindowAndAlignedMask() {
        Random random = new Random(0x4d334d41534b434fL);
        for (int trial = 0; trial < 1_024; trial++) {
            int length = random.nextInt(231);
            int[] ints = new int[length], expectedInts = new int[length];
            long[] longs = new long[length], expectedLongs = new long[length];
            for (int i = 0; i < length; i++) {
                ints[i] = random.nextInt(41) - 20;
                longs[i] = random.nextLong();
            }
            System.arraycopy(ints, 0, expectedInts, 0, length);
            System.arraycopy(longs, 0, expectedLongs, 0, length);
            long[] mask = new long[(length + 63) >>> 6];
            for (int j = 0; j < mask.length; j++)
                mask[j] = trial % 3 == 0 ? -1L : trial % 3 == 1 ? 0L : random.nextLong();
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int write = from;
            for (int read = from; read < to; read++) {
                if ((mask[read >>> 6] & (1L << read)) != 0) {
                    expectedInts[write] = ints[read];
                    expectedLongs[write] = longs[read];
                    write++;
                }
            }
            long[] expectedMask = mask.clone();
            for (int i = from; i < to; i++) {
                long bit = 1L << i;
                if (i < write) expectedMask[i >>> 6] |= bit;
                else expectedMask[i >>> 6] &= ~bit;
            }
            long[] intMask = mask.clone(), longMask = mask.clone();
            assertEquals(write - from, LaneAlgorithms.compact(ints, intMask, from, to));
            assertEquals(write - from, LaneAlgorithms.compact(longs, longMask, from, to));
            assertArrayEquals(expectedInts, ints, "int stable compaction " + trial);
            assertArrayEquals(expectedLongs, longs, "long stable compaction " + trial);
            assertArrayEquals(expectedMask, intMask);
            assertArrayEquals(expectedMask, longMask);
        }
    }
}
