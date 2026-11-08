// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.m3.collections.graph.IntCsrGraph;
import jdk.m3.collections.graph.IntGraphTraversal;
import jdk.m3.collections.graph.WeightedIntCsrGraph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.graph.LongShortestPaths;

class PrimitiveGraphKernelsTest {
    @Test void weightedCsrPreservesStableArcsIdsAndSnapshot() {
        int[] from = {2, 0, 2, 1, 2}, to = {1, 2, 1, 1, 0};
        long[] weights = {7, -4, Long.MIN_VALUE, 0, Long.MAX_VALUE};
        for (boolean undirected : new boolean[]{false, true}) {
            WeightedIntCsrGraph graph = undirected ? WeightedIntCsrGraph.undirected(4, from, to, weights)
                    : WeightedIntCsrGraph.directed(4, from, to, weights);
            assertEquals(undirected, graph.isUndirected()); assertEquals(from.length, graph.inputEdges());
            assertEquals(from.length * (undirected ? 2 : 1), graph.arcs()); assertFalse(graph.hasOnlyNonnegativeWeights());
            int arc = 0;
            for (int vertex = 0; vertex < 4; vertex++) {
                assertEquals(arc, graph.topology().start(vertex));
                for (int edge = 0; edge < from.length; edge++) {
                    if (from[edge] == vertex) {
                        assertEquals(to[edge], graph.topology().target(arc));
                        assertEquals(edge, graph.inputEdgeId(arc)); assertEquals(weights[edge], graph.weight(arc++));
                    }
                    if (undirected && to[edge] == vertex) {
                        assertEquals(from[edge], graph.topology().target(arc));
                        assertEquals(edge, graph.inputEdgeId(arc)); assertEquals(weights[edge], graph.weight(arc++));
                    }
                }
                assertEquals(arc, graph.topology().end(vertex));
            }
        }
        WeightedIntCsrGraph snapshot = WeightedIntCsrGraph.directed(4, from, to, weights);
        from[1] = 3; to[1] = 3; weights[1] = 9;
        assertEquals(2, snapshot.topology().target(0)); assertEquals(-4, snapshot.weight(0));
        assertEquals(snapshot.topology().payloadBytes() + 12L * snapshot.arcs(), snapshot.payloadBytes());
        assertThrows(IllegalArgumentException.class, () -> WeightedIntCsrGraph.directed(1, new int[0], new int[1], new long[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> WeightedIntCsrGraph.directed(1, new int[]{1}, new int[]{0}, new long[]{1}));
    }

    @Test void traversalMatchesQueueAndRecursiveReferenceIncludingOrder() {
        Random random = new Random(0x5452415645525345L);
        for (int trial = 0; trial < 400; trial++) {
            int n = 1 + random.nextInt(14), edges = random.nextInt(50);
            int[] from = new int[edges], to = new int[edges]; List<List<Integer>> adjacent = new ArrayList<>();
            for (int v = 0; v < n; v++) { adjacent.add(new ArrayList<>()); }
            for (int e = 0; e < edges; e++) {
                from[e] = random.nextInt(n); to[e] = random.nextInt(n); adjacent.get(from[e]).add(to[e]);
            }
            IntGraphTraversal traversal = new IntGraphTraversal(IntCsrGraph.fromEdges(n, from, to));
            for (int source = 0; source < n; source++) {
                int[] distances = new int[n], parents = new int[n]; Arrays.fill(distances, -1); Arrays.fill(parents, -1);
                ArrayDeque<Integer> queue = new ArrayDeque<>(); List<Integer> expected = new ArrayList<>();
                queue.add(source); distances[source] = 0;
                while (!queue.isEmpty()) {
                    int v = queue.remove(); expected.add(v);
                    for (int target : adjacent.get(v)) {
                        if (distances[target] < 0) { parents[target] = v; distances[target] = distances[v] + 1; queue.add(target); }
                    }
                }
                assertEquals(expected.size(), traversal.breadthFirst(source));
                for (int i = 0; i < expected.size(); i++) { assertEquals(expected.get(i).intValue(), traversal.vertexAt(i)); }
                for (int v = 0; v < n; v++) { assertEquals(distances[v], traversal.distanceTo(v)); assertEquals(parents[v], traversal.parentOf(v)); }
                expected.clear(); List<Integer> post = new ArrayList<>(); Arrays.fill(distances, -1); Arrays.fill(parents, -1);
                referenceDfs(source, 0, adjacent, distances, parents, expected, post);
                assertEquals(expected.size(), traversal.depthFirst(source));
                for (int i = 0; i < expected.size(); i++) {
                    assertEquals(expected.get(i).intValue(), traversal.vertexAt(i));
                    assertEquals(post.get(i).intValue(), traversal.postorderAt(i));
                }
                for (int v = 0; v < n; v++) { assertEquals(distances[v], traversal.distanceTo(v)); assertEquals(parents[v], traversal.parentOf(v)); }
            }
        }
    }

    @Test void topologyMatchesCycleClosureAndEveryArcOrder() {
        Random random = new Random(0x544f504fL);
        for (int trial = 0; trial < 600; trial++) {
            int n = random.nextInt(15), edges = n == 0 ? 0 : random.nextInt(45);
            int[] from = new int[edges], to = new int[edges]; boolean[][] reach = new boolean[n][n];
            for (int e = 0; e < edges; e++) { from[e] = random.nextInt(n); to[e] = random.nextInt(n); reach[from[e]][to[e]] = true; }
            for (int k = 0; k < n; k++) { for (int i = 0; i < n; i++) { for (int j = 0; j < n; j++) { reach[i][j] |= reach[i][k] && reach[k][j]; } } }
            boolean cyclic = false; for (int v = 0; v < n; v++) { cyclic |= reach[v][v]; }
            IntGraphTraversal traversal = new IntGraphTraversal(IntCsrGraph.fromEdges(n, from, to));
            assertEquals(!cyclic, traversal.topologicalOrder());
            int[] rank = new int[n]; Arrays.fill(rank, -1);
            for (int i = 0; i < traversal.size(); i++) { int v = traversal.vertexAt(i); assertEquals(-1, rank[v]); rank[v] = i; }
            for (int e = 0; e < edges; e++) { if (rank[to[e]] >= 0) { assertTrue(rank[from[e]] >= 0 && rank[from[e]] < rank[to[e]]); } }
        }
    }

    @Test void traversalStateAndDeepGraphAreSafe() {
        int n = 120_000; int[] from = new int[n - 1], to = new int[n - 1];
        for (int e = 0; e < from.length; e++) { from[e] = e; to[e] = e + 1; }
        IntGraphTraversal traversal = new IntGraphTraversal(IntCsrGraph.fromEdges(n, from, to));
        assertThrows(IllegalStateException.class, traversal::size);
        assertEquals(n, traversal.depthFirst(0)); assertEquals(n - 1, traversal.distanceTo(n - 1));
        assertEquals(n - 1, traversal.postorderAt(0)); assertEquals(0, traversal.postorderAt(n - 1));
        assertEquals(1, traversal.breadthFirst(n - 1)); assertEquals(-1, traversal.distanceTo(0));
        assertThrows(IllegalStateException.class, () -> traversal.postorderAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> traversal.depthFirst(n));
        assertEquals(1, traversal.size()); assertTrue(traversal.topologicalOrder());
        assertThrows(IllegalStateException.class, () -> traversal.parentOf(0));
        IntGraphTraversal empty = new IntGraphTraversal(IntCsrGraph.fromEdges(0, new int[0], new int[0]));
        assertTrue(empty.topologicalOrder()); assertEquals(0, empty.size());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.breadthFirst(0));
    }

    @Test void dijkstraAndBellmanFordMatchFloydWarshall() {
        Random random = new Random(0x53484f5254L);
        for (int trial = 0; trial < 600; trial++) {
            int n = 1 + random.nextInt(9), edges = random.nextInt(35);
            int[] from = new int[edges], to = new int[edges]; long[] weights = new long[edges];
            boolean nonnegative = (trial & 1) == 0;
            long infinity = 1_000_000_000L; long[][] distance = new long[n][n];
            for (int i = 0; i < n; i++) { Arrays.fill(distance[i], infinity); distance[i][i] = 0; }
            for (int e = 0; e < edges; e++) {
                from[e] = random.nextInt(n); to[e] = random.nextInt(n); weights[e] = random.nextInt(15) - (nonnegative ? 0 : 5);
                distance[from[e]][to[e]] = Math.min(distance[from[e]][to[e]], weights[e]);
            }
            for (int k = 0; k < n; k++) { for (int i = 0; i < n; i++) { for (int j = 0; j < n; j++) {
                if (distance[i][k] != infinity && distance[k][j] != infinity) { distance[i][j] = Math.min(distance[i][j], distance[i][k] + distance[k][j]); }
            } } }
            LongShortestPaths paths = new LongShortestPaths(WeightedIntCsrGraph.directed(n, from, to, weights));
            for (int source = 0; source < n; source++) {
                boolean[] unbounded = new boolean[n]; boolean negativeCycle = false;
                for (int v = 0; v < n; v++) {
                    for (int k = 0; k < n; k++) { unbounded[v] |= distance[source][k] != infinity && distance[k][k] < 0 && distance[k][v] != infinity; }
                    negativeCycle |= unbounded[v];
                }
                assertEquals(!negativeCycle, paths.bellmanFord(source));
                checkDistances(paths, source, distance, infinity, unbounded, from, to, weights);
                if (nonnegative) { paths.dijkstra(source); checkDistances(paths, source, distance, infinity, unbounded, from, to, weights); }
            }
        }
    }

    @Test void shortestPathsHandleLongExtremesAndInvalidateOverflowedRuns() {
        LongShortestPaths positive = new LongShortestPaths(WeightedIntCsrGraph.directed(4, new int[]{0, 1}, new int[]{1, 2}, new long[]{Long.MAX_VALUE, 0}));
        assertThrows(IllegalStateException.class, () -> positive.reachable(0));
        positive.dijkstra(0); assertEquals(Long.MAX_VALUE, positive.distanceTo(2)); assertFalse(positive.reachable(3));
        assertThrows(IllegalStateException.class, () -> positive.distanceTo(3));
        assertTrue(positive.bellmanFord(0)); assertEquals(Long.MAX_VALUE, positive.distanceTo(1));
        LongShortestPaths negative = new LongShortestPaths(WeightedIntCsrGraph.directed(2, new int[]{0}, new int[]{1}, new long[]{Long.MIN_VALUE}));
        assertTrue(negative.bellmanFord(0)); assertEquals(Long.MIN_VALUE, negative.distanceTo(1));
        assertThrows(IllegalArgumentException.class, () -> negative.dijkstra(1));
        assertEquals(Long.MIN_VALUE, negative.distanceTo(1));
        LongShortestPaths overflow = new LongShortestPaths(WeightedIntCsrGraph.directed(3, new int[]{0, 1}, new int[]{1, 2}, new long[]{Long.MAX_VALUE, 1}));
        overflow.dijkstra(2); assertThrows(ArithmeticException.class, () -> overflow.dijkstra(0));
        assertThrows(IllegalStateException.class, () -> overflow.reachable(2));
        assertThrows(ArithmeticException.class, () -> overflow.bellmanFord(0));
        assertThrows(IllegalStateException.class, () -> overflow.distanceTo(0));
        overflow.dijkstra(2); assertEquals(0, overflow.distanceTo(2));
        LongShortestPaths underflow = new LongShortestPaths(WeightedIntCsrGraph.directed(3, new int[]{0, 1}, new int[]{1, 2}, new long[]{Long.MIN_VALUE, -1}));
        assertThrows(ArithmeticException.class, () -> underflow.bellmanFord(0));
        assertThrows(IllegalStateException.class, () -> underflow.reachable(0));
    }

    private static void referenceDfs(int v, int depth, List<List<Integer>> adjacent, int[] distance,
            int[] parent, List<Integer> pre, List<Integer> post) {
        distance[v] = depth; pre.add(v);
        for (int target : adjacent.get(v)) {
            if (distance[target] < 0) { parent[target] = v; referenceDfs(target, depth + 1, adjacent, distance, parent, pre, post); }
        }
        post.add(v);
    }
    private static void checkDistances(LongShortestPaths paths, int source, long[][] expected, long infinity,
            boolean[] unbounded, int[] from, int[] to, long[] weights) {
        for (int v = 0; v < expected.length; v++) {
            assertEquals(expected[source][v] != infinity, paths.reachable(v));
            assertEquals(unbounded[v], paths.negativelyUnbounded(v));
            if (expected[source][v] == infinity || unbounded[v]) {
                int vertex = v; assertThrows(IllegalStateException.class, () -> paths.distanceTo(vertex));
            } else {
                assertEquals(expected[source][v], paths.distanceTo(v));
                if (v == source) { assertEquals(-1, paths.parentOf(v)); assertEquals(-1, paths.parentInputEdge(v)); }
                else {
                    int edge = paths.parentInputEdge(v), parent = paths.parentOf(v);
                    assertEquals(v, to[edge]); assertEquals(parent, from[edge]);
                    assertEquals(paths.distanceTo(v), paths.distanceTo(parent) + weights[edge]);
                }
            }
        }
    }
}
