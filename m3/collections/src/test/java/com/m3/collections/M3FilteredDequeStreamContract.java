// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent Java 21 oracle for lazy filtered Deque stream demand. */
public final class M3FilteredDequeStreamContract {
    private static long checks;
    private M3FilteredDequeStreamContract() { }

    private static void check(boolean passed, String reason) {
        checks++;
        if (!passed) throw new AssertionError(reason + " #" + checks);
    }

    private static void equal(Object expected, Object actual, String reason) {
        check(java.util.Objects.equals(expected, actual),
                reason + ": expected=" + expected + " actual=" + actual);
    }

    private static void demandAndLateBinding() {
        Deque<Integer> owner = new ArrayDeque<>();
        for (int i = 0; i < 20000; i++) owner.addLast(i);
        AtomicInteger visits = new AtomicInteger();
        Deque<Integer> selected = M3FilteredDeques.filtered(owner, value -> {
            visits.incrementAndGet();
            return value % 3 == 0;
        });
        Spliterator<Integer> cursor = selected.spliterator();
        check(visits.get() == 0, "spliterator creation evaluated predicates");
        check(cursor.estimateSize() == Long.MAX_VALUE,
                "filtered live view must not claim an eagerly computed size");
        check(cursor.characteristics() == Spliterator.ORDERED,
                "encounter order must be retained without SIZED/SUBSIZED");
        check(cursor.trySplit() == null, "stream cursor remains single-owner");
        owner.addFirst(-3); // Mutation before first demand must be visible.
        List<Integer> captured = new ArrayList<>();
        check(cursor.tryAdvance(captured::add), "first item missing");
        equal(List.of(-3), captured, "late-bound first element");
        check(visits.get() == 1, "first demand must be one predicate");
        visits.set(0);
        equal(-3, selected.stream().findFirst().orElseThrow(), "short-circuit stream");
        check(visits.get() == 1, "stream.findFirst performed a hidden full scan");

        visits.set(0);
        Deque<Integer> readOnly = M3FilteredDeques.readOnlyFiltered(owner, value -> {
            visits.incrementAndGet();
            return value == -3;
        });
        Spliterator<Integer> readonlyCursor = readOnly.spliterator();
        check(visits.get() == 0 && readonlyCursor.characteristics() == Spliterator.ORDERED,
                "read-only wrapper must retain lazy cursor metadata");
        equal(-3, readOnly.stream().findFirst().orElseThrow(), "read-only short circuit");
        check(visits.get() == 1, "read-only wrapper performed a full scan");
        check(readOnly.stream().count() == 1L, "read-only complete stream count");
        check(readOnly.reversed().stream().findFirst().orElseThrow() == -3,
                "reverse live view");
    }

    private static void nullsDuplicatesAndConsumerExceptions() {
        Deque<Integer> owner = new LinkedList<>(Arrays.asList(null, 4, null, 4, 3));
        Deque<Integer> selected = M3FilteredDeques.filtered(owner,
                item -> item == null || item == 4);
        equal(Arrays.asList(null, 4, null, 4), selected.stream().toList(),
                "null values and duplicate occurrences");
        equal(Arrays.asList(4, null, 4, null), selected.reversed().stream().toList(),
                "reversed duplicate and null order");

        Deque<Integer> numbers = M3FilteredDeques.filtered(
                new ArrayDeque<>(List.of(2, 4, 6)), n -> true);
        Spliterator<Integer> cursor = numbers.spliterator();
        RuntimeException witness = new RuntimeException("consumer witness");
        try {
            cursor.tryAdvance(item -> { throw witness; });
            throw new AssertionError("expected consumer failure");
        } catch (RuntimeException exception) {
            check(exception == witness, "consumer throwable identity must be preserved");
        }
        List<Integer> after = new ArrayList<>();
        cursor.forEachRemaining(after::add);
        equal(List.of(4, 6), after, "consumer failure consumed exactly the first row");
    }

    private static void randomizedEncounterOrder() {
        for (int seed = 0; seed < 96; seed++) {
            Random random = new Random(0x4d330002L + seed);
            Deque<Integer> owner = new LinkedList<>();
            for (int i = 0; i < 128; i++) {
                owner.addLast(random.nextInt(16) == 0 ? null : random.nextInt(37) - 18);
            }
            int divisor = 1 + seed % 11, residue = seed % divisor;
            Deque<Integer> selected = M3FilteredDeques.filtered(owner,
                    item -> item == null || Math.floorMod(item, divisor) == residue);
            List<Integer> expected = new ArrayList<>();
            for (Integer item : owner) {
                if (item == null || Math.floorMod(item, divisor) == residue) expected.add(item);
            }
            equal(expected, selected.stream().toList(), "stream identity and order");
            equal(expected, M3FilteredDeques.readOnlyFiltered(owner,
                    item -> item == null || Math.floorMod(item, divisor) == residue)
                    .stream().toList(), "read-only stream parity");
            List<Integer> reversed = new ArrayList<>(expected);
            java.util.Collections.reverse(reversed);
            equal(reversed, selected.reversed().stream().toList(), "reversed selection order");
        }
    }

    public static void main(String[] args) {
        demandAndLateBinding();
        nullsDuplicatesAndConsumerExceptions();
        randomizedEncounterOrder();
        System.out.println("M3_FILTERED_DEQUE_STREAM_PASS checks=" + checks);
    }
}
