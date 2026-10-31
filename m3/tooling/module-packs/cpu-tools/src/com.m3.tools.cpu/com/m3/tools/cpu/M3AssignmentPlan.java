// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Exact bounded nonnegative min-cost one-to-one assignment using subset DP.
 * Not a general integer/linear programming solver. Tasks and workers are indexed 0..n-1.
 */
public final class M3AssignmentPlan {
    private M3AssignmentPlan() { }
    public static final long FORBIDDEN = Long.MAX_VALUE;

    public record Plan(boolean feasible, long cost, int[] taskByWorker) {
        public Plan { taskByWorker = taskByWorker.clone(); }
        @Override public int[] taskByWorker() { return taskByWorker.clone(); }
    }

    /** Row-major worker x task costs. Forbidden edges have Long.MAX_VALUE.
     * Equal-cost solutions choose the lexicographically smallest assignment.
     * A sum that exceeds long range is intentionally considered not representable.
     */
    public static Plan minCost(int workers, long[] costs) {
        Objects.requireNonNull(costs, "costs");
        if (workers < 0 || workers > 18 || costs.length != workers * workers)
            throw new IllegalArgumentException("bounded square assignment geometry");
        for (long cost : costs) if (cost < 0)
            throw new IllegalArgumentException("negative assignment cost");
        int states = 1 << workers, full = states - 1;
        long[] best = new long[states]; Arrays.fill(best, FORBIDDEN);
        best[full] = 0;
        int[] choose = new int[states]; Arrays.fill(choose, -1);
        for (int mask = full - 1; mask >= 0; mask--) {
            int worker = Integer.bitCount(mask);
            for (int task = 0; task < workers; task++) {
                if ((mask & (1 << task)) != 0) continue;
                long cost = costs[worker * workers + task], suffix = best[mask | (1 << task)];
                if (cost == FORBIDDEN || suffix == FORBIDDEN || cost > Long.MAX_VALUE - suffix) continue;
                long candidate = cost + suffix;
                if (candidate < best[mask] || (candidate == best[mask]
                        && (choose[mask] < 0 || task < choose[mask]))) {
                    best[mask] = candidate; choose[mask] = task;
                }
            }
        }
        if (best[0] == FORBIDDEN) return new Plan(false, FORBIDDEN, new int[0]);
        int[] taskByWorker = new int[workers];
        int mask = 0;
        for (int worker = 0; worker < workers; worker++) {
            int task = choose[mask];
            if (task < 0) throw new IllegalStateException("broken assignment predecessor");
            taskByWorker[worker] = task; mask |= 1 << task;
        }
        return new Plan(true, best[0], taskByWorker);
    }
}
