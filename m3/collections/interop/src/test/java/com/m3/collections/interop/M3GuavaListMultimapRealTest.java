/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Authentic Guava 33.4.8-jre donor differential, not a replaced Guava implementation. */
final class M3GuavaListMultimapRealTest {
    @Test void strictM3OwnerAndFullJdkReferenceCorpus() {
        M3GuavaListMultimapContract.run();
        ListMultimap<String,Integer> direct = M3FiveListFamilies.guava();
        assertInstanceOf(M3GuavaListMultimap.class, direct);
        direct.put("k", 7);
        assertTrue(direct.get("k") instanceof java.util.AbstractList<?>);
        assertEquals(List.of(7), direct.get("k"));
    }

    @Test void actualGuavaArrayListMultimapSixThousandOperationComparison() {
        ListMultimap<Integer,Integer> direct = new M3GuavaListMultimap<>();
        ListMultimap<Integer,Integer> donor = ArrayListMultimap.create();
        Random random = new Random(0xCC0011DDL);
        for (int step = 0; step < 6000; step++) {
            Integer key = random.nextInt(8) == 0 ? null : random.nextInt(39) - 19;
            Integer value = random.nextInt(9) == 0 ? null : random.nextInt(25) - 12;
            switch (random.nextInt(13)) {
                case 0, 1, 2 -> assertEquals(donor.put(key, value), direct.put(key, value));
                case 3 -> assertEquals(donor.remove(key, value), direct.remove(key, value));
                case 4 -> assertEquals(donor.removeAll(key), direct.removeAll(key));
                case 5 -> {
                    List<Integer> replacement = Arrays.asList(value, value, null);
                    assertEquals(donor.replaceValues(key, replacement),
                            direct.replaceValues(key, replacement));
                }
                case 6 -> {
                    assertEquals(donor.get(key).add(value), direct.get(key).add(value));
                }
                case 7 -> {
                    if (!donor.get(key).isEmpty()) {
                        int i = random.nextInt(donor.get(key).size());
                        assertEquals(donor.get(key).set(i, value), direct.get(key).set(i, value));
                    }
                }
                case 8 -> {
                    if (!donor.get(key).isEmpty()) {
                        int i = random.nextInt(donor.get(key).size());
                        assertEquals(donor.get(key).remove(i), direct.get(key).remove(i));
                    }
                }
                case 9 -> {
                    int occurrences = random.nextInt(5);
                    assertEquals(donor.keys().remove(key, occurrences),
                            direct.keys().remove(key, occurrences));
                }
                case 10 -> {
                    assertEquals(donor.asMap().remove(key), direct.asMap().remove(key));
                }
                case 11 -> {
                    List<Integer> values = Arrays.asList(value, value);
                    assertEquals(donor.putAll(key, values), direct.putAll(key, values));
                }
                case 12 -> {
                    if (!donor.get(key).isEmpty()) {
                        int size = donor.get(key).size();
                        int i = random.nextInt(size);
                        donor.get(key).subList(i, i + 1).clear();
                        direct.get(key).subList(i, i + 1).clear();
                    }
                }
                default -> throw new AssertionError();
            }
            assertEquals(donor.size(), direct.size(), "pair size at step " + step);
            assertEquals(donor.asMap(), direct.asMap(), "group and null semantics at " + step);
            assertEquals(donor.keys().count(key), direct.keys().count(key),
                    "counted keys at " + step);
            assertEquals(donor.get(key), direct.get(key), "live key values at " + step);
            assertEquals(donor.containsValue(value), direct.containsValue(value),
                    "value presence at " + step);
        }
    }

    @Test void directSubListAndAsMapViewsPreserveCountsAndDetachedRemoval() {
        M3GuavaListMultimap<String,Integer> map = new M3GuavaListMultimap<>();
        List<Integer> live = map.get("a");
        assertTrue(live.isEmpty());
        live.addAll(Arrays.asList(1, null, 2, 2, 3));
        assertEquals(5, map.size());
        live.subList(1, 4).clear();
        assertEquals(List.of(1, 3), map.get("a"));
        assertEquals(2, map.keys().count("a"));
        List<Integer> removed = map.removeAll("a");
        assertEquals(List.of(1, 3), removed);
        assertThrows(UnsupportedOperationException.class, () -> removed.add(4));
        assertFalse(map.containsKey("a"));
        live.add(77);
        assertEquals(List.of(77), map.get("a"));
        assertEquals(List.of(77), map.asMap().get("a"));
        map.asMap().get("a").clear();
        assertFalse(map.containsKey("a"));
        assertEquals(0, map.size());
    }

    @Test void signedBitMasksAndRequiredNativeParity() {
        M3GuavaListMultimap<String,Integer> input = new M3GuavaListMultimap<>();
        for (int i = 0; i < 256; i++) input.put("group" + i % 11, i % 8);
        int[] states = new int[input.size()];
        for (int i = 0; i < states.length; i++)
            states[i] = (i % 3 == 0 ? Integer.MIN_VALUE : 0) | (i % 5 == 0 ? 2 : 0);
        var java = input.selectedByStates(states, Integer.MIN_VALUE, 2,
                M3IndexPathMasks.Backend.JAVA);
        var reference = ArrayListMultimap.<String,Integer>create();
        List<Map.Entry<String,Integer>> rows = new ArrayList<>(input.entries());
        for (int i = 0; i < states.length; i++)
            if ((states[i] & Integer.MIN_VALUE) != 0 && (states[i] & 2) == 0)
                reference.put(rows.get(i).getKey(), rows.get(i).getValue());
        assertEquals(reference.asMap(), java.asMap());
        if (Boolean.getBoolean("m3.interop.requireNative")) {
            assertTrue(M3IndexPathMasks.nativeAvailable(), "native JNI required and not available");
            var nativeMap = input.selectedByStates(states, Integer.MIN_VALUE, 2,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            assertEquals(reference.asMap(), nativeMap.asMap());
        }
        java.put("extra", null);
        assertFalse(input.containsKey("extra"));
        assertEquals(256, input.size());
    }
}
