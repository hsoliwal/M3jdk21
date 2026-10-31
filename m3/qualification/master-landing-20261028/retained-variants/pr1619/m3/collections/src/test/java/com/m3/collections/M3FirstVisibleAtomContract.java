// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Exact caller-boundary traces and failure prefixes for private first-visible selection. */
public final class M3FirstVisibleAtomContract {
    private M3FirstVisibleAtomContract() { }

    private static final class Trace {
        final ArrayList<String> tape = new ArrayList<>();
        final ArrayList<Integer> faultPositions = new ArrayList<>();
        final int failAt;
        final Throwable marker;
        int faultable;

        Trace(int failAt, boolean error) {
            this.failAt = failAt;
            marker = error ? new AssertionError("caller-owned error")
                    : new IllegalArgumentException("caller-owned exception");
            marker.setStackTrace(new StackTraceElement[0]);
        }

        void record(String event) { tape.add(event); }

        void callback(String event) {
            faultPositions.add(tape.size());
            tape.add(event);
            if (++faultable == failAt) {
                if (marker instanceof Error error) throw error;
                throw (RuntimeException) marker;
            }
        }
    }

    private static Object invoke(Object source, Method method, Object[] arguments) throws Throwable {
        try { return method.invoke(source, arguments); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }

    private static String arguments(Object[] values) {
        if (values == null) return "";
        StringBuilder result = new StringBuilder();
        for (Object value : values) {
            if (!result.isEmpty()) result.append(',');
            result.append(value);
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static NavigableMap<Integer, String> tracedMap(
            NavigableMap<Integer, String> source, Trace trace, String route) {
        return (NavigableMap<Integer, String>) Proxy.newProxyInstance(
                M3FirstVisibleAtomContract.class.getClassLoader(),
                new Class<?>[] {NavigableMap.class}, (proxy, method, arguments) -> {
                    String next = route + "." + method.getName();
                    trace.record(next + "(" + arguments(arguments) + ")");
                    Object result = invoke(source, method, arguments);
                    if (result instanceof NavigableMap<?, ?> map) {
                        return tracedMap((NavigableMap<Integer, String>) map, trace, next);
                    }
                    if (method.getName().equals("entrySet")) {
                        Set<Map.Entry<Integer, String>> entries = (Set<Map.Entry<Integer, String>>) result;
                        return new AbstractSet<Map.Entry<Integer, String>>() {
                            @Override public int size() { return entries.size(); }
                            @Override public Iterator<Map.Entry<Integer, String>> iterator() {
                                trace.record(next + ".iterator()");
                                Iterator<Map.Entry<Integer, String>> cursor = entries.iterator();
                                return new Iterator<>() {
                                    @Override public boolean hasNext() {
                                        trace.callback(next + ".hasNext()");
                                        return cursor.hasNext();
                                    }
                                    @Override public Map.Entry<Integer, String> next() {
                                        trace.callback(next + ".next()");
                                        return tracedEntry(cursor.next(), trace, next);
                                    }
                                };
                            }
                        };
                    }
                    return result;
                });
    }

    private static Map.Entry<Integer, String> tracedEntry(
            Map.Entry<Integer, String> source, Trace trace, String route) {
        return new Map.Entry<>() {
            @Override public Integer getKey() {
                trace.callback(route + ".getKey()");
                return source.getKey();
            }
            @Override public String getValue() {
                trace.callback(route + ".getValue()");
                return source.getValue();
            }
            @Override public String setValue(String value) { return source.setValue(value); }
        };
    }

    @SuppressWarnings("unchecked")
    private static NavigableSet<Integer> tracedSet(
            NavigableSet<Integer> source, Trace trace, String route) {
        return (NavigableSet<Integer>) Proxy.newProxyInstance(
                M3FirstVisibleAtomContract.class.getClassLoader(),
                new Class<?>[] {NavigableSet.class}, (proxy, method, arguments) -> {
                    String next = route + "." + method.getName();
                    trace.record(next + "(" + arguments(arguments) + ")");
                    Object result = invoke(source, method, arguments);
                    if (result instanceof NavigableSet<?> set) {
                        return tracedSet((NavigableSet<Integer>) set, trace, next);
                    }
                    if (result instanceof Iterator<?> values) {
                        Iterator<Integer> cursor = (Iterator<Integer>) values;
                        return new Iterator<Integer>() {
                            @Override public boolean hasNext() {
                                trace.callback(next + ".hasNext()");
                                return cursor.hasNext();
                            }
                            @Override public Integer next() {
                                trace.callback(next + ".next()");
                                return cursor.next();
                            }
                        };
                    }
                    return result;
                });
    }

    private static boolean visible(int selector, Integer key) {
        return switch (selector) {
            case 0 -> true;
            case 1 -> key != null && (key == 2 || key == 4);
            case 2 -> false;
            case 3 -> key == null;
            default -> throw new AssertionError("selector");
        };
    }

    private static Map.Entry<Integer, String> mapOperation(
            NavigableMap<Integer, String> source, int operation) {
        return switch (operation) {
            case 0 -> source.firstEntry();
            case 1 -> source.lastEntry();
            case 2 -> source.lowerEntry(7);
            case 3 -> source.ceilingEntry(3);
            default -> throw new AssertionError("operation");
        };
    }

    private static Integer setOperation(NavigableSet<Integer> source, int operation) {
        return switch (operation) {
            case 0 -> source.lower(7);
            case 1 -> source.floor(7);
            case 2 -> source.ceiling(3);
            case 3 -> source.higher(3);
            default -> throw new AssertionError("operation");
        };
    }

    private static Object exercise(boolean map, boolean descending, int selector, int operation, Trace trace) {
        Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
        TreeMap<Integer, String> owner = new TreeMap<>(order);
        owner.put(null, null);
        for (int key : new int[] {2, 4, 6, 8}) owner.put(key, key == 6 ? null : new String("value-" + key));
        NavigableMap<Integer, String> backing = descending ? owner.descendingMap() : owner;
        TreeMap<Integer, String> expected = new TreeMap<>(backing.comparator());
        backing.forEach((key, value) -> { if (visible(selector, key)) expected.put(key, value); });
        if (map) {
            NavigableMap<Integer, String> filtered = M3FilteredViews.filterEntries(
                    tracedMap(backing, trace, "map"), (key, value) -> {
                        trace.callback("predicate(" + key + "," + value + ")");
                        return visible(selector, key);
                    });
            require(trace.tape.isEmpty(), "map construction remains lazy");
            Map.Entry<Integer, String> result = mapOperation(filtered, operation);
            Map.Entry<Integer, String> oracle = mapOperation(expected, operation);
            require(Objects.equals(oracle, result), "map selection result");
            if (result != null) {
                require(result.getKey() == oracle.getKey(), "map key identity");
                require(result.getValue() == oracle.getValue(), "map value identity");
            }
            return result;
        }
        NavigableSet<Integer> filtered = M3FilteredViews.filter(
                tracedSet(backing.navigableKeySet(), trace, "set"), key -> {
                    trace.callback("predicate(" + key + ")");
                    return visible(selector, key);
                });
        require(trace.tape.isEmpty(), "set construction remains lazy");
        Integer result = setOperation(filtered, operation);
        require(Objects.equals(setOperation(expected.navigableKeySet(), operation), result),
                "set selection result");
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Returns the complete deterministic transcript digest for the owning JUnit adapter. */
    public static String verify() {
        StringBuilder transcript = new StringBuilder();
        int normalCases = 0;
        int failureCases = 0;
        for (boolean map : new boolean[] {false, true}) {
            for (boolean descending : new boolean[] {false, true}) {
                for (int selector = 0; selector < 4; selector++) {
                    for (int operation = 0; operation < 4; operation++) {
                        String scenario = map + ":" + descending + ":" + selector + ":" + operation;
                        Trace success = new Trace(0, false);
                        Object value = exercise(map, descending, selector, operation, success);
                        transcript.append(scenario).append(':').append(value).append(':')
                                .append(success.tape).append('\n');
                        normalCases++;
                        for (int point = 1; point <= success.faultPositions.size(); point++) {
                            List<String> prefix = success.tape.subList(0, success.faultPositions.get(point - 1) + 1);
                            for (boolean error : new boolean[] {false, true}) {
                                Trace failure = new Trace(point, error);
                                Throwable caught = null;
                                try { exercise(map, descending, selector, operation, failure); }
                                catch (RuntimeException | Error thrown) { caught = thrown; }
                                require(caught == failure.marker, "failure identity: " + scenario + ":" + point);
                                require(prefix.equals(failure.tape), "failure prefix/termination: " + scenario + ":" + point);
                                transcript.append(scenario).append(':').append(point).append(':').append(error)
                                        .append(':').append(failure.tape).append('\n');
                                failureCases++;
                            }
                        }
                    }
                }
            }
        }
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(transcript.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
        System.out.println("PASS first-visible normal=" + normalCases + " failureProbes=" + failureCases
                + " tape=" + hash);
        return hash;
    }

    public static void main(String[] args) { verify(); }
}
