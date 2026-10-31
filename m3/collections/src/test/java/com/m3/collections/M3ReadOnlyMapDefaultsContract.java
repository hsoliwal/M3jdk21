/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 21 oracle for strict non-mutating filtered sorted/navigable Map contracts.
 * Existing M3FilteredViews is the only view owner; no new payload arrays/entries.
 */
public final class M3ReadOnlyMapDefaultsContract {
    private static long checks;

    private M3ReadOnlyMapDefaultsContract() { }

    private static void eq(Object expected, Object actual, String why) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(why + ": " + expected + " != " + actual);
        }
    }

    private static void refuses(Runnable operation, String why) {
        checks++;
        try {
            operation.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("NO_UOE: " + why);
    }

    private static void strictMapDefaults(SortedMap<Integer, String> view) {
        AtomicInteger callbacks = new AtomicInteger();
        refuses(() -> view.putIfAbsent(101, "new"), "absent putIfAbsent");
        refuses(() -> view.remove(101, "missing"), "absent conditional remove");
        refuses(() -> view.replace(101, "new"), "absent replace");
        refuses(() -> view.replace(101, "old", "new"), "absent conditional replace");
        refuses(() -> view.computeIfAbsent(101, key -> {
            callbacks.incrementAndGet();
            return null;
        }), "null-result computeIfAbsent");
        refuses(() -> view.computeIfPresent(101, (key, previous) -> {
            callbacks.incrementAndGet();
            return previous;
        }), "absent computeIfPresent");
        refuses(() -> view.compute(101, (key, previous) -> {
            callbacks.incrementAndGet();
            return null;
        }), "no-op compute");
        refuses(() -> view.merge(101, "new", (a, b) -> {
            callbacks.incrementAndGet();
            return a;
        }), "absent merge");
        refuses(() -> view.replaceAll((key, previous) -> {
            callbacks.incrementAndGet();
            return previous;
        }), "replaceAll");
        refuses(() -> view.entrySet().removeIf(entry -> {
            callbacks.incrementAndGet();
            return false;
        }), "entrySet no-op removeIf");
        refuses(() -> view.keySet().removeIf(key -> {
            callbacks.incrementAndGet();
            return false;
        }), "keySet no-op removeIf");
        refuses(() -> view.values().removeIf(value -> {
            callbacks.incrementAndGet();
            return false;
        }), "values no-op removeIf");
        refuses(() -> view.entrySet().removeAll(Collections.emptySet()),
                "empty entrySet.removeAll");
        refuses(() -> view.keySet().retainAll(Collections.emptySet()),
                "empty keySet.retainAll");
        eq(0, callbacks.get(), "mutator callbacks must not run");
    }

    private static void mapFamiliesAndDerivedViews() {
        TreeMap<Integer, String> source = new TreeMap<>();
        SortedMap<Integer, String> sortedSource = source;
        NavigableMap<Integer, String> navigableSource = source;

        SortedMap<Integer, String> sorted = M3FilteredViews.filterEntries(
                sortedSource, (key, value) -> (key & 1) == 0);
        NavigableMap<Integer, String> navigable = M3FilteredViews.filterEntries(
                navigableSource, (key, value) -> (key & 1) == 0);

        strictMapDefaults(sorted);
        strictMapDefaults(navigable);
        eq(true, sorted.isEmpty(), "empty sorted snapshot");
        eq(true, navigable.isEmpty(), "empty navigable snapshot");

        source.put(1, "hidden");
        source.put(2, "two");
        source.put(4, null);
        source.put(6, "six");
        eq(Map.of(2, "two", 6, "six").keySet(), sorted.keySet().stream()
                .filter(key -> key != 4).collect(java.util.stream.Collectors.toSet()),
                "visible keys after backing changes");
        eq(true, navigable.containsKey(4), "visible null mapping");
        eq(null, navigable.get(4), "null payload preserved");

        strictMapDefaults(sorted);
        strictMapDefaults(navigable);
        strictMapDefaults(sorted.headMap(6));
        strictMapDefaults(sorted.tailMap(2));
        strictMapDefaults(sorted.subMap(2, 8));
        strictMapDefaults(navigable.descendingMap());
        strictMapDefaults(navigable.reversed());
        strictMapDefaults(navigable.headMap(6, true));
        strictMapDefaults(navigable.tailMap(2, true));
        strictMapDefaults(navigable.subMap(2, true, 8, false));
        refuses(() -> navigable.pollFirstEntry(), "pollFirstEntry");
        refuses(() -> navigable.pollLastEntry(), "pollLastEntry");
        refuses(() -> navigable.navigableKeySet().removeIf(key -> false),
                "navigableKeySet no-op removeIf");
        refuses(() -> navigable.descendingKeySet().removeIf(key -> false),
                "descendingKeySet no-op removeIf");
        refuses(() -> navigable.entrySet().iterator().next().setValue("changed"),
                "detached immutable entry");
        eq("two", source.get(2), "rejected writes do not affect owner");
        eq(null, source.get(4), "rejected writes preserve stored null");
        eq(4, source.size(), "rejected writes preserve hidden members");
        source.remove(2);
        eq(false, sorted.containsKey(2), "live deletion");
        source.put(8, "eight");
        eq(true, navigable.containsKey(8), "live addition");
        eq(List.of(4, 6, 8), new ArrayList<>(navigable.navigableKeySet()),
                "stable physical backing order");
    }

    private static void representativesAndRandomizedOrdering() {
        TreeMap<String, String> owner = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        owner.put("Actual", "stored");
        NavigableMap<String, String> view = M3FilteredViews.filterEntries(owner,
                (key, value) -> key.equals("Actual"));
        eq(true, view.containsKey("ACTUAL"), "stored comparator representative");
        eq("stored", view.get("actual"), "stored value via alias");
        refuses(() -> view.putIfAbsent("actual", "ignored"),
                "comparator-equivalent putIfAbsent");
        eq("stored", owner.get("Actual"), "comparator alias cannot mutate owner");

        Random random = new Random(0x4D33524F4D415031L);
        for (int round = 0; round < 300; round++) {
            Comparator<Integer> order = random.nextBoolean()
                    ? Comparator.naturalOrder() : Comparator.reverseOrder();
            TreeMap<Integer, String> backing = new TreeMap<>(order);
            for (int i = 0; i < 50; i++) {
                int key = random.nextInt(150) - 75;
                backing.put(key, (key % 13 == 0) ? null : "v" + key);
            }
            NavigableMap<Integer, String> selected = M3FilteredViews.filterEntries(
                    backing, (key, value) -> (key & 1) == 0);
            TreeMap<Integer, String> expected = new TreeMap<>(order);
            backing.forEach((key, value) -> {
                if ((key & 1) == 0) expected.put(key, value);
            });
            eq(expected, selected, "randomized navigable membership");
            eq(new ArrayList<>(expected.descendingKeySet()),
                    new ArrayList<>(selected.descendingKeySet()),
                    "randomized descending index order");
            strictMapDefaults(selected);
            backing.put(102, "new");
            eq(true, selected.containsKey(102), "randomized live owner change");
        }
    }

    public static void main(String[] args) {
        mapFamiliesAndDerivedViews();
        representativesAndRandomizedOrdering();
        System.out.println("PASS M3ReadOnlyMapDefaultsContract checks=" + checks);
    }
}
