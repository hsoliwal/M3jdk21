// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M3HashMapSetTest {
    private record Collision(int id) {
        @Override public int hashCode() { return 17; }
    }

    @Test void emptyOwnersAreUnallocatedAndGetDoesNotCreateEntries() {
        M3HashMap<String, Integer> map = new M3HashMap<>();
        M3HashSet<String> set = new M3HashSet<>();
        assertEquals(0, map.allocatedReferenceSlots());
        assertEquals(0, set.allocatedReferenceSlots());
        assertNull(map.get("absent"));
        assertFalse(map.containsKey(null));
        assertTrue(map.entrySet().isEmpty());
        assertFalse(set.contains("absent"));
        assertEquals(0, map.allocatedReferenceSlots());
        assertEquals(0, set.allocatedReferenceSlots());

        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        assertEquals(M3HashMap.LIVE, map.stateBits(null));
        assertTrue(map.updateState(null, M3HashMap.PINNED | M3HashMap.COLD, 0));
        assertTrue((map.stateBits(null) & M3HashMap.PINNED) != 0);
        assertThrows(IllegalArgumentException.class,
                () -> map.updateState(null, 0, M3HashMap.LIVE));
        assertNull(map.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(0, map.stateBits(null));
        map.verifyInvariants();
    }

    @Test void hashMapDifferentialWithNullsCollisionsAndTombstoneReuse() {
        Map<Collision, Integer> expected = new HashMap<>();
        M3HashMap<Collision, Integer> actual = new M3HashMap<>();
        Random random = new Random(1325);
        Collision[] keys = new Collision[301];
        for (int i = 0; i < keys.length; i++) keys[i] = new Collision(i);
        for (int step = 0; step < 20000; step++) {
            Collision key = random.nextInt(13) == 0 ? null : keys[random.nextInt(keys.length)];
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(1000);
            int op = random.nextInt(11);
            Object a, b;
            switch (op) {
                case 0 -> { a = expected.put(key, value); b = actual.put(key, value); }
                case 1 -> { a = expected.remove(key); b = actual.remove(key); }
                case 2 -> { a = expected.putIfAbsent(key, value); b = actual.putIfAbsent(key, value); }
                case 3 -> { a = expected.replace(key, value); b = actual.replace(key, value); }
                case 4 -> { a = expected.get(key); b = actual.get(key); }
                case 5 -> { a = expected.containsKey(key); b = actual.containsKey(key); }
                case 6 -> { a = expected.remove(key, value); b = actual.remove(key, value); }
                case 7 -> { a = expected.compute(key, (k, v) -> value); b = actual.compute(key, (k, v) -> value); }
                case 8 -> { a = expected.computeIfAbsent(key, k -> value); b = actual.computeIfAbsent(key, k -> value); }
                case 9 -> { a = expected.computeIfPresent(key, (k, v) -> value); b = actual.computeIfPresent(key, (k, v) -> value); }
                default -> { a = expected.getOrDefault(key, -1); b = actual.getOrDefault(key, -1); }
            }
            assertEquals(a, b, "step " + step + " operation " + op);
            if (step % 73 == 0) {
                assertEquals(expected.size(), actual.size());
                assertEquals(expected, actual);
                assertEquals(actual, expected);
                assertEquals(expected.hashCode(), actual.hashCode());
                actual.verifyInvariants();
            }
        }
        assertEquals(expected, actual);
        actual.verifyInvariants();
        assertEquals(expected, actual.clone());

        var iterator = actual.keySet().iterator();
        if (iterator.hasNext()) {
            Collision removed = iterator.next();
            iterator.remove();
            assertFalse(actual.containsKey(removed));
            assertThrows(IllegalStateException.class, iterator::remove);
        }
        if (!actual.isEmpty()) {
            Iterator<Collision> invalid = actual.keySet().iterator();
            actual.put(new Collision(9999), 9999);
            assertThrows(ConcurrentModificationException.class, invalid::next);
        }
        actual.verifyInvariants();
    }

    @Test void entryApiProjectsLiveMappingWhileStorageRemainsEntryless() {
        M3HashMap<String, Integer> map = new M3HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        map.verifyInvariants();
        assertEquals(1, map.get("a"));
        assertTrue(map.containsKey("b"));
        Map.Entry<String, Integer> e = map.entrySet().iterator().next();
        assertEquals(map.get(e.getKey()), e.getValue());
        assertEquals(e.getValue(), e.setValue(57));
        assertEquals(57, map.get(e.getKey()));
        assertThrows(ConcurrentModificationException.class,
                () -> map.compute("z", (k, v) -> { map.put("other", 7); return 5; }));
        assertFalse(map.containsKey("z"));
        map.verifyInvariants();
    }

    @Test void setDifferentialAndNoValueLane() {
        M3HashSet<Collision> actual = new M3HashSet<>();
        Set<Collision> expected = new HashSet<>();
        Random random = new Random(20261018);
        Collision[] keys = new Collision[170];
        for (int i = 0; i < keys.length; i++) keys[i] = new Collision(i);
        for (int step = 0; step < 17000; step++) {
            Collision key = random.nextInt(11) == 0 ? null : keys[random.nextInt(keys.length)];
            boolean a, b;
            if (random.nextBoolean()) {
                a = expected.add(key); b = actual.add(key);
            } else {
                a = expected.remove(key); b = actual.remove(key);
            }
            assertEquals(a, b, "set step " + step);
            if (step % 67 == 0) {
                assertEquals(expected, actual);
                assertEquals(actual, expected);
                actual.verifyInvariants();
            }
        }
        assertEquals(expected, actual);
        assertEquals(expected, actual.clone());
        Iterator<Collision> iterator = actual.iterator();
        if (iterator.hasNext()) {
            Collision removed = iterator.next();
            iterator.remove();
            assertFalse(actual.contains(removed));
        }
        actual.verifyInvariants();
        assertTrue(actual.allocatedReferenceSlots() >= actual.size());
    }
}
