/*******************************************************************************
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
import jdk.m3.collections.graph.IntLongShortestPaths;
import jdk.m3.collections.graph.IntMinimumSpanningForest;
import jdk.m3.collections.range.LongFenwickTree;
import jdk.m3.collections.range.LongSegmentTree;
import jdk.m3.collections.range.LongSparseTable;
import jdk.m3.collections.range.PersistentLongSumTree;
import jdk.m3.collections.range.RangeAddLongSumTree;
import jdk.m3.collections.text.IntAhoCorasick;
import jdk.m3.collections.text.IntKmpPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in immutable registry for {@link FuzzyAlgorithmFactory}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/StandardAlgorithmCandidates.java}
 * blob {@code aee594e5d6066cd077219bbab150bda69d4fd577} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives algorithm candidates (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
final class StandardAlgorithmCandidates {

    private StandardAlgorithmCandidates() {}

    static List<AlgorithmCandidate> create() {
        ArrayList<AlgorithmCandidate> candidates = new ArrayList<>();

        // Shortest path.
        candidates.add(candidate(
                "bfs",
                AlgorithmRequirements.Purpose.SHORTEST_PATH,
                IntGraphAlgorithms.class,
                "unweighted breadth-first shortest paths",
                r -> graphLaw(r, AlgorithmRequirements.GraphWeightLaw.UNWEIGHTED),
                r -> mul(r.scale().vertices(), Integer.BYTES * 2L),
                traits(100, 100, 0, 100, 100, 100)));

        candidates.add(candidate(
                "zero-one-bfs",
                AlgorithmRequirements.Purpose.SHORTEST_PATH,
                IntZeroOneShortestPaths.class,
                "0-1 BFS over a primitive deque",
                StandardAlgorithmCandidates::zeroOneMatch,
                r -> mul(r.scale().vertices(), Integer.BYTES * 3L),
                traits(100, 96, 0, 96, 100, 95)));

        candidates.add(candidate(
                "dijkstra",
                AlgorithmRequirements.Purpose.SHORTEST_PATH,
                IntLongShortestPaths.class,
                "non-negative weighted Dijkstra with primitive lazy heap",
                StandardAlgorithmCandidates::dijkstraMatch,
                r -> add(
                        mul(r.scale().vertices(), Long.BYTES),
                        mul(Math.max(r.scale().vertices(), r.scale().edges()),
                                Integer.BYTES + Long.BYTES)),
                traits(100, 88, 0, 88, 95, 85)));

        candidates.add(candidate(
                "bellman-ford",
                AlgorithmRequirements.Purpose.SHORTEST_PATH,
                IntLongBellmanFord.class,
                "signed-weight Bellman-Ford with reachable negative-cycle propagation",
                r -> intVertices(r),
                r -> mul(r.scale().vertices(),
                        Long.BYTES + Integer.BYTES * 2L + 1L),
                traits(100, 30, 0, 25, 100, 95)));

        // Range indexes.
        candidates.add(candidate(
                "prefix-sums",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                LongPrefixSums.class,
                "immutable long prefix sums with O(1) range query",
                StandardAlgorithmCandidates::prefixMatch,
                r -> mul(add(r.scale().items(), 1L), Long.BYTES),
                traits(95, 100, 0, 100, 100, 100)));

        candidates.add(candidate(
                "fenwick",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                LongFenwickTree.class,
                "sum/rank Fenwick lane with point updates",
                StandardAlgorithmCandidates::fenwickMatch,
                r -> mul(add(r.scale().items(), 1L), Long.BYTES),
                traits(80, 90, 96, 96, 100, 100)));

        candidates.add(candidate(
                "segment-tree",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                LongSegmentTree.class,
                "generic associative range tree with point replacement",
                StandardAlgorithmCandidates::segmentMatch,
                r -> mul(Math.max(1L, mul(r.scale().items(), 2L)), Long.BYTES),
                traits(90, 85, 90, 90, 100, 95)));

        candidates.add(candidate(
                "range-add-sum-tree",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                RangeAddLongSumTree.class,
                "lazy range-add/range-sum tree with primitive sum/tag lanes",
                StandardAlgorithmCandidates::rangeAddMatch,
                r -> mul(Math.max(1L, mul(r.scale().items(), 8L)), Long.BYTES),
                traits(85, 82, 90, 88, 100, 90)));

        candidates.add(candidate(
                "sparse-table",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                LongSparseTable.class,
                "immutable idempotent sparse table with O(1) range query",
                StandardAlgorithmCandidates::sparseTableMatch,
                StandardAlgorithmCandidates::sparseTableBytes,
                traits(35, 100, 0, 96, 100, 80)));

        candidates.add(candidate(
                "persistent-sum-tree",
                AlgorithmRequirements.Purpose.RANGE_INDEX,
                PersistentLongSumTree.class,
                "versioned point-update/range-sum tree with primitive node indexes",
                StandardAlgorithmCandidates::persistentRangeMatch,
                StandardAlgorithmCandidates::persistentTreeBytes,
                traits(70, 84, 78, 78, 100, 80)));

        // Connectivity.
        candidates.add(candidate(
                "disjoint-set",
                AlgorithmRequirements.Purpose.CONNECTIVITY,
                IntDisjointSet.class,
                "path-compressed union-find for online connectivity",
                StandardAlgorithmCandidates::disjointSetMatch,
                r -> mul(r.scale().items(), Integer.BYTES * 2L),
                traits(100, 100, 100, 100, 100, 100)));

        candidates.add(candidate(
                "rollback-disjoint-set",
                AlgorithmRequirements.Purpose.CONNECTIVITY,
                IntRollbackDisjointSet.class,
                "union-by-size rollback connectivity with primitive history lanes",
                r -> intItems(r),
                StandardAlgorithmCandidates::rollbackDsuBytes,
                traits(100, 88, 88, 85, 100, 95)));

        // Text search/index.
        candidates.add(candidate(
                "kmp",
                AlgorithmRequirements.Purpose.TEXT_SEARCH,
                IntKmpPattern.class,
                "single-pattern KMP over integer/token symbols",
                StandardAlgorithmCandidates::kmpMatch,
                r -> mul(Math.max(0L, r.scale().patternSymbols()), Integer.BYTES * 2L),
                traits(100, 95, 0, 98, 100, 100)));

        candidates.add(candidate(
                "aho-corasick",
                AlgorithmRequirements.Purpose.TEXT_SEARCH,
                IntAhoCorasick.class,
                "multi-pattern integer-symbol automaton with CSR transitions",
                StandardAlgorithmCandidates::ahoMatch,
                r -> mul(Math.max(1L, r.scale().patternSymbols()), Integer.BYTES * 8L),
                traits(65, 100, 0, 100, 100, 85)));

        candidates.add(candidate(
                "suffix-array",
                AlgorithmRequirements.Purpose.TEXT_SEARCH,
                IntSuffixArrayIndex.class,
                "static text suffix-array/LCP index for many pattern queries",
                StandardAlgorithmCandidates::suffixMatch,
                r -> mul(Math.max(0L, r.scale().items()), Integer.BYTES * 4L),
                traits(45, 100, 0, 95, 100, 100)));

        // Rank / quantile.
        candidates.add(candidate(
                "wavelet-matrix",
                AlgorithmRequirements.Purpose.RANK_QUANTILE,
                IntWaveletMatrix.class,
                "immutable subrange kth/rank/frequency wavelet matrix",
                StandardAlgorithmCandidates::waveletMatch,
                r -> mul(Math.max(0L, r.scale().items()), 6L),
                traits(55, 100, 0, 95, 100, 85)));

        candidates.add(candidate(
                "order-statistic-multiset",
                AlgorithmRequirements.Purpose.RANK_QUANTILE,
                LongOrderStatisticMultiset.class,
                "mutable global rank/select multiset over parallel AVL lanes",
                StandardAlgorithmCandidates::orderStatisticMatch,
                r -> mul(Math.max(17L, r.scale().items()), 33L),
                traits(100, 90, 90, 90, 100, 80)));

        // Graph analysis.
        candidates.add(candidate(
                "topological-order",
                AlgorithmRequirements.Purpose.GRAPH_ANALYSIS,
                IntGraphAlgorithms.class,
                "Kahn topological ordering over primitive adjacency lanes",
                r -> graphAnalysisMatch(
                        r, AlgorithmRequirements.GraphAnalysisMode.TOPOLOGICAL_ORDER),
                StandardAlgorithmCandidates::topologicalBytes,
                traits(100, 95, 0, 98, 100, 95)));

        candidates.add(candidate(
                "scc-kosaraju",
                AlgorithmRequirements.Purpose.GRAPH_ANALYSIS,
                IntGraphAlgorithms.class,
                "iterative Kosaraju SCC with primitive reverse-edge lanes",
                r -> graphAnalysisMatch(
                        r, AlgorithmRequirements.GraphAnalysisMode.STRONGLY_CONNECTED_COMPONENTS),
                StandardAlgorithmCandidates::sccBytes,
                traits(90, 100, 0, 95, 100, 88)));

        // Spanning forest.
        candidates.add(candidate(
                "kruskal-msf",
                AlgorithmRequirements.Purpose.SPANNING_FOREST,
                IntMinimumSpanningForest.class,
                "Kruskal minimum spanning forest over primitive edge lanes",
                StandardAlgorithmCandidates::spanningMatch,
                StandardAlgorithmCandidates::kruskalBytes,
                traits(65, 100, 0, 90, 100, 95)));

        // Maximum flow.
        candidates.add(candidate(
                "push-relabel-max-flow",
                AlgorithmRequirements.Purpose.MAX_FLOW,
                LongPushRelabelMaxFlow.class,
                "one-shot push-relabel max-flow/min-cut with primitive residual lanes",
                StandardAlgorithmCandidates::flowMatch,
                StandardAlgorithmCandidates::pushRelabelBytes,
                traits(70, 100, 20, 95, 100, 88)));

        // Matching.
        candidates.add(candidate(
                "hopcroft-karp",
                AlgorithmRequirements.Purpose.MATCHING,
                IntBipartiteMatcher.class,
                "iterative Hopcroft-Karp maximum cardinality matching over frozen CSR",
                StandardAlgorithmCandidates::matchingMatch,
                StandardAlgorithmCandidates::matchingBytes,
                traits(75, 100, 0, 98, 100, 92)));

        // Tree indexes. LCA_DISTANCE deliberately has two valid candidates.
        candidates.add(candidate(
                "binary-lifting-lca",
                AlgorithmRequirements.Purpose.TREE_INDEX,
                IntBinaryLiftingLca.class,
                "binary-lifting LCA/distance index with flat level-major ancestor table",
                StandardAlgorithmCandidates::binaryLiftingMatch,
                StandardAlgorithmCandidates::binaryLiftingBytes,
                traits(55, 100, 0, 100, 100, 92)));

        candidates.add(candidate(
                "heavy-light-index",
                AlgorithmRequirements.Purpose.TREE_INDEX,
                IntHeavyLightIndex.class,
                "heavy-light decomposition for LCA, subtree ranges and path segments",
                StandardAlgorithmCandidates::heavyLightMatch,
                StandardAlgorithmCandidates::heavyLightBytes,
                traits(72, 90, 0, 94, 100, 95)));

        // Sliding windows. Extrema deliberately have scan-vs-monotone alternatives.
        candidates.add(candidate(
                "sliding-window-sum",
                AlgorithmRequirements.Purpose.WINDOW,
                LongSlidingWindow.class,
                "single-ring running sum/average window",
                r -> windowMatch(r, AlgorithmRequirements.WindowMode.SUM_AVERAGE),
                StandardAlgorithmCandidates::singleLaneWindowBytes,
                traits(100, 100, 100, 100, 100, 100)));

        candidates.add(candidate(
                "sliding-window-extrema-scan",
                AlgorithmRequirements.Purpose.WINDOW,
                LongSlidingWindow.class,
                "single-ring window with O(window) min/max scan and minimal retained bytes",
                StandardAlgorithmCandidates::extremaWindowMatch,
                StandardAlgorithmCandidates::singleLaneWindowBytes,
                traits(100, 25, 100, 72, 100, 100)));

        candidates.add(candidate(
                "monotone-window-min",
                AlgorithmRequirements.Purpose.WINDOW,
                MonotoneLongDeque.class,
                "monotone increasing deque with O(1) window minimum",
                r -> windowMatch(r, AlgorithmRequirements.WindowMode.MINIMUM),
                StandardAlgorithmCandidates::monotoneWindowBytes,
                traits(100, 100, 95, 100, 100, 95)));

        candidates.add(candidate(
                "monotone-window-max",
                AlgorithmRequirements.Purpose.WINDOW,
                MonotoneLongDeque.class,
                "monotone decreasing deque with O(1) window maximum",
                r -> windowMatch(r, AlgorithmRequirements.WindowMode.MAXIMUM),
                StandardAlgorithmCandidates::monotoneWindowBytes,
                traits(100, 100, 95, 100, 100, 95)));

        // Coordinate compression.
        candidates.add(candidate(
                "int-coordinate-compressor",
                AlgorithmRequirements.Purpose.COORDINATE_COMPRESSION,
                IntCoordinateCompressor.class,
                "sorted-unique int dictionary for rank/value compression",
                StandardAlgorithmCandidates::compressionMatch,
                r -> mul(Math.max(0L, r.scale().items()), Integer.BYTES * 2L),
                traits(55, 100, 0, 95, 100, 100)));

        // Binary-search-on-answer / monotone partition search.
        candidates.add(candidate(
                "long-partition-search",
                AlgorithmRequirements.Purpose.MONOTONE_SEARCH,
                LongPartitionSearch.class,
                "overflow-safe signed-long binary search over a monotone false..true predicate",
                StandardAlgorithmCandidates::partitionSearchMatch,
                r -> 0L,
                traits(100, 100, 0, 100, 100, 100)));

        return List.copyOf(candidates);
    }

    private static AlgorithmCandidate candidate(
            String id,
            AlgorithmRequirements.Purpose purpose,
            Class<?> type,
            String description,
            AlgorithmCandidate.Matcher matcher,
            AlgorithmCandidate.Estimator estimator,
            AlgorithmCandidate.Traits traits) {
        return new AlgorithmCandidate(id, purpose, type, description, matcher, estimator, traits);
    }

    private static AlgorithmCandidate.Traits traits(
            int preprocessing,
            int query,
            int update,
            int throughput,
            int allocation,
            int locality) {
        return new AlgorithmCandidate.Traits(
                preprocessing, query, update, throughput, allocation, locality);
    }

    private static String graphLaw(
            AlgorithmRequirements requirements,
            AlgorithmRequirements.GraphWeightLaw exact) {
        String intSize = intVertices(requirements);
        if (intSize != null) return intSize;
        return requirements.graphWeightLaw() == exact ? null : "weight law mismatch";
    }

    private static String zeroOneMatch(AlgorithmRequirements requirements) {
        String intSize = intVertices(requirements);
        if (intSize != null) return intSize;
        return switch (requirements.graphWeightLaw()) {
            case UNWEIGHTED, ZERO_ONE -> null;
            default -> "requires unweighted or 0/1 edges";
        };
    }

    private static String dijkstraMatch(AlgorithmRequirements requirements) {
        String intSize = intVertices(requirements);
        if (intSize != null) return intSize;
        return requirements.graphWeightLaw() == AlgorithmRequirements.GraphWeightLaw.SIGNED
                ? "negative weights unsupported"
                : null;
    }

    private static String prefixMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        if (requirements.rangeAggregation() != AlgorithmRequirements.RangeAggregation.SUM) {
            return "requires SUM aggregation";
        }
        return requirements.rangeMutation() == AlgorithmRequirements.RangeMutation.IMMUTABLE
                ? null : "immutable only";
    }

    private static String fenwickMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        if (requirements.rangeAggregation() != AlgorithmRequirements.RangeAggregation.SUM) {
            return "requires SUM aggregation";
        }
        return switch (requirements.rangeMutation()) {
            case IMMUTABLE, POINT_UPDATE -> null;
            default -> "requires immutable or point-update sum workload";
        };
    }

    private static String segmentMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return switch (requirements.rangeMutation()) {
            case IMMUTABLE, POINT_UPDATE -> null;
            default -> "requires immutable or point replacement";
        };
    }

    private static String rangeAddMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        if (requirements.rangeAggregation() != AlgorithmRequirements.RangeAggregation.SUM) {
            return "requires SUM aggregation";
        }
        return requirements.rangeMutation() == AlgorithmRequirements.RangeMutation.RANGE_ADD
                ? null : "requires RANGE_ADD mutation";
    }

    private static String sparseTableMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        if (requirements.rangeAggregation() != AlgorithmRequirements.RangeAggregation.IDEMPOTENT) {
            return "requires idempotent associative aggregation";
        }
        return requirements.rangeMutation() == AlgorithmRequirements.RangeMutation.IMMUTABLE
                ? null : "immutable only";
    }

    private static String persistentRangeMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        if (requirements.rangeAggregation() != AlgorithmRequirements.RangeAggregation.SUM) {
            return "requires SUM aggregation";
        }
        return requirements.rangeMutation() == AlgorithmRequirements.RangeMutation.VERSIONED_POINT
                ? null : "requires VERSIONED_POINT mutation";
    }

    private static String disjointSetMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return requirements.connectivityMode() == AlgorithmRequirements.ConnectivityMode.ONLINE
                ? null : "rollback required";
    }

    private static String kmpMatch(AlgorithmRequirements requirements) {
        if (requirements.textMode() != AlgorithmRequirements.TextMode.SINGLE_PATTERN) {
            return "requires SINGLE_PATTERN";
        }
        if (requirements.scale().patternSymbols() > Integer.MAX_VALUE) {
            return "pattern exceeds int[] indexing";
        }
        return null;
    }

    private static String ahoMatch(AlgorithmRequirements requirements) {
        if (requirements.textMode() == AlgorithmRequirements.TextMode.STATIC_TEXT_INDEX) {
            return "static text index uses suffix array";
        }
        if (requirements.scale().patternSymbols() > Integer.MAX_VALUE) {
            return "pattern corpus exceeds int[] indexing";
        }
        if (requirements.textMode() == AlgorithmRequirements.TextMode.SINGLE_PATTERN
                && requirements.scale().patterns() > 1L) {
            return "single-pattern mode has multiple patterns";
        }
        return null;
    }

    private static String suffixMatch(AlgorithmRequirements requirements) {
        if (requirements.textMode() != AlgorithmRequirements.TextMode.STATIC_TEXT_INDEX) {
            return "requires STATIC_TEXT_INDEX";
        }
        return intItems(requirements);
    }

    private static String waveletMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return requirements.rankMode() == AlgorithmRequirements.RankMode.STATIC_RANGE
                ? null : "requires STATIC_RANGE";
    }

    private static String orderStatisticMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return requirements.rankMode() == AlgorithmRequirements.RankMode.DYNAMIC_MULTISET
                ? null : "requires DYNAMIC_MULTISET";
    }

    private static String graphAnalysisMatch(
            AlgorithmRequirements requirements,
            AlgorithmRequirements.GraphAnalysisMode mode) {
        String vertices = intVertices(requirements);
        if (vertices != null) return vertices;
        String edges = intEdges(requirements);
        if (edges != null) return edges;
        return requirements.graphAnalysisMode() == mode ? null : "graph analysis mode mismatch";
    }

    private static String spanningMatch(AlgorithmRequirements requirements) {
        String vertices = intVertices(requirements);
        if (vertices != null) return vertices;
        String edges = intEdges(requirements);
        if (edges != null) return edges;
        return requirements.spanningMode()
                == AlgorithmRequirements.SpanningMode.MINIMUM_SPANNING_FOREST
                ? null : "spanning mode mismatch";
    }

    private static String flowMatch(AlgorithmRequirements requirements) {
        String vertices = intVertices(requirements);
        if (vertices != null) return vertices;
        if (requirements.scale().edges() > Integer.MAX_VALUE / 2L) {
            return "edge count exceeds residual int[] indexing";
        }
        return requirements.flowMode() == AlgorithmRequirements.FlowMode.MAX_FLOW_MIN_CUT
                ? null : "flow mode mismatch";
    }

    private static String matchingMatch(AlgorithmRequirements requirements) {
        if (requirements.scale().items() > Integer.MAX_VALUE) {
            return "left partition exceeds int indexing";
        }
        String right = intVertices(requirements);
        if (right != null) return right;
        String edges = intEdges(requirements);
        if (edges != null) return edges;
        return requirements.matchingMode()
                == AlgorithmRequirements.MatchingMode.BIPARTITE_MAXIMUM_CARDINALITY
                ? null : "matching mode mismatch";
    }

    private static String binaryLiftingMatch(AlgorithmRequirements requirements) {
        String tree = treeShape(requirements);
        if (tree != null) return tree;
        return requirements.treeMode() == AlgorithmRequirements.TreeMode.LCA_DISTANCE
                ? null : "binary lifting does not expose path segments";
    }

    private static String heavyLightMatch(AlgorithmRequirements requirements) {
        String tree = treeShape(requirements);
        if (tree != null) return tree;
        return switch (requirements.treeMode()) {
            case LCA_DISTANCE, PATH_SEGMENTS_SUBTREE -> null;
        };
    }

    private static String treeShape(AlgorithmRequirements requirements) {
        String vertices = intVertices(requirements);
        if (vertices != null) return vertices;
        long n = requirements.scale().vertices();
        long expectedEdges = n == 0L ? 0L : mul(n - 1L, 2L);
        if (requirements.scale().edges() != expectedEdges) {
            return "tree requires exactly 2*(vertices-1) directed adjacency rows";
        }
        return null;
    }

    private static String windowMatch(
            AlgorithmRequirements requirements,
            AlgorithmRequirements.WindowMode mode) {
        String size = intItems(requirements);
        if (size != null) return size;
        return requirements.windowMode() == mode ? null : "window mode mismatch";
    }

    private static String extremaWindowMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return switch (requirements.windowMode()) {
            case MINIMUM, MAXIMUM -> null;
            case SUM_AVERAGE -> "sum/average uses running-sum window";
        };
    }

    private static String compressionMatch(AlgorithmRequirements requirements) {
        String size = intItems(requirements);
        if (size != null) return size;
        return requirements.compressionMode()
                == AlgorithmRequirements.CompressionMode.INT_SORTED_UNIQUE
                ? null : "compression mode mismatch";
    }

    private static String partitionSearchMatch(AlgorithmRequirements requirements) {
        return switch (requirements.searchMode()) {
            case FIRST_TRUE, LAST_FALSE -> null;
        };
    }

    private static String intVertices(AlgorithmRequirements requirements) {
        return requirements.scale().vertices() <= Integer.MAX_VALUE
                ? null : "vertex count exceeds int indexing";
    }

    private static String intItems(AlgorithmRequirements requirements) {
        return requirements.scale().items() <= Integer.MAX_VALUE
                ? null : "item count exceeds int indexing";
    }

    private static String intEdges(AlgorithmRequirements requirements) {
        return requirements.scale().edges() <= Integer.MAX_VALUE
                ? null : "edge count exceeds int indexing";
    }

    private static long topologicalBytes(AlgorithmRequirements requirements) {
        return mul(requirements.scale().vertices(), Integer.BYTES * 3L);
    }

    private static long sccBytes(AlgorithmRequirements requirements) {
        long n = requirements.scale().vertices();
        long m = requirements.scale().edges();
        return add(mul(n, Integer.BYTES * 6L + 1L), mul(m, Integer.BYTES * 2L));
    }

    private static long kruskalBytes(AlgorithmRequirements requirements) {
        long n = requirements.scale().vertices();
        long m = requirements.scale().edges();
        long selected = Math.min(m, Math.max(0L, n - 1L));
        return add(mul(m, Integer.BYTES), add(mul(selected, Integer.BYTES), mul(n, Integer.BYTES)));
    }

    private static long pushRelabelBytes(AlgorithmRequirements requirements) {
        long n = requirements.scale().vertices();
        long m = requirements.scale().edges();
        long retained = add(mul(n, Integer.BYTES), mul(mul(m, 2L), Integer.BYTES * 2L + Long.BYTES));
        long working = mul(n, Long.BYTES + Integer.BYTES * 3L + 1L);
        return add(retained, working);
    }

    private static long matchingBytes(AlgorithmRequirements requirements) {
        long left = requirements.scale().items();
        long right = requirements.scale().vertices();
        long edges = requirements.scale().edges();
        long ints = add(add(mul(left, 7L), right), add(edges, 1L));
        return mul(ints, Integer.BYTES);
    }

    private static long binaryLiftingBytes(AlgorithmRequirements requirements) {
        long n = Math.max(1L, requirements.scale().vertices());
        int levels = Math.max(1, 64 - Long.numberOfLeadingZeros(n));
        return mul(n, mul(1L + levels, Integer.BYTES));
    }

    private static long heavyLightBytes(AlgorithmRequirements requirements) {
        return mul(requirements.scale().vertices(), Integer.BYTES * 7L);
    }

    private static long singleLaneWindowBytes(AlgorithmRequirements requirements) {
        return mul(requirements.scale().items(), Long.BYTES);
    }

    private static long monotoneWindowBytes(AlgorithmRequirements requirements) {
        return mul(requirements.scale().items(), Long.BYTES * 2L);
    }

    private static long sparseTableBytes(AlgorithmRequirements requirements) {
        long n = requirements.scale().items();
        if (n == 0L) return 0L;
        int levels = 64 - Long.numberOfLeadingZeros(n);
        return mul(mul(n, levels), Long.BYTES);
    }

    private static long persistentTreeBytes(AlgorithmRequirements requirements) {
        long n = Math.max(1L, requirements.scale().items());
        long updates = Math.max(1L, requirements.scale().updates());
        int depth = 64 - Long.numberOfLeadingZeros(n - 1L);
        long nodes = add(1L, mul(updates, depth + 1L));
        return add(mul(nodes, Integer.BYTES * 2L + Long.BYTES),
                mul(updates + 1L, Integer.BYTES));
    }

    private static long rollbackDsuBytes(AlgorithmRequirements requirements) {
        long items = requirements.scale().items();
        long updates = Math.max(1L, requirements.scale().updates());
        return add(mul(items, Integer.BYTES), mul(updates, Integer.BYTES * 2L));
    }

    private static long mul(long left, long right) {
        if (left == 0L || right == 0L) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    private static long add(long left, long right) {
        if (left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
