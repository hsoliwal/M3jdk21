// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** LC #315 primitive-index merge-count differential over Java and JNI. */
public final class M3LongCountSmallerRightContract {
    private static long checks;

    private M3LongCountSmallerRightContract() { }

    private static void same(int[] expected, int[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static int[] quadraticOracle(long[] values) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                if (values[j] < values[i]) result[i]++;
            }
        }
        return result;
    }

    private static void check(long[] values, boolean required, boolean oldLibrary) {
        long[] saved = values.clone();
        int[] expected = quadraticOracle(values);
        same(expected, M3LongRankBatchNative.countSmallerRightJava(values), "Java oracle");
        same(expected, M3LongRankBatchNative.countSmallerRight(values), "optional JNI");
        if (required) {
            same(expected, M3LongRankBatchNative.countSmallerRightNativeRequired(values),
                    "required JNI");
        }
        if (!Arrays.equals(saved, values)) {
            throw new AssertionError("input array mutated");
        }
    }

    private static void large(boolean nativeRequired) {
        final int size = 100_000;
        long[] descending = new long[size];
        for (int i = 0; i < size; i++) descending[i] = size - i;
        int[] result = M3LongRankBatchNative.countSmallerRight(descending);
        for (int i = 0; i < size; i++) {
            if (result[i] != size - i - 1) throw new AssertionError("large descending " + i);
        }
        checks++;
        if (nativeRequired) {
            same(result, M3LongRankBatchNative.countSmallerRightNativeRequired(descending),
                    "native large count parity");
        }
        Arrays.fill(descending, Long.MIN_VALUE);
        result = M3LongRankBatchNative.countSmallerRight(descending);
        for (int count : result) if (count != 0) throw new AssertionError("duplicate large input");
        checks++;
    }

    public static void main(String[] args) {
        boolean required = Arrays.asList(args).contains("--native");
        boolean older = Arrays.asList(args).contains("--old-native");
        boolean missing = Arrays.asList(args).contains("--missing-native");
        if (required && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("required JNI library unavailable");
        }
        if (older && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("expected legacy JNI library");
        }
        if (older || missing) {
            try {
                M3LongRankBatchNative.countSmallerRightNativeRequired(new long[] {4, 3});
                throw new AssertionError("missing JNI ABI silently downgraded");
            } catch (UnsatisfiedLinkError expected) {
                checks++;
            }
        }
        check(new long[0], required, older);
        check(new long[] {9}, required, older);
        check(new long[] {5, 2, 6, 1}, required, older);
        check(new long[] {Long.MAX_VALUE, Long.MIN_VALUE, 0, Long.MIN_VALUE}, required, older);
        check(new long[] {3, 3, 3, 3, 3, 3, 3, 3}, required, older);
        check(new long[] {8, 7, 6, 5, 4, 3, 2, 1}, required, older);
        Random random = new Random(315315L);
        for (int size : new int[] {0, 1, 2, 3, 7, 31, 32, 33, 63, 64, 65,
                127, 128, 255, 256, 257, 511, 512, 1024}) {
            for (int iteration = 0; iteration < 75; iteration++) {
                long[] values = new long[size];
                for (int i = 0; i < size; i++) {
                    long value = random.nextInt(23) - 11;
                    values[i] = i % 29 == 0 ? Long.MIN_VALUE
                            : i % 31 == 0 ? Long.MAX_VALUE : value;
                }
                check(values, required, older);
            }
        }
        large(required);
        String mode = required ? "native" : older ? "old-native" : missing ? "missing-native" : "java";
        System.out.println("PASS M3_LC315_COUNT_SMALLER mode=" + mode + " checks=" + checks);
    }
}
