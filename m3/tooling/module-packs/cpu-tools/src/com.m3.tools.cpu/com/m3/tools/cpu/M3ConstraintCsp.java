// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Exact bounded graph-coloring CSP using bitset domains and primitive adjacency lanes.
 * This is not a general SMT/CSP language or an arbitrary-size decision procedure.
 */
public final class M3ConstraintCsp {
    private M3ConstraintCsp() { }
    private static final int MAX_VERTICES = 64;

    /** A null result means UNSAT; exceeded budgets raise rather than fabricate UNSAT. */
    public static int[] color(int vertices, int colors, int[] from, int[] to,
                              int[] allowedMasks, long maxVisited) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(allowedMasks, "allowedMasks");
        if (vertices < 0 || vertices > MAX_VERTICES || colors < 1 || colors > 16
                || from.length != to.length || from.length > 200_000
                || allowedMasks.length != vertices || maxVisited < 1)
            throw new IllegalArgumentException("CSP geometry/budget");
        long[] adjacency = new long[vertices];
        boolean self = false;
        for (int i = 0; i < from.length; i++) {
            int a = from[i], b = to[i];
            if (a < 0 || b < 0 || a >= vertices || b >= vertices)
                throw new IllegalArgumentException("CSP vertex");
            if (a == b) self = true;
            else {
                adjacency[a] |= 1L << b;
                adjacency[b] |= 1L << a;
            }
        }
        int cap = (1 << colors) - 1;
        int[] domains = allowedMasks.clone();
        for (int value : domains)
            if ((value & ~cap) != 0)
                throw new IllegalArgumentException("CSP domain out of range");
        if (self) return null;
        int[] result = new int[vertices];
        Arrays.fill(result, -1);
        return search(adjacency, domains, result, new long[]{0}, maxVisited) ? result : null;
    }

    private static boolean search(long[] adjacency, int[] domains, int[] values,
                                  long[] visited, long budget) {
        if (visited[0] >= budget) throw new IllegalStateException("CSP visit budget exhausted");
        visited[0]++;
        int selected = -1, available = 0, lowest = Integer.MAX_VALUE;
        for (int vertex = 0; vertex < values.length; vertex++) {
            if (values[vertex] >= 0) continue;
            int used = 0;
            long neighbors = adjacency[vertex];
            while (neighbors != 0) {
                int peer = Long.numberOfTrailingZeros(neighbors);
                neighbors &= neighbors - 1;
                if (values[peer] >= 0) used |= 1 << values[peer];
            }
            int options = domains[vertex] & ~used;
            int choices = Integer.bitCount(options);
            if (choices == 0) return false;
            if (choices < lowest) {
                selected = vertex;
                available = options;
                lowest = choices;
            }
        }
        if (selected < 0) return true;
        while (available != 0) {
            int candidate = Integer.numberOfTrailingZeros(available);
            available &= available - 1;
            values[selected] = candidate;
            if (search(adjacency, domains, values, visited, budget)) return true;
            values[selected] = -1;
        }
        return false;
    }

    /** Independent caller-side geometry check for a returned witness. */
    public static boolean valid(int[] assignment, int colors, int[] from, int[] to,
                                int[] allowedMasks) {
        Objects.requireNonNull(assignment); Objects.requireNonNull(from);
        Objects.requireNonNull(to); Objects.requireNonNull(allowedMasks);
        if (colors < 1 || colors > 16 || assignment.length != allowedMasks.length
                || from.length != to.length) return false;
        for (int v = 0; v < assignment.length; v++)
            if (assignment[v] < 0 || assignment[v] >= colors
                    || (allowedMasks[v] & (1 << assignment[v])) == 0) return false;
        for (int e = 0; e < from.length; e++)
            if (from[e] < 0 || to[e] < 0 || from[e] >= assignment.length
                    || to[e] >= assignment.length
                    || assignment[from[e]] == assignment[to[e]]) return false;
        return true;
    }
}
