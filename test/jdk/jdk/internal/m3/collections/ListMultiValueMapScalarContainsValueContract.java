/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

/**
 * Scalar membership retains ordered key/list/query callbacks without entry projections.
 * Run with the actual M3 array JNI library configured by m3.arrays.native.path.
 * The optional "allocation" argument checks bounded workspace under -Xint.
 */
public final class ListMultiValueMapScalarContainsValueContract {
    private static int checks;
    private static volatile boolean observed;

    private ListMultiValueMapScalarContainsValueContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void sameFailure(RuntimeException expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + expected.getClass().getSimpleName());
        } catch (RuntimeException actual) {
            check(actual == expected, "preserve callback exception identity");
        }
    }

    private static final class Key {
        private final String name;
        private final List<String> trace;
        private RuntimeException failure;

        Key(String name, List<String> trace) {
            this.name = name;
            this.trace = trace;
        }

        @Override public int hashCode() {
            trace.add("hash:" + name);
            if (failure != null) throw failure;
            return name.hashCode();
        }
    }

    private static final class TracedList extends AbstractList<Object> {
        private final String name;
        private final List<String> trace;
        private final Object first;
        private RuntimeException failure;

        TracedList(String name, List<String> trace, Object first) {
            this.name = name;
            this.trace = trace;
            this.first = first;
        }

        @Override public int size() { return 1; }
        @Override public boolean isEmpty() {
            trace.add("empty:" + name);
            if (failure != null) throw failure;
            return false;
        }
        @Override public Object get(int index) {
            if (index != 0) throw new IndexOutOfBoundsException(index);
            trace.add("get:" + name);
            return first;
        }
    }

    private static void nullsAndSnapshots() {
        ListMultiValueMap<String, Object> owner = new ListMultiValueMap<>();
        Map<String, Object> view = owner.asSingleValueMap();
        check(!view.containsValue(null), "empty view");
        owner.put("empty", List.of());
        check(view.containsValue(null), "empty list projects to null");
        owner.put("empty", null);
        check(view.containsValue(null), "null list projects to null");
        owner.put("empty", Arrays.asList(null, "tail"));
        check(view.containsValue(null), "null first value");
        check(!view.containsValue("tail"), "membership reads only first values");
        owner.set("empty", "first");
        check(view.containsValue("first"), "view observes replacement");
        Map.Entry<String, Object> entry = view.entrySet().iterator().next();
        check(entry != view.entrySet().iterator().next(), "caller owns fresh entry snapshots");
        try {
            entry.setValue("changed");
            throw new AssertionError("entry snapshot must remain immutable");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        owner.set("empty", "second");
        check(entry.getValue().equals("first"), "entry snapshot remains detached");
        check(view.containsValue("second"), "live view remains current");
    }

    private static void callbacksAndAbruptCompletion() {
        List<String> trace = new ArrayList<>();
        ListMultiValueMap<Key, Object> owner = new ListMultiValueMap<>();
        Key one = new Key("one", trace), two = new Key("two", trace);
        TracedList first = new TracedList("one", trace, "a");
        owner.put(one, first);
        owner.put(two, new TracedList("two", trace, "b"));
        Map<Key, Object> view = owner.asSingleValueMap();
        Object probe = new Object() {
            @Override public boolean equals(Object value) {
                trace.add("equals:" + value);
                return "a".equals(value);
            }
            @Override public int hashCode() { return 0; }
        };
        trace.clear();
        check(view.containsValue(probe), "early matching value");
        check(trace.equals(List.of("hash:one", "empty:one", "get:one", "equals:a")),
                "key, list and query callbacks retain order and early exit: " + trace);

        for (RuntimeException failure : List.of(new NullPointerException("key"),
                new ClassCastException("key"), new IllegalStateException("key"))) {
            one.failure = failure;
            sameFailure(failure, () -> view.containsValue(null));
        }
        one.failure = null;
        for (RuntimeException failure : List.of(new NullPointerException("list"),
                new ClassCastException("list"), new IllegalStateException("list"))) {
            first.failure = failure;
            sameFailure(failure, () -> view.containsValue(null));
        }
        first.failure = null;
        for (RuntimeException failure : List.of(new NullPointerException("equals"),
                new ClassCastException("equals"), new IllegalStateException("equals"))) {
            Object throwing = new Object() {
                @Override public boolean equals(Object value) { throw failure; }
                @Override public int hashCode() { return 0; }
            };
            sameFailure(failure, () -> view.containsValue(throwing));
        }

        int[] calls = {0};
        Object identical = new Object() {
            @Override public boolean equals(Object value) { calls[0]++; return this == value; }
            @Override public int hashCode() { return 0; }
        };
        owner.clear();
        owner.set(one, identical);
        check(view.containsValue(identical) && calls[0] == 1,
                "even identical query/value invokes the query equals callback");
    }

    private static void reentrantMatchAndMiss() {
        for (boolean match : new boolean[] {false, true}) {
            ListMultiValueMap<String, Object> owner = new ListMultiValueMap<>();
            owner.set("one", "a");
            owner.set("two", "b");
            Object query = new Object() {
                @Override public boolean equals(Object value) { owner.clear(); return match; }
                @Override public int hashCode() { return 0; }
            };
            if (match) {
                check(owner.asSingleValueMap().containsValue(query), "matching callback returns immediately");
            } else {
                try {
                    owner.asSingleValueMap().containsValue(query);
                    throw new AssertionError("miss after mutation must check the next iterator epoch");
                } catch (ConcurrentModificationException expected) {
                    checks++;
                }
            }
            check(owner.isEmpty(), "callback mutation is retained");
        }
    }

    private static Map<Integer, Object> membershipView(int size) {
        ListMultiValueMap<Integer, Object> owner = new ListMultiValueMap<>();
        List<Object> value = List.of("present");
        for (int i = 0; i < size; i++) owner.put(i, value);
        return owner.asSingleValueMap();
    }

    private static long allocated(com.sun.management.ThreadMXBean bean, Map<Integer, Object> view) {
        Object absent = new Object();
        for (int i = 0; i < 8; i++) observed = view.containsValue(absent);
        long before = bean.getCurrentThreadAllocatedBytes();
        observed = view.containsValue(absent);
        return bean.getCurrentThreadAllocatedBytes() - before;
    }

    private static void boundedAllocation() {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported(), "allocation measurement supported");
        bean.setThreadAllocatedMemoryEnabled(true);
        long small = allocated(bean, membershipView(64));
        long large = allocated(bean, membershipView(4096));
        System.out.println("ScalarContainsValue allocation bytes: 64=" + small + " 4096=" + large);
        check(large <= small + 4096, "membership workspace must not grow per mapping");
        check(!observed, "allocation probe must remain an absent-value scan");
    }

    public static void main(String[] args) {
        nullsAndSnapshots();
        callbacksAndAbruptCompletion();
        reentrantMatchAndMiss();
        if (args.length != 0 && args[0].equals("allocation")) boundedAllocation();
        System.out.println("ListMultiValueMapScalarContainsValueContract PASS checks=" + checks);
    }
}
