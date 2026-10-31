// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Standalone Java 21 contract oracle for repeated strict read-only filters. */
public final class M3FilteredListsReadonlyDepthProof {
    private static int checks;
    private M3FilteredListsReadonlyDepthProof() { }

    private static void check(boolean result, String cause) {
        checks++;
        if (!result) throw new AssertionError(cause);
    }
    private static void equalsTo(Object expected, Object actual, String cause) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(cause + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void refuses(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("wrong mutation failure", ex);
        }
        throw new AssertionError("mutation was not rejected: " + type);
    }
    private static void deepReadOnlyChainAndLateBinding() {
        var source = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        AtomicInteger calls = new AtomicInteger();
        List<Integer> view = source;
        for (int i = 0; i < 20_000; i++) {
            final int index = i;
            view = M3FilteredLists.readOnlyFiltered(view, value -> {
                calls.incrementAndGet();
                return index != 0 || value != null && (value & 1) == 0;
            });
        }
        equalsTo(0, calls.get(), "construction is predicate-lazy");
        var pending = view.stream();
        Spliterator<Integer> split = view.spliterator();
        equalsTo(0, calls.get(), "metadata and stream construction are lazy");
        equalsTo(Long.MAX_VALUE, split.estimateSize(), "no fake cardinality");
        equalsTo(Spliterator.ORDERED, split.characteristics(), "only ORDERED");
        check(split.trySplit() == null, "no split buffer");
        source.add(6);
        equalsTo(List.of(2, 4, 6), pending.toList(), "late-bound source mutation");
        var observed = new ArrayList<Integer>();
        split.forEachRemaining(observed::add);
        equalsTo(List.of(2, 4, 6), observed, "late-bound spliterator");
        equalsTo(3, view.size(), "deep selected size");
        equalsTo(4, view.get(1), "deep indexed get");
        check(calls.get() >= 3 * 20_000, "every admitted predicate is evaluated");
        final List<Integer> guarded = view;
        refuses(UnsupportedOperationException.class, () -> guarded.add(8));
        refuses(UnsupportedOperationException.class, () -> guarded.addAll(List.of()));
        refuses(UnsupportedOperationException.class, () -> guarded.removeIf(v -> false));
        refuses(UnsupportedOperationException.class, guarded::removeFirst);
        refuses(UnsupportedOperationException.class, () -> guarded.reversed().clear());
        refuses(UnsupportedOperationException.class, () -> guarded.subList(0, 1).clear());
        ListIterator<Integer> iterator = view.listIterator();
        equalsTo(2, iterator.next(), "read-only iterator result");
        refuses(UnsupportedOperationException.class, iterator::remove);
        refuses(UnsupportedOperationException.class, () -> iterator.set(12));
        equalsTo(List.of(6, 4, 2), view.reversed(), "reversed view");
    }
    private static void orderedShortCircuitAndExceptionIdentity() {
        var source = new ArrayList<>(List.of(9));
        var sequence = new ArrayList<Integer>();
        List<Integer> view = source;
        for (int i = 0; i < 128; i++) {
            final int index = i;
            view = M3FilteredLists.readOnlyFiltered(view, v -> {
                sequence.add(index);
                if (index > 27) throw new AssertionError("executed after rejected predicate");
                return index != 27;
            });
        }
        equalsTo(0, view.size(), "rejection stops later predicates");
        equalsTo(28, sequence.size(), "short circuit count");
        for (int i = 0; i < 28; i++) equalsTo(i, sequence.get(i), "predicate order");
        var sameException = new IllegalStateException("user-thrown");
        sequence.clear();
        List<Integer> other = source;
        for (int i = 0; i < 128; i++) {
            final int index = i;
            other = M3FilteredLists.readOnlyFiltered(other, v -> {
                sequence.add(index);
                if (index == 42) throw sameException;
                return true;
            });
        }
        checks++;
        try { other.size(); throw new AssertionError("missing user exception"); }
        catch (IllegalStateException caught) {
            check(caught == sameException, "preserve exact exception object");
        }
        equalsTo(43, sequence.size(), "do not execute after exception");
    }
    private static void protectedWritableAndSublistBoundaries() {
        var owner = new ArrayList<>(Arrays.asList(null, 1, 2, 3, 4, 5, 6));
        List<Integer> readonly = M3FilteredLists.readOnlyFiltered(owner,
                n -> n == null || (n & 1) == 0);
        equalsTo(Arrays.asList(null, 2, 4, 6), readonly, "null and even membership");
        List<Integer> nested = M3FilteredLists.readOnlyFiltered(readonly, n -> n != null);
        equalsTo(List.of(2, 4, 6), nested, "null exclusion in composed predicate");
        List<Integer> ranged = M3FilteredLists.readOnlyFiltered(nested.subList(1, 3), n -> n >= 6);
        equalsTo(List.of(6), ranged, "physical sublist source retained");
        refuses(UnsupportedOperationException.class, () -> ranged.set(0, 8));
        List<Integer> writable = M3FilteredLists.filtered(readonly, n -> n != null);
        equalsTo(List.of(2, 4, 6), writable, "writable filter reads protected source");
        refuses(UnsupportedOperationException.class, () -> writable.add(0, 8));
        equalsTo(Arrays.asList(null, 1, 2, 3, 4, 5, 6), owner,
                "protected source cannot be bypassed by writable view");
    }
    private static void sortedBackingAndTrulyReadOnly() {
        TreeSet<Integer> source = new TreeSet<>(Comparator.reverseOrder());
        source.addAll(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        var backing = new ArrayList<>(source);
        List<Integer> view = backing;
        for (int i = 0; i < 2_048; i++) {
            view = M3FilteredLists.readOnlyFiltered(view, n -> (n & 1) == 0);
        }
        equalsTo(List.of(8, 6, 4, 2), view, "source sorted encounter");
        equalsTo(List.of(2, 4, 6, 8), view.reversed(), "reversed sorted encounter");
        backing.add(10);
        equalsTo(List.of(8, 6, 4, 2, 10), view, "live sorted-seeded source");
        check((view.spliterator().characteristics() &
                (Spliterator.SIZED | Spliterator.SORTED |
                        Spliterator.SUBSIZED | Spliterator.DISTINCT)) == 0,
                "never invent sorted/sized metadata");
    }
    public static void main(String[] args) {
        deepReadOnlyChainAndLateBinding();
        orderedShortCircuitAndExceptionIdentity();
        protectedWritableAndSublistBoundaries();
        sortedBackingAndTrulyReadOnly();
        System.out.println("M3_READONLY_FILTER_DEPTH_PASS checks=" + checks);
    }
}
