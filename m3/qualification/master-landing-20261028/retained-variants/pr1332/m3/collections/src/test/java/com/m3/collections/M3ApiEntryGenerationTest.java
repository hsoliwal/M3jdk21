// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A caller-held API Entry is a projection of one logical generation, never
 * a mutable entry carrier that may be recycled as another mapping.
 */
final class M3ApiEntryGenerationTest {
    @Test
    void hashEntryDoesNotRebindWhenPhysicalSlotIsReusedForEqualKey() {
        M3LinkedHashMap<Integer, Integer> map = new M3LinkedHashMap<>();
        map.put(1, 10);
        Map.Entry<Integer, Integer> original = map.entrySet().iterator().next();

        map.put(2, 20);
        assertSame(original, map.entrySet().iterator().next());
        assertEquals(10, original.getValue());

        map.remove(1);
        map.put(1, 30);
        assertThrows(IllegalStateException.class, original::getValue);
        assertThrows(IllegalStateException.class, () -> original.setValue(99));
        assertEquals(30, map.get(1));
        map.verifyInvariants();
    }

    @Test
    void treeEntryGenerationsRemainStableThroughUnrelatedRotations() {
        M3TreeMap<Integer, Integer> tree = new M3TreeMap<>();
        tree.put(10, 100);
        Map.Entry<Integer, Integer> original = tree.entrySet().iterator().next();

        tree.put(20, 200);
        tree.put(30, 300);
        tree.put(40, 400);
        assertSame(original, tree.entrySet().iterator().next());
        assertEquals(100, original.getValue());
        tree.verifyInvariants();

        tree.remove(10);
        tree.put(10, 500);
        assertThrows(IllegalStateException.class, original::getValue);
        assertThrows(IllegalStateException.class, () -> original.setValue(50));
        assertEquals(500, tree.get(10));
        tree.verifyInvariants();
    }
}
