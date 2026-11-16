/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the pair and triple keyed map. */
final class MultiKeyMapContractTest {
    /** A key whose hash keeps only a few bits, so probe runs collide, wrap around and close their gaps. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    private static Object lane(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    /** Independent model: a HashMap keyed by the list of key components; the list length is the arity. */
    private static List<Object> key(Object... components) {
        return Arrays.asList(components);
    }

    /** Slots 0..size-1 are live and nothing lies behind them; the hash lane names every live slot exactly once. */
    private static void checkLanes(MultiKeyMap<Crowded, String> map, Map<List<Object>, String> model) {
        Object[] key1 = (Object[]) lane(map, "key1Lane");
        Object[] key3 = (Object[]) lane(map, "key3Lane");
        Object[] values = (Object[]) lane(map, "valueLane");
        int[] index = (int[]) lane(map, "hashIndexLane");
        long[] states = (long[]) lane(map, "stateMaskWords");
        assertEquals(model.size(), map.size());
        assertEquals(model.isEmpty(), map.isEmpty());
        assertTrue(index.length == 0 || 2 * model.size() <= index.length, "hash lane at most half full");
        Set<Integer> named = new HashSet<>();
        for (int entry : index) {
            if (entry != 0) {
                assertTrue(entry >= 1 && entry <= model.size(), "hash lane names a live slot");
                assertTrue(named.add(entry), "a slot named twice");
            }
        }
        assertEquals(model.size(), named.size(), "every live slot is named");
        for (int slot = 0; slot < values.length; slot++) {
            boolean occupied = (states[slot >>> 6] >>> slot & MultiKeyMap.STATE_OCCUPIED) != 0;
            assertEquals(slot < model.size(), occupied, "OCCUPIED bit of slot " + slot);
            if (slot >= model.size()) {
                assertNull(key1[slot], "stale key in slot " + slot);
                assertNull(key3[slot], "stale third key in slot " + slot);
                assertNull(values[slot], "stale value in slot " + slot);
            }
        }
        for (Map.Entry<List<Object>, String> entry : model.entrySet()) {
            List<Object> k = entry.getKey();
            String found = k.size() == 2 ? map.get(k.get(0), k.get(1)) : map.get(k.get(0), k.get(1), k.get(2));
            assertEquals(entry.getValue(), found, "every mapping is reachable");
        }
    }

    @Test
    void pairsAndTriplesAreDifferentKeys() {
        MultiKeyMap<String, Integer> map = new MultiKeyMap<>();
        assertTrue(map.isEmpty());
        assertNull(map.get("a", "b"));
        assertFalse(map.containsKey("a", "b"));
        assertNull(map.removeMultiKey("a", "b"));
        assertFalse(map.removeAll("a"));
        assertNull(map.put("a", "b", 1));
        assertNull(map.put("a", "b", null, 2));
        assertNull(map.put("b", "a", 3));
        assertNull(map.put(null, null, 4));
        assertEquals(4, map.size());
        assertEquals(1, map.get("a", "b"));
        assertEquals(2, map.get("a", "b", null));
        assertEquals(3, map.get("b", "a"));
        assertEquals(4, map.get(null, null));
        assertNull(map.get(null, null, null));
        assertEquals(1, map.put("a", "b", 5));
        assertEquals(5, map.removeMultiKey("a", "b"));
        assertFalse(map.containsKey("a", "b"));
        assertEquals(2, map.get("a", "b", null), "removing the pair keeps the triple");
        assertTrue(map.removeAll("a"));
        assertNull(map.get("a", "b", null));
        assertEquals(2, map.size());
        map.clear();
        assertTrue(map.isEmpty());
        assertNull(map.get("b", "a"));
        assertEquals("MultiKeyMap|MULTIKEY_MAP|OBJECT|OCCUPIED,DELETED|Object[] key1Lane;Object[] key2Lane;Object[] key3Lane;"
                + "Object[] valueLane;int[] hashIndexLane", MultiKeyMap.scaffoldDescriptor());
    }

    @Test
    void randomOperationsMatchAHashMapModel() {
        for (int seed = 0; seed < 8; seed++) {
            SplittableRandom random = new SplittableRandom(seed * 104729L + 13);
            int bits = seed % 2 == 0 ? 0x3 : -1;
            MultiKeyMap<Crowded, String> map = new MultiKeyMap<>();
            Map<List<Object>, String> model = new HashMap<>();
            for (int step = 0; step < 8000; step++) {
                Crowded k1 = random.nextInt(25) == 0 ? null : new Crowded(random.nextInt(12), bits);
                Crowded k2 = random.nextInt(25) == 0 ? null : new Crowded(random.nextInt(12), bits);
                Crowded k3 = random.nextInt(8) == 0 ? null : new Crowded(random.nextInt(4), bits);
                String value = random.nextInt(15) == 0 ? null : "v" + random.nextInt(30);
                switch (random.nextInt(12)) {
                    case 0, 1, 2 -> assertEquals(model.put(key(k1, k2), value), map.put(k1, k2, value));
                    case 3, 4, 5 -> assertEquals(model.put(key(k1, k2, k3), value), map.put(k1, k2, k3, value));
                    case 6 -> assertEquals(model.remove(key(k1, k2)), map.removeMultiKey(k1, k2));
                    case 7 -> assertEquals(model.keySet().removeIf(k -> Objects.equals(k.get(0), k1)), map.removeAll(k1));
                    case 8 -> assertEquals(model.get(key(k1, k2)), map.get(k1, k2));
                    case 9 -> assertEquals(model.get(key(k1, k2, k3)), map.get(k1, k2, k3));
                    case 10 -> assertEquals(model.containsKey(key(k1, k2)), map.containsKey(k1, k2));
                    default -> {
                        if (random.nextInt(60) == 0) {
                            map.clear();
                            model.clear();
                        }
                    }
                }
                assertEquals(model.size(), map.size());
                if (step % 67 == 0) {
                    checkLanes(map, model);
                }
            }
            checkLanes(map, model);
        }
    }
}
