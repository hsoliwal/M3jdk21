// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongArrayDeque;
import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongDeque;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class LeanFlatArrayPatternTest {
    @Test
    void longDequeGrowthLinearizesWrappedRingWithoutChangingOrder() {
        LongArrayDeque deque = new LongArrayDeque(4);
        for (long value = 1; value <= 4; value++) deque.addLast(value);
        assertEquals(1, deque.removeFirst());
        assertEquals(2, deque.removeFirst());
        deque.addLast(5);
        deque.addLast(6);
        assertArrayEquals(new long[]{3, 4, 5, 6}, deque.toArray());

        deque.addLast(7);

        assertEquals(8, deque.capacity());
        assertArrayEquals(new long[]{3, 4, 5, 6, 7}, deque.toArray());
    }

    @Test
    void allPrimitiveDequesShareWrappedGrowthSemantics() {
        var bytes = new PrimitiveDeques.ByteDeque(8);
        for (byte value = 0; value < 8; value++) bytes.addLast(value);
        for (int i = 0; i < 3; i++) bytes.removeFirst();
        bytes.addLast((byte) 8); bytes.addLast((byte) 9); bytes.addLast((byte) 10); bytes.addLast((byte) 11);
        assertArrayEquals(new byte[]{3,4,5,6,7,8,9,10,11}, bytes.toArray());

        var shorts = new PrimitiveDeques.ShortDeque(8);
        for (short value = 0; value < 8; value++) shorts.addLast(value);
        for (int i = 0; i < 3; i++) shorts.removeFirst();
        shorts.addLast((short) 8); shorts.addLast((short) 9); shorts.addLast((short) 10); shorts.addLast((short) 11);
        assertArrayEquals(new short[]{3,4,5,6,7,8,9,10,11}, shorts.toArray());

        var chars = new PrimitiveDeques.CharDeque(8);
        for (char value = 0; value < 8; value++) chars.addLast(value);
        for (int i = 0; i < 3; i++) chars.removeFirst();
        chars.addLast((char) 8); chars.addLast((char) 9); chars.addLast((char) 10); chars.addLast((char) 11);
        assertArrayEquals(new char[]{3,4,5,6,7,8,9,10,11}, chars.toArray());

        var ints = new PrimitiveDeques.IntDeque(8);
        for (int value = 0; value < 8; value++) ints.addLast(value);
        for (int i = 0; i < 3; i++) ints.removeFirst();
        ints.addLast(8); ints.addLast(9); ints.addLast(10); ints.addLast(11);
        assertArrayEquals(new int[]{3,4,5,6,7,8,9,10,11}, ints.toArray());

        var longs = new PrimitiveDeques.LongDeque(8);
        for (long value = 0; value < 8; value++) longs.addLast(value);
        for (int i = 0; i < 3; i++) longs.removeFirst();
        longs.addLast(8); longs.addLast(9); longs.addLast(10); longs.addLast(11);
        assertArrayEquals(new long[]{3,4,5,6,7,8,9,10,11}, longs.toArray());

        var booleans = new PrimitiveDeques.BooleanDeque(8);
        boolean[] initial = {false,true,false,true,true,false,true,false};
        for (boolean value : initial) booleans.addLast(value);
        booleans.removeFirst(); booleans.removeFirst(); booleans.removeFirst();
        booleans.addLast(true); booleans.addLast(false); booleans.addLast(false); booleans.addLast(true);
        assertArrayEquals(
                new boolean[]{true,true,false,true,false,true,false,false,true},
                booleans.toArray());

        var floats = new PrimitiveDeques.FloatDeque(8);
        float[] f = {0f,-0f,1f,2f,3f,4f,5f,Float.intBitsToFloat(0x7fc00001)};
        for (float value : f) floats.addLast(value);
        floats.removeFirst(); floats.removeFirst(); floats.removeFirst();
        floats.addLast(6f); floats.addLast(7f); floats.addLast(8f); floats.addLast(-0f);
        float[] fa = floats.toArray();
        float[] fe = {2f,3f,4f,5f,Float.intBitsToFloat(0x7fc00001),6f,7f,8f,-0f};
        assertEquals(fe.length, fa.length);
        for (int i = 0; i < fe.length; i++) {
            assertEquals(Float.floatToRawIntBits(fe[i]), Float.floatToRawIntBits(fa[i]));
        }

        var doubles = new PrimitiveDeques.DoubleDeque(8);
        double[] d = {0d,-0d,1d,2d,3d,4d,5d,Double.longBitsToDouble(0x7ff8000000000001L)};
        for (double value : d) doubles.addLast(value);
        doubles.removeFirst(); doubles.removeFirst(); doubles.removeFirst();
        doubles.addLast(6d); doubles.addLast(7d); doubles.addLast(8d); doubles.addLast(-0d);
        double[] da = doubles.toArray();
        double[] de = {2d,3d,4d,5d,Double.longBitsToDouble(0x7ff8000000000001L),6d,7d,8d,-0d};
        assertEquals(de.length, da.length);
        for (int i = 0; i < de.length; i++) {
            assertEquals(Double.doubleToRawLongBits(de[i]), Double.doubleToRawLongBits(da[i]));
        }
    }

    @Test
    void fixedRingSnapshotUsesSameLogicalCopy() {
        PackedLongRingBuffer ring = new PackedLongRingBuffer(4);
        assertTrue(ring.offer(1));
        assertTrue(ring.offer(2));
        assertTrue(ring.offer(3));
        assertTrue(ring.offer(4));
        assertEquals(1, ring.poll());
        assertEquals(2, ring.poll());
        assertTrue(ring.offer(5));
        assertTrue(ring.offer(6));
        assertArrayEquals(new long[]{3,4,5,6}, ring.toArray());
    }

    @Test
    void contiguousAndParallelOwnersPreserveCapacitySemantics() {
        LongArrayList list = new LongArrayList(1);
        for (int i = 0; i < 100; i++) list.add(i);
        assertEquals(100, list.size());
        assertEquals(99, list.get(99));

        PackedLongHeap heap = PackedLongHeap.minHeap(1);
        for (int i = 99; i >= 0; i--) heap.offer(i);
        for (int i = 0; i < 100; i++) assertEquals(i, heap.poll());

        PackedLongSortedSet set = new PackedLongSortedSet(1);
        for (int i = 99; i >= 0; i--) assertTrue(set.add(i));
        assertEquals(100, set.size());
        assertEquals(0, set.first());
        assertEquals(99, set.last());

        PackedLongPairs pairs = new PackedLongPairs(1);
        for (int i = 0; i < 100; i++) pairs.add(i, -i);
        assertEquals(100, pairs.size());
        assertEquals(99, pairs.first(99));
        assertEquals(-99, pairs.second(99));

        PackedLongLongMultiMap multimap = new PackedLongLongMultiMap(1, 1);
        for (int i = 0; i < 100; i++) multimap.add(7, i);
        assertEquals(100, multimap.size());
        assertArrayEquals(
                java.util.stream.LongStream.range(0, 100).toArray(),
                multimap.values(7));
    }

    @Test
    void powerOfTwoCapacityPolicyIsSharedByDequeAndPrimitiveHeapFamilies() {
        assertEquals(8, PackedFlatArrays.powerOfTwoCapacity(0, 8));
        assertEquals(8, PackedFlatArrays.powerOfTwoCapacity(8, 8));
        assertEquals(16, PackedFlatArrays.powerOfTwoCapacity(9, 8));
        assertEquals(16, PackedFlatArrays.growPowerOfTwo(8));

        var heap = new PrimitivePriorityQueues.IntMinHeap(1);
        for (int i = 31; i >= 0; i--) heap.add(i);
        for (int i = 0; i < 32; i++) assertEquals(i, heap.removeMin());
    }
}
