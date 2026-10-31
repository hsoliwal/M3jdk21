/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/** Same-failure counterexamples: callbacks and opaque owner failures are never range signals. */
public final class M3NavigationFailureContract {
    private static int checks;
    private M3NavigationFailureContract() { }

    private static final class CheckedOrder implements Comparator<Integer> {
        private final IllegalArgumentException failure = new IllegalArgumentException("rejected query");
        private final List<String> calls = new ArrayList<>();

        @Override public int compare(Integer left, Integer right) {
            calls.add(left + ":" + right);
            if (Objects.equals(left, 99) || Objects.equals(right, 99)) throw failure;
            return Integer.compare(left, right);
        }
    }

    /** A comparator with a deterministic rejected input must fail once, using its original exception. */
    static void comparatorFailureIsNotRetried() {
        CheckedOrder order = new CheckedOrder();
        TreeSet<Integer> set = new TreeSet<>(order);
        set.addAll(List.of(1, 2, 3));
        TreeMap<Integer, Integer> map = new TreeMap<>(order);
        map.putAll(Map.of(1, 1, 2, 2, 3, 3));
        NavigableSet<Integer> readSet = M3FilteredViews.filter(set, value -> true);
        NavigableSet<Integer> mutableSet = M3MutableFilteredViews.filter(set, value -> true);
        NavigableMap<Integer, Integer> readMap = M3FilteredViews.filterEntries(map, (key, value) -> true);
        List<NavigableSet<Integer>> directSets = List.of(readSet,
                M3FilteredViews.filter(readSet, value -> true),
                M3FilteredNavigableSet.view(readSet, value -> true));
        List<NavigableSet<Integer>> rangeSets = List.of(mutableSet,
                M3FilteredViews.filter(mutableSet, value -> true),
                M3FilteredNavigableSet.view(M3FilteredViews.filter(mutableSet, value -> true),
                        value -> true));
        List<NavigableMap<Integer, Integer>> rangeMaps = List.of(readMap,
                M3FilteredViews.filterEntries(readMap, (key, value) -> true),
                M3FilteredViews.filterEntries(M3MutableFilteredViews.filterEntries(map,
                        (key, value) -> true), (key, value) -> true));
        for (int direction = 0; direction < 4; direction++) {
            final int operation = direction;
            for (NavigableSet<Integer> view : directSets) {
                order.calls.clear();
                sameFailure(order.failure, () -> neighbor(view, operation, 99));
                equal(List.of("99:2"), order.calls, "read-only set retains direct owner failure demand");
            }
            for (NavigableSet<Integer> view : rangeSets) {
                order.calls.clear();
                sameFailure(order.failure, () -> neighbor(view, operation, 99));
                equal(List.of("99:99"), order.calls, "mutable query admission fails exactly once");
            }
            for (NavigableMap<Integer, Integer> view : rangeMaps) {
                order.calls.clear();
                sameFailure(order.failure, () -> neighbor(view, operation, 99));
                equal(List.of("99:99"), order.calls, "map query admission fails exactly once");
            }
        }
    }

    private static final class OpaqueSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final IllegalArgumentException failure = new IllegalArgumentException("opaque set range");
        private final ArrayList<String> calls = new ArrayList<>();
        private OpaqueSet() { super(List.of(1, 2, 3)); }
        @Override public NavigableSet<Integer> headSet(Integer key, boolean inclusive) {
            calls.add("head:" + key + ":" + inclusive);
            if (key == 2) throw failure;
            return super.headSet(key, inclusive);
        }
        @Override public NavigableSet<Integer> tailSet(Integer key, boolean inclusive) {
            calls.add("tail:" + key + ":" + inclusive);
            if (key == 2) throw failure;
            return super.tailSet(key, inclusive);
        }
        @Override public Integer lower(Integer key) { calls.add("lower:" + key); return super.lower(key); }
        @Override public Integer floor(Integer key) { calls.add("floor:" + key); return super.floor(key); }
        @Override public Integer ceiling(Integer key) { calls.add("ceiling:" + key); return super.ceiling(key); }
        @Override public Integer higher(Integer key) { calls.add("higher:" + key); return super.higher(key); }
    }

    private static final class OpaqueMap extends TreeMap<Integer, Integer> {
        private static final long serialVersionUID = 1L;
        private final IllegalArgumentException failure = new IllegalArgumentException("opaque map range");
        private final ArrayList<String> calls = new ArrayList<>();
        private OpaqueMap() { putAll(Map.of(1, 1, 2, 2, 3, 3)); }
        @Override public NavigableMap<Integer, Integer> headMap(Integer key, boolean inclusive) {
            calls.add("head:" + key + ":" + inclusive);
            if (key == 2) throw failure;
            return super.headMap(key, inclusive);
        }
        @Override public NavigableMap<Integer, Integer> tailMap(Integer key, boolean inclusive) {
            calls.add("tail:" + key + ":" + inclusive);
            if (key == 2) throw failure;
            return super.tailMap(key, inclusive);
        }
        @Override public Map.Entry<Integer, Integer> lowerEntry(Integer key) {
            calls.add("lower:" + key); return super.lowerEntry(key);
        }
        @Override public Map.Entry<Integer, Integer> floorEntry(Integer key) {
            calls.add("floor:" + key); return super.floorEntry(key);
        }
        @Override public Map.Entry<Integer, Integer> ceilingEntry(Integer key) {
            calls.add("ceiling:" + key); return super.ceilingEntry(key);
        }
        @Override public Map.Entry<Integer, Integer> higherEntry(Integer key) {
            calls.add("higher:" + key); return super.higherEntry(key);
        }
    }

    /** Custom owners remain opaque even when they inherit a supported JDK collection class. */
    static void opaqueOwnerFailureIsPreserved() {
        OpaqueSet set = new OpaqueSet();
        OpaqueMap map = new OpaqueMap();
        NavigableSet<Integer> mutable = M3MutableFilteredViews.filter(set, value -> true);
        NavigableMap<Integer, Integer> readMap = M3FilteredViews.filterEntries(map, (key, value) -> true);
        List<NavigableSet<Integer>> sets = List.of(mutable,
                M3MutableFilteredViews.filter(mutable, value -> true));
        List<NavigableMap<Integer, Integer>> maps = List.of(readMap,
                M3FilteredViews.filterEntries(readMap, (key, value) -> true));
        for (int direction = 0; direction < 4; direction++) {
            final int operation = direction;
            String expected = (direction < 2 ? "head:" : "tail:") + "2:"
                    + (direction == 1 || direction == 2);
            for (NavigableSet<Integer> view : sets) {
                set.calls.clear();
                sameFailure(set.failure, () -> neighbor(view, operation, 2));
                equal(List.of(expected), set.calls, "opaque set range failure is not retried");
            }
            for (NavigableMap<Integer, Integer> view : maps) {
                map.calls.clear();
                sameFailure(map.failure, () -> neighbor(view, operation, 2));
                equal(List.of(expected), map.calls, "opaque map range failure is not retried");
            }
        }
        NavigableSet<Integer> read = M3FilteredViews.filter(set, value -> true);
        for (NavigableSet<Integer> view : List.of(read, M3FilteredViews.filter(read, value -> true),
                M3FilteredNavigableSet.view(read, value -> true))) {
            set.calls.clear();
            equal(1, view.lower(2), "existing read-only set direct navigation remains distinct");
            equal(List.of("lower:2"), set.calls, "accepted direct neighbor creates no range");
        }
    }

    /** Read-only sets inherit actual empty-owner navigation; range-based owners still validate queries. */
    static void emptyOwnerAdmissionIsPreserved() {
        TreeSet<Integer> empty = new TreeSet<>();
        NavigableSet<Integer> read = M3FilteredViews.filter(empty, value -> true);
        for (int direction = 0; direction < 4; direction++) {
            for (NavigableSet<Integer> view : List.of(read, M3FilteredViews.filter(read, value -> true),
                    M3FilteredNavigableSet.view(read, value -> true))) {
                equal(neighbor(empty, direction, null), neighbor(view, direction, null),
                        "empty read-only navigation follows existing JDK owner");
            }
        }
        CheckedOrder order = new CheckedOrder();
        NavigableSet<Integer> guarded = M3FilteredViews.filter(new TreeSet<>(order), value -> true);
        for (NavigableSet<Integer> view : List.of(guarded, M3FilteredViews.filter(guarded, value -> true),
                M3FilteredNavigableSet.view(guarded, value -> true))) {
            order.calls.clear();
            equal(null, view.lower(99), "empty direct owner does not compare rejected query");
            equal(List.of(), order.calls, "no new comparator call on empty read-only set");
        }
        NavigableSet<Integer> mutable = M3MutableFilteredViews.filter(new TreeSet<>(order), value -> true);
        NavigableMap<Integer, Integer> map = M3FilteredViews.filterEntries(new TreeMap<>(order), (k, v) -> true);
        for (NavigableSet<Integer> view : List.of(mutable, M3FilteredViews.filter(mutable, value -> true),
                M3FilteredNavigableSet.view(mutable, value -> true))) {
            order.calls.clear();
            sameFailure(order.failure, () -> view.lower(99));
            equal(List.of("99:99"), order.calls, "empty mutable range admission retained");
        }
        order.calls.clear();
        sameFailure(order.failure, () -> map.lowerEntry(99));
        equal(List.of("99:99"), order.calls, "empty map range admission retained");
    }

    /** The existing parent-owned array/offset AVL owners use the same bounded anchor contract. */
    static void m3ArrayTreeOwnersRemainSupported() {
        M3TreeSet<Integer> set = new M3TreeSet<>();
        set.addAll(List.of(1, 2, 3, 4, 5));
        M3TreeMap<Integer, Integer> map = new M3TreeMap<>();
        map.putAll(Map.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5));
        NavigableSet<Integer> view = M3MutableFilteredViews.filter(set.subSet(2, true, 4, true),
                value -> (value & 1) == 0);
        NavigableMap<Integer, Integer> mapping = M3FilteredViews.filterEntries(map.subMap(2, true, 4, true),
                (key, value) -> (key & 1) == 0);
        equal(2, view.ceiling(-100), "M3 mutable array tree lower boundary");
        equal(4, view.floor(100), "M3 mutable array tree upper boundary");
        equal(2, mapping.ceilingKey(-100), "M3 array map lower boundary");
        equal(4, mapping.floorKey(100), "M3 array map upper boundary");
    }

    private static Integer neighbor(NavigableSet<Integer> source, int direction, Integer key) {
        return switch (direction) {
            case 0 -> source.lower(key);
            case 1 -> source.floor(key);
            case 2 -> source.ceiling(key);
            case 3 -> source.higher(key);
            default -> throw new AssertionError(direction);
        };
    }

    private static Map.Entry<Integer, Integer> neighbor(NavigableMap<Integer, Integer> source,
            int direction, Integer key) {
        return switch (direction) {
            case 0 -> source.lowerEntry(key);
            case 1 -> source.floorEntry(key);
            case 2 -> source.ceilingEntry(key);
            case 3 -> source.higherEntry(key);
            default -> throw new AssertionError(direction);
        };
    }

    private static void sameFailure(Throwable expected, Supplier<?> operation) {
        checks++;
        try { operation.get(); }
        catch (Throwable actual) {
            if (actual == expected) return;
            throw new AssertionError("failure identity changed", actual);
        }
        throw new AssertionError("owner failure was swallowed");
    }

    private static void equal(Object expected, Object actual, String contract) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(contract + ": expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        comparatorFailureIsNotRetried();
        opaqueOwnerFailureIsPreserved();
        emptyOwnerAdmissionIsPreserved();
        m3ArrayTreeOwnersRemainSupported();
        System.out.println("PASS M3NavigationFailureContract checks=" + checks);
    }
}
