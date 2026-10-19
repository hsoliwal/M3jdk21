/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * Java21 seed-fixed JDK differential oracle for DOOP read-only navigable views.
 * Predicates evaluate the retained representative, never a caller's equal alias.
 */
public final class M3ReadonlyFilteredSlotSelfTest {
    private static long checks;
    private static final BiPredicate<Integer, Integer> ADMIT =
            (key, value) -> (key & 1) == 0 && (value == null || value >= 0);

    private M3ReadonlyFilteredSlotSelfTest() { }

    private static void equal(Object expected, Object observed, String label) {
        checks++;
        if (!Objects.equals(expected, observed)) {
            throw new AssertionError(label + " expected=" + expected + " observed=" + observed);
        }
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void refuses(Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (UnsupportedOperationException accepted) {
            return;
        }
        throw new AssertionError("read-only view allowed mutation: " + label);
    }

    private static <K, V> List<String> pairs(Map<K, V> view) {
        List<String> result = new ArrayList<>();
        view.forEach((key, value) -> result.add(key + "=" + value));
        return result;
    }

    private static void verify(NavigableMap<Integer, Integer> reference,
            NavigableMap<Integer, Integer> product, M3TreeMap<Integer, Integer> owner,
            int step) {
        equal(reference.size(), product.size(), "size " + step);
        equal(reference.isEmpty(), product.isEmpty(), "empty " + step);
        equal(new ArrayList<>(reference.entrySet()), new ArrayList<>(product.entrySet()),
                "entry order " + step);
        equal(new ArrayList<>(reference.values()), new ArrayList<>(product.values()),
                "value order " + step);
        equal(new ArrayList<>(reference.navigableKeySet()), new ArrayList<>(product.navigableKeySet()),
                "key order " + step);
        equal(new ArrayList<>(reference.descendingMap().entrySet()),
                new ArrayList<>(product.descendingMap().entrySet()), "descending order " + step);
        equal(reference, product, "map equality " + step);
        equal(product, reference, "inverse map equality " + step);
        equal(reference.hashCode(), product.hashCode(), "hash " + step);
        equal(reference.toString(), product.toString(), "map string " + step);
        equal(pairs(reference), pairs(product), "direct forEach " + step);
        if (!reference.isEmpty()) {
            equal(reference.firstKey(), product.firstKey(), "firstKey " + step);
            equal(reference.lastKey(), product.lastKey(), "lastKey " + step);
        }
        equal(reference.firstEntry(), product.firstEntry(), "firstEntry " + step);
        equal(reference.lastEntry(), product.lastEntry(), "lastEntry " + step);
        for (Integer value : Arrays.asList(null, -99, -1, 0, 6, 17)) {
            equal(reference.containsValue(value), product.containsValue(value),
                    "containsValue " + value + " step " + step);
        }
        for (int key = -22; key <= 22; key++) {
            equal(reference.containsKey(key), product.containsKey(key), "containsKey " + key);
            equal(reference.get(key), product.get(key), "get " + key);
            equal(reference.lowerKey(key), product.lowerKey(key), "lowerKey " + key);
            equal(reference.floorKey(key), product.floorKey(key), "floorKey " + key);
            equal(reference.ceilingKey(key), product.ceilingKey(key), "ceilingKey " + key);
            equal(reference.higherKey(key), product.higherKey(key), "higherKey " + key);
            equal(reference.lowerEntry(key), product.lowerEntry(key), "lowerEntry " + key);
            equal(reference.floorEntry(key), product.floorEntry(key), "floorEntry " + key);
            equal(reference.ceilingEntry(key), product.ceilingEntry(key), "ceilingEntry " + key);
            equal(reference.higherEntry(key), product.higherEntry(key), "higherEntry " + key);
        }
        owner.verifyInvariants();
    }

    private static void seeded(boolean descending, boolean bounded, long seed) {
        TreeMap<Integer, Integer> jdk = new TreeMap<>();
        M3TreeMap<Integer, Integer> m3 = new M3TreeMap<>();
        for (int key = -20; key <= 20; key++) {
            Integer value = key % 5 == 0 ? null : key % 3 == 0 ? -key : key;
            jdk.put(key, value);
            m3.put(key, value);
        }
        NavigableMap<Integer, Integer> refBase = bounded
                ? jdk.subMap(-12, true, 13, false) : jdk;
        NavigableMap<Integer, Integer> m3Base = bounded
                ? m3.subMap(-12, true, 13, false) : m3;
        if (descending) {
            refBase = refBase.descendingMap();
            m3Base = m3Base.descendingMap();
        }
        NavigableMap<Integer, Integer> reference = M3FilteredViews.filterEntries(refBase, ADMIT);
        NavigableMap<Integer, Integer> product = M3FilteredViews.filterEntries(m3Base, ADMIT);
        refuses(() -> product.pollFirstEntry(), "poll");
        refuses(() -> product.clear(), "clear");
        refuses(() -> product.put(2, 20), "put");
        refuses(() -> product.entrySet().clear(), "entrySet clear");
        verify(reference, product, m3, 0);
        Random random = new Random(seed);
        for (int i = 1; i <= 140; i++) {
            int key = random.nextInt(51) - 25;
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(51) - 25;
            if (random.nextInt(3) == 0) {
                equal(jdk.remove(key), m3.remove(key), "owner removal");
            } else {
                equal(jdk.put(key, value), m3.put(key, value), "owner put");
            }
            verify(reference, product, m3, i);
        }
        Iterator<Map.Entry<Integer, Integer>> iterator = product.entrySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            m3.put(200, 2);
            boolean caught = false;
            try { iterator.hasNext(); } catch (ConcurrentModificationException expected) {
                caught = true;
            }
            require(caught, "structural drift must invalidate physical-slot iterator");
        }
    }

    private static void representativeAndSortedOnly() {
        TreeMap<String, Integer> jdk = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        M3TreeMap<String, Integer> m3 = new M3TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String text : List.of("alpha", "beta", "gamma", "delta")) {
            jdk.put(text, text.length());
            m3.put(text, text.length());
        }
        BiPredicate<String, Integer> retained =
                (key, value) -> key.equals("beta") || key.equals("delta");
        NavigableMap<String, Integer> reference = M3FilteredViews.filterEntries(jdk, retained);
        NavigableMap<String, Integer> product = M3FilteredViews.filterEntries(m3, retained);
        equal(reference.containsKey("BETA"), product.containsKey("BETA"), "stored alias");
        equal(reference.get("BETA"), product.get("BETA"), "stored alias value");
        equal(reference.containsKey("ALPHA"), product.containsKey("ALPHA"), "hidden alias");
        equal(reference.lowerKey("GAMMA"), product.lowerKey("GAMMA"), "alias lower");
        equal(reference.ceilingEntry("alpha"), product.ceilingEntry("alpha"), "alias ceiling");
        equal(reference.firstKey(), product.firstKey(), "case comparator first");
        equal(reference.lastKey(), product.lastKey(), "case comparator last");
        equal(new ArrayList<>(reference.entrySet()), new ArrayList<>(product.entrySet()),
                "case comparator entry snapshots");

        // Verify the SortedMap overload independently preserves non-navigable API shape.
        SortedMap<String, Integer> sortedReference = M3FilteredViews.filterEntries(
                (SortedMap<String, Integer>) jdk, retained);
        SortedMap<String, Integer> sortedProduct = M3FilteredViews.filterEntries(
                (SortedMap<String, Integer>) m3, retained);
        equal(sortedReference.firstKey(), sortedProduct.firstKey(), "sorted-only first");
        equal(sortedReference.lastKey(), sortedProduct.lastKey(), "sorted-only last");
        equal(sortedReference.entrySet(), sortedProduct.entrySet(), "sorted-only entry set");
        refuses(() -> sortedProduct.remove("beta"), "sorted-only write");
        m3.verifyInvariants();
    }

    public static void main(String[] args) {
        seeded(false, false, 0x4D330011L);
        seeded(true, false, 0x4D330012L);
        seeded(false, true, 0x4D330013L);
        seeded(true, true, 0x4D330014L);
        representativeAndSortedOnly();
        System.out.println("PASS M3ReadonlyFilteredSlotSelfTest checks=" + checks);
    }
}
