// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Dependency-free boundary/count oracle against a linear signed-long reference. */
public final class M3RankBatchIntoContract {
    private static long checks;
    private M3RankBatchIntoContract() { }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }

    private static void same(int[] a, int[] b, String what) {
        check(Arrays.equals(a, b), what + " expected=" + Arrays.toString(a)
                + " actual=" + Arrays.toString(b));
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("unexpected exception: " + failure, failure);
        }
        throw new AssertionError("no expected exception: " + expected);
    }

    private static void caseOf(long[] original, long[] queries) {
        long[] sorted = original.clone();
        Arrays.sort(sorted);
        long[] sourceCopy = sorted.clone();
        long[] queryCopy = queries.clone();
        int[] expectedUpper = new int[queries.length];
        int[] expectedCounts = new int[queries.length];
        for (int i = 0; i < queries.length; i++) {
            for (long value : sorted) {
                if (value <= queries[i]) expectedUpper[i]++;
                if (value == queries[i]) expectedCounts[i]++;
            }
        }
        same(expectedUpper, M3LongRankBatchNative.upperBounds(sorted, queries), "upperBounds");
        same(expectedCounts, M3LongRankBatchNative.counts(sorted, queries), "counts");

        int[] output = new int[queries.length + 9];
        Arrays.fill(output, -73);
        M3LongRankBatchNative.countInto(sorted, queries, 0, queries.length, output, 4);
        for (int i = 0; i < queries.length; i++) check(output[4 + i] == expectedCounts[i], "countInto");
        for (int i = 0; i < 4; i++) check(output[i] == -73, "prefix canary");
        for (int i = queries.length + 4; i < output.length; i++) check(output[i] == -73, "suffix canary");

        if (queries.length >= 3) {
            Arrays.fill(output, -73);
            M3LongRankBatchNative.countInto(sorted, queries, 1, queries.length - 2, output, 2);
            for (int i = 0; i < queries.length - 2; i++) {
                check(output[2 + i] == expectedCounts[i + 1], "sliced query/output window");
            }
            check(output[1] == -73, "slice prefix canary");
            check(output[queries.length] == -73, "slice suffix canary");
        }
        check(Arrays.equals(sorted, sourceCopy), "input sorted unchanged");
        check(Arrays.equals(queries, queryCopy), "queries unchanged");
    }

    private static void invalid() {
        long[] sorted = {-1, 0, 0, Long.MAX_VALUE};
        long[] queries = {0, 1};
        int[] out = {17, 18, 19, 20};
        throwsType(NullPointerException.class, () -> M3LongRankBatchNative.countInto(null, queries, 0, 1, out, 0));
        throwsType(NullPointerException.class, () -> M3LongRankBatchNative.countInto(sorted, null, 0, 1, out, 0));
        throwsType(NullPointerException.class, () -> M3LongRankBatchNative.countInto(sorted, queries, 0, 1, null, 0));
        throwsType(IndexOutOfBoundsException.class, () -> M3LongRankBatchNative.countInto(sorted, queries, -1, 1, out, 0));
        throwsType(IndexOutOfBoundsException.class, () -> M3LongRankBatchNative.countInto(sorted, queries, 0, -1, out, 0));
        throwsType(IndexOutOfBoundsException.class, () -> M3LongRankBatchNative.countInto(sorted, queries, 2, 1, out, 0));
        throwsType(IndexOutOfBoundsException.class, () -> M3LongRankBatchNative.countInto(sorted, queries, 0, 2, out, 3));
        M3LongRankBatchNative.countInto(sorted, queries, 2, 0, out, 4);
        same(new int[]{17, 18, 19, 20}, out, "invalid and zero windows preserve output");
    }

    private static void nativeInvalid() throws Exception {
        if (!M3LongRankBatchNative.nativeAvailable()) return;
        Method rank = M3LongRankBatchNative.class.getDeclaredMethod("boundBatchInto0",
                long[].class, long[].class, int.class, int.class, int[].class, int.class, int.class);
        rank.setAccessible(true);
        int[] output = {31, 32, 33};
        for (int[] input : new int[][] {{0, 1, 0, 0}, {-1, 1, 0, 2}, {0, 4, 0, 2}}) {
            try {
                rank.invoke(null, new long[]{3, 4}, new long[]{3, 4},
                        input[0], input[1], output, input[2], input[3]);
                throw new AssertionError("native validation accepted bad input " + Arrays.toString(input));
            } catch (InvocationTargetException e) {
                check(e.getCause() instanceof IllegalArgumentException ||
                        e.getCause() instanceof IndexOutOfBoundsException,
                        "native validation cause " + e.getCause());
            }
            same(new int[]{31, 32, 33}, output, "native validation untouched output");
        }
    }

    public static void main(String[] args) throws Exception {
        boolean required = Boolean.getBoolean("m3.test.native.required");
        if (required) check(M3LongRankBatchNative.nativeAvailable(), "native library required");
        else if (!Boolean.getBoolean("m3.collections.rank.native"))
            check(!M3LongRankBatchNative.nativeAvailable(), "no unrequested JNI on Java path");
        caseOf(new long[0], new long[0]);
        caseOf(new long[0], new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE});
        caseOf(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, 0, 0, 0, Long.MAX_VALUE, Long.MAX_VALUE},
                new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
        caseOf(new long[]{Long.MIN_VALUE, 1, Long.MAX_VALUE}, new long[512]);
        Random random = new Random(0x34_35_704L);
        for (int round = 0; round < 250; round++) {
            int size = switch (round % 9) {case 0 -> 0; case 1 -> 1; case 2 -> 32;
                case 3 -> 64; case 4 -> 65; case 5 -> 256; case 6 -> 257;
                case 7 -> 1024; default -> random.nextInt(256);};
            long[] sorted = new long[size];
            for (int i = 0; i < size; i++) sorted[i] = switch (i % 23) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE;
                default -> random.nextInt(73) - 36L;
            };
            int queryCount = switch (round % 8) {case 0 -> 0; case 1 -> 1;
                case 2 -> 63; case 3 -> 64; case 4 -> 65;
                case 5 -> 255; case 6 -> 256; default -> 513;};
            long[] queries = new long[queryCount];
            for (int i = 0; i < queries.length; i++) queries[i] = switch (i % 19) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE;
                default -> random.nextInt(79) - 39L;
            };
            if ((round & 1) == 0) Arrays.sort(queries); // sorted/unsorted query modes
            caseOf(sorted, queries);
        }
        invalid();
        nativeInvalid();
        System.out.println("PASS M3_RANK_BATCH_INTO checks=" + checks + " native=" + M3LongRankBatchNative.nativeAvailable());
    }
}
