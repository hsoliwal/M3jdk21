// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.List;

/** Dependency-free JDK21 executable contract oracle for the CPU mask-runs atom. */
public final class M3CpuMaskRunsSelfTest {
    private M3CpuMaskRunsSelfTest() { }

    public static void main(String[] args) {
        basic();
        multiWindow();
        budgetsAndValidation();
        System.out.println("M3_CPU_MASK_RUNS_PASS");
    }

    private static void basic() {
        int[] rows = {7, 7, 3, 3, 3, -5, -5, 11};
        int[] offsets = {0, 1, 2, 3, 4, 5, 6, 7};
        int[][] planes = {{1, 1, 0, 1, 1, 1, 1, 1}};
        List<String> runs = new ArrayList<>();
        int count = M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1},
                new int[]{0}, 0, rows.length, 1, 20,
                M3IndexPathMasks.Backend.JAVA,
                (row, len) -> runs.add(row + ":" + len));
        require(count == 4 && runs.equals(List.of("7:2", "3:2", "-5:2", "11:1")),
                "forward filter and run grouping");
        runs.clear();
        count = M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1},
                new int[]{0}, 7, 8, -1, 20,
                M3IndexPathMasks.Backend.JAVA,
                (row, len) -> runs.add(row + ":" + len));
        require(count == 4 && runs.equals(List.of("11:1", "-5:2", "3:2", "7:2")),
                "negative stride and groups");
    }

    private static void multiWindow() {
        int n = 10000;
        int[] rows = new int[n];
        int[] offsets = new int[n];
        int[] state = new int[n];
        for (int i = 0; i < n; i++) {
            rows[i] = i < 5000 ? 41 : 42;
            offsets[i] = i;
            state[i] = 0x80000000;
        }
        List<String> runs = new ArrayList<>();
        int count = M3CpuMaskRuns.scan(rows, offsets, new int[][]{state},
                new int[]{0x80000000}, new int[]{0},
                0, n, 1, 4, M3IndexPathMasks.Backend.JAVA,
                (row, len) -> runs.add(row + ":" + len));
        require(count == 2 && runs.equals(List.of("41:5000", "42:5000")),
                "run must cross output windows");
        runs.clear();
        count = M3CpuMaskRuns.scan(rows, offsets, new int[][]{state},
                new int[]{0x80000000}, new int[]{0},
                0, n, 1, 1, M3IndexPathMasks.Backend.JAVA,
                (row, len) -> runs.add(row + ":" + len));
        require(count == 1 && runs.equals(List.of("41:5000")),
                "bounded output without second run");
    }

    private static void budgetsAndValidation() {
        int[] rows = {1, 2};
        int[] offsets = {0, 1};
        int[][] planes = {{1, 1}};
        int count = M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1},
                new int[]{0}, 0, 2, 1, 0,
                M3IndexPathMasks.Backend.JAVA,
                (row, len) -> { throw new AssertionError("zero run budget"); });
        require(count == 0, "zero output budget");
        expect(IllegalArgumentException.class, () ->
                M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1}, new int[]{0},
                        0, 2, 1, -1, M3IndexPathMasks.Backend.JAVA,
                        (row, len) -> { }));
        expect(IndexOutOfBoundsException.class, () ->
                M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1}, new int[]{0},
                        1, 2, 1, 0, M3IndexPathMasks.Backend.JAVA,
                        (row, len) -> { }));
        if (!M3IndexPathMasks.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class, () ->
                    M3CpuMaskRuns.scan(rows, offsets, planes, new int[]{1}, new int[]{0},
                            0, 2, 1, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED,
                            (row, len) -> { }));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable caught) {
            if (type.isInstance(caught)) return;
            throw new AssertionError("wrong exception " + caught, caught);
        }
        throw new AssertionError("missing expected " + type.getSimpleName());
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
