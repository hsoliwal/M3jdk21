// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Exact bounded nonnegative 0/1 knapsack over primitive DP rows and packed choice bits.
 * Pseudo-polynomial in capacity, not a general ILP or branch-and-bound engine.
 */
public final class M3KnapsackSelection {
    private M3KnapsackSelection() { }
    public record Plan(long value, int weight, int[] chosen) {
        public Plan { chosen = chosen.clone(); }
        @Override public int[] chosen() { return chosen.clone(); }
    }

    public static Plan maximize(int[] weights, long[] values, int capacity) {
        Objects.requireNonNull(weights, "weights");
        Objects.requireNonNull(values, "values");
        if (weights.length != values.length || weights.length > 128
                || capacity < 0 || capacity > 4096)
            throw new IllegalArgumentException("knapsack geometry");
        int stride = capacity + 1;
        long[] choices = new long[(weights.length * stride + 63) >>> 6];
        long[] best = new long[stride];
        for (int item = 0; item < weights.length; item++) {
            int weight = weights[item];
            long value = values[item];
            if (weight < 0 || value < 0) throw new IllegalArgumentException("negative item");
            if (weight > capacity) continue;
            for (int remaining = capacity; remaining >= weight; remaining--) {
                long candidate = Math.addExact(best[remaining - weight], value);
                if (candidate > best[remaining]) {
                    best[remaining] = candidate;
                    int bit = item * stride + remaining;
                    choices[bit >>> 6] |= 1L << (bit & 63);
                }
            }
        }
        int[] reversed = new int[weights.length];
        int size = 0, left = capacity, occupied = 0;
        for (int item = weights.length - 1; item >= 0; item--) {
            int bit = item * stride + left;
            if ((choices[bit >>> 6] & (1L << (bit & 63))) == 0) continue;
            reversed[size++] = item;
            left -= weights[item];
            occupied = Math.addExact(occupied, weights[item]);
        }
        int[] chosen = new int[size];
        for (int i = 0; i < size; i++) chosen[i] = reversed[size - i - 1];
        return new Plan(best[capacity], occupied, chosen);
    }
}
