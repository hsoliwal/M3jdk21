/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class SegmentedLaneBulkContractTest {
    @Test void longCopiesMatchArraycopyAcrossBothOverlapDirections() {
        Random random = new Random(0x5e6);
        long[] expected = new long[17000];
        M3LongLane28 lane = new M3LongLane28();
        for (int pass = 0; pass < 1500; pass++) {
            int from = random.nextInt(expected.length + 1);
            int length = random.nextInt(expected.length - from + 1);
            int to = random.nextInt(expected.length - length + 1);
            if ((pass & 3) == 0) {
                long value = pass % 8 == 0 ? 0 : random.nextLong();
                Arrays.fill(expected, from, from + length, value);
                lane.fill(from, from + length, value);
            } else {
                System.arraycopy(expected, from, expected, to, length);
                lane.copyFrom(lane, from, to, length);
            }
            long[] actual = new long[expected.length];
            lane.copyTo(0, actual, 0, actual.length);
            assertArrayEquals(expected, actual, "pass " + pass);
        }
    }

    @Test void intCopiesAndImportsMatchFlatArrays() {
        Random random = new Random(91);
        int[] expected = new int[12000];
        M3IntLane28 lane = new M3IntLane28();
        for (int pass = 0; pass < 700; pass++) {
            int length = random.nextInt(6000);
            int from = random.nextInt(expected.length - length + 1);
            int to = random.nextInt(expected.length - length + 1);
            if (pass % 3 == 0) {
                int[] input = random.ints(length).toArray();
                System.arraycopy(input, 0, expected, to, length);
                lane.copyFrom(input, 0, to, length);
            } else if (pass % 3 == 1) {
                Arrays.fill(expected, from, from + length, 0);
                lane.fill(from, from + length, 0);
            } else {
                System.arraycopy(expected, from, expected, to, length);
                lane.copyFrom(lane, from, to, length);
            }
            int[] actual = new int[expected.length];
            lane.copyTo(0, actual, 0, actual.length);
            assertArrayEquals(expected, actual);
        }
    }

    @Test void sparseZeroPropagationAndExtremeEndpointsDoNotGrowHoles() {
        int end = M3Address28.MAX_SLOTS;
        M3LongLane28 source = new M3LongLane28();
        M3LongLane28 target = new M3LongLane28();
        source.fill(0, end, 0);
        target.copyFrom(source, 0, 0, end);
        assertEquals(0, source.allocatedBlockCount());
        assertEquals(0, target.allocatedBlockCount());
        source.copyFrom(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, 0, end - 3, 3);
        target.copyFrom(source, end - 3, (1 << 20) - 1, 3);
        long[] actual = new long[3];
        target.copyTo((1 << 20) - 1, actual, 0, 3);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, actual);
        assertEquals(2, target.allocatedBlockCount());
        target.copyFrom(source, 0, (1 << 20) - 1, 3);
        target.copyTo((1 << 20) - 1, actual, 0, 3);
        assertArrayEquals(new long[3], actual);
        target.copyFrom(source, end, end, 0);
        target.copyTo(end, new long[0], 0, 0);
        target.fill(end, end, 1);
    }

    @Test void invalidRangesAreRejectedBeforeWriting() {
        M3LongLane28 lane = new M3LongLane28();
        lane.set(0, 19);
        long[] output = {7, 8};
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyTo(0, output, 1, 2));
        assertArrayEquals(new long[] {7, 8}, output);
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyFrom(new long[] {1}, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyFrom(lane, 0, Integer.MAX_VALUE, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.fill(-1, 1, 3));
        assertThrows(NullPointerException.class, () -> lane.copyFrom((long[]) null, 0, 0, 0));
        assertEquals(19, lane.get(0));
    }

    @Test void bitmapRankSelectAndTraversalMatchBitSet() {
        int limit = (1 << 20) + 129;
        BitSet oracle = new BitSet();
        M3BitLane28 lane = new M3BitLane28();
        Random random = new Random(102);
        int[] edges = {0, 1, 62, 63, 64, 65, 4095, 4096, (1 << 20) - 1, 1 << 20};
        for (int bit : edges) { lane.set(bit); oracle.set(bit); }
        for (int i = 0; i < 3000; i++) {
            int bit = random.nextInt(limit);
            boolean value = random.nextBoolean();
            lane.set(bit, value); oracle.set(bit, value);
        }
        assertEquals(oracle.cardinality(), lane.cardinality());
        int ordinal = 0;
        for (int bit = oracle.nextSetBit(0); bit >= 0; bit = oracle.nextSetBit(bit + 1)) {
            assertEquals(bit, lane.select(ordinal++));
        }
        for (int i = 0; i < 500; i++) {
            int from = random.nextInt(limit + 1);
            int to = from + random.nextInt(limit - from + 1);
            assertEquals(oracle.nextSetBit(from), lane.nextSetBit(from));
            assertEquals(oracle.previousSetBit(from - 1), lane.previousSetBit(from - 1));
            assertEquals(oracle.get(from, to).cardinality(), lane.cardinality(from, to));
            assertEquals(oracle.get(0, to).cardinality(), lane.rank(to));
        }
        assertEquals(-1, lane.select(-1));
        assertEquals(-1, lane.select(ordinal));
        assertEquals(-1, lane.nextSetBit(M3Address28.MAX_SLOTS));
        lane.set(M3Address28.MAX_SLOT);
        assertEquals(M3Address28.MAX_SLOT, lane.previousSetBit(M3Address28.MAX_SLOT));
        assertEquals(1, lane.cardinality(M3Address28.MAX_SLOT, M3Address28.MAX_SLOTS));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.rank(-1));
    }

    @Test void sparseRowsRetainAscendingSlotAndGenerationSemantics() {
        M3LongLaneStore28 rows = new M3LongLaneStore28(1);
        long[] handles = new long[9000];
        for (int i = 0; i < handles.length; i++) handles[i] = rows.allocate1(i);
        for (int i = 0; i < handles.length; i++) if (i % 4095 != 0) rows.release(handles[i]);
        long reused = rows.allocate1(123);
        assertThrows(IllegalStateException.class, () -> rows.get(handles[8999], 0));
        assertTrue(rows.isLive(reused));
        long[] output = new long[rows.size() + 2];
        assertEquals(rows.size(), rows.copyLiveLane(0, output, 1));
        assertArrayEquals(new long[] {0, 0, 4095, 8190, 123, 0}, output);
        int[] count = {0};
        rows.forEachLive(handle -> assertEquals(output[++count[0]], rows.get(handle, 0)));
        assertEquals(rows.size(), count[0]);
    }

}
