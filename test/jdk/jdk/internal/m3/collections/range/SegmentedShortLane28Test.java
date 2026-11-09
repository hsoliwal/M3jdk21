// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class SegmentedShortLane28Test {
    @Test
    void sparseFlagsAllocateOnlyTouchedBlocks() {
        SegmentedShortLane28 lane = new SegmentedShortLane28();
        int[] slots = {0, 4096, 1 << 20, SegmentedAddress28.MAX_SLOT};
        for (int i = 0; i < slots.length; i++) lane.set(slots[i], (short) (i + 1));
        assertEquals(slots.length, lane.allocatedBlockCount());
        for (int i = 0; i < slots.length; i++) assertEquals((short) (i + 1), lane.get(slots[i]));
        assertEquals(0, lane.get(1));
        assertEquals((long) slots.length * SegmentedAddress28.VALUES_PER_BLOCK * Short.BYTES,
                lane.payloadBytes());
        lane.clear(slots[0]);
        assertEquals(0, lane.get(slots[0]));
    }

    @Test
    void bulkCopiesHaveMemmoveSemantics() {
        Random random = new Random(0x53484f5254L);
        short[] expected = new short[12000];
        SegmentedShortLane28 lane = new SegmentedShortLane28();
        for (int pass = 0; pass < 800; pass++) {
            int length = random.nextInt(5000);
            int from = random.nextInt(expected.length - length + 1);
            int to = random.nextInt(expected.length - length + 1);
            if ((pass & 3) == 0) {
                short value = (short) random.nextInt();
                Arrays.fill(expected, from, from + length, value);
                lane.fill(from, from + length, value);
            } else if ((pass & 3) == 1) {
                short[] input = new short[length];
                for (int i = 0; i < input.length; i++) input[i] = (short) random.nextInt();
                System.arraycopy(input, 0, expected, to, length);
                lane.copyFrom(input, 0, to, length);
            } else {
                System.arraycopy(expected, from, expected, to, length);
                lane.copyFrom(lane, from, to, length);
            }
            short[] actual = new short[expected.length];
            lane.copyTo(0, actual, 0, actual.length);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void zeroRangesStaySparseAndFactoryExposesLane() {
        SegmentedShortLane28 lane = LeanSegmentedCollections.shortLane();
        lane.fill(0, SegmentedAddress28.MAX_SLOTS, (short) 0);
        assertEquals(0, lane.allocatedBlockCount());
        lane.copyFrom(new short[4], 0, SegmentedAddress28.MAX_SLOTS - 4, 4);
        assertEquals(0, lane.allocatedBlockCount());
        assertThrows(IndexOutOfBoundsException.class, () -> lane.set(-1, (short) 1));
    }
}
