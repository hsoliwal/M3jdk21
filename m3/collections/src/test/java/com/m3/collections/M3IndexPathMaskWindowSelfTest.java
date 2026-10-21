// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Differential replay against the unchanged detached indexed path selector. */
public final class M3IndexPathMaskWindowSelfTest {
    private static int checks;
    private static final int SENTINEL = 0x64a519ee;
    private static final Random RANDOM = new Random(0x33a7f08cL);
    private M3IndexPathMaskWindowSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void equal(int[] actual, int[] expected, String label) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(label + " actual=" + Arrays.toString(actual)
                    + " expected=" + Arrays.toString(expected));
        }
    }
    @FunctionalInterface private interface Action { void run(); }
    private static void refuses(Class<? extends Throwable> expected, Action action, String label) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError(label + " wrong exception " + error, error);
        }
        throw new AssertionError(label + " accepted invalid arguments");
    }
    private static void scenario(int[] rows, int[] physical, int[][] planes,
                                 int[] required, int[] forbidden,
                                 int first, int count, int stride,
                                 M3IndexPathMasks.Backend backend,
                                 int capacity) {
        int[] expected = M3IndexPathMasks.select(rows, physical, planes,
                required, forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA);
        ArrayList<Integer> collected = new ArrayList<>(); // Test oracle only, never product state.
        int cursor = 0;
        while (cursor < count) {
            int limit = Math.min(M3IndexPathMasks.MAX_WINDOW, 1 + RANDOM.nextInt(capacity));
            int offset = 3;
            int[] output = new int[offset + limit + 5];
            Arrays.fill(output, SENTINEL);
            long result = M3IndexPathMasks.scanWindow(rows, physical, planes,
                    required, forbidden, first, count, stride, cursor,
                    output, offset, limit, backend);
            int next = M3IndexPathMasks.nextOrdinal(result);
            int written = M3IndexPathMasks.written(result);
            check(next > cursor && next <= count, "strict progress");
            check(written >= 0 && written <= limit, "bounded result");
            check(written <= next - cursor, "output cannot exceed input rows");
            for (int i = 0; i < output.length; i++) {
                if (i >= offset && i < offset + written) {
                    collected.add(output[i]);
                } else check(output[i] == SENTINEL, "untouched reserved/prefix/suffix");
            }
            cursor = next;
        }
        int[] actual = collected.stream().mapToInt(Integer::intValue).toArray();
        equal(actual, expected, "exact ordinal/path/JNI parity");
        int[] zero = {SENTINEL, SENTINEL, SENTINEL};
        long hold = M3IndexPathMasks.scanWindow(rows, physical, planes,
                required, forbidden, first, count, stride,
                count, zero, 1, 0, backend);
        check(M3IndexPathMasks.nextOrdinal(hold) == count && M3IndexPathMasks.written(hold) == 0,
                "terminal zero limit");
        equal(zero, new int[] {SENTINEL,SENTINEL,SENTINEL}, "terminal unchanged");
        hold = M3IndexPathMasks.scanWindow(rows, physical, planes,
                required, forbidden, first, count, stride, 0, zero, 1, 0, backend);
        check(M3IndexPathMasks.nextOrdinal(hold) == 0 && M3IndexPathMasks.written(hold) == 0,
                "zero limit retains cursor");
        equal(zero, new int[] {SENTINEL,SENTINEL,SENTINEL}, "zero result unchanged");
    }
    private static void randomized(M3IndexPathMasks.Backend backend) {
        for (int seed = 0; seed < 650; seed++) {
            int n = RANDOM.nextInt(140);
            int[] rows = new int[n];
            int[] offsets = new int[n];
            int stages = 1 + RANDOM.nextInt(5);
            int capacity = 1 + RANDOM.nextInt(145);
            int[][] planes = new int[stages][capacity];
            int[] required = new int[stages];
            int[] forbidden = new int[stages];
            for (int i = 0; i < n; i++) {
                rows[i] = RANDOM.nextInt(8) == 0 ? Integer.MIN_VALUE
                        : RANDOM.nextInt(90) - 45; // Duplicate IDs are common.
                offsets[i] = RANDOM.nextInt(capacity);
            }
            for (int stage = 0; stage < stages; stage++) {
                for (int i = 0; i < capacity; i++) planes[stage][i] = RANDOM.nextInt();
                required[stage] = switch (RANDOM.nextInt(4)) {
                    case 0 -> 0;
                    case 1 -> Integer.MIN_VALUE;
                    case 2 -> 0xff;
                    default -> RANDOM.nextInt();
                };
                forbidden[stage] = RANDOM.nextBoolean() ? 0 : RANDOM.nextInt();
            }
            int stride = 1 + RANDOM.nextInt(5);
            if (RANDOM.nextBoolean()) stride = -stride;
            int first;
            int count;
            if (n == 0 || RANDOM.nextInt(12) == 0) {
                first = n;
                count = 0;
            } else {
                first = RANDOM.nextInt(n);
                int possible = stride > 0 ? (n - 1 - first) / stride + 1
                        : first / -stride + 1;
                count = RANDOM.nextInt(possible + 1);
            }
            scenario(rows, offsets, planes, required, forbidden,
                    first, count, stride, backend, 1 + RANDOM.nextInt(32));
        }
    }
    /** Both sides of the 4,096-element bounded window are actual output tests. */
    private static void maximumWindow(M3IndexPathMasks.Backend backend) {
        final int n = 5000;
        int[] rows = new int[n];
        int[] offsets = new int[n];
        int[][] planes = {new int[n]};
        for (int i = 0; i < n; i++) {
            rows[i] = n - i - 2500; // Input row IDs may be negative and unsorted.
            offsets[i] = i;
            planes[0][i] = Integer.MIN_VALUE;
        }
        int[] buffer = new int[M3IndexPathMasks.MAX_WINDOW + 2];
        Arrays.fill(buffer, SENTINEL);
        long result = M3IndexPathMasks.scanWindow(rows, offsets, planes,
                new int[] {Integer.MIN_VALUE}, new int[] {0}, 0, n, 1, 0,
                buffer, 1, M3IndexPathMasks.MAX_WINDOW, backend);
        check(M3IndexPathMasks.nextOrdinal(result) == 4096
                && M3IndexPathMasks.written(result) == 4096, "full 4096 window");
        for (int i = 0; i < 4096; i++) check(buffer[i + 1] == rows[i], "full forward window value");
        check(buffer[0] == SENTINEL && buffer[4097] == SENTINEL,
                "full window preserves surrounding state");
        result = M3IndexPathMasks.scanWindow(rows, offsets, planes,
                new int[] {Integer.MIN_VALUE}, new int[] {0}, n - 1, n, -1, 0,
                buffer, 1, M3IndexPathMasks.MAX_WINDOW, backend);
        check(M3IndexPathMasks.nextOrdinal(result) == 4096
                && M3IndexPathMasks.written(result) == 4096, "full reversed 4096 window");
        for (int i = 0; i < 4096; i++) check(buffer[i + 1] == rows[n - 1 - i],
                "full reverse window value");
        Arrays.fill(buffer, SENTINEL);
        result = M3IndexPathMasks.scanWindow(rows, offsets, planes,
                new int[] {Integer.MIN_VALUE}, new int[] {0}, n - 1, n, -1, 4096,
                buffer, 1, M3IndexPathMasks.MAX_WINDOW, backend);
        check(M3IndexPathMasks.nextOrdinal(result) == n
                && M3IndexPathMasks.written(result) == n - 4096, "resumed trailing window");
        check(buffer[0] == SENTINEL && buffer[1 + n - 4096] == SENTINEL,
                "reused output untouched suffix");
    }

    private static void edges(M3IndexPathMasks.Backend backend) {
        int[] rows = {5, 5, -7, Integer.MIN_VALUE, 0};
        int[] offsets = {2, 2, 0, 1, 2};
        int[][] planes = {{Integer.MIN_VALUE, 0, Integer.MIN_VALUE}};
        int[] required = {Integer.MIN_VALUE};
        int[] forbidden = {0};
        scenario(rows, offsets, planes, required, forbidden, 4, 5, -1, backend, 1);
        scenario(rows, offsets, planes, required, forbidden, 0, 5, 1, backend, 9);
        scenario(rows, offsets, planes, new int[] {0}, new int[] {0}, 0, 5, 1, backend, 1);
        scenario(rows, offsets, planes, required, required, 0, 5, 1, backend, 3);
        scenario(rows, offsets, planes, required, forbidden, 5, 0, -1, backend, 1);
        int[] output = {SENTINEL, SENTINEL, SENTINEL, SENTINEL, SENTINEL};
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 0, output, 0, M3IndexPathMasks.MAX_WINDOW + 1, backend),
                "bounded limit");
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 6, output, 0, 1, backend), "invalid ordinal");
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 0, output, 4, 2, backend), "out of output bounds");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 0, rows, 0, 1, backend), "row/output alias");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 0, offsets, 0, 1, backend), "offset/output alias");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 1, 0, planes[0], 0, 1, backend), "state/output alias");
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, required,
                        forbidden, 0, 5, 0, 0, output, 0, 1, backend), "zero stride");
        refuses(IllegalArgumentException.class,
                () -> M3IndexPathMasks.scanWindow(rows, offsets, new int[][] {}, required,
                        forbidden, 0, 5, 1, 0, output, 0, 1, backend), "stage shape");
        int[] lateBad = {2, 2, 0, 99, 2};
        long result = M3IndexPathMasks.scanWindow(rows, lateBad, planes, required,
                forbidden, 0, 5, 1, 0, output, 0, 1, backend);
        check(M3IndexPathMasks.nextOrdinal(result) == 1, "later invalid offset not scanned early");
        int[] preserved = output.clone();
        refuses(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.scanWindow(rows, lateBad, planes, required,
                        forbidden, 0, 5, 1, 1, output, 0, 3, backend), "next window invalid physical offset");
        equal(output, preserved, "preflight refusal cannot mutate output");
        // Full valid geometry but all masks fail: cursor must still finish.
        int[][] empty = {{0, 0, 0}};
        Arrays.fill(output, SENTINEL);
        long nothing = M3IndexPathMasks.scanWindow(rows, offsets, empty,
                required, forbidden, 0, 5, 1, 0, output, 0, 2, backend);
        check(M3IndexPathMasks.nextOrdinal(nothing) == 5
                && M3IndexPathMasks.written(nothing) == 0, "empty-match full progress");
        equal(output, new int[] {SENTINEL,SENTINEL,SENTINEL,SENTINEL,SENTINEL}, "no output written");
    }
    public static void main(String[] args) {
        M3IndexPathMasks.Backend backend = args.length > 0 && args[0].equals("--native")
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        if (backend == M3IndexPathMasks.Backend.JAVA) {
            int[] output = {SENTINEL};
            if (!M3IndexPathMasks.nativeAvailable()) {
                refuses(UnsatisfiedLinkError.class,
                        () -> M3IndexPathMasks.scanWindow(new int[] {1}, new int[] {0},
                                new int[][] {{1}}, new int[] {1}, new int[] {0}, 0, 1,
                                1, 0, output, 0, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED),
                        "explicit native-required refusal");
            }
        } else check(M3IndexPathMasks.nativeAvailable(), "real JNI loaded");
        edges(backend);
        maximumWindow(backend);
        randomized(backend);
        System.out.println("M3_PATH_MASK_WINDOW_PASS checks=" + checks
                + " backend=" + backend + " nativeAvailable=" + M3IndexPathMasks.nativeAvailable());
    }
}
