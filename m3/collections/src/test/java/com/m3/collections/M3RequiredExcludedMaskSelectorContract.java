/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Modified 2026 by Hitesh Soliwal and contributors: preserve unsigned high-bit rows in the required/excluded boundary oracle.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent oracle for exact required/excluded logical-position selection. */
public final class M3RequiredExcludedMaskSelectorContract {
    private static long checks;

    private M3RequiredExcludedMaskSelectorContract() { }

    public static void main(String[] args) {
        boundary();
        allMasks();
        randomized();
        signalReuse();
        System.out.println("PASS M3RequiredExcludedMaskSelectorContract checks=" + checks);
    }

    private static void boundary() {
        var java = M3IndexMaskCompactor.Backend.JAVA;
        byte[] states = {0, 1, 2, 3, (byte) 0x80, (byte) 0xff};
        // 0x80 is unsigned state 128 and has no excluded low bit, so row 4 is selected.
        eq(new int[] {0, 2, 4}, M3IndexMaskCompactor.selectPositions(states, 0, 1, java));
        eq(new int[] {1}, M3IndexMaskCompactor.selectPositions(states, 1, 2, java));
        eq(new int[] {4}, M3IndexMaskCompactor.selectPositions(states, 128, 1, java));
        eq(new int[0], M3IndexMaskCompactor.selectPositions(states, 1, 1, java));
        eq(new int[] {0, 1, 2, 3, 4, 5},
                M3IndexMaskCompactor.selectPositions(states, 0, 0, java));
        for (int required : new int[] {0, 1, 3, 128, 255}) {
            eq(M3IndexMaskCompactor.selectPositions(states, required, java),
                    M3IndexMaskCompactor.selectPositions(states, required, 0, java));
            yes(M3IndexMaskCompactor.selectedCount(states, required)
                    == M3IndexMaskCompactor.selectedCount(states, required, 0));
        }
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectPositions(states, -1, 0, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectPositions(states, 0, 256, java));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectPositions(null, 0, 0, java));
    }

    private static void allMasks() {
        byte[] states = new byte[64];
        for (int i = 0; i < states.length; i++) {
            states[i] = (byte) ((i * 73 + (i >>> 2)) & 255);
        }
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                int[] expected = oracle(states, required, excluded);
                eq(expected, M3IndexMaskCompactor.selectPositions(
                        states, required, excluded, M3IndexMaskCompactor.Backend.JAVA));
                yes(expected.length
                        == M3IndexMaskCompactor.selectedCount(states, required, excluded));
            }
        }
    }

    private static void randomized() {
        Random random = new Random(0x4d33524551455843L);
        for (int trial = 0; trial < 1024; trial++) {
            int length = random.nextInt(640);
            byte[] states = new byte[length];
            random.nextBytes(states);
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            byte[] copy = states.clone();
            eq(oracle(states, required, excluded),
                    M3IndexMaskCompactor.selectPositions(states, required, excluded,
                            M3IndexMaskCompactor.Backend.JAVA));
            yes(Arrays.equals(copy, states));
        }
    }

    private static void signalReuse() {
        double[][] signals = {{6, 1, 5, 2, 4, 3}, {0, 9, 8, 7, 6, 5}};
        boolean[] descending = {false, true};
        byte[] states = {0, 1, 2, 3, (byte) 0x80, (byte) 0xff};
        int[] expectedRows = M3IndexMaskCompactor.selectPositions(
                states, 0, 1, M3IndexMaskCompactor.Backend.JAVA);
        int[] expected = oracleOrder(signals, descending, expectedRows);
        eq(expected, M3SignalDimensions.filtered(
                signals, descending, states, 0, 1, M3SignalOrder.Backend.JAVA));
        eq(expected, M3SignalDimensions.filtered(
                signals, descending, states, 0, 1, M3SignalOrder.Backend.JAVA,
                M3IndexMaskCompactor.Backend.JAVA));
    }

    private static int[] oracle(byte[] states, int required, int excluded) {
        int[] result = new int[states.length];
        int count = 0;
        for (int i = 0; i < states.length; i++) {
            int bits = states[i] & 0xff;
            if ((bits & required) == required && (bits & excluded) == 0) {
                result[count++] = i;
            }
        }
        return Arrays.copyOf(result, count);
    }

    private static int[] oracleOrder(double[][] signals, boolean[] descending, int[] rows) {
        Integer[] boxed = new Integer[rows.length];
        for (int i = 0; i < rows.length; i++) boxed[i] = rows[i];
        Arrays.sort(boxed, (left, right) -> {
            for (int d = 0; d < signals.length; d++) {
                int cmp = Double.compare(signals[d][left], signals[d][right]);
                if (cmp != 0) return descending[d] ? -cmp : cmp;
            }
            return Integer.compare(indexOf(rows, left), indexOf(rows, right));
        });
        int[] result = new int[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = boxed[i];
        return result;
    }

    private static int indexOf(int[] rows, int value) {
        for (int i = 0; i < rows.length; i++) if (rows[i] == value) return i;
        throw new AssertionError("row absent");
    }

    private static void eq(int[] expected, int[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void yes(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("condition false");
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
