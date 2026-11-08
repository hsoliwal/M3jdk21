// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class LeanGapSortedCollectionsTest {
    @Test
    void gapMapMatchesTreeMapAcrossLocalizedAndDistantEdits() {
        PackedLongLongGapSortedMap actual = new PackedLongLongGapSortedMap(0, 0.72);
        TreeMap<Long, Long> expected = new TreeMap<>();
        Random random = new Random(0x6A70504DL);

        for (int step = 0; step < 60_000; step++) {
            long key = key(random, step);
            long value = random.nextLong();
            switch (random.nextInt(10)) {
                case 0, 1, 2 -> {
                    boolean inserted = !expected.containsKey(key);
                    expected.put(key, value);
                    assertEquals(inserted, actual.put(key, value));
                }
                case 3 -> assertEquals(expected.remove(key) != null, actual.remove(key));
                case 4 -> assertEquals(expected.containsKey(key), actual.containsKey(key));
                case 5 -> assertEquals(expected.getOrDefault(key, value),
                        actual.getOrDefault(key, value));
                case 6 -> compareNavigation(actual, expected, key, random.nextLong());
                case 7 -> {
                    if ((step & 511) == 0) {
                        int before = expected.size();
                        expected.entrySet().removeIf(entry -> (entry.getKey() & 15L) == 0L);
                        assertEquals(before - expected.size(),
                                actual.filterInPlace((k, v) -> (k & 15L) != 0L));
                    }
                }
                case 8 -> {
                    if ((step & 4095) == 0) {
                        actual.trimToSize();
                    }
                }
                default -> {
                    if ((step & 8191) == 0) {
                        actual.clear();
                        expected.clear();
                    }
                }
            }
            if ((step & 63) == 0) compareMap(actual, expected);
        }
        compareMap(actual, expected);
        assertTrue(actual.movedMappings() >= 0);
        assertTrue(actual.backingArrayPayloadBytes() >= 16L * actual.size());
    }

    @Test
    void gapSetMatchesTreeSetAcrossLocalizedAndDistantEdits() {
        PackedLongGapSortedSet actual = new PackedLongGapSortedSet(0, 0.72);
        TreeSet<Long> expected = new TreeSet<>();
        Random random = new Random(0x6A705357L);

        for (int step = 0; step < 60_000; step++) {
            long value = key(random, step);
            switch (random.nextInt(9)) {
                case 0, 1, 2 -> assertEquals(expected.add(value), actual.add(value));
                case 3 -> assertEquals(expected.remove(value), actual.remove(value));
                case 4 -> assertEquals(expected.contains(value), actual.contains(value));
                case 5 -> compareNavigation(actual, expected, value, random.nextLong());
                case 6 -> {
                    if ((step & 511) == 0) {
                        int before = expected.size();
                        expected.removeIf(candidate -> (candidate & 7L) == 0L);
                        assertEquals(before - expected.size(),
                                actual.filterInPlace(candidate -> (candidate & 7L) != 0L));
                    }
                }
                case 7 -> {
                    if ((step & 4095) == 0) actual.trimToSize();
                }
                default -> {
                    if ((step & 8191) == 0) {
                        actual.clear();
                        expected.clear();
                    }
                }
            }
            if ((step & 63) == 0) compareSet(actual, expected);
        }
        compareSet(actual, expected);
        assertTrue(actual.movedValues() >= 0);
        assertTrue(actual.backingArrayPayloadBytes() >= 8L * actual.size());
    }

    @Test
    void nearbyInsertionsReuseTheMovableGap() {
        PackedLongLongGapSortedMap map = new PackedLongLongGapSortedMap(64, 0.75);
        for (int i = 0; i < 32; i++) map.put(i * 2L, i);
        long before = map.movedMappings();
        map.put(1, 100);
        long firstMove = map.movedMappings() - before;
        before = map.movedMappings();
        for (int i = 1; i < 16; i++) map.put(i * 2L + 1L, 100 + i);
        long followingMoves = map.movedMappings() - before;

        assertTrue(firstMove > 0);
        assertTrue(followingMoves <= 30,
                "nearby ascending inserts should move the already-near gap, moved=" + followingMoves);
        map.checkInvariants();
    }

    @Test
    void callbackFailuresAndReentrancyAreFailClosed() {
        PackedLongLongGapSortedMap map = new PackedLongLongGapSortedMap();
        PackedLongGapSortedSet set = new PackedLongGapSortedSet();
        for (int i = 0; i < 32; i++) {
            map.put(i, i * 11L);
            set.add(i);
        }

        long[] mapKeys = map.keysToArray();
        long[] mapValues = map.valuesToArray();
        int[] calls = {0};
        assertThrows(Deliberate.class, () -> map.filterInPlace((key, value) -> {
            if (++calls[0] == 5) throw new Deliberate();
            return false;
        }));
        assertArrayEquals(mapKeys, map.keysToArray());
        assertArrayEquals(mapValues, map.valuesToArray());
        assertThrows(IllegalStateException.class,
                () -> map.forEach((key, value) -> map.remove(key)));
        assertThrows(IllegalStateException.class,
                () -> map.putIf(100, 1, (key, value) -> {
                    map.clear();
                    return true;
                }));
        assertArrayEquals(mapKeys, map.keysToArray());

        long[] setValues = set.toArray();
        calls[0] = 0;
        assertThrows(Deliberate.class, () -> set.filterInPlace(value -> {
            if (++calls[0] == 5) throw new Deliberate();
            return false;
        }));
        assertArrayEquals(setValues, set.toArray());
        assertThrows(IllegalStateException.class,
                () -> set.forEachLong(value -> set.remove(value)));
        assertArrayEquals(setValues, set.toArray());
    }

    @Test
    void extremesEmptyContractsFactoriesAndStructure() {
        PackedLongLongGapSortedMap map = PackedCollectionShapes.gapSortedMap(0, 0.8);
        PackedLongGapSortedSet set = PackedCollectionShapes.gapSortedSet(0, 0.8);
        assertTrue(map.isEmpty());
        assertTrue(set.isEmpty());
        assertThrows(java.util.NoSuchElementException.class, map::firstKey);
        assertThrows(java.util.NoSuchElementException.class, set::last);

        assertTrue(map.put(Long.MIN_VALUE, Long.MAX_VALUE));
        assertTrue(map.put(0, 0));
        assertTrue(map.put(Long.MAX_VALUE, Long.MIN_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, map.keysToArray());
        assertEquals(Long.MAX_VALUE, map.getOrThrow(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, map.getOrThrow(Long.MAX_VALUE));

        assertTrue(set.add(Long.MIN_VALUE));
        assertTrue(set.add(0));
        assertTrue(set.add(Long.MAX_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, set.toArray());

        assertLean(PackedLongLongGapSortedMap.class);
        assertLean(PackedLongGapSortedSet.class);
        assertEquals(0, PackedLongLongGapSortedMap.class.getDeclaredClasses().length);
        assertEquals(0, PackedLongGapSortedSet.class.getDeclaredClasses().length);

        assertThrows(IllegalArgumentException.class,
                () -> new PackedLongLongGapSortedMap(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new PackedLongGapSortedSet(0, 1.0));
    }

    private static void compareMap(
            PackedLongLongGapSortedMap actual,
            TreeMap<Long, Long> expected) {
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
        if (!expected.isEmpty()) {
            assertEquals(expected.firstKey().longValue(), actual.firstKey());
            assertEquals(expected.lastKey().longValue(), actual.lastKey());
        }
        actual.checkInvariants();
        assertEquals(actual.capacity() - actual.size(), actual.gapSize());
        assertTrue(actual.gapRank() >= 0 && actual.gapRank() <= actual.size());
    }

    private static void compareSet(
            PackedLongGapSortedSet actual,
            TreeSet<Long> expected) {
        assertEquals(expected.size(), actual.size());
        long[] values = new long[expected.size()];
        int index = 0;
        for (long value : expected) values[index++] = value;
        assertArrayEquals(values, actual.toArray());
        if (!expected.isEmpty()) {
            assertEquals(expected.first().longValue(), actual.first());
            assertEquals(expected.last().longValue(), actual.last());
        }
        actual.checkInvariants();
        assertEquals(actual.capacity() - actual.size(), actual.gapSize());
        assertTrue(actual.gapRank() >= 0 && actual.gapRank() <= actual.size());
    }

    private static void compareNavigation(
            PackedLongLongGapSortedMap actual,
            TreeMap<Long, Long> expected,
            long key,
            long fallback) {
        assertEquals(or(expected.floorKey(key), fallback), actual.floorKey(key, fallback));
        assertEquals(or(expected.ceilingKey(key), fallback), actual.ceilingKey(key, fallback));
        assertEquals(or(expected.lowerKey(key), fallback), actual.lowerKey(key, fallback));
        assertEquals(or(expected.higherKey(key), fallback), actual.higherKey(key, fallback));
    }

    private static void compareNavigation(
            PackedLongGapSortedSet actual,
            TreeSet<Long> expected,
            long value,
            long fallback) {
        assertEquals(or(expected.floor(value), fallback), actual.floor(value, fallback));
        assertEquals(or(expected.ceiling(value), fallback), actual.ceiling(value, fallback));
        assertEquals(or(expected.lower(value), fallback), actual.lower(value, fallback));
        assertEquals(or(expected.higher(value), fallback), actual.higher(value, fallback));
    }

    private static long key(Random random, int step) {
        return switch (random.nextInt(32)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3, 4, 5, 6 -> (step & 255) - 128L;
            default -> random.nextInt(8192) - 4096L;
        };
    }

    private static long or(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private static void assertLean(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> retained = field.getType();
            Class<?> component = retained.isArray() ? retained.getComponentType() : retained;
            assertFalse(Map.Entry.class.isAssignableFrom(component), field.toString());
            String name = component.getSimpleName();
            assertFalse(name.endsWith("Node") || name.endsWith("Entry")
                    || name.endsWith("Bucket") || name.endsWith("Cell"), field.toString());
        }
    }

    private static final class Deliberate extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
