// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Target-owner regression checks against ordinary, unpacked long-array expectations. */
final class M3PackedUnsignedLongVectorBulkTest {
    @Test
    void factoriesChooseExactWidthFromOnlyTheRequestedSlice() {
        assertEquals(1, M3PackedUnsignedLongVector.copyOf(new long[0]).width());
        assertEquals(1, M3PackedUnsignedLongVector.copyOf(new long[137]).width());
        for (int bit = 0; bit < Long.SIZE; bit++) {
            long[] source = {-1L, 0L, 1L << bit, 0L, -1L};
            M3PackedUnsignedLongVector vector =
                    M3PackedUnsignedLongVector.copyOf(source, 1, 3);
            assertEquals(bit + 1, vector.width());
            assertEquals(3, vector.size());
            assertArrayEquals(new long[] {0L, 1L << bit, 0L}, vector.toLongArray());
            assertEquals(((3L * (bit + 1) + 63) / 64) * Long.BYTES,
                    vector.payloadBytes());
            source[2] = 0L;
            assertEquals(1L << bit, vector.get(1));
        }
        M3PackedUnsignedLongVector empty =
                M3PackedUnsignedLongVector.copyOf(new long[] {-1L}, 1, 0);
        assertEquals(1, empty.width());
        assertEquals(0, empty.size());
        assertEquals(0, empty.payloadBytes());
    }

    @Test
    void everyWidthAndStartingOffsetMatchesUnpackedArrayOperations() {
        Random random = new Random(0x4d335041434b2026L);
        for (int width = 1; width <= Long.SIZE; width++) {
            long mask = width == Long.SIZE ? -1L : (1L << width) - 1L;
            for (int prefixLength = 0; prefixLength < Long.SIZE; prefixLength++) {
                M3PackedUnsignedLongVector vector = new M3PackedUnsignedLongVector(width);
                long[] expected = new long[prefixLength + 131];
                for (int i = 0; i < prefixLength; i++) {
                    expected[i] = random.nextLong() & mask;
                    vector.add(expected[i]);
                }
                long[] source = new long[137];
                Arrays.fill(source, -1L);
                for (int i = 3; i < 134; i++) source[i] = random.nextLong() & mask;
                long[] sourceBefore = source.clone();
                System.arraycopy(source, 3, expected, prefixLength, 131);
                vector.addAll(source, 3, 131);
                assertArrayEquals(sourceBefore, source);
                assertArrayEquals(expected, vector.toLongArray(),
                        "append width=" + width + " prefix=" + prefixLength);

                long[] replacement = new long[51];
                Arrays.fill(replacement, -1L);
                for (int i = 2; i < 49; i++) replacement[i] = random.nextLong() & mask;
                long[] replacementBefore = replacement.clone();
                System.arraycopy(replacement, 2, expected, prefixLength + 1, 47);
                vector.setAll(prefixLength + 1, replacement, 2, 47);
                assertArrayEquals(replacementBefore, replacement);
                assertArrayEquals(expected, vector.toLongArray(),
                        "replace width=" + width + " prefix=" + prefixLength);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], vector.get(i));
                }

                long[] destination = new long[140];
                Arrays.fill(destination, 0x7123456789abcdefL);
                long[] expectedDestination = destination.clone();
                System.arraycopy(expected, prefixLength, expectedDestination, 5, 131);
                vector.copyInto(prefixLength, destination, 5, 131);
                assertArrayEquals(expectedDestination, destination,
                        "copy width=" + width + " prefix=" + prefixLength);
            }
        }
    }

    @Test
    void lateInvalidValueCannotPartiallyAppendReplaceOrGrowAnyNarrowWidth() {
        for (int width = 1; width < Long.SIZE; width++) {
            long mask = (1L << width) - 1L;
            M3PackedUnsignedLongVector vector = new M3PackedUnsignedLongVector(width);
            vector.addAll(new long[] {mask, 0L, mask, 0L, 1L});
            long[] expected = vector.toLongArray();
            long bytes = vector.payloadBytes();

            long[] append = new long[257];
            append[append.length - 1] = 1L << width;
            assertThrows(IllegalArgumentException.class, () -> vector.addAll(append));
            assertEquals(expected.length, vector.size());
            assertEquals(bytes, vector.payloadBytes());
            assertArrayEquals(expected, vector.toLongArray());

            long[] replace = {0L, 1L, 1L << width};
            assertThrows(IllegalArgumentException.class,
                    () -> vector.setAll(1, replace, 0, replace.length));
            assertEquals(bytes, vector.payloadBytes());
            assertArrayEquals(expected, vector.toLongArray());
        }
    }

    @Test
    void invalidRangesAndNullsLeaveBothOwnersUnchanged() {
        M3PackedUnsignedLongVector vector = new M3PackedUnsignedLongVector(64);
        vector.addAll(new long[] {Long.MIN_VALUE, -1L, Long.MAX_VALUE});
        long[] expected = vector.toLongArray();
        long bytes = vector.payloadBytes();
        long[] source = {7L, 8L};
        assertThrows(NullPointerException.class, () -> vector.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.addAll(source, -1, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> vector.addAll(source, 1, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.setAll(2, source, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.setAll(0, source, 2, 1));
        assertThrows(NullPointerException.class, () -> vector.setAll(0, null, 0, 1));
        assertEquals(bytes, vector.payloadBytes());
        assertArrayEquals(expected, vector.toLongArray());
        assertArrayEquals(new long[] {7L, 8L}, source);

        long[] destination = {31L, 32L, 33L, 34L};
        long[] destinationBefore = destination.clone();
        assertThrows(IndexOutOfBoundsException.class,
                () -> vector.copyInto(-1, destination, 0, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> vector.copyInto(2, destination, 0, 2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> vector.copyInto(0, destination, 3, 2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> vector.copyInto(0, destination, 1, Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> vector.copyInto(0, null, 0, 1));
        assertArrayEquals(destinationBefore, destination);
        assertArrayEquals(expected, vector.toLongArray());

        assertThrows(NullPointerException.class, () -> M3PackedUnsignedLongVector.copyOf(null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3PackedUnsignedLongVector.copyOf(source, 1, Integer.MAX_VALUE));
    }

    @Test
    void detachedArraysAndZeroLengthBoundaryOperationsPreserveOwnership() {
        long[] source = {Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE};
        long[] expected = source.clone();
        M3PackedUnsignedLongVector vector = M3PackedUnsignedLongVector.copyOf(source);
        Arrays.fill(source, 17L);
        assertArrayEquals(expected, vector.toLongArray());
        long[] unpacked = vector.toLongArray();
        Arrays.fill(unpacked, 23L);
        assertArrayEquals(expected, vector.toLongArray());
        long[] replacement = {41L, 43L};
        vector.setAll(1, replacement, 0, 2);
        replacement[0] = 0L;
        expected[1] = 41L;
        expected[2] = 43L;
        assertArrayEquals(expected, vector.toLongArray());

        long bytes = vector.payloadBytes();
        vector.addAll(source, source.length, 0);
        vector.setAll(vector.size(), source, source.length, 0);
        vector.copyInto(vector.size(), unpacked, unpacked.length, 0);
        assertEquals(bytes, vector.payloadBytes());
        assertArrayEquals(expected, vector.toLongArray());
        assertArrayEquals(new long[] {23L, 23L, 23L, 23L}, unpacked);
    }

    @Test
    void bulkRefillAfterClearAndTrimCannotExposePriorPackedValues() {
        for (int width = 1; width <= Long.SIZE; width++) {
            long mask = width == Long.SIZE ? -1L : (1L << width) - 1L;
            M3PackedUnsignedLongVector vector = new M3PackedUnsignedLongVector(width);
            long[] old = new long[193];
            Arrays.fill(old, mask);
            vector.addAll(old);
            vector.clear();
            long[] expected = new long[129];
            expected[0] = mask;
            expected[64] = mask;
            expected[128] = mask;
            vector.addAll(expected);
            vector.trimToSize();
            assertArrayEquals(expected, vector.toLongArray());
            assertEquals(((129L * width + 63) / 64) * Long.BYTES, vector.payloadBytes());
            long[] destination = new long[expected.length];
            vector.copyInto(0, destination, 0, destination.length);
            assertArrayEquals(expected, destination);
        }
    }
}
