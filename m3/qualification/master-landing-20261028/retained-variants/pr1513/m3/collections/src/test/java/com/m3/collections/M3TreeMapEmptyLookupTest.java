// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class M3TreeMapEmptyLookupTest {
    private static final List<String> OPERATIONS = List.of("get", "getOrDefault", "containsKey", "remove",
            "keys.contains", "keys.remove", "entries.contains", "entries.remove");

    private static final class Probe implements Comparable<Object> {
        private final RuntimeException failure;
        private int comparisons;
        Probe(RuntimeException failure) { this.failure = failure; }
        @Override public int compareTo(Object other) {
            this.comparisons++;
            if (this.failure != null) { throw this.failure; }
            return 0;
        }
    }

    private static NavigableMap<Object, String> emptyMap(int state, boolean descending) {
        NavigableMap<Object, String> map = new M3TreeMap<>();
        if (state != 0) { map.put(7, "seed"); }
        if (state == 1) { map.clear(); }
        if (state == 2) { map.remove(7); }
        return descending ? map.descendingMap() : map;
    }

    private static Object lookup(Map<Object, String> map, String operation, Object key) {
        return switch (operation) {
            case "get" -> map.get(key);
            case "getOrDefault" -> map.getOrDefault(key, "fallback");
            case "containsKey" -> map.containsKey(key);
            case "remove" -> map.remove(key);
            case "keys.contains" -> map.keySet().contains(key);
            case "keys.remove" -> map.keySet().remove(key);
            case "entries.contains" -> map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(key, "seed"));
            case "entries.remove" -> map.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(key, "seed"));
            default -> throw new AssertionError(operation);
        };
    }

    private static Object absent(String operation) {
        return switch (operation) {
            case "get", "remove" -> null;
            case "getOrDefault" -> "fallback";
            default -> false;
        };
    }

    @Test void emptyNaturalLookupsDoNotCompareEvenWhenComparisonThrows() {
        for (int state = 0; state < 3; state++) {
            for (boolean descending : List.of(false, true)) {
                for (boolean throwing : List.of(false, true)) {
                    for (String operation : OPERATIONS) {
                        NavigableMap<Object, String> map = emptyMap(state, descending);
                        Probe key = new Probe(throwing ? new IllegalStateException("unexpected compare") : null);
                        assertEquals(absent(operation), lookup(map, operation, key), operation);
                        assertEquals(0, key.comparisons, operation);
                        assertEquals(0, map.size(), operation);
                    }
                }
            }
        }
    }

    @Test void emptyNaturalLookupsStillRejectNullAndNonComparable() {
        for (int state = 0; state < 3; state++) {
            for (boolean descending : List.of(false, true)) {
                for (String operation : OPERATIONS) {
                    NavigableMap<Object, String> map = emptyMap(state, descending);
                    assertThrows(NullPointerException.class, () -> lookup(map, operation, null), operation);
                    assertThrows(ClassCastException.class, () -> lookup(map, operation, new Object()), operation);
                    assertEquals(0, map.size(), operation);
                }
            }
        }
    }

    @Test void populatedNaturalLookupStillInvokesComparison() {
        NavigableMap<Object, String> map = new M3TreeMap<>();
        map.put(7, "seed");
        RuntimeException marker = new IllegalStateException("populated-lookup-marker");
        Probe key = new Probe(marker);
        assertSame(marker, assertThrows(IllegalStateException.class, () -> map.get(key)));
        assertEquals(1, key.comparisons);
        assertEquals("seed", map.get(7));
    }

    @Test void emptyComparatorLookupDoesNotAdmitThroughNaturalOrdering() {
        Comparator<Object> comparator = (left, right) -> { throw new AssertionError("unexpected comparator"); };
        for (boolean descending : List.of(false, true)) {
            for (String operation : OPERATIONS) {
                NavigableMap<Object, String> root = new M3TreeMap<>(comparator);
                Map<Object, String> selected = descending ? root.descendingMap() : root;
                assertEquals(absent(operation), lookup(selected, operation, new Object()), operation);
            }
        }
    }
}
