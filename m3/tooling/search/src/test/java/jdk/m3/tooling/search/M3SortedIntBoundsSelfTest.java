/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.search;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Standalone, deterministic Java/native differential proof against an independent linear reference. */
public final class M3SortedIntBoundsSelfTest {
    private static int checks;

    private M3SortedIntBoundsSelfTest() {
    }

    /**
     * Executes adversarial Java 21 and C17/JNI bounds oracles; no product admission is inferred.
     *
     * @param args unused
     * @throws Exception when asynchronous fixture work cannot complete
     */
    public static void main(String[] args) throws Exception {
        testExceptions();
        testRangesAndDuplicates();
        testDeterministicSpectrum();
        testLargeBatch();
        testParallel();
        System.out.println("M3_SORTED_INT_BOUNDS_TEST\tchecks=" + checks
                + "\tjava=PASS\tnative_jni=PASS\tproduct_jdk=HOLD");
    }

    private static void testExceptions() {
        requireThrows(NullPointerException.class,
                () -> M3SortedIntBounds.bounds(null, 0, 0, new int[0]));
        requireThrows(NullPointerException.class,
                () -> M3NativeSortedIntBounds.bounds(null, 0, 0, new int[0]));
        requireThrows(NullPointerException.class,
                () -> M3SortedIntBounds.bounds(new int[0], 0, 0, null));
        requireThrows(NullPointerException.class,
                () -> M3NativeSortedIntBounds.bounds(new int[0], 0, 0, null));
        for (int[] invalid : new int[][]{{-1, 0}, {0, -1}, {2, 1}, {0, 3}, {1, 0}}) {
            requireThrows(IndexOutOfBoundsException.class,
                    () -> M3SortedIntBounds.bounds(new int[2], invalid[0], invalid[1], new int[0]));
            requireThrows(IndexOutOfBoundsException.class,
                    () -> M3NativeSortedIntBounds.bounds(new int[2], invalid[0], invalid[1], new int[0]));
        }
        requireThrows(NullPointerException.class, () -> M3SortedIntBounds.bounds(null, new int[0]));
    }

    private static void testRangesAndDuplicates() {
        verify(new int[0], 0, 0, new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE});
        verify(new int[]{4, 4, 4, 4}, 0, 4,
                new int[]{Integer.MIN_VALUE, 3, 4, 5, Integer.MAX_VALUE});
        verify(new int[]{Integer.MIN_VALUE, -3, -3, 0, 0, 0, 9, Integer.MAX_VALUE},
                0, 8, new int[]{Integer.MIN_VALUE, -3, -2, 0, 9, Integer.MAX_VALUE});
        verify(new int[]{-3, -2, -1, 0, 1, 2, 3}, 3, 3, new int[]{-3, 0, 2, 3});
        verify(new int[]{-3, -2, -1, 0, 1, 2, 3}, 7, 7, new int[]{-3, 0, 2, 3});
        verify(new int[]{-3, -2, -1, 0, 1, 2, 3}, 2, 5, new int[]{-5, -1, 0, 1, 3});
        verify(new int[]{1, 2, 3}, 0, 3, new int[0]);
        int[] emptyJava = M3SortedIntBounds.bounds(new int[0], new int[0]);
        check(emptyJava.length == 0, "empty full-range convenience result");
    }

    private static void testDeterministicSpectrum() {
        for (int n = 0; n <= 300; n++) {
            int[] sorted = new int[n];
            for (int i = 0; i < n; i++) {
                sorted[i] = i / 3 - n / 6;
            }
            if (n > 0) {
                sorted[0] = Integer.MIN_VALUE;
                sorted[n - 1] = Integer.MAX_VALUE;
            }
            int[] probes = new int[42];
            for (int i = 0; i < probes.length; i++) {
                probes[i] = i == 0 ? Integer.MIN_VALUE : i == 1 ? Integer.MAX_VALUE
                        : (i * 19 % 97) - 48;
            }
            verify(sorted, 0, n, probes);
            int from = n / 5;
            int to = n - n / 7;
            verify(sorted, from, to, probes);
            verify(sorted, from, from, probes);
        }
    }

    private static void testLargeBatch() {
        int[] sorted = new int[20_000];
        int[] probes = new int[8193];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i / 7 - 2000;
        }
        for (int i = 0; i < probes.length; i++) {
            probes[i] = (i * 11) % 4099 - 2050;
        }
        verify(sorted, 0, sorted.length, probes);
        verify(sorted, 117, 16_111, probes);
        Arrays.sort(probes);
        verify(sorted, 0, sorted.length, probes);
        verify(sorted, 117, 16_111, probes);
    }

    private static void testParallel() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                final int w = worker;
                tasks.add(() -> {
                    int local = 0;
                    for (int n = 1; n <= 513; n += 19) {
                        int[] source = new int[n];
                        for (int i = 0; i < n; i++) {
                            source[i] = i / 3 - 41;
                        }
                        int[] keys = new int[]{w - 4, -500, 0, 500, Integer.MIN_VALUE, Integer.MAX_VALUE};
                        int[] expected = linearOracle(source, 0, source.length, keys);
                        if (!Arrays.equals(expected, M3SortedIntBounds.bounds(source, 0, n, keys))
                                || !Arrays.equals(expected, M3NativeSortedIntBounds.bounds(source, 0, n, keys))) {
                            throw new AssertionError("native batch parity divergence across worker and length");
                        }
                        local += 12;
                    }
                    return local;
                });
            }
            for (Future<Integer> result : pool.invokeAll(tasks)) {
                checks += result.get();
            }
        }
    }

    private static void verify(int[] input, int from, int to, int[] keys) {
        int[] copy = input.clone();
        int[] keyCopy = keys.clone();
        int[] expected = linearOracle(input, from, to, keys);
        int[] java = M3SortedIntBounds.bounds(input, from, to, keys);
        int[] nativeResult = M3NativeSortedIntBounds.bounds(input, from, to, keys);
        check(Arrays.equals(expected, java), "Java divergence at [" + from + "," + to + ")");
        check(Arrays.equals(expected, nativeResult), "native divergence at [" + from + "," + to + ")");
        check(Arrays.equals(copy, input), "sorted input mutated");
        check(Arrays.equals(keyCopy, keys), "keys mutated");
        check(java != nativeResult && java != expected && nativeResult != expected,
                "output reference must be independently owned");
        for (int i = 0; i < keys.length; i++) {
            int lower = java[i * 2];
            int upper = java[i * 2 + 1];
            check(from <= lower && lower <= upper && upper <= to, "invalid absolute bounds");
            if (lower < to) {
                check(input[lower] >= keys[i], "lower not greater-or-equal");
            }
            if (upper < to) {
                check(input[upper] > keys[i], "upper not strictly greater");
            }
        }
    }

    private static int[] linearOracle(int[] input, int from, int to, int[] keys) {
        int[] output = new int[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            int lower = from;
            int upper = from;
            while (lower < to && input[lower] < keys[i]) {
                lower++;
            }
            while (upper < to && input[upper] <= keys[i]) {
                upper++;
            }
            output[2 * i] = lower;
            output[2 * i + 1] = upper;
        }
        return output;
    }

    private static void requireThrows(Class<? extends Throwable> expected, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable caught) {
            check(expected.isInstance(caught), "wrong exception " + caught);
            return;
        }
        throw new AssertionError("missing required " + expected.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

}
