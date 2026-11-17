/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.PrimitiveKind;
import org.junit.jupiter.api.Test;

/** The storage helpers of {@code PrimitiveKind} are public: another package calls them. */
final class PrimitiveKindHelpersTest {
    private static final long PAYLOAD_NAN = 0x7ff8_0000_0000_0001L;

    @Test
    void storageArraysMatchEachKind() {
        assertInstanceOf(byte[].class, PrimitiveKind.BOOLEAN.newArray(2));
        assertInstanceOf(byte[].class, PrimitiveKind.BYTE.newArray(2));
        assertInstanceOf(short[].class, PrimitiveKind.SHORT.newArray(2));
        assertInstanceOf(char[].class, PrimitiveKind.CHAR.newArray(2));
        assertInstanceOf(int[].class, PrimitiveKind.INT.newArray(2));
        assertInstanceOf(long[].class, PrimitiveKind.LONG.newArray(2));
        assertInstanceOf(float[].class, PrimitiveKind.FLOAT.newArray(2));
        assertInstanceOf(double[].class, PrimitiveKind.DOUBLE.newArray(2));
        assertThrows(NegativeArraySizeException.class, () -> PrimitiveKind.INT.newArray(-1));
        assertThrows(IllegalArgumentException.class,
                () -> PrimitiveKind.INT.checkArray(new long[1]));
        PrimitiveKind.LONG.checkArray(new long[0]);
    }

    @Test
    void lanesRoundTripThroughEveryKind() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            Object lanes = kind.newArray(4);
            kind.writeBits(lanes, 3, 5L);
            assertEquals(kind.storageBits(5L), kind.readBits(lanes, 3), kind.name());
            assertEquals(3, kind.indexOfBits(lanes, 0, 4, 5L), kind.name());
            Object longer = kind.copyOf(lanes, 6);
            assertEquals(kind.readBits(lanes, 3), kind.readBits(longer, 3), kind.name());
            kind.fillBits(longer, 0, 6, 5L);
            assertEquals(0, kind.indexOfBits(longer, 0, 6, 5L), kind.name());
            kind.clear(longer, 0, 6);
            assertEquals(-1, kind.indexOfBits(longer, 0, 6, 5L), kind.name());
        }
    }

    @Test
    void canonicalAndStoredBitsDifferOnlyForNaNPayloads() {
        long canonical = Double.doubleToRawLongBits(Double.NaN);
        assertEquals(PAYLOAD_NAN, PrimitiveKind.DOUBLE.storageBits(PAYLOAD_NAN));
        assertEquals(canonical, PrimitiveKind.DOUBLE.normalizeBits(PAYLOAD_NAN));
        assertTrue(PrimitiveKind.DOUBLE.equalBits(PAYLOAD_NAN, canonical));
        assertEquals(PrimitiveKind.DOUBLE.hashBits(canonical),
                PrimitiveKind.DOUBLE.hashBits(PAYLOAD_NAN));
        long minusZero = Double.doubleToRawLongBits(-0.0d);
        assertTrue(PrimitiveKind.DOUBLE.compareBits(minusZero, 0L) < 0);
        assertFalse(PrimitiveKind.DOUBLE.equalBits(minusZero, 0L));
        assertEquals(-1L, PrimitiveKind.INT.normalizeBits(0xffff_ffffL));
        assertEquals(1L, PrimitiveKind.BOOLEAN.normalizeBits(42L));
    }

    @Test
    void sortAndSearchUseTheStorageOrder() {
        short[] lanes = {7, -2, 40, 3, -9};
        PrimitiveKind.SHORT.sort(lanes, 0, 5);
        assertArrayEquals(new short[] {-9, -2, 3, 7, 40}, lanes);
        assertEquals(2, PrimitiveKind.SHORT.binarySearch(lanes, 0, 5, 3L));
        assertTrue(PrimitiveKind.SHORT.binarySearch(lanes, 0, 5, 4L) < 0);
        assertTrue(PrimitiveKind.SHORT.compareBits(-2L, 3L) < 0);
    }

    @Test
    void ringHelpersMapCopyMoveAndClearAcrossTheWrap() {
        int[] ring = {4, 5, 0, 0, 1, 2, 3}; // capacity 7, head 4: logical 1, 2, 3, 4, 5
        assertEquals(6, PrimitiveKind.ringIndex(4, 2, 7));
        assertEquals(1, PrimitiveKind.ringIndex(4, 4, 7));
        int[] linear = new int[5];
        PrimitiveKind.copyRing(ring, 4, 5, 7, linear, 0);
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, linear);
        PrimitiveKind.moveRing(ring, 4, 7, 1, 0, 4);
        PrimitiveKind.copyRing(ring, 4, 5, 7, linear, 0);
        assertArrayEquals(new int[] {2, 3, 4, 5, 5}, linear);
        PrimitiveKind.INT.clearRing(ring, 4, 5, 7);
        assertArrayEquals(new int[7], ring);
    }

    @Test
    void rangesAreCheckedAndBooleanCopiesNormalized() {
        assertEquals(3, PrimitiveKind.INT.checkRange(new int[5], 1, 3));
        assertThrows(IndexOutOfBoundsException.class,
                () -> PrimitiveKind.INT.checkRange(new int[5], 3, 3));
        assertThrows(NullPointerException.class, () -> PrimitiveKind.INT.checkRange(null, 0, 0));
        byte[] target = new byte[4];
        PrimitiveKind.BOOLEAN.copyStorage(new byte[] {0, 7, -1, 0}, 0, target, 0, 4);
        assertArrayEquals(new byte[] {0, 1, 1, 0}, target);
        byte[] same = {0, 5, 9, 0};
        PrimitiveKind.BOOLEAN.copyStorage(same, 0, same, 1, 3);
        assertArrayEquals(new byte[] {0, 0, 1, 1}, same);
    }
}
