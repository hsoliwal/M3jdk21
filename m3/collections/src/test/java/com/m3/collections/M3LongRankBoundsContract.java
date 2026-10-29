// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Standalone Java21/JNI differential for LeetCode 34 and 35 boundary cases. */
public final class M3LongRankBoundsContract {
    private static long checks;

    private M3LongRankBoundsContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable block) {
        checks++;
        try { block.run(); }
        catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("Wrong exception: " + ex, ex);
        }
        throw new AssertionError("Missing " + type.getSimpleName());
    }

    private static int[] linearBounds(long[] sorted, long key) {
        int lo = 0, hi = 0;
        for (long value : sorted) {
            if (value < key) lo++;
            if (value <= key) hi++;
        }
        return new int[] {lo, hi};
    }

    private static void verify(long[] values, long[] queries) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        long[] original = sorted.clone();
        long[] unchangedQueries = queries.clone();
        int[] pairs = M3LongRankBatchNative.lowerUpperBounds(sorted, queries);
        check(pairs.length == queries.length * 2, "interleaved length");
        int[] reused = new int[pairs.length];
        Arrays.fill(reused, -1);
        M3LongRankBatchNative.lowerUpperBoundsInto(sorted, queries, reused);
        check(Arrays.equals(pairs, reused), "new vs reused parity");
        int[] oldLower = M3LongRankBatchNative.lowerBounds(sorted, queries);
        for (int i = 0; i < queries.length; i++) {
            int[] expected = linearBounds(sorted, queries[i]);
            check(pairs[i * 2] == expected[0], "lower signed order case=" + i);
            check(pairs[i * 2 + 1] == expected[1], "upper signed order case=" + i);
            check(oldLower[i] == expected[0], "original lower bound unchanged case=" + i);
            check(pairs[i * 2 + 1] >= pairs[i * 2], "lower <= upper");
        }
        check(Arrays.equals(original, sorted), "input payload untouched");
        check(Arrays.equals(unchangedQueries, queries), "queries untouched");
    }

    public static long contracts() {
        checks = 0;
        boolean expectedNative = Boolean.getBoolean("m3.rank.requireNative");
        check(!expectedNative || M3LongRankBatchNative.nativeAvailable(), "JNI was required");
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerUpperBounds(null, new long[0]));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerUpperBounds(new long[0], null));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerUpperBoundsInto(null, new long[0], new int[0]));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerUpperBoundsInto(new long[0], null, new int[0]));
        throwsType(NullPointerException.class, () ->
                M3LongRankBatchNative.lowerUpperBoundsInto(new long[0], new long[0], null));
        int[] unchanged = {123};
        throwsType(IllegalArgumentException.class, () ->
                M3LongRankBatchNative.lowerUpperBoundsInto(new long[]{1}, new long[]{1}, unchanged));
        check(unchanged[0] == 123, "invalid output length leaves output untouched");
        int[] reset = {-1, -1, -1, -1};
        M3LongRankBatchNative.lowerUpperBoundsInto(new long[0], new long[]{1, 2}, reset);
        check(Arrays.equals(reset, new int[4]), "empty sorted owner resets reused pair lane");
        verify(new long[0], new long[0]);
        verify(new long[0], new long[]{Long.MIN_VALUE, -3, 0, 9, Long.MAX_VALUE});
        verify(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, 0, 0, 0, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
        verify(new long[]{3, 3, 3, 3}, new long[]{2, 3, 4});
        for (int count : new int[]{0, 1, 2, 3, 31, 63, 64, 65, 127, 128, 257, 1025}) {
            for (int length : new int[]{0, 1, 2, 7, 31, 64, 128, 511}) {
                Random random = new Random(0x1EE7C0DEL + count * 17L + length);
                long[] values = new long[length];
                long[] queries = new long[count];
                for (int i = 0; i < length; i++) values[i] = randomValue(random);
                for (int i = 0; i < count; i++) queries[i] = randomValue(random);
                verify(values, queries);
            }
        }
        System.out.println("PASS M3_LONG_RANK_BOUNDS checks=" + checks);
        return checks;
    }

    private static long randomValue(Random random) {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> random.nextLong();
            default -> random.nextInt(23) - 11;
        };
    }

    public static void main(String[] args) { contracts(); }
}
