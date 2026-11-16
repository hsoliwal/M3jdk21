/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.jupiter.api.Test;

/** Product JDK/JUnit oracles for the two existing immutable bijection owners. */
final class FrozenBiMapContractTest {
    @Test
    void objectSnapshotAndInverseMatchIndependentMapsInEncounterOrder() {
        Random random = new Random(0x46524f5a42494d31L);
        for (int size = 0; size < 80; size++) {
            LinkedHashMap<Key, Key> source = new LinkedHashMap<>();
            ArrayList<Integer> values = new ArrayList<>();
            for (int i = 0; i < size; i++) values.add(i + 1000);
            java.util.Collections.shuffle(values, random);
            for (int i = 0; i < size; i++) source.put(new Key(i), new Key(values.get(i)));
            LinkedHashMap<Key, Key> forward = new LinkedHashMap<>(source);
            LinkedHashMap<Key, Key> reverse = new LinkedHashMap<>();
            source.forEach((key, value) -> reverse.put(value, key));
            FrozenBiMap<Key, Key> owner = FrozenBiMap.copyOf(source);
            source.clear();
            assertEquals(size, owner.size());
            assertEquals(Map.copyOf(forward), owner.asMap());
            assertEquals(reverse, owner.inverse().asMap());
            assertEquals(forward.hashCode(), owner.asMap().hashCode());
            assertEquals(new ArrayList<>(forward.keySet()), owner.asMap().keySet().stream().toList());
            assertEquals(new ArrayList<>(forward.values()), owner.asMap().values().parallelStream().toList());
            assertEquals(new ArrayList<>(reverse.entrySet()), owner.inverse().asMap().entrySet().stream().toList());
            for (Map.Entry<Key, Key> entry : forward.entrySet()) {
                assertSame(entry.getValue(), owner.get(new Key(entry.getKey().id)));
                assertSame(entry.getKey(), owner.inverseGet(new Key(entry.getValue().id)));
                assertTrue(owner.containsKey(new Key(entry.getKey().id)));
                assertTrue(owner.containsValue(new Key(entry.getValue().id)));
            }
            assertNull(owner.get(new Key(-1)));
            assertNull(owner.inverseGet(new Key(-1)));
            assertFalse(owner.containsKey(new Key(-1)));
            assertFalse(owner.containsValue(new Key(-1)));
            assertSame(owner.inverse(), owner.inverse());
            assertSame(owner, owner.inverse().inverse());
        }
    }

    @Test
    void longSnapshotAndInverseKeepExactInputPairOrdinals() {
        Random random = new Random(0x46524f5a42494d32L);
        for (int seed = 0; seed < 96; seed++) {
            int size = seed * 7 % 97;
            LinkedHashSet<Long> unique = new LinkedHashSet<>();
            for (long edge : new long[]{0, Long.MIN_VALUE, Long.MAX_VALUE, -1, 1}) {
                if (unique.size() < size) unique.add(edge);
            }
            while (unique.size() < size) unique.add(random.nextLong());
            long[] keys = unique.stream().mapToLong(Long::longValue).toArray();
            long[] values = keys.clone();
            for (int i = size - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                long swap = values[i]; values[i] = values[j]; values[j] = swap;
            }
            long[] expectedKeys = keys.clone(), expectedValues = values.clone();
            FrozenLongBiMap owner = FrozenLongBiMap.copyOf(keys, values);
            Arrays.fill(keys, 17); Arrays.fill(values, 17);
            assertEquals(size, owner.size());
            assertSame(owner.inverse(), owner.inverse());
            assertSame(owner, owner.inverse().inverse());
            for (int i = 0; i < size; i++) {
                assertEquals(expectedKeys[i], owner.keyAt(i));
                assertEquals(expectedValues[i], owner.valueAt(i));
                assertEquals(expectedValues[i], owner.inverse().keyAt(i));
                assertEquals(expectedKeys[i], owner.inverse().valueAt(i));
                assertEquals(expectedValues[i], owner.getOrDefault(expectedKeys[i], 99));
                assertEquals(expectedKeys[i], owner.inverseGetOrDefault(expectedValues[i], 99));
                assertEquals(expectedKeys[i], owner.inverse().getOrDefault(expectedValues[i], 99));
                assertTrue(owner.containsKey(expectedKeys[i]));
                assertTrue(owner.containsValue(expectedValues[i]));
            }
            long absent = 19;
            while (unique.contains(absent)) absent++;
            assertFalse(owner.containsKey(absent));
            assertFalse(owner.containsValue(absent));
            assertEquals(Long.MIN_VALUE, owner.getOrDefault(absent, Long.MIN_VALUE));
            assertEquals(Long.MAX_VALUE, owner.inverseGetOrDefault(absent, Long.MAX_VALUE));
        }
    }

    @Test
    void primitiveIndexHandlesExactHashCollisionsZeroExtremaAndCapacityBounds() {
        assertEquals(PackedLongRowIndex.hash(85078L, 0, false),
                PackedLongRowIndex.hash(177926L, 0, false));
        long[] keys = {0, Long.MIN_VALUE, Long.MAX_VALUE, 85078L, 177926L, -1};
        long[] values = {177926L, 85078L, 0, Long.MIN_VALUE, -1, Long.MAX_VALUE};
        FrozenLongBiMap map = FrozenLongBiMap.copyOf(keys, values);
        for (int i = 0; i < keys.length; i++) {
            assertEquals(values[i], map.getOrDefault(keys[i], 123));
            assertEquals(keys[i], map.inverseGetOrDefault(values[i], 123));
        }
        for (int size : new int[]{0, 1, 2, 3, 4, 5, 8, 13, 26, 64, 127}) {
            int capacity = PackedLongRowIndex.capacityFor(size);
            assertTrue(capacity >= 4 && (capacity & (capacity - 1)) == 0);
            assertTrue((long) capacity * 13 / 20 >= size);
        }
        int max = (int) (((1L << 30) * 13) / 20);
        assertEquals(1 << 30, PackedLongRowIndex.capacityFor(max));
        assertThrows(IllegalArgumentException.class, () -> PackedLongRowIndex.capacityFor(max + 1));
        assertThrows(IllegalArgumentException.class, () -> PackedLongRowIndex.capacityFor(-1));
    }

    @Test
    void duplicateDomainsAreRejectedWithoutChangingCallerStorage() {
        IdentityHashMap<Key, Key> duplicateKeys = new IdentityHashMap<>();
        duplicateKeys.put(new Key(1), new Key(10));
        duplicateKeys.put(new Key(1), new Key(11));
        assertThrows(IllegalArgumentException.class, () -> FrozenBiMap.copyOf(duplicateKeys));
        LinkedHashMap<Key, Key> duplicateValues = new LinkedHashMap<>();
        duplicateValues.put(new Key(1), new Key(10));
        duplicateValues.put(new Key(2), new Key(10));
        assertThrows(IllegalArgumentException.class, () -> FrozenBiMap.copyOf(duplicateValues));
        assertEquals(2, duplicateKeys.size()); assertEquals(2, duplicateValues.size());
        long[] keys = {1, 1}, values = {2, 3};
        assertThrows(IllegalArgumentException.class, () -> FrozenLongBiMap.copyOf(keys, values));
        assertArrayEquals(new long[]{1, 1}, keys); assertArrayEquals(new long[]{2, 3}, values);
        assertThrows(IllegalArgumentException.class, () -> FrozenLongBiMap.copyOf(new long[]{1, 2}, new long[]{3, 3}));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongBiMap.copyOf(new long[]{0, 0}, new long[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongBiMap.copyOf(new long[]{1, 2}, new long[]{0, 0}));
    }

    @Test
    void nullIngressIsRejectedAndObjectNullQueriesAreAbsentInBothDirections() {
        assertThrows(NullPointerException.class, () -> FrozenBiMap.copyOf(null));
        LinkedHashMap<String, String> input = new LinkedHashMap<>();
        input.put(null, "value");
        assertThrows(NullPointerException.class, () -> FrozenBiMap.copyOf(input));
        input.clear(); input.put("key", null);
        assertThrows(NullPointerException.class, () -> FrozenBiMap.copyOf(input));
        assertThrows(NullPointerException.class, () -> FrozenLongBiMap.copyOf(null, new long[0]));
        assertThrows(NullPointerException.class, () -> FrozenLongBiMap.copyOf(new long[0], null));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongBiMap.copyOf(new long[1], new long[0]));
        for (Map<String, String> source : List.of(Map.<String, String>of(), Map.of("key", "value"))) {
            FrozenBiMap<String, String> owner = FrozenBiMap.copyOf(source);
            for (FrozenBiMap<String, String> direction : List.of(owner, owner.inverse())) {
                assertNull(direction.get(null)); assertNull(direction.inverseGet(null));
                assertFalse(direction.containsKey(null)); assertFalse(direction.containsValue(null));
                assertFalse(direction.asMap().keySet().contains(null));
                assertFalse(direction.asMap().values().contains(null));
                assertEquals("missing", direction.asMap().getOrDefault(null, "missing"));
                assertFalse(direction.asMap().entrySet().contains(
                        new AbstractMap.SimpleImmutableEntry<>(null, "value")));
            }
        }
    }

    @Test
    void emptyOwnersAreSelfInverseAndOrdinalBoundsStayChecked() {
        FrozenBiMap<String, Integer> object = new FrozenBiMap<>();
        assertEquals(0, object.size()); assertSame(object, object.inverse());
        assertEquals(Map.of(), object.asMap());
        assertThrows(NoSuchElementException.class, () -> object.asMap().entrySet().iterator().next());
        FrozenLongBiMap primitive = new FrozenLongBiMap();
        assertEquals(0, primitive.size()); assertSame(primitive, primitive.inverse());
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.keyAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.valueAt(-1));
        FrozenLongBiMap one = FrozenLongBiMap.copyOf(new long[]{7}, new long[]{9});
        assertThrows(IndexOutOfBoundsException.class, () -> one.keyAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> one.inverse().valueAt(Integer.MIN_VALUE));
        long[] both = {3, 1, 7};
        FrozenLongBiMap identity = FrozenLongBiMap.copyOf(both, both);
        Arrays.fill(both, 0);
        assertEquals(3, identity.keyAt(0)); assertEquals(3, identity.valueAt(0));
        assertEquals(1, identity.getOrDefault(1, 99));
        assertEquals("M3FrozenBiMap|FROZEN_BIMAP|OBJECT|FROZEN|Object[] keyLane;Object[] valueLane;int[] forwardIndexLane;int[] inverseIndexLane", FrozenBiMap.scaffoldDescriptor());
        assertEquals("M3FrozenLongBiMap|FROZEN_LONG_BIMAP|LONG|FROZEN|long[] keyLane;long[] valueLane;int[] forwardIndexLane;int[] inverseIndexLane", FrozenLongBiMap.scaffoldDescriptor());
    }

    @Test
    void allOwnerAndFacadeMutationsRefuseBeforeCallbacksInBothDirections() {
        for (Map<String, String> source : List.of(Map.<String, String>of(), Map.of("key", "value"))) {
            FrozenBiMap<String, String> owner = FrozenBiMap.copyOf(source);
            for (FrozenBiMap<String, String> direction : List.of(owner, owner.inverse())) {
                assertThrows(UnsupportedOperationException.class, () -> direction.put(null, null));
                assertThrows(UnsupportedOperationException.class, () -> direction.forcePut(null, null));
                assertThrows(UnsupportedOperationException.class, () -> direction.remove("missing"));
                assertMapFrozen(direction.asMap());
            }
        }
        for (FrozenLongBiMap map : List.of(new FrozenLongBiMap(), FrozenLongBiMap.copyOf(new long[]{0}, new long[]{1}))) {
            for (FrozenLongBiMap direction : List.of(map, map.inverse())) {
                assertThrows(UnsupportedOperationException.class, () -> direction.put(0, 1));
                assertThrows(UnsupportedOperationException.class, () -> direction.forcePut(0, 1));
                assertThrows(UnsupportedOperationException.class, () -> direction.remove(99));
            }
        }
    }

    @Test
    void entryProjectionsAreIndependentImmutableAndOrdered() {
        LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
        source.put("b", 2); source.put("a", 1); source.put("c", 3);
        FrozenBiMap<String, Integer> map = FrozenBiMap.copyOf(source);
        Iterator<Map.Entry<String, Integer>> iterator = map.asMap().entrySet().iterator();
        Map.Entry<String, Integer> first = iterator.next();
        assertEquals(Map.entry("b", 2), first);
        assertNotSame(first, map.asMap().entrySet().iterator().next());
        iterator.next(); iterator.next();
        assertEquals(Map.entry("b", 2), first);
        assertThrows(UnsupportedOperationException.class, () -> first.setValue(4));
        assertThrows(NoSuchElementException.class, iterator::next);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(List.of(2, 1, 3), map.inverse().asMap().keySet().stream().toList());
        assertEquals(List.of("b", "a", "c"), map.inverse().asMap().values().stream().toList());
        assertTrue(map.asMap().entrySet().spliterator().hasCharacteristics(
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL));
        assertTrue(map.asMap().entrySet().contains(Map.entry("a", 1)));
        assertFalse(map.asMap().entrySet().contains(Map.entry("a", 2)));
        assertFalse(map.asMap().entrySet().contains("not an entry"));
    }

    @Test
    void factoryUsesActualEntriesAndReadsEachAccessorOnce() {
        Key key = new Key(1), value = new Key(2);
        int[] reads = new int[2];
        Map.Entry<Key, Key> entry = new Map.Entry<>() {
            @Override public Key getKey() { reads[0]++; return key; }
            @Override public Key getValue() { reads[1]++; return value; }
            @Override public Key setValue(Key replacement) { throw new UnsupportedOperationException(); }
        };
        FrozenBiMap<Key, Key> map = FrozenBiMap.copyOf(entries(List.of(entry), Integer.MAX_VALUE));
        assertArrayEquals(new int[]{1, 1}, reads);
        assertEquals(1, map.size()); assertSame(value, map.get(key)); assertSame(key, map.inverseGet(value));
    }

    @Test
    void keyValueCallbackFailuresPropagateWithoutPublishingPartialSnapshots() {
        RuntimeException failure = new IllegalStateException("bimap hash witness");
        Key key = new Key(1), value = new Key(2);
        Map<Key, Key> source = entries(List.of(new AbstractMap.SimpleImmutableEntry<>(key, value)), 1);
        FrozenBiMap<Key, Key> preserved = FrozenBiMap.copyOf(source);
        key.hashFailure = failure;
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenBiMap.copyOf(source)));
        key.hashFailure = null; value.hashFailure = failure;
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenBiMap.copyOf(source)));
        assertEquals(1, preserved.size());
        assertSame(key, preserved.asMap().entrySet().iterator().next().getKey());
        value.hashFailure = null;
        assertSame(value, preserved.get(key));
        Key firstValue = new Key(5), nextValue = new Key(6);
        firstValue.equalityFailure = failure;
        Map<Key, Key> equalitySource = entries(List.of(Map.entry(new Key(7), firstValue), Map.entry(new Key(8), nextValue)), 2);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenBiMap.copyOf(equalitySource)));
    }

    @Test
    void facadeForEachKeepsOrderNullChecksAndExceptionIdentity() {
        LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
        source.put("c", 3); source.put("a", 1); source.put("b", 2);
        FrozenBiMap<String, Integer> map = FrozenBiMap.copyOf(source);
        ArrayList<String> tape = new ArrayList<>();
        map.asMap().forEach((key, value) -> tape.add(key + value));
        assertEquals(List.of("c3", "a1", "b2"), tape);
        tape.clear(); map.inverse().asMap().forEach((key, value) -> tape.add(value + key));
        assertEquals(List.of("c3", "a1", "b2"), tape);
        assertThrows(NullPointerException.class, () -> map.asMap().forEach(null));
        assertThrows(NullPointerException.class, () -> new FrozenBiMap<>().asMap().forEach(null));
        RuntimeException failure = new IllegalArgumentException("callback");
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> map.asMap().forEach((key, value) -> { throw failure; })));
        assertEquals(source, map.asMap());
    }

    @Test
    void ownerStateIsFinalAndDoesNotStoreEntryNodeOrJavaCollectionPayload() {
        for (Class<?> type : List.of(FrozenBiMap.class, FrozenLongBiMap.class)) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(Modifier.isFinal(field.getModifiers()), field.toString());
                assertFalse(Collection.class.isAssignableFrom(field.getType()), field.toString());
                assertFalse(Map.class.isAssignableFrom(field.getType()), field.toString());
                Class<?> component = field.getType();
                while (component.isArray()) component = component.getComponentType();
                assertFalse(Map.Entry.class.isAssignableFrom(component), field.toString());
                assertFalse(Set.of("Node", "Entry", "Cell", "Bucket").contains(component.getSimpleName()), field.toString());
            }
        }
    }

    private static <K, V> Map<K, V> entries(List<Map.Entry<K, V>> entries, int reportedSize) {
        return new AbstractMap<>() {
            @Override public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return reportedSize; }
                    @Override public Iterator<Entry<K, V>> iterator() { return entries.iterator(); }
                };
            }
        };
    }

    private static <K, V> void assertMapFrozen(Map<K, V> map) {
        assertThrows(UnsupportedOperationException.class, () -> map.put(null, null));
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("missing"));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("missing", "missing"));
        assertThrows(UnsupportedOperationException.class, map::clear);
        assertThrows(UnsupportedOperationException.class, () -> map.compute(null, (k, v) -> fail("callback")));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent(null, k -> fail("callback")));
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent(null, (k, v) -> fail("callback")));
        assertThrows(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> fail("callback")));
        for (Collection<?> view : List.of(map.entrySet(), map.keySet(), map.values())) {
            assertThrows(UnsupportedOperationException.class, () -> view.addAll(List.of()));
            assertThrows(UnsupportedOperationException.class, () -> view.remove("missing"));
            assertThrows(UnsupportedOperationException.class, () -> view.removeAll(List.of()));
            assertThrows(UnsupportedOperationException.class, () -> view.retainAll(view));
            assertThrows(UnsupportedOperationException.class, () -> view.removeIf(v -> fail("callback")));
            assertThrows(UnsupportedOperationException.class, view::clear);
            assertThrows(UnsupportedOperationException.class, () -> view.iterator().remove());
        }
    }

    private static final class Key {
        private final int id;
        private RuntimeException hashFailure;
        private RuntimeException equalityFailure;
        Key(int id) { this.id = id; }
        @Override public int hashCode() { if (hashFailure != null) throw hashFailure; return 19; }
        @Override public boolean equals(Object other) {
            if (equalityFailure != null) throw equalityFailure;
            return other instanceof Key key && id == key.id;
        }
        @Override public String toString() { return "key-" + id; }
    }
}
