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
 * Finite differential comparison of {@link M3EnumLongMap} against {@code EnumMap<E, Long>}; this
 * is not JDK EnumMap replacement proof. The wide enum crosses the 64-constant presence-word
 * boundary; the narrow one exercises a single word.
 */
final class M3EnumLongMapTest {
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

    @Test
    void wideEnumMatchesJdkAcrossMutationsAndOrdinalOrder() {
        Wide[] keys = Wide.values();
        var actual = new M3EnumLongMap<>(Wide.class);
        var expected = new EnumMap<Wide, Long>(Wide.class);
        Random random = new Random(0x4d33454e554dL);
        assertEquals(70, actual.keyCapacity());
        for (int step = 0; step < 20_000; step++) {
            Wide key = keys[random.nextInt(keys.length)];
            long v = value(random, step);
            switch (random.nextInt(9)) {
                case 0 -> {
                    Long previous = expected.put(key, v);
                    assertEquals(previous == null ? Long.MIN_VALUE + 1 : previous,
                            actual.put(key, v, Long.MIN_VALUE + 1));
                }
                case 1 -> assertEquals(expected.put(key, v) == null, actual.put(key, v));
                case 2 -> {
                    Long previous = expected.remove(key);
                    assertEquals(previous == null ? 7L : previous, actual.remove(key, 7L));
                }
                case 3 -> assertEquals(expected.remove(key) != null, actual.removeKey(key));
                case 4 -> assertEquals(expected.putIfAbsent(key, v) == null, actual.putIfAbsent(key, v));
                case 5 -> {
                    assertEquals(expected.containsKey(key), actual.containsKey(key));
                    assertEquals(expected.getOrDefault(key, 99L).longValue(), actual.getOrDefault(key, 99L));
                    assertEquals(expected.containsValue(v), actual.containsValue(v));
                }
                case 6 -> {
                    if (expected.size() > 60) { expected.clear(); actual.clear(); }
                }
                default -> {
                    if (expected.containsKey(key)) {
                        assertEquals(expected.get(key).longValue(), actual.getAsLong(key));
                    } else {
                        assertThrows(NoSuchElementException.class, () -> actual.getAsLong(key));
                    }
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
            if (step % 97 == 0) {
                assertArrayEquals(expected.values().stream().mapToLong(Long::longValue).toArray(), actual.values());
                assertArrayEquals(expected.keySet().toArray(new Wide[0]), actual.keys());
                List<Wide> seenKeys = new ArrayList<>();
                List<Long> seenValues = new ArrayList<>();
                actual.forEach((k, value) -> { seenKeys.add(k); seenValues.add(value); });
                assertEquals(new ArrayList<>(expected.keySet()), seenKeys);
                assertEquals(new ArrayList<>(expected.values()), seenValues);
                List<Integer> ordinals = new ArrayList<>();
                actual.forEachOrdinal((ordinal, value) -> ordinals.add(ordinal));
                assertEquals(expected.keySet().stream().map(Enum::ordinal).toList(), ordinals);
            }
        }
    }

    @Test
    void narrowEnumAndEdgeContracts() {
        var map = new M3EnumLongMap<>(Narrow.class);
        assertEquals(3, map.keyCapacity());
        assertEquals(Narrow.class, map.keyType());
        assertEquals(4L * Long.BYTES, map.payloadBytes());
        assertFalse(map.containsKey(null));
        assertEquals(5L, map.getOrDefault(null, 5L));
        assertEquals(5L, map.remove(null, 5L));
        assertFalse(map.removeKey(null));
        assertThrows(NullPointerException.class, () -> map.put(null, 1L));
        assertThrows(NullPointerException.class, () -> map.put(null, 1L, 0L));
        assertThrows(NullPointerException.class, () -> map.getAsLong(null));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
        assertThrows(NullPointerException.class, () -> map.forEachOrdinal(null));
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
        assertFalse(map.containsValue(Long.MAX_VALUE));
        map.clear();
        assertTrue(map.isEmpty());
        assertArrayEquals(new Narrow[0], map.keys());
        map.clear();
        assertEquals(0, map.size());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void foreignEnumKeysAreRejectedLikeJdk() {
        M3EnumLongMap raw = new M3EnumLongMap<>(Narrow.class);
        EnumMap rawJdk = new EnumMap<>(Narrow.class);
        assertThrows(ClassCastException.class, () -> rawJdk.put(Other.X, 1L));
        assertThrows(ClassCastException.class, () -> raw.put(Other.X, 1L));
        assertThrows(ClassCastException.class, () -> raw.put(Other.X, 1L, 0L));
        assertThrows(ClassCastException.class, () -> raw.putIfAbsent(Other.X, 1L));
        assertThrows(ClassCastException.class, () -> raw.getAsLong(Other.X));
        assertFalse(rawJdk.containsKey(Other.X));
        assertFalse(raw.containsKey(Other.X));
        assertEquals(3L, raw.getOrDefault(Other.X, 3L));
        assertEquals(3L, raw.remove(Other.X, 3L));
        assertFalse(raw.removeKey(Other.X));
        assertThrows(IllegalArgumentException.class, () -> new M3EnumLongMap(String.class));
        assertThrows(NullPointerException.class, () -> new M3EnumLongMap(null));
    }

    @Test
    void retainsOnlyPrimitiveLanesAndTheSharedUniverse() {
        var fields = Arrays.stream(M3EnumLongMap.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("Class", "Enum[]", "int", "long[]", "long[]"), fields);
        var map = new M3EnumLongMap<>(Wide.class);
        for (Wide key : Wide.values()) {
            map.put(key, key.ordinal());
        }
        assertEquals(70, map.size());
        assertEquals(70L * Long.BYTES + 2L * Long.BYTES, map.payloadBytes());
        Map<Wide, Long> jdk = new EnumMap<>(Wide.class);
        map.forEach(jdk::put);
        assertEquals(70, jdk.size());
        assertEquals(69L, jdk.get(Wide.C69));
    }
}
