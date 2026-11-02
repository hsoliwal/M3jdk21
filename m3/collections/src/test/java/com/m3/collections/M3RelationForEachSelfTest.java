/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Target-owned behavior and allocation oracle for the three native relation traversals. */
public final class M3RelationForEachSelfTest {
    private static int checks;
    private static long observed;
    private M3RelationForEachSelfTest() { }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Throwable failure(Runnable action) {
        try { action.run(); } catch (RuntimeException | Error problem) { return problem; }
        throw new AssertionError("expected failure");
    }
    private static void same(Throwable expected, Runnable action, String label) {
        check(failure(action) == expected, label);
    }
    private static void contracts() {
        M3IndexedBiMap<String, Integer> bi = new M3IndexedBiMap<>();
        check(failure(() -> bi.forEach(null)) instanceof NullPointerException, "empty bimap rejects null");
        bi.put(null, 3); bi.put("two", null); bi.put("three", 1);
        List<String> seen = new ArrayList<>();
        bi.forEach((k, v) -> seen.add(k + ":" + v));
        check(seen.equals(List.of("null:3", "two:null", "three:1")), "bimap nulls and forward order");
        seen.clear(); bi.inverse().forEach((k, v) -> seen.add(k + ":" + v));
        check(seen.equals(List.of("3:null", "null:two", "1:three")), "inverse order");
        BiConsumer<String, Integer> identity = (k, v) -> bi.put(k, v);
        bi.forEach(identity);
        bi.forEach((k, v) -> { if (Objects.equals(k, "two")) bi.put(k, 2); });
        check(Objects.equals(bi.inverse().get(2), "two") && !bi.inverse().containsKey(null), "replacement updates reverse index");
        RuntimeException callback = new IllegalStateException("callback");
        Error error = new AssertionError("callback error");
        same(callback, () -> bi.forEach((k, v) -> { throw callback; }), "callback ISE is not wrapped");
        same(error, () -> bi.forEach((k, v) -> { throw error; }), "callback Error identity");
        for (boolean last : new boolean[] {false, true}) {
            M3IndexedBiMap<String, Integer> changed = new M3IndexedBiMap<>();
            changed.put("first", 1); changed.put("last", 2);
            Throwable caught = failure(() -> changed.forEach((k, v) -> {
                if (k.equals(last ? "last" : "first")) changed.put("new", 3);
            }));
            check(caught instanceof ConcurrentModificationException, "bimap structural callback at " + last);
            check(Objects.equals(changed.inverse().get(3), "new"), "structural callback survives");
        }
        Iterator<Map.Entry<String, Integer>> entries = bi.entrySet().iterator();
        Map.Entry<String, Integer> first = entries.next();
        Map.Entry<String, Integer> second = entries.next();
        check(first != second, "fresh public entries");
        Integer removedValue = second.getValue(); second.setValue(7); entries.remove();
        check(!bi.containsKey(second.getKey()) && !bi.inverse().containsKey(7)
                && !bi.inverse().containsKey(removedValue), "entry setValue/removal keeps inverse");

        M3ListMultiValueMap<String, Integer> lists = new M3ListMultiValueMap<>();
        Map<String, Integer> single = lists.asSingleValueMap();
        check(failure(() -> lists.forEach(null)) instanceof NullPointerException, "empty list map null");
        check(failure(() -> single.forEach(null)) instanceof NullPointerException, "empty single view null");
        List<Integer> alias = new ArrayList<>(List.of(4, 5));
        lists.put(null, null); lists.put("empty", List.of()); lists.put("alias", alias);
        List<String> listOrder = new ArrayList<>();
        lists.forEach((k, v) -> { listOrder.add(String.valueOf(k)); if (k != null && k.equals("alias")) check(v == alias, "caller list alias retained"); });
        check(listOrder.equals(java.util.Arrays.asList("null", "empty", "alias")), "list map order and nulls");
        seen.clear(); single.forEach((k, v) -> seen.add(k + ":" + v));
        check(seen.equals(List.of("null:null", "empty:null", "alias:4")), "single null empty first value");
        alias.set(0, 9);
        seen.clear(); single.forEach((k, v) -> seen.add(k + ":" + v));
        check(seen.getLast().equals("alias:9"), "live first-value alias");
        lists.forEach((k, v) -> lists.put(k, v));
        single.forEach((k, v) -> { if (Objects.equals(k, "alias")) single.put(k, 11); });
        check(Objects.equals(lists.getFirst("alias"), 11), "single replacement writes through");
        same(callback, () -> lists.forEach((k, v) -> { throw callback; }), "list callback identity");
        same(callback, () -> single.forEach((k, v) -> { throw callback; }), "single callback identity");
        same(error, () -> lists.forEach((k, v) -> { throw error; }), "list Error identity");
        same(error, () -> single.forEach((k, v) -> { throw error; }), "single Error identity");
        for (boolean view : new boolean[] {false, true}) for (boolean last : new boolean[] {false, true}) {
            M3ListMultiValueMap<String, Integer> changed = new M3ListMultiValueMap<>();
            changed.put("first", List.of(1)); changed.put("last", List.of(2));
            BiConsumer<String, Object> mutation = (k, v) -> {
                if (k.equals(last ? "last" : "first")) changed.put("new", List.of(3));
            };
            Throwable caught = failure(() -> {
                if (view) changed.asSingleValueMap().forEach(mutation);
                else changed.forEach(mutation);
            });
            check(caught instanceof ConcurrentModificationException, "list structural callback " + view + "/" + last);
        }
        lookupExceptionContracts();
    }
    private static final class Key {
        private RuntimeException problem;
        @Override public int hashCode() { if (problem != null) throw problem; return 1; }
    }
    private static void lookupExceptionContracts() {
        Key key = new Key();
        M3IndexedBiMap<Key, Integer> bi = new M3IndexedBiMap<>(); bi.put(key, 1);
        M3ListMultiValueMap<Key, Integer> lists = new M3ListMultiValueMap<>(); lists.put(key, List.of(1));
        IllegalStateException problem = new IllegalStateException("lookup"); key.problem = problem;
        Throwable caught = failure(() -> bi.forEach((k, v) -> { throw new AssertionError("unreachable"); }));
        check(caught instanceof ConcurrentModificationException && caught.getCause() == problem, "bimap wraps value lookup ISE");
        caught = failure(() -> lists.forEach((k, v) -> { throw new AssertionError("unreachable"); }));
        check(caught instanceof ConcurrentModificationException && caught.getCause() == problem, "list wraps value lookup ISE");
        same(problem, () -> lists.asSingleValueMap().forEach((k, v) -> { throw new AssertionError("unreachable"); }), "single iterator value lookup ISE remains direct");
        RuntimeException other = new IllegalArgumentException("lookup other"); key.problem = other;
        same(other, () -> bi.forEach((k, v) -> { }), "bimap other lookup exception");
        same(other, () -> lists.forEach((k, v) -> { }), "list other lookup exception");
    }
    private static void allocation(String owner) {
        final int size = 4096, rounds = 128;
        M3IndexedBiMap<Integer, Integer> bi = new M3IndexedBiMap<>();
        M3ListMultiValueMap<Integer, Integer> lists = new M3ListMultiValueMap<>();
        for (int i = 0; i < size; i++) { bi.put(i, i); lists.put(i, List.of(i)); }
        Map<Integer, Integer> single = lists.asSingleValueMap();
        BiConsumer<Integer, Integer> scalar = (k, v) -> observed += v;
        BiConsumer<Integer, List<Integer>> plural = (k, v) -> observed += v.getFirst();
        Runnable traversal = switch (owner) {
            case "bimap" -> () -> bi.forEach(scalar);
            case "list" -> () -> lists.forEach(plural);
            case "single" -> () -> single.forEach(scalar);
            default -> throw new IllegalArgumentException(owner);
        };
        for (int i = 0; i < 64; i++) traversal.run();
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported(), "allocation accounting supported");
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < rounds; i++) traversal.run();
        long bytes = bean.getThreadAllocatedBytes(thread) - before;
        System.out.println("ALLOC " + owner + " bytes=" + bytes + " mappings=" + ((long) size * rounds));
        // The key iterator/view has constant per-traversal cost. An Entry per mapping exceeds this budget.
        check(bytes < (long) size * rounds * 8, owner + " allocated per-mapping projections: " + bytes);
        check(observed > 0, "traversal consumed values");
    }
    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("contracts")) contracts();
        else allocation(args[0]);
        System.out.println("PASS checks=" + checks);
    }
}
