/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class M3JdkShapedPrimitiveCollectionsTest {
    @Test
    void arrayListMatchesJdkAcrossDeterministicMutationSequence() {
        Random random = new Random(0x4d334c495354L);
        M3LongArrayList actual = new M3LongArrayList();
        ArrayList<Long> expected = new ArrayList<>();

        for (int step = 0; step < 10_000; step++) {
            int operation = random.nextInt(6);
            if (operation == 0 || expected.isEmpty()) {
                long value = value(random);
                assertTrue(actual.add(value));
                assertTrue(expected.add(value));
            } else if (operation == 1) {
                int index = random.nextInt(expected.size() + 1);
                long value = value(random);
                actual.add(index, value);
                expected.add(index, value);
            } else if (operation == 2) {
                int index = random.nextInt(expected.size());
                assertEquals(expected.remove(index).longValue(), actual.removeAt(index));
            } else if (operation == 3) {
                int index = random.nextInt(expected.size());
                long value = value(random);
                assertEquals(expected.set(index, value).longValue(), actual.set(index, value));
            } else if (operation == 4) {
                long value = value(random);
                assertEquals(expected.remove(Long.valueOf(value)), actual.remove(value));
            } else {
                long value = value(random);
                assertEquals(expected.contains(value), actual.contains(value));
            }
            assertEquals(expected.size(), actual.size());
            assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
        }
    }

    @Test
    void hashSetMatchesJdkAcrossCollisionsDeletionAndRehash() {
        Random random = new Random(0x4d33534554L);
        M3LongHashSet actual = new M3LongHashSet();
        HashSet<Long> expected = new HashSet<>();

        for (int step = 0; step < 30_000; step++) {
            long value = value(random);
            switch (random.nextInt(3)) {
                case 0 -> assertEquals(expected.add(value), actual.add(value));
                case 1 -> assertEquals(expected.remove(value), actual.remove(value));
                default -> assertEquals(expected.contains(value), actual.contains(value));
            }
            assertEquals(expected.size(), actual.size());
            if ((step & 255) == 0) {
                HashSet<Long> snapshot = new HashSet<>();
                for (long item : actual.toArray()) {
                    assertTrue(snapshot.add(item));
                }
                assertEquals(expected, snapshot);
            }
        }
    }

    @Test
    void hashMapMatchesJdkAcrossReplacementDeletionAndRehash() {
        Random random = new Random(0x4d334d4150L);
        M3LongLongHashMap actual = new M3LongLongHashMap();
        HashMap<Long, Long> expected = new HashMap<>();
        long missing = 0x5aa55aa55aa55aa5L;

        for (int step = 0; step < 30_000; step++) {
            long key = value(random);
            long candidate = value(random);
            switch (random.nextInt(5)) {
                case 0 -> {
                    boolean inserted = !expected.containsKey(key);
                    expected.put(key, candidate);
                    assertEquals(inserted, actual.put(key, candidate));
                }
                case 1 -> assertEquals(expected.remove(key) != null, actual.remove(key));
                case 2 -> assertEquals(
                        expected.getOrDefault(key, missing).longValue(),
                        actual.getOrDefault(key, missing));
                case 3 -> assertEquals(expected.containsKey(key), actual.containsKey(key));
                default -> {
                    if (expected.containsKey(key)) {
                        long oldValue = expected.get(key);
                        long replacement = value(random);
                        assertTrue(actual.replace(key, oldValue, replacement));
                        expected.put(key, replacement);
                    } else {
                        assertFalse(actual.replace(key, candidate, value(random)));
                    }
                }
            }
            assertEquals(expected.size(), actual.size());
            if ((step & 255) == 0) {
                Map<Long, Long> snapshot = new HashMap<>();
                actual.forEach(snapshot::put);
                assertEquals(expected, snapshot);
            }
        }
    }

    @Test
    void arrayDequeMatchesJdkOrderAndOccurrenceRemoval() {
        Random random = new Random(0x4d334445515545L);
        M3LongArrayDeque actual = new M3LongArrayDeque();
        ArrayDeque<Long> expected = new ArrayDeque<>();

        for (int step = 0; step < 20_000; step++) {
            int operation = random.nextInt(8);
            if (operation == 0 || expected.isEmpty()) {
                long value = value(random);
                actual.addFirst(value);
                expected.addFirst(value);
            } else if (operation == 1) {
                long value = value(random);
                actual.addLast(value);
                expected.addLast(value);
            } else if (operation == 2) {
                assertEquals(expected.removeFirst().longValue(), actual.removeFirstLong());
            } else if (operation == 3) {
                assertEquals(expected.removeLast().longValue(), actual.removeLastLong());
            } else if (operation == 4) {
                long value = value(random);
                assertEquals(expected.removeFirstOccurrence(value), actual.removeFirstOccurrence(value));
            } else if (operation == 5) {
                long value = value(random);
                assertEquals(expected.removeLastOccurrence(value), actual.removeLastOccurrence(value));
            } else if (operation == 6) {
                long value = value(random);
                assertEquals(expected.contains(value), actual.contains(value));
            } else {
                assertEquals(expected.getFirst().longValue(), actual.getFirst());
                assertEquals(expected.getLast().longValue(), actual.getLast());
            }
            assertEquals(expected.size(), actual.size());
            assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
        }
    }

    @Test
    void priorityQueueMatchesJdkHeadContentsAndRemoval() {
        Random random = new Random(0x4d335051L);
        M3LongPriorityQueue actual = new M3LongPriorityQueue();
        PriorityQueue<Long> expected = new PriorityQueue<>();

        for (int step = 0; step < 20_000; step++) {
            int operation = random.nextInt(5);
            if (operation == 0 || expected.isEmpty()) {
                long value = value(random);
                assertTrue(actual.offer(value));
                assertTrue(expected.offer(value));
            } else if (operation == 1) {
                assertEquals(expected.remove().longValue(), actual.removeFirstLong());
            } else if (operation == 2) {
                long value = value(random);
                assertEquals(expected.remove(value), actual.remove(value));
            } else if (operation == 3) {
                long value = value(random);
                assertEquals(expected.contains(value), actual.contains(value));
            } else {
                assertEquals(expected.element().longValue(), actual.firstLong());
            }

            assertEquals(expected.size(), actual.size());
            if (!expected.isEmpty()) {
                assertEquals(expected.element().longValue(), actual.firstLong());
            }
            long[] sortedExpected = expected.stream().mapToLong(Long::longValue).sorted().toArray();
            long[] sortedActual = actual.toSortedArray();
            assertTrue(Arrays.equals(sortedExpected, sortedActual));
        }
    }

    @Test
    void duplicateSetAddAndMapValueReplacementAreNonStructural() {
        M3LongHashSet set = new M3LongHashSet();
        set.add(7);
        M3LongIterator setIterator = set.iterator();
        assertFalse(set.add(7));
        assertTrue(setIterator.hasNext());
        assertEquals(7L, setIterator.nextLong());

        M3LongLongHashMap map = new M3LongLongHashMap();
        map.put(7, 11);
        M3LongIterator keyIterator = map.keyIterator();
        assertFalse(map.put(7, 13));
        assertTrue(keyIterator.hasNext());
        assertEquals(7L, keyIterator.nextLong());
        assertEquals(13L, map.getOrDefault(7, Long.MIN_VALUE));
    }

    @Test
    void iteratorsAreFailFastOnStructuralMutation() {
        M3LongArrayList list = new M3LongArrayList();
        list.add(1);
        M3LongIterator listIterator = list.iterator();
        list.add(2);
        assertThrows(java.util.ConcurrentModificationException.class, listIterator::hasNext);

        M3LongHashSet set = new M3LongHashSet();
        set.add(1);
        M3LongIterator setIterator = set.iterator();
        set.add(2);
        assertThrows(java.util.ConcurrentModificationException.class, setIterator::nextLong);

        M3LongArrayDeque deque = new M3LongArrayDeque();
        deque.addLast(1);
        M3LongIterator dequeIterator = deque.iterator();
        deque.addFirst(2);
        assertThrows(java.util.ConcurrentModificationException.class, dequeIterator::hasNext);

        M3LongPriorityQueue queue = new M3LongPriorityQueue();
        queue.add(1);
        M3LongIterator queueIterator = queue.iterator();
        queue.add(0);
        assertThrows(java.util.ConcurrentModificationException.class, queueIterator::nextLong);

        M3LongLongHashMap map = new M3LongLongHashMap();
        map.put(1, 1);
        M3LongIterator keyIterator = map.keyIterator();
        map.put(2, 2);
        assertThrows(java.util.ConcurrentModificationException.class, keyIterator::hasNext);
    }

    private static long value(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            default -> random.nextInt(200) - 100L;
        };
    }
}
