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

    @Test void extendedProviderMaterializationsAreActualM3Collections() {
        var rows = M3FiveLibraryIndexViews.capture(
                new String[] {"alpha", "beta", "alpha", "beta"},
                new Integer[] {10, 20, 10, 21},
                new int[] {0, 2, 3, 1, 0},
                new byte[] {1, 3, 1, 1});
        assertEquals(java.util.Set.of(10, 20, 21), rows.asJdkSet());
        var guavaCounts = rows.asGuavaMultiset();
        assertEquals(3, guavaCounts.count(10));
        assertEquals(1, guavaCounts.count(21));
        assertEquals(5, guavaCounts.size());
        var apacheCounts = rows.asApacheMultiSet();
        assertEquals(3, apacheCounts.getCount(10));
        assertEquals(5, apacheCounts.size());
        var eclipseList = rows.asEclipseListMultimap();
        assertEquals(3, eclipseList.get("alpha").size());
        assertEquals(2, eclipseList.get("beta").size());
        var eclipseSet = rows.asEclipseSetMultimap();
        assertEquals(1, eclipseSet.get("alpha").size());
        assertEquals(2, eclipseSet.get("beta").size());
        var spring = rows.asSpringLastValueMap();
        assertEquals(2, spring.size());
        assertEquals(java.util.List.of(10), spring.get("alpha"));
        assertEquals(java.util.List.of(20), spring.get("beta"));
        var table = rows.asGuavaOrdinalTable();
        assertEquals(5, table.size());
        assertEquals(Integer.valueOf(10), table.get("alpha", 0));
        assertEquals(Integer.valueOf(10), table.get("alpha", 4));
        assertEquals(3, table.row("alpha").size());
        assertEquals(5, rows.size()); // vendor result mutation never reaches the snapshot
    }

    @Test void actualGuavaBiMapRejectsValueCollisionsAndExposesLiveInverse() {
        var rows = M3FiveLibraryIndexViews.capture(
                new String[] {"first", "second", "first"},
                new Integer[] {7, 8, 9},
                new int[] {0, 1, 2},
                new byte[] {1, 1, 1});
        var result = rows.asGuavaBiMap();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(9), result.get("first"));
        assertEquals("first", result.inverse().get(9));
        result.inverse().put(10, "third");
        assertEquals(Integer.valueOf(10), result.get("third"));
        assertEquals(3, rows.size());
        var clash = M3FiveLibraryIndexViews.capture(
                new String[] {"x", "y"},
                new Integer[] {1, 1},
                new int[] {0, 1},
                new byte[] {1, 1});
        assertThrows(IllegalArgumentException.class, clash::asGuavaBiMap);
    }

    @Test void composedOffsetMasksMaterializeCorrectProviderSemantics() {
        var source = M3FiveLibraryIndexViews.capture(
                new String[] {"z", "a", "a", "z", "b"},
                new Integer[] {3, 1, 1, 4, 2},
                new int[] {0, 1, 2, 4, 3, 2},
                new byte[] {1, 3, 1, 1, 3});
        var selected = source.slice(1, 5).filteredByState(1, 0);
        assertEquals(5, selected.size());
        assertEquals(3, selected.asGuavaMultiset().count(1));
        assertEquals(3, selected.asApacheMultiSet().getCount(1));
        assertEquals(3, selected.asEclipseListMultimap().get("a").size());
        assertEquals(1, selected.asEclipseSetMultimap().get("a").size());
        var ordered = selected.sortedByKey(Comparator.naturalOrder());
        assertEquals(List.of("a", "a", "a", "b", "z"),
                java.util.stream.IntStream.range(0, ordered.size())
                    .mapToObj(ordered::keyAt).toList());
        assertEquals(5, ordered.asGuavaOrdinalTable().size());
        if (M3IndexMaskCompactor.nativeAvailable()) {
            var nativeSelected = source.slice(1, 5).filteredByState(1, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            assertEquals(selected.asJdkList(), nativeSelected.asJdkList());
            assertEquals(selected.asGuavaMultiset(), nativeSelected.asGuavaMultiset());
        }
    }

    @Test void seededCrossProviderCountsPreserveLogicalOccurrences() {
        Random random = new Random(20261018L);
        for (int trial = 0; trial < 500; trial++) {
            int n = 1 + random.nextInt(48);
            Integer[] key = new Integer[n], value = new Integer[n];
            byte[] flags = new byte[n];
            int[] order = new int[n + 12];
            for (int i = 0; i < n; i++) {
                key[i] = random.nextInt(11);
                value[i] = random.nextInt(8);
                flags[i] = (byte) random.nextInt(256);
            }
            for (int i = 0; i < order.length; i++) order[i] = random.nextInt(n);
            var indexed = M3FiveLibraryIndexViews.capture(key, value, order, flags);
            int required = random.nextInt(256);
            var filtered = indexed.filteredByState(required, 0);
            java.util.Map<Integer, Integer> oracle = new java.util.HashMap<>();
            int[] expectedCounts = new int[8];
            for (int p : order) {
                if (((flags[p] & 0xff) & required) == required) {
                    oracle.put(key[p], value[p]);
                    expectedCounts[value[p]]++;
                }
            }
            assertEquals(oracle, filtered.asJdkMapLastWins());
            var guava = filtered.asGuavaMultiset();
            var apache = filtered.asApacheMultiSet();
            int total = 0;
            for (int v = 0; v < expectedCounts.length; v++) {
                assertEquals(expectedCounts[v], guava.count(v));
                assertEquals(expectedCounts[v], apache.getCount(v));
                total += expectedCounts[v];
            }
            assertEquals(total, guava.size());
            assertEquals(total, apache.size());
            assertEquals(total, filtered.asGuavaOrdinalTable().size());
            assertEquals(oracle.size(), filtered.asSpringLastValueMap().size());
        }
    }
}
