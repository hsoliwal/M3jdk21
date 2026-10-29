/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Provider factory and real comparator failures must not be mistaken for range rejection. */
public final class M3FilteredCursorExceptionContract {
    private static int checks;
    private M3FilteredCursorExceptionContract() { }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static final class Trace {
        final List<String> events = new ArrayList<>();
        final IllegalArgumentException failure = new IllegalArgumentException("cursor factory witness");
        String failAt;
        boolean armComparator;
        boolean armed;
        Comparator<Integer> comparator() {
            return (left, right) -> {
                events.add("compare:" + left + ":" + right);
                if (armed) { armed = false; throw failure; }
                return Integer.compare(left, right);
            };
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Object owner, Trace trace, String label) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (self, method, args) -> {
            String name = method.getName();
            String location = label + "." + name;
            trace.events.add(location + (args == null ? "" : Arrays.toString(args)));
            if (location.equals(trace.failAt)) throw trace.failure;
            if (trace.armComparator && (location.equals("range.iterator")
                    || location.equals("range.descendingIterator") || location.equals("range.entries.iterator"))) {
                trace.armed = true;
                trace.armComparator = false;
            }
            Object result;
            try { result = method.invoke(owner, args); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
            boolean range = name.equals("headMap") || name.equals("tailMap") || name.equals("headSet") || name.equals("tailSet");
            String nextLabel = range ? "range" : label;
            if (result instanceof NavigableMap<?, ?>) return proxy(NavigableMap.class, result, trace, nextLabel);
            if (result instanceof NavigableSet<?>) return proxy(NavigableSet.class, result, trace, nextLabel);
            if (result instanceof Set<?> && name.equals("entrySet")) return proxy(Set.class, result, trace, nextLabel + ".entries");
            if (result instanceof Iterator<?>) return proxy(Iterator.class, result, trace, nextLabel + ".cursor");
            return result;
        });
    }
    private static Integer navigate(NavigableSet<Integer> set, int query, int operation) {
        return switch (operation) { case 0 -> set.lower(query); case 1 -> set.floor(query);
            case 2 -> set.ceiling(query); default -> set.higher(query); };
    }
    private static Map.Entry<Integer, Integer> navigate(NavigableMap<Integer, Integer> map, int query, int operation) {
        return switch (operation) { case 0 -> map.lowerEntry(query); case 1 -> map.floorEntry(query);
            case 2 -> map.ceilingEntry(query); default -> map.higherEntry(query); };
    }
    private static void expectIdentity(Trace trace, Runnable action) {
        try { action.run(); throw new AssertionError("cursor failure swallowed: " + trace.events); }
        catch (IllegalArgumentException failure) { require(failure == trace.failure, "original exception identity"); }
        require(trace.events.stream().noneMatch(event -> event.startsWith("owner.entries")
                || event.startsWith("owner.iterator") || event.startsWith("owner.descendingIterator")), "no fallback cursor after factory failure");
    }
    @SuppressWarnings("unchecked")
    private static void cursorFailures() {
        for (int operation = 0; operation < 4; operation++) {
            final int op = operation;
            for (String failAt : List.of("range.entrySet", "range.entries.iterator", "range.descendingMap")) {
                if (operation >= 2 && failAt.equals("range.descendingMap")) continue;
                Trace trace = new Trace();
                TreeMap<Integer, Integer> tree = new TreeMap<>();
                for (int i = 0; i < 7; i++) tree.put(i, i * 10);
                NavigableMap<Integer, Integer> source = proxy(NavigableMap.class, tree, trace, "owner");
                NavigableMap<Integer, Integer> view = M3FilteredViews.filterEntries(source, (key, value) -> true);
                trace.failAt = failAt;
                expectIdentity(trace, () -> navigate(view, 3, op));
            }
            Trace trace = new Trace();
            TreeSet<Integer> tree = new TreeSet<>(List.of(0, 1, 2, 3, 4, 5, 6));
            NavigableSet<Integer> source = proxy(NavigableSet.class, tree, trace, "owner");
            NavigableSet<Integer> view = M3MutableFilteredViews.filter(source, value -> true);
            trace.failAt = operation < 2 ? "range.descendingIterator" : "range.iterator";
            expectIdentity(trace, () -> navigate(view, 3, op));

            Trace mapComparator = new Trace();
            TreeMap<Integer, Integer> comparatorMap = new TreeMap<>(mapComparator.comparator());
            for (int i = 0; i < 7; i++) comparatorMap.put(i, i);
            NavigableMap<Integer, Integer> wrappedMap = proxy(NavigableMap.class, comparatorMap, mapComparator, "owner");
            NavigableMap<Integer, Integer> filteredMap = M3FilteredViews.filterEntries(wrappedMap, (key, value) -> true);
            mapComparator.events.clear(); mapComparator.armComparator = true;
            expectIdentity(mapComparator, () -> navigate(filteredMap, 3, op));

            Trace setComparator = new Trace();
            TreeSet<Integer> comparatorSet = new TreeSet<>(setComparator.comparator());
            comparatorSet.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
            NavigableSet<Integer> wrappedSet = proxy(NavigableSet.class, comparatorSet, setComparator, "owner");
            NavigableSet<Integer> filteredSet = M3MutableFilteredViews.filter(wrappedSet, value -> true);
            setComparator.events.clear(); setComparator.armComparator = true;
            expectIdentity(setComparator, () -> navigate(filteredSet, 3, op));
        }
    }
    private static void readOnlyEndpoints() {
        for (boolean empty : new boolean[] {false, true}) {
            for (boolean visible : new boolean[] {false, true}) {
                TreeSet<Integer> owner = new TreeSet<>();
                if (!empty) owner.addAll(List.of(1, 2, 3));
                SortedSet<Integer> sorted = M3FilteredViews.filter((SortedSet<Integer>) owner, value -> visible);
                NavigableSet<Integer> nav = M3FilteredViews.filter((NavigableSet<Integer>) owner, value -> visible);
                for (SortedSet<Integer> view : List.of(sorted, sorted.reversed(), nav, nav.reversed())) {
                    expectReadOnly(view::removeFirst);
                    expectReadOnly(view::removeLast);
                    expectReadOnly(() -> view.addFirst(4));
                    expectReadOnly(() -> view.addLast(4));
                    require(new ArrayList<>(view.reversed().reversed()).equals(new ArrayList<>(view)), "double reverse encounter order");
                }
                require(owner.size() == (empty ? 0 : 3), "read-only owner unchanged");
            }
        }
    }
    private static void expectReadOnly(Runnable action) {
        try { action.run(); throw new AssertionError("read-only endpoint admitted mutation"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }
    @SuppressWarnings("unchecked")
    public static String successfulTrace() {
        StringBuilder transcript = new StringBuilder();
        for (int range = 0; range < 3; range++) {
            for (int filter = 0; filter < 3; filter++) {
                final int visibility = filter;
                for (boolean reversed : new boolean[] {false, true}) {
                    for (int query : new int[] {-3, -1, 0, 2, 4, 7, 10}) {
                        for (int operation = 0; operation < 4; operation++) {
                            for (boolean map : new boolean[] {false, true}) {
                                Trace trace = new Trace();
                                Predicate<Integer> selected = value -> visibility == 0 || (visibility == 1 ? (value & 1) == 0 : false);
                                Predicate<Integer> predicate = value -> { trace.events.add("predicate:" + value); return selected.test(value); };
                                String result;
                                if (map) {
                                    NavigableMap<Integer, Integer> source = new TreeMap<>(trace.comparator());
                                    for (int i = -1; i <= 7; i++) source.put(i, i * 17);
                                    if (range == 1) source = source.subMap(0, false, 7, false);
                                    if (range == 2) source = source.subMap(0, true, 7, true).subMap(2, false, 4, true);
                                    if (reversed) source = source.descendingMap();
                                    NavigableMap<Integer, Integer> oracle = new TreeMap<>(source.comparator());
                                    for (Map.Entry<Integer, Integer> entry : source.entrySet()) if (selected.test(entry.getKey())) oracle.put(entry.getKey(), entry.getValue());
                                    Map.Entry<Integer, Integer> expected = navigate(oracle, query, operation);
                                    NavigableMap<Integer, Integer> wrapped = proxy(NavigableMap.class, source, trace, "owner");
                                    NavigableMap<Integer, Integer> view = M3FilteredViews.filterEntries(wrapped, (key, value) -> predicate.test(key));
                                    trace.events.clear();
                                    Map.Entry<Integer, Integer> actual = navigate(view, query, operation);
                                    require(java.util.Objects.equals(actual, expected), "map bounded recovery");
                                    if (actual != null) expectReadOnly(() -> actual.setValue(99));
                                    result = String.valueOf(actual);
                                } else {
                                    NavigableSet<Integer> source = new TreeSet<>(trace.comparator());
                                    for (int i = -1; i <= 7; i++) source.add(i);
                                    if (range == 1) source = source.subSet(0, false, 7, false);
                                    if (range == 2) source = source.subSet(0, true, 7, true).subSet(2, false, 4, true);
                                    if (reversed) source = source.descendingSet();
                                    NavigableSet<Integer> oracle = new TreeSet<>(source.comparator());
                                    for (Integer value : source) if (selected.test(value)) oracle.add(value);
                                    Integer expected = navigate(oracle, query, operation);
                                    NavigableSet<Integer> wrapped = proxy(NavigableSet.class, source, trace, "owner");
                                    NavigableSet<Integer> view = M3MutableFilteredViews.filter(wrapped, predicate);
                                    trace.events.clear();
                                    Integer actual = navigate(view, query, operation);
                                    require(java.util.Objects.equals(actual, expected), "set bounded recovery");
                                    result = String.valueOf(actual);
                                }
                                transcript.append(range).append(':').append(filter).append(':').append(reversed)
                                        .append(':').append(query).append(':').append(operation).append(':').append(map)
                                        .append(':').append(result).append(':').append(trace.events).append('\n');
                            }
                        }
                    }
                }
            }
        }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(transcript.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }
    public static int run() {
        checks = 0;
        cursorFailures();
        readOnlyEndpoints();
        successfulTrace();
        return checks;
    }
    public static void main(String[] arguments) {
        if (arguments.length > 0 && arguments[0].equals("trace")) {
            System.out.println("TRACE cases=1008 sha256=" + successfulTrace());
        } else {
            System.out.println("PASS filtered checks=" + run());
        }
    }
}
