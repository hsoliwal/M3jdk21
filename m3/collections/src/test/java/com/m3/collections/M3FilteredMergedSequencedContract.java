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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * Cross-feature contract: merged read-only endpoints, reversed sorted ranges and
 * recursive split metadata must compose on the same live Java 21 owner.
 * Expected membership/order comes from an independent JDK TreeSet and explicit
 * selection. No target implementation or reflection into target state is used.
 */
public final class M3FilteredMergedSequencedContract {
    private static long checks;

    private M3FilteredMergedSequencedContract() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void readonly(Runnable action, String label) {
        checks++;
        try { action.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError(label + " did not refuse mutation");
    }

    private static <E> void strict(SortedSet<E> view, E sample, String label) {
        SequencedSet<E> sequenced = view;
        readonly(sequenced::removeFirst, label + " removeFirst");
        readonly(sequenced::removeLast, label + " removeLast");
        readonly(() -> sequenced.addFirst(sample), label + " addFirst");
        readonly(() -> sequenced.addLast(sample), label + " addLast");
        readonly(() -> view.add(sample), label + " add");
        readonly(() -> view.remove(sample), label + " remove");
        readonly(() -> view.addAll(List.of()), label + " empty addAll");
        readonly(() -> view.removeAll(List.of()), label + " empty removeAll");
        readonly(() -> view.retainAll(List.of()), label + " retainAll");
        readonly(() -> view.removeIf(value -> { throw new AssertionError("mutation callback"); }),
                label + " removeIf");
        readonly(view::clear, label + " clear");
        readonly(() -> view.iterator().remove(), label + " iterator removal");
    }

    private static <E> void collect(Spliterator<E> cursor, Comparator<? super E> order,
            List<E> result, int depth) {
        check(depth < 32, "finite split tree");
        check(cursor.hasCharacteristics(Spliterator.ORDERED | Spliterator.SORTED
                | Spliterator.DISTINCT), "sorted properties on every split");
        check(cursor.getComparator() == order, "owner comparator identity on every split");
        Spliterator<E> prefix = cursor.trySplit();
        if (prefix != null) collect(prefix, order, result, depth + 1);
        cursor.forEachRemaining(result::add);
    }

    private static <E> void verify(SortedSet<E> actual, List<E> expected, String label) {
        same(expected, new ArrayList<>(actual), label + " live encounter order");
        same(expected.size(), actual.size(), label + " cardinality");
        same(expected.isEmpty(), actual.isEmpty(), label + " emptiness");
        if (!expected.isEmpty()) {
            same(expected.getFirst(), actual.getFirst(), label + " first endpoint");
            same(expected.getLast(), actual.getLast(), label + " last endpoint");
        }
        List<E> split = new ArrayList<>();
        collect(actual.spliterator(), actual.comparator(), split, 0);
        same(expected, split, label + " recursive split order");
        same(expected, actual.parallelStream().toList(), label + " parallel encounter order");
    }

    private static List<Integer> selected(SortedSet<Integer> owner,
            Predicate<Integer> predicate) {
        List<Integer> result = new ArrayList<>();
        for (Integer value : owner) if (predicate.test(value)) result.add(value);
        return result;
    }

    private static void sortedReversedRangesAndStreams() {
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.comparingInt((Integer value) -> Math.floorMod(value, 5))
                        .thenComparingInt(Integer::intValue));
        Predicate<Integer> keep = value -> (value & 1) == 0;
        for (Comparator<Integer> order : orders) {
            TreeSet<Integer> source = new TreeSet<>(order);
            for (int value = -64; value <= 64; value++) source.add(value);
            List<Integer> ownerOrder = new ArrayList<>(source);
            // A SortedSet-only exposure exercises the JDK21 default reversed view,
            // including its asymmetric half-open ranges, rather than a nav shortcut.
            SortedSet<Integer> owner = Collections.unmodifiableSortedSet(source);
            List<SortedSet<Integer>> owners = List.of(owner,
                    owner.subSet(ownerOrder.get(7), ownerOrder.get(119)),
                    owner.headSet(ownerOrder.get(96)), owner.tailSet(ownerOrder.get(31)));
            for (SortedSet<Integer> bounded : owners) {
                SortedSet<Integer> forward = M3FilteredViews.filter(bounded, keep);
                List<Integer> expected = selected(bounded, keep);
                SortedSet<Integer> reverse = forward.reversed();
                List<Integer> reversed = new ArrayList<>(expected.reversed());
                verify(forward, expected, "sorted forward");
                verify(reverse, reversed, "sorted reverse");
                verify(reverse.reversed(), expected, "sorted twice reversed");
                strict(reverse, 999, "sorted reverse");
                if (reversed.size() >= 4) {
                    Integer from = reversed.get(1), to = reversed.get(reversed.size() - 1);
                    SortedSet<Integer> range = reverse.subSet(from, to);
                    List<Integer> rangeExpected = reversed.subList(1, reversed.size() - 1);
                    verify(range, rangeExpected, "reverse half-open range");
                    strict(range, 999, "reverse range");
                    verify(range.reversed(), new ArrayList<>(rangeExpected.reversed()),
                            "reverse range reversed again");
                }
                // A custom-order split marked SORTED must still be naturally sorted
                // by Stream.sorted(); a null child comparator would wrongly elide it.
                Spliterator<Integer> child = reverse.spliterator().trySplit();
                if (child != null) {
                    List<Integer> natural = StreamSupport.stream(child, false).sorted().toList();
                    List<Integer> wanted = new ArrayList<>(reversed.subList(0, natural.size()));
                    wanted.sort(null);
                    same(wanted, natural, "natural sort cannot elide a custom-order child");
                }
            }
        }
    }

    private static void liveAndUnconditionalRefusal() {
        for (boolean initiallyEmpty : List.of(false, true)) {
            TreeSet<Integer> source = new TreeSet<>();
            if (!initiallyEmpty) source.addAll(List.of(1, 2, 3, 4));
            long[] callbacks = {0};
            Predicate<Integer> hidden = value -> { callbacks[0]++; return false; };
            SortedSet<Integer> forward = M3FilteredViews.filter(
                    Collections.unmodifiableSortedSet(source), hidden);
            List<SortedSet<Integer>> views = List.of(forward, forward.reversed(),
                    forward.reversed().reversed(), forward.reversed().subSet(4, 1),
                    forward.headSet(3).reversed(), forward.tailSet(1).reversed());
            List<Integer> before = new ArrayList<>(source);
            for (SortedSet<Integer> view : views) {
                callbacks[0] = 0;
                strict(view, 7, "hidden/empty reverse composition");
                same(0L, callbacks[0], "strict refusal before any predicate");
            }
            same(before, new ArrayList<>(source), "refusal preserves source");
        }
        TreeSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4));
        long[] callbacks = {0};
        SortedSet<Integer> reverse = M3FilteredViews.filter(
                Collections.unmodifiableSortedSet(source), value -> {
                    callbacks[0]++;
                    return (value & 1) == 0;
                }).reversed();
        callbacks[0] = 0;
        Spliterator<Integer> deferred = reverse.spliterator();
        same(0L, callbacks[0], "reverse split construction is lazy");
        try { deferred.tryAdvance(null); throw new AssertionError("null action accepted"); }
        catch (NullPointerException expected) { checks++; }
        same(0L, callbacks[0], "null action rejected before traversal");
        source.add(6);
        List<Integer> seen = new ArrayList<>();
        collect(deferred, reverse.comparator(), seen, 0);
        same(List.of(6, 4, 2), seen, "reverse splitting binds after preceding owner writes");
        source.remove(4);
        same(List.of(6, 2), new ArrayList<>(reverse), "existing reverse view remains live");
    }

    private static void nullableNavigationAndRepresentativeIdentity() {
        Comparator<Integer> middleNull = (left, right) -> Integer.compare(
                left == null ? 0 : left, right == null ? 0 : right);
        NavigableSet<Integer> source = new TreeSet<>(middleNull);
        source.addAll(Arrays.asList(-4, -3, -2, -1, null, 1, 2, 3, 4));
        Predicate<Integer> keep = value -> value == null || (value & 1) == 0;
        List<NavigableSet<Integer>> owners = List.of(source, source.descendingSet(),
                source.subSet(-3, true, 3, true),
                source.subSet(-3, true, 3, true).descendingSet());
        for (NavigableSet<Integer> owner : owners) {
            NavigableSet<Integer> actual = M3FilteredViews.filter(owner, keep);
            NavigableSet<Integer> oracle = new TreeSet<>(owner.comparator());
            for (Integer value : owner) if (keep.test(value)) oracle.add(value);
            verify(actual, new ArrayList<>(oracle), "nullable nav");
            verify(actual.reversed(), new ArrayList<>(oracle.descendingSet()), "nullable nav reverse");
            strict(actual.reversed(), null, "nullable nav reverse");
            for (Integer query : Arrays.asList(-7, -4, -1, null, 0, 1, 4, 7)) {
                same(oracle.lower(query), actual.lower(query), "middle-null lower");
                same(oracle.floor(query), actual.floor(query), "middle-null floor");
                same(oracle.ceiling(query), actual.ceiling(query), "middle-null ceiling");
                same(oracle.higher(query), actual.higher(query), "middle-null higher");
            }
        }
        SortedSet<Integer> sortedOnly = M3FilteredViews.filter(
                Collections.unmodifiableSortedSet(source), keep);
        verify(sortedOnly.reversed(), Arrays.asList(4, 2, null, -2, -4),
                "sorted-only whole reversal retains nullable representative");

        String stored = new String("Alpha");
        String alias = new String("ALPHA");
        NavigableSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.add(stored);
        List<String> evaluated = new ArrayList<>();
        SortedSet<String> reversed = M3FilteredViews.filter(
                Collections.unmodifiableSortedSet(names), value -> {
                    evaluated.add(value); return value == stored;
                }).reversed();
        check(reversed.contains(alias), "comparator-equal query finds retained representative");
        check(!evaluated.isEmpty() && evaluated.stream().allMatch(value -> value == stored),
                "predicate receives stored references across reversal");
        check(reversed.getFirst() == stored, "reverse endpoint retains reference identity");
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        sortedReversedRangesAndStreams();
        liveAndUnconditionalRefusal();
        nullableNavigationAndRepresentativeIdentity();
        System.out.println("FILTERED_MERGED_SEQUENCED_PASS checks=" + checks);
    }
}
