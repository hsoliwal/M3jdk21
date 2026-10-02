/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.collections.M3LongIterator;
import com.m3.collections.M3PackedLongDeque;
import com.m3.collections.M3PackedLongLongHashMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

public final class M3CollectionsFoundationTest {
    private static long checks;

    public static void main(String[] args) {
        testDequeDeterministic();
        testDequeRandomized();
        testMapDeterministic();
        testMapRandomized();
        testFailuresAndCallbacks();
        System.out.println("M3_COLLECTIONS_FOUNDATION_PASS checks=" + checks);
    }

    private static void testDequeDeterministic() {
        M3PackedLongDeque deque = new M3PackedLongDeque(0);
        equal(2, deque.capacity());
        truth(deque.isEmpty());
        expect(NoSuchElementException.class, deque::first);
        expect(NoSuchElementException.class, deque::last);
        expect(NoSuchElementException.class, deque::poll);

        deque.addLast(2);
        deque.addFirst(1);
        deque.addLast(3);
        array(new long[] {1, 2, 3}, deque.toArray());
        equal(1L, deque.first());
        equal(3L, deque.last());
        equal(2L, deque.get(1));
        truth(deque.contains(2));
        truth(!deque.contains(9));
        equal(1L, deque.removeFirst());
        equal(3L, deque.removeLast());
        equal(2L, deque.poll());
        truth(deque.isEmpty());

        for (long value = 0; value < 64; value++) {
            deque.addLast(value);
        }
        long[] batch = new long[17];
        equal(17, deque.drainTo(batch, 0, batch.length));
        for (int i = 0; i < batch.length; i++) {
            equal((long) i, batch[i]);
        }
        equal(47, deque.size());
        equal(24, deque.filterInPlace(value -> (value & 1L) == 0L));
        for (long value : deque.toArray()) {
            truth((value & 1L) == 0L);
        }

        M3LongIterator iterator = deque.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.nextLong();
            count++;
        }
        equal(deque.size(), count);
        expect(NoSuchElementException.class, iterator::nextLong);
    }

    private static void testDequeRandomized() {
        Random random = new Random(0x4d3344455155454cL);
        M3PackedLongDeque actual = new M3PackedLongDeque();
        ArrayDeque<Long> oracle = new ArrayDeque<>();
        for (int step = 0; step < 120_000; step++) {
            int op = random.nextInt(8);
            long value = random.nextLong();
            switch (op) {
                case 0 -> { actual.addFirst(value); oracle.addFirst(value); }
                case 1 -> { actual.addLast(value); oracle.addLast(value); }
                case 2 -> {
                    if (oracle.isEmpty()) expect(NoSuchElementException.class, actual::removeFirst);
                    else equal(oracle.removeFirst().longValue(), actual.removeFirst());
                }
                case 3 -> {
                    if (oracle.isEmpty()) expect(NoSuchElementException.class, actual::removeLast);
                    else equal(oracle.removeLast().longValue(), actual.removeLast());
                }
                case 4 -> {
                    truth(actual.contains(value) == oracle.contains(value));
                }
                case 5 -> {
                    if (!oracle.isEmpty()) {
                        int index = random.nextInt(oracle.size());
                        long expected = oracle.stream().skip(index).findFirst().orElseThrow();
                        equal(expected, actual.get(index));
                    }
                }
                case 6 -> {
                    if (oracle.size() < 256) {
                        actual.offer(value);
                        oracle.offer(value);
                    }
                }
                default -> {
                    if ((step & 1023) == 0) {
                        actual.clear();
                        oracle.clear();
                    }
                }
            }
            equal(oracle.size(), actual.size());
            if ((step & 255) == 0) {
                long[] expected = oracle.stream().mapToLong(Long::longValue).toArray();
                array(expected, actual.toArray());
            }
        }
    }

    private static void testMapDeterministic() {
        M3PackedLongLongHashMap map = new M3PackedLongLongHashMap(0);
        truth(map.isEmpty());
        truth(map.capacity() >= 2);
        truth(map.put(1, 10));
        truth(!map.put(1, 11));
        equal(11L, map.getOrThrow(1));
        equal(7L, map.getOrDefault(2, 7));
        truth(map.containsKey(1));
        truth(!map.containsKey(2));
        truth(map.replace(1, 11, 12));
        truth(!map.replace(1, 11, 13));
        equal(12L, map.computeIfAbsent(1, key -> 99));
        equal(40L, map.computeIfAbsent(4, key -> key * 10));
        equal(17L, map.addTo(1, 5, 0));
        equal(8L, map.addTo(8, 3, 5));
        truth(map.remove(8, 8));
        truth(!map.remove(8));
        equal(2, map.size());

        long[] keys = map.keysToArray();
        long[] values = map.valuesToArray();
        equal(keys.length, values.length);
        Map<Long, Long> reconstructed = new HashMap<>();
        for (int i = 0; i < keys.length; i++) reconstructed.put(keys[i], values[i]);
        equal(Map.of(1L, 17L, 4L, 40L), reconstructed);

        equal(1, map.filterInPlace((key, value) -> key == 1));
        equal(1, map.size());
        map.trimToSize();
        equal(17L, map.getOrThrow(1));
        map.clear();
        truth(map.isEmpty());
        map.trimToSize();
        truth(map.capacity() >= 2);
        expect(IllegalArgumentException.class, () -> map.getOrThrow(1));
    }

    private static void testMapRandomized() {
        Random random = new Random(0x4d33484153484d41L);
        M3PackedLongLongHashMap actual = new M3PackedLongLongHashMap();
        HashMap<Long, Long> oracle = new HashMap<>();
        for (int step = 0; step < 160_000; step++) {
            long key = random.nextInt(4096) - 2048L;
            long value = random.nextLong();
            int op = random.nextInt(8);
            switch (op) {
                case 0 -> {
                    boolean inserted = !oracle.containsKey(key);
                    oracle.put(key, value);
                    truth(actual.put(key, value) == inserted);
                }
                case 1 -> {
                    boolean expected = oracle.remove(key) != null;
                    truth(actual.remove(key) == expected);
                }
                case 2 -> truth(actual.containsKey(key) == oracle.containsKey(key));
                case 3 -> {
                    long fallback = 0x55aa55aa55aa55aaL;
                    equal(oracle.getOrDefault(key, fallback).longValue(), actual.getOrDefault(key, fallback));
                }
                case 4 -> {
                    long expected = oracle.computeIfAbsent(key, k -> k * 31L + 7L);
                    equal(expected, actual.computeIfAbsent(key, k -> k * 31L + 7L));
                }
                case 5 -> {
                    long initial = 11L;
                    long delta = random.nextInt(33) - 16L;
                    long expected = oracle.merge(key, initial + delta, (oldValue, ignored) -> oldValue + delta);
                    equal(expected, actual.addTo(key, delta, initial));
                }
                case 6 -> {
                    if ((step & 511) == 0) {
                        actual.trimToSize();
                    }
                }
                default -> {
                    if ((step & 4095) == 0) {
                        actual.clear();
                        oracle.clear();
                    }
                }
            }
            equal(oracle.size(), actual.size());
            if ((step & 511) == 0) assertMapEquals(oracle, actual);
        }
        assertMapEquals(oracle, actual);
    }

    private static void testFailuresAndCallbacks() {
        expect(IllegalArgumentException.class, () -> new M3PackedLongDeque(-1));
        expect(IllegalArgumentException.class, () -> new M3PackedLongLongHashMap(-1));

        M3PackedLongLongHashMap map = new M3PackedLongLongHashMap();
        map.put(1, 2);
        expect(IllegalStateException.class, () -> map.computeIfAbsent(3, key -> {
            throw new IllegalStateException("boom");
        }));
        truth(!map.containsKey(3));

        Map<Long, Long> visited = new HashMap<>();
        map.forEach((key, value) -> visited.put(key, value));
        equal(Map.of(1L, 2L), visited);
    }

    private static void assertMapEquals(Map<Long, Long> expected, M3PackedLongLongHashMap actual) {
        equal(expected.size(), actual.size());
        long[] keys = actual.keysToArray();
        long[] values = actual.valuesToArray();
        equal(keys.length, values.length);
        Map<Long, Long> reconstructed = new HashMap<>();
        for (int index = 0; index < keys.length; index++) reconstructed.put(keys[index], values[index]);
        equal(expected, reconstructed);
        for (Map.Entry<Long, Long> entry : expected.entrySet()) {
            equal(entry.getValue().longValue(), actual.getOrThrow(entry.getKey()));
        }
    }

    private static void array(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
        }
    }

    private static void truth(boolean value) {
        checks++;
        if (!value) throw new AssertionError("condition failed");
    }

    private static void equal(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName() + " but got " + error, error);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
