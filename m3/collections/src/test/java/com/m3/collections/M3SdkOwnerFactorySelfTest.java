/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;

/** Differential proof of SDK factories over the existing M3 array/index/mask owners. */
public final class M3SdkOwnerFactorySelfTest {
    private static long checks;
    private M3SdkOwnerFactorySelfTest() { }

    private static void yes(boolean test, String context) {
        checks++;
        if (!test) throw new AssertionError("M3 SDK contract " + context + " #" + checks);
    }
    private static void eq(Object expected, Object actual, String context) {
        yes(Objects.equals(expected, actual), context + " expected=" + expected + " actual=" + actual);
    }

    private static void arrayList() {
        M3ArrayList<Integer> m3 = M3CollectionSdk.m3ArrayList();
        ArrayList<Integer> jdk = new ArrayList<>();
        Random random = new Random(2026101801L);
        for (int step = 0; step < 4_096; step++) {
            int operation = random.nextInt(4);
            Integer value = step % 11 == 0 ? null : random.nextInt(241) - 120;
            switch (operation) {
                case 0, 1 -> {
                    int index = random.nextInt(jdk.size() + 1);
                    jdk.add(index, value);
                    m3.add(index, value);
                }
                case 2 -> {
                    if (!jdk.isEmpty()) {
                        int index = random.nextInt(jdk.size());
                        eq(jdk.set(index, value), m3.set(index, value), "set");
                    }
                }
                default -> {
                    if (!jdk.isEmpty()) {
                        int index = random.nextInt(jdk.size());
                        eq(jdk.remove(index), m3.remove(index), "remove");
                    }
                }
            }
            eq(jdk, m3, "reference array-list step " + step);
            eq(jdk.size(), m3.size(), "array-list size");
        }
        var detached = m3.snapshot();
        var before = List.copyOf(m3.stream().filter(Objects::nonNull).toList());
        m3.clear();
        yes(detached.size() >= before.size(), "detached indexed snapshot");
        yes(m3.isEmpty(), "cleared dynamic array");
    }

    private static void table() {
        M3IndexedTable<String, String, Integer> table = M3CollectionSdk.indexedTable();
        yes(table.isEmpty(), "new indexed table");
        eq(null, table.put("r", "c", null, M3IndexedTable.VISIBLE), "null-bearing cell insertion");
        yes(table.contains("r", "c"), "present null-bearing cell");
        eq(1, table.rowSize("r"), "row index");
        eq(1, table.columnSize("c"), "column index");
        eq(null, table.put("r", "c", 13, M3IndexedTable.DIRTY), "slot-stable replacement");
        eq(13, table.get("r", "c"), "replacement read");
        eq(1, table.row("r").size(), "row index view");
        eq(1, table.column("c").size(), "column index view");
        eq(1, table.all().size(), "global order view");
        yes(table.removeCell("r", "c"), "stable slot deletion");
        yes(!table.contains("r", "c"), "deleted lookup");
        eq(0, table.size(), "empty table after deletion");
    }

    private static void multivalue() {
        M3IndexedMultiValueMap<String, Integer> map = M3CollectionSdk.indexedMultiValueMap();
        map.add("k", 7, M3IndexedMultiValueMap.VISIBLE);
        map.add("k", 7, M3IndexedMultiValueMap.DIRTY);
        map.add("k", null, M3IndexedMultiValueMap.VISIBLE);
        map.add(null, -1, M3IndexedMultiValueMap.VISIBLE);
        eq(4, map.size(), "total array occurrences");
        eq(2, map.keyCount(), "distinct M3 slot-index groups");
        eq(3, map.valueCount("k"), "per-key occurrences");
        eq(Arrays.asList(7, 7, null), map.snapshot("k"), "detached duplicate/null snapshot");
        eq(Arrays.asList(7, null),
                map.selected("k", M3IndexedMultiValueMap.VISIBLE, 0,
                        M3IndexMaskCompactor.Backend.JAVA), "bitmasked index selection");
        eq(3, map.removeKey("k"), "whole group deletion");
        yes(!map.containsKey("k"), "key index invalidated");
        eq(-1, map.getFirst(null), "null-key M3 group");
    }

    private static void ranges() {
        M3LongRangeSet set = M3CollectionSdk.longRangeSet();
        set.add(0, 10);
        set.add(10, 20);
        eq(1, set.rangeCount(), "adjacent merge by two parallel long lanes");
        set.remove(5, 15);
        eq(2, set.rangeCount(), "in-place interval split");
        yes(set.contains(0) && set.contains(19) && !set.contains(10), "binary interval membership");
        eq(BigInteger.TEN, set.cardinality(), "exact range cardinality");
        eq(Arrays.toString(new long[]{0, 15}), Arrays.toString(set.startsToArray()),
                "sorted start offsets");
        eq(Arrays.toString(new long[]{5, 20}), Arrays.toString(set.endsToArray()),
                "sorted end offsets");
        set.verifyInvariants();
    }

    private static void existingSortedContracts() {
        TreeSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        source.addAll(List.of("Alpha", "Beta", "Gamma"));
        var sorted = M3CollectionSdk.readOnlyFilteredSortedSet(source, s -> !s.startsWith("B"));
        eq(List.of("Alpha", "Gamma"), new ArrayList<>(sorted), "existing sorted filter");
        source.add("Delta");
        eq(List.of("Alpha", "Delta", "Gamma"), new ArrayList<>(sorted), "live sorted filter");

        TreeMap<Integer, Integer> map = new TreeMap<>();
        map.put(1, 2); map.put(3, 4); map.put(5, 6);
        BiPredicate<Integer,Integer> predicate = (key, value) -> (key & 1) == 1 && value > 3;
        var view = M3CollectionSdk.readOnlyFilteredSortedMap(map, predicate);
        eq(List.of(3, 5), new ArrayList<>(view.keySet()), "existing sorted map");
        map.put(7, 8);
        eq(List.of(3, 5, 7), new ArrayList<>(view.keySet()), "live sorted map filter");
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no CLI arguments");
        arrayList();
        table();
        multivalue();
        ranges();
        existingSortedContracts();
        System.out.println("PASS M3_SDK_OWNER_FACTORY checks=" + checks);
    }
}
