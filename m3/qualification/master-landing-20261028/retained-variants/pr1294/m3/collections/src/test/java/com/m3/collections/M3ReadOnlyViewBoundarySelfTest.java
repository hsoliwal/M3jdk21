/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Explicit read-only effects, nullable nested navigation, and retained JDK reversed read traces. */
public final class M3ReadOnlyViewBoundarySelfTest {
    private static long checks;
    private static int callbacks;
    private static final RuntimeException CALLBACK = new IllegalStateException("mutation callback");

    private M3ReadOnlyViewBoundarySelfTest() { }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(why + ": " + expected + " != " + actual);
    }

    private static void refuse(Runnable action, String why) {
        int before = callbacks;
        checks++;
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            equal(before, callbacks, why + " called predicate or user callback");
            return;
        }
        throw new AssertionError(why + " accepted a mutation");
    }

    private static <E> void collectionWrites(Collection<E> view, E probe) {
        refuse(() -> view.add(probe), "add");
        refuse(() -> view.addAll(List.of()), "empty addAll");
        refuse(() -> view.addAll(null), "null addAll");
        refuse(() -> view.remove(probe), "remove");
        refuse(() -> view.removeAll(List.of()), "empty removeAll");
        refuse(() -> view.removeAll(null), "null removeAll");
        refuse(() -> view.retainAll(List.of()), "retainAll");
        refuse(() -> view.retainAll(null), "null retainAll");
        refuse(() -> view.removeIf(null), "null removeIf");
        refuse(() -> view.removeIf(value -> { callbacks++; throw CALLBACK; }), "removeIf callback");
        refuse(view::clear, "clear");
        Iterator<E> iterator = view.iterator();
        refuse(iterator::remove, "iterator remove before next");
        if (view instanceof SequencedCollection<?> sequence) {
            @SuppressWarnings("unchecked") SequencedCollection<E> sequenced = (SequencedCollection<E>) sequence;
            refuse(() -> sequenced.addFirst(probe), "addFirst");
            refuse(() -> sequenced.addLast(probe), "addLast");
            refuse(sequenced::removeFirst, "removeFirst");
            refuse(sequenced::removeLast, "removeLast");
        }
    }

    private static void mapWrites(SortedMap<Integer, Integer> view) {
        refuse(() -> view.put(null, null), "put nulls");
        refuse(() -> view.putAll(Map.of()), "empty putAll");
        refuse(() -> view.putAll(null), "null putAll");
        refuse(() -> view.remove(99), "absent remove");
        refuse(() -> view.remove(99, 7), "absent conditional remove");
        refuse(() -> view.remove(null, null), "null conditional remove");
        refuse(() -> view.putIfAbsent(99, 7), "putIfAbsent");
        refuse(() -> view.replace(99, 7), "absent replace");
        refuse(() -> view.replace(99, 7, 8), "absent conditional replace");
        refuse(() -> view.replaceAll(null), "null replaceAll");
        refuse(() -> view.replaceAll((k, v) -> { callbacks++; throw CALLBACK; }), "replaceAll callback");
        refuse(() -> view.computeIfAbsent(99, null), "null computeIfAbsent");
        refuse(() -> view.computeIfAbsent(99, k -> { callbacks++; throw CALLBACK; }), "computeIfAbsent callback");
        refuse(() -> view.computeIfPresent(99, null), "null computeIfPresent");
        refuse(() -> view.computeIfPresent(99, (k, v) -> { callbacks++; throw CALLBACK; }), "computeIfPresent callback");
        refuse(() -> view.compute(99, null), "null compute");
        refuse(() -> view.compute(99, (k, v) -> { callbacks++; throw CALLBACK; }), "compute callback");
        refuse(() -> view.merge(99, null, null), "null merge");
        refuse(() -> view.merge(99, 7, (left, right) -> { callbacks++; throw CALLBACK; }), "merge callback");
        refuse(() -> view.putFirst(null, null), "putFirst");
        refuse(() -> view.putLast(null, null), "putLast");
        refuse(view::pollFirstEntry, "pollFirstEntry");
        refuse(view::pollLastEntry, "pollLastEntry");
        refuse(view::clear, "map clear");
        collectionWrites(view.keySet(), 99);
        collectionWrites(view.values(), 99);
        collectionWrites(view.entrySet(), Map.entry(99, 7));
        collectionWrites(view.sequencedKeySet(), 99);
        collectionWrites(view.sequencedValues(), 99);
        collectionWrites(view.sequencedEntrySet(), Map.entry(99, 7));
        collectionWrites(view.sequencedKeySet().reversed(), 99);
        collectionWrites(view.sequencedValues().reversed(), 99);
        collectionWrites(view.sequencedEntrySet().reversed(), Map.entry(99, 7));
        if (view instanceof NavigableMap<Integer, Integer> nav) {
            collectionWrites(nav.navigableKeySet(), 99);
            collectionWrites(nav.descendingKeySet(), 99);
        }
    }

    static void readOnlyMutations() {
        for (boolean populated : new boolean[] {false, true}) {
            TreeSet<Integer> source = new TreeSet<>();
            TreeMap<Integer, Integer> map = new TreeMap<>();
            if (populated) {
                source.addAll(List.of(1, 2, 3, 4));
                source.forEach(value -> map.put(value, value));
            }
            Predicate<Integer> predicate = value -> { callbacks++; return value % 2 == 0; };
            SortedSet<Integer> sorted = M3FilteredViews.filter((SortedSet<Integer>) source, predicate);
            NavigableSet<Integer> nav = M3FilteredViews.filter(source, predicate);
            NavigableSet<Integer> facade = M3FilteredNavigableSet.view(source, predicate);
            for (SortedSet<Integer> set : List.of(sorted, sorted.reversed(), sorted.headSet(4),
                    sorted.reversed().headSet(1), nav, nav.reversed(), nav.headSet(4, false),
                    facade, facade.reversed(), facade.headSet(4, false))) {
                collectionWrites(set, 99);
            }
            SortedMap<Integer, Integer> sortedMap = M3FilteredViews.filterEntries(
                    (SortedMap<Integer, Integer>) map, (k, v) -> predicate.test(k));
            NavigableMap<Integer, Integer> navMap = M3FilteredViews.filterEntries(map, (k, v) -> predicate.test(k));
            for (SortedMap<Integer, Integer> selected : List.of(sortedMap, sortedMap.reversed(),
                    sortedMap.headMap(4), sortedMap.reversed().headMap(1), navMap,
                    navMap.reversed(), navMap.headMap(4, false), navMap.descendingMap().headMap(1, true))) {
                mapWrites(selected);
            }
            equal(populated ? List.of(1, 2, 3, 4) : List.of(), new ArrayList<>(source), "set unchanged");
            equal(source, map.keySet(), "map unchanged");
            equal(true, sorted.reversed().reversed() == sorted, "sorted double reversal keeps base identity");
            equal(true, sortedMap.reversed().reversed() == sortedMap, "map double reversal keeps base identity");
        }
    }

    private static boolean accepted(Integer value, int stage) {
        return switch (stage) {
            case 0 -> !Objects.equals(value, 1);
            case 1 -> !Objects.equals(value, 0);
            case 2 -> value == null || value % 2 == 0;
            default -> throw new AssertionError(stage);
        };
    }

    private static <E> E navigate(NavigableSet<E> set, E query, int operation) {
        return switch (operation) {
            case 0 -> set.lower(query);
            case 1 -> set.floor(query);
            case 2 -> set.ceiling(query);
            case 3 -> set.higher(query);
            default -> throw new AssertionError(operation);
        };
    }

    static void nestedNavigation() {
        Comparator<Integer> middle = Comparator.comparingInt(v -> v == null ? 0 : v < 0 ? v : v + 1);
        List<Comparator<Integer>> orders = List.of(Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.naturalOrder()), middle, middle.reversed());
        for (Comparator<Integer> order : orders) {
            TreeSet<Integer> source = new TreeSet<>(order);
            source.addAll(Arrays.asList(-3, -2, -1, null, 0, 1, 2, 3));
            List<Integer> all = new ArrayList<>(source);
            NavigableSet<Integer> bounded = source.subSet(all.get(1), true, all.get(6), false);
            for (boolean reverse : new boolean[] {false, true}) {
                NavigableSet<Integer> owner = reverse ? bounded.descendingSet() : bounded;
                for (int pattern = 0; pattern < 27; pattern++) {
                    List<String> actual = new ArrayList<>();
                    NavigableSet<Integer> view = owner;
                    int code = pattern;
                    for (int stage = 0; stage < 3; stage++, code /= 3) {
                        int index = stage;
                        Predicate<Integer> predicate = value -> {
                            actual.add(index + ":" + value);
                            return accepted(value, index);
                        };
                        view = switch (code % 3) {
                            case 0 -> M3FilteredViews.filter(view, predicate);
                            case 1 -> M3MutableFilteredViews.filter(view, predicate);
                            case 2 -> M3FilteredNavigableSet.view(view, predicate);
                            default -> throw new AssertionError();
                        };
                    }
                    List<Integer> queries = new ArrayList<>(all);
                    queries.add(-100);
                    queries.add(100);
                    for (Integer query : queries) {
                        for (int operation = 0; operation < 4; operation++) {
                            List<String> expected = new ArrayList<>();
                            Iterator<Integer> values = operation < 2 ? owner.descendingIterator() : owner.iterator();
                            Integer result = null;
                            while (values.hasNext()) {
                                Integer value = values.next();
                                int relation = owner.comparator().compare(value, query);
                                boolean eligible = operation < 2 ? relation < 0 : relation > 0;
                                if (!eligible && !((operation == 1 || operation == 2) && relation == 0)) continue;
                                boolean visible = true;
                                for (int stage = 0; stage < 3; stage++) {
                                    expected.add(stage + ":" + value);
                                    if (!accepted(value, stage)) { visible = false; break; }
                                }
                                if (visible) { result = value; break; }
                            }
                            actual.clear();
                            equal(result, navigate(view, query, operation), "nested navigation result");
                            equal(expected, actual, "nested callback prefix pattern=" + pattern);
                        }
                    }
                }
            }
        }
        RuntimeException marker = new IllegalArgumentException("null unsupported");
        Comparator<Integer> rejecting = (left, right) -> {
            if (left == null || right == null) throw marker;
            return Integer.compare(left, right);
        };
        TreeSet<Integer> raw = new TreeSet<>(rejecting);
        raw.addAll(List.of(1, 2, 3));
        NavigableSet<Integer> bounded = raw.subSet(1, true, 3, false);
        NavigableSet<Integer> view = M3FilteredViews.filter(bounded, x -> true);
        equal(null, view.lower(-100), "do not fabricate unsupported null probe");
        equal(null, view.higher(100), "unsupported null reverse probe");
        equal(2, view.floor(100), "nonnull bounded result");
    }

    private static <K, V> Map.Entry<K, V> navigate(NavigableMap<K, V> map, K query, int operation) {
        return switch (operation) {
            case 0 -> map.lowerEntry(query);
            case 1 -> map.floorEntry(query);
            case 2 -> map.ceilingEntry(query);
            case 3 -> map.higherEntry(query);
            default -> throw new AssertionError(operation);
        };
    }

    static void nestedMapNavigation() {
        Comparator<Integer> middle = Comparator.comparingInt(v -> v == null ? 0 : v < 0 ? v : v + 1);
        List<Comparator<Integer>> orders = List.of(Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.naturalOrder()), middle, middle.reversed());
        for (Comparator<Integer> order : orders) {
            TreeMap<Integer, String> source = new TreeMap<>(order);
            Arrays.asList(-3, -2, -1, null, 0, 1, 2, 3).forEach(key -> source.put(key, "v" + key));
            List<Integer> all = new ArrayList<>(source.keySet());
            NavigableMap<Integer, String> bounded = source.subMap(all.get(1), true, all.get(6), false);
            for (boolean reverse : new boolean[] {false, true}) {
                NavigableMap<Integer, String> owner = reverse ? bounded.descendingMap() : bounded;
                for (int pattern = 0; pattern < 8; pattern++) {
                    NavigableMap<Integer, String> view = owner;
                    for (int stage = 0; stage < 3; stage++) {
                        int index = stage;
                        view = (pattern & (1 << stage)) == 0
                                ? M3FilteredViews.filterEntries(view, (key, value) -> accepted(key, index))
                                : M3MutableFilteredViews.filterEntries(view, (key, value) -> accepted(key, index));
                    }
                    List<Integer> queries = new ArrayList<>(all);
                    queries.add(-100);
                    queries.add(100);
                    for (Integer query : queries) {
                        for (int operation = 0; operation < 4; operation++) {
                            Map.Entry<Integer, String> expected = null;
                            NavigableMap<Integer, String> direction = operation < 2 ? owner.descendingMap() : owner;
                            for (Map.Entry<Integer, String> entry : direction.entrySet()) {
                                int relation = owner.comparator().compare(entry.getKey(), query);
                                boolean eligible = operation < 2 ? relation < 0 : relation > 0;
                                if (!eligible && !((operation == 1 || operation == 2) && relation == 0)) continue;
                                boolean visible = true;
                                for (int stage = 0; stage < 3; stage++) {
                                    if (!accepted(entry.getKey(), stage)) { visible = false; break; }
                                }
                                if (visible) { expected = entry; break; }
                            }
                            Map.Entry<Integer, String> actual = navigate(view, query, operation);
                            equal(expected, actual, "nested map result pattern=" + pattern);
                            if (actual != null) {
                                String oldValue = actual.getValue();
                                refuse(() -> actual.setValue("changed"), "navigation entry immutable");
                                source.put(actual.getKey(), "changed");
                                equal(oldValue, actual.getValue(), "navigation entry detached");
                                equal("changed", navigate(view, query, operation).getValue(), "view remains live");
                                source.put(actual.getKey(), oldValue);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Compared byte-for-byte against the exact predecessor by the external qualification driver. */
    static void readTrace() {
        List<String> seen = new ArrayList<>();
        TreeSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        TreeMap<Integer, Integer> map = new TreeMap<>();
        source.forEach(value -> map.put(value, value));
        SortedSet<Integer> set = M3FilteredViews.filter((SortedSet<Integer>) source,
                value -> { seen.add("s" + value); return value % 2 == 0; });
        SortedMap<Integer, Integer> selected = M3FilteredViews.filterEntries((SortedMap<Integer, Integer>) map,
                (key, value) -> { seen.add("m" + key); return key % 2 == 0; });
        Object[] results = {set.reversed().first(), set.reversed().last(),
                Arrays.toString(set.reversed().toArray()),
                set.reversed().headSet(1).toString(), set.reversed().hashCode(),
                selected.reversed().firstKey(), selected.reversed().lastKey(),
                selected.reversed().entrySet().toString(),
                selected.reversed().headMap(1).toString(), selected.reversed().hashCode()};
        System.out.println("READ_RESULTS " + Arrays.toString(results));
        System.out.println("READ_TRACE " + seen);
        for (int pattern = 0; pattern < 8; pattern++) {
            NavigableMap<Integer, Integer> owner = map.subMap(1, true, 6, true);
            for (int stage = 0; stage < 3; stage++) {
                int index = stage;
                owner = (pattern & (1 << stage)) == 0
                        ? M3FilteredViews.filterEntries(owner, (key, value) -> {
                            seen.add(index + ":" + key); return accepted(key, index);
                        })
                        : M3MutableFilteredViews.filterEntries(owner, (key, value) -> {
                            seen.add(index + ":" + key); return accepted(key, index);
                        });
            }
            for (int operation = 0; operation < 4; operation++) {
                seen.clear();
                Map.Entry<Integer, Integer> found = navigate(owner, 3, operation);
                System.out.println("MAP_TRACE " + pattern + ":" + operation + "=" + found + " " + seen);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--read-trace")) {
            readTrace();
            return;
        }
        readOnlyMutations();
        nestedNavigation();
        nestedMapNavigation();
        System.out.println("PASS M3ReadOnlyViewBoundarySelfTest checks=" + checks);
    }
}
