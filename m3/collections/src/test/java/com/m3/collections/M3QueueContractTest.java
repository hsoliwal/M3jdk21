// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;
import java.util.ConcurrentModificationException;
import org.junit.jupiter.api.Test;

class M3QueueContractTest {
    @Test void dequeCircularOffsetsAndJdkDifferential() {
        ArrayDeque<Integer> oracle = new ArrayDeque<>();
        M3ArrayDeque<Integer> actual = new M3ArrayDeque<>();
        assertEquals(0, actual.capacity());
        assertNull(actual.pollFirst());
        assertNull(actual.pollLast());
        assertThrows(NullPointerException.class, () -> actual.addFirst(null));
        Random random = new Random(20261325);

        for (int step = 0; step < 18000; step++) {
            int value = random.nextInt(41) - 20;
            int op = random.nextInt(10);
            Object expected, found;
            switch (op) {
                case 0 -> { oracle.addFirst(value); actual.addFirst(value); expected = null; found = null; }
                case 1 -> { oracle.addLast(value); actual.addLast(value); expected = null; found = null; }
                case 2 -> { expected = oracle.pollFirst(); found = actual.pollFirst(); }
                case 3 -> { expected = oracle.pollLast(); found = actual.pollLast(); }
                case 4 -> { expected = oracle.peekFirst(); found = actual.peekFirst(); }
                case 5 -> { expected = oracle.peekLast(); found = actual.peekLast(); }
                case 6 -> { expected = oracle.removeFirstOccurrence(value); found = actual.removeFirstOccurrence(value); }
                case 7 -> { expected = oracle.removeLastOccurrence(value); found = actual.removeLastOccurrence(value); }
                case 8 -> { expected = oracle.contains(value); found = actual.contains(value); }
                default -> { expected = oracle.size(); found = actual.size(); }
            }
            assertEquals(expected, found, "deque step " + step);
            if (step % 29 == 0) {
                assertEquals(new ArrayList<>(oracle), new ArrayList<>(actual));
                assertArrayEquals(oracle.toArray(), actual.toArray());
                assertEquals(oracle.peekFirst(), actual.peekFirst());
                assertEquals(oracle.peekLast(), actual.peekLast());
            }
        }
        assertEquals(new ArrayList<>(oracle), new ArrayList<>(actual.clone()));
        Iterator<Integer> reverse = actual.descendingIterator();
        Iterator<Integer> reverseOracle = oracle.descendingIterator();
        while (reverseOracle.hasNext()) {
            assertEquals(reverseOracle.next(), reverse.next());
        }
        assertFalse(reverse.hasNext());
        if (!actual.isEmpty()) {
            Iterator<Integer> it = actual.iterator();
            Integer removed = it.next();
            it.remove();
            assertEquals(actual.size() + 1, oracle.size());
            assertThrows(IllegalStateException.class, it::remove);
            assertFalse(actual.toString().isEmpty());
        }
    }

    @Test void dequeIteratorMutationsAndFailFast() {
        M3ArrayDeque<Integer> q = new M3ArrayDeque<>();
        for (int v = 0; v < 10; v++) q.addLast(v);
        var asc = q.iterator();
        assertEquals(0, asc.next());
        assertEquals(1, asc.next());
        asc.remove();
        assertEquals(List.of(0,2,3,4,5,6,7,8,9), new ArrayList<>(q));
        assertEquals(2, asc.next());

        var desc = q.descendingIterator();
        assertEquals(9, desc.next());
        assertEquals(8, desc.next());
        desc.remove();
        assertEquals(List.of(0,2,3,4,5,6,7,9), new ArrayList<>(q));
        assertEquals(7, desc.next());

        var stale = q.iterator();
        q.addFirst(12);
        assertThrows(ConcurrentModificationException.class, stale::next);
        assertArrayEquals(new Integer[] {12,0,2,3,4,5,6,7,9},
                q.toArray(new Integer[0]));
        q.clear();
        assertEquals(0, q.size());
        assertNull(q.peekLast());
    }

    @Test void priorityQueueHeapOffsetsAndJdkDifferential() {
        PriorityQueue<Integer> oracle = new PriorityQueue<>();
        M3PriorityQueue<Integer> actual = new M3PriorityQueue<>();
        assertEquals(0, actual.capacity());
        assertThrows(NullPointerException.class, () -> actual.offer(null));
        Random random = new Random(20261019);

        for (int step = 0; step < 17000; step++) {
            int value = random.nextInt(70) - 35;
            int op = random.nextInt(7);
            Object expected, found;
            switch (op) {
                case 0, 1, 2 -> { expected = oracle.offer(value); found = actual.offer(value); }
                case 3 -> { expected = oracle.poll(); found = actual.poll(); }
                case 4 -> { expected = oracle.peek(); found = actual.peek(); }
                case 5 -> { expected = oracle.remove(value); found = actual.remove(value); }
                default -> { expected = oracle.contains(value); found = actual.contains(value); }
            }
            assertEquals(expected, found, "priority step " + step);
            if (step % 43 == 0) {
                assertEquals(oracle.size(), actual.size());
                assertEquals(oracle.peek(), actual.peek());
                Integer[] o = oracle.toArray(new Integer[0]);
                Integer[] a = actual.toArray(new Integer[0]);
                Arrays.sort(o);
                Arrays.sort(a);
                assertArrayEquals(o, a);
                actual.verifyInvariants();
            }
        }
        actual.verifyInvariants();
        PriorityQueue<Integer> sortedOracle = new PriorityQueue<>(oracle);
        M3PriorityQueue<Integer> copy = actual.clone();
        while (!sortedOracle.isEmpty()) assertEquals(sortedOracle.poll(), copy.poll());
        assertTrue(copy.isEmpty());
    }

    @Test void comparatorCarryoverIteratorProjectionAndIdentityRemoval() {
        TreeSet<Integer> reverse = new TreeSet<>(Comparator.reverseOrder());
        reverse.addAll(List.of(7, 1, 9, 3));
        M3PriorityQueue<Integer> q = new M3PriorityQueue<>(reverse);
        assertEquals(9, q.peek());
        assertEquals(List.of(9, 7, 3, 1),
                List.of(q.poll(), q.poll(), q.poll(), q.poll()));

        q = new M3PriorityQueue<>();
        q.offer(8);
        q.offer(7);
        q.offer(9);
        Iterator<Integer> iterator = q.iterator();
        Integer removed = iterator.next();
        iterator.remove();
        assertFalse(q.contains(removed));
        q.verifyInvariants();

        Iterator<Integer> stale = q.iterator();
        q.offer(1);
        assertThrows(ConcurrentModificationException.class, stale::next);
    }
}
