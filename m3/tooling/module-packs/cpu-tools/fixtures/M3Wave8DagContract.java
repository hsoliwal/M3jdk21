// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project

import jdk.m3.tools.cpu.M3DagReduction;
import java.util.Arrays;
import java.util.Random;

/** Independent finite reachability and edge-minimality oracle for M3DagReduction. */
public final class M3Wave8DagContract {
    private static long checks;
    private M3Wave8DagContract() { }

    private static void check(boolean valid, String reason) {
        checks++;
        if (!valid) { throw new AssertionError(reason + " at check " + checks); }
    }

    /** Independent breadth-first search; omitEdgeIndex -1 means no omission. */
    private static boolean[] reachable(int root, int[] offsets, int[] to, int omitEdgeIndex) {
        int nodes = offsets.length - 1;
        boolean[] seen = new boolean[nodes];
        int[] stack = new int[nodes];
        int count = 1;
        stack[0] = root;
        seen[root] = true;
        while (count != 0) {
            int node = stack[--count];
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                if (edge == omitEdgeIndex) { continue; }
                int next = to[edge];
                if (!seen[next]) { seen[next] = true; stack[count++] = next; }
            }
        }
        return seen;
    }

    private static void checkGraph(int[] originalOffsets, int[] originalTargets) {
        int nodes = originalOffsets.length - 1;
        int[][] result = M3DagReduction.reduce(originalOffsets, originalTargets, 50_000_000);
        int[] offsets = result[0], to = result[1];
        check(offsets.length == originalOffsets.length, "DAG offset dimensions");
        check(to.length <= originalTargets.length, "DAG edge removal monotonic");
        for (int node = 0; node < nodes; node++) {
            check(Arrays.equals(reachable(node, originalOffsets, originalTargets, -1),
                    reachable(node, offsets, to, -1)), "DAG closure exactly preserved");
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                check(!reachable(node, offsets, to, edge)[to[edge]], "DAG essential retained edge");
            }
        }
        int[][] repeat = M3DagReduction.reduce(offsets, to, 50_000_000);
        check(Arrays.equals(offsets, repeat[0]) && Arrays.equals(to, repeat[1]),
                "DAG reduction idempotent");
    }

    public static void main(String[] args) {
        int[][] empty = M3DagReduction.reduce(new int[]{0}, new int[0], 0);
        check(Arrays.equals(empty[0], new int[]{0}) && empty[1].length == 0, "empty DAG");
        int[][] single = M3DagReduction.reduce(new int[]{0,0}, new int[0], 0);
        check(Arrays.equals(single[0], new int[]{0,0}), "isolated vertex");
        Random random = new Random(0xDAC2026L);
        for (int t = 0; t < 5000; t++) {
            int nodes = random.nextInt(27);
            int[] offsets = new int[nodes + 1];
            int[] tmp = new int[nodes * (nodes - 1) / 2];
            int count = 0;
            for (int source = 0; source < nodes; source++) {
                for (int target = source + 1; target < nodes; target++) {
                    if (random.nextInt(100) < 38) { tmp[count++] = target; }
                }
                offsets[source + 1] = count;
            }
            int[] targets = Arrays.copyOf(tmp, count);
            int[] oldOffsets = offsets.clone(), oldTargets = targets.clone();
            checkGraph(offsets, targets);
            check(Arrays.equals(oldOffsets, offsets) && Arrays.equals(oldTargets, targets),
                    "DAG caller arrays unchanged");
        }
        int nodes = 128;
        int[] denseOffsets = new int[nodes + 1];
        int[] denseTargets = new int[nodes * (nodes - 1) / 2];
        int at = 0;
        for (int source = 0; source < nodes; source++) {
            for (int target = source + 1; target < nodes; target++) {
                denseTargets[at++] = target;
            }
            denseOffsets[source + 1] = at;
        }
        int[][] dense = M3DagReduction.reduce(denseOffsets, denseTargets, 50_000_000);
        check(dense[1].length == nodes - 1, "complete DAG reduces to chain");
        checkGraph(denseOffsets, denseTargets);
        int[] longChainOffsets = new int[2049];
        int[] longChainTargets = new int[2047];
        for (int i = 0; i < 2048; i++) {
            longChainOffsets[i + 1] = i < 2047 ? i + 1 : 2047;
            if (i < 2047) { longChainTargets[i] = i + 1; }
        }
        int[][] chain = M3DagReduction.reduce(longChainOffsets, longChainTargets, 50_000_000);
        check(Arrays.equals(chain[1], longChainTargets), "2048-node chain preserved");
        boolean refused = false;
        try { M3DagReduction.reduce(new int[]{0,1,1}, new int[]{1}, 0); }
        catch (IllegalStateException expected) { refused = true; }
        check(refused, "DAG budget exhaustion");
        refused = false;
        try { M3DagReduction.reduce(new int[]{0,1,1}, new int[]{0}, 100); }
        catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "DAG self-loop refusal");
        refused = false;
        try { M3DagReduction.reduce(new int[]{0,2,2,2}, new int[]{2,2}, 100); }
        catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "DAG duplicate refusal");
        System.out.println("M3_WAVE8_DAG_PASS checks=" + checks);
    }
}
