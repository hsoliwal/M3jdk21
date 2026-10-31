// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** LC #35 signed predecessor-index oracle with original Java and JNI parity. */
public final class M3LongFloorIndicesContract {
    private static long assertions;
    private M3LongFloorIndicesContract() { }

    private static void equal(int expected, int actual, String label) {
        assertions++;
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }

    private static void same(int[] expected, int[] actual, String label) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " actual=" + Arrays.toString(actual)
                    + " expected=" + Arrays.toString(expected));
        }
    }

    private static int reference(long[] sorted, long key) {
        int result = -1;
        for (int i = 0; i < sorted.length; i++) if (sorted[i] <= key) result = i;
        return result;
    }

    private static void check(long[] sorted, long[] queries, boolean nativeRequired) {
        int[] expected = new int[queries.length];
        for (int i = 0; i < queries.length; i++) expected[i] = reference(sorted, queries[i]);
        if (!nativeRequired) {
            same(expected, M3LongRankBatchNative.floorIndices(sorted, queries), "detached");
        }
        int[] destination = new int[queries.length + 6];
        Arrays.fill(destination, 13579);
        M3LongRankBatchNative.floorIndicesInto(
                sorted, queries, 0, queries.length, destination, 3, nativeRequired);
        same(expected, Arrays.copyOfRange(destination, 3, destination.length - 3), "window");
        for (int i : new int[] {0, 1, 2, destination.length - 3,
                destination.length - 2, destination.length - 1}) {
            equal(13579, destination[i], "untouched padding");
        }
    }

    private static void invalid(long[] sorted) {
        int[] output = {7, 7, 7};
        try {
            M3LongRankBatchNative.floorIndicesInto(
                    sorted, new long[] {1, 2}, 0, 2, output, 2, false);
            throw new AssertionError("invalid output window accepted");
        } catch (IndexOutOfBoundsException expected) {
            same(new int[] {7, 7, 7}, output, "invalid window preflight");
        }
        try {
            M3LongRankBatchNative.floorIndicesInto(
                    sorted, new long[] {1}, 0, 1, null, 0, false);
            throw new AssertionError("null output accepted");
        } catch (NullPointerException expected) {
            assertions++;
        }
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length != 0 && args[0].equals("--native");
        if (nativeRequired && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("native m3rankbatch was required but not linked");
        }
        check(new long[0], new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE}, nativeRequired);
        check(new long[] {Long.MIN_VALUE, -1, -1, 0, Long.MAX_VALUE},
                new long[] {Long.MIN_VALUE, -2, -1, 0, 1, Long.MAX_VALUE}, nativeRequired);
        Random random = new Random(0x3500C0DEL);
        for (int round = 0; round < 100; round++) {
            long[] sorted = new long[random.nextInt(250)];
            for (int i = 0; i < sorted.length; i++) {
                sorted[i] = random.nextInt(25) - 12;
            }
            Arrays.sort(sorted);
            long[] queries = new long[70 + random.nextInt(180)];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = i % 29 == 0 ? Long.MAX_VALUE
                        : i % 31 == 0 ? Long.MIN_VALUE : random.nextInt(33) - 16;
            }
            check(sorted, queries, nativeRequired);
        }
        invalid(new long[] {-3, 1, 2});
        System.out.println("PASS M3_FLOOR_INDEX_CONTRACT mode="
                + (nativeRequired ? "native" : "java") + " assertions=" + assertions);
    }
}
