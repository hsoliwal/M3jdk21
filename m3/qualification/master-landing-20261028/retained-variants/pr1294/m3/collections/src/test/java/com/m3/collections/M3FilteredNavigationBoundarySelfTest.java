/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

/** Finite ordered-navigation oracle; no source or predicate-result snapshot is retained by a view. */
public final class M3FilteredNavigationBoundarySelfTest {
    private static long checks;
    private static long queries;
    private static long tape = 0xcbf29ce484222325L;
    private static final Integer[] NUMBERS = {-3, -2, -1, 0, 1, 2, 3};

    private M3FilteredNavigationBoundarySelfTest() { }

    private static void equal(Object expected, Object actual, String operation) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(operation + " expected=" + expected + " actual=" + actual);
        }
        tape = (tape ^ Objects.hashCode(actual)) * 0x100000001b3L;
    }

    private static void same(Object expected, Object actual, String operation) {
        checks++;
        if (expected != actual) throw new AssertionError(operation + " lost identity");
    }

    private static void throwsType(Class<? extends RuntimeException> type,
            Runnable action, String operation) {
        checks++;
        try {
            action.run();
        } catch (RuntimeException failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(operation + " wrong exception", failure);
        }
        throw new AssertionError(operation + " failed to refuse");
    }

    private static boolean accepted(Integer value, int mask) {
        if (value == null) return (mask & 1) != 0;
        return (mask & (value < 0 ? 2 : (value == 0 ? 4 : 8))) != 0;
    }

    private static int compare(Comparator<? super Integer> order, Integer a, Integer b) {
        return order == null ? a.compareTo(b) : order.compare(a, b);
    }

    /** Linear independent contract model, including the exact demanded predicate prefix. */
    private static List<Integer> prefix(NavigableSet<Integer> owner, Integer probe,
            int operation, int mask) {
        List<Integer> visited = new ArrayList<>();
        boolean descending = operation < 2;
        boolean inclusive = operation == 1 || operation == 2;
        Iterator<Integer> cursor = descending ? owner.descendingIterator() : owner.iterator();
        while (cursor.hasNext()) {
            Integer value = cursor.next();
            int relation = compare(owner.comparator(), value, probe);
            boolean eligible = descending ? relation < 0 : relation > 0;
            if (!eligible && !(inclusive && relation == 0)) continue;
            visited.add(value);
            if (accepted(value, mask)) break;
        }
        return visited;
    }

    private static Integer navigate(NavigableSet<Integer> view, Integer probe, int operation) {
        return switch (operation) {
            case 0 -> view.lower(probe);
            case 1 -> view.floor(probe);
            case 2 -> view.ceiling(probe);
            case 3 -> view.higher(probe);
            default -> throw new AssertionError(operation);
        };
    }

    private static Map.Entry<Integer, Integer> navigate(
            NavigableMap<Integer, Integer> view, Integer probe, int operation) {
        return switch (operation) {
            case 0 -> view.lowerEntry(probe);
            case 1 -> view.floorEntry(probe);
            case 2 -> view.ceilingEntry(probe);
            case 3 -> view.higherEntry(probe);
            default -> throw new AssertionError(operation);
        };
    }

    private static void verify(NavigableSet<Integer> owner, NavigableMap<Integer, Integer> map,
            List<Integer> probes, int mask) {
        List<Integer> seen = new ArrayList<>();
        Predicate<Integer> predicate = value -> { seen.add(value); return accepted(value, mask); };
        List<NavigableSet<Integer>> sets = List.of(
                M3FilteredViews.filter(owner, predicate),
                M3MutableFilteredViews.filter(owner, predicate),
                M3FilteredNavigableSet.view(owner, predicate));
        List<NavigableMap<Integer, Integer>> maps = List.of(
                M3FilteredViews.filterEntries(map, (key, value) -> predicate.test(key)),
                M3MutableFilteredViews.filterEntries(map, (key, value) -> predicate.test(key)));
        equal(List.of(), seen, "construction is lazy");
        for (NavigableSet<Integer> view : sets) same(owner.comparator(), view.comparator(), "set order");
        for (NavigableMap<Integer, Integer> view : maps) same(map.comparator(), view.comparator(), "map order");
        for (Integer probe : probes) {
            for (int operation = 0; operation < 4; operation++) {
                List<Integer> expectedTrace = prefix(owner, probe, operation, mask);
                boolean found = !expectedTrace.isEmpty()
                        && accepted(expectedTrace.getLast(), mask);
                Integer expected = found ? expectedTrace.getLast() : null;
                for (NavigableSet<Integer> view : sets) {
                    seen.clear();
                    equal(expected, navigate(view, probe, operation), "set result");
                    equal(expectedTrace, seen, "set predicate occurrence order");
                    queries++;
                }
                Map.Entry<Integer, Integer> expectedEntry = found
                        ? new java.util.AbstractMap.SimpleImmutableEntry<>(expected, map.get(expected))
                        : null;
                for (NavigableMap<Integer, Integer> view : maps) {
                    seen.clear();
                    Map.Entry<Integer, Integer> entry = navigate(view, probe, operation);
                    equal(expectedEntry, entry, "map result including nullable key/value");
                    equal(expectedTrace, seen, "map predicate occurrence order");
                    if (entry != null) {
                        throwsType(UnsupportedOperationException.class,
                                () -> entry.setValue(7), "navigation entry is detached read-only");
                    }
                    queries++;
                }
            }
        }
    }

    private static List<Comparator<Integer>> orders() {
        Comparator<Integer> middle = Comparator.comparingInt(
                value -> value == null ? 0 : value < 0 ? value : value + 1);
        return Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.naturalOrder()), middle, middle.reversed());
    }

    /** All endpoint pairs/inclusivity/directions in a small universe, plus hostile probes. */
    static void boundedCorpus(boolean stableOnly) {
        int profile = 0;
        for (Comparator<Integer> order : orders()) {
            boolean nullable = profile++ >= 2;
            TreeSet<Integer> endpoints = new TreeSet<>(order);
            endpoints.addAll(Arrays.asList(NUMBERS));
            if (nullable) endpoints.add(null);
            List<Integer> bounds = new ArrayList<>(endpoints);
            List<Integer> probes = new ArrayList<>(bounds);
            probes.add(-1000);
            probes.add(1000);
            int ownerKinds = nullable ? 1 : 2;
            for (int kind = 0; kind < ownerKinds; kind++) {
                for (int shape = 0; shape < 4; shape++) {
                    NavigableSet<Integer> set = kind == 0
                            ? new TreeSet<>(order) : new ConcurrentSkipListSet<>(order);
                    NavigableMap<Integer, Integer> map = kind == 0
                            ? new TreeMap<>(order) : new ConcurrentSkipListMap<>(order);
                    for (int index = 0; index < bounds.size(); index++) {
                        if (shape == 1 && (index & 1) == 0 || shape == 2 && index != 2 || shape == 3) continue;
                        Integer value = bounds.get(index);
                        set.add(value);
                        map.put(value, value == null ? null : value * 17);
                    }
                    for (int mask : new int[] {0, 1, 6, 9, 15}) {
                        verify(set, map, probes, mask);
                        verify(set.descendingSet(), map.descendingMap(), probes, mask);
                        if (stableOnly) continue;
                        for (int from = 0; from < bounds.size(); from++) {
                            for (int to = from; to < bounds.size(); to++) {
                                for (int flags = 0; flags < 4; flags++) {
                                    boolean fromInclusive = (flags & 1) != 0;
                                    boolean toInclusive = (flags & 2) != 0;
                                    NavigableSet<Integer> range = set.subSet(bounds.get(from),
                                            fromInclusive, bounds.get(to), toInclusive);
                                    NavigableMap<Integer, Integer> rangeMap = map.subMap(bounds.get(from),
                                            fromInclusive, bounds.get(to), toInclusive);
                                    verify(range, rangeMap, probes, mask);
                                    verify(range.descendingSet(), rangeMap.descendingMap(), probes, mask);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private record Key(int rank, String tag) { }

    static void identityAndFailure() {
        Comparator<Key> byRank = Comparator.comparingInt(Key::rank);
        TreeSet<Key> keys = new TreeSet<>(byRank);
        Key low = new Key(1, "owned low");
        Key high = new Key(2, "owned high");
        keys.add(low);
        keys.add(high);
        NavigableSet<Key> filtered = M3FilteredViews.filter(keys.subSet(low, true, high, true), k -> true);
        same(low, filtered.ceiling(new Key(-1000, "outside alias")), "outside query returns stored reference");
        same(high, filtered.floor(new Key(1000, "outside alias")), "reverse query returns stored reference");
        same(low, filtered.floor(new Key(1, "equivalent alias")), "comparator-equivalent stored reference");
        TreeMap<Key, Object> map = new TreeMap<>(byRank);
        Object payload = new Object();
        map.put(low, payload);
        NavigableMap<Key, Object> selectedMap = M3FilteredViews.filterEntries(map, (k, v) -> true);
        Map.Entry<Key, Object> entry = selectedMap.floorEntry(new Key(1, "equivalent alias"));
        same(low, entry.getKey(), "map key reference");
        same(payload, entry.getValue(), "map value reference");
        map.put(low, new Object());
        same(payload, entry.getValue(), "detached navigation entry");

        RuntimeException boom = new RuntimeException("predicate witness");
        TreeSet<Integer> source = new TreeSet<>(List.of(1, 2, 3));
        Predicate<Integer> throwsOnTwo = value -> {
            if (value == 2) throw boom;
            return false;
        };
        for (NavigableSet<Integer> view : List.of(M3FilteredViews.filter(source, throwsOnTwo),
                M3MutableFilteredViews.filter(source, throwsOnTwo))) {
            try {
                view.ceiling(1);
                throw new AssertionError("predicate failure was lost");
            } catch (RuntimeException failure) {
                same(boom, failure, "predicate failure identity");
            }
        }
        TreeMap<Integer, Integer> rawMap = new TreeMap<>();
        source.forEach(k -> rawMap.put(k, k));
        NavigableMap<Integer, Integer> viewMap = M3FilteredViews.filterEntries(
                rawMap, (k, v) -> throwsOnTwo.test(k));
        try {
            viewMap.ceilingEntry(1);
            throw new AssertionError("map predicate failure was lost");
        } catch (RuntimeException failure) {
            same(boom, failure, "map predicate failure identity");
        }
    }

    static void liveAndQueryContracts() {
        NavigableSet<Integer> source = new TreeSet<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        NavigableSet<Integer> range = source.subSet(2, true, 8, false);
        NavigableSet<Integer> read = M3FilteredViews.filter(range, value -> (value & 1) == 0);
        NavigableSet<Integer> mutable = M3MutableFilteredViews.filter(range, value -> (value & 1) == 0);
        equal(6, read.lower(100), "frozen original bounded-range defect");
        equal(6, read.floor(8), "exclusive upper endpoint probe");
        equal(2, read.ceiling(-100), "outside lower probe");
        equal(6, read.descendingSet().higher(100), "descending outside query");
        equal(6, mutable.lower(100), "mutable bounded-range defect");
        source.remove(6);
        equal(4, read.lower(100), "live removal");
        source.add(6);
        equal(6, read.lower(100), "live insertion");
        equal(2, mutable.pollFirst(), "visible poll");
        equal(true, source.contains(3), "hidden survivor");
        equal(4, read.ceiling(-100), "read-only wrapper sees mutable peer");
        throwsType(UnsupportedOperationException.class, read::pollFirst, "read-only poll");
        Iterator<Integer> iterator = read.iterator();
        iterator.next();
        throwsType(UnsupportedOperationException.class, iterator::remove, "read-only iterator removal");
        throwsType(IllegalArgumentException.class, () -> read.headSet(100, true),
                "invalid range creation is still invalid");
        throwsType(IllegalArgumentException.class, () -> mutable.add(3), "predicate admission");

        // The current read-only set delegates directly: the empty JDK owner does
        // not compare its query. Mutable sets retain their former range admission.
        TreeSet<Integer> emptyOwner = new TreeSet<>();
        equal(emptyOwner.lower(null), M3FilteredViews.filter(emptyOwner, x -> true).lower(null),
                "empty read-only set retains direct owner admission");
        NavigableSet<Integer> emptyMutable = M3MutableFilteredViews.filter(new TreeSet<>(), x -> true);
        throwsType(NullPointerException.class, () -> emptyMutable.lower(null), "empty mutable natural null query");
        NavigableMap<Integer, Integer> emptyMap = M3FilteredViews.filterEntries(
                new TreeMap<Integer, Integer>(), (k, v) -> true);
        throwsType(NullPointerException.class, () -> emptyMap.floorEntry(null), "empty map natural null query");
        Comparator<Integer> checked = (left, right) -> {
            if (Objects.equals(left, 99) || Objects.equals(right, 99)) {
                throw new IllegalArgumentException("rejected query");
            }
            return Integer.compare(left, right);
        };
        NavigableSet<Integer> strict = M3FilteredViews.filter(new TreeSet<>(checked), x -> true);
        equal(null, strict.floor(99), "empty read-only owner makes no comparator query");

        Comparator<Integer> nullOrder = Comparator.nullsFirst(Comparator.naturalOrder());
        TreeSet<Integer> nullable = new TreeSet<>(nullOrder);
        nullable.add(null);
        nullable.add(1);
        NavigableSet<Integer> hiddenNull = M3FilteredViews.filter(nullable, Objects::nonNull);
        equal(1, hiddenNull.ceiling(null), "hidden null is not an absence sentinel");
        equal(null, hiddenNull.lower(1), "end after hidden null");
    }

    public static void main(String[] args) {
        boolean stableOnly = args.length == 1 && args[0].equals("--stable-only");
        boundedCorpus(stableOnly);
        if (!stableOnly) {
            identityAndFailure();
            liveAndQueryContracts();
        }
        System.out.println("PASS M3FilteredNavigationBoundarySelfTest checks=" + checks
                + " queries=" + queries + " stableOnly=" + stableOnly
                + " tape=" + Long.toUnsignedString(tape, 16));
    }
}
