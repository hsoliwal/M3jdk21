// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Standalone LeetCode #239 Java/native differential, no Maven dependencies. */
public final class M3LaneSlidingMaximumContract {
    private static long assertions;
    private M3LaneSlidingMaximumContract() { }

    private static void check(boolean ok, String why) {
        assertions++;
        if (!ok) throw new AssertionError(why);
    }

    private static void same(int[] expected, int[] actual, String why) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected exception " + failure, failure);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }

    private static int[] oracle(long[] values, int from, int to, int window) {
        int[] result = new int[to - from - window + 1];
        for (int start = from; start + window <= to; start++) {
            int best = start;
            for (int i = start + 1; i < start + window; i++) {
                if (values[i] > values[best]) best = i; // oldest equal maximum wins
            }
            result[start - from] = best;
        }
        return result;
    }

    private static void exercise(long[] values, int from, int to, int k,
            boolean requireNative) {
        long[] before = values.clone();
        int[] expected = oracle(values, from, to, k);
        int[] actual = new int[expected.length + 8];
        Arrays.fill(actual, -17);
        M3LaneAlgorithms.slidingMaximumIndicesInto(values, from, to, k,
                actual, 3, requireNative);
        int[] selected = Arrays.copyOfRange(actual, 3, 3 + expected.length);
        same(expected, selected, "range=" + from + ":" + to + " window=" + k);
        for (int i = 0; i < 3; i++) check(actual[i] == -17, "untouched prefix");
        for (int i = 3 + expected.length; i < actual.length; i++) {
            check(actual[i] == -17, "untouched suffix");
        }
        check(Arrays.equals(before, values), "original owner must be immutable");
        if (from == 0 && to == values.length) {
            same(expected, M3LaneAlgorithms.slidingMaximumIndices(values, k),
                    "allocation convenience API");
        }
    }

    private static void invalid() {
        long[] source = {Long.MIN_VALUE, 4, 4, Long.MAX_VALUE};
        int[] output = {-2, -2, -2, -2};
        expect(NullPointerException.class, () -> M3LaneAlgorithms.slidingMaximumIndices(null, 1));
        expect(IllegalArgumentException.class, () -> M3LaneAlgorithms.slidingMaximumIndices(source, 0));
        expect(IllegalArgumentException.class, () -> M3LaneAlgorithms.slidingMaximumIndices(source, 5));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.slidingMaximumIndicesInto(source, -1, 3, 1, output, 0, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.slidingMaximumIndicesInto(source, 2, 5, 1, output, 0, false));
        expect(IllegalArgumentException.class, () ->
                M3LaneAlgorithms.slidingMaximumIndicesInto(source, 1, 2, 2, output, 0, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.slidingMaximumIndicesInto(source, 0, 4, 1, output, 1, false));
        expect(NullPointerException.class, () ->
                M3LaneAlgorithms.slidingMaximumIndicesInto(source, 0, 4, 1, null, 0, false));
        same(new int[]{-2, -2, -2, -2}, output, "invalid shape must not write output");
        if (!M3LaneAlgorithms.slidingMaximumNativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () ->
                    M3LaneAlgorithms.slidingMaximumIndicesInto(source, 0, 4, 2,
                            output, 0, true));
            same(new int[]{-2, -2, -2, -2}, output, "required JNI failure leaves output intact");
        }
    }

    private static void directJniInvalid() throws Exception {
        if (!M3LaneAlgorithms.slidingMaximumNativeAvailable()) return;
        var method = M3LaneAlgorithms.class.getDeclaredMethod("slidingMaximumIndices0",
                long[].class, int.class, int.class, int.class, int[].class, int.class);
        method.setAccessible(true);
        int[] out = {-22, -22, -22, -22};
        for (int[] shape : new int[][] {{-1, 3, 2, 0}, {0, 5, 2, 0},
                {0, 4, 0, 0}, {0, 4, 5, 0}, {0, 4, 1, 1}}) {
            try {
                method.invoke(null, new long[]{1, 2, 3, 4},
                        shape[0], shape[1], shape[2], out, shape[3]);
                throw new AssertionError("JNI accepted invalid " + Arrays.toString(shape));
            } catch (java.lang.reflect.InvocationTargetException ex) {
                check(ex.getCause() instanceof IllegalArgumentException
                        || ex.getCause() instanceof IndexOutOfBoundsException,
                        "native prevalidation exception " + ex.getCause());
            }
            same(new int[]{-22, -22, -22, -22}, out,
                    "JNI validation must leave caller output unchanged");
        }
    }

    public static long contracts() throws Exception {
        assertions = 0;
        boolean requireNative = Boolean.getBoolean("m3.test.native.required");
        if (requireNative) check(M3LaneAlgorithms.slidingMaximumNativeAvailable(),
                "requested JNI library must actually load");
        invalid();
        long[] example = {1, 3, -1, -3, 5, 3, 6, 7};
        same(new int[] {1, 1, 4, 4, 6, 7},
                M3LaneAlgorithms.slidingMaximumIndices(example, 3), "LC239 canonical example");
        for (long[] values : new long[][] {
                {Long.MIN_VALUE}, {Long.MAX_VALUE, Long.MIN_VALUE},
                {5, 5, 5, 5, 5}, {Long.MIN_VALUE, 0, Long.MAX_VALUE, -1, 0},
                {1, 3, -1, -3, 5, 3, 6, 7}, {9, 8, 7, 6, 5, 4},
                {0, Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE}}) {
            for (int k = 1; k <= values.length; k++) {
                exercise(values, 0, values.length, k, requireNative);
                for (int from = 0; from < values.length; from++) {
                    for (int to = from + k; to <= values.length; to++) {
                        exercise(values, from, to, k, requireNative);
                    }
                }
            }
        }
        Random random = new Random(0x23935L);
        for (int size : new int[]{1, 2, 3, 31, 32, 33, 63, 64, 127, 255, 256, 257, 512, 513, 2049}) {
            for (int round = 0; round < 22; round++) {
                long[] data = new long[size];
                for (int i = 0; i < size; i++) {
                    data[i] = switch (random.nextInt(9)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        default -> random.nextInt(19) - 9L;
                    };
                }
                int k = 1 + random.nextInt(size);
                exercise(data, 0, size, k, requireNative);
                int from = random.nextInt(size);
                int to = from + 1 + random.nextInt(size - from);
                exercise(data, from, to, 1 + random.nextInt(to - from), requireNative);
            }
        }
        directJniInvalid();
        System.out.println("PASS M3_LC239_SLIDING_INDEX checks=" + assertions
                + " native=" + M3LaneAlgorithms.slidingMaximumNativeAvailable());
        return assertions;
    }

    public static void main(String[] ignored) throws Exception { contracts(); }
}
