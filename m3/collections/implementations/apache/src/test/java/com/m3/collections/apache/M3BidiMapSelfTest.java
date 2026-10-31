/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.apache;

import com.m3.collections.M3LinkedHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

/**
 * Differential qualification of one M3-owned BidiMap API body against real Commons 4.5.0.
 * No provider implementation is permitted inside the candidate; the donor is an oracle only.
 * These tests do not qualify other library families, serialization, concurrency or performance.
 */
public final class M3BidiMapSelfTest {
    private static long checks;
    private static long cases;
    private static long traceOperations;
    private static int groups;

    private M3BidiMapSelfTest() { }

    @FunctionalInterface
    private interface Operation<K, V> { Object run(BidiMap<K, V> map); }
    @FunctionalInterface
    private interface Action { Object run(); }

    private record PairValue(Object key, Object value) { }
    private record Sequence(String kind, List<Object> items) { }
    private record MapValue(boolean bidirectional, List<PairValue> entries) { }
    private record Outcome(Object value, String exception) { }

    /** Distinct instances compare by id, and every instance has the same hash. */
    private static final class Collision {
        private final int id;
        Collision(int id) { this.id = id; }
        @Override public int hashCode() { return 17; }
        @Override public boolean equals(Object other) {
            return other instanceof Collision collision && collision.id == id;
        }
        @Override public String toString() { return "collision:" + id; }
    }

    private static final class ProbeFailure extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;
        ProbeFailure(String phase) { super(phase); }
    }

    private static final class Models<K, V> {
        final BidiMap<K, V> actual;
        final BidiMap<K, V> oracle;
        final BidiMap<V, K> actualInverse;
        final BidiMap<V, K> oracleInverse;
        Models() { this(new M3BidiMap<>(), new DualLinkedHashBidiMap<>()); }
        Models(BidiMap<K, V> actual, BidiMap<K, V> oracle) {
            this.actual = actual; this.oracle = oracle;
            actualInverse = actual.inverseBidiMap(); oracleInverse = oracle.inverseBidiMap();
        }
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static void equal(Object expected, Object actual, String context) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(context + " expected=" + expected + " actual=" + actual);
        }
    }

    private static Object snapshot(Object value) {
        if (value instanceof Map.Entry<?, ?> entry) {
            return new PairValue(entry.getKey(), entry.getValue());
        }
        if (value instanceof Map<?, ?> map) {
            return new MapValue(value instanceof BidiMap<?, ?>, entries(map));
        }
        if (value instanceof Collection<?> collection) {
            List<Object> items = new ArrayList<>();
            for (Object item : collection) items.add(snapshot(item));
            String kind = value instanceof Set<?> ? "SET" : value instanceof List<?> ? "LIST" : "COLLECTION";
            return new Sequence(kind, items);
        }
        return value;
    }

    private static Outcome observe(Action action) {
        try { return new Outcome(snapshot(action.run()), ""); }
        catch (RuntimeException failure) { return new Outcome(null, failure.getClass().getName()); }
    }

    private static List<PairValue> entries(Map<?, ?> map) {
        List<PairValue> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.add(new PairValue(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static <K, V> void state(Models<K, V> models, String context) {
        BidiMap<K, V> actual = models.actual;
        BidiMap<K, V> oracle = models.oracle;
        equal(oracle.size(), actual.size(), context + " size");
        equal(oracle.isEmpty(), actual.isEmpty(), context + " empty");
        equal(entries(oracle), entries(actual), context + " forward entry order");
        equal(new ArrayList<>(oracle.keySet()), new ArrayList<>(actual.keySet()), context + " forward key order");
        equal(new ArrayList<>(oracle.values()), new ArrayList<>(actual.values()), context + " forward value order");
        equal(entries(models.oracleInverse), entries(models.actualInverse), context + " inverse entry order");
        equal(new ArrayList<>(models.oracleInverse.keySet()), new ArrayList<>(models.actualInverse.keySet()), context + " inverse key order");
        equal(new ArrayList<>(models.oracleInverse.values()), new ArrayList<>(models.actualInverse.values()), context + " inverse value order");
        equal(oracle.hashCode(), actual.hashCode(), context + " map hash");
        check(actual.equals(oracle) && oracle.equals(actual), context + " map equality symmetry");
        check(actual.inverseBidiMap() == models.actualInverse, context + " cached inverse");
        check(models.actualInverse.inverseBidiMap() == actual, context + " inverse identity");
        check(models.actualInverse.getClass() == M3BidiMap.class, context + " owned inverse body");
        int visited = 0;
        for (K key : actual.keySet()) {
            V value = actual.get(key);
            check(actual.containsKey(key), context + " live key presence");
            check(actual.containsValue(value), context + " live value presence");
            check(models.actualInverse.containsKey(value), context + " inverse key presence including null");
            equal(key, actual.getKey(value), context + " reverse lookup");
            equal(key, models.actualInverse.get(value), context + " inverse lookup");
            visited++;
        }
        equal(actual.size(), visited, context + " visited forward cardinality");
        for (V value : models.actualInverse.keySet()) {
            K key = models.actualInverse.get(value);
            check(actual.containsKey(key), context + " reverse has forward key");
            equal(value, actual.get(key), context + " reverse has forward value");
        }
    }

    private static <K, V> Outcome compare(Models<K, V> models, String context, Operation<K, V> operation) {
        Outcome actual = observe(() -> operation.run(models.actual));
        Outcome oracle = observe(() -> operation.run(models.oracle));
        equal(oracle, actual, context + " result/error class");
        state(models, context);
        cases++;
        return actual;
    }

    private static <K, V> void put(Models<K, V> models, K key, V value, String context) {
        compare(models, context, map -> map.put(key, value));
    }

    /** Declaration/type boundary only; not a whole-program allocation proof. */
    private static void ownedStorageBoundary() {
        groups++;
        Class<?> body = M3BidiMap.class;
        for (Class<?> type = body; type != null; type = type.getSuperclass()) {
            check(!type.getName().startsWith("org.apache.commons.collections4."), "provider implementation superclass " + type);
        }
        int ownedMapFields = 0;
        for (Field field : body.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            check(!(type.getName().startsWith("org.apache.commons.collections4.") && !type.isInterface()),
                    "provider implementation field " + field);
            if (type == M3LinkedHashMap.class) ownedMapFields++;
            else if (Map.class.isAssignableFrom(type)) {
                check(type == M3BidiMap.class || type == BidiMap.class,
                        "unqualified extra map storage field " + field);
            }
            check(!Map.Entry.class.isAssignableFrom(type), "retained entry field on collection body " + field);
        }
        equal(2, ownedMapFields, "two existing M3 map owners; no replacement hash table");
    }

    private static void nullsDuplicatesAndInverse() {
        groups++;
        Models<String, Integer> models = new Models<>();
        put(models, null, null, "null/null");
        compare(models, "present null distinguished from absent", map -> Arrays.asList(
                map.containsKey(null), map.containsValue(null), map.get(null), map.getKey(null),
                map.getOrDefault(null, 99), map.getOrDefault("absent", 99)));
        put(models, "a", 1, "a/1");
        put(models, "b", 2, "b/2");
        put(models, "a", 2, "existing key and existing other value evict b");
        compare(models, "evicted key absent", map -> !map.containsKey("b"));
        put(models, "c", 2, "new key steals value");
        put(models, "c", 2, "same mapping repeated");
        put(models, "d", null, "null value steals null-key mapping");
        put(models, null, 3, "reintroduce null key");
        compare(models, "inverse put returns previous key", map -> map.inverseBidiMap().put(3, "e"));
        compare(models, "remove null value", map -> map.removeValue(null));
        compare(models, "remove absent null key", map -> map.remove(null));
        compare(models, "inverse clear", map -> { map.inverseBidiMap().clear(); return null; });
        compare(models, "cleared cached inverse reused", map -> {
            BidiMap<Integer, String> inverse = map.inverseBidiMap();
            inverse.put(null, null); return inverse.inverseBidiMap() == map;
        });
    }

    private static void constructorsAndInputCustody() {
        groups++;
        Map<String, Integer> source = new M3LinkedHashMap<>();
        source.put("a", 1); source.put("b", 2); source.put("c", 1); source.put(null, 3); source.put("d", null);
        Models<String, Integer> models = new Models<>(new M3BidiMap<>(source), new DualLinkedHashBidiMap<>(source));
        state(models, "copy constructor duplicate-value/null order");
        source.clear(); source.put("source-only", 99);
        compare(models, "copy constructor directory independent of caller", map -> !map.containsKey("source-only"));
        Models<String, Integer> copied = new Models<>(new M3BidiMap<>(models.actual), new DualLinkedHashBidiMap<>(models.oracle));
        state(copied, "copy constructor from existing BidiMap");
        models.actual.clear(); models.oracle.clear();
        state(copied, "copy remains after original BidiMap clear");
        equal(observe(() -> new DualLinkedHashBidiMap<String, Integer>(null)),
                observe(() -> new M3BidiMap<String, Integer>(null)), "null source constructor outcome");
    }

    private static void confirmedVersionQuirksAndIdentity() {
        groups++;
        Models<String, Integer> entries = new Models<>();
        put(entries, "a", 1, "entry return setup");
        equal(new Outcome(2, ""), compare(entries, "4.5.0 entry setter returns new value",
                map -> map.entrySet().iterator().next().setValue(2)), "confirmed entry return");
        Models<String, Integer> cursor = new Models<>();
        put(cursor, "a", 1, "cursor return setup");
        equal(new Outcome(1, ""), compare(cursor, "4.5.0 cursor setter returns prior value", map -> {
            MapIterator<String, Integer> iterator = map.mapIterator(); iterator.next(); return iterator.setValue(2);
        }), "confirmed cursor return");

        Collision original = new Collision(7);
        Collision equalKey = new Collision(7);
        Collision value = new Collision(1007);
        Models<Collision, Collision> identity = new Models<>();
        put(identity, original, value, "original reference key");
        compare(identity, "initial canonical payload identity", map -> Arrays.asList(
                map.keySet().iterator().next() == original, map.get(original) == value, map.getKey(value) == original));
        put(identity, equalKey, value, "equal distinct key replacement");
        compare(identity, "retained forward key and replacement reverse key identity", map -> Arrays.asList(
                map.keySet().iterator().next() == original, map.getKey(value) == equalKey));
        equal(IllegalArgumentException.class.getName(), compare(identity, "entry identity duplicate quirk",
                map -> map.entrySet().iterator().next().setValue(value)).exception(), "entry duplicate exception");
        equal(IllegalArgumentException.class.getName(), compare(identity, "cursor identity duplicate quirk", map -> {
            MapIterator<Collision, Collision> iterator = map.mapIterator(); iterator.next(); return iterator.setValue(value);
        }).exception(), "cursor duplicate exception");
        compare(identity, "cached inverse reference identities", map -> {
            BidiMap<Collision, Collision> inverse = map.inverseBidiMap();
            return Arrays.asList(inverse == map.inverseBidiMap(), inverse.inverseBidiMap() == map);
        });
    }

    private static Models<String, Integer> viewModel(boolean full) {
        Models<String, Integer> result = new Models<>();
        if (full) {
            put(result, "a", 1, "view fixture a");
            put(result, null, 2, "view fixture null key");
            put(result, "c", null, "view fixture null value");
        }
        return result;
    }

    private static Collection<?> view(BidiMap<String, Integer> map, int kind) {
        return switch (kind) { case 0 -> map.keySet(); case 1 -> map.values(); default -> map.entrySet(); };
    }

    private static Object addOne(BidiMap<String, Integer> map, int kind) {
        return switch (kind) {
            case 0 -> map.keySet().add("forbidden");
            case 1 -> map.values().add(99);
            default -> map.entrySet().add(new AbstractMap.SimpleImmutableEntry<>("forbidden", 99));
        };
    }

    private static Object addMany(BidiMap<String, Integer> map, int kind) {
        return switch (kind) {
            case 0 -> map.keySet().addAll(List.of("forbidden"));
            case 1 -> map.values().addAll(List.of(99));
            default -> map.entrySet().addAll(List.of(new AbstractMap.SimpleImmutableEntry<>("forbidden", 99)));
        };
    }

    private static void viewRefusalsAndNullBulk() {
        groups++;
        for (boolean full : List.of(false, true)) for (int kind = 0; kind < 3; kind++) {
            final int selected = kind;
            String label = "view=" + kind + " full=" + full;
            compare(viewModel(full), label + " add refused", map -> addOne(map, selected));
            compare(viewModel(full), label + " addAll nonempty refused", map -> addMany(map, selected));
            compare(viewModel(full), label + " add null refused", map -> view(map, selected).add(null));
            compare(viewModel(full), label + " addAll empty", map -> view(map, selected).addAll(Collections.emptyList()));
            compare(viewModel(full), label + " addAll null", map -> view(map, selected).addAll(null));
            compare(viewModel(full), label + " remove null", map -> view(map, selected).remove(null));
            compare(viewModel(full), label + " removeAll null", map -> view(map, selected).removeAll(null));
            compare(viewModel(full), label + " retainAll null", map -> view(map, selected).retainAll(null));
            compare(viewModel(full), label + " removeIf null", map -> view(map, selected).removeIf(null));
            compare(viewModel(full), label + " removeAll empty", map -> view(map, selected).removeAll(Collections.emptyList()));
            compare(viewModel(full), label + " retainAll empty", map -> view(map, selected).retainAll(Collections.emptyList()));
            compare(viewModel(full), label + " self removeAll", map -> {
                Collection<?> collection = view(map, selected); return collection.removeAll(collection);
            });
            compare(viewModel(full), label + " self retainAll", map -> {
                Collection<?> collection = view(map, selected); return collection.retainAll(collection);
            });
        }
    }

    private static void liveViewsBulkAndReturnedEntries() {
        groups++;
        compare(viewModel(true), "key removeAll including null", map -> map.keySet().removeAll(Arrays.asList(null, "a", "absent")));
        compare(viewModel(true), "value retainAll including null", map -> map.values().retainAll(Arrays.asList(null, 2, 77)));
        compare(viewModel(true), "entry conditional removal", map -> Arrays.asList(
                map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>("a", 99)),
                map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(null, 2))));
        compare(viewModel(true), "entry bulk remove with null/mismatch", map -> map.entrySet().removeAll(Arrays.asList(
                null, new AbstractMap.SimpleImmutableEntry<>("a", 99), new AbstractMap.SimpleImmutableEntry<>("c", null))));
        compare(viewModel(true), "key removeIf", map -> map.keySet().removeIf(key -> key == null || key.equals("a")));
        compare(viewModel(true), "value removeIf", map -> map.values().removeIf(value -> value == null || value == 1));
        compare(viewModel(true), "entry removeIf", map -> map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null));
        compare(viewModel(true), "inverse views remove forward mappings", map -> map.inverseBidiMap().values().remove("a"));
        compare(viewModel(true), "borrowed live views see both direction mutation", map -> {
            Set<String> keys = map.keySet(); Set<Integer> values = map.values(); Set<Map.Entry<String, Integer>> pairs = map.entrySet();
            map.inverseBidiMap().put(7, "later"); map.remove(null);
            return Arrays.asList(new ArrayList<>(keys), new ArrayList<>(values), new ArrayList<>(pairs));
        });
        compare(viewModel(true), "retained entry sees nonstructural value replacement", map -> {
            Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
            map.put(entry.getKey(), 77); return Arrays.asList(entry.getKey(), entry.getValue(), entry.hashCode());
        });
        compare(viewModel(true), "entry equality/hash and owned API tuple", map -> {
            Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
            Map.Entry<String, Integer> peer = new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue());
            return Arrays.asList(entry.equals(peer), peer.equals(entry), entry.hashCode() == peer.hashCode());
        });
        compare(viewModel(true), "entry duplicate setter refusal", map -> map.entrySet().iterator().next().setValue(2));
        compare(viewModel(true), "entry iterator remove after setValue", map -> {
            Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
            Map.Entry<String, Integer> entry = iterator.next(); Object result = entry.setValue(88);
            iterator.remove(); return Arrays.asList(result, map.containsValue(88), map.containsKey("a"));
        });
        for (int kind = 0; kind < 3; kind++) {
            final int selected = kind;
            compare(viewModel(true), "view iterator removal lifecycle " + kind, map -> {
                Iterator<?> iterator = view(map, selected).iterator(); List<Outcome> events = new ArrayList<>();
                events.add(observe(() -> { iterator.remove(); return null; }));
                events.add(observe(iterator::next));
                events.add(observe(() -> { iterator.remove(); return null; }));
                events.add(observe(() -> { iterator.remove(); return null; }));
                while (iterator.hasNext()) { iterator.next(); iterator.remove(); }
                events.add(observe(iterator::next)); return events;
            });
        }
    }

    private static void valuesIdentityBoundary() {
        groups++;
        compare(viewModel(true), "4.5.0 values identity equality, not Set equality", map -> {
            Set<Integer> values = map.values(); Set<Integer> contentSet = new LinkedHashSet<>(values);
            List<Integer> contentList = new ArrayList<>(values); int before = values.hashCode();
            List<Boolean> result = new ArrayList<>(Arrays.asList(values == map.values(), values.equals(values),
                    values.equals(contentSet), contentSet.equals(values), values.equals(contentList),
                    values.equals(map.inverseBidiMap().keySet()), map.inverseBidiMap().keySet().equals(values)));
            map.put("later", 77); result.add(before == values.hashCode());
            map.clear(); result.add(before == values.hashCode()); return result;
        });
    }

    private static void mapCursorLifecycleAndReset() {
        groups++;
        for (boolean full : List.of(false, true)) {
            compare(viewModel(full), "MapIterator lifecycle full=" + full, map -> {
                MapIterator<String, Integer> cursor = map.mapIterator();
                check(cursor instanceof ResettableIterator<?>, "candidate/oracle resettable cursor");
                List<Outcome> events = new ArrayList<>();
                events.add(observe(cursor::getKey)); events.add(observe(cursor::getValue));
                events.add(observe(() -> cursor.setValue(41)));
                events.add(observe(() -> { cursor.remove(); return null; }));
                if (cursor.hasNext()) {
                    events.add(observe(cursor::next)); events.add(observe(cursor::getKey)); events.add(observe(cursor::getValue));
                    events.add(observe(() -> cursor.setValue(41))); events.add(observe(cursor::getValue));
                    events.add(observe(() -> { cursor.remove(); return null; }));
                    events.add(observe(() -> { cursor.remove(); return null; }));
                    events.add(observe(cursor::getKey)); events.add(observe(cursor::getValue));
                    events.add(observe(() -> cursor.setValue(42)));
                }
                while (cursor.hasNext()) events.add(observe(cursor::next));
                events.add(observe(cursor::next));
                events.add(observe(() -> { ((ResettableIterator<?>) cursor).reset(); return null; }));
                events.add(observe(cursor::getKey)); events.add(observe(cursor::getValue));
                events.add(observe(() -> { cursor.remove(); return null; }));
                while (cursor.hasNext()) events.add(observe(cursor::next));
                return events;
            });
        }
        compare(viewModel(true), "cursor duplicate setter refused", map -> {
            MapIterator<String, Integer> cursor = map.mapIterator(); cursor.next(); return cursor.setValue(2);
        });
        compare(viewModel(true), "cursor forEachRemaining keys and current value", map -> {
            MapIterator<String, Integer> cursor = map.mapIterator(); List<Object> events = new ArrayList<>();
            cursor.forEachRemaining(key -> events.add(Arrays.asList(key, cursor.getKey(), cursor.getValue())));
            return events;
        });
        compare(viewModel(true), "cursor forEachRemaining null consumer", map -> {
            map.mapIterator().forEachRemaining(null); return null;
        });
    }

    private static Models<String, Integer> defaultModel() {
        Models<String, Integer> models = new Models<>();
        put(models, "a", 1, "default fixture a");
        put(models, "b", 2, "default fixture b");
        put(models, "null-value", null, "default fixture null");
        return models;
    }

    private static void mapDefaultsAndCallbackOutcomes() {
        groups++;
        compare(defaultModel(), "putIfAbsent null-valued mapping", map -> map.putIfAbsent("null-value", 7));
        compare(defaultModel(), "putIfAbsent steals occupied value", map -> map.putIfAbsent("new", 2));
        compare(defaultModel(), "conditional remove mismatch and null", map -> Arrays.asList(
                map.remove("a", 99), map.remove("null-value", null), map.remove("missing", null)));
        compare(defaultModel(), "conditional replace steals value", map -> map.replace("a", 1, 2));
        compare(defaultModel(), "replace absent unchanged", map -> map.replace("missing", 7));
        compare(defaultModel(), "computeIfAbsent callback invocation", map -> {
            List<String> calls = new ArrayList<>();
            Object old = map.computeIfAbsent("a", key -> { calls.add(key); return 9; });
            Object absent = map.computeIfAbsent("new", key -> { calls.add(key); return null; });
            Object presentNull = map.computeIfAbsent("null-value", key -> { calls.add(key); return 9; });
            return Arrays.asList(old, absent, presentNull, calls);
        });
        compare(defaultModel(), "computeIfPresent skips absent/null", map -> {
            List<String> calls = new ArrayList<>();
            Object absent = map.computeIfPresent("missing", (key, value) -> { calls.add(key); return 9; });
            Object presentNull = map.computeIfPresent("null-value", (key, value) -> { calls.add(key); return 9; });
            Object removed = map.computeIfPresent("a", (key, value) -> { calls.add(key); return null; });
            return Arrays.asList(absent, presentNull, removed, calls);
        });
        compare(defaultModel(), "compute changes both directions", map -> map.compute("a", (key, old) -> 2));
        compare(defaultModel(), "compute null removes present-null mapping", map -> map.compute("null-value", (key, old) -> null));
        compare(defaultModel(), "merge skips callback for null mapping", map -> {
            List<String> calls = new ArrayList<>();
            Object result = map.merge("null-value", 8, (left, right) -> { calls.add("called"); return 9; });
            return Arrays.asList(result, calls);
        });
        compare(defaultModel(), "merge null result removes", map -> map.merge("a", 8, (left, right) -> null));
        compare(defaultModel(), "merge duplicate value evicts other key", map -> map.merge("a", 8, (left, right) -> 2));
        compare(defaultModel(), "merge null input rejected", map -> map.merge("a", null, Integer::sum));
        compare(defaultModel(), "compute callback failure leaves current pair", map -> map.compute("a", (key, old) -> { throw new ProbeFailure("compute"); }));
        compare(defaultModel(), "merge callback failure leaves current pair", map -> map.merge("a", 8, (left, right) -> { throw new ProbeFailure("merge"); }));
        compare(defaultModel(), "replaceAll callback prefix commit on failure", map -> {
            map.replaceAll((key, value) -> { if (Objects.equals(key, "b")) throw new ProbeFailure("replaceAll"); return 11; });
            return null;
        });
        compare(defaultModel(), "replaceAll duplicate rejection prefix", map -> { map.replaceAll((key, value) -> 11); return null; });
        compare(defaultModel(), "forEach callback order and failure", map -> {
            List<Object> calls = new ArrayList<>();
            Outcome result = observe(() -> { map.forEach((key, value) -> {
                calls.add(Arrays.asList(key, value)); if (Objects.equals(key, "b")) throw new ProbeFailure("forEach");
            }); return null; });
            return Arrays.asList(calls, result);
        });
        compare(defaultModel(), "null Map callbacks refused", map -> Arrays.asList(
                observe(() -> map.compute("a", null)), observe(() -> map.computeIfAbsent("a", null)),
                observe(() -> map.computeIfPresent("a", null)), observe(() -> map.merge("a", 3, null)),
                observe(() -> { map.replaceAll(null); return null; }), observe(() -> { map.forEach(null); return null; })));
        compare(defaultModel(), "removeIf callback prefix commit", map -> {
            return map.keySet().removeIf(key -> { if (Objects.equals(key, "b")) throw new ProbeFailure("removeIf"); return true; });
        });
        compare(defaultModel(), "putAll null rejected", map -> { map.putAll(null); return null; });
        compare(defaultModel(), "putAll self", map -> { map.putAll(map); return map; });
        Map<String, Integer> input = new M3LinkedHashMap<>(); input.put("a", 7); input.put("new", 2); input.put(null, 9);
        compare(defaultModel(), "putAll conflict order", map -> { map.putAll(input); return map; });
    }

    private static void collidingResizeAndOrderedRemoval() {
        groups++;
        Models<Collision, Collision> models = new Models<>();
        for (int index = 0; index < 192; index++) {
            put(models, new Collision(index), new Collision(10000 + index), "collision growth " + index);
        }
        for (int index = 0; index < 192; index += 3) {
            Collision key = new Collision(index);
            compare(models, "collision delete " + index, map -> map.remove(key));
        }
        for (int index = 0; index < 64; index++) {
            put(models, new Collision(300 + index), new Collision(10000 + index), "collision reuse/duplicate value " + index);
        }
        compare(models, "inverse collision iteration delete alternating", map -> {
            Iterator<Collision> iterator = map.inverseBidiMap().keySet().iterator(); int index = 0;
            while (iterator.hasNext()) { iterator.next(); if ((index++ & 1) == 0) iterator.remove(); }
            return map.size();
        });
        compare(models, "all collision key and value reference associations", map -> {
            List<Boolean> result = new ArrayList<>(); Iterator<Collision> keys = map.keySet().iterator();
            Iterator<Collision> values = map.values().iterator();
            while (keys.hasNext()) { Collision key = keys.next(); result.add(map.get(key) == values.next()); }
            result.add(!values.hasNext()); return result;
        });
    }

    private static Collision chosen(Random random, int base) {
        return random.nextInt(9) == 0 ? null : new Collision(base + random.nextInt(48));
    }

    private static Object firstEntrySet(BidiMap<Collision, Collision> map, Collision value) {
        Iterator<Map.Entry<Collision, Collision>> iterator = map.entrySet().iterator();
        return iterator.hasNext() ? iterator.next().setValue(value) : "EMPTY";
    }

    private static Object firstCursorSet(BidiMap<Collision, Collision> map, Collision value) {
        MapIterator<Collision, Collision> iterator = map.mapIterator();
        if (!iterator.hasNext()) return "EMPTY";
        iterator.next(); return iterator.setValue(value);
    }

    private static void randomTraces(long seed) {
        groups++;
        Models<Collision, Collision> models = new Models<>(); Random random = new Random(seed);
        for (int step = 0; step < 4000; step++) {
            boolean inverse = random.nextBoolean();
            Collision key = chosen(random, inverse ? 1000 : 0);
            Collision value = chosen(random, inverse ? 0 : 1000);
            Collision otherKey = chosen(random, inverse ? 1000 : 0);
            Collision otherValue = chosen(random, inverse ? 0 : 1000);
            int operation = random.nextInt(29); int residue = random.nextInt(4);
            int ordinal = random.nextInt(4); boolean clear = random.nextInt(80) == 0;
            Map<Collision, Collision> input = new M3LinkedHashMap<>(); input.put(key, value); input.put(otherKey, otherValue);
            compare(models, "trace seed=" + seed + " step=" + step + " inverse=" + inverse + " op=" + operation, supplied -> {
                BidiMap<Collision, Collision> map = inverse ? supplied.inverseBidiMap() : supplied;
                return switch (operation) {
                    case 0, 1, 2 -> map.put(key, value);
                    case 3 -> map.remove(key);
                    case 4 -> map.removeValue(value);
                    case 5 -> map.keySet().remove(key);
                    case 6 -> map.values().remove(value);
                    case 7 -> map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(key, value));
                    case 8 -> map.putIfAbsent(key, value);
                    case 9 -> map.replace(key, value);
                    case 10 -> map.replace(key, otherValue, value);
                    case 11 -> map.remove(key, value);
                    case 12 -> map.compute(key, (current, old) -> value);
                    case 13 -> map.computeIfAbsent(key, current -> value);
                    case 14 -> map.computeIfPresent(key, (current, old) -> value);
                    case 15 -> map.merge(key, value, (old, incoming) -> otherValue);
                    case 16 -> {
                        MapIterator<Collision, Collision> iterator = map.mapIterator();
                        for (int index = 0; index <= ordinal && iterator.hasNext(); index++) iterator.next();
                        Outcome result = observe(() -> { iterator.remove(); return null; }); yield result;
                    }
                    case 17 -> firstEntrySet(map, value);
                    case 18 -> firstCursorSet(map, value);
                    case 19 -> map.keySet().removeAll(Arrays.asList(key, otherKey));
                    case 20 -> map.values().retainAll(Arrays.asList(value, otherValue));
                    case 21 -> map.values().removeIf(item -> item == null || (item.id & 3) == residue);
                    case 22 -> map.keySet().removeIf(item -> item == null || (item.id & 3) == residue);
                    case 23 -> map.entrySet().removeIf(entry -> entry.getKey() == null || (entry.getKey().id & 3) == residue);
                    case 24 -> { map.putAll(input); yield null; }
                    case 25 -> { map.replaceAll((current, old) -> otherValue); yield null; }
                    case 26 -> { if (clear) map.clear(); yield map.size(); }
                    case 27 -> {
                        List<PairValue> ordered = new ArrayList<>(); map.forEach((current, item) -> ordered.add(new PairValue(current, item))); yield ordered;
                    }
                    default -> Arrays.asList(map.get(key), map.getKey(value), map.containsKey(key), map.containsValue(value));
                };
            });
            traceOperations++;
        }
    }

    public static void main(String[] args) {
        ownedStorageBoundary();
        constructorsAndInputCustody();
        nullsDuplicatesAndInverse();
        confirmedVersionQuirksAndIdentity();
        viewRefusalsAndNullBulk();
        liveViewsBulkAndReturnedEntries();
        valuesIdentityBoundary();
        mapCursorLifecycleAndReset();
        mapDefaultsAndCallbackOutcomes();
        collidingResizeAndOrderedRemoval();
        randomTraces(0x4D33424944494D41L);
        randomTraces(20261017L);
        System.out.println("PASS M3_BIDIMAP_OWNED_SELF_TEST groups=" + groups + " cases=" + cases
                + " checks=" + checks + " random_operations=" + traceOperations
                + " oracle=Apache-Commons-Collections-4.5.0-DualLinkedHashBidiMap"
                + " scope=one-owned-BidiMap-API-body");
    }
}
