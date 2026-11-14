/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Parallel-lane window merge versus detached-copy and TreeMap oracles. */
final class LongLongTreeMapWindowBulkTest {
    private static long key(Random random) {
        return switch (random.nextInt(19)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(257) - 128L;
        };
    }

    private static void assertContent(LongLongTreeMap actual, LongLongTreeMap copied,
            TreeMap<Long, Long> model) {
        assertEquals(model.size(), actual.size());
        assertEquals(copied.capacity(), actual.capacity());
        assertArrayEquals(copied.keysToArray(), actual.keysToArray());
        assertArrayEquals(copied.valuesToArray(), actual.valuesToArray());
        assertArrayEquals(model.keySet().stream().mapToLong(Long::longValue).toArray(),
                actual.keysToArray());
        assertArrayEquals(model.values().stream().mapToLong(Long::longValue).toArray(),
                actual.valuesToArray());
    }

    @Test
    void offsetsPreserveExactCopiedMergeBehaviorAndInputOwnership() {
        Random random = new Random(0x4d33574e44574cL);
        for (int trial = 0; trial < 8; trial++) {
            LongLongTreeMap actual = new LongLongTreeMap();
            LongLongTreeMap copied = new LongLongTreeMap();
            TreeMap<Long, Long> model = new TreeMap<>();
            for (int step = 0; step < 4_000; step++) {
                if (step > 0 && step % 251 == 0) {
                    actual.clear();
                    copied.clear();
                    model.clear();
                }
                int count = random.nextInt(53);
                long[] keys = new long[count];
                long[] values = new long[count];
                for (int i = 0; i < count; i++) {
                    keys[i] = key(random);
                    values[i] = random.nextLong();
                }
                int from = random.nextInt(count + 1);
                int to = from + random.nextInt(count - from + 1);
                // Outside the checked window the source is not required sorted.
                Arrays.sort(keys, from, to);
                long[] originalKeys = keys.clone();
                long[] originalValues = values.clone();
                int expected = copied.putAllSorted(Arrays.copyOfRange(keys, from, to),
                        Arrays.copyOfRange(values, from, to));
                int result = actual.putAllSorted(keys, values, from, to);
                assertEquals(expected, result);
                assertArrayEquals(originalKeys, keys);
                assertArrayEquals(originalValues, values);
                for (int i = from; i < to; i++) model.put(keys[i], values[i]);
                if ((step & 31) == 0) assertContent(actual, copied, model);
            }
            assertContent(actual, copied, model);
        }
    }

    @Test
    void defensiveAliasFallbackAndFailFastEpochRemainIntact() throws Exception {
        LongLongTreeMap actual = new LongLongTreeMap(8);
        LongLongTreeMap copied = new LongLongTreeMap(8);
        for (long key : new long[] {10L, 20L, 30L, 40L}) {
            actual.put(key, -key);
            copied.put(key, -key);
        }
        Field keyField = LongLongTreeMap.class.getDeclaredField("keys");
        Field valueField = LongLongTreeMap.class.getDeclaredField("values");
        keyField.setAccessible(true);
        valueField.setAccessible(true);
        long[] internalKeys = (long[]) keyField.get(actual);
        long[] internalValues = (long[]) valueField.get(actual);
        assertEquals(copied.putAllSorted(Arrays.copyOfRange(internalKeys, 1, 4),
                        Arrays.copyOfRange(internalValues, 1, 4)),
                actual.putAllSorted(internalKeys, internalValues, 1, 4));
        assertArrayEquals(copied.keysToArray(), actual.keysToArray());
        assertArrayEquals(copied.valuesToArray(), actual.valuesToArray());
        assertEquals(copied.capacity(), actual.capacity());

        long[] keys = {999L, 30L, 20L, -999L};
        long[] values = {1L, 2L, 3L, 4L};
        long[] unchanged = actual.keysToArray();
        assertThrows(IllegalArgumentException.class,
                () -> actual.putAllSorted(keys, values, 1, 3));
        assertThrows(IllegalArgumentException.class,
                () -> actual.putAllSorted(keys, new long[2], 1, 3));
        assertThrows(IndexOutOfBoundsException.class,
                () -> actual.putAllSorted(keys, values, -1, 2));
        assertThrows(NullPointerException.class,
                () -> actual.putAllSorted(null, values, 0, 0));
        assertEquals(0, actual.putAllSorted(keys, values, 2, 2));
        assertArrayEquals(unchanged, actual.keysToArray());

        LongIterator iterator = actual.keyIterator();
        assertTrue(iterator.hasNext());
        assertEquals(0, actual.putAllSorted(new long[] {1, 20, 0},
                new long[] {1, 200, 1}, 1, 2));
        assertTrue(iterator.hasNext());
        assertEquals(1, actual.putAllSorted(new long[] {1, 21, 0},
                new long[] {1, 210, 1}, 1, 2));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
    }
}
