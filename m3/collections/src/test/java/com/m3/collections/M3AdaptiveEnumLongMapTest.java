/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3AdaptiveEnumLongMap} against {@code EnumMap<E,
 * Long>} across both backings; this is not JDK EnumMap replacement proof. The wide enum has a
 * sparse regime below the switch size and crosses the 64-constant presence-word boundary once
 * dense; the narrow enum is dense from the first mapping.
 */
final class M3AdaptiveEnumLongMapTest {
    enum Narrow { A, B, C }

    enum Wide {
        C00, C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C11, C12, C13, C14, C15, C16, C17,
        C18, C19, C20, C21, C22, C23, C24, C25, C26, C27, C28, C29, C30, C31, C32, C33, C34, C35,
        C36, C37, C38, C39, C40, C41, C42, C43, C44, C45, C46, C47, C48, C49, C50, C51, C52, C53,
        C54, C55, C56, C57, C58, C59, C60, C61, C62, C63, C64, C65, C66, C67, C68, C69
    }

    enum Other { X }

    private static long value(Random random, int step) {
        return switch (step % 11) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(61) - 30L;
        };
    }

    /** The documented rule, evaluated independently of the binary search in the owner. */
    private static int expectedSwitchSize(int domainSize) {
        long direct = (long) domainSize * Long.BYTES + (long) Math.max(1, (domainSize + 63) >>> 6) * Long.BYTES;
        for (int n = 0; n <= domainSize; n++) {
            if (direct <= M3IntLongHashMap.payloadBytesFor(Math.max(1, n))) {
                return n;
            }
        }
        return domainSize;
    }

    @Test
    void wideEnumMatchesJdkAcrossMutationsAndModes() {
        Wide[] keys = Wide.values();
        var actual = new M3AdaptiveEnumLongMap<>(Wide.class);
        var expected = new EnumMap<Wide, Long>(Wide.class);
        Random random = new Random(0x4d33414441505441L);
        int switchSize = actual.directSwitchSize();
        assertEquals(expectedSwitchSize(70), switchSize);
        assertTrue(switchSize > 1 && switchSize < 70, "switch=" + switchSize);
        assertFalse(actual.isDirect());
        for (int step = 0; step < 20_000; step++) {
            Wide key = keys[random.nextInt(keys.length)];
            long v = value(random, step);
            switch (random.nextInt(10)) {
                case 0 -> {
                    Long previous = expected.put(key, v);
                    assertEquals(previous == null ? Long.MIN_VALUE + 1 : previous,
                            actual.put(key, v, Long.MIN_VALUE + 1));
                }
                case 1 -> assertEquals(expected.put(key, v) == null, actual.put(key, v));
                case 2 -> assertEquals(expected.putIfAbsent(key, v) == null, actual.putIfAbsent(key, v));
                case 3 -> {
                    Long removed = expected.remove(key);
                    assertEquals(removed == null ? -7L : removed, actual.remove(key, -7L));
                }
                case 4 -> assertEquals(expected.remove(key) != null, actual.removeKey(key));
                case 5 -> {
                    assertEquals(expected.containsKey(key), actual.containsKey(key));
                    assertEquals(expected.getOrDefault(key, 42L), actual.getOrDefault(key, 42L));
                    assertEquals(expected.containsValue(v), actual.containsValue(v));
                    if (expected.containsKey(key)) {
                        assertEquals(expected.get(key), actual.getAsLong(key));
                    } else {
                        assertThrows(NoSuchElementException.class, () -> actual.getAsLong(key));
                    }
                }
                case 6 -> {
                    actual.compact();
                    assertEquals(expected.size() >= switchSize, actual.isDirect());
                    assertEquals(actual.isDirect()
                                    ? actual.directPayloadBytes()
                                    : M3IntLongHashMap.payloadBytesFor(actual.size()),
                            actual.payloadBytes());
                }
                case 7 -> {
                    if (expected.size() > 60 && random.nextInt(4) == 0) {
                        expected.clear();
                        actual.clear();
                    }
                }
                default -> {
                    assertArrayEquals(expected.keySet().toArray(new Wide[0]), actual.keys());
                    assertArrayEquals(expected.values().stream().mapToLong(Long::longValue).toArray(),
                            actual.values());
                    List<Wide> visited = new ArrayList<>();
                    actual.forEach((k, val) -> {
                        visited.add(k);
                        assertEquals(expected.get(k), val);
                    });
                    assertEquals(new ArrayList<>(expected.keySet()), visited);
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
            if (!actual.isDirect()) {
                assertTrue(actual.size() < switchSize, "sparse at size " + actual.size());
                assertTrue(actual.payloadBytes() >= M3IntLongHashMap.payloadBytesFor(actual.size()));
            } else {
                assertEquals(actual.directPayloadBytes(), actual.payloadBytes());
            }
        }
    }

    @Test
    void modeTransitionsAndOrdinalOrderInSparseMode() {
        Wide[] keys = Wide.values();
        var map = new M3AdaptiveEnumLongMap<>(Wide.class);
        int switchSize = map.directSwitchSize();
        assertEquals(21, switchSize);
        assertFalse(map.isDirect());
        assertEquals(M3IntLongHashMap.payloadBytesFor(0), map.payloadBytes());
        // Insert in reverse ordinal order; traversal must still be ordinal order.
        for (int i = 0; i < switchSize - 1; i++) {
            assertTrue(map.put(keys[69 - i], i));
            assertFalse(map.isDirect(), "size " + map.size());
        }
        assertEquals(switchSize - 1, map.size());
        Wide[] ordered = map.keys();
        assertEquals(switchSize - 1, ordered.length);
        for (int i = 1; i < ordered.length; i++) {
            assertTrue(ordered[i - 1].ordinal() < ordered[i].ordinal());
        }
        assertEquals(keys[69 - (switchSize - 2)], ordered[0]);
        assertEquals(switchSize - 2, map.values()[0]);
        List<Integer> ordinals = new ArrayList<>();
        map.forEachOrdinal((ordinal, v) -> ordinals.add(ordinal));
        assertEquals(ordinals.stream().sorted().toList(), ordinals);
        // Overwriting never migrates; the next distinct key flips to dense and keeps every mapping.
        assertFalse(map.put(keys[69], 100L));
        assertFalse(map.isDirect());
        assertTrue(map.put(keys[0], -1L));
        assertTrue(map.isDirect());
        assertEquals(switchSize, map.size());
        assertEquals(map.directPayloadBytes(), map.payloadBytes());
        assertEquals(-1L, map.getAsLong(keys[0]));
        assertEquals(100L, map.getAsLong(keys[69]));
        for (int i = 1; i < switchSize - 1; i++) {
            assertEquals(i, map.getAsLong(keys[69 - i]));
        }
        // Sizing at construction follows the same rule.
        assertTrue(new M3AdaptiveEnumLongMap<>(Wide.class, switchSize).isDirect());
        var below = new M3AdaptiveEnumLongMap<>(Wide.class, switchSize - 1);
        assertFalse(below.isDirect());
        assertEquals(M3IntLongHashMap.payloadBytesFor(switchSize - 1), below.payloadBytes());
        assertEquals(0, new M3AdaptiveEnumLongMap<>(Narrow.class).directSwitchSize());
        // Removal never migrates implicitly; compact re-chooses the backing.
        for (int i = 0; i < switchSize - 1; i++) {
            assertTrue(map.removeKey(keys[69 - i]));
        }
        assertEquals(1, map.size());
        assertTrue(map.isDirect());
        map.compact();
        assertFalse(map.isDirect());
        assertEquals(1, map.size());
        assertEquals(-1L, map.getAsLong(keys[0]));
        assertEquals(M3IntLongHashMap.payloadBytesFor(1), map.payloadBytes());
        // A sparse table that grew and emptied keeps its capacity until compact shrinks it.
        for (int i = 1; i < switchSize - 1; i++) {
            assertTrue(map.put(keys[i], i));
        }
        assertFalse(map.isDirect());
        long grown = map.payloadBytes();
        assertTrue(grown > M3IntLongHashMap.payloadBytesFor(1));
        for (int i = 1; i < switchSize - 1; i++) {
            assertTrue(map.removeKey(keys[i]));
        }
        assertEquals(grown, map.payloadBytes());
        map.compact();
        assertFalse(map.isDirect());
        assertEquals(M3IntLongHashMap.payloadBytesFor(1), map.payloadBytes());
        assertEquals(1, map.size());
        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.isDirect());
        map.clear();
        map.put(keys[65], 65L);
        map.put(keys[2], 2L);
        map.put(keys[64], 64L);
        List<Integer> visited = new ArrayList<>();
        map.forEachOrdinal((ordinal, v) -> {
            visited.add(ordinal);
            assertEquals(ordinal, v);
        });
        assertEquals(List.of(2, 64, 65), visited);
        assertArrayEquals(new Wide[]{Wide.C02, Wide.C64, Wide.C65}, map.keys());
        assertArrayEquals(new long[]{2L, 64L, 65L}, map.values());
    }

    @Test
    void narrowEnumIsDenseFromTheStartAndEdgeContracts() {
        var map = new M3AdaptiveEnumLongMap<>(Narrow.class);
        assertTrue(map.isDirect());
        assertEquals(3, map.keyCapacity());
        assertEquals(Narrow.class, map.keyType());
        assertEquals(4L * Long.BYTES, map.directPayloadBytes());
        assertEquals(map.directPayloadBytes(), map.payloadBytes());
        map.compact();
        assertTrue(map.isDirect());
        assertFalse(map.containsKey(null));
        assertEquals(5L, map.getOrDefault(null, 5L));
        assertEquals(5L, map.remove(null, 5L));
        assertFalse(map.removeKey(null));
        assertThrows(NullPointerException.class, () -> map.put(null, 1L));
        assertThrows(NullPointerException.class, () -> map.put(null, 1L, 0L));
        assertThrows(NullPointerException.class, () -> map.putIfAbsent(null, 1L));
        assertThrows(NullPointerException.class, () -> map.getAsLong(null));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        assertThrows(NullPointerException.class, () -> map.forEachOrdinal(null));
        assertThrows(NoSuchElementException.class, () -> map.getAsLong(Narrow.B));
        assertTrue(map.put(Narrow.C, Long.MIN_VALUE));
        assertFalse(map.put(Narrow.C, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, map.getAsLong(Narrow.C));
        assertTrue(map.putIfAbsent(Narrow.A, 0L));
        assertFalse(map.putIfAbsent(Narrow.A, 9L));
        assertEquals(0L, map.getAsLong(Narrow.A));
        assertTrue(map.containsValue(0L));
        assertFalse(map.containsValue(9L));
        assertArrayEquals(new Narrow[]{Narrow.A, Narrow.C}, map.keys());
        assertArrayEquals(new long[]{0L, Long.MAX_VALUE}, map.values());
        assertEquals(2, map.size());
        assertEquals(Long.MAX_VALUE, map.remove(Narrow.C, -1L));
        assertEquals(-1L, map.remove(Narrow.C, -1L));
        map.clear();
        assertTrue(map.isEmpty());
        assertTrue(map.isDirect());
        assertThrows(IllegalArgumentException.class, () -> new M3AdaptiveEnumLongMap<>(Narrow.class, 4));
        assertThrows(IllegalArgumentException.class, () -> new M3AdaptiveEnumLongMap<>(Narrow.class, -1));
        assertThrows(NullPointerException.class, () -> new M3AdaptiveEnumLongMap<>(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void foreignEnumKeysAreRejectedLikeJdkInBothModes() {
        EnumMap rawJdk = new EnumMap<>(Narrow.class);
        assertThrows(ClassCastException.class, () -> rawJdk.put(Other.X, 1L));
        assertFalse(rawJdk.containsKey(Other.X));
        for (M3AdaptiveEnumLongMap raw : List.of(
                new M3AdaptiveEnumLongMap<>(Narrow.class), new M3AdaptiveEnumLongMap<>(Wide.class))) {
            assertThrows(ClassCastException.class, () -> raw.put(Other.X, 1L));
            assertThrows(ClassCastException.class, () -> raw.put(Other.X, 1L, 0L));
            assertThrows(ClassCastException.class, () -> raw.putIfAbsent(Other.X, 1L));
            assertThrows(ClassCastException.class, () -> raw.getAsLong(Other.X));
            assertFalse(raw.containsKey(Other.X));
            assertEquals(3L, raw.getOrDefault(Other.X, 3L));
            assertEquals(3L, raw.remove(Other.X, 3L));
            assertFalse(raw.removeKey(Other.X));
            assertTrue(raw.isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> new M3AdaptiveEnumLongMap(String.class));
    }

    @Test
    void retainsOnlyTheActiveBackingAndNoKeyReferences() {
        var fields = Arrays.stream(M3AdaptiveEnumLongMap.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("Class", "M3EnumLongMap", "M3IntLongHashMap", "int"), fields);
        var map = new M3AdaptiveEnumLongMap<>(Wide.class);
        for (Wide key : Wide.values()) {
            map.put(key, key.ordinal());
        }
        assertTrue(map.isDirect());
        assertEquals(70, map.size());
        assertEquals(70L * Long.BYTES + 2L * Long.BYTES, map.payloadBytes());
        Map<Wide, Long> jdk = new EnumMap<>(Wide.class);
        map.forEach(jdk::put);
        assertEquals(70, jdk.size());
        assertEquals(69L, jdk.get(Wide.C69));
    }
}
