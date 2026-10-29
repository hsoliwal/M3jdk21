// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Signed/strided multi-plane contradictory mask contracts; no contest source borrowed. */
public final class M3IndexPathImpossibleStageSelfTest {
    private static long checks;
    private static final long SEED = 0x4d3350415448494dL;
    private M3IndexPathImpossibleStageSelfTest() { }

    private static void ok(boolean valid, String why) {
        checks++;
        if (!valid) throw new AssertionError(why);
    }
    private static void eq(int expected, int actual, String why) {
        checks++;
        if (expected != actual) throw new AssertionError(why + " expected=" + expected + " actual=" + actual);
    }
    private static void eq(int[] expected, int[] actual, String why) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }
    private static void fails(Class<? extends Throwable> type, Runnable work) {
        checks++;
        try { work.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong failure " + error, error);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
    private static boolean accepted(int[][] states, int[] required, int[] forbidden, int physical) {
        for (int stage = 0; stage < states.length; stage++) {
            int flags = states[stage][physical];
            if ((flags & required[stage]) != required[stage]
                    || (flags & forbidden[stage]) != 0) return false;
        }
        return true;
    }
    private static void verify(int[] rows, int[] offsets, int[][] states,
            int[] required, int[] forbidden, int first, int count, int stride,
            boolean nativeRun, String label) {
        List<Integer> oracle = new ArrayList<>();
        int firstMatch = -1;
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int at = (int) ((long) first + (long) ordinal * stride);
            if (accepted(states, required, forbidden, offsets[at])) {
                if (firstMatch < 0) firstMatch = at;
                oracle.add(rows[at]);
            }
        }
        int[] expected = new int[oracle.size()];
        for (int i = 0; i < expected.length; i++) expected[i] = oracle.get(i);
        int[] oldRows = rows.clone();
        int[] oldOffsets = offsets.clone();
        int[][] oldStates = Arrays.stream(states).map(int[]::clone).toArray(int[][]::new);

        int[] selected = M3IndexPathMasks.select(rows, offsets, states, required, forbidden,
                first, count, stride, M3IndexPathMasks.Backend.JAVA);
        eq(expected, selected, label + " detached rows");
        eq(firstMatch, M3IndexPathMasks.firstMatchingIndex(rows, offsets, states, required,
                forbidden, first, count, stride), label + " first matching position");
        eq(oldRows, rows, label + " input order");
        eq(oldOffsets, offsets, label + " input offsets");
        for (int i = 0; i < states.length; i++) {
            eq(oldStates[i], states[i], label + " state-plane mutation " + i);
        }
        if (selected.length > 0) {
            selected[0] = 0x12345678;
            eq(oldRows, rows, label + " output independence");
        }
        if (nativeRun) {
            eq(expected, M3IndexPathMasks.select(rows, offsets, states, required, forbidden,
                    first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED),
                    label + " checked native parity");
        }
    }
    private static void originalBoundaries(boolean nativeRun) {
        int[] rows = {Integer.MIN_VALUE, -9, 17, 17, Integer.MAX_VALUE};
        int[] offsets = {2, 0, 2, 1, 0};
        int[][] states = {{Integer.MIN_VALUE, 0, -1}, {0, 1, -1}};
        verify(rows, offsets, states, new int[] {1, 0}, new int[] {1, 0},
                0, rows.length, 1, nativeRun, "same-stage overlap");
        verify(rows, offsets, states, new int[] {Integer.MIN_VALUE, 0},
                new int[] {Integer.MIN_VALUE, 0}, 4, rows.length, -1, nativeRun,
                "signed high-bit contradiction reverse");
        verify(rows, offsets, states, new int[] {0, 0}, new int[] {0, 0},
                4, 3, -2, nativeRun, "all-zero identity in opposite direction");
        verify(rows, offsets, states, new int[] {1, 0}, new int[] {0, 1},
                0, rows.length, 1, nativeRun, "cross-stage overlapping bit is not impossible");
        verify(rows, offsets, states, new int[] {0, 0}, new int[] {1, 1},
                0, rows.length, 1, nativeRun, "forbidden-only expression");
        verify(rows, offsets, states, new int[] {1, 0}, new int[] {1, 0},
                rows.length, 0, -1, nativeRun, "empty terminal path");

        int[] badOffsets = {2, 0, 5, 1, 0};
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(
                rows, badOffsets, states, new int[] {1, 0}, new int[] {1, 0},
                0, rows.length, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.firstMatchingIndex(
                rows, badOffsets, states, new int[] {1, 0}, new int[] {1, 0},
                0, rows.length, 1));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(
                rows, offsets, states, new int[] {1, 0}, new int[] {1, 0},
                0, 4, 0, M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.select(
                rows, offsets, states, new int[] {1}, new int[] {1},
                0, rows.length, 1, M3IndexPathMasks.Backend.JAVA));
        if (!nativeRun && !M3IndexPathMasks.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> M3IndexPathMasks.select(
                    rows, offsets, states, new int[] {1, 0}, new int[] {1, 0},
                    0, rows.length, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }
    private static void windowBaseline(boolean nativeRun) {
        int[] rows = {8, 8, Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        int[] offsets = {0, 0, 1, 2, 1};
        int[][] states = {{0, Integer.MIN_VALUE, -1}, {3, 5, 7}};
        int[] required = {0x08, 1};
        int[] forbidden = {0x08, 0};
        int[] window = {111, 111, 111, 111};
        M3IndexPathMasks.Backend backend = nativeRun
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        long result = M3IndexPathMasks.scanWindow(rows, offsets, states,
                required, forbidden, 4, 5, -1, 0, window, 1, 2, backend);
        eq(5, M3IndexPathMasks.nextOrdinal(result), "contradictory window advances path");
        eq(0, M3IndexPathMasks.written(result), "contradictory window writes nothing");
        eq(new int[]{111, 111, 111, 111}, window, "untouched window canaries");
        int[] badOffsets = {0, 0, 1, 999, 1};
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.scanWindow(
                rows, badOffsets, states, required, forbidden,
                4, 5, -1, 0, window, 1, 2, M3IndexPathMasks.Backend.JAVA));
        eq(new int[]{111, 111, 111, 111}, window, "refusal before writes");
    }
    private static void seededPaths(boolean nativeRun) {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 2048; trial++) {
            int length = random.nextInt(145);
            int[] rows = new int[length], offsets = new int[length];
            int stages = 1 + random.nextInt(5), capacity = 1 + random.nextInt(91);
            int[][] states = new int[stages][capacity];
            for (int i = 0; i < length; i++) {
                rows[i] = switch (i % 23) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(49) - 24;
                };
                offsets[i] = random.nextInt(capacity);
            }
            int[] required = new int[stages], forbidden = new int[stages];
            for (int stage = 0; stage < stages; stage++) {
                for (int pos = 0; pos < capacity; pos++) states[stage][pos] = random.nextInt();
                required[stage] = random.nextInt();
                forbidden[stage] = random.nextInt();
            }
            if (trial % 7 == 0) {
                Arrays.fill(required, 0);
                Arrays.fill(forbidden, 0);
            } else if (trial % 7 == 1) {
                int stage = random.nextInt(stages);
                int bit = trial % 2 == 0 ? Integer.MIN_VALUE : (1 << random.nextInt(31));
                required[stage] |= bit;
                forbidden[stage] |= bit;
            } else if (trial % 7 == 2 && stages > 1) {
                required[0] = 1;
                forbidden[0] = 0;
                required[1] = 0;
                forbidden[1] = 1;
            }
            int count = length == 0 ? 0 : random.nextInt(length + 1);
            int stride = (trial & 1) == 0 ? 1 : -1;
            int first = count == 0 ? length
                    : stride == 1 ? random.nextInt(length - count + 1)
                    : count - 1 + random.nextInt(length - count + 1);
            verify(rows, offsets, states, required, forbidden,
                    first, count, stride, nativeRun, "seeded trial " + trial);
        }
    }
    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !nativeRun))
            throw new IllegalArgumentException("use no args or --native");
        if (nativeRun) ok(M3IndexPathMasks.nativeAvailable(), "native-required library missing");
        originalBoundaries(nativeRun);
        windowBaseline(nativeRun);
        seededPaths(nativeRun);
        System.out.println("M3_IMPOSSIBLE_STAGE_MASK_PASS checks=" + checks
                + " JNI=" + nativeRun);
    }
}
