/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Existing M3 rank and bitmap owners; independently generated regression shape.
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Strict Java21 differential test with no JUnit or JNI requirement. */
public final class M3IndexPathRankBulkSelfTest {
    private static final int SENTINEL = 0x515d3a77;
    private static long checks;

    private M3IndexPathRankBulkSelfTest() { }

    public static void main(String[] args) {
        boundaryCases();
        malformedRequestsDoNotWrite();
        Random rng = new Random(0x4d3342554c4b2026L);
        for (int trial = 0; trial < 2_000; trial++) {
            int length = rng.nextInt(300);
            int capacity = 1 + rng.nextInt(70);
            int[] rows = new int[length];
            int[] physical = new int[length];
            int stages = 1 + rng.nextInt(4);
            int[][] states = new int[stages][capacity];
            int[] required = new int[stages];
            int[] forbidden = new int[stages];
            for (int ordinal = 0; ordinal < length; ordinal++) {
                rows[ordinal] = rng.nextInt(19) - 9; // negative IDs, repeated physical IDs
                physical[ordinal] = rng.nextInt(capacity);
            }
            for (int stage = 0; stage < stages; stage++) {
                required[stage] = rng.nextBoolean() ? 0 : 1 << rng.nextInt(32);
                forbidden[stage] = rng.nextBoolean() ? 0 : 1 << rng.nextInt(32);
                for (int physicalSlot = 0; physicalSlot < capacity; physicalSlot++) {
                    states[stage][physicalSlot] = rng.nextInt();
                }
            }
            int count = length == 0 ? 0 : rng.nextInt(length + 1);
            int stride = rng.nextBoolean() ? 1 : -1;
            int first = count == 0 ? length
                    : stride > 0 ? rng.nextInt(length - count + 1)
                    : count - 1 + rng.nextInt(length - count + 1);
            M3IndexPathRank snapshot = M3IndexPathRank.snapshot(
                    rows, physical, states, required, forbidden, first, count, stride);
            int[] original = rows.clone();
            for (int trialWindow = 0; trialWindow < 8; trialWindow++) {
                int from = rng.nextInt(count + 1);
                int to = from + rng.nextInt(count - from + 1);
                var window = snapshot.window(from, to);
                int[] expected = reference(
                        original, physical, states, required, forbidden, first, stride, from, to);
                assertArray(expected, window.toArray(), "unchanged toArray contract");
                for (int sample = 0; sample < 5; sample++) {
                    int selectedFrom = rng.nextInt(expected.length + 1);
                    int take = rng.nextInt(expected.length - selectedFrom + 1);
                    int destinationOffset = sample % 3;
                    int[] destination = new int[take + 4];
                    Arrays.fill(destination, SENTINEL);
                    window.copySelectedRowIds(selectedFrom, destination, destinationOffset, take);
                    for (int i = 0; i < destination.length; i++) {
                        int expectedValue = i >= destinationOffset && i < destinationOffset + take
                                ? expected[selectedFrom + i - destinationOffset] : SENTINEL;
                        equal(expectedValue, destination[i], "bulk/page exact positions");
                    }
                    checks++;
                }
            }
            Arrays.fill(rows, Integer.MIN_VALUE);
            Arrays.fill(physical, -1);
            for (int[] stage : states) Arrays.fill(stage, 0);
            equal(snapshot.size(), snapshot.toArray().length, "source mutation isolation");
        }
        System.out.println("PASS M3IndexPathRankBulkSelfTest checks=" + checks
                + " scenarios=2000 no JNI fallback");
    }

    private static int[] reference(int[] rows, int[] physical, int[][] states,
            int[] required, int[] forbidden, int first, int stride, int from, int to) {
        List<Integer> selected = new ArrayList<>();
        for (int ordinal = from; ordinal < to; ordinal++) {
            int physicalPosition = first + ordinal * stride;
            int slot = physical[physicalPosition];
            boolean include = true;
            for (int stage = 0; stage < states.length; stage++) {
                int bits = states[stage][slot];
                if ((bits & required[stage]) != required[stage]
                        || (bits & forbidden[stage]) != 0) {
                    include = false;
                    break;
                }
            }
            if (include) selected.add(rows[physicalPosition]);
        }
        return selected.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void boundaryCases() {
        int length = 257;
        int[] rows = new int[length];
        int[] physical = new int[length];
        int[][] states = {new int[length]};
        for (int i = 0; i < length; i++) {
            rows[i] = (i & 1) == 0 ? i : -i;
            physical[i] = i;
            states[0][i] = (i % 5 == 0 || i == 63 || i == 64 || i == 65
                    || i == 127 || i == 128 || i == 255) ? 0 : Integer.MIN_VALUE;
        }
        var rank = M3IndexPathRank.snapshot(rows, physical, states,
                new int[]{Integer.MIN_VALUE}, new int[]{0}, 0, length, 1);
        for (int from : new int[]{0, 1, 62, 63, 64, 65, 66, 126, 127, 128, 129, 255, 256, 257}) {
            for (int to = from; to <= length; to += 17) {
                var view = rank.window(from, to);
                int[] reference = reference(rows, physical, states,
                        new int[]{Integer.MIN_VALUE}, new int[]{0}, 0, 1, from, to);
                int[] result = new int[view.size() + 2];
                Arrays.fill(result, SENTINEL);
                view.copySelectedRowIds(0, result, 1, view.size());
                assertArray(reference, Arrays.copyOfRange(result, 1, result.length - 1),
                        "boundary selected bits");
                equal(SENTINEL, result[0], "prefix sentinel");
                equal(SENTINEL, result[result.length - 1], "suffix sentinel");
                checks++;
            }
        }
        var empty = rank.window(length, length);
        int[] output = {SENTINEL};
        empty.copySelectedRowIds(0, output, 1, 0);
        equal(SENTINEL, output[0], "empty at output end");
    }

    private static void malformedRequestsDoNotWrite() {
        int[] rows = {8, -2, 8, 4};
        int[] physical = {0, 1, 0, 2};
        int[][] states = {{Integer.MIN_VALUE, 0, Integer.MIN_VALUE}};
        var rank = M3IndexPathRank.snapshot(rows, physical, states,
                new int[]{Integer.MIN_VALUE}, new int[]{0}, 0, rows.length, 1);
        var view = rank.window(1, 4);
        int[] output = {SENTINEL, SENTINEL, SENTINEL};
        expect(IndexOutOfBoundsException.class, () -> view.copySelectedRowIds(-1, output, 0, 1));
        expect(IndexOutOfBoundsException.class, () -> view.copySelectedRowIds(0, output, -1, 1));
        expect(IndexOutOfBoundsException.class, () -> view.copySelectedRowIds(0, output, 2, 2));
        expect(IndexOutOfBoundsException.class, () -> view.copySelectedRowIds(0, output, 0, view.size() + 1));
        expect(IndexOutOfBoundsException.class, () -> view.copySelectedRowIds(0, output, 0, -1));
        expect(NullPointerException.class, () -> view.copySelectedRowIds(0, null, 0, 0));
        assertArray(new int[]{SENTINEL, SENTINEL, SENTINEL}, output, "refusals leave output unchanged");
        view.copySelectedRowIds(view.size(), output, output.length, 0);
        checks++;
    }

    private static void assertArray(int[] expected, int[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void equal(int expected, int actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        Objects.requireNonNull(type, "type");
        try {
            action.run();
        } catch (Throwable thrown) {
            checks++;
            if (type.isInstance(thrown)) return;
            throw new AssertionError("unexpected exception class " + thrown, thrown);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
