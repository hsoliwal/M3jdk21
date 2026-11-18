/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Independent oracle for immutable source-relative ordinal pages and existing
 * repeated row-ID pages. No JNI or external libraries are required to run main.
 */
public final class IndexPathRankOrdinalPageSelfTest {
    private static final int GUARD = 0x4d334f52;
    private static long checks;
    private IndexPathRankOrdinalPageSelfTest() { }

    public static void main(String[] args) {
        sparseLongPrefix();
        malformedRequestsAreFailureAtomic();
        Random random = new Random(0x4d334f5244504147L);
        for (int trial = 0; trial < 2_500; trial++) {
            int length = random.nextInt(260);
            int capacity = 1 + random.nextInt(80);
            int[] rows = new int[length], physical = new int[length];
            int stageCount = 1 + random.nextInt(3);
            int[][] states = new int[stageCount][capacity];
            int[] needs = new int[stageCount], excludes = new int[stageCount];
            for (int i = 0; i < length; i++) {
                rows[i] = random.nextInt(9) - 4;
                physical[i] = random.nextInt(capacity);
            }
            for (int stage = 0; stage < stageCount; stage++) {
                needs[stage] = trial % 7 == 0 ? 0
                        : (random.nextBoolean() ? Integer.MIN_VALUE : 1 << random.nextInt(30));
                excludes[stage] = trial % 11 == 0 ? 0 : 1 << random.nextInt(30);
                for (int index = 0; index < capacity; index++) {
                    states[stage][index] = random.nextInt();
                }
            }
            int stride = random.nextBoolean() ? 1 : -1;
            int count = length == 0 ? 0 : random.nextInt(length + 1);
            int first = count == 0 ? length
                    : stride == 1 ? random.nextInt(length - count + 1)
                    : count - 1 + random.nextInt(length - count + 1);
            IndexPathRank rank = IndexPathRank.snapshot(
                    rows, physical, states, needs, excludes, first, count, stride);

            List<Integer> included = new ArrayList<>();
            for (int ordinal = 0; ordinal < count; ordinal++) {
                int slot = physical[first + ordinal * stride];
                boolean match = true;
                for (int stage = 0; stage < stageCount; stage++) {
                    int flags = states[stage][slot];
                    if ((flags & needs[stage]) != needs[stage]
                            || (flags & excludes[stage]) != 0) {
                        match = false;
                        break;
                    }
                }
                if (match) included.add(ordinal);
            }
            int[] frozenRows = rows.clone();
            Arrays.fill(rows, 7_777_777);
            Arrays.fill(physical, -1);
            for (int[] lane : states) Arrays.fill(lane, 0);

            for (int window = 0; window < 10; window++) {
                int begin = random.nextInt(count + 1);
                int end = begin + random.nextInt(count - begin + 1);
                IndexPathRank.Window view = rank.window(begin, end);
                List<Integer> ordinalReference = new ArrayList<>();
                List<Integer> rowReference = new ArrayList<>();
                for (int ordinal : included) {
                    if (ordinal >= begin && ordinal < end) {
                        ordinalReference.add(ordinal - begin);
                        rowReference.add(frozenRows[first + ordinal * stride]);
                    }
                }
                equal(ordinalReference.size(), view.size(), "selected window count");
                for (int iteration = 0; iteration < 6; iteration++) {
                    int selectedFrom = random.nextInt(view.size() + 1);
                    int selectedCount = random.nextInt(view.size() - selectedFrom + 1);
                    int offset = iteration % 3;
                    int[] ordinals = new int[selectedCount + 5];
                    int[] rowIds = new int[selectedCount + 5];
                    Arrays.fill(ordinals, GUARD);
                    Arrays.fill(rowIds, GUARD);
                    view.copySelectedSourceOrdinals(selectedFrom, ordinals, offset, selectedCount);
                    view.copySelectedRowIds(selectedFrom, rowIds, offset, selectedCount);
                    for (int i = 0; i < ordinals.length; i++) {
                        int wantedOrdinal = (i >= offset && i < offset + selectedCount)
                                ? ordinalReference.get(selectedFrom + i - offset) : GUARD;
                        int wantedRow = (i >= offset && i < offset + selectedCount)
                                ? rowReference.get(selectedFrom + i - offset) : GUARD;
                        equal(wantedOrdinal, ordinals[i], "relative ordinal / guard");
                        equal(wantedRow, rowIds[i], "row ID / guard");
                    }
                }
            }
        }
        System.out.println("PASS M3 IndexPathRankOrdinalPage checks=" + checks);
    }

    private static void sparseLongPrefix() {
        int length = 262_145;
        int[] rows = new int[length];
        int[] physical = new int[length];
        int[][] planes = {new int[length]};
        for (int i = 0; i < length; i++) {
            rows[i] = i % 7;
            physical[i] = i;
        }
        for (int i : new int[] {65_535, 65_536, 131_071, 262_144}) planes[0][i] = 1;
        IndexPathRank rank = IndexPathRank.snapshot(
                rows, physical, planes, new int[] {1}, new int[] {0},
                0, length, 1);
        IndexPathRank.Window view = rank.window(1, length);
        int[] ordinals = new int[4], rowIds = new int[4];
        view.copySelectedSourceOrdinals(0, ordinals, 0, 4);
        view.copySelectedRowIds(0, rowIds, 0, 4);
        equal(65_534, ordinals[0], "first sparse leading gap");
        equal(65_535, ordinals[1], "adjacent word sparse boundary");
        equal(131_070, ordinals[2], "third sparse rank");
        equal(262_143, ordinals[3], "last sparse rank");
        for (int i = 0; i < 4; i++) {
            equal(rows[ordinals[i] + 1], rowIds[i], "sparse row ID");
        }
        int[] partial = {GUARD, GUARD, GUARD};
        view.copySelectedSourceOrdinals(2, partial, 1, 2);
        equal(GUARD, partial[0], "partial prefix guard");
        equal(131_070, partial[1], "partial resumed ordinal");
        equal(262_143, partial[2], "partial last ordinal");
    }

    private static void malformedRequestsAreFailureAtomic() {
        int[] rows = {9, 3, 9, 4}, positions = {0, 1, 2, 3};
        int[][] planes = {{0, 1, 0, 1}};
        IndexPathRank.Window view = IndexPathRank.snapshot(
                rows, positions, planes, new int[] {1}, new int[] {0},
                0, rows.length, 1).window(1, 4);
        int[] destination = {GUARD, GUARD, GUARD};
        expect(IndexOutOfBoundsException.class,
                () -> view.copySelectedSourceOrdinals(-1, destination, 0, 1));
        expect(IndexOutOfBoundsException.class,
                () -> view.copySelectedSourceOrdinals(0, destination, -1, 1));
        expect(IndexOutOfBoundsException.class,
                () -> view.copySelectedSourceOrdinals(0, destination, 3, 1));
        expect(IndexOutOfBoundsException.class,
                () -> view.copySelectedSourceOrdinals(0, destination, 0, view.size() + 1));
        expect(IndexOutOfBoundsException.class,
                () -> view.copySelectedSourceOrdinals(0, destination, 0, -1));
        expect(NullPointerException.class,
                () -> view.copySelectedSourceOrdinals(0, null, 0, 0));
        same(new int[] {GUARD, GUARD, GUARD}, destination, "validation before output");
        view.copySelectedSourceOrdinals(view.size(), destination, destination.length, 0);
        same(new int[] {GUARD, GUARD, GUARD}, destination, "zero count at end");
    }

    private static void expect(Class<? extends Throwable> expected, Runnable runnable) {
        checks++;
        try {
            runnable.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong exception " + failure, failure);
        }
        throw new AssertionError("expected failure " + expected.getName());
    }
    private static void same(int[] a, int[] b, String message) {
        checks++;
        if (!Arrays.equals(a, b)) {
            throw new AssertionError(message + " expected=" + Arrays.toString(a)
                    + " actual=" + Arrays.toString(b));
        }
    }
    private static void equal(int expected, int actual, String message) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected
                    + " actual=" + actual + " check=" + checks);
        }
    }
}
