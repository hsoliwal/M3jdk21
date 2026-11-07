// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

final class M3PrimitiveFrozenSetNavigationTest {
    @Test
    void snapshotIsDetachedAndNavigable() {
        M3PrimitiveHashSet source = new M3PrimitiveHashSet(PrimitiveKind.INT);
        source.addBits(10);
        source.addBits(30);
        source.addBits(20);
        M3PrimitiveFrozenSet frozen = M3PrimitiveFrozenSet.copyOf(source);

        assertEquals(10L, frozen.firstBits());
        assertEquals(30L, frozen.lastBits());
        assertEquals(10L, frozen.lowerOrDefaultBits(20, -1L));
        assertEquals(20L, frozen.floorOrDefaultBits(20, -1L));
        assertEquals(20L, frozen.ceilingOrDefaultBits(20, -1L));
        assertEquals(30L, frozen.higherOrDefaultBits(20, -1L));
        assertEquals(-1L, frozen.lowerOrDefaultBits(10, -1L));
        assertEquals(-1L, frozen.higherOrDefaultBits(30, -1L));

        source.clear();
        source.addBits(99);
        assertTrue(frozen.containsBits(20));
        assertFalse(frozen.containsBits(99));

        int[] exported = (int[]) frozen.toPrimitiveArray();
        assertArrayEquals(new int[] {10, 20, 30}, exported);
        exported[0] = 999;
        assertEquals(10L, frozen.firstBits());
    }

    @Test
    void emptyFirstAndLastThrow() {
        M3PrimitiveFrozenSet empty =
                M3PrimitiveFrozenSet.copyOf(new M3PrimitiveHashSet(PrimitiveKind.LONG));
        assertThrows(NoSuchElementException.class, empty::firstBits);
        assertThrows(NoSuchElementException.class, empty::lastBits);
        assertEquals(7L, empty.floorOrDefaultBits(1L, 7L));
        assertEquals(8L, empty.ceilingOrDefaultBits(1L, 8L));
    }
}
