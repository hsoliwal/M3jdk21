/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Original-owner regression for the historical strict read-only endpoint contract.
 * Inherited JDK defaults inspect emptiness before refusing, which both exposes a
 * callback and produces the wrong exception for an empty filtered result.
 * The public compatibility type, private sorted owner, and private navigable owner
 * must all refuse before inspecting the source. No payload or native bridge is mocked.
 */
public final class M3FilteredEndpointRestorationContract {
    private static int checks;

    private M3FilteredEndpointRestorationContract() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void refuse(Runnable operation, AtomicInteger predicates,
            AtomicInteger comparisons, String label) {
        predicates.set(0);
        comparisons.set(0);
        checks++;
        try {
            operation.run();
            throw new AssertionError(label + " accepted a strict read-only mutation");
        } catch (UnsupportedOperationException expected) {
            equal(0, predicates.get(), label + " predicate demand");
            equal(0, comparisons.get(), label + " comparator demand");
        }
    }

    private static SortedSet<Integer> filter(int family, TreeSet<Integer> source,
            Predicate<Integer> predicate) {
        return switch (family) {
            case 0 -> M3FilteredViews.filter(Collections.unmodifiableSortedSet(source), predicate);
            case 1 -> M3FilteredViews.filter((NavigableSet<Integer>) source, predicate);
            case 2 -> M3FilteredNavigableSet.view(source, predicate);
            default -> throw new AssertionError("unknown owner family");
        };
    }

    private static void strictEndpoints(int firstFamily, int lastFamily) {
        for (int family = firstFamily; family <= lastFamily; family++) {
            for (boolean populated : List.of(false, true)) {
                for (boolean accept : List.of(false, true)) {
                    AtomicInteger predicates = new AtomicInteger();
                    AtomicInteger comparisons = new AtomicInteger();
                    Comparator<Integer> order = (left, right) -> {
                        comparisons.incrementAndGet();
                        return Integer.compare(left, right);
                    };
                    TreeSet<Integer> source = new TreeSet<>(order);
                    if (populated) source.addAll(List.of(1, 2, 3, 4));
                    Predicate<Integer> predicate = value -> {
                        predicates.incrementAndGet();
                        return accept;
                    };
                    SortedSet<Integer> view = filter(family, source, predicate);
                    SortedSet<Integer> reverse = view.reversed();
                    List<SortedSet<Integer>> forms = List.of(view, reverse,
                            reverse.reversed(), view.headSet(3), view.tailSet(1),
                            view.subSet(1, 3), reverse.headSet(1), reverse.tailSet(3),
                            reverse.subSet(3, 1));
                    List<Integer> before = new ArrayList<>(source);
                    for (int index = 0; index < forms.size(); index++) {
                        SortedSet<Integer> form = forms.get(index);
                        String label = "family=" + family + " populated=" + populated
                                + " accept=" + accept + " form=" + index;
                        refuse(form::removeFirst, predicates, comparisons, label + " removeFirst");
                        refuse(form::removeLast, predicates, comparisons, label + " removeLast");
                        refuse(() -> form.addFirst(99), predicates, comparisons, label + " addFirst");
                        refuse(() -> form.addLast(99), predicates, comparisons, label + " addLast");
                        if (form instanceof NavigableSet<Integer> nav) {
                            refuse(nav::pollFirst, predicates, comparisons, label + " pollFirst");
                            refuse(nav::pollLast, predicates, comparisons, label + " pollLast");
                        }
                        equal(before, new ArrayList<>(source), label + " owner unchanged");
                    }
                }
            }
        }
    }

    private static void facadeRemainsLive() {
        TreeSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> view = M3FilteredNavigableSet.view(source, value -> (value & 1) == 0);
        NavigableSet<Integer> reverse = view.reversed();
        NavigableSet<Integer> range = view.subSet(2, true, 8, true);
        equal(true, reverse instanceof M3FilteredNavigableSet<?>, "reverse retains established type");
        equal(true, range instanceof M3FilteredNavigableSet<?>, "range retains established type");
        equal(List.of(6, 4, 2), new ArrayList<>(reverse), "reverse selected order");
        source.add(8);
        source.remove(4);
        equal(List.of(8, 6, 2), new ArrayList<>(reverse), "reverse sees owner mutations");
        equal(List.of(2, 6, 8), new ArrayList<>(range), "range sees owner mutations");
        equal(Integer.valueOf(6), view.floor(7), "floor retains owner navigation");
        equal(Integer.valueOf(8), view.ceiling(7), "ceiling retains owner navigation");
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "all" : args[0];
        if (args.length > 1) throw new IllegalArgumentException("one optional mode");
        switch (mode) {
            case "all" -> { strictEndpoints(0, 2); facadeRemainsLive(); }
            case "private" -> strictEndpoints(0, 1);
            case "facade" -> { strictEndpoints(2, 2); facadeRemainsLive(); }
            default -> throw new IllegalArgumentException("unknown mode " + mode);
        }
        System.out.println("PASS M3FilteredEndpointRestorationContract mode=" + mode + " checks=" + checks);
    }
}
