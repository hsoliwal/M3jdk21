package jdk.internal.m3.collections;

import jdk.internal.m3.collections.graph.IntLongBellmanFord;
import jdk.internal.m3.collections.range.LongPrefixSums;
import jdk.m3.collections.graph.IntLongAdjacencyGraph;
import jdk.m3.collections.text.IntKmpPattern;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FuzzyAlgorithmFactoryTest {

    private final FuzzyAlgorithmFactory factory = FuzzyAlgorithmFactory.standard();

    @Test
    void shortestPathSelectionFollowsWeightLawExactly() {
        assertShortest(AlgorithmRequirements.GraphWeightLaw.UNWEIGHTED, "bfs");
        assertShortest(AlgorithmRequirements.GraphWeightLaw.ZERO_ONE, "zero-one-bfs");
        assertShortest(AlgorithmRequirements.GraphWeightLaw.NON_NEGATIVE, "dijkstra");
        assertShortest(AlgorithmRequirements.GraphWeightLaw.SIGNED, "bellman-ford");
    }

    @Test
    void rangeSelectionTracksMutationAndAlgebra() {
        AlgorithmRequirements immutableSum =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 1_000_000, 0)
                        .range(
                                AlgorithmRequirements.RangeAggregation.SUM,
                                AlgorithmRequirements.RangeMutation.IMMUTABLE,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .priorities(AlgorithmRequirements.Priorities.queryFirst())
                        .build();
        assertEquals("prefix-sums", factory.select(immutableSum).candidateId());

        AlgorithmRequirements pointSum =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 100_000, 100_000)
                        .range(
                                AlgorithmRequirements.RangeAggregation.SUM,
                                AlgorithmRequirements.RangeMutation.POINT_UPDATE,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .build();
        assertEquals("fenwick", factory.select(pointSum).candidateId());

        AlgorithmRequirements immutableIdempotent =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 1_000_000, 0)
                        .range(
                                AlgorithmRequirements.RangeAggregation.IDEMPOTENT,
                                AlgorithmRequirements.RangeMutation.IMMUTABLE,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .priorities(AlgorithmRequirements.Priorities.queryFirst())
                        .build();
        assertEquals("sparse-table", factory.select(immutableIdempotent).candidateId());

        AlgorithmRequirements genericPoint =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 100_000, 100_000)
                        .range(
                                AlgorithmRequirements.RangeAggregation.ASSOCIATIVE,
                                AlgorithmRequirements.RangeMutation.POINT_UPDATE,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .build();
        assertEquals("segment-tree", factory.select(genericPoint).candidateId());

        AlgorithmRequirements rangeAdd =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 100_000, 100_000)
                        .range(
                                AlgorithmRequirements.RangeAggregation.SUM,
                                AlgorithmRequirements.RangeMutation.RANGE_ADD,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .build();
        assertEquals("range-add-sum-tree", factory.select(rangeAdd).candidateId());

        AlgorithmRequirements persistent =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(100_000, 100_000, 50_000)
                        .range(
                                AlgorithmRequirements.RangeAggregation.SUM,
                                AlgorithmRequirements.RangeMutation.VERSIONED_POINT,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .build();
        assertEquals("persistent-sum-tree", factory.select(persistent).candidateId());
    }

    @Test
    void connectivityTextAndRankFamiliesSelectNativeKernels() {
        AlgorithmRequirements online =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.CONNECTIVITY)
                        .items(100_000, 100_000, 100_000)
                        .connectivity(AlgorithmRequirements.ConnectivityMode.ONLINE)
                        .build();
        assertEquals("disjoint-set", factory.select(online).candidateId());

        AlgorithmRequirements rollback =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.CONNECTIVITY)
                        .items(100_000, 100_000, 100_000)
                        .connectivity(AlgorithmRequirements.ConnectivityMode.ROLLBACK)
                        .build();
        assertEquals("rollback-disjoint-set", factory.select(rollback).candidateId());

        AlgorithmRequirements single =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TEXT_SEARCH)
                        .patterns(1, 32, 1_000)
                        .text(AlgorithmRequirements.TextMode.SINGLE_PATTERN)
                        .build();
        assertEquals("kmp", factory.select(single).candidateId());

        AlgorithmRequirements multi =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TEXT_SEARCH)
                        .patterns(500, 10_000, 1_000)
                        .text(AlgorithmRequirements.TextMode.MULTI_PATTERN_STREAM)
                        .build();
        assertEquals("aho-corasick", factory.select(multi).candidateId());

        AlgorithmRequirements staticText =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TEXT_SEARCH)
                        .items(1_000_000, 100_000, 0)
                        .patterns(0, 0, 100_000)
                        .text(AlgorithmRequirements.TextMode.STATIC_TEXT_INDEX)
                        .build();
        assertEquals("suffix-array", factory.select(staticText).candidateId());

        AlgorithmRequirements staticRank =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANK_QUANTILE)
                        .items(1_000_000, 100_000, 0)
                        .rank(AlgorithmRequirements.RankMode.STATIC_RANGE)
                        .build();
        assertEquals("wavelet-matrix", factory.select(staticRank).candidateId());

        AlgorithmRequirements dynamicRank =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANK_QUANTILE)
                        .items(100_000, 100_000, 100_000)
                        .rank(AlgorithmRequirements.RankMode.DYNAMIC_MULTISET)
                        .build();
        assertEquals("order-statistic-multiset", factory.select(dynamicRank).candidateId());
    }

    @Test
    void prefixSumsMatchNaiveRanges() {
        Random random = new Random(0x51aL);
        long[] values = new long[1_000];
        for (int i = 0; i < values.length; i++) values[i] = random.nextInt(2_001) - 1_000L;

        LongPrefixSums prefix = new LongPrefixSums(values);
        for (int q = 0; q < 20_000; q++) {
            int a = random.nextInt(values.length + 1);
            int b = random.nextInt(values.length + 1);
            int from = Math.min(a, b);
            int to = Math.max(a, b);
            long expected = 0L;
            for (int i = from; i < to; i++) expected += values[i];
            assertEquals(expected, prefix.rangeSum(from, to));
        }
        assertEquals(values.length, prefix.size());
        assertEquals(Arrays.stream(values).sum(), prefix.total());
    }

    @Test
    void kmpMatchesNaiveIncludingOverlapsAndEmptyPattern() {
        int[] text = {1, 2, 1, 2, 1, 2, 1};
        IntKmpPattern pattern = new IntKmpPattern(new int[] {1, 2, 1});

        assertEquals(0, pattern.indexOf(text));
        assertEquals(3, pattern.count(text));

        List<Integer> starts = new ArrayList<>();
        pattern.scan(text, (start, end) -> starts.add(start));
        assertEquals(List.of(0, 2, 4), starts);

        IntKmpPattern empty = new IntKmpPattern(new int[0]);
        assertEquals(0, empty.indexOf(text));
        assertEquals(text.length + 1, empty.count(text));
    }

    @Test
    void bellmanFordHandlesSignedWeightsAndNegativeCycleReachability() {
        IntLongAdjacencyGraph graph = new IntLongAdjacencyGraph(5, 8);
        graph.addDirected(0, 1, 4);
        graph.addDirected(0, 2, 5);
        graph.addDirected(1, 2, -2);
        graph.addDirected(2, 3, 3);
        graph.addDirected(1, 3, 8);

        IntLongBellmanFord.Result result = IntLongBellmanFord.shortestPaths(graph, 0);
        assertFalse(result.negativeCycleReachable());
        assertArrayEquals(new long[] {0, 4, 2, 5, Long.MAX_VALUE}, result.copyDistances());
        assertArrayEquals(new int[] {0, 1, 2, 3}, result.pathTo(3));

        IntLongAdjacencyGraph cycle = new IntLongAdjacencyGraph(6, 8);
        cycle.addDirected(0, 1, 1);
        cycle.addDirected(1, 2, 1);
        cycle.addDirected(2, 1, -3);
        cycle.addDirected(2, 3, 1);
        cycle.addDirected(4, 5, -2);
        cycle.addDirected(5, 4, 1);

        IntLongBellmanFord.Result affected = IntLongBellmanFord.shortestPaths(cycle, 0);
        assertTrue(affected.negativeCycleReachable());
        assertFalse(affected.isNegativeCycleAffected(0));
        assertTrue(affected.isNegativeCycleAffected(1));
        assertTrue(affected.isNegativeCycleAffected(2));
        assertTrue(affected.isNegativeCycleAffected(3));
        assertFalse(affected.isNegativeCycleAffected(4), "unreachable negative cycle must not leak");
        assertFalse(affected.isNegativeCycleAffected(5));
        assertThrows(IllegalStateException.class, () -> affected.finiteDistanceTo(3));
    }

    @Test
    void oneShotShortestPathDispatchReturnsUnifiedSentinels() {
        IntLongAdjacencyGraph graph = new IntLongAdjacencyGraph(4, 4);
        graph.addDirected(0, 1, 1);
        graph.addDirected(1, 2, 1);

        AlgorithmRequirements requirements =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.SHORTEST_PATH)
                        .graph(4, 2, AlgorithmRequirements.GraphWeightLaw.UNWEIGHTED)
                        .build();
        AlgorithmKernels.ShortestPathResult result =
                AlgorithmKernels.shortestPaths(requirements, graph, 0);

        assertEquals("bfs", result.algorithmId());
        assertEquals(0L, result.distanceTo(0));
        assertEquals(2L, result.distanceTo(2));
        assertEquals(Long.MAX_VALUE, result.distanceTo(3));
        assertFalse(result.hasPathTo(3));
    }

    @Test
    void pinnedCandidateStillChecksHardSemantics() {
        AlgorithmRequirements signed =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.SHORTEST_PATH)
                        .graph(10, 20, AlgorithmRequirements.GraphWeightLaw.SIGNED)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.selectPinned("dijkstra", signed));
        assertEquals("bellman-ford", factory.selectPinned("bellman-ford", signed).candidateId());
    }

    private void assertShortest(
            AlgorithmRequirements.GraphWeightLaw law,
            String expected) {
        AlgorithmRequirements requirements =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.SHORTEST_PATH)
                        .graph(100_000, 300_000, law)
                        .build();
        assertEquals(expected, factory.select(requirements).candidateId());
    }
}
