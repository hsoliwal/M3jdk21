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
        assertThrows(IllegalArgumentException.class, () -> longs.subList(2, 1));
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
}
