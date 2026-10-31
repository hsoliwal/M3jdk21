/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Dependency-free JDK-21 reference-model differential and regression oracle. */
public final class M3FilteredNavigableSetRangeContract {
    private static long checks;
    private M3FilteredNavigableSetRangeContract() { }

    private static void equal(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void rejected(Runnable operation, String what) {
        checks++;
        try { operation.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError(what + " must refuse mutation");
    }

    private static NavigableSet<Integer> expected(NavigableSet<Integer> backing,
            Comparator<Integer> order, Predicate<Integer> predicate) {
        NavigableSet<Integer> result = new TreeSet<>(order);
        for (Integer element : backing) {
            if (predicate.test(element)) result.add(element);
        }
        return result;
    }

    private static void compare(NavigableSet<Integer> backing, Comparator<Integer> order,
            Predicate<Integer> predicate, String label) {
        NavigableSet<Integer> view = M3FilteredNavigableSet.view(backing, predicate);
        NavigableSet<Integer> truth = expected(backing, order, predicate);
        equal(new ArrayList<>(truth), new ArrayList<>(view), label + " asc");
        equal(new ArrayList<>(truth.descendingSet()), new ArrayList<>(view.descendingSet()),
                label + " descending");
        equal(truth.size(), view.size(), label + " size");
        equal(truth.isEmpty(), view.isEmpty(), label + " empty");
        for (Integer probe : Arrays.asList(null, -999, -8, -3, 0, 3, 8, 999)) {
            equal(truth.contains(probe), view.contains(probe), label + " contains " + probe);
            equal(truth.lower(probe), view.lower(probe), label + " lower " + probe);
            equal(truth.floor(probe), view.floor(probe), label + " floor " + probe);
            equal(truth.ceiling(probe), view.ceiling(probe), label + " ceiling " + probe);
            equal(truth.higher(probe), view.higher(probe), label + " higher " + probe);
        }
        if (!truth.isEmpty()) {
            equal(truth.first(), view.first(), label + " first");
            equal(truth.last(), view.last(), label + " last");
        }
    }

    private static void regressions() {
        Comparator<Integer> first = Comparator.nullsFirst(Integer::compare);
        NavigableSet<Integer> set = new TreeSet<>(first);
        set.addAll(Arrays.asList(null, 1, 2, 3));
        NavigableSet<Integer> justNull = M3FilteredNavigableSet.view(set, Objects::isNull);
        equal(true, justNull.contains(null), "nullsFirst stored-null membership");
        equal(false, justNull.contains(1), "hidden ordinary member");
        equal(Arrays.asList((Integer) null), new ArrayList<>(justNull),
                "null element in iterator");
        equal(null, justNull.first(), "first nullable member");
        Predicate<Integer> nonnullEven = x -> x != null && (x & 1) == 0;
        NavigableSet<Integer> skipNull = M3FilteredNavigableSet.view(set, nonnullEven);
        equal(2, skipNull.ceiling(null), "nullable floor bound, skip rejected null");
        equal(2, skipNull.higher(null), "nullable exclusive bound, skip rejected null");
        NavigableSet<Integer> bounded = skipNull.subSet(1, true, 3, true);
        equal(2, bounded.floor(999), "bounded floor beyond upper endpoint");
        equal(2, bounded.ceiling(-999), "bounded ceiling below lower endpoint");
        equal(2, bounded.lower(999), "bounded lower beyond upper endpoint");
        equal(2, bounded.higher(-999), "bounded higher below lower endpoint");
        equal(null, bounded.lower(-999), "bounded lower exhaustion");
        equal(null, bounded.higher(999), "bounded higher exhaustion");
        // Only a truly stored, comparator-equivalent representative is tested.
        NavigableSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.add("Alpha");
        NavigableSet<String> namesView = M3FilteredNavigableSet.view(
                names, item -> item.equals("Alpha"));
        equal(true, namesView.contains("ALPHA"), "comparator-equivalent stored value");
        equal(false, M3FilteredNavigableSet.view(names,
                item -> item.equals("ALPHA")).contains("ALPHA"),
                "the query alias must not substitute for stored member");
        rejected(() -> bounded.remove(2), "bounded read-only remove");
        rejected(() -> bounded.add(4), "bounded read-only add");
        rejected(bounded::pollFirst, "bounded read-only poll");
        rejected(() -> bounded.removeIf(item -> true), "bounded read-only removeIf");
        rejected(() -> bounded.addAll(java.util.List.of()), "empty batch admission");
        equal(true, set.contains(2), "backing unchanged after refusal");
        set.remove(2);
        equal(true, bounded.isEmpty(), "bounded view is live after backing mutation");
    }

    private static void randomized() {
        for (int polarity = 0; polarity < 2; polarity++) {
            for (int nullOrder = 0; nullOrder < 2; nullOrder++) {
                Comparator<Integer> order = nullOrder == 0
                        ? Comparator.nullsFirst(Integer::compare)
                        : Comparator.nullsLast(Integer::compare);
                if (polarity == 1) order = order.reversed();
                TreeSet<Integer> root = new TreeSet<>(order);
                final Comparator<Integer> stableOrder = order;
                Random random = new Random(0x4d334e554c4cL + polarity * 71L + nullOrder);
                for (int step = 0; step < 200; step++) {
                    Integer changed = step % 23 == 0 ? null : random.nextInt(29) - 14;
                    if (random.nextBoolean()) root.add(changed);
                    else root.remove(changed);
                    Predicate<Integer> predicate = step % 3 == 0
                            ? value -> value == null || value % 2 == 0
                            : step % 3 == 1 ? value -> value != null && value % 3 == 0
                                    : value -> value == null || value >= 0;
                    compare(root, stableOrder, predicate,
                            "root mode=" + polarity + ":" + nullOrder + " step=" + step);
                    if (!root.isEmpty()) {
                        NavigableSet<Integer> allBounded = root.subSet(
                                root.first(), true, root.last(), true);
                        compare(allBounded, stableOrder, predicate,
                                "bounded mode=" + polarity + ":" + nullOrder + " step=" + step);
                        compare(allBounded.descendingSet(), stableOrder.reversed(), predicate,
                                "reverse mode=" + polarity + ":" + nullOrder + " step=" + step);
                    }
                }
            }
        }
    }

    public static void run() {
        checks = 0;
        regressions();
        randomized();
        System.out.println("PASS M3FilteredNavigableSetRangeContract checks=" + checks);
    }

    public static void main(String[] args) { run(); }
}
