/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Standalone JDK21 test without non-repository custom JUnit artifacts. */
public final class M3CollectionSdkSortedFactorySelfTest {
    private M3CollectionSdkSortedFactorySelfTest() { }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void run() {
        long setFactories = 0;
        long mapFactories = 0;
        for (Method method : M3CollectionSdk.class.getDeclaredMethods()) {
            if (method.getName().equals("readOnlyFilteredSortedSet")) setFactories++;
            if (method.getName().equals("readOnlyFilteredSortedMap")) mapFactories++;
        }
        check(setFactories == 1, "exactly one public SortedSet factory");
        check(mapFactories == 1, "exactly one public SortedMap factory");

        TreeSet<Integer> setSource = new TreeSet<>(Comparator.reverseOrder());
        setSource.addAll(List.of(1, 2, 3, 4, 5, 6));
        SortedSet<Integer> set = M3CollectionSdk.readOnlyFilteredSortedSet(
                (SortedSet<Integer>) setSource, x -> x % 2 == 0);
        check(new ArrayList<>(set).equals(List.of(6, 4, 2)), "original comparator order");
        setSource.add(8);
        check(set.first() == 8, "backed SortedSet reflects caller mutation");
        check(set.last() == 2, "last visible");
        boolean refused = false;
        try { set.add(10); } catch (UnsupportedOperationException valid) { refused = true; }
        check(refused, "read-only SortedSet refuses mutation");

        TreeMap<String, Integer> mapSource = new TreeMap<>(Comparator.comparingInt(String::length));
        mapSource.put("aa", 2);
        mapSource.put("bbbb", 4);
        mapSource.put("dddddd", 6);
        SortedMap<String, Integer> map = M3CollectionSdk.readOnlyFilteredSortedMap(
                (SortedMap<String, Integer>) mapSource,
                (key, value) -> key.startsWith("a") || value > 4);
        check(map.containsKey("zz"), "lookup uses stored comparator representative");
        check(map.get("zz") == 2, "stored value reused");
        check(map.size() == 2, "filtered SortedMap visibility");
        mapSource.put("eeeeeeee", 8);
        check(map.size() == 3, "backed SortedMap reflects caller mutation");
        refused = false;
        try { map.put("ccc", 3); } catch (UnsupportedOperationException valid) { refused = true; }
        check(refused, "read-only SortedMap refuses mutation");
    }

    public static void main(String[] args) {
        run();
        System.out.println("PASS M3CollectionSdkSortedFactorySelfTest");
    }
}
