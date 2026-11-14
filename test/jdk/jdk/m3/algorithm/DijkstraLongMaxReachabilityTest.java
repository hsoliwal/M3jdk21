// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** O(VE) independent oracle over reachability/BigInteger, not a Dijkstra copy. */
//FLLDCIM: RELOCATE - donor blob 727aab26183e at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/graph-trie-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/graph-trie-20261106/relocate.py v1
final class DijkstraLongMaxReachabilityTest {
    private record Edge(int from, int to, long weight) { }

    private static Map<Integer, Long> oracle(int n, List<Edge> edges) {
        BigInteger[] best = new BigInteger[n];
        best[0] = BigInteger.ZERO;
        BigInteger maximum = BigInteger.valueOf(Long.MAX_VALUE);
        for (int pass = 0; pass < n; pass++) {
            boolean changed = false;
            for (Edge edge : edges) {
                if (best[edge.from()] == null) continue;
                BigInteger path = best[edge.from()].add(BigInteger.valueOf(edge.weight()));
                if (path.compareTo(maximum) > 0) continue;
                if (best[edge.to()] == null || path.compareTo(best[edge.to()]) < 0) {
                    best[edge.to()] = path;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        Map<Integer, Long> results = new HashMap<>();
        for (int vertex = 0; vertex < n; vertex++)
            if (best[vertex] != null) results.put(vertex, best[vertex].longValueExact());
        return results;
    }

    private static Map<Integer, List<GraphAlgorithms.WeightedEdge<Integer>>> graph(
            int n, List<Edge> edges) {
        Map<Integer, List<GraphAlgorithms.WeightedEdge<Integer>>> adjacency =
                new LinkedHashMap<>();
        for (int vertex = 0; vertex < n; vertex++)
            adjacency.put(vertex, new ArrayList<>());
        for (Edge edge : edges)
            adjacency.get(edge.from()).add(
                    new GraphAlgorithms.WeightedEdge<>(edge.to(), edge.weight()));
        return adjacency;
    }

    private static void check(int n, List<Edge> edges) {
        assertEquals(oracle(n, edges),
                GraphAlgorithms.dijkstra(graph(n, edges), 0, ProgressMonitor.none()));
    }

    @Test
    void reachableMaximumIsNotTheSameAsMissingVertex() {
        check(2, List.of(new Edge(0, 1, Long.MAX_VALUE)));
        check(3, List.of(new Edge(0, 1, Long.MAX_VALUE), new Edge(1, 2, 0)));
        check(3, List.of(new Edge(0, 1, Long.MAX_VALUE - 1), new Edge(1, 2, 1)));
        check(4, List.of(new Edge(0, 1, Long.MAX_VALUE), new Edge(1, 2, 0),
                new Edge(2, 3, 0)));
        check(3, List.of(new Edge(0, 1, Long.MAX_VALUE), new Edge(0, 2, 4),
                new Edge(2, 1, 2)));
        check(2, List.of(new Edge(0, 1, Long.MAX_VALUE), new Edge(0, 1, 3)));
        assertEquals(Map.of(0, 0L),
                GraphAlgorithms.dijkstra(graph(3, List.of()), 0, ProgressMonitor.none()));
    }

    @Test
    void newlyReachedMaximumMustNotIntroduceAnOverflowException() {
        List<Edge> afterMax = List.of(new Edge(0, 1, Long.MAX_VALUE),
                new Edge(1, 2, 1));
        assertEquals(Map.of(0, 0L, 1, Long.MAX_VALUE),
                GraphAlgorithms.dijkstra(graph(3, afterMax), 0, ProgressMonitor.none()));
        List<Edge> preexistingOverflow = List.of(new Edge(0, 1, Long.MAX_VALUE - 1),
                new Edge(1, 2, 2));
        assertThrows(ArithmeticException.class, () ->
                GraphAlgorithms.dijkstra(graph(3, preexistingOverflow), 0,
                        ProgressMonitor.none()));
        assertThrows(IllegalArgumentException.class, () ->
                new GraphAlgorithms.WeightedEdge<>(2, -1L));
    }

    @Test
    void seededSmallGraphsMatchIndependentBellmanFord() {
        Random random = new Random(0x5e21743L);
        for (int trial = 0; trial < 8_000; trial++) {
            int n = 1 + random.nextInt(8);
            List<Edge> edges = new ArrayList<>();
            for (int from = 0; from < n; from++)
                for (int to = 0; to < n; to++)
                    if (from != to && random.nextInt(4) == 0)
                        edges.add(new Edge(from, to, random.nextInt(100)));
            check(n, edges);
        }
    }

    @Test
    void lifecycleAndEarlyCancellationStillApply() {
        class CountingMonitor implements ProgressMonitor {
            int begins, worked, done;
            boolean canceled;
            @Override public boolean isCanceled() { return canceled; }
            @Override public void beginTask(String name, int work) {
                begins++; assertEquals("dijkstra", name);
            }
            @Override public void worked(int work) { worked += work; }
            @Override public void done() { done++; }
        }
        CountingMonitor normal = new CountingMonitor();
        assertEquals(Map.of(0, 0L, 1, Long.MAX_VALUE),
                GraphAlgorithms.dijkstra(graph(2,
                        List.of(new Edge(0, 1, Long.MAX_VALUE))), 0, normal));
        assertEquals(1, normal.begins);
        assertEquals(2, normal.worked);
        assertEquals(1, normal.done);
        CountingMonitor canceled = new CountingMonitor();
        canceled.canceled = true;
        assertThrows(CancellationException.class, () ->
                GraphAlgorithms.dijkstra(graph(2, List.of()), 0, canceled));
        assertEquals(0, canceled.begins);
        assertEquals(0, canceled.done);
    }
}
