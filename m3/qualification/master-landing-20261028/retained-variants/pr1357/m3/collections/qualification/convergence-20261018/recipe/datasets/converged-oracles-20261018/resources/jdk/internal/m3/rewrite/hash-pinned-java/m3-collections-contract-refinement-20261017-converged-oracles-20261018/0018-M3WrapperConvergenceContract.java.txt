// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Frozen Java 21 wrapper counterexamples and contract checks.
 * Modes isolate admitted leaves from explicitly pending source findings.
 * No mode establishes full collection or JDK runtime qualification.
 */
public final class M3WrapperConvergenceContract {
    private static int checks;

    private M3WrapperConvergenceContract() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": reference identity differs");
    }

    private static void require(boolean accepted, String label) {
        checks++;
        if (!accepted) throw new AssertionError(label);
    }

    private static Throwable failure(Class<? extends Throwable> type,
            Runnable operation, String label) {
        checks++;
        try {
            operation.run();
        } catch (Throwable caught) {
            if (type.isInstance(caught)) return caught;
            throw new AssertionError(label + ": unexpected " + caught, caught);
        }
        throw new AssertionError(label + ": missing " + type.getSimpleName());
    }

    /** The ordinary iterator must use the same read-only boundary as listIterator. */
    private static void readOnlyRefusalAndIdentity() {
        String first = new String("equal");
        String second = new String("equal");
        List<String> owner = new ArrayList<>(Arrays.asList("hidden", null, first, second));
        AtomicInteger calls = new AtomicInteger();
        List<String> selected = M3FilteredLists.readOnlyFiltered(owner, value -> {
            calls.incrementAndGet();
            return value == null || value == first || value == second;
        });
        equal(0, calls.get(), "factory remains predicate-lazy");
        Iterator<String> cursor = selected.iterator();
        int preparedCalls = calls.get();
        failure(UnsupportedOperationException.class, cursor::remove, "remove before next");
        equal(preparedCalls, calls.get(), "read-only remove does not call predicates");
        equal(true, cursor.hasNext(), "null candidate exists");
        equal(true, cursor.hasNext(), "repeated lookahead");
        same(null, cursor.next(), "stored null");
        failure(UnsupportedOperationException.class, cursor::remove, "remove after null");
        same(first, cursor.next(), "first equal stored reference");
        same(second, cursor.next(), "second equal stored reference");
        equal(false, cursor.hasNext(), "iterator exhaustion");
        failure(UnsupportedOperationException.class, cursor::remove, "remove after exhaustion");
        equal(4, owner.size(), "owner unchanged by all refusals");
        same(first, owner.get(2), "first owner identity retained");
        same(second, owner.get(3), "second owner identity retained");

        List<Integer> empty = M3FilteredLists.readOnlyFiltered(new ArrayList<>(), value -> true);
        failure(UnsupportedOperationException.class, empty.iterator()::remove,
                "empty ordinary iterator removal");
        failure(UnsupportedOperationException.class, empty.reversed().iterator()::remove,
                "empty reversed ordinary iterator removal");
        failure(UnsupportedOperationException.class, empty.subList(0, 0).iterator()::remove,
                "empty ranged ordinary iterator removal");
    }

    /** Predicate counts bound traversal work without substituting a timing benchmark. */
    private static void readOnlyLinearTraversal() {
        int length = 1024;
        ArrayList<Integer> owner = new ArrayList<>();
        ArrayList<Integer> expected = new ArrayList<>();
        for (int index = 0; index < length; index++) {
            owner.add(index);
            if ((index & 1) == 0) expected.add(index);
        }
        AtomicInteger calls = new AtomicInteger();
        List<Integer> selected = M3FilteredLists.readOnlyFiltered(owner, value -> {
            calls.incrementAndGet();
            return (value & 1) == 0;
        });
        ArrayList<Integer> actual = new ArrayList<>();
        Iterator<Integer> cursor = selected.iterator();
        while (cursor.hasNext()) actual.add(cursor.next());
        equal(expected, actual, "ordinary iterator encounter order");
        require(calls.get() <= 2 * length + 2,
                "ordinary iterator predicate work must be linear, actual=" + calls.get());

        calls.set(0);
        actual.clear();
        selected.forEach(actual::add);
        equal(expected, actual, "forEach encounter order");
        require(calls.get() <= 2 * length + 2,
                "forEach predicate work must be linear, actual=" + calls.get());

        calls.set(0);
        equal(expected, Arrays.asList(selected.toArray()), "toArray encounter order");
        require(calls.get() <= 3 * length + 2,
                "toArray permits size preflight plus linear traversal, actual=" + calls.get());

        List<Integer> linked = M3FilteredLists.readOnlyFiltered(
                new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6)), value -> (value & 1) == 0);
        equal(List.of(2, 4, 6), collect(linked.iterator()), "linked owner iterator");
        equal(List.of(6, 4, 2), collect(linked.reversed().iterator()), "reversed iterator");
        equal(List.of(4, 6), collect(linked.subList(1, 3).iterator()), "physical range iterator");
    }

    private static <E> List<E> collect(Iterator<E> cursor) {
        ArrayList<E> result = new ArrayList<>();
        cursor.forEachRemaining(result::add);
        return result;
    }

    /**
     * Freeze the old AbstractList read path using its unchanged filtered delegate.
     * The baseline intentionally has no iterator override; it is independent of
     * the new guard and does not import any proposed traversal optimization.
     */
    private static <E> List<E> baselineReadOnly(List<E> delegate) {
        return new AbstractList<>() {
            @Override public int size() { return delegate.size(); }
            @Override public E get(int index) { return delegate.get(index); }
        };
    }

    private static List<Object> readTrace(boolean candidate, boolean linked) {
        List<Integer> owner = linked
                ? new LinkedList<>(Arrays.asList(1, 2, null, 4, 5, 6))
                : new ArrayList<>(Arrays.asList(1, 2, null, 4, 5, 6));
        ArrayList<Integer> calls = new ArrayList<>();
        Predicate<Integer> predicate = value -> {
            calls.add(value);
            return value == null || (value & 1) == 0;
        };
        List<Integer> selected = candidate ? M3FilteredLists.readOnlyFiltered(owner, predicate)
                : baselineReadOnly(M3FilteredLists.filtered(owner, predicate));
        ArrayList<Object> trace = new ArrayList<>();
        Iterator<Integer> cursor = selected.iterator();
        trace.add(calls.size());
        failure(NullPointerException.class, () -> cursor.forEachRemaining(null),
                "forEachRemaining null action");
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.next());
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.next());
        trace.add(calls.size());
        ArrayList<Integer> remaining = new ArrayList<>();
        cursor.forEachRemaining(remaining::add);
        trace.add(remaining);
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(new ArrayList<>(calls));
        return trace;
    }

    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException | Error caught) { return caught; }
    }

    private static List<Object> predicateFailureTrace(boolean candidate,
            IllegalStateException marker) {
        ArrayList<Integer> owner = new ArrayList<>(List.of(1, 2, 4, 6));
        ArrayList<Integer> calls = new ArrayList<>();
        Predicate<Integer> predicate = value -> {
            calls.add(value);
            if (value == 4) throw marker;
            return (value & 1) == 0;
        };
        List<Integer> selected = candidate ? M3FilteredLists.readOnlyFiltered(owner, predicate)
                : baselineReadOnly(M3FilteredLists.filtered(owner, predicate));
        ArrayList<Object> trace = new ArrayList<>();
        Iterator<Integer> cursor = selected.iterator();
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        // A failed size scan must not silently poison the original indexed cursor.
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(new ArrayList<>(calls));
        return trace;
    }

    private static void readOnlyPreservedReadEffects() {
        for (boolean linked : new boolean[] {false, true}) {
            equal(readTrace(false, linked), readTrace(true, linked),
                    "ordinary read callback tape, linked=" + linked);
        }
        IllegalStateException marker = new IllegalStateException("same predicate-owned failure");
        List<Object> expected = predicateFailureTrace(false, marker);
        List<Object> actual = predicateFailureTrace(true, marker);
        equal(expected, actual, "predicate callback order, failure identity and retry behavior");
        require(expected.contains(marker) && actual.contains(marker),
                "same caller exception occurs in both read paths");
    }

    private static void readOnlyFailureContracts() {
        ArrayList<Integer> owner = new ArrayList<>(List.of(1, 2, 3, 4));
        List<Integer> selected = M3FilteredLists.readOnlyFiltered(owner, value -> (value & 1) == 0);
        Iterator<Integer> cursor = selected.iterator();
        equal(2, cursor.next(), "fail-fast first member");
        owner.add(6);
        failure(ConcurrentModificationException.class, cursor::next,
                "ordinary read-only iterator retains backing fail-fast detection");
        failure(UnsupportedOperationException.class, cursor::remove,
                "read-only refusal remains authoritative after backing failure");

        IllegalStateException marker = new IllegalStateException("stable predicate failure");
        ArrayList<Integer> tape = new ArrayList<>();
        List<Integer> failed = M3FilteredLists.readOnlyFiltered(
                new ArrayList<>(List.of(1, 2, 4, 6)), value -> {
                    tape.add(value);
                    if (value == 4) throw marker;
                    return (value & 1) == 0;
                });
        Iterator<Integer> failureCursor = failed.iterator();
        equal(2, failureCursor.next(), "member preceding failure");
        same(marker, failure(IllegalStateException.class, failureCursor::hasNext,
                "predicate failure identity"), "same thrown predicate object");
        require(!tape.contains(6), "no callback after the failing member");
        failure(IllegalStateException.class, failureCursor::next,
                "backing filtered cursor is poisoned after its predicate fails");
        failure(UnsupportedOperationException.class, failureCursor::remove,
                "poisoned read-only cursor still refuses removal");
    }

    /** Retained current failure semantics; historical blanket IAE catches must not swallow this. */
    private static void comparatorFailureIdentity() {
        IllegalArgumentException marker = new IllegalArgumentException("comparator-owned failure");
        ArrayList<String> tape = new ArrayList<>();
        boolean[] armed = {false};
        Comparator<Integer> order = (left, right) -> {
            if (armed[0]) {
                tape.add(left + ":" + right);
                if (left == 5 && right == 2) throw marker;
            }
            return Integer.compare(left, right);
        };
        TreeMap<Integer, Integer> map = new TreeMap<>(order);
        for (int key : new int[] {2, 4, 6, 8}) map.put(key, key);
        NavigableMap<Integer, Integer> boundedMap = map.subMap(2, true, 8, false);
        NavigableMap<Integer, Integer> readOnlyMap = M3FilteredViews.filterEntries(
                boundedMap, (key, value) -> true);
        NavigableSet<Integer> mutableSet = M3MutableFilteredViews.filter(
                boundedMap.navigableKeySet(), value -> true);
        armed[0] = true;
        same(marker, failure(IllegalArgumentException.class, () -> readOnlyMap.floorEntry(5),
                "read-only map comparator failure"), "read-only map same failure");
        equal(List.of("5:2"), tape, "map comparator callback tape stops at original failure");
        tape.clear();
        same(marker, failure(IllegalArgumentException.class, () -> mutableSet.floor(5),
                "mutable set comparator failure"), "mutable set same failure");
        equal(List.of("5:2"), tape, "set comparator callback tape stops at original failure");
    }

    /** Pending baseline counterexample until an exception-safe bounded atom is admitted. */
    private static void boundedMapNavigation() {
        TreeMap<Integer, Integer> owner = new TreeMap<>();
        for (int key = 1; key <= 9; key++) owner.put(key, key);
        NavigableMap<Integer, Integer> backing = owner.subMap(2, true, 8, false);
        for (boolean descending : new boolean[] {false, true}) {
            NavigableMap<Integer, Integer> source = descending ? backing.descendingMap() : backing;
            NavigableMap<Integer, Integer> view = M3FilteredViews.filterEntries(
                    source, (key, value) -> (key & 1) == 0);
            TreeMap<Integer, Integer> expected = new TreeMap<>(source.comparator());
            source.forEach((key, value) -> { if ((key & 1) == 0) expected.put(key, value); });
            for (int query : new int[] {-100, 0, 2, 3, 6, 7, 8, 100}) {
                equal(expected.floorEntry(query), view.floorEntry(query), "bounded map floor " + query);
                equal(expected.lowerEntry(query), view.lowerEntry(query), "bounded map lower " + query);
                equal(expected.ceilingEntry(query), view.ceilingEntry(query), "bounded map ceiling " + query);
                equal(expected.higherEntry(query), view.higherEntry(query), "bounded map higher " + query);
            }
        }
    }

    /** Pending baseline counterexample; mutable maps expose this same SetView through key sets. */
    private static void boundedMutableSetNavigation() {
        TreeSet<Integer> owner = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        NavigableSet<Integer> backing = owner.subSet(2, true, 8, false);
        for (boolean descending : new boolean[] {false, true}) {
            NavigableSet<Integer> source = descending ? backing.descendingSet() : backing;
            NavigableSet<Integer> view = M3MutableFilteredViews.filter(source, value -> (value & 1) == 0);
            TreeSet<Integer> expected = new TreeSet<>(source.comparator());
            for (int value : source) if ((value & 1) == 0) expected.add(value);
            for (int query : new int[] {-100, 0, 2, 3, 6, 7, 8, 100}) {
                equal(expected.floor(query), view.floor(query), "bounded mutable floor " + query);
                equal(expected.lower(query), view.lower(query), "bounded mutable lower " + query);
                equal(expected.ceiling(query), view.ceiling(query), "bounded mutable ceiling " + query);
                equal(expected.higher(query), view.higher(query), "bounded mutable higher " + query);
            }
        }
    }

    /** A total order puts null strictly between nonpositive and positive integers. */
    private static void interiorNullNavigation() {
        Comparator<Integer> order = (left, right) -> {
            if (left == right) return 0;
            if (left == null) return right <= 0 ? 1 : -1;
            if (right == null) return left <= 0 ? -1 : 1;
            return Integer.compare(left, right);
        };
        TreeSet<Integer> source = new TreeSet<>(order);
        source.addAll(Arrays.asList(-2, null, 2));
        NavigableSet<Integer> view = M3FilteredViews.filter(source, Objects::nonNull);
        equal(-2, view.floor(null), "interior-null floor skips hidden null");
        equal(2, view.ceiling(null), "interior-null ceiling skips hidden null");
        equal(2, view.higher(0), "interior-null higher skips hidden null");
        equal(-2, view.lower(1), "interior-null lower skips hidden null");
        equal(2, view.descendingSet().floor(null), "descending interior-null floor");
        equal(-2, view.descendingSet().ceiling(null), "descending interior-null ceiling");
    }

    /** Only admitted leaves are called by the JUnit wrapper; pending modes remain reproducible. */
    public static void run(String mode) {
        checks = 0;
        switch (mode) {
            case "readonly-policy" -> {
                readOnlyRefusalAndIdentity();
                readOnlyPreservedReadEffects();
            }
            case "readonly" -> {
                readOnlyRefusalAndIdentity();
                readOnlyPreservedReadEffects();
                comparatorFailureIdentity();
            }
            case "readonly-effects" -> readOnlyPreservedReadEffects();
            case "readonly-linear" -> readOnlyLinearTraversal();
            case "readonly-failfast" -> readOnlyFailureContracts();
            case "comparator-failure" -> comparatorFailureIdentity();
            case "bounded-map" -> boundedMapNavigation();
            case "bounded-set" -> boundedMutableSetNavigation();
            case "interior-null" -> interiorNullNavigation();
            default -> throw new IllegalArgumentException("unknown wrapper contract mode: " + mode);
        }
        System.out.println("PASS M3WrapperConvergenceContract mode=" + mode + " checks=" + checks);
    }

    public static void main(String[] args) { run(args.length == 0 ? "readonly" : args[0]); }
}
