/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Complete real M3FilteredViews with real JDK-backed owners; no replacement classes or native claims. */
public final class M3FilteredNullNavigationProbe {
    private static long checks, failures;
    private static final Comparator<Integer> MIDDLE = Comparator.comparingLong(v -> v == null ? 1L : 2L * v);
    private static final Integer[] VALUES = {Integer.MIN_VALUE, -3, -1, 0, null, 1, 3, Integer.MAX_VALUE};
    private M3FilteredNullNavigationProbe() { }
    private static void check(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            if (failures++ < 8) System.out.println("FAIL " + label + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void reject(Runnable action, Class<? extends Throwable> expected) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }
    public static void main(String[] args) {
        var owner = new TreeSet<Integer>(MIDDLE);
        owner.addAll(Arrays.asList(-2, null, 2));
        var view = M3FilteredViews.filter(owner, Objects::nonNull);
        check(2, view.higher(0), "null-middle-higher");
        check(-2, view.lower(1), "null-middle-lower");
        check(2, view.ceiling(null), "null-middle-ceiling");
        check(-2, view.floor(null), "null-middle-floor");
        check(2, view.descendingSet().lower(0), "descending-null-middle");
        if (args.length == 0 || !args[0].equals("witness")) {
            corpus(); contracts();
        }
        System.out.println("FILTERED_NAVIGATION checks=" + checks + " failures=" + failures + " real_owner=YES");
        if (failures != 0) throw new AssertionError("filtered navigation failures=" + failures);
    }
    private static void corpus() {
        List<Comparator<Integer>> orders = Arrays.asList(MIDDLE, MIDDLE.reversed(),
                Comparator.nullsFirst(Comparator.naturalOrder()), Comparator.nullsLast(Comparator.naturalOrder()));
        Integer[] queries = {Integer.MIN_VALUE, -4, -3, -2, -1, 0, null, 1, 2, 3, 4, Integer.MAX_VALUE};
        for (Comparator<Integer> order : orders) {
            for (int bits = 0; bits < (1 << VALUES.length); bits++) {
                var owner = new TreeSet<Integer>(order);
                for (int i = 0; i < VALUES.length; i++) if ((bits & (1 << i)) != 0) owner.add(VALUES[i]);
                List<NavigableSet<Integer>> ranges = new ArrayList<>();
                ranges.add(owner);
                for (Integer endpoint : Arrays.asList(-1, null, 1)) {
                    ranges.add(owner.headSet(endpoint, false));
                    ranges.add(owner.tailSet(endpoint, true));
                }
                for (int mode = 0; mode < 4; mode++) {
                    final int selection = mode;
                    Predicate<Integer> predicate = v -> switch (selection) {
                        case 0 -> v != null;
                        case 1 -> v == null;
                        case 2 -> v == null || (v & 1) != 0;
                        default -> false;
                    };
                    for (NavigableSet<Integer> range : ranges) for (int reverse = 0; reverse < 2; reverse++) {
                        NavigableSet<Integer> source = reverse == 0 ? range : range.descendingSet();
                        var expected = new TreeSet<Integer>(source.comparator());
                        for (Integer item : source) if (predicate.test(item)) expected.add(item);
                        var actual = M3FilteredViews.filter(source, predicate);
                        check(new ArrayList<>(expected), new ArrayList<>(actual), "encounter-order");
                        check(expected.size(), actual.size(), "size");
                        for (Integer query : queries) {
                            check(expected.lower(query), actual.lower(query), "lower");
                            check(expected.floor(query), actual.floor(query), "floor");
                            check(expected.ceiling(query), actual.ceiling(query), "ceiling");
                            check(expected.higher(query), actual.higher(query), "higher");
                            check(expected.contains(query), actual.contains(query), "contains");
                        }
                    }
                }
            }
        }
    }
    private static void contracts() {
        var owner = new TreeSet<Integer>(MIDDLE);
        owner.addAll(Arrays.asList(-2, null, 2));
        List<Integer> tested = new ArrayList<>();
        var view = M3FilteredViews.filter(owner, value -> { tested.add(value); return value != null; });
        check(List.of(), tested, "lazy-construction");
        check(2, view.higher(0), "trace-result");
        check(Arrays.asList(null, 2), tested, "actual-representatives-only");
        tested.clear(); check(null, view.higher(9), "missing-candidate");
        check(List.of(), tested, "missing-does-not-test-null");
        owner.add(1); check(1, view.higher(0), "live-add");
        owner.remove(1); check(2, view.higher(0), "live-remove");
        reject(() -> view.add(4), UnsupportedOperationException.class);
        reject(() -> view.clear(), UnsupportedOperationException.class);
        reject(() -> view.removeAll(List.of()), UnsupportedOperationException.class);
        reject(() -> view.pollFirst(), UnsupportedOperationException.class);
        var iterator = view.iterator(); iterator.next(); reject(iterator::remove, UnsupportedOperationException.class);
        // The fix never speculatively submits null to a null-rejecting comparator.
        Comparator<Integer> rejectsNull = (a, b) -> Integer.compare(Objects.requireNonNull(a), Objects.requireNonNull(b));
        var strict = new TreeSet<Integer>(rejectsNull); strict.addAll(List.of(-2, 2));
        var strictView = M3FilteredViews.filter(strict, value -> true);
        check(null, strictView.lower(-9), "strict-empty-lower");
        check(null, strictView.higher(9), "strict-empty-higher");
        reject(() -> strictView.ceiling(null), NullPointerException.class);
        var map = new TreeMap<Integer, String>(MIDDLE);
        map.put(-2, "keep"); map.put(null, "hide"); map.put(2, "keep");
        var keys = M3FilteredViews.filterEntries(map, (k, v) -> !v.equals("hide")).navigableKeySet();
        check(2, keys.higher(0), "map-derived-null-middle");
        check(-2, keys.floor(null), "map-derived-floor");
        check(2, keys.descendingSet().lower(0), "map-derived-descending");
        map.put(1, "keep"); check(1, keys.ceiling(null), "map-derived-live");
    }
}
