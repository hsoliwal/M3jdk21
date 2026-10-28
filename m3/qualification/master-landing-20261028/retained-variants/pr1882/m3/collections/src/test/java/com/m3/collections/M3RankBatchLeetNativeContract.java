// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** LeetCode 34/35/350/88 category oracle over original primitive Java/JNI owners. */
public final class M3RankBatchLeetNativeContract {
    private static long checks;
    private M3RankBatchLeetNativeContract() { }
    private static void eq(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    private static int bound(long[] values, long key, boolean upper) {
        int lo = 0, hi = values.length;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (upper ? values[mid] <= key : values[mid] < key) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }
    private static long intersection(long[] left, long[] right, boolean distinct) {
        int i = 0, j = 0;
        long hits = 0, last = 0;
        boolean seen = false;
        while (i < left.length && j < right.length) {
            if (left[i] < right[j]) i++;
            else if (left[i] > right[j]) j++;
            else {
                if (!distinct || !seen || last != left[i]) {
                    hits++; last = left[i]; seen = true;
                }
                i++; j++;
            }
        }
        return hits;
    }
    public static void main(String[] args) {
        boolean nativeRequired = Boolean.getBoolean("m3.test.native.required");
        if (nativeRequired && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("Required JNI library not loaded");
        }
        Random random = new Random(0x343535088L);
        int[] sizes = {0, 1, 2, 63, 64, 65, 255, 256, 257, 512};
        for (int size : sizes) for (int round = 0; round < 80; round++) {
            long[] owner = new long[size];
            long[] other = new long[size];
            for (int i = 0; i < size; i++) {
                owner[i] = i % 13 == 0 ? Long.MIN_VALUE
                        : i % 13 == 1 ? Long.MAX_VALUE : random.nextInt(19) - 9;
                other[i] = random.nextInt(25) - 12;
            }
            Arrays.sort(owner);
            Arrays.sort(other);
            long[] queries = new long[260];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = i % 17 == 0 ? Long.MIN_VALUE
                        : i % 17 == 1 ? Long.MAX_VALUE
                        : round % 2 == 0 ? i / 12L - 10L : random.nextInt(29) - 14L;
            }
            int[] lower = new int[queries.length + 6];
            int[] upper = new int[queries.length + 6];
            int[] ranges = new int[2 * queries.length + 6];
            Arrays.fill(lower, -7); Arrays.fill(upper, -7); Arrays.fill(ranges, -7);
            M3LongRankBatchNative.lowerBoundsInto(owner, queries, 1, 258, lower, 3);
            M3LongRankBatchNative.upperBoundsInto(owner, queries, 1, 258, upper, 3);
            M3LongRankBatchNative.equalRangesInto(owner, queries, 1, 258,
                    ranges, 3, nativeRequired);
            for (int i = 0; i < 258; i++) {
                int low = bound(owner, queries[i + 1], false);
                int high = bound(owner, queries[i + 1], true);
                eq(low, lower[i + 3]); eq(high, upper[i + 3]);
                eq(low, ranges[3 + (i << 1)]); eq(high, ranges[4 + (i << 1)]);
            }
            eq(-7, lower[0]); eq(-7, upper[2]); eq(-7, ranges[2]);
            eq(-7, ranges[ranges.length - 1]);
            for (boolean distinct : new boolean[]{false, true}) {
                eq(intersection(owner, other, distinct),
                    M3LongRankBatchNative.intersectionWindowCount(
                        owner, 0, size, other, 0, size, distinct, nativeRequired));
            }
        }
        System.out.println("PASS M3_RANK_LEET_JNI checks=" + checks
                + " native=" + nativeRequired);
    }
}
