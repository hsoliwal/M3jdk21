// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic DAG earliest/latest schedule using only primitive indexed arrays. */
public final class M3CriticalPath {
    private M3CriticalPath() { }
    public record Schedule(int[] order, long[] earliestStart, long[] latestStart,
                           long[] slack, long makespan) {
        public Schedule {
            order = order.clone(); earliestStart = earliestStart.clone();
            latestStart = latestStart.clone(); slack = slack.clone();
            if (order.length != earliestStart.length || slack.length != order.length
                    || latestStart.length != order.length || makespan < 0)
                throw new IllegalArgumentException("schedule geometry");
        }
        @Override public int[] order() { return order.clone(); }
        @Override public long[] earliestStart() { return earliestStart.clone(); }
        @Override public long[] latestStart() { return latestStart.clone(); }
        @Override public long[] slack() { return slack.clone(); }
    }

    /** Durations are nonnegative. An arc u->v means v cannot start before u finishes. */
    public static Schedule analyze(long[] duration, int[] offsets, int[] targets) {
        Objects.requireNonNull(duration); Objects.requireNonNull(offsets); Objects.requireNonNull(targets);
        int n = duration.length;
        if (n > 4096 || offsets.length != n + 1 || offsets[0] != 0
                || offsets[n] != targets.length || targets.length > 262144)
            throw new IllegalArgumentException("DAG geometry");
        int[] indegree = new int[n];
        for (int i = 0; i < n; i++) {
            if (duration[i] < 0 || offsets[i] > offsets[i + 1])
                throw new IllegalArgumentException("negative duration/unsorted offset");
        }
        for (int v : targets) {
            if (v < 0 || v >= n) throw new IllegalArgumentException("invalid target");
            indegree[v] = Math.addExact(indegree[v], 1);
        }
        int[] queue = new int[n], order = new int[n];
        long[] earliest = new long[n], latest = new long[n], slack = new long[n];
        int head = 0, tail = 0, written = 0;
        for (int v = 0; v < n; v++) if (indegree[v] == 0) queue[tail++] = v;
        long makespan = 0;
        while (head < tail) {
            int v = queue[head++];
            order[written++] = v;
            long finish = Math.addExact(earliest[v], duration[v]);
            makespan = Math.max(makespan, finish);
            for (int e = offsets[v]; e < offsets[v + 1]; e++) {
                int dest = targets[e];
                earliest[dest] = Math.max(earliest[dest], finish);
                if (--indegree[dest] == 0) queue[tail++] = dest;
            }
        }
        if (written != n) throw new IllegalArgumentException("cyclic dependency graph");
        for (int i = n - 1; i >= 0; i--) {
            int v = order[i];
            long finish = makespan;
            for (int e = offsets[v]; e < offsets[v + 1]; e++)
                finish = Math.min(finish, latest[targets[e]]);
            latest[v] = Math.subtractExact(finish, duration[v]);
            slack[v] = Math.subtractExact(latest[v], earliest[v]);
            if (slack[v] < 0) throw new IllegalStateException("negative critical slack");
        }
        return new Schedule(order, earliest, latest, slack, makespan);
    }
}
