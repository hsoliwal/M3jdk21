/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Adversarial bounded-navigation differential oracle on actual JDK owners.
 * It deliberately exercises queries outside pre-existing views, not merely
 * valid sub-range constructors. No JNI callback or extra M3 index owner.
 */
public final class M3BoundedFilteredNavigationContract {
    private static long checks;

    private M3BoundedFilteredNavigationContract() { }

    private static void equal(Object expected, Object observed, String label) {
        checks++;
        if (!Objects.equals(expected, observed)) {
            throw new AssertionError(label + " expected=" + expected + " observed=" + observed);
        }
    }

    private static Predicate<Integer> visible() {
        return n -> n == null || Math.floorMod(n, 5) == 2;
    }

    private static void checkSet(NavigableSet<Integer> source, boolean nullable) {
        Predicate<Integer> predicate = visible();
        NavigableSet<Integer> readonly = M3FilteredViews.filter(source, predicate);
        NavigableSet<Integer> mutable = M3MutableFilteredViews.filter(source, predicate);
        NavigableSet<Integer> expected = new TreeSet<>(source.comparator());
        for (Integer value : source) {
            if (predicate.test(value)) expected.add(value);
        }
        equal(new ArrayList<>(expected), new ArrayList<>(readonly), "readonly projection");
        equal(new ArrayList<>(expected), new ArrayList<>(mutable), "mutable projection");
        equal(expected.comparator(), readonly.comparator(), "readonly comparator");
        equal(expected.comparator(), mutable.comparator(), "mutable comparator");
        List<Integer> probes = new ArrayList<>(List.of(
                -100, -40, -19, -8, -7, -6, -5, -2, 0, 2, 3, 7, 8, 9, 35, 100));
        if (nullable) probes.add(null);
        for (Integer probe : probes) {
            equal(expected.lower(probe), readonly.lower(probe), "read-only lower " + probe);
            equal(expected.floor(probe), readonly.floor(probe), "read-only floor " + probe);
            equal(expected.ceiling(probe), readonly.ceiling(probe), "read-only ceiling " + probe);
            equal(expected.higher(probe), readonly.higher(probe), "read-only higher " + probe);
            equal(expected.lower(probe), mutable.lower(probe), "mutable lower " + probe);
            equal(expected.floor(probe), mutable.floor(probe), "mutable floor " + probe);
            equal(expected.ceiling(probe), mutable.ceiling(probe), "mutable ceiling " + probe);
            equal(expected.higher(probe), mutable.higher(probe), "mutable higher " + probe);
            equal(expected.contains(probe), readonly.contains(probe), "readonly contains " + probe);
            equal(expected.contains(probe), mutable.contains(probe), "mutable contains " + probe);
        }
        if (!expected.isEmpty()) {
            equal(expected.first(), readonly.first(), "read-only first");
            equal(expected.last(), readonly.last(), "read-only last");
            equal(expected.first(), mutable.first(), "mutable first");
            equal(expected.last(), mutable.last(), "mutable last");
        }
        equal(new ArrayList<>(expected.descendingSet()),
                new ArrayList<>(readonly.descendingSet()), "readonly descending");
        equal(new ArrayList<>(expected.descendingSet()),
                new ArrayList<>(mutable.descendingSet()), "mutable descending");
        boolean refused = false;
        try {
            readonly.pollFirst();
        } catch (UnsupportedOperationException ex) {
            refused = true;
        }
        equal(true, refused, "read-only mutation refusal");
    }

    private static void checkMap(NavigableMap<Integer, String> source, boolean nullable) {
        NavigableMap<Integer, String> readonly = M3FilteredViews.filterEntries(source,
                (key, value) -> key == null || Math.floorMod(key, 4) == 0);
        NavigableMap<Integer, String> expected = new TreeMap<>(source.comparator());
        source.forEach((key, value) -> {
            if (key == null || Math.floorMod(key, 4) == 0) expected.put(key, value);
        });
        equal(new ArrayList<>(expected.entrySet()),
                new ArrayList<>(readonly.entrySet()), "map projected entries");
        equal(new ArrayList<>(expected.descendingMap().entrySet()),
                new ArrayList<>(readonly.descendingMap().entrySet()), "map descending");
        List<Integer> probes = new ArrayList<>(List.of(
                -100, -40, -19, -8, -7, -6, -5, -2, 0, 2, 3, 7, 8, 9, 35, 100));
        if (nullable) probes.add(null);
        for (Integer key : probes) {
            equal(expected.lowerEntry(key), readonly.lowerEntry(key), "lowerEntry " + key);
            equal(expected.floorEntry(key), readonly.floorEntry(key), "floorEntry " + key);
            equal(expected.ceilingEntry(key), readonly.ceilingEntry(key), "ceilingEntry " + key);
            equal(expected.higherEntry(key), readonly.higherEntry(key), "higherEntry " + key);
            equal(expected.lowerKey(key), readonly.lowerKey(key), "lowerKey " + key);
            equal(expected.floorKey(key), readonly.floorKey(key), "floorKey " + key);
            equal(expected.ceilingKey(key), readonly.ceilingKey(key), "ceilingKey " + key);
            equal(expected.higherKey(key), readonly.higherKey(key), "higherKey " + key);
        }
        boolean refused = false;
        try {
            readonly.clear();
        } catch (UnsupportedOperationException ex) {
            refused = true;
        }
        equal(true, refused, "readonly map clear refusal");
    }

    private static NavigableSet<Integer> bounded(NavigableSet<Integer> root,
            Comparator<Integer> comparator, int lower, int upper) {
        int from = comparator.compare(lower, upper) <= 0 ? lower : upper;
        int to = comparator.compare(lower, upper) <= 0 ? upper : lower;
        return root.subSet(from, true, to, false);
    }

    private static NavigableMap<Integer, String> bounded(NavigableMap<Integer, String> root,
            Comparator<Integer> comparator, int lower, int upper) {
        int from = comparator.compare(lower, upper) <= 0 ? lower : upper;
        int to = comparator.compare(lower, upper) <= 0 ? upper : lower;
        return root.subMap(from, true, to, false);
    }

    public static void run() {
        List<Comparator<Integer>> comparators = List.of(
                Comparator.naturalOrder(),
                Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.naturalOrder()),
                Comparator.nullsFirst(Comparator.reverseOrder()),
                Comparator.nullsLast(Comparator.reverseOrder()));
        Random random = new Random(0x33B0A7DEDL);
        for (int c = 0; c < comparators.size(); c++) {
            Comparator<Integer> comparator = comparators.get(c);
            boolean nullable = c >= 2;
            for (int trial = 0; trial < 42; trial++) {
                NavigableSet<Integer> setRoot = new TreeSet<>(comparator);
                NavigableMap<Integer, String> mapRoot = new TreeMap<>(comparator);
                for (int i = -24; i <= 24; i++) {
                    if (random.nextInt(6) != 0) {
                        setRoot.add(i);
                        mapRoot.put(i, i % 9 == 0 ? null : "v" + i);
                    }
                }
                if (nullable && trial % 2 == 0) {
                    setRoot.add(null);
                    mapRoot.put(null, null);
                }
                boolean isBounded = (trial % 3) != 0;
                NavigableSet<Integer> set = isBounded
                        ? bounded(setRoot, comparator, -7, 9) : setRoot;
                NavigableMap<Integer, String> map = isBounded
                        ? bounded(mapRoot, comparator, -7, 9) : mapRoot;
                if (trial % 2 != 0) {
                    set = set.descendingSet();
                    map = map.descendingMap();
                }
                checkSet(set, nullable);
                checkMap(map, nullable);
                if (trial % 7 == 0) {
                    setRoot.add(22);
                    mapRoot.put(22, "live");
                    checkSet(set, nullable);
                    checkMap(map, nullable);
                }
            }
        }
        // A comparator-equivalent alias must not replace the source representative.
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        NavigableSet<String> root = new TreeSet<>(byLength);
        root.add("aa");
        root.add("cccc");
        NavigableSet<String> filtered = M3FilteredViews.filter(root,
                s -> s.startsWith("a"));
        equal(true, filtered.contains("zz"), "source representative predicate");
        equal("aa", filtered.ceiling("xy"), "source representative returned");
        // No user callback IllegalArgumentException may be swallowed by range fallback.
        NavigableSet<Integer> sorted = new TreeSet<>(List.of(2, 3, 4));
        NavigableSet<Integer> raising = M3FilteredViews.filter(sorted,
                n -> { if (n == 3) throw new IllegalArgumentException("from predicate");
                    return true; });
        boolean thrown = false;
        try {
            raising.ceiling(3);
        } catch (IllegalArgumentException ex) {
            thrown = "from predicate".equals(ex.getMessage());
        }
        equal(true, thrown, "predicate exception intact");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASS M3_BOUNDED_FILTERED_NAVIGATION checks=" + checks);
    }
}
