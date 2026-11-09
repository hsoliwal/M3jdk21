// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.graph.LongMinimumSpanningForest;
import jdk.m3.collections.graph.IntBipartiteMatching;
import jdk.m3.collections.graph.WeightedIntCsrGraph;

class AdvancedGraphStructuresTest {
    @Test void forestsMatchEnumerationOfEverySmallEdgeSubset() {
        Random random = new Random(0x4d535446L);
        for (int trial = 0; trial < 700; trial++) {
            int n = random.nextInt(7), edges = n == 0 ? 0 : random.nextInt(11);
            int[] from = new int[edges], to = new int[edges]; long[] weight = new long[edges];
            boolean[][] all = new boolean[n][n];
            for (int e = 0; e < edges; e++) {
                from[e] = random.nextInt(n); to[e] = random.nextInt(n); weight[e] = random.nextInt(19) - 9;
                all[from[e]][to[e]] = true; all[to[e]][from[e]] = true;
            }
            int[] groups = labels(all); int components = groupCount(groups); long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << edges); mask++) {
                if (Integer.bitCount(mask) != n - components) { continue; }
                boolean[][] chosen = new boolean[n][n]; long sum = 0;
                for (int e = 0; e < edges; e++) { if ((mask & (1 << e)) != 0) {
                    chosen[from[e]][to[e]] = true; chosen[to[e]][from[e]] = true; sum += weight[e];
                } }
                if (groupCount(labels(chosen)) == components) { best = Math.min(best, sum); }
            }
            var forest = new LongMinimumSpanningForest(WeightedIntCsrGraph.undirected(n, from, to, weight));
            assertEquals(best, forest.totalWeight()); assertEquals(components, forest.componentCount());
            assertEquals(n - components, forest.selectedEdgeCount());
            long[] componentWeights = new long[components]; boolean[][] chosen = new boolean[n][n];
            int previous = -1;
            for (int i = 0; i < forest.selectedEdgeCount(); i++) {
                int edge = forest.edgeAt(i); assertTrue(forest.containsEdge(edge));
                if (previous >= 0) { assertTrue(weight[previous] < weight[edge] || weight[previous] == weight[edge] && previous < edge); }
                previous = edge; componentWeights[groups[from[edge]]] += weight[edge];
                chosen[from[edge]][to[edge]] = true; chosen[to[edge]][from[edge]] = true;
            }
            assertArrayEquals(groups, labels(chosen));
            for (int v = 0; v < n; v++) { assertEquals(groups[v], forest.componentOf(v)); }
            for (int c = 0; c < components; c++) { assertEquals(componentWeights[c], forest.componentWeight(c)); }
            int selected = 0; for (int e = 0; e < edges; e++) { if (forest.containsEdge(e)) { selected++; } }
            assertEquals(forest.selectedEdgeCount(), selected);
        }
    }

    @Test void forestTiesExtremesAndRejectionContracts() {
        var ties = new LongMinimumSpanningForest(WeightedIntCsrGraph.undirected(4,
                new int[]{0, 1, 0, 2, 0}, new int[]{1, 2, 2, 3, 0}, new long[]{5, 5, 5, 5, -99}));
        assertEquals(0, ties.edgeAt(0)); assertEquals(1, ties.edgeAt(1)); assertEquals(3, ties.edgeAt(2));
        assertFalse(ties.containsEdge(4)); assertEquals(15, ties.totalWeight());
        var extreme = new LongMinimumSpanningForest(WeightedIntCsrGraph.undirected(2, new int[]{0}, new int[]{1}, new long[]{Long.MIN_VALUE}));
        assertEquals(Long.MIN_VALUE, extreme.totalWeight());
        assertThrows(ArithmeticException.class, () -> new LongMinimumSpanningForest(WeightedIntCsrGraph.undirected(3,
                new int[]{0, 1}, new int[]{1, 2}, new long[]{Long.MAX_VALUE, 1})));
        assertThrows(ArithmeticException.class, () -> new LongMinimumSpanningForest(WeightedIntCsrGraph.undirected(3,
                new int[]{0, 1}, new int[]{1, 2}, new long[]{Long.MIN_VALUE, -1})));
        assertThrows(IllegalArgumentException.class, () -> new LongMinimumSpanningForest(WeightedIntCsrGraph.directed(0, new int[0], new int[0], new long[0])));
        assertThrows(IndexOutOfBoundsException.class, () -> ties.edgeAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> ties.componentWeight(1));
    }

    @Test void matchingAndCoverMatchExhaustiveAssignment() {
        Random random = new Random(0x484f5043524f4654L);
        for (int trial = 0; trial < 1800; trial++) {
            int l = random.nextInt(7), r = random.nextInt(7), edges = l == 0 || r == 0 ? 0 : random.nextInt(28);
            int[] from = new int[edges], to = new int[edges]; boolean[][] adjacent = new boolean[l][r];
            for (int e = 0; e < edges; e++) { from[e] = random.nextInt(l); to[e] = random.nextInt(r); adjacent[from[e]][to[e]] = true; }
            var match = new IntBipartiteMatching(l, r, from, to);
            assertEquals(maximum(adjacent, 0, 0), match.cardinality());
            int matchedLeft = 0, matchedRight = 0, cover = 0;
            for (int a = 0; a < l; a++) {
                int b = match.partnerOfLeft(a);
                if (b >= 0) { matchedLeft++; assertTrue(adjacent[a][b]); assertEquals(a, match.partnerOfRight(b)); }
                if (match.leftInMinimumCover(a)) { cover++; }
            }
            for (int b = 0; b < r; b++) {
                if (match.partnerOfRight(b) >= 0) { matchedRight++; }
                if (match.rightInMinimumCover(b)) { cover++; }
            }
            assertEquals(match.cardinality(), matchedLeft); assertEquals(matchedLeft, matchedRight); assertEquals(cover, matchedLeft);
            for (int e = 0; e < edges; e++) { assertTrue(match.leftInMinimumCover(from[e]) || match.rightInMinimumCover(to[e])); }
        }
    }

    @Test void matchingUsesAnExplicitStackForDeepAlternatingPaths() {
        int n = 120_000; int[] from = new int[2 * n - 1], to = new int[from.length]; int at = 0;
        for (int vertex = 0; vertex < n - 1; vertex++) {
            from[at] = vertex; to[at++] = vertex; from[at] = vertex; to[at++] = vertex + 1;
        }
        from[at] = n - 1; to[at] = 0;
        var match = new IntBipartiteMatching(n, n, from, to);
        assertEquals(n, match.cardinality()); assertEquals(0, match.partnerOfLeft(n - 1));
        for (int vertex = 0; vertex < n - 1; vertex++) { assertEquals(vertex + 1, match.partnerOfLeft(vertex)); }
    }

    @Test void matchingValidatesAndRetainsOnlyResults() {
        int[] left = {0, 0, 1}, right = {0, 0, 1}; var match = new IntBipartiteMatching(2, 2, left, right);
        Arrays.fill(left, 100); Arrays.fill(right, 100); assertEquals(2, match.cardinality());
        assertEquals(0, match.partnerOfLeft(0)); assertEquals(1, match.partnerOfLeft(1));
        assertEquals(0, new IntBipartiteMatching(0, 5, new int[0], new int[0]).cardinality());
        assertThrows(IllegalArgumentException.class, () -> new IntBipartiteMatching(-1, 0, new int[0], new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new IntBipartiteMatching(1, 1, new int[0], new int[1]));
        assertThrows(IndexOutOfBoundsException.class, () -> new IntBipartiteMatching(1, 1, new int[]{0}, new int[]{1}));
    }

    private static int maximum(boolean[][] adjacent, int left, int used) {
        if (left == adjacent.length) { return 0; }
        int best = maximum(adjacent, left + 1, used);
        for (int right = 0; right < adjacent[left].length; right++) {
            if (adjacent[left][right] && (used & (1 << right)) == 0) { best = Math.max(best, 1 + maximum(adjacent, left + 1, used | (1 << right))); }
        }
        return best;
    }
    private static int[] labels(boolean[][] adjacent) {
        int[] result = new int[adjacent.length], queue = new int[result.length]; Arrays.fill(result, -1); int groups = 0;
        for (int start = 0; start < result.length; start++) {
            if (result[start] >= 0) { continue; }
            int head = 0, tail = 0; result[start] = groups; queue[tail++] = start;
            while (head < tail) {
                int vertex = queue[head++];
                for (int target = 0; target < result.length; target++) {
                    if (adjacent[vertex][target] && result[target] < 0) { result[target] = groups; queue[tail++] = target; }
                }
            }
            groups++;
        }
        return result;
    }
    private static int groupCount(int[] labels) { int groups = 0; for (int c : labels) { groups = Math.max(groups, c + 1); } return groups; }
}
