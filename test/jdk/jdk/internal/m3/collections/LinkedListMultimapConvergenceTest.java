/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Live object multimap views, slot generations, callback failures and independent ordered-pair oracle.
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED LinkedListMultimapConvergenceTest
 */

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.LinkedListMultimap;

public class LinkedListMultimapConvergenceTest {
    private static long checks;
    private static final int OPERATIONS = 40000;

    public static void main(String[] args) throws Exception {
        viewsAndOrder();
        cursorsAndEntries();
        callbacksAndFailureRecovery();
        storageAndGeneration();
        randomOracle();
        System.out.println("LINKED_LIST_MULTIMAP_PASS checks=" + checks + " random_operations=" + OPERATIONS);
    }

    private static void viewsAndOrder() {
        LinkedListMultimap<String, Integer> map = new LinkedListMultimap<>();
        List<Integer> a = map.get("a");
        Set<String> keys = map.keySet();
        List<Integer> values = map.values();
        Map<String, Collection<Integer>> asMap = map.asMap();
        eq(true, map.isEmpty());
        eq(List.of(), a);
        eq(null, asMap.get("a"));
        a.add(1);
        map.put("b", 2);
        a.add(3);
        map.put(null, null);
        a.add(1, 4);
        eq(Arrays.asList(1, 2, 4, 3, null), values);
        eq(Arrays.asList("a", "b", null), new ArrayList<>(keys));
        eq(Arrays.asList(1, 4, 3), a);
        List<Integer> old = map.replaceValues("a", Arrays.asList(10, 11, 12, 13));
        eq(Arrays.asList(1, 4, 3), old);
        eq(Arrays.asList(10, 2, 11, 12, null, 13), values);
        expect(UnsupportedOperationException.class, () -> old.add(9));
        eq(Arrays.asList(10, 11, 12, 13), map.replaceValues("a", a));
        eq(Arrays.asList(10, 2, 11, 12, null, 13), values);
        eq(Arrays.asList(10, 11, 12, 13), map.replaceValues("a", Arrays.asList(20, 21)));
        eq(Arrays.asList(20, 2, 21, null), values);
        eq(Arrays.asList(20, 21), asMap.remove("a"));
        eq(List.of(), a);
        a.add(30);
        eq(Arrays.asList("b", null, "a"), new ArrayList<>(keys));
        eq(30, asMap.get("a").iterator().next());
        eq(true, map.containsEntry(null, null));
        eq(true, map.containsKey(null));
        eq(Arrays.asList((Integer) null), map.removeAll(null));
        eq(false, map.containsKey(null));
        expect(UnsupportedOperationException.class, () -> map.removeAll("missing").clear());
        expect(UnsupportedOperationException.class, () -> asMap.put("x", List.of(1)));
        expect(UnsupportedOperationException.class, () -> values.add(9));
        expect(UnsupportedOperationException.class, () -> map.entries().add(new SimpleEntry<>("x", 1)));
        expect(UnsupportedOperationException.class, () -> map.entries().set(0, new SimpleEntry<>("x", 1)));
        Iterator<Map.Entry<String, Collection<Integer>>> iterator = asMap.entrySet().iterator();
        Map.Entry<String, Collection<Integer>> entry = iterator.next();
        eq("b", entry.getKey());
        expect(UnsupportedOperationException.class, () -> entry.setValue(List.of(7)));
        entry.getValue().clear();
        eq(false, map.containsKey("b"));
        asMap.values().iterator().next().clear();
        eq(true, map.isEmpty());
        eq(Set.of(), keys);
        eq(List.of(), values);
        eq(Map.of(), asMap);
    }

    private static void cursorsAndEntries() {
        LinkedListMultimap<String, Integer> map = new LinkedListMultimap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 3);
        map.put("b", 4);
        ListIterator<Integer> key = map.get("a").listIterator(2);
        eq(3, key.previous());
        key.set(30);
        key.add(20);
        eq(20, key.previous());
        key.remove();
        eq(30, key.next());
        key.remove();
        eq(Arrays.asList(1, 2, 4), map.values());
        eq(false, key.hasNext());
        key.add(5);
        eq(Arrays.asList(1, 2, 4, 5), map.values());
        expect(IllegalStateException.class, key::remove);
        ListIterator<Integer> global = map.values().listIterator(map.size());
        eq(5, global.previous());
        global.remove();
        eq(4, global.previous());
        global.set(40);
        eq(2, global.previous());
        global.remove();
        eq(Arrays.asList(1, 40), map.values());
        expect(NoSuchElementException.class, () -> map.values().listIterator(0).previous());
        expect(IndexOutOfBoundsException.class, () -> map.get("a").listIterator(-1));
        expect(IndexOutOfBoundsException.class, () -> map.values().listIterator(3));
        Map.Entry<String, Integer> escaped = map.entries().get(0);
        Map.Entry<String, Integer> peer = map.entries().get(0);
        check(escaped != peer, "entry projections must be caller-owned");
        eq(1, escaped.setValue(10));
        eq(10, peer.getValue());
        map.get("a").set(0, 11);
        eq(11, escaped.getValue());
        eq(new SimpleEntry<>("a", 11), escaped);
        eq(new SimpleEntry<>("a", 11).hashCode(), escaped.hashCode());
        map.removeAll("a");
        eq("a", escaped.getKey());
        eq(null, escaped.getValue());
        expect(IllegalStateException.class, () -> escaped.setValue(100));
        map.put("a", 99);
        eq(null, escaped.getValue());
        eq(List.of(99), map.get("a"));
        expect(IllegalStateException.class, () -> escaped.setValue(100));
        Map.Entry<String, Integer> cleared = map.entries().get(0);
        map.clear();
        map.put("b", 1000);
        eq(null, cleared.getValue());
        expect(IllegalStateException.class, () -> cleared.setValue(2000));
        Iterator<Integer> invalidated = map.get("b").iterator();
        map.put("b", 1001);
        expect(ConcurrentModificationException.class, invalidated::hasNext);
        Iterator<String> keys = map.keySet().iterator();
        eq("b", keys.next());
        keys.remove();
        eq(true, map.isEmpty());
        expect(IllegalStateException.class, keys::remove);
        for (int n = 0; n < 20; n++) { map.put(n % 2 == 0 ? "a" : "b", n); }
        keys = map.keySet().iterator();
        eq("a", keys.next());
        keys.remove();
        eq("b", keys.next());
        keys.remove();
        eq(false, keys.hasNext());
        eq(true, map.isEmpty());
    }

    private static final class CollisionKey {
        final int id;
        boolean fail;
        RuntimeException equalsFailure;
        Runnable callback;
        CollisionKey(int id) { this.id = id; }
        @Override public int hashCode() {
            if (callback != null) { callback.run(); }
            if (fail) { throw new ArithmeticException("hash failure " + id); }
            return 0;
        }
        @Override public boolean equals(Object other) {
            if (equalsFailure != null) { throw equalsFailure; }
            return other instanceof CollisionKey k && id == k.id;
        }
    }

    private static void callbacksAndFailureRecovery() {
        LinkedListMultimap<Object, Object> equalityOwner = new LinkedListMultimap<>();
        CollisionKey equalityKey = new CollisionKey(10);
        equalityOwner.put(equalityKey, "unchanged");
        RuntimeException equalityMarker = new RuntimeException("key equality failure");
        equalityKey.equalsFailure = equalityMarker;
        try {
            equalityOwner.containsKey(new CollisionKey(10));
            throw new AssertionError("missing equality callback failure");
        } catch (RuntimeException actual) {
            check(actual == equalityMarker, "original key equality exception identity");
        }
        eq(List.of("unchanged"), equalityOwner.values());
        equalityKey.equalsFailure = null;
        eq(List.of("unchanged"), equalityOwner.get(new CollisionKey(10)));
        LinkedListMultimap<Object, Object> map = new LinkedListMultimap<>();
        CollisionKey a = new CollisionKey(1);
        CollisionKey b = new CollisionKey(2);
        CollisionKey c = new CollisionKey(3);
        map.put(a, "a"); map.put(b, "b"); map.put(c, "c");
        c.fail = true;
        // Backward-shift removal visits b before c fails. No payload mutation
        // may escape, even if the inherited key index had begun shifting.
        expect(ArithmeticException.class, () -> map.removeAll(a));
        c.fail = false;
        eq(3, map.size());
        eq(List.of("a"), map.get(a));
        eq(List.of("b"), map.get(b));
        eq(List.of("c"), map.get(c));
        eq(List.of("a"), map.removeAll(a));
        map.put(new CollisionKey(4), "d");
        eq(List.of("b", "c", "d"), map.values());
        CollisionKey reentrant = new CollisionKey(5);
        reentrant.callback = () -> map.clear();
        expect(ConcurrentModificationException.class, () -> map.put(reentrant, "x"));
        eq(List.of("b", "c", "d"), map.values());
        reentrant.callback = () -> { eq(3, map.size()); eq(List.of("b"), map.get(b)); };
        map.put(reentrant, "read-only callback");
        reentrant.callback = null;
        eq(4, map.size());
        Object value = new Object() {
            @Override public boolean equals(Object other) { map.put("bad", "bad"); return false; }
            @Override public int hashCode() { return System.identityHashCode(this); }
        };
        expect(ConcurrentModificationException.class, () -> map.containsEntry(b, value));
        eq(false, map.containsKey("bad"));
        RuntimeException marker = new RuntimeException("iterator failed");
        Iterable<Object> broken = () -> new Iterator<>() {
            int cursor;
            @Override public boolean hasNext() { return true; }
            @Override public Object next() { if (cursor++ == 0) { return "prefix"; } throw marker; }
        };
        try { map.replaceValues(b, broken); throw new AssertionError("missing callback exception"); }
        catch (RuntimeException actual) { check(actual == marker, "original exception identity"); }
        eq(List.of("b"), map.get(b));
        expect(NullPointerException.class, () -> map.replaceValues(b, null));
        map.clear();
        eq(true, map.isEmpty());
        map.put(a, null);
        eq(Arrays.asList((Object) null), map.get(a));
    }

    private static void storageAndGeneration() throws Exception {
        LinkedListMultimap<Integer, Integer> map = new LinkedListMultimap<>();
        int capacity = 0;
        for (int cycle = 0; cycle < 1000; cycle++) {
            for (int n = 0; n < 16; n++) { map.put(n % 3, n); }
            if (cycle == 0) { capacity = ((Object[]) field(map, "keyLane")).length; }
            for (int n = 0; n < 3; n++) { map.removeAll(n); }
            eq(capacity, ((Object[]) field(map, "keyLane")).length);
            for (Object key : (Object[]) field(map, "keyLane")) { eq(null, key); }
            for (Object value : (Object[]) field(map, "valueLane")) { eq(null, value); }
        }
        for (Field field : LinkedListMultimap.class.getDeclaredFields()) {
            check(!Collection.class.isAssignableFrom(field.getType()), "no retained collection per key");
            check(!Map.Entry.class.isAssignableFrom(field.getType()), "no retained entry projection");
            if (field.getType().isArray() && !field.getType().getComponentType().isPrimitive()) {
                check(field.getName().equals("keyLane") || field.getName().equals("valueLane"), "only canonical object payload arrays");
            }
        }
        Field generation = LinkedListMultimap.class.getDeclaredField("nextGeneration");
        generation.setAccessible(true);
        generation.setLong(map, Long.MAX_VALUE);
        expect(IllegalStateException.class, () -> map.put(1, 1));
        eq(true, map.isEmpty());
    }

    private static void randomOracle() {
        for (int seed = 0; seed < 8; seed++) {
            Random random = new Random(0x51A7C0DEL + seed);
            LinkedListMultimap<Integer, Integer> map = new LinkedListMultimap<>();
            ArrayList<SimpleEntry<Integer, Integer>> pairs = new ArrayList<>();
            for (int step = 0; step < OPERATIONS / 8; step++) {
                Integer key = random.nextInt(9) == 0 ? null : random.nextInt(19);
                Integer value = random.nextInt(11) == 0 ? null : random.nextInt(101) - 50;
                List<Integer> slots = slots(pairs, key);
                switch (random.nextInt(12)) {
                    case 0, 1 -> { eq(true, map.put(key, value)); pairs.add(new SimpleEntry<>(key, value)); }
                    case 2 -> {
                        int index = random.nextInt(slots.size() + 1);
                        map.get(key).add(index, value);
                        pairs.add(index == slots.size() ? pairs.size() : slots.get(index), new SimpleEntry<>(key, value));
                    }
                    case 3 -> {
                        if (!slots.isEmpty()) {
                            int index = random.nextInt(slots.size());
                            eq(pairs.get(slots.get(index)).setValue(value), map.get(key).set(index, value));
                        }
                    }
                    case 4 -> {
                        if (!slots.isEmpty()) {
                            int index = random.nextInt(slots.size());
                            eq(pairs.remove((int) slots.get(index)).getValue(), map.get(key).remove(index));
                        }
                    }
                    case 5 -> {
                        eq(run(pairs, key), map.removeAll(key));
                        pairs.removeIf(pair -> Objects.equals(key, pair.getKey()));
                    }
                    case 6 -> {
                        ArrayList<Integer> replacement = new ArrayList<>();
                        for (int n = random.nextInt(6); n > 0; n--) { replacement.add(random.nextBoolean() ? null : n); }
                        eq(run(pairs, key), map.replaceValues(key, replacement));
                        int common = Math.min(slots.size(), replacement.size());
                        for (int n = 0; n < common; n++) { pairs.get(slots.get(n)).setValue(replacement.get(n)); }
                        for (int n = slots.size() - 1; n >= common; n--) { pairs.remove((int) slots.get(n)); }
                        for (int n = common; n < replacement.size(); n++) { pairs.add(new SimpleEntry<>(key, replacement.get(n))); }
                    }
                    case 7 -> {
                        if (!pairs.isEmpty()) {
                            int index = random.nextInt(pairs.size());
                            eq(pairs.get(index).setValue(value), map.values().set(index, value));
                        }
                    }
                    case 8 -> {
                        if (!pairs.isEmpty()) {
                            int index = random.nextInt(pairs.size());
                            ListIterator<Map.Entry<Integer, Integer>> cursor = map.entries().listIterator(index);
                            eq(pairs.remove(index), cursor.next()); cursor.remove();
                        }
                    }
                    case 9 -> {
                        List<Integer> expected = slots.isEmpty() ? null : run(pairs, key);
                        eq(expected, map.asMap().remove(key));
                        pairs.removeIf(pair -> Objects.equals(key, pair.getKey()));
                    }
                    case 10 -> {
                        eq(!slots.isEmpty(), map.keySet().remove(key));
                        pairs.removeIf(pair -> Objects.equals(key, pair.getKey()));
                    }
                    case 11 -> {
                        if (random.nextInt(20) == 0 || pairs.size() > 200) { map.clear(); pairs.clear(); }
                    }
                }
                eq(pairs.size(), map.size());
                eq(pairs.isEmpty(), map.isEmpty());
                eq(pairs, map.entries());
                ArrayList<Integer> allValues = new ArrayList<>();
                LinkedHashMap<Integer, List<Integer>> grouped = new LinkedHashMap<>();
                for (SimpleEntry<Integer, Integer> pair : pairs) {
                    allValues.add(pair.getValue());
                    grouped.computeIfAbsent(pair.getKey(), ignored -> new ArrayList<>()).add(pair.getValue());
                }
                eq(allValues, map.values());
                eq(new ArrayList<>(grouped.keySet()), new ArrayList<>(map.keySet()));
                eq(grouped, map.asMap());
                eq(grouped.containsKey(key), map.containsKey(key));
                eq(run(pairs, key).contains(value), map.containsEntry(key, value));
                eq(run(pairs, key), map.get(key));
            }
        }
    }

    private static List<Integer> slots(List<SimpleEntry<Integer, Integer>> pairs, Integer key) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int index = 0; index < pairs.size(); index++) {
            if (Objects.equals(key, pairs.get(index).getKey())) { slots.add(index); }
        }
        return slots;
    }
    private static List<Integer> run(List<SimpleEntry<Integer, Integer>> pairs, Integer key) {
        ArrayList<Integer> values = new ArrayList<>();
        for (SimpleEntry<Integer, Integer> pair : pairs) { if (Objects.equals(key, pair.getKey())) { values.add(pair.getValue()); } }
        return values;
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) { throw new AssertionError("expected " + expected + " actual " + actual); }
    }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) { return; }
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
