// Modified 2026 by Hitesh Soliwal and contributors: qualify loaded and missing JNI independently.
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** Deterministic comparison-oracle differential proof; independent of production radix passes. */
public final class M3SignalDimensionsContract {
    private static long checks;
    private static final Random RANDOM = new Random(0x4d33434f4d504f55L);
    private M3SignalDimensionsContract() { }

    public static void main(String[] args) {
        edgeCases();
        projectionOnceAndFilteredFirst();
        randomizedParity();
        comparisonEvidence();
        refusals();
        System.out.println("PASS M3SignalDimensions checks=" + checks);
    }

    private static void edgeCases() {
        double nan2 = Double.longBitsToDouble(0x7ff8000000000123L);
        double[][] dimensions = {
            {+0.0, -0.0, Double.NaN, nan2, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, 1.0, 1.0, 1.0},
            {2, 1, 4, 3, 1, 2, -9, -9, 10}
        };
        boolean[] descending = {false, true};
        same(oracle(dimensions, descending, null, 0, 0),
                M3SignalDimensions.order(dimensions, descending), "IEEE compound");
        byte[] states = {1, 2, 1, 2, 1, 2, 1, 1, 1};
        same(oracle(dimensions, descending, states, 1, 2),
                M3SignalDimensions.filtered(dimensions, descending, states, 1, 2,
                        M3SignalOrder.Backend.JAVA), "masked IEEE compound");
        same(new int[0], M3SignalDimensions.order(
                new double[][] {new double[0]}, new boolean[] {false}), "empty");
        same(new int[] {0}, M3SignalDimensions.order(
                new double[][] {{42}}, new boolean[] {true}), "singleton");
        same(new int[] {1, 2, 0}, M3SignalDimensions.order(
                new double[][] {{10, 10, 10}, {5, 1, 3}},
                new boolean[] {false, false}), "first signal ties refine");
    }

    private static void projectionOnceAndFilteredFirst() {
        final class Person {
            final int id;
            final double nameSignal;
            final double organizationSignal;
            Person(int id, double nameSignal, double organizationSignal) {
                this.id = id;
                this.nameSignal = nameSignal;
                this.organizationSignal = organizationSignal;
            }
        }
        Person[] people = {
            new Person(0, 4, 0), new Person(1, -9, 9), new Person(2, 2, 1),
            new Person(3, 2, 5), new Person(4, 2, 5)
        };
        byte[] states = {1, 0, 1, 1, 1};
        int[][] calls = new int[2][people.length];
        List<ToDoubleFunction<? super Person>> projections = List.of(
                person -> {calls[0][person.id]++; return person.nameSignal;},
                person -> {calls[1][person.id]++; return person.organizationSignal;});
        int[] rows = M3SignalDimensions.project(people.length, row -> people[row],
                projections, new boolean[] {false, true}, states, 1, 0,
                M3SignalOrder.Backend.JAVA);
        same(new int[] {3, 4, 2, 0}, rows, "source-time signal columns");
        for (int i = 0; i < people.length; i++) {
            for (int d = 0; d < 2; d++) {
                yes(calls[d][i] == (i == 1 ? 0 : 1), "each selected projection once");
            }
        }
    }

    private static void randomizedParity() {
        for (int trial = 0; trial < 240; trial++) {
            int n = (trial * 17) % 300;
            int dims = 1 + trial % 5;
            double[][] columns = new double[dims][n];
            boolean[] descending = new boolean[dims];
            for (int d = 0; d < dims; d++) {
                descending[d] = RANDOM.nextBoolean();
                for (int row = 0; row < n; row++) {
                    long bits = RANDOM.nextLong();
                    columns[d][row] = Double.longBitsToDouble(bits);
                    if (row % 11 == 0) columns[d][row] = -0.0;
                    if (row % 17 == 0) columns[d][row] = Double.NaN;
                    if (row % 7 == 0 && row > 0) columns[d][row] = columns[d][row - 1];
                }
            }
            byte[] state = new byte[n];
            RANDOM.nextBytes(state);
            double[][] pristine = copy(columns);
            byte[] originalState = state.clone();
            int[] expected = oracle(columns, descending, state, 1, 2);
            int[] actual = M3SignalDimensions.filtered(columns, descending, state,
                    1, 2, M3SignalOrder.Backend.JAVA);
            same(expected, actual, "random masked multi order " + trial);
            same(oracle(columns, descending, null, 0, 0),
                    M3SignalDimensions.order(columns, descending),
                    "random unmasked order " + trial);
            for (int d = 0; d < dims; d++) {
                yes(Arrays.equals(pristine[d], columns[d]), "sidecars retained caller ownership");
            }
            yes(Arrays.equals(originalState, state), "mask retained caller ownership");
        }
    }

    private static void comparisonEvidence() {
        double[][] dimensions = {{-0.0, 0.0, 7, 1e308, -1e308},
                {3, 3, 8, 4, 1}, {0.0, Double.NaN, 5.0, 0.0, 0.0}};
        boolean[] direction = {true, false, false};
        M3SignalDimensions.Comparison c = M3SignalDimensions.compare(
                dimensions, direction, 0, 1);
        yes(c.sign() == 1 && c.decisiveDimension() == 0, "signed zero decisive");
        yes(c.numericSeparations()[0] == 0.0, "zero ordered but no numeric gap");
        yes(Double.isNaN(c.numericSeparations()[2]), "NaN numeric distance unavailable");
        double[] returned = c.numericSeparations();
        returned[0] = 12345;
        yes(c.numericSeparations()[0] == 0.0, "immutable receipt copy");
        var same = M3SignalDimensions.compare(dimensions, direction, 0, 0);
        yes(same.sign() == 0 && same.decisiveDimension() == -1, "equal comparison");
        var overflow = M3SignalDimensions.compare(dimensions, direction, 3, 4);
        yes(Double.isNaN(overflow.numericSeparations()[0]), "overflow numeric gap unavailable");
        var reverse = M3SignalDimensions.compare(dimensions, direction, 1, 0);
        yes(reverse.sign() == -c.sign(), "antisymmetry");
    }

    private static void refusals() {
        rejects(NullPointerException.class,
                () -> M3SignalDimensions.order(null, new boolean[] {false}), "null input");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.order(new double[0][], new boolean[0]), "zero dimensions");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.order(new double[][] {{1}, {2, 3}},
                        new boolean[] {false, false}), "jagged columns");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.order(new double[][] {{1}}, new boolean[0]), "directions");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.filtered(new double[][] {{1}},
                        new boolean[] {false}, null, 1, 0,
                        M3SignalOrder.Backend.JAVA), "mask requires states");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.filtered(new double[][] {{1}},
                        new boolean[] {false}, new byte[1], -1, 0,
                        M3SignalOrder.Backend.JAVA), "negative mask");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.filtered(new double[][] {{1}},
                        new boolean[] {false}, new byte[1], 0, 256,
                        M3SignalOrder.Backend.JAVA), "wide mask");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.filtered(new double[][] {{1}},
                        new boolean[] {false}, new byte[2], 0, 0,
                        M3SignalOrder.Backend.JAVA), "state length");
        rejects(IndexOutOfBoundsException.class,
                () -> M3SignalDimensions.compare(new double[][] {{1}},
                        new boolean[] {false}, 0, 1), "index bounds");
        rejects(IllegalArgumentException.class,
                () -> M3SignalDimensions.project(-1, row -> "x",
                        List.of((ToDoubleFunction<String>) x -> 3.0), new boolean[] {false},
                        null, 0, 0, M3SignalOrder.Backend.JAVA), "negative rows");
        if (M3SignalOrderNative.available()) {
            same(new int[] {1, 0}, M3SignalDimensions.order(new double[][] {{3, 1}},
                    new boolean[] {false}, M3SignalOrder.Backend.NATIVE_REQUIRED),
                    "loaded JNI produces the exact ascending order");
        } else {
            rejects(UnsatisfiedLinkError.class,
                    () -> M3SignalDimensions.order(new double[][] {{3, 1}},
                            new boolean[] {false}, M3SignalOrder.Backend.NATIVE_REQUIRED),
                    "JNI cannot silently fall back");
        }
    }

    private static int[] oracle(double[][] columns, boolean[] descending,
            byte[] state, int required, int excluded) {
        List<Integer> accepted = new ArrayList<>();
        for (int row = 0; row < columns[0].length; row++) {
            int flags = state == null ? 0 : Byte.toUnsignedInt(state[row]);
            if (state == null || ((flags & required) == required && (flags & excluded) == 0)) {
                accepted.add(row);
            }
        }
        accepted.sort((a, b) -> {
            for (int d = 0; d < columns.length; d++) {
                int sign = descending[d]
                        ? Double.compare(columns[d][b], columns[d][a])
                        : Double.compare(columns[d][a], columns[d][b]);
                if (sign != 0) return sign;
            }
            return Integer.compare(a, b);
        });
        return accepted.stream().mapToInt(Integer::intValue).toArray();
    }

    private static double[][] copy(double[][] a) {
        double[][] b = new double[a.length][];
        for (int d = 0; d < a.length; d++) b[d] = a[d].clone();
        return b;
    }

    private static void same(int[] expected, int[] actual, String message) {
        yes(Arrays.equals(expected, actual), message + ": " + Arrays.toString(expected)
                + " != " + Arrays.toString(actual));
    }

    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action, String label) {
        try { action.run(); }
        catch (Throwable error) {yes(type.isInstance(error), label + " -> " + error); return;}
        throw new AssertionError("did not refuse " + label);
    }
}
