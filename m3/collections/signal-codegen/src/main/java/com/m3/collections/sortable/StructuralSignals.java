// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.util.Objects;

/**
 * Allocation-free comparisons of generated, unsigned-sorted, deduplicated 64-bit token lanes.
 * Hash collisions mean these are approximate source-structure distances, not equality proofs.
 */
public final class StructuralSignals {
    public static final int DIMENSIONS = 6;
    private StructuralSignals() { }

    /** Empty-to-empty is zero; disjoint nonempty sets have unit distance. */
    public static double jaccardDistance(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        requireSortedUnique(left);
        requireSortedUnique(right);
        int i = 0, j = 0;
        long intersection = 0;
        while (i < left.length && j < right.length) {
            int cmp = Long.compareUnsigned(left[i], right[j]);
            if (cmp == 0) { intersection++; i++; j++; }
            else if (cmp < 0) i++;
            else j++;
        }
        long union = (long) left.length + right.length - intersection;
        return union == 0 ? 0.0 : 1.0 - intersection / (double) union;
    }

    /**
     * Weighted aggregate over package, class, fields, methods, dependencies, and calls.
     *
     * <p>Weights are nonnegative finite domain evidence, not ordering keys. Divide by the
     * greatest positive weight before accumulation: multiplying every weight by the same
     * positive finite factor must not change the distance or cause a spurious overflow when
     * the sum of individually finite weights exceeds {@code Double.MAX_VALUE}. Six scaled
     * weights sum to at most six. All token lanes, including zero-weight lanes, retain their
     * previous sorted-unique validation contract.</p>
     */
    public static double weightedDistance(long[][] left, long[][] right, double[] weights) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(weights, "weights");
        if (left.length != DIMENSIONS || right.length != DIMENSIONS
                || weights.length != DIMENSIONS) {
            throw new IllegalArgumentException("expected six signal dimensions");
        }
        double greatest = 0.0;
        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException("weight must be finite and nonnegative");
            }
            if (weight > greatest) greatest = weight;
        }
        if (greatest == 0.0) {
            throw new IllegalArgumentException("total weight must be finite and positive");
        }
        double total = 0.0, mass = 0.0;
        for (int d = 0; d < DIMENSIONS; d++) {
            // Always validate the lane, including zero-weight and underflowed-weight lanes.
            double delta = jaccardDistance(left[d], right[d]);
            double scaled = weights[d] / greatest;
            total += delta * scaled;
            mass += scaled;
        }
        // greatest contributes 1.0, so mass >= 1 and is finite even at extreme scales.
        return Math.min(1.0, Math.max(0.0, total / mass));
    }

    private static void requireSortedUnique(long[] values) {
        for (int i = 1; i < values.length; i++) {
            if (Long.compareUnsigned(values[i - 1], values[i]) >= 0) {
                throw new IllegalArgumentException("signal tokens must be unique unsigned sorted");
            }
        }
    }
}
