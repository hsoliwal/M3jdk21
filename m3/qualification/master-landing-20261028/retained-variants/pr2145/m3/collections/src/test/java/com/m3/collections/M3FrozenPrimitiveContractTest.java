/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Three independent original-JDK primitive immutability and ordering oracles. */
final class M3FrozenPrimitiveContractTest {
    @Test
    void frozenIntAndLongListsRetainDuplicatesAndDetachedOwnership() {
        Random random = new Random(0x4d3346524f5a4e31L);
        for (int seed = 0; seed < 192; seed++) {
            int size = seed * 31 % 127;
            int[] ints = new int[size];
            long[] longs = new long[size];
            List<Integer> boxedInts = new ArrayList<>();
            List<Long> boxedLongs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int v = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(31) - 15;
                };
                long w = switch (i % 7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(31) - 15L;
                };
                ints[i] = v;
                longs[i] = w;
                boxedInts.add(v);
                boxedLongs.add(w);
            }
            List<Integer> iOracle = List.copyOf(boxedInts);
            List<Long> lOracle = List.copyOf(boxedLongs);
            M3FrozenIntList iOwner = M3FrozenIntList.copyOf(ints);
            M3FrozenLongList lOwner = M3FrozenLongList.copyOf(longs);
            int[] iBefore = ints.clone();
            long[] lBefore = longs.clone();
            Arrays.fill(ints, 17);
            Arrays.fill(longs, 17L);
            assertArrayEquals(iBefore, iOwner.toArray());
            assertArrayEquals(lBefore, lOwner.toArray());
            assertEquals(size, iOwner.size());
            assertEquals(size, lOwner.size());
            assertEquals(size == 0, lOwner.isEmpty());
            for (int i = 0; i < size; i++) {
                assertEquals(iOracle.get(i).intValue(), iOwner.get(i));
                assertEquals(lOracle.get(i).longValue(), lOwner.get(i));
                assertEquals(iOracle.indexOf(iBefore[i]), iOwner.indexOf(iBefore[i]));
                assertEquals(lOracle.indexOf(lBefore[i]), lOwner.indexOf(lBefore[i]));
                assertTrue(lOwner.contains(lBefore[i]));
            }
            var intObserved = new ArrayList<Integer>();
            var longObserved = new ArrayList<Long>();
            iOwner.forEach((int v) -> intObserved.add(v));
            lOwner.forEach((long v) -> longObserved.add(v));
            assertEquals(iOracle, intObserved);
            assertEquals(lOracle, longObserved);
            intObserved.clear();
            longObserved.clear();
            iOwner.iterator().forEachRemaining((int v) -> intObserved.add(v));
            lOwner.iterator().forEachRemaining((long v) -> longObserved.add(v));
            assertEquals(iOracle, intObserved);
            assertEquals(lOracle, longObserved);
            if (size > 1) {
                int start = size / 3, end = size - size / 4;
                M3FrozenLongList view = lOwner.subList(start, end);
                assertArrayEquals(lOracle.subList(start, end).stream()
                        .mapToLong(Long::longValue).toArray(), view.toArray());
                assertArrayEquals(view.subList(1, view.size()).toArray(),
                        lOracle.subList(start + 1, end).stream()
                                .mapToLong(Long::longValue).toArray());
            }
        }
    }

    @Test
    void frozenSortedLongSetMatchesTreeSetAndBinarySearchRanks() {
        Random random = new Random(0x4d3346524f5a4e53L);
        for (int seed = 0; seed < 192; seed++) {
            long[] source = new long[seed * 29 % 137];
            var oracle = new TreeSet<Long>();
            for (int i = 0; i < source.length; i++) {
                source[i] = switch (i % 9) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    default -> random.nextInt(39) - 19L;
                };
                oracle.add(source[i]);
            }
            long[] expected = oracle.stream().mapToLong(Long::longValue).toArray();
            M3FrozenLongSet owner = M3FrozenLongSet.copyOf(source);
            Arrays.fill(source, 900);
            assertArrayEquals(expected, owner.toArray());
            assertEquals(expected.length, owner.size());
            assertEquals(expected.length == 0, owner.isEmpty());
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], owner.get(i));
                assertEquals(i, owner.rank(expected[i]));
            }
            for (long q : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -19, -1, 0, 7, 43}) {
                assertEquals(oracle.contains(q), owner.contains(q));
                if (!oracle.contains(q)) assertEquals(-1, owner.rank(q));
            }
            if (oracle.isEmpty()) {
                assertThrows(NoSuchElementException.class, owner::first);
                assertThrows(NoSuchElementException.class, owner::last);
            } else {
                assertEquals(oracle.first().longValue(), owner.first());
                assertEquals(oracle.last().longValue(), owner.last());
            }
            long[] observed = new long[owner.size()];
            int[] count = {0};
            owner.iterator().forEachRemaining((long v) -> observed[count[0]++] = v);
            assertArrayEquals(expected, observed);
        }
    }

    @Test
    void primitiveListsAndSetRejectMutationAndInvalidRanges() throws Exception {
        M3FrozenIntList ints = M3FrozenIntList.copyOf(new int[]{3, 3, 1});
        M3FrozenLongList longs = M3FrozenLongList.copyOf(new long[]{3, 3, 1});
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(new long[]{3, 3, 1});
        assertThrows(UnsupportedOperationException.class, () -> ints.add(4));
        assertThrows(UnsupportedOperationException.class, () -> longs.add(4));
        assertThrows(UnsupportedOperationException.class, () -> longs.set(0, 4));
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(3));
        assertThrows(UnsupportedOperationException.class, () -> ints.iterator().remove());
        assertThrows(UnsupportedOperationException.class, () -> longs.iterator().remove());
        assertThrows(UnsupportedOperationException.class, () -> set.iterator().remove());
        assertThrows(IndexOutOfBoundsException.class, () -> ints.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> longs.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> longs.subList(2, 1));
        assertThrows(NullPointerException.class, () -> M3FrozenLongSet.copyOf(null));
        assertThrows(NullPointerException.class, () -> M3FrozenLongList.copyOf(null));
        assertThrows(NullPointerException.class, () -> M3FrozenIntList.copyOf(null));
        assertThrows(NullPointerException.class, () -> ints.forEach(null));
        assertThrows(NullPointerException.class, () -> longs.forEach(null));
        assertThrows(NullPointerException.class, () -> set.forEach(null));
        for (Class<?> type : new Class<?>[]{M3FrozenLongList.class,
                M3FrozenIntList.class, M3FrozenLongSet.class}) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(Modifier.isFinal(field.getModifiers()), field.getName());
                assertTrue(field.getType().isPrimitive() ||
                        field.getType() == long[].class ||
                        field.getType() == int[].class, field.getName());
            }
        }
        Field payload = M3FrozenLongList.class.getDeclaredField("valueLane");
        payload.setAccessible(true);
        M3FrozenLongList slice = longs.subList(1, 3);
        assertSame(payload.get(longs), payload.get(slice));
        assertArrayEquals(new long[]{3, 1}, slice.toArray());
    }

    @Test
    void exportedArraysAreDetachedAndCallbacksPreserveFailureIdentityAndOrder() {
        M3FrozenIntList ints = M3FrozenIntList.copyOf(
                new int[]{Integer.MIN_VALUE, 3, 3, Integer.MAX_VALUE});
        M3FrozenLongList longs = M3FrozenLongList.copyOf(
                new long[]{Long.MIN_VALUE, 3, 3, Long.MAX_VALUE});
        M3FrozenLongSet set = M3FrozenLongSet.copyOf(
                new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, 0, Long.MIN_VALUE, 0});
        int[] intCopy = ints.toArray();
        long[] longCopy = longs.toArray();
        long[] setCopy = set.toArray();
        Arrays.fill(intCopy, 91);
        Arrays.fill(longCopy, 91);
        Arrays.fill(setCopy, 91);
        assertArrayEquals(new int[]{Integer.MIN_VALUE, 3, 3, Integer.MAX_VALUE}, ints.toArray());
        assertArrayEquals(new long[]{Long.MIN_VALUE, 3, 3, Long.MAX_VALUE}, longs.toArray());
        assertArrayEquals(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}, set.toArray());
        assertEquals(0, set.rank(Long.MIN_VALUE));
        assertEquals(2, set.rank(Long.MAX_VALUE));
        assertEquals(-1, set.rank(1));
        assertEquals(-1, ints.indexOf(47));
        assertEquals(-1, longs.indexOf(47));

        RuntimeException failure = new IllegalArgumentException("original callback failure");
        AtomicInteger calls = new AtomicInteger();
        List<Integer> intObserved = new ArrayList<>();
        assertSame(failure, assertThrows(RuntimeException.class, () -> ints.forEach(value -> {
            intObserved.add(value);
            if (calls.incrementAndGet() == 2) throw failure;
        })));
        assertEquals(2, calls.get());
        assertEquals(List.of(Integer.MIN_VALUE, 3), intObserved);
        calls.set(0);
        List<Long> longObserved = new ArrayList<>();
        assertSame(failure, assertThrows(RuntimeException.class, () -> longs.forEach(value -> {
            longObserved.add(value);
            if (calls.incrementAndGet() == 2) throw failure;
        })));
        assertEquals(2, calls.get());
        assertEquals(List.of(Long.MIN_VALUE, 3L), longObserved);
        calls.set(0);
        longObserved.clear();
        assertSame(failure, assertThrows(RuntimeException.class, () -> set.forEach(value -> {
            longObserved.add(value);
            if (calls.incrementAndGet() == 2) throw failure;
        })));
        assertEquals(2, calls.get());
        assertEquals(List.of(Long.MIN_VALUE, 0L), longObserved);
    }

    @Test
    void primitiveIteratorExhaustionAndNestedEmptyWindowsMatchTheirContracts() {
        M3FrozenIntList ints = M3FrozenIntList.copyOf(new int[]{2, 2});
        var intIterator = ints.iterator();
        assertThrows(UnsupportedOperationException.class, intIterator::remove);
        assertTrue(intIterator.hasNext());
        assertEquals(2, intIterator.nextInt());
        assertThrows(UnsupportedOperationException.class, intIterator::remove);
        assertEquals(2, intIterator.nextInt());
        assertFalse(intIterator.hasNext());
        assertThrows(NoSuchElementException.class, intIterator::nextInt);
        assertThrows(NoSuchElementException.class, intIterator::nextInt);
        assertThrows(UnsupportedOperationException.class, intIterator::remove);
        intIterator.forEachRemaining((int value) -> fail("exhausted iterator callback"));
        assertThrows(NullPointerException.class, () -> intIterator.forEachRemaining(
                (java.util.function.IntConsumer) null));
        assertEquals(0, new M3FrozenIntList().size());
        assertThrows(NoSuchElementException.class, () -> new M3FrozenIntList().iterator().nextInt());
        M3FrozenLongList owner = M3FrozenLongList.copyOf(new long[]{0, 1, 2, 3, 4, 5, 6});
        M3FrozenLongList nested = owner.subList(1, 6).subList(1, 4);
        assertArrayEquals(new long[]{2, 3, 4}, nested.toArray());
        M3FrozenLongList empty = nested.subList(2, 2);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertArrayEquals(new long[0], empty.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> nested.subList(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> nested.subList(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> nested.subList(3, 2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> nested.subList(Integer.MIN_VALUE, Integer.MAX_VALUE));
        long[] detachedWindow = nested.toArray();
        detachedWindow[0] = 91;
        assertEquals(2, nested.get(0));
        assertEquals(2, owner.get(2));
        for (var iterator : List.of(empty.iterator(), new M3FrozenLongList().iterator(),
                new M3FrozenLongSet().iterator())) {
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::nextLong);
            assertThrows(UnsupportedOperationException.class, iterator::remove);
            assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(
                    (java.util.function.LongConsumer) null));
        }
        for (var iterator : List.of(nested.iterator(),
                M3FrozenLongSet.copyOf(new long[]{4, 2, 3, 2}).iterator())) {
            assertThrows(UnsupportedOperationException.class, iterator::remove);
            for (long value = 2; value <= 4; value++) {
                assertTrue(iterator.hasNext());
                assertEquals(value, iterator.nextLong());
                assertThrows(UnsupportedOperationException.class, iterator::remove);
            }
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::nextLong);
            assertThrows(NoSuchElementException.class, iterator::nextLong);
            iterator.forEachRemaining((long value) -> fail("exhausted iterator callback"));
        }
    }
}
