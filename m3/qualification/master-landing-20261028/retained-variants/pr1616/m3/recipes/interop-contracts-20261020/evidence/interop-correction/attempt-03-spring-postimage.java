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
import java.util.function.Supplier;
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

    @Test void realSpringSixTwoDifferentialSeeded() {
        MultiValueMap<Integer, Integer> actual = new M3SpringArrayMultiValueMap<>();
        MultiValueMap<Integer, Integer> expected = new MultiValueMapAdapter<>(new LinkedHashMap<>());
        Random random = new Random(0x20261017L);
        for (int step = 0; step < 4500; step++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(32);
            Integer value = random.nextInt(8) == 0 ? null : random.nextInt(50);
            int operation = random.nextInt(13);
            List<Integer> actualInput = null;
            List<Integer> expectedInput = null;
            if (operation == 5 && random.nextInt(6) != 0) {
                actualInput = new ArrayList<>(Arrays.asList(value));
                expectedInput = new ArrayList<>(actualInput);
            }
            final List<Integer> a = actualInput, b = expectedInput;
            final boolean clear = step % 47 == 0;
            // Borrowed fixed-size lists deliberately refuse structural writes.
            // Run both providers even when one throws, then compare state below.
            var expectedOutcome = outcome(() -> apply(expected, operation, key, value, b, clear));
            var actualOutcome = outcome(() -> apply(actual, operation, key, value, a, clear));
            assertEquals(expectedOutcome, actualOutcome, "operation step " + step);
            assertEquals(expected, actual, "step " + step);
            assertEquals(expected.size(), actual.size(), "size step " + step);
            assertEquals(expected.getFirst(key), actual.getFirst(key), "first step " + step);
            assertEquals(expected.toSingleValueMap(), actual.toSingleValueMap(), "copy step " + step);
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()), "order step " + step);
            assertEquals(expected.asSingleValueMap().get(key),
                    actual.asSingleValueMap().get(key), "view step " + step);
        }
    }

    private record Outcome(Object value, Class<? extends RuntimeException> failure) { }

    private static Outcome outcome(Supplier<Object> operation) {
        try {
            return new Outcome(operation.get(), null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass());
        }
    }

    private static Object apply(MultiValueMap<Integer, Integer> map, int operation,
            Integer key, Integer value, List<Integer> input, boolean clear) {
        return switch (operation) {
            case 0 -> { map.add(key, value); yield null; }
            case 1 -> { map.set(key, value); yield null; }
            case 2 -> { map.addIfAbsent(key, value); yield null; }
            case 3 -> map.remove(key);
            case 4 -> { map.addAll(key, Arrays.asList(value, value)); yield null; }
            case 5 -> map.put(key, input);
            case 6 -> map.asSingleValueMap().put(key, value);
            case 7 -> map.asSingleValueMap().remove(key);
            case 8 -> map.putIfAbsent(key, Arrays.asList(value));
            case 9 -> { map.setAll(Map.of(5, 9)); yield null; }
            case 10 -> map.containsKey(key);
            case 11 -> { if (clear) map.clear(); yield null; }
            case 12 -> {
                Map<Integer, List<Integer>> copy = new LinkedHashMap<>();
                copy.put(key, Arrays.asList(value));
                map.putAll(copy);
                yield null;
            }
            default -> throw new AssertionError(operation);
        };
    }
}
