// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Random;

/** Brute-force executable contract for the exact bounded min-cost-flow leaf. */
//FLLDCIM: RELOCATE - donor blob 1f0dcde31649 at commit fd9a233680b4 (repository, path, URL and digests: m3/algorithms/recipe/mincost-flow-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/mincost-flow-20261106/relocate.py v1
public final class MinCostFlowOracle {
    private static long checks;
    private MinCostFlowOracle() { }

    private static void check(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message + " at check " + checks);
    }

    private static void example(int n, int[] from, int[] to, int[] cap, int[] cost,
            int demand) {
        MinCostFlow.Result actual = MinCostFlow.solve(n, from, to,
                cap, cost, 0, n - 1, demand);
        MinCostFlow.Result repeat = MinCostFlow.solve(n, from, to,
                cap, cost, 0, n - 1, demand);
        check(actual.flow() == repeat.flow() && actual.cost() == repeat.cost()
                && Arrays.equals(actual.edgeFlows(), repeat.edgeFlows()), "replay");
        int[] balance = new int[n];
        long costFromEdges = 0;
        int[] edgeFlow = actual.edgeFlows();
        for (int e = 0; e < cap.length; e++) {
            check(edgeFlow[e] >= 0 && edgeFlow[e] <= cap[e], "capacity");
            balance[from[e]] -= edgeFlow[e];
            balance[to[e]] += edgeFlow[e];
            costFromEdges += (long) cost[e] * edgeFlow[e];
        }
        for (int v = 0; v < n; v++) {
            check(balance[v] == (v == 0 ? -actual.flow() : v == n - 1
                    ? actual.flow() : 0), "conservation");
        }
        check(costFromEdges == actual.cost() && actual.flow() <= demand, "cost");
        int[] best = {-1, Integer.MAX_VALUE};
        exhaustive(0, new int[cap.length], n, from, to, cap, cost, demand, best);
        check(actual.flow() == best[0] && actual.cost() == best[1], "optimality");
        Arrays.fill(edgeFlow, Integer.MIN_VALUE);
        check(actual.edgeFlows() != edgeFlow
                && (edgeFlow.length == 0 || !Arrays.equals(actual.edgeFlows(), edgeFlow)),
                "detached results");
    }

    private static void exhaustive(int at, int[] values, int n, int[] from,
            int[] to, int[] cap, int[] cost, int demand, int[] best) {
        if (at < values.length) {
            for (int i = 0; i <= cap[at]; i++) {
                values[at] = i;
                exhaustive(at + 1, values, n, from, to, cap, cost, demand, best);
            }
            return;
        }
        int[] balance = new int[n];
        int price = 0;
        for (int i = 0; i < values.length; i++) {
            balance[from[i]] -= values[i];
            balance[to[i]] += values[i];
            price += values[i] * cost[i];
        }
        int quantity = -balance[0];
        if (quantity < 0 || quantity > demand || balance[n - 1] != quantity) return;
        for (int i = 1; i < n - 1; i++) if (balance[i] != 0) return;
        if (quantity > best[0] || quantity == best[0] && price < best[1]) {
            best[0] = quantity;
            best[1] = price;
        }
    }

    private static void refusal(Runnable attempt) {
        boolean rejected = false;
        try { attempt.run(); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "invalid input must refuse");
    }

    public static void main(String[] args) {
        example(4, new int[]{0, 0, 1, 1, 2, 2},
                new int[]{1, 2, 2, 3, 1, 3},
                new int[]{2, 1, 1, 1, 1, 1},
                new int[]{1, 2, 0, 5, 0, 1}, 3);
        // Reverse-arc reroute: first shortest path is not globally sufficient.
        example(6, new int[]{0, 0, 1, 1, 2, 2, 3, 4},
                new int[]{1, 2, 3, 4, 3, 4, 5, 5},
                new int[]{1, 1, 1, 1, 1, 1, 1, 1},
                new int[]{0, 0, 0, 1, 0, 100, 0, 0}, 2);
        Random random = new Random(0x4d33534c414c45L);
        for (int trial = 0; trial < 600; trial++) {
            int n = 2 + random.nextInt(4), m = random.nextInt(8);
            int[] from = new int[m], to = new int[m], cap = new int[m], cost = new int[m];
            for (int i = 0; i < m; i++) {
                from[i] = random.nextInt(n);
                to[i] = random.nextInt(n - 1);
                if (to[i] >= from[i]) to[i]++;
                cap[i] = random.nextInt(3);
                cost[i] = random.nextInt(8);
            }
            example(n, from, to, cap, cost, random.nextInt(5));
        }
        refusal(() -> MinCostFlow.solve(2, new int[]{0}, new int[]{0},
                new int[]{1}, new int[]{0}, 0, 1, 1));
        refusal(() -> MinCostFlow.solve(2, new int[]{0}, new int[]{1},
                new int[]{1}, new int[]{-1}, 0, 1, 1));
        refusal(() -> MinCostFlow.solve(2, new int[]{0}, new int[]{1},
                new int[]{1}, new int[]{0}, 0, 0, 1));
        refusal(() -> MinCostFlow.solve(2, new int[]{0}, new int[]{1},
                new int[]{1}, new int[]{0}, 0, 1, 1001));
        System.out.println("M3_MINCOST_FLOW_PASS checks=" + checks);
    }
}
