/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Exact, deterministic Java 21 + opt-in real JNI index-path contract oracle. */
public final class M3IndexPathMasksSelfTest {
    private static long checks;
    private M3IndexPathMasksSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (nativeRun && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("required JNI library did not load");
        }
        boundaries(nativeRun);
        randomPaths(nativeRun);
        System.out.println("PASS M3IndexPathMasksSelfTest checks=" + checks + " JNI=" + nativeRun);
    }

    private static void boundaries(boolean nativeRun) {
        int[] rows = {9, -20, 9, 3, Integer.MIN_VALUE, Integer.MAX_VALUE};
        int[] locations = {3, 0, 3, 1, 2, 0};
        int[][] planes = {{0x80000001, 0x03, 0x02, 0x80000003},
                {0x00, 0x08, 0x04, 0x08}};
        int[] need = {0x02, 0x08};
        int[] deny = {0x04, 0x01};
        eq(new int[] {9, 9, 3}, M3IndexPathMasks.select(rows, locations, planes,
                need, deny, 0, 6, 1, M3IndexPathMasks.Backend.JAVA));
        eq(new int[] {3, 9, 9}, M3IndexPathMasks.select(rows, locations, planes,
                need, deny, 3, 4, -1, M3IndexPathMasks.Backend.JAVA));
        eq(new int[] {9, 9}, M3IndexPathMasks.select(rows, locations, planes,
                need, deny, 0, 3, 2, M3IndexPathMasks.Backend.JAVA));
        eq(0, M3IndexPathMasks.firstMatchingIndex(rows, locations, planes,
                need, deny, 0, 6, 1));
        eq(-1, M3IndexPathMasks.firstMatchingIndex(rows, locations, planes,
                new int[] {4, 0}, new int[] {0, 0}, 0, 6, 1));
        eq(new int[0], M3IndexPathMasks.select(rows, locations, planes,
                need, deny, rows.length, 0, 1, M3IndexPathMasks.Backend.JAVA));
        int[] originals = rows.clone();
        M3IndexPathMasks.select(rows, locations, planes, need, deny,
                0, 6, 1, M3IndexPathMasks.Backend.JAVA)[0] = 200;
        eq(originals, rows);
        if (nativeRun) {
            eq(new int[] {9, 9, 3}, M3IndexPathMasks.select(rows, locations, planes,
                    need, deny, 0, 6, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            eq(new int[] {3, 9, 9}, M3IndexPathMasks.select(rows, locations, planes,
                    need, deny, 3, 4, -1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
        fails(NullPointerException.class, () -> M3IndexPathMasks.select(null, locations,
                planes, need, deny, 0, 1, 1, M3IndexPathMasks.Backend.JAVA));
        fails(NullPointerException.class, () -> M3IndexPathMasks.select(rows, locations,
                new int[][] {null}, new int[] {0}, new int[] {0}, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(NullPointerException.class, () -> M3IndexPathMasks.select(rows, locations,
                planes, need, deny, 0, 1, 1, null));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.select(rows,
                new int[] {0}, planes, need, deny, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.select(rows,
                locations, new int[0][], new int[0], new int[0], 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.select(rows,
                locations, planes, new int[] {1}, deny, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                locations, planes, need, deny, 0, 1, 0,
                M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                locations, planes, need, deny, 1, Integer.MAX_VALUE, Integer.MAX_VALUE,
                M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                locations, planes, need, deny, 0, 2, -1,
                M3IndexPathMasks.Backend.JAVA));
        int[] invalid = locations.clone();
        invalid[4] = Integer.MAX_VALUE;
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                invalid, planes, need, deny, 0, 6, 1,
                M3IndexPathMasks.Backend.JAVA));
        // Precomputed integer masks use all 32 bits (including the sign bit).
        eq(new int[] {9}, M3IndexPathMasks.select(rows, locations, planes,
                new int[] {Integer.MIN_VALUE, 0}, new int[] {4, 0},
                0, 1, 1, M3IndexPathMasks.Backend.JAVA));
    }

    private static void randomPaths(boolean nativeRun) {
        Random random = new Random(0x4d33504154484d31L);
        for (int trial = 0; trial < 768; trial++) {
            int n = random.nextInt(130);
            int capacity = random.nextInt(180) + 1;
            int stages = random.nextInt(5) + 1;
            int[] rows = new int[n];
            int[] offsets = new int[n];
            int[][] planes = new int[stages][];
            int[] required = new int[stages];
            int[] forbidden = new int[stages];
            for (int s = 0; s < stages; s++) {
                planes[s] = new int[capacity + random.nextInt(12)];
                for (int i = 0; i < planes[s].length; i++) planes[s][i] = random.nextInt();
                required[s] = random.nextInt();
                forbidden[s] = random.nextInt();
                if ((trial & 3) == 0) { // Low-selectivity / all-pass mask cases too.
                    required[s] = 0;
                    forbidden[s] = 0;
                }
                if ((trial & 3) == 1) { // Independent bit constraints.
                    required[s] &= 0x55555555;
                    forbidden[s] &= 0xaaaaaaaa;
                }
            }
            for (int i = 0; i < n; i++) {
                rows[i] = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> 0;
                    default -> random.nextInt(9) - 4;
                };
                offsets[i] = random.nextInt(capacity);
            }
            int first;
            int count;
            int stride;
            if (n == 0 || (trial % 11) == 0) {
                first = n;
                count = 0;
                stride = -1;
            } else {
                first = random.nextInt(n);
                stride = (random.nextBoolean() ? 1 : -1) * (random.nextInt(4) + 1);
                count = 1 + random.nextInt(stride > 0
                        ? (n - 1 - first) / stride + 1
                        : first / -stride + 1);
            }
            int[] expected = oracle(rows, offsets, planes, required, forbidden,
                    first, count, stride);
            eq(expected, M3IndexPathMasks.select(rows, offsets, planes, required,
                    forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA));
            if (nativeRun) {
                eq(expected, M3IndexPathMasks.select(rows, offsets, planes, required,
                        forbidden, first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            }
            int expectedFirst = -1;
            if (expected.length > 0) {
                for (int i = 0; i < count; i++) {
                    int candidate = first + i * stride;
                    if (passes(planes, required, forbidden, offsets[candidate])) {
                        expectedFirst = candidate;
                        break;
                    }
                }
            }
            eq(expectedFirst, M3IndexPathMasks.firstMatchingIndex(rows, offsets,
                    planes, required, forbidden, first, count, stride));
        }
    }

    private static boolean passes(int[][] planes, int[] required, int[] forbidden, int physical) {
        for (int i = 0; i < planes.length; i++) {
            int state = planes[i][physical];
            if ((state & required[i]) != required[i]) return false;
            if ((state & forbidden[i]) != 0) return false;
        }
        return true;
    }

    private static int[] oracle(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        ArrayList<Integer> found = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = first + i * stride;
            if (passes(planes, required, forbidden, offsets[index])) found.add(rows[index]);
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void eq(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError("expected " + expected + " got " + actual);
    }

    private static void eq(int[] expected, int[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " got " + Arrays.toString(actual));
        }
    }

    private static void fails(Class<? extends Throwable> expected, Runnable code) {
        checks++;
        try { code.run(); }
        catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("expected " + expected.getName(), t);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
