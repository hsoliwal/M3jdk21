// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * LeetCode 350 problem-category differential: duplicate-aware signed-long
 * sorted intersection cardinality. Hash frequencies are only a test oracle;
 * production Java/JNI use parent-owned primitive lanes and two cursors.
 */
public final class M3LongRankIntersectionContract {
    private static long checks;
    private M3LongRankIntersectionContract() { }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void same(long[] expected, long[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " changed caller input");
        }
    }

    private static void nullRejected(Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("null rank index was accepted");
    }

    private static long oracle(long[] a, long[] b) {
        Map<Long, Integer> counts = new HashMap<>();
        for (long value : a) counts.merge(value, 1, Integer::sum);
        long intersection = 0L;
        for (long value : b) {
            int available = counts.getOrDefault(value, 0);
            if (available > 0) {
                intersection++;
                counts.put(value, available - 1);
            }
        }
        return intersection;
    }

    private static void pair(long[] inputLeft, long[] inputRight) {
        long[] beforeLeft = inputLeft.clone();
        long[] beforeRight = inputRight.clone();
        M3LongRankIndex left = M3LongRankIndex.copyOf(inputLeft);
        M3LongRankIndex right = M3LongRankIndex.copyOf(inputRight);
        long expected = oracle(inputLeft, inputRight);
        equal(expected, left.intersectionCount(right), "intersection");
        equal(expected, right.intersectionCount(left), "symmetric intersection");
        long union = (long) inputLeft.length + inputRight.length - expected;
        equal(union, left.unionCount(right), "multiset union");
        equal(union, right.unionCount(left), "symmetric union");
        same(beforeLeft, inputLeft, "left");
        same(beforeRight, inputRight, "right");
        equal(inputLeft.length, left.intersectionCount(left), "self intersection");
        equal(inputRight.length, right.unionCount(right), "self union");
    }

    private static long key(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> random.nextLong();
            default -> random.nextInt(17) - 8L;
        };
    }

    public static long contracts(boolean requireNative) {
        if (M3LongRankBatchNative.nativeAvailable() != requireNative) {
            throw new AssertionError("JNI availability differs from requested test mode");
        }
        checks = 0L;
        pair(new long[0], new long[0]);
        pair(new long[0], new long[]{1, 2});
        pair(new long[]{1, 1, 1}, new long[]{1});
        pair(new long[]{Long.MIN_VALUE, -4, -4, 0, Long.MAX_VALUE},
             new long[]{Long.MIN_VALUE, -4, -4, -4, Long.MAX_VALUE});
        pair(new long[]{-3, -1, 0, 2}, new long[]{-2, 1, 3});
        M3LongRankIndex original = M3LongRankIndex.copyOf(new long[]{2, 2, 1});
        long[] mutation = {1, 2, 2};
        M3LongRankIndex preserved = M3LongRankIndex.copyOf(mutation);
        mutation[0] = Long.MIN_VALUE;
        equal(3, original.intersectionCount(preserved), "immutable snapshot");
        nullRejected(() -> original.intersectionCount(null));
        nullRejected(() -> original.unionCount(null));

        Random random = new Random(0x350AAC26L);
        int[] sizes = {0, 1, 2, 3, 31, 63, 64, 127, 128, 129, 256, 512, 1024, 2048, 4096};
        for (int trial = 0; trial < 1000; trial++) {
            int n = sizes[random.nextInt(sizes.length)];
            int m = sizes[random.nextInt(sizes.length)];
            long[] a = new long[n];
            long[] b = new long[m];
            for (int i = 0; i < n; i++) a[i] = key(random);
            for (int i = 0; i < m; i++) b[i] = key(random);
            pair(a, b);
        }
        System.out.println("PASS M3_LEETCODE350_JNI checks=" + checks);
        return checks;
    }

    public static void main(String[] args) {
        if (args.length != 1 || (!args[0].equals("--java") && !args[0].equals("--native"))) {
            throw new IllegalArgumentException("expected --java or --native");
        }
        contracts(args[0].equals("--native"));
    }
}
