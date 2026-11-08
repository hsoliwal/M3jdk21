/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Port of the matching semantics of com.synexia {@code LeanGapSortedCollectionsTest} (common module, blob
 * 2deb436110d7, develop 0616288bfa7d) onto the M3 owners that absorbed the positional gap: the gap-buffer set
 * and map scenarios run on {@link LongTreeSet} and {@link LongLongTreeMap} with the donor seeds, key
 * distribution and step counts, and every comparison also walks the positional API. Classification of each
 * donor test and assertion: qualification/primitive-method-gaps-20261107/PORT_CLASSIFICATION.tsv (gap-buffer
 * instrumentation, the fail-closed callback rule and the factories do not match and are not ported).
 */
final class LeanGapSortedPortTest {
    private static final long MAP_SEED = 0x6A70504DL;
    private static final long SET_SEED = 0x6A705357L;
    private static final int STEPS = 60_000;

    @Test
    void treeMapMatchesTreeMapAcrossLocalizedAndDistantEdits() {
        LongLongTreeMap actual = new LongLongTreeMap();
        TreeMap<Long, Long> expected = new TreeMap<>();
        Random random = new Random(MAP_SEED);
        for (int step = 0; step < STEPS; step++) {
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
                case 5 -> assertEquals(expected.getOrDefault(key, value), actual.getOrDefault(key, value));
                case 6 -> compareNavigation(actual, expected, key, random.nextLong());
                case 7 -> {
                    // donor case 7 is filterInPlace, which LongLongTreeMap does not carry (NOT_APPLICABLE)
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
            if ((step & 63) == 0) {
                compareMap(actual, expected);
            }
        }
        compareMap(actual, expected);
        assertTrue(actual.capacity() >= actual.size());
    }

    @Test
    void treeSetMatchesTreeSetAcrossLocalizedAndDistantEdits() {
        LongTreeSet actual = new LongTreeSet();
        TreeSet<Long> expected = new TreeSet<>();
        Random random = new Random(SET_SEED);
        for (int step = 0; step < STEPS; step++) {
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
                        assertEquals(before - expected.size(), actual.filterInPlace(candidate -> (candidate & 7L) != 0L));
                    }
                }
                case 7 -> {
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
            if ((step & 63) == 0) {
                compareSet(actual, expected);
            }
        }
        compareSet(actual, expected);
        assertTrue(actual.payloadBytes() >= 8L * actual.size());
    }

    @Test
    void extremesEmptyContractsAndStructure() {
        LongLongTreeMap map = new LongLongTreeMap();
        LongTreeSet set = new LongTreeSet();
        assertTrue(map.isEmpty());
        assertTrue(set.isEmpty());
        assertThrows(NoSuchElementException.class, map::firstKey);
        assertThrows(NoSuchElementException.class, set::last);

        assertTrue(map.put(Long.MIN_VALUE, Long.MAX_VALUE));
        assertTrue(map.put(0, 0));
        assertTrue(map.put(Long.MAX_VALUE, Long.MIN_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, map.keysToArray());
        assertEquals(Long.MAX_VALUE, map.getOrThrow(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, map.getOrThrow(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, map.keyAt(0));
        assertEquals(Long.MIN_VALUE, map.valueAt(2));

        assertTrue(set.add(Long.MIN_VALUE));
        assertTrue(set.add(0));
        assertTrue(set.add(Long.MAX_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, set.toArray());
        assertEquals(Long.MAX_VALUE, set.get(2));
        assertEquals(0, set.indexOf(Long.MIN_VALUE));

        assertLean(LongLongTreeMap.class);
        assertLean(LongTreeSet.class);
        assertEquals(0, LongLongTreeMap.class.getDeclaredClasses().length);
        assertEquals(0, LongTreeSet.class.getDeclaredClasses().length);
        assertThrows(IllegalArgumentException.class, () -> new LongLongTreeMap(-1));
    }

    private static void compareMap(LongLongTreeMap actual, TreeMap<Long, Long> expected) {
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
        for (int rank = 0; rank < keys.length; rank++) {
            assertEquals(keys[rank], actual.keyAt(rank));
            assertEquals(values[rank], actual.valueAt(rank));
            assertEquals(rank, actual.indexOfKey(keys[rank]));
        }
    }

    private static void compareSet(LongTreeSet actual, TreeSet<Long> expected) {
        assertEquals(expected.size(), actual.size());
        long[] values = new long[expected.size()];
        int index = 0;
        for (long value : expected) {
            values[index++] = value;
        }
        assertArrayEquals(values, actual.toArray());
        if (!expected.isEmpty()) {
            assertEquals(expected.first().longValue(), actual.first());
            assertEquals(expected.last().longValue(), actual.last());
        }
        for (int rank = 0; rank < values.length; rank++) {
            assertEquals(values[rank], actual.get(rank));
            assertEquals(rank, actual.indexOf(values[rank]));
        }
    }

    private static void compareNavigation(LongLongTreeMap actual, TreeMap<Long, Long> expected, long key,
            long fallback) {
        assertEquals(or(expected.floorKey(key), fallback), actual.floorKey(key, fallback));
        assertEquals(or(expected.ceilingKey(key), fallback), actual.ceilingKey(key, fallback));
        assertEquals(or(expected.lowerKey(key), fallback), actual.lowerKey(key, fallback));
        assertEquals(or(expected.higherKey(key), fallback), actual.higherKey(key, fallback));
    }

    private static void compareNavigation(LongTreeSet actual, TreeSet<Long> expected, long value, long fallback) {
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
}
