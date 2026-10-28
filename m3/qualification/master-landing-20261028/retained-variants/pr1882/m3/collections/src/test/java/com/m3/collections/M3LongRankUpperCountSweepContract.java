// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * Java 21/JNI ordinal-search oracle: LeetCode 34, 35, 88 pattern categories.
 * Signed long input and mutable outputs only; no source payload or callback JNI.
 */
public final class M3LongRankUpperCountSweepContract {
    private static long checks;
    private static final int CANARY = 0x3a51c07;

    private M3LongRankUpperCountSweepContract() { }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void same(int[] expected, int[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void mustThrow(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("wrong exception: " + ex, ex);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static int bound(long[] source, long key, boolean inclusive) {
        int first = 0, last = source.length;
        while (first < last) {
            int mid = first + ((last - first) >>> 1);
            if (inclusive ? source[mid] <= key : source[mid] < key) first = mid + 1;
            else last = mid;
        }
        return first;
    }

    private static long nextValue(Random random) {
        return switch (random.nextInt(11)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            case 4 -> 1L;
            default -> random.nextInt(89) - 44L;
        };
    }

    private static void verify(long[] source, long[] query, String label) {
        long[] ownerBefore = source.clone(), queriesBefore = query.clone();
        int[] upper = new int[query.length], counts = new int[query.length];
        for (int i = 0; i < query.length; i++) {
            upper[i] = bound(source, query[i], true);
            counts[i] = upper[i] - bound(source, query[i], false);
        }
        same(upper, M3LongRankBatchNative.upperBounds(source, query), label + " upper");
        same(counts, M3LongRankBatchNative.counts(source, query), label + " counts");

        int[] bounded = new int[query.length + 4];
        Arrays.fill(bounded, CANARY);
        M3LongRankBatchNative.countInto(source, query, 0, query.length, bounded, 2);
        int[] expected = bounded.clone();
        Arrays.fill(expected, CANARY);
        System.arraycopy(counts, 0, expected, 2, counts.length);
        same(expected, bounded, label + " count window canaries");

        int window = query.length / 2;
        int sourceFrom = query.length / 4;
        int[] partial = new int[window + 5];
        Arrays.fill(partial, CANARY);
        M3LongRankBatchNative.countInto(source, query, sourceFrom, window, partial, 3);
        for (int i = 0; i < partial.length; i++) {
            int wanted = i >= 3 && i < 3 + window ? counts[sourceFrom + i - 3] : CANARY;
            require(partial[i] == wanted, label + " partial count offset=" + i);
        }

        // No output write before Java's complete output/query window validation.
        if (query.length > 0) {
            int[] invalid = new int[query.length + 2];
            Arrays.fill(invalid, CANARY);
            mustThrow(IndexOutOfBoundsException.class,
                    () -> M3LongRankBatchNative.countInto(source, query, 0,
                            query.length + 1, invalid, 1));
            for (int value : invalid) require(value == CANARY, label + " invalid output changed");
        }
        require(Arrays.equals(source, ownerBefore), label + " owner input mutated");
        require(Arrays.equals(query, queriesBefore), label + " query input mutated");
    }

    public static long contracts(boolean requireNative) {
        checks = 0;
        require(!requireNative || M3LongRankBatchNative.nativeAvailable(),
                "required actual m3rankbatch JNI library is unavailable");
        mustThrow(NullPointerException.class,
                () -> M3LongRankBatchNative.upperBounds(null, new long[0]));
        mustThrow(NullPointerException.class,
                () -> M3LongRankBatchNative.counts(new long[0], null));
        mustThrow(IndexOutOfBoundsException.class,
                () -> M3LongRankBatchNative.countInto(new long[0], new long[0],
                        1, 0, new int[0], 0));
        verify(new long[0], new long[0], "empty");
        verify(new long[0], new long[] {Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE},
                "empty owner");
        verify(new long[] {Long.MIN_VALUE, Long.MIN_VALUE, 0L, 0L, Long.MAX_VALUE},
                new long[] {Long.MIN_VALUE, Long.MIN_VALUE + 1L, 0L, Long.MAX_VALUE},
                "signed extremes and duplicate groups");

        Random random = new Random(0x3488f00dL);
        int[] sizes = {0, 1, 2, 3, 7, 31, 64, 127, 256, 257, 1024, 4096, 8192};
        int[] batches = {0, 1, 2, 63, 64, 65, 255, 256, 257, 511, 512, 513, 1024};
        for (int size : sizes) {
            long[] sorted = new long[size];
            for (int i = 0; i < size; i++) sorted[i] = nextValue(random);
            Arrays.sort(sorted);
            for (int count : batches) {
                for (int repeat = 0; repeat < 3; repeat++) {
                    long[] ascending = new long[count];
                    for (int i = 0; i < count; i++) ascending[i] = nextValue(random);
                    Arrays.sort(ascending);
                    verify(sorted, ascending, "ascending n=" + size + " q=" + count);
                    long[] repeated = new long[count];
                    Arrays.fill(repeated, repeat == 0 ? Long.MIN_VALUE
                            : repeat == 1 ? 0L : Long.MAX_VALUE);
                    verify(sorted, repeated, "repeated n=" + size + " q=" + count);
                    long[] descending = ascending.clone();
                    for (int i = 0, j = count - 1; i < j; i++, j--) {
                        long tmp = descending[i]; descending[i] = descending[j]; descending[j] = tmp;
                    }
                    verify(sorted, descending, "descending n=" + size + " q=" + count);
                    long[] zigzag = ascending.clone();
                    for (int i = 1; i + 1 < count; i += 2) {
                        long tmp = zigzag[i]; zigzag[i] = zigzag[i + 1]; zigzag[i + 1] = tmp;
                    }
                    verify(sorted, zigzag, "zigzag n=" + size + " q=" + count);
                }
            }
        }
        System.out.println("PASS M3_LONG_RANK_UPPER_COUNT_SWEEP backend="
                + (M3LongRankBatchNative.nativeAvailable() ? "native" : "java")
                + " checks=" + checks);
        return checks;
    }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !"--native".equals(args[0]))) {
            throw new IllegalArgumentException("use --native or no argument");
        }
        contracts(args.length == 1);
    }
}
