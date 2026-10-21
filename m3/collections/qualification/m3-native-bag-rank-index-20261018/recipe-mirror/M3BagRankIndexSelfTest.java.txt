/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic M3-native order-statistics and Java/JNI-masks oracle. */
public final class M3BagRankIndexSelfTest {
    private static long checks;
    private static final Comparator<Integer> NATURAL = Comparator.nullsFirst(Integer::compareTo);
    private static final Comparator<Integer> REVERSE = NATURAL.reversed();

    private M3BagRankIndexSelfTest() { }
    private static void require(boolean yes, String text) {
        checks++;
        if (!yes) throw new AssertionError(text + " check=" + checks);
    }
    private static void eq(Object expected, Object actual, String text) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(text + " expected=" + expected + " actual=" + actual + " check=" + checks);
        }
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action, String text) {
        checks++;
        try { action.run(); }
        catch (Throwable actual) {
            if (expected.isInstance(actual)) return;
            throw new AssertionError(text + " wrong throwable=" + actual, actual);
        }
        throw new AssertionError(text + " did not refuse");
    }
    private static <E> List<E> expanded(M3BagRankIndex<E> index) {
        ArrayList<E> result = new ArrayList<>();
        for (E value : index) result.add(value);
        return result;
    }
    private static void small(boolean nativeRequired) {
        M3CountedBag<String> owner = new M3CountedBag<>();
        owner.addOccurrences("b", 3);
        owner.addOccurrences("a", 2);
        owner.addOccurrences("z", 1);
        M3BagRankIndex<String> rank = M3BagRankIndex.snapshot(owner, null);
        eq(3, rank.distinctSize(), "groups");
        eq(6L, rank.exactSize(), "occurrences");
        eq(List.of("a", "a", "b", "b", "b", "z"), expanded(rank), "ascending");
        eq(2L, rank.occurrencesBefore("b"), "rank < b");
        eq(5L, rank.occurrencesThrough("b"), "rank <= b");
        eq("b", rank.occurrenceAt(4), "select last b");
        eq("a", rank.distinctAt(0), "first group");
        eq(3, rank.occurrencesAtDistinct(1), "count b");
        eq(3, rank.exactCount("b"), "exact count b");
        require(rank.contains("b") && !rank.contains("x"), "indexed contains");
        eq(3L, rank.comparatorCount("b"), "comparator count b");
        eq(2, rank.lowerBound("c"), "lowerBound c");
        eq(2, rank.upperBound("b"), "upperBound b");
        eq(List.of("b", "b", "b"), expanded(rank.slice(1, 2)), "zero-copy index slice");
        eq(List.of("b", "b", "b"), expanded(rank.subRange("b", true, "b", true)), "closed range");
        eq(List.of(), expanded(rank.subRange("b", false, "b", true)), "empty point range");
        eq(List.of("z", "b", "b", "b", "a", "a"), expanded(rank.reversed()), "descending");
        eq(1L, rank.reversed().occurrencesBefore("b"), "descending rank");
        eq(4L, rank.reversed().occurrencesThrough("b"), "descending through");
        eq(List.of("a", "a", "z"), expanded(rank.filtered(x -> !"b".equals(x))), "predicate groups");
        byte[] states = {(byte) 0x81, 0x02, (byte) 0x80};
        eq(List.of("a", "a", "z"), expanded(rank.selected(states, 128, 2,
                M3IndexMaskCompactor.Backend.JAVA)), "8-bit mask flags");
        eq(List.of("a", "a", "z"), expanded(rank.selected(states, 128, 2,
                nativeRequired ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                        : M3IndexMaskCompactor.Backend.JAVA)), "Java/native masks parity");
        eq(List.of("a", "a", "z"), expanded(rank.selected(states, 128, 2,
                M3IndexMaskCompactor.Backend.JAVA).slice(0, 2)), "masked slicing");
        M3BagRankIndex<String> previouslyCreated = rank;
        owner.addOccurrences("b", 100);
        owner.addOccurrences("new", 19);
        eq(6L, previouslyCreated.exactSize(), "snapshot independent of owner writes");
        eq("b", previouslyCreated.occurrenceAt(3), "snapshot retained original object");
        fails(IndexOutOfBoundsException.class, () -> rank.occurrenceAt(6), "end bound");
        fails(IndexOutOfBoundsException.class, () -> rank.occurrenceAt(-1), "negative rank");
        fails(IndexOutOfBoundsException.class, () -> rank.slice(0, 4), "slice overflow");
        fails(IllegalArgumentException.class, () -> rank.subRange("z", true, "a", true), "range inverted");
        fails(IllegalArgumentException.class, () -> rank.selected(states, 1, 1,
                M3IndexMaskCompactor.Backend.JAVA), "mask conflicts");
        fails(IllegalArgumentException.class, () -> rank.selected(new byte[0], 0, 0,
                M3IndexMaskCompactor.Backend.JAVA), "mask alignment");
        fails(UnsupportedOperationException.class, () -> rank.add("c"), "add refused");
        fails(UnsupportedOperationException.class, () -> rank.remove("a"), "remove refused");
        fails(UnsupportedOperationException.class, () -> rank.addAll(List.of()), "no-op addAll refused");
        fails(UnsupportedOperationException.class, () -> rank.removeIf(x -> false), "no-op removeIf refused");
        fails(UnsupportedOperationException.class, rank::clear, "clear refused");
        Iterator<String> iterator = rank.iterator();
        iterator.next();
        fails(UnsupportedOperationException.class, iterator::remove, "iterator remove refused");
        Spliterator<String> split = rank.spliterator();
        eq(Spliterator.ORDERED | Spliterator.IMMUTABLE, split.characteristics(), "spliterator claims");
        require(split.trySplit() == null, "no payload split");
        eq(6L, split.estimateSize(), "initial unsplit estimate");
        split.tryAdvance(x -> {});
        eq(5L, split.estimateSize(), "decreasing unsplit estimate");
    }

    private static void tiesAndNulls() {
        M3CountedBag<String> bag = new M3CountedBag<>();
        bag.addOccurrences("b", 1);
        bag.addOccurrences("B", 2);
        bag.addOccurrences(null, 3);
        bag.addOccurrences("a", 4);
        Comparator<String> cmp = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        var view = M3BagRankIndex.snapshot(bag, cmp);
        eq(List.of("a", "a", "a", "a", "b", "B", "B"),
                expanded(view.slice(1, 4)), "stable comparator ties");
        eq(3L, view.comparatorCount("B"), "comparator-equivalent counts");
        eq(1, view.exactCount("b"), "stored b count");
        eq(2, view.exactCount("B"), "stored B count");
        eq(3L, view.occurrencesBefore("a"), "nulls-first rank");
        eq(7L, view.occurrencesThrough("a"), "nulls-first <=a");
        eq(null, view.occurrenceAt(1), "null select");
        eq(2, view.lowerBound("b"), "first b after null,a"); // corrected below if comparator tie starts at 2 distinct
        eq(Arrays.asList(null, null, null), expanded(view.slice(0, 1)), "null group");
        var reverse = view.reversed();
        eq(3L, reverse.comparatorCount("B"), "reverse tie count");
        eq("B", reverse.occurrenceAt(0), "reverse stable order of groups");
    }

    private static void hugeCounts() {
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        bag.addOccurrences(1, Integer.MAX_VALUE);
        bag.addOccurrences(2, Integer.MAX_VALUE);
        var ranks = M3BagRankIndex.snapshot(bag, null);
        eq(4294967294L, ranks.exactSize(), "long total");
        eq(Integer.MAX_VALUE, ranks.size(), "collection saturated int size");
        eq(2147483647L, ranks.occurrencesBefore(2), "long rank count");
        eq(2, ranks.occurrenceAt(4294967293L), "long occurrence selection");
        eq(4294967294L, ranks.reversed().exactSize(), "reverse total long");
        eq(1, ranks.reversed().occurrenceAt(4294967293L), "reverse long selection");
    }

    private static void randomized(boolean nativeRequired) {
        Random rng = new Random(0x4d3342414752414eL);
        long scenarios = 0;
        for (int trial = 0; trial < 1200; trial++) {
            Comparator<Integer> cmp = (trial & 1) == 0 ? NATURAL : REVERSE;
            M3CountedBag<Integer> source = new M3CountedBag<>();
            LinkedHashMap<Integer, Integer> reference = new LinkedHashMap<>();
            for (int j = 0, operations = 15 + rng.nextInt(65); j < operations; j++) {
                Integer value = rng.nextInt(11) == 0 ? null : rng.nextInt(61) - 30;
                int amount = 1 + rng.nextInt(9);
                source.addOccurrences(value, amount);
                reference.merge(value, amount, Integer::sum);
            }
            var sorted = M3BagRankIndex.snapshot(source, cmp);
            List<Map.Entry<Integer, Integer>> groups = new ArrayList<>(reference.entrySet());
            groups.sort((a, b) -> cmp.compare(a.getKey(), b.getKey()));
            eq(groups.size(), sorted.distinctSize(), "distinct groups randomized");
            long occurrenceCount = 0;
            for (int i = 0; i < groups.size(); i++) {
                Map.Entry<Integer, Integer> group = groups.get(i);
                eq(group.getKey(), sorted.distinctAt(i), "distinct representative order");
                eq(group.getValue(), sorted.occurrencesAtDistinct(i), "count retention");
                occurrenceCount += group.getValue();
                eq(occurrenceCount, sorted.occurrencesThrough(group.getKey()), "last through rank");
            }
            eq(occurrenceCount, sorted.exactSize(), "total count randomized");
            for (int i = 0; i < 50; i++) {
                long rank = rng.nextInt((int) occurrenceCount);
                long left = 0;
                Integer expect = null;
                for (var group : groups) {
                    left += group.getValue();
                    if (rank < left) { expect = group.getKey(); break; }
                }
                eq(expect, sorted.occurrenceAt(rank), "rank-select random");
            }
            byte[] flags = new byte[groups.size()];
            List<Integer> selected = new ArrayList<>();
            int required = 2, forbidden = 4;
            for (int i = 0; i < flags.length; i++) {
                flags[i] = (byte) rng.nextInt(256);
                int state = flags[i] & 0xff;
                if ((state & required) == required && (state & forbidden) == 0) {
                    for (int n = groups.get(i).getValue(); n > 0; n--) selected.add(groups.get(i).getKey());
                }
            }
            eq(selected, expanded(sorted.selected(flags, required, forbidden,
                    M3IndexMaskCompactor.Backend.JAVA)), "mask selected occurrence order");
            if (nativeRequired && trial < 90) {
                eq(selected, expanded(sorted.selected(flags, required, forbidden,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)), "actual JNI mask parity");
            }
            var reversed = sorted.reversed();
            eq(sorted.exactSize(), reversed.exactSize(), "reverse occurrence sum");
            eq(sorted.distinctSize(), reversed.distinctSize(), "reverse distinct");
            for (int i = 0; i < groups.size(); i++) {
                eq(sorted.distinctAt(groups.size() - i - 1), reversed.distinctAt(i), "reversed distinct");
            }
            scenarios++;
        }
        System.out.println("M3BagRankIndex randomized scenarios=" + scenarios);
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1 && args[0].equals("--native");
        if (args.length > 1 || args.length == 1 && !nativeRequired)
            throw new IllegalArgumentException("usage: [--native]");
        if (nativeRequired && !M3IndexMaskCompactor.nativeAvailable())
            throw new AssertionError("JNI requested but unavailable");
        small(nativeRequired);
        tiesAndNulls();
        hugeCounts();
        randomized(nativeRequired);
        System.out.println("PASS M3BagRankIndexSelfTest checks=" + checks + " native=" + nativeRequired);
    }
}
