// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3PrimitiveHashSetAlgebraTest {
    @Test
    void jdkStyleBulkMutationPreservesSetSemantics() {
        M3PrimitiveHashSet left = longs(0, 1, 2, 3);
        M3PrimitiveHashSet right = longs(2, 3, 4);

        assertFalse(left.containsAll(right));
        assertTrue(left.addAll(right));
        assertTrue(left.containsAll(right));
        assertEquals(5, left.size());
        assertFalse(left.addAll(right));

        assertTrue(left.removeAll(longs(1, 4)));
        assertFalse(left.containsBits(1));
        assertFalse(left.containsBits(4));

        assertTrue(left.retainAll(longs(0, 3, 99)));
        assertEquals(2, left.size());
        assertTrue(left.containsBits(0));
        assertTrue(left.containsBits(3));
        assertFalse(left.retainAll(left));
    }

    @Test
    void callerOwnedUnionIntersectionDifferenceDoNotMutateInputs() {
        M3PrimitiveHashSet left = longs(0, 1, 2, 3);
        M3PrimitiveHashSet right = longs(2, 3, 4);

        M3PrimitiveHashSet union = left.union(right);
        M3PrimitiveHashSet intersection = left.intersection(right);
        M3PrimitiveHashSet difference = left.difference(right);

        assertEquals(5, union.size());
        for (long value : new long[] {0, 1, 2, 3, 4}) assertTrue(union.containsBits(value));
        assertEquals(2, intersection.size());
        assertTrue(intersection.containsBits(2));
        assertTrue(intersection.containsBits(3));
        assertEquals(2, difference.size());
        assertTrue(difference.containsBits(0));
        assertTrue(difference.containsBits(1));

        assertEquals(4, left.size());
        assertEquals(3, right.size());
        union.removeBits(4);
        assertTrue(right.containsBits(4));
    }

    @Test
    void allPrimitiveKindsUseTheirExistingEqualityRules() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            M3PrimitiveHashSet left = new M3PrimitiveHashSet(kind);
            M3PrimitiveHashSet right = new M3PrimitiveHashSet(kind);
            left.addBits(0L);
            left.addBits(1L);
            left.addBits(2L);
            right.addBits(2L);
            right.addBits(3L);

            M3PrimitiveHashSet union = left.union(right);
            M3PrimitiveHashSet intersection = left.intersection(right);
            M3PrimitiveHashSet difference = left.difference(right);

            int expectedUnion = kind == PrimitiveKind.BOOLEAN ? 2 : 4;
            int expectedDifference = kind == PrimitiveKind.BOOLEAN ? 1 : 2;
            assertEquals(expectedUnion, union.size(), kind.name());
            assertEquals(1, intersection.size(), kind.name());
            assertEquals(expectedDifference, difference.size(), kind.name());
        }
    }

    @Test
    void floatingNaNsAndSignedZerosFollowPrimitiveKindIdentity() {
        M3PrimitiveHashSet floats = new M3PrimitiveHashSet(PrimitiveKind.FLOAT);
        long nanA = Integer.toUnsignedLong(0x7fc00001);
        long nanB = Integer.toUnsignedLong(0x7fc00002);
        long negativeZero = Integer.toUnsignedLong(0x80000000);
        long positiveZero = 0L;

        assertTrue(floats.addBits(nanA));
        assertFalse(floats.addBits(nanB));
        assertTrue(floats.addBits(negativeZero));
        assertTrue(floats.addBits(positiveZero));
        assertEquals(3, floats.size());

        M3PrimitiveHashSet other = new M3PrimitiveHashSet(PrimitiveKind.FLOAT);
        other.addBits(nanB);
        other.addBits(positiveZero);
        assertEquals(2, floats.intersection(other).size());
    }

    @Test
    void kindMismatchFailsBeforeMutation() {
        M3PrimitiveHashSet ints = new M3PrimitiveHashSet(PrimitiveKind.INT);
        M3PrimitiveHashSet longs = new M3PrimitiveHashSet(PrimitiveKind.LONG);
        ints.addBits(1);
        longs.addBits(1);

        assertThrows(IllegalArgumentException.class, () -> ints.addAll(longs));
        assertThrows(IllegalArgumentException.class, () -> ints.removeAll(longs));
        assertThrows(IllegalArgumentException.class, () -> ints.retainAll(longs));
        assertThrows(IllegalArgumentException.class, () -> ints.union(longs));
        assertEquals(1, ints.size());
        assertTrue(ints.containsBits(1));
    }

    private static M3PrimitiveHashSet longs(long... values) {
        M3PrimitiveHashSet result = new M3PrimitiveHashSet(PrimitiveKind.LONG);
        for (long value : values) result.addBits(value);
        return result;
    }
}
