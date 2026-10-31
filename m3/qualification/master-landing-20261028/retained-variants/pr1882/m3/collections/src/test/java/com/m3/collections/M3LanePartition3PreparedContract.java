// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Zero-dependency LC #75 Java/JNI byte-for-byte permutation oracle. */
public final class M3LanePartition3PreparedContract {
    private static long checks;
    private M3LanePartition3PreparedContract() { }

    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable runnable) {
        checks++;
        try {
            runnable.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("wrong exception: " + error, error);
        }
        throw new AssertionError("missing exception " + expected.getSimpleName());
    }

    private static void intCase(int[] source, int from, int to, int pivot) {
        int[] java = source.clone();
        int[] prepared = source.clone();
        long expected = M3LaneAlgorithms.partition3(java, from, to, pivot);
        long got = M3LaneAlgorithms.partition3Prepared(prepared, from, to, pivot, false);
        check(got == expected && Arrays.equals(prepared, java), "int exact partition");
        int low = M3LaneAlgorithms.lowBound(got);
        int high = M3LaneAlgorithms.highBound(got);
        check(from <= low && low <= high && high <= to, "int bounded partition");
        for (int i = from; i < to; i++) {
            check(i < low ? prepared[i] < pivot : i < high ? prepared[i] == pivot
                    : prepared[i] > pivot, "int three-way ordering");
        }
        if (M3LaneAlgorithms.partition3NativeAvailable()) {
            int[] required = source.clone();
            long nativeBounds = M3LaneAlgorithms.partition3Prepared(
                    required, from, to, pivot, true);
            check(nativeBounds == expected && Arrays.equals(required, java),
                    "int forced JNI byte-for-byte parity");
        }
    }

    private static void longCase(long[] source, int from, int to, long pivot) {
        long[] java = source.clone();
        long[] prepared = source.clone();
        long expected = M3LaneAlgorithms.partition3(java, from, to, pivot);
        long got = M3LaneAlgorithms.partition3Prepared(prepared, from, to, pivot, false);
        check(got == expected && Arrays.equals(prepared, java), "long exact partition");
        int low = M3LaneAlgorithms.lowBound(got);
        int high = M3LaneAlgorithms.highBound(got);
        check(from <= low && low <= high && high <= to, "long bounded partition");
        for (int i = from; i < to; i++) {
            check(i < low ? prepared[i] < pivot : i < high ? prepared[i] == pivot
                    : prepared[i] > pivot, "long three-way ordering");
        }
        if (M3LaneAlgorithms.partition3NativeAvailable()) {
            long[] required = source.clone();
            long nativeBounds = M3LaneAlgorithms.partition3Prepared(
                    required, from, to, pivot, true);
            check(nativeBounds == expected && Arrays.equals(required, java),
                    "long forced JNI byte-for-byte parity");
        }
    }

    public static long contracts() {
        checks = 0L;
        if (Boolean.getBoolean("m3.collections.partition3.native.required")) {
            check(M3LaneAlgorithms.partition3NativeAvailable(), "required JNI unavailable");
        }
        Random random = new Random(0x75D3F123L);
        int[] sizes = {0, 1, 2, 3, 7, 63, 255, 256, 257, 511, 512, 513, 1024, 4097};
        for (int size : sizes) {
            for (int trial = 0; trial < 65; trial++) {
                int[] ints = new int[size + 4];
                long[] longs = new long[size + 4];
                ints[0] = Integer.MIN_VALUE;
                ints[ints.length - 1] = Integer.MAX_VALUE;
                longs[0] = Long.MIN_VALUE;
                longs[longs.length - 1] = Long.MAX_VALUE;
                for (int i = 1; i <= size + 2; i++) {
                    ints[i] = switch (trial % 8) {
                        case 0 -> Integer.MIN_VALUE;
                        case 1 -> Integer.MAX_VALUE;
                        case 2 -> i & 1;
                        case 3 -> i % 7 - 3;
                        case 4 -> 0;
                        default -> random.nextInt(101) - 50;
                    };
                    longs[i] = switch (trial % 8) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> i & 1;
                        case 3 -> i % 7 - 3L;
                        case 4 -> 0L;
                        case 5 -> random.nextLong();
                        default -> random.nextInt(101) - 50L;
                    };
                }
                int offset = trial % 3;
                int from = offset;
                int to = offset + size;
                int pivotInt = trial % 5 == 0 ? Integer.MIN_VALUE
                        : trial % 5 == 1 ? Integer.MAX_VALUE : trial % 5 - 2;
                long pivotLong = trial % 5 == 0 ? Long.MIN_VALUE
                        : trial % 5 == 1 ? Long.MAX_VALUE : trial % 5 - 2L;
                intCase(ints, from, to, pivotInt);
                longCase(longs, from, to, pivotLong);
            }
        }
        int[] ints = {8, 7, 6, 5};
        long[] longs = {8, 7, 6, 5};
        expect(NullPointerException.class, () ->
                M3LaneAlgorithms.partition3Prepared((int[]) null, 0, 0, 0, false));
        expect(NullPointerException.class, () ->
                M3LaneAlgorithms.partition3Prepared((long[]) null, 0, 0, 0, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.partition3Prepared(ints, -1, 3, 0, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.partition3Prepared(longs, 3, 2, 0, false));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.partition3Prepared(ints, 0, 5, 0, true));
        expect(IndexOutOfBoundsException.class, () ->
                M3LaneAlgorithms.partition3Prepared(longs, 0, 5, 0, true));
        check(Arrays.equals(ints, new int[] {8, 7, 6, 5}), "int validation mutation");
        check(Arrays.equals(longs, new long[] {8, 7, 6, 5}), "long validation mutation");
        if (!M3LaneAlgorithms.partition3NativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () ->
                    M3LaneAlgorithms.partition3Prepared(ints, 0, 4, 6, true));
            expect(UnsatisfiedLinkError.class, () ->
                    M3LaneAlgorithms.partition3Prepared(longs, 0, 4, 6, true));
            check(Arrays.equals(ints, new int[] {8, 7, 6, 5}), "int missing JNI mutation");
            check(Arrays.equals(longs, new long[] {8, 7, 6, 5}), "long missing JNI mutation");
        }
        System.out.println("PASS M3_LANE_PARTITION3_PREPARED checks=" + checks
                + " native=" + M3LaneAlgorithms.partition3NativeAvailable());
        return checks;
    }

    public static void main(String[] args) { contracts(); }
}
