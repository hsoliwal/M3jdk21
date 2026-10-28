// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Slot state and generation regressions for the no-Entry internal representation. */
final class M3LinkedHashMapSlotStateTest {
    @Test
    void insertMoveDeleteReuseAndClearPreserveParallelSlotLanes() {
        M3LinkedHashMap<Integer, Integer> map = new M3LinkedHashMap<>(true);
        for (int i = 0; i < 400; i++) {
            map.put(i, i * 10);
            if ((i & 7) == 0) { map.verifyInvariants(); }
        }
        Map.Entry<Integer, Integer> old = map.entrySet().iterator().next();
        assertSame(old, map.entrySet().iterator().next());
        map.get(old.getKey());
        map.verifyInvariants();
        for (int i = 0; i < 400; i += 2) {
            map.remove(i);
            if ((i & 15) == 0) { map.verifyInvariants(); }
        }
        map.verifyInvariants();
        for (int i = 400; i < 600; i++) {
            map.put(i, i * 10);
        }
        map.verifyInvariants();
        assertEquals(400, map.size());
        map.clear();
        map.verifyInvariants();
        map.put(10_000, 10_000);
        map.verifyInvariants();
        Map.Entry<Integer, Integer> current = map.entrySet().iterator().next();
        assertNotSame(old, current);
        assertEquals(Integer.valueOf(10_000), current.getKey());
        assertEquals(Integer.valueOf(0), old.getKey());
    }

    @Test
    void reversedOrderAndNullKeysStayInCanonicalArrays() {
        M3LinkedHashMap<String, String> map = new M3LinkedHashMap<>();
        map.put(null, null);
        map.put("a", "A");
        map.put("b", "B");
        map.verifyInvariants();
        assertEquals("b", map.reversed().firstEntry().getKey());
        map.reversed().putFirst("c", "C");
        map.verifyInvariants();
        assertEquals("c", map.reversed().firstEntry().getKey());
        map.remove(null);
        map.verifyInvariants();
        assertEquals(3, map.size());
    }
}
