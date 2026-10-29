// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/**
 * Bounded primitive-array statistical atoms for structured event/window tools.
 * These are mathematical building blocks, not complete CEP, finance, or inference engines.
 */
public final class M3StreamMath {
    private M3StreamMath() { }

    /** Finite values and a bounded positive window; detached mean array. */
    public static double[] movingMean(double[] samples, int window) {
        Objects.requireNonNull(samples, "samples");
        if (window < 1 || window > samples.length) throw new IllegalArgumentException("window");
        for (double value : samples) if (!Double.isFinite(value)) throw new IllegalArgumentException("sample");
        double[] result = new double[samples.length - window + 1];
        // Sliding compensated sum limits repeated cancellation without materializing windows.
        double sum = 0, compensation = 0;
        for (int i = 0; i < samples.length; i++) {
            double delta = samples[i] - (i >= window ? samples[i - window] : 0.0);
            double corrected = delta - compensation;
            double next = sum + corrected;
            compensation = (next - sum) - corrected;
            sum = next;
            if (i + 1 >= window) {
                result[i + 1 - window] = sum / window;
                if (!Double.isFinite(result[i + 1 - window]))
                    throw new ArithmeticException("moving mean overflow");
            }
        }
        return result;
    }

    /** Exponentially weighted moving average with first input as the initial state. */
    public static double[] ewma(double[] samples, double alpha) {
        Objects.requireNonNull(samples, "samples");
        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1)
            throw new IllegalArgumentException("alpha");
        double[] out = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            double value = samples[i];
            if (!Double.isFinite(value)) throw new IllegalArgumentException("sample");
            out[i] = i == 0 ? value : Math.fma(alpha, value - out[i - 1], out[i - 1]);
            if (!Double.isFinite(out[i])) throw new ArithmeticException("EWMA overflow");
        }
        return out;
    }

    /** Dense fixed-domain integer histogram without boxed keys or Entry objects. */
    public static long[] histogram(int[] observations, int domain) {
        Objects.requireNonNull(observations, "observations");
        if (domain < 1 || domain > 1_000_000) throw new IllegalArgumentException("domain");
        long[] counts = new long[domain];
        for (int value : observations) {
            if (value < 0 || value >= domain) throw new IllegalArgumentException("observation");
            counts[value] = Math.addExact(counts[value], 1);
        }
        return counts;
    }

    /** Shannon entropy in bits; zero counts contribute nothing, empty distribution is zero. */
    public static double entropyBits(long[] counts) {
        Objects.requireNonNull(counts, "counts");
        long total = total(counts);
        if (total == 0) return 0;
        double entropy = 0;
        for (long count : counts) if (count > 0) {
            double p = (double) count / (double) total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /** Gini impurity of an integer-category histogram; empty distribution is zero. */
    public static double gini(long[] counts) {
        Objects.requireNonNull(counts, "counts");
        long total = total(counts);
        if (total == 0) return 0;
        double concentration = 0;
        for (long count : counts) {
            double p = (double) count / (double) total;
            concentration = Math.fma(p, p, concentration);
        }
        return Math.max(0, 1 - concentration);
    }

    /** Jensen–Shannon divergence (bits) between histograms, symmetric and bounded [0,1]. */
    public static double jsDivergence(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) throw new IllegalArgumentException("incompatible histogram domains");
        long a = total(left), b = total(right);
        if (a == 0 || b == 0) throw new IllegalArgumentException("empty probability mass");
        double divergence = 0;
        for (int i = 0; i < left.length; i++) {
            double p = (double) left[i] / a, q = (double) right[i] / b;
            double mean = (p + q) * 0.5;
            if (p > 0) divergence += 0.5 * p * (Math.log(p / mean) / Math.log(2));
            if (q > 0) divergence += 0.5 * q * (Math.log(q / mean) / Math.log(2));
        }
        return Math.min(1, Math.max(0, divergence));
    }

    private static long total(long[] values) {
        long sum = 0;
        for (long value : values) {
            if (value < 0) throw new IllegalArgumentException("negative frequency");
            sum = Math.addExact(sum, value);
        }
        return sum;
    }
}
