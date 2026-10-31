/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.Table;
import com.m3.collections.M3IndexPathMasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Real JDK/Guava/Eclipse/Spring/Apache contract differential against M3
 * parallel-array captured occurrence paths. No SDK-type test stand-ins.
 * Selected materializations own independent M3 data, not borrowed key/value arrays.
 */
final class M3FiveLibraryExpandedContractsTest {
    private static M3FiveLibraryIndexViews<String, Integer> selected() {
        var source = M3FiveLibraryIndexViews.capture(
                new String[] {"a", "b", "b", "c", "d", "e"},
                new Integer[] {1, 2, 2, 3, 2, 4},
                new int[] {0, 1, 2, 3, 4, 5, 1},
                new byte[] {1, 1, 1, 0, 1, 1});
        return source.filteredByState(1, 0);
    }

    @Test
    void exactCountsAndDetachmentAcrossFiveDistinctProviders() {
        var source = selected();
        assertEquals(6, source.size());
        var expected = List.of(1, 2, 2, 2, 4, 2);
        assertEquals(expected, source.asJdkList());
        assertEquals(expected, new ArrayList<>(source.asJdkDeque()));
        assertEquals(List.of(1, 2, 4), new ArrayList<>(source.asJdkSet()));
        var guava = source.asGuavaMultiset();
        var eclipse = source.asEclipseBag();
        var apache = source.asApacheMultiSet();
        assertEquals(6, guava.size());
        assertEquals(6, eclipse.size());
        assertEquals(6, apache.size());
        assertEquals(4, guava.count(2));
        assertEquals(4, eclipse.occurrencesOf(2));
        assertEquals(4, apache.getCount(2));
        assertEquals(3, guava.elementSet().size());
        assertEquals(3, eclipse.sizeDistinct());
        assertEquals(3, apache.uniqueSet().size());
        var eclipseMultimap = source.asEclipseBagMultimap();
        assertEquals(6, eclipseMultimap.size());
        assertEquals(3, eclipseMultimap.get("b").occurrencesOf(2));
        assertEquals(1, eclipseMultimap.get("d").occurrencesOf(2));

        var spring = source.asSpringMultiValueMap();
        var springSingle = source.asSpringSingleValueMapLastWins();
        assertEquals(4, spring.size()); // Spring counts keys, not occurrences
        assertEquals(4, springSingle.size());
        assertEquals(List.of(2, 2, 2), spring.get("b"));
        assertEquals(List.of(2), springSingle.get("b"));
        assertEquals(Integer.valueOf(2), springSingle.getFirst("b"));
        assertEquals(6, source.asApacheListValuedMap().size()); // pairs
        assertEquals(4, source.asApacheSetValuedMap().size());

        guava.add(2);
        eclipse.add(2);
        apache.add(2);
        spring.add("b", 99);
        assertEquals(expected, source.asJdkList());
        assertEquals(6, source.size());
        assertEquals(4, source.asGuavaMultiset().count(2));
    }

    @Test
    void strictGuavaBijectionAndApacheValueDisplacementRemainDistinct() {
        var unique = M3FiveLibraryIndexViews.capture(
                new String[] {"a", "b", "c"},
                new Integer[] {1, 2, 3}, new int[] {0, 1, 2},
                new byte[] {1, 1, 1});
        BiMap<String, Integer> guava = unique.asGuavaBiMap();
        assertEquals(3, guava.size());
        assertEquals("b", guava.inverse().get(2));
        assertSame(guava, guava.inverse().inverse());
        assertEquals(3, unique.asApacheBidiMap().size());

        var duplicates = M3FiveLibraryIndexViews.capture(
                new String[] {"x", "y"},
                new Integer[] {1, 1}, new int[] {0, 1},
                new byte[] {1, 1});
        assertThrows(IllegalArgumentException.class, duplicates::asGuavaBiMap);
        var apache = duplicates.asApacheBidiMap();
        assertEquals(1, apache.size());
        assertEquals("y", apache.getKey(1));
        assertNull(apache.get("x")); // Apache put displaces previous owner
    }

    @Test
    void indexedSortingAndFilteredWrappersUseExistingM3Owners() {
        var selected = selected();
        var jdkSorted = selected.asJdkNavigableMapLastWins(Comparator.naturalOrder());
        var eclipseSorted = selected.asEclipseSortedMapLastWins(Comparator.naturalOrder());
        assertEquals(jdkSorted, eclipseSorted);
        assertEquals(List.of("a", "b", "d", "e"), new ArrayList<>(jdkSorted.keySet()));
        var filtered = selected.asJdkReadOnlyFilteredSortedSet(
                Comparator.naturalOrder(), value -> (value & 1) == 0);
        assertEquals(List.of(2, 4), new ArrayList<>(filtered));
        assertThrows(UnsupportedOperationException.class, () -> filtered.add(6));
        NavigableSet<Integer> head = filtered.headSet(3, true);
        assertEquals(List.of(2), new ArrayList<>(head));

        var sortedMultimap = selected.asGuavaSortedSetMultimap(Comparator.naturalOrder());
        assertEquals(4, sortedMultimap.size());
        assertEquals(List.of(2), new ArrayList<>(sortedMultimap.get("b")));
        assertEquals(4, selected.asEclipseSetMultimap().size());
        assertEquals(6, selected.asEclipseListMultimap().size());

        AtomicInteger minimum = new AtomicInteger(3);
        var guavaFiltered = selected.asGuavaFilteredCollection(
                value -> value != null && value >= minimum.get());
        assertEquals(List.of(4), new ArrayList<>(guavaFiltered));
        minimum.set(2);
        assertEquals(List.of(2, 2, 2, 4, 2), new ArrayList<>(guavaFiltered));
        assertThrows(IllegalArgumentException.class, () -> guavaFiltered.add(1));
        guavaFiltered.add(9);
        assertEquals(6, selected.size());
        assertEquals(List.of(1, 2, 2, 2, 4, 2), selected.asJdkList());

        record Cell(String row, int col) { }
        Cell[] keys = {new Cell("r", 1), new Cell("r", 2), new Cell("r", 1)};
        var cells = M3FiveLibraryIndexViews.capture(keys, new String[] {"a", "b", "new"},
                new int[] {0, 1, 2}, new byte[] {1, 1, 1});
        Table<String, Integer, String> table = cells.asGuavaTable(Cell::row, Cell::col);
        assertEquals(2, table.size());
        assertEquals("new", table.get("r", 1));
        assertEquals("b", table.get("r", 2));
    }

    @Test
    void seededMultiStageMaskAndBagCountsAgainstJdkOracle() {
        Random random = new Random(0x51_8A_2026L);
        for (int trial = 0; trial < 600; trial++) {
            int n = random.nextInt(70);
            int occurrences = n == 0 ? 0 : random.nextInt(120);
            Integer[] keys = new Integer[n];
            Integer[] values = new Integer[n];
            byte[] bytes = new byte[n];
            int[] indexes = new int[occurrences];
            int[][] flags = new int[2][n];
            for (int i = 0; i < n; i++) {
                keys[i] = random.nextInt(10);
                values[i] = random.nextInt(15) - 7;
                bytes[i] = (byte) random.nextInt(256);
                flags[0][i] = random.nextInt();
                flags[1][i] = random.nextInt();
            }
            for (int i = 0; i < occurrences; i++) indexes[i] = random.nextInt(n);
            int[] expectedPath = indexes.clone();
            var source = M3FiveLibraryIndexViews.capture(keys, values, indexes, bytes);
            var want = new ArrayList<Integer>();
            Map<Integer, Integer> wantCounts = new HashMap<>();
            int[] required = {0x80000000, 0x00000002};
            int[] forbidden = {0x40000000, 0x00000004};
            for (int index : indexes) {
                if ((flags[0][index] & required[0]) != required[0]
                        || (flags[0][index] & forbidden[0]) != 0
                        || (flags[1][index] & required[1]) != required[1]
                        || (flags[1][index] & forbidden[1]) != 0) continue;
                want.add(values[index]);
                wantCounts.merge(values[index], 1, Integer::sum);
            }
            var selected = source.filteredByStatePlanes(
                    flags, required, forbidden, M3IndexPathMasks.Backend.JAVA);
            assertArrayEquals(expectedPath, indexes);
            assertEquals(want, selected.asJdkList(), "path " + trial);
            assertEquals(want, new ArrayList<>(selected.asJdkDeque()));
            var guava = selected.asGuavaMultiset();
            var eclipse = selected.asEclipseBag();
            var apache = selected.asApacheMultiSet();
            assertEquals(want.size(), guava.size());
            assertEquals(want.size(), eclipse.size());
            assertEquals(want.size(), apache.size());
            for (int value = -7; value <= 7; value++) {
                int amount = wantCounts.getOrDefault(value, 0);
                assertEquals(amount, guava.count(value));
                assertEquals(amount, eclipse.occurrencesOf(value));
                assertEquals(amount, apache.getCount(value));
            }
            if (M3IndexPathMasks.nativeAvailable() && trial % 19 == 0) {
                assertEquals(want, source.filteredByStatePlanes(
                        flags, required, forbidden, M3IndexPathMasks.Backend.NATIVE_REQUIRED)
                        .asJdkList(), "real JNI parity trial " + trial);
            }
        }
    }
}
