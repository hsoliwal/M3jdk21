/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * Independent Java21 reference BFS/mutual-reach differential of attributed type-level SCCs.
 * Does not infer dynamic invocation identity or certify a domain metric from ordinal IDs.
 */
public final class M3SignalGraphComponentsSelfTest {
    private static long checks;
    private M3SignalGraphComponentsSelfTest() { }

    public static void main(String[] args) {
        boundaries();
        example();
        randomized();
        deepChain();
        System.out.println("PASS M3SignalGraphComponentsSelfTest checks=" + checks);
    }

    private static void boundaries() {
        M3SignalRelations empty = M3SignalRelations.of(0, new int[0], new int[0],
                new byte[0], new int[0]);
        var noTypes = M3SignalGraphComponents.from(empty, false);
        eq(0, noTypes.componentCount(), "zero row graph");
        eq(0, noTypes.structuralColumns(123).rowCount(), "zero row signal columns");
        M3SignalRelations singleton = M3SignalRelations.of(1, new int[]{0},
                new int[]{0}, new byte[]{M3SignalRelations.DIRECT}, new int[]{11});
        var one = M3SignalGraphComponents.from(singleton, false);
        eq(1, one.componentCount(), "singleton with self cycle");
        check(one.cyclic(0), "self loop is cyclic");
        eq(11, one.internalCallSites(0), "self edge multiplicity");
        eq(0, one.dependencyDepth(0), "singleton component dependency depth");
        refuse(NullPointerException.class, () -> M3SignalGraphComponents.from(null, false));
        refuse(IndexOutOfBoundsException.class, () -> one.componentOf(1));
        refuse(IndexOutOfBoundsException.class, () -> one.member(0, 1));
        refuse(IndexOutOfBoundsException.class, () -> one.topologicalComponent(1));
    }

    private static void example() {
        int[] from = {0, 1, 1, 2, 3, 4, 5};
        int[] to = {1, 0, 2, 3, 4, 3, 5};
        byte[] types = {1, 1, 1, 2, 1, 1, 1};
        int[] multiplicity = {2, 3, 5, 7, 11, 13, 17};
        var graph = M3SignalRelations.of(6, from, to, types, multiplicity);
        var strict = M3SignalGraphComponents.from(graph, false);
        var permissive = M3SignalGraphComponents.from(graph, true);
        eq(4, strict.componentCount(), "two SCCs plus two other components");
        eq(4, permissive.componentCount(), "candidate joins components without cycles");
        eq(strict.componentOf(0), strict.componentOf(1), "0 and 1 mutually direct-reachable");
        eq(strict.componentOf(3), strict.componentOf(4), "3 and 4 mutually direct-reachable");
        check(strict.componentOf(1) != strict.componentOf(2), "1->2 not mutually reachable");
        check(strict.cyclic(strict.componentOf(5)), "self loop 5 remains cyclic");
        check(!strict.cyclic(strict.componentOf(2)), "single without self loop acyclic");
        eq(5, strict.internalCallSites(strict.componentOf(0)), "0 and 1 internal static sites");
        eq(24, strict.internalCallSites(strict.componentOf(3)), "3 and 4 internal sites");
        eq(7, permissive.incomingCallSites(permissive.componentOf(3)), "candidate cross site counts");
        eq(0, strict.incomingCallSites(strict.componentOf(3)), "candidate edge excluded");
        eq(1, strict.dependencyDepth(strict.componentOf(2)), "depth from SCC {0,1}");
        eq(2, permissive.dependencyDepth(permissive.componentOf(3)), "candidate depth path");
        check(strict.structuralColumns(0x734L).columnCount() == 6,
                "typed structural signal family generated");
    }

    private static void randomized() {
        Random rng = new Random(0x4d335343435f5459L);
        for (int trial = 0; trial < 1_500; trial++) {
            int nodes = 1 + rng.nextInt(34), edges = rng.nextInt(180);
            int[] from = new int[edges], to = new int[edges], sites = new int[edges];
            byte[] types = new byte[edges];
            for (int i = 0; i < edges; i++) {
                from[i] = rng.nextInt(nodes);
                to[i] = rng.nextInt(nodes);
                sites[i] = 1 + rng.nextInt(20);
                types[i] = rng.nextBoolean() ? M3SignalRelations.DIRECT
                        : M3SignalRelations.DISPATCH_CANDIDATE;
            }
            var graph = M3SignalRelations.of(nodes, from, to, types, sites);
            // Source file/permutation order must not change canonical component IDs.
            int[] permutation = new int[edges];
            for (int i = 0; i < edges; i++) permutation[i] = i;
            for (int i = edges - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int old = permutation[i]; permutation[i] = permutation[j]; permutation[j] = old;
            }
            int[] f2 = new int[edges], t2 = new int[edges], s2 = new int[edges];
            byte[] k2 = new byte[edges];
            for (int i = 0; i < edges; i++) {
                int index = permutation[i];
                f2[i] = from[index]; t2[i] = to[index];
                s2[i] = sites[index]; k2[i] = types[index];
            }
            var permuted = M3SignalRelations.of(nodes, f2, t2, k2, s2);
            for (int mode = 0; mode < 2; mode++) {
                boolean includeDispatch = mode != 0;
                var actual = M3SignalGraphComponents.from(graph, includeDispatch);
                var alternate = M3SignalGraphComponents.from(permuted, includeDispatch);
                boolean[][] reach = graphReachability(nodes, from, to, types, includeDispatch);
                int[] componentSize = new int[actual.componentCount()];
                for (int node = 0; node < nodes; node++) {
                    int c = actual.componentOf(node);
                    componentSize[c]++;
                    eq(c, alternate.componentOf(node), "stable component IDs under edge shuffle");
                    for (int other = 0; other < nodes; other++) {
                        boolean same = reach[node][other] && reach[other][node];
                        check((c == actual.componentOf(other)) == same,
                                "SCC equivalent to independent mutual reachability");
                    }
                }
                int[] position = new int[actual.componentCount()];
                for (int ordinal = 0; ordinal < actual.componentCount(); ordinal++) {
                    int c = actual.topologicalComponent(ordinal);
                    position[c] = ordinal;
                    eq(componentSize[c], actual.componentSize(c), "component member length");
                    eq(c, alternate.topologicalComponent(ordinal), "deterministic Kahn tie break");
                    for (int slot = 0; slot < actual.componentSize(c); slot++) {
                        int member = actual.member(c, slot);
                        check(actual.componentOf(member) == c, "source type membership");
                        if (slot > 0) check(actual.member(c, slot - 1) < member,
                                "member directory sorted by type ordinal");
                    }
                    boolean cyc = componentSize[c] > 1;
                    for (int i = 0; i < edges; i++) {
                        if (!includeDispatch && types[i] != M3SignalRelations.DIRECT) continue;
                        if (from[i] == to[i] && actual.componentOf(from[i]) == c) cyc = true;
                    }
                    check(actual.cyclic(c) == cyc, "SCC cycle flag matches independent proof");
                }
                for (int component = 0; component < actual.componentCount(); component++) {
                    for (int i = 0; i < actual.outgoingComponentCount(component); i++) {
                        int next = actual.outgoingComponent(component, i);
                        check(position[component] < position[next],
                                "every condensation DAG edge obeys topological order");
                        check(actual.dependencyDepth(next) > actual.dependencyDepth(component),
                                "each DAG edge strictly increases dependency depth");
                    }
                }
                long[] in = new long[actual.componentCount()], out = new long[actual.componentCount()],
                        internal = new long[actual.componentCount()];
                for (int i = 0; i < edges; i++) {
                    if (!includeDispatch && types[i] != M3SignalRelations.DIRECT) continue;
                    int a = actual.componentOf(from[i]);
                    int b = actual.componentOf(to[i]);
                    if (a == b) internal[a] += sites[i];
                    else { out[a] += sites[i]; in[b] += sites[i]; }
                }
                for (int c = 0; c < actual.componentCount(); c++) {
                    eq(in[c], actual.incomingCallSites(c), "cross-component incoming sites");
                    eq(out[c], actual.outgoingCallSites(c), "cross-component outgoing sites");
                    eq(internal[c], actual.internalCallSites(c), "component internal sites");
                }
                M3SignalColumns columns = actual.structuralColumns(0x314159L);
                eq(nodes, columns.rowCount(), "structural column source rows");
                eq(6, columns.columnCount(), "six typed structural dimensions");
            }
        }
    }

    private static boolean[][] graphReachability(int nodes, int[] from, int[] to,
            byte[] types, boolean includeDispatch) {
        boolean[][] reachable = new boolean[nodes][nodes];
        for (int root = 0; root < nodes; root++) {
            boolean[] seen = reachable[root];
            int[] queue = new int[nodes];
            int head = 0, tail = 1;
            seen[root] = true;
            queue[0] = root;
            while (head < tail) {
                int current = queue[head++];
                for (int edge = 0; edge < from.length; edge++) {
                    if (from[edge] == current &&
                            (includeDispatch || types[edge] == M3SignalRelations.DIRECT)
                            && !seen[to[edge]]) {
                        seen[to[edge]] = true;
                        queue[tail++] = to[edge];
                    }
                }
            }
        }
        return reachable;
    }

    private static void deepChain() {
        int nodes = 50_000;
        int[] from = new int[nodes - 1];
        int[] to = new int[nodes - 1];
        int[] sites = new int[nodes - 1];
        byte[] kinds = new byte[nodes - 1];
        Arrays.fill(sites, 1);
        Arrays.fill(kinds, M3SignalRelations.DIRECT);
        for (int i = 0; i < nodes - 1; i++) { from[i] = i; to[i] = i + 1; }
        var graph = M3SignalGraphComponents.from(
                M3SignalRelations.of(nodes, from, to, kinds, sites), false);
        eq(nodes, graph.componentCount(), "large non-recursive chain has distinct SCCs");
        eq(nodes - 1, graph.dependencyDepth(nodes - 1), "large chain compile-time depth");
        eq(0, graph.topologicalComponent(0), "deterministic root order");
        eq(nodes - 1, graph.topologicalComponent(nodes - 1), "deterministic leaf order");
    }

    private static void eq(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + " expected=" + expected
                + " actual=" + actual + " check=" + checks);
    }
    private static void check(boolean truth, String label) {
        checks++;
        if (!truth) throw new AssertionError(label + " check=" + checks);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("unexpected exception", problem);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
