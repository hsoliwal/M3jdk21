/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Product-owned JDK/JUnit oracles and explicit selected null-query contracts; no provider JAR. */
final class FrozenObjectCoreContractTest {
    @Test
    void listSnapshotsMatchJdkAndEncounterOrderAndKeepElementIdentity() {
        Random random = new Random(0x46524f5a454e4c31L);
        for (int seed = 0; seed < 96; seed++) {
            ArrayList<Key> source = new ArrayList<>();
            for (int i = 0; i < seed; i++) source.add(new Key(random.nextInt(17)));
            List<Key> jdk = List.copyOf(source);
            List<Key> ordered = Collections.unmodifiableList(new ArrayList<>(source));
            FrozenList<Key> owner = FrozenList.copyOf(source);
            source.clear();
            assertEquals(jdk.size(), owner.size());
            assertEquals(jdk.isEmpty(), owner.isEmpty());
            assertEquals(jdk, owner.asList());
            assertEquals(ordered, owner.asList());
            assertEquals(jdk.hashCode(), owner.asList().hashCode());
            assertArrayEquals(jdk.toArray(), owner.toArray());
            assertArrayEquals(jdk.toArray(new Key[0]), owner.asList().toArray(new Key[0]));
            for (int i = 0; i < jdk.size(); i++) assertSame(jdk.get(i), owner.get(i));
            for (int q = -1; q <= 18; q++) {
                Key probe = new Key(q);
                assertEquals(jdk.indexOf(probe), owner.indexOf(probe));
                assertEquals(ordered.contains(probe), owner.contains(probe));
            }
            int split = owner.size() / 2;
            assertEquals(jdk.subList(split, jdk.size()), owner.asList().subList(split, owner.size()));
            assertEquals(jdk, owner.asList().stream().toList());
            assertEquals(jdk, owner.asList().parallelStream().toList());
            Object[] exported = owner.toArray();
            if (exported.length != 0) {
                exported[0] = new Key(-91);
                assertSame(jdk.get(0), owner.get(0));
            }
        }
    }

    @Test
    void setSnapshotsCollapseDuplicatesKeepFirstIdentityAndEncounterOrder() {
        Random random = new Random(0x46524f5a454e5331L);
        for (int seed = 0; seed < 96; seed++) {
            ArrayList<Key> source = new ArrayList<>();
            for (int i = 0; i < seed * 2; i++) source.add(new Key(random.nextInt(31)));
            Set<Key> jdk = Set.copyOf(source);
            Set<Key> ordered = Collections.unmodifiableSet(new LinkedHashSet<>(source));
            List<Key> firstEncounter = new ArrayList<>(ordered);
            FrozenSet<Key> owner = FrozenSet.copyOf(source);
            source.clear();
            assertEquals(jdk.size(), owner.size());
            assertEquals(jdk.isEmpty(), owner.isEmpty());
            assertEquals(jdk, owner.asSet());
            assertEquals(ordered, owner.asSet());
            assertEquals(jdk.hashCode(), owner.asSet().hashCode());
            assertArrayEquals(ordered.toArray(), owner.toArray());
            assertEquals(firstEncounter, owner.asSet().stream().toList());
            assertEquals(firstEncounter, owner.asSet().parallelStream().toList());
            for (int i = 0; i < owner.size(); i++) assertSame(firstEncounter.get(i), owner.get(i));
            for (int q = -1; q <= 32; q++) {
                assertEquals(jdk.contains(new Key(q)), owner.contains(new Key(q)));
            }
            Object[] exported = owner.toArray();
            if (exported.length != 0) {
                exported[0] = new Key(-92);
                assertSame(firstEncounter.get(0), owner.get(0));
            }
            assertTrue(owner.asSet().spliterator().hasCharacteristics(
                    Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL));
        }
    }

    @Test
    void mapSnapshotsMatchJdkIncludingOrderedViewsAndDetachedEntries() {
        for (int size = 0; size <= 80; size++) {
            LinkedHashMap<Key, String> source = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) source.put(new Key(i), new String("value-" + (i % 7)));
            Map<Key, String> jdk = Map.copyOf(source);
            Map<Key, String> ordered = Collections.unmodifiableMap(new LinkedHashMap<>(source));
            FrozenMap<Key, String> owner = FrozenMap.copyOf(source);
            source.clear();
            assertEquals(jdk.size(), owner.size());
            assertEquals(jdk.isEmpty(), owner.isEmpty());
            assertEquals(jdk, owner.asMap());
            assertEquals(ordered, owner.asMap());
            assertEquals(jdk.hashCode(), owner.asMap().hashCode());
            assertEquals(new ArrayList<>(ordered.keySet()), owner.asMap().keySet().stream().toList());
            assertEquals(new ArrayList<>(ordered.values()), owner.asMap().values().parallelStream().toList());
            assertEquals(new ArrayList<>(ordered.entrySet()), owner.entrySet().stream().toList());
            for (int i = 0; i < size; i++) {
                Key key = new ArrayList<>(ordered.keySet()).get(i);
                assertSame(key, owner.keyAt(i));
                assertSame(ordered.get(key), owner.valueAt(i));
                assertSame(ordered.get(key), owner.get(new Key(i)));
                assertSame(ordered.get(key), owner.getOrDefault(new Key(i), "missing"));
                assertTrue(owner.containsKey(new Key(i)));
                assertTrue(owner.containsValue(new String("value-" + (i % 7))));
                assertTrue(owner.entrySet().contains(Map.entry(new Key(i), "value-" + (i % 7))));
            }
            assertFalse(owner.containsKey(new Key(-1)));
            assertNull(owner.get(new Key(-1)));
            assertEquals("missing", owner.getOrDefault(new Key(-1), "missing"));
            assertFalse(owner.containsValue("absent"));
            assertFalse(owner.entrySet().contains("not an entry"));
            assertFalse(owner.entrySet().contains(Map.entry(new Key(-1), "value-0")));
            if (size != 0) {
                Map.Entry<Key, String> first = owner.entrySet().iterator().next();
                Map.Entry<Key, String> another = owner.entrySet().iterator().next();
                assertNotSame(first, another);
                assertEquals(first, another);
                assertSame(owner.keyAt(0), first.getKey());
                assertSame(owner.valueAt(0), first.getValue());
                assertThrows(UnsupportedOperationException.class, () -> first.setValue("replacement"));
            }
        }
    }

    @Test
    void factoriesAreNullHostileButQueriesFollowDeclaredAbsenceSemantics() {
        assertThrows(NullPointerException.class, () -> FrozenList.copyOf(null));
        assertThrows(NullPointerException.class, () -> FrozenSet.copyOf(null));
        assertThrows(NullPointerException.class, () -> FrozenMap.copyOf(null));
        assertThrows(NullPointerException.class, () -> FrozenList.copyOf(Arrays.asList("a", null)));
        assertThrows(NullPointerException.class, () -> FrozenSet.copyOf(Arrays.asList("a", null)));
        Map<String, String> source = new LinkedHashMap<>();
        source.put(null, "value");
        assertThrows(NullPointerException.class, () -> FrozenMap.copyOf(source));
        source.clear(); source.put("key", null);
        assertThrows(NullPointerException.class, () -> FrozenMap.copyOf(source));
        for (List<String> values : List.of(List.<String>of(), List.of("a", "a", "b"))) {
            FrozenList<String> list = FrozenList.copyOf(values);
            FrozenSet<String> set = FrozenSet.copyOf(values);
            assertEquals(-1, list.indexOf(null));
            assertFalse(list.contains(null));
            assertFalse(list.asList().contains(null));
            assertFalse(set.contains(null));
            assertFalse(set.asSet().contains(null));
        }
        for (Map<String, String> values : List.of(Map.<String, String>of(), Map.of("key", "value"))) {
            FrozenMap<String, String> map = FrozenMap.copyOf(values);
            assertNull(map.get(null));
            assertFalse(map.containsKey(null));
            assertFalse(map.containsValue(null));
            assertEquals("missing", map.getOrDefault(null, "missing"));
            assertFalse(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(null, "value")));
            assertFalse(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("key", null)));
            assertFalse(map.asMap().keySet().contains(null));
            assertFalse(map.asMap().values().contains(null));
        }
    }

    @Test
    void allOwnerAndFacadeMutatorsRejectWithoutInvokingUserCallbacks() {
        for (List<String> input : List.of(List.<String>of(), List.of("a", "b"))) {
            FrozenList<String> list = FrozenList.copyOf(input);
            FrozenSet<String> set = FrozenSet.copyOf(input);
            assertThrows(UnsupportedOperationException.class, () -> list.add(null));
            assertThrows(UnsupportedOperationException.class, () -> list.set(-1, null));
            assertThrows(UnsupportedOperationException.class, () -> set.add(null));
            assertThrows(UnsupportedOperationException.class, () -> set.remove("missing"));
            assertCollectionFrozen(list.asList());
            assertCollectionFrozen(set.asSet());
            assertThrows(UnsupportedOperationException.class, () -> list.asList().set(-1, null));
            assertThrows(UnsupportedOperationException.class, () -> list.asList().replaceAll(v -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, () -> list.asList().sort((a, b) -> fail("comparator")));
            assertThrows(UnsupportedOperationException.class, () -> list.asList().listIterator().add("x"));
            assertThrows(UnsupportedOperationException.class, () -> list.asList().listIterator().set("x"));
            assertThrows(UnsupportedOperationException.class, () -> list.asList().subList(0, 0).clear());
        }
        for (Map<String, String> input : List.of(Map.<String, String>of(), Map.of("key", "value"))) {
            FrozenMap<String, String> owner = FrozenMap.copyOf(input);
            Map<String, String> map = owner.asMap();
            assertThrows(UnsupportedOperationException.class, () -> owner.put(null, null));
            assertThrows(UnsupportedOperationException.class, () -> owner.remove("missing"));
            assertThrows(UnsupportedOperationException.class, () -> map.putAll(Map.of()));
            assertThrows(UnsupportedOperationException.class, map::clear);
            assertThrows(UnsupportedOperationException.class, () -> map.remove("missing"));
            assertThrows(UnsupportedOperationException.class, () -> map.remove("missing", "absent"));
            assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("key", "value"));
            assertThrows(UnsupportedOperationException.class, () -> map.replace("missing", "absent"));
            assertThrows(UnsupportedOperationException.class, () -> map.replace("missing", "old", "new"));
            assertThrows(UnsupportedOperationException.class, () -> map.compute("key", (k, v) -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("missing", k -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("key", (k, v) -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, () -> map.merge("key", "v", (a, b) -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> fail("callback")));
            assertCollectionFrozen(map.keySet());
            assertCollectionFrozen(map.values());
            assertCollectionFrozen(owner.entrySet());
        }
    }

    @Test
    void emptyOwnersBoundsAndIteratorFailureContractsAreComplete() {
        FrozenList<String> list = new FrozenList<>();
        FrozenSet<String> set = new FrozenSet<>();
        FrozenMap<String, String> map = new FrozenMap<>();
        assertTrue(list.isEmpty()); assertTrue(set.isEmpty()); assertTrue(map.isEmpty());
        assertEquals(List.of(), list.asList()); assertEquals(Set.of(), set.asSet()); assertEquals(Map.of(), map.asMap());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(Integer.MAX_VALUE));
        for (Iterator<?> iterator : List.of(list.iterator(), set.iterator(), map.entrySet().iterator())) {
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            assertThrows(UnsupportedOperationException.class, iterator::remove);
            assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(null));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> FrozenList.copyOf(List.of(1)).get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> FrozenSet.copyOf(List.of(1)).get(Integer.MIN_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> FrozenMap.copyOf(Map.of(1, 2)).valueAt(1));
        assertEquals("M3FrozenList|FROZEN_LIST|OBJECT|FROZEN|Object[] valueLane", FrozenList.scaffoldDescriptor());
        assertEquals("M3FrozenSet|FROZEN_SET|OBJECT|FROZEN|Object[] valueLane;int[] hashIndexLane", FrozenSet.scaffoldDescriptor());
        assertEquals("M3FrozenMap|FROZEN_MAP|OBJECT|FROZEN|Object[] keyLane;Object[] valueLane;int[] hashIndexLane", FrozenMap.scaffoldDescriptor());
    }

    @Test
    void duplicateEqualMapKeysRejectEvenWhenSourceUsesIdentity() {
        IdentityHashMap<Key, String> source = new IdentityHashMap<>();
        source.put(new Key(7), "first"); source.put(new Key(7), "second");
        assertThrows(IllegalArgumentException.class, () -> Map.copyOf(source));
        assertThrows(IllegalArgumentException.class, () -> FrozenMap.copyOf(source));
        assertEquals(2, source.size());
    }

    @Test
    void admissionTraversesActualElementsAndDoesNotTrustReportedSizes() {
        Object first = new Object(); Object second = new Object();
        Collection<Object> source = new AbstractCollection<>() {
            @Override public int size() { return Integer.MAX_VALUE; }
            @Override public Iterator<Object> iterator() { return List.of(first, second, first).iterator(); }
        };
        assertArrayEquals(new Object[]{first, second, first}, FrozenList.copyOf(source).toArray());
        assertArrayEquals(new Object[]{first, second}, FrozenSet.copyOf(source).toArray());
        AtomicInteger keys = new AtomicInteger(), values = new AtomicInteger();
        Map.Entry<Object, Object> entry = new Map.Entry<>() {
            @Override public Object getKey() { keys.incrementAndGet(); return first; }
            @Override public Object getValue() { values.incrementAndGet(); return second; }
            @Override public Object setValue(Object value) { throw new UnsupportedOperationException(); }
        };
        Map<Object, Object> map = new AbstractMap<>() {
            @Override public Set<Entry<Object, Object>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return Integer.MAX_VALUE; }
                    @Override public Iterator<Entry<Object, Object>> iterator() { return List.of(entry).iterator(); }
                };
            }
        };
        FrozenMap<Object, Object> frozen = FrozenMap.copyOf(map);
        assertEquals(1, keys.get()); assertEquals(1, values.get());
        assertSame(first, frozen.keyAt(0)); assertSame(second, frozen.valueAt(0));
    }

    @Test
    void callbackFailuresPropagateExactlyAndConstructionDoesNotPublishPartialOwners() {
        RuntimeException failure = new IllegalStateException("hash witness");
        Key failing = new Key(8); failing.hashFailure = failure;
        List<Key> source = List.of(new Key(1), failing);
        FrozenList<Key> list = FrozenList.copyOf(source);
        assertSame(failing, list.get(1));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenSet.copyOf(source)));
        Map<Key, String> map = new AbstractMap<>() {
            @Override public Set<Entry<Key, String>> entrySet() {
                return Set.of(new AbstractMap.SimpleImmutableEntry<>(failing, "value"));
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenMap.copyOf(map)));
        assertSame(failing, list.get(1));
        Key first = new Key(9), other = new Key(10);
        FrozenSet<Key> oldSet = FrozenSet.copyOf(List.of(first));
        RuntimeException equalityFailure = new IllegalStateException("equality witness");
        first.equalityFailure = equalityFailure;
        assertSame(equalityFailure, assertThrows(IllegalStateException.class,
                () -> FrozenSet.copyOf(List.of(first, other))));
        assertEquals(1, oldSet.size()); assertSame(first, oldSet.get(0));
    }

    @Test
    void java21ReverseAndSubListViewsPreserveOrderAndRejectNoOpMutations() {
        for (List<String> input : List.of(List.<String>of(), List.of("first", "middle", "first", "last"))) {
            FrozenList<String> owner = FrozenList.copyOf(input);
            List<String> forward = owner.asList();
            List<String> reverse = forward.reversed();
            List<String> expected = new ArrayList<>();
            for (int index = input.size() - 1; index >= 0; index--) expected.add(input.get(index));
            assertEquals(expected, reverse);
            assertEquals(forward, reverse.reversed());
            for (List<String> view : List.of(reverse, forward.subList(0, forward.size()),
                    reverse.subList(0, reverse.size()), forward.subList(0, 0), reverse.subList(0, 0))) {
                assertCollectionFrozen(view);
                assertThrows(UnsupportedOperationException.class, () -> view.removeIf(v -> false));
                assertThrows(UnsupportedOperationException.class, () -> view.replaceAll(v -> fail("callback")));
                assertThrows(UnsupportedOperationException.class, () -> view.sort((a, b) -> fail("comparator")));
                assertThrows(UnsupportedOperationException.class, () -> view.addFirst("first"));
                assertThrows(UnsupportedOperationException.class, () -> view.addLast("last"));
            }
            if (!forward.isEmpty()) {
                assertSame(forward.getFirst(), reverse.getLast());
                assertSame(forward.getLast(), reverse.getFirst());
            }
        }
    }

    @Test
    void frozenOwnersRetainFinalStateAndNoCollectionEntryPayload() throws Exception {
        for (Class<?> type : List.of(FrozenList.class, FrozenSet.class, FrozenMap.class)) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(Modifier.isFinal(field.getModifiers()), field.toString());
                assertFalse(Map.Entry.class.isAssignableFrom(field.getType()), field.toString());
                assertFalse(Collection.class.isAssignableFrom(field.getType()), field.toString());
                assertFalse(Map.class.isAssignableFrom(field.getType()), field.toString());
                Class<?> component = field.getType();
                while (component.isArray()) component = component.getComponentType();
                assertFalse(Map.Entry.class.isAssignableFrom(component), field.toString());
                assertFalse(Set.of("Node", "Entry", "Cell", "Bucket").contains(component.getSimpleName()), field.toString());
            }
        }
    }

    private static void assertCollectionFrozen(Collection<?> collection) {
        assertThrows(UnsupportedOperationException.class, () -> collection.add(null));
        assertThrows(UnsupportedOperationException.class, () -> collection.addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> collection.remove(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> collection.removeAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> collection.retainAll(collection));
        assertThrows(UnsupportedOperationException.class, () -> collection.removeIf(v -> fail("predicate")));
        assertThrows(UnsupportedOperationException.class, collection::clear);
        assertThrows(UnsupportedOperationException.class, () -> collection.iterator().remove());
    }

    private static final class Key {
        private final int id;
        private RuntimeException hashFailure;
        private RuntimeException equalityFailure;
        Key(int id) { this.id = id; }
        @Override public int hashCode() { if (hashFailure != null) throw hashFailure; return 7; }
        @Override public boolean equals(Object other) {
            if (equalityFailure != null) throw equalityFailure;
            return other instanceof Key key && id == key.id;
        }
        @Override public String toString() { return "key-" + id; }
    }
}
