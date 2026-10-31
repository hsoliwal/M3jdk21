/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

final class M3LibraryCollectionsTest {
    private record Key(int id) {
        @Override public int hashCode() { return 1; }
    }

    @Test void jdkFactoriesReuseOwners() {
        assertInstanceOf(M3LinkedHashMap.class, M3LibraryCollections.jdkMap());
        assertInstanceOf(M3LinkedHashSet.class, M3LibraryCollections.jdkSet());
        assertInstanceOf(M3LinkedList.class, M3LibraryCollections.jdkList());
        assertInstanceOf(M3LinkedList.class, M3LibraryCollections.jdkDeque());
    }

    @Test void jdkMapCollisionNullAndDefaultMethods() { mapTrace(M3LibraryCollections.jdkMap()); }
    @Test void eclipseMapCollisionNullAndDefaultMethods() { mapTrace(M3LibraryCollections.eclipseMap()); }

    private static void mapTrace(Map<Key, Integer> actual) {
        Map<Key, Integer> expected = new LinkedHashMap<>();
        Random random = new Random(20261015L);
        for (int step = 0; step < 1500; step++) {
            Key key = random.nextInt(8) == 0 ? null : new Key(random.nextInt(40));
            Integer value = random.nextInt(6) == 0 ? null : random.nextInt(50);
            switch (random.nextInt(8)) {
                case 0 -> assertEquals(expected.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(expected.remove(key), actual.remove(key));
                case 2 -> assertEquals(expected.putIfAbsent(key, value), actual.putIfAbsent(key, value));
                case 3 -> assertEquals(expected.compute(key, (k, v) -> value), actual.compute(key, (k, v) -> value));
                case 4 -> assertEquals(expected.replace(key, value), actual.replace(key, value));
                case 5 -> assertEquals(expected.remove(key, value), actual.remove(key, value));
                case 6 -> assertEquals(expected.computeIfAbsent(key, k -> value), actual.computeIfAbsent(key, k -> value));
                default -> assertEquals(expected.get(key), actual.get(key));
            }
            assertEquals(expected, actual, "step=" + step);
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
        }
    }

    @Test void jdkListAndBackedSubList() {
        List<Integer> actual = M3LibraryCollections.jdkList();
        List<Integer> expected = new LinkedList<>();
        Random random = new Random(31L);
        for (int step = 0; step < 1000; step++) {
            int position = random.nextInt(expected.size() + 1);
            if (expected.isEmpty() || random.nextBoolean()) {
                Integer value = step % 9 == 0 ? null : step;
                expected.add(position, value); actual.add(position, value);
            } else {
                int index = position % expected.size();
                assertEquals(expected.remove(index), actual.remove(index));
            }
            assertEquals(expected, actual);
        }
        actual.clear(); actual.addAll(List.of(1, 2, 3, 4));
        actual.subList(1, 3).clear(); assertEquals(List.of(1, 4), actual);
        var iterator = actual.listIterator(); iterator.next(); iterator.set(9); iterator.add(8);
        assertEquals(List.of(9, 8, 4), actual);
    }

    @Test void jdkSetAndIteratorRemoval() {
        Set<Integer> actual = M3LibraryCollections.jdkSet();
        Set<Integer> expected = new LinkedHashSet<>();
        for (int i = 0; i < 100; i++) assertEquals(expected.add(i % 13), actual.add(i % 13));
        assertEquals(expected.add(null), actual.add(null));
        var iterator = actual.iterator(); Integer first = iterator.next(); iterator.remove(); expected.remove(first);
        assertEquals(expected, actual); assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
    }

    @Test void jdkDequeEnds() {
        var actual = M3LibraryCollections.<Integer>jdkDeque();
        var expected = new LinkedList<Integer>();
        for (int i = 0; i < 200; i++) {
            if ((i & 1) == 0) { actual.addFirst(i); expected.addFirst(i); }
            else { actual.addLast(i); expected.addLast(i); }
        }
        while (!expected.isEmpty()) {
            assertEquals(expected.pollFirst(), actual.pollFirst());
            assertEquals(expected.pollLast(), actual.pollLast());
        }
        assertNull(actual.pollFirst()); assertThrows(java.util.NoSuchElementException.class, actual::removeLast);
    }

    @Test void jdkSortedMapRangesAndReverse() {
        var actual = M3LibraryCollections.<Integer, String>jdkSortedMap(Comparator.naturalOrder());
        var expected = new TreeMap<Integer, String>();
        for (int i = 0; i < 30; i++) { actual.put(i, "v" + i); expected.put(i, "v" + i); }
        actual.subMap(5, true, 20, false).clear(); expected.subMap(5, true, 20, false).clear();
        assertEquals(expected, actual);
        assertEquals(new ArrayList<>(expected.descendingKeySet()), new ArrayList<>(actual.descendingKeySet()));
        assertEquals(expected.pollFirstEntry(), actual.pollFirstEntry());
    }

    @Test void jdkSortedSetRangesAndReverse() {
        var actual = M3LibraryCollections.<Integer>jdkSortedSet(Comparator.reverseOrder());
        var expected = new TreeSet<Integer>(Comparator.reverseOrder());
        actual.addAll(List.of(1, 2, 3, 4)); expected.addAll(actual);
        actual.headSet(2, false).clear(); expected.headSet(2, false).clear();
        assertEquals(expected, actual); assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
    }

    @Test void guavaListMultimapDifferentialTrace() {
        var actual = M3LibraryCollections.<Integer, Integer>guavaListMultimap();
        var expected = ArrayListMultimap.<Integer, Integer>create();
        Random random = new Random(41L);
        for (int step = 0; step < 1500; step++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(15);
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(10);
            switch (random.nextInt(5)) {
                case 0 -> assertEquals(expected.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(expected.remove(key, value), actual.remove(key, value));
                case 2 -> assertEquals(expected.removeAll(key), actual.removeAll(key));
                case 3 -> { expected.get(key).add(value); actual.get(key).add(value); }
                default -> assertEquals(expected.replaceValues(key, Arrays.asList(value, value)),
                        actual.replaceValues(key, Arrays.asList(value, value)));
            }
            assertEquals(expected.asMap(), actual.asMap()); assertEquals(expected.size(), actual.size());
        }
    }

    @Test void guavaListMultimapLiveAbsentViewsAndDetachedRemovals() {
        var map = M3LibraryCollections.<String, Integer>guavaListMultimap();
        List<Integer> values = map.get("a"); assertFalse(map.containsKey("a"));
        values.addAll(List.of(1, 1, 2)); assertEquals(3, map.size());
        var removed = map.removeAll("a"); assertEquals(List.of(1, 1, 2), removed);
        assertTrue(values.isEmpty()); values.add(7); assertEquals(List.of(7), map.get("a"));
        assertEquals(List.of(1, 1, 2), removed);
        values.clear(); assertFalse(map.containsKey("a")); assertTrue(map.asMap().isEmpty());
        assertFalse(map.putAll("b", List.of())); assertFalse(map.containsKey("b"));
    }

    @Test void guavaListIteratorAndAsMapRemove() {
        var map = M3LibraryCollections.<String, Integer>guavaListMultimap();
        map.putAll("a", List.of(1, 2, 3));
        var iterator = map.get("a").listIterator(); iterator.next(); iterator.set(5); iterator.add(6);
        assertEquals(List.of(5, 6, 2, 3), map.get("a")); assertEquals(4, map.size());
        map.asMap().get("a").clear(); assertEquals(0, map.size()); assertFalse(map.containsKey("a"));
    }

    @Test void guavaSetMultimapDifferentialTrace() {
        var actual = M3LibraryCollections.<Integer, Integer>guavaSetMultimap();
        var expected = HashMultimap.<Integer, Integer>create();
        Random random = new Random(51L);
        for (int i = 0; i < 1000; i++) {
            int key = random.nextInt(20), value = random.nextInt(20);
            if (random.nextBoolean()) assertEquals(expected.put(key, value), actual.put(key, value));
            else assertEquals(expected.remove(key, value), actual.remove(key, value));
            assertEquals(expected, actual); assertEquals(expected.size(), actual.size());
        }
        actual.clear(); assertTrue(actual.put(null, null)); assertFalse(actual.put(null, null));
        actual.get(null).clear(); assertFalse(actual.containsKey(null));
    }

    @Test void guavaSortedValuesAndBackedRanges() {
        var actual = M3LibraryCollections.<String, Integer>guavaSortedSetMultimap(Comparator.reverseOrder());
        actual.putAll("a", List.of(1, 3, 2, 3));
        assertEquals(List.of(3, 2, 1), new ArrayList<>(actual.get("a")));
        actual.get("a").headSet(2).clear(); assertEquals(List.of(2, 1), new ArrayList<>(actual.get("a")));
        assertEquals(2, actual.size());
    }

    @Test void guavaTableRowsColumnsAndCellRemoval() {
        var actual = M3LibraryCollections.<String, String, Integer>guavaTable();
        var expected = HashBasedTable.<String, String, Integer>create();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 9; column++) {
            actual.put("r" + row, "c" + column, row * column);
            expected.put("r" + row, "c" + column, row * column);
        }
        actual.column("c2").clear(); expected.column("c2").clear();
        actual.row("r3").put("new", 10); expected.row("r3").put("new", 10);
        actual.rowMap().remove("r4"); expected.rowMap().remove("r4");
        assertEquals(expected, actual); assertEquals(expected.cellSet(), actual.cellSet());
        var iterator = actual.cellSet().iterator(); var cell = iterator.next(); iterator.remove();
        expected.remove(cell.getRowKey(), cell.getColumnKey()); assertEquals(expected, actual);
        assertThrows(NullPointerException.class, () -> actual.put(null, "x", 1));
        assertThrows(NullPointerException.class, () -> actual.put("x", "y", null));
    }

    @Test void eclipseListRichOperationsAndLiveView() {
        var actual = M3LibraryCollections.<Integer>eclipseList(); actual.addAll(List.of(1, 2, 3, 4));
        assertEquals(List.of(2, 4), actual.select(i -> i % 2 == 0));
        assertEquals(List.of("1", "2", "3", "4"), actual.collect(Object::toString));
        var view = actual.asUnmodifiable(); actual.add(5); assertEquals(5, view.size());
        assertThrows(UnsupportedOperationException.class, () -> view.add(6));
        var frozen = actual.toImmutable(); actual.clear(); assertEquals(5, frozen.size());
    }

    @Test void eclipseMapRichOperationsAndLiveView() {
        var actual = M3LibraryCollections.<String, Integer>eclipseMap();
        assertEquals(2, actual.getIfAbsentPut("a", () -> 2).intValue());
        assertEquals(2, actual.getIfAbsentPut("a", () -> 99).intValue());
        actual.updateValue("a", () -> 0, v -> v + 3); assertEquals(5, actual.get("a").intValue());
        var view = actual.asUnmodifiable(); actual.put("b", 7); assertEquals(7, view.get("b").intValue());
        assertThrows(UnsupportedOperationException.class, () -> view.put("c", 8));
    }

    @Test void eclipseSetAndSortedFamilies() {
        var set = M3LibraryCollections.<Integer>eclipseSet(); set.addAll(List.of(1, 2, 2, 3));
        assertEquals(Set.of(1, 2, 3), set); assertEquals(Set.of(2, 3), set.select(i -> i > 1));
        var sortedSet = M3LibraryCollections.<Integer>eclipseSortedSet(Comparator.reverseOrder());
        sortedSet.addAll(set); assertEquals(List.of(3, 2, 1), new ArrayList<>(sortedSet));
        sortedSet.headSet(2).clear(); assertEquals(Set.of(1, 2), sortedSet);
        var sortedMap = M3LibraryCollections.<Integer, String>eclipseSortedMap(Comparator.naturalOrder());
        sortedMap.put(3, "c"); sortedMap.put(1, "a"); sortedMap.put(2, "b");
        sortedMap.headMap(2).clear(); assertEquals(Map.of(2, "b", 3, "c"), sortedMap);
    }

    @Test void springDifferentialTrace() {
        var actual = M3LibraryCollections.<Integer, Integer>springMultiValueMap();
        var expected = new LinkedMultiValueMap<Integer, Integer>();
        Random random = new Random(61L);
        for (int i = 0; i < 1000; i++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(15);
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(10);
            switch (random.nextInt(5)) {
                case 0 -> { actual.add(key, value); expected.add(key, value); }
                case 1 -> { actual.set(key, value); expected.set(key, value); }
                case 2 -> { actual.addIfAbsent(key, value); expected.addIfAbsent(key, value); }
                case 3 -> assertEquals(expected.remove(key), actual.remove(key));
                default -> { actual.addAll(key, List.of()); expected.addAll(key, List.of()); }
            }
            assertEquals(expected, actual); assertEquals(expected.getFirst(key), actual.getFirst(key));
            assertEquals(expected.toSingleValueMap(), actual.toSingleValueMap());
        }
    }

    @Test void springEmptyAbsentNullCopyAndLiveView() {
        var map = M3LibraryCollections.<String, Integer>springMultiValueMap();
        map.addAll("empty", List.of()); assertTrue(map.containsKey("empty"));
        map.addIfAbsent("empty", 9); assertTrue(map.get("empty").isEmpty());
        map.add("a", 1); map.add("a", 2); assertEquals(2, map.size());
        var copy = map.toSingleValueMap(); var view = map.asSingleValueMap();
        map.set("a", 7); assertEquals(1, copy.get("a").intValue()); assertEquals(7, view.get("a").intValue());
        view.put("b", 8); assertEquals(List.of(8), map.get("b"));
        map.put("nullList", null); assertTrue(map.containsKey("nullList")); assertNull(map.getFirst("nullList"));
        map.add("nullList", null); assertEquals(Arrays.asList((Integer) null), map.get("nullList"));
    }

    @Test void springAndGuavaAreNotInterchangeableContracts() {
        var spring = M3LibraryCollections.<String, Integer>springMultiValueMap();
        var guava = M3LibraryCollections.<String, Integer>guavaListMultimap();
        spring.add("a", 1); spring.add("a", 2); guava.put("a", 1); guava.put("a", 2);
        assertEquals(1, spring.size()); assertEquals(2, guava.size());
        assertNull(spring.get("missing")); assertTrue(guava.get("missing").isEmpty());
        spring.get("a").clear(); guava.get("a").clear();
        assertTrue(spring.containsKey("a")); assertFalse(guava.containsKey("a"));
    }

    @Test void eachFactoryCallHasIndependentOwnership() {
        var a = M3LibraryCollections.<String, Integer>guavaListMultimap();
        var b = M3LibraryCollections.<String, Integer>guavaListMultimap();
        a.put("a", 1); a.put("b", 2); a.get("a").clear(); assertEquals(List.of(2), a.get("b")); assertTrue(b.isEmpty());
        var x = M3LibraryCollections.<String, Integer>springMultiValueMap();
        var y = M3LibraryCollections.<String, Integer>springMultiValueMap(); x.add("x", 3); assertTrue(y.isEmpty());
    }

    @Test void nullComparatorRefusal() {
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.jdkSortedMap(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.jdkSortedSet(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.guavaSortedSetMultimap(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.eclipseSortedMap(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.eclipseSortedSet(null));
    }
}
