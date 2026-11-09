/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.graph.IntBipartiteMatcher;
import jdk.internal.m3.collections.graph.IntGraphAlgorithms;
import jdk.internal.m3.collections.graph.IntLongBellmanFord;
import jdk.internal.m3.collections.graph.IntZeroOneShortestPaths;
import jdk.internal.m3.collections.graph.LongPushRelabelMaxFlow;
import jdk.internal.m3.collections.range.LongPrefixSums;
import jdk.internal.m3.collections.text.IntSuffixArrayIndex;
import jdk.m3.collections.IntDisjointSet;
import jdk.m3.collections.IntRollbackDisjointSet;
import jdk.m3.collections.LongOrderStatisticMultiset;
import jdk.m3.collections.LongSlidingWindow;
import jdk.m3.collections.MonotoneLongDeque;
import jdk.m3.collections.graph.IntBinaryLiftingLca;
import jdk.m3.collections.graph.IntHeavyLightIndex;
import jdk.m3.collections.graph.IntLongAdjacencyGraph;
import jdk.m3.collections.graph.IntLongShortestPaths;
import jdk.m3.collections.graph.IntMinimumSpanningForest;
import jdk.m3.collections.range.LongFenwickTree;
import jdk.m3.collections.range.LongSegmentTree;
import jdk.m3.collections.range.LongSparseTable;
import jdk.m3.collections.range.PersistentLongSumTree;
import jdk.m3.collections.range.RangeAddLongSumTree;
import jdk.m3.collections.text.IntAhoCorasick;
import jdk.m3.collections.text.IntKmpPattern;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;

/**
 * Construction/execution helpers that dispatch once, then expose concrete kernels/results.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/AlgorithmKernels.java}
 * blob {@code 1a3b71bd176d0493b3bc34550f1077670aa28258} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives DenseCollections chain after API_UNION U7/U8 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AlgorithmKernels {

    public static final class ShortestPathResult {
        private final String algorithmId;
        private final long[] distance;
        private final boolean[] negativeCycleAffected;

        private ShortestPathResult(
                String algorithmId,
                long[] distance,
                boolean[] negativeCycleAffected) {
            this.algorithmId = algorithmId;
            this.distance = distance;
            this.negativeCycleAffected = negativeCycleAffected;
        }

        public String algorithmId() { return algorithmId; }
        public int vertexCount() { return distance.length; }

        public boolean hasPathTo(int vertex) {
            checkVertex(vertex);
            return distance[vertex] != Long.MAX_VALUE;
        }

        public long distanceTo(int vertex) {
            checkVertex(vertex);
            return distance[vertex];
        }

        public boolean isNegativeCycleAffected(int vertex) {
            checkVertex(vertex);
            return negativeCycleAffected[vertex];
        }

        public boolean negativeCycleReachable() {
            for (boolean affected : negativeCycleAffected) if (affected) return true;
            return false;
        }

        public long[] copyDistances() { return distance.clone(); }
        public boolean[] copyNegativeCycleAffected() { return negativeCycleAffected.clone(); }

        private void checkVertex(int vertex) {
            if (vertex < 0 || vertex >= distance.length) throw new IndexOutOfBoundsException(vertex);
        }
    }

    private AlgorithmKernels() {}

    public static FuzzyAlgorithmFactory factory() {
        return FuzzyAlgorithmFactory.standard();
    }

    public static ShortestPathResult shortestPaths(
            AlgorithmRequirements requirements,
            IntLongAdjacencyGraph graph,
            int source) {
        return shortestPaths(factory().select(requirements), graph, source);
    }

    public static ShortestPathResult shortestPaths(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph graph,
            int source) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(graph, "graph");
        if (selection.requirements().purpose() != AlgorithmRequirements.Purpose.SHORTEST_PATH) {
            throw new IllegalArgumentException("selection is not SHORTEST_PATH");
        }

        return switch (selection.candidateId()) {
            case "bfs" -> fromUnweighted(
                    "bfs",
                    IntGraphAlgorithms.bfsDistances(graph, source),
                    -1);
            case "zero-one-bfs" -> fromUnweighted(
                    "zero-one-bfs",
                    IntZeroOneShortestPaths.distances(graph, source),
                    Integer.MAX_VALUE);
            case "dijkstra" -> new ShortestPathResult(
                    "dijkstra",
                    IntLongShortestPaths.dijkstra(graph, source),
                    new boolean[graph.vertexCount()]);
            case "bellman-ford" -> {
                IntLongBellmanFord.Result result =
                        IntLongBellmanFord.shortestPaths(graph, source);
                yield new ShortestPathResult(
                        "bellman-ford",
                        result.copyDistances(),
                        result.copyNegativeCycleAffected());
            }
            default -> throw new IllegalArgumentException(
                    "unsupported shortest-path candidate: " + selection.candidateId());
        };
    }

    /**
     * Creates the selected non-persistent range index.
     *
     * <p>The returned value is the concrete native implementation named by the selection.
     */
    public static Object createRangeIndex(
            AlgorithmSelection selection,
            long[] values,
            long identity,
            LongBinaryOperator operation) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.RANGE_INDEX);
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(operation, "operation");

        return switch (selection.candidateId()) {
            case "prefix-sums" -> new LongPrefixSums(values);
            case "fenwick" -> fenwick(values);
            case "segment-tree" -> new LongSegmentTree(values, identity, operation);
            case "range-add-sum-tree" -> new RangeAddLongSumTree(values);
            case "sparse-table" -> new LongSparseTable(values, operation);
            case "persistent-sum-tree" -> throw new IllegalArgumentException(
                    "persistent selection requires createPersistentRangeIndex");
            default -> throw new IllegalArgumentException(
                    "unsupported range candidate: " + selection.candidateId());
        };
    }

    public static PersistentLongSumTree createPersistentRangeIndex(
            AlgorithmSelection selection,
            int size) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.RANGE_INDEX);
        if (!selection.candidateId().equals("persistent-sum-tree")) {
            throw new IllegalArgumentException("selection is not persistent-sum-tree");
        }
        return new PersistentLongSumTree(size);
    }

    /** Returns IntDisjointSet or IntRollbackDisjointSet according to the selection. */
    public static Object createConnectivityIndex(AlgorithmSelection selection, int size) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.CONNECTIVITY);
        return switch (selection.candidateId()) {
            case "disjoint-set" -> IntDisjointSet.of(size);
            case "rollback-disjoint-set" -> new IntRollbackDisjointSet(size);
            default -> throw new IllegalArgumentException(
                    "unsupported connectivity candidate: " + selection.candidateId());
        };
    }

    public static IntKmpPattern createSinglePatternIndex(
            AlgorithmSelection selection,
            int[] pattern) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.TEXT_SEARCH);
        if (!selection.candidateId().equals("kmp")) {
            throw new IllegalArgumentException("selection is not KMP");
        }
        return new IntKmpPattern(pattern);
    }

    public static IntAhoCorasick createMultiPatternIndex(
            AlgorithmSelection selection,
            int[][] patterns) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.TEXT_SEARCH);
        if (!selection.candidateId().equals("aho-corasick")) {
            throw new IllegalArgumentException("selection is not Aho-Corasick");
        }
        return IntAhoCorasick.compile(patterns);
    }

    public static IntSuffixArrayIndex createStaticTextIndex(
            AlgorithmSelection selection,
            int[] text) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.TEXT_SEARCH);
        if (!selection.candidateId().equals("suffix-array")) {
            throw new IllegalArgumentException("selection is not suffix-array");
        }
        return new IntSuffixArrayIndex(text);
    }

    /** Returns IntWaveletMatrix for STATIC_RANGE or LongOrderStatisticMultiset for DYNAMIC_MULTISET. */
    public static Object createRankIndex(AlgorithmSelection selection, int[] staticValues) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.RANK_QUANTILE);
        return switch (selection.candidateId()) {
            case "wavelet-matrix" -> new IntWaveletMatrix(
                    Objects.requireNonNull(staticValues, "staticValues"));
            case "order-statistic-multiset" -> new LongOrderStatisticMultiset();
            default -> throw new IllegalArgumentException(
                    "unsupported rank candidate: " + selection.candidateId());
        };
    }

    public static int[] topologicalOrder(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph graph) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.GRAPH_ANALYSIS);
        Objects.requireNonNull(graph, "graph");
        if (!selection.candidateId().equals("topological-order")) {
            throw new IllegalArgumentException("selection is not topological-order");
        }
        return IntGraphAlgorithms.topologicalOrder(graph);
    }

    public static IntGraphAlgorithms.SccResult stronglyConnectedComponents(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph graph) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.GRAPH_ANALYSIS);
        Objects.requireNonNull(graph, "graph");
        if (!selection.candidateId().equals("scc-kosaraju")) {
            throw new IllegalArgumentException("selection is not scc-kosaraju");
        }
        return IntGraphAlgorithms.stronglyConnectedComponents(graph);
    }

    public static IntMinimumSpanningForest.Result minimumSpanningForest(
            AlgorithmSelection selection,
            int vertexCount,
            int[] from,
            int[] to,
            long[] weight) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.SPANNING_FOREST);
        if (!selection.candidateId().equals("kruskal-msf")) {
            throw new IllegalArgumentException("selection is not kruskal-msf");
        }
        return IntMinimumSpanningForest.kruskal(vertexCount, from, to, weight);
    }

    public static LongPushRelabelMaxFlow createMaxFlow(
            AlgorithmSelection selection,
            int vertexCount,
            int expectedEdges) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.MAX_FLOW);
        if (!selection.candidateId().equals("push-relabel-max-flow")) {
            throw new IllegalArgumentException("selection is not push-relabel-max-flow");
        }
        return new LongPushRelabelMaxFlow(vertexCount, expectedEdges);
    }

    public static IntBipartiteMatcher createBipartiteMatcher(
            AlgorithmSelection selection,
            int leftCount,
            int rightCount,
            int[] leftVertices,
            int[] rightVertices) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.MATCHING);
        if (!selection.candidateId().equals("hopcroft-karp")) {
            throw new IllegalArgumentException("selection is not hopcroft-karp");
        }
        return new IntBipartiteMatcher(leftCount, rightCount, leftVertices, rightVertices);
    }

    /** Returns IntBinaryLiftingLca or IntHeavyLightIndex according to the selection. */
    public static Object createTreeIndex(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph tree,
            int root) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.TREE_INDEX);
        Objects.requireNonNull(tree, "tree");
        return switch (selection.candidateId()) {
            case "binary-lifting-lca" -> new IntBinaryLiftingLca(tree, root);
            case "heavy-light-index" -> new IntHeavyLightIndex(tree, root);
            default -> throw new IllegalArgumentException(
                    "unsupported tree candidate: " + selection.candidateId());
        };
    }

    public static IntBinaryLiftingLca createBinaryLiftingLca(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph tree,
            int root) {
        if (!selection.candidateId().equals("binary-lifting-lca")) {
            throw new IllegalArgumentException("selection is not binary-lifting-lca");
        }
        requirePurpose(selection, AlgorithmRequirements.Purpose.TREE_INDEX);
        return new IntBinaryLiftingLca(Objects.requireNonNull(tree, "tree"), root);
    }

    public static IntHeavyLightIndex createHeavyLightIndex(
            AlgorithmSelection selection,
            IntLongAdjacencyGraph tree,
            int root) {
        if (!selection.candidateId().equals("heavy-light-index")) {
            throw new IllegalArgumentException("selection is not heavy-light-index");
        }
        requirePurpose(selection, AlgorithmRequirements.Purpose.TREE_INDEX);
        return new IntHeavyLightIndex(Objects.requireNonNull(tree, "tree"), root);
    }

    /** Returns LongSlidingWindow or MonotoneLongDeque according to the selection. */
    public static Object createWindow(AlgorithmSelection selection, int capacity) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.WINDOW);
        return switch (selection.candidateId()) {
            case "sliding-window-sum", "sliding-window-extrema-scan" ->
                    new LongSlidingWindow(capacity);
            case "monotone-window-min" ->
                    new MonotoneLongDeque(capacity, MonotoneLongDeque.Mode.MIN);
            case "monotone-window-max" ->
                    new MonotoneLongDeque(capacity, MonotoneLongDeque.Mode.MAX);
            default -> throw new IllegalArgumentException(
                    "unsupported window candidate: " + selection.candidateId());
        };
    }

    public static IntCoordinateCompressor createCoordinateCompressor(
            AlgorithmSelection selection,
            int[] values) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.COORDINATE_COMPRESSION);
        if (!selection.candidateId().equals("int-coordinate-compressor")) {
            throw new IllegalArgumentException("selection is not int-coordinate-compressor");
        }
        return new IntCoordinateCompressor(Objects.requireNonNull(values, "values"));
    }

    public static long monotoneSearch(
            AlgorithmSelection selection,
            long lowInclusive,
            long highInclusive,
            LongPredicate predicate) {
        requirePurpose(selection, AlgorithmRequirements.Purpose.MONOTONE_SEARCH);
        if (!selection.candidateId().equals("long-partition-search")) {
            throw new IllegalArgumentException("selection is not long-partition-search");
        }
        return switch (selection.requirements().searchMode()) {
            case FIRST_TRUE ->
                    LongPartitionSearch.firstTrue(lowInclusive, highInclusive, predicate);
            case LAST_FALSE ->
                    LongPartitionSearch.lastFalse(lowInclusive, highInclusive, predicate);
        };
    }

    private static LongFenwickTree fenwick(long[] values) {
        LongFenwickTree tree = new LongFenwickTree(values.length);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0L) tree.add(i, values[i]);
        }
        return tree;
    }

    private static ShortestPathResult fromUnweighted(
            String id,
            int[] source,
            int unreachableSentinel) {
        long[] distance = new long[source.length];
        for (int i = 0; i < source.length; i++) {
            distance[i] = source[i] == unreachableSentinel
                    ? Long.MAX_VALUE
                    : source[i];
        }
        return new ShortestPathResult(id, distance, new boolean[source.length]);
    }

    private static void requirePurpose(
            AlgorithmSelection selection,
            AlgorithmRequirements.Purpose purpose) {
        Objects.requireNonNull(selection, "selection");
        if (selection.requirements().purpose() != purpose) {
            throw new IllegalArgumentException(
                    "selection purpose is " + selection.requirements().purpose()
                            + ", expected " + purpose);
        }
    }
}
