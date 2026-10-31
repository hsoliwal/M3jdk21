/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Standalone deterministic behavioral oracle executable with only Java 21. */
public final class M3FilteredViewsContract {
    private M3FilteredViewsContract() { }
    private static int checks;

    private static void equal(Object expected, Object actual, String description) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void require(boolean yes, String description) {
        checks++;
        if (!yes) throw new AssertionError(description);
    }
    private static void refuses(Class<? extends Throwable> type, Runnable action, String label) {
        checks++;
        try { action.run(); }
        catch (Throwable exception) {
            if (type.isInstance(exception)) return;
            throw new AssertionError(label + ": unexpected " + exception, exception);
        }
        throw new AssertionError(label + ": did not refuse");
    }
    private static <T> List<T> values(Iterable<T> source) {
        ArrayList<T> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    static void setAndCollectionContracts() {
        ArrayList<Integer> source = new ArrayList<>(Arrays.asList(7, 8, 9, 9, 10, 11));
        Collection<Integer> view = M3FilteredViews.filter(source, v -> (v & 1) == 1);
        equal(List.of(7, 9, 9, 11), values(view), "collection order and multiplicity");
        equal(4, view.size(), "filtered count");
        require(view.contains(9) && !view.contains(8), "contains visible only");
        source.add(13);
        equal(List.of(7, 9, 9, 11, 13), values(view), "live backing");
        refuses(UnsupportedOperationException.class, () -> view.add(99), "read-only add");
        refuses(UnsupportedOperationException.class, () -> view.remove(9), "read-only remove");
        refuses(UnsupportedOperationException.class, view::clear, "read-only clear");
        refuses(UnsupportedOperationException.class, () -> view.removeIf(x -> true), "read-only removeIf");
        Iterator<Integer> it = view.iterator();
        require(it.hasNext() && it.hasNext(), "idempotent hasNext");
        equal(7, it.next(), "next after prefetch");
        refuses(UnsupportedOperationException.class, it::remove, "iterator remove");
        Set<Integer> set = M3FilteredViews.filter(new TreeSet<>(source), x -> x % 2 == 0);
        equal(Set.of(8, 10), set, "AbstractSet equality");
        equal(Set.of(8, 10).hashCode(), set.hashCode(), "AbstractSet hash");
        refuses(UnsupportedOperationException.class, () -> set.add(12), "set add blocked");
    }

    static void navigableSetContracts() {
        NavigableSet<Integer> source = new TreeSet<>();
        for (int i = -20; i <= 20; i++) source.add(i);
        Predicate<Integer> predicate = i -> (i & 3) == 1;
        NavigableSet<Integer> filtered = M3FilteredViews.filter(source, predicate);
        checkSetView(source, filtered, predicate);
        equal(1, filtered.ceiling(0), "ceiling skips invisible");
        equal(-3, filtered.floor(-2), "floor skips invisible");
        equal(1, filtered.higher(-3), "higher skips invisible");
        equal(-3, filtered.lower(1), "lower skips invisible");
        require(filtered.descendingSet().descendingSet().equals(filtered), "double descending");
        equal(null, filtered.higher(21), "higher exhaustion");
        equal(null, filtered.lower(-20), "lower exhaustion");
        refuses(UnsupportedOperationException.class, filtered::pollFirst, "pollFirst blocked");
        refuses(UnsupportedOperationException.class, filtered::pollLast, "pollLast blocked");
        refuses(NoSuchElementException.class,
                () -> M3FilteredViews.filter(new TreeSet<Integer>(), predicate).first(), "empty first");
        source.add(25);
        require(filtered.contains(25), "visible new backing value");
        equal(25, filtered.last(), "new last");
        SortedSet<Integer> baseSorted = Collections.unmodifiableSortedSet(new TreeSet<>(source));
        SortedSet<Integer> sorted = M3FilteredViews.filter(baseSorted, predicate);
        equal(values(filtered), values(sorted), "plain sorted comparator order");
        equal(values(filtered.subSet(-8, true, 12, false)),
                values(sorted.subSet(-8, 12)), "plain sorted subset");
        refuses(UnsupportedOperationException.class, () -> sorted.add(29), "plain sorted add");
    }

    static void navigableMapContracts() {
        TreeMap<Integer, String> source = new TreeMap<>();
        for (int i = -30; i <= 30; i++) source.put(i, i % 7 == 0 ? null : "v" + i);
        NavigableMap<Integer, String> filtered = M3FilteredViews.filterEntries(source,
                (key, value) -> key % 3 == 0 && (value == null || !value.endsWith("6")));
        checkMapView(source, filtered);
        equal(null, filtered.get(-29), "hidden key get");
        require(!filtered.containsKey(-29), "hidden key containment");
        require(filtered.containsKey(0) && filtered.get(0) == null, "visible null value");
        require(filtered.entrySet().stream().allMatch(e -> source.containsKey(e.getKey())),
                "entries backed by source");
        var entry = filtered.firstEntry();
        refuses(UnsupportedOperationException.class, () -> entry.setValue("modified"),
                "entry is read only");
        refuses(UnsupportedOperationException.class, () -> filtered.put(0, "changed"),
                "map put blocked");
        refuses(UnsupportedOperationException.class, () -> filtered.remove(0), "map remove blocked");
        refuses(UnsupportedOperationException.class, filtered::pollFirstEntry, "map poll blocked");
        equal(null, filtered.lowerKey(-100), "map absent lower");
        source.put(33, "v33");
        equal(33, filtered.lastKey(), "live map new last");
        SortedMap<Integer, String> sorted = M3FilteredViews.filterEntries(
                Collections.unmodifiableSortedMap(source), (k, v) -> k % 3 == 0
                        && (v == null || !v.endsWith("6")));
        equal(filtered, sorted, "plain SortedMap filtered parity");
        equal(filtered.hashCode(), sorted.hashCode(), "plain SortedMap hash");
        equal(filtered.firstKey(), sorted.firstKey(), "plain sorted first");
        equal(filtered.lastKey(), sorted.lastKey(), "plain sorted last");
    }

    static void randomizedContracts() {
        Random random = new Random(0xDEFE_2026_1016L);
        for (int trial = 0; trial < 750; trial++) {
            Comparator<Integer> comparator = random.nextBoolean()
                    ? Comparator.naturalOrder() : Comparator.reverseOrder();
            NavigableSet<Integer> set = new TreeSet<>(comparator);
            NavigableMap<Integer, Integer> map = new TreeMap<>(comparator);
            int divisor = 1 + random.nextInt(8);
            int residue = random.nextInt(divisor);
            Predicate<Integer> selected = x -> Math.floorMod(x, divisor) == residue;
            for (int i = 0; i < random.nextInt(96); i++) {
                int value = random.nextInt(120) - 60;
                set.add(value);
                map.put(value, value % 11 == 0 ? null : value * 7);
            }
            NavigableSet<Integer> view = M3FilteredViews.filter(set, selected);
            NavigableMap<Integer, Integer> mapView = M3FilteredViews.filterEntries(map,
                    (key, value) -> selected.test(key) && (value == null || value % 5 != 0));
            checkSetView(set, view, selected);
            checkMapView(map, mapView);
            if (trial % 10 == 0) {
                int value = 60 + trial;
                set.add(value);
                map.put(value, value);
                checkSetView(set, view, selected);
                checkMapView(map, mapView);
            }
        }
    }

    private static void checkSetView(NavigableSet<Integer> base,
            NavigableSet<Integer> view, Predicate<Integer> selected) {
        TreeSet<Integer> expected = new TreeSet<>(base.comparator());
        for (Integer value : base) if (selected.test(value)) expected.add(value);
        equal(values(expected), values(view), "set projection");
        equal(values(expected.descendingSet()), values(view.descendingSet()), "reverse set");
        equal(expected.comparator(), view.comparator(), "set comparator");
        equal(expected.size(), view.size(), "set size");
        equal(expected.isEmpty(), view.isEmpty(), "set emptiness");
        for (int probe = -65; probe <= 65; probe += 13) {
            equal(expected.floor(probe), view.floor(probe), "set floor");
            equal(expected.ceiling(probe), view.ceiling(probe), "set ceiling");
            equal(expected.higher(probe), view.higher(probe), "set higher");
            equal(expected.lower(probe), view.lower(probe), "set lower");
            equal(expected.headSet(probe, true), view.headSet(probe, true), "headSet view");
            equal(expected.tailSet(probe, false), view.tailSet(probe, false), "tailSet view");
        }
        if (!expected.isEmpty()) {
            equal(expected.first(), view.first(), "set first");
            equal(expected.last(), view.last(), "set last");
            Integer from = base.first();
            Integer to = base.last();
            equal(expected.subSet(from, true, to, true), view.subSet(from, true, to, true),
                    "set inclusive range");
        }
    }

    private static void checkMapView(NavigableMap<Integer, ?> base,
            NavigableMap<Integer, ?> view) {
        TreeMap<Integer, Object> expected = new TreeMap<>(base.comparator());
        base.forEach((key, value) -> {
            if (view.containsKey(key)) expected.put(key, value);
        });
        equal(expected, view, "map projection");
        equal(values(expected.navigableKeySet()), values(view.navigableKeySet()), "key order");
        equal(values(expected.descendingKeySet()), values(view.descendingKeySet()), "reverse key order");
        equal(expected.size(), view.size(), "map size");
        equal(expected.comparator(), view.comparator(), "map comparator");
        equal(expected.isEmpty(), view.isEmpty(), "map emptiness");
        for (int probe = -65; probe <= 65; probe += 13) {
            equal(expected.floorEntry(probe), view.floorEntry(probe), "map floor");
            equal(expected.ceilingEntry(probe), view.ceilingEntry(probe), "map ceiling");
            equal(expected.higherEntry(probe), view.higherEntry(probe), "map higher");
            equal(expected.lowerEntry(probe), view.lowerEntry(probe), "map lower");
            equal(expected.headMap(probe, true), view.headMap(probe, true), "map head");
            equal(expected.tailMap(probe, false), view.tailMap(probe, false), "map tail");
        }
        equal(expected.firstEntry(), view.firstEntry(), "map first entry");
        equal(expected.lastEntry(), view.lastEntry(), "map last entry");
        equal(expected.descendingMap(), view.descendingMap(), "map descending");
        if (!base.isEmpty()) {
            int from = base.firstKey(), to = base.lastKey();
            equal(expected.subMap(from, true, to, true),
                    view.subMap(from, true, to, true), "map inclusive range");
        }
    }

    public static void main(String[] args) {
        setAndCollectionContracts();
        navigableSetContracts();
        navigableMapContracts();
        randomizedContracts();
        System.out.println("PASS M3_FILTERED_VIEWS checks=" + checks + " randomized=750");
    }
}
