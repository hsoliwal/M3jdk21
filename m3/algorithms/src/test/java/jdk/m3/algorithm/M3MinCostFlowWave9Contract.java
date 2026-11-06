// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Independent exhaustive small-flow oracle: the flow half of the wave9 contract, mapped onto the landed
 * {@link M3MinCostFlow}. The rectangle-intersection half has no M3 owner and is not relocated.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-cpu-structured-wave9-20261031/M3Wave9FlowSpatialContract.java.txt blob 66fab6b48f17; recipe m3/algorithms/recipe/census2-a5-20261106/relocate.py v1
public final class M3MinCostFlowWave9Contract {
    private static long checks;

    private M3MinCostFlowWave9Contract() { }

    private static void check(boolean condition, String failure) {
        checks++;
        if (!condition) { throw new AssertionError(failure); }
    }

    private static void refuses(Class<? extends Throwable> type, Runnable call) {
        try { call.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "wrong refusal: " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getSimpleName() + " refusal");
    }

    private static void flowOracle(int edge, int[] from, int[] to, int[] cap, int[] cost,
            int[] used, long[] balance, int source, int sink, int demand, long spent,
            long[] best) {
        if (edge == from.length) {
            long flow = balance[source];
            if (flow < 0 || flow > demand || balance[sink] != -flow) { return; }
            for (int v = 0; v < balance.length; v++) {
                if (v != source && v != sink && balance[v] != 0L) { return; }
            }
            if (flow > best[0] || (flow == best[0] && spent < best[1])) {
                best[0] = flow;
                best[1] = spent;
            }
            return;
        }
        for (int quantity = 0; quantity <= cap[edge]; quantity++) {
            used[edge] = quantity;
            balance[from[edge]] += quantity;
            balance[to[edge]] -= quantity;
            flowOracle(edge + 1, from, to, cap, cost, used, balance,
                    source, sink, demand, spent + (long) quantity * cost[edge], best);
            balance[from[edge]] -= quantity;
            balance[to[edge]] += quantity;
        }
    }

    private static void flowCases() {
        SplittableRandom r = new SplittableRandom(0x92026031L);
        for (int iteration = 0; iteration < 3_100; iteration++) {
            int vertices = 2 + r.nextInt(4);
            int edges = r.nextInt(8);
            int[] from = new int[edges], to = new int[edges];
            int[] cap = new int[edges], cost = new int[edges];
            for (int i = 0; i < edges; i++) {
                from[i] = r.nextInt(vertices);
                to[i] = r.nextInt(vertices);
                cap[i] = r.nextInt(3);
                cost[i] = r.nextInt(8);
            }
            int demand = r.nextInt(5);
            int[] fc = cap.clone(), wc = cost.clone();
            long[] best = {-1L, Long.MAX_VALUE};
            flowOracle(0, from, to, cap, cost, new int[edges],
                    new long[vertices], 0, vertices - 1, demand, 0L, best);
            long[] actual = solve(vertices, from, to, cap, cost,
                    0, vertices - 1, demand, 50_000_000L);
            check(Arrays.equals(best, actual), "flow mismatch iteration " + iteration
                    + " best=" + Arrays.toString(best) + " actual=" + Arrays.toString(actual)
                    + " from=" + Arrays.toString(from) + " to=" + Arrays.toString(to)
                    + " cap=" + Arrays.toString(cap) + " cost=" + Arrays.toString(cost));
            check(Arrays.equals(cap, fc) && Arrays.equals(cost, wc), "flow aliased input");
        }
        //FLLDCIM: MRL - census2 P2-070 addition: a costed reverse-arc reroute; the first shortest path 0-1-2-3 is undone
        int[] rerouteFrom = {0, 0, 1, 1, 2}, rerouteTo = {1, 2, 2, 3, 3};
        int[] rerouteCap = {1, 1, 1, 1, 1}, rerouteCost = {1, 3, 1, 3, 1};
        long[] rerouteBest = {-1L, Long.MAX_VALUE};
        flowOracle(0, rerouteFrom, rerouteTo, rerouteCap, rerouteCost, new int[5], new long[4], 0, 3, 2, 0L, rerouteBest);
        check(Arrays.equals(rerouteBest, new long[] {2L, 8L}) && Arrays.equals(rerouteBest,
                solve(4, rerouteFrom, rerouteTo, rerouteCap, rerouteCost, 0, 3, 2, 10L)), "costed reverse-arc reroute");
        check(Arrays.equals(solve(2, new int[0], new int[0],
                new int[0], new int[0], 0, 1, 10, 500L), new long[] {0L, 0L}),
                "empty flow");
        // The owner has no work budget: the donor's zero-budget refusal has no counterpart.
        refuses(IllegalArgumentException.class,
                () -> M3MinCostFlow.solve(2, new int[] {0}, new int[] {1},
                        new int[] {-1}, new int[] {1}, 0, 1, 1));
        refuses(IllegalArgumentException.class,
                () -> M3MinCostFlow.solve(2, new int[] {0}, new int[] {1},
                        new int[] {1}, new int[] {-1}, 0, 1, 1));
    }

    //FLLDCIM: MRL - census2 P2-070 (crate m3-cpu-structured-wave9-20261031): the wave9 call shape over the landed owner
    /**
     * The wave9 call shape {@code long[] {flow, cost}} over the landed {@link M3MinCostFlow}. The owner refuses a
     * self-loop edge, so each instance that has one is first checked as that refusal and then solved without its
     * self-loops: costs are nonnegative and a self-loop moves no flow, so the exhaustive oracle's optimum over the
     * full instance is the optimum without them. The work budget has no owner counterpart and is not used.
     */
    private static long[] solve(int vertices, int[] from, int[] to, int[] cap, int[] cost,
            int source, int sink, int demand, long budget) {
        int kept = 0;
        for (int i = 0; i < from.length; i++) {
            if (from[i] != to[i]) { kept++; }
        }
        if (kept < from.length) {
            refuses(IllegalArgumentException.class,
                    () -> M3MinCostFlow.solve(vertices, from, to, cap, cost, source, sink, demand));
        }
        int[] f = new int[kept], t = new int[kept], c = new int[kept], w = new int[kept];
        for (int i = 0, k = 0; i < from.length; i++) {
            if (from[i] != to[i]) {
                f[k] = from[i];
                t[k] = to[i];
                c[k] = cap[i];
                w[k] = cost[i];
                k++;
            }
        }
        M3MinCostFlow.Result result = M3MinCostFlow.solve(vertices, f, t, c, w, source, sink, demand);
        return new long[] {result.flow(), result.cost()};
    }

    public static void main(String[] args) {
        flowCases();
        System.out.println("M3_MINCOST_FLOW_WAVE9_PASS checks=" + checks);
    }
}
