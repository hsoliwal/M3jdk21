// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
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
import java.util.function.Supplier;

/** Generic terminal transcript over actual JDK owners; interior-null repair is a separate proof. */
public final class M3GenericFilteredTerminalProof {
    private static final StringBuilder TAPE = new StringBuilder();
    private static final List<Integer> KEYS = List.of(-3, -1, 0, 2, 5, 8);
    private static long checks;

    private M3GenericFilteredTerminalProof() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static final class Observation {
        final StringBuilder events = new StringBuilder();
        final List<Integer> primaryVisits = new ArrayList<>();
        final RuntimeException marker = new RuntimeException("generic terminal marker");
        final char failurePhase;
        final int failureOrdinal;

        Observation(char failurePhase, int failureOrdinal) {
            this.failurePhase = failurePhase;
            this.failureOrdinal = failureOrdinal;
        }

        void failIf(char phase, int ordinal) {
            if (failurePhase == phase && failureOrdinal == ordinal) throw marker;
        }

        boolean primary(Integer value, int mode) {
            int ordinal = primaryVisits.size();
            primaryVisits.add(value);
            events.append('P').append(ordinal).append('=').append(value).append(';');
            failIf('P', ordinal);
            return accepted(value, mode);
        }

        boolean secondary(Integer value) {
            events.append('Q').append(value).append(';');
            return second(value);
        }
    }

    /** Decorate only the source boundary; delegate every operation to its actual JDK owner. */
    @SuppressWarnings("unchecked")
    private static <T> T tap(Class<?> contract, T owner, Observation observation, String path) {
        return (T) Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract},
                (proxy, method, arguments) -> {
                    String next = path + '.' + method.getName();
                    observation.events.append('S').append(next).append('(')
                            .append(arguments == null ? "" : Arrays.toString(arguments)).append(");");
                    Object result;
                    try {
                        result = method.invoke(owner, arguments);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                    if (result instanceof Iterator<?> cursor) {
                        return new Iterator<Object>() {
                            private int ordinal;

                            @Override public boolean hasNext() {
                                observation.events.append('H').append(next).append(':').append(ordinal).append(';');
                                observation.failIf('H', ordinal);
                                return cursor.hasNext();
                            }

                            @Override public Object next() {
                                observation.events.append('N').append(next).append(':').append(ordinal).append(';');
                                observation.failIf('N', ordinal);
                                Object value = cursor.next();
                                ordinal++;
                                return value;
                            }

                            @Override public void remove() { cursor.remove(); }
                        };
                    }
                    if (result instanceof NavigableMap<?, ?>) return tap(NavigableMap.class, result, observation, next);
                    if (result instanceof SortedMap<?, ?>) return tap(SortedMap.class, result, observation, next);
                    if (result instanceof NavigableSet<?>) return tap(NavigableSet.class, result, observation, next);
                    if (result instanceof SortedSet<?>) return tap(SortedSet.class, result, observation, next);
                    if (result instanceof Set<?>) return tap(Set.class, result, observation, next);
                    if (result instanceof Collection<?>) return tap(Collection.class, result, observation, next);
                    return result;
                });
    }

    private static boolean accepted(Integer value, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> value != null && (value & 1) == 0;
            default -> value != null && value >= 5;
        };
    }

    private static boolean second(Integer value) { return value != null && value % 3 != 0; }

    private static void outcome(String label, Observation observation,
                                Supplier<?> operation, Object expected) {
        check(observation.events.length() == 0, "construction remains lazy");
        TAPE.append("\ncase:").append(label).append(':').append(observation.failurePhase)
                .append(':').append(observation.failureOrdinal).append(';');
        try {
            Object actual = operation.get();
            check(observation.failurePhase == '-', "configured source/predicate failure was lost");
            check(Objects.equals(expected, actual), "independent expected terminal result");
            TAPE.append("result=").append(actual).append(';');
        } catch (RuntimeException failure) {
            check(observation.failurePhase != '-', "unexpected source/predicate failure");
            check(failure == observation.marker, "source/predicate exception identity");
            TAPE.append("marker;");
        }
        TAPE.append(observation.events);
    }

    private static List<Integer> visitedUntilAccepted(Iterable<Integer> candidates, int mode,
                                                       boolean nested) {
        List<Integer> visits = new ArrayList<>();
        for (Integer value : candidates) {
            visits.add(value);
            if (accepted(value, mode) && (!nested || second(value))) break;
        }
        return visits;
    }

    private static void collection(List<Integer> values, int mode, boolean nested,
                                   char failurePhase, int failureOrdinal) {
        Observation observation = new Observation(failurePhase, failureOrdinal);
        Collection<Integer> owner = new ArrayList<>(values);
        Collection<Integer> source = tap(Collection.class, owner, observation, "collection");
        Predicate<Integer> primary = value -> observation.primary(value, mode);
        Collection<Integer> view = M3FilteredViews.filter(source, primary);
        if (nested) view = M3FilteredViews.filter(view, observation::secondary);
        Collection<Integer> target = view;
        boolean empty = true;
        for (Integer value : values) if (accepted(value, mode) && (!nested || second(value))) empty = false;
        outcome("collection:" + values.size() + ':' + mode + ':' + nested,
                observation, target::isEmpty, empty);
        if (failurePhase == '-') {
            check(observation.primaryVisits.equals(visitedUntilAccepted(values, mode, nested)),
                    "collection predicate encounter order and demand");
        }
    }

    private static void set(int mode, boolean nested, boolean backward,
                            char failurePhase, int failureOrdinal) {
        Observation observation = new Observation(failurePhase, failureOrdinal);
        NavigableSet<Integer> owner = new TreeSet<>(KEYS);
        NavigableSet<Integer> source = tap(NavigableSet.class, owner, observation, "set");
        Predicate<Integer> primary = value -> observation.primary(value, mode);
        NavigableSet<Integer> view = M3FilteredViews.filter(source, primary);
        if (nested) view = M3FilteredViews.filter(view, observation::secondary);
        NavigableSet<Integer> target = view;
        NavigableSet<Integer> oracle = new TreeSet<>();
        for (Integer value : owner) if (accepted(value, mode) && (!nested || second(value))) oracle.add(value);
        Integer expected = backward ? oracle.floor(5) : oracle.ceiling(-1);
        outcome("set:" + mode + ':' + nested + ':' + backward, observation,
                () -> backward ? target.floor(5) : target.ceiling(-1), expected);
        if (failurePhase == '-' && !nested) {
            Iterable<Integer> candidates = backward ? owner.headSet(5, true).descendingSet()
                    : owner.tailSet(-1, true);
            check(observation.primaryVisits.equals(visitedUntilAccepted(candidates, mode, false)),
                    "navigation predicate encounter order and demand");
        }
    }

    private static TreeMap<Integer, String> mapOwner() {
        TreeMap<Integer, String> result = new TreeMap<>();
        for (Integer key : KEYS) result.put(key, key == 0 ? null : "value:" + key);
        return result;
    }

    private static void map(int mode, boolean nested, int operation,
                            char failurePhase, int failureOrdinal) {
        Observation observation = new Observation(failurePhase, failureOrdinal);
        TreeMap<Integer, String> owner = mapOwner();
        BiPredicate<Integer, String> primary = (key, value) -> {
            observation.events.append('V').append(key).append('=').append(value).append(';');
            return observation.primary(key, mode);
        };
        BiPredicate<Integer, String> secondary = (key, value) -> observation.secondary(key);
        TreeMap<Integer, String> oracle = new TreeMap<>();
        owner.forEach((key, value) -> {
            if (accepted(key, mode) && (!nested || second(key))) oracle.put(key, value);
        });
        if (operation == 0) {
            SortedMap<Integer, String> backing = owner;
            SortedMap<Integer, String> source = tap(SortedMap.class, backing, observation, "sorted-map");
            SortedMap<Integer, String> view = M3FilteredViews.filterEntries(source, primary);
            if (nested) view = M3FilteredViews.filterEntries(view, secondary);
            SortedMap<Integer, String> target = view;
            outcome("map-empty:" + mode + ':' + nested, observation, target::isEmpty, oracle.isEmpty());
        } else {
            NavigableMap<Integer, String> backing = owner;
            NavigableMap<Integer, String> source = tap(NavigableMap.class, backing, observation, "map");
            NavigableMap<Integer, String> view = M3FilteredViews.filterEntries(source, primary);
            if (nested) view = M3FilteredViews.filterEntries(view, secondary);
            NavigableMap<Integer, String> target = view;
            outcome("map-entry:" + operation + ':' + mode + ':' + nested, observation,
                    () -> operation == 1 ? target.firstEntry() : target.lastEntry(),
                    operation == 1 ? oracle.firstEntry() : oracle.lastEntry());
        }
        if (failurePhase == '-' && !nested) {
            Iterable<Integer> candidates = operation == 2 ? owner.descendingKeySet() : owner.keySet();
            check(observation.primaryVisits.equals(visitedUntilAccepted(candidates, mode, false)),
                    "map predicate encounter order and demand");
        }
    }

    private static void repeated(Iterator<?> cursor, Observation observation, int expected) {
        int count = 0;
        while (true) {
            boolean more = cursor.hasNext();
            int events = observation.events.length();
            check(cursor.hasNext() == more && cursor.hasNext() == more, "repeat hasNext outcome");
            check(observation.events.length() == events, "repeat hasNext replays no source/predicate");
            if (!more) break;
            cursor.next();
            check(observation.events.length() == events, "next consumes prepared candidate without callbacks");
            count++;
        }
        check(count == expected, "iteration count");
        int events = observation.events.length();
        try {
            cursor.remove();
            throw new AssertionError("read-only iterator removal accepted");
        } catch (UnsupportedOperationException expectedRefusal) {
            check(observation.events.length() == events, "read-only refusal has no source/predicate calls");
        }
        TAPE.append(observation.events);
    }

    private static void iteratorsAndEntryIdentity() {
        Observation collection = new Observation('-', -1);
        Collection<Integer> values = new ArrayList<>(KEYS);
        Collection<Integer> collectionView = M3FilteredViews.filter(
                tap(Collection.class, values, collection, "iter-collection"),
                value -> collection.primary(value, 2));
        repeated(collectionView.iterator(), collection, 3);

        Observation set = new Observation('-', -1);
        NavigableSet<Integer> keys = new TreeSet<>(KEYS);
        NavigableSet<Integer> setView = M3FilteredViews.filter(
                tap(NavigableSet.class, keys, set, "iter-set"), value -> set.primary(value, 2));
        repeated(setView.iterator(), set, 3);

        Observation map = new Observation('-', -1);
        NavigableMap<Integer, String> owner = mapOwner();
        NavigableMap<Integer, String> mapView = M3FilteredViews.filterEntries(
                tap(NavigableMap.class, owner, map, "iter-map"),
                (key, value) -> map.primary(key, 2));
        repeated(mapView.entrySet().iterator(), map, 3);
        Map.Entry<Integer, String> first = mapView.firstEntry();
        Map.Entry<Integer, String> again = mapView.firstEntry();
        check(first != again && first.equals(again), "fresh equal immutable entry per request");
        String oldValue = first.getValue();
        owner.put(first.getKey(), "changed");
        check(Objects.equals(oldValue, first.getValue()), "previous entry detached from backing replacement");
        check("changed".equals(mapView.firstEntry().getValue()), "future entry observes backing replacement");
        int events = map.events.length();
        try {
            first.setValue("forbidden");
            throw new AssertionError("immutable map entry changed");
        } catch (UnsupportedOperationException expectedRefusal) {
            check(map.events.length() == events, "entry refusal has no predicate or source callbacks");
        }
        TAPE.append("identity;").append(map.events);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        List<Integer> values = Arrays.asList(null, -3, 0, 2, 5, 2, 8);
        for (boolean nested : new boolean[]{false, true}) {
            for (int mode = 0; mode < 4; mode++) {
                collection(List.of(), mode, nested, '-', -1);
                collection(values, mode, nested, '-', -1);
                set(mode, nested, false, '-', -1);
                set(mode, nested, true, '-', -1);
                for (int operation = 0; operation < 3; operation++) map(mode, nested, operation, '-', -1);
            }
            for (char phase : new char[]{'H', 'N', 'P'}) {
                for (int ordinal : new int[]{0, 1}) {
                    collection(values, 0, nested, phase, ordinal);
                    set(0, nested, false, phase, ordinal);
                    set(0, nested, true, phase, ordinal);
                    for (int operation = 0; operation < 3; operation++) map(0, nested, operation, phase, ordinal);
                }
            }
        }
        iteratorsAndEntryIdentity();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("GENERIC_FILTERED_TERMINAL_PASS checks=" + checks
                + " tape_sha256=" + digest + " tape_chars=" + TAPE.length());
    }
}
