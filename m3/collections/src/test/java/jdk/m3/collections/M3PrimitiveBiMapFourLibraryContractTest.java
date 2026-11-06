package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3PrimitiveBiMapFourLibraryContractTest {
    @Test
    void strictPutReplacesOwnValueAndRejectsForeignValueWithoutMutation() {
        M3PrimitiveBiMap map = new M3PrimitiveBiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        assertEquals(-1, map.putStrictBits(1, 10, -1));
        assertEquals(-1, map.putStrictBits(2, 20, -1));
        assertEquals(10, map.putStrictBits(1, 30, -1));

        assertThrows(IllegalArgumentException.class, () -> map.putStrictBits(1, 20, -1));

        assertEquals(2, map.size());
        assertEquals(30, map.getValueOrDefaultBits(1, -1));
        assertEquals(20, map.getValueOrDefaultBits(2, -1));
        assertEquals(1, map.getKeyOrDefaultBits(30, -1));
        assertEquals(2, map.getKeyOrDefaultBits(20, -1));
    }

    @Test
    void replacingPutEvictsTheExistingValueOwner() {
        M3PrimitiveBiMap map = new M3PrimitiveBiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        map.putStrictBits(1, 10, -1);
        map.putStrictBits(2, 20, -1);

        assertEquals(10, map.putReplacingBits(1, 20, -1));

        assertEquals(1, map.size());
        assertTrue(map.containsKeyBits(1));
        assertFalse(map.containsKeyBits(2));
        assertEquals(20, map.getValueOrDefaultBits(1, -1));
        assertEquals(1, map.getKeyOrDefaultBits(20, -1));
    }

    @Test
    void inverseIsCachedAndInverseOfInverseIsOriginal() {
        M3PrimitiveBiMap map = new M3PrimitiveBiMap(M3PrimitiveKind.INT, M3PrimitiveKind.LONG);
        M3PrimitiveBiMap inverse = map.inverse();

        assertSame(inverse, map.inverse());
        assertSame(map, inverse.inverse());

        inverse.putReplacingBits(10, 1, -1);
        assertEquals(10, map.getValueOrDefaultBits(1, -1));
        map.putStrictBits(2, 20, -1);
        assertEquals(2, inverse.getValueOrDefaultBits(20, -1));
    }

    @Test
    void legacyInsertOnlyPutRemainsUnchanged() {
        M3PrimitiveBiMap map = new M3PrimitiveBiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        assertTrue(map.putBits(1, 10));
        assertFalse(map.putBits(1, 20));
        assertEquals(10, map.getValueOrDefaultBits(1, -1));
        assertTrue(map.putBits(2, 20));
        assertFalse(map.putBits(3, 20));
        assertFalse(map.containsKeyBits(3));
    }

    @Test
    void collisionPoliciesWorkAcrossAllPrimitiveKindPairs() {
        for (M3PrimitiveKind keyKind : M3PrimitiveKind.values()) {
            for (M3PrimitiveKind valueKind : M3PrimitiveKind.values()) {
                M3PrimitiveBiMap map = new M3PrimitiveBiMap(keyKind, valueKind);
                assertSame(map, map.inverse().inverse());
                assertTrue(map.putBits(0, 0));
                assertTrue(map.putBits(1, 1));

                assertThrows(
                        IllegalArgumentException.class,
                        () -> map.putStrictBits(0, 1, 99));
                assertEquals(2, map.size());

                assertEquals(0, map.putReplacingBits(0, 1, 99));
                assertEquals(1, map.size());
                assertTrue(map.containsKeyBits(0));
                assertFalse(map.containsKeyBits(1));
                assertTrue(map.containsValueBits(1));
            }
        }
    }
    @Test
    void returningRemovalsMatchMapAndBidiMapShapes() {
        M3PrimitiveBiMap map = new M3PrimitiveBiMap(M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        map.putStrictBits(1, 10, -1);
        map.putStrictBits(2, 20, -1);

        assertEquals(10, map.removeKeyOrDefaultBits(1, -7));
        assertEquals(-7, map.removeKeyOrDefaultBits(1, -7));
        assertEquals(2, map.removeValueOrDefaultBits(20, -9));
        assertEquals(-9, map.removeValueOrDefaultBits(20, -9));
        assertTrue(map.isEmpty());
    }

}
