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
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Independent JDK-comparator oracle for full-width mask and stable signal ordering.
 * This is an executable source-only contract; JNI is checked when explicitly available.
 */
public final class M3SignalMask32Contract {
    private static final Random RANDOM = new Random(0x4d334d41534b3332L);
    private static long checks;

    private M3SignalMask32Contract() { }

    public static void main(String[] args) {
        edgeMasksAndStability();
        randomizedOracle();
        refusals();
        if (args.length > 0 && args[0].equals("--require-jni")
                && !M3SignalOrderNative.available()) {
            throw new AssertionError("JNI explicitly required but unavailable");
        }
        System.out.println("PASS M3SignalMask32Contract checks=" + checks
                + " native=" + M3SignalOrderNative.available());
    }

    private static void edgeMasksAndStability() {
        double[][] columns = {
            {3.0, 3.0, -0.0, +0.0, Double.NaN, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, 3.0},
            {7.0, 7.0, 0.0, 0.0, -1.0, 1.0, 2.0, 7.0}
        };
        boolean[] descending = {false, true};
        int[] states = {
            Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE | 2,
            Integer.MIN_VALUE, 0, -1, Integer.MIN_VALUE, Integer.MIN_VALUE
        };
        verify(columns, descending, states, Integer.MIN_VALUE, 2, "high bit");
        verify(columns, descending, states, 0, 0, "all selected");
        verify(columns, descending, states,
                Integer.MIN_VALUE, Integer.MIN_VALUE, "contradictory");
        same(new int[] {0, 1, 3, 6, 7},
                M3SignalDimensions.filtered32(
                        new double[][] {{1, 1, 1, 1, 1, 1, 1, 1}},
                        new boolean[] {false}, states, Integer.MIN_VALUE, 2,
                        M3SignalOrder.Backend.JAVA),
                "stable equal-signal physical order");
        var gap = M3SignalDimensions.compare(
                new double[][] {{3.0, 9.0}}, new boolean[] {false}, 0, 1);
        check(gap.sign() < 0 && gap.numericSeparations()[0] == 6.0,
                "signed order and numeric separation remain distinct");
    }

    private static void randomizedOracle() {
        int[] required = {0, Integer.MIN_VALUE, 1, 0x40000200, -1};
        int[] forbidden = {0, 1, 2, Integer.MIN_VALUE, 0};
        for (int round = 0; round < 160; round++) {
            int n = (round * 43) % 191;
            int dimensions = 1 + round % 5;
            double[][] columns = new double[dimensions][n];
            boolean[] descending = new boolean[dimensions];
            for (int d = 0; d < dimensions; d++) {
                descending[d] = RANDOM.nextBoolean();
                for (int row = 0; row < n; row++) {
                    double value = Double.longBitsToDouble(RANDOM.nextLong());
                    if (row % 11 == 0) value = -0.0;
                    if (row % 13 == 0) value = +0.0;
                    if (row % 17 == 0) value = Double.NaN;
                    if (row % 19 == 0) value = Double.POSITIVE_INFINITY;
                    if (row > 0 && row % 7 == 0) value = columns[d][row - 1];
                    columns[d][row] = value;
                }
            }
            int[] states = new int[n];
            for (int row = 0; row < n; row++) {
                int bits = RANDOM.nextInt();
                if ((row & 3) == 0) bits |= Integer.MIN_VALUE;
                if ((row & 7) == 0) bits = -1;
                states[row] = bits;
            }
            int selector = round % required.length;
            verify(columns, descending, states, required[selector],
                    forbidden[selector], "random-" + round);
        }
    }

    private static void verify(double[][] columns, boolean[] descending,
            int[] states, int required, int excluded, String label) {
        double[][] original = new double[columns.length][];
        for (int d = 0; d < columns.length; d++) original[d] = columns[d].clone();
        int[] originalStates = states.clone();

        int[] expected = oracle(columns, descending, states, required, excluded);
        int[] actual = M3SignalDimensions.filtered32(
                columns, descending, states, required, excluded,
                M3SignalOrder.Backend.JAVA);
        same(expected, actual, label + "/dimensions");

        int[] directExpected = oracle(
                new double[][] {columns[0]},
                new boolean[] {descending[0]}, states, required, excluded);
        M3SignalOrder direct = M3SignalOrder.filtered32(
                columns[0], states, required, excluded, !descending[0],
                M3SignalOrder.Backend.JAVA);
        same(directExpected, direct.rows(), label + "/scalar");
        check(direct.retainedPrimitiveBytes() == 4L * direct.size(),
                label + "/retained primitive row payload");

        if (M3SignalOrderNative.available()) {
            same(expected, M3SignalDimensions.filtered32(
                    columns, descending, states, required, excluded,
                    M3SignalOrder.Backend.NATIVE_REQUIRED),
                    label + "/dimensions JNI");
            same(directExpected, M3SignalOrder.filtered32(
                    columns[0], states, required, excluded, !descending[0],
                    M3SignalOrder.Backend.NATIVE_REQUIRED).rows(),
                    label + "/scalar JNI");
        }

        for (int d = 0; d < columns.length; d++) {
            check(Arrays.equals(original[d], columns[d]), label + "/signals immutable");
        }
        check(Arrays.equals(originalStates, states), label + "/states immutable");
    }

    private static int[] oracle(double[][] columns, boolean[] descending,
            int[] states, int required, int excluded) {
        List<Integer> accepted = new ArrayList<>();
        for (int row = 0; row < states.length; row++) {
            int flags = states[row];
            if ((flags & required) == required && (flags & excluded) == 0) {
                accepted.add(row);
            }
        }
        accepted.sort((a, b) -> {
            for (int d = 0; d < columns.length; d++) {
                int order = descending[d]
                        ? Double.compare(columns[d][b], columns[d][a])
                        : Double.compare(columns[d][a], columns[d][b]);
                if (order != 0) return order;
            }
            return Integer.compare(a, b);
        });
        return accepted.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void refusals() {
        var scalar = new double[] {0.0, 1.0};
        var dims = new double[][] {scalar};
        var direction = new boolean[] {false};
        refuses(NullPointerException.class, () -> M3SignalOrder.filtered32(
                scalar, null, 0, 0, true, M3SignalOrder.Backend.JAVA));
        refuses(IllegalArgumentException.class, () -> M3SignalOrder.filtered32(
                scalar, new int[1], 0, 0, true, M3SignalOrder.Backend.JAVA));
        refuses(NullPointerException.class, () -> M3SignalDimensions.filtered32(
                dims, direction, null, 0, 0, M3SignalOrder.Backend.JAVA));
        refuses(IllegalArgumentException.class, () -> M3SignalDimensions.filtered32(
                dims, direction, new int[1], 0, 0, M3SignalOrder.Backend.JAVA));
        refuses(NullPointerException.class, () -> M3SignalOrder.filtered32(
                scalar, new int[2], 0, 0, true, null));
        if (!M3SignalOrderNative.available()) {
            refuses(UnsatisfiedLinkError.class, () -> M3SignalDimensions.filtered32(
                    dims, direction, new int[2], 0, 0,
                    M3SignalOrder.Backend.NATIVE_REQUIRED));
            refuses(UnsatisfiedLinkError.class, () -> M3SignalOrder.filtered32(
                    scalar, new int[2], 0, 0, true,
                    M3SignalOrder.Backend.NATIVE_REQUIRED));
        }
        // Existing byte-state API must retain its strict 8-bit mask validation.
        refuses(IllegalArgumentException.class, () -> M3SignalOrder.filtered(
                scalar, new byte[2], Integer.MIN_VALUE, 0, true,
                M3SignalOrder.Backend.JAVA));
    }

    private static void refuses(Class<? extends Throwable> expected,
            Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(expected.isInstance(failure), "refusal " + expected + ": " + failure);
            return;
        }
        throw new AssertionError("did not refuse " + expected);
    }

    private static void same(int[] expected, int[] actual, String message) {
        check(Arrays.equals(expected, actual), message + ": "
                + Arrays.toString(expected) + " vs " + Arrays.toString(actual));
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }
}
