/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Deterministic behavioral-oracle test; executable with plain Java 21. */
public final class PackedStateCollectionsTest {
    private static final long[] EDGE_KEYS = {
        0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
        0x5555555555555555L, 0xAAAAAAAAAAAAAAAAL,
        31L, 32L, 63L, 64L
    };

    private PackedStateCollectionsTest() { }

    public static void main(String[] args) {
        testPackedWords();
        for (int seed = 0; seed < 8; seed++) {
            testSet(seed);
            testMap(seed);
        }
        System.out.println("PASS PackedSlotStates and primitive collection oracles (8 seeds, 320,000 operations)");
    }

    private static void testPackedWords() {
        for (int capacity : new int[] {0, 1, 8, 31, 32, 33, 63, 64, 65, 127, 128, 129}) {
            PackedSlotStates states = new PackedSlotStates(capacity);
            require(states.length() == capacity, "logical state length");
            for (int i = 0; i < capacity; i++) {
                require(states.get(i) == 0, "new lane not empty");
            }
            for (int i = 0; i < capacity; i++) {
                states.set(i, (byte) ((i % 3) + 0));
            }
            for (int i = 0; i < capacity; i++) {
                require(states.get(i) == (byte) (i % 3), "packed neighbor overwritten at " + i);
            }
            for (int i = capacity - 1; i >= 0; i--) {
                states.set(i, (byte) 2);
            }
            for (int i = 0; i < capacity; i++) {
                require(states.get(i) == 2, "high-state boundary " + i);
            }
            states.clear();
            for (int i = 0; i < capacity; i++) {
                require(states.get(i) == 0, "clear at " + i);
            }
        }
        boolean failed = false;
        try {
            new PackedSlotStates(-1);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        require(failed, "negative capacity must fail");
    }

    private static void testSet(int seed) {
        Random random = new Random(0x41278ED1L + seed);
        LongHashSet actual = new LongHashSet(1);
        Set<Long> expected = new HashSet<>();
        for (long key : EDGE_KEYS) {
            require(actual.add(key) == expected.add(key), "edge set insert");
        }
        assertSet(actual, expected);
        for (int step = 0; step < 20_000; step++) {
            long key = nextKey(random);
            int action = random.nextInt(100);
            if (action < 35) {
                require(actual.add(key) == expected.add(key), "set add at " + step);
            } else if (action < 64) {
                require(actual.remove(key) == expected.remove(key), "set remove at " + step);
            } else if (action < 88) {
                require(actual.contains(key) == expected.contains(key), "set contains at " + step);
            } else if (action < 94) {
                // Proves the sparse and dense clear paths inherited from PR #2986.
                actual.clear();
                expected.clear();
            } else if (action < 97) {
                LongIterator iterator = actual.iterator();
                long absent = 0x6500000000000000L + step;
                while (expected.contains(absent)) absent++;
                require(actual.add(absent), "fail-fast insertion");
                expected.add(absent);
                expectCme(iterator);
            } else {
                assertSet(actual, expected);
            }
            require(actual.size() == expected.size(), "set cardinality at " + step);
            if (step % 607 == 0) assertSet(actual, expected);
        }
        assertSet(actual, expected);
        for (long key : EDGE_KEYS) {
            require(actual.add(key) == expected.add(key), "reuse after stress");
            require(actual.remove(key) == expected.remove(key), "remove after stress");
        }
        assertSet(actual, expected);
    }

    private static void testMap(int seed) {
        Random random = new Random(0x1278BEDFL + seed);
        LongLongHashMap actual = new LongLongHashMap(1);
        Map<Long, Long> expected = new HashMap<>();
        for (long key : EDGE_KEYS) {
            long value = ~key;
            require(actual.put(key, value) == (expected.put(key, value) == null),
                    "edge map insert");
        }
        assertMap(actual, expected);
        for (int step = 0; step < 20_000; step++) {
            long key = nextKey(random);
            long value = nextKey(random);
            int action = random.nextInt(100);
            if (action < 32) {
                require(actual.put(key, value) == !expected.containsKey(key), "map put at " + step);
                expected.put(key, value);
            } else if (action < 47) {
                long result = expected.getOrDefault(key, 0L) + value;
                expected.put(key, result);
                require(actual.addTo(key, value) == result, "map addTo at " + step);
            } else if (action < 62) {
                require(actual.remove(key) == (expected.remove(key) != null), "map remove at " + step);
            } else if (action < 69) {
                long previous = expected.getOrDefault(key, value);
                boolean matching = expected.containsKey(key) && previous == value;
                require(actual.remove(key, value) == matching, "map conditional remove at " + step);
                if (matching) expected.remove(key);
            } else if (action < 76) {
                long before = expected.getOrDefault(key, value);
                boolean matching = expected.containsKey(key) && before == value;
                require(actual.replace(key, value, ~value) == matching, "map replace at " + step);
                if (matching) expected.put(key, ~value);
            } else if (action < 84) {
                require(actual.containsKey(key) == expected.containsKey(key), "map contains at " + step);
                require(actual.getOrDefault(key, 987L)
                        == expected.getOrDefault(key, 987L), "map getOrDefault at " + step);
            } else if (action < 90) {
                actual.clear();
                expected.clear();
            } else if (action < 94) {
                actual.trimToSize();
            } else if (action < 97) {
                LongIterator iterator = actual.keyIterator();
                long absent = 0x7600000000000000L + step;
                while (expected.containsKey(absent)) absent++;
                require(actual.put(absent, 1L), "map fail-fast insertion");
                expected.put(absent, 1L);
                expectCme(iterator);
            } else {
                assertMap(actual, expected);
            }
            require(actual.size() == expected.size(), "map cardinality at " + step);
            if (step % 607 == 0) assertMap(actual, expected);
        }
        assertMap(actual, expected);
    }

    private static void assertSet(LongHashSet actual, Set<Long> expected) {
        require(actual.size() == expected.size(), "set size mismatch");
        Set<Long> fromArray = new HashSet<>();
        for (long key : actual.toArray()) {
            require(fromArray.add(key), "duplicate set array entry");
        }
        require(fromArray.equals(expected), "set array differs from oracle");
        Set<Long> fromIterator = new HashSet<>();
        LongIterator iterator = actual.iterator();
        while (iterator.hasNext()) {
            require(fromIterator.add(iterator.nextLong()), "duplicate set iterator entry");
        }
        require(fromIterator.equals(expected), "set iteration differs from oracle");
    }

    private static void assertMap(LongLongHashMap actual, Map<Long, Long> expected) {
        require(actual.size() == expected.size(), "map size mismatch");
        Set<Long> seen = new HashSet<>();
        LongIterator iterator = actual.keyIterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            require(seen.add(key), "duplicate map iterator key");
            require(expected.containsKey(key), "unexpected map key");
            require(actual.getOrDefault(key, -7L) == expected.get(key), "map value mismatch");
        }
        require(seen.equals(expected.keySet()), "map iteration differs from oracle");
        long[] array = actual.keysToArray();
        require(array.length == seen.size(), "map key array length");
        Set<Long> arrayKeys = new HashSet<>();
        Arrays.stream(array).forEach(arrayKeys::add);
        require(arrayKeys.equals(seen), "map key array mismatch");
        Map<Long, Long> viaCallback = new HashMap<>();
        actual.forEach(viaCallback::put);
        require(viaCallback.equals(expected), "map callback differs from oracle");
        for (long v : EDGE_KEYS) {
            require(actual.containsValue(v) == expected.containsValue(v), "containsValue oracle");
        }
    }

    private static long nextKey(Random random) {
        int choice = random.nextInt(16);
        if (choice < 3) return EDGE_KEYS[random.nextInt(EDGE_KEYS.length)];
        if (choice < 15) return random.nextInt(257) - 128L;
        return random.nextLong();
    }

    private static void expectCme(LongIterator iterator) {
        try {
            iterator.hasNext();
            throw new AssertionError("iterator failed to notice structural modification");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
