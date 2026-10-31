/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Dependency-free Java 21 contract gate for the pinned filtered predicate plan. */
public final class M3FilteredSortedIteratorPlanSelfTest {
    private static long checks;

    private M3FilteredSortedIteratorPlanSelfTest() { }

    public static void main(String[] args) {
        deepLiveFiltering();
        callbackTraceAndExceptions();
        sortedContracts();
        mapContracts();
        System.out.println("M3FilteredSortedIteratorPlanSelfTest checks=" + checks);
    }

    private static void deepLiveFiltering() {
        List<Integer> owner = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collection<Integer> view = owner;
        for (int layer = 0; layer < 1_200; layer++) {
            view = M3FilteredViews.filter(view, value -> value >= 0);
        }
        view = M3FilteredViews.filter(view, value -> (value & 1) == 0);
        eq(List.of(0, 2, 4), consume(view));
        owner.add(6);
        eq(List.of(0, 2, 4, 6), consume(view));
        owner.remove(Integer.valueOf(2));
        eq(List.of(0, 4, 6), consume(view));
        eq(3, view.size());
    }

    private static void callbackTraceAndExceptions() {
        List<String> calls = new ArrayList<>();
        Collection<Integer> inner = M3FilteredViews.filter(
                List.of(1, 2, 3), value -> {
                    calls.add("inner:" + value);
                    return value != 2;
                });
        Collection<Integer> outer = M3FilteredViews.filter(inner, value -> {
            calls.add("outer:" + value);
            return true;
        });
        List<String> expected = List.of("inner:1", "outer:1", "inner:2",
                "inner:3", "outer:3");
        for (int iteration = 0; iteration < 2; iteration++) {
            calls.clear();
            eq(List.of(1, 3), consume(outer));
            eq(expected, calls);
        }

        RuntimeException sentinel = new RuntimeException("predicate sentinel");
        Collection<Integer> throwing = M3FilteredViews.filter(outer, value -> {
            throw sentinel;
        });
        try {
            throwing.iterator().hasNext();
            throw new AssertionError("Expected predicate exception");
        } catch (RuntimeException thrown) {
            same(sentinel, thrown);
        }
    }

    private static void sortedContracts() {
        TreeSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> even = M3FilteredViews.filter(
                (NavigableSet<Integer>) backing, n -> (n & 1) == 0);
        eq(List.of(2, 4, 6), consume(even));
        eq(List.of(6, 4, 2), consume(even.descendingSet()));
        eq(4, even.floor(5));
        eq(6, even.ceiling(5));
        eq(2, even.first());
        eq(6, even.last());
        unsupported(even::pollFirst);
        unsupported(() -> even.remove(2));
        backing.add(8);
        eq(List.of(2, 4, 6, 8), consume(even));

        Comparator<Integer> reverse = Comparator.reverseOrder();
        SortedSet<Integer> reverseOwner = new TreeSet<>(reverse);
        reverseOwner.addAll(List.of(1, 2, 3, 4, 5));
        SortedSet<Integer> odds = M3FilteredViews.filter(
                reverseOwner, n -> (n & 1) == 1);
        eq(List.of(5, 3, 1), consume(odds));
        Spliterator<Integer> split = odds.spliterator();
        truth(split.hasCharacteristics(Spliterator.SORTED));
        eq(reverse, split.getComparator());
        unsupported(() -> odds.add(7));
    }

    private static void mapContracts() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        for (int i = 0; i < 8; i++) backing.put(i, "v" + i);
        NavigableMap<Integer, String> even = M3FilteredViews.filterEntries(
                backing, (key, value) -> (key & 1) == 0);
        eq(List.of(0, 2, 4, 6), consume(even.navigableKeySet()));
        eq(2, even.floorKey(3));
        eq(4, even.ceilingKey(3));
        var first = even.firstEntry();
        eq(0, first.getKey());
        unsupported(() -> first.setValue("no"));
        unsupported(even::pollFirstEntry);
        backing.remove(0);
        backing.put(8, "v8");
        eq(List.of(2, 4, 6, 8), consume(even.navigableKeySet()));
    }

    /** Do not use new ArrayList<>(view), which probes size() before iterator(). */
    private static <E> List<E> consume(Collection<E> collection) {
        ArrayList<E> result = new ArrayList<>();
        Iterator<E> cursor = collection.iterator();
        while (cursor.hasNext()) {
            result.add(cursor.next());
        }
        return result;
    }

    private static void truth(boolean condition) {
        if (!condition) throw new AssertionError("Expected true");
        checks++;
    }

    private static void same(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError("Wrong throwable identity");
        checks++;
    }

    private static void eq(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
        checks++;
    }

    private static void unsupported(Runnable call) {
        try {
            call.run();
        } catch (UnsupportedOperationException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Mutation did not refuse");
    }
}
