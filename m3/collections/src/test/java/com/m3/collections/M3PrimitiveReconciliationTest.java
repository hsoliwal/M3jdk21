/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** Cross-contract checks for the current primitive owners and the restored donor APIs. */
final class M3PrimitiveReconciliationTest {
    @Test
    void listRetainsCurrentBooleanMutationsAndZeroCapacityThroughSequenceInterface() {
        M3LongArrayList list = new M3LongArrayList(0);
        M3LongSequence sequence = list;
        assertEquals(0, list.capacity());
        assertEquals(0, new M3LongArrayList().capacity());
        assertEquals(0, M3LongArrayList.copyOf(new long[0]).capacity());
        assertFalse(sequence.addAll(new long[0]));
        assertEquals(0, list.capacity());
        assertTrue(sequence.add(2));
        assertTrue(sequence.addAll(new long[] {4, 6}));
        sequence.insert(0, 1);
        list.add(2, 3);
        assertTrue(sequence.removeValue(6));
        assertTrue(list.remove(4));
        assertArrayEquals(new long[] {1, 2, 3}, list.toArray());
        M3LongStack stack = list;
        stack.push(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, stack.peek());
        assertEquals(Long.MIN_VALUE, stack.pop());
        sequence.clear();
        list.trimToSize();
        assertEquals(0, list.capacity());
        assertThrows(NoSuchElementException.class, stack::peek);
        assertThrows(NoSuchElementException.class, stack::pop);
    }

    @Test
    void addedListMutationsParticipateInCurrentFailFastIteration() {
        assertInvalidates(list -> list.insert(0, 9));
        assertInvalidates(list -> list.push(9));
        assertInvalidates(M3LongArrayList::pop);
        assertInvalidates(list -> list.removeValue(1));
        assertInvalidates(M3LongArrayList::sort);
        assertInvalidates(list -> list.filterInPlace(value -> value != 1));
        M3LongArrayList duplicate = M3LongArrayList.copyOf(new long[] {1, 1, 2});
        M3LongIterator duplicateIterator = duplicate.iterator();
        assertEquals(1, duplicate.deduplicateSorted());
        assertThrows(ConcurrentModificationException.class, duplicateIterator::nextLong);

        M3LongArrayList unchanged = M3LongArrayList.copyOf(new long[] {1, 2, 3});
        M3LongIterator cursor = unchanged.iterator();
        assertFalse(unchanged.addIf(9, value -> false));
        assertFalse(unchanged.addAll(new long[0]));
        assertFalse(unchanged.removeValue(9));
        assertEquals(0, unchanged.deduplicateSorted());
        assertEquals(0, unchanged.filterInPlace(value -> true));
        unchanged.ensureCapacity(128);
        unchanged.set(0, 7);
        assertEquals(7, cursor.nextLong());
        assertEquals(2, cursor.nextLong());
        assertEquals(3, cursor.nextLong());
        assertFalse(cursor.hasNext());
    }

    @Test
    void listFilterFailurePreservesDonorSuffixAndInvalidatesCurrentIterator() {
        M3LongArrayList list = M3LongArrayList.copyOf(new long[] {1, 2, 3, 4, 5});
        M3LongIterator cursor = list.iterator();
        RuntimeException failure = new IllegalStateException("predicate failure");
        assertSame(failure, assertThrows(RuntimeException.class, () -> list.filterInPlace(value -> {
            if (value == 4) {
                throw failure;
            }
            return (value & 1L) == 0L;
        })));
        assertArrayEquals(new long[] {2, 4, 5}, list.toArray());
        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
        list.sort();
        assertEquals(1, list.binarySearch(4));
        assertArrayEquals(new long[] {2, 4, 5}, list.parallelLongStream().toArray());
    }

    @Test
    void listSplitsFollowRelocatedStorageAndRetainFailFastTraversal() {
        M3LongArrayList list = M3LongArrayList.copyOf(new long[] {1, 2, 3, 4, 5, 6});
        Spliterator.OfLong right = list.longSpliterator();
        Spliterator.OfLong left = right.trySplit();
        assertNotNull(left);
        assertTrue(right.hasCharacteristics(Spliterator.ORDERED | Spliterator.SIZED
                | Spliterator.SUBSIZED | Spliterator.NONNULL));
        list.ensureCapacity(4096);
        list.set(0, 9);
        LongStream.Builder traversal = LongStream.builder();
        left.forEachRemaining((long value) -> traversal.add(value));
        right.forEachRemaining((long value) -> traversal.add(value));
        assertArrayEquals(new long[] {9, 2, 3, 4, 5, 6}, traversal.build().toArray());

        LongStream staleStream = list.longStream();
        list.add(7);
        assertThrows(ConcurrentModificationException.class, staleStream::toArray);
        Spliterator.OfLong callback = list.longSpliterator();
        assertThrows(ConcurrentModificationException.class,
                () -> callback.tryAdvance((long value) -> list.add(value)));
        assertArrayEquals(list.toArray(), list.parallelLongStream().toArray());
    }

    @Test
    void dequeCurrentAndDonorNamesShareOneRingAcrossWrapGrowthAndOccurrenceRemoval() {
        M3LongArrayDeque deque = new M3LongArrayDeque(0);
        M3LongDeque donor = deque;
        ArrayDeque<Long> oracle = new ArrayDeque<>();
        assertEquals(8, deque.capacity());
        for (long value = 0; value < 48; value++) {
            assertTrue(donor.offer(value));
            oracle.offer(value);
        }
        for (int i = 0; i < 32; i++) {
            assertEquals(oracle.removeFirst().longValue(), donor.poll());
        }
        for (long value = 48; value < 128; value++) {
            assertTrue(deque.offerLast(value));
            oracle.offerLast(value);
        }
        donor.addFirst(77);
        oracle.addFirst(77L);
        donor.addLast(77);
        oracle.addLast(77L);
        assertEquals(oracle.removeFirstOccurrence(77L), deque.removeFirstOccurrence(77));
        assertEquals(oracle.removeLastOccurrence(77L), deque.removeLastOccurrence(77));
        assertEquals(oracle.getFirst().longValue(), donor.first());
        assertEquals(donor.first(), deque.getFirst());
        assertEquals(oracle.getLast().longValue(), donor.last());
        assertEquals(donor.last(), deque.getLast());
        assertEquals(oracle.removeFirst().longValue(), donor.removeFirst());
        assertEquals(oracle.removeLast().longValue(), donor.removeLast());
        long[] expected = oracle.stream().mapToLong(Long::longValue).toArray();
        assertArrayEquals(expected, deque.toArray());
        assertArrayEquals(expected, donor.parallelLongStream().toArray());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], donor.get(i));
        }
        ArrayList<Long> descending = new ArrayList<>();
        M3LongIterator cursor = deque.descendingIterator();
        while (cursor.hasNext()) {
            descending.add(cursor.nextLong());
        }
        assertEquals(new ArrayList<>(oracle).reversed(), descending);
        cursor = deque.descendingIterator();
        M3LongIterator stale = cursor;
        assertTrue(donor.addLastIf(-1, value -> value < 0));
        assertThrows(ConcurrentModificationException.class, stale::hasNext);
    }

    @Test
    void dequeFilterKeepsDonorFailurePublicationAndCurrentFailFastBehavior() {
        M3LongArrayDeque deque = new M3LongArrayDeque();
        for (long value = 1; value <= 5; value++) {
            deque.addLast(value);
        }
        M3LongIterator cursor = deque.iterator();
        RuntimeException failure = new IllegalArgumentException("predicate failure");
        assertSame(failure, assertThrows(RuntimeException.class, () -> deque.filterInPlace(value -> {
            if (value == 3) {
                throw failure;
            }
            return value == 2;
        })));
        assertArrayEquals(new long[] {2}, deque.toArray());
        assertThrows(ConcurrentModificationException.class, cursor::nextLong);
        assertThrows(NullPointerException.class, () -> deque.filterInPlace(null));
        assertArrayEquals(new long[] {2}, deque.toArray());
        deque.clear();
        assertThrows(NoSuchElementException.class, deque::poll);
        assertThrows(NoSuchElementException.class, deque::peek);
        assertThrows(NoSuchElementException.class, deque::removeFirstLong);
        assertThrows(NoSuchElementException.class, deque::removeLast);
    }

    @Test
    void newFactoriesExposeTheUnchangedPrimitiveHashAndHeapOwners() {
        M3LongHashSet set = M3Collections.longHashSet();
        M3LongLongHashMap map = M3Collections.longLongHashMap();
        M3LongPriorityQueue queue = M3Collections.longPriorityQueue();
        assertEquals(M3LongHashSet.class, set.getClass());
        assertEquals(M3LongLongHashMap.class, map.getClass());
        assertEquals(M3LongPriorityQueue.class, queue.getClass());
        for (long value : new long[] {Long.MAX_VALUE, 0, Long.MIN_VALUE}) {
            assertTrue(set.add(value));
            assertFalse(set.add(value));
            assertTrue(map.put(value, value));
            assertFalse(map.put(value, -value));
            assertEquals(-value, map.getOrDefault(value, 8));
            assertTrue(queue.offer(value));
        }
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, queue.toSortedArray());
        assertEquals(Long.MIN_VALUE, queue.removeFirstLong());
        assertEquals(0, queue.removeFirstLong());
        assertEquals(Long.MAX_VALUE, queue.removeFirstLong());
    }

    private static void assertInvalidates(Consumer<M3LongArrayList> operation) {
        M3LongArrayList list = M3LongArrayList.copyOf(new long[] {3, 1, 1, 2});
        M3LongIterator cursor = list.iterator();
        operation.accept(list);
        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
    }
}
