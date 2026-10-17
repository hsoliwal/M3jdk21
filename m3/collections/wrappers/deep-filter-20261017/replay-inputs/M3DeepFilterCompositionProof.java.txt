// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Exact owner tests. Run with -Xss256k to expose linear predicate recursion. */
public final class M3DeepFilterCompositionProof {
    private static long checks;
    private M3DeepFilterCompositionProof() { }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void equal(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual), label + ": " + expected + " != " + actual);
    }
    private static <T extends Throwable> T expect(Class<T> type, Runnable task) {
        try { task.run(); }
        catch (Throwable error) {
            if (!type.isInstance(error)) throw new AssertionError("wrong failure", error);
            checks++;
            return type.cast(error);
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static List<Integer> rows(Collection<Integer> view) {
        return view.stream().toList();
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(object);
            } catch (NoSuchFieldException absent) { /* Continue through wrapper superclass. */ }
        }
        throw new NoSuchFieldException(name);
    }
    private static void shape(Object view, int leaves) throws ReflectiveOperationException {
        Object predicate = field(view, "predicate");
        Object root = field(predicate, "root");
        int maximum = leaves <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(leaves - 1);
        equal(maximum, field(root, "height"), "logarithmic composition height");
        List<Object> pending = new ArrayList<>();
        pending.add(root);
        int seen = 0;
        while (!pending.isEmpty()) {
            Object node = pending.removeLast();
            Object leaf = field(node, "leaf");
            if (leaf != null) seen++;
            else {
                pending.add(field(node, "right"));
                pending.add(field(node, "left"));
            }
            for (Field f : node.getClass().getDeclaredFields()) {
                check(java.lang.reflect.Modifier.isFinal(f.getModifiers()), "mutable predicate node");
            }
        }
        equal(leaves, seen, "no deduplicated predicate occurrences");
    }
    private static void deepCollection(int depth, boolean inspect) throws Exception {
        ArrayList<Integer> source = new ArrayList<>(List.of(7));
        AtomicInteger calls = new AtomicInteger();
        Predicate<Integer> same = value -> { calls.incrementAndGet(); return value != null; };
        Collection<Integer> view = source;
        for (int i = 0; i < depth; i++) view = M3CollectionViews.filtered(view, same);
        equal(0, calls.get(), "constructor evaluated predicates");
        if (inspect) shape(view, depth);
        Spliterator<Integer> spliterator = view.spliterator();
        spliterator.estimateSize();
        check(!spliterator.hasCharacteristics(Spliterator.SIZED), "false sized");
        equal(0, calls.get(), "metadata evaluated predicates");
        equal(List.of(7), rows(view), "deep collection traversal");
        equal(depth, calls.get(), "repeated callback occurrence count");
        source.add(9);
        source.add(null);
        equal(List.of(7, 9), rows(view), "deep collection stays live");
        Collection<Integer> prefix = view;
        Collection<Integer> reject = M3CollectionViews.filtered(view, value -> false);
        equal(List.of(), rows(reject), "rejecting suffix");
        equal(List.of(7, 9), rows(prefix), "append mutated retained prefix");
        check(field(view, "backing") == source, "second element owner or wrapper chain");
        check(view.remove(7), "deep removal");
        view.clear();
        equal(Arrays.asList((Integer) null), source, "hidden row survives clear");
    }
    private static void deepMap(int depth, boolean inspect) throws Exception {
        Map<Integer, Integer> source = new LinkedHashMap<>();
        source.put(7, 70);
        AtomicInteger calls = new AtomicInteger();
        BiPredicate<Integer, Integer> same = (key, value) -> {
            calls.incrementAndGet(); return value != null;
        };
        Map<Integer, Integer> view = source;
        for (int i = 0; i < depth; i++) view = M3FilteredMaps.filtered(view, same);
        equal(0, calls.get(), "map constructor evaluated predicates");
        if (inspect) shape(view, depth);
        equal(70, view.get(7), "deep map get");
        equal(depth, calls.get(), "map callback occurrences");
        source.put(9, null);
        equal(List.of(7), view.keySet().stream().toList(), "hidden null mapping");
        Map<Integer, Integer> guarded = view;
        expect(IllegalArgumentException.class, () -> guarded.entrySet().iterator().next().setValue(null));
        equal(70, source.get(7), "guarded entry refusal changed source");
        check(field(view, "backing") == source, "map retained wrapper chain");
        Map<Integer, Integer> old = view;
        view = M3FilteredMaps.filtered(view, (key, value) -> false);
        check(view.isEmpty(), "map false suffix");
        equal(70, old.get(7), "map append changed old plan");
        old.clear();
        check(source.size() == 1 && source.containsKey(9), "hidden map entry lost");
    }
    private static void sortedViews(int depth) throws Exception {
        Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
        NavigableSet<Integer> source = new TreeSet<>(order);
        source.addAll(Arrays.asList(null, -3, -2, -1, 0, 1, 2, 3));
        Collection<Integer> erased = source;
        NavigableSet<Integer> view = (NavigableSet<Integer>) M3CollectionViews.filtered(erased, Objects::nonNull);
        for (int i = 1; i < depth; i++) view = M3CollectionViews.filtered(view, value -> true);
        check(view.comparator() == order, "comparator identity lost");
        NavigableSet<Integer> range = view.subSet(-2, true, 3, false).descendingSet();
        equal(List.of(2, 1, 0, -1, -2), rows(range), "deep reversed bounded set");
        equal(2, range.ceiling(100), "out-of-range descending navigation");
        equal(-2, range.floor(-100), "out-of-range floor");
        source.remove(1);
        equal(List.of(2, 0, -1, -2), rows(range), "range not live");
        equal(2, range.pollFirst(), "poll first through deep predicate");
        check(!source.contains(2), "poll not write-through");
        NavigableSet<Integer> frozen = Collections.unmodifiableNavigableSet(view);
        expect(UnsupportedOperationException.class, () -> frozen.add(99));
        SortedSet<Integer> nonNavigable = Collections.unmodifiableSortedSet(source);
        SortedSet<Integer> sortedOnly = M3CollectionViews.filtered(nonNavigable, Objects::nonNull);
        for (int i = 1; i < depth; i++) sortedOnly = M3CollectionViews.filtered(sortedOnly, value -> true);
        equal(-3, sortedOnly.first(), "sorted-only capability");

        NavigableMap<String, Integer> mapSource = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        mapSource.put("Alpha", 1); mapSource.put("Beta", 2); mapSource.put("Gamma", null);
        NavigableMap<String, Integer> map = M3FilteredMaps.filtered(mapSource,
                (key, value) -> Character.isUpperCase(key.charAt(0)) && value != null);
        for (int i = 1; i < depth; i++) map = M3FilteredMaps.filtered(map, (key, value) -> true);
        equal(1, map.get("ALPHA"), "stored comparator representative");
        equal(List.of("Beta", "Alpha"), map.descendingKeySet().stream().toList(), "descending map keys");
        equal("Alpha", map.subMap("A", true, "C", false).ceilingKey("0"), "bounded map lookup");
        map.put("ALPHA", 4);
        equal("Alpha", mapSource.firstKey(), "stored key replaced by probe");
        equal(4, mapSource.get("alpha"), "map write did not reach source");
        Map.Entry<String, Integer> snapshot = map.firstEntry();
        expect(UnsupportedOperationException.class, () -> snapshot.setValue(8));
        map.clear();
        check(mapSource.size() == 1 && mapSource.containsKey("Gamma"), "hidden sorted mapping lost");
        SortedMap<String, Integer> sortedMap = Collections.unmodifiableSortedMap(mapSource);
        SortedMap<String, Integer> filteredMap = M3FilteredMaps.filtered(sortedMap, (key, value) -> true);
        for (int i = 1; i < depth; i++) filteredMap = M3FilteredMaps.filtered(filteredMap, (key, value) -> true);
        equal("Gamma", filteredMap.firstKey(), "sorted-only map");
    }
    private static void orderAndExceptions() {
        for (int stop : new int[] {0, 1, 2, 7, 31, 32, 63, 127, 128, 255, 256}) {
            List<Integer> trace = new ArrayList<>();
            Collection<Integer> view = List.of(4);
            for (int i = 0; i < 257; i++) {
                int stage = i;
                view = M3CollectionViews.filtered(view, value -> { trace.add(stage); return stage != stop; });
            }
            check(view.isEmpty(), "false predicate ignored");
            equal(java.util.stream.IntStream.rangeClosed(0, stop).boxed().toList(), trace,
                    "short-circuit order changed");
            RuntimeException marker = new IllegalStateException("marker");
            Map<Integer, Integer> map = new LinkedHashMap<>(Map.of(1, 2));
            trace.clear();
            for (int i = 0; i < 257; i++) {
                int stage = i;
                map = M3FilteredMaps.filtered(map, (key, value) -> {
                    trace.add(stage);
                    if (stage == stop) throw marker;
                    return true;
                });
            }
            Map<Integer, Integer> exact = map;
            check(expect(RuntimeException.class, () -> exact.get(1)) == marker, "exception object replaced");
            equal(java.util.stream.IntStream.rangeClosed(0, stop).boxed().toList(), trace,
                    "exception order changed");
        }
        Collection<Integer> nulls = M3CollectionViews.filtered(Arrays.asList(null, 1), Objects::nonNull);
        nulls = M3CollectionViews.filtered(nulls, n -> n > 0);
        equal(List.of(1), rows(nulls), "null guard order");
        Collection<Integer> existing = nulls;
        expect(NullPointerException.class, () -> M3CollectionViews.filtered(existing, null));
        expect(NullPointerException.class, () -> M3FilteredMaps.filtered(Map.of(1, 2), null));
        Iterator<Integer> iterator = nulls.iterator();
        iterator.next();
        expect(UnsupportedOperationException.class, iterator::remove);
    }
    private static void differential() {
        for (int seed = 0; seed < 192; seed++) {
            Random random = new Random(seed);
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < 80; i++) input.add(random.nextInt(17) == 0 ? null : random.nextInt(101) - 50);
            List<Predicate<Integer>> predicates = new ArrayList<>();
            predicates.add(Objects::nonNull);
            for (int i = 0; i < 1 + seed % 31; i++) {
                int modulus = 2 + random.nextInt(13);
                int residue = random.nextInt(modulus);
                predicates.add(value -> Math.floorMod(value, modulus) != residue);
            }
            List<Integer> expected = new ArrayList<>();
            for (Integer value : input) {
                boolean keep = true;
                for (Predicate<Integer> predicate : predicates) if (!predicate.test(value)) { keep = false; break; }
                if (keep) expected.add(value);
            }
            Collection<Integer> view = input;
            for (Predicate<Integer> predicate : predicates) view = M3CollectionViews.filtered(view, predicate);
            equal(expected, rows(view), "differential list");
            equal(expected, view.parallelStream().toList(), "parallel shared immutable plan");
            equal(expected.size(), view.size(), "exact filtered size");
            List<Integer> sorted = new ArrayList<>(expected); sorted.sort(Comparator.reverseOrder());
            equal(sorted, M3CollectionViews.sortedSnapshot(view, Comparator.reverseOrder()), "stable snapshot");
            Set<Integer> set = new LinkedHashSet<>(input);
            Set<Integer> filteredSet = set;
            for (Predicate<Integer> predicate : predicates) filteredSet = M3CollectionViews.filtered(filteredSet, predicate);
            equal(new LinkedHashSet<>(expected), filteredSet, "set contract");
            Map<Integer, Integer> backing = new LinkedHashMap<>();
            for (int i = 0; i < input.size(); i++) backing.put(i, input.get(i));
            Map<Integer, Integer> filtered = backing;
            for (Predicate<Integer> predicate : predicates) filtered = M3FilteredMaps.filtered(filtered, (k, v) -> predicate.test(v));
            equal(expected, filtered.values().stream().toList(), "map chain differential");
            int checkpoint = input.size();
            backing.put(checkpoint, 103);
            boolean keep = predicates.stream().allMatch(predicate -> predicate.test(103));
            check(filtered.containsKey(checkpoint) == keep, "live map new row");
        }
    }
    private static void sharedPlanAndReentrancy() {
        Collection<Integer> source = java.util.stream.IntStream.range(0, 1024).boxed().toList();
        Collection<Integer> view = source;
        for (int i = 0; i < 4096; i++) view = M3CollectionViews.filtered(view, value -> true);
        Collection<Integer> immutable = view;
        List<CompletableFuture<Long>> workers = new ArrayList<>();
        for (int i = 0; i < 4; i++) workers.add(CompletableFuture.supplyAsync(() -> immutable.stream().count()));
        for (CompletableFuture<Long> worker : workers) equal(1024L, worker.join(), "concurrent read plan");
        Collection<Integer> reentrant = M3CollectionViews.filtered(view, value -> immutable.contains(value));
        check(reentrant.contains(9), "reentrant traversal state corruption");
    }
    public static void main(String[] args) throws Exception {
        int depth = Integer.getInteger("m3.deep.depth", 65_536);
        if (depth < 2) throw new IllegalArgumentException("depth must be >= 2");
        if (args.length != 0 && args[0].equals("collection-witness")) {
            deepCollection(depth, false); return;
        }
        if (args.length != 0 && args[0].equals("map-witness")) {
            deepMap(depth, false); return;
        }
        deepCollection(depth, true);
        deepMap(depth, true);
        sortedViews(Math.min(depth, 16_384));
        orderAndExceptions();
        differential();
        sharedPlanAndReentrancy();
        System.out.println("M3_DEEP_FILTER_PASS checks=" + checks + " depth=" + depth + " seeds=192");
    }
}
