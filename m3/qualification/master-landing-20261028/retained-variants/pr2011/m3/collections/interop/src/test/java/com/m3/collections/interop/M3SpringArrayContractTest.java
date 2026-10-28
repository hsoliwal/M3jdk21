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
        var actual = M3LibraryCollections.<String, Integer>springMultiValueMap();
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

    private record Outcome(Object value, Class<? extends RuntimeException> thrown) { }

    private static Outcome result(java.util.function.Supplier<?> command) {
        try {
            return new Outcome(command.get(), null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass());
        }
    }

    @Test void realSpringSixTwoDifferentialSeeded() {
        MultiValueMap<Integer, Integer> actual = new M3SpringArrayMultiValueMap<>();
        MultiValueMap<Integer, Integer> expected = new MultiValueMapAdapter<>(new LinkedHashMap<>());
        Random random = new Random(0x20261017L);
        for (int step = 0; step < 4500; step++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(32);
            Integer value = random.nextInt(8) == 0 ? null : random.nextInt(50);
            java.util.function.Supplier<?> donor;
            java.util.function.Supplier<?> candidate;
            int operation = random.nextInt(13);
            switch (operation) {
                case 0 -> {
                    donor = () -> { expected.add(key, value); return null; };
                    candidate = () -> { actual.add(key, value); return null; };
                }
                case 1 -> {
                    donor = () -> { expected.set(key, value); return null; };
                    candidate = () -> { actual.set(key, value); return null; };
                }
                case 2 -> {
                    donor = () -> { expected.addIfAbsent(key, value); return null; };
                    candidate = () -> { actual.addIfAbsent(key, value); return null; };
                }
                case 3 -> {
                    donor = () -> expected.remove(key);
                    candidate = () -> actual.remove(key);
                }
                case 4 -> {
                    List<Integer> input = Arrays.asList(value, value);
                    donor = () -> { expected.addAll(key, input); return null; };
                    candidate = () -> { actual.addAll(key, input); return null; };
                }
                case 5 -> {
                    List<Integer> borrowed = random.nextInt(6) == 0
                            ? null : new ArrayList<>(Arrays.asList(value));
                    List<Integer> expectedBorrowed = borrowed == null
                            ? null : new ArrayList<>(borrowed);
                    donor = () -> expected.put(key, expectedBorrowed);
                    candidate = () -> actual.put(key, borrowed);
                }
                case 6 -> {
                    donor = () -> expected.asSingleValueMap().put(key, value);
                    candidate = () -> actual.asSingleValueMap().put(key, value);
                }
                case 7 -> {
                    donor = () -> expected.asSingleValueMap().remove(key);
                    candidate = () -> actual.asSingleValueMap().remove(key);
                }
                case 8 -> {
                    donor = () -> expected.putIfAbsent(key, Arrays.asList(value));
                    candidate = () -> actual.putIfAbsent(key, Arrays.asList(value));
                }
                case 9 -> {
                    donor = () -> { expected.setAll(Map.of(5, 9)); return null; };
                    candidate = () -> { actual.setAll(Map.of(5, 9)); return null; };
                }
                case 10 -> {
                    donor = () -> expected.containsKey(key);
                    candidate = () -> actual.containsKey(key);
                }
                case 11 -> {
                    boolean doClear = step % 47 == 0;
                    donor = () -> { if (doClear) expected.clear(); return null; };
                    candidate = () -> { if (doClear) actual.clear(); return null; };
                }
                case 12 -> {
                    // Never share the same mutable input list across reference and candidate.
                    donor = () -> {
                        Map<Integer, List<Integer>> input = new LinkedHashMap<>();
                        input.put(key, Arrays.asList(value));
                        expected.putAll(input);
                        return null;
                    };
                    candidate = () -> {
                        Map<Integer, List<Integer>> input = new LinkedHashMap<>();
                        input.put(key, Arrays.asList(value));
                        actual.putAll(input);
                        return null;
                    };
                }
                default -> throw new AssertionError();
            }
            assertEquals(result(donor), result(candidate),
                    "operation/exception parity step=" + step + " operation=" + operation);
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
