/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/** Frozen before/after effect trace; comparator and predicate callbacks are observable. */
public final class M3FilteredTerminalTrace {
    private M3FilteredTerminalTrace() { }
    private static String observe(Supplier<?> action) {
        try { return String.valueOf(action.get()); }
        catch (RuntimeException failure) { return failure.getClass().getName() + ":" + failure.getMessage(); }
    }
    public static void main(String[] args) {
        int cases = 0;
        for (int reverse = 0; reverse < 2; reverse++) for (int pattern = 0; pattern < 4; pattern++) {
            for (int throwAt = -1; throwAt < 12; throwAt++) for (int query = -2; query <= 12; query++) {
                List<String> effects = new ArrayList<>();
                Comparator<Integer> order = Comparator.nullsFirst(Integer::compare);
                if (reverse != 0) order = order.reversed();
                Comparator<Integer> actual = order;
                Comparator<Integer> traced = (a, b) -> { effects.add("C:" + a + ":" + b); return actual.compare(a, b); };
                NavigableSet<Integer> set = new TreeSet<>(traced);
                NavigableMap<Integer, Integer> map = new TreeMap<>(traced);
                for (int n = 0; n < 10; n++) { set.add(n); map.put(n, n % 3 == 0 ? null : n); }
                set.add(null); map.put(null, null);
                int wanted = pattern, failureAt = throwAt;
                int[] calls = { 0 };
                java.util.function.Predicate<Integer> predicate = value -> {
                    effects.add("P:" + value);
                    if (calls[0]++ == failureAt) throw new IllegalStateException("predicate failure");
                    return wanted == 0 || (value != null && (wanted == 1 ? value % 2 == 0 : wanted == 2 ? value > 7 : false));
                };
                NavigableSet<Integer> view = M3FilteredViews.filter(set, predicate);
                NavigableMap<Integer, Integer> mapped = M3FilteredViews.filterEntries(map, (k, v) -> predicate.test(k));
                Integer pivot = query == -2 ? null : query;
                List<Supplier<?>> operations = List.of(() -> view.lower(pivot), () -> view.floor(pivot),
                        () -> view.ceiling(pivot), () -> view.higher(pivot), view::first, view::last,
                        mapped::isEmpty, mapped::firstEntry, mapped::lastEntry,
                        () -> mapped.lowerEntry(pivot), () -> mapped.ceilingEntry(pivot),
                        () -> mapped.descendingMap().firstEntry());
                for (int operation = 0; operation < operations.size(); operation++) {
                    effects.clear(); calls[0] = 0;
                    String value = observe(operations.get(operation));
                    System.out.println(cases++ + "|" + value + "|" + effects);
                }
            }
        }
    }
}
