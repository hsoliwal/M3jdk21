/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free current-owner proof; no stand-in collection implementation. */
public final class M3BulkCaptureContract {
    private static int assertions;
    private M3BulkCaptureContract() { }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void equal(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": " + expected + " != " + actual);
    }
    private static void refuses(Class<? extends Throwable> kind, Runnable action) {
        try { action.run(); }
        catch (RuntimeException error) { check(kind.isInstance(error), "wrong failure " + error); return; }
        throw new AssertionError("missing " + kind.getSimpleName());
    }

    private static void singleReadRegressions() {
        TreeSet<Integer> source = new TreeSet<>();
        var filtered = M3MutableFilteredViews.filter(source, x -> (x & 1) == 0);
        AtomicInteger iterators = new AtomicInteger();
        Collection<Integer> changing = new AbstractCollection<>() {
            @Override public int size() { return 2; }
            @Override public Iterator<Integer> iterator() {
                return (iterators.incrementAndGet() == 1 ? List.of(2, 4) : List.of(3, 5)).iterator();
            }
        };
        filtered.addAll(changing);
        equal(new TreeSet<>(List.of(2, 4)), source, "UNVALIDATED_SECOND_TRAVERSAL_SET");
        equal(1, iterators.get(), "one original collection traversal");

        TreeMap<String, Integer> map = new TreeMap<>();
        var view = M3MutableFilteredViews.filterEntries(map, (k, v) -> v % 2 == 0);
        AtomicInteger entrySets = new AtomicInteger();
        Map<String, Integer> changingMap = new AbstractMap<>() {
            @Override public int size() { return 1; }
            @Override public Set<Entry<String, Integer>> entrySet() {
                return Set.of(Map.entry("a", entrySets.incrementAndGet() == 1 ? 2 : 3));
            }
        };
        view.putAll(changingMap);
        equal(Map.of("a", 2), map, "UNVALIDATED_SECOND_TRAVERSAL_MAP");
        equal(1, entrySets.get(), "one original map traversal");

        AtomicInteger valueReads = new AtomicInteger();
        Map.Entry<String, Integer> volatileEntry = new Map.Entry<>() {
            @Override public String getKey() { return "b"; }
            @Override public Integer getValue() { return valueReads.incrementAndGet() == 1 ? 6 : 7; }
            @Override public Integer setValue(Integer value) { throw new UnsupportedOperationException(); }
        };
        Map<String, Integer> lazyValues = new AbstractMap<>() {
            @Override public int size() { return 1; }
            @Override public Set<Entry<String, Integer>> entrySet() { return Set.of(volatileEntry); }
        };
        view.putAll(lazyValues);
        equal(6, map.get("b"), "admitted value reference only");
        equal(1, valueReads.get(), "one value read");

        // A reusable entry cursor is copied into call-local key/value lanes, not retained.
        Map<String, Integer> reusedEntryMap = new AbstractMap<>() {
            @Override public Set<Entry<String, Integer>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return 3; }
                    @Override public Iterator<Entry<String, Integer>> iterator() {
                        return new Iterator<>() {
                            int position;
                            final Entry<String, Integer> entry = new Entry<>() {
                                @Override public String getKey() { return "r" + position; }
                                @Override public Integer getValue() { return position * 2; }
                                @Override public Integer setValue(Integer v) { throw new UnsupportedOperationException(); }
                            };
                            @Override public boolean hasNext() { return position < 3; }
                            @Override public Entry<String, Integer> next() { position++; return entry; }
                        };
                    }
                };
            }
        };
        view.putAll(reusedEntryMap);
        equal(2, map.get("r1"), "reused entry first");
        equal(4, map.get("r2"), "reused entry second");
        equal(6, map.get("r3"), "reused entry third");
    }

    private static void refusalsAndReferences() {
        TreeSet<Integer> source = new TreeSet<>(List.of(9));
        var set = M3MutableFilteredViews.filter(source, x -> x % 2 == 0);
        refuses(IllegalArgumentException.class, () -> set.addAll(List.of(2, 3, 4)));
        equal(new TreeSet<>(List.of(9)), source, "predicate refusal before writes");
        var throwing = M3MutableFilteredViews.filter(source, x -> {
            if (x == 4) throw new IllegalStateException("predicate failure");
            return true;
        });
        refuses(IllegalStateException.class, () -> throwing.addAll(List.of(2, 4)));
        equal(new TreeSet<>(List.of(9)), source, "predicate throw before writes");
        Collection<Integer> inputFailure = new AbstractCollection<>() {
            @Override public int size() { return 2; }
            @Override public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    int i;
                    @Override public boolean hasNext() { if (i == 1) throw new IllegalStateException("input"); return true; }
                    @Override public Integer next() { i++; return 2; }
                };
            }
        };
        refuses(IllegalStateException.class, () -> set.addAll(inputFailure));
        equal(new TreeSet<>(List.of(9)), source, "input failure before writes");
        refuses(NullPointerException.class, () -> set.addAll(null));

        Comparator<String> order = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        TreeMap<String, Integer> owner = new TreeMap<>(order);
        String first = new String("alpha");
        String alias = new String("ALPHA");
        var view = M3MutableFilteredViews.filterEntries(owner,
                (k, v) -> (k == null || k.equals(k.toLowerCase(java.util.Locale.ROOT)))
                        && (v == null || v % 2 == 0));
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        values.put(first, 2); values.put(alias, 4); values.put(null, null);
        view.putAll(values);
        check(owner.higherKey(null) == first, "first comparator representative identity");
        equal(4, owner.get(alias), "encounter-order last value wins");
        check(view.containsKey(null), "null key/value retained");
        equal(order, view.comparator(), "same comparator object");
        values.clear(); values.put("BETA", 2); values.put("beta", 4);
        Map<String, Integer> before = new LinkedHashMap<>(owner);
        refuses(IllegalArgumentException.class, () -> view.putAll(values));
        equal(before, owner, "rejected first representative never written");
        refuses(UnsupportedOperationException.class, () -> view.navigableKeySet().addAll(List.of()));
        refuses(UnsupportedOperationException.class, () -> view.descendingKeySet().addAll(List.of("z")));
        refuses(UnsupportedOperationException.class, () -> view.navigableKeySet().headSet("z", true).addAll(null));
        owner.put("omega", 8);
        check(view.containsKey("omega"), "still a live view, not a retained payload copy");
        check(!view.containsKey("hidden"), "absent hidden key");
        view.putAll(view); // self-alias remains valid, including nullable values.
        equal(8, owner.get("omega"), "self putAll");
        set.addAll(set);

        TreeMap<Integer, Integer> rangeOwner = new TreeMap<>();
        NavigableMap<Integer, Integer> bounded = rangeOwner.subMap(0, true, 10, false);
        var rangeView = M3MutableFilteredViews.filterEntries(bounded, (k, v) -> true);
        LinkedHashMap<Integer, Integer> outOfRange = new LinkedHashMap<>();
        outOfRange.put(1, 2); outOfRange.put(20, 4);
        refuses(IllegalArgumentException.class, () -> rangeView.putAll(outOfRange));
        equal(Map.of(1, 2), rangeOwner, "owner range failure remains partial, not falsely transactional");
    }

    private static void randomized() {
        Random random = new Random(0x3a7719L);
        for (int trial = 0; trial < 1500; trial++) {
            Comparator<Integer> comparator = trial % 2 == 0 ? Comparator.naturalOrder() : Comparator.reverseOrder();
            TreeMap<Integer, Integer> owner = new TreeMap<>(comparator);
            TreeSet<Integer> setOwner = new TreeSet<>(comparator);
            for (int i = -20; i <= 20; i++) {
                if (random.nextBoolean()) { owner.put(i, i * 2); setOwner.add(i); }
            }
            NavigableMap<Integer, Integer> view = M3MutableFilteredViews.filterEntries(owner,
                    (k, v) -> k % 3 != 0 && (v == null || (v & 1) == 0));
            NavigableSet<Integer> set = M3MutableFilteredViews.filter(setOwner, k -> k % 3 != 0);
            if (trial % 3 == 0) { view = view.descendingMap(); set = set.descendingSet(); }
            var input = new LinkedHashMap<Integer, Integer>();
            var setInput = new ArrayList<Integer>();
            boolean validMap = true, validSet = true;
            for (int i = 0, n = random.nextInt(20); i < n; i++) {
                int key = random.nextInt(51) - 25;
                Integer value = random.nextInt(10) == 0 ? null : random.nextInt(100);
                input.put(key, value); setInput.add(key);
            }
            for (var entry : input.entrySet())
                validMap &= entry.getKey() % 3 != 0 && (entry.getValue() == null || (entry.getValue() & 1) == 0);
            for (int key : setInput) validSet &= key % 3 != 0;
            var expectedMap = new TreeMap<Integer, Integer>(comparator); expectedMap.putAll(owner);
            var expectedSet = new TreeSet<Integer>(comparator); expectedSet.addAll(setOwner);
            if (validMap) { expectedMap.putAll(input); view.putAll(input); }
            else { final var target = view; refuses(IllegalArgumentException.class, () -> target.putAll(input)); }
            if (validSet) { expectedSet.addAll(setInput); set.addAll(setInput); }
            else { final var target = set; refuses(IllegalArgumentException.class, () -> target.addAll(setInput)); }
            equal(expectedMap, owner, "bulk map parity"); equal(expectedSet, setOwner, "bulk set parity");
            var visibleKeys = new ArrayList<Integer>();
            for (int key : (trial % 3 == 0 ? expectedMap.descendingKeySet() : expectedMap.navigableKeySet())) {
                Integer v = expectedMap.get(key);
                if (key % 3 != 0 && (v == null || (v & 1) == 0)) visibleKeys.add(key);
                equal(key % 3 != 0 && (v == null || (v & 1) == 0), view.containsKey(key), "stored visibility");
            }
            equal(visibleKeys, new ArrayList<>(view.navigableKeySet()), "sorted encounter order");
            equal(visibleKeys.size(), view.size(), "visible count");
            var setKeys = new ArrayList<Integer>();
            for (int key : (trial % 3 == 0 ? expectedSet.descendingSet() : expectedSet))
                if (key % 3 != 0) setKeys.add(key);
            equal(setKeys, new ArrayList<>(set), "filtered set order");
        }
    }

    public static void main(String[] args) {
        singleReadRegressions();
        refusalsAndReferences();
        randomized();
        System.out.println("BULK_CAPTURE_PASS trials=1500 assertions=" + assertions + " toolkit=REAL_JDK_OWNERS");
    }
}
