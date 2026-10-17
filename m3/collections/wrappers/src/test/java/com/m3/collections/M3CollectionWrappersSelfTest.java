/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** JDK-only contract and randomized differential proof for purpose wrappers. */
public final class M3CollectionWrappersSelfTest {
    private static int checks;

    private M3CollectionWrappersSelfTest() {
    }

    public static void main(String[] args) {
        readOnlyRetainsShapeAndLiveness();
        synchronizedJdkDelegation();
        checkedJdkDelegation();
        visibilityVersusAdmission();
        listAdmissionAndReplacement();
        fixedSizeVersusReadOnly();
        capacityAndMutationPaths();
        mappedLiveReadOnly();
        concatenatedLiveReadOnly();
        flatMapAndValueProjection();
        rangesPagesAndReverse();
        compositionOrderAndContracts();
        distinctAndDetachedCopies();
        negativeArguments();
        randomizedContracts();
        System.out.println("PASS M3CollectionWrappers checks=" + checks);
    }

    private static void readOnlyRetainsShapeAndLiveness() {
        List<String> backing = new ArrayList<>(List.of("a", "b"));
        List<String> view = M3CollectionWrappers.readOnly(backing);
        eq(List.of("a", "b"), view);
        backing.add("c");
        eq(List.of("a", "b", "c"), view);
        throwsType(UnsupportedOperationException.class, () -> view.set(0, "z"));
        throwsType(UnsupportedOperationException.class, () -> view.clear());
        throwsType(UnsupportedOperationException.class, () -> view.reversed().add("d"));

        Set<Integer> source = new HashSet<>(Set.of(1, 2));
        Set<Integer> set = M3CollectionWrappers.readOnly(source);
        eq(Set.of(1, 2), set);
        eq(Set.of(1, 2).hashCode(), set.hashCode());
        source.add(3);
        eq(Set.of(1, 2, 3), set);
        throwsType(UnsupportedOperationException.class, () -> set.add(4));

        NavigableSet<Integer> navBacking = new TreeSet<>(Set.of(1, 3, 5));
        NavigableSet<Integer> nav = M3CollectionWrappers.readOnly(navBacking);
        eq(3, nav.ceiling(2));
        eq(List.of(5, 3, 1), new ArrayList<>(nav.descendingSet()));
        throwsType(UnsupportedOperationException.class, () -> nav.pollFirst());
        throwsType(UnsupportedOperationException.class, () -> nav.headSet(4, true).clear());

        NavigableMap<Integer, String> mapBacking = new TreeMap<>();
        mapBacking.put(1, "a");
        mapBacking.put(2, "b");
        NavigableMap<Integer, String> map = M3CollectionWrappers.readOnly(mapBacking);
        eq(2, map.lastKey());
        eq(Map.of(1, "a"), map.headMap(2, false));
        mapBacking.put(3, "c");
        eq("c", map.get(3));
        throwsType(UnsupportedOperationException.class, () -> map.put(4, "d"));
        throwsType(UnsupportedOperationException.class,
                () -> map.firstEntry().setValue("changed"));
        throwsType(UnsupportedOperationException.class,
                () -> map.descendingMap().pollFirstEntry());
    }

    private static void synchronizedJdkDelegation() {
        List<Integer> backing = new ArrayList<>();
        List<Integer> guarded = M3CollectionWrappers.synchronizedView(backing);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            final int offset = t * 250;
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 250; i++) guarded.add(offset + i);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(error);
            }
        }
        eq(1000, guarded.size());
        synchronized (guarded) {
            eq(1000, new HashSet<>(guarded).size());
        }
        backing.add(1001);
        eq(1001, guarded.size());
        NavigableSet<Integer> sorted = M3CollectionWrappers.synchronizedView(
                new TreeSet<>(Set.of(1, 2, 3)));
        eq(3, sorted.last());
        Map<Integer, Integer> map = M3CollectionWrappers.synchronizedView(
                new TreeMap<>(Map.of(1, 5)));
        eq(5, map.get(1));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkedJdkDelegation() {
        List<String> source = new ArrayList<>();
        List<String> checked = M3CollectionWrappers.checked(source, String.class);
        checked.add("hello");
        List raw = checked;
        throwsType(ClassCastException.class, () -> raw.add(42));
        throwsType(ClassCastException.class, () -> raw.set(0, 12));
        eq(List.of("hello"), source);
        Set<Integer> set = M3CollectionWrappers.checked(new TreeSet<>(), Integer.class);
        ((Set) set).add(2);
        throwsType(ClassCastException.class, () -> ((Set) set).add("wrong"));
        eq(Set.of(2), set);
        Map<String, Integer> map = M3CollectionWrappers.checked(
                new TreeMap<>(), String.class, Integer.class);
        map.put("one", 1);
        Map rawMap = map;
        throwsType(ClassCastException.class, () -> rawMap.put(1, 2));
        throwsType(ClassCastException.class, () -> rawMap.put("two", "bad"));
        Map.Entry rawEntry = (Map.Entry) map.entrySet().iterator().next();
        throwsType(ClassCastException.class, () -> rawEntry.setValue("bad"));
        eq(Map.of("one", 1), map);
    }

    private static void visibilityVersusAdmission() {
        ArrayList<Integer> backing = new ArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> visible = M3CollectionWrappers.filtered(backing, x -> x % 2 == 0);
        Collection<Integer> admission = M3CollectionWrappers.admissionFiltered(
                backing, x -> x % 2 == 0);
        eq(List.of(2, 4), new ArrayList<>(visible));
        eq(List.of(1, 3), new ArrayList<>(M3CollectionWrappers.rejected(backing, x -> x % 2 == 0)));
        eq(List.of(1, 2, 3, 4), new ArrayList<>(admission));
        throwsType(IllegalArgumentException.class, () -> admission.add(5));
        throwsType(IllegalArgumentException.class, () -> admission.addAll(List.of(6, 7)));
        eq(List.of(1, 2, 3, 4), backing);
        ok(admission.add(6));
        eq(List.of(2, 4, 6), new ArrayList<>(visible));
        admission.remove(1);
        eq(List.of(2, 3, 4, 6), backing);
        Set<Integer> ordered = new TreeSet<>(Set.of(1, 2));
        Set<Integer> admittedSet = M3CollectionWrappers.admissionFiltered(ordered, x -> x > 0);
        eq(ordered, admittedSet);
        eq(ordered.hashCode(), admittedSet.hashCode());
        ok(admittedSet.add(3));
        throwsType(IllegalArgumentException.class, () -> admittedSet.add(-1));
        throwsType(IllegalArgumentException.class,
                () -> admittedSet.addAll(List.of(4, -1)));
        eq(Set.of(1, 2, 3), ordered);
        eq(List.of(2), new ArrayList<>(
                M3CollectionWrappers.filtered(ordered, x -> x % 2 == 0)));
    }

    private static void listAdmissionAndReplacement() {
        List<String> source = new ArrayList<>(List.of("good", "existing"));
        List<String> guarded = M3CollectionWrappers.admissionFiltered(
                source, value -> value != null && value.length() >= 3);
        ok(guarded instanceof RandomAccess);
        eq("good", guarded.set(0, "great"));
        throwsType(IllegalArgumentException.class, () -> guarded.set(0, "x"));
        ListIterator<String> iterator = guarded.listIterator();
        eq("great", iterator.next());
        throwsType(IllegalArgumentException.class, () -> iterator.set("a"));
        iterator.set("valid");
        throwsType(IllegalArgumentException.class, () -> iterator.add("a"));
        iterator.add("another");
        throwsType(IllegalArgumentException.class,
                () -> guarded.subList(0, 2).add("b"));
        throwsType(IllegalArgumentException.class,
                () -> guarded.addAll(1, List.of("allowed", "x")));
        eq(List.of("valid", "another", "existing"), source);
        guarded.replaceAll(String::toUpperCase);
        eq(List.of("VALID", "ANOTHER", "EXISTING"), source);
        guarded.remove(1);
        eq(List.of("VALID", "EXISTING"), source);
    }

    private static void fixedSizeVersusReadOnly() {
        List<String> backing = new ArrayList<>(List.of("a", "b"));
        List<String> fixed = M3CollectionWrappers.fixedSize(backing);
        ok(fixed instanceof RandomAccess);
        eq("a", fixed.set(0, "z"));
        eq(List.of("z", "b"), backing);
        throwsType(UnsupportedOperationException.class, () -> fixed.add("c"));
        throwsType(UnsupportedOperationException.class, () -> fixed.remove(0));
        throwsType(UnsupportedOperationException.class, () -> fixed.listIterator().add("d"));
        List<String> readOnly = M3CollectionWrappers.readOnly(backing);
        throwsType(UnsupportedOperationException.class, () -> readOnly.set(0, "x"));
        List<String> sequential = M3CollectionWrappers.fixedSize(new LinkedList<>(backing));
        ok(!(sequential instanceof RandomAccess));
        eq(backing, sequential);
    }

    private static void capacityAndMutationPaths() {
        List<Integer> source = new ArrayList<>(List.of(1, 2));
        List<Integer> bounded = M3CollectionWrappers.bounded(source, 4);
        ok(bounded instanceof RandomAccess);
        throwsType(IllegalStateException.class,
                () -> bounded.addAll(List.of(3, 4, 5)));
        eq(List.of(1, 2), source);
        ok(bounded.addAll(List.of(3, 4)));
        eq(List.of(1, 2, 3, 4), bounded);
        throwsType(IllegalStateException.class, () -> bounded.add(5));
        ListIterator<Integer> iterator = bounded.listIterator();
        eq(1, iterator.next());
        throwsType(IllegalStateException.class, () -> iterator.add(6));
        iterator.remove();
        iterator.add(6);
        eq(List.of(6, 2, 3, 4), source);
        throwsType(IllegalStateException.class, () -> bounded.subList(0, 2).add(7));
        eq(3, bounded.remove(2));
        bounded.add(0, 8);
        eq(List.of(8, 6, 2, 4), source);
        List<Integer> linked = M3CollectionWrappers.bounded(new LinkedList<>(List.of(1)), 2);
        ok(!(linked instanceof RandomAccess));
        linked.add(2);
        eq(List.of(1, 2), linked);
    }

    private static void mappedLiveReadOnly() {
        List<String> source = new ArrayList<>(List.of("a", "aa"));
        AtomicInteger evaluations = new AtomicInteger();
        List<Integer> lengths = M3CollectionWrappers.mappedView(source, s -> {
            evaluations.incrementAndGet();
            return s.length();
        });
        eq(0, evaluations.get());
        eq(List.of(1, 2), lengths);
        ok(evaluations.get() >= 2);
        int before = evaluations.get();
        source.add("aaa");
        eq(3, lengths.size());
        eq(before, evaluations.get());
        eq(3, lengths.get(2));
        throwsType(UnsupportedOperationException.class, () -> lengths.set(0, 3));
        throwsType(UnsupportedOperationException.class, () -> lengths.listIterator().add(5));
        List<String> linked = new LinkedList<>(source);
        List<String> mapped = M3CollectionWrappers.mappedView(linked, String::toUpperCase);
        ok(!(mapped instanceof RandomAccess));
        eq(List.of("A", "AA", "AAA"), mapped);
        Collection<Integer> general = M3CollectionWrappers.mappedView(
                (Collection<String>) source, String::length);
        eq(List.of(1, 2, 3), new ArrayList<>(general));
        throwsType(UnsupportedOperationException.class, () -> general.clear());
        source.set(0, "bbbb");
        eq(4, general.iterator().next());
        Collection<Integer> duplicateKeys = M3CollectionWrappers.mappedView(
                (Collection<String>) source, ignored -> 1);
        eq(List.of(1, 1, 1), new ArrayList<>(duplicateKeys));
        throwsType(IllegalArgumentException.class,
                () -> M3CollectionWrappers.mappedView(source, ignored -> {
                    throw new IllegalArgumentException("mapper failure");
                }).get(0));
    }

    private static void concatenatedLiveReadOnly() {
        List<Integer> left = new ArrayList<>(List.of(1, 2));
        List<Integer> right = new LinkedList<>(List.of(2, 3));
        Collection<Integer> merged = M3CollectionWrappers.concatenatedView(left, right);
        eq(List.of(1, 2, 2, 3), new ArrayList<>(merged));
        left.add(0);
        right.add(4);
        eq(List.of(1, 2, 0, 2, 3, 4), new ArrayList<>(merged));
        eq(6, merged.size());
        throwsType(UnsupportedOperationException.class, merged::clear);
        throwsType(UnsupportedOperationException.class, () -> merged.add(5));
    }

    private static void flatMapAndValueProjection() {
        List<List<String>> source = new ArrayList<>();
        source.add(new ArrayList<>(List.of("a", "b")));
        source.add(new ArrayList<>());
        source.add(new ArrayList<>(List.of("c")));
        AtomicInteger calls = new AtomicInteger();
        Collection<String> flattened = M3CollectionWrappers.flatMappedView(source, part -> {
            calls.incrementAndGet();
            return part;
        });
        eq(0, calls.get());
        ArrayList<String> firstWalk = new ArrayList<>();
        for (String value : flattened) firstWalk.add(value);
        eq(List.of("a", "b", "c"), firstWalk);
        eq(3, calls.get());
        source.get(0).add("aa");
        eq(List.of("a", "b", "aa", "c"), new ArrayList<>(flattened));
        eq(4, flattened.size());
        throwsType(UnsupportedOperationException.class, () -> flattened.add("d"));
        throwsType(NoSuchElementException.class, () -> {
            var it = M3CollectionWrappers.flatMappedView(
                    List.of(List.of(1)), list -> list).iterator();
            it.next();
            it.next();
        });
        Collection<Integer> broken = M3CollectionWrappers.flatMappedView(
                List.of(1), value -> null);
        throwsType(NullPointerException.class, () -> broken.iterator().hasNext());

        Map<String, Integer> backing = new TreeMap<>();
        backing.put("a", 10);
        backing.put("b", null);
        Map<String, String> values = M3CollectionWrappers.mappedValuesView(
                backing, value -> value == null ? "missing" : "value:" + value);
        eq(2, values.size());
        eq("value:10", values.get("a"));
        eq("missing", values.get("b"));
        eq(null, values.get("unknown"));
        ok(values.containsKey("b"));
        ok(!values.containsKey("unknown"));
        eq(Map.of("a", "value:10", "b", "missing"), values);
        eq(Map.of("a", "value:10", "b", "missing").hashCode(), values.hashCode());
        throwsType(UnsupportedOperationException.class, () -> values.put("c", "new"));
        throwsType(UnsupportedOperationException.class,
                () -> values.entrySet().iterator().next().setValue("bad"));
        backing.put("a", 11);
        eq("value:11", values.get("a"));
        backing.remove("b");
        eq(1, values.size());
    }

    private static void rangesPagesAndReverse() {
        List<Integer> backing = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        List<Integer> window = M3CollectionWrappers.slice(backing, 1, 4);
        eq(List.of(2, 3, 4), window);
        window.set(1, 30);
        eq(List.of(1, 2, 30, 4, 5), backing);
        List<Integer> reverse = M3CollectionWrappers.reversedView(backing);
        eq(List.of(5, 4, 30, 2, 1), reverse);
        reverse.set(0, 50);
        eq(50, backing.get(4));
        NavigableSet<Integer> ordered = new TreeSet<>(Set.of(1, 3, 5));
        eq(List.of(5, 3, 1),
                new ArrayList<>(M3CollectionWrappers.reversedView(ordered)));
        List<List<Integer>> pages = M3CollectionWrappers.partitionedView(backing, 2);
        eq(List.of(List.of(1, 2), List.of(30, 4), List.of(50)), pages);
        throwsType(UnsupportedOperationException.class, () -> pages.add(List.of(6)));
        throwsType(UnsupportedOperationException.class, () -> pages.get(0).set(0, 22));
        backing.add(6);
        eq(List.of(List.of(1, 2), List.of(30, 4), List.of(50, 6)), pages);
        backing.add(7);
        eq(4, pages.size());
        eq(List.of(7), pages.get(3));
        throwsType(IndexOutOfBoundsException.class, () -> pages.get(4));
        eq(0, M3CollectionWrappers.partitionedView(List.of(), 10).size());
    }

    private static void compositionOrderAndContracts() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        Collection<Integer> visible = M3CollectionWrappers.filtered(
                (Collection<Integer>) source, value -> value % 2 == 0);
        Collection<Integer> guarded = M3CollectionWrappers.readOnly(visible);
        eq(List.of(2, 4), new ArrayList<>(guarded));
        throwsType(UnsupportedOperationException.class, guarded::clear);
        eq(List.of(1, 2, 3, 4, 5), source);

        List<Integer> bounded = M3CollectionWrappers.bounded(source, 6);
        List<Integer> admissible = M3CollectionWrappers.admissionFiltered(
                bounded, value -> value > 0);
        ok(admissible instanceof RandomAccess);
        throwsType(IllegalArgumentException.class, () -> admissible.add(-1));
        ok(admissible.add(6));
        throwsType(IllegalStateException.class, () -> admissible.add(7));
        throwsType(IllegalArgumentException.class, () -> admissible.add(-2));
        eq(List.of(1, 2, 3, 4, 5, 6), source);

        List<Integer> fixed = M3CollectionWrappers.fixedSize(admissible);
        eq(1, fixed.set(0, 10));
        throwsType(UnsupportedOperationException.class, () -> fixed.add(7));
        eq(10, source.get(0));
        List<Integer> mapped = M3CollectionWrappers.mappedView(
                M3CollectionWrappers.readOnly(source), value -> value * value);
        eq(List.of(100, 4, 9, 16, 25, 36), mapped);
        source.set(0, 2);
        eq(List.of(4, 4, 9, 16, 25, 36), mapped);
        List<Integer> independent = M3CollectionWrappers.snapshot(mapped);
        source.set(0, 20);
        eq(List.of(4, 4, 9, 16, 25, 36), independent);
    }

    private static void distinctAndDetachedCopies() {
        List<String> source = new ArrayList<>(Arrays.asList("b", "a", "b", null, null));
        List<String> snapshot = M3CollectionWrappers.snapshot(source);
        List<String> distinct = M3CollectionWrappers.distinctSnapshot(source);
        eq(Arrays.asList("b", "a", "b", null, null), snapshot);
        eq(Arrays.asList("b", "a", null), distinct);
        source.clear();
        eq(Arrays.asList("b", "a", "b", null, null), snapshot);
        throwsType(UnsupportedOperationException.class, () -> snapshot.add("x"));
        List<Integer> sorted = M3CollectionWrappers.sortedSnapshot(
                List.of(3, 1, 3, 2), Comparator.naturalOrder());
        eq(List.of(1, 2, 3, 3), sorted);
    }

    private static void negativeArguments() {
        throwsType(NullPointerException.class,
                () -> M3CollectionWrappers.readOnly((List<Integer>) null));
        throwsType(NullPointerException.class,
                () -> M3CollectionWrappers.mappedView(List.of(1), null));
        throwsType(NullPointerException.class,
                () -> M3CollectionWrappers.admissionFiltered((Set<Integer>) null, x -> true));
        throwsType(NullPointerException.class,
                () -> M3CollectionWrappers.concatenatedView(List.of(), null));
        throwsType(IllegalArgumentException.class,
                () -> M3CollectionWrappers.partitionedView(List.of(1), 0));
        throwsType(IllegalArgumentException.class,
                () -> M3CollectionWrappers.bounded(List.of(1), 0));
        throwsType(IllegalArgumentException.class,
                () -> M3CollectionWrappers.bounded(List.of(), -1));
        throwsType(IndexOutOfBoundsException.class,
                () -> M3CollectionWrappers.slice(List.of(1), 2, 3));
    }

    private static void randomizedContracts() {
        Random random = new Random(0xC011EC710BEEFL);
        for (int trial = 0; trial < 1_250; trial++) {
            int maximum = 1 + random.nextInt(24);
            List<Integer> source = new ArrayList<>();
            List<Integer> wrapper = M3CollectionWrappers.bounded(source, maximum);
            List<Integer> oracle = new ArrayList<>();
            for (int step = 0; step < 55; step++) {
                int value = random.nextInt(31) - 15;
                switch (random.nextInt(5)) {
                    case 0 -> {
                        if (oracle.size() < maximum) {
                            ok(wrapper.add(value));
                            oracle.add(value);
                        } else {
                            throwsType(IllegalStateException.class, () -> wrapper.add(value));
                        }
                    }
                    case 1 -> {
                        if (!oracle.isEmpty()) {
                            int index = random.nextInt(oracle.size());
                            eq(oracle.remove(index), wrapper.remove(index));
                        }
                    }
                    case 2 -> {
                        if (!oracle.isEmpty()) {
                            int index = random.nextInt(oracle.size());
                            eq(oracle.set(index, value), wrapper.set(index, value));
                        }
                    }
                    case 3 -> {
                        int count = random.nextInt(5);
                        List<Integer> incoming = new ArrayList<>();
                        for (int i = 0; i < count; i++) incoming.add(random.nextInt(25));
                        if (oracle.size() + count > maximum) {
                            throwsType(IllegalStateException.class, () -> wrapper.addAll(incoming));
                        } else {
                            eq(oracle.addAll(incoming), wrapper.addAll(incoming));
                        }
                    }
                    case 4 -> {
                        eq(oracle, wrapper);
                        eq(oracle.hashCode(), wrapper.hashCode());
                        List<Integer> projected = M3CollectionWrappers.mappedView(
                                wrapper, valueAt -> valueAt * 2);
                        eq(oracle.stream().map(v -> v * 2).toList(), projected);
                        List<List<Integer>> pages = M3CollectionWrappers.partitionedView(wrapper, 3);
                        ArrayList<Integer> roundTrip = new ArrayList<>();
                        for (List<Integer> page : pages) roundTrip.addAll(page);
                        eq(oracle, roundTrip);
                    }
                    default -> throw new AssertionError();
                }
                eq(oracle, source);
                eq(oracle, wrapper);
                ok(wrapper.size() <= maximum);
            }
        }
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void ok(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("expected true");
        }
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("expected " + type.getName() + " got " + error, error);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
