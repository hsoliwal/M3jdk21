// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongSortedMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class LeanSortedMapVariantsTest {
    @Test
    void sparseMapMatchesTreeMap() {
        PackedLongLongSparseSortedMap actual =
                new PackedLongLongSparseSortedMap(0, 16, 0.75);
        exercise(actual, actual::checkInvariants, 0x51A25EL);
        assertTrue(actual.blockCount() <= actual.capacity() / actual.blockWidth());
        assertTrue(actual.backingArrayPayloadBytes() >= 0);
    }

    @Test
    void batchMapMatchesTreeMap() {
        PackedLongLongBatchSortedMap actual =
                new PackedLongLongBatchSortedMap(0, 23);
        exercise(actual, actual::checkInvariants, 0xBA7C4L);
        assertEquals(0, actual.pendingSize());
        assertTrue(actual.backingArrayPayloadBytes() > 0);
    }

    @Test
    void callbackFailureAndReentrancyAreFailClosed() {
        PackedLongLongSparseSortedMap sparse = new PackedLongLongSparseSortedMap();
        PackedLongLongBatchSortedMap batch = new PackedLongLongBatchSortedMap(0, 64);
        for (int i = 0; i < 32; i++) {
            sparse.put(i, i * 7L);
            batch.put(i, i * 7L);
        }
        assertCallbackSafety(sparse);
        assertCallbackSafety(batch);

        assertThrows(
                IllegalStateException.class,
                () -> sparse.putIf(100, 1, (key, value) -> {
                    sparse.clear();
                    return true;
                }));

        PackedLongLongBatchSortedMap admitted =
                new PackedLongLongBatchSortedMap(0, 8, (key, value) -> value >= 0);
        assertFalse(admitted.put(1, -1));
        assertTrue(admitted.isEmpty());
        assertTrue(admitted.put(1, 10));
        assertEquals(10, admitted.getOrThrow(1));
        assertFalse(admitted.put(1, -10));
        assertEquals(10, admitted.getOrThrow(1));
    }

    @Test
    void storageHasNoPerMappingStructuralObjectFields() {
        assertLean(PackedLongLongSparseSortedMap.class);
        assertLean(PackedLongLongBatchSortedMap.class);
    }

    @Test
    void factoriesExposeBothShapes() {
        assertTrue(PackedCollectionShapes.sparseSortedMap(0).isEmpty());
        assertTrue(PackedCollectionShapes.sparseSortedMap(0, 16, 0.75).isEmpty());
        assertTrue(PackedCollectionShapes.batchSortedMap(0, 8).isEmpty());
        assertFalse(PackedCollectionShapes.batchSortedMap(
                0, 8, (key, value) -> false).put(1, 1));
    }

    private static void exercise(
            LongLongSortedMap actual,
            Runnable invariants,
            long seed) {
        TreeMap<Long, Long> expected = new TreeMap<>();
        Random random = new Random(seed);
        for (int step = 0; step < 50_000; step++) {
            long key = key(random);
            long value = random.nextLong();
            switch (random.nextInt(10)) {
                case 0, 1, 2 -> {
                    boolean inserted = !expected.containsKey(key);
                    expected.put(key, value);
                    assertEquals(inserted, actual.put(key, value));
                }
                case 3 -> assertEquals(expected.remove(key) != null, actual.remove(key));
                case 4 -> assertEquals(
                        expected.getOrDefault(key, value),
                        actual.getOrDefault(key, value));
                case 5 -> assertEquals(expected.containsKey(key), actual.containsKey(key));
                case 6 -> {
                    long fallback = random.nextLong();
                    assertEquals(or(expected.floorKey(key), fallback),
                            actual.floorKey(key, fallback));
                    assertEquals(or(expected.ceilingKey(key), fallback),
                            actual.ceilingKey(key, fallback));
                    assertEquals(or(expected.lowerKey(key), fallback),
                            actual.lowerKey(key, fallback));
                    assertEquals(or(expected.higherKey(key), fallback),
                            actual.higherKey(key, fallback));
                }
                case 7 -> {
                    if ((step & 255) == 0) {
                        int before = expected.size();
                        expected.entrySet().removeIf(entry -> (entry.getKey() & 7L) == 0L);
                        assertEquals(before - expected.size(),
                                actual.filterInPlace((k, v) -> (k & 7L) != 0L));
                    }
                }
                case 8 -> {
                    if ((step & 2047) == 0) {
                        actual.clear();
                        expected.clear();
                    }
                }
                default -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.firstKey().longValue(), actual.firstKey());
                        assertEquals(expected.lastKey().longValue(), actual.lastKey());
                    }
                }
            }
            if ((step & 63) == 0) compare(actual, expected, invariants);
        }
        compare(actual, expected, invariants);
    }

    private static void compare(
            LongLongSortedMap actual,
            TreeMap<Long, Long> expected,
            Runnable invariants) {
        assertEquals(expected.size(), actual.size());
        long[] keys = new long[expected.size()];
        long[] values = new long[expected.size()];
        int index = 0;
        for (Map.Entry<Long, Long> entry : expected.entrySet()) {
            keys[index] = entry.getKey();
            values[index] = entry.getValue();
            index++;
        }
        assertArrayEquals(keys, actual.keysToArray());
        assertArrayEquals(values, actual.valuesToArray());
        invariants.run();
    }

    private static void assertCallbackSafety(LongLongSortedMap map) {
        long[] keys = map.keysToArray();
        long[] values = map.valuesToArray();
        int[] calls = {0};
        assertThrows(
                Deliberate.class,
                () -> map.filterInPlace((key, value) -> {
                    if (++calls[0] == 5) throw new Deliberate();
                    return false;
                }));
        assertArrayEquals(keys, map.keysToArray());
        assertArrayEquals(values, map.valuesToArray());

        assertThrows(
                IllegalStateException.class,
                () -> map.filterInPlace((key, value) -> {
                    map.clear();
                    return true;
                }));
        assertArrayEquals(keys, map.keysToArray());
        assertArrayEquals(values, map.valuesToArray());

        assertThrows(
                IllegalStateException.class,
                () -> map.forEach((key, value) -> map.remove(key)));
        assertArrayEquals(keys, map.keysToArray());
        assertArrayEquals(values, map.valuesToArray());
    }

    private static void assertLean(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> retained = field.getType();
            Class<?> component = retained.isArray() ? retained.getComponentType() : retained;
            assertFalse(Map.Entry.class.isAssignableFrom(component), field.toString());
            String name = component.getSimpleName();
            assertFalse(
                    name.endsWith("Node")
                            || name.endsWith("Entry")
                            || name.endsWith("Bucket")
                            || name.endsWith("Cell"),
                    field.toString());
        }
        assertEquals(0, type.getDeclaredClasses().length);
    }

    private static long key(Random random) {
        return switch (random.nextInt(32)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(4096) - 2048L;
        };
    }

    private static long or(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private static final class Deliberate extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
