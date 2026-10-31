// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Bounded, standalone Java 21 contract oracle; no donor runtime dependency. */
public final class M3FilteredSortedContractGate20261029 {
    private static int assertions;

    private M3FilteredSortedContractGate20261029() {}

    public static void main(String[] args) {
        liveSortedSet();
        liveNavigableMap();
        comparatorIdentityAndHiddenMembers();
        System.out.println("M3_FILTERED_SORTED_CONTRACT_GATE_PASS " + assertions);
    }

    private static void liveSortedSet() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        TreeSet<Integer> owner = new TreeSet<>(reverse);
        owner.addAll(List.of(1, 2, 3, 4, 5));
        NavigableSet<Integer> filtered = M3FilteredViews.filter(owner, n -> (n & 1) != 0);
        same(reverse, filtered.comparator());
        eq(List.of(5, 3, 1), new ArrayList<>(filtered));
        check(!filtered.contains(4), "hidden even member visible");
        owner.add(7);
        eq(List.of(7, 5, 3, 1), new ArrayList<>(filtered));
        eq(Integer.valueOf(5), filtered.higher(7));
        eq(Integer.valueOf(5), filtered.floor(4));
        eq(Integer.valueOf(3), filtered.ceiling(4));
        reject(() -> filtered.add(9));
        reject(() -> filtered.remove(3));
        reject(() -> filtered.pollFirst());
        check(owner.contains(3) && owner.contains(4), "read-only view mutated backing");
    }

    private static void liveNavigableMap() {
        TreeMap<Integer, String> owner = new TreeMap<>();
        owner.put(1, "one");
        owner.put(2, "two");
        owner.put(3, "three");
        NavigableMap<Integer, String> visible =
                M3FilteredViews.filterEntries(owner, (key, value) -> (key & 1) != 0);
        eq(List.of(1, 3), new ArrayList<>(visible.navigableKeySet()));
        check(!visible.containsKey(2), "hidden map key visible");
        owner.put(5, "five");
        eq(List.of(1, 3, 5), new ArrayList<>(visible.navigableKeySet()));
        eq(Integer.valueOf(3), visible.lowerKey(4));
        eq(Integer.valueOf(5), visible.higherKey(3));
        reject(() -> visible.put(7, "seven"));
        reject(() -> visible.remove(1));
        reject(() -> visible.firstEntry().setValue("changed"));
        eq("one", owner.get(1));
        eq("two", owner.get(2));
    }

    private static void comparatorIdentityAndHiddenMembers() {
        Comparator<String> byLengthThenText =
                Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());
        TreeSet<String> owner = new TreeSet<>(byLengthThenText);
        owner.addAll(List.of("a", "bb", "ccc"));
        NavigableSet<String> visible = M3FilteredViews.filter(owner, value -> value.length() > 1);
        same(byLengthThenText, visible.comparator());
        eq(List.of("bb", "ccc"), new ArrayList<>(visible));
        check(!visible.contains("a"), "hidden comparator member visible");
        owner.add("dddd");
        eq("dddd", visible.last());
        reject(() -> visible.clear());
        eq(4, owner.size());
    }

    private static void check(boolean pass, String message) {
        assertions++;
        if (!pass) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void same(Object expected, Object actual) {
        assertions++;
        if (expected != actual) throw new AssertionError("comparator identity lost");
    }

    private static void reject(Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("mutation was accepted");
    }
}
