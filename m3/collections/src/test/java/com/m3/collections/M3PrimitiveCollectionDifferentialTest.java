/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Primitive contracts use the JDK's boxed/array results as independent value oracles. */
final class M3PrimitiveCollectionDifferentialTest {
    private static long[] values(Collection<Long> values) {
        return values.stream().mapToLong(Long::longValue).toArray();
    }
    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(61) - 30L;
        };
    }

    @Test void primitiveListMatchesJdkAcrossGrowthCompactionSortingAndStreams() {
        var actual = new M3LongArrayList(0); var expected = new ArrayList<Long>();
        Random random = new Random(0x4d334c495354L);
        for (int step = 0; step < 4_000; step++) {
            long v = value(random, step); int index = random.nextInt(expected.size() + 1);
            switch (random.nextInt(12)) {
                case 0 -> { actual.add(v); expected.add(v); }
                case 1 -> { actual.insert(index, v); expected.add(index, v); }
                case 2 -> {
                    if (index < expected.size()) { assertEquals(expected.set(index, v).longValue(), actual.set(index, v)); }
                }
                case 3 -> {
                    if (index < expected.size()) { assertEquals(expected.remove(index).longValue(), actual.removeAt(index)); }
                }
                case 4 -> assertEquals(expected.remove(v), actual.removeValue(v));
                case 5 -> {
                    long[] added = {v, -v, 0}; actual.addAll(added);
                    for (long item : added) { expected.add(item); }
                }
                case 6 -> {
                    actual.sort(); expected.sort(Long::compare);
                    assertEquals(Arrays.binarySearch(values(expected), v), actual.binarySearch(v));
                }
                case 7 -> {
                    int before = expected.size(); expected.removeIf(x -> (x & 1L) != 0);
                    assertEquals(before - expected.size(), actual.filterInPlace(x -> (x & 1L) == 0));
                }
                case 8 -> { actual.trimToSize(); assertTrue(actual.capacity() >= actual.size()); }
                case 9 -> {
                    boolean accept = v >= 0; assertEquals(accept, actual.addIf(v, x -> x >= 0));
                    if (accept) { expected.add(v); }
                }
                case 10 -> {
                    actual.push(v); expected.add(v);
                    assertEquals(expected.getLast().longValue(), actual.peek());
                    assertEquals(expected.removeLast().longValue(), actual.pop());
                }
                default -> {
                    if (expected.size() > 80) { expected.clear(); actual.clear(); }
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.indexOf(v), actual.indexOf(v));
            assertEquals(expected.lastIndexOf(v), actual.lastIndexOf(v));
            assertArrayEquals(values(expected), actual.toArray(), "step " + step);
            if (step % 47 == 0) {
                assertArrayEquals(values(expected), actual.longStream().toArray());
                assertArrayEquals(values(expected), actual.parallelLongStream().toArray());
                assertEquals(expected.stream().mapToLong(Long::longValue).sum(), actual.parallelLongStream().sum());
            }
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.insert(actual.size() + 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new M3LongArrayList(-1));
    }

    @Test void primitiveListSnapshotFilterFailureAndOwnerBoundStream() {
        long[] input = {1, 2, 3, 4, 5}; var actual = M3LongArrayList.copyOf(input);
        input[0] = 99; assertEquals(1, actual.get(0));
        var stream = actual.longStream(); actual.ensureCapacity(4096); actual.set(0, 8);
        assertArrayEquals(new long[] {8, 2, 3, 4, 5}, stream.toArray());
        RuntimeException sentinel = new IllegalStateException("predicate");
        assertSame(sentinel, assertThrows(RuntimeException.class, () -> actual.filterInPlace(v -> {
            if (v == 4) { throw sentinel; } return v % 2 == 0;
        })));
        assertArrayEquals(new long[] {8, 2, 4, 5}, actual.toArray());
        actual.addAll(new long[] {2, 8, 8}); actual.sort();
        long[] unique = actual.longStream().distinct().toArray(); int before = actual.size();
        assertEquals(before - unique.length, actual.deduplicateSorted());
        assertArrayEquals(unique, actual.toArray());
    }

    @Test void primitiveDequeMatchesJdkAcrossWrapGrowthReuseAndStreams() {
        var actual = new M3LongArrayDeque(0); var expected = new ArrayDeque<Long>();
        Random random = new Random(0x4d334445515545L);
        for (int step = 0; step < 6_000; step++) {
            long v = value(random, step);
            switch (random.nextInt(9)) {
                case 0 -> { actual.addFirst(v); expected.addFirst(v); }
                case 1 -> { actual.addLast(v); expected.addLast(v); }
                case 2 -> {
                    if (!expected.isEmpty()) { assertEquals(expected.removeFirst().longValue(), actual.removeFirst()); }
                }
                case 3 -> {
                    if (!expected.isEmpty()) { assertEquals(expected.removeLast().longValue(), actual.removeLast()); }
                }
                case 4 -> { assertEquals(expected.offer(v), actual.offer(v)); }
                case 5 -> {
                    int before = expected.size(); expected.removeIf(x -> (x & 3L) == 1L);
                    assertEquals(before - expected.size(), actual.filterInPlace(x -> (x & 3L) != 1L));
                }
                case 6 -> {
                    boolean accept = v < 0; assertEquals(accept, actual.addLastIf(v, x -> x < 0));
                    if (accept) { expected.addLast(v); }
                }
                case 7 -> {
                    // Force repeated ring wrap and then a resize from a nonzero head.
                    for (int i = 0; i < 17; i++) { actual.addLast(v + i); expected.addLast(v + i); }
                    for (int i = 0; i < 11; i++) { assertEquals(expected.removeFirst().longValue(), actual.removeFirst()); }
                }
                default -> { if (expected.size() > 90) { expected.clear(); actual.clear(); } }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.contains(v), actual.contains(v));
            assertArrayEquals(values(expected), actual.toArray(), "step " + step);
            if (!expected.isEmpty()) {
                assertEquals(expected.getFirst().longValue(), actual.first());
                assertEquals(expected.getLast().longValue(), actual.last());
            }
            if (step % 47 == 0) {
                assertArrayEquals(values(expected), actual.longStream().toArray());
                assertArrayEquals(values(expected), actual.parallelLongStream().toArray());
            }
        }
        actual.clear();
        // Primitive poll has no null sentinel: preserve its established throwing contract.
        assertThrows(NoSuchElementException.class, actual::poll);
        assertThrows(NoSuchElementException.class, actual::peek);
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(0));
        assertThrows(IllegalArgumentException.class, () -> new M3LongArrayDeque(-1));
    }
}
