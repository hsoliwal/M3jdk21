/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3IndexPathMasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-provider JUnit: one captured indexed payload serves five distinct SDKs.
 * Full-width multi-stage flags use the established Java/C17 JNI selector.
 * No provider implementation stubs count as qualification.
 */
final class M3FiveLibraryState32Test {
    private static int[][] planes() {
        return new int[][] {
                {0x80000001, 0x80000003, 0x40000001, 0x80000001, 0x80000001},
                {2, 2, 2, 4, 2}
        };
    }

    private static int[] required() { return new int[] {0x80000000, 2}; }
    private static int[] excluded() { return new int[] {0x40000000, 0}; }

    private static M3FiveLibraryIndexViews<String, Integer> fixture() {
        return M3FiveLibraryIndexViews.capture(
                new String[] {"a", "b", "c", null, "d"},
                new Integer[] {1, 2, 2, 3, 4},
                new int[] {1, 0, 1, 2, 3, 4},
                new byte[] {1, (byte) 0x81, 0, 1, (byte) 0x81});
    }

    @Test
    void full32BitMasksRespectStagesPhysicalIndexesAndDuplicates() {
        var root = fixture();
        var selected = root.filteredByStatePlanes(planes(), required(), excluded());
        assertEquals(4, selected.size());
        assertEquals(List.of("b", "a", "b", "d"),
                IntStream.range(0, selected.size()).mapToObj(selected::keyAt).toList());
        assertEquals(List.of(2, 1, 2, 4), selected.asJdkList());
        assertEquals(0, root.firstMatchingOrdinalByStatePlanes(planes(), required(), excluded()));
        assertEquals(-1, root.firstMatchingOrdinalByStatePlanes(
                planes(), new int[] {-1, -1}, excluded()));
        assertEquals(0, root.slice(1, 5).firstMatchingOrdinalByStatePlanes(
                planes(), required(), excluded()));
        assertEquals(List.of(1, 2), root.slice(1, 5)
                .filteredByStatePlanes(planes(), required(), excluded())
                .asJdkList());
        var sorted = selected.sortedByKey(Comparator.naturalOrder());
        assertEquals(List.of("a", "b", "b", "d"),
                IntStream.range(0, sorted.size()).mapToObj(sorted::keyAt).toList());
        assertEquals(List.of(1, 2, 2, 4), sorted.asJdkList());
    }

    @Test
    void realFiveProviderResultsHaveTheirOwnSemantics() {
        var selected = fixture().filteredByStatePlanes(planes(), required(), excluded());

        var jdkMap = selected.asJdkMapLastWins();
        var jdkSorted = selected.asJdkNavigableMapLastWins(Comparator.naturalOrder());
        assertEquals(3, jdkMap.size());
        assertEquals(jdkMap, jdkSorted);
        assertEquals(List.of("a", "b", "d"), new ArrayList<>(jdkSorted.keySet()));

        var guavaList = selected.asGuavaListMultimap();
        var guavaSorted = selected.asGuavaSortedSetMultimap(Comparator.naturalOrder());
        assertEquals(4, guavaList.size());
        assertEquals(List.of(2, 2), guavaList.get("b"));
        assertEquals(3, guavaSorted.size());
        assertEquals(List.of(2), new ArrayList<>(guavaSorted.get("b")));

        var eclipseMap = selected.asEclipseMapLastWins();
        var eclipseSet = selected.asEclipseSet();
        var eclipseList = selected.asEclipseListMultimap();
        var eclipsePairs = selected.asEclipseSetMultimap();
        assertEquals(3, eclipseMap.size());
        assertEquals(3, eclipseSet.size());
        assertEquals(4, eclipseList.size());
        assertEquals(List.of(2, 2), eclipseList.get("b").toList());
        assertEquals(3, eclipsePairs.size());
        assertEquals(1, eclipsePairs.get("b").size());

        var spring = selected.asSpringMultiValueMap();
        assertEquals(3, spring.size()); // keys, unlike pair-count Guava/Apache
        assertEquals(List.of(2, 2), spring.get("b"));
        var apacheList = selected.asApacheListValuedMap();
        var apacheSet = selected.asApacheSetValuedMap();
        assertEquals(4, apacheList.size());
        assertEquals(List.of(2, 2), apacheList.get("b"));
        assertEquals(3, apacheSet.size());

        guavaList.put("b", 99);
        eclipseList.put("b", 88);
        spring.add("b", 77);
        apacheList.put("b", 66);
        assertEquals(4, selected.size());
        assertEquals(List.of(2, 1, 2, 4), selected.asJdkList());
    }

    @Test
    void malformedPlanesAndExplicitNativeRequireRefuseCorrectly() {
        var source = fixture();
        assertThrows(IllegalArgumentException.class, () -> source.filteredByStatePlanes(
                new int[][] {new int[4]}, new int[] {0}, new int[] {0}));
        assertThrows(NullPointerException.class, () -> source.filteredByStatePlanes(
                new int[][] {null}, new int[] {0}, new int[] {0}));
        assertThrows(IllegalArgumentException.class, () -> source.filteredByStatePlanes(
                new int[0][], new int[0], new int[0]));
        assertThrows(IllegalArgumentException.class, () -> source.filteredByStatePlanes(
                planes(), new int[] {1}, new int[] {0}));
        assertThrows(NullPointerException.class, () -> source.filteredByStatePlanes(
                planes(), null, excluded()));
        var empty = source.slice(0, 0);
        if (!M3IndexPathMasks.nativeAvailable()) {
            assertThrows(UnsatisfiedLinkError.class, () -> empty.filteredByStatePlanes(
                    planes(), required(), excluded(), M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        } else {
            assertTrue(empty.filteredByStatePlanes(
                    planes(), required(), excluded(), M3IndexPathMasks.Backend.NATIVE_REQUIRED)
                    .isEmpty());
            assertEquals(source.filteredByStatePlanes(planes(), required(), excluded()).asJdkList(),
                    source.filteredByStatePlanes(planes(), required(), excluded(),
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED).asJdkList());
        }
    }

    @Test
    void seededMultiStageMaskOracleAndOptionalNativeParity() {
        Random random = new Random(0x5_1B17_2026L);
        for (int trial = 0; trial < 1_024; trial++) {
            int count = random.nextInt(97);
            int occurrences = count == 0 ? 0 : random.nextInt(170);
            Integer[] keys = new Integer[count];
            Integer[] values = new Integer[count];
            byte[] captured = new byte[count];
            int[] rows = new int[occurrences];
            int[][] stages = new int[3][count];
            for (int i = 0; i < count; i++) {
                keys[i] = random.nextInt(12);
                values[i] = i;
                captured[i] = (byte) random.nextInt(256);
                for (int s = 0; s < stages.length; s++) stages[s][i] = random.nextInt();
            }
            for (int i = 0; i < occurrences; i++) rows[i] = random.nextInt(count);
            int[] need = new int[3];
            int[] forbid = new int[3];
            for (int s = 0; s < 3; s++) {
                need[s] = random.nextInt() & 0x8000000F;
                forbid[s] = random.nextInt() & 0x400000F0;
            }
            int[] original = rows.clone();
            var source = M3FiveLibraryIndexViews.capture(keys, values, rows, captured);
            var expected = new ArrayList<Integer>();
            int firstOrdinal = -1;
            for (int index = 0; index < rows.length; index++) {
                int row = rows[index];
                boolean passes = true;
                for (int stage = 0; stage < 3; stage++) {
                    int word = stages[stage][row];
                    if ((word & need[stage]) != need[stage]
                            || (word & forbid[stage]) != 0) {
                        passes = false;
                        break;
                    }
                }
                if (passes) {
                    if (firstOrdinal < 0) firstOrdinal = index;
                    expected.add(values[row]);
                }
            }
            var output = source.filteredByStatePlanes(stages, need, forbid);
            assertEquals(expected, output.asJdkList(), "trial " + trial);
            assertEquals(firstOrdinal,
                    source.firstMatchingOrdinalByStatePlanes(stages, need, forbid),
                    "first matching ordinal at trial " + trial);
            assertArrayEquals(original, rows);
            if (M3IndexPathMasks.nativeAvailable() && trial % 17 == 0) {
                assertEquals(expected, source.filteredByStatePlanes(stages, need, forbid,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED).asJdkList(),
                        "JNI parity trial " + trial);
            }
        }
    }
}
