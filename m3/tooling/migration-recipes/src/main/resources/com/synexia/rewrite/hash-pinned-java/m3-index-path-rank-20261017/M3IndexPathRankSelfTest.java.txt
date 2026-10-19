/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Differential oracle for source-sealed M3IndexPathRank.
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Executable without Maven; deterministic bounded contract regression. */
public final class M3IndexPathRankSelfTest {
    private M3IndexPathRankSelfTest() { }

    public static void main(String[] args) {
        basic();
        denseAndSparseWordBoundaries();
        rejectsInvalidInputs();
        Random rng = new Random(0x4d3352414e4b2026L);
        int checked = 0;
        for (int trial = 0; trial < 900; trial++) {
            int length = rng.nextInt(160);
            int capacity = 1 + rng.nextInt(55);
            int[] rows = new int[length];
            int[] offsets = new int[length];
            int[][] stages = new int[1 + rng.nextInt(4)][capacity];
            int[] needs = new int[stages.length];
            int[] denies = new int[stages.length];
            for (int i = 0; i < length; i++) {
                rows[i] = rng.nextInt(11) - 5; // repeated and signed IDs
                offsets[i] = rng.nextInt(capacity);
            }
            for (int i = 0; i < stages.length; i++) {
                for (int j = 0; j < capacity; j++) stages[i][j] = rng.nextInt();
                needs[i] = 1 << rng.nextInt(32);
                denies[i] = rng.nextBoolean() ? 0 : 1 << rng.nextInt(32);
            }
            int count = length == 0 ? 0 : rng.nextInt(length + 1);
            int stride = rng.nextBoolean() ? 1 : -1;
            int first = count == 0 ? length
                    : stride > 0 ? rng.nextInt(length - count + 1)
                    : count - 1 + rng.nextInt(length - count + 1);
            M3IndexPathRank rank = M3IndexPathRank.snapshot(rows, offsets, stages,
                    needs, denies, first, count, stride);
            List<Integer> expected = new ArrayList<>();
            List<Integer> ordinals = new ArrayList<>();
            int[] prefix = new int[count + 1];
            for (int n = 0; n < count; n++) {
                int ix = first + n * stride;
                boolean matches = true;
                for (int s = 0; s < stages.length; s++) {
                    int bits = stages[s][offsets[ix]];
                    if ((bits & needs[s]) != needs[s] || (bits & denies[s]) != 0) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    expected.add(rows[ix]);
                    ordinals.add(n);
                }
                prefix[n + 1] = expected.size();
            }
            assertEqual(count, rank.sourceSize(), "source-size");
            assertEqual(expected.size(), rank.size(), "selected-size");
            assertArray(expected.stream().mapToInt(Integer::intValue).toArray(), rank.toArray(), "selected-array");
            for (int end = 0; end <= count; end++) {
                assertEqual(prefix[end], rank.rank(end), "exclusive-rank");
                if (end < count) {
                    assertEqual(prefix[end + 1] > prefix[end], rank.containsSourceOrdinal(end), "contains-occurrence");
                }
                checked++;
            }
            for (int k = 0; k < expected.size(); k++) {
                assertEqual(ordinals.get(k).intValue(), rank.selectedSourceOrdinal(k), "selected-source-index");
                assertEqual(expected.get(k).intValue(), rank.selectedRowId(k), "selected-row-ID");
            }
            for (int w = 0; w < 10; w++) {
                int from = rng.nextInt(count + 1);
                int to = from + rng.nextInt(count - from + 1);
                M3IndexPathRank.Window window = rank.window(from, to);
                int[] expectWindow = expectedWindow(prefix, expected, from, to);
                assertArray(expectWindow, window.toArray(), "window selection");
                assertEqual(to - from, window.sourceSize(), "window source-size");
                assertEqual(prefix[to] - prefix[from], window.size(), "window selected-size");
                for (int end = 0; end <= to - from; end++) {
                    assertEqual(prefix[from + end] - prefix[from], window.rank(end), "window rank");
                }
                for (int k = 0; k < window.size(); k++) {
                    assertEqual(ordinals.get(prefix[from] + k) - from,
                            window.selectedSourceOrdinal(k), "window selected ordinal");
                    assertEqual(expectWindow[k], window.selectedRowId(k), "window selected row");
                }
                assertArray(expectWindow, window.window(0, to - from).toArray(), "nested window");
            }
            int[] before = rank.toArray();
            Arrays.fill(rows, Integer.MIN_VALUE);
            Arrays.fill(offsets, -1);
            for (int[] stage : stages) Arrays.fill(stage, 0);
            assertArray(before, rank.toArray(), "snapshot isolation from caller arrays");
        }
        System.out.println("PASS M3IndexPathRank Java21 deterministic differential: " + checked + " rank checks; 900 randomized paths");
    }

    private static int[] expectedWindow(int[] prefix, List<Integer> rows, int from, int to) {
        int[] result = new int[prefix[to] - prefix[from]];
        for (int i = 0; i < result.length; i++) result[i] = rows.get(prefix[from] + i);
        return result;
    }

    private static void basic() {
        int[] rows = {7, -1, 7, 0, 4};
        int[] offsets = {1, 0, 1, 2, 0};
        int[][] planes = {{0b11, 0b01, 0b10}, {0b10, 0b11, 0b10}};
        int[] needs = {1, 2};
        int[] excludes = {2, 0};
        M3IndexPathRank index = M3IndexPathRank.snapshot(rows, offsets, planes, needs, excludes, 4, 5, -1);
        assertArray(new int[]{7, 7}, index.toArray(), "reversed-path multi-stage");
        assertEqual(2, index.size(), "selected-count");
        assertEqual(0, index.rank(1), "rank-before-1");
        assertEqual(4, index.selectedSourceOrdinal(1), "second selected source-index");
        assertArray(new int[]{7}, index.window(1, 4).toArray(), "window-relative");
        M3IndexPathRank empty = M3IndexPathRank.snapshot(rows, offsets, planes, needs, excludes,
                rows.length, 0, 3);
        assertEqual(0, empty.size(), "empty-path");
        assertEqual(0, empty.rank(0), "empty rank");
        assertArray(new int[0], empty.toArray(), "empty array");
    }

    private static void denseAndSparseWordBoundaries() {
        int[] rows = new int[257];
        int[] slots = new int[257];
        int[][] stages = new int[2][257];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = (i % 7) - 3;
            slots[i] = i;
            stages[0][i] = (i == 63 || i == 128 || i == 255) ? 0 : 0x8000_0000;
            stages[1][i] = 0x8000_0000;
        }
        M3IndexPathRank index = M3IndexPathRank.snapshot(rows, slots, stages,
                new int[]{0x8000_0000, 0x8000_0000}, new int[]{0, 0}, 0, rows.length, 1);
        assertEqual(254, index.size(), "three sparse holes across word boundaries");
        for (int i = 0; i <= rows.length; i++) {
            int exclusions = (i > 63 ? 1 : 0) + (i > 128 ? 1 : 0) + (i > 255 ? 1 : 0);
            assertEqual(i - exclusions, index.rank(i), "rank at 64/128/256 words");
        }
        for (int i = 0; i < index.size(); i++) {
            int sourceIndex = index.selectedSourceOrdinal(i);
            assertEqual(rows[sourceIndex], index.selectedRowId(i), "select across holes");
        }
        assertArray(new int[0], index.window(63, 64).toArray(), "fully-excluded single-row window");
        assertEqual(128 - 1, index.window(1, 130).size(), "window rank across 128");
        M3IndexPathRank none = M3IndexPathRank.snapshot(rows, slots, stages,
                new int[]{0x8000_0000, 0}, new int[]{0x8000_0000, 0}, 0, 257, 1);
        assertEqual(0, none.size(), "overlapping masks select nothing");
        assertEqual(0, none.rank(257), "empty selection at full boundary");
    }

    private static void rejectsInvalidInputs() {
        int[] row = {1, 2};
        int[] slots = {0, 1};
        int[][] states = {{1, 2}};
        int[] need = {1};
        int[] deny = {0};
        fail(NullPointerException.class, () -> M3IndexPathRank.snapshot(null, slots, states, need, deny, 0, 0, 1));
        fail(IllegalArgumentException.class, () -> M3IndexPathRank.snapshot(row, new int[0], states, need, deny, 0, 0, 1));
        fail(IllegalArgumentException.class, () -> M3IndexPathRank.snapshot(row, slots, new int[0][], need, deny, 0, 0, 1));
        fail(IndexOutOfBoundsException.class, () -> M3IndexPathRank.snapshot(row, slots, states, need, deny, 0, 2, -1));
        fail(IndexOutOfBoundsException.class, () -> M3IndexPathRank.snapshot(row, slots, states, need, deny, 2, 1, 1));
        fail(IndexOutOfBoundsException.class, () -> M3IndexPathRank.snapshot(row, slots, states, need, deny, 0, 1, 0));
        fail(IndexOutOfBoundsException.class, () -> M3IndexPathRank.snapshot(row, new int[]{2, 0}, states, need, deny, 0, 1, 1));
        M3IndexPathRank index = M3IndexPathRank.snapshot(row, slots, states, need, deny, 0, 2, 1);
        fail(IndexOutOfBoundsException.class, () -> index.rank(3));
        fail(IndexOutOfBoundsException.class, () -> index.selectedRowId(index.size()));
        fail(IndexOutOfBoundsException.class, () -> index.window(1, 3));
        fail(IndexOutOfBoundsException.class, () -> index.window(1, 2).rank(2));
    }

    private static void fail(Class<? extends Throwable> expected, Runnable code) {
        try { code.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) return;
            throw new AssertionError("Expected " + expected.getName() + ", got " + problem, problem);
        }
        throw new AssertionError("Expected failure " + expected.getName());
    }

    private static void assertArray(int[] expected, int[] actual, String step) {
        if (!Arrays.equals(expected, actual)) throw new AssertionError(step + " expected="
                + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }

    private static void assertEqual(int expected, int actual, String step) {
        if (expected != actual) throw new AssertionError(step + ": expected " + expected + ", actual " + actual);
    }

    private static void assertEqual(boolean expected, boolean actual, String step) {
        if (expected != actual) throw new AssertionError(step + ": expected " + expected + ", actual " + actual);
    }
}
