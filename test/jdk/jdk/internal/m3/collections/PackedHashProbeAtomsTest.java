// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.packed.PackedLongLongMap;
import jdk.internal.m3.collections.packed.PackedLongLongQuadraticHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongRobinHoodHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongSwissHashMap;
import jdk.internal.m3.collections.packed.PackedLongQuadraticHashSet;
import jdk.internal.m3.collections.packed.PackedLongRobinHoodHashSet;
import jdk.internal.m3.collections.packed.PackedLongSwissHashSet;
import jdk.internal.m3.collections.packed.PackedMutableLongSet;

final class PackedHashProbeAtomsTest {
    @Test
    void quadraticMapAndSetMatchJdkReference() {
        exerciseMap(PackedLongLongQuadraticHashMap::new);
        exerciseSet(PackedLongQuadraticHashSet::new);
    }

    @Test
    void robinHoodMapAndSetMatchJdkReference() {
        exerciseMap(PackedLongLongRobinHoodHashMap::new);
        exerciseSet(PackedLongRobinHoodHashSet::new);
    }

    @Test
    void swissMapAndSetMatchJdkReference() {
        exerciseMap(PackedLongLongSwissHashMap::new);
        exerciseSet(PackedLongSwissHashSet::new);
    }

    private static void exerciseMap(IntFunction<PackedLongLongMap> factory) {
        PackedLongLongMap actual = factory.apply(4);
        HashMap<Long, Long> expected = new HashMap<>();
        Random random = new Random(0x48415348L);
        long[] edges = {0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L};

        for (int round = 0; round < 20_000; round++) {
            long key = round < edges.length ? edges[round] : random.nextInt(1024) - 512L;
            long value = random.nextLong();
            switch (round & 3) {
                case 0 -> {
                    boolean inserted = !expected.containsKey(key);
                    expected.put(key, value);
                    assertEquals(inserted, actual.put(key, value));
                }
                case 1 -> assertEquals(expected.remove(key) != null, actual.remove(key));
                case 2 -> assertEquals(expected.containsKey(key), actual.containsKey(key));
                default -> assertEquals(expected.getOrDefault(key, Long.MIN_VALUE),
                        actual.getOrDefault(key, Long.MIN_VALUE));
            }
            if ((round & 255) == 0) {
                assertEquals(expected.size(), actual.size());
                actual.trimToSize();
                for (var entry : expected.entrySet()) {
                    assertEquals(entry.getValue().longValue(),
                            actual.getOrThrow(entry.getKey().longValue()));
                }
            }
        }
        assertEquals(expected.size(), actual.size());
    }

    private static void exerciseSet(IntFunction<PackedMutableLongSet> factory) {
        PackedMutableLongSet actual = factory.apply(4);
        HashSet<Long> expected = new HashSet<>();
        Random random = new Random(0x534554L);
        long[] edges = {0L, Long.MIN_VALUE, Long.MAX_VALUE, -1L, 1L};

        for (int round = 0; round < 20_000; round++) {
            long value = round < edges.length ? edges[round] : random.nextInt(1024) - 512L;
            switch (round & 3) {
                case 0 -> assertEquals(expected.add(value), actual.add(value));
                case 1 -> assertEquals(expected.remove(value), actual.remove(value));
                case 2 -> assertEquals(expected.contains(value), actual.contains(value));
                default -> {
                    if (expected.contains(value)) {
                        assertTrue(actual.contains(value));
                    } else {
                        assertFalse(actual.contains(value));
                    }
                }
            }
            if ((round & 255) == 0) {
                assertEquals(expected.size(), actual.size());
                actual.trimToSize();
                for (long retained : expected) {
                    assertTrue(actual.contains(retained));
                }
            }
        }
        assertEquals(expected.size(), actual.size());
    }
}
