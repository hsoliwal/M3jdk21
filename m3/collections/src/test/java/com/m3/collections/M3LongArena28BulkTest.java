/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent list oracle for block-granular bulk append. */
final class M3LongArena28BulkTest {
    @Test
    void preservesSignedValuesAndCrossesPrimitiveBlockBoundaries() {
        M3LongArena28 arena = new M3LongArena28();
        for (int i = 0; i < M3Address28.VALUES_PER_BLOCK - 2; i++) {
            assertEquals(i, arena.append(0L));
        }
        assertEquals(0, arena.allocatedBlockCount());
        long[] input = {Long.MIN_VALUE, 0L, -1L, Long.MAX_VALUE, 0L, 42L};
        int start = M3Address28.VALUES_PER_BLOCK - 2;
        assertEquals(start, arena.appendAll(input));
        input[0] = 77L;
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, -1L, Long.MAX_VALUE, 0L, 42L},
                arena.copyRange(start, start + 6));
        assertEquals(start + 6, arena.size());
        assertEquals(2, arena.allocatedBlockCount());
    }

    @Test
    void allZeroBulkAppendAvoidsPhysicalPageAllocation() {
        M3LongArena28 arena = new M3LongArena28();
        long[] zeros = new long[3 * M3Address28.VALUES_PER_BLOCK + 7];
        assertEquals(0, arena.appendAll(zeros));
        assertEquals(zeros.length, arena.size());
        assertEquals(0, arena.allocatedBlockCount());
        arena.set(4095, -5L);
        assertEquals(1, arena.allocatedBlockCount());
        assertEquals(-5L, arena.get(4095));
        assertEquals(0L, arena.get(4096));
        assertArrayEquals(new long[] {0L, -5L, 0L}, arena.copyRange(4094, 4097));
        assertEquals(zeros.length, arena.appendAll(new long[0]));
    }

    @Test
    void randomizedBulkAndScalarOperationsMatchIndependentReference() {
        M3LongArena28 arena = new M3LongArena28();
        List<Long> reference = new ArrayList<>();
        Random random = new Random(0x4d334152454e4132L);
        for (int step = 0; step < 5_000; step++) {
            int kind = random.nextInt(4);
            if (kind == 0 || reference.isEmpty()) {
                long value = sample(random);
                assertEquals(reference.size(), arena.append(value));
                reference.add(value);
            } else if (kind == 1) {
                int length = random.nextInt(65);
                long[] input = new long[length];
                for (int i = 0; i < input.length; i++) {
                    input[i] = sample(random);
                }
                assertEquals(reference.size(), arena.appendAll(input));
                for (long value : input) reference.add(value);
                Arrays.fill(input, 7L); // Incoming array must be detached.
            } else if (kind == 2) {
                int index = random.nextInt(reference.size());
                long value = sample(random);
                arena.set(index, value);
                reference.set(index, value);
            } else {
                int from = random.nextInt(reference.size() + 1);
                int to = from + random.nextInt(reference.size() - from + 1);
                assertArrayEquals(expected(reference, from, to), arena.copyRange(from, to));
            }
            assertEquals(reference.size(), arena.size());
            if ((step & 127) == 0) {
                assertArrayEquals(expected(reference, 0, reference.size()),
                        arena.copyRange(0, arena.size()));
            }
        }
        assertArrayEquals(expected(reference, 0, reference.size()),
                arena.copyRange(0, arena.size()));
    }

    @Test
    void rejectsInvalidRequestsWithoutChangingLogicalSize() {
        M3LongArena28 arena = new M3LongArena28();
        assertEquals(0, arena.appendAll(new long[0]));
        assertThrows(NullPointerException.class, () -> arena.appendAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> arena.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> arena.set(-1, 9L));
        assertThrows(IndexOutOfBoundsException.class, () -> arena.copyRange(1, 0));
        assertEquals(0, arena.size());
        assertEquals(0, arena.allocatedBlockCount());
    }

    private static long sample(Random random) {
        return switch (random.nextInt(7)) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            default -> random.nextLong();
        };
    }

    private static long[] expected(List<Long> reference, int from, int to) {
        long[] values = new long[to - from];
        for (int i = 0; i < values.length; i++) {
            values[i] = reference.get(from + i);
        }
        return values;
    }
}
