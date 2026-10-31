/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Actual-owner callback and sorted/filter oracle; not an installed-image claim. */
public final class M3LiveViewContinuationTest {
    private static int assertions;
    private M3LiveViewContinuationTest() { }
    private static void check(boolean result, String reason) {
        assertions++;
        if (!result) throw new AssertionError(reason);
    }
    private static void callbacks(String mode) {
        AtomicReference<Spliterator<Integer>> cursor = new AtomicReference<>();
        List<Integer> delivered = new ArrayList<>();
        RuntimeException failure = new RuntimeException("exact nested failure");
        if (mode.equals("predicate")) {
            cursor.set(M3CollectionViews.filtered(new ArrayList<>(List.of(1, 2, 3)), value -> {
                if (value == 1) check(cursor.get().tryAdvance(delivered::add), "nested predicate advance");
                return true;
            }).spliterator());
            check(cursor.get().tryAdvance(delivered::add), "outer delivery");
            check(delivered.equals(List.of(2, 1)), "restore enclosing consumer");
            cursor.get().forEachRemaining(delivered::add);
            check(delivered.equals(List.of(2, 1, 3)), "remaining traversal");
        } else if (mode.equals("exhaustion")) {
            cursor.set(M3CollectionViews.filtered(new ArrayList<>(List.of(1)), value -> true).spliterator());
            boolean actual = cursor.get().tryAdvance(value -> {
                delivered.add(value);
                check(!cursor.get().tryAdvance(delivered::add), "nested end");
            });
            check(actual, "outer true after nested exhaustion");
            check(delivered.equals(List.of(1)), "one delivery");
        } else if (mode.equals("exception")) {
            cursor.set(M3CollectionViews.filtered(new ArrayList<>(List.of(1, 2, 3)), value -> {
                if (value == 1) {
                    try {
                        cursor.get().tryAdvance(nested -> { throw failure; });
                        throw new AssertionError("nested failure absent");
                    } catch (RuntimeException thrown) { check(thrown == failure, "exception identity"); }
                }
                return true;
            }).spliterator());
            check(cursor.get().tryAdvance(delivered::add), "resume outer after caught failure");
            check(delivered.equals(List.of(1)), "correct resumed consumer");
            check(cursor.get().tryAdvance(delivered::add), "later independent call");
            check(delivered.equals(List.of(1, 3)), "exact consumed occurrences");
        } else if (mode.equals("null")) {
            cursor.set(M3CollectionViews.filtered(new ArrayList<>(List.of(1)), value -> {
                try { cursor.get().tryAdvance(null); throw new AssertionError("missing null check"); }
                catch (NullPointerException expected) { assertions++; }
                return true;
            }).spliterator());
            check(cursor.get().tryAdvance(delivered::add), "null callback preserved enclosing state");
            check(delivered.equals(List.of(1)), "null check order");
        } else throw new IllegalArgumentException(mode);
    }
    private static void ordinary() {
        Random random = new Random(0x4d33564945574cL);
        for (int trial = 0; trial < 1000; trial++) {
            Comparator<Integer> order = trial % 2 == 0 ? Comparator.naturalOrder() : Comparator.reverseOrder();
            TreeSet<Integer> source = new TreeSet<>(order);
            for (int i = 0; i < 40; i++) source.add(random.nextInt(100));
            Predicate<Integer> predicate = value -> (value & 3) != 1;
            NavigableSet<Integer> view = M3CollectionViews.filtered(source, predicate);
            TreeSet<Integer> oracle = new TreeSet<>(order);
            source.stream().filter(predicate).forEach(oracle::add);
            check(new ArrayList<>(view).equals(new ArrayList<>(oracle)), "ordered filter");
            check(new ArrayList<>(view.descendingSet()).equals(new ArrayList<>(oracle.descendingSet())), "descending");
            for (int key = -1; key <= 101; key += 3) {
                check(Objects.equals(view.lower(key), oracle.lower(key)), "lower");
                check(Objects.equals(view.floor(key), oracle.floor(key)), "floor");
                check(Objects.equals(view.ceiling(key), oracle.ceiling(key)), "ceiling");
                check(Objects.equals(view.higher(key), oracle.higher(key)), "higher");
            }
            Spliterator<Integer> spliterator = view.spliterator();
            check(spliterator.hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.ORDERED), "metadata");
            check(!spliterator.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED), "unsized");
            check(spliterator.getComparator() == source.comparator(), "same comparator");
            List<Integer> traversal = new ArrayList<>();
            Spliterator<Integer> split = spliterator.trySplit();
            if (split != null) split.forEachRemaining(traversal::add);
            spliterator.forEachRemaining(traversal::add);
            check(traversal.equals(new ArrayList<>(oracle)), "split traversal");
            if (!view.isEmpty()) {
                Integer first = view.first();
                check(view.remove(first) && !source.contains(first), "write through");
            }
        }
        ArrayList<String> source = new ArrayList<>(Arrays.asList(null, "b", "a", "b", "hidden"));
        Collection<String> view = M3CollectionViews.filtered(source, v -> !"hidden".equals(v));
        check(new ArrayList<>(view).equals(Arrays.asList(null,"b","a","b")), "null duplicates");
        List<String> sorted = M3CollectionViews.filteredSortedSnapshot(source, v -> !"hidden".equals(v), Comparator.nullsFirst(Comparator.naturalOrder()));
        source.clear();
        check(view.isEmpty(), "live owner");
        check(sorted.equals(Arrays.asList(null,"a","b","b")), "detached stable snapshot");
        try { sorted.add("x"); throw new AssertionError("snapshot writable"); }
        catch (UnsupportedOperationException expected) { assertions++; }
        // Retain the newer balanced conjunction owner and callback order/short circuit.
        ArrayList<Integer> calls = new ArrayList<>();
        Collection<Integer> deep = new ArrayList<>(List.of(7));
        for (int i = 0; i < 8192; i++) {
            int step = i;
            deep = M3CollectionViews.filtered(deep, v -> { calls.add(step); return step < 4096; });
        }
        check(deep.isEmpty(), "deep filter rejection");
        check(calls.size() == 4097, "short circuit count");
        for (int i = 0; i < calls.size(); i++) check(calls.get(i) == i, "predicate order");
    }
    private static final class CowSubclass extends java.util.concurrent.CopyOnWriteArrayList<Integer> {
        private static final long serialVersionUID = 1L;
        int overrides;
        CowSubclass() { super(List.of(1, 2)); }
        @Override public boolean removeIf(Predicate<? super Integer> predicate) {
            overrides++;
            return super.removeIf(predicate);
        }
    }
    private static void currentOwnerRepairs() {
        for (int kind = 0; kind < 2; kind++) {
            Collection<Integer> owner = kind == 0
                    ? new java.util.concurrent.CopyOnWriteArrayList<>(List.of(1, 2, 3, 4))
                    : new java.util.concurrent.CopyOnWriteArraySet<>(List.of(1, 2, 3, 4));
            Collection<Integer> view = M3CollectionViews.filtered(owner, v -> v % 2 == 0);
            check(view.removeAll(List.of(2, 3)), "COW visible bulk remove");
            check(new ArrayList<>(owner).equals(List.of(1, 3, 4)), "hidden COW occurrence survives");
            check(view.addAll(List.of(6, 8)), "COW admitted addAll");
            check(view.retainAll(List.of(4, 8)), "COW retain visible only");
            check(new ArrayList<>(owner).equals(List.of(1, 3, 4, 8)), "COW stable retained owner");
            view.clear();
            check(new ArrayList<>(owner).equals(List.of(1, 3)), "COW visible clear");
            check(view.isEmpty(), "COW filtered empty");
            check(!view.removeIf(v -> true), "empty COW no change");
        }
        CowSubclass subclass = new CowSubclass();
        try {
            M3CollectionViews.filtered(subclass, v -> true).clear();
            throw new AssertionError("subclass iterator contract changed");
        } catch (UnsupportedOperationException expected) { assertions++; }
        check(subclass.overrides == 0, "exact-class dispatch preserves subclass");
        check(subclass.equals(List.of(1, 2)), "subclass not mutated");
        int[] visits = {0};
        Collection<Integer> view = M3CollectionViews.filtered(new ArrayList<>(List.of(1, 2, 3)), v -> { visits[0]++; return v == 2; });
        check(!view.isEmpty() && visits[0] == 2, "isEmpty short circuit retained");
    }
    public static void main(String[] args) {
        if (args.length == 1) callbacks(args[0]);
        else { callbacks("predicate"); callbacks("exhaustion"); callbacks("exception"); callbacks("null"); ordinary(); currentOwnerRepairs(); }
        System.out.println("PASS LIVE_VIEWS assertions=" + assertions);
    }
}
