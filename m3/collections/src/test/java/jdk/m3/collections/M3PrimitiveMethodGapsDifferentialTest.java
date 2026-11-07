/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Reference-model differential for the primitive-method-gaps-20261107 absorptions (recipe
 * qualification/primitive-method-gaps-20261107): seeded random operation sequences mixed with mutations, each
 * positional answer checked against {@code TreeSet}/{@code TreeMap} plus a sorted {@code ArrayList}
 * ({@link Collections#binarySearch(List, Object)} for the absent encoding); sorted batches against
 * {@code addAll}/{@code putAll}; {@code trimToSize} against the pre-trim contents and the canonical rebuilt
 * order; {@code M3IntArrayDeque.get} against {@code ArrayDeque}, including ring wrap-around. Page capacity
 * four, five and the default exercise page boundaries, splits, merges and fresh tail pages.
 */
final class M3PrimitiveMethodGapsDifferentialTest {
    private static final long SEED = 0x7072696D67617073L;
    private static final int STEPS = 30_000;
    private static final int FULL_CHECK_MASK = 63;
    private static final int RARE_MASK = 1_023;
    private static final int DENSE_RANGE = 3_000;
    private static final int MAX_BATCH = 48;
    private static final int[] PAGE_CAPACITIES = {4, 5, M3LongTreeSet.DEFAULT_PAGE_CAPACITY};
    private static final int ALLOCATION_CALLS = 200_000;
    private static final long ALLOCATION_SLACK_BYTES = 1_024;

    private static long value(SplittableRandom random) {
        return switch (random.nextInt(16)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> random.nextLong();
            default -> random.nextInt(DENSE_RANGE) - DENSE_RANGE / 2L;
        };
    }

    private static long[] sortedBatch(SplittableRandom random) {
        long[] batch = new long[random.nextInt(MAX_BATCH + 1)];
        for (int i = 0; i < batch.length; i++) {
            batch[i] = random.nextInt(4) == 0 && i > 0 ? batch[i - 1] : value(random);
        }
        Arrays.sort(batch);
        return batch;
    }

    private static List<Long> sorted(TreeSet<Long> expected) {
        return new ArrayList<>(expected);
    }

    private static void assertPositions(M3LongTreeSet actual, TreeSet<Long> expected) {
        List<Long> list = sorted(expected);
        assertEquals(list.size(), actual.size());
        for (int i = 0; i < list.size(); i++) {
            long v = list.get(i);
            assertEquals(v, actual.get(i), "get(" + i + ")");
            assertEquals(i, actual.indexOf(v), "indexOf(" + v + ")");
            if (v != Long.MAX_VALUE) {
                assertEquals(Collections.binarySearch(list, v + 1), actual.indexOf(v + 1));
            }
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(list.size()));
        assertArrayEquals(list.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
    }

    @Test
    void treeSetGetIndexOfAndAddAllSortedMatchTreeSet() {
        for (int pageCapacity : PAGE_CAPACITIES) {
            SplittableRandom random = new SplittableRandom(SEED + pageCapacity);
            M3LongTreeSet actual = new M3LongTreeSet(0, pageCapacity);
            TreeSet<Long> expected = new TreeSet<>();
            for (int step = 0; step < STEPS; step++) {
                treeSetStep(random, step, actual, expected);
                if ((step & FULL_CHECK_MASK) == 0) {
                    assertPositions(actual, expected);
                }
            }
            assertPositions(actual, expected);
        }
    }

    private static void treeSetStep(SplittableRandom random, int step, M3LongTreeSet actual, TreeSet<Long> expected) {
        long v = value(random);
        switch (random.nextInt(12)) {
            case 0, 1, 2, 3 -> assertEquals(expected.add(v), actual.add(v));
            case 4, 5 -> assertEquals(expected.remove(v), actual.remove(v));
            case 6 -> addSortedSlice(random, actual, expected);
            case 7 -> {
                if ((step & RARE_MASK) == 0) {
                    int before = expected.size();
                    expected.removeIf(x -> (x & 7L) == 3L);
                    assertEquals(before - expected.size(), actual.filterInPlace(x -> (x & 7L) != 3L));
                }
            }
            case 8 -> {
                if ((step & RARE_MASK) == 1) {
                    actual.trimToSize();
                }
            }
            case 9 -> {
                if ((step & (RARE_MASK * 4 + 3)) == 2) {
                    actual.clear();
                    expected.clear();
                }
            }
            case 10 -> {
                if (!expected.isEmpty()) {
                    int index = random.nextInt(expected.size());
                    assertEquals(sorted(expected).get(index).longValue(), actual.get(index));
                }
            }
            default -> assertEquals(Collections.binarySearch(sorted(expected), v), actual.indexOf(v));
        }
    }

    private static void addSortedSlice(SplittableRandom random, M3LongTreeSet actual, TreeSet<Long> expected) {
        long[] batch = sortedBatch(random);
        int from = batch.length == 0 ? 0 : random.nextInt(batch.length + 1);
        int to = from + random.nextInt(batch.length - from + 1);
        int before = expected.size();
        for (int i = from; i < to; i++) {
            expected.add(batch[i]);
        }
        assertEquals(expected.size() - before, actual.addAllSorted(batch, from, to));
    }

    @Test
    void treeSetAddAllSortedShapesMatchAddAll() {
        for (int pageCapacity : PAGE_CAPACITIES) {
            SplittableRandom random = new SplittableRandom(SEED ^ pageCapacity);
            for (int trial = 0; trial < 200; trial++) {
                M3LongTreeSet actual = new M3LongTreeSet(0, pageCapacity);
                TreeSet<Long> expected = new TreeSet<>();
                int existing = random.nextInt(4 * pageCapacity);
                for (int i = 0; i < existing; i++) {
                    long v = random.nextInt(DENSE_RANGE);
                    assertEquals(expected.add(v), actual.add(v));
                }
                long[] batch = new long[random.nextInt(6 * pageCapacity)];
                long base = switch (trial % 4) {
                    case 0 -> DENSE_RANGE;                  // all above the current last value
                    case 1 -> -DENSE_RANGE;                 // all below the first value
                    default -> 0L;                          // interleaved with the present values
                };
                for (int i = 0; i < batch.length; i++) {
                    batch[i] = base + random.nextInt(DENSE_RANGE);
                }
                Arrays.sort(batch);
                int before = expected.size();
                for (long v : batch) {
                    expected.add(v);
                }
                assertEquals(expected.size() - before, actual.addAllSorted(batch, 0, batch.length));
                assertPositions(actual, expected);
            }
        }
    }

    @Test
    void treeSetEdgesPageBoundariesAndRefusals() {
        M3LongTreeSet empty = new M3LongTreeSet();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertEquals(-1, empty.indexOf(42L));
        assertEquals(0, empty.addAllSorted(new long[0], 0, 0));

        M3LongTreeSet set = new M3LongTreeSet(0, 4);
        SplittableRandom random = new SplittableRandom(SEED);
        int count = 1_000;
        long[] shuffled = new long[count];
        for (int i = 0; i < count; i++) {
            shuffled[i] = 2L * i;
        }
        for (int i = count - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            long t = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = t;
        }
        for (long v : shuffled) {
            set.add(v);
        }
        assertTrue(set.pageCount() > count / 4);
        for (int i = 0; i < count; i++) {
            assertEquals(2L * i, set.get(i));
            assertEquals(i, set.indexOf(2L * i));
            assertEquals(-(i + 1) - 1, set.indexOf(2L * i + 1));
        }
        assertEquals(-1, set.indexOf(-1L));
        assertEquals(-count - 1, set.indexOf(Long.MAX_VALUE));
        assertRefusalsLeaveSetUnchanged(set);
    }

    private static void assertRefusalsLeaveSetUnchanged(M3LongTreeSet set) {
        long[] before = set.toArray();
        M3LongIterator live = set.iterator();
        assertThrows(IllegalArgumentException.class, () -> set.addAllSorted(new long[] {1, 5, 3}, 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> set.addAllSorted(new long[] {1, 2}, 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> set.addAllSorted(new long[] {1, 2}, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.addAllSorted(new long[] {1, 2}, 2, 1));
        assertThrows(NullPointerException.class, () -> set.addAllSorted(null, 0, 0));
        assertArrayEquals(before, set.toArray());
        assertTrue(live.hasNext(), "no refusal may count as a modification");
        // Only the slice is validated (donor rule): unsorted values outside [from, to) are ignored.
        assertEquals(2, set.addAllSorted(new long[] {9, 3_001, 3_003, -7}, 1, 3));
        assertEquals(3_001L, set.get(set.size() - 2));
    }

    @Test
    void treeSetPrefixLaneSurvivesTrimAndIsCounted() {
        M3LongTreeSet set = new M3LongTreeSet(4_096, 8);
        for (long v = 0; v < 1_000; v++) {
            set.add(v * 3);
        }
        long beforePositional = set.payloadBytes();
        assertEquals(999L * 3, set.get(999));
        assertTrue(set.payloadBytes() > beforePositional, "the built prefix lane is retained payload");
        for (long v = 0; v < 1_000; v += 2) {
            set.remove(v * 3);
        }
        set.trimToSize();
        long trimmed = set.payloadBytes();
        long perPage = Long.BYTES + 4L * Integer.BYTES;  // maxima + counts, slots, free slots and prefix lanes
        assertEquals(set.capacity() * (long) Long.BYTES + set.pageCount() * perPage, trimmed,
                "trimToSize shrinks the prefix lane with the page lanes");
        for (int i = 0; i < set.size(); i++) {
            assertEquals((2L * i + 1) * 3, set.get(i));
        }
        assertEquals(trimmed, set.payloadBytes(), "the trimmed prefix lane is reused by later positional calls");
    }

    @Test
    void positionalReadsAllocateNothing() {
        M3LongTreeSet set = new M3LongTreeSet();
        M3LongLongTreeMap map = new M3LongLongTreeMap();
        for (long v = 0; v < 50_000; v++) {
            set.add(v * 7);
            map.put(v * 7, v);
        }
        SplittableRandom random = new SplittableRandom(SEED);
        long sink = readPositions(set, map, random);
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        sink += readPositions(set, map, random);
        long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
        assertTrue(allocated < ALLOCATION_SLACK_BYTES, "allocated " + allocated + " bytes, sink " + sink);
    }

    private static long readPositions(M3LongTreeSet set, M3LongLongTreeMap map, SplittableRandom random) {
        long sink = 0;
        for (int call = 0; call < ALLOCATION_CALLS; call++) {
            int index = random.nextInt(set.size());
            sink += set.get(index) + set.indexOf(index) + map.keyAt(index) + map.valueAt(index) + map.indexOfKey(index);
        }
        return sink;
    }

    private static void assertEntries(M3LongLongTreeMap actual, TreeMap<Long, Long> expected) {
        List<Long> keys = new ArrayList<>(expected.keySet());
        assertEquals(keys.size(), actual.size());
        int i = 0;
        for (Map.Entry<Long, Long> entry : expected.entrySet()) {
            assertEquals(entry.getKey().longValue(), actual.keyAt(i));
            assertEquals(entry.getValue().longValue(), actual.valueAt(i));
            assertEquals(i, actual.indexOfKey(entry.getKey()));
            if (entry.getKey() != Long.MIN_VALUE) {
                assertEquals(Collections.binarySearch(keys, entry.getKey() - 1), actual.indexOfKey(entry.getKey() - 1));
            }
            i++;
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.keyAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.keyAt(keys.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.valueAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.valueAt(keys.size()));
    }

    @Test
    void treeMapKeyAtValueAtIndexOfKeyAndPutAllSortedMatchTreeMap() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        M3LongLongTreeMap actual = new M3LongLongTreeMap();
        TreeMap<Long, Long> expected = new TreeMap<>();
        for (int step = 0; step < STEPS; step++) {
            long k = value(random);
            long v = random.nextLong();
            switch (random.nextInt(10)) {
                case 0, 1, 2 -> assertEquals(expected.put(k, v) == null, actual.put(k, v));
                case 3, 4 -> assertEquals(expected.remove(k) != null, actual.remove(k));
                case 5 -> putSortedSlice(random, actual, expected);
                case 6 -> {
                    if (!expected.isEmpty() && (step & 15) == 0) {
                        assertEquals(expected.pollFirstEntry().getKey().longValue(), actual.pollFirstKey());
                    }
                }
                case 7 -> {
                    if ((step & RARE_MASK) == 0) {
                        actual.trimToSize();
                    }
                    if ((step & (RARE_MASK * 4 + 3)) == 7) {
                        actual.clear();
                        expected.clear();
                    }
                }
                case 8 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        Map.Entry<Long, Long> entry = new ArrayList<>(expected.entrySet()).get(index);
                        assertEquals(entry.getKey().longValue(), actual.keyAt(index));
                        assertEquals(entry.getValue().longValue(), actual.valueAt(index));
                    }
                }
                default -> assertEquals(Collections.binarySearch(new ArrayList<>(expected.keySet()), k),
                        actual.indexOfKey(k));
            }
            if ((step & FULL_CHECK_MASK) == 0) {
                assertEntries(actual, expected);
            }
        }
        assertEntries(actual, expected);
    }

    private static void putSortedSlice(SplittableRandom random, M3LongLongTreeMap actual, TreeMap<Long, Long> expected) {
        long[] keys = sortedBatch(random);
        long[] values = new long[keys.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextLong();
        }
        int from = keys.length == 0 ? 0 : random.nextInt(keys.length + 1);
        int to = from + random.nextInt(keys.length - from + 1);
        int before = expected.size();
        for (int i = from; i < to; i++) {
            expected.put(keys[i], values[i]);
        }
        assertEquals(expected.size() - before, actual.putAllSorted(keys, values, from, to));
    }

    @Test
    void treeMapPutAllSortedRangeRefusalsAndFullRange() {
        M3LongLongTreeMap map = new M3LongLongTreeMap();
        map.put(5, 50);
        long[] keys = {1, 5, 5, 9};
        long[] values = {10, 51, 52, 90};
        M3LongIterator live = map.keyIterator();
        assertThrows(IllegalArgumentException.class, () -> map.putAllSorted(keys, new long[3], 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> map.putAllSorted(keys, values, 3, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> map.putAllSorted(keys, values, -1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> map.putAllSorted(new long[] {0, 3, 2}, new long[3], 0, 3));
        assertThrows(NullPointerException.class, () -> map.putAllSorted(null, values, 0, 0));
        assertTrue(live.hasNext(), "no refusal may count as a modification");
        assertEquals(0, map.putAllSorted(keys, values, 2, 2));
        assertEquals(0, map.putAllSorted(keys, values, 1, 3));
        assertEquals(52L, map.getOrDefault(5, 0), "the last incoming value wins");
        assertEquals(2, map.putAllSorted(keys, values, 0, keys.length));
        assertArrayEquals(new long[] {1, 5, 9}, map.keysToArray());
        assertArrayEquals(new long[] {10, 52, 90}, map.valuesToArray());
        assertEquals(1, map.putAllSorted(new long[] {7, -1}, new long[] {70, 0}, 0, 1));
        assertEquals(70L, map.valueAt(map.indexOfKey(7)));
        assertEquals(-1, new M3LongLongTreeMap().indexOfKey(3));
        assertThrows(IndexOutOfBoundsException.class, () -> new M3LongLongTreeMap().keyAt(0));
    }

    /** One owner's trim observations: capacity, keys in iteration order, contents. */
    private record Shape(int capacity, long[] order, Map<Long, Long> contents) {}

    private static Shape shape(M3IntIntHashMap map) {
        Map<Long, Long> contents = new HashMap<>();
        map.forEach((k, v) -> contents.put((long) k, (long) v));
        return new Shape(map.capacity(), Arrays.stream(map.keysToArray()).asLongStream().toArray(), contents);
    }

    private static Shape shape(M3IntLongHashMap map) {
        Map<Long, Long> contents = new HashMap<>();
        map.forEach((k, v) -> contents.put((long) k, v));
        return new Shape(map.capacity(), Arrays.stream(map.keysToArray()).asLongStream().toArray(), contents);
    }

    private static Shape shape(M3LongLongHashMap map) {
        Map<Long, Long> contents = new HashMap<>();
        map.forEach(contents::put);
        return new Shape(map.capacity(), map.keysToArray(), contents);
    }

    private static int intKey(SplittableRandom random) {
        return switch (random.nextInt(12)) {
            case 0 -> 0;
            case 1 -> Integer.MIN_VALUE;
            case 2 -> Integer.MAX_VALUE;
            default -> random.nextInt();
        };
    }

    private static void assertTrimmed(Shape before, Shape after, Shape canonical) {
        assertEquals(before.contents(), after.contents(), "trimToSize preserves the mappings");
        assertEquals(canonical.capacity(), after.capacity(), "trimToSize reaches the minimal capacity");
        assertArrayEquals(canonical.order(), after.order(), "iteration follows the rebuilt table");
        if (after.capacity() == before.capacity()) {
            assertArrayEquals(before.order(), after.order(), "a no-op trim keeps the order");
        } else {
            assertTrue(after.capacity() < before.capacity());
        }
    }

    @Test
    void intIntTrimToSizePreservesContentsAndReducesCapacity() {
        SplittableRandom random = new SplittableRandom(SEED + 2);
        for (int trial = 0; trial < 60; trial++) {
            M3IntIntHashMap map = new M3IntIntHashMap(random.nextInt(64));
            HashMap<Integer, Integer> model = new HashMap<>();
            int inserts = random.nextInt(2_000);
            for (int i = 0; i < inserts; i++) {
                int k = intKey(random);
                assertEquals(model.put(k, i) == null, map.put(k, i));
            }
            for (Integer k : new ArrayList<>(model.keySet())) {
                if (random.nextInt(8) != 0) {
                    assertTrue(map.remove((int) k));
                    model.remove(k);
                }
            }
            Shape before = shape(map);
            M3IntIntHashMap fresh = new M3IntIntHashMap(map.size() - (map.containsKey(0) ? 1 : 0));
            for (long k : before.order()) {
                fresh.put((int) k, map.getOrDefault((int) k, 0));
            }
            M3IntIterator live = map.keyIterator();
            map.trimToSize();
            assertTrimmed(before, shape(map), shape(fresh));
            if (shape(map).capacity() < before.capacity()) {
                assertThrows(ConcurrentModificationException.class, live::hasNext);
            }
            continueIntInt(random, map, model);
        }
    }

    private static void continueIntInt(SplittableRandom random, M3IntIntHashMap map, Map<Integer, Integer> model) {
        for (int i = 0; i < 500; i++) {
            int k = intKey(random);
            if (random.nextBoolean()) {
                assertEquals(model.put(k, i) == null, map.put(k, i));
            } else {
                assertEquals(model.remove(k) != null, map.remove(k));
            }
            if ((i & 127) == 0) {
                map.trimToSize();
            }
        }
        Map<Long, Long> expected = new HashMap<>();
        model.forEach((k, v) -> expected.put((long) k, (long) v));
        assertEquals(expected, shape(map).contents());
    }

    @Test
    void intLongTrimToSizePreservesContentsAndReducesCapacity() {
        SplittableRandom random = new SplittableRandom(SEED + 3);
        for (int trial = 0; trial < 60; trial++) {
            M3IntLongHashMap map = new M3IntLongHashMap(random.nextInt(64));
            HashMap<Integer, Long> model = new HashMap<>();
            int inserts = random.nextInt(2_000);
            for (int i = 0; i < inserts; i++) {
                int k = intKey(random);
                long v = random.nextLong();
                assertEquals(model.put(k, v) == null, map.put(k, v));
            }
            for (Integer k : new ArrayList<>(model.keySet())) {
                if (random.nextInt(8) != 0) {
                    assertTrue(map.remove((int) k));
                    model.remove(k);
                }
            }
            Shape before = shape(map);
            M3IntLongHashMap fresh = new M3IntLongHashMap(map.size() - (map.containsKey(0) ? 1 : 0));
            for (long k : before.order()) {
                fresh.put((int) k, map.getOrDefault((int) k, 0L));
            }
            map.trimToSize();
            assertTrimmed(before, shape(map), shape(fresh));
            map.trimToSize();
            assertArrayEquals(shape(fresh).order(), shape(map).order(), "trimToSize is idempotent");
            for (int i = 0; i < 300; i++) {
                int k = intKey(random);
                assertEquals(model.put(k, (long) i) == null, map.put(k, i));
            }
            Map<Long, Long> expected = new HashMap<>();
            model.forEach((k, v) -> expected.put((long) k, v));
            assertEquals(expected, shape(map).contents());
        }
    }

    @Test
    void longLongTrimToSizePurgesTombstonesAndReducesCapacity() {
        SplittableRandom random = new SplittableRandom(SEED + 4);
        for (int trial = 0; trial < 60; trial++) {
            M3LongLongHashMap map = new M3LongLongHashMap(random.nextInt(64));
            HashMap<Long, Long> model = new HashMap<>();
            int inserts = random.nextInt(2_000);
            for (int i = 0; i < inserts; i++) {
                long k = value(random);
                long v = random.nextLong();
                assertEquals(model.put(k, v) == null, map.put(k, v));
            }
            for (Long k : new ArrayList<>(model.keySet())) {
                if (random.nextInt(trial % 2 == 0 ? 8 : 3) != 0) {
                    assertTrue(map.remove((long) k));
                    model.remove(k);
                }
            }
            Shape before = shape(map);
            M3LongLongHashMap fresh = new M3LongLongHashMap(map.size());
            for (long k : before.order()) {
                fresh.put(k, map.getOrDefault(k, 0L));
            }
            map.trimToSize();
            Shape after = shape(map);
            assertEquals(before.contents(), after.contents());
            assertEquals(fresh.capacity(), after.capacity());
            assertArrayEquals(shape(fresh).order(), after.order(), "iteration follows the rebuilt table");
            assertTrue(after.capacity() <= before.capacity());
            map.trimToSize();
            assertArrayEquals(after.order(), shape(map).order(), "a second trim is a no-op");
            for (int i = 0; i < 300; i++) {
                long k = value(random);
                assertEquals(model.remove(k) != null, map.remove(k));
            }
            assertEquals(model, shape(map).contents());
        }
    }

    @Test
    void longLongTrimToSizeFailsLiveIteratorsOnlyWhenItRebuilds() {
        M3LongLongHashMap map = new M3LongLongHashMap();
        for (long k = 0; k < 1_000; k++) {
            map.put(k, k);
        }
        int grown = map.capacity();
        for (long k = 0; k < 990; k++) {
            map.remove(k);
        }
        M3LongIterator live = map.keyIterator();
        map.trimToSize();
        assertTrue(map.capacity() < grown);
        assertThrows(ConcurrentModificationException.class, live::hasNext);
        M3LongIterator after = map.keyIterator();
        map.trimToSize();
        assertTrue(after.hasNext(), "a no-op trim is not a modification");
    }

    @Test
    void intArrayDequeGetMatchesArrayDequeAcrossWrapAround() {
        SplittableRandom random = new SplittableRandom(SEED + 5);
        M3IntArrayDeque actual = new M3IntArrayDeque(2);
        ArrayDeque<Integer> expected = new ArrayDeque<>();
        for (int step = 0; step < STEPS; step++) {
            int v = random.nextInt();
            switch (random.nextInt(9)) {
                case 0, 1 -> { actual.addLast(v); expected.addLast(v); }
                case 2, 3 -> { actual.addFirst(v); expected.addFirst(v); }
                case 4 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.pollFirst().intValue(), actual.removeFirst());
                    }
                }
                case 5 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.pollLast().intValue(), actual.removeLast());
                    }
                }
                case 6 -> {
                    int[] batch = random.ints(random.nextInt(9)).toArray();
                    actual.addAll(batch, 0, batch.length);
                    Arrays.stream(batch).forEach(expected::addLast);
                }
                case 7 -> {
                    if ((step & RARE_MASK) == 0) {
                        actual.clear();
                        expected.clear();
                    }
                }
                default -> assertDequeGets(actual, expected);
            }
        }
        assertDequeGets(actual, expected);
    }

    private static void assertDequeGets(M3IntArrayDeque actual, ArrayDeque<Integer> expected) {
        List<Integer> list = new ArrayList<>(expected);
        assertEquals(list.size(), actual.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).intValue(), actual.get(i), "get(" + i + ")");
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(list.size()));
    }

    @Test
    void intArrayDequeGetReadsAcrossTheRingSeam() {
        M3IntArrayDeque deque = new M3IntArrayDeque(8);
        assertThrows(IndexOutOfBoundsException.class, () -> deque.get(0));
        for (int i = 0; i < 6; i++) {
            deque.addLast(i);
        }
        for (int i = 0; i < 5; i++) {
            deque.removeFirst();
        }
        for (int i = 6; i < 13; i++) {
            deque.addLast(i);
        }
        assertEquals(8, deque.capacity(), "the values wrap inside the original ring");
        for (int i = 0; i < deque.size(); i++) {
            assertEquals(5 + i, deque.get(i));
        }
        deque.addFirst(-1);
        assertEquals(-1, deque.get(0));
        assertEquals(12, deque.get(deque.size() - 1));
        assertFalse(deque.isEmpty());
    }
}
