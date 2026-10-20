/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import com.m3.collections.M3TreeSet;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual pinned donors and existing M3 owners are required; no test doubles are permitted here. */
class M3SdkFamiliesTest {
    @Test void everyFactoryHasExactNamesAndAvailableM3Owners() throws Exception {
        assertEquals(25, M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));
    }
    @Test void apacheBackingOwnersAreM3RatherThanRenamedDonorStorage() throws Exception {
        var list = new M3ApacheListValuedMap<String, Integer>();
        var set = new M3ApacheSetValuedMap<String, Integer>();
        assertInstanceOf(M3LinkedList.class, list.createCollection());
        assertInstanceOf(M3LinkedHashSet.class, set.createCollection());
        for (Object map : List.of(list, set)) {
            var getMap = map.getClass().getSuperclass().getDeclaredMethod("getMap");
            getMap.setAccessible(true);
            assertInstanceOf(M3LinkedHashMap.class, getMap.invoke(map));
        }
    }
    @Test void apacheListAndSetMatchPinnedDonorMutationContracts() {
        compare(new M3ApacheListValuedMap<>(), new ArrayListValuedHashMap<>(), true);
        compare(new M3ApacheSetValuedMap<>(), new HashSetValuedHashMap<>(), false);
    }
    private static void compare(MultiValuedMap<Integer, Integer> actual,
            MultiValuedMap<Integer, Integer> oracle, boolean lists) {
        Random random = new Random(20261017);
        for (int i = 0; i < 4000; i++) {
            Integer key = random.nextInt(8), value = random.nextInt(5);
            switch (random.nextInt(7)) {
                case 0, 1 -> assertEquals(oracle.put(key, value), actual.put(key, value));
                case 2 -> assertEquals(oracle.removeMapping(key, value), actual.removeMapping(key, value));
                case 3 -> { oracle.get(key).clear(); actual.get(key).clear(); }
                case 4 -> assertEquals(oracle.get(key).add(value), actual.get(key).add(value));
                case 5 -> assertEquals(oracle.keySet().remove(key), actual.keySet().remove(key));
                default -> assertEquals(oracle.putAll(key, Arrays.asList(value, value, null)), actual.putAll(key, Arrays.asList(value, value, null)));
            }
            assertEquals(oracle.size(), actual.size());
            assertEquals(oracle.asMap(), actual.asMap());
            assertEquals(oracle.hashCode(), actual.hashCode());
            for (int k = 0; k < 8; k++) {
                if (lists) assertEquals(new ArrayList<>(oracle.get(k)), new ArrayList<>(actual.get(k)));
                else assertEquals(new java.util.HashSet<>(oracle.get(k)), new java.util.HashSet<>(actual.get(k)));
                assertEquals(oracle.keys().getCount(k), actual.keys().getCount(k));
            }
        }
    }
    @Test void apacheAbsentViewsAndLastValueRemovalRemainLive() {
        var map = M3LibraryCollections.<String, Integer>apacheListValuedMap();
        var values = map.get("key");
        assertFalse(map.containsKey("key"));
        values.addAll(List.of(1, 1, 2)); assertEquals(3, map.size());
        assertEquals(List.of(1, 1, 2), values);
        var iterator = values.iterator(); iterator.next(); iterator.remove();
        assertEquals(List.of(1, 2), map.get("key"));
        values.clear(); assertFalse(map.containsKey("key"));
        values.add(9); assertEquals(List.of(9), map.get("key"));
        map.asMap().remove("key"); assertTrue(values.isEmpty());
    }
    @Test void springAndApacheNamesDoNotConflateCardinalityOrEmptyMappings() {
        var apache = M3LibraryCollections.<String, Integer>apacheListValuedMap();
        var spring = M3LibraryCollections.<String, Integer>springMultiValueMap();
        apache.put("a", 1); apache.put("a", 2);
        spring.add("a", 1); spring.add("a", 2);
        assertEquals(2, apache.size()); assertEquals(1, spring.size());
        spring.put("empty", new ArrayList<>()); assertTrue(spring.containsKey("empty"));
        apache.get("empty"); assertFalse(apache.containsKey("empty"));
        var borrowed = new ArrayList<>(List.of(7)); spring.put("borrowed", borrowed);
        assertSame(borrowed, spring.get("borrowed"));
    }
    @Test void guavaFilteredCollectionKeepsOwnerAndPredicateLive() {
        var owner = new M3LinkedList<Integer>(); owner.addAll(List.of(1, 2, 3, 4));
        var threshold = new AtomicInteger(2);
        var actual = M3LibraryCollections.guavaFilteredCollection(owner, value -> value >= threshold.get());
        assertEquals(List.of(2, 3, 4), new ArrayList<>(actual));
        threshold.set(4); assertEquals(List.of(4), new ArrayList<>(actual));
        owner.add(5); assertEquals(List.of(4, 5), new ArrayList<>(actual));
        assertThrows(IllegalArgumentException.class, () -> actual.add(1));
        var iterator = actual.iterator(); iterator.next(); assertThrows(UnsupportedOperationException.class, iterator::remove);
        var oracle = Collections2.filter(new ArrayList<>(owner), value -> value >= threshold.get());
        assertEquals(new ArrayList<>(oracle), new ArrayList<>(actual));
        actual.clear(); assertEquals(List.of(1, 2, 3), owner);
    }
    @Test void guavaSortedRangesAndDescendingViewsStayBacked() {
        var owner = new M3TreeSet<Integer>(Comparator.naturalOrder());
        owner.addAll(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        var actual = M3LibraryCollections.guavaFilteredNavigableSet(owner, value -> value % 2 == 0);
        var oracleOwner = new TreeSet<>(owner);
        var oracle = Sets.filter(oracleOwner, value -> value % 2 == 0);
        assertEquals(new ArrayList<>(oracle.descendingSet()), new ArrayList<>(actual.descendingSet()));
        var range = actual.subSet(2, true, 6, false);
        range.remove(4); assertFalse(owner.contains(4));
        assertThrows(IllegalArgumentException.class, () -> range.add(3));
        assertThrows(IllegalArgumentException.class, () -> range.add(8));
        owner.add(0); assertEquals(0, actual.first());
        assertEquals(8, actual.pollLast()); assertFalse(owner.contains(8));
    }
}
