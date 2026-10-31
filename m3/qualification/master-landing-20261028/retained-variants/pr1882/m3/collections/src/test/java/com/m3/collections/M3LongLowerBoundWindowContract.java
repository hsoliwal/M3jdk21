// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** LeetCode #35 lower-bound windows: source and caller output stay primitive arrays. */
public final class M3LongLowerBoundWindowContract {
    private static long checks;
    private M3LongLowerBoundWindowContract() { }

    private static void check(boolean correct, String message) {
        checks++;
        if (!correct) throw new AssertionError(message);
    }

    private static void equal(int[] expected, int[] actual) {
        check(Arrays.equals(expected, actual),
                "expected=" + Arrays.toString(expected) + ", actual=" + Arrays.toString(actual));
    }

    private static int oracle(long[] sorted, long key) {
        int i = 0;
        while (i < sorted.length && sorted[i] < key) i++;
        return i;
    }

    private static void expect(Class<? extends Throwable> error, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable caught) {
            if (error.isInstance(caught)) return;
            throw new AssertionError("unexpected exception", caught);
        }
        throw new AssertionError("expected " + error.getSimpleName());
    }

    public static long contracts() {
        checks = 0;
        Random random = new Random(0x35A71B21L);
        int[] lengths = {0, 1, 2, 63, 64, 65, 255, 256, 257, 513};
        for (int n : lengths) {
            for (int round = 0; round < 32; round++) {
                long[] sorted = new long[n];
                for (int i = 0; i < n; i++) {
                    sorted[i] = switch (random.nextInt(12)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        default -> random.nextInt(19) - 9L;
                    };
                }
                Arrays.sort(sorted);
                long[] old = sorted.clone();
                long[] queries = new long[270 + round];
                for (int i = 0; i < queries.length; i++) {
                    queries[i] = switch (random.nextInt(8)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        default -> random.nextInt(25) - 12L;
                    };
                }
                int[] output = new int[queries.length + 6];
                Arrays.fill(output, -999);
                M3LongRankBatchNative.lowerBoundsInto(
                        sorted, queries, 3, queries.length - 5, output, 2);
                for (int i = 0; i < queries.length - 5; i++) {
                    check(output[i + 2] == oracle(sorted, queries[i + 3]),
                            "wrong lower bound n=" + n + ", round=" + round);
                }
                check(output[0] == -999 && output[1] == -999, "prefix changed");
                check(output[output.length - 1] == -999, "suffix changed");
                equal(M3LongRankBatchNative.lowerBounds(sorted, queries),
                        Arrays.stream(queries).mapToInt(k -> oracle(sorted, k)).toArray());
                check(Arrays.equals(old, sorted), "source mutated");
            }
        }
        long[] values = {-2, -2, 0, 7, Long.MAX_VALUE};
        long[] q = {Long.MIN_VALUE, -2, 0, 3, Long.MAX_VALUE};
        int[] untouched = {-7, -7, -7, -7, -7};
        expect(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.lowerBoundsInto(values, q, -1, 3, untouched, 0));
        equal(new int[] {-7, -7, -7, -7, -7}, untouched);
        expect(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.lowerBoundsInto(values, q, 0, 5, untouched, 1));
        equal(new int[] {-7, -7, -7, -7, -7}, untouched);
        expect(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerBoundsInto(null, q, 0, 0, untouched, 0));
        M3LongRankBatchNative.lowerBoundsInto(new long[0], q, 0, 5, untouched, 0);
        equal(new int[] {0, 0, 0, 0, 0}, untouched);
        System.out.println("PASS M3_LONG_LOWER_BOUND_WINDOW checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
        return checks;
    }

    public static void main(String[] ignored) { contracts(); }
}
