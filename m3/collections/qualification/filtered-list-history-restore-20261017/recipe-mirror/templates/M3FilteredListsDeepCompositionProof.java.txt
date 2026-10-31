// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Deep filter stack and semantic-order oracle, executable without JUnit. */
public final class M3FilteredListsDeepCompositionProof {
    private static long checks;
    private M3FilteredListsDeepCompositionProof() { }
    private static void check(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError(description);
    }
    private static void eq(Object expected, Object actual, String description) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static List<Integer> deep(List<Integer> source, int count,
            Predicate<Integer> rule) {
        List<Integer> result = source;
        for (int i = 0; i < count; i++) result = M3FilteredLists.filtered(result, rule);
        return result;
    }
    private static void depth20000AndLateBoundSnapshot() {
        var source = new ArrayList<>(Arrays.asList(-5, 0, 2, 6));
        AtomicInteger calls = new AtomicInteger();
        List<Integer> view = deep(source, 20_000, n -> {
            calls.incrementAndGet();
            return true;
        });
        check(calls.get() == 0, "construction must not call predicates");
        Stream<Integer> pending = view.stream();
        Spliterator<Integer> split = view.spliterator();
        check(calls.get() == 0, "stream/spliterator metadata must be lazy");
        check(split.estimateSize() == Long.MAX_VALUE, "filtered traversal is unsized");
        check(split.characteristics() == Spliterator.ORDERED, "only encounter order is known");
        check(split.trySplit() == null, "no extra split array");
        source.add(7);
        eq(List.of(-5, 0, 2, 6, 7), pending.toList(), "preterminal source change");
        var observed = new ArrayList<Integer>();
        split.forEachRemaining(observed::add);
        eq(source, observed, "late-bound spliterator source change");
        eq(5, view.size(), "deep size");
        eq(0, view.get(1), "deep index");
        check(calls.get() >= 7 * 20_000, "full-depth predicate evaluation");
    }
    private static void orderingRefusalAndExceptions() {
        var owner = new ArrayList<>(List.of(11));
        var order = new ArrayList<Integer>();
        List<Integer> view = owner;
        for (int slot = 0; slot < 256; slot++) {
            final int index = slot;
            view = M3FilteredLists.filtered(view, n -> {
                order.add(index);
                if (index > 37) throw new AssertionError("predicate executed after refusal");
                return index != 37;
            });
        }
        eq(0, view.size(), "short-circuit before any later predicate");
        eq(38, order.size(), "exact number of executed predicates");
        for (int i = 0; i <= 37; i++) eq(i, order.get(i), "left-to-right order " + i);
        order.clear();
        List<Integer> throwing = owner;
        for (int slot = 0; slot < 256; slot++) {
            final int index = slot;
            throwing = M3FilteredLists.filtered(throwing, n -> {
                order.add(index);
                if (index == 63) throw new IllegalStateException("user predicate 63");
                return true;
            });
        }
        try {
            throwing.size();
            throw new AssertionError("missing predicate exception");
        } catch (IllegalStateException expected) {
            eq("user predicate 63", expected.getMessage(), "exception identity");
        }
        eq(64, order.size(), "no predicates after thrown exception");
    }
    private static void sortedAndMutationDifferential() {
        TreeSet<Integer> sorted = new TreeSet<>(Comparator.reverseOrder());
        sorted.addAll(List.of(8, 7, 6, 5, 4, 3, 2, 1));
        List<Integer> backing = new ArrayList<>(sorted);
        List<Integer> view = deep(backing, 2_048, n -> n % 2 == 0);
        eq(List.of(8, 6, 4, 2), view, "source comparator encounter order");
        eq(List.of(2, 4, 6, 8), view.reversed(), "reversed List cursor");
        view.set(1, 10);
        eq(List.of(8, 7, 10, 5, 4, 3, 2, 1), backing, "write-through physical positions");
        view.add(2, 12);
        eq(List.of(8, 10, 12, 4, 2), view, "accepted insertion");
        eq(12, view.remove(2), "selected index removal");
        eq(List.of(8, 10, 4, 2), view, "source remains live");
        boolean rejected = false;
        try { view.add(2, 3); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "rejected writes must fail before modification");
        List<Integer> readonly = M3FilteredLists.readOnlyFiltered(view, n -> true);
        rejected = false;
        try { readonly.removeFirst(); }
        catch (UnsupportedOperationException expected) { rejected = true; }
        check(rejected, "read-only mutation refusal");
        check((view.spliterator().characteristics() &
                (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.SORTED |
                        Spliterator.DISTINCT)) == 0, "metadata refusal");
    }
    private static void randomizedShortCircuit() {
        Random random = new Random(0x4d33535441434b53L);
        for (int round = 0; round < 128; round++) {
            var owner = new ArrayList<Integer>();
            for (int i = 0, n = random.nextInt(40); i < n; i++) owner.add(random.nextInt(101) - 50);
            List<Integer> nested = owner;
            Predicate<Integer> rule = n -> (n & 1) == 0;
            for (int depth = 0; depth < 256; depth++) nested = M3FilteredLists.filtered(nested, rule);
            eq(owner.stream().filter(rule).toList(), nested.stream().toList(),
                    "deterministic nested filter " + round);
        }
    }
    public static void main(String[] args) {
        depth20000AndLateBoundSnapshot();
        orderingRefusalAndExceptions();
        sortedAndMutationDifferential();
        randomizedShortCircuit();
        System.out.println("M3_FILTERED_LIST_DEEP_PLAN_PASS checks=" + checks);
    }
}
