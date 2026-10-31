/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Frozen defect witnesses and tests of evolved atoms retained during recipe convergence. */
public final class M3WrapperConvergenceSelfTest {
    private static int checks;
    private M3WrapperConvergenceSelfTest() { }

    private static void equal(Object expected, Object actual, String contract) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(contract + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void refuse(Runnable operation, String contract) {
        checks++;
        try { operation.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError(contract + ": mutation did not refuse");
    }

    static void interiorNullNavigation() {
        Comparator<Integer> middle = Comparator.comparingInt(v -> v == null ? 0 : v < 0 ? v : v + 1);
        TreeSet<Integer> owner = new TreeSet<>(middle);
        owner.addAll(Arrays.asList(-2, null, 2));
        NavigableSet<Integer> read = M3FilteredViews.filter(owner, Objects::nonNull);
        equal(-2, read.floor(0), "interior-null predecessor");
        equal(2, read.ceiling(-1), "interior-null successor");
        equal(-2, read.descendingSet().ceiling(0), "descending interior-null predecessor");
        equal(2, read.descendingSet().floor(-1), "descending interior-null successor");
        equal(Arrays.asList(-2, null, 2), new ArrayList<>(owner), "navigation preserves owner");
    }

    static void boundedNavigation() {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int value = 0; value < 10; value++) map.put(value, value);
        NavigableMap<Integer, Integer> selected = M3FilteredViews.filterEntries(
                map.subMap(2, true, 8, false), (key, value) -> (key & 1) == 0);
        equal(Map.entry(6, 6), selected.floorEntry(100), "bounded map upper query");
        equal(Map.entry(2, 2), selected.ceilingEntry(-100), "bounded map lower query");
        equal(null, selected.lowerEntry(-100), "bounded map absent predecessor");
        equal(null, selected.higherEntry(100), "bounded map absent successor");
        NavigableSet<Integer> editable = M3MutableFilteredViews.filter(
                new TreeSet<>(map.keySet()).subSet(2, true, 8, false), value -> (value & 1) == 0);
        equal(6, editable.lower(100), "bounded mutable set upper query");
        equal(2, editable.higher(-100), "bounded mutable set lower query");
        equal(6, editable.descendingSet().ceiling(100), "bounded descending mutable query");
        equal(10, map.size(), "navigation does not edit map");
    }

    static void strictSortedEndpoints() {
        SortedSet<Integer> set = M3FilteredViews.filter((SortedSet<Integer>) new TreeSet<Integer>(), x -> true);
        refuse(set::removeFirst, "empty sorted removeFirst");
        refuse(set::removeLast, "empty sorted removeLast");
        refuse(set.reversed()::removeFirst, "empty reversed sorted removeFirst");
        refuse(set.reversed()::removeLast, "empty reversed sorted removeLast");
        NavigableSet<Integer> facade = M3FilteredNavigableSet.view(new TreeSet<>(), x -> true);
        refuse(facade::removeFirst, "empty facade removeFirst");
        refuse(facade::removeLast, "empty facade removeLast");
        int[] calls = {0};
        TreeMap<Integer, Integer> owner = new TreeMap<>();
        SortedMap<Integer, Integer> map = M3FilteredViews.filterEntries((SortedMap<Integer, Integer>) owner,
                (key, value) -> { calls[0]++; return true; });
        refuse(map::pollFirstEntry, "empty sorted map pollFirstEntry");
        refuse(map::pollLastEntry, "empty sorted map pollLastEntry");
        refuse(map.reversed()::pollFirstEntry, "empty reversed map pollFirstEntry");
        owner.put(1, 1);
        refuse(map::pollFirstEntry, "populated sorted map pollFirstEntry");
        refuse(map::pollLastEntry, "populated sorted map pollLastEntry");
        equal(0, calls[0], "read-only endpoint refusal precedes predicates");
        equal(Map.of(1, 1), owner, "read-only endpoint owner unchanged");
    }

    static void retainedCurrentAtoms() {
        int[] traversals = {0};
        Collection<Integer> changingInput = new AbstractCollection<>() {
            @Override public int size() { return 2; }
            @Override public Iterator<Integer> iterator() {
                return (++traversals[0] == 1 ? List.of(2, 4) : List.of(3, 5)).iterator();
            }
        };
        TreeSet<Integer> setOwner = new TreeSet<>();
        M3MutableFilteredViews.filter(setOwner, value -> (value & 1) == 0).addAll(changingInput);
        equal(1, traversals[0], "set bulk input traversed exactly once");
        equal(Set.of(2, 4), setOwner, "set writes only admitted references");

        int[] entryReads = {0};
        int[] valueReads = {0};
        Map<Integer, Integer> changingMap = new AbstractMap<>() {
            @Override public Set<Entry<Integer, Integer>> entrySet() {
                entryReads[0]++;
                return Set.of(new Entry<>() {
                    @Override public Integer getKey() { return 1; }
                    @Override public Integer getValue() { return ++valueReads[0] == 1 ? 2 : 3; }
                    @Override public Integer setValue(Integer value) { throw new UnsupportedOperationException(); }
                });
            }
        };
        TreeMap<Integer, Integer> mapOwner = new TreeMap<>();
        M3MutableFilteredViews.filterEntries(mapOwner, (key, value) -> (value & 1) == 0).putAll(changingMap);
        equal(1, entryReads[0], "map bulk entries traversed exactly once");
        equal(1, valueReads[0], "input entry value captured exactly once");
        equal(Map.of(1, 2), mapOwner, "map writes only admitted value");

        ArrayList<Integer> listOwner = new ArrayList<>(List.of(1, 2));
        List<Integer> view = M3FilteredLists.filtered(listOwner, value -> (value & 1) == 0);
        var stream = view.stream();
        listOwner.add(4);
        equal(List.of(2, 4), stream.toList(), "List stream binds at first traversal");
        List<Integer> readOnly = listOwner;
        for (int depth = 0; depth < 20_000; depth++) {
            readOnly = M3FilteredLists.readOnlyFiltered(readOnly, value -> value > 0);
        }
        equal(3, readOnly.size(), "historical read-only List fusion remains stack-safe");
        refuse(() -> M3FilteredLists.filtered(
                M3FilteredLists.readOnlyFiltered(listOwner, x -> true), x -> true).add(6),
                "writable filter does not bypass protected List owner");
        equal(List.of(1, 2, 4), listOwner, "protected owner remains unchanged");
    }

    public static void main(String[] args) {
        String selected = args.length == 0 ? "all" : args[0];
        switch (selected) {
            case "null" -> interiorNullNavigation();
            case "bounds" -> boundedNavigation();
            case "readonly" -> strictSortedEndpoints();
            case "preserved" -> retainedCurrentAtoms();
            case "all" -> {
                interiorNullNavigation();
                boundedNavigation();
                strictSortedEndpoints();
                retainedCurrentAtoms();
            }
            default -> throw new IllegalArgumentException("unknown case " + selected);
        }
        System.out.println("PASS M3WrapperConvergenceSelfTest case=" + selected + " checks=" + checks);
    }
}
