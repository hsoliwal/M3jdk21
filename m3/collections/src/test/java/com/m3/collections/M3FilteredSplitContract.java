/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/** Recursive split oracle, live-view checks and callback-order compatibility. */
public final class M3FilteredSplitContract {
    private static int checks;
    private M3FilteredSplitContract() { }
    private static final class CountingSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private int getterCalls;
        private boolean failGetter;
        CountingSet() { super(Comparator.reverseOrder()); }
        @Override public Comparator<? super Integer> comparator() {
            getterCalls++;
            if (failGetter) throw new IllegalStateException("comparator getter");
            return super.comparator();
        }
    }
    private static void check(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError(description);
    }
    private static SortedSet<Integer> view(int mode, NavigableSet<Integer> source,
                                          Predicate<Integer> predicate) {
        return switch (mode) {
            case 0 -> M3FilteredViews.filter((SortedSet<Integer>) source, predicate);
            case 1 -> M3FilteredViews.filter(source, predicate);
            case 2 -> M3FilteredNavigableSet.view(source, predicate);
            case 3 -> M3MutableFilteredViews.filter(source, predicate);
            case 4 -> {
                TreeMap<Integer, Integer> map = new TreeMap<>(source.comparator());
                for (Integer value : source) map.put(value, value);
                yield M3MutableFilteredViews.filterEntries(map, (k, v) -> predicate.test(k)).navigableKeySet();
            }
            default -> throw new AssertionError(mode);
        };
    }
    private static <E> void collect(Spliterator<E> split, Comparator<? super E> comparator,
                                   List<E> output, int depth) {
        check(depth < 32, "bounded split depth");
        check(split.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED | Spliterator.DISTINCT),
                "sorted characteristics retained");
        check(split.getComparator() == comparator, "split comparator identity");
        Spliterator<E> prefix = split.trySplit();
        if (prefix != null) collect(prefix, comparator, output, depth + 1);
        split.forEachRemaining(output::add);
    }
    private static void shapes() {
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.reverseOrder()));
        for (Comparator<Integer> order : orders) {
            for (int mode = 0; mode < 5; mode++) {
                for (int length : new int[] {0, 1, 17, 127, 1025, 4097}) {
                    TreeSet<Integer> source = new TreeSet<>(order);
                    for (int i = 0; i < length; i++) source.add(i - length / 2);
                    if (order != null && order != Comparator.<Integer>reverseOrder()) source.add(null);
                    SortedSet<Integer> filtered = view(mode, source, x -> x == null || (x & 1) == 0);
                    List<Integer> expected = new ArrayList<>(filtered);
                    List<Integer> actual = new ArrayList<>();
                    collect(filtered.spliterator(), filtered.comparator(), actual, 0);
                    check(actual.equals(expected), "split order, membership, nulls and duplicates");
                    check(filtered.parallelStream().toList().equals(expected), "parallel stream order");
                    Spliterator<Integer> child = filtered.spliterator().trySplit();
                    if (child != null && !expected.contains(null)) {
                        List<Integer> sorted = StreamSupport.stream(child, false).sorted().toList();
                        // Natural sorted() must not elide work for a reverse-order child.
                        List<Integer> want = new ArrayList<>(expected.subList(0, sorted.size()));
                        want.sort(null);
                        check(sorted.equals(want), "natural stream sort over split");
                    }
                    if (filtered instanceof NavigableSet<Integer> nav) {
                        NavigableSet<Integer> descending = nav.descendingSet();
                        actual.clear(); collect(descending.spliterator(), descending.comparator(), actual, 0);
                        check(actual.equals(new ArrayList<>(descending)), "descending recursive split");
                        if (!expected.isEmpty()) {
                            SortedSet<Integer> range = filtered.tailSet(expected.get(expected.size() / 2));
                            actual.clear(); collect(range.spliterator(), range.comparator(), actual, 0);
                            check(actual.equals(new ArrayList<>(range)), "range recursive split");
                        }
                    }
                }
            }
        }
    }
    private static void effects() {
        for (int mode = 0; mode < 4; mode++) {
            TreeSet<Integer> source = new TreeSet<>(Comparator.reverseOrder());
            source.addAll(List.of(1, 2, 3, 4));
            List<Integer> effects = new ArrayList<>();
            SortedSet<Integer> filtered = view(mode, source, x -> { effects.add(x); return x % 2 == 0; });
            Spliterator<Integer> split = filtered.spliterator();
            check(effects.isEmpty(), "construction is lazy");
            source.add(6);
            List<Integer> values = new ArrayList<>();
            split.tryAdvance(values::add);
            List<Integer> actualEffects = List.copyOf(effects);
            effects.clear();
            Spliterator<Integer> old = Spliterators.spliterator(filtered,
                    Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT);
            old.tryAdvance(x -> { });
            check(effects.equals(actualEffects), "same iterator/size callback order");
            check(values.equals(List.of(6)), "late binding sees preceding backing change");
            int before = effects.size();
            try { filtered.spliterator().tryAdvance(null); throw new AssertionError("null action accepted"); }
            catch (NullPointerException expected) { check(effects.size() == before, "null action before callbacks"); }
            if (mode < 3) {
                source.add(8);
                try { split.tryAdvance(x -> { }); throw new AssertionError("lost fail fast"); }
                catch (ConcurrentModificationException expected) { checks++; }
                try { filtered.clear(); throw new AssertionError("readonly mutation accepted"); }
                catch (UnsupportedOperationException expected) { checks++; }
            }
        }
        for (int mode = 0; mode < 4; mode++) {
            CountingSet source = new CountingSet(); source.addAll(List.of(1, 2, 3, 4));
            SortedSet<Integer> filtered = view(mode, source, x -> true);
            source.getterCalls = 0;
            Spliterator<Integer> split = filtered.spliterator();
            check(source.getterCalls == 0, "comparator getter remains lazy");
            check(split.getComparator() == Comparator.<Integer>reverseOrder(), "owner comparator");
            check(source.getterCalls == 1, "root delegates one getter call");
            Spliterator<Integer> child = split.trySplit();
            check(child != null, "nonempty child");
            int before = source.getterCalls;
            child.getComparator();
            check(source.getterCalls == before + 1, "child delegates one getter call");
            source.failGetter = true;
            Spliterator<Integer> lazy = filtered.spliterator();
            try { lazy.getComparator(); throw new AssertionError("getter exception lost"); }
            catch (IllegalStateException expected) { checks++; }
        }
    }
    public static void main(String[] args) {
        shapes(); effects();
        System.out.println("FILTERED_SPLITS_PASS checks=" + checks);
    }
}
