// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Generated from the source-sealed Synexia M3 long-path JNI recipe.
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Java21 oracle of the optional real JNI Region backend (no native stand-in). */
public final class M3LongIndexPathMasksJNISelfTest {
    private static long assertions;
    private static final long CANARY = 0x73a5a5a567c3c3c3L;

    private M3LongIndexPathMasksJNISelfTest() { }

    public static void main(String[] arguments) {
        boolean nativeRequired = arguments.length == 1 && "--native".equals(arguments[0]);
        if (arguments.length > 1 || (arguments.length == 1 && !nativeRequired))
            throw new IllegalArgumentException("use --native or no argument");
        if (!nativeRequired) {
            if (M3LongIndexPathMasksJNI.nativeAvailable())
                throw new AssertionError("no-native test must run without library path");
            fail(UnsatisfiedLinkError.class,
                    () -> M3LongIndexPathMasksJNI.select(new long[0], new int[0],
                            new int[][] {new int[0]}, new int[] {0}, new int[] {0}, 0, 0, 1));
            fail(UnsatisfiedLinkError.class,
                    () -> M3LongIndexPathMasksJNI.scanWindow(new long[0], new int[0],
                            new int[][] {new int[0]}, new int[] {0}, new int[] {0},
                            0, 0, 1, 0, new long[0], 0, 0));
            System.out.println("PASS M3_LONG_PATH_JNI_ABSENT|assertions=" + assertions);
            return;
        }
        check(M3LongIndexPathMasksJNI.nativeAvailable(), "native library must actually load");
        boundaries();
        randomDifferential();
        System.out.println("PASS M3_LONG_PATH_JNI|assertions=" + assertions + "|rounds=1600");
    }

    private static void boundaries() {
        long[] rows = {Long.MIN_VALUE, 7, 7, Long.MAX_VALUE, -999};
        int[] offsets = {0, 1, 1, 3, 4};
        int[][] planes = {{1, 3, 0, 1, 1}, {8, 8, 8, 8, 0}};
        int[] needs = {1, 8}, denies = {2, 0};
        long[] expected = {Long.MIN_VALUE, Long.MAX_VALUE};
        eq(expected, M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                needs, denies, 0, 5, 1));
        eq(new long[] {Long.MAX_VALUE, Long.MIN_VALUE},
                M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                        needs, denies, 3, 4, -1));
        eq(new long[] {Long.MAX_VALUE},
                M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                        needs, denies, 3, 2, -2));
        eq(new long[0], M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                needs, denies, rows.length, 0, 1));
        long[] detached = M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                needs, denies, 0, 5, 1);
        detached[0] = 33;
        eq(Long.MIN_VALUE, rows[0]);
        long[] output = new long[5];
        Arrays.fill(output, CANARY);
        long result = M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                needs, denies, 0, 5, 1, 0, output, 1, 1);
        eq(1, M3LongIndexPathMasks.nextOrdinal(result));
        eq(1, M3LongIndexPathMasks.written(result));
        eq(Long.MIN_VALUE, output[1]);
        eq(CANARY, output[0]);
        eq(CANARY, output[2]);
        long resume = M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                needs, denies, 0, 5, 1, 1, output, 2, 1);
        eq(4, M3LongIndexPathMasks.nextOrdinal(resume));
        eq(1, M3LongIndexPathMasks.written(resume));
        eq(Long.MAX_VALUE, output[2]);
        long zero = M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                needs, denies, 0, 5, 1, 2, output, 0, 0);
        eq(2, M3LongIndexPathMasks.nextOrdinal(zero));
        eq(0, M3LongIndexPathMasks.written(zero));
        eq(CANARY, output[0]);
        fail(IllegalArgumentException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, new int[0], planes,
                        needs, denies, 0, 5, 1));
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                        needs, denies, 0, 5, 0));
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                        needs, denies, 0, 6, 1));
        fail(IllegalArgumentException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, offsets, planes,
                        new int[]{1}, denies, 0, 5, 1));
        fail(NullPointerException.class,
                () -> M3LongIndexPathMasksJNI.select(null, offsets, planes,
                        needs, denies, 0, 5, 1));
        fail(NullPointerException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, offsets,
                        new int[][] {null, new int[8]}, needs, denies, 0, 5, 1));
        fail(IllegalArgumentException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                        needs, denies, 0, 5, 1, 0, rows, 0, 1));
        fail(IllegalArgumentException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                        needs, denies, 0, 5, 1, 0, output, 0,
                        M3LongIndexPathMasks.MAX_WINDOW + 1));
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                        needs, denies, 0, 5, 1, 6, output, 0, 1));
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                        needs, denies, 0, 5, 1, 0, output, output.length, 1));
        long[] sentinel = {1, 2, 3};
        int[] broken = {0, 1, 99, 3, 4};
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, broken, planes,
                        needs, denies, 0, 5, 1, 0, sentinel, 0, 3));
        eq(new long[]{1, 2, 3}, sentinel);
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.select(rows, broken, planes,
                        needs, denies, 0, 5, 1));
        // Validate a later bad offset only when the window reaches that entry.
        long[] prefix = new long[2];
        long prefixToken = M3LongIndexPathMasksJNI.scanWindow(rows, broken, planes,
                new int[]{0, 0}, new int[]{0, 0}, 0, 5, 1, 0, prefix, 0, 2);
        eq(2, M3LongIndexPathMasks.nextOrdinal(prefixToken));
        fail(IndexOutOfBoundsException.class,
                () -> M3LongIndexPathMasksJNI.scanWindow(rows, broken, planes,
                        new int[]{0, 0}, new int[]{0, 0}, 0, 5, 1, 2,
                        sentinel, 0, 1));
        eq(new long[]{1, 2, 3}, sentinel);
        // Exercise the entire bounded native stack region and trailing canary.
        long[] large = new long[M3LongIndexPathMasks.MAX_WINDOW];
        int[] identity = new int[large.length];
        int[] states = new int[large.length];
        for (int i = 0; i < large.length; i++) {
            large[i] = (long)i * -17L;
            identity[i] = i;
            states[i] = Integer.MIN_VALUE;
        }
        long[] page = new long[large.length + 2];
        Arrays.fill(page, CANARY);
        long full = M3LongIndexPathMasksJNI.scanWindow(large, identity,
                new int[][]{states}, new int[]{Integer.MIN_VALUE}, new int[]{0},
                0, large.length, 1, 0, page, 1, large.length);
        eq(large.length, M3LongIndexPathMasks.written(full));
        eq(large.length, M3LongIndexPathMasks.nextOrdinal(full));
        eq(CANARY, page[0]);
        eq(CANARY, page[page.length - 1]);
        eq(large, Arrays.copyOfRange(page, 1, page.length - 1));
    }

    private static void randomDifferential() {
        Random random = new Random(0x4d334a4e493231L);
        for (int round = 0; round < 1600; round++) {
            int length = random.nextInt(100);
            int capacity = random.nextInt(125) + 1;
            int stages = random.nextInt(4) + 1;
            long[] rows = new long[length];
            int[] offsets = new int[length];
            int[][] planes = new int[stages][capacity];
            int[] required = new int[stages];
            int[] forbidden = new int[stages];
            for (int i = 0; i < length; i++) {
                rows[i] = switch (i % 9) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextLong(25) - 12L;
                };
                offsets[i] = random.nextInt(capacity);
            }
            for (int s = 0; s < stages; s++) {
                for (int i = 0; i < capacity; i++) planes[s][i] = random.nextInt();
                switch (round % 7) {
                    case 0 -> { required[s] = 0; forbidden[s] = 0; }
                    case 1 -> { required[s] = Integer.MIN_VALUE; forbidden[s] = 0; }
                    case 2 -> { required[s] = 0; forbidden[s] = Integer.MIN_VALUE; }
                    case 3 -> { required[s] = 3; forbidden[s] = 4; }
                    case 4 -> {
                        int known = length == 0 ? 0 : planes[s][offsets[0]];
                        required[s] = known & 0x81000001;
                        forbidden[s] = ~known & 0x04000002;
                    }
                    case 5 -> { required[s] = -1; forbidden[s] = -1; }
                    default -> { required[s] = random.nextInt(8); forbidden[s] = random.nextInt(8) << 4; }
                }
            }
            int first = length == 0 ? 0 : random.nextInt(length);
            int stride = (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(4));
            int maximum = length == 0 ? 0
                    : (stride > 0 ? (length - 1 - first) / stride : first / -stride) + 1;
            int count = random.nextInt(maximum + 1);
            long[] javaResult = M3LongIndexPathMasks.select(rows, offsets,
                    planes, required, forbidden, first, count, stride);
            long[] nativeResult = M3LongIndexPathMasksJNI.select(rows, offsets,
                    planes, required, forbidden, first, count, stride);
            eq(javaResult, nativeResult);
            if (nativeResult.length != 0) {
                nativeResult[0] ^= 51L;
                eq(javaResult, M3LongIndexPathMasks.select(rows, offsets,
                        planes, required, forbidden, first, count, stride));
            }
            int limit = random.nextInt(8) + 1;
            long[] javaBuffer = new long[limit + 2];
            long[] nativeBuffer = new long[limit + 2];
            ArrayList<Long> observed = new ArrayList<>();
            int next = 0;
            while (next < count) {
                Arrays.fill(javaBuffer, CANARY);
                Arrays.fill(nativeBuffer, CANARY);
                long a = M3LongIndexPathMasks.scanWindow(rows, offsets, planes,
                        required, forbidden, first, count, stride, next,
                        javaBuffer, 1, limit);
                long b = M3LongIndexPathMasksJNI.scanWindow(rows, offsets, planes,
                        required, forbidden, first, count, stride, next,
                        nativeBuffer, 1, limit);
                eq(a, b);
                eq(javaBuffer, nativeBuffer);
                int advanced = M3LongIndexPathMasks.nextOrdinal(b);
                check(advanced > next, "native cursor must progress");
                for (int i = 0; i < M3LongIndexPathMasks.written(b); i++) {
                    observed.add(nativeBuffer[i + 1]);
                }
                next = advanced;
            }
            eq(javaResult, observed.stream().mapToLong(Long::longValue).toArray());
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(long expected, long actual) {
        check(expected == actual, "expected " + expected + " got " + actual);
    }

    private static void eq(long[] expected, long[] actual) {
        check(Arrays.equals(expected, actual),
                "expected " + Arrays.toString(expected) + " got " + Arrays.toString(actual));
    }

    private static void fail(Class<? extends Throwable> expected, Runnable action) {
        assertions++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong exception " + failure, failure);
        }
        throw new AssertionError("missing " + expected.getName());
    }
}
