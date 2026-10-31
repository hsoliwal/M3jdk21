/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LaneAlgorithms} against boxed JDK oracles
 * ({@code Collections.rotate}/{@code reverse} over {@code List<Long>}, stream filters, sorted
 * copies and brute-force absence search); this is not JDK replacement proof. Ranges, distances
 * (including {@code Integer.MIN_VALUE}/{@code MAX_VALUE}) and mask bits are randomised and the
 * untouched remainder of every lane and mask is checked after each step.
 */
final class M3LaneAlgorithmsTest {
    private static final int STEPS = 40_000;

    private static long[] randomLane(Random random, int length, int spread) {
        long[] lane = new long[length];
        for (int i = 0; i < length; i++) {
            lane[i] = switch (random.nextInt(9)) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> random.nextLong();
                default -> random.nextInt(spread) - spread / 3;
            };
        }
        return lane;
    }

    private static int randomDistance(Random random, int step) {
        return switch (step % 7) {
            case 0 -> Integer.MIN_VALUE;
            case 1 -> Integer.MAX_VALUE;
            case 2 -> 0;
            default -> random.nextInt(201) - 100;
        };
    }

    private static List<Long> boxed(long[] lane, int from, int to) {
        List<Long> list = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            list.add(lane[i]);
        }
        return list;
    }

    private static void assertOutsideUntouched(long[] before, long[] after, int from, int to) {
        assertArrayEquals(Arrays.copyOfRange(before, 0, from), Arrays.copyOfRange(after, 0, from));
        assertArrayEquals(Arrays.copyOfRange(before, to, before.length), Arrays.copyOfRange(after, to, after.length));
    }

    private static boolean bit(long[] words, int index) {
        return (words[index >>> 6] & (1L << index)) != 0;
    }

    private static long[] randomWords(Random random, int words) {
        long[] result = new long[words];
        for (int i = 0; i < words; i++) {
            result[i] = random.nextLong();
        }
        return result;
    }

    @Test
    void longLanesMatchJdkOraclesAcrossRandomRanges() {
        Random random = new Random(0x4c414e454c4f4e47L);
        for (int step = 0; step < STEPS; step++) {
            int length = random.nextInt(41);
            long[] before = randomLane(random, length, 24);
            long[] lane = before.clone();
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int distance = randomDistance(random, step);
            switch (random.nextInt(7)) {
                case 0, 1 -> {
                    List<Long> expected = boxed(before, from, to);
                    Collections.rotate(expected, distance);
                    if (random.nextBoolean()) {
                        M3LaneAlgorithms.rotate(lane, from, to, distance);
                    } else {
                        M3LaneAlgorithms.rotateByCycles(lane, from, to, distance);
                    }
                    assertEquals(expected, boxed(lane, from, to));
                }
                case 2 -> {
                    List<Long> expected = boxed(before, from, to);
                    Collections.reverse(expected);
                    M3LaneAlgorithms.reverse(lane, from, to);
                    assertEquals(expected, boxed(lane, from, to));
                }
                case 3 -> {
                    long pivot = to > from ? before[from + random.nextInt(to - from)] : random.nextLong();
                    long bounds = M3LaneAlgorithms.partition3(lane, from, to, pivot);
                    int low = M3LaneAlgorithms.lowBound(bounds);
                    int high = M3LaneAlgorithms.highBound(bounds);
                    assertTrue(from <= low && low <= high && high <= to);
                    for (int i = from; i < low; i++) {
                        assertTrue(lane[i] < pivot);
                    }
                    for (int i = low; i < high; i++) {
                        assertEquals(pivot, lane[i]);
                    }
                    for (int i = high; i < to; i++) {
                        assertTrue(lane[i] > pivot);
                    }
                    long[] sortedBefore = Arrays.copyOfRange(before, from, to);
                    long[] sortedAfter = Arrays.copyOfRange(lane, from, to);
                    Arrays.sort(sortedBefore);
                    Arrays.sort(sortedAfter);
                    assertArrayEquals(sortedBefore, sortedAfter);
                }
                case 4 -> {
                    long[] keepBefore = randomWords(random, 1 + (length >>> 6));
                    long[] keep = keepBefore.clone();
                    List<Long> expected = new ArrayList<>();
                    for (int i = from; i < to; i++) {
                        if (bit(keepBefore, i)) {
                            expected.add(before[i]);
                        }
                    }
                    int count = M3LaneAlgorithms.compact(lane, keep, from, to);
                    assertEquals(expected.size(), count);
                    assertEquals(expected, boxed(lane, from, from + count));
                    for (int i = 0; i < keep.length << 6; i++) {
                        boolean expectedBit = i < from || i >= to ? bit(keepBefore, i) : i < from + count;
                        assertEquals(expectedBit, bit(keep, i), "bit " + i);
                    }
                }
                case 5 -> {
                    long base = switch (random.nextInt(4)) {
                        case 0 -> 0L;
                        case 1 -> 1L;
                        case 2 -> Long.MIN_VALUE;
                        default -> random.nextInt(11) - 5;
                    };
                    TreeSet<Long> present = new TreeSet<>(boxed(before, from, to));
                    long expected = base;
                    while (present.contains(expected)) {
                        expected++;
                    }
                    assertEquals(expected, M3LaneAlgorithms.firstAbsent(lane, from, to, base));
                    long[] sortedBefore = Arrays.copyOfRange(before, from, to);
                    long[] sortedAfter = Arrays.copyOfRange(lane, from, to);
                    Arrays.sort(sortedBefore);
                    Arrays.sort(sortedAfter);
                    assertArrayEquals(sortedBefore, sortedAfter);
                }
                default -> {
                    long[] wordsBefore = randomWords(random, 1 + random.nextInt(3));
                    long[] words = wordsBefore.clone();
                    int bits = words.length << 6;
                    int bitFrom = random.nextInt(bits + 1);
                    int bitTo = bitFrom + random.nextInt(bits - bitFrom + 1);
                    List<Boolean> expected = new ArrayList<>();
                    for (int i = bitFrom; i < bitTo; i++) {
                        expected.add(bit(wordsBefore, i));
                    }
                    if (random.nextBoolean()) {
                        Collections.rotate(expected, distance);
                        M3LaneAlgorithms.rotateBits(words, bitFrom, bitTo, distance);
                    } else {
                        Collections.reverse(expected);
                        M3LaneAlgorithms.reverseBits(words, bitFrom, bitTo);
                    }
                    for (int i = 0; i < bits; i++) {
                        boolean expectedBit = i < bitFrom || i >= bitTo ? bit(wordsBefore, i) : expected.get(i - bitFrom);
                        assertEquals(expectedBit, bit(words, i), "bit " + i);
                    }
                }
            }
            assertOutsideUntouched(before, lane, from, to);
        }
    }

    @Test
    void intLanesMatchTheLongLaneResults() {
        Random random = new Random(0x4c414e45494e54L);
        for (int step = 0; step < 20_000; step++) {
            int length = random.nextInt(41);
            int[] before = new int[length];
            for (int i = 0; i < length; i++) {
                before[i] = switch (random.nextInt(9)) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(24) - 8;
                };
            }
            long[] longBefore = Arrays.stream(before).asLongStream().toArray();
            int[] lane = before.clone();
            long[] longLane = longBefore.clone();
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int distance = randomDistance(random, step);
            switch (random.nextInt(6)) {
                case 0 -> {
                    M3LaneAlgorithms.rotate(lane, from, to, distance);
                    M3LaneAlgorithms.rotate(longLane, from, to, distance);
                }
                case 1 -> {
                    M3LaneAlgorithms.rotateByCycles(lane, from, to, distance);
                    M3LaneAlgorithms.rotateByCycles(longLane, from, to, distance);
                }
                case 2 -> {
                    M3LaneAlgorithms.reverse(lane, from, to);
                    M3LaneAlgorithms.reverse(longLane, from, to);
                }
                case 3 -> {
                    int pivot = to > from ? before[from + random.nextInt(to - from)] : random.nextInt();
                    assertEquals(M3LaneAlgorithms.partition3(longLane, from, to, pivot),
                            M3LaneAlgorithms.partition3(lane, from, to, pivot));
                }
                case 4 -> {
                    long[] keep = randomWords(random, 1 + (length >>> 6));
                    long[] keepCopy = keep.clone();
                    assertEquals(M3LaneAlgorithms.compact(longLane, keepCopy, from, to),
                            M3LaneAlgorithms.compact(lane, keep, from, to));
                    assertArrayEquals(keepCopy, keep);
                }
                default -> {
                    int base = random.nextInt(5) - 1;
                    assertEquals(M3LaneAlgorithms.firstAbsent(longLane, from, to, base),
                            M3LaneAlgorithms.firstAbsent(lane, from, to, base));
                }
            }
            assertArrayEquals(longLane, Arrays.stream(lane).asLongStream().toArray());
        }
    }

    @Test
    void knownVectorsEdgeRangesAndShape() {
        int[] colors = {2, 0, 2, 1, 1, 0};
        long bounds = M3LaneAlgorithms.partition3(colors, 0, colors.length, 1);
        assertArrayEquals(new int[] {0, 0, 1, 1, 2, 2}, colors);
        assertEquals(2, M3LaneAlgorithms.lowBound(bounds));
        assertEquals(4, M3LaneAlgorithms.highBound(bounds));
        assertEquals(0L, M3LaneAlgorithms.partition3(new int[0], 0, 0, 0));
        assertEquals(3, M3LaneAlgorithms.firstMissingPositive(new int[] {1, 2, 0}, 0, 3));
        assertEquals(2, M3LaneAlgorithms.firstMissingPositive(new int[] {3, 4, -1, 1}, 0, 4));
        assertEquals(1, M3LaneAlgorithms.firstMissingPositive(new int[] {7, 8, 9, 11, 12}, 0, 5));
        assertEquals(2, M3LaneAlgorithms.missingNumber(new int[] {3, 0, 1}, 0, 3));
        assertEquals(2, M3LaneAlgorithms.missingNumber(new int[] {0, 1}, 0, 2));
        assertEquals(8, M3LaneAlgorithms.missingNumber(new int[] {9, 6, 4, 2, 3, 5, 7, 0, 1}, 0, 9));
        int[] placed = {5, 4, 3, 2, 1, 9, -7, 2};
        M3LaneAlgorithms.cyclicPlace(placed, 1, 6, 1);
        assertArrayEquals(new int[] {5, 1, 2, 3, 4, 9, -7, 2}, placed);
        long[] rotated = {1, 2, 3, 4, 5};
        M3LaneAlgorithms.rotate(rotated, 0, 5, 2);
        assertArrayEquals(new long[] {4, 5, 1, 2, 3}, rotated);
        M3LaneAlgorithms.rotateByCycles(rotated, 0, 5, -2);
        assertArrayEquals(new long[] {1, 2, 3, 4, 5}, rotated);
        M3LaneAlgorithms.rotate(rotated, 0, 1, Integer.MIN_VALUE);
        M3LaneAlgorithms.rotate(rotated, 2, 2, Integer.MAX_VALUE);
        assertArrayEquals(new long[] {1, 2, 3, 4, 5}, rotated);
        long[] words = new long[2];
        M3LaneAlgorithms.reverseBits(words, 0, 128);
        M3LaneAlgorithms.rotateBits(words, 0, 128, 77);
        assertArrayEquals(new long[2], words);
        assertEquals(0, M3LaneAlgorithms.compact(new long[0], new long[1], 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.reverse(new long[3], -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.reverse(new int[3], 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.rotate(new long[3], 0, 4, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.rotateBits(new long[1], 0, 65, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.compact(new long[4], new long[1], 0, 65));
        assertThrows(IndexOutOfBoundsException.class, () -> M3LaneAlgorithms.partition3(new int[2], 1, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> M3LaneAlgorithms.firstAbsent(new int[2], 0, 2, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> M3LaneAlgorithms.firstAbsent(new long[2], 0, 2, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, M3LaneAlgorithms.firstAbsent(new int[] {Integer.MAX_VALUE - 1}, 0, 1, Integer.MAX_VALUE - 1));
        assertTrue(Modifier.isFinal(M3LaneAlgorithms.class.getModifiers()));
        assertEquals(1, M3LaneAlgorithms.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPrivate(M3LaneAlgorithms.class.getDeclaredConstructors()[0].getModifiers()));
        assertEquals(0, Arrays.stream(M3LaneAlgorithms.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).count());
    }
}
