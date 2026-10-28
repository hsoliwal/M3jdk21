/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Stable indexed sorting preserves identity, physical order and callback behavior
 * @modules java.base/jdk.internal.m3
 * @run main M3IndexedArraySortTraceTest
 */

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import jdk.internal.m3.M3IndexedArray;

public class M3IndexedArraySortTraceTest {
    private record Item(int key, int identity) { }
    private static long comparisons, checks;
    private static MessageDigest trace;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static int order(Item left, Item right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        return Integer.compare(left.key(), right.key());
    }

    private static void record(int value) {
        for (int shift = 24; shift >= 0; shift -= 8) trace.update((byte) (value >>> shift));
    }

    private static int observed(Item left, Item right) {
        comparisons++;
        record(left == null ? -1 : left.identity());
        record(right == null ? -1 : right.identity());
        return order(left, right);
    }

    // An independent stable insertion sort is the bounded test oracle.
    private static List<Item> expected(List<Item> source, boolean filter) {
        ArrayList<Item> result = new ArrayList<>();
        for (Item item : source) {
            if (filter && item != null && (item.identity() & 3) == 0) continue;
            int position = result.size();
            result.add(item);
            while (position > 0 && order(result.get(position - 1), item) > 0) {
                result.set(position, result.get(position - 1));
                position--;
            }
            result.set(position, item);
        }
        return result;
    }

    private static void trial(int length, Random random) {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        List<Item> physical = new ArrayList<>();
        long[] handles = new long[length];
        for (int index = 0; index < length; index++) {
            Item value = index % 17 == 0 ? null : new Item(random.nextInt(13), index);
            physical.add(value);
            handles[index] = owner.append(value);
        }
        for (boolean filtered : new boolean[] {false, true}) {
            List<Integer> predicates = new ArrayList<>();
            List<Item> view = filtered
                    ? owner.filteredSorted(item -> {
                        predicates.add(item == null ? -1 : item.identity());
                        return item == null || (item.identity() & 3) != 0;
                    }, M3IndexedArraySortTraceTest::observed)
                    : owner.sorted(M3IndexedArraySortTraceTest::observed);
            List<Item> oracle = expected(physical, filtered);
            check(view.size() == oracle.size(), "selection cardinality");
            for (int index = 0; index < oracle.size(); index++) {
                check(view.get(index) == oracle.get(index), "stable identity at sorted position");
            }
            if (filtered) {
                check(predicates.size() == length, "predicate called once per physical slot");
                for (int index = 0; index < length; index++) {
                    check(predicates.get(index) == (physical.get(index) == null ? -1 : index),
                            "predicate encounter order");
                }
            }
            long before = comparisons;
            view.size();
            check(comparisons == before, "unchanged owner reuses sorted index");
            for (int index = 0; index < length; index++) {
                check(owner.at(index) == physical.get(index), "physical order changed");
                check(owner.get(handles[index]) == physical.get(index), "stable handle changed");
            }
        }
    }

    private static void exceptionIdentity() {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        owner.append(new Item(2, 0));
        owner.append(new Item(1, 1));
        RuntimeException sentinel = new IllegalStateException("comparator sentinel");
        long version = owner.version();
        try {
            owner.sorted((left, right) -> { throw sentinel; }).size();
            throw new AssertionError("comparator exception was swallowed");
        } catch (RuntimeException actual) {
            check(actual == sentinel, "comparator exception identity");
        }
        check(owner.version() == version && owner.at(0).identity() == 0,
                "failed comparison changed owner");
    }

    public static void main(String[] args) throws Exception {
        trace = MessageDigest.getInstance("SHA-256");
        Random random = new Random(0x4d334a444b3231L);
        for (int length : new int[] {0, 1, 2, 7, 8, 9, 15, 16, 17, 31, 32, 33,
                127, 128, 129, 255, 256, 257, 513}) trial(length, random);
        exceptionIdentity();
        System.out.println("PASS M3IndexedArraySortTraceTest checks=" + checks
                + " comparisons=" + comparisons + " trace="
                + HexFormat.of().formatHex(trace.digest()));
    }
}
