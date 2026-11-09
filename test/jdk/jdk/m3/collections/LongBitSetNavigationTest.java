// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import jdk.m3.collections.bits.LongBitSet;

final class LongBitSetNavigationTest {
    @Test
    void navigationMatchesJdkBoundarySemantics() {
        LongBitSet bits = new LongBitSet(64);
        bits.set(0);
        bits.set(2);
        bits.set(63);
        bits.set(130);

        assertEquals(1, bits.nextClearBit(0));
        assertEquals(64, bits.nextClearBit(63));
        assertEquals(131, bits.nextClearBit(130));
        assertEquals(500, bits.nextClearBit(500));

        assertEquals(130, bits.previousSetBit(500));
        assertEquals(63, bits.previousSetBit(129));
        assertEquals(2, bits.previousSetBit(2));
        assertEquals(-1, bits.previousSetBit(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bits.previousSetBit(-2));

        assertEquals(500, bits.previousClearBit(500));
        assertEquals(129, bits.previousClearBit(130));
        assertEquals(1, bits.previousClearBit(2));
        assertEquals(-1, bits.previousClearBit(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bits.previousClearBit(-2));
        assertThrows(IndexOutOfBoundsException.class, () -> bits.nextClearBit(-1));
    }

    @Test
    void lengthAndEmptyTrackHighestSetBitNotCapacity() {
        LongBitSet bits = new LongBitSet(256);
        assertTrue(bits.isEmpty());
        assertEquals(0, bits.length());
        bits.set(130);
        assertFalse(bits.isEmpty());
        assertEquals(131, bits.length());
        bits.clear(130);
        assertTrue(bits.isEmpty());
        assertEquals(0, bits.length());
        assertEquals(256, bits.capacityBits());
    }

    @Test
    void intersectsAndAndNotMatchExactSetSemantics() {
        LongBitSet left = new LongBitSet();
        LongBitSet right = new LongBitSet();
        left.set(1);
        left.set(64);
        left.set(130);
        right.set(64);
        right.set(200);

        assertTrue(left.intersects(right));
        left.andNot(right);
        assertFalse(left.get(64));
        assertTrue(left.get(1));
        assertTrue(left.get(130));
        assertFalse(left.intersects(right));
    }

    @Test
    void allAllocatedBitsSetStillFindsClearBitAtLogicalEnd() {
        LongBitSet bits = new LongBitSet(64);
        for (int i = 0; i < 64; i++) bits.set(i);
        assertEquals(64, bits.nextClearBit(0));
        assertEquals(-1, bits.previousClearBit(63));
    }

    @Test
    void nullExactSetOperandsAreRejected() {
        LongBitSet bits = new LongBitSet();
        assertThrows(NullPointerException.class, () -> bits.intersects(null));
        assertThrows(NullPointerException.class, () -> bits.andNot(null));
    }
}
