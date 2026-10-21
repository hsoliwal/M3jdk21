// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent scalar oracle for bounded primitive path, offset and mask scans. */
public final class M3IndexPathMaskBudgetSelfTest {
    private static final int GUARD = 0x64a519ee;
    private static long checks;
    private M3IndexPathMaskBudgetSelfTest() { }

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }

    private static boolean matches(int[][] planes, int[] required, int[] forbidden, int slot) {
        for (int stage = 0; stage < planes.length; stage++) {
            int word = planes[stage][slot];
            if ((word & required[stage]) != required[stage]
                    || (word & forbidden[stage]) != 0) return false;
        }
        return true;
    }

    private static void one(M3IndexPathMasks.Backend backend, int[] rows, int[] offsets,
            int[][] planes, int[] required, int[] forbidden, int first, int count,
            int stride, int next, int budget, int limit) {
        int[] output = new int[limit + 5], expected = new int[limit + 5];
        Arrays.fill(output, GUARD);
        Arrays.fill(expected, GUARD);
        int stop = next, written = 0;
        while (stop < count && stop - next < budget && written < limit) {
            int path = (int) ((long) first + (long) stop * stride);
            if (matches(planes, required, forbidden, offsets[path])) {
                expected[2 + written++] = rows[path];
            }
            stop++;
        }
        long actual = M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                required, forbidden, first, count, stride, next, budget,
                output, 2, limit, backend);
        check(M3IndexPathMasks.nextOrdinal(actual) == stop, "ordinal progress");
        check(M3IndexPathMasks.written(actual) == written, "written count");
        check(Arrays.equals(output, expected), "row payload and output guards");
        check(stop - next <= budget, "at most requested input ordinals");
        if (budget == Integer.MAX_VALUE) {
            int[] legacy = new int[output.length];
            Arrays.fill(legacy, GUARD);
            long old = M3IndexPathMasks.scanWindow(rows, offsets, planes,
                    required, forbidden, first, count, stride, next,
                    legacy, 2, limit, backend);
            check(old == actual && Arrays.equals(legacy, output),
                    "whole-path legacy behavior parity");
        }
    }

    private static void random(M3IndexPathMasks.Backend backend, int iterations) {
        Random rnd = new Random(0x1985a3b047L);
        for (int i = 0; i < iterations; i++) {
            int n = i % 67 == 0 ? 4097 : rnd.nextInt(190);
            int capacity = 1 + rnd.nextInt(185);
            int stages = 1 + rnd.nextInt(5);
            int[] rows = new int[n], positions = new int[n];
            int[][] planes = new int[stages][capacity];
            int[] required = new int[stages], forbidden = new int[stages];
            for (int k = 0; k < n; k++) {
                rows[k] = k % 13 == 0 ? Integer.MIN_VALUE : rnd.nextInt(11) - 5;
                positions[k] = rnd.nextInt(capacity);
            }
            for (int s = 0; s < stages; s++) {
                for (int k = 0; k < capacity; k++) planes[s][k] = rnd.nextInt();
                required[s] = switch (i % 9) {
                    case 0 -> 0; case 1 -> Integer.MIN_VALUE;
                    default -> rnd.nextInt();
                };
                forbidden[s] = i % 7 == 0 ? 0 : rnd.nextInt();
            }
            int stride = 1 + rnd.nextInt(5);
            if (rnd.nextBoolean()) stride = -stride;
            int first, count;
            if (n == 0 || rnd.nextInt(15) == 0) {
                first = n;
                count = 0;
            } else {
                first = rnd.nextInt(n);
                int possible = (stride > 0 ? (n - 1 - first) / stride
                        : first / -stride) + 1;
                count = rnd.nextInt(possible + 1);
            }
            int next = count == 0 ? 0 : rnd.nextInt(count + 1);
            int budget = switch (i % 9) {
                case 0 -> 0; case 1 -> 1; case 2 -> 2; case 3 -> 63;
                case 4 -> 64; case 5 -> 512; case 6 -> 4096;
                case 7 -> Integer.MAX_VALUE; default -> rnd.nextInt(350);
            };
            int limit = switch (i % 7) {
                case 0 -> 0; case 1 -> 1; case 2 -> 2; case 3 -> 7;
                case 4 -> 64; case 5 -> M3IndexPathMasks.MAX_WINDOW;
                default -> 129;
            };
            one(backend, rows, positions, planes, required, forbidden,
                    first, count, stride, next, budget, limit);
        }
    }

    private static void boundaries(M3IndexPathMasks.Backend backend) {
        int[] rows = {5, 5, -7, Integer.MIN_VALUE, 0};
        int[] offsets = {2, 2, 0, 1, 2};
        int[][] planes = {{Integer.MIN_VALUE, 0, Integer.MIN_VALUE}};
        int[] required = {Integer.MIN_VALUE}, forbidden = {0};
        for (int budget : new int[] {0, 1, 2, 5, 512, Integer.MAX_VALUE}) {
            for (int limit : new int[] {0, 1, 2, 7, 4096}) {
                one(backend, rows, offsets, planes, required, forbidden,
                        4, 5, -1, 0, budget, limit);
                one(backend, rows, offsets, planes, required, forbidden,
                        0, 5, 1, 2, budget, limit);
                one(backend, rows, offsets, planes, required, forbidden,
                        5, 0, -1, 0, budget, limit);
                one(backend, rows, offsets, planes, required, required,
                        0, 5, 1, 0, budget, limit);
            }
        }
    }

    private static void sparseMillion(M3IndexPathMasks.Backend backend) {
        int n = 1_000_000;
        int[] rows = new int[n], offsets = new int[n];
        int[][] planes = {new int[n]};
        for (int i = 0; i < n; i++) offsets[i] = i;
        rows[n - 1] = 42;
        planes[0][n - 1] = Integer.MIN_VALUE;
        int[] output = {GUARD, GUARD};
        long first = M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                new int[] {Integer.MIN_VALUE}, new int[] {0},
                0, n, 1, 0, 512, output, 0, 2, backend);
        check(M3IndexPathMasks.nextOrdinal(first) == 512
                && M3IndexPathMasks.written(first) == 0, "sparse million early yield");
        check(output[0] == GUARD && output[1] == GUARD, "empty window is untouched");
        int ordinal = 0, calls = 0;
        List<Integer> found = new ArrayList<>();
        while (ordinal < n) {
            long window = M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                    new int[] {Integer.MIN_VALUE}, new int[] {0},
                    0, n, 1, ordinal, 512, output, 0, 2, backend);
            int after = M3IndexPathMasks.nextOrdinal(window);
            check(after > ordinal && after - ordinal <= 512, "budgeted progress");
            ordinal = after;
            calls++;
            for (int i = 0; i < M3IndexPathMasks.written(window); i++) found.add(output[i]);
        }
        check(calls == 1954 && found.equals(List.of(42)), "complete sparse reconciliation");
    }

    @FunctionalInterface private interface Action { void run(); }
    private static void refuses(Class<? extends Throwable> type, Action action,
            int[] output, String reason) {
        int[] before = output.clone();
        try { action.run(); }
        catch (Throwable error) {
            check(type.isInstance(error), reason + " wrong exception " + error);
            check(Arrays.equals(before, output), reason + " mutated destination");
            return;
        }
        throw new AssertionError("missing refusal: " + reason);
    }

    private static void invalid(M3IndexPathMasks.Backend backend) {
        int[] rows = {1, 2, 3}, offsets = {0, 0, 0};
        int[][] planes = {{1}};
        int[] required = {1}, forbidden = {0};
        int[] output = {GUARD, GUARD, GUARD};
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, -1, output, 0, 1, backend),
                output, "negative examination budget");
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 0, 0, 1, output, 0, 1, backend),
                output, "invalid stride");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, 0, rows, 0, 0, backend),
                rows, "row alias when both budgets are zero");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, 0, offsets, 0, 0, backend),
                offsets, "offset alias");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, 0, planes[0], 0, 0, backend),
                planes[0], "state plane alias");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, 0, required, 0, 0, backend),
                required, "required mask alias");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, offsets, planes,
                        required, forbidden, 0, 3, 1, 0, 0, forbidden, 0, 0, backend),
                forbidden, "forbidden mask alias");
        int[] invalidLater = {0, 99, 0};
        long first = M3IndexPathMasks.scanWindowBounded(rows, invalidLater, planes,
                required, forbidden, 0, 3, 1, 0, 1, output, 0, 2, backend);
        check(M3IndexPathMasks.nextOrdinal(first) == 1
                && M3IndexPathMasks.written(first) == 1, "deferred invalid position");
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindowBounded(rows, invalidLater, planes,
                        required, forbidden, 0, 3, 1, 1, 2, output, 0, 2, backend),
                output, "bad offset refuses before writes");
    }

    public static void main(String[] args) {
        M3IndexPathMasks.Backend backend = args.length == 1 && args[0].equals("--native")
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        if (backend == M3IndexPathMasks.Backend.NATIVE_REQUIRED) {
            check(M3IndexPathMasks.nativeAvailable(), "required C17 JNI library absent");
        } else if (!M3IndexPathMasks.nativeAvailable()) {
            int[] destination = {GUARD};
            refuses(UnsatisfiedLinkError.class,
                    () -> M3IndexPathMasks.scanWindowBounded(
                            new int[] {1}, new int[] {0}, new int[][] {{1}},
                            new int[] {1}, new int[] {0}, 0, 1, 1, 0, 0,
                            destination, 0, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED),
                    destination, "native-required refusal at zero budget");
        }
        boundaries(backend);
        random(backend, backend == M3IndexPathMasks.Backend.JAVA ? 5000 : 400);
        sparseMillion(backend);
        invalid(backend);
        System.out.println("M3_INDEX_PATH_MASK_BUDGET_PASS checks=" + checks
                + " backend=" + backend);
    }
}
