// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Standalone Java 21 differential oracle for bounded filtered navigation. */
public final class M3NavigableBoundaryProof {
    private static int checks;
    private M3NavigableBoundaryProof() {}

    public static void main(String[] args) {
        naturalAndLive();
        comparatorAndRandomized();
        System.out.println("M3_NAVIGABLE_BOUNDARY_PROOF_PASS checks=" + checks);
    }

    private static void naturalAndLive() {
        NavigableSet<Integer> parent = new TreeSet<>();
        for (int v = 0; v < 20; v++) parent.add(v);
        NavigableSet<Integer> bounded = parent.subSet(3, true, 8, false);
        NavigableSet<Integer> evens = M3FilteredNavigableSet.view(bounded, v -> v % 2 == 0);
        same(6, evens.lower(100), "lower/outside upper bound");
        same(6, evens.floor(100), "floor/outside upper bound");
        same(4, evens.ceiling(-100), "ceiling/outside lower bound");
        same(4, evens.higher(-100), "higher/outside lower bound");
        same(null, evens.lower(-100), "lower/outside lower bound");
        same(null, evens.ceiling(100), "ceiling/outside upper bound");
        same(List.of(4, 6), new ArrayList<>(evens), "range iteration");
        parent.remove(4);
        same(List.of(6), new ArrayList<>(evens), "live deletion");
        parent.add(4);
        same(List.of(4, 6), new ArrayList<>(evens), "live insertion");
        try {
            evens.headSet(100, false);
            throw new AssertionError("range API lost exception");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void comparatorAndRandomized() {
        Random random = new Random(0x4D335E7L);
        for (boolean reversed : new boolean[] {false, true}) {
            NavigableSet<Integer> parent = reversed
                    ? new TreeSet<>(Comparator.reverseOrder()) : new TreeSet<>();
            for (int i = 0; i < 45; i++) parent.add(i);
            for (int start = 2; start < 12; start++) {
                int end = start + 13;
                NavigableSet<Integer> bounded = reversed
                        ? parent.subSet(end, true, start, false)
                        : parent.subSet(start, true, end, false);
                Predicate<Integer> admitted = n -> n % 4 == 0 || n % 7 == 0;
                for (boolean descending : new boolean[] {false, true}) {
                    NavigableSet<Integer> owner = descending ? bounded.descendingSet() : bounded;
                    NavigableSet<Integer> view = M3FilteredNavigableSet.view(owner, admitted);
                    for (int j = 0; j < 140; j++) {
                        int probe = random.nextInt(76) - 15;
                        for (int nav = 0; nav < 4; nav++) {
                            Integer expected = oracle(owner, admitted, probe, nav);
                            Integer actual = switch (nav) {
                                case 0 -> view.lower(probe);
                                case 1 -> view.floor(probe);
                                case 2 -> view.ceiling(probe);
                                default -> view.higher(probe);
                            };
                            same(expected, actual,
                                    "nav=" + nav + ",range=" + start + ".." + end
                                            + ",probe=" + probe + ",reverse=" + reversed
                                            + ",descending=" + descending);
                        }
                    }
                }
            }
        }
    }

    private static Integer oracle(
            NavigableSet<Integer> owner, Predicate<Integer> admitted, int probe, int operation) {
        @SuppressWarnings("unchecked")
        Comparator<Integer> order = (Comparator<Integer>) (owner.comparator() == null
                ? Comparator.<Integer>naturalOrder() : owner.comparator());
        Integer last = null;
        for (Integer value : owner) {
            if (!admitted.test(value)) continue;
            int cmp = order.compare(value, probe);
            if (operation == 0 && cmp < 0) last = value;
            if (operation == 1 && cmp <= 0) last = value;
            if (operation == 2 && cmp >= 0) return value;
            if (operation == 3 && cmp > 0) return value;
        }
        return last;
    }

    private static void same(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
        checks++;
    }
}
