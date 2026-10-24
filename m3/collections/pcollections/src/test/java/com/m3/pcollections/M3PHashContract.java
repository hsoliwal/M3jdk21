/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.pcollections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.pcollections.HashTreePBag;
import org.pcollections.HashTreePMap;
import org.pcollections.PBag;
import org.pcollections.PMap;

/** Independent JDK/donor oracles for immutable bitmap-map, set, and bag contracts. */
public final class M3PHashContract {
    private static long checks;

    private M3PHashContract() { }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3PHashContract PASS checks=" + checks);
    }

    public static void run() throws Exception {
        mapVersions();
        collisionGroups();
        representativesAndNulls();
        mapReadOnly();
        sets();
        bags();
        serialization();
        sharing();
    }

    private static void mapVersions() {
        M3PMap<Key, Integer> actual = M3PMap.empty();
        Map<Key, Integer> expected = new HashMap<>();
        PMap<Key, Integer> donor = HashTreePMap.empty();
        List<M3PMap<Key, Integer>> versions = new ArrayList<>();
        List<Map<Key, Integer>> expectedVersions = new ArrayList<>();
        for (int step = 0; step < 4_096; step++) {
            int id = ((step * 73) ^ (step >>> 3)) & 511;
            Key key = step % 31 == 0 ? null : new Key(id, hashFor(id));
            if ((step * 29) % 7 < 4) {
                Integer value = step % 11 == 0 ? null : step;
                actual = actual.plus(key, value);
                expected.put(key, value);
                donor = donor.plus(key, value);
            } else {
                actual = actual.minus(key);
                expected.remove(key);
                donor = donor.minus(key);
            }
            equal(actual.size(), expected.size(), "size after operation");
            equal(actual.get(key), expected.get(key), "lookup after operation");
            require(actual.containsKey(key) == expected.containsKey(key), "presence after operation");
            if (step % 47 == 0) {
                mapEquals(actual, expected);
                require(actual.equals(donor) && donor.equals(actual), "original PMap equality");
            }
            if (step % 101 == 0) {
                versions.add(actual);
                expectedVersions.add(new HashMap<>(expected));
            }
        }
        mapEquals(actual, expected);
        for (int i = 0; i < versions.size(); i++) {
            M3PMap<Key, Integer> old = versions.get(i);
            Map<Key, Integer> oldExpected = expectedVersions.get(i);
            mapEquals(old, oldExpected);
            Key branchKey = new Key(10_000 + i, i);
            M3PMap<Key, Integer> branch = old.plus(branchKey, i);
            require(!old.containsKey(branchKey), "branch does not change earlier version");
            require(branch.containsKey(branchKey), "branch contains new key");
            mapEquals(old, oldExpected);
        }
        M3PMap<Key, Integer> copied = M3PMap.from(actual);
        require(copied.equals(actual), "from map");
        require(actual.plusAll(Collections.emptyMap()) == actual, "empty plusAll fixed point");
        require(actual.minusAll(Collections.emptyList()) == actual, "empty minusAll fixed point");
        require(actual.minusAll(actual.keySet()).isEmpty(), "remove all snapshot keys");
        require(!actual.isEmpty(), "bulk removal preserves source");
        expect(NullPointerException.class, () -> actualNullArguments());
    }

    private static void actualNullArguments() {
        M3PMap.empty().plusAll(null);
    }

    private static void collisionGroups() {
        for (int hash : new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            M3PMap<Key, Integer> map = M3PMap.empty();
            Map<Key, Integer> oracle = new HashMap<>();
            for (int i = 0; i < 257; i++) {
                Key key = new Key(i, hash);
                map = map.plus(key, i);
                oracle.put(key, i);
            }
            M3PMap<Key, Integer> original = map;
            mapEquals(map, oracle);
            for (int i = 0; i < 257; i++) {
                Key key = new Key((i * 131) % 257, hash);
                map = map.minus(key);
                oracle.remove(key);
                mapEquals(map, oracle);
                require(original.containsKey(key), "collision removal preserves old bucket");
            }
            require(map.isEmpty(), "collision bucket fully drained");
            require(map.minus(new Key(999, hash)) == map, "absent collision fixed point");
            equal(original.size(), 257, "original collision size");
        }

        // These spread hashes share all six lower five-bit fragments. The final
        // directory level and collision children must still remain distinguishable.
        int[] spreadHashes = {0, 1 << 30, Integer.MIN_VALUE, 3 << 30, 31, -1};
        M3PMap<Key, Integer> deep = M3PMap.empty();
        Map<Key, Integer> expected = new HashMap<>();
        for (int i = 0; i < spreadHashes.length * 3; i++) {
            int spread = spreadHashes[i % spreadHashes.length];
            Key key = new Key(500 + i, spread ^ (spread >>> 16));
            deep = deep.plus(key, i);
            expected.put(key, i);
        }
        mapEquals(deep, expected);
        M3PMap<Key, Integer> old = deep;
        for (Key key : new ArrayList<>(expected.keySet())) {
            deep = deep.minus(key);
            expected.remove(key);
            mapEquals(deep, expected);
        }
        equal(old.size(), 18, "deep old version");
    }

    private static void representativesAndNulls() {
        Key first = new Key(1, 31);
        Key second = new Key(1, 31);
        String value1 = new String("value");
        String value2 = new String("value");
        M3PMap<Key, String> old = M3PMap.singleton(first, value1);
        M3PMap<Key, String> changed = old.plus(second, value2);
        require(changed != old, "equal but distinct references require new map");
        require(changed.get(first) == value2, "incoming equal value reference retained");
        require(changed.keySet().iterator().next() == second, "incoming equal key reference retained");
        require(old.get(first) == value1, "older value identity retained");
        require(old.keySet().iterator().next() == first, "older key identity retained");
        require(changed.plus(second, value2) == changed, "identical references fixed point");

        M3PMap<Object, Object> nulls = M3PMap.<Object, Object>empty()
                .plus(null, null).plus("present", null).plus("value", "v");
        require(nulls.containsKey(null), "null key present");
        require(nulls.containsKey("present"), "null value present");
        require(!nulls.containsKey("missing"), "absent differs from null value");
        require(nulls.getOrDefault("present", "fallback") == null, "default preserves null");
        equal(nulls.getOrDefault("missing", "fallback"), "fallback", "absent default");
        require(nulls.containsValue(null), "null value membership");
        require(nulls.entrySet().contains(new java.util.AbstractMap.SimpleImmutableEntry<>(null, null)),
                "null entry membership");
        require(!nulls.entrySet().contains(
                new java.util.AbstractMap.SimpleImmutableEntry<>("missing", null)), "absent null entry");
        Map<Object, Object> oracle = new HashMap<>();
        oracle.put(null, null);
        oracle.put("present", null);
        oracle.put("value", "v");
        mapEquals(nulls, oracle);
        Map<Object, Object> forEach = new HashMap<>();
        nulls.forEach(forEach::put);
        equal(forEach, oracle, "direct forEach");
        require(nulls.minus(null).size() == 2 && nulls.containsKey(null), "null removal persistent");
    }

    private static void mapReadOnly() {
        Map<Integer, Integer> map = M3PMap.singleton(1, 2);
        Map<Integer, Integer> empty = M3PMap.empty();
        for (Map<Integer, Integer> candidate : List.of(map, empty)) {
            expect(UnsupportedOperationException.class, candidate::clear);
            expect(UnsupportedOperationException.class, () -> candidate.put(1, 2));
            expect(UnsupportedOperationException.class, () -> candidate.remove(99));
            expect(UnsupportedOperationException.class, () -> candidate.putAll(Collections.emptyMap()));
            expect(UnsupportedOperationException.class, () -> candidate.putIfAbsent(1, 2));
            expect(UnsupportedOperationException.class, () -> candidate.remove(99, 99));
            expect(UnsupportedOperationException.class, () -> candidate.replace(99, 99));
            expect(UnsupportedOperationException.class, () -> candidate.replace(99, 99, 99));
            expect(UnsupportedOperationException.class,
                    () -> candidate.compute(1, (key, value) -> forbiddenCallback()));
            expect(UnsupportedOperationException.class,
                    () -> candidate.computeIfAbsent(1, key -> forbiddenCallback()));
            expect(UnsupportedOperationException.class,
                    () -> candidate.computeIfPresent(99, (key, value) -> forbiddenCallback()));
            expect(UnsupportedOperationException.class,
                    () -> candidate.merge(1, 2, (left, right) -> forbiddenCallback()));
            expect(UnsupportedOperationException.class,
                    () -> candidate.replaceAll((key, value) -> forbiddenCallback()));
            immutable(candidate.keySet(), 99);
            immutable(candidate.values(), 99);
            immutable(candidate.entrySet(),
                    new java.util.AbstractMap.SimpleImmutableEntry<>(99, 99));
        }
        Map.Entry<Integer, Integer> projected = map.entrySet().iterator().next();
        expect(UnsupportedOperationException.class, () -> projected.setValue(7));
        require(projected != map.entrySet().iterator().next(), "entries are fresh caller projections");
        Iterator<Integer> keys = map.keySet().iterator();
        require(keys.hasNext() && keys.hasNext(), "iterator lookahead stable");
        equal(keys.next(), 1, "iterator entry");
        require(!keys.hasNext(), "iterator exhausted");
        expect(NoSuchElementException.class, keys::next);
        expect(NoSuchElementException.class, keys::next);
        expect(NullPointerException.class, () -> map.forEach(null));
        expect(NullPointerException.class, () -> M3PMap.empty().minusAll(null));
        expect(NullPointerException.class, () -> M3PMap.from(null));
    }

    private static Integer forbiddenCallback() {
        throw new AssertionError("immutable mutator invoked a callback");
    }

    private static <T> void immutable(Collection<T> collection, T value) {
        expect(UnsupportedOperationException.class, collection::clear);
        expect(UnsupportedOperationException.class, () -> collection.add(value));
        expect(UnsupportedOperationException.class,
                () -> collection.addAll(Collections.emptyList()));
        expect(UnsupportedOperationException.class, () -> collection.remove(value));
        expect(UnsupportedOperationException.class,
                () -> collection.removeAll(Collections.emptyList()));
        expect(UnsupportedOperationException.class, () -> collection.retainAll(collection));
        expect(UnsupportedOperationException.class, () -> collection.removeIf(ignored -> {
            throw new AssertionError("immutable view invoked a predicate");
        }));
        expect(UnsupportedOperationException.class, () -> collection.iterator().remove());
    }

    private static void sets() {
        M3PSet<Key> set = M3PSet.empty();
        Set<Key> expected = new HashSet<>();
        for (int i = 0; i < 1_024; i++) {
            int id = (i * 41) & 255;
            Key key = i % 23 == 0 ? null : new Key(id, hashFor(id));
            M3PSet<Key> before = set;
            Set<Key> previous = new HashSet<>(expected);
            if (i % 3 == 0) {
                set = set.minus(key);
                expected.remove(key);
            } else {
                set = set.plus(key);
                expected.add(key);
            }
            require(before.equals(previous), "set old version");
            require(set.equals(expected) && expected.equals(set), "set JDK equality");
            equal(set.hashCode(), expected.hashCode(), "set JDK hash");
        }
        Key first = new Key(1_001, 0);
        Key equal = new Key(1_001, 0);
        M3PSet<Key> representative = M3PSet.singleton(first);
        require(representative.plus(equal) == representative, "duplicate set plus fixed point");
        require(representative.iterator().next() == first, "set retains first representative");
        M3PSet<Key> selected = set.intersect(Collections.singleton(null));
        require(selected.size() <= 1, "intersection membership");
        require(selected.isEmpty() || selected.contains(null), "intersection null");
        require(set.minusAll(set).isEmpty(), "set self minusAll");
        require(set.plusAll(Collections.emptyList()) == set, "set empty plusAll");
        require(M3PSet.from(set).equals(set), "set from");
        Set<Key> readOnly = set;
        immutable(readOnly, new Key(9_999, 0));
        expect(NullPointerException.class, () -> M3PSet.from(null));
        expect(NullPointerException.class, () -> representative.plusAll(null));
        expect(NullPointerException.class, () -> representative.minusAll(null));
        expect(NullPointerException.class, () -> representative.intersect(null));
    }

    private static void bags() throws Exception {
        M3PBag<Integer> bag = M3PBag.empty();
        PBag<Integer> donor = HashTreePBag.empty();
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < 1_024; i++) {
            int value = (i * 17) & 63;
            M3PBag<Integer> old = bag;
            int oldCount = bag.count(value);
            if (i % 5 == 0) {
                bag = bag.minus(value);
                donor = donor.minus(value);
                counts.computeIfPresent(value, (key, count) -> count == 1 ? null : count - 1);
            } else {
                bag = bag.plus(value);
                donor = donor.plus(value);
                counts.merge(value, 1, Integer::sum);
            }
            equal(old.count(value), oldCount, "bag old count");
            equal(bag.count(value), counts.getOrDefault(value, 0), "bag frequency");
            require(bag.equals(donor) && donor.equals(bag), "bag donor equality");
            equal(bag.hashCode(), donor.hashCode(), "bag donor hash");
        }
        M3PBag<Integer> old = bag;
        bag = bag.minusAll(Arrays.asList(3, 3, 5, 7));
        donor = donor.minusAll(Arrays.asList(3, 3, 5, 7));
        require(bag.equals(donor) && donor.equals(bag), "minusAll removes all requested occurrences");
        equal(bag.count(3), 0, "complete frequency removal");
        require(old.count(3) > 0, "old bag frequency retained");
        require(!bag.equals(new ArrayList<>(bag)), "bag not equal to list");
        require(!bag.equals(new HashSet<>(bag)), "bag not equal to set");
        require(M3PBag.empty().equals(HashTreePBag.empty()), "empty donor bag equality");

        M3PBag<String> nulls = M3PBag.<String>empty().plus(null).plus("x").plus(null);
        equal(nulls.size(), 3, "null bag total count");
        equal(nulls.count(null), 2, "null bag multiplicity");
        equal(nulls.hashCode(), "x".hashCode(), "null hash contribution is zero");
        equal(nulls.minus(null).count(null), 1, "minus subtracts one null");
        equal(nulls.minusAll(Collections.singleton(null)).count(null), 0, "minusAll removes all nulls");
        equal(new ArrayList<>(nulls).size(), 3, "iterator emits occurrences");
        require(nulls.equals(M3PBag.from(Arrays.asList(null, null, "x"))), "bag order independence");
        require(nulls.minusAll(nulls).isEmpty(), "bag self minusAll");
        Collection<String> readOnly = nulls;
        immutable(readOnly, "missing");
        expect(NullPointerException.class, () -> nulls.plusAll(null));
        expect(NullPointerException.class, () -> nulls.minusAll(null));
        expect(NullPointerException.class, () -> M3PBag.from(null));

        // One packed count exercises the int boundary without allocating billions
        // of occurrences. This exact valid state cannot be built cheaply by plus.
        M3PBag<Integer> maximal = maximalBag();
        equal(maximal.size(), Integer.MAX_VALUE, "maximal valid size");
        expect(ArithmeticException.class, () -> maximal.plus(1));
        equal(maximal.count(1), Integer.MAX_VALUE, "overflow leaves old count unchanged");
        equal(maximal.minus(1).count(1), Integer.MAX_VALUE - 1, "large count minus");
        require(maximal.minusAll(Collections.singleton(1)).isEmpty(), "large count minusAll");
        M3PBag<Integer> restored = roundTrip(maximal);
        equal(restored.count(1), Integer.MAX_VALUE, "serialization uses distinct counts");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static M3PBag<Integer> maximalBag() throws Exception {
        Constructor<M3PBag> constructor = M3PBag.class.getDeclaredConstructor(M3PMap.class, int.class);
        constructor.setAccessible(true);
        return (M3PBag<Integer>) constructor.newInstance(
                M3PMap.singleton(1, Integer.MAX_VALUE), Integer.MAX_VALUE);
    }

    private static void serialization() throws Exception {
        M3PMap<Key, String> map = M3PMap.empty();
        for (int i = 0; i < 150; i++) map = map.plus(new Key(i, i % 3), "v" + i);
        map = map.plus(null, null);
        M3PMap<Key, String> restored = roundTrip(map);
        require(restored.equals(map) && map.equals(restored), "map serialization equality");
        equal(restored.hashCode(), map.hashCode(), "map serialization hash");
        M3PMap<Key, String> branch = restored.plus(new Key(999, 0), "new");
        require(!restored.containsKey(new Key(999, 0)), "deserialized map is persistent");
        equal(branch.size(), restored.size() + 1, "deserialized map update");
        immutable(restored.entrySet(), new java.util.AbstractMap.SimpleImmutableEntry<>(null, null));
        M3PSet<Key> set = M3PSet.from(map.keySet());
        M3PSet<Key> setCopy = roundTrip(set);
        require(setCopy.equals(set), "set serialization");
        M3PBag<String> bag = M3PBag.from(Arrays.asList("a", "b", "a", null, null));
        M3PBag<String> bagCopy = roundTrip(bag);
        require(bagCopy.equals(bag), "bag serialization");
        equal(bagCopy.hashCode(), bag.hashCode(), "bag serialization hash");
        require(roundTrip(M3PMap.empty()).isEmpty(), "empty map serialization");
        require(roundTrip(M3PSet.empty()).isEmpty(), "empty set serialization");
        require(roundTrip(M3PBag.empty()).isEmpty(), "empty bag serialization");
    }

    private static void sharing() throws Exception {
        M3PMap<Integer, Integer> original = M3PMap.empty();
        for (int i = 0; i < 4_096; i++) original = original.plus(i, i);
        M3PMap<Integer, Integer> updated = original.plus(2_001, -1);
        Field root = M3PMap.class.getDeclaredField("root");
        root.setAccessible(true);
        Object oldRoot = root.get(original);
        Object newRoot = root.get(updated);
        require(oldRoot != newRoot, "changed path has a new root");
        Set<Object> oldNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> newNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        nodes(oldRoot, oldNodes);
        nodes(newRoot, newNodes);
        int shared = 0;
        for (Object node : newNodes) if (oldNodes.contains(node)) shared++;
        require(shared > 0, "unchanged trie branches share node identity");
        require(newNodes.size() - shared <= 8, "a scalar update copies only a bounded hash path");
        require(oldNodes.size() < original.size() / 2, "directories are not one node per mapping");
        equal(original.get(2_001), 2_001, "sharing does not mutate old value");
        equal(updated.get(2_001), -1, "updated value");
    }

    private static void nodes(Object node, Set<Object> result) throws Exception {
        if (node == null || !result.add(node)) return;
        Field slots = node.getClass().getDeclaredField("slots");
        slots.setAccessible(true);
        for (Object slot : (Object[]) slots.get(node)) {
            if (slot != null && slot.getClass() == node.getClass()) nodes(slot, result);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static <K, V> void mapEquals(M3PMap<K, V> actual, Map<K, V> expected) {
        require(actual.equals(expected) && expected.equals(actual), "JDK map equality");
        equal(actual.hashCode(), expected.hashCode(), "JDK map hash");
        equal(actual.size(), expected.size(), "map size");
        equal(new HashSet<>(actual.entrySet()), expected.entrySet(), "entry set");
        equal(new HashSet<>(actual.keySet()), expected.keySet(), "key set");
        equal(actual.values().size(), expected.values().size(), "values size");
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            require(actual.containsKey(entry.getKey()), "expected key present");
            equal(actual.get(entry.getKey()), entry.getValue(), "expected mapping");
        }
    }

    private static int hashFor(int id) {
        return switch (id & 7) {
            case 0 -> 0;
            case 1 -> Integer.MIN_VALUE;
            case 2 -> Integer.MAX_VALUE;
            case 3 -> -1;
            default -> Integer.rotateLeft(id * 0x9e3779b9, id & 31);
        };
    }

    private static void equal(Object actual, Object expected, String description) {
        require(Objects.equals(actual, expected), description + ": expected=" + expected + ", actual=" + actual);
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void expect(Class<? extends Throwable> type, Throwing operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }

    private static final class Key implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final int id;
        private final int hash;

        Key(int id, int hash) { this.id = id; this.hash = hash; }

        @Override public int hashCode() { return hash; }

        @Override public boolean equals(Object other) {
            return other instanceof Key key && id == key.id;
        }

        @Override public String toString() { return "Key(" + id + "," + hash + ")"; }
    }
}
