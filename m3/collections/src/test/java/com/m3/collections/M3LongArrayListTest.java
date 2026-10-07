/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

/**
 * Finite primitive-list contract comparisons for the union owner; this is not JDK List
 * replacement proof. Adapted from the Synexia {@code PackedLongList} lineage test: the union keeps
 * the JDK-shaped growth geometry (shared empty backing, default capacity 10), zeroing on removal
 * and fail-fast iteration, so {@code trimToSize} invalidates live iterators and an emptied,
 * trimmed list reports capacity 0.
 */
final class M3LongArrayListTest {
    @Test
    void mixedOperationsMatchJdkValueOrder() {
        for (int seed = 0; seed < 32; seed++) {
            var random = new Random(seed);
            var expected = new ArrayList<Long>();
            var actual = new M3LongArrayList(seed % 9);
            for (int step = 0; step < 1024; step++) {
                long value = switch (step % 11) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(41) - 20;
                };
                int operation = random.nextInt(9);
                if (expected.size() > 128) operation = 6;
                switch (operation) {
                    case 0 -> { expected.add(value); assertTrue(actual.add(value)); }
                    case 1 -> {
                        int index = random.nextInt(expected.size() + 1);
                        expected.add(index, value);
                        actual.insert(index, value);
                    }
                    case 2 -> {
                        if (!expected.isEmpty()) {
                            int index = random.nextInt(expected.size());
                            assertEquals(expected.set(index, value).longValue(), actual.set(index, value));
                        }
                    }
                    case 3 -> {
                        if (!expected.isEmpty()) {
                            int index = random.nextInt(expected.size());
                            assertEquals(expected.remove(index).longValue(), actual.removeAt(index));
                        }
                    }
                    case 4 -> assertEquals(expected.remove(Long.valueOf(value)), actual.removeValue(value));
                    case 5 -> {
                        long[] additions = {value, ~value};
                        expected.add(value);
                        expected.add(~value);
                        assertTrue(actual.addAll(additions));
                        additions[0] ^= 123; // imported mutable array is not retained
                    }
                    case 6 -> { expected.clear(); actual.clear(); }
                    case 7 -> {
                        assertEquals(expected.indexOf(value), actual.indexOf(value));
                        assertEquals(expected.lastIndexOf(value), actual.lastIndexOf(value));
                        assertEquals(expected.contains(value), actual.contains(value));
                    }
                    default -> actual.trimToSize();
                }
                assertEquals(expected.size(), actual.size());
                assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
            }
        }
    }

    @Test
    void callbackFailurePreservesThrowingValueAndUnreadSuffix() {
        for (int failAt = 0; failAt < 16; failAt++) {
            long[] values = java.util.stream.LongStream.range(0, 16).toArray();
            var list = M3LongArrayList.copyOf(values);
            var seen = new ArrayList<Long>();
            var expected = new ArrayList<Long>();
            for (int i = 0; i < failAt; i++) if ((i & 1) == 0) expected.add((long) i);
            for (int i = failAt; i < 16; i++) expected.add((long) i);
            int point = failAt;
            var failure = new IllegalStateException("predicate");
            assertSame(failure, assertThrows(IllegalStateException.class, () -> list.filterInPlace(v -> {
                seen.add(v);
                if (v == point) throw failure;
                return (v & 1) == 0;
            })));
            assertArrayEquals(java.util.stream.LongStream.rangeClosed(0, failAt).toArray(),
                    seen.stream().mapToLong(Long::longValue).toArray());
            assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), list.toArray());
        }
        var list = M3LongArrayList.copyOf(new long[]{1, 2, 3});
        assertThrows(NullPointerException.class, () -> list.filterInPlace(null));
        assertArrayEquals(new long[]{1, 2, 3}, list.toArray());
        assertEquals(2, list.filterInPlace(v -> v == 2));
        assertArrayEquals(new long[]{2}, list.toArray());
        assertFalse(list.addIf(4, v -> false));
        assertTrue(list.addIf(4, v -> true));
        assertArrayEquals(new long[]{2, 4}, list.toArray());
    }

    @Test
    void directStreamsRetainOwnerAcrossStorageRelocation() {
        var list = M3LongArrayList.copyOf(new long[]{5, 6, 7, 8});
        var iterator = list.iterator();
        var split = list.longSpliterator();
        var prefix = split.trySplit();
        assertNotNull(prefix);
        list.ensureCapacity(4096);
        list.set(0, 15);
        list.set(3, 18);
        var seen = new ArrayList<Long>();
        prefix.forEachRemaining((long v) -> seen.add(v));
        split.forEachRemaining((long v) -> seen.add(v));
        assertEquals(List.of(15L, 6L, 7L, 18L), seen);
        assertEquals(15, iterator.nextLong());
        list.trimToSize();
        // JDK shape: trimToSize is structural, so the live iterator fails fast.
        assertThrows(ConcurrentModificationException.class, iterator::nextLong);
        var fresh = list.iterator();
        assertEquals(15, fresh.nextLong());
        assertEquals(6, fresh.nextLong());
        assertEquals(46, list.parallelLongStream().sum());
        assertArrayEquals(new long[]{15, 6, 7, 18}, list.longStream().toArray());
        assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
                list.longSpliterator().characteristics());
        assertThrows(NullPointerException.class,
                () -> list.longSpliterator().tryAdvance((java.util.function.LongConsumer) null));
        assertThrows(NullPointerException.class,
                () -> list.longSpliterator().forEachRemaining((java.util.function.LongConsumer) null));
    }

    @Test
    void sortDedupAndSearchPreserveSignedLongSemantics() {
        for (int seed = 0; seed < 64; seed++) {
            Random random = new Random(seed);
            long[] data = new long[seed * 3];
            for (int i = 0; i < data.length; i++) {
                data[i] = i % 5 == 0 ? Long.MIN_VALUE
                        : i % 7 == 0 ? Long.MAX_VALUE : random.nextInt(17) - 8;
            }
            var list = M3LongArrayList.copyOf(data);
            Arrays.sort(data);
            list.sort();
            assertArrayEquals(data, list.toArray());
            for (long query : new long[]{Long.MIN_VALUE, -9, -1, 0, 3, 9, Long.MAX_VALUE}) {
                assertEquals(Arrays.binarySearch(data, query), list.binarySearch(query));
            }
            long[] distinct = Arrays.stream(data).distinct().toArray();
            assertEquals(data.length - distinct.length, list.deduplicateSorted());
            assertArrayEquals(distinct, list.toArray());
        }
    }

    @Test
    void structuralMutationsFailFastAndStackIsJdkShapedRemoval() {
        var list = M3LongArrayList.copyOf(new long[]{1, 2, 3, 4});
        var iterator = list.iterator();
        assertEquals(1, iterator.nextLong());
        list.sort();
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        var dedup = M3LongArrayList.copyOf(new long[]{1, 1, 2});
        var dedupIterator = dedup.iterator();
        assertEquals(1, dedup.deduplicateSorted());
        assertThrows(ConcurrentModificationException.class, dedupIterator::hasNext);
        var filtered = M3LongArrayList.copyOf(new long[]{1, 2, 3});
        var filteredIterator = filtered.iterator();
        assertEquals(0, filtered.filterInPlace(v -> true));
        assertTrue(filteredIterator.hasNext()); // no structural change, no fail-fast
        assertEquals(2, filtered.filterInPlace(v -> v == 2));
        assertThrows(ConcurrentModificationException.class, filteredIterator::hasNext);
        var stack = new M3LongArrayList();
        stack.push(7);
        stack.push(9);
        var stackIterator = stack.iterator();
        assertEquals(9, stack.peek());
        assertEquals(9, stack.pop());
        assertThrows(ConcurrentModificationException.class, stackIterator::hasNext);
        assertEquals(7, stack.pop());
        assertEquals(0, stack.size());
    }

    @Test
    void boundsOwnershipAndStackExceptions() {
        assertThrows(IllegalArgumentException.class, () -> new M3LongArrayList(-1));
        assertThrows(NullPointerException.class, () -> M3LongArrayList.copyOf(null));
        var empty = M3Collections.longArrayList();
        assertThrows(NoSuchElementException.class, empty::peek);
        assertThrows(NoSuchElementException.class, empty::pop);
        assertThrows(NoSuchElementException.class, () -> empty.iterator().nextLong());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.insert(1, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.add(1, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(-1, 9));
        assertThrows(NullPointerException.class, () -> empty.addAll(null));
        long[] source = {Long.MIN_VALUE, Long.MAX_VALUE};
        var list = M3LongArrayList.copyOf(source);
        source[0] = 0;
        assertEquals(Long.MIN_VALUE, list.get(0));
        long[] exported = list.toArray();
        exported[0] = 0;
        assertEquals(Long.MIN_VALUE, list.get(0));
        list.push(7);
        assertEquals(7, list.peek());
        assertEquals(7, list.pop());
        assertArrayEquals(new long[]{Long.MIN_VALUE, Long.MAX_VALUE}, list.toArray());
    }

    @Test
    void primitiveStorageAndCapacityChecksStayBounded() throws Exception {
        var fields = Arrays.stream(M3LongArrayList.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(java.lang.reflect.Field::getType)
                .toList();
        assertEquals(List.of(long[].class, int.class, int.class), fields);
        assertThrows(OutOfMemoryError.class, () -> new M3LongArrayList().ensureCapacity(-1));
        var list = new M3LongArrayList(8192);
        list.add(3);
        list.add(4);
        var storage = M3LongArrayList.class.getDeclaredField("elements");
        storage.setAccessible(true);
        Object old = storage.get(list);
        list.trimToSize();
        assertNotSame(old, storage.get(list));
        assertEquals(2, ((long[]) storage.get(list)).length);
        assertArrayEquals(new long[]{3, 4}, list.toArray());
        list.clear();
        list.trimToSize();
        assertEquals(0, list.capacity());
        assertEquals(10, M3Collections.longArrayList(10).capacity());
    }
}
