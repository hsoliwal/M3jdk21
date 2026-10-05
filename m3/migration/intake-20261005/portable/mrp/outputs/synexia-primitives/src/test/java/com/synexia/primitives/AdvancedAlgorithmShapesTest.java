package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvancedAlgorithmShapesTest {

    @Test
    void rangeAndPersistentShapes() {
        LongSegmentTree segment = new LongSegmentTree(new long[] {1, 2, 3, 4}, 0L, Long::sum);
        assertEquals(9L, segment.query(1, 4));
        segment.set(2, 10);
        long[] afterSet = {1, 2, 10, 4};
        assertEquals(Arrays.stream(afterSet).sum(), segment.query(0, 4));
        for (int index = 0; index < afterSet.length; index++) {
            assertEquals(afterSet[index], segment.get(index));
        }

        RangeAddLongSumTree lazy = new RangeAddLongSumTree(new long[] {1, 2, 3, 4});
        lazy.add(1, 4, 5);
        long[] afterRangeAdd = {1, 7, 8, 9};
        assertEquals(Arrays.stream(afterRangeAdd).sum(), lazy.total());
        assertEquals(Arrays.stream(afterRangeAdd, 1, 3).sum(), lazy.sum(1, 3));
        for (int index = 0; index < afterRangeAdd.length; index++) {
            assertEquals(afterRangeAdd[index], lazy.get(index));
        }

        LongSparseTable sparse = new LongSparseTable(new long[] {7, 2, 9, 1, 5}, Math::min);
        assertEquals(1L, sparse.query(1, 5));

        PersistentLongSumTree persistent = new PersistentLongSumTree(8);
        int v1 = persistent.set(0, 3, 10);
        int v2 = persistent.set(v1, 4, 7);
        int branch = persistent.set(v1, 3, 99);
        assertEquals(0L, persistent.total(0));
        assertEquals(10L, persistent.total(v1));
        assertEquals(17L, persistent.total(v2));
        assertEquals(99L, persistent.total(branch));
    }

    @Test
    void rankQuantileAndTextIndexes() {
        FrozenRankSelect rank = FrozenRankSelect.fromBooleans(
                new boolean[] {true, false, true, true, false});
        assertEquals(3, rank.rank1(4));
        assertEquals(2, rank.select1(1));
        assertEquals(4, rank.select0(1));

        IntWaveletMatrix wavelet = new IntWaveletMatrix(new int[] {5, -1, 7, 5, 2});
        assertEquals(2, wavelet.kthSmallest(0, 5, 1));
        assertEquals(2, wavelet.frequency(0, 5, 5));
        assertEquals(2, wavelet.countLessThan(0, 5, 5));

        LongOrderStatisticMultiset multiset = new LongOrderStatisticMultiset();
        multiset.add(10, 2);
        multiset.add(3);
        multiset.add(7, 3);
        assertEquals(1L, multiset.rank(7));
        assertEquals(7L, multiset.select(3));
        assertEquals(3L, multiset.count(7));

        IntSuffixArrayIndex suffix = new IntSuffixArrayIndex(new int[] {1, 2, 1, 2, 1});
        assertEquals(2, suffix.countOccurrences(new int[] {1, 2, 1}));

        IntAhoCorasick aho = IntAhoCorasick.compile(new int[][] {{1, 2}, {2}, {1, 2, 3}});
        List<String> hits = new ArrayList<>();
        aho.scan(new int[] {1, 2, 3}, (id, from, to) -> hits.add(id + ":" + from + ":" + to));
        assertTrue(hits.contains("0:0:2"));
        assertTrue(hits.contains("1:1:2"));
        assertTrue(hits.contains("2:0:3"));
    }

    @Test
    void graphKernels() {
        IntLongAdjacencyGraph graph = new IntLongAdjacencyGraph(5);
        graph.addDirected(0, 1, 2);
        graph.addDirected(1, 2, 3);
        graph.addDirected(0, 2, 10);
        graph.addDirected(2, 3, 1);
        graph.addDirected(3, 4, 4);
        assertArrayEquals(new long[] {0, 2, 5, 6, 10}, IntLongShortestPaths.dijkstra(graph, 0));

        IntLongAdjacencyGraph sccGraph = new IntLongAdjacencyGraph(4);
        sccGraph.addDirected(0, 1, 1);
        sccGraph.addDirected(1, 0, 1);
        sccGraph.addDirected(1, 2, 1);
        sccGraph.addDirected(2, 3, 1);
        sccGraph.addDirected(3, 2, 1);
        IntGraphAlgorithms.SccResult scc = IntGraphAlgorithms.stronglyConnectedComponents(sccGraph);
        assertEquals(2, scc.componentCount());
        assertEquals(scc.componentOf(0), scc.componentOf(1));
        assertEquals(scc.componentOf(2), scc.componentOf(3));
        assertNotEquals(scc.componentOf(0), scc.componentOf(2));

        IntBipartiteMatcher matcher = new IntBipartiteMatcher(
                3, 3,
                new int[] {0, 0, 1, 1, 2},
                new int[] {0, 1, 1, 2, 0});
        assertEquals(3, matcher.maximumMatching());

        IntLongAdjacencyGraph tree = new IntLongAdjacencyGraph(5, 8);
        tree.addUndirected(0, 1, 1);
        tree.addUndirected(0, 2, 1);
        tree.addUndirected(1, 3, 1);
        tree.addUndirected(1, 4, 1);
        IntHeavyLightIndex hld = new IntHeavyLightIndex(tree, 0);
        assertEquals(1, hld.lca(3, 4));
        assertEquals(0, hld.lca(3, 2));
        assertEquals(3, hld.subtreeSize(1));

        boolean[] path = new boolean[5];
        hld.forEachPathSegment(3, 2, (from, to) -> {
            for (int position = from; position < to; position++) {
                path[hld.vertexAtPosition(position)] = true;
            }
        });
        assertArrayEquals(new boolean[] {true, true, true, true, false}, path);
    }

    @Test
    void seriousGraphCompressionAndFlowKernels() {
        IntCoordinateCompressor compressor =
                new IntCoordinateCompressor(new int[] {9, -4, 9, 2, -4, 100});
        assertArrayEquals(new int[] {-4, 2, 9, 100}, compressor.dictionary());
        assertEquals(2, compressor.rankOf(9));
        assertEquals(3, compressor.lowerBound(10));

        IntLongAdjacencyGraph zeroOne = new IntLongAdjacencyGraph(4);
        zeroOne.addDirected(0, 1, 0);
        zeroOne.addDirected(0, 2, 1);
        zeroOne.addDirected(1, 2, 0);
        zeroOne.addDirected(2, 3, 1);
        assertArrayEquals(new int[] {0, 0, 0, 1}, IntZeroOneShortestPaths.distances(zeroOne, 0));

        IntMinimumSpanningForest.Result forest =
                IntMinimumSpanningForest.kruskal(
                        4,
                        new int[] {0, 1, 2, 0, 0},
                        new int[] {1, 2, 3, 3, 2},
                        new long[] {1, 2, 3, 10, 4});
        assertEquals(6L, forest.totalWeight());
        assertEquals(1, forest.componentCount());
        assertEquals(3, forest.edgeCount());

        IntLongAdjacencyGraph tree = new IntLongAdjacencyGraph(7, 12);
        tree.addUndirected(0, 1, 1);
        tree.addUndirected(0, 2, 1);
        tree.addUndirected(1, 3, 1);
        tree.addUndirected(1, 4, 1);
        tree.addUndirected(2, 5, 1);
        tree.addUndirected(5, 6, 1);
        IntBinaryLiftingLca lca = new IntBinaryLiftingLca(tree, 0);
        assertEquals(1, lca.lca(3, 4));
        assertEquals(0, lca.lca(3, 6));
        assertEquals(5, lca.distanceEdges(4, 6));
        assertEquals(0, lca.kthAncestor(6, 3));

        LongPushRelabelMaxFlow flow = new LongPushRelabelMaxFlow(6, 10);
        flow.addEdge(0, 1, 16);
        flow.addEdge(0, 2, 13);
        flow.addEdge(1, 2, 10);
        flow.addEdge(2, 1, 4);
        flow.addEdge(1, 3, 12);
        flow.addEdge(3, 2, 9);
        flow.addEdge(2, 4, 14);
        flow.addEdge(4, 3, 7);
        flow.addEdge(3, 5, 20);
        flow.addEdge(4, 5, 4);
        assertEquals(23L, flow.maxFlow(0, 5));
        assertEquals(23L, flow.flowValue());
        boolean[] cut = flow.minCutReachable();
        assertTrue(cut[0]);
        assertFalse(cut[5]);
        assertThrows(IllegalStateException.class, () -> flow.maxFlow(0, 5));
    }

    @Test
    void rollbackAndMonotoneShapes() {
        IntRollbackDisjointSet dsu = new IntRollbackDisjointSet(6);
        dsu.union(0, 1);
        int snapshot = dsu.snapshot();
        dsu.union(1, 2);
        assertTrue(dsu.connected(0, 2));
        dsu.rollback(snapshot);
        assertFalse(dsu.connected(0, 2));

        MonotoneLongDeque deque = new MonotoneLongDeque(4, MonotoneLongDeque.Mode.MAX);
        deque.slide(0, 2, 0);
        deque.slide(1, 7, 0);
        deque.slide(2, 4, 0);
        assertEquals(7L, deque.firstValue());
        deque.slide(3, 8, 1);
        assertEquals(8L, deque.firstValue());
    }

    @Test
    void denseFactoriesExposeTheShapes() {
        assertNotNull(DenseCollections.longOrderStatisticMultiset());
        assertEquals(3, DenseCollections.intWaveletMatrix(new int[] {3, 2, 1}).size());
        assertEquals(3, DenseCollections.intSuffixArrayIndex(new int[] {1, 2, 3}).length());
        assertEquals(1, DenseCollections.intAhoCorasick(new int[][] {{1}}).patternCount());
    }
}
