// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.Spliterator;
import java.util.TreeMap;

/** Executable Java 21 oracle for explicit equals-map collision policies. */
public final class MapOperatorViewsSelfTest {
    private static int checks;

    private MapOperatorViewsSelfTest() { }

    public static void main(String[] args) {
        emptyAndNullable();
        collisionsAndKeyIdentity();
        liveReadOnlyAndIteration();
        specialSourceEquivalence();
        wordAndGrowthBoundaries();
        defaultsAndFailures();
        randomizedDifferentials();
        System.out.println("PASS M3MapOperatorViewsSelfTest checks=" + checks);
    }

    private static void emptyAndNullable() {
        Map<String, String> empty = new LinkedHashMap<>();
        Map<String, String> left = new LinkedHashMap<>();
        left.put(null, null);
        left.put("a", "left");
        Map<String, String> right = new LinkedHashMap<>();
        right.put("b", null);
        assertOverlay(left, right, MapOperatorViews.CollisionPolicy.REJECT);
        assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_LEFT);
        assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        assertOverlay(empty, right, MapOperatorViews.CollisionPolicy.REJECT);
        assertOverlay(empty, empty, MapOperatorViews.CollisionPolicy.REJECT);
        Map<String, String> result = MapOperatorViews.overlayByEquals(left, right,
                MapOperatorViews.CollisionPolicy.KEEP_LEFT);
        check(result.containsKey(null), "null key present even with null value");
        eq(null, result.get(null), "null key value");
        eq("fallback", result.getOrDefault("not-present", "fallback"), "getOrDefault absent");
        eq(null, result.getOrDefault(null, "fallback"), "getOrDefault present null");
        check(result.containsValue(null), "null value retained");
        check(result.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(null, null)),
                "null entry lookup");
        check(!result.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(null, "other")),
                "null entry wrong value");
    }

    private static void collisionsAndKeyIdentity() {
        String leftKey = new String("key");
        String rightKey = new String("key");
        check(leftKey != rightKey, "distinct but equals keys");
        Map<String, Integer> left = new LinkedHashMap<>();
        Map<String, Integer> right = new LinkedHashMap<>();
        left.put(leftKey, 4);
        right.put(rightKey, 9);
        right.put("fresh", 17);

        Map<String, Integer> keepLeft = MapOperatorViews.asEqualsOperator(left)
                .plus(right, MapOperatorViews.CollisionPolicy.KEEP_LEFT);
        Map<String, Integer> keepRight = MapOperatorViews.asEqualsOperator(left)
                .plus(right, MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        eq(4, keepLeft.get("key"), "left-value collision policy");
        eq(9, keepRight.get("key"), "right-value collision policy");
        check(keepRight.entrySet().iterator().next().getKey() == leftKey,
                "left key identity retained on right-value override");
        eq(List.of("key", "fresh"), List.copyOf(keepRight.keySet()),
                "left-then-right-new key order");
        eq(2, keepRight.size(), "cross-equals key collapsed only by explicit policy");

        Map<String, Integer> reject = MapOperatorViews.overlayByEquals(left, right,
                MapOperatorViews.CollisionPolicy.REJECT);
        // Projection is lazy. An equal key collision refuses on read.
        expect(IllegalArgumentException.class, reject::size, "collision reject on touch");
        right.remove(rightKey);
        eq(2, reject.size(), "no collision after right mutation");
        right.put(rightKey, 9);
        expect(IllegalArgumentException.class, () -> reject.get("key"),
                "collision reintroduced by mutation");
    }

    private static void liveReadOnlyAndIteration() {
        Map<String, Integer> left = new LinkedHashMap<>();
        Map<String, Integer> right = new LinkedHashMap<>();
        left.put("a", 1);
        right.put("b", 2);
        Map<String, Integer> view = MapOperatorViews.overlayByEquals(left, right,
                MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        Iterator<Map.Entry<String, Integer>> old = view.entrySet().iterator();
        Spliterator<Map.Entry<String, Integer>> late = view.entrySet().spliterator();
        left.put("c", 3);
        right.put("d", null);
        eq(List.of("a", "b"), keys(old), "old iterator uses captured snapshot");
        eq(List.of("a", "c", "b", "d"), List.copyOf(view.keySet()),
                "new reads see current source maps");
        List<Map.Entry<String, Integer>> lateValues = new ArrayList<>();
        late.forEachRemaining(lateValues::add);
        eq(4, lateValues.size(), "first-touch spliterator sees changes");
        eq(List.copyOf(view.entrySet()), lateValues, "spliterator source order");
        check((late.characteristics() & Spliterator.DISTINCT) != 0, "entry DISTINCT");
        check((late.characteristics() & Spliterator.ORDERED) != 0, "entry ORDERED");
        check((late.characteristics() & Spliterator.SIZED) == 0, "entry not stale SIZED");
        check((late.characteristics() & Spliterator.NONNULL) != 0, "non-null entry objects");

        expect(UnsupportedOperationException.class, () -> view.put("e", 5), "put");
        expect(UnsupportedOperationException.class, () -> view.remove("absent"), "remove absent");
        expect(UnsupportedOperationException.class, view::clear, "clear");
        expect(UnsupportedOperationException.class, () -> view.putAll(Map.of()), "putAll empty");
        expect(UnsupportedOperationException.class, () -> view.putIfAbsent("a", 6), "putIfAbsent");
        expect(UnsupportedOperationException.class, () -> view.remove("absent", 99),
                "remove(key,value)");
        expect(UnsupportedOperationException.class, () -> view.replace("a", 12), "replace");
        expect(UnsupportedOperationException.class, () -> view.replace("a", 1, 12),
                "replace CAS");
        expect(UnsupportedOperationException.class, () -> view.replaceAll((k, v) -> 7),
                "replaceAll");
        expect(UnsupportedOperationException.class, () -> view.computeIfAbsent("a", k -> 7),
                "computeIfAbsent");
        expect(UnsupportedOperationException.class, () -> view.computeIfPresent("a", (k, v) -> 8),
                "computeIfPresent");
        expect(UnsupportedOperationException.class, () -> view.compute("a", (k, v) -> 8),
                "compute");
        expect(UnsupportedOperationException.class, () -> view.merge("a", 7, Integer::sum),
                "Map.merge");
        expect(UnsupportedOperationException.class, () -> view.entrySet().add(
                new AbstractMap.SimpleImmutableEntry<>("z", 99)), "entrySet add");
        expect(UnsupportedOperationException.class, () -> view.entrySet().remove(
                new AbstractMap.SimpleImmutableEntry<>("z", 99)), "entrySet remove absent");
        expect(UnsupportedOperationException.class, () -> view.entrySet().addAll(Set.of()),
                "entrySet addAll empty");
        expect(UnsupportedOperationException.class, () -> view.entrySet().removeAll(Set.of()),
                "entrySet removeAll empty");
        expect(UnsupportedOperationException.class, () -> view.entrySet().retainAll(Set.of()),
                "entrySet retainAll");
        expect(UnsupportedOperationException.class, () -> view.entrySet().removeIf(e -> false),
                "entrySet removeIf");
        expect(UnsupportedOperationException.class, view.entrySet()::clear,
                "entrySet clear");
        Map.Entry<String, Integer> entry = view.entrySet().iterator().next();
        expect(UnsupportedOperationException.class, () -> entry.setValue(100), "entry value");
        Iterator<Map.Entry<String, Integer>> iterator = view.entrySet().iterator();
        iterator.next();
        expect(UnsupportedOperationException.class, iterator::remove, "entry iterator remove");
        expect(UnsupportedOperationException.class, () -> view.keySet().remove("a"),
                "keySet backed mutation");
        expect(UnsupportedOperationException.class, () -> view.values().remove(1),
                "values backed mutation");
        eq(Map.of("a", 1, "c", 3), left, "left source not mutated");
    }

    private static void specialSourceEquivalence() {
        Map<String, Integer> identity = new IdentityHashMap<>();
        identity.put(new String("same"), 1);
        identity.put(new String("same"), 2);
        eq(2, identity.size(), "identity source permits two equals keys");
        Map<String, Integer> result = MapOperatorViews.overlayByEquals(identity, Map.of(),
                MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        expect(IllegalArgumentException.class, result::size,
                "intra-source equals duplicates refuse for all policies");

        TreeMap<String, Integer> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.put("a", 1);
        Map<String, Integer> normalized = MapOperatorViews.overlayByEquals(sorted,
                Map.of("A", 2), MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        eq(Map.of("a", 1, "A", 2), normalized,
                "Java equals projection does not inherit comparator");
        check(!(normalized instanceof java.util.SortedMap<?, ?>), "no incorrect sorted marker");
    }

    private static void wordAndGrowthBoundaries() {
        for (int size : new int[] {0, 1, 7, 8, 9, 63, 64, 65, 66, 127, 128, 129, 1025}) {
            Map<Integer, String> left = new LinkedHashMap<>();
            Map<Integer, String> right = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                if ((i & 1) == 0) left.put(i, "L" + i);
                if (i % 3 == 0) right.put(i, "R" + i);
            }
            assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_LEFT);
            assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
        }
    }

    private static void defaultsAndFailures() {
        for (java.lang.reflect.Method method : MapOperatorViews.MapOperator.class
                .getDeclaredMethods()) {
            check(!method.getName().equals("plus") || method.getParameterCount() == 2,
                    "there must be no policy-free map plus");
        }
        MapOperatorViews.MapOperator<String, Integer> custom = new MapOperatorViews.MapOperator<>() {
            @Override public Map<String, Integer> source() { return Map.of("key", 1); }
            @Override public Map<String, Integer> plus(Map<? extends String, ? extends Integer> right,
                                                      MapOperatorViews.CollisionPolicy policy) {
                return Map.of("overridden", 2);
            }
        };
        eq(Map.of("overridden", 2), custom.plus(Map.of(),
                MapOperatorViews.CollisionPolicy.REJECT), "overridable default");
        expect(NullPointerException.class, () -> MapOperatorViews.asEqualsOperator(null),
                "null adapter");
        expect(NullPointerException.class, () -> MapOperatorViews.overlayByEquals(null,
                Map.of(), MapOperatorViews.CollisionPolicy.KEEP_LEFT), "null left");
        expect(NullPointerException.class, () -> MapOperatorViews.overlayByEquals(Map.of(),
                null, MapOperatorViews.CollisionPolicy.KEEP_LEFT), "null right");
        expect(NullPointerException.class, () -> MapOperatorViews.overlayByEquals(Map.of(),
                Map.of(), null), "null collision policy");
        Iterator<?> exhausted = MapOperatorViews.overlayByEquals(Map.of(), Map.of(),
                MapOperatorViews.CollisionPolicy.REJECT).entrySet().iterator();
        expect(NoSuchElementException.class, exhausted::next, "exhausted entry iterator");
    }

    private static void randomizedDifferentials() {
        Random random = new Random(20261118L);
        for (int trial = 0; trial < 350; trial++) {
            Map<Integer, Integer> left = new LinkedHashMap<>();
            Map<Integer, Integer> right = new LinkedHashMap<>();
            for (int j = 0, n = random.nextInt(85); j < n; j++) {
                left.put(random.nextInt(120) - 60, random.nextInt(100) - 50);
            }
            for (int j = 0, n = random.nextInt(85); j < n; j++) {
                right.put(random.nextInt(120) - 60, random.nextInt(100) - 50);
            }
            if ((trial & 1) == 0) left.put(null, random.nextInt(10));
            if (trial % 3 == 0) right.put(null, null);
            assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_LEFT);
            assertOverlay(left, right, MapOperatorViews.CollisionPolicy.KEEP_RIGHT);
            if (Collections.disjoint(left.keySet(), right.keySet())) {
                assertOverlay(left, right, MapOperatorViews.CollisionPolicy.REJECT);
            } else {
                Map<Integer, Integer> reject = MapOperatorViews.overlayByEquals(left, right,
                        MapOperatorViews.CollisionPolicy.REJECT);
                expect(IllegalArgumentException.class, reject::size, "random key collision");
            }
        }
    }

    private static <K, V> void assertOverlay(Map<K, V> left, Map<K, V> right,
                                             MapOperatorViews.CollisionPolicy policy) {
        Map<K, V> expected = new LinkedHashMap<>(left);
        for (Map.Entry<K, V> entry : right.entrySet()) {
            if (policy == MapOperatorViews.CollisionPolicy.KEEP_RIGHT
                    || !expected.containsKey(entry.getKey())) {
                expected.put(entry.getKey(), entry.getValue());
            }
        }
        Map<K, V> actual = MapOperatorViews.asEqualsOperator(left).plus(right, policy);
        eq(expected, actual, policy + " equality");
        eq(actual, expected, policy + " reverse equality");
        eq(expected.hashCode(), actual.hashCode(), policy + " map hash");
        eq(expected.size(), actual.size(), policy + " size");
        eq(expected.isEmpty(), actual.isEmpty(), policy + " empty");
        eq(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()),
                policy + " key order");
        eq(new ArrayList<>(expected.values()), new ArrayList<>(actual.values()),
                policy + " value order");
        eq(new LinkedHashMap<>(actual), expected, policy + " entry projection");
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            check(actual.containsKey(entry.getKey()), policy + " containsKey");
            eq(entry.getValue(), actual.get(entry.getKey()), policy + " get");
            check(actual.containsValue(entry.getValue()), policy + " containsValue");
            check(actual.entrySet().contains(entry), policy + " entrySet.contains");
        }
        eq((long) expected.size(), actual.entrySet().stream().count(), policy + " stream");
    }

    private static <K, V> List<K> keys(Iterator<Map.Entry<K, V>> iterator) {
        List<K> keys = new ArrayList<>();
        iterator.forEachRemaining(entry -> keys.add(entry.getKey()));
        return keys;
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation,
                               String message) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName() + ": " + message);
        } catch (Throwable actual) {
            check(type.isInstance(actual), message + " wrong exception: " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual),
                message + " expected=" + expected + " actual=" + actual);
    }
}
