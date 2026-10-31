// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Deterministic LeetCode #88/#26 multiset-union differential oracle.
 * Boxed TreeMap is deliberately ONLY the independent TEST reference;
 * production M3 long[] and JNI paths retain no per-element object.
 */
public final class M3LongUnionValuesContract {
    private static long assertions;

    private M3LongUnionValuesContract() { }

    private static void check(boolean good, String note) {
        assertions++;
        if (!good) throw new AssertionError(note);
    }

    private static long[] reference(long[] left, long[] right, boolean distinct) {
        TreeMap<Long, Integer> multiplicity = new TreeMap<>();
        for (long v : left) multiplicity.merge(v, 1, Integer::sum);
        TreeMap<Long, Integer> other = new TreeMap<>();
        for (long v : right) other.merge(v, 1, Integer::sum);
        for (Map.Entry<Long, Integer> row : other.entrySet()) {
            multiplicity.merge(row.getKey(), row.getValue(), Math::max);
        }
        int size = 0;
        for (int count : multiplicity.values()) size += distinct ? 1 : count;
        long[] result = new long[size];
        int at = 0;
        for (var row : multiplicity.entrySet()) {
            int copies = distinct ? 1 : row.getValue();
            while (copies-- > 0) result[at++] = row.getKey();
        }
        return result;
    }

    private static void verify(long[] left, long[] right) {
        long[] lCopy = left.clone(), rCopy = right.clone();
        for (boolean distinct : new boolean[] {false, true}) {
            long[] want = reference(left, right, distinct);
            long[] got = M3LongRankBatchNative.unionValues(left, right, distinct);
            check(Arrays.equals(want, got), "Java/optional-JNI union values difference:"
                    + "\nleft=" + Arrays.toString(left)
                    + "\nright=" + Arrays.toString(right)
                    + "\nexpected=" + Arrays.toString(want)
                    + "\nactual=" + Arrays.toString(got));
            if (M3LongRankBatchNative.nativeAvailable()) {
                long[] forced = M3LongRankBatchNative.unionValues(left, right, distinct, true);
                check(Arrays.equals(want, forced), "required JNI union values difference");
            }
        }
        check(Arrays.equals(left, lCopy), "left snapshot mutated");
        check(Arrays.equals(right, rCopy), "right snapshot mutated");
    }

    private static void expect(Class<? extends Throwable> type, Runnable job) {
        assertions++;
        try {
            job.run();
            throw new AssertionError("missing " + type);
        } catch (Throwable thrown) {
            if (!type.isInstance(thrown)) throw thrown;
        }
    }

    public static long contracts(String mode) {
        assertions = 0L;
        boolean nativeEnabled = M3LongRankBatchNative.nativeAvailable();
        if ("native".equals(mode)) {
            check(nativeEnabled, "native library not loaded");
        } else if ("java".equals(mode)) {
            check(!nativeEnabled, "native library unexpectedly loaded");
            expect(UnsatisfiedLinkError.class, () ->
                    M3LongRankBatchNative.unionValues(new long[] {1}, new long[] {1},
                            false, true));
        }
        expect(NullPointerException.class,
                () -> M3LongRankBatchNative.unionValues(null, new long[] {1}, false));
        expect(NullPointerException.class,
                () -> M3LongRankBatchNative.unionValues(new long[] {1}, null, false));
        long[] empty = {};
        verify(empty, empty);
        verify(new long[] {1}, empty);
        verify(empty, new long[] {3, 3, 3});
        verify(new long[] {1, 1, 2}, new long[] {1, 2, 2});
        verify(new long[] {Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE},
                new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE, Long.MAX_VALUE});
        verify(new long[] {0, 0, 0, 0}, new long[] {0, 0, 0, 0, 0, 0, 0});
        verify(new long[] {-5, -5, 1, 9, 10, 20},
                new long[] {-9, -8, 10, 11, 22, 30});
        Random random = new Random(0x5EED77L);
        int[] sizes = {0, 1, 2, 3, 15, 63, 127, 128, 129, 255, 256, 257,
                511, 512, 513, 1024};
        for (int m : sizes) {
            for (int n : sizes) {
                for (int repeat = 0; repeat < 7; repeat++) {
                    long[] left = new long[m], right = new long[n];
                    for (int i = 0; i < m; i++) left[i] = random.nextInt(73) - 36;
                    for (int i = 0; i < n; i++) right[i] = random.nextInt(73) - 36;
                    Arrays.sort(left);
                    Arrays.sort(right);
                    verify(left, right);
                }
            }
        }
        System.out.println("PASS M3_UNION_VALUES_CONTRACT assertions=" + assertions
                + " native=" + nativeEnabled);
        return assertions;
    }

    public static void main(String[] args) {
        contracts(args.length == 0 ? "" : args[0]);
    }
}
