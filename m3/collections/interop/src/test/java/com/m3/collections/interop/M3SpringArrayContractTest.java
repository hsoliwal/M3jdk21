/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/** Exact real Spring 6.2 interface, donor differential and ownership qualification. */
final class M3SpringArrayContractTest {
    @Test void directContractNotSpringAdapter() {
        MultiValueMap<String, Integer> actual = M3FiveProviderDirectSdk.springMultiValueMap();
        assertInstanceOf(M3SpringArrayMultiValueMap.class, actual);
        assertEquals(AbstractMap.class, actual.getClass().getSuperclass());
        assertFalse(actual instanceof MultiValueMapAdapter<?, ?>);
        assertInstanceOf(M3LinkedHashMap.class, actual.toSingleValueMap());
        actual.set("k", 9);
        assertInstanceOf(M3LinkedList.class, actual.get("k"));
    }

    @Test void absentEmptyNullAndCallerListIdentity() {
        MultiValueMap<String, Integer> actual = new M3SpringArrayMultiValueMap<>();
        assertNull(actual.get("absent"));
        actual.addAll("empty", List.of());
        assertTrue(actual.containsKey("empty"));
        assertEquals(List.of(), actual.get("empty"));
        assertNull(actual.getFirst("empty"));
        actual.addIfAbsent("empty", 9);
        assertTrue(actual.get("empty").isEmpty());
        List<Integer> borrowed = new ArrayList<>(Arrays.asList(3, null, 5));
        actual.put("borrowed", borrowed);
        assertSame(borrowed, actual.get("borrowed"));
        borrowed.add(7);
        assertEquals(Arrays.asList(3, null, 5, 7), actual.get("borrowed"));
        actual.put("nullList", null);
        assertTrue(actual.containsKey("nullList"));
        assertNull(actual.getFirst("nullList"));
        actual.add("nullList", null);
        assertEquals(Arrays.asList((Integer) null), actual.get("nullList"));
        assertFalse(actual.toSingleValueMap().containsKey("empty"));
        assertEquals(3, actual.toSingleValueMap().get("borrowed"));
    }

    @Test void detachedCopyVersusLiveViewAndUnsupportedSingletonMutation() {
        MultiValueMap<String, Integer> actual = new M3SpringArrayMultiValueMap<>();
        actual.add("a", 1);
        Map<String, Integer> copy = actual.toSingleValueMap();
        Map<String, Integer> live = actual.asSingleValueMap();
        actual.set("a", 2);
        assertEquals(1, copy.get("a"));
        assertEquals(2, live.get("a"));
        assertEquals(2, live.put("a", 3));
        assertEquals(List.of(3), actual.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> actual.get("a").add(4));
        assertNull(live.put("null", null));
        assertTrue(actual.containsKey("null"));
        assertNull(actual.get("null"));
        assertEquals(actual.hashCode(), live.hashCode());
        assertEquals(actual.toString(), live.toString());
        assertEquals(3, live.remove("a"));
        assertFalse(actual.containsKey("a"));
    }

    @Test void realSpringSixTwoDifferentialSeeded() {
        MultiValueMap<Integer, Integer> actual = new M3SpringArrayMultiValueMap<>();
        MultiValueMap<Integer, Integer> expected = new MultiValueMapAdapter<>(new LinkedHashMap<>());
        Random random = new Random(0x20261017L);
        for (int step = 0; step < 4500; step++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(32);
            Integer value = random.nextInt(8) == 0 ? null : random.nextInt(50);
            switch (random.nextInt(13)) {
                case 0 -> { actual.add(key, value); expected.add(key, value); }
                case 1 -> { actual.set(key, value); expected.set(key, value); }
                case 2 -> { actual.addIfAbsent(key, value); expected.addIfAbsent(key, value); }
                case 3 -> assertEquals(expected.remove(key), actual.remove(key));
                case 4 -> {
                    List<Integer> input = Arrays.asList(value, value);
                    actual.addAll(key, input); expected.addAll(key, input);
                }
                case 5 -> {
                    List<Integer> a = random.nextInt(6) == 0 ? null : new ArrayList<>(Arrays.asList(value));
                    List<Integer> b = a == null ? null : new ArrayList<>(a);
                    assertEquals(expected.put(key, b), actual.put(key, a));
                }
                case 6 -> assertEquals(expected.asSingleValueMap().put(key, value),
                        actual.asSingleValueMap().put(key, value));
                case 7 -> assertEquals(expected.asSingleValueMap().remove(key),
                        actual.asSingleValueMap().remove(key));
                case 8 -> assertEquals(expected.putIfAbsent(key, Arrays.asList(value)),
                        actual.putIfAbsent(key, Arrays.asList(value)));
                case 9 -> { actual.setAll(Map.of(5, 9)); expected.setAll(Map.of(5, 9)); }
                case 10 -> assertEquals(expected.containsKey(key), actual.containsKey(key));
                case 11 -> {
                    if (step % 47 == 0) { actual.clear(); expected.clear(); }
                }
                case 12 -> {
                    Map<Integer, List<Integer>> copy = new LinkedHashMap<>();
                    copy.put(key, Arrays.asList(value));
                    actual.putAll(copy);
                    expected.putAll(copy);
                }
                default -> throw new AssertionError();
            }
            assertEquals(expected, actual, "step " + step);
            assertEquals(expected.size(), actual.size(), "size step " + step);
            assertEquals(expected.getFirst(key), actual.getFirst(key), "first step " + step);
            assertEquals(expected.toSingleValueMap(), actual.toSingleValueMap(), "copy step " + step);
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()), "order step " + step);
            assertEquals(expected.asSingleValueMap().get(key),
                    actual.asSingleValueMap().get(key), "view step " + step);
        }
    }
}
