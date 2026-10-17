/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Additive receiver oracle; no java.base or native ABI changes.
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Cross-family oracle for the existing Synexia-received wrapper owners.
 *
 * <p>Live filtering is not insertion-only predication or snapshot sorting.
 * Predicates in this oracle are pure and equality/order consistent except for
 * the deliberate stored-representative cases. Java predicates never cross JNI.
 */
final class M3FilteredCrossFamilyOracleTest {
    private static final Predicate<Integer> EVEN = value -> (value & 1) == 0;
    private static final BiPredicate<Integer, Integer> MAP_FILTER =
            (key, value) -> (key & 1) == 0 && value != null && value >= 0;

    @Test
    void erasedDispatchPreservesNavigationAndLiveRangeMutation() {
        NavigableSet<Integer> source = new TreeSet<>(
                Arrays.asList(1, 2, 3, 4, 5, 6));
        Collection<Integer> erased = source;
        Collection<Integer> selected = M3CollectionViews.filtered(erased, EVEN);
        NavigableSet<?> dynamic = assertInstanceOf(NavigableSet.class, selected);
        assertEquals(List.of(2, 4, 6), List.copyOf(dynamic));

        NavigableSet<Integer> view = M3CollectionViews.filtered(source, EVEN);
        assertEquals(Integer.valueOf(4), view.floor(5));
        assertEquals(Integer.valueOf(6), view.ceiling(5));
        assertEquals(Integer.valueOf(4), view.lower(6));
        assertEquals(Integer.valueOf(6), view.higher(4));
        assertThrows(IllegalArgumentException.class, () -> view.add(7));

        source.add(8);
        assertEquals(List.of(8, 6, 4, 2), List.copyOf(view.descendingSet()));
        NavigableSet<Integer> range = view.subSet(2, true, 8, true);
        assertEquals(List.of(2, 4, 6, 8), List.copyOf(range));
        range.clear();
        assertEquals(List.of(1, 3, 5), List.copyOf(source));
        assertTrue(view.isEmpty());
        assertFalse((view.spliterator().characteristics() & Spliterator.SIZED) != 0);
        assertFalse((view.spliterator().characteristics() & Spliterator.SUBSIZED) != 0);
    }

    @Test
    void comparatorRepresentativeAndPurposeWrappersRetainContracts() {
        NavigableSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        source.add("alpha");
        source.add("BRAVO");
        NavigableSet<String> visible =
                M3CollectionViews.filtered(source, "alpha"::equals);
        assertTrue(visible.contains("ALPHA"),
                "selection must examine the stored representative");
        assertFalse(visible.contains("bravo"));
        assertTrue(visible.remove("ALPHA"));
        assertEquals(Set.of("BRAVO"), source);
        assertThrows(IllegalArgumentException.class, () -> visible.add("ALPHA"));

        NavigableMap<String, Integer> map =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put("alpha", 2);
        map.put("BRAVO", -3);
        map.put("charlie", 4);
        Map<String, Integer> erasedMap = map;
        Map<String, Integer> filtered =
                M3FilteredMaps.filtered(erasedMap,
                        (key, value) -> !"BRAVO".equals(key) && value >= 0);
        NavigableMap<?, ?> dynamic =
                assertInstanceOf(NavigableMap.class, filtered);
        assertEquals(2, dynamic.size());
        assertEquals(Integer.valueOf(2), filtered.get("ALPHA"));
        assertFalse(filtered.containsKey("bravo"));

        NavigableMap<String, Integer> selected =
                M3FilteredMaps.filtered(map,
                        (key, value) -> !"BRAVO".equals(key) && value >= 0);
        assertEquals("alpha", selected.firstKey());
        assertEquals("charlie", selected.lastKey());
        assertEquals("charlie", selected.higherKey("alpha"));
        assertEquals("charlie", selected.descendingMap().firstKey());
        assertEquals(List.of("alpha", "charlie"),
                List.copyOf(selected.headMap("zz", true).navigableKeySet()));

        Map.Entry<String, Integer> writable =
                selected.entrySet().iterator().next();
        assertEquals(Integer.valueOf(2), writable.setValue(5));
        assertEquals(Integer.valueOf(5), map.get("ALPHA"));
        assertThrows(IllegalArgumentException.class, () -> writable.setValue(-10));

        NavigableMap<String, Integer> checked =
                Collections.checkedNavigableMap(
                        selected, String.class, Integer.class);
        NavigableMap<String, Integer> readOnly =
                Collections.unmodifiableNavigableMap(checked);
        assertEquals(Integer.valueOf(5), readOnly.get("alpha"));
        assertThrows(UnsupportedOperationException.class,
                () -> readOnly.put("delta", 6));
        checked.clear();
        assertTrue(map.containsKey("BRAVO"),
                "hidden mapping must survive visible clear");
        assertFalse(map.containsKey("alpha"));
    }

    @Test
    void boundedRandomNavigationAgreesWithIndependentJdkOracles() {
        Random random = new Random(0x53A9C017L);
        NavigableSet<Integer> elements = new TreeSet<>();
        NavigableMap<Integer, Integer> values = new TreeMap<>();
        NavigableSet<Integer> setView = M3CollectionViews.filtered(elements, EVEN);
        NavigableMap<Integer, Integer> mapView = M3FilteredMaps.filtered(values, MAP_FILTER);

        for (int iteration = 0; iteration < 192; iteration++) {
            int key = random.nextInt(64) - 32;
            int value = random.nextInt(9) - 4;
            if (random.nextBoolean()) {
                elements.add(key);
                values.put(key, value);
            } else {
                elements.remove(key);
                values.remove(key);
            }
            NavigableSet<Integer> expectedSet = new TreeSet<>();
            elements.stream().filter(EVEN).forEach(expectedSet::add);
            NavigableMap<Integer, Integer> expectedMap = new TreeMap<>();
            values.forEach((k, v) -> {
                if (MAP_FILTER.test(k, v)) expectedMap.put(k, v);
            });

            assertEquals(new ArrayList<>(expectedSet), new ArrayList<>(setView));
            assertEquals(new ArrayList<>(expectedMap.entrySet()),
                    new ArrayList<>(mapView.entrySet()));
            int probe = random.nextInt(72) - 36;
            assertEquals(expectedSet.lower(probe), setView.lower(probe));
            assertEquals(expectedSet.floor(probe), setView.floor(probe));
            assertEquals(expectedSet.ceiling(probe), setView.ceiling(probe));
            assertEquals(expectedSet.higher(probe), setView.higher(probe));
            assertEquals(expectedMap.lowerKey(probe), mapView.lowerKey(probe));
            assertEquals(expectedMap.floorKey(probe), mapView.floorKey(probe));
            assertEquals(expectedMap.ceilingKey(probe), mapView.ceilingKey(probe));
            assertEquals(expectedMap.higherKey(probe), mapView.higherKey(probe));
            assertEquals(expectedMap.get(probe), mapView.get(probe));
        }
    }

    @Test
    void packedOwnersAndSnapshotStaySeparateFromLiveSelection() {
        M3TreeSet<Integer> packedSet = new M3TreeSet<>();
        M3TreeMap<Integer, Integer> packedMap = new M3TreeMap<>();
        for (int index = 0; index < 12; index++) {
            packedSet.add(index);
            packedMap.put(index, index - 5);
        }
        NavigableSet<Integer> selectedSet =
                M3CollectionViews.filtered(packedSet, EVEN);
        NavigableMap<Integer, Integer> selectedMap =
                M3FilteredMaps.filtered(packedMap, MAP_FILTER);
        assertEquals(List.of(0, 2, 4, 6, 8, 10), List.copyOf(selectedSet));
        assertEquals(List.of(6, 8, 10), List.copyOf(selectedMap.navigableKeySet()));

        List<Integer> snapshot =
                M3CollectionViews.sortedSnapshot(
                        selectedSet, Comparator.reverseOrder());
        assertEquals(List.of(10, 8, 6, 4, 2, 0), snapshot);
        packedSet.add(14);
        assertEquals(List.of(10, 8, 6, 4, 2, 0), snapshot,
                "sortedSnapshot copies; filtered is backed");
        assertTrue(selectedSet.contains(14));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(20));

        SortedSet<Integer> sorted = M3CollectionViews.filtered(
                (SortedSet<Integer>) packedSet, EVEN);
        assertEquals(selectedSet.first(), sorted.first());
        assertEquals(selectedSet.last(), sorted.last());
    }
}
