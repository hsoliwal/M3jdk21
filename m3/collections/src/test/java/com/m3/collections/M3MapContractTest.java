// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class M3MapContractTest {
    static Stream<Arguments> maps() {
        return Stream.of(
                Arguments.of("hash", (Supplier<Map<Integer, Integer>>) M3LinkedHashMap::new, (Supplier<Map<Integer, Integer>>) LinkedHashMap::new),
                Arguments.of("tree", (Supplier<Map<Integer, Integer>>) M3TreeMap::new, (Supplier<Map<Integer, Integer>>) TreeMap::new),
                Arguments.of("weak", (Supplier<Map<Integer, Integer>>) M3WeakHashMap::new, (Supplier<Map<Integer, Integer>>) WeakHashMap::new),
                Arguments.of("access", (Supplier<Map<Integer, Integer>>) () -> new M3LinkedHashMap<>(true), (Supplier<Map<Integer, Integer>>) () -> new LinkedHashMap<>(16, .75f, true)));
    }
    @ParameterizedTest(name = "{0} differential") @MethodSource("maps")
    void differential(String name, Supplier<Map<Integer, Integer>> actualFactory, Supplier<Map<Integer, Integer>> oracleFactory) {
        Map<Integer, Integer> actual = actualFactory.get(), oracle = oracleFactory.get(); Random random = new Random(48191);
        // All boxed keys stay strongly reachable for the weak-map oracle.
        Integer[] keys = new Integer[401]; for (int i = 0; i < keys.length; i++) { keys[i] = i - 200; }
        for (int step = 0; step < 30000; step++) {
            Integer k = keys[random.nextInt(keys.length)], v = random.nextInt(9) == 0 ? null : random.nextInt(1000);
            int op = random.nextInt(12);
            Object expected = apply(oracle, k, v, op), found = apply(actual, k, v, op);
            assertEquals(expected, found, name + " step " + step + " op " + op);
            if (step % 79 == 0) {
                assertEquals(oracle.size(), actual.size());
                if (!name.equals("weak")) { assertEquals(new ArrayList<>(oracle.keySet()), new ArrayList<>(actual.keySet())); }
                assertEquals(oracle, actual); assertEquals(actual, oracle);
                assertEquals(oracle.hashCode(), actual.hashCode());
                if (!name.equals("weak")) { assertEquals(new ArrayList<>(oracle.keySet()), new ArrayList<>(actual.keySet())); }
                verify(actual);
            }
        }
        assertEquals(oracle, actual); verify(actual); Reference.reachabilityFence(keys);
    }
    private static Object apply(Map<Integer, Integer> map, Integer key, Integer value, int operation) {
        return switch (operation) {
            case 0 -> map.put(key, value);
            case 1 -> map.remove(key);
            case 2 -> map.putIfAbsent(key, value);
            case 3 -> map.replace(key, value);
            case 4 -> map.remove(key, value);
            case 5 -> map.replace(key, value, 77);
            case 6 -> map.getOrDefault(key, -100);
            case 7 -> map.compute(key, (k, old) -> value);
            case 8 -> map.computeIfAbsent(key, k -> value);
            case 9 -> map.computeIfPresent(key, (k, old) -> value);
            case 10 -> map.merge(key, value == null ? 1 : value, (a, b) -> (a + b) % 5 == 0 ? null : a + b);
            default -> map.containsKey(key);
        };
    }
    private static void verify(Map<?, ?> map) {
        if (map instanceof M3TreeMap<?, ?> tree) { tree.verifyInvariants(); }
        if (map instanceof M3LinkedHashMap<?, ?> hash) { hash.verifyInvariants(); }
    }
    @Test void treeRotationsAndEveryDeletionOrder() {
        for (int trial = 0; trial < 80; trial++) {
            var map = new M3TreeMap<Integer, Integer>(); var order = new ArrayList<Integer>();
            for (int i = 0; i < 500; i++) { order.add(i); }
            if (trial % 3 == 0) { Collections.reverse(order); } else if (trial % 3 == 1) { Collections.shuffle(order, new Random(trial)); }
            for (int i : order) { map.put(i, i); map.verifyInvariants(); }
            Collections.shuffle(order, new Random(trial + 140));
            for (int i : order) { assertEquals(i, map.remove(i)); map.verifyInvariants(); }
            assertTrue(map.isEmpty());
        }
    }
    @Test void treeRangesNavigationAndMutation() {
        var actual = new M3TreeMap<Integer, Integer>(); var oracle = new TreeMap<Integer, Integer>();
        for (int i = -30; i <= 30; i++) { actual.put(i, i); oracle.put(i, i); }
        for (int lower = -35; lower <= 30; lower += 5) {
            for (int upper = lower; upper <= 35; upper += 5) {
                for (int flags = 0; flags < 8; flags++) {
                    NavigableMap<Integer, Integer> a = actual.subMap(lower, (flags & 1) != 0, upper, (flags & 2) != 0);
                    NavigableMap<Integer, Integer> b = oracle.subMap(lower, (flags & 1) != 0, upper, (flags & 2) != 0);
                    if ((flags & 4) != 0) { a = a.descendingMap(); b = b.descendingMap(); }
                    assertEquals(b, a); assertEquals(new ArrayList<>(b.keySet()), new ArrayList<>(a.keySet()));
                    assertEquals(b.firstEntry(), a.firstEntry()); assertEquals(b.lastEntry(), a.lastEntry());
                    for (int k = -40; k <= 40; k++) {
                        assertEquals(b.lowerEntry(k), a.lowerEntry(k)); assertEquals(b.floorEntry(k), a.floorEntry(k));
                        assertEquals(b.ceilingEntry(k), a.ceilingEntry(k)); assertEquals(b.higherEntry(k), a.higherEntry(k));
                    }
                }
            }
        }
        var a = actual.subMap(-10, false, 10, false).descendingMap();
        var b = oracle.subMap(-10, false, 10, false).descendingMap();
        a.subMap(8, false, -8, true).clear(); b.subMap(8, false, -8, true).clear(); assertEquals(oracle, actual);
        assertThrows(IllegalArgumentException.class, () -> a.put(10, 10));
        assertThrows(IllegalArgumentException.class, () -> a.tailMap(-10, true));
        assertTrue(a.tailMap(-10, false).isEmpty()); actual.verifyInvariants();
        assertThrows(UnsupportedOperationException.class, () -> actual.firstEntry().setValue(9));
    }
    @Test void comparatorNullKeysAndEquality() {
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.reverseOrder());
        var a = new M3TreeMap<Integer, String>(comparator); var b = new TreeMap<Integer, String>(comparator);
        for (Integer key : new Integer[] {2, null, 4, 0, -4}) { a.put(key, "v"); b.put(key, "v"); }
        assertEquals(new ArrayList<>(b.keySet()), new ArrayList<>(a.keySet()));
        assertEquals(b.headMap(0, true), a.headMap(0, true));
        assertEquals(b.descendingMap().tailMap(null, true), a.descendingMap().tailMap(null, true));
        a.verifyInvariants(); assertEquals("v", a.remove(null)); a.verifyInvariants();
        var caseMap = new M3TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        caseMap.put("Key", 1); assertEquals(1, caseMap.put("KEY", 2)); assertEquals("Key", caseMap.firstKey());
    }
    @ParameterizedTest(name = "{0} views") @MethodSource("maps")
    void backedViewsAndCallbacks(String name, Supplier<Map<Integer, Integer>> actualFactory, Supplier<Map<Integer, Integer>> ignored) {
        var map = actualFactory.get(); map.put(1, null); map.put(2, 20); map.put(3, 30);
        assertTrue(map.keySet().remove(1)); assertFalse(map.containsKey(1));
        var entries = map.entrySet().iterator(); var entry = entries.next(); Integer key = entry.getKey();
        assertNotNull(entry.setValue(99)); assertEquals(99, map.get(key));
        // Access-order get may invalidate an iterator; create a fresh one for iterator removal.
        var it = map.keySet().iterator(); key = it.next(); it.remove(); assertFalse(map.containsKey(key));
        assertThrows(IllegalStateException.class, it::remove);
        map.put(4, 40); map.put(5, 50); var invalid = map.values().iterator(); map.put(6, 60);
        assertThrows(ConcurrentModificationException.class, invalid::next);
        assertThrows(ConcurrentModificationException.class, () -> map.compute(9, (k, v) -> { map.put(10, 100); return 90; }));
        assertFalse(map.containsKey(9)); assertEquals(100, map.get(10));
        assertThrows(ConcurrentModificationException.class, () -> map.forEach((k, v) -> map.put(500, 1)));
        map.replaceAll((k, v) -> k); map.forEach((k, v) -> assertEquals(k, v)); verify(map);
        map.values().clear(); assertTrue(map.isEmpty());
    }
    private record Collision(int value) { @Override public int hashCode() { return 7; } }
    @Test void collisionChainsRecyclingAndNull() {
        var a = new M3LinkedHashMap<Collision, Integer>(); var b = new HashMap<Collision, Integer>();
        Random r = new Random(183);
        for (int i = 0; i < 15000; i++) {
            Collision key = i % 101 == 0 ? null : new Collision(r.nextInt(500));
            if (r.nextBoolean()) { assertEquals(b.put(key, i), a.put(key, i)); }
            else { assertEquals(b.remove(key), a.remove(key)); }
            if (i % 199 == 0) { assertEquals(b, a); a.verifyInvariants(); }
        }
        a.clear(); a.put(null, null); assertTrue(a.keySet().remove(null)); a.verifyInvariants();
    }
    @Test void sequencedAndReversedHashViews() {
        for (boolean access : new boolean[] {false, true}) {
            var a = new M3LinkedHashMap<Integer, Integer>(access); var b = new LinkedHashMap<Integer, Integer>(16, .75f, access);
            Random r = new Random(835);
            for (int i = 0; i < 8000; i++) {
                int k = r.nextInt(50), op = r.nextInt(10);
                var av = i % 2 == 0 ? a : a.reversed(); var bv = i % 2 == 0 ? b : b.reversed();
                Object expected = switch (op) {
                    case 0 -> bv.putFirst(k, i); case 1 -> bv.putLast(k, i); case 2 -> bv.put(k, i);
                    case 3 -> bv.pollFirstEntry(); case 4 -> bv.pollLastEntry(); case 5 -> bv.get(k);
                    case 6 -> bv.remove(k); case 7 -> bv.putIfAbsent(k, i); case 8 -> bv.replace(k, i); default -> bv.compute(k, (x, v) -> k);
                };
                Object found = switch (op) {
                    case 0 -> av.putFirst(k, i); case 1 -> av.putLast(k, i); case 2 -> av.put(k, i);
                    case 3 -> av.pollFirstEntry(); case 4 -> av.pollLastEntry(); case 5 -> av.get(k);
                    case 6 -> av.remove(k); case 7 -> av.putIfAbsent(k, i); case 8 -> av.replace(k, i); default -> av.compute(k, (x, v) -> k);
                };
                assertEquals(expected, found); assertEquals(new ArrayList<>(bv.keySet()), new ArrayList<>(av.keySet()));
                assertEquals(new ArrayList<>(bv.values()), new ArrayList<>(av.values())); a.verifyInvariants();
            }
            a.putFirst(90, 90); assertEquals(90, a.sequencedKeySet().getFirst());
            assertEquals(a, new HashMap<>(a)); assertEquals(a.entrySet().hashCode(), a.hashCode());
            assertThrows(UnsupportedOperationException.class, () -> a.sequencedKeySet().addFirst(3));
            var reverseValues = a.sequencedValues().reversed(); assertEquals(a.lastEntry().getValue(), reverseValues.removeFirst());
            a.verifyInvariants();
        }
    }
    @Test void iteratorDeletionPreservesAllTreeSuccessors() {
        for (boolean reverse : new boolean[] {false, true}) {
            var map = new M3TreeMap<Integer, Integer>(); for (int i = 0; i < 1000; i++) { map.put(i, i); }
            Iterator<Integer> it = (reverse ? map.descendingMap() : map).keySet().iterator(); int count = 0;
            while (it.hasNext()) { assertEquals(reverse ? 999 - count : count, it.next()); it.remove(); map.verifyInvariants(); count++; }
            assertEquals(1000, count); assertTrue(map.isEmpty());
        }
    }
    @Test void weakCleanupIsDeterministicAndDoesNotDeleteReusedKeys() throws Exception {
        var map = new M3WeakHashMap<Object, String>(); Object key = new Object(); map.put(key, "old"); map.put(null, "null");
        Field field = M3WeakHashMap.class.getDeclaredField("references"); field.setAccessible(true);
        Reference<?> ref = Stream.of((Object[]) field.get(map)).filter(Reference.class::isInstance).map(Reference.class::cast).findFirst().orElseThrow();
        ref.clear(); ref.enqueue(); map.expungeStaleKeys(); assertEquals(1, map.size()); assertEquals("null", map.get(null));
        map.put(key, "new"); map.expungeStaleKeys(); assertEquals("new", map.get(key));
        Object[] held = new Object[1000]; for (int i = 0; i < held.length; i++) { held[i] = new Object(); map.put(held[i], "v"); }
        assertEquals(1002, map.size()); map.clear(); assertTrue(map.isEmpty()); Reference.reachabilityFence(held); Reference.reachabilityFence(key);
    }
    @Test void sequencedEndpointsUseLiveAndDetachedEntriesCorrectly() {
        for (M3OrderedSlotMap<Integer, Integer> map : List.of(new M3LinkedHashMap<Integer, Integer>(), new M3TreeMap<Integer, Integer>())) {
            map.put(1, 10); map.put(2, 20);
            var first = map.sequencedEntrySet().getFirst(); assertEquals(10, first.setValue(11)); assertEquals(11, map.get(1));
            var removed = map.sequencedEntrySet().removeLast(); assertEquals(20, removed.setValue(21)); assertFalse(map.containsKey(2));
            assertEquals(1, map.sequencedKeySet().getFirst()); assertEquals(11, map.sequencedValues().getLast());
            assertEquals(11, map.sequencedValues().reversed().removeFirst()); assertTrue(map.isEmpty());
        }
    }
    @Test void nestedRangeExceptionContractsMatchTreeMap() {
        for (int flags = 0; flags < 8; flags++) {
            NavigableMap<Integer, Integer> a = new M3TreeMap<Integer, Integer>().subMap(0, (flags & 1) != 0, 10, (flags & 2) != 0);
            NavigableMap<Integer, Integer> b = new TreeMap<Integer, Integer>().subMap(0, (flags & 1) != 0, 10, (flags & 2) != 0);
            if ((flags & 4) != 0) { a = a.descendingMap(); b = b.descendingMap(); }
            for (int endpoint = -1; endpoint <= 11; endpoint++) {
                for (boolean inclusive : new boolean[] {false, true}) {
                    int key = endpoint; NavigableMap<Integer, Integer> av = a, bv = b;
                    assertEquals(outcome(() -> bv.headMap(key, inclusive)), outcome(() -> av.headMap(key, inclusive)));
                    assertEquals(outcome(() -> bv.tailMap(key, inclusive)), outcome(() -> av.tailMap(key, inclusive)));
                    assertEquals(outcome(() -> bv.subMap(key, inclusive, key, inclusive)), outcome(() -> av.subMap(key, inclusive, key, inclusive)));
                }
            }
        }
    }
    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); } catch (IllegalArgumentException failure) { return failure.getClass(); }
    }
    @Test void entryAndKeyViewsRemoveNullValuedMappings() {
        var map = new M3LinkedHashMap<String, String>(); map.put(null, null); map.put("x", null);
        assertTrue(map.entrySet().remove(new java.util.AbstractMap.SimpleEntry<>(null, null)));
        assertTrue(map.keySet().remove("x")); assertTrue(map.isEmpty());
        assertNull(map.pollFirstEntry());
    }
}
