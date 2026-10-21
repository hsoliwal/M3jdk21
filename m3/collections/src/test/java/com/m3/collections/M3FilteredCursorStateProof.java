// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Pure Java 21 contract oracle for existing filtered list/deque/collection owners. */
public final class M3FilteredCursorStateProof {
    private static long checks;

    private M3FilteredCursorStateProof() { }

    public static void main(String[] args) {
        filteredListReversibleProbe();
        filteredListFailurePoisoning();
        filteredDequePrefetchedRemoval();
        filteredDequeFailurePoisoning();
        filteredReadOnlyNullsAndLiveBacking();
        nestedPredicateOrderAndSortedViews();
        randomizedStableOccurrences();
        System.out.println("PASS M3FilteredCursorStateProof checks=" + checks);
    }

    private static void filteredListReversibleProbe() {
        List<Integer> backing = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6));
        List<Integer> view = M3FilteredLists.filtered(backing, item -> (item & 1) == 0);
        ListIterator<Integer> it = view.listIterator();
        check(it.hasNext() && it.hasNext(), "list lookahead repeat");
        equal(0, it.next(), "list first selected");
        check(it.hasNext() && it.hasNext(), "lookahead then remove delivered");
        it.remove();
        equal(List.of(1, 2, 3, 4, 5, 6), backing, "physical remove targets delivered");
        equal(2, it.next(), "physical cached next visible");
        equal(2, it.previous(), "bidirectional cursor direction");
        equal(2, it.next(), "revisit same occurrence");
        it.set(12);
        check(backing.contains(12) && !backing.contains(2), "list accepted set");
        refuses(IllegalArgumentException.class, () -> it.set(13), "list rejected set");
        it.add(14);
        check(backing.contains(14), "list accepted add");
        check(view.contains(14) && !view.contains(1), "backed visibility");
        refuses(IllegalArgumentException.class, () -> it.add(15), "list rejected add");
    }

    private static void filteredListFailurePoisoning() {
        List<Integer> backing = new ArrayList<>(List.of(2, 4, 6));
        ListIterator<Integer> it = M3FilteredLists.filtered(backing, item -> {
            if (item == 4) throw new IllegalArgumentException("predicate refused");
            return true;
        }).listIterator();
        equal(2, it.next(), "first selected before exception");
        refuses(IllegalArgumentException.class, it::hasNext, "list failing probe");
        refuses(IllegalStateException.class, it::nextIndex, "cursor remains poisoned");
        refuses(IllegalStateException.class, it::hasNext, "probe cannot be retried");
        equal(List.of(2, 4, 6), backing, "failed predicate no write");
    }

    private static void filteredDequePrefetchedRemoval() {
        Deque<Integer> backing = new ArrayDeque<>(List.of(1, 2, 3, 4, 5, 6));
        Deque<Integer> view = M3FilteredDeques.filtered(backing, value -> (value & 1) == 0);
        Iterator<Integer> it = view.iterator();
        equal(2, it.next(), "first deque selected");
        check(it.hasNext() && it.hasNext(), "deque prepared next");
        it.remove();
        equal(List.of(1, 3, 4, 5, 6), new ArrayList<>(backing),
                "deque remove delivered, not prefetched");
        equal(4, it.next(), "next after prefetched mutation");
        it.remove();
        equal(List.of(1, 3, 5, 6), new ArrayList<>(backing),
                "deque immediate removal retains physical cursor");
        equal(6, it.next(), "deque trailing selected");
        check(!it.hasNext() && !it.hasNext(), "deque terminal state idempotent");
        refuses(java.util.NoSuchElementException.class, it::next, "deque exhausted next");
    }

    private static void filteredDequeFailurePoisoning() {
        Deque<Integer> backing = new ArrayDeque<>(List.of(1, 2, 3));
        Iterator<Integer> it = M3FilteredDeques.filtered(backing, item -> {
            if (item == 2) throw new IllegalArgumentException("bad predicate");
            return item % 2 == 0;
        }).iterator();
        refuses(IllegalArgumentException.class, it::hasNext, "deque callback throws");
        refuses(IllegalStateException.class, it::hasNext, "deque remains poisoned");
        refuses(IllegalStateException.class, it::remove, "deque rejected mutation");
        equal(List.of(1, 2, 3), new ArrayList<>(backing),
                "deque failure did not mutate backing");
    }

    private static void filteredReadOnlyNullsAndLiveBacking() {
        List<Integer> backing = new ArrayList<>(Arrays.asList(null, 1, 2, null, 3, 4));
        Collection<Integer> view = M3FilteredViews.filter(
                (Collection<Integer>) backing, value -> value == null || (value & 1) == 0);
        Iterator<Integer> it = view.iterator();
        check(it.hasNext() && it.hasNext(), "read-only hasNext repeat");
        equal(null, it.next(), "selected null distinct from empty");
        equal(2, it.next(), "visible after hidden");
        equal(null, it.next(), "duplicate selected null occurrence");
        refuses(UnsupportedOperationException.class, it::remove,
                "read-only iterator refuses even when deliverable");
        equal(4, it.next(), "last selected value");
        check(!it.hasNext() && !it.hasNext(), "read-only terminal state");
        backing.add(6);
        equal(Arrays.asList(null, 2, null, 4, 6), selected(view), "new cursor sees live backing");
    }

    private static void nestedPredicateOrderAndSortedViews() {
        Collection<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4));
        List<String> trace = new ArrayList<>();
        Collection<Integer> nested = owner;
        for (int i = 0; i < 256; i++) {
            int depth = i;
            nested = M3FilteredViews.filter(nested, value -> {
                trace.add(depth + ":" + value);
                return value % 2 == 0;
            });
        }
        Iterator<Integer> cursor = nested.iterator();
        check(trace.isEmpty(), "nested filter metadata construction stays lazy");
        equal(2, cursor.next(), "nested predicate accepted 2");
        equal(257, trace.size(), "inner-first 1 then accepted 2 predicate trace");
        equal("0:1", trace.get(0), "first rejected source candidate");
        for (int i = 0; i < 256; i++) {
            equal(i + ":2", trace.get(i + 1), "nested predicate callback sequence");
        }
        equal(4, cursor.next(), "second accepted even candidate");
        NavigableSet<Integer> ordered = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> filtered = M3FilteredViews.filter(
                ordered, value -> value % 2 == 0);
        equal(List.of(2, 4, 6), selected(filtered), "navigable sorted filter order");
        equal(List.of(6, 4, 2), selected(filtered.descendingSet()),
                "descending sorted metadata and order");
    }

    private static void randomizedStableOccurrences() {
        Random random = new Random(0x4d3346494c544552L);
        for (int trial = 0; trial < 500; trial++) {
            List<Integer> original = new ArrayList<>();
            for (int i = 0, n = random.nextInt(80); i < n; i++) {
                original.add(random.nextInt(17) - 8);
            }
            int mod = 1 + random.nextInt(5);
            List<Integer> expected = original.stream()
                    .filter(value -> Math.floorMod(value, mod) == 0).toList();
            Collection<Integer> readOnly = M3FilteredViews.filter(
                    (Collection<Integer>) original,
                    value -> Math.floorMod(value, mod) == 0);
            equal(expected, selected(readOnly), "read-only filtered encounter order");
            List<Integer> live = M3FilteredLists.filtered(new ArrayList<>(original),
                    value -> Math.floorMod(value, mod) == 0);
            equal(expected, selected(live), "list cursor encounter order");
            Deque<Integer> deque = new ArrayDeque<>(original);
            equal(expected, selected(M3FilteredDeques.filtered(deque,
                    value -> Math.floorMod(value, mod) == 0)),
                    "deque filtered encounter order");
        }
    }

    private static <T> List<T> selected(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        for (T item : iterable) result.add(item);
        return result;
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(why + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void refuses(Class<? extends Throwable> expected,
            Runnable action, String description) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError(description + ": wrong failure", error);
        }
        throw new AssertionError(description + ": expected " + expected.getName());
    }
}
