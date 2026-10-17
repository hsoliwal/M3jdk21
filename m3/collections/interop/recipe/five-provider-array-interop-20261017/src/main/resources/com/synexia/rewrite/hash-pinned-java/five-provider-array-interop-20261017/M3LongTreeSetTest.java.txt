/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Finite differential comparison of {@link M3LongTreeSet} against {@code TreeSet<Long>} as a
 * {@code NavigableSet}; this is not JDK replacement proof. Zero, the extrema, a dense small range,
 * a moving cluster and monotone runs are over-represented so page splits at both ends and in the
 * interior, merges, free-slot reuse and lane growth are exercised with order checked throughout.
 * Range views come from {@link M3LongSortedViews} over the owner, as the interface defines them.
 */
final class M3LongTreeSetTest {
    private static final long FALLBACK = Long.MIN_VALUE + 7;

    private static long value(Random random, int step) {
        return switch (step % 17) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> random.nextLong();
            case 4 -> Long.MIN_VALUE + 1;
            case 5 -> Long.MAX_VALUE - 1;
            case 6, 7 -> (step / 1_000) * 10_000L + random.nextInt(4_096);
            case 8 -> step * 3L;
            case 9 -> -step * 5L;
            default -> random.nextInt(97) - 48L;
        };
    }

    private static long[] order(Collection<Long> values) {
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    private static void assertNavigation(NavigableSet<Long> expected, M3LongSortedSet actual, long pivot) {
        assertNavigated(expected.floor(pivot), actual.floor(pivot, FALLBACK));
        assertNavigated(expected.ceiling(pivot), actual.ceiling(pivot, FALLBACK));
        assertNavigated(expected.lower(pivot), actual.lower(pivot, FALLBACK));
        assertNavigated(expected.higher(pivot), actual.higher(pivot, FALLBACK));
    }

    private static void assertNavigated(Long expected, long actual) {
        assertEquals(expected == null ? FALLBACK : expected.longValue(), actual);
    }

    private static void assertEnds(NavigableSet<Long> expected, M3LongSortedSet actual) {
        if (expected.isEmpty()) {
            assertTrue(actual.isEmpty());
            assertThrows(NoSuchElementException.class, actual::first);
            assertThrows(NoSuchElementException.class, actual::last);
        } else {
            assertEquals(expected.first().longValue(), actual.first());
            assertEquals(expected.last().longValue(), actual.last());
        }
    }

    private static void differential(M3LongTreeSet actual, int steps, long seed) {
        Random random = new Random(seed);
        TreeSet<Long> expected = new TreeSet<>();
        for (int step = 0; step < steps; step++) {
            long v = value(random, step);
            switch (random.nextInt(16)) {
                case 0, 1, 2, 3 -> assertEquals(expected.add(v), actual.add(v));
                case 4 -> assertEquals(expected.remove(v), actual.remove(v));
                case 5 -> assertEquals(expected.contains(v), actual.contains(v));
                case 6 -> assertEnds(expected, actual);
                case 7 -> assertNavigation(expected, actual, v);
                case 8 -> {
                    long other = value(random, step + 5);
                    long from = Math.min(v, other);
                    long to = Math.max(v, other);
                    NavigableSet<Long> jdkSub = expected.subSet(from, true, to, false);
                    M3LongSortedSet sub = M3LongSortedViews.subSet(actual, from, to);
                    assertArrayEquals(order(jdkSub), sub.toArray());
                    assertEnds(jdkSub, sub);
                    assertNavigation(jdkSub, sub, value(random, step + 11));
                    assertArrayEquals(order(expected.headSet(to, false)),
                            M3LongSortedViews.headSet(actual, to).toArray());
                    assertArrayEquals(order(expected.tailSet(from, true)),
                            M3LongSortedViews.tailSet(actual, from).toArray());
                }
                case 9 -> {
                    if (!expected.isEmpty()) {
                        boolean head = random.nextBoolean();
                        long polled = head ? expected.pollFirst() : expected.pollLast();
                        assertEquals(polled, head ? actual.first() : actual.last());
                        assertTrue(actual.remove(polled));
                    }
                }
                case 10 -> {
                    if (random.nextInt(40) == 0) {
                        long mask = 1L << random.nextInt(8);
                        int before = expected.size();
                        int removed = actual.filterInPlace(x -> (x & mask) == 0);
                        expected.removeIf(x -> (x & mask) != 0);
                        assertEquals(before - expected.size(), removed);
                    }
                }
                case 11 -> {
                    M3LongIterator cursor = actual.iterator();
                    if (actual.add(v)) {
                        expected.add(v);
                        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
                    }
                }
                case 12 -> {
                    if (expected.size() > 6_000) {
                        expected.clear();
                        actual.clear();
                        assertEquals(0, actual.pageCount());
                    }
                }
                default -> assertArrayEquals(order(expected), actual.toArray());
            }
            assertEquals(expected.size(), actual.size());
        }
        assertArrayEquals(order(expected), actual.longStream().toArray());
        assertEquals(expected.size(), actual.parallelLongStream().count());
        assertTrue(actual.capacity() >= actual.size());
        assertTrue((long) actual.pageCount() * actual.pageCapacity() >= actual.size());
        long[] snapshot = actual.toArray();
        actual.trimToSize();
        int pages = Math.max(1, actual.pageCount());
        assertEquals(pages * actual.pageCapacity(), actual.capacity());
        assertArrayEquals(snapshot, actual.toArray());
        assertEquals((long) actual.capacity() * Long.BYTES + pages * (Long.BYTES + 3L * Integer.BYTES),
                actual.payloadBytes());
    }

    @Test
    void matchesJdkNavigableSetAcrossChurnIncludingViewsAndFilters() {
        differential(new M3LongTreeSet(), 40_000, 0x4d334c545345L);
    }

    @Test
    void matchesJdkNavigableSetWithTinyPages() {
        differential(new M3LongTreeSet(0, 4), 10_000, 0x50414745345L);
    }

    @Test
    void growthSplitsPagesAndRemovalMergesAndTrimsThem() {
        M3LongTreeSet set = new M3LongTreeSet();
        TreeSet<Long> expected = new TreeSet<>();
        assertEquals(64, set.capacity());
        for (long i = 1; i <= 100_000; i++) {
            assertTrue(set.add(i * 7));
            expected.add(i * 7);
        }
        for (long i = 1; i <= 100_000; i++) {
            assertTrue(set.add(-i * 7));
            expected.add(-i * 7);
        }
        assertEquals(200_000, set.size());
        int pages = set.pageCount();
        assertTrue(pages >= 200_000 / 64, "pages " + pages);
        assertTrue(pages <= 200_000 / 32 + 8, "pages " + pages);
        assertTrue(set.capacity() >= (long) pages * 64);
        for (long i = 1; i <= 100_000; i++) {
            if (i % 3 != 0) {
                assertTrue(set.remove(i * 7));
                assertTrue(set.remove(-i * 7));
                expected.remove(i * 7);
                expected.remove(-i * 7);
            }
        }
        assertEquals(66_666, set.size());
        long[] survivors = set.toArray();
        assertEquals(66_666, survivors.length);
        assertEquals(-699_993L, survivors[0]);
        assertEquals(699_993L, survivors[survivors.length - 1]);
        assertArrayEquals(order(expected), survivors);
        assertFalse(set.contains(0L), "zero was never inserted; -21 is followed by +21");
        assertEquals(-699_993L, set.first());
        assertEquals(699_993L, set.last());
        assertTrue(set.pageCount() < pages / 2, "pages after merges " + set.pageCount());
        int capacityBefore = set.capacity();
        set.trimToSize();
        assertEquals(set.pageCount() * 64, set.capacity());
        assertTrue(set.capacity() < capacityBefore);
        assertArrayEquals(survivors, set.toArray());
        assertEquals(21L, set.higher(0, FALLBACK));
        assertEquals(-21L, set.lower(0, FALLBACK));
        assertEquals(FALLBACK, set.floor(-700_000, FALLBACK));
        assertEquals(FALLBACK, set.ceiling(700_000, FALLBACK));
        set.clear();
        assertEquals(0, set.pageCount());
        assertEquals(0, set.size());
        assertFalse(set.iterator().hasNext());
        assertTrue(set.add(42L));
        assertEquals(1, set.pageCount());
        assertEquals(42L, set.first());
    }

    @Test
    void edgeContractsFailFastAndSizing() {
        M3LongTreeSet set = new M3LongTreeSet(0);
        assertEquals(64, set.capacity());
        assertEquals(64, set.pageCapacity());
        assertEquals(0, set.pageCount());
        assertThrows(IllegalArgumentException.class, () -> new M3LongTreeSet(-1));
        assertThrows(IllegalArgumentException.class, () -> new M3LongTreeSet(0, 3));
        assertThrows(NoSuchElementException.class, set::first);
        assertThrows(NoSuchElementException.class, set::last);
        assertEquals(FALLBACK, set.floor(0, FALLBACK));
        assertEquals(FALLBACK, set.ceiling(0, FALLBACK));
        assertEquals(FALLBACK, set.lower(0, FALLBACK));
        assertEquals(FALLBACK, set.higher(0, FALLBACK));
        assertFalse(set.remove(0L));
        assertEquals(0, set.filterInPlace(x -> false));
        assertTrue(set.add(0L));
        assertFalse(set.add(0L));
        assertTrue(set.add(Long.MIN_VALUE));
        assertTrue(set.add(Long.MAX_VALUE));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, set.toArray());
        assertEquals(Long.MIN_VALUE, set.first());
        assertEquals(Long.MAX_VALUE, set.last());
        assertEquals(FALLBACK, set.lower(Long.MIN_VALUE, FALLBACK));
        assertEquals(FALLBACK, set.higher(Long.MAX_VALUE, FALLBACK));
        assertEquals(Long.MIN_VALUE, set.floor(Long.MIN_VALUE, FALLBACK));
        assertEquals(Long.MAX_VALUE, set.ceiling(Long.MAX_VALUE, FALLBACK));
        assertEquals(Long.MIN_VALUE, set.floor(-1L, FALLBACK));
        assertEquals(0L, set.ceiling(-1L, FALLBACK));
        assertEquals(0L, set.lower(1L, FALLBACK));
        assertEquals(Long.MAX_VALUE, set.higher(1L, FALLBACK));
        assertEquals(0L, set.lower(Long.MAX_VALUE, FALLBACK));
        assertEquals(0L, set.higher(Long.MIN_VALUE, FALLBACK));
        M3LongIterator iterator = set.iterator();
        assertEquals(Long.MIN_VALUE, iterator.nextLong());
        assertTrue(set.remove(0L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertThrows(ConcurrentModificationException.class, iterator::nextLong);
        Spliterator.OfLong cursor = set.longSpliterator();
        assertEquals(Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.SIZED,
                cursor.characteristics() & (Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.SIZED));
        assertNull(cursor.getComparator());
        assertEquals(2L, cursor.estimateSize());
        assertArrayEquals(new long[] {Long.MIN_VALUE, Long.MAX_VALUE}, set.longStream().toArray());
        set.clear();
        set.clear();
        M3LongIterator empty = set.iterator();
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::nextLong);
        assertEquals(64, set.capacity());
        assertEquals(64L * Long.BYTES + Long.BYTES + 3L * Integer.BYTES, set.payloadBytes());
    }

    @Test
    void filterInPlaceCommitsPriorRejectionsAndKeepsTheThrowingSuffix() {
        M3LongTreeSet set = new M3LongTreeSet(0, 4);
        for (long i = 1; i <= 10; i++) {
            assertTrue(set.add(i));
        }
        // A full page splits in half; ten ascending elements occupy four pages.
        assertEquals(4, set.pageCount());
        RuntimeException boom = new RuntimeException("boom");
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> set.filterInPlace(v -> {
            if (v == 7) {
                throw boom;
            }
            return v % 2 != 0;
        }));
        assertSame(boom, thrown);
        assertArrayEquals(new long[] {1, 3, 5, 7, 8, 9, 10}, set.toArray());
        assertEquals(7, set.size());
        assertEquals(3, set.filterInPlace(v -> v < 7 || v == 10));
        assertArrayEquals(new long[] {1, 3, 5, 10}, set.toArray());
        assertEquals(0, set.filterInPlace(v -> true));
        M3LongIterator cursor = set.iterator();
        assertEquals(4, set.filterInPlace(v -> false));
        assertThrows(ConcurrentModificationException.class, cursor::hasNext);
        assertTrue(set.isEmpty());
        assertEquals(0, set.pageCount());
        assertTrue(set.add(5L));
        assertEquals(5L, set.first());
    }

    @Test
    void sortedViewsAndUnionsWorkOverTheOwnerAsBackingSet() {
        M3LongTreeSet actual = new M3LongTreeSet(0, 4);
        TreeSet<Long> expected = new TreeSet<>();
        for (long i = -200; i <= 200; i += 3) {
            assertTrue(actual.add(i));
            expected.add(i);
        }
        M3LongSortedSet sub = M3LongSortedViews.subSet(actual, -50, 50);
        NavigableSet<Long> jdk = expected.subSet(-50L, true, 50L, false);
        assertArrayEquals(order(jdk), sub.toArray());
        assertFalse(sub.add(7));
        assertFalse(jdk.add(7L));
        assertTrue(sub.add(8));
        assertTrue(jdk.add(8L));
        assertThrows(IllegalArgumentException.class, () -> sub.add(500));
        assertTrue(sub.remove(-50));
        assertTrue(jdk.remove(-50L));
        assertFalse(sub.remove(100));
        assertFalse(jdk.remove(100L));
        assertEnds(jdk, sub);
        assertNavigation(jdk, sub, 11);
        assertNavigation(jdk, sub, -49);
        assertNavigation(jdk, sub, 49);
        assertEquals(jdk.size(), sub.size());
        assertArrayEquals(order(expected), actual.toArray());
        sub.clear();
        jdk.clear();
        assertArrayEquals(order(expected), actual.toArray());
        assertEnds(expected.headSet(-100L, false), M3LongSortedViews.headSet(actual, -100));
        assertEnds(expected.tailSet(100L, true), M3LongSortedViews.tailSet(actual, 100));
        M3LongSortedSet unmodifiable = M3LongSortedViews.unmodifiable(actual);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(1));
        assertEquals(expected.first().longValue(), unmodifiable.first());
        M3LongTreeSet other = new M3LongTreeSet();
        TreeSet<Long> union = new TreeSet<>(expected);
        for (long i = -205; i <= 205; i += 5) {
            other.add(i);
            union.add(i);
        }
        M3LongSortedSet merged = M3LongCompositeViews.sortedUnion(actual, other);
        assertArrayEquals(order(union), merged.toArray());
        assertEnds(union, merged);
        assertNavigation(union, merged, 0);
        assertNavigation(union, merged, 101);
        assertNavigation(union, merged, -203);
        assertEquals(union.size(), merged.size());
    }

    @Test
    void retainsOnlyPrimitiveLanes() {
        var fields = Arrays.stream(M3LongTreeSet.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> f.getType().getSimpleName())
                .sorted()
                .toList();
        assertEquals(List.of("int", "int", "int", "int", "int", "int", "int[]", "int[]", "int[]",
                "long[]", "long[]"), fields);
    }
}
