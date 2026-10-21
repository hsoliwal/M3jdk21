/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3IndexPathMasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Real five-provider JUnit oracle for one indexed M3 source group directory.
 * No vendor interface stubs; real Guava/Eclipse/Spring/Apache Maven jars required.
 */
final class M3FiveProviderKeyGroupClosureTest {

    private static M3FiveProviderKeyGroups<String, Integer> fixture() {
        var captured = M3FiveLibraryIndexViews.capture(
                new String[] {"red", "blue", "red", "green", "red", "blue", null},
                new Integer[] {1, 2, 1, 3, 4, 2, null},
                new int[] {0, 1, 2, 3, 4, 5, 0, 6},
                new byte[] {1, 1, 1, 1, 1, 1, 1});
        return M3FiveProviderKeyGroups.snapshot(captured);
    }

    private static <K, V> List<K> keys(M3FiveProviderKeyGroups<K, V> groups) {
        List<K> result = new ArrayList<>();
        for (int group = 0; group < groups.groupCount(); group++) result.add(groups.keyAt(group));
        return result;
    }

    @Test
    void typedCountsAndSetsPreserveEachVendorContractAndDetachedCapture() {
        var groups = fixture();
        assertEquals(4, groups.groupCount());
        assertEquals(8, groups.occurrenceCount());
        assertEquals(Arrays.asList("red", "blue", "green", null), keys(groups));
        assertEquals(List.of(1, 1, 4, 1), groups.valuesFor("red"));
        assertEquals(Arrays.asList((Integer) null), groups.valuesFor(null));

        Map<String, Set<Integer>> expectedSets = new LinkedHashMap<>();
        expectedSets.put("red", new LinkedHashSet<>(List.of(1, 4)));
        expectedSets.put("blue", new LinkedHashSet<>(List.of(2)));
        expectedSets.put("green", new LinkedHashSet<>(List.of(3)));
        expectedSets.put(null, new LinkedHashSet<>(Arrays.asList((Integer) null)));
        assertEquals(expectedSets, groups.asJdkMapOfSets());
        assertEquals(new LinkedHashSet<>(expectedSets.keySet()),
                groups.asJdkDistinctKeySet());
        assertEquals(Map.of("red", 4, "blue", 2, "green", 1),
                groups.filteredKeys(k -> k != null).asJdkOccurrenceCounts());

        var guava = groups.asGuavaSetMultimap();
        var eclipseSet = groups.asEclipseSetMultimap();
        var apacheSet = groups.asApacheSetValuedMap();
        assertEquals(5, guava.size());
        assertEquals(5, eclipseSet.size());
        assertEquals(5, apacheSet.size());
        assertEquals(Set.of(1, 4), guava.get("red"));
        assertEquals(Set.of(1, 4), eclipseSet.get("red").toSet());
        assertEquals(Set.of(1, 4), apacheSet.get("red"));

        var guavaBag = groups.asGuavaKeyMultiset();
        var eclipseBag = groups.asEclipseKeyBag();
        var apacheBag = groups.asApacheKeyMultiSet();
        assertEquals(8, guavaBag.size());
        assertEquals(8, eclipseBag.size());
        assertEquals(8, apacheBag.size());
        assertEquals(4, guavaBag.count("red"));
        assertEquals(4, eclipseBag.occurrencesOf("red"));
        assertEquals(4, apacheBag.getCount("red"));
        assertEquals(1, guavaBag.count(null));
        assertEquals(1, eclipseBag.occurrencesOf(null));
        assertEquals(1, apacheBag.getCount(null));

        var eclipseMulti = groups.asEclipseBagMultimap();
        assertEquals(8, eclipseMulti.size());
        assertEquals(3, eclipseMulti.get("red").occurrencesOf(1));
        var spring = groups.asSpringSingleValueMapLastWins();
        assertEquals(4, spring.size()); // Spring distinct keys, not pair counts
        assertEquals(Integer.valueOf(1), spring.getFirst("red"));
        assertEquals(Integer.valueOf(2), spring.getFirst("blue"));
        // Three non-null groups, their sorted unique values are red[1,4], blue[2], green[3].
        assertEquals(4, groups.filteredKeys(k -> k != null)
                .asGuavaSortedSetMultimap(Comparator.naturalOrder()).size());

        guava.put("red", 42);
        apacheSet.put("blue", 66);
        eclipseMulti.put("red", 88);
        eclipseBag.addOccurrences("red", 2);
        spring.set("red", 99);
        assertEquals(8, groups.occurrenceCount());
        assertEquals(List.of(1, 1, 4, 1), groups.valuesFor("red"));
        assertEquals(5, groups.asGuavaSetMultimap().size());
    }

    @Test
    void fullWidthGroupStateMasksUsePhysicalIdsAndNativeFailClosed() {
        var groups = fixture();
        int[][] states = {
                {0x80000001, 0x80000002, 0x1, 0x80000001},
                {0x20, 0x20, 0x20, 0x20}
        };
        int[] required = {0x80000000, 0x20};
        int[] forbidden = {0x02, 0};
        var selected = groups.filteredGroupStates(states, required, forbidden);
        assertEquals(Arrays.asList("red", null), keys(selected));
        assertEquals(5, selected.occurrenceCount());
        assertEquals(0, groups.firstMatchingGroupOrdinal(states, required, forbidden));
        assertEquals(-1, groups.firstMatchingGroupOrdinal(states,
                new int[] {-1, 0x40}, forbidden));

        var window = groups.groupWindow(1, 3);
        assertEquals(List.of("blue", "green"), keys(window));
        assertEquals(List.of("blue"), keys(window.filteredGroupStates(states,
                new int[] {0x80000000, 0x20}, new int[] {0, 0})));
        assertEquals(0, window.firstMatchingGroupOrdinal(states,
                new int[] {0x80000000, 0x20}, new int[] {0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> groups.filteredGroupStates(new int[][] {new int[3]},
                        new int[] {0}, new int[] {0}));
        assertThrows(NullPointerException.class,
                () -> groups.filteredGroupStates(new int[][] {null},
                        new int[] {0}, new int[] {0}));
        assertThrows(IllegalArgumentException.class,
                () -> groups.filteredGroupStates(new int[0][], new int[0], new int[0]));
        assertThrows(IllegalArgumentException.class,
                () -> groups.filteredGroupStates(states, new int[] {0}, forbidden));

        var empty = groups.groupWindow(0, 0);
        if (M3IndexPathMasks.nativeAvailable()) {
            assertEquals(keys(selected), keys(groups.filteredGroupStates(states,
                    required, forbidden, M3IndexPathMasks.Backend.NATIVE_REQUIRED)));
            assertTrue(empty.filteredGroupStates(states, required, forbidden,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED).groupCount() == 0);
        } else {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> empty.filteredGroupStates(states, required, forbidden,
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
        assertEquals(Arrays.asList("green", null, "blue", "red"),
                keys(groups.rankedGroupsByCount(false)));
        assertEquals(Arrays.asList("red", "blue", "green", null),
                keys(groups.rankedGroupsByCount(true)));
        assertEquals(List.of("red", "blue"), keys(groups.filteredGroupCounts(2, 4)));
        assertThrows(IllegalArgumentException.class, () -> groups.filteredGroupCounts(4, 2));
        assertThrows(IllegalArgumentException.class, () -> groups.filteredGroupCounts(-1, 2));
    }

    @Test
    void seededGroupCountersIndexFilteringAndFiveVendorDifferential() {
        Random random = new Random(0x5163_2026L);
        for (int trial = 0; trial < 850; trial++) {
            int physical = random.nextInt(70);
            int occurrences = physical == 0 ? 0 : random.nextInt(150);
            Integer[] keys = new Integer[physical];
            Integer[] values = new Integer[physical];
            int[] indexes = new int[occurrences];
            byte[] bytes = new byte[physical];
            for (int row = 0; row < physical; row++) {
                keys[row] = random.nextInt(14) - 7;
                values[row] = random.nextInt(16) - 8;
                bytes[row] = (byte) random.nextInt(256);
            }
            for (int i = 0; i < occurrences; i++) indexes[i] = random.nextInt(physical);
            int[] savedIndexes = indexes.clone();
            var captured = M3FiveLibraryIndexViews.capture(keys, values, indexes, bytes);
            var groups = M3FiveProviderKeyGroups.snapshot(captured);
            Map<Integer, List<Integer>> oracle = new LinkedHashMap<>();
            for (int row : indexes) {
                oracle.computeIfAbsent(keys[row], unused -> new ArrayList<>()).add(values[row]);
            }
            assertArrayEquals(savedIndexes, indexes);
            assertEquals(oracle, groups.asJdkMapOfLists(), "JDK grouped trial " + trial);
            assertEquals(oracle.size(), groups.groupCount());
            int groupSize = oracle.size();
            int[][] planes = new int[][] {new int[groupSize], new int[groupSize]};
            int[] needed = {0x80000000, 0x2};
            int[] denied = {0x40000000, 0x4};
            for (int i = 0; i < groupSize; i++) {
                planes[0][i] = random.nextInt();
                planes[1][i] = random.nextInt();
            }
            List<Integer> expectedKeys = new ArrayList<>();
            Map<Integer, List<Integer>> retained = new LinkedHashMap<>();
            int id = 0;
            int first = -1;
            for (var e : oracle.entrySet()) {
                boolean keep = true;
                for (int stage = 0; stage < planes.length; stage++) {
                    int state = planes[stage][id];
                    if ((state & needed[stage]) != needed[stage]
                            || (state & denied[stage]) != 0) keep = false;
                }
                if (keep) {
                    if (first < 0) first = id;
                    expectedKeys.add(e.getKey());
                    retained.put(e.getKey(), e.getValue());
                }
                id++;
            }
            var selected = groups.filteredGroupStates(planes, needed, denied);
            assertEquals(expectedKeys, keys(selected), "state grouped trial " + trial);
            assertEquals(first, groups.firstMatchingGroupOrdinal(planes, needed, denied));
            int count = retained.values().stream().mapToInt(List::size).sum();
            assertEquals(count, selected.occurrenceCount());
            assertEquals(retained, selected.asJdkMapOfLists());

            List<Integer> ranked = new ArrayList<>(expectedKeys);
            ranked.sort(Comparator.comparingInt((Integer key) -> retained.get(key).size()).reversed());
            assertEquals(ranked, keys(selected.rankedGroupsByCount(true)));

            Map<Integer, Set<Integer>> expectedSets = new LinkedHashMap<>();
            retained.forEach((key, vals) -> expectedSets.put(key, new LinkedHashSet<>(vals)));
            assertEquals(expectedSets, selected.asJdkMapOfSets());

            if (trial % 17 == 0) {
                int pairs = expectedSets.values().stream().mapToInt(Set::size).sum();
                assertEquals(pairs, selected.asGuavaSetMultimap().size());
                assertEquals(pairs, selected.asEclipseSetMultimap().size());
                assertEquals(pairs, selected.asApacheSetValuedMap().size());
                assertEquals(count, selected.asGuavaKeyMultiset().size());
                assertEquals(count, selected.asEclipseKeyBag().size());
                assertEquals(count, selected.asApacheKeyMultiSet().size());
                assertEquals(retained.size(), selected.asSpringSingleValueMapLastWins().size());
                for (var e : retained.entrySet()) {
                    Integer key = e.getKey();
                    assertEquals(e.getValue().size(), selected.asGuavaKeyMultiset().count(key));
                    assertEquals(e.getValue().size(), selected.asEclipseKeyBag().occurrencesOf(key));
                    assertEquals(e.getValue().size(), selected.asApacheKeyMultiSet().getCount(key));
                }
            }
            if (M3IndexPathMasks.nativeAvailable() && trial % 29 == 0) {
                assertEquals(expectedKeys, keys(groups.filteredGroupStates(planes,
                        needed, denied, M3IndexPathMasks.Backend.NATIVE_REQUIRED)));
            }
        }
    }
}
