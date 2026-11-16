/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Independent membership and mutable-row oracles for sorted keyed snapshots. */
public final class ListDiffSortedTest {
    private static final long[] EXTREMES = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1,
        Long.MAX_VALUE - 1, Long.MAX_VALUE
    };

    @Test
    void everyPairOfSmallSortedSubsetsPreservesExactRuns() {
        int limit = 1 << EXTREMES.length;
        for (int beforeMask = 0; beforeMask < limit; beforeMask++) {
            long[] before = subset(beforeMask);
            for (int afterMask = 0; afterMask < limit; afterMask++) {
                assertSortedPatch(before, subset(afterMask));
            }
        }
    }

    @Test
    void seededSortedChurnPreservesEveryObservableAndResidentRow() {
        Random random = new Random(0x4d33494445444946L);
        for (int round = 0; round < 1000; round++) {
            TreeSet<Long> oldKeys = new TreeSet<>();
            TreeSet<Long> newKeys = new TreeSet<>();
            int oldCount = random.nextInt(97);
            for (int i = 0; i < oldCount; i++) oldKeys.add(random.nextLong());
            for (long key : EXTREMES) if (random.nextBoolean()) oldKeys.add(key);
            for (long key : oldKeys) if (random.nextInt(4) != 0) newKeys.add(key);
            int newCount = random.nextInt(97);
            for (int i = 0; i < newCount; i++) newKeys.add(random.nextLong());
            for (long key : EXTREMES) if (random.nextBoolean()) newKeys.add(key);
            assertSortedPatch(toArray(oldKeys), toArray(newKeys));
        }
    }

    @Test
    void gapsCoalesceInTheirOwnSnapshotCoordinates() {
        // Interleaved keys still form one entire old removal and one new insertion.
        assertSortedPatch(new long[] {0, 2, 4, 6, 8}, new long[] {1, 3, 5, 7, 9});
        assertSortedPatch(new long[] {0, 1, 2, 9}, new long[] {0, 3, 4, 9});
        assertSortedPatch(new long[] {0, 3, 4, 9}, new long[] {0, 1, 2, 9});
        assertSortedPatch(new long[] {Long.MIN_VALUE, -2, -1, 0, 2, 3, Long.MAX_VALUE},
                new long[] {Long.MIN_VALUE, -3, -1, 1, 2, 4, Long.MAX_VALUE});
        assertSortedPatch(new long[] {-4, -3, -2}, new long[] {2, 3, 4});
        assertSortedPatch(new long[] {2, 3, 4}, new long[] {-4, -3, -2});
        long[] sameOwner = {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE};
        assertSortedPatch(sameOwner, sameOwner);
    }

    @Test
    void reorderedInputsKeepTheEstablishedRunAndMoveTrace() {
        assertFallbackTrace(new long[] {1, 2, 3, 4, 5, 6},
                new long[] {1, 9, 3, 2, 6, 7, 8}, 3,
                List.of("-3x2", "-1x1", "+1x1", "+3x1", "+5x2"),
                new int[] {1}, new int[] {3});
        assertFallbackTrace(new long[] {1, 2, 3, 4},
                new long[] {1, 3, 2, 4}, 3,
                List.of("-1x1", "+2x1"), new int[] {1}, new int[] {2});
        assertFallbackTrace(new long[] {1, 3, 2, 4},
                new long[] {1, 2, 3, 4}, 3,
                List.of("-1x1", "+2x1"), new int[] {1}, new int[] {2});
        assertFallbackTrace(new long[] {3, 2, 1}, new long[] {3, 2, 1}, 3,
                List.of(), new int[0], new int[0]);
        assertFallbackTrace(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE},
                new long[] {Long.MAX_VALUE, 0, Long.MIN_VALUE}, 1,
                List.of("-0x2", "+1x2"), new int[] {0, 1}, new int[] {2, 1});
    }

    @Test
    void duplicatesAndNullsKeepTheEstablishedValidationPrecedence() {
        assertDuplicate(new long[] {7, 7}, new long[] {3, 3},
                "duplicate key in after: 3");
        assertDuplicate(null, new long[] {3, 3}, "duplicate key in after: 3");
        assertDuplicate(new long[] {Long.MIN_VALUE, Long.MIN_VALUE}, new long[0],
                "duplicate key in before: " + Long.MIN_VALUE);
        assertDuplicate(new long[0], new long[] {Long.MAX_VALUE, Long.MAX_VALUE},
                "duplicate key in after: " + Long.MAX_VALUE);
        assertDuplicate(new long[] {1}, new long[] {4, 3, 4, 3},
                "duplicate key in after: 4");
        assertDuplicate(new long[] {4, 3, 4, 3}, new long[] {1},
                "duplicate key in before: 4");
        assertThrows(NullPointerException.class, () -> ListDiff.diff(null, null));
        assertThrows(NullPointerException.class, () -> ListDiff.diff(null, new long[0]));
        assertThrows(NullPointerException.class, () -> ListDiff.diff(new long[0], null));
        long[] duplicate = {7, 7};
        assertThrows(NullPointerException.class, () -> ListDiff.diff(duplicate, null));
        assertArrayEquals(new long[] {7, 7}, duplicate);
    }

    private static void assertSortedPatch(long[] before, long[] after) {
        long[] originalBefore = before.clone();
        long[] originalAfter = after.clone();
        Set<Long> beforeKeys = keys(before);
        Set<Long> afterKeys = keys(after);
        List<Run> removals = absentRuns(before, afterKeys);
        Collections.reverse(removals);
        List<Run> insertions = absentRuns(after, beforeKeys);
        Set<Long> common = new HashSet<>(beforeKeys);
        common.retainAll(afterKeys);

        Patch patch = ListDiff.diff(before, after);
        assertEquals(removals.size(), patch.removeRuns());
        for (int i = 0; i < removals.size(); i++) {
            assertEquals(removals.get(i).start(), patch.removeAt(i));
            assertEquals(removals.get(i).length(), patch.removeCount(i));
        }
        assertEquals(insertions.size(), patch.insertRuns());
        for (int i = 0; i < insertions.size(); i++) {
            assertEquals(insertions.get(i).start(), patch.insertAt(i));
            assertEquals(insertions.get(i).length(), patch.insertCount(i));
        }
        assertEquals(common.size(), patch.keptCount());
        assertEquals(before.length - common.size(), patch.removedCount());
        assertEquals(after.length - common.size(), patch.insertedCount());
        assertEquals(0, patch.moveCount());
        assertEquals(Arrays.equals(before, after), patch.isEmpty());
        assertArrayEquals(after, patch.apply(before, after));

        List<Row> resident = new ArrayList<>();
        Map<Long, Row> oldRows = new HashMap<>();
        for (long key : before) {
            Row row = new Row(key);
            resident.add(row);
            oldRows.put(key, row);
        }
        List<Row> requested = new ArrayList<>();
        for (long key : after) requested.add(new Row(key));
        List<String> operations = new ArrayList<>();
        patch.applyTo(new Patch.Target() {
            @Override
            public void remove(int index, int count) {
                operations.add("-" + index + "x" + count);
                resident.subList(index, index + count).clear();
            }

            @Override
            public void insert(int index, int count) {
                operations.add("+" + index + "x" + count);
                resident.addAll(index, requested.subList(index, index + count));
            }
        });
        List<String> expectedOperations = new ArrayList<>();
        for (Run run : removals) expectedOperations.add("-" + run.start() + "x" + run.length());
        for (Run run : insertions) expectedOperations.add("+" + run.start() + "x" + run.length());
        assertEquals(expectedOperations, operations);
        assertEquals(after.length, resident.size());
        for (int i = 0; i < after.length; i++) {
            assertEquals(after[i], resident.get(i).key());
            Row expected = oldRows.containsKey(after[i]) ? oldRows.get(after[i]) : requested.get(i);
            assertSame(expected, resident.get(i));
        }
        assertArrayEquals(originalBefore, before);
        assertArrayEquals(originalAfter, after);
    }

    private static void assertFallbackTrace(long[] before, long[] after, int kept,
            List<String> expectedOperations, int[] moveFrom, int[] moveTo) {
        long[] originalBefore = before.clone();
        long[] originalAfter = after.clone();
        Patch patch = ListDiff.diff(before, after);
        List<Long> resident = new ArrayList<>();
        for (long key : before) resident.add(key);
        List<String> operations = new ArrayList<>();
        patch.applyTo(new Patch.Target() {
            @Override
            public void remove(int index, int count) {
                operations.add("-" + index + "x" + count);
                resident.subList(index, index + count).clear();
            }

            @Override
            public void insert(int index, int count) {
                operations.add("+" + index + "x" + count);
                for (int i = 0; i < count; i++) resident.add(index + i, after[index + i]);
            }
        });
        assertEquals(expectedOperations, operations);
        assertEquals(kept, patch.keptCount());
        assertEquals(before.length - kept, patch.removedCount());
        assertEquals(after.length - kept, patch.insertedCount());
        assertEquals(moveFrom.length, patch.moveCount());
        for (int i = 0; i < moveFrom.length; i++) {
            assertEquals(moveFrom[i], patch.moveFrom(i));
            assertEquals(moveTo[i], patch.moveTo(i));
            assertEquals(before[patch.moveFrom(i)], after[patch.moveTo(i)]);
        }
        assertEquals(Arrays.equals(before, after), patch.isEmpty());
        assertArrayEquals(after, patch.apply(before, after));
        assertArrayEquals(after, toArray(resident));
        assertArrayEquals(originalBefore, before);
        assertArrayEquals(originalAfter, after);
    }

    private static void assertDuplicate(long[] before, long[] after, String message) {
        long[] originalBefore = before == null ? null : before.clone();
        long[] originalAfter = after == null ? null : after.clone();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ListDiff.diff(before, after));
        assertEquals(message, failure.getMessage());
        assertArrayEquals(originalBefore, before);
        assertArrayEquals(originalAfter, after);
    }

    /** Membership comes from independent JDK sets, not sorted merging or LIS. */
    private static List<Run> absentRuns(long[] snapshot, Set<Long> otherKeys) {
        List<Run> result = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < snapshot.length; i++) {
            if (!otherKeys.contains(snapshot[i])) {
                if (start < 0) start = i;
            } else if (start >= 0) {
                result.add(new Run(start, i - start));
                start = -1;
            }
        }
        if (start >= 0) result.add(new Run(start, snapshot.length - start));
        return result;
    }

    private static Set<Long> keys(long[] values) {
        Set<Long> result = new HashSet<>();
        for (long value : values) result.add(value);
        return result;
    }

    private static long[] subset(int mask) {
        long[] values = new long[Integer.bitCount(mask)];
        int size = 0;
        for (int i = 0; i < EXTREMES.length; i++) {
            if ((mask & (1 << i)) != 0) values[size++] = EXTREMES[i];
        }
        return values;
    }

    private static long[] toArray(Iterable<Long> values) {
        List<Long> boxed = new ArrayList<>();
        for (long value : values) boxed.add(value);
        long[] result = new long[boxed.size()];
        for (int i = 0; i < result.length; i++) result[i] = boxed.get(i);
        return result;
    }

    private record Run(int start, int length) { }
    private record Row(long key) { }
}
