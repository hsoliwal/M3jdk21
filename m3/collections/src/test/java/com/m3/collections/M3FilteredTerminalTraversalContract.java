/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * One frozen callback tape for the actual baseline and candidate owners.
 * The fixture wraps real JDK sources; it does not implement a filtered traversal.
 * Qualification compares the complete output, including injected exception order.
 */
public final class M3FilteredTerminalTraversalContract {
    private static int cases;
    private static int faultCases;

    private M3FilteredTerminalTraversalContract() { }

    @FunctionalInterface
    private interface Case {
        String run(Tape tape);
    }

    private static final class ProbeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ProbeFailure(int ordinal, String event) {
            super("callback[" + ordinal + "]=" + event);
        }
    }

    private static final class Tape {
        private final List<String> events = new ArrayList<>();
        private final int failure;

        Tape(int failure) {
            this.failure = failure;
        }

        void hit(String event) {
            int ordinal = events.size();
            events.add(event);
            if (ordinal == failure) throw new ProbeFailure(ordinal, event);
        }
    }

    private record Observation(String outcome, List<String> events) { }

    private static Observation observe(Case test, int failure) {
        Tape tape = new Tape(failure);
        try {
            return new Observation("RETURN:" + test.run(tape), List.copyOf(tape.events));
        } catch (ProbeFailure expected) {
            return new Observation("THROW:" + expected.getClass().getName() + ":"
                    + expected.getMessage(), List.copyOf(tape.events));
        }
    }

    private static void emit(String name, int failure, Observation observation) {
        System.out.println(name + "|" + failure + "|" + observation.outcome()
                + "|" + String.join(";", observation.events()));
    }

    private static void check(String name, Case test, String expected) {
        Observation baseline = observe(test, -1);
        if (!baseline.outcome().equals("RETURN:" + expected)) {
            throw new AssertionError(name + " outcome " + baseline.outcome()
                    + " expected RETURN:" + expected);
        }
        emit(name, -1, baseline);
        cases++;
        for (int failure = 0; failure < baseline.events().size(); failure++) {
            Observation injected = observe(test, failure);
            String expectedType = "THROW:" + ProbeFailure.class.getName() + ":";
            if (!injected.outcome().startsWith(expectedType)) {
                throw new AssertionError(name + " did not propagate callback " + failure
                        + ": " + injected.outcome());
            }
            emit(name, failure, injected);
            faultCases++;
        }
    }

    /*
     * Proxy public collection interfaces, leaving primitive/scalar results intact.
     * An invocation exception is unwrapped so the owner sees the source exception.
     */
    private static Object trace(Object source, Tape tape, String name) {
        Class<?> type;
        if (source instanceof NavigableMap<?, ?>) type = NavigableMap.class;
        else if (source instanceof SortedMap<?, ?>) type = SortedMap.class;
        else if (source instanceof Map<?, ?>) type = Map.class;
        else if (source instanceof NavigableSet<?>) type = NavigableSet.class;
        else if (source instanceof SortedSet<?>) type = SortedSet.class;
        else if (source instanceof Set<?>) type = Set.class;
        else if (source instanceof Collection<?>) type = Collection.class;
        else if (source instanceof Iterator<?>) type = Iterator.class;
        else if (source instanceof Map.Entry<?, ?>) type = Map.Entry.class;
        else return source;
        return Proxy.newProxyInstance(M3FilteredTerminalTraversalContract.class.getClassLoader(),
                new Class<?>[] { type }, (proxy, method, args) -> {
                    tape.hit(name + "." + method.getName()
                            + (args == null ? "" : Arrays.toString(args)));
                    try {
                        return trace(method.invoke(source, args), tape,
                                name + "/" + method.getName());
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static Collection<Integer> collection(Collection<Integer> source, Tape tape) {
        return (Collection<Integer>) trace(source, tape, "source");
    }

    @SuppressWarnings("unchecked")
    private static NavigableSet<Integer> set(NavigableSet<Integer> source, Tape tape) {
        return (NavigableSet<Integer>) trace(source, tape, "source");
    }

    @SuppressWarnings("unchecked")
    private static NavigableMap<Integer, Integer> map(
            NavigableMap<Integer, Integer> source, Tape tape) {
        return (NavigableMap<Integer, Integer>) trace(source, tape, "source");
    }

    private static TreeMap<Integer, Integer> map(int size) {
        TreeMap<Integer, Integer> source = new TreeMap<>();
        for (int value = 0; value < size; value++) source.put(value, value * 10);
        return source;
    }

    private static Predicate<Integer> predicate(Tape tape, int accepted) {
        return value -> {
            tape.hit("predicate(" + value + ")");
            return Objects.equals(value, accepted);
        };
    }

    private static BiPredicate<Integer, Integer> entryPredicate(Tape tape, int accepted) {
        return (key, value) -> {
            tape.hit("predicate(" + key + "," + value + ")");
            return Objects.equals(key, accepted);
        };
    }

    private static String entry(Map.Entry<Integer, Integer> value) {
        return value == null ? "null" : value.getKey() + "=" + value.getValue();
    }

    public static void main(String[] args) {
        for (int size : new int[] { 0, 1, 5 }) {
            for (int accepted : new int[] { -1, 0, 2, 4 }) {
                String expected = Boolean.toString(accepted < 0 || accepted >= size);
                check("collection-empty-" + size + "-" + accepted, tape -> {
                    ArrayList<Integer> owner = new ArrayList<>();
                    for (int value = 0; value < size; value++) owner.add(value);
                    Collection<Integer> source = collection(owner, tape);
                    return Boolean.toString(M3FilteredViews.filter(source,
                            predicate(tape, accepted)).isEmpty());
                }, expected);
                check("sorted-map-empty-" + size + "-" + accepted, tape -> {
                    SortedMap<Integer, Integer> source = map(map(size), tape);
                    return Boolean.toString(M3FilteredViews.filterEntries(source,
                            entryPredicate(tape, accepted)).isEmpty());
                }, expected);
            }
        }

        check("collection-null", tape -> {
            Collection<Integer> source = collection(
                    new ArrayList<>(Arrays.asList(1, null, 2)), tape);
            return Boolean.toString(M3FilteredViews.filter(source, value -> {
                tape.hit("predicate(" + value + ")");
                return value == null;
            }).isEmpty());
        }, "false");

        for (boolean forward : new boolean[] { false, true }) {
            for (int accepted : new int[] { -1, 1, 2, 3 }) {
                check("navigable-set-" + forward + "-" + accepted, tape -> {
                    NavigableSet<Integer> source = set(
                            new TreeSet<>(List.of(0, 1, 2, 3, 4)), tape);
                    NavigableSet<Integer> view = M3FilteredViews.filter(source,
                            predicate(tape, accepted));
                    return String.valueOf(forward ? view.ceiling(0) : view.floor(4));
                }, accepted < 0 ? "null" : Integer.toString(accepted));
                check("navigable-map-" + forward + "-" + accepted, tape -> {
                    NavigableMap<Integer, Integer> view = M3FilteredViews.filterEntries(
                            map(map(5), tape), entryPredicate(tape, accepted));
                    return entry(forward ? view.firstEntry() : view.lastEntry());
                }, accepted < 0 ? "null" : accepted + "=" + accepted * 10);
            }
        }

        check("navigable-set-empty", tape -> String.valueOf(M3FilteredViews.filter(
                set(new TreeSet<>(), tape), predicate(tape, 2)).ceiling(0)), "null");

        check("navigable-set-null", tape -> {
            TreeSet<Integer> owner = new TreeSet<>(Comparator.nullsFirst(Integer::compareTo));
            owner.addAll(Arrays.asList(null, 0, 1, 2));
            return String.valueOf(M3FilteredViews.filter(set(owner, tape), value -> {
                tape.hit("predicate(" + value + ")");
                return value == null;
            }).floor(2));
        }, "null");

        check("navigable-map-empty", tape -> entry(M3FilteredViews.filterEntries(
                map(map(0), tape), entryPredicate(tape, 2)).firstEntry()), "null");

        check("navigable-map-null-value", tape -> {
            TreeMap<Integer, Integer> owner = map(5);
            owner.put(2, null);
            return entry(M3FilteredViews.filterEntries(map(owner, tape),
                    entryPredicate(tape, 2)).firstEntry());
        }, "2=null");

        check("sorted-map-live-owner", tape -> {
            TreeMap<Integer, Integer> owner = map(2);
            SortedMap<Integer, Integer> source = map(owner, tape);
            SortedMap<Integer, Integer> view = M3FilteredViews.filterEntries(
                    source, entryPredicate(tape, 2));
            boolean first = view.isEmpty();
            owner.put(2, 20);
            return first + ":" + view.isEmpty();
        }, "true:false");

        check("navigable-map-nested-descending", tape -> {
            NavigableMap<Integer, Integer> owner = map(8).subMap(1, true, 7, false)
                    .descendingMap().subMap(5, true, 2, true);
            return entry(M3FilteredViews.filterEntries(map(owner, tape),
                    entryPredicate(tape, 3)).firstEntry());
        }, "3=30");

        check("navigable-map-fresh-immutable-entry", tape -> {
            NavigableMap<Integer, Integer> view = M3FilteredViews.filterEntries(
                    map(map(5), tape), entryPredicate(tape, 2));
            Map.Entry<Integer, Integer> first = view.firstEntry();
            Map.Entry<Integer, Integer> second = view.firstEntry();
            if (first == second) throw new AssertionError("entry projection was reused");
            try {
                first.setValue(99);
                throw new AssertionError("entry projection was mutable");
            } catch (UnsupportedOperationException expected) {
                // The caller owns an immutable detached result from each invocation.
            }
            return entry(first) + ":" + entry(second);
        }, "2=20:2=20");

        System.out.println("PASS M3FilteredTerminalTraversalContract cases=" + cases
                + " injected=" + faultCases);
    }
}
