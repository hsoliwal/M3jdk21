// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

final class M3PrimitiveFrozenMapNavigationTest {
    @Test
    void snapshotIsDetachedNavigableAndValueSearchable() {
        M3PrimitivePrimitiveHashMap source =
                new M3PrimitivePrimitiveHashMap(M3PrimitiveKind.INT, M3PrimitiveKind.LONG);
        source.putBits(10, 100L, -1L);
        source.putBits(30, 300L, -1L);
        source.putBits(20, 200L, -1L);
        M3PrimitiveFrozenMap frozen = M3PrimitiveFrozenMap.copyOf(source);

        assertEquals(10L, frozen.firstKeyBits());
        assertEquals(30L, frozen.lastKeyBits());
        assertEquals(10L, frozen.lowerKeyOrDefaultBits(20, -1L));
        assertEquals(20L, frozen.floorKeyOrDefaultBits(20, -1L));
        assertEquals(20L, frozen.ceilingKeyOrDefaultBits(20, -1L));
        assertEquals(30L, frozen.higherKeyOrDefaultBits(20, -1L));
        assertTrue(frozen.containsValueBits(200L));
        assertFalse(frozen.containsValueBits(999L));

        source.clear();
        source.putBits(99, 999L, -1L);
        assertEquals(200L, frozen.getOrDefaultBits(20, -1L));
        assertFalse(frozen.containsKeyBits(99));
        assertFalse(frozen.containsValueBits(999L));
    }

    @Test
    void emptyFirstAndLastThrow() {
        M3PrimitiveFrozenMap empty =
                M3PrimitiveFrozenMap.copyOf(
                        new M3PrimitivePrimitiveHashMap(
                                M3PrimitiveKind.INT, M3PrimitiveKind.INT));
        assertThrows(NoSuchElementException.class, empty::firstKeyBits);
        assertThrows(NoSuchElementException.class, empty::lastKeyBits);
        assertEquals(7L, empty.floorKeyOrDefaultBits(1L, 7L));
        assertEquals(8L, empty.ceilingKeyOrDefaultBits(1L, 8L));
    }
}
