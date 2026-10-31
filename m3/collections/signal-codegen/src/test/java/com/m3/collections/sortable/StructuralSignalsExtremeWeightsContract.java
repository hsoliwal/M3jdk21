// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.util.Arrays;
import java.util.Random;

/** Standalone Java 21 differential contract for source-time distance normalization. */
public final class StructuralSignalsExtremeWeightsContract {
    private static int checks;
    private static final Random RANDOM = new Random(0x53d11a7eL);
    private StructuralSignalsExtremeWeightsContract() { }

    public static void main(String[] args) {
        long[][] empty = six(new long[0]);
        long[][] a = six(new long[0]);
        long[][] b = six(new long[0]);
        a[0] = new long[] {1};
        b[0] = new long[0]; // first dimension disjoint, second dimension identical
        eq(0.5, StructuralSignals.weightedDistance(a, b,
                new double[] {Double.MAX_VALUE, Double.MAX_VALUE, 0, 0, 0, 0}), "huge balanced weights");
        eq(0.5, StructuralSignals.weightedDistance(a, b,
                new double[] {Double.MIN_VALUE, Double.MIN_VALUE, 0, 0, 0, 0}), "tiny balanced weights");
        eq(0.5, StructuralSignals.weightedDistance(a, b,
                new double[] {1, 1, 0, 0, 0, 0}), "ordinary balanced weights");
        eq(0.0, StructuralSignals.weightedDistance(empty, empty,
                new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                        Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}), "huge identity");
        eq(0.0, StructuralSignals.weightedDistance(a, a,
                new double[] {1, 1, 1, 1, 1, 1}), "same tokens identity");
        for (int round = 0; round < 2048; round++) {
            long[][] x = corpus(), y = corpus();
            double[] weights = new double[6];
            for (int d = 0; d < weights.length; d++) {
                // Include moderate, tiny and extremely large values in the same metadata schema.
                weights[d] = (round % 7 == 0) ? Double.MAX_VALUE
                        : (round % 7 == 1) ? Double.MIN_VALUE
                        : (1 + RANDOM.nextInt(10000)) / 101.0;
            }
            double actual = StructuralSignals.weightedDistance(x, y, weights);
            double mirror = StructuralSignals.weightedDistance(y, x, weights);
            check(Double.isFinite(actual) && actual >= 0.0 && actual <= 1.0, "bounded finite");
            check(Math.abs(actual - mirror) <= 1e-14, "symmetry");
            eq(0.0, StructuralSignals.weightedDistance(x, x, weights), "identity");
            // Multiplying by a safe common factor cannot change the weighted mean.
            if (round % 7 >= 2) {
                double[] scaled = weights.clone();
                for (int d = 0; d < 6; d++) scaled[d] *= 2.0;
                double projected = StructuralSignals.weightedDistance(x, y, scaled);
                check(Math.abs(actual - projected) <= 1e-14, "scale invariance");
            }
        }
        expectInvalid(() -> StructuralSignals.weightedDistance(empty, empty,
                new double[] {0, 0, 0, 0, 0, 0}), "no metric weight");
        expectInvalid(() -> StructuralSignals.weightedDistance(empty, empty,
                new double[] {Double.POSITIVE_INFINITY, 0, 0, 0, 0, 0}), "infinite weight");
        expectInvalid(() -> StructuralSignals.weightedDistance(empty, empty,
                new double[] {-1, 0, 0, 0, 0, 0}), "negative weight");
        expectInvalid(() -> StructuralSignals.weightedDistance(empty, empty,
                new double[] {Double.NaN, 0, 0, 0, 0, 0}), "NaN weight");
        long[][] invalid = six(new long[0]);
        invalid[5] = new long[] {2, 1};
        expectInvalid(() -> StructuralSignals.weightedDistance(invalid, empty,
                new double[] {1, 0, 0, 0, 0, 0}), "zero-weight lane still validated");
        System.out.println("PASS structural-signal-distance assertions=" + checks);
    }

    private static long[][] six(long[] lane) {
        long[][] result = new long[6][];
        Arrays.fill(result, lane);
        return result;
    }

    private static long[][] corpus() {
        long[][] lanes = new long[6][];
        for (int d = 0; d < lanes.length; d++) {
            int len = RANDOM.nextInt(32);
            long[] values = new long[len];
            for (int i = 0; i < len; i++) values[i] = RANDOM.nextInt(150);
            Arrays.sort(values);
            int unique = 0;
            for (int i = 0; i < len; i++) {
                if (unique == 0 || values[i] != values[unique - 1]) values[unique++] = values[i];
            }
            lanes[d] = Arrays.copyOf(values, unique);
        }
        return lanes;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void eq(double expected, double actual, String message) {
        checks++;
        if (Double.compare(expected, actual) != 0) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void expectInvalid(Runnable task, String message) {
        try { task.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(message);
    }
}
