/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;

/** Exact-order oracle for bounded, filter-composed, Java/JNI sorted-index windows. */
public final class M3IndexMaskWindowSelfTest {
    private static int checks;
    private static final int CANARY = 0x12345678;

    private M3IndexMaskWindowSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !nativeRun)) {
            throw new IllegalArgumentException("use no arguments or --native");
        }
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("actual m3_collections_mask JNI library required");
        }
        boundaryChecks(nativeRun);
        geometryAndAliasRefusal(nativeRun);
        randomizedDifferential(nativeRun);
        deepSparseWindow(nativeRun);
        snapshotVsLiveBoundary(nativeRun);
        System.out.println("PASS M3_INDEX_MASK_WINDOW checks=" + checks + " native=" + nativeRun);
    }

    private static void boundaryChecks(boolean nativeRun) {
        int[] rows = {31, -2, 31, Integer.MIN_VALUE, Integer.MAX_VALUE};
        byte[] states = {(byte) 0xff, 0x01, 0x03, 0x00, (byte) 0x80};
        assertWindows(rows, states, 0x01, 0x02, 2, nativeRun);
        assertWindows(rows, states, 0xff, 0x00, 1, nativeRun);
        assertWindows(rows, states, 0x00, 0x00, 1, nativeRun);
        assertWindows(rows, states, 0x01, 0x01, 3, nativeRun);
        assertWindows(new int[0], new byte[0], 0, 0, 1, nativeRun);
        int[] output = {CANARY, CANARY, CANARY};
        for (M3IndexMaskCompactor.Backend backend : backends(nativeRun)) {
            long zero = M3IndexMaskWindow.scan(rows, states, 1, 0, 0, output, 1, 0, backend);
            eq(M3IndexMaskWindow.nextIndex(zero), 1);
            eq(M3IndexMaskWindow.written(zero), 0);
            eq(output, CANARY, CANARY, CANARY);
            long empty = M3IndexMaskWindow.scan(rows, states, rows.length, 0, 0, output, 1, 1, backend);
            eq(M3IndexMaskWindow.nextIndex(empty), rows.length);
            eq(M3IndexMaskWindow.written(empty), 0);
            eq(output, CANARY, CANARY, CANARY);
        }
    }

    private static void geometryAndAliasRefusal(boolean nativeRun) {
        int[] rows = {4, 7, 6};
        byte[] states = {1, 2, 3};
        int[] output = {CANARY, CANARY, CANARY, CANARY};
        for (M3IndexMaskCompactor.Backend backend : backends(nativeRun)) {
            rejects(NullPointerException.class, () -> scan(null, states, 0, 0, 0, output, 0, 1, backend));
            rejects(NullPointerException.class, () -> scan(rows, null, 0, 0, 0, output, 0, 1, backend));
            rejects(NullPointerException.class, () -> scan(rows, states, 0, 0, 0, null, 0, 1, backend));
            rejects(NullPointerException.class, () -> scan(rows, states, 0, 0, 0, output, 0, 1, null));
            rejects(IllegalArgumentException.class,
                    () -> scan(rows, new byte[] {1}, 0, 0, 0, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, -1, 0, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 256, 0, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 0, -1, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 0, 256, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 0, 0, rows, 0, 1, backend));
            rejects(IndexOutOfBoundsException.class, () -> scan(rows, states, -1, 0, 0, output, 0, 1, backend));
            rejects(IndexOutOfBoundsException.class, () -> scan(rows, states, 4, 0, 0, output, 0, 1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 0, 0, output, 0, -1, backend));
            rejects(IllegalArgumentException.class, () -> scan(rows, states, 0, 0, 0, output, 0, 4097, backend));
            rejects(IndexOutOfBoundsException.class, () -> scan(rows, states, 0, 0, 0, output, -1, 1, backend));
            rejects(IndexOutOfBoundsException.class, () -> scan(rows, states, 0, 0, 0, output, 4, 1, backend));
            rejects(IndexOutOfBoundsException.class, () -> scan(rows, states, 0, 0, 0, output, 3, 2, backend));
            eq(output, CANARY, CANARY, CANARY, CANARY);
            eq(rows, 4, 7, 6);
        }
    }

    private static void randomizedDifferential(boolean nativeRun) {
        Random rng = new Random(0x4D334A444B57494EL);
        for (int trial = 0; trial < 2048; trial++) {
            Comparator<Integer> comparator = (trial & 1) == 0 ? Comparator.naturalOrder()
                    : Comparator.reverseOrder();
            TreeSet<Integer> sorted = new TreeSet<>(comparator);
            for (int i = 0; i < trial % 224; i++) sorted.add(rng.nextInt(2000) - 1000);
            List<Integer> rowList = new ArrayList<>(sorted);
            if (trial % 7 == 0 && !rowList.isEmpty()) {
                rowList.add(rowList.get(rowList.size() - 1));
            }
            int[] rows = new int[rowList.size()];
            byte[] states = new byte[rows.length];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = rowList.get(i);
                states[i] = (byte) rng.nextInt(256);
            }
            int required = rng.nextInt(256);
            int excluded = rng.nextInt(256);
            int window = 1 + rng.nextInt(12);
            assertWindows(rows, states, required, excluded, window, nativeRun);
        }
    }

    private static void deepSparseWindow(boolean nativeRun) {
        int n = 40_000;
        int[] rows = new int[n];
        byte[] states = new byte[n];
        for (int i = 0; i < n; i++) {
            rows[i] = n - 1 - i;
            states[i] = (byte) (i % 991 == 0 ? 255 : 0);
        }
        assertWindows(rows, states, 255, 0, 7, nativeRun);
        assertWindows(rows, states, 0, 255, 4096, nativeRun);
    }

    private static void snapshotVsLiveBoundary(boolean nativeRun) {
        int[] source = {10, 20, 30};
        byte[] states = {1, 1, 1};
        int[] selected = new int[3];
        for (M3IndexMaskCompactor.Backend backend : backends(nativeRun)) {
            Arrays.fill(selected, CANARY);
            long first = scan(source, states, 0, 1, 0, selected, 0, 2, backend);
            eq(M3IndexMaskWindow.nextIndex(first), 2);
            eq(selected[0], 10);
            eq(selected[1], 20);
            // Caller changes authoritative state only between scans; existing output is detached.
            states[2] = 0;
            long next = scan(source, states, M3IndexMaskWindow.nextIndex(first),
                    1, 0, selected, 2, 1, backend);
            eq(M3IndexMaskWindow.nextIndex(next), 3);
            eq(M3IndexMaskWindow.written(next), 0);
            eq(selected, 10, 20, CANARY);
            states[2] = 1;
        }
    }

    private static void assertWindows(int[] rows, byte[] states, int required, int excluded,
            int window, boolean nativeRun) {
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            int bits = states[i] & 0xff;
            if ((bits & required) == required && (bits & excluded) == 0) {
                expected.add(rows[i]);
            }
        }
        for (M3IndexMaskCompactor.Backend backend : backends(nativeRun)) {
            List<Integer> actual = new ArrayList<>();
            int cursor = 0;
            int cycles = 0;
            while (cursor < rows.length) {
                int[] output = new int[window + 2];
                Arrays.fill(output, CANARY);
                long result = scan(rows, states, cursor, required, excluded,
                        output, 1, window, backend);
                int next = M3IndexMaskWindow.nextIndex(result);
                int written = M3IndexMaskWindow.written(result);
                check(next > cursor && next <= rows.length);
                check(written >= 0 && written <= window);
                check(written <= next - cursor);
                eq(output[0], CANARY);
                eq(output[output.length - 1], CANARY);
                for (int i = 0; i < written; i++) actual.add(output[1 + i]);
                cursor = next;
                cycles++;
                check(cycles <= rows.length + 1);
            }
            check(actual.equals(expected));
        }
    }

    private static M3IndexMaskCompactor.Backend[] backends(boolean nativeRun) {
        return nativeRun ? M3IndexMaskCompactor.Backend.values()
                : new M3IndexMaskCompactor.Backend[] {M3IndexMaskCompactor.Backend.JAVA};
    }

    private static long scan(int[] rows, byte[] states, int first, int required,
            int excluded, int[] output, int offset, int limit,
            M3IndexMaskCompactor.Backend backend) {
        return M3IndexMaskWindow.scan(rows, states, first, required, excluded,
                output, offset, limit, backend);
    }

    private static void eq(int actual, int expected) {
        checks++;
        if (actual != expected) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }

    private static void eq(int[] actual, int... expected) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError("expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void check(boolean accepted) {
        checks++;
        if (!accepted) throw new AssertionError("contract violation");
    }

    private static void rejects(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try {
            task.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected refusal", failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
