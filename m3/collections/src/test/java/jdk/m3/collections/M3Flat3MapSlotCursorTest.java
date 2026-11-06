/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Slot cursor of the single owner (census leaf L075): nextSlot/slotKey/slotValue/setSlotValue/removeSlot/copy
 * against a java.util.HashMap model, with keys that collide and sizes crossing the inline/hash boundary.
 */
final class M3Flat3MapSlotCursorTest {
    private static final long SEED = 0x4C303735_534C4F54L;
    private static final int STEPS = 200_000;
    private static final int KEYS = 11;

    @Test
    void cursorMatchesModel() {
        Random random = new Random(SEED);
        M3Flat3Map<Integer, Integer> map = new M3Flat3Map<>();
        Map<Integer, Integer> model = new HashMap<>();
        for (int step = 0; step < STEPS; step++) {
            Integer key = random.nextInt(KEYS + 1) == KEYS ? null : random.nextInt(KEYS) * (step % 2 == 0 ? 1 : 16);
            int action = random.nextInt(10);
            if (action < 5) {
                assertEquals(model.put(key, step), map.put(key, step));
            } else if (action < 8) {
                assertEquals(model.remove(key), map.remove(key));
            } else if (action == 8) {
                sweep(map, model, random.nextInt());
            } else {
                assertEquals(snapshot(map), snapshot(map.copy()));
            }
            assertEquals(model, snapshot(map));
        }
    }

    @Test
    void copyIsIndependentAndKeepsOrder() {
        M3Flat3Map<String, Integer> map = new M3Flat3Map<>();
        for (int i = 0; i < 6; i++) map.put("k" + i, i);
        M3Flat3Map<String, Integer> copy = map.copy();
        assertEquals(order(map), order(copy));
        copy.put("extra", 1);
        copy.remove("k0");
        assertEquals(6, map.size());
        assertTrue(map.containsKey("k0"));
        assertEquals(-1, new M3Flat3Map<String, Integer>().copy().nextSlot(0));
    }

    @Test
    void deadSlotsAreRefused() {
        M3Flat3Map<String, Integer> map = new M3Flat3Map<>();
        assertThrows(IllegalStateException.class, () -> map.slotKey(0));
        map.put("a", 1);
        assertThrows(IllegalStateException.class, () -> map.slotValue(1));
        assertThrows(IllegalStateException.class, () -> map.setSlotValue(-1, 2));
        for (int i = 0; i < 5; i++) map.put("h" + i, i);
        int slot = map.nextSlot(0);
        map.removeSlot(slot);
        assertThrows(IllegalStateException.class, () -> map.removeSlot(slot));
    }

    /** Removes every entry whose (key, value) hash meets the seed and rewrites the others through the cursor. */
    private static void sweep(M3Flat3Map<Integer, Integer> map, Map<Integer, Integer> model, int seed) {
        for (int slot = map.nextSlot(0); slot >= 0; ) {
            Integer key = map.slotKey(slot);
            Integer value = map.slotValue(slot);
            if (((key == null ? 0 : key) * 31 + value ^ seed) % 3 == 0) {
                model.remove(key);
                slot = map.nextSlot(map.removeSlot(slot));
            } else {
                assertEquals(value, map.setSlotValue(slot, value + 1));
                model.put(key, value + 1);
                slot = map.nextSlot(slot + 1);
            }
        }
    }

    private static <K, V> Map<K, V> snapshot(M3Flat3Map<K, V> map) {
        Map<K, V> out = new HashMap<>();
        int count = 0;
        for (int slot = map.nextSlot(0); slot >= 0; slot = map.nextSlot(slot + 1)) {
            out.put(map.slotKey(slot), map.slotValue(slot));
            count++;
        }
        assertEquals(map.size(), count);
        return out;
    }

    private static List<Object> order(M3Flat3Map<?, ?> map) {
        List<Object> keys = new ArrayList<>();
        for (int slot = map.nextSlot(0); slot >= 0; slot = map.nextSlot(slot + 1)) keys.add(map.slotKey(slot));
        return keys;
    }
}
