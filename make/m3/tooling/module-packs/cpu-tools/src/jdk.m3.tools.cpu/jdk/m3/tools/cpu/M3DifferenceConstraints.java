// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

/** Bounded exact feasibility of integer difference constraints using primitive lanes. */
public final class M3DifferenceConstraints {
    private static final int MAX_VARIABLES = 4096;
    private static final int MAX_EDGES = 131072;
    private static final long MAX_STEPS = 50_000_000L;

    private M3DifferenceConstraints() { }

    /**
     * Solve x[to[e]] - x[from[e]] <= upper[e].
     *
     * @return a detached feasible long-valued witness, or null for a proven
     *         negative cycle; all zero potentials are the deterministic seed
     * @throws IllegalArgumentException for malformed lanes or budgets
     * @throws IllegalStateException if the exact edge-relaxation budget is exhausted
     */
    public static long[] solve(int variables, int[] from, int[] to, int[] upper, long maxSteps) {
        validate(variables, from, to, upper);
        if (maxSteps < 0L || maxSteps > MAX_STEPS) {
            throw new IllegalArgumentException("invalid relaxation budget");
        }
        long[] potentials = new long[variables];
        long executed = 0L;
        for (int pass = 0; pass < variables; pass++) {
            boolean changed = false;
            for (int edge = 0; edge < from.length; edge++) {
                if (executed == maxSteps) {
                    throw new IllegalStateException("difference constraint work budget exhausted");
                }
                executed++;
                // At most variables signed-int edges can influence a finite
                // shortest simple path; their sum fits comfortably in long.
                long candidate = potentials[from[edge]] + upper[edge];
                if (candidate < potentials[to[edge]]) {
                    potentials[to[edge]] = candidate;
                    changed = true;
                }
            }
            if (!changed) { return potentials; }
            if (pass == variables - 1) { return null; }
        }
        throw new AssertionError("unreachable difference constraint iteration");
    }

    /** Validate a caller-provided witness without trusting signed-long addition to wrap. */
    public static boolean verify(int variables, int[] from, int[] to, int[] upper,
            long[] potentials) {
        validate(variables, from, to, upper);
        if (potentials == null || potentials.length != variables) { return false; }
        for (int edge = 0; edge < from.length; edge++) {
            long source = potentials[from[edge]];
            int weight = upper[edge];
            if (weight > 0 && source > Long.MAX_VALUE - weight) {
                // The mathematical upper bound is above all representable longs.
                continue;
            }
            if (weight < 0 && source < Long.MIN_VALUE - weight) {
                // The mathematical upper bound is below all representable longs.
                return false;
            }
            if (potentials[to[edge]] > source + weight) { return false; }
        }
        return true;
    }

    private static void validate(int variables, int[] from, int[] to, int[] upper) {
        if (variables < 1 || variables > MAX_VARIABLES || from == null || to == null
                || upper == null || from.length > MAX_EDGES || from.length != to.length
                || from.length != upper.length) {
            throw new IllegalArgumentException("invalid difference constraints");
        }
        for (int edge = 0; edge < from.length; edge++) {
            if (from[edge] < 0 || from[edge] >= variables
                    || to[edge] < 0 || to[edge] >= variables) {
                throw new IllegalArgumentException("difference constraint vertex out of range");
            }
        }
    }
}
