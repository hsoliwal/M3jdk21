/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/** Standalone differential of M3 native-slot-backed filtered navigable maps. */
public final class M3MutableFilteredSlotSelfTest {
    private static final BiPredicate<Integer, Integer> VISIBLE =
            (key, value) -> (key & 1) == 0 && (value == null || value >= 0);
    private static long checks;
    private M3MutableFilteredSlotSelfTest() { }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void truth(boolean good, String label) {
        checks++;
        if (!good) throw new AssertionError(label);
    }

    private static void rejected(Runnable action, String label) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException correct) { return; }
        throw new AssertionError(label + ": expected IllegalArgumentException");
    }

    private static Map<Integer, Integer> batch(int a, Integer av, int b, Integer bv) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        result.put(a, av); result.put(b, bv); return result;
    }

    private static void verify(NavigableMap<Integer, Integer> ref,
            NavigableMap<Integer, Integer> actual, M3TreeMap<Integer, Integer> owner, int phase) {
        same(new ArrayList<>(ref.entrySet()), new ArrayList<>(actual.entrySet()),
                "visible traversal " + phase);
        same(new ArrayList<>(ref.descendingMap().entrySet()),
                new ArrayList<>(actual.descendingMap().entrySet()), "descending " + phase);
        same(ref.size(), actual.size(), "size " + phase);
        same(ref.isEmpty(), actual.isEmpty(), "empty " + phase);
        same(ref.firstEntry(), actual.firstEntry(), "first " + phase);
        same(ref.lastEntry(), actual.lastEntry(), "last " + phase);
        for (int key=-16; key<=16; key++) {
            same(ref.containsKey(key), actual.containsKey(key), "membership "+key);
            same(ref.get(key), actual.get(key), "value "+key);
            same(ref.lowerEntry(key), actual.lowerEntry(key), "lower "+key);
            same(ref.floorEntry(key), actual.floorEntry(key), "floor "+key);
            same(ref.ceilingEntry(key), actual.ceilingEntry(key), "ceiling "+key);
            same(ref.higherEntry(key), actual.higherEntry(key), "higher "+key);
        }
        owner.verifyInvariants();
    }

    private static void exercise(boolean descending, boolean bounded, long seed) {
        TreeMap<Integer, Integer> baselineRoot = new TreeMap<>();
        M3TreeMap<Integer, Integer> m3Root = new M3TreeMap<>();
        for (int key=-20; key<=20; key++) {
            Integer value = key % 4 == 0 ? null : key % 3 == 0 ? -key : key;
            baselineRoot.put(key, value); m3Root.put(key, value);
        }
        NavigableMap<Integer, Integer> baseline = bounded
                ? baselineRoot.subMap(-12, true, 13, false) : baselineRoot;
        NavigableMap<Integer, Integer> storage = bounded
                ? m3Root.subMap(-12, true, 13, false) : m3Root;
        if (descending) {
            baseline = baseline.descendingMap(); storage = storage.descendingMap();
        }
        NavigableMap<Integer, Integer> expected =
                M3MutableFilteredViews.filterEntries(baseline, VISIBLE);
        NavigableMap<Integer, Integer> actual =
                M3MutableFilteredViews.filterEntries(storage, VISIBLE);
        Random random = new Random(seed);
        verify(expected, actual, m3Root, 0);
        for (int step=1;step<=340;step++) {
            int key=random.nextInt(25)-12;
            Integer value=random.nextInt(7)==0?null:random.nextInt(41)-20;
            int op=random.nextInt(9);
            switch (op) {
                case 0 -> {
                    same(baseline.put(key, value), storage.put(key, value), "source put");
                }
                case 1 -> same(baseline.remove(key), storage.remove(key), "source remove");
                case 2 -> {
                    if (VISIBLE.test(key, value)) {
                        same(expected.put(key, value), actual.put(key, value), "visible put");
                    } else {
                        rejected(() -> actual.put(key, value), "reject M3");
                        rejected(() -> expected.put(key, value), "reject baseline");
                    }
                }
                case 3 -> same(expected.remove(key), actual.remove(key), "visible remove");
                case 4 -> {
                    expected.clear(); actual.clear();
                }
                case 5 -> {
                    Map<Integer, Integer> incoming = batch(key, 4,
                            key == 12 ? 10 : key + (key % 2 == 0 ? 0 : 1), 6);
                    if (incoming.keySet().stream().allMatch(k -> k >= -12 && k < 13)
                            && incoming.keySet().stream().allMatch(k -> (k & 1) == 0)) {
                        expected.putAll(incoming); actual.putAll(incoming);
                    }
                }
                case 6 -> {
                    Iterator<Map.Entry<Integer, Integer>> a = expected.entrySet().iterator();
                    Iterator<Map.Entry<Integer, Integer>> b = actual.entrySet().iterator();
                    same(a.hasNext(), b.hasNext(), "iterator hasNext");
                    if (a.hasNext()) {
                        same(a.next(), b.next(), "iterator next");
                        a.remove(); b.remove();
                    }
                }
                case 7 -> {
                    // Comparing externally constructed entries must never mutate storage.
                    Map.Entry<Integer, Integer> probe = new AbstractMap.SimpleImmutableEntry<>(key,value);
                    same(expected.entrySet().contains(probe),
                            actual.entrySet().contains(probe), "entry contains");
                    same(expected.entrySet().remove(probe),
                            actual.entrySet().remove(probe), "entry remove");
                }
                default -> { /* query-only phase */ }
            }
            same(baselineRoot, m3Root, "underlying canonical mappings");
            verify(expected, actual, m3Root, step);
        }
        Iterator<Map.Entry<Integer, Integer>> it = actual.entrySet().iterator();
        if (it.hasNext()) {
            it.next();
            m3Root.put(99, 1);
            boolean detected=false;
            try { it.hasNext(); } catch (ConcurrentModificationException expectedException) {
                detected=true;
            }
            truth(detected, "native iterator rejects structural drift");
        }
    }

    public static void main(String[] args) {
        exercise(false, false, 0x130A1L);
        exercise(true, false, 0x130A2L);
        exercise(false, true, 0x130A3L);
        exercise(true, true, 0x130A4L);
        System.out.println("PASS M3MutableFilteredSlotSelfTest checks=" + checks);
    }
}
