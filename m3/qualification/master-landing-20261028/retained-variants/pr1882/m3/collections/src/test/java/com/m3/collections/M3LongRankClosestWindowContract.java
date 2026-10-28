// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Independent distance-sorted oracle, not a copy of the indexed JNI algorithm. */
public final class M3LongRankClosestWindowContract {
    private static long checks;
    private M3LongRankClosestWindowContract() { }

    private static void yes(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static int oracle(long[] sorted, long target, int k) {
        Long[] nearest = Arrays.stream(sorted).boxed().toArray(Long[]::new);
        BigInteger x = BigInteger.valueOf(target);
        Arrays.sort(nearest, Comparator
                .comparing((Long v) -> BigInteger.valueOf(v).subtract(x).abs())
                .thenComparingLong(Long::longValue));
        long[] selected = new long[k];
        for (int i = 0; i < k; i++) selected[i] = nearest[i];
        Arrays.sort(selected);
        for (int start = 0; start <= sorted.length - k; start++) {
            boolean equal = true;
            for (int i = 0; i < k; i++) {
                if (sorted[start + i] != selected[i]) { equal = false; break; }
            }
            if (equal) return start;
        }
        throw new AssertionError("oracle selection must be contiguous");
    }

    private static void failuresAreAtomic() {
        long[] sorted = {-3L, 2L, 2L, Long.MAX_VALUE};
        long[] targets = {Long.MIN_VALUE, 0L, Long.MAX_VALUE};
        int[] output = {73, 73, 73, 73, 73};
        for (Runnable action : new Runnable[] {
                () -> M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, -1, 2, 2, output, 1, false),
                () -> M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, 0, 4, 2, output, 1, false),
                () -> M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, 0, 2, 2, output, 4, false),
                () -> M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, 0, 2, -1, output, 1, false),
                () -> M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, 0, 2, 5, output, 1, false)
        }) {
            boolean thrown = false;
            try { action.run(); } catch (IndexOutOfBoundsException | IllegalArgumentException valid) { thrown = true; }
            yes(thrown, "invalid range/k must throw before any output write");
            yes(Arrays.equals(output, new int[] {73, 73, 73, 73, 73}), "invalid request wrote output");
        }
        yes(!M3LongRankBatchNative.nativeAvailable() || Boolean.getBoolean("m3.collections.rank.native"),
                "native availability must be explicit");
        if (!M3LongRankBatchNative.nativeAvailable()) {
            boolean denied = false;
            try { M3LongRankBatchNative.closestWindowStartsInto(sorted, targets, 0, 1, 1, output, 0, true); }
            catch (UnsatisfiedLinkError expected) { denied = true; }
            yes(denied, "required native must not fall back");
        }
    }

    private static void check(long[] sorted, long[] queries, int k, boolean forceNative) {
        long[] initialSorted = sorted.clone();
        long[] initialQueries = queries.clone();
        int[] output = new int[queries.length + 6];
        Arrays.fill(output, -8675309);
        M3LongRankBatchNative.closestWindowStartsInto(sorted, queries, 0,
                queries.length, k, output, 3, forceNative);
        int[] boxed = M3LongRankBatchNative.closestWindowStarts(sorted, queries, k);
        for (int i = 0; i < queries.length; i++) {
            int expected = oracle(sorted, queries[i], k);
            yes(expected == output[i + 3], "window oracle mismatch size=" + sorted.length
                    + " k=" + k + " query=" + queries[i] + " got=" + output[i+3] + " expected=" + expected);
            yes(expected == boxed[i], "returned starts mismatch");
        }
        for (int i = 0; i < 3; i++) {
            yes(output[i] == -8675309, "prefix corrupted");
            yes(output[output.length - i - 1] == -8675309, "suffix corrupted");
        }
        yes(Arrays.equals(sorted, initialSorted), "sorted values were modified");
        yes(Arrays.equals(queries, initialQueries), "queries were modified");
    }

    public static long run(boolean forceNative) {
        checks = 0;
        yes(!forceNative || M3LongRankBatchNative.nativeAvailable(), "native mode must load libm3rankbatch");
        failuresAreAtomic();
        check(new long[0], new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}, 0, forceNative);
        long[][] examples = {
                {1, 2, 3, 4, 5}, {-3, -3, -3, -2},
                {Long.MIN_VALUE, Long.MIN_VALUE, -1, 0, 0, Long.MAX_VALUE},
                {Long.MIN_VALUE, Long.MAX_VALUE},
                {0, 0, 0, 0, 0},
                {-2, -1, -1, 0, 0, 0, 1, 1, 2}
        };
        long[] edges = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -8, -2, -1, 0, 1, 2, 8, Long.MAX_VALUE - 1, Long.MAX_VALUE};
        for (long[] array : examples) {
            for (int k = 0; k <= array.length; k++) check(array, edges, k, forceNative);
        }
        Random random = new Random(0x65834_2026L);
        for (int round = 0; round < 330; round++) {
            int n = random.nextInt(55);
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) {
                sorted[i] = switch (random.nextInt(9)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2, 3 -> random.nextInt(9) - 4;
                    default -> random.nextLong();
                };
            }
            Arrays.sort(sorted);
            long[] queries = new long[66 + random.nextInt(12)];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = switch (random.nextInt(6)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    default -> random.nextLong();
                };
            }
            check(sorted, queries, random.nextInt(n + 1), forceNative);
        }
        System.out.println("PASS CLOSEST_WINDOW_JNI_658 checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable() + " forced=" + forceNative);
        return checks;
    }

    public static void main(String[] args) { run(args.length != 0 && args[0].equals("--native")); }
}
