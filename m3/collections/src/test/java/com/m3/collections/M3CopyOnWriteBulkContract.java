/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/** Standalone finite oracle: JDK owner semantics, filtered bulk mutation, and sorted control. */
public final class M3CopyOnWriteBulkContract {
    private static long checks;

    private M3CopyOnWriteBulkContract() {
    }

    public static void main(String[] args) {
        fixed(CopyOnWriteArrayList::new, true);
        fixed(CopyOnWriteArraySet::new, false);
        randomized(CopyOnWriteArrayList::new, true, 0x331799L);
        randomized(CopyOnWriteArraySet::new, false, 0x331799L);
        standardOwners();
        System.out.println("M3_COW_BULK_CONTRACT_PASS checks=" + checks);
    }

    private static void fixed(Factory factory, boolean allowsDuplicates) {
        Collection<Integer> owner = factory.make(Arrays.asList(null, 1, 2, 2, 3, 4, 6, 8));
        Collection<Integer> view = M3CollectionViews.filtered(owner, even());
        check(owner.contains(null), "owner retains hidden null");
        equal(allowsDuplicates ? List.of(2, 2, 4, 6, 8) : List.of(2, 4, 6, 8),
                new ArrayList<>(view), "filtered order/multiplicity");
        check(view.removeIf(n -> n == 4), "removeIf must remove visible 4");
        check(owner.contains(null) && owner.contains(1) && owner.contains(3), "hidden survivors");
        check(!view.removeAll(List.of(1, 3)), "removeAll hidden values is no-op");
        check(view.retainAll(Set.of(2, 8)), "retainAll visible values");
        Collection<Integer> nested = M3CollectionViews.filtered(view, n -> n > 2);
        check(nested.removeAll(nested), "aliased nested removeAll");
        check(owner.contains(2) && !owner.contains(8), "nested backed ownership");
        view.clear();
        check(owner.contains(null) && owner.contains(1) && owner.contains(3), "clear keeps hidden");
        check(!view.removeIf(n -> true), "empty visible view removeIf false");
        Iterator<Integer> it = view.iterator();
        try {
            it.remove();
            throw new AssertionError("filtered iterator remove unexpectedly supported");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        try {
            view.removeIf(null);
            throw new AssertionError("removeIf null accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void randomized(Factory factory, boolean allowsDuplicates, long seed) {
        Random rng = new Random(seed + (allowsDuplicates ? 17L : 37L));
        for (int round = 0; round < 120; round++) {
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < 28; i++) {
                input.add(rng.nextInt(17) - 4);
            }
            Collection<Integer> owner = factory.make(input);
            Collection<Integer> oracle = factory.make(input);
            Collection<Integer> view = M3CollectionViews.filtered(owner, even());
            for (int step = 0; step < 30; step++) {
                int mode = rng.nextInt(5);
                Predicate<Integer> drop = switch (mode) {
                    case 0 -> n -> n == 0;
                    case 1 -> n -> n % 4 == 0;
                    case 2 -> Set.of(2, 6)::contains;
                    case 3 -> n -> !Set.of(0, 4, 8).contains(n);
                    default -> n -> true;
                };
                boolean actual = switch (mode) {
                    case 0, 1 -> view.removeIf(drop);
                    case 2 -> view.removeAll(Set.of(2, 6));
                    case 3 -> view.retainAll(Set.of(0, 4, 8));
                    default -> {
                        boolean nonempty = !view.isEmpty();
                        view.clear();
                        yield nonempty;
                    }
                };
                boolean expected = oracle.removeIf(n -> even().test(n) && drop.test(n));
                check(actual == expected, "changed: " + round + "/" + step);
                equal(new ArrayList<>(oracle), new ArrayList<>(owner), "owner: " + round + "/" + step);
                equal(visible(oracle), new ArrayList<>(view), "view: " + round + "/" + step);
                if (step % 5 == 0) {
                    Integer next = rng.nextInt(17) - 4;
                    owner.add(next);
                    oracle.add(next);
                }
            }
        }
    }

    private static void standardOwners() {
        List<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        Collection<Integer> view = M3CollectionViews.filtered(owner, even());
        check(view.removeIf(n -> n == 2), "ordinary ArrayList path");
        equal(List.of(1, 3, 4, 5, 6), owner, "ordinary survivors");
        check(view.removeAll(Set.of(4, 6)), "ordinary removeAll path");
        equal(List.of(1, 3, 5), owner, "ordinary hidden survivors");

        NavigableSet<Integer> sorted = new TreeSet<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));
        NavigableSet<Integer> sortedView = M3CollectionViews.filtered(sorted, even());
        check(sortedView.headSet(6, false).removeIf(n -> n >= 2), "bounded sorted owner path");
        equal(List.of(0, 1, 3, 5, 6, 7), new ArrayList<>(sorted), "sorted hidden/range survivors");
        check(sortedView.descendingSet().removeAll(Set.of(6)), "descending sorted bulk removal");
        equal(List.of(0, 1, 3, 5, 7), new ArrayList<>(sorted), "descending survivors");
    }

    private static Predicate<Integer> even() {
        return n -> n != null && (n & 1) == 0;
    }

    private static List<Integer> visible(Collection<Integer> source) {
        List<Integer> result = new ArrayList<>();
        for (Integer n : source) {
            if (even().test(n)) {
                result.add(n);
            }
        }
        return result;
    }

    private static void equal(Object expected, Object actual, String context) {
        check(Objects.equals(expected, actual), context + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) {
            throw new AssertionError(context);
        }
    }

    @FunctionalInterface
    private interface Factory {
        Collection<Integer> make(Collection<Integer> values);
    }
}
