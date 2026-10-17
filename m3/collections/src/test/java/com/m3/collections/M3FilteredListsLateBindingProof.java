// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Executable JDK21 contract regression for late-bound live filtered List cursors. */
public final class M3FilteredListsLateBindingProof {
    private static long checks;
    private M3FilteredListsLateBindingProof() { }

    private static void ok(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static void eq(Object expected, Object actual, String reason) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void throwsType(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError("unexpected " + thrown, thrown);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static void firstTraversalBindsToCurrentBacking() {
        var source = new ArrayList<>(List.of(1, 2, 3));
        var probes = new AtomicInteger();
        Predicate<Integer> even = n -> { probes.incrementAndGet(); return (n & 1) == 0; };
        List<Integer> selected = M3FilteredLists.filtered(source, even);
        Spliterator<Integer> pending = selected.spliterator();
        Stream<Integer> pendingStream = selected.stream();
        Stream<Integer> readOnlyStream = M3FilteredLists.readOnlyFiltered(source, even).stream();
        eq(0, probes.get(), "preterminal stream construction must not evaluate predicate");
        eq(Long.MAX_VALUE, pending.estimateSize(), "unknown size before binding");
        eq(Spliterator.ORDERED, pending.characteristics(), "only encounter order is known");
        ok(pending.trySplit() == null, "no eager split buffer");
        throwsType(NullPointerException.class, () -> pending.tryAdvance(null));
        eq(0, probes.get(), "null consumer and metadata must not bind source");

        source.add(0, 0);
        source.add(4);
        eq(List.of(0, 2, 4), pendingStream.toList(), "pending writable stream sees source updates");
        eq(List.of(0, 2, 4), readOnlyStream.toList(), "pending read-only stream sees updates");
        var consumed = new ArrayList<Integer>();
        pending.forEachRemaining(consumed::add);
        eq(List.of(0, 2, 4), consumed, "pending cursor sees source updates");

        Spliterator<Integer> bound = selected.spliterator();
        var first = new ArrayList<Integer>();
        ok(bound.tryAdvance(first::add), "first traversal");
        eq(List.of(0), first, "first element");
        source.add(6);
        throwsType(java.util.ConcurrentModificationException.class,
                () -> bound.tryAdvance(first::add)); // mutation AFTER binding remains fail-fast
    }

    private static void nestedAndReverseMaintainEncounterOrder() {
        var owner = new ArrayList<>(Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1));
        List<Integer> filtered = M3FilteredLists.filtered(owner, n -> n % 2 == 0);
        List<Integer> nested = M3FilteredLists.filtered(filtered, n -> n >= 4);
        var stream = nested.stream();
        owner.add(10); // visible before traversal
        eq(List.of(8, 6, 4, 10), stream.toList(), "nested predicates retain source encounter order");
        eq(List.of(10, 4, 6, 8), nested.reversed(), "reversed view remains backed");
        eq(0, nested.spliterator().characteristics() &
                (Spliterator.SIZED | Spliterator.SUBSIZED |
                 Spliterator.SORTED | Spliterator.DISTINCT), "no false metadata");
        List<Integer> readonly = M3FilteredLists.readOnlyFiltered(nested, n -> n > 0);
        throwsType(UnsupportedOperationException.class, () -> readonly.removeIf(n -> false));
        throwsType(UnsupportedOperationException.class, readonly::clear);
        eq(List.of(8, 6, 4, 10), readonly.stream().toList(), "readonly source order");
    }

    private static void deterministicPreterminalMutationFuzz() {
        var random = new Random(0x4d334c4953543231L);
        for (int round = 0; round < 512; round++) {
            var owner = new ArrayList<Integer>();
            int size = random.nextInt(40);
            for (int i = 0; i < size; i++) owner.add(random.nextInt(101) - 50);
            List<Integer> filtered = M3FilteredLists.filtered(owner, n -> (n & 1) == 0);
            Stream<Integer> stream = filtered.stream();
            Spliterator<Integer> cursor = filtered.spliterator();
            owner.add(0, random.nextInt(101) - 50);
            owner.add(random.nextInt(101) - 50);
            List<Integer> expected = owner.stream().filter(n -> (n & 1) == 0).toList();
            eq(expected, stream.toList(), "late-bound stream seeded round " + round);
            var actual = new ArrayList<Integer>();
            cursor.forEachRemaining(actual::add);
            eq(expected, actual, "late-bound spliterator seeded round " + round);
        }
    }

    public static void main(String[] args) {
        firstTraversalBindsToCurrentBacking();
        nestedAndReverseMaintainEncounterOrder();
        deterministicPreterminalMutationFuzz();
        System.out.println("M3_FILTERED_LIST_LATE_BOUND_PASS checks=" + checks);
    }
}
