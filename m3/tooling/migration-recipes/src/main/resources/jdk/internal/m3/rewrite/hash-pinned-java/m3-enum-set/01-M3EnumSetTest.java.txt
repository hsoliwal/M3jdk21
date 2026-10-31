/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3EnumSet} against {@link EnumSet}; this is not JDK
 * EnumSet replacement proof. The wide enum crosses the 64-constant word boundary (the JDK switches
 * to its jumbo representation there), the narrow one stays in a single word.
 */
final class M3EnumSetTest {
    enum Narrow { A, B, C }

    enum Wide {
        C00, C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C11, C12, C13, C14, C15, C16, C17,
        C18, C19, C20, C21, C22, C23, C24, C25, C26, C27, C28, C29, C30, C31, C32, C33, C34, C35,
        C36, C37, C38, C39, C40, C41, C42, C43, C44, C45, C46, C47, C48, C49, C50, C51, C52, C53,
        C54, C55, C56, C57, C58, C59, C60, C61, C62, C63, C64, C65, C66, C67, C68, C69
    }

    enum Other { X }

    private static <E extends Enum<E>> void assertSame(EnumSet<E> expected, M3EnumSet<E> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        assertArrayEquals(expected.toArray(), actual.toArray());
        List<E> seen = new ArrayList<>();
        actual.forEach(seen::add);
        assertEquals(new ArrayList<>(expected), seen);
        for (E constant : actual.elementType().getEnumConstants()) {
            assertEquals(expected.contains(constant), actual.contains(constant));
        }
    }

    private static <E extends Enum<E>> M3EnumSet<E> mirror(EnumSet<E> source, Class<E> type) {
        M3EnumSet<E> set = M3EnumSet.noneOf(type);
        for (E element : source) {
            set.add(element);
        }
        return set;
    }

    @Test
    void wideEnumMatchesJdkAcrossMutationsAndBulkOperations() {
        Wide[] keys = Wide.values();
        Random random = new Random(0x4d33454e5345L);
        EnumSet<Wide> expected = EnumSet.noneOf(Wide.class);
        M3EnumSet<Wide> actual = M3EnumSet.noneOf(Wide.class);
        for (int step = 0; step < 20_000; step++) {
            Wide key = keys[random.nextInt(keys.length)];
            switch (random.nextInt(10)) {
                case 0, 1 -> assertEquals(expected.add(key), actual.add(key));
                case 2 -> assertEquals(expected.remove(key), actual.remove(key));
                case 3 -> assertEquals(expected.contains(key), actual.contains(key));
                case 4 -> {
                    Wide to = keys[random.nextInt(keys.length)];
                    Wide from = key.compareTo(to) <= 0 ? key : to;
                    Wide upper = key.compareTo(to) <= 0 ? to : key;
                    EnumSet<Wide> jdk = EnumSet.range(from, upper);
                    M3EnumSet<Wide> m3 = M3EnumSet.range(from, upper);
                    assertSame(jdk, m3);
                    assertEquals(expected.addAll(jdk), actual.addAll(m3));
                }
                case 5 -> {
                    EnumSet<Wide> jdk = EnumSet.of(key, keys[(key.ordinal() + 17) % keys.length]);
                    M3EnumSet<Wide> m3 = M3EnumSet.of(key, keys[(key.ordinal() + 17) % keys.length]);
                    assertEquals(expected.removeAll(jdk), actual.removeAll(m3));
                }
                case 6 -> {
                    EnumSet<Wide> jdk = EnumSet.complementOf(EnumSet.of(key));
                    M3EnumSet<Wide> m3 = M3EnumSet.complementOf(M3EnumSet.of(key));
                    assertSame(jdk, m3);
                    assertEquals(expected.retainAll(jdk), actual.retainAll(m3));
                }
                case 7 -> {
                    EnumSet<Wide> jdk = EnumSet.copyOf(expected);
                    M3EnumSet<Wide> m3 = M3EnumSet.copyOf(actual);
                    assertEquals(expected.containsAll(jdk), actual.containsAll(m3));
                    assertTrue(actual.equals(m3));
                    assertEquals(actual.hashCode(), m3.hashCode());
                    assertEquals(expected.containsAll(EnumSet.allOf(Wide.class)), actual.containsAll(M3EnumSet.allOf(Wide.class)));
                }
                case 8 -> {
                    if (expected.size() > 60) { expected.clear(); actual.clear(); }
                }
                default -> {
                    expected = EnumSet.complementOf(expected);
                    actual.complement();
                }
            }
            assertSame(expected, actual);
        }
        assertSame(EnumSet.allOf(Wide.class), M3EnumSet.allOf(Wide.class));
        assertEquals(70, M3EnumSet.allOf(Wide.class).size());
        assertEquals(2L * Long.BYTES, actual.payloadBytes());
        long[] words = M3EnumSet.allOf(Wide.class).toLongArray();
        assertEquals(-1L, words[0]);
        assertEquals((1L << 6) - 1L, words[1]);
    }

    @Test
    void narrowEnumEdgeContractsMatchJdk() {
        M3EnumSet<Narrow> set = M3EnumSet.noneOf(Narrow.class);
        EnumSet<Narrow> jdk = EnumSet.noneOf(Narrow.class);
        assertEquals(Narrow.class, set.elementType());
        assertEquals(Long.BYTES, set.payloadBytes());
        assertThrows(NullPointerException.class, () -> set.add(null));
        assertThrows(NullPointerException.class, () -> jdk.add(null));
        assertFalse(set.contains(null));
        assertFalse(set.remove(null));
        assertFalse(set.contains("A"));
        assertFalse(set.remove(Other.X));
        assertFalse(set.contains(Other.X));
        assertTrue(set.add(Narrow.C));
        assertFalse(set.add(Narrow.C));
        assertTrue(set.add(Narrow.A));
        assertArrayEquals(new Narrow[]{Narrow.A, Narrow.C}, set.toArray());
        assertEquals("[A, C]", set.toString());
        M3EnumSet<Narrow> complement = M3EnumSet.complementOf(set);
        assertArrayEquals(new Narrow[]{Narrow.B}, complement.toArray());
        assertTrue(set.equals(mirror(EnumSet.of(Narrow.A, Narrow.C), Narrow.class)));
        assertFalse(set.equals(complement));
        assertFalse(set.equals(M3EnumSet.noneOf(Wide.class)));
        assertTrue(set.remove(Narrow.A));
        assertFalse(set.remove(Narrow.A));
        assertEquals(1, set.size());
        set.clear();
        assertTrue(set.isEmpty());
        assertSame(EnumSet.range(Narrow.A, Narrow.B), M3EnumSet.range(Narrow.A, Narrow.B));
        assertSame(EnumSet.range(Narrow.B, Narrow.B), M3EnumSet.range(Narrow.B, Narrow.B));
        assertThrows(IllegalArgumentException.class, () -> M3EnumSet.range(Narrow.C, Narrow.A));
        assertThrows(IllegalArgumentException.class, () -> EnumSet.range(Narrow.C, Narrow.A));
        assertThrows(NullPointerException.class, () -> M3EnumSet.range(null, Narrow.A));
        assertThrows(NullPointerException.class, () -> M3EnumSet.of(null));
        assertThrows(NullPointerException.class, () -> set.forEach(null));
        M3EnumSet<Narrow> all = M3EnumSet.allOf(Narrow.class);
        assertEquals(3, all.size());
        all.complement();
        assertTrue(all.isEmpty());
        all.complement();
        assertEquals(3, all.size());
        assertEquals(7L, all.toLongArray()[0]);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void foreignEnumConstantsAreRejectedLikeJdk() {
        M3EnumSet raw = M3EnumSet.noneOf(Narrow.class);
        EnumSet rawJdk = EnumSet.noneOf(Narrow.class);
        assertThrows(ClassCastException.class, () -> rawJdk.add(Other.X));
        assertThrows(ClassCastException.class, () -> raw.add(Other.X));
        assertThrows(ClassCastException.class, () -> raw.addAll(M3EnumSet.noneOf(Other.class)));
        assertThrows(ClassCastException.class, () -> raw.retainAll(M3EnumSet.allOf(Other.class)));
        assertThrows(NullPointerException.class, () -> raw.addAll(null));
        // A non-enum class cannot reach noneOf through the typed API (E extends Enum<E>).
        assertThrows(NullPointerException.class, () -> M3EnumSet.noneOf(null));
    }

    @Test
    void retainsOnlyPresenceWordsAndTheSharedUniverse() {
        List<String> fields = Arrays.stream(M3EnumSet.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("Class", "Enum[]", "int", "long[]"), fields);
    }
}
