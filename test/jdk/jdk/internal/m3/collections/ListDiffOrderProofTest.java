/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

/** Independent order-proof regression and a trace probe for actual runtime classes. */
public final class ListDiffOrderProofTest {
    private static final long[] SMALL = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
    private static final long[] EDGES = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE
    };

    @Test
    void sortedSubsetsAndEveryPartialPermutationPreserveExactTraceInBothDirections() {
        permutationCases(ListDiffOrderProofTest::verifySuccess);
    }

    @Test
    void seededAscendingDescendingAndShuffledChurnPreservesResidentIdentity() {
        seededCases(ListDiffOrderProofTest::verifySuccess);
    }

    @Test
    void duplicateAndNullMatrixRetainsAfterBeforePrecedenceWithoutMutation() {
        invalidCases(ListDiffOrderProofTest::verifyCase);
    }

    @Test
    void aliasedSnapshotsKeepEveryOriginalRow() {
        aliasCases(ListDiffOrderProofTest::verifySuccess);
    }

    /**
     * Compile this fixture once against the real baseline; run its unchanged class files
     * against each isolated runtime. The digest includes all exact observable traces,
     * including exception class/message and resident-row origin. --trace emits each row.
     */
    public static void main(String[] args) throws Exception {
        boolean verbose = args.length == 1 && args[0].equals("--trace");
        if (args.length > 1 || args.length == 1 && !verbose)
            throw new IllegalArgumentException("optional argument: --trace");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int[] count = {0};
        BiConsumer<long[], long[]> visit = (before, after) -> {
            String observation = verifyCase(before, after);
            String line = count[0]++ + "\t" + Arrays.toString(before) + "\t"
                    + Arrays.toString(after) + "\t" + observation + "\n";
            digest.update(line.getBytes(StandardCharsets.UTF_8));
            if (verbose) System.out.print(line);
        };
        permutationCases(visit);
        seededCases(visit);
        invalidCases(visit);
        aliasCases(visit);
        String result = "{\"cases\":" + count[0] + ",\"trace_sha256\":\""
                + HexFormat.of().formatHex(digest.digest()) + "\"}";
        if (verbose) System.err.println(result);
        else System.out.println(result);
    }

    private static void permutationCases(BiConsumer<long[], long[]> visit) {
        List<long[]> permutations = new ArrayList<>();
        partialPermutations(new long[SMALL.length], 0, new boolean[SMALL.length], permutations);
        for (int mask = 0; mask < 1 << SMALL.length; mask++) {
            long[] sorted = new long[Integer.bitCount(mask)];
            for (int i = 0, at = 0; i < SMALL.length; i++)
                if ((mask & (1 << i)) != 0) sorted[at++] = SMALL[i];
            for (long[] permutation : permutations) {
                visit.accept(sorted, permutation);
                visit.accept(permutation, sorted);
            }
        }
    }

    private static void partialPermutations(long[] values, int used, boolean[] present,
            List<long[]> result) {
        result.add(Arrays.copyOf(values, used));
        for (int i = 0; i < SMALL.length; i++) {
            if (present[i]) continue;
            present[i] = true;
            values[used] = SMALL[i];
            partialPermutations(values, used + 1, present, result);
            present[i] = false;
        }
    }

    private static void seededCases(BiConsumer<long[], long[]> visit) {
        Random random = new Random(0x4d334f5244455250L);
        for (int round = 0; round < 512; round++) {
            TreeSet<Long> oldKeys = new TreeSet<>();
            int oldSize = random.nextInt(11);
            while (oldKeys.size() < oldSize)
                oldKeys.add(random.nextBoolean() ? EDGES[random.nextInt(EDGES.length)] : random.nextLong());
            TreeSet<Long> newKeys = new TreeSet<>();
            for (long key : oldKeys) if (random.nextBoolean()) newKeys.add(key);
            int newSize = random.nextInt(11);
            while (newKeys.size() < newSize)
                newKeys.add(random.nextBoolean() ? EDGES[random.nextInt(EDGES.length)] : random.nextLong());
            long[] oldSorted = toArray(oldKeys), newSorted = toArray(newKeys);
            long[] oldShuffled = shuffled(oldSorted, random), newShuffled = shuffled(newSorted, random);
            visit.accept(oldSorted, newShuffled);
            visit.accept(oldShuffled, newSorted);
            visit.accept(oldSorted, reversed(newSorted));
            visit.accept(reversed(oldSorted), newSorted);
            visit.accept(oldShuffled, newShuffled);
        }
    }

    private static void invalidCases(BiConsumer<long[], long[]> visit) {
        List<long[]> inputs = new ArrayList<>();
        inputs.add(null);
        repeatedWords(new long[4], 0, inputs);
        for (long[] before : inputs)
            for (long[] after : inputs) visit.accept(before, after);
    }

    private static void repeatedWords(long[] values, int size, List<long[]> result) {
        result.add(Arrays.copyOf(values, size));
        if (size == values.length) return;
        for (long value : new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}) {
            values[size] = value;
            repeatedWords(values, size + 1, result);
        }
    }

    private static void aliasCases(BiConsumer<long[], long[]> visit) {
        long[][] owners = {new long[0], {0}, EDGES.clone(),
                {Long.MAX_VALUE, 0, Long.MIN_VALUE}, {0, Long.MAX_VALUE, -1, Long.MIN_VALUE}};
        for (long[] owner : owners) visit.accept(owner, owner);
    }

    private static String verifyCase(long[] before, long[] after) {
        Failure expected = failure(after, "after");
        if (expected == null) expected = failure(before, "before");
        if (expected == null) return verifySuccess(before, after);
        long[] oldCopy = before == null ? null : before.clone();
        long[] newCopy = after == null ? null : after.clone();
        String label = Arrays.toString(before) + " -> " + Arrays.toString(after);
        RuntimeException actual = assertThrows(expected.type(), () -> ListDiff.diff(before, after), label);
        assertEquals(expected.type(), actual.getClass(), label);
        if (expected.message() != null) assertEquals(expected.message(), actual.getMessage(), label);
        assertArrayEquals(oldCopy, before, label);
        assertArrayEquals(newCopy, after, label);
        return "failure=" + actual.getClass().getName() + ":" + actual.getMessage();
    }

    private static Failure failure(long[] values, String side) {
        if (values == null) return new Failure(NullPointerException.class, null);
        Set<Long> seen = new HashSet<>();
        for (long value : values)
            if (!seen.add(value)) return new Failure(IllegalArgumentException.class,
                    "duplicate key in " + side + ": " + value);
        return null;
    }

    private static String verifySuccess(long[] before, long[] after) {
        long[] oldCopy = before.clone(), newCopy = after.clone();
        String label = Arrays.toString(before) + " -> " + Arrays.toString(after);
        Expected expected = expected(before, after);
        Patch patch = ListDiff.diff(before, after);
        assertEquals(expected.removes().size(), patch.removeRuns(), label);
        assertEquals(expected.inserts().size(), patch.insertRuns(), label);
        assertEquals(expected.moves().size(), patch.moveCount(), label);
        List<Run> actualRemoves = new ArrayList<>(), actualInserts = new ArrayList<>();
        List<Move> actualMoves = new ArrayList<>();
        for (int i = 0; i < patch.removeRuns(); i++)
            actualRemoves.add(new Run(patch.removeAt(i), patch.removeCount(i)));
        for (int i = 0; i < patch.insertRuns(); i++)
            actualInserts.add(new Run(patch.insertAt(i), patch.insertCount(i)));
        for (int i = 0; i < patch.moveCount(); i++) {
            actualMoves.add(new Move(patch.moveFrom(i), patch.moveTo(i)));
            assertEquals(before[patch.moveFrom(i)], after[patch.moveTo(i)], label);
        }
        assertEquals(expected.removes(), actualRemoves, label);
        assertEquals(expected.inserts(), actualInserts, label);
        assertEquals(expected.moves(), actualMoves, label);
        assertEquals(expected.kept(), patch.keptCount(), label);
        assertEquals(before.length - expected.kept(), patch.removedCount(), label);
        assertEquals(after.length - expected.kept(), patch.insertedCount(), label);
        assertEquals(Arrays.equals(before, after), patch.isEmpty(), label);
        long[] applied = patch.apply(before, after);
        assertArrayEquals(after, applied, label);

        List<Row> resident = new ArrayList<>(), requested = new ArrayList<>();
        Map<Long, Row> oldRows = new HashMap<>();
        Map<Row, String> origins = new IdentityHashMap<>();
        for (int i = 0; i < before.length; i++) {
            Row row = new Row(before[i]);
            resident.add(row);
            oldRows.put(before[i], row);
            origins.put(row, "old:" + i);
        }
        for (int i = 0; i < after.length; i++) {
            Row row = new Row(after[i]);
            requested.add(row);
            origins.put(row, "new:" + i);
        }
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
        for (Run run : expected.removes()) expectedOperations.add("-" + run.start() + "x" + run.count());
        for (Run run : expected.inserts()) expectedOperations.add("+" + run.start() + "x" + run.count());
        assertEquals(expectedOperations, operations, label);
        assertEquals(after.length, resident.size(), label);
        List<String> residentOrigins = new ArrayList<>();
        for (int i = 0; i < after.length; i++) {
            assertEquals(after[i], resident.get(i).key(), label);
            Row wanted = expected.keptNew()[i] ? oldRows.get(after[i]) : requested.get(i);
            assertSame(wanted, resident.get(i), label);
            residentOrigins.add(origins.get(resident.get(i)));
        }
        assertArrayEquals(oldCopy, before, label);
        assertArrayEquals(newCopy, after, label);
        return "remove=" + actualRemoves + ";insert=" + actualInserts + ";moves=" + actualMoves
                + ";counts=" + patch.keptCount() + "," + patch.removedCount() + "," + patch.insertedCount()
                + ";empty=" + patch.isEmpty() + ";applied=" + Arrays.toString(applied)
                + ";ops=" + operations + ";rows=" + residentOrigins;
    }

    /**
     * Exhaustive subsequence search, independent of the production patience-sorting code.
     * Among maximum-length choices, the baseline keeps the smallest reversed sequence of
     * new positions. Enumerating that ordering also freezes its deterministic tie behavior.
     */
    private static Expected expected(long[] before, long[] after) {
        Map<Long, Integer> newPosition = new HashMap<>();
        for (int i = 0; i < after.length; i++) newPosition.put(after[i], i);
        int bestMask = 0;
        int[] best = new int[0];
        for (int mask = 1; mask < 1 << before.length; mask++) {
            int size = Integer.bitCount(mask);
            if (size < best.length) continue;
            int[] candidate = new int[size];
            boolean increasing = true;
            for (int i = 0, at = 0, last = -1; i < before.length; i++) {
                if ((mask & (1 << i)) == 0) continue;
                Integer position = newPosition.get(before[i]);
                if (position == null || position <= last) {
                    increasing = false;
                    break;
                }
                candidate[at++] = position;
                last = position;
            }
            if (increasing && (candidate.length > best.length || reverseLess(candidate, best))) {
                bestMask = mask;
                best = candidate;
            }
        }
        boolean[] keptOld = new boolean[before.length], keptNew = new boolean[after.length];
        for (int i = 0; i < before.length; i++)
            if ((bestMask & (1 << i)) != 0) keptOld[i] = true;
        for (int position : best) keptNew[position] = true;
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < before.length; i++)
            if (!keptOld[i] && newPosition.containsKey(before[i]))
                moves.add(new Move(i, newPosition.get(before[i])));
        return new Expected(coalescedEdits(keptOld, true), coalescedEdits(keptNew, false),
                moves, best.length, keptNew);
    }

    private static boolean reverseLess(int[] candidate, int[] best) {
        for (int i = candidate.length - 1; i >= 0; i--)
            if (candidate[i] != best[i]) return candidate[i] < best[i];
        return false;
    }

    private static List<Run> coalescedEdits(boolean[] kept, boolean descending) {
        List<Integer> edited = new ArrayList<>();
        for (int i = 0; i < kept.length; i++) if (!kept[i]) edited.add(i);
        List<Run> runs = new ArrayList<>();
        for (int index : edited) {
            if (!runs.isEmpty()) {
                Run last = runs.getLast();
                if (last.start() + last.count() == index) {
                    runs.set(runs.size() - 1, new Run(last.start(), last.count() + 1));
                    continue;
                }
            }
            runs.add(new Run(index, 1));
        }
        if (descending) Collections.reverse(runs);
        return runs;
    }

    private static long[] shuffled(long[] sorted, Random random) {
        long[] result = sorted.clone();
        for (int i = result.length - 1; i > 0; i--) {
            int at = random.nextInt(i + 1);
            long value = result[i];
            result[i] = result[at];
            result[at] = value;
        }
        if (result.length > 1 && Arrays.equals(result, sorted)) {
            long value = result[0];
            result[0] = result[result.length - 1];
            result[result.length - 1] = value;
        }
        return result;
    }

    private static long[] reversed(long[] sorted) {
        long[] result = new long[sorted.length];
        for (int i = 0; i < sorted.length; i++) result[i] = sorted[sorted.length - 1 - i];
        return result;
    }

    private static long[] toArray(Set<Long> values) {
        long[] result = new long[values.size()];
        int i = 0;
        for (long value : values) result[i++] = value;
        return result;
    }

    private record Failure(Class<? extends RuntimeException> type, String message) { }
    private record Run(int start, int count) { }
    private record Move(int from, int to) { }
    private record Row(long key) { }
    private record Expected(List<Run> removes, List<Run> inserts, List<Move> moves,
                            int kept, boolean[] keptNew) { }
}
