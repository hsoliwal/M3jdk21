/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Independent whole-path Java 21 and real JNI differential oracle for 64-bit row IDs. */
public final class M3IndexPathMasksLongSelfTest {
    private static long checks;
    private M3IndexPathMasksLongSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !nativeRun)) {
            throw new IllegalArgumentException("expected --native or no argument");
        }
        if (nativeRun && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("JNI library required for long-row parity");
        }
        boundaries(nativeRun);
        randomized(nativeRun);
        System.out.println("PASS M3IndexPathMasksLongSelfTest checks=" + checks + " JNI=" + nativeRun);
    }

    private static void boundaries(boolean nativeRun) {
        long[] ids = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, Long.MIN_VALUE, 42};
        int[] positions = {3, 0, 3, 1, 2, 0};
        int[][] planes = {{0x80000001, 3, 2, 0x80000003}, {0, 8, 4, 8}};
        int[] required = {2, 8};
        int[] forbidden = {4, 1};
        eq(new long[] {Long.MIN_VALUE, -1, 0}, select(ids, positions, planes,
                required, forbidden, 0, 6, 1, false));
        eq(new long[] {0, -1, Long.MIN_VALUE}, select(ids, positions, planes,
                required, forbidden, 3, 4, -1, false));
        eq(new long[] {Long.MIN_VALUE, -1}, select(ids, positions, planes,
                required, forbidden, 0, 3, 2, false));
        eq(new long[0], select(ids, positions, planes,
                required, forbidden, ids.length, 0, -1, false));
        eq(new long[] {Long.MIN_VALUE}, select(ids, positions, planes,
                new int[] {Integer.MIN_VALUE, 0}, new int[] {4, 0}, 0, 1, 1, false));
        if (nativeRun) {
            eq(new long[] {Long.MIN_VALUE, -1, 0}, select(ids, positions, planes,
                    required, forbidden, 0, 6, 1, true));
            eq(new long[] {0, -1, Long.MIN_VALUE}, select(ids, positions, planes,
                    required, forbidden, 3, 4, -1, true));
        }
        long[] immutableSource = ids.clone();
        long[] detached = select(ids, positions, planes, required, forbidden, 0, 6, 1, nativeRun);
        detached[0] = 1;
        eq(immutableSource, ids);
        fails(NullPointerException.class, () -> M3IndexPathMasks.selectLong(
                null, positions, planes, required, forbidden, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(NullPointerException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, planes, required, forbidden, 0, 1, 1, null));
        fails(NullPointerException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, new int[][] {null}, new int[] {0}, new int[] {0},
                0, 1, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.selectLong(
                ids, new int[0], planes, required, forbidden, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, new int[0][], new int[0], new int[0], 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, planes, new int[] {0}, forbidden, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, planes, required, forbidden, 0, 1, 0,
                M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, planes, required, forbidden, 1, Integer.MAX_VALUE,
                Integer.MAX_VALUE, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.selectLong(
                ids, positions, planes, required, forbidden, 0, 2, -1,
                M3IndexPathMasks.Backend.JAVA));
        int[] invalid = positions.clone();
        invalid[4] = Integer.MAX_VALUE;
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.selectLong(
                ids, invalid, planes, required, forbidden, 0, 6, 1,
                M3IndexPathMasks.Backend.JAVA));
    }

    private static void randomized(boolean nativeRun) {
        Random random = new Random(0x4d33504c4f4e4731L);
        for (int trial = 0; trial < 3_072; trial++) {
            int n = random.nextInt(122);
            int capacity = random.nextInt(140) + 1;
            int stages = 1 + random.nextInt(6);
            long[] rows = new long[n];
            int[] positions = new int[n];
            int[][] planes = new int[stages][];
            int[] required = new int[stages];
            int[] forbidden = new int[stages];
            for (int s = 0; s < stages; s++) {
                planes[s] = new int[capacity + random.nextInt(11)];
                for (int p = 0; p < planes[s].length; p++) planes[s][p] = random.nextInt();
                required[s] = random.nextInt();
                forbidden[s] = random.nextInt();
                if ((trial & 7) == 0) { required[s] = 0; forbidden[s] = 0; }
                if ((trial & 7) == 1) {
                    required[s] &= 0x55555555;
                    forbidden[s] &= 0xaaaaaaaa;
                }
            }
            for (int i = 0; i < n; i++) {
                rows[i] = switch (i % 7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0;
                    case 3 -> -1;
                    default -> random.nextLong();
                };
                positions[i] = random.nextInt(capacity);
            }
            int first, count, stride;
            if (n == 0 || trial % 19 == 0) {
                first = n;
                count = 0;
                stride = -1;
            } else {
                first = random.nextInt(n);
                stride = (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(5));
                int maxCount = stride > 0 ? (n - 1 - first) / stride + 1
                        : first / -stride + 1;
                count = 1 + random.nextInt(maxCount);
            }
            long[] expected = oracle(rows, positions, planes, required, forbidden,
                    first, count, stride);
            eq(expected, select(rows, positions, planes, required,
                    forbidden, first, count, stride, false));
            if (nativeRun) {
                eq(expected, select(rows, positions, planes, required,
                        forbidden, first, count, stride, true));
            }
        }
    }

    private static long[] oracle(long[] rows, int[] positions, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        ArrayList<Long> selected = new ArrayList<>();
        long at = first;
        for (int ordinal = 0; ordinal < count; ordinal++, at += stride) {
            int physical = positions[(int)at];
            boolean pass = true;
            for (int s = 0; s < planes.length; s++) {
                int state = planes[s][physical];
                if ((state & required[s]) != required[s] || (state & forbidden[s]) != 0) {
                    pass = false;
                    break;
                }
            }
            if (pass) selected.add(rows[(int)at]);
        }
        long[] output = new long[selected.size()];
        for (int i = 0; i < output.length; i++) output[i] = selected.get(i);
        return output;
    }

    private static long[] select(long[] rows, int[] positions, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride,
            boolean nativeRun) {
        return M3IndexPathMasks.selectLong(rows, positions, planes, required,
                forbidden, first, count, stride,
                nativeRun ? M3IndexPathMasks.Backend.NATIVE_REQUIRED
                        : M3IndexPathMasks.Backend.JAVA);
    }

    private static void eq(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " got " + Arrays.toString(actual));
        }
    }

    private static void fails(Class<? extends Throwable> type, Runnable work) {
        checks++;
        try { work.run(); }
        catch (Throwable exception) {
            if (type.isInstance(exception)) return;
            throw new AssertionError("expected " + type.getName(), exception);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
