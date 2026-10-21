// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regressions for callback multiplicity, dispatch and M3 extension boundaries. */
final class M3TreeMapGetOrDefaultTest {
    private static final class Counting implements Comparator<Integer> {
        private final List<String> events = new ArrayList<>();
        private final RuntimeException marker = new IllegalStateException("second lookup");
        private int throwAt;
        @Override public int compare(Integer left, Integer right) {
            events.add(left + ":" + right);
            if (throwAt > 0 && events.size() == throwAt) { throw marker; }
            return Comparator.nullsFirst(Integer::compareTo).compare(left, right);
        }
        void reset() { events.clear(); throwAt = 0; }
    }

    private static NavigableMap<Integer, String> view(NavigableMap<Integer, String> map, int mode) {
        return switch (mode) {
            case 0 -> map;
            case 1 -> map.descendingMap();
            case 2 -> map.subMap(0, true, 20, false);
            case 3 -> map.subMap(0, true, 20, false).descendingMap();
            default -> throw new AssertionError(mode);
        };
    }

    @Test void eagerCallbacksAndResultsMatchJdkForAbsentNullAndNonnullValues() {
        for (int mode = 0; mode < 4; mode++) {
            for (boolean nullValue : List.of(false, true)) {
                for (Integer query : new Integer[] {null, -1, 7, 17, 25}) {
                    Counting expected = new Counting();
                    Counting actual = new Counting();
                    NavigableMap<Integer, String> jdkRoot = new TreeMap<>(expected);
                    M3TreeMap<Integer, String> m3Root = new M3TreeMap<>(actual);
                    String value = nullValue ? null : "seed";
                    jdkRoot.put(7, value); m3Root.put(7, value);
                    var jdk = view(jdkRoot, mode); var m3 = view(m3Root, mode);
                    expected.reset(); actual.reset();
                    assertEquals(jdk.getOrDefault(query, "fallback"), m3.getOrDefault(query, "fallback"));
                    assertEquals(expected.events, actual.events, "mode=" + mode + ",query=" + query);
                    assertEquals(jdkRoot, m3Root);
                    m3Root.verifyInvariants();
                }
            }
        }
    }

    @Test void ordinaryGetDoesNotDispatchThroughTheTwoSearchDefaultOperation() {
        for (int mode = 0; mode < 4; mode++) {
            for (Integer query : new Integer[] {null, 7, 17}) {
                Counting expected = new Counting(); Counting actual = new Counting();
                NavigableMap<Integer, String> jdk = new TreeMap<>(expected);
                NavigableMap<Integer, String> m3 = new M3TreeMap<>(actual);
                jdk.put(7, null); m3.put(7, null);
                jdk = view(jdk, mode); m3 = view(m3, mode);
                expected.reset(); actual.reset();
                assertEquals(jdk.get(query), m3.get(query));
                assertEquals(expected.events, actual.events);
            }
        }
    }

    @Test void exceptionFromTheSecondSearchIsPropagatedWithIdentity() {
        for (boolean descending : List.of(false, true)) {
            for (int query : new int[] {7, 17}) {
                for (boolean original : List.of(false, true)) {
                    Counting comparator = new Counting();
                    NavigableMap<Integer, String> root = original
                            ? new TreeMap<>(comparator) : new M3TreeMap<>(comparator);
                    root.put(7, null);
                    var selected = descending ? root.descendingMap() : root;
                    comparator.reset(); comparator.throwAt = 2;
                    assertSame(comparator.marker, assertThrows(IllegalStateException.class,
                            () -> selected.getOrDefault(query, "fallback")));
                    assertEquals(List.of(query + ":7", query + ":7"), comparator.events);
                    comparator.reset(); assertEquals(1, root.size()); assertNull(root.get(7));
                }
            }
        }
    }

    @Test void nonnullValueDoesNotTriggerASecondComparison() {
        Counting comparator = new Counting();
        var map = new M3TreeMap<Integer, String>(comparator);
        map.put(7, "seed"); comparator.reset(); comparator.throwAt = 2;
        assertEquals("seed", map.getOrDefault(7, "fallback"));
        assertEquals(List.of("7:7"), comparator.events);
    }

    @Test void lazyAndKeyOnlyExtensionsRetainTheirExistingSingleSearchBehavior() {
        Counting comparator = new Counting(); int[] loads = {0};
        M3TreeMap<Integer, String> lazy = new M3TreeMap<>(comparator, (key, progress) -> { loads[0]++; return null; });
        lazy.defer(7); comparator.reset();
        assertNull(lazy.getOrDefault(7, "fallback"));
        assertEquals(List.of("7:7"), comparator.events); assertEquals(1, loads[0]);
        assertEquals(M3LazyValueProvider.LOADED, lazy.valueFlags(7));
        comparator.reset(); assertEquals("fallback", lazy.getOrDefault(17, "fallback"));
        assertEquals(List.of("17:7"), comparator.events); assertEquals(1, loads[0]);
        comparator.reset(); assertNull(lazy.get(7));
        assertEquals(List.of("7:7"), comparator.events); assertEquals(1, loads[0]);
        M3TreeMap<Integer, String> keys = new M3TreeMap<>(comparator, true);
        keys.put(7, null); comparator.reset();
        assertNull(keys.getOrDefault(7, "fallback"));
        assertEquals(List.of("7:7"), comparator.events);
        comparator.reset(); assertEquals("fallback", keys.getOrDefault(17, "fallback"));
        assertEquals(List.of("17:7"), comparator.events);
        lazy.verifyInvariants(); keys.verifyInvariants();
    }
}
