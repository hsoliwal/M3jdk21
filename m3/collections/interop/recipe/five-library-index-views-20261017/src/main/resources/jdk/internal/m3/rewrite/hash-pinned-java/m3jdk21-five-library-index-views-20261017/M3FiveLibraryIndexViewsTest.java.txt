/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Real-provider differential contracts. No donor library source is copied. */
final class M3FiveLibraryIndexViewsTest {
    private static M3FiveLibraryIndexViews<String, Integer> fixture() {
        return M3FiveLibraryIndexViews.capture(
                new String[] {"b", "a", "b", null, "a"},
                new Integer[] {2, 1, 2, null, 3},
                new int[] {2, 0, 1, 3, 4, 0},
                new byte[] {1, 3, 1, (byte) 0x81, 2});
    }

    @Test void providerContractsAreDistinctButShareM3BackingStrategy() {
        var source = fixture();
        assertEquals(List.of(2, 2, 1), source.slice(0, 3).asJdkList());
        assertEquals(Arrays.asList(2, 2, 1, null, 3, 2), source.asJdkList());
        assertEquals(Map.of("a", 1, "b", 2), source.slice(0, 3).asJdkMapLastWins()
                .entrySet().stream().filter(e -> e.getKey() != null)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> b)));
        var guava = source.asGuavaListMultimap();
        assertEquals(6, guava.size());
        assertEquals(List.of(2, 2, 2), guava.get("b"));
        assertEquals(List.of(1, 3), guava.get("a"));
        assertEquals(Arrays.asList((Integer) null), guava.get(null));
        assertEquals(4, source.asGuavaSetMultimap().size());
        assertEquals(Arrays.asList(2, 2, 1, null, 3, 2), source.asEclipseList());
        var eclipseSorted = source.slice(0, 3).asEclipseSortedSet(Comparator.naturalOrder());
        assertEquals(List.of(1, 2), new ArrayList<>(eclipseSorted));
        assertEquals(new TreeSet<>(List.of(1, 2)), source.slice(0, 3).asJdkSortedSet(Comparator.naturalOrder()));
        var spring = source.asSpringMultiValueMap();
        assertEquals(3, spring.size()); // key count, NOT pair count
        assertEquals(List.of(2, 2, 2), spring.get("b"));
        assertTrue(spring.containsKey(null));
        var apache = source.asApacheListValuedMap();
        assertEquals(6, apache.size());
        assertEquals(List.of(2, 2, 2), apache.get("b"));
        assertEquals(4, source.asApacheSetValuedMap().size());
        guava.put("a", 99);
        apache.put("a", 77);
        spring.add("a", 88);
        assertEquals(6, source.size());
        assertEquals(Integer.valueOf(2), source.valueAt(0));
    }

    @Test void stateFiltersHonorOffsetsUnsignedBitsDuplicatesAndSortStability() {
        var source = fixture();
        var filtered = source.filteredByState(1, 2);
        assertEquals(Arrays.asList(2, 2, null, 2), filtered.asJdkList());
        assertEquals(0x81, filtered.unsignedStateAt(2));
        assertEquals(List.of(2, 2, 2), filtered.asGuavaListMultimap().get("b"));
        assertEquals(4, filtered.size());
        var sorted = source.sortedByKey(Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEquals(Arrays.asList(null, "a", "a", "b", "b", "b"),
                java.util.stream.IntStream.range(0, sorted.size()).mapToObj(sorted::keyAt).toList());
        assertEquals(Arrays.asList(2, 2, 2), sorted.asGuavaListMultimap().get("b"));
        assertEquals(List.of(1, 3), sorted.asGuavaListMultimap().get("a"));
        assertEquals(List.of(), source.filteredByState(1, 1).asJdkList());
        assertEquals(source.asJdkList(), source.filteredByState(0, 0).asJdkList());
        assertThrows(IllegalArgumentException.class, () -> source.filteredByState(256, 0));
        assertThrows(IllegalArgumentException.class, () -> source.filteredByState(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> source.slice(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> source.slice(2, 10));
        assertThrows(NullPointerException.class, () -> source.sortedByKey(null));
    }

    @Test void defensiveCaptureAndNativeContract() {
        String[] keys = {"a", "b"}; Integer[] values = {1, 2};
        int[] rows = {1, 0, 1}; byte[] states = {1, 3};
        var view = M3FiveLibraryIndexViews.capture(keys, values, rows, states);
        keys[1] = "changed"; values[1] = 99; rows[0] = 0; states[1] = 0;
        assertEquals("b", view.keyAt(0)); assertEquals(Integer.valueOf(2), view.valueAt(0));
        assertEquals(3, view.unsignedStateAt(0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3FiveLibraryIndexViews.capture(keys, values, new int[] {-1}, states));
        assertThrows(IllegalArgumentException.class,
                () -> M3FiveLibraryIndexViews.capture(keys, new Integer[1], rows, states));
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            assertThrows(UnsatisfiedLinkError.class, () -> view.filteredByState(0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            assertThrows(UnsatisfiedLinkError.class, () -> view.filteredByState(1, 1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        } else {
            assertEquals(view.filteredByState(1, 0).asJdkList(),
                    view.filteredByState(1, 0, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)
                            .asJdkList());
            assertEquals(view.filteredByState(1, 1).asJdkList(),
                    view.filteredByState(1, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)
                            .asJdkList());
        }
    }

    @Test void seededMaskAndStableSortDifferential() {
        Random random = new Random(20261017);
        for (int trial = 0; trial < 1200; trial++) {
            int n = random.nextInt(65), m = random.nextInt(100);
            Integer[] keys = new Integer[n], values = new Integer[n];
            byte[] states = new byte[n]; int[] order = new int[m];
            for (int i = 0; i < n; i++) {
                keys[i] = random.nextInt(9) - 4;
                values[i] = i;
                states[i] = (byte) random.nextInt(256);
            }
            if (n == 0) order = new int[0];
            else for (int i = 0; i < m; i++) order[i] = random.nextInt(n);
            var view = M3FiveLibraryIndexViews.capture(keys, values, order, states);
            int required = random.nextInt(256), forbidden = random.nextInt(256);
            var expected = new ArrayList<Integer>();
            for (int row : order) {
                int f = states[row] & 0xff;
                if ((f & required) == required && (f & forbidden) == 0) expected.add(values[row]);
            }
            assertEquals(expected, view.filteredByState(required, forbidden).asJdkList());
            if (M3IndexMaskCompactor.nativeAvailable()) {
                assertEquals(expected, view.filteredByState(required, forbidden,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).asJdkList());
            }
            var positions = new ArrayList<Integer>();
            for (int row : order) positions.add(row);
            positions.sort(Comparator.comparingInt((Integer row) -> keys[row]));
            var actual = view.sortedByKey(Comparator.naturalOrder());
            for (int i = 0; i < positions.size(); i++) {
                assertEquals(positions.get(i).intValue(), actual.physicalIndexAt(i));
            }
            assertArrayEquals(order, Arrays.copyOf(order, order.length));
        }
    }
}
