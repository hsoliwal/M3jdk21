/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Immutable object CSR multimap ordering, snapshots, null validation, views and independent grouped oracle.
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED FrozenMultimapConvergenceTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.FrozenMultimap;

public class FrozenMultimapConvergenceTest {
    private static long checks;
    private static final int CASES = 1000;

    public static void main(String[] args) throws Exception {
        snapshotsAndViews();
        validationAndCallbacks();
        immutableEmptyViews();
        prefixBoundariesAndStorage();
        randomOracle();
        System.out.println("FROZEN_MULTIMAP_PASS checks=" + checks + " random_cases=" + CASES);
    }

    private static void snapshotsAndViews() {
        String key = new String("a");
        String firstValue = new String("one");
        ArrayList<String> values = new ArrayList<>(Arrays.asList(firstValue, "two", "one"));
        LinkedHashMap<String, Collection<String>> source = new LinkedHashMap<>();
        source.put("empty", List.of());
        source.put(key, values);
        source.put("b", List.of("three"));
        FrozenMultimap<String, String> map = FrozenMultimap.copyOf(source);
        eq(4, map.size());
        eq(2, map.keyCount());
        eq(List.of("a", "b"), new ArrayList<>(map.keySet()));
        eq(List.of("one", "two", "one"), map.get("a"));
        check(map.keySet().iterator().next() == key, "preserve input key identity");
        check(map.get("a").iterator().next() == firstValue, "shallow object payload identity");
        eq(false, map.containsKey("empty"));
        eq(true, map.containsEntry("a", "one"));
        eq(false, map.containsEntry("a", null));
        eq(false, map.containsKey(null));
        eq(List.of(), map.get(null));
        eq(null, map.asMap().get("missing"));
        eq(List.of(new SimpleImmutableEntry<>("a", "one"), new SimpleImmutableEntry<>("a", "two"),
                new SimpleImmutableEntry<>("a", "one"), new SimpleImmutableEntry<>("b", "three")),
                new ArrayList<>(map.entries()));
        Map.Entry<String, String> entry = map.entries().iterator().next();
        Map.Entry<String, String> secondProjection = map.entries().iterator().next();
        check(entry != secondProjection, "caller-owned immutable projections");
        expect(UnsupportedOperationException.class, () -> entry.setValue("changed"));
        expect(UnsupportedOperationException.class, () -> map.get("a").clear());
        expect(UnsupportedOperationException.class, () -> map.get("a").remove("missing"));
        expect(UnsupportedOperationException.class, () -> map.get("a").add("extra"));
        expect(UnsupportedOperationException.class, () -> map.get("a").removeIf(x -> false));
        expect(UnsupportedOperationException.class, () -> map.keySet().remove("a"));
        expect(UnsupportedOperationException.class, () -> map.entries().clear());
        expect(UnsupportedOperationException.class, () -> map.asMap().remove("a"));
        expect(UnsupportedOperationException.class, () -> map.asMap().put("x", List.of("v")));
        expect(UnsupportedOperationException.class, () -> map.asMap().entrySet().iterator().next().setValue(List.of("v")));
        expect(UnsupportedOperationException.class, () -> map.asMap().entrySet().iterator().next().getValue().clear());
        expect(UnsupportedOperationException.class, () -> map.asMap().values().iterator().next().clear());
        Iterator<String> iterator = map.get("a").iterator();
        iterator.next();
        expect(UnsupportedOperationException.class, iterator::remove);
        values.set(0, "mutated");
        values.clear();
        source.clear();
        eq(List.of("one", "two", "one"), map.get("a"));
        eq(4, map.size());
        eq("one", entry.getValue());
        expect(UnsupportedOperationException.class, () -> map.put("new", "new"));
        expect(UnsupportedOperationException.class, () -> map.removeAll("missing"));
    }

    private static void validationAndCallbacks() {
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(null));
        Map<String, Collection<String>> source = new HashMap<>();
        source.put(null, List.of());
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(source));
        source.clear(); source.put("key", null);
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(source));
        source.clear(); source.put("key", Arrays.asList("ok", null));
        expect(NullPointerException.class, () -> FrozenMultimap.copyOf(source));
        IdentityHashMap<String, Collection<Integer>> duplicateKeys = new IdentityHashMap<>();
        duplicateKeys.put(new String("equal"), List.of(1));
        duplicateKeys.put(new String("equal"), List.of(2));
        expect(IllegalArgumentException.class, () -> FrozenMultimap.copyOf(duplicateKeys));
        RuntimeException marker = new RuntimeException("key callback failure");
        Object key = new Object() { @Override public int hashCode() { throw marker; } };
        Map<Object, Collection<Integer>> unsafe = new IdentityHashMap<>();
        unsafe.put(key, List.of(1));
        try { FrozenMultimap.copyOf(unsafe); throw new AssertionError("missing callback failure"); }
        catch (RuntimeException actual) { check(actual == marker, "preserve callback exception identity"); }
        eq(1, unsafe.size());
        eq(List.of(1), unsafe.get(key));
        LinkedHashMap<Integer, Collection<Integer>> empty = new LinkedHashMap<>();
        empty.put(1, List.of()); empty.put(2, List.of());
        FrozenMultimap<Integer, Integer> result = FrozenMultimap.copyOf(empty);
        eq(0, result.size()); eq(0, result.keyCount()); eq(List.of(), new ArrayList<>(result.entries()));
    }

    private static void immutableEmptyViews() {
        FrozenMultimap<String, String> empty = new FrozenMultimap<>();
        eq(0, empty.size()); eq(0, empty.keyCount());
        eq(Set.of(), empty.keySet()); eq(Map.of(), empty.asMap());
        eq(false, empty.containsKey(null)); eq(false, empty.containsEntry(null, null));
        eq(List.of(), empty.get("missing"));
        expect(UnsupportedOperationException.class, () -> empty.get("missing").clear());
        expect(UnsupportedOperationException.class, () -> empty.keySet().clear());
        expect(UnsupportedOperationException.class, () -> empty.entries().clear());
        expect(UnsupportedOperationException.class, () -> empty.asMap().clear());
        expect(UnsupportedOperationException.class, () -> empty.asMap().entrySet().clear());
        expect(UnsupportedOperationException.class, () -> empty.put(null, null));
        expect(UnsupportedOperationException.class, () -> empty.removeAll(null));
        expect(NoSuchElementException.class, () -> empty.entries().iterator().next());
        expect(NoSuchElementException.class, () -> empty.keySet().iterator().next());
    }

    private static void prefixBoundariesAndStorage() throws Exception {
        for (int count : new int[] {0, 1, 7, 8, 31, 32, 63, 64, 65, 127, 128, 129}) {
            LinkedHashMap<Integer, Collection<Integer>> source = new LinkedHashMap<>();
            for (int n = 0; n < count; n++) { source.put(n, Arrays.asList(n, n, -n)); }
            FrozenMultimap<Integer, Integer> map = FrozenMultimap.copyOf(source);
            int[] offsets = (int[]) field(map, "valueOffsetLane");
            long[] frozen = (long[]) field(map, "stateMaskWords");
            eq(count + 1, offsets.length);
            for (int n = 0; n <= count; n++) { eq(3 * n, offsets[n]); }
            eq((count + 63) / 64, frozen.length);
            for (int n = 0; n < count; n++) { eq(1L, frozen[n >>> 6] >>> (n & 63) & 1L); }
            if (count != 0 && (count & 63) != 0) { eq(0L, frozen[frozen.length - 1] >>> (count & 63)); }
            eq(3 * count, ((Object[]) field(map, "valueLane")).length);
            for (Field f : FrozenMultimap.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) { check(Modifier.isFinal(f.getModifiers()), "frozen owner fields final"); }
                check(!Collection.class.isAssignableFrom(f.getType()), "no retained collection per run");
                check(!Map.Entry.class.isAssignableFrom(f.getType()), "no retained entry objects");
            }
        }
    }

    private record CollisionKey(int number) { @Override public int hashCode() { return 1; } }

    private static void randomOracle() {
        Random random = new Random(0xC5A0B1ECL);
        for (int trial = 0; trial < CASES; trial++) {
            LinkedHashMap<CollisionKey, Collection<Integer>> source = new LinkedHashMap<>();
            LinkedHashMap<CollisionKey, List<Integer>> expected = new LinkedHashMap<>();
            ArrayList<Map.Entry<CollisionKey, Integer>> pairs = new ArrayList<>();
            int groups = random.nextInt(70);
            for (int n = 0; n < groups; n++) {
                CollisionKey key = new CollisionKey(n * 3 - 17);
                ArrayList<Integer> values = new ArrayList<>();
                for (int j = random.nextInt(15); j > 0; j--) { values.add(random.nextInt(13) - 6); }
                source.put(key, values);
                if (!values.isEmpty()) {
                    expected.put(key, new ArrayList<>(values));
                    for (Integer value : values) { pairs.add(new SimpleImmutableEntry<>(key, value)); }
                }
            }
            FrozenMultimap<CollisionKey, Integer> map = FrozenMultimap.copyOf(source);
            eq(pairs.size(), map.size());
            eq(expected.size(), map.keyCount());
            eq(new ArrayList<>(expected.keySet()), new ArrayList<>(map.keySet()));
            eq(expected, map.asMap());
            eq(pairs, new ArrayList<>(map.entries()));
            for (Map.Entry<CollisionKey, Collection<Integer>> entry : source.entrySet()) {
                eq(!entry.getValue().isEmpty(), map.containsKey(new CollisionKey(entry.getKey().number())));
                eq(new ArrayList<>(entry.getValue()), map.get(entry.getKey()));
                for (int value = -7; value <= 7; value++) {
                    eq(entry.getValue().contains(value), map.containsEntry(entry.getKey(), value));
                }
                entry.getValue().clear();
            }
            source.clear();
            eq(expected, map.asMap());
            eq(pairs, new ArrayList<>(map.entries()));
        }
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
