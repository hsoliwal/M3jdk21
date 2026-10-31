/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Exact original-owner Java21 sequenced read-only proof. No mock collections,
 * per-entry nodes, JNI callbacks, or copied third-party implementation code.
 */
public final class M3StrictFilteredSequencedContract {
    private static int checks;

    private M3StrictFilteredSequencedContract() { }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void equal(Object expected, Object actual, String description) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void refuses(Class<? extends Throwable> type,
            Runnable action, String description) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError(description + " wrong exception: " + actual, actual);
        }
        throw new AssertionError(description + " did not refuse");
    }

    private static void strictSequenced(SortedSet<Integer> view, String label) {
        refuses(UnsupportedOperationException.class, view::removeFirst,
                label + " removeFirst must refuse even when empty");
        refuses(UnsupportedOperationException.class, view::removeLast,
                label + " removeLast must refuse even when empty");
        refuses(UnsupportedOperationException.class, () -> view.addFirst(99),
                label + " addFirst");
        refuses(UnsupportedOperationException.class, () -> view.addLast(99),
                label + " addLast");
        refuses(UnsupportedOperationException.class, () -> view.remove(99),
                label + " absent remove");
        refuses(UnsupportedOperationException.class, () -> view.clear(),
                label + " clear");
    }

    private static void emptySortedRefusesWithoutCallbacks() {
        SortedSet<Integer> source = new TreeSet<>(List.of(1, 3, 5));
        AtomicInteger callbacks = new AtomicInteger();
        SortedSet<Integer> hidden = M3FilteredViews.filter(source, item -> {
            callbacks.incrementAndGet();
            return false;
        });
        // Refusal is unconditional; no iteration, predicate or backing write.
        strictSequenced(hidden, "hidden sorted");
        equal(0, callbacks.get(), "empty strict mutation does not evaluate predicate");
        SortedSet<Integer> reverse = hidden.reversed();
        strictSequenced(reverse, "hidden sorted reversed");
        strictSequenced(reverse.reversed(), "hidden sorted twice reversed");
        strictSequenced(hidden.subSet(1, 5), "hidden bounded");
        strictSequenced(hidden.headSet(3).reversed(), "hidden reversed head view");
        equal(0, callbacks.get(), "all failed mutations avoid predicate callbacks");
        refuses(NoSuchElementException.class, hidden::getFirst,
                "getFirst remains a read and reports no selected member");
        refuses(NoSuchElementException.class, reverse::getLast,
                "reversed getLast remains a read");
        equal(List.of(1, 3, 5), List.copyOf(source), "unmodified source order");
    }

    private static void liveSortedReversedAndSlices() {
        SortedSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        SortedSet<Integer> selected = M3FilteredViews.filter(source, v -> (v & 1) == 0);
        equal(List.of(2, 4, 6), List.copyOf(selected), "selected order");
        equal(2, selected.getFirst(), "forward first");
        equal(6, selected.getLast(), "forward last");
        SortedSet<Integer> reverse = selected.reversed();
        equal(List.of(6, 4, 2), List.copyOf(reverse), "reverse remains filtered");
        equal(6, reverse.getFirst(), "reverse first");
        equal(2, reverse.getLast(), "reverse last");
        equal(List.of(2, 4, 6), List.copyOf(reverse.reversed()), "double reversed");
        strictSequenced(selected, "nonempty sorted");
        strictSequenced(reverse, "nonempty reverse");
        strictSequenced(selected.subSet(2, 6).reversed(), "reverse bounded subset");
        strictSequenced(selected.headSet(5), "head subset");
        strictSequenced(selected.tailSet(3), "tail subset");
        source.add(8);
        source.add(0);
        equal(List.of(8, 6, 4, 2, 0), List.copyOf(reverse),
                "reversed view remains live through external owner additions");
        equal(0, selected.first(), "forward view tracks new minimum");
        equal(List.of(0, 1, 2, 3, 4, 5, 6, 8), List.copyOf(source),
                "refused operations did not change backing owner");
    }

    private static void navigableSequencedAndComparatorNull() {
        NavigableSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4, 5));
        NavigableSet<Integer> selected = M3FilteredViews.filter(source, v -> v % 2 == 0);
        NavigableSet<Integer> reverse = selected.reversed();
        equal(List.of(4, 2), List.copyOf(reverse), "covariant NavigableSet reversed");
        equal(List.of(4, 2), List.copyOf(selected.descendingSet()),
                "descendingSet and reversed traverse the same owner");
        equal(4, reverse.first(), "descending filtered first");
        equal(2, reverse.last(), "descending filtered last");
        strictSequenced(selected, "navigable");
        strictSequenced(reverse, "navigable reversed");
        strictSequenced(selected.subSet(2, true, 4, true).reversed(),
                "navigable bounded reversed");
        refuses(UnsupportedOperationException.class, selected::pollFirst,
                "explicit pollFirst also refuses");
        refuses(UnsupportedOperationException.class, reverse::pollLast,
                "explicit reverse pollLast also refuses");
        NavigableSet<Integer> empty = M3FilteredViews.filter(source,
                (Predicate<Integer>) v -> false);
        strictSequenced(empty, "empty navigable");
        strictSequenced(empty.reversed(), "empty navigable reversed");

        NavigableSet<Integer> nullable = new TreeSet<>(Comparator.nullsFirst(
                Integer::compareTo));
        nullable.addAll(Arrays.asList(null, 1, 2, 3));
        NavigableSet<Integer> nullableVisible = M3FilteredViews.filter(nullable,
                v -> v == null || v == 2);
        equal(null, nullableVisible.first(), "stored null remains visible");
        equal(2, nullableVisible.reversed().first(), "reversed null-friendly comparator");
        equal(null, nullableVisible.reversed().last(), "reversed stored null");
        strictSequenced(nullableVisible, "nullable navigable");
        strictSequenced(nullableVisible.reversed(), "nullable reversed");
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments accepted");
        emptySortedRefusesWithoutCallbacks();
        liveSortedReversedAndSlices();
        navigableSequencedAndComparatorNull();
        System.out.println("PASS M3StrictFilteredSequencedContract checks=" + checks);
    }
}
