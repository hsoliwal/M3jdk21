/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Dependency-free Java 21 differential oracle for bounded M3 mask windows. */
public final class M3CpuMaskWindowStatsSelfTest {
    private M3CpuMaskWindowStatsSelfTest() { }

    public static void main(String[] args) {
        exactCases();
        exhaustiveMasksAndWindows();
        largerThanScratch();
        failClosed();
        System.out.println("M3_CPU_MASK_WINDOW_STATS_PASS");
    }

    private static void exactCases() {
        int[] ids = {7, 7, 3, 0, -5, Integer.MAX_VALUE, Integer.MIN_VALUE};
        int[] offsets = {0, 1, 2, 3, 4, 5, 6};
        int[][] masks = {{Integer.MIN_VALUE, Integer.MIN_VALUE, 0,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE}};
        List<String> seen = new ArrayList<>();
        int count = M3CpuMaskWindowStats.scan(ids, offsets, masks,
                new int[]{Integer.MIN_VALUE}, new int[]{0}, 0, 7, 1,
                2, 10, M3IndexPathMasks.Backend.JAVA,
                (at, n, sum, min, max) -> seen.add(at + ":" + n + ":" + sum
                        + ":" + min + ":" + max));
        require(count == 3 && seen.equals(List.of(
                "0:2:14:7:7", "2:2:-5:-5:0",
                "4:2:-1:-2147483648:2147483647")), "signed state and sums");
        seen.clear();
        count = M3CpuMaskWindowStats.scan(ids, offsets, masks,
                new int[]{Integer.MIN_VALUE}, new int[]{0}, 6, 7, -1,
                3, 10, M3IndexPathMasks.Backend.JAVA,
                (at, n, sum, min, max) -> seen.add(at + ":" + n + ":" + sum
                        + ":" + min + ":" + max));
        require(count == 2 && seen.equals(List.of(
                "0:3:-6:-2147483648:2147483647",
                "3:3:14:0:7")), "reverse stride / partial");
    }

    private static void exhaustiveMasksAndWindows() {
        Random random = new Random(0x4d334f50L);
        for (int trial = 0; trial < 900; trial++) {
            int n = random.nextInt(45);
            int[] ids = new int[n], offsets = new int[n], state = new int[n];
            for (int i = 0; i < n; i++) {
                ids[i] = random.nextInt(20) - 10;
                offsets[i] = n - i - 1;
                state[i] = random.nextInt(4);
            }
            int required = random.nextInt(4), forbidden = random.nextInt(4);
            int stride = random.nextBoolean() ? 1 : -1;
            int first = stride > 0 || n == 0 ? 0 : n - 1;
            int width = 1 + random.nextInt(12), budget = random.nextInt(9);
            List<Integer> accepted = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int at = first + i * stride;
                if ((state[offsets[at]] & required) == required
                        && (state[offsets[at]] & forbidden) == 0) accepted.add(ids[at]);
            }
            List<String> oracle = new ArrayList<>();
            for (int i = 0; i < accepted.size() && oracle.size() < budget; i += width) {
                long sum = 0;
                int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
                int end = Math.min(accepted.size(), i + width);
                for (int j = i; j < end; j++) {
                    int x = accepted.get(j);
                    sum += x;
                    low = Math.min(low, x);
                    high = Math.max(high, x);
                }
                oracle.add(i + ":" + (end - i) + ":" + sum + ":" + low + ":" + high);
            }
            List<String> observed = new ArrayList<>();
            int windows = M3CpuMaskWindowStats.scan(ids, offsets, new int[][]{state},
                    new int[]{required}, new int[]{forbidden}, first, n, stride,
                    width, budget, M3IndexPathMasks.Backend.JAVA,
                    (at, count, sum, low, high) -> observed.add(
                            at + ":" + count + ":" + sum + ":" + low + ":" + high));
            require(windows == oracle.size() && observed.equals(oracle),
                    "differential trial=" + trial + " expected=" + oracle + " got=" + observed);
        }
    }

    private static void largerThanScratch() {
        int n = 10000;
        int[] ids = new int[n], slots = new int[n], masks = new int[n];
        for (int i = 0; i < n; i++) { ids[i] = 2; slots[i] = i; masks[i] = 1; }
        List<String> groups = new ArrayList<>();
        int count = M3CpuMaskWindowStats.scan(ids, slots, new int[][]{masks},
                new int[]{1}, new int[]{0}, 0, n, 1,
                3000, 10, M3IndexPathMasks.Backend.JAVA,
                (at, num, sum, min, max) -> groups.add(at + ":" + num + ":" + sum));
        require(count == 4 && groups.equals(List.of(
                "0:3000:6000", "3000:3000:6000", "6000:3000:6000",
                "9000:1000:2000")), "cross-4096 batches, partial tail");
    }

    private static void failClosed() {
        int[] ids = {1, 2}, slots = {0, 1};
        int[][] states = {{1, 1}};
        expect(IllegalArgumentException.class, () ->
                M3CpuMaskWindowStats.scan(ids, slots, states, new int[]{1}, new int[]{0},
                        0, 2, 1, 0, 1, M3IndexPathMasks.Backend.JAVA,
                        (a,b,c,d,e) -> { }));
        expect(IllegalArgumentException.class, () ->
                M3CpuMaskWindowStats.scan(ids, slots, states, new int[]{1}, new int[]{0},
                        0, 2, 1, 1, -1, M3IndexPathMasks.Backend.JAVA,
                        (a,b,c,d,e) -> { }));
        expect(IndexOutOfBoundsException.class, () ->
                M3CpuMaskWindowStats.scan(ids, slots, states, new int[]{1}, new int[]{0},
                        1, 2, 1, 1, 0, M3IndexPathMasks.Backend.JAVA,
                        (a,b,c,d,e) -> { }));
        if (!M3IndexPathMasks.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () ->
                    M3CpuMaskWindowStats.scan(ids, slots, states, new int[]{1}, new int[]{0},
                            0, 2, 1, 1, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED,
                            (a,b,c,d,e) -> { }));
        }
        List<Integer> seen = new ArrayList<>();
        int accepted = M3CpuMaskWindowStats.scan(ids, slots, states,
                new int[]{1}, new int[]{0}, 0, 2, 1, 1, 1,
                M3IndexPathMasks.Backend.JAVA,
                (a, b, c, d, e) -> seen.add((int) a));
        require(accepted == 1 && seen.equals(List.of(0)), "early output budget");
    }

    private static void expect(Class<? extends Throwable> expected, Runnable work) {
        try { work.run(); }
        catch (Throwable got) {
            if (expected.isInstance(got)) return;
            throw new AssertionError("wrong exception " + got, got);
        }
        throw new AssertionError("missing expected exception " + expected.getSimpleName());
    }

    private static void require(boolean passes, String contract) {
        if (!passes) throw new AssertionError(contract);
    }
}
