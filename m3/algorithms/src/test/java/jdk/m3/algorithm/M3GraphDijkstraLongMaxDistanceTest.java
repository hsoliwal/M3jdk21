// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BigInteger shortest-path oracle; no competitive challenge solution imported. */
//FLLDCIM: RELOCATE - donor blob 96b87962b6db at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/graph-trie-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/graph-trie-20261106/relocate.py v1
final class M3GraphDijkstraLongMaxDistanceTest {
    @Test
    void exactlyMaximumFinitePathIsNotMissing() {
        Map<Integer, List<M3GraphAlgorithms.WeightedEdge<Integer>>> graph =
                Map.of(0, List.of(new M3GraphAlgorithms.WeightedEdge<>(1, Long.MAX_VALUE)));
        assertEquals(Map.of(0, 0L, 1, Long.MAX_VALUE),
                M3GraphAlgorithms.dijkstra(graph, 0, M3ProgressMonitor.none()));
    }

    @Test
    void zeroWeightExtensionOfMaximumPathRetainsFullDistance() {
        Map<Integer, List<M3GraphAlgorithms.WeightedEdge<Integer>>> graph =
                Map.of(0, List.of(new M3GraphAlgorithms.WeightedEdge<>(1, Long.MAX_VALUE)),
                       1, List.of(new M3GraphAlgorithms.WeightedEdge<>(2, 0L)));
        assertEquals(Map.of(0, 0L, 1, Long.MAX_VALUE, 2, Long.MAX_VALUE),
                M3GraphAlgorithms.dijkstra(graph, 0, M3ProgressMonitor.none()));
    }

    @Test
    void exhaustiveSmallGraphsAndSeededDagsMatchIndependentBigIntegerBellmanFord() {
        int[][] endpoints = {{0, 1}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 1}};
        int cases = 0;
        for (int mask = 0; mask < 4096; mask++) {
            int code = mask;
            List<Integer> from = new ArrayList<>(), to = new ArrayList<>();
            List<Long> weights = new ArrayList<>();
            for (int edge = 0; edge < endpoints.length; edge++) {
                int kind = code & 3;
                code >>>= 2;
                if (kind > 0) {
                    from.add(endpoints[edge][0]);
                    to.add(endpoints[edge][1]);
                    weights.add((long) kind - 1);
                }
            }
            check(3, from, to, weights);
            cases++;
        }

        Random random = new Random(0x4377_2026L);
        for (int trial = 0; trial < 3000; trial++) {
            int nodes = 2 + random.nextInt(8);
            List<Integer> from = new ArrayList<>(), to = new ArrayList<>();
            List<Long> weights = new ArrayList<>();
            for (int left = 0; left < nodes; left++) {
                for (int right = left + 1; right < nodes; right++) {
                    if (random.nextInt(4) == 0) {
                        from.add(left);
                        to.add(right);
                        weights.add((long) random.nextInt(25));
                    }
                }
            }
            check(nodes, from, to, weights);
            cases++;
        }
        assertEquals(7096, cases);
    }

    private static void check(int n, List<Integer> from, List<Integer> to, List<Long> weights) {
        Map<Integer, List<M3GraphAlgorithms.WeightedEdge<Integer>>> graph = new HashMap<>();
        for (int i = 0; i < weights.size(); i++) {
            graph.computeIfAbsent(from.get(i), unused -> new ArrayList<>())
                    .add(new M3GraphAlgorithms.WeightedEdge<>(to.get(i), weights.get(i)));
        }
        BigInteger[] dist = new BigInteger[n];
        dist[0] = BigInteger.ZERO;
        for (int pass = 1; pass < n; pass++) {
            boolean changed = false;
            for (int edge = 0; edge < weights.size(); edge++) {
                BigInteger current = dist[from.get(edge)];
                if (current == null) {
                    continue;
                }
                BigInteger candidate = current.add(BigInteger.valueOf(weights.get(edge)));
                BigInteger previous = dist[to.get(edge)];
                if (previous == null || candidate.compareTo(previous) < 0) {
                    dist[to.get(edge)] = candidate;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        Map<Integer, Long> expected = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (dist[i] != null) {
                expected.put(i, dist[i].longValueExact());
            }
        }
        assertEquals(expected, M3GraphAlgorithms.dijkstra(graph, 0, M3ProgressMonitor.none()));
    }

    @Test
    void newReachabilityRetainsProgressAndCancellationOwnership() {
        final class Trace implements M3ProgressMonitor {
            int starts, work, finishes, checks;
            boolean cancel;
            @Override public void beginTask(String name, int total) {
                starts++;
                assertEquals("dijkstra", name);
            }
            @Override public void worked(int amount) { work += amount; }
            @Override public void done() { finishes++; }
            @Override public boolean isCanceled() { checks++; return cancel; }
        }

        Map<Integer, List<M3GraphAlgorithms.WeightedEdge<Integer>>> graph =
                Map.of(0, List.of(new M3GraphAlgorithms.WeightedEdge<>(1, Long.MAX_VALUE)),
                       1, List.of(new M3GraphAlgorithms.WeightedEdge<>(2, 0L)));
        Trace ordinary = new Trace();
        assertEquals(3, M3GraphAlgorithms.dijkstra(graph, 0, ordinary).size());
        assertEquals(1, ordinary.starts);
        assertEquals(1, ordinary.finishes);
        assertEquals(3, ordinary.work);
        assertTrue(ordinary.checks >= ordinary.work);

        Trace cancelled = new Trace();
        cancelled.cancel = true;
        assertThrows(CancellationException.class,
                () -> M3GraphAlgorithms.dijkstra(graph, 0, cancelled));
        assertEquals(0, cancelled.starts);
        assertEquals(0, cancelled.finishes);
    }
}
