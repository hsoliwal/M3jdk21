/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real five-provider interfaces are resolved from the pinned optional interop dependencies. */
final class M3FiveLibraryKeyRangesTest {
    @Test void allFiveTypedMaterializationsPreserveDistinctProviderContracts() {
        String[] keys = {"b", "a", "a", "c", "B"};
        Integer[] values = {4, 1, 2, 8, 5};
        int[] occurrences = {0, 1, 2, 4, 3, 1};
        byte[] states = {1, 3, 1, 0, 1};
        var source = M3FiveLibraryIndexViews.capture(keys, values, occurrences, states);
        var plan = M3FiveLibraryKeyRanges.sorted(source, String.CASE_INSENSITIVE_ORDER);
        assertEquals(6, plan.size());
        assertEquals(3, plan.lowerBound("b"));
        assertEquals(5, plan.upperBound("B"));
        assertEquals(2, plan.countEquivalent("b"));
        assertEquals(0, plan.countEquivalent("missing"));
        assertTrue(plan.containsEquivalent("A"));
        assertFalse(plan.containsEquivalent("z"));
        var eligible = plan.filteredByState(1, 2);
        var window = eligible.between("a", true, "b", true).view();
        assertEquals(List.of(2, 4, 5), window.asJdkList());
        assertEquals(Map.of("a", 2, "b", 4, "B", 5), window.asJdkMapLastWins());
        assertEquals(List.of(2, 4, 5), new ArrayList<>(window.asJdkSortedSet(Comparator.naturalOrder())));
        assertEquals(List.of(2), window.asGuavaListMultimap().get("a"));
        assertEquals(3, window.asGuavaListMultimap().size());
        assertEquals(3, window.asGuavaSetMultimap().size());
        assertEquals(List.of(2, 4, 5), window.asEclipseList());
        assertEquals(List.of(2, 4, 5), new ArrayList<>(window.asEclipseSortedSet(Comparator.naturalOrder())));
        assertEquals(3, window.asSpringMultiValueMap().size()); // distinct keys, unlike multimaps
        assertEquals(3, window.asApacheListValuedMap().size()); // occurrence pair-count
        assertEquals(3, window.asApacheSetValuedMap().size());
        // Detachment: caller arrays and materialized results never replace snapshot ownership.
        keys[2] = "new";
        values[2] = 909;
        states[2] = 0;
        occurrences[2] = 0;
        assertEquals(List.of(2, 4, 5), window.asJdkList());
        var mutable = window.asGuavaListMultimap();
        mutable.put("a", 99);
        assertEquals(List.of(2, 4, 5), window.asJdkList());
        assertThrows(IllegalArgumentException.class, () -> plan.between("z", true, "a", true));
        assertTrue(plan.between("a", false, "a", true).isEmpty());
        assertTrue(plan.between("a", true, "a", false).isEmpty());
        assertEquals(3, plan.equalTo("A").size());
        assertEquals(0, plan.ordinalWindow(2, 2).size());
        assertThrows(IndexOutOfBoundsException.class, () -> plan.ordinalWindow(-1, 1));
    }

    @Test void naturalReverseAndNullAwareComparatorAgainstStableArrayOracle() {
        Random random = new Random(0x5c01_1ec7);
        for (int trial = 0; trial < 1200; trial++) {
            int physicalCount = random.nextInt(70);
            Integer[] keys = new Integer[physicalCount];
            Integer[] values = new Integer[physicalCount];
            byte[] masks = new byte[physicalCount];
            for (int i = 0; i < physicalCount; i++) {
                keys[i] = random.nextInt(17) == 0 ? null : random.nextInt(16) - 8;
                values[i] = i;
                masks[i] = (byte) random.nextInt(256);
            }
            int n = physicalCount == 0 ? 0 : random.nextInt(110);
            int[] rows = new int[n];
            for (int i = 0; i < n; i++) rows[i] = random.nextInt(physicalCount);
            Comparator<Integer> comparator = Comparator.nullsFirst(Integer::compareTo);
            if (random.nextBoolean()) comparator = comparator.reversed();
            Integer[] expected = new Integer[n];
            for (int i = 0; i < n; i++) expected[i] = rows[i];
            final Comparator<Integer> cmp = comparator;
            Arrays.sort(expected, Comparator.comparing(row -> keys[row], cmp));
            var snapshot = M3FiveLibraryIndexViews.capture(keys, values, rows, masks);
            var plan = M3FiveLibraryKeyRanges.sorted(snapshot, comparator);
            assertEquals(n, plan.size());
            for (int i = 0; i < n; i++) {
                assertEquals(expected[i], plan.view().physicalIndexAt(i), "stable index " + trial);
            }
            for (int value = -10; value <= 10; value++) {
                int lower = 0;
                while (lower < n && cmp.compare(keys[expected[lower]], value) < 0) lower++;
                int upper = lower;
                while (upper < n && cmp.compare(keys[expected[upper]], value) == 0) upper++;
                assertEquals(lower, plan.lowerBound(value), "lower " + trial);
                assertEquals(upper, plan.upperBound(value), "upper " + trial);
                assertEquals(upper - lower, plan.countEquivalent(value), "count " + trial);
                assertEquals(upper - lower, plan.equalTo(value).size(), "equals " + trial);
            }
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            var selected = plan.filteredByState(required, excluded);
            List<Integer> expectedAdmitted = new ArrayList<>();
            for (Integer row : expected) {
                int state = masks[row] & 0xff;
                if ((state & required) == required && (state & excluded) == 0) {
                    expectedAdmitted.add(row);
                }
            }
            assertEquals(expectedAdmitted.size(), selected.size());
            for (int i = 0; i < selected.size(); i++) {
                assertEquals(expectedAdmitted.get(i), selected.view().physicalIndexAt(i));
            }
            if (M3IndexMaskCompactor.nativeAvailable() && trial < 20) {
                var nativeSelected = plan.filteredByState(required, excluded,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                assertEquals(selected.view().asJdkList(), nativeSelected.view().asJdkList());
            }
        }
    }

    @Test void exclusiveAndInclusiveRangeSemanticsAgainstNavigableOracle() {
        Integer[] keys = {-3, -3, 0, 0, 1, 1, 7, 9};
        Integer[] values = {0, 1, 2, 3, 4, 5, 6, 7};
        int[] occurrences = {0, 2, 3, 1, 1, 4, 5, 6, 7};
        byte[] states = new byte[keys.length];
        var plan = M3FiveLibraryKeyRanges.sorted(
                M3FiveLibraryIndexViews.capture(keys, values, occurrences, states),
                Comparator.naturalOrder());
        assertEquals(List.of(0, 1, 1, 2, 3, 4, 5, 6, 7), plan.view().asJdkList());
        assertEquals(List.of(2, 3, 4, 5), plan.between(0, true, 1, true).view().asJdkList());
        assertEquals(0, plan.between(0, false, 0, false).size());
        assertEquals(0, plan.head(-99, true).size());
        assertEquals(0, plan.tail(99, false).size());
        assertEquals(plan.size(), plan.between(-99, true, 99, true).size());
        assertEquals(5, plan.head(0, true).size());
        assertNotSame(plan, plan.ordinalWindow(0, plan.size()));
    }
}
