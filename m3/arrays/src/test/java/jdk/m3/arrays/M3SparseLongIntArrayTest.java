// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import org.junit.jupiter.api.Test;

/** Independently test each long-index/primitive-storage atom plus seeded oracle. */
final class M3SparseLongIntArrayTest {
    @Test
    void longIndexAndLengthDoNotAllocateDenseArray() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, values.length());
        assertEquals(0, values.get(Long.MAX_VALUE - 1));
        assertEquals(0, values.allocatedBlockCount());
        assertEquals(0, values.set(Long.MAX_VALUE - 1, 41));
        assertEquals(41, values.get(Long.MAX_VALUE - 1));
        assertEquals(1, values.allocatedBlockCount());
        assertEquals(41, values.set(Long.MAX_VALUE - 1, 0));
        assertEquals(0, values.allocatedBlockCount());
        assertThrows(IndexOutOfBoundsException.class, () -> values.get(Long.MAX_VALUE));
    }

    @Test
    void zeroValuesReclaimTheParentOwnedPrimitiveBlock() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(1L << 40);
        long index = 17L << M3SparseLongIntArray.BLOCK_SHIFT;
        assertEquals(0, values.set(index, 0));
        assertEquals(0, values.allocatedBlockCount());
        values.set(index, -8);
        values.set(index + 1, 9);
        assertEquals(1, values.allocatedBlockCount());
        assertEquals(2, values.nonZeroCount());
        values.set(index, 0);
        assertEquals(1, values.allocatedBlockCount());
        values.set(index + 1, 0);
        assertEquals(0, values.allocatedBlockCount());
        assertEquals(0, values.nonZeroCount());
    }

    @Test
    void tombstonesAndGrowthPreserveEveryPrimitiveKeyValue() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(1L << 40);
        for (int i = 0; i < 512; i++) {
            values.set((long) i * M3SparseLongIntArray.BLOCK_SIZE, i + 1);
        }
        assertEquals(512, values.nonZeroCount());
        for (int i = 0; i < 512; i += 2) {
            values.set((long) i * M3SparseLongIntArray.BLOCK_SIZE, 0);
        }
        assertEquals(256, values.allocatedBlockCount());
        for (int i = 512; i < 768; i++) {
            values.set((long) i * M3SparseLongIntArray.BLOCK_SIZE, i + 1);
        }
        for (int i = 0; i < 768; i++) {
            long index = (long) i * M3SparseLongIntArray.BLOCK_SIZE;
            assertEquals(i < 512 && i % 2 == 0 ? 0 : i + 1, values.get(index));
        }
        assertEquals(512, values.nonZeroCount());
    }

    @Test
    void bulkCopyAndGrowingLengthRespectSegmentBoundaries() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(4096);
        assertThrows(IndexOutOfBoundsException.class, () -> values.set(4096, 1));
        values.growTo((long) Integer.MAX_VALUE + 4096);
        int[] input = {-7, 0, 2, 3, 0, 5, 0, Integer.MIN_VALUE};
        values.copyFrom(4092, input, 0, input.length);
        int[] actual = new int[10];
        Arrays.fill(actual, 99);
        values.copyTo(4090, actual, 0, actual.length);
        assertArrayEquals(new int[] {0, 0, -7, 0, 2, 3, 0, 5, 0, Integer.MIN_VALUE}, actual);
        assertEquals(2, values.allocatedBlockCount());
        assertThrows(IndexOutOfBoundsException.class, () -> values.copyTo(values.length(), actual, 0, 1));
    }

    @Test
    void detachedSnapshotsAndCallbackMutationGuards() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(Long.MAX_VALUE);
        long base = (1L << 44) + 10;
        values.set(base, 2);
        M3SparseLongIntArray snapshot = values.snapshot();
        values.set(base, 5);
        assertEquals(2, snapshot.get(base));
        assertEquals(5, values.get(base));
        assertThrows(ConcurrentModificationException.class,
                () -> values.forEachNonZero((index, value) -> values.set(index, value + 1)));
        assertEquals(6, values.get(base));
        values.clear();
        assertEquals(0, values.allocatedBlockCount());
        assertEquals(0, values.get(base));
        assertEquals(2, snapshot.get(base));
        assertTrue(values.length() > Integer.MAX_VALUE);
    }

    @Test
    void nativeOccupancyAuditIsRequiredAndNeverAnImplicitFallback() {
        M3SparseLongIntArray values = new M3SparseLongIntArray(8192);
        values.set(0, 13);
        values.set(4096, -7);
        String path = System.getProperty("m3.arrays.native.path");
        if ((path == null || path.isBlank()) && !M3ArrayNative.isLoaded()) {
            assertThrows(UnsatisfiedLinkError.class, values::verifyNonZeroCountRequiredNative);
        } else {
            assertEquals(2, values.verifyNonZeroCountRequiredNative());
            values.set(0, 0);
            assertEquals(1, values.verifyNonZeroCountRequiredNative());
        }
    }

    @Test
    void deterministicReferenceMapMatchesSparseLanesAcrossLongBoundaries() {
        M3SparseLongIntArraySelfTest.run();
    }
}
