package jdk.internal.m3.collections;

import jdk.internal.m3.collections.graph.IntBipartiteMatcher;
import jdk.m3.collections.graph.IntLongAdjacencyGraph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FuzzyAlgorithmFactoryWave2Test {

    private final FuzzyAlgorithmFactory factory = FuzzyAlgorithmFactory.standard();

    @Test
    void graphAnalysisSpanningFlowAndMatchingSelectExactKernels() {
        AlgorithmRequirements topo =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.GRAPH_ANALYSIS)
                        .graphAnalysis(
                                100_000,
                                300_000,
                                AlgorithmRequirements.GraphAnalysisMode.TOPOLOGICAL_ORDER)
                        .build();
        assertEquals("topological-order", factory.select(topo).candidateId());

        AlgorithmRequirements scc =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.GRAPH_ANALYSIS)
                        .graphAnalysis(
                                100_000,
                                300_000,
                                AlgorithmRequirements.GraphAnalysisMode.STRONGLY_CONNECTED_COMPONENTS)
                        .build();
        assertEquals("scc-kosaraju", factory.select(scc).candidateId());

        AlgorithmRequirements msf =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.SPANNING_FOREST)
                        .spanningForest(100_000, 500_000)
                        .build();
        assertEquals("kruskal-msf", factory.select(msf).candidateId());

        AlgorithmRequirements flow =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MAX_FLOW)
                        .flow(100_000, 500_000)
                        .build();
        assertEquals("push-relabel-max-flow", factory.select(flow).candidateId());

        AlgorithmRequirements matching =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MATCHING)
                        .matching(50_000, 60_000, 200_000)
                        .build();
        assertEquals("hopcroft-karp", factory.select(matching).candidateId());
    }

    @Test
    void treeSelectionUsesRealMemoryVsQueryTradeoff() {
        long vertices = 100_000L;
        long edges = (vertices - 1L) * 2L;

        AlgorithmRequirements queryFirst =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TREE_INDEX)
                        .tree(vertices, edges, AlgorithmRequirements.TreeMode.LCA_DISTANCE)
                        .items(0, 1_000_000, 0)
                        .priorities(AlgorithmRequirements.Priorities.queryFirst())
                        .build();
        assertEquals("binary-lifting-lca", factory.select(queryFirst).candidateId());

        AlgorithmRequirements memoryFirst =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TREE_INDEX)
                        .tree(vertices, edges, AlgorithmRequirements.TreeMode.LCA_DISTANCE)
                        .items(0, 1_000_000, 0)
                        .priorities(AlgorithmRequirements.Priorities.memoryFirst())
                        .build();
        assertEquals("heavy-light-index", factory.select(memoryFirst).candidateId());

        AlgorithmRequirements pathSegments =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TREE_INDEX)
                        .tree(vertices, edges, AlgorithmRequirements.TreeMode.PATH_SEGMENTS_SUBTREE)
                        .build();
        assertEquals("heavy-light-index", factory.select(pathSegments).candidateId());
    }

    @Test
    void windowSelectionUsesQueryUpdateAndMemoryCharacteristics() {
        AlgorithmRequirements queryHeavyMax =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.WINDOW)
                        .window(
                                1_000,
                                1_000_000,
                                100_000,
                                AlgorithmRequirements.WindowMode.MAXIMUM)
                        .priorities(AlgorithmRequirements.Priorities.queryFirst())
                        .build();
        assertEquals("monotone-window-max", factory.select(queryHeavyMax).candidateId());

        AlgorithmRequirements updateHeavyMin =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.WINDOW)
                        .window(
                                1_000,
                                10,
                                1_000_000,
                                AlgorithmRequirements.WindowMode.MINIMUM)
                        .priorities(AlgorithmRequirements.Priorities.memoryFirst())
                        .build();
        assertEquals("sliding-window-extrema-scan", factory.select(updateHeavyMin).candidateId());

        AlgorithmRequirements sum =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.WINDOW)
                        .window(
                                1_000,
                                1_000_000,
                                1_000_000,
                                AlgorithmRequirements.WindowMode.SUM_AVERAGE)
                        .build();
        assertEquals("sliding-window-sum", factory.select(sum).candidateId());
    }

    @Test
    void compressionAndPartitionSearchAreFirstClassRequirements() {
        AlgorithmRequirements compression =
                AlgorithmRequirements.builder(
                                AlgorithmRequirements.Purpose.COORDINATE_COMPRESSION)
                        .compression(1_000_000)
                        .build();
        assertEquals("int-coordinate-compressor", factory.select(compression).candidateId());

        AlgorithmRequirements firstTrue =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MONOTONE_SEARCH)
                        .monotoneSearch(AlgorithmRequirements.SearchMode.FIRST_TRUE)
                        .build();
        assertEquals("long-partition-search", factory.select(firstTrue).candidateId());

        AlgorithmRequirements lastFalse =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MONOTONE_SEARCH)
                        .monotoneSearch(AlgorithmRequirements.SearchMode.LAST_FALSE)
                        .build();
        assertEquals("long-partition-search", factory.select(lastFalse).candidateId());
    }

    @Test
    void dispatchHelpersUseSelectedNativeKernels() {
        IntLongAdjacencyGraph dag = new IntLongAdjacencyGraph(4, 4);
        dag.addDirected(0, 1, 1);
        dag.addDirected(0, 2, 1);
        dag.addDirected(1, 3, 1);
        dag.addDirected(2, 3, 1);
        AlgorithmSelection topoSelection =
                factory.select(
                        AlgorithmRequirements.builder(
                                        AlgorithmRequirements.Purpose.GRAPH_ANALYSIS)
                                .graphAnalysis(
                                        4,
                                        4,
                                        AlgorithmRequirements.GraphAnalysisMode.TOPOLOGICAL_ORDER)
                                .build());
        int[] order = AlgorithmKernels.topologicalOrder(topoSelection, dag);
        assertEquals(4, order.length);

        IntLongAdjacencyGraph sccGraph = new IntLongAdjacencyGraph(4, 5);
        sccGraph.addDirected(0, 1, 1);
        sccGraph.addDirected(1, 0, 1);
        sccGraph.addDirected(1, 2, 1);
        sccGraph.addDirected(2, 3, 1);
        sccGraph.addDirected(3, 2, 1);
        AlgorithmSelection sccSelection =
                factory.select(
                        AlgorithmRequirements.builder(
                                        AlgorithmRequirements.Purpose.GRAPH_ANALYSIS)
                                .graphAnalysis(
                                        4,
                                        5,
                                        AlgorithmRequirements.GraphAnalysisMode.STRONGLY_CONNECTED_COMPONENTS)
                                .build());
        assertEquals(
                2,
                AlgorithmKernels.stronglyConnectedComponents(sccSelection, sccGraph)
                        .componentCount());

        AlgorithmSelection matchingSelection =
                factory.select(
                        AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MATCHING)
                                .matching(3, 3, 5)
                                .build());
        IntBipartiteMatcher matcher =
                AlgorithmKernels.createBipartiteMatcher(
                        matchingSelection,
                        3,
                        3,
                        new int[] {0, 0, 1, 1, 2},
                        new int[] {0, 1, 1, 2, 0});
        assertEquals(3, matcher.maximumMatching());

        AlgorithmSelection compressionSelection =
                factory.select(
                        AlgorithmRequirements.builder(
                                        AlgorithmRequirements.Purpose.COORDINATE_COMPRESSION)
                                .compression(6)
                                .build());
        IntCoordinateCompressor compressor =
                AlgorithmKernels.createCoordinateCompressor(
                        compressionSelection,
                        new int[] {9, -4, 9, 2, -4, 100});
        assertArrayEquals(new int[] {-4, 2, 9, 100}, compressor.dictionary());
    }

    @Test
    void partitionSearchHandlesEntireSignedLongDomainWithoutOverflow() {
        long threshold = 1_234_567_890_123_456_789L;

        assertEquals(
                threshold,
                LongPartitionSearch.firstTrue(
                        Long.MIN_VALUE,
                        Long.MAX_VALUE,
                        value -> value >= threshold));
        assertEquals(
                threshold - 1L,
                LongPartitionSearch.lastFalse(
                        Long.MIN_VALUE,
                        Long.MAX_VALUE,
                        value -> value >= threshold));

        assertEquals(
                Long.MIN_VALUE,
                LongPartitionSearch.firstTrue(
                        Long.MIN_VALUE,
                        Long.MAX_VALUE,
                        value -> true));
        assertEquals(
                Long.MAX_VALUE,
                LongPartitionSearch.lastFalse(
                        Long.MIN_VALUE,
                        Long.MAX_VALUE,
                        value -> false));

        assertThrows(
                NoSuchElementException.class,
                () -> LongPartitionSearch.firstTrue(0L, 100L, value -> false));
        assertThrows(
                NoSuchElementException.class,
                () -> LongPartitionSearch.lastFalse(0L, 100L, value -> true));
    }

    @Test
    void partitionSearchMatchesLinearOracleOnSmallRandomIntervals() {
        Random random = new Random(0xA11CE5EEDL);
        for (int round = 0; round < 10_000; round++) {
            long low = random.nextInt(2_000) - 1_000L;
            long high = low + random.nextInt(200);
            long threshold = low + random.nextInt((int) (high - low + 1L));

            long actual =
                    LongPartitionSearch.firstTrue(low, high, value -> value >= threshold);
            assertEquals(threshold, actual);

            if (threshold > low) {
                assertEquals(
                        threshold - 1L,
                        LongPartitionSearch.lastFalse(low, high, value -> value >= threshold));
            }
        }
    }

    @Test
    void hardContractsRejectImpossibleScaleAndWrongTreeShape() {
        AlgorithmRequirements tooManyEdges =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.MAX_FLOW)
                        .flow(10, (long) Integer.MAX_VALUE)
                        .build();
        assertThrows(IllegalArgumentException.class, () -> factory.select(tooManyEdges));

        AlgorithmRequirements notATree =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.TREE_INDEX)
                        .tree(10, 10, AlgorithmRequirements.TreeMode.LCA_DISTANCE)
                        .build();
        assertThrows(IllegalArgumentException.class, () -> factory.select(notATree));
    }
}
