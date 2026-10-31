/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.m3.collections.M3IndexPathMasks;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Real Guava 33.4.8-jre donor differential. No donor source implementation copied. */
final class M3GuavaSetMultimapTest {
    @Test void actualFactoryAndIndexedStorage() {
        SetMultimap<String, Integer> existing = M3LibraryCollections.guavaSetMultimap();
        existing.put("old", 77);
        assertEquals(Set.of(77), existing.get("old"));
        SetMultimap<String, Integer> actual = M3GuavaSetMultimap.create();
        assertInstanceOf(M3GuavaSetMultimap.class, actual);
        actual.put("x", 1);
        actual.put("x", 1);
        actual.put("x", 2);
        assertEquals(2, actual.size());
        assertEquals(1, actual.keySet().size());
        assertEquals(Set.of(1, 2), actual.get("x"));
        assertEquals(1, ((M3GuavaSetMultimap<?, ?>) actual).distinctKeys());
    }

    @Test void liveAbsentViewsAndBackedMutation() {
        M3GuavaSetMultimap<String, Integer> map = new M3GuavaSetMultimap<>();
        Set<Integer> a = map.get("a");
        assertFalse(map.containsKey("a"));
        a.add(1); a.add(2); a.add(2);
        assertEquals(2, map.size());
        assertEquals(2, map.keys().count("a"));
        assertEquals(Set.of(1, 2), map.asMap().get("a"));
        map.entries().remove(new AbstractMap.SimpleImmutableEntry<>("a", 1));
        assertEquals(Set.of(2), a);
        map.keySet().remove("a");
        assertTrue(a.isEmpty());
        assertFalse(map.containsKey("a"));
        a.add(null);
        assertTrue(map.containsEntry("a", null));
        assertEquals(1, map.size());
        assertEquals(1, map.keys().count("a"));
        map.get("a").clear();
        assertTrue(map.isEmpty());
    }

    @Test void differentialAgainstRealHashMultimap() {
        M3GuavaSetMultimap<Integer, Integer> actual = new M3GuavaSetMultimap<>();
        SetMultimap<Integer, Integer> oracle = HashMultimap.create();
        Random random = new Random(0x5E7C0FFEEL);
        for (int step = 0; step < 7000; step++) {
            Integer key = random.nextInt(9) == 0 ? null : random.nextInt(35) - 17;
            Integer value = random.nextInt(9) == 0 ? null : random.nextInt(35) - 17;
            switch (random.nextInt(10)) {
                case 0 -> assertEquals(oracle.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(oracle.remove(key, value), actual.remove(key, value));
                case 2 -> assertEquals(oracle.get(key).add(value), actual.get(key).add(value));
                case 3 -> assertEquals(oracle.get(key).remove(value), actual.get(key).remove(value));
                case 4 -> assertEquals(oracle.removeAll(key), actual.removeAll(key));
                case 5 -> {
                    var values = Arrays.asList(value, value, null);
                    assertEquals(oracle.replaceValues(key, values), actual.replaceValues(key, values));
                }
                case 6 -> { oracle.get(key).clear(); actual.get(key).clear(); }
                case 7 -> assertEquals(oracle.keySet().remove(key), actual.keySet().remove(key));
                case 8 -> {
                    var entry = new AbstractMap.SimpleImmutableEntry<>(key, value);
                    assertEquals(oracle.entries().remove(entry), actual.entries().remove(entry));
                }
                default -> {
                    var items = Arrays.asList(value, null);
                    assertEquals(oracle.putAll(key, items), actual.putAll(key, items));
                }
            }
            assertEquals(oracle.size(), actual.size(), "size at " + step);
            assertEquals(oracle.asMap(), actual.asMap(), "values by key at " + step);
            assertEquals(oracle, actual, "equality at " + step);
            assertEquals(oracle.hashCode(), actual.hashCode(), "hash at " + step);
            assertEquals(oracle.keySet(), actual.keySet(), "key set at " + step);
            assertEquals(oracle.entries(), actual.entries(), "entries at " + step);
            for (Integer k : oracle.keySet()) {
                assertEquals(oracle.keys().count(k), actual.keys().count(k), "key count " + step);
            }
        }
    }

    @Test void stateWordSelectionAndNativeRequirement() {
        M3GuavaSetMultimap<String, Integer> source = new M3GuavaSetMultimap<>();
        source.put("a", 1); source.put("a", 2); source.put("b", null);
        source.put(null, 3); source.put("b", 4);
        List<Map.Entry<String, Integer>> encounter = new ArrayList<>(source.entries());
        int[] states = new int[encounter.size()];
        SetMultimap<String, Integer> expected = HashMultimap.create();
        for (int i = 0; i < states.length; i++) {
            states[i] = i % 2 == 0 ? 0x8000_0005 : 0x0000_0001;
            if ((states[i] & 0x8000_0004) == 0x8000_0004) {
                Map.Entry<String, Integer> e = encounter.get(i);
                expected.put(e.getKey(), e.getValue());
            }
        }
        var selected = source.selectedByStates(states, 0x8000_0004, 0,
                M3IndexPathMasks.Backend.JAVA);
        assertEquals(expected, selected);
        source.clear();
        assertEquals(expected, selected); // detached payload/index owner
        assertThrows(IllegalArgumentException.class, () -> selected.selectedByStates(
                new int[selected.size()+1], 0, 0, M3IndexPathMasks.Backend.JAVA));
        if (M3IndexPathMasks.nativeAvailable()) {
            var nativeSource = new M3GuavaSetMultimap<String, Integer>();
            for (Map.Entry<String, Integer> e : encounter) nativeSource.put(e.getKey(), e.getValue());
            assertEquals(expected, nativeSource.selectedByStates(states, 0x8000_0004, 0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        } else {
            assertThrows(UnsatisfiedLinkError.class, () -> selected.selectedByStates(
                    new int[selected.size()], 0, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }

    @Test void fiveProvidersKeepDistinctContracts() {
        var jdk = M3LibraryCollections.<String, Integer>jdkMap();
        var guava = M3GuavaSetMultimap.<String, Integer>create();
        var eclipse = M3LibraryCollections.<Integer>eclipseSet();
        var apache = M3LibraryCollections.<String, Integer>apacheListValuedMap();
        var spring = M3LibraryCollections.<String, Integer>springMultiValueMap();
        jdk.put("k", 1);
        guava.put("k", 1); guava.put("k", 1);
        eclipse.add(1);
        apache.put("k", 1); apache.put("k", 1);
        spring.add("k", 1); spring.add("k", 1);
        assertEquals(1, jdk.size());
        assertEquals(1, guava.size());
        assertEquals(1, eclipse.size());
        assertEquals(2, apache.size());
        assertEquals(1, spring.size());
        assertEquals(2, spring.get("k").size());
        assertEquals(1, guava.distinctKeys());
    }
}
