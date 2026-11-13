/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.id;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * M3LongObjectLinkedHashMap (family ID_MAP of recipe ARRAY_MAP_FILL) against its oracle,
 * {@code java.util.LinkedHashMap<Long, String>}: the same seeded scripts on both, every return value compared, and
 * after every check point the size, the insertion order (ids, forEach, first and last), positional access, the hash
 * code and the text. Ids built to share their fold exercise long probe chains; wide runs exercise growth and
 * compaction.
 */
class M3IdMapDifferentialTest {

    private static final int SEEDS = 40;
    private static final int STEPS = 3_000;
    private static final int CHECK_EVERY = 97;

    private static void sameState(M3LongObjectLinkedHashMap<String> map, LinkedHashMap<Long, String> oracle) {
        assertEquals(oracle.size(), map.size());
        assertEquals(oracle.isEmpty(), map.isEmpty());
        List<Long> keys = new ArrayList<>(oracle.keySet());
        assertArrayEquals(keys.stream().mapToLong(Long::longValue).toArray(), map.ids());
        assertEquals(oracle.hashCode(), map.hashCode());
        assertEquals(oracle.toString(), map.toString());
        List<Long> seenIds = new ArrayList<>();
        List<String> seenValues = new ArrayList<>();
        map.forEach((id, value) -> {
            seenIds.add(id);
            seenValues.add(value);
        });
        assertEquals(keys, seenIds);
        assertEquals(new ArrayList<>(oracle.values()), seenValues);
        if (keys.isEmpty()) {
            assertThrows(NoSuchElementException.class, map::firstId);
            assertThrows(NoSuchElementException.class, map::lastId);
        } else {
            assertEquals(keys.get(0), map.firstId());
            assertEquals(keys.get(keys.size() - 1), map.lastId());
        }
    }

    private static void positional(M3LongObjectLinkedHashMap<String> map, LinkedHashMap<Long, String> oracle,
            Random random, long id) {
        List<Long> keys = new ArrayList<>(oracle.keySet());
        int position = random.nextInt(keys.size() + 2) - 1;
        if (position < 0 || position >= keys.size()) {
            assertThrows(IndexOutOfBoundsException.class, () -> map.idAt(position));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(position));
        } else {
            assertEquals(keys.get(position), map.idAt(position));
            assertEquals(oracle.get(keys.get(position)), map.valueAt(position));
        }
        assertEquals(keys.indexOf(id), map.indexOf(id));
    }

    private static void step(M3LongObjectLinkedHashMap<String> map, LinkedHashMap<Long, String> oracle, Random random,
            long id, int step) {
        switch (random.nextInt(10)) {
            case 0, 1, 2, 3 -> {
                String value = random.nextInt(5) == 0 ? null : "v" + step;
                assertEquals(oracle.put(id, value), map.put(id, value));
            }
            case 4, 5 -> assertEquals(oracle.remove(id), map.remove(id));
            case 6 -> assertEquals(oracle.get(id), map.get(id));
            case 7 -> assertEquals(oracle.containsKey(id), map.containsKey(id));
            case 8 -> positional(map, oracle, random, id);
            default -> assertEquals(oracle.getOrDefault(id, "fallback"), map.getOrDefault(id, "fallback"));
        }
    }

    @Test
    void randomScriptsMatchLinkedHashMap() {
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            M3LongObjectLinkedHashMap<String> map = new M3LongObjectLinkedHashMap<>(random.nextInt(8));
            LinkedHashMap<Long, String> oracle = new LinkedHashMap<>();
            long range = 1 + random.nextInt(200);
            for (int step = 0; step < STEPS; step++) {
                step(map, oracle, random, random.nextLong(range) - range / 2, step);
                if (step % CHECK_EVERY == 0) {
                    sameState(map, oracle);
                }
                if (random.nextInt(STEPS / 4) == 0) {
                    oracle.clear();
                    map.clear();
                }
            }
            sameState(map, oracle);
        }
    }

    @Test
    void idsThatShareTheirFoldStayDistinct() {
        for (int seed = 0; seed < SEEDS / 4; seed++) {
            Random random = new Random(seed);
            M3LongObjectLinkedHashMap<String> map = new M3LongObjectLinkedHashMap<>();
            LinkedHashMap<Long, String> oracle = new LinkedHashMap<>();
            int[] highs = random.ints(64).toArray();
            for (int step = 0; step < STEPS; step++) {
                int high = highs[random.nextInt(highs.length)];
                long id = (long) high << 32 | Integer.toUnsignedLong(high ^ random.nextInt(3)); // folds to 0, 1 or 2
                step(map, oracle, random, id, step);
            }
            sameState(map, oracle);
        }
    }

    @Test
    void wideRunsGrowAndCompact() {
        Random random = new Random(11);
        M3LongObjectLinkedHashMap<String> map = new M3LongObjectLinkedHashMap<>();
        LinkedHashMap<Long, String> oracle = new LinkedHashMap<>();
        for (int round = 0; round < 6; round++) {
            for (int i = 0; i < 20_000; i++) {
                long id = random.nextLong();
                assertEquals(oracle.put(id, "w" + i), map.put(id, "w" + i));
            }
            List<Long> keys = new ArrayList<>(oracle.keySet());
            for (int i = 0; i < keys.size(); i += 1 + random.nextInt(3)) {
                assertEquals(oracle.remove(keys.get(i)), map.remove(keys.get(i)));
            }
            sameState(map, oracle);
            for (int probe = 0; probe < 200; probe++) {
                positional(map, oracle, random, keys.get(random.nextInt(keys.size())));
            }
        }
    }

    @Test
    void equalityIgnoresOrderLikeMapEquality() {
        M3LongObjectLinkedHashMap<String> a = new M3LongObjectLinkedHashMap<>();
        M3LongObjectLinkedHashMap<String> b = new M3LongObjectLinkedHashMap<>();
        for (long id = 0; id < 50; id++) {
            a.put(id, "x" + id);
            b.put(49 - id, "x" + (49 - id));
        }
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.put(7, "changed");
        assertNotEquals(a, b);
        b.put(7, "x7");
        b.put(50, null);
        assertNotEquals(a, b);
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a map"));
    }

    @Test
    void edgesAndFailFastTraversal() {
        assertThrows(IllegalArgumentException.class, () -> new M3LongObjectLinkedHashMap<String>(-1));
        M3LongObjectLinkedHashMap<Object> self = new M3LongObjectLinkedHashMap<>();
        self.put(1, self);
        LinkedHashMap<Long, Object> oracle = new LinkedHashMap<>();
        oracle.put(1L, oracle);
        assertEquals(oracle.toString(), self.toString());
        M3LongObjectLinkedHashMap<String> map = new M3LongObjectLinkedHashMap<>();
        map.put(Long.MIN_VALUE, "min");
        map.put(Long.MAX_VALUE, "max");
        map.put(0, "zero");
        assertThrows(ConcurrentModificationException.class, () -> map.forEach((id, value) -> map.put(id + 1, value)));
        map.put(0, "replaced"); // a present id keeps its position and is no structural change
        assertEquals(2, map.indexOf(0));
        assertTrue(map.containsKey(Long.MIN_VALUE) && map.containsKey(Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> map.forEach(null));
    }
}
