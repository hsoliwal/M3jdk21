/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * PackedLongLongSortedMap.putAll sorts, deduplicates and merges from the end: against putting the
 * pairs one by one on a twin built by the same operations, equal keys, values, size, capacity and
 * return value, and the batch arrays untouched.
 */
//FLLDCIM: LEETCODIFY — differential proof for PackedLongLongSortedMap.putAll
class PackedLongLongSortedMapPutAllTest {

    private static void compare(PackedLongLongSortedMap map, PackedLongLongSortedMap twin,
            long[] keys, long[] values, String at) {
        long[] keysCopy = keys.clone();
        long[] valuesCopy = values.clone();
        int before = twin.size();
        for (int i = 0; i < keys.length; i++) {
            twin.put(keys[i], values[i]);
        }
        assertEquals(twin.size() - before, map.putAll(keys, values), at + " added");
        assertArrayEquals(twin.keysToArray(), map.keysToArray(), at + " keys");
        assertArrayEquals(twin.valuesToArray(), map.valuesToArray(), at + " values");
        assertEquals(twin.capacity(), map.capacity(), at + " capacity");
        assertArrayEquals(keysCopy, keys, at + " batch keys untouched");
        assertArrayEquals(valuesCopy, values, at + " batch values untouched");
    }

    private static long key(Random random, int span) {
        return switch (random.nextInt(20)) {
            case 0 -> Long.MIN_VALUE + random.nextInt(3);
            case 1 -> Long.MAX_VALUE - random.nextInt(3);
            default -> random.nextInt(span) - span / 2;
        };
    }

    @Test
    void randomBatchesAgainstOneByOne() {
        int[] expected = {0, 1, 5, 8, 64};
        for (int seed = 0; seed < 3_000; seed++) {
            Random random = new Random(seed);
            int span = 4 + random.nextInt(500);
            int capacity = expected[seed % expected.length];
            PackedLongLongSortedMap map = new PackedLongLongSortedMap(capacity);
            PackedLongLongSortedMap twin = new PackedLongLongSortedMap(capacity);
            for (int i = random.nextInt(120); i > 0; i--) {
                long k = key(random, span);
                long v = random.nextLong();
                boolean remove = random.nextInt(6) == 0;
                for (PackedLongLongSortedMap m : new PackedLongLongSortedMap[] {map, twin}) {
                    if (remove) m.remove(k);
                    else m.put(k, v);
                }
            }
            int count = random.nextInt(80);
            long[] keys = new long[count];
            long[] values = new long[count];
            boolean ascending = random.nextInt(4) == 0;
            for (int i = 0; i < count; i++) {
                keys[i] = ascending ? (i == 0 ? -span : keys[i - 1] + random.nextInt(3))
                        : key(random, span);
                values[i] = random.nextLong();
            }
            compare(map, twin, keys, values, "seed " + seed);
        }
    }

    @Test
    void duplicatesInTheBatchLastWins() {
        PackedLongLongSortedMap map = new PackedLongLongSortedMap();
        PackedLongLongSortedMap twin = new PackedLongLongSortedMap();
        map.put(5, 50);
        twin.put(5, 50);
        compare(map, twin, new long[] {9, 5, 9, 1, 5, 9, 1}, new long[] {1, 2, 3, 4, 5, 6, 7},
                "dups");
        assertEquals(6, map.getOrThrow(9));
        assertEquals(5, map.getOrThrow(5));
        assertEquals(7, map.getOrThrow(1));
    }

    @Test
    void appendedAndPrependedBatches() {
        for (boolean after : new boolean[] {true, false}) {
            PackedLongLongSortedMap map = new PackedLongLongSortedMap(3);
            PackedLongLongSortedMap twin = new PackedLongLongSortedMap(3);
            for (long k = 0; k < 1_000; k++) {
                map.put(k, k);
                twin.put(k, k);
            }
            long[] keys = new long[500];
            long[] values = new long[500];
            for (int i = 0; i < 500; i++) {
                keys[i] = after ? 2_000 - i : -1 - i;
                values[i] = i;
            }
            compare(map, twin, keys, values, after ? "after" : "before");
        }
    }

    @Test
    void emptyMismatchAndNull() {
        PackedLongLongSortedMap map = new PackedLongLongSortedMap(2);
        assertEquals(0, map.putAll(new long[0], new long[0]));
        assertEquals(2, map.capacity());
        assertThrows(IllegalArgumentException.class, () -> map.putAll(new long[4], new long[5]));
        assertThrows(NullPointerException.class, () -> map.putAll(null, new long[0]));
        assertThrows(NullPointerException.class, () -> map.putAll(new long[0], null));
    }

    @Test
    void largeRandomBatch() {
        Random random = new Random(42);
        PackedLongLongSortedMap map = new PackedLongLongSortedMap();
        PackedLongLongSortedMap twin = new PackedLongLongSortedMap();
        for (int i = 0; i < 20_000; i++) {
            long k = random.nextInt(1_000_000);
            map.put(k, i);
            twin.put(k, i);
        }
        long[] keys = random.longs(20_000, 0, 1_000_000).toArray();
        long[] values = random.longs(20_000).toArray();
        compare(map, twin, keys, values, "20,000 into 20,000");
    }
}
