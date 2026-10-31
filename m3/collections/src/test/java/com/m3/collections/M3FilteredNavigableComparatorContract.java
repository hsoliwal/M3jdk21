/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Exact Java 21 oracle: user-comparator exceptions may not be reclassified
 * as bounded-range exceptions by a filtered navigable view.
 */
public final class M3FilteredNavigableComparatorContract {
    private M3FilteredNavigableComparatorContract() { }

    private static int assertions;

    private static final class OneShotComparator implements Comparator<Integer> {
        private boolean armed;

        void arm() { armed = true; }

        @Override public int compare(Integer left, Integer right) {
            if (armed) {
                armed = false;
                throw new IllegalArgumentException("M3_INJECTED_COMPARATOR");
            }
            return Integer.compare(left, right);
        }
    }

    private static void equal(Object expected, Object actual, String label) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void requiresException(Runnable operation, String label) {
        assertions++;
        try {
            operation.run();
        } catch (IllegalArgumentException failure) {
            if (!"M3_INJECTED_COMPARATOR".equals(failure.getMessage())) {
                throw new AssertionError(label + ": wrong exception", failure);
            }
            return;
        }
        throw new AssertionError(label + ": comparator exception was swallowed");
    }

    private static NavigableMap<Integer, String> populated(OneShotComparator comparator) {
        TreeMap<Integer, String> source = new TreeMap<>(comparator);
        source.put(1, "one");
        source.put(3, null);
        source.put(5, "five");
        return source;
    }

    public static void exceptionPreservation() {
        OneShotComparator comparator = new OneShotComparator();
        NavigableMap<Integer, String> bounded = populated(comparator)
                .subMap(1, true, 5, true);
        NavigableMap<Integer, String> filtered =
                M3FilteredViews.filterEntries(bounded, (key, value) -> true);

        comparator.arm();
        requiresException(() -> filtered.ceilingEntry(2), "ceiling comparator");
        comparator.arm();
        requiresException(() -> filtered.higherEntry(2), "higher comparator");
        comparator.arm();
        requiresException(() -> filtered.floorEntry(4), "floor comparator");
        comparator.arm();
        requiresException(() -> filtered.lowerEntry(4), "lower comparator");
    }

    public static void boundedAndNullableParity() {
        OneShotComparator comparator = new OneShotComparator();
        NavigableMap<Integer, String> bounded = populated(comparator)
                .subMap(3, true, 5, true);
        NavigableMap<Integer, String> filtered =
                M3FilteredViews.filterEntries(bounded, (key, value) -> true);

        equal(Map.entry(5, "five"), filtered.ceilingEntry(4), "bounded ceiling");
        equal(3, filtered.ceilingKey(0), "query below bounded range");
        equal(5, filtered.floorKey(6), "query above bounded range");
        equal(null, filtered.floorEntry(0), "bounded lower exhaustion");
        equal(null, filtered.ceilingEntry(6), "bounded upper exhaustion");
        assertions++;
        if (!filtered.containsKey(3)) {
            throw new AssertionError("stored null value was classified as missing");
        }
        equal(null, filtered.get(3), "stored null value");
        equal(3, filtered.firstKey(), "stored nullable representative");
    }

    public static void main(String[] args) {
        exceptionPreservation();
        boundedAndNullableParity();
        System.out.println("PASS M3_FILTERED_NAV_COMPARATOR assertions=" + assertions);
    }
}
