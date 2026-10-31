/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Generated from the canonical Synexia source-sealed Java recipe.
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3ArrayList;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** True five-provider JUnit contracts, not stand-in provider classes. */
final class M3FiveProviderKeyGroupsTest {
    private static M3FiveLibraryIndexViews<Integer, Integer> capture(
            Integer[] keys, Integer[] values, int[] rows, byte[] states) {
        return M3FiveLibraryIndexViews.capture(keys, values, rows, states);
    }

    @Test void groupedDuplicatesNullsAndStablePhysicalOccurences() {
        Integer[] keys = {null, 3, 1, 3, null, 1, 7};
        Integer[] values = {null, 30, 10, 31, 40, 11, 70};
        int[] rows = {3, 0, 3, 5, 4, 2, 0};
        byte[] states = {1, 1, 0, 1, 1, 1, 0};
        var source = capture(keys, values, rows, states);
        var groups = M3FiveProviderKeyGroups.snapshot(source);
        assertEquals(3, groups.groupCount());
        assertEquals(7, groups.occurrenceCount());
        assertEquals(List.of(31, 31), groups.valuesFor(3).subList(0, 2));
        assertEquals(Arrays.asList(null, 40, null), groups.valuesFor(null));
        assertEquals(List.of(11, 10), groups.valuesFor(1));
        assertEquals(List.of(), groups.valuesFor(999));
        assertEquals(0, groups.sourceOrdinalAt(0, 0));
        assertEquals(2, groups.sourceOrdinalAt(0, 1));
        assertEquals(31, groups.valueAt(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> groups.valueAt(0, 3));
        assertThrows(UnsupportedOperationException.class, () -> groups.valuesAt(0).clear());
        keys[3] = 500; values[3] = 999; rows[0] = 6; states[3] = 0;
        assertEquals(List.of(31, 31), groups.valuesFor(3));
        var selected = M3FiveProviderKeyGroups.snapshotSelected(
                source, 1, 0, M3IndexMaskCompactor.Backend.JAVA);
        assertEquals(6, selected.occurrenceCount());
        assertEquals(List.of(31, 31), selected.valuesFor(3));
        assertEquals(Arrays.asList(null, 40, null), selected.valuesFor(null));
        assertEquals(List.of(11), selected.valuesFor(1));
    }

    @Test void groupWindowsComparatorTiesAndPredicateCardinality() {
        Integer[] keys = {4, 1, 6, 3, 2, 7};
        Integer[] values = {40, 10, 60, 30, 20, 70};
        var groups = M3FiveProviderKeyGroups.snapshot(capture(keys, values,
                new int[] {0, 1, 2, 3, 4, 5}, new byte[6]));
        Comparator<Integer> parity = Comparator.comparingInt(x -> x % 2);
        var sorted = groups.sortedGroups(parity);
        assertEquals(List.of(4, 6, 2, 1, 3, 7), keys(sorted));
        assertEquals(List.of(4, 1, 6, 3, 2, 7), keys(groups));
        AtomicInteger evaluated = new AtomicInteger();
        var filtered = sorted.filteredKeys(k -> {
            evaluated.incrementAndGet(); return k <= 3;
        });
        assertEquals(sorted.groupCount(), evaluated.get());
        assertEquals(List.of(2, 1, 3), keys(filtered));
        assertEquals(List.of(20), filtered.valuesFor(2));
        assertEquals(List.of(), filtered.valuesFor(4));
        assertEquals(List.of(1, 3), keys(filtered.groupWindow(1, 3)));
        assertEquals(0, filtered.groupWindow(0, 0).occurrenceCount());
        assertThrows(IndexOutOfBoundsException.class, () -> filtered.groupWindow(-1, 0));
        assertThrows(IllegalStateException.class,
                () -> filtered.filteredKeys(k -> { throw new IllegalStateException("predicate"); }));
        assertEquals(List.of(2, 1, 3), keys(filtered));
    }

    @Test void allFiveTrueProviderContractsPreserveDistinctCardinalitySemantics() {
        Integer[] keys = {2, 1, 2, 1, 2, 4};
        Integer[] values = {20, 10, 20, 11, 22, 40};
        int[] rows = {1, 0, 2, 3, 4, 5, 0};
        var groups = M3FiveProviderKeyGroups.snapshot(
                capture(keys, values, rows, new byte[keys.length]));
        assertEquals(7, groups.occurrenceCount());
        var jdk = groups.asJdkMapOfLists();
        assertEquals(3, jdk.size());
        assertEquals(List.of(10, 11), jdk.get(1));
        assertEquals(List.of(20, 20, 22, 20), jdk.get(2));
        assertTrue(jdk.get(2) instanceof M3ArrayList);
        var guava = groups.asGuavaListMultimap();
        assertEquals(7, guava.size());
        assertEquals(jdk.get(2), guava.get(2));
        var eclipse = groups.asEclipseListMultimap();
        assertEquals(jdk.get(2), new ArrayList<>(eclipse.get(2)));
        var spring = groups.asSpringMultiValueMap();
        assertEquals(3, spring.size());
        assertEquals(jdk.get(2), spring.get(2));
        var apache = groups.asApacheListValuedMap();
        assertEquals(7, apache.size());
        assertEquals(jdk.get(2), apache.get(2));
        guava.put(2, 999);
        apache.put(1, 998);
        spring.add(4, 997);
        jdk.get(2).add(996);
        assertEquals(List.of(20, 20, 22, 20), groups.valuesFor(2));
    }

    @Test void randomizedGroupingStableSortingWindowsAndMaskOracle() {
        Random random = new Random(0x553F1A11L);
        int trials = 1400;
        for (int trial = 0; trial < trials; trial++) {
            int physical = random.nextInt(70);
            Integer[] keys = new Integer[physical];
            Integer[] values = new Integer[physical];
            byte[] states = new byte[physical];
            for (int i = 0; i < physical; i++) {
                keys[i] = random.nextInt(16) - 8;
                values[i] = random.nextInt(9) == 0 ? null : random.nextInt(30) - 15;
                states[i] = (byte) random.nextInt(256);
            }
            int occurrences = physical == 0 ? 0 : random.nextInt(110);
            int[] rows = new int[occurrences];
            for (int i = 0; i < occurrences; i++) rows[i] = random.nextInt(physical);
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            var view = capture(keys, values, rows, states);
            var selected = M3FiveProviderKeyGroups.snapshotSelected(view, required, excluded,
                    M3IndexMaskCompactor.Backend.JAVA);
            Map<Integer, List<Integer>> expected = new LinkedHashMap<>();
            for (int row : rows) {
                int flags = states[row] & 0xff;
                if ((flags & required) == required && (flags & excluded) == 0) {
                    expected.computeIfAbsent(keys[row], unused -> new ArrayList<>()).add(values[row]);
                }
            }
            assertEquals(expected.size(), selected.groupCount(), "distinct trial=" + trial);
            assertEquals(expected, selected.asJdkMapOfLists(), "jdk trial=" + trial);
            assertEquals(new ArrayList<>(expected.keySet()), keys(selected));
            assertEquals(expected.values().stream().mapToInt(List::size).sum(),
                    selected.occurrenceCount());
            Comparator<Integer> order = Comparator.reverseOrder();
            var sorted = selected.sortedGroups(order);
            List<Integer> expectedKeys = new ArrayList<>(expected.keySet());
            expectedKeys.sort(order);
            assertEquals(expectedKeys, keys(sorted), "sorted trial=" + trial);
            for (int group = 0; group < sorted.groupCount(); group++) {
                Integer key = sorted.keyAt(group);
                assertEquals(expected.get(key), sorted.valuesAt(group));
                for (int at = 0; at < sorted.countAt(group); at++) {
                    assertEquals(expected.get(key).get(at), sorted.valueAt(group, at));
                    assertTrue(sorted.sourceOrdinalAt(group, at) < view.size());
                }
            }
            if (trial % 35 == 0) {
                var guava = selected.asGuavaListMultimap();
                var apache = selected.asApacheListValuedMap();
                var eclipse = selected.asEclipseListMultimap();
                var spring = selected.asSpringMultiValueMap();
                assertEquals(selected.occurrenceCount(), guava.size());
                assertEquals(selected.occurrenceCount(), apache.size());
                assertEquals(expected.size(), spring.size());
                for (Integer key : expected.keySet()) {
                    assertEquals(expected.get(key), guava.get(key));
                    assertEquals(expected.get(key), apache.get(key));
                    assertEquals(expected.get(key), spring.get(key));
                    assertEquals(expected.get(key), new ArrayList<>(eclipse.get(key)));
                }
            }
        }
    }

    @Test void explicitNativeRequirementIsNotSilentlyDowngraded() {
        var view = capture(new Integer[] {1, 2}, new Integer[] {10, 20},
                new int[] {1, 0}, new byte[] {1, 3});
        var javaResult = M3FiveProviderKeyGroups.snapshotSelected(
                view, 1, 2, M3IndexMaskCompactor.Backend.JAVA);
        assertEquals(List.of(10), javaResult.asJdkMapOfLists().get(1));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            var nativeResult = M3FiveProviderKeyGroups.snapshotSelected(
                    view, 1, 2, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            assertEquals(javaResult.asJdkMapOfLists(), nativeResult.asJdkMapOfLists());
        } else {
            assertThrows(UnsatisfiedLinkError.class, () -> M3FiveProviderKeyGroups.snapshotSelected(
                    view, 1, 2, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        assertFalse(javaResult.valuesFor(2).contains(20));
    }

    private static <K, V> List<K> keys(M3FiveProviderKeyGroups<K, V> groups) {
        List<K> result = new ArrayList<>();
        for (int i = 0; i < groups.groupCount(); i++) result.add(groups.keyAt(i));
        return result;
    }
}
