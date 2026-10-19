/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.m3.collections.M3FrozenLongMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * LS2 provider differential for {@link M3FrozenLongMap} against Guava, each oracle on its own: {@code ImmutableMap}
 * (membership, values, duplicate-key refusal, hash, immutability; insertion order NOT claimed) and
 * {@code ImmutableSortedMap} (ascending key order, key/value ordinals, rendering). At least 40,000 operations.
 */
final class M3FrozenLongMapProviderTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
    private static final Long MISSING = 0x5EED_0000_0000_0002L;

    @Test
    void differentialAgainstGuavaImmutableMapAndImmutableSortedMap() {
        SplittableRandom random = new SplittableRandom(0x6A7A_2000L);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            int length = round % 11 == 0 ? 0 : random.nextInt(1, 36);
            long[] keys = new long[length], values = new long[length];
            boolean unique = random.nextInt(5) != 0;
            for (int i = 0; i < length; i++) {
                keys[i] = unique ? i * 7L - 100 : random.nextLong(-15, 16);
                values[i] = random.nextInt(3) == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-40, 41);
            }
            ImmutableMap.Builder<Long, Long> hashed = ImmutableMap.builder();
            ImmutableSortedMap.Builder<Long, Long> sorted = ImmutableSortedMap.naturalOrder();
            for (int i = 0; i < length; i++) { hashed.put(keys[i], values[i]); sorted.put(keys[i], values[i]); }
            ImmutableMap<Long, Long> guava;
            try {
                guava = hashed.buildOrThrow();
            } catch (IllegalArgumentException duplicate) {
                assertThrows(IllegalArgumentException.class, sorted::buildOrThrow);
                assertThrows(IllegalArgumentException.class, () -> M3FrozenLongMap.copyOf(keys, values));
                operations += 3;
                continue;
            }
            ImmutableSortedMap<Long, Long> order = sorted.buildOrThrow();
            M3FrozenLongMap map = M3FrozenLongMap.copyOf(keys, values);
            List<Long> orderedKeys = order.keySet().asList();
            List<Long> orderedValues = order.values().asList();
            assertEquals(guava.size(), map.size());
            assertEquals(guava.hashCode(), map.hashCode());
            assertEquals(order.hashCode(), map.hashCode());
            assertEquals(order.toString(), map.toString());
            assertArrayEquals(orderedKeys.stream().mapToLong(Long::longValue).toArray(), map.keysToArray());
            operations += 5;
            for (int step = 0; step < 30; step++) {
                long probe = random.nextInt(3) == 0 && length > 0 ? keys[random.nextInt(length)] : random.nextLong(-120, 121);
                assertEquals(guava.containsKey(probe), map.containsKey(probe));
                assertEquals((long) guava.getOrDefault(probe, MISSING), map.getOrDefault(probe, MISSING));
                assertEquals(guava.containsValue(probe), map.containsValue(probe));
                int ordinal = random.nextInt(-1, orderedKeys.size() + 1);
                if (ordinal >= 0 && ordinal < orderedKeys.size()) {
                    assertEquals((long) orderedKeys.get(ordinal), map.keyAt(ordinal));
                    assertEquals((long) orderedValues.get(ordinal), map.valueAt(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> orderedKeys.get(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(bad));
                }
                operations += 4;
            }
            Map<Long, Long> asMap = guava;
            long value = random.nextLong(-5, 6);
            assertThrows(UnsupportedOperationException.class, () -> asMap.put(value, value));
            assertThrows(UnsupportedOperationException.class, () -> map.put(value, value));
            assertThrows(UnsupportedOperationException.class, () -> asMap.remove(value));
            assertThrows(UnsupportedOperationException.class, () -> map.remove(value));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }
}
