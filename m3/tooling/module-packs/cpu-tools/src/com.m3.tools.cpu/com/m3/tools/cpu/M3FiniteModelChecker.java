// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Bounded CTL state-set operations over indexed, totalized Kripke transitions.
 * Dead-end vertices have an implicit self-loop. Not a general CTL/LTL parser or model checker.
 */
public final class M3FiniteModelChecker {
    private M3FiniteModelChecker() { }
    private static final int MAX_VERTICES = 4096;
    private static final int MAX_EDGES = 250_000;

    public record Graph(int[] offsets, int[] targets) {
        public Graph {
            Objects.requireNonNull(offsets, "offsets");
            Objects.requireNonNull(targets, "targets");
            offsets = offsets.clone();
            targets = targets.clone();
            if (offsets.length < 1 || offsets.length - 1 > MAX_VERTICES
                    || targets.length > MAX_EDGES || offsets[0] != 0
                    || offsets[offsets.length - 1] != targets.length)
                throw new IllegalArgumentException("invalid CSR geometry");
            int vertices = offsets.length - 1;
            for (int i = 1; i < offsets.length; i++)
                if (offsets[i] < offsets[i - 1])
                    throw new IllegalArgumentException("unsorted CSR offsets");
            for (int target : targets)
                if (target < 0 || target >= vertices)
                    throw new IllegalArgumentException("out-of-range target");
        }
        @Override public int[] offsets() { return offsets.clone(); }
        @Override public int[] targets() { return targets.clone(); }
        public int vertices() { return offsets.length - 1; }
    }

    private static void validate(Graph graph, long[] mask) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(mask, "mask");
        int n = graph.vertices();
        if (mask.length != (n + 63) / 64)
            throw new IllegalArgumentException("state-mask geometry");
        if ((n & 63) != 0 && mask.length != 0
                && (mask[mask.length - 1] >>> (n & 63)) != 0)
            throw new IllegalArgumentException("state mask has out-of-range bits");
    }

    public static long[] all(Graph graph) {
        Objects.requireNonNull(graph, "graph");
        int n = graph.vertices();
        long[] bits = new long[(n + 63) / 64];
        Arrays.fill(bits, -1L);
        if ((n & 63) != 0) bits[bits.length - 1] = (1L << (n & 63)) - 1L;
        return bits;
    }

    private static boolean get(long[] bits, int i) {
        return (bits[i >>> 6] & (1L << (i & 63))) != 0;
    }
    private static void put(long[] bits, int i) { bits[i >>> 6] |= 1L << (i & 63); }

    private static long[] not(Graph graph, long[] bits) {
        long[] result = all(graph);
        for (int i = 0; i < result.length; i++) result[i] &= ~bits[i];
        return result;
    }

    /** CTL EX(phi): states having at least one successor satisfying phi. */
    public static long[] ex(Graph graph, long[] phi) {
        validate(graph, phi);
        long[] result = new long[phi.length];
        for (int state = 0; state < graph.vertices(); state++) {
            int from = graph.offsets[state], to = graph.offsets[state + 1];
            if (from == to && get(phi, state)) put(result, state);
            for (int edge = from; edge < to; edge++)
                if (get(phi, graph.targets[edge])) { put(result, state); break; }
        }
        return result;
    }

    /** CTL E[condition U goal], inclusive goal states, least fixed point. */
    public static long[] eu(Graph graph, long[] condition, long[] goal) {
        validate(graph, condition);
        validate(graph, goal);
        int n = graph.vertices();
        int[] start = new int[n + 1];
        for (int target : graph.targets) start[target + 1]++;
        for (int i = 1; i <= n; i++) start[i] += start[i - 1];
        int[] next = start.clone(), predecessors = new int[graph.targets.length];
        for (int state = 0; state < n; state++)
            for (int e = graph.offsets[state]; e < graph.offsets[state + 1]; e++)
                predecessors[next[graph.targets[e]]++] = state;
        long[] result = goal.clone();
        int[] queue = new int[n];
        int used = 0;
        for (int i = 0; i < n; i++) if (get(result, i)) queue[used++] = i;
        for (int head = 0; head < used; head++) {
            int state = queue[head];
            for (int e = start[state]; e < start[state + 1]; e++) {
                int prior = predecessors[e];
                if (get(condition, prior) && !get(result, prior)) {
                    put(result, prior);
                    queue[used++] = prior;
                }
            }
        }
        return result;
    }

    /** CTL EG(phi), greatest fixed point; dead ends are interpreted as self-loops. */
    public static long[] eg(Graph graph, long[] phi) {
        validate(graph, phi);
        int n = graph.vertices();
        int[] start = new int[n + 1];
        for (int target : graph.targets) start[target + 1]++;
        for (int i = 1; i <= n; i++) start[i] += start[i - 1];
        int[] next = start.clone(), predecessors = new int[graph.targets.length];
        for (int state = 0; state < n; state++)
            for (int e = graph.offsets[state]; e < graph.offsets[state + 1]; e++)
                predecessors[next[graph.targets[e]]++] = state;
        int[] viable = new int[n], queue = new int[n];
        boolean[] alive = new boolean[n];
        int used = 0;
        for (int state = 0; state < n; state++) {
            if (!get(phi, state)) continue;
            alive[state] = true;
            int from = graph.offsets[state], to = graph.offsets[state + 1];
            if (from == to) viable[state] = 1;
            else for (int e = from; e < to; e++)
                if (get(phi, graph.targets[e])) viable[state]++;
            if (viable[state] == 0) queue[used++] = state;
        }
        for (int head = 0; head < used; head++) {
            int state = queue[head];
            if (!alive[state]) continue;
            alive[state] = false;
            for (int e = start[state]; e < start[state + 1]; e++) {
                int prior = predecessors[e];
                if (alive[prior] && --viable[prior] == 0) queue[used++] = prior;
            }
        }
        long[] result = new long[phi.length];
        for (int i = 0; i < n; i++) if (alive[i]) put(result, i);
        return result;
    }

    /** CTL AG(phi) via the complement of E[true U !phi]. */
    public static long[] ag(Graph graph, long[] phi) {
        validate(graph, phi);
        return not(graph, eu(graph, all(graph), not(graph, phi)));
    }

    /** CTL AF(phi): on every infinite totalized path, eventually phi. */
    public static long[] af(Graph graph, long[] phi) {
        validate(graph, phi);
        return not(graph, eg(graph, not(graph, phi)));
    }

    /** CTL A[condition U goal], including the eventual-goal obligation. */
    public static long[] au(Graph graph, long[] condition, long[] goal) {
        validate(graph, condition);
        validate(graph, goal);
        long[] notGoal = not(graph, goal);
        long[] violation = not(graph, condition);
        for (int i = 0; i < violation.length; i++) violation[i] &= notGoal[i];
        long[] finiteBad = eu(graph, notGoal, violation);
        long[] infiniteBad = eg(graph, notGoal);
        for (int i = 0; i < finiteBad.length; i++) finiteBad[i] |= infiniteBad[i];
        return not(graph, finiteBad);
    }
}
