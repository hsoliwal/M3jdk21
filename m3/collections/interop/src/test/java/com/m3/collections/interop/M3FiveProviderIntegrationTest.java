/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multiset;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Cross-provider ownership and actual parallel collector composition. */
final class M3FiveProviderIntegrationTest {
    @Test
    @SuppressWarnings("deprecation")
    void oneOwnerExposesProviderSpecificMutationReturnsAndFlags() {
        M3ObjectCounts<String> owner = new M3ObjectCounts<>();
        Multiset<String> guava = M3LibraryCollections.guavaMultiset(owner);
        MultiSet<String> apache = M3LibraryCollections.apacheMultiSet(owner);
        var legacy = M3LibraryCollections.apacheBag(owner);
        MutableBag<String> eclipse = M3LibraryCollections.eclipseBag(owner);
        assertEquals(0, guava.add("shared", 3));
        owner.setFlags("shared", Integer.MIN_VALUE | 4);
        assertEquals(3, apache.add("shared", 2));
        assertEquals(5, eclipse.occurrencesOf("shared"));
        assertFalse(legacy.add("shared"));
        assertEquals(6, guava.count("shared"));
        assertEquals(List.of("shared"), owner.flaggedElements(4, 2, M3IndexPathMasks.Backend.JAVA));
        assertTrue(guava.remove("shared"));
        assertEquals(5, owner.count("shared"));
        assertTrue(legacy.remove("shared"));
        assertTrue(guava.isEmpty());
        assertEquals(0, owner.flags("shared"));
        eclipse.addOccurrences(null, 2);
        assertEquals(2, apache.getCount(null));
        owner.verifyInvariants();
    }

    @Test
    @SuppressWarnings("deprecation")
    void independentFactoriesDoNotShareStorage() {
        var first = M3LibraryCollections.guavaMultiset();
        var second = M3LibraryCollections.guavaMultiset();
        first.add("first");
        assertTrue(second.isEmpty());
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.guavaMultiset(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.apacheMultiSet(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.apacheBag(null));
        assertThrows(NullPointerException.class, () -> M3LibraryCollections.eclipseBag(null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void countCollectorsMergeCountsAndPreserveNulls() {
        List<Integer> data = IntStream.range(0, 12000).mapToObj(i -> i % 101 == 0 ? null : i % 19).toList();
        Map<Integer, Integer> expected = new java.util.HashMap<>();
        data.forEach(value -> expected.merge(value, 1, Integer::sum));
        var guava = data.parallelStream().collect(M3LibraryCollectors.toGuavaMultiset());
        var apache = data.parallelStream().collect(M3LibraryCollectors.toApacheMultiSet());
        var legacy = data.parallelStream().collect(M3LibraryCollectors.toApacheBag());
        var eclipse = data.parallelStream().collect(M3LibraryCollectors.toEclipseBag());
        assertEquals(data.size(), guava.size());
        assertEquals(data.size(), apache.size());
        assertEquals(data.size(), legacy.size());
        assertEquals(data.size(), eclipse.size());
        expected.forEach((value, count) -> {
            assertEquals(count.intValue(), guava.count(value));
            assertEquals(count.intValue(), apache.getCount(value));
            assertEquals(count.intValue(), legacy.getCount(value));
            assertEquals(count.intValue(), eclipse.occurrencesOf(value));
        });
        assertEquals(data.stream().distinct().toList(), new ArrayList<>(guava.elementSet()));
    }

    @Test
    void apacheMultivalueCollectorsPreserveEncounterOrderAndSetRules() {
        List<Integer> data = IntStream.range(0, 5000).mapToObj(i -> i % 67 == 0 ? null : i % 41).toList();
        Function<Integer, Integer> key = value -> value == null ? null : value % 5;
        var lists = data.parallelStream().collect(M3LibraryCollectors.toApacheListValuedMap(key, Function.identity()));
        var sets = data.parallelStream().collect(M3LibraryCollectors.toApacheSetValuedMap(key, Function.identity()));
        for (Integer k : Arrays.asList(null, 0, 1, 2, 3, 4)) {
            List<Integer> values = data.stream().filter(value -> java.util.Objects.equals(key.apply(value), k)).toList();
            assertEquals(values, lists.get(k));
            assertEquals(values.stream().distinct().toList(), new ArrayList<>(sets.get(k)));
        }
        lists.get(0).clear();
        assertFalse(lists.containsKey(0));
        sets.get(null).clear();
        assertFalse(sets.containsKey(null));
    }

    @Test
    void bidiCollectorsHaveAssociativeUniqueInputAndProviderResults() {
        List<Integer> input = IntStream.range(0, 4000).boxed().toList();
        BiMap<Integer, String> guava = input.parallelStream().collect(
                M3LibraryCollectors.toGuavaBiMap(Function.identity(), value -> "v" + value));
        BidiMap<Integer, String> apache = input.parallelStream().collect(
                M3LibraryCollectors.toApacheBidiMap(Function.identity(), value -> "v" + value));
        assertEquals(guava, apache);
        assertEquals(input, new ArrayList<>(guava.keySet()));
        assertEquals(input, new ArrayList<>(apache.keySet()));
        assertEquals(3000, guava.inverse().get("v3000"));
        assertEquals(3000, apache.inverseBidiMap().get("v3000"));
        assertThrows(IllegalArgumentException.class, () -> guava.put(5000, "v3000"));
        apache.put(5000, "v3000");
        assertFalse(apache.containsKey(3000));
        assertEquals(5000, apache.getKey("v3000"));
    }

    @Test
    void bidiCollectorConflictsAcrossPartitionsAreRefused() {
        List<Integer> data = IntStream.range(0, 3000).boxed().toList();
        assertThrows(IllegalStateException.class, () -> data.parallelStream().collect(
                M3LibraryCollectors.toGuavaBiMap(value -> value % 1000, Function.identity())));
        assertThrows(IllegalStateException.class, () -> data.parallelStream().collect(
                M3LibraryCollectors.toApacheBidiMap(Function.identity(), value -> value % 1000)));
        assertThrows(NullPointerException.class, () -> Stream.of(1).collect(
                M3LibraryCollectors.toGuavaBiMap(Function.identity(), value -> null)));
        assertThrows(NullPointerException.class, () -> M3LibraryCollectors.toApacheListValuedMap(null, Function.identity()));
        assertThrows(NullPointerException.class, () -> M3LibraryCollectors.toApacheSetValuedMap(Function.identity(), null));
    }

    @Test
    void eclipseOrderedMapKeepsItsRichOrderedContract() {
        List<Integer> input = List.of(7, 3, 9, 1);
        var map = input.parallelStream().collect(M3LibraryCollectors.toEclipseOrderedMap(
                Function.identity(), value -> "v" + value));
        assertEquals(input, new ArrayList<>(map.keySet()));
        assertEquals("v7", map.getFirst());
        assertEquals("v1", map.getLast());
        map.keySet().remove(3);
        assertFalse(map.containsKey(3));
        var empty = M3LibraryCollections.eclipseOrderedMap();
        empty.put(null, null);
        assertTrue(empty.containsKey(null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void newCollectorsDoNotPromiseConcurrentOrUnorderedResults() {
        List<Collector<?, ?, ?>> collectors = List.of(
                M3LibraryCollectors.toGuavaMultiset(), M3LibraryCollectors.toApacheMultiSet(),
                M3LibraryCollectors.toApacheBag(), M3LibraryCollectors.toEclipseBag(),
                M3LibraryCollectors.toGuavaBiMap(Function.identity(), Function.identity()),
                M3LibraryCollectors.toApacheBidiMap(Function.identity(), Function.identity()),
                M3LibraryCollectors.toApacheListValuedMap(Function.identity(), Function.identity()),
                M3LibraryCollectors.toApacheSetValuedMap(Function.identity(), Function.identity()),
                M3LibraryCollectors.toEclipseOrderedMap(Function.identity(), Function.identity()));
        for (Collector<?, ?, ?> collector : collectors) {
            assertFalse(collector.characteristics().contains(Collector.Characteristics.CONCURRENT));
            assertFalse(collector.characteristics().contains(Collector.Characteristics.UNORDERED));
        }
    }
}
