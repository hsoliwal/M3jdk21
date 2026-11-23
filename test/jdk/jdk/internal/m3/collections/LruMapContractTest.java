/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the bounded least-recently-used map. */
final class LruMapContractTest {
    /** A key whose hash keeps only a few bits, so probe runs collide, wrap and close their gaps. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    /** Independent oracle: a list from the least to the most recently used mapping, searched linearly. */
    private static final class ListModel {
        private final List<Object[]> order = new ArrayList<>();
        private final int maxSize;

        ListModel(int maxSize) { this.maxSize = maxSize; }

        private int at(Object key) {
            for (int i = 0; i < order.size(); i++) {
                if (Objects.equals(order.get(i)[0], key)) {
                    return i;
                }
            }
            return -1;
        }

        Object get(Object key, boolean touch) {
            int i = at(key);
            if (i < 0) {
                return null;
            }
            Object[] entry = touch ? order.remove(i) : order.get(i);
            if (touch) {
                order.add(entry);
            }
            return entry[1];
        }

        Object put(Object key, Object value) {
            int i = at(key);
            if (i >= 0) {
                Object[] entry = order.remove(i);
                Object old = entry[1];
                entry[1] = value;
                order.add(entry);
                return old;
            }
            if (order.size() >= maxSize) {
                order.remove(0);
            }
            order.add(new Object[] {key, value});
            return null;
        }

        Object remove(Object key) {
            int i = at(key);
            return i < 0 ? null : order.remove(i)[1];
        }
    }

    private static Object field(LruMap<?, ?> map, String name) throws Exception {
        Field field = LruMap.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(map);
    }

    /** Decode the current two-bit physical-slot contract independently of the owner. */
    private static long state(long[] words, int slot) {
        return (words[slot / 32] >>> (2 * (slot % 32))) & 3L;
    }

    /** The links run from head to tail over occupied slots. Returns the number of linked slots. */
    private static int linked(LruMap<?, ?> map) throws Exception {
        int[] next = (int[]) field(map, "nextLane");
        int[] previous = (int[]) field(map, "prevLane");
        long[] states = (long[]) field(map, "stateMaskWords");
        int count = 0;
        int before = -1;
        for (int slot = (Integer) field(map, "head"); slot >= 0; before = slot, slot = next[slot], count++) {
            assertEquals(LruMap.STATE_OCCUPIED, state(states, slot), "linked slot is occupied");
            assertEquals(before, previous[slot], "back link");
        }
        assertEquals(before, (Integer) field(map, "tail"), "tail is the last linked slot");
        return count;
    }

    /** The free chain holds only deleted slots. Returns the number of free slots. */
    private static int freed(LruMap<?, ?> map) throws Exception {
        int[] next = (int[]) field(map, "nextLane");
        long[] states = (long[]) field(map, "stateMaskWords");
        int count = 0;
        for (int slot = (Integer) field(map, "free"); slot >= 0; slot = next[slot], count++) {
            assertEquals(LruMap.STATE_DELETED, state(states, slot), "free slot is deleted");
        }
        return count;
    }

    /** The hash lane names only occupied slots. Returns the number of its entries. */
    private static int indexed(LruMap<?, ?> map) throws Exception {
        int[] index = (int[]) field(map, "hashIndexLane");
        long[] states = (long[]) field(map, "stateMaskWords");
        int count = 0;
        for (int entry : index) {
            if (entry != 0) {
                count++;
                assertEquals(LruMap.STATE_OCCUPIED, state(states, entry - 1), "hash lane names a live slot");
            }
        }
        assertTrue(index.length == 0 || 2 * map.size() <= index.length, "hash lane at most half full");
        return count;
    }

    /** The lanes agree: links, states, free chain and hash lane describe exactly the live mappings. */
    private static void lanesAgree(LruMap<?, ?> map) throws Exception {
        int used = (Integer) field(map, "used");
        int linked = linked(map);
        assertEquals(map.size(), linked, "linked slots");
        assertEquals(used, linked + freed(map), "every used slot is linked or free");
        assertEquals(map.size(), indexed(map), "hash lane entries");
        assertTrue(used <= map.maxSize(), "slots stay within the bound");
    }

    private static void sameOrder(ListModel model, LruMap<Object, Object> map) {
        List<Object> keys = new ArrayList<>(map.asMap().keySet());
        List<Object> values = new ArrayList<>(map.asMap().values());
        assertEquals(model.order.size(), map.size());
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(model.order.get(i)[0], keys.get(i), "key order at " + i);
            assertEquals(model.order.get(i)[1], values.get(i), "value order at " + i);
        }
        assertEquals(model.order.isEmpty(), map.isEmpty());
        assertEquals(model.order.size() >= model.maxSize, map.isFull());
        if (!model.order.isEmpty()) {
            assertEquals(model.order.get(0)[0], map.firstKey());
            assertEquals(model.order.get(model.order.size() - 1)[0], map.lastKey());
        }
    }

    private static Object key(SplittableRandom random, int universe, int bits) {
        int id = random.nextInt(universe + 1);
        return id == universe ? null : bits < 0 ? (Object) id : new Crowded(id, bits);
    }

    private static void step(SplittableRandom random, ListModel model, LruMap<Object, Object> map, Object key) {
        Object value = random.nextInt(8) == 0 ? null : random.nextInt(1000);
        switch (random.nextInt(16)) {
            case 0, 1, 2, 3, 4, 5 -> assertEquals(model.put(key, value), map.put(key, value));
            case 6, 7, 8 -> assertEquals(model.get(key, true), map.get(key));
            case 9 -> assertEquals(model.get(key, false), map.get(key, false));
            case 10, 11 -> assertEquals(model.remove(key), map.remove(key));
            case 12 -> assertEquals(model.at(key) >= 0, map.containsKey(key));
            case 13 -> assertEquals(model.get(key, true), map.asMap().get(key));
            case 14 -> assertEquals(model.put(key, value), map.asMap().put(key, value));
            default -> clearSometimes(random, model, map);
        }
    }

    private static void clearSometimes(SplittableRandom random, ListModel model, LruMap<Object, Object> map) {
        if (random.nextInt(64) == 0) {
            model.order.clear();
            map.clear();
        }
    }

    private static void rounds(SplittableRandom random, int maxSize, int bits) throws Exception {
        ListModel model = new ListModel(maxSize);
        LruMap<Object, Object> map = new LruMap<>(maxSize);
        for (int round = 0; round < 4000; round++) {
            step(random, model, map, key(random, 3 * maxSize + 2, bits));
            if ((round & 15) == 0) {
                sameOrder(model, map);
                lanesAgree(map);
            }
        }
        sameOrder(model, map);
        lanesAgree(map);
        assertEquals(maxSize, map.maxSize());
    }

    @Test void randomOperationsMatchTheListModelAndKeepTheLanesConsistent() throws Exception {
        SplittableRandom random = new SplittableRandom(20261116);
        for (int maxSize : new int[] {1, 2, 3, 4, 5, 7, 16, 33, 100}) {
            for (int bits : new int[] {-1, 0, 1, 7}) {
                rounds(random, maxSize, bits);
            }
        }
    }

    @Test void aFullMapReplacesTheLeastRecentlyUsedMappingInItsSlot() throws Exception {
        LruMap<String, Integer> map = new LruMap<>(3);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(1, map.get("a"));
        assertNull(map.put("d", 4));
        assertFalse(map.containsKey("b"));
        assertEquals(List.of("c", "a", "d"), new ArrayList<>(map.asMap().keySet()));
        assertEquals(3, (Integer) field(map, "used"), "eviction reuses the slot and takes no new one");
        assertEquals(3, map.get("c", false));
        assertEquals("c", map.firstKey(), "a lookup that does not update keeps the order");
        assertEquals(3, map.put("c", 30));
        assertEquals("c", map.lastKey());
    }

    private static LruMap<String, Integer> abc() {
        LruMap<String, Integer> map = new LruMap<>(4);
        map.asMap().putAll(Map.of("a", 1));
        map.asMap().put("b", 2);
        map.asMap().put("c", 3);
        return map;
    }

    @Test void theMapViewBehavesLikeAMap() {
        LruMap<String, Integer> map = abc();
        Map<String, Integer> view = map.asMap();
        assertSame(view, map.asMap());
        assertEquals(new HashMap<>(Map.of("a", 1, "b", 2, "c", 3)), view);
        assertEquals(new HashMap<>(Map.of("a", 1, "b", 2, "c", 3)).hashCode(), view.hashCode());
        assertEquals("{a=1, b=2, c=3}", view.toString());
        assertTrue(view.containsValue(3));
        view.clear();
        assertTrue(map.isEmpty());
        assertEquals("{}", view.toString());
        assertFalse(view.entrySet().iterator().hasNext());
    }

    @Test void theMapViewWritesThrough() {
        LruMap<String, Integer> map = abc();
        Map<String, Integer> view = map.asMap();
        Map.Entry<String, Integer> first = view.entrySet().iterator().next();
        assertEquals(1, first.setValue(10));
        assertEquals(10, map.get("a", false));
        assertEquals(Map.entry("a", 10), first);
        assertTrue(view.keySet().remove("b"));
        assertFalse(map.containsKey("b"));
        assertTrue(view.values().remove(3));
        assertEquals(1, map.size());
    }

    private static LruMap<Integer, Integer> counted(int size) {
        LruMap<Integer, Integer> map = new LruMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        return map;
    }

    @Test void iteratorsRemoveInPlace() {
        LruMap<Integer, Integer> map = counted(8);
        Iterator<Integer> keys = map.asMap().keySet().iterator();
        assertThrows(IllegalStateException.class, keys::remove);
        while (keys.hasNext()) {
            if ((keys.next() & 1) == 0) {
                keys.remove();
                assertThrows(IllegalStateException.class, keys::remove);
            }
        }
        assertThrows(NoSuchElementException.class, keys::next);
        assertEquals(List.of(1, 3, 5, 7), new ArrayList<>(map.asMap().keySet()));
    }

    @Test void iteratorsFailWhenTheMapChangesBehindThem() {
        LruMap<Integer, Integer> map = counted(4);
        Iterator<Integer> reordered = map.asMap().keySet().iterator();
        map.get(0);
        assertThrows(ConcurrentModificationException.class, reordered::next);
        Iterator<Integer> untouched = map.asMap().keySet().iterator();
        map.get(0);
        map.get(1, false);
        map.put(0, 100);
        assertEquals(1, untouched.next(), "the most recently used mapping moves nowhere, so nothing changed");
        map.remove(3);
        assertThrows(ConcurrentModificationException.class, untouched::remove);
        Iterator<Integer> onEmpty = new LruMap<Integer, Integer>(2).asMap().keySet().iterator();
        assertFalse(onEmpty.hasNext());
    }

    @Test void boundsAndEmptyMapsKeepTheirExceptions() {
        assertThrows(IllegalArgumentException.class, () -> new LruMap<String, String>(0));
        assertThrows(IllegalArgumentException.class, () -> new LruMap<String, String>(-1));
        LruMap<String, String> map = new LruMap<>();
        assertEquals(LruMap.DEFAULT_MAX_SIZE, map.maxSize());
        assertThrows(NoSuchElementException.class, map::firstKey);
        assertThrows(NoSuchElementException.class, map::lastKey);
        assertNull(map.get("absent"));
        assertNull(map.remove("absent"));
        assertFalse(map.containsKey(null));
    }

    @Test void aNullKeyIsAnOrdinaryKey() {
        LruMap<String, String> map = new LruMap<>(1);
        map.put("a", "1");
        assertNull(map.put(null, null));
        assertTrue(map.containsKey(null), "a null key that replaced the least recently used mapping stays findable");
        assertNull(map.firstKey());
        assertNull(map.put(null, "2"));
        assertEquals("2", map.get(null));
        map.clear();
        assertThrows(NoSuchElementException.class, map::firstKey);
        assertEquals("LruMap|LRU_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] prevLane;"
                + "int[] nextLane;int[] hashIndexLane;int[] hashLane", LruMap.scaffoldDescriptor());
    }
}
