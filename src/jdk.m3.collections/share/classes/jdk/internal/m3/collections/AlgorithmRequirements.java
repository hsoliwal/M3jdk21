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

import java.util.Objects;

/**
 * Structured characteristics used by {@link FuzzyAlgorithmFactory}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/AlgorithmRequirements.java}
 * blob {@code aeedd331f21a9088433e39297f0d90ad1fd030e1} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class AlgorithmRequirements {

    public enum Purpose {
        SHORTEST_PATH,
        RANGE_INDEX,
        CONNECTIVITY,
        TEXT_SEARCH,
        RANK_QUANTILE,
        GRAPH_ANALYSIS,
        SPANNING_FOREST,
        MAX_FLOW,
        MATCHING,
        TREE_INDEX,
        WINDOW,
        COORDINATE_COMPRESSION,
        MONOTONE_SEARCH
    }

    public enum GraphWeightLaw {
        UNWEIGHTED,
        ZERO_ONE,
        NON_NEGATIVE,
        SIGNED
    }

    public enum RangeAggregation {
        SUM,
        IDEMPOTENT,
        ASSOCIATIVE
    }

    public enum RangeMutation {
        IMMUTABLE,
        POINT_UPDATE,
        RANGE_ADD,
        VERSIONED_POINT
    }

    public enum RangeQuery {
        PREFIX,
        RANGE
    }

    public enum ConnectivityMode {
        ONLINE,
        ROLLBACK
    }

    public enum TextMode {
        SINGLE_PATTERN,
        MULTI_PATTERN_STREAM,
        STATIC_TEXT_INDEX
    }

    public enum RankMode {
        STATIC_RANGE,
        DYNAMIC_MULTISET
    }

    public enum GraphAnalysisMode {
        TOPOLOGICAL_ORDER,
        STRONGLY_CONNECTED_COMPONENTS
    }

    public enum SpanningMode {
        MINIMUM_SPANNING_FOREST
    }

    public enum FlowMode {
        MAX_FLOW_MIN_CUT
    }

    public enum MatchingMode {
        BIPARTITE_MAXIMUM_CARDINALITY
    }

    public enum TreeMode {
        LCA_DISTANCE,
        PATH_SEGMENTS_SUBTREE
    }

    public enum WindowMode {
        SUM_AVERAGE,
        MINIMUM,
        MAXIMUM
    }

    public enum CompressionMode {
        INT_SORTED_UNIQUE
    }

    public enum SearchMode {
        FIRST_TRUE,
        LAST_FALSE
    }

    public record Scale(
            long items,
            long vertices,
            long edges,
            long patterns,
            long patternSymbols,
            long queries,
            long updates) {

        public Scale {
            if (items < 0L || vertices < 0L || edges < 0L || patterns < 0L
                    || patternSymbols < 0L || queries < 0L || updates < 0L) {
                throw new IllegalArgumentException("scale values must be >= 0");
            }
        }

        public static Scale items(long items) {
            return new Scale(items, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    public record Priorities(
            int memory,
            int preprocessing,
            int queryLatency,
            int updateLatency,
            int throughput,
            int allocation,
            int locality) {

        public Priorities {
            checkWeight("memory", memory);
            checkWeight("preprocessing", preprocessing);
            checkWeight("queryLatency", queryLatency);
            checkWeight("updateLatency", updateLatency);
            checkWeight("throughput", throughput);
            checkWeight("allocation", allocation);
            checkWeight("locality", locality);
        }

        public static Priorities balanced() {
            return new Priorities(60, 50, 70, 60, 70, 80, 70);
        }

        public static Priorities memoryFirst() {
            return new Priorities(100, 50, 60, 50, 60, 95, 80);
        }

        public static Priorities queryFirst() {
            return new Priorities(40, 50, 100, 50, 90, 75, 85);
        }
    }

    private final Purpose purpose;
    private final Scale scale;
    private final Priorities priorities;
    private final GraphWeightLaw graphWeightLaw;
    private final RangeAggregation rangeAggregation;
    private final RangeMutation rangeMutation;
    private final RangeQuery rangeQuery;
    private final ConnectivityMode connectivityMode;
    private final TextMode textMode;
    private final RankMode rankMode;
    private final GraphAnalysisMode graphAnalysisMode;
    private final SpanningMode spanningMode;
    private final FlowMode flowMode;
    private final MatchingMode matchingMode;
    private final TreeMode treeMode;
    private final WindowMode windowMode;
    private final CompressionMode compressionMode;
    private final SearchMode searchMode;

    private AlgorithmRequirements(Builder builder) {
        purpose = builder.purpose;
        scale = builder.scale;
        priorities = builder.priorities;
        graphWeightLaw = builder.graphWeightLaw;
        rangeAggregation = builder.rangeAggregation;
        rangeMutation = builder.rangeMutation;
        rangeQuery = builder.rangeQuery;
        connectivityMode = builder.connectivityMode;
        textMode = builder.textMode;
        rankMode = builder.rankMode;
        graphAnalysisMode = builder.graphAnalysisMode;
        spanningMode = builder.spanningMode;
        flowMode = builder.flowMode;
        matchingMode = builder.matchingMode;
        treeMode = builder.treeMode;
        windowMode = builder.windowMode;
        compressionMode = builder.compressionMode;
        searchMode = builder.searchMode;
        validate();
    }

    public static Builder builder(Purpose purpose) {
        return new Builder(purpose);
    }

    public Purpose purpose() { return purpose; }
    public Scale scale() { return scale; }
    public Priorities priorities() { return priorities; }
    public GraphWeightLaw graphWeightLaw() { return graphWeightLaw; }
    public RangeAggregation rangeAggregation() { return rangeAggregation; }
    public RangeMutation rangeMutation() { return rangeMutation; }
    public RangeQuery rangeQuery() { return rangeQuery; }
    public ConnectivityMode connectivityMode() { return connectivityMode; }
    public TextMode textMode() { return textMode; }
    public RankMode rankMode() { return rankMode; }
    public GraphAnalysisMode graphAnalysisMode() { return graphAnalysisMode; }
    public SpanningMode spanningMode() { return spanningMode; }
    public FlowMode flowMode() { return flowMode; }
    public MatchingMode matchingMode() { return matchingMode; }
    public TreeMode treeMode() { return treeMode; }
    public WindowMode windowMode() { return windowMode; }
    public CompressionMode compressionMode() { return compressionMode; }
    public SearchMode searchMode() { return searchMode; }

    private void validate() {
        switch (purpose) {
            case SHORTEST_PATH -> {
                if (graphWeightLaw == null) throw new IllegalArgumentException("graphWeightLaw required");
                if (scale.vertices() <= 0L) throw new IllegalArgumentException("vertices must be > 0");
            }
            case RANGE_INDEX -> {
                if (rangeAggregation == null || rangeMutation == null || rangeQuery == null) {
                    throw new IllegalArgumentException("range semantics required");
                }
                if (scale.items() < 0L) throw new IllegalArgumentException("items < 0");
            }
            case CONNECTIVITY -> {
                if (connectivityMode == null) throw new IllegalArgumentException("connectivityMode required");
                if (scale.items() <= 0L) throw new IllegalArgumentException("items must be > 0");
            }
            case TEXT_SEARCH -> {
                if (textMode == null) throw new IllegalArgumentException("textMode required");
                if (textMode == TextMode.MULTI_PATTERN_STREAM && scale.patterns() <= 0L) {
                    throw new IllegalArgumentException("patterns must be > 0");
                }
            }
            case RANK_QUANTILE -> {
                if (rankMode == null) throw new IllegalArgumentException("rankMode required");
            }
            case GRAPH_ANALYSIS -> {
                if (graphAnalysisMode == null) {
                    throw new IllegalArgumentException("graphAnalysisMode required");
                }
            }
            case SPANNING_FOREST -> {
                if (spanningMode == null) throw new IllegalArgumentException("spanningMode required");
            }
            case MAX_FLOW -> {
                if (flowMode == null) throw new IllegalArgumentException("flowMode required");
                if (scale.vertices() <= 0L) throw new IllegalArgumentException("vertices must be > 0");
            }
            case MATCHING -> {
                if (matchingMode == null) throw new IllegalArgumentException("matchingMode required");
            }
            case TREE_INDEX -> {
                if (treeMode == null) throw new IllegalArgumentException("treeMode required");
                if (scale.vertices() <= 0L) throw new IllegalArgumentException("vertices must be > 0");
            }
            case WINDOW -> {
                if (windowMode == null) throw new IllegalArgumentException("windowMode required");
                if (scale.items() <= 0L) throw new IllegalArgumentException("window capacity must be > 0");
            }
            case COORDINATE_COMPRESSION -> {
                if (compressionMode == null) {
                    throw new IllegalArgumentException("compressionMode required");
                }
            }
            case MONOTONE_SEARCH -> {
                if (searchMode == null) throw new IllegalArgumentException("searchMode required");
            }
        }
    }

    private static void checkWeight(String name, int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be in [0,100]");
        }
    }

    public static final class Builder {
        private final Purpose purpose;
        private Scale scale = new Scale(16L, 0L, 0L, 1L, 16L, 1L, 0L);
        private Priorities priorities = Priorities.balanced();
        private GraphWeightLaw graphWeightLaw;
        private RangeAggregation rangeAggregation;
        private RangeMutation rangeMutation;
        private RangeQuery rangeQuery;
        private ConnectivityMode connectivityMode;
        private TextMode textMode;
        private RankMode rankMode;
        private GraphAnalysisMode graphAnalysisMode;
        private SpanningMode spanningMode;
        private FlowMode flowMode;
        private MatchingMode matchingMode;
        private TreeMode treeMode;
        private WindowMode windowMode;
        private CompressionMode compressionMode;
        private SearchMode searchMode;

        private Builder(Purpose purpose) {
            this.purpose = Objects.requireNonNull(purpose, "purpose");
            switch (purpose) {
                case SHORTEST_PATH -> graphWeightLaw = GraphWeightLaw.NON_NEGATIVE;
                case RANGE_INDEX -> {
                    rangeAggregation = RangeAggregation.SUM;
                    rangeMutation = RangeMutation.IMMUTABLE;
                    rangeQuery = RangeQuery.RANGE;
                }
                case CONNECTIVITY -> connectivityMode = ConnectivityMode.ONLINE;
                case TEXT_SEARCH -> textMode = TextMode.SINGLE_PATTERN;
                case RANK_QUANTILE -> rankMode = RankMode.STATIC_RANGE;
                case GRAPH_ANALYSIS -> graphAnalysisMode = GraphAnalysisMode.TOPOLOGICAL_ORDER;
                case SPANNING_FOREST -> spanningMode = SpanningMode.MINIMUM_SPANNING_FOREST;
                case MAX_FLOW -> flowMode = FlowMode.MAX_FLOW_MIN_CUT;
                case MATCHING -> matchingMode = MatchingMode.BIPARTITE_MAXIMUM_CARDINALITY;
                case TREE_INDEX -> treeMode = TreeMode.LCA_DISTANCE;
                case WINDOW -> windowMode = WindowMode.SUM_AVERAGE;
                case COORDINATE_COMPRESSION -> compressionMode = CompressionMode.INT_SORTED_UNIQUE;
                case MONOTONE_SEARCH -> searchMode = SearchMode.FIRST_TRUE;
            }
        }

        public Builder scale(Scale value) {
            scale = Objects.requireNonNull(value, "scale");
            return this;
        }

        public Builder priorities(Priorities value) {
            priorities = Objects.requireNonNull(value, "priorities");
            return this;
        }

        public Builder graph(long vertices, long edges, GraphWeightLaw law) {
            scale = new Scale(
                    scale.items(), vertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            graphWeightLaw = Objects.requireNonNull(law, "law");
            return this;
        }

        public Builder items(long items, long queries, long updates) {
            scale = new Scale(
                    items, scale.vertices(), scale.edges(), scale.patterns(), scale.patternSymbols(),
                    queries, updates);
            return this;
        }

        public Builder patterns(long patterns, long symbols, long queries) {
            scale = new Scale(
                    scale.items(), scale.vertices(), scale.edges(), patterns, symbols,
                    queries, scale.updates());
            return this;
        }

        public Builder range(
                RangeAggregation aggregation,
                RangeMutation mutation,
                RangeQuery query) {
            rangeAggregation = Objects.requireNonNull(aggregation, "aggregation");
            rangeMutation = Objects.requireNonNull(mutation, "mutation");
            rangeQuery = Objects.requireNonNull(query, "query");
            return this;
        }

        public Builder connectivity(ConnectivityMode value) {
            connectivityMode = Objects.requireNonNull(value, "connectivityMode");
            return this;
        }

        public Builder text(TextMode value) {
            textMode = Objects.requireNonNull(value, "textMode");
            return this;
        }

        public Builder rank(RankMode value) {
            rankMode = Objects.requireNonNull(value, "rankMode");
            return this;
        }

        public Builder graphAnalysis(
                long vertices,
                long edges,
                GraphAnalysisMode mode) {
            scale = new Scale(
                    scale.items(), vertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            graphAnalysisMode = Objects.requireNonNull(mode, "graphAnalysisMode");
            return this;
        }

        public Builder spanningForest(long vertices, long edges) {
            scale = new Scale(
                    scale.items(), vertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            spanningMode = SpanningMode.MINIMUM_SPANNING_FOREST;
            return this;
        }

        public Builder flow(long vertices, long edges) {
            scale = new Scale(
                    scale.items(), vertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            flowMode = FlowMode.MAX_FLOW_MIN_CUT;
            return this;
        }

        /**
         * Matching scale uses items=left partition, vertices=right partition, edges=edge count.
         */
        public Builder matching(long leftVertices, long rightVertices, long edges) {
            scale = new Scale(
                    leftVertices, rightVertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            matchingMode = MatchingMode.BIPARTITE_MAXIMUM_CARDINALITY;
            return this;
        }

        public Builder tree(long vertices, long edges, TreeMode mode) {
            scale = new Scale(
                    scale.items(), vertices, edges, scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            treeMode = Objects.requireNonNull(mode, "treeMode");
            return this;
        }

        public Builder window(
                long capacity,
                long queries,
                long updates,
                WindowMode mode) {
            scale = new Scale(
                    capacity, scale.vertices(), scale.edges(), scale.patterns(),
                    scale.patternSymbols(), queries, updates);
            windowMode = Objects.requireNonNull(mode, "windowMode");
            return this;
        }

        public Builder compression(long items) {
            scale = new Scale(
                    items, scale.vertices(), scale.edges(), scale.patterns(), scale.patternSymbols(),
                    scale.queries(), scale.updates());
            compressionMode = CompressionMode.INT_SORTED_UNIQUE;
            return this;
        }

        public Builder monotoneSearch(SearchMode mode) {
            searchMode = Objects.requireNonNull(mode, "searchMode");
            return this;
        }

        public AlgorithmRequirements build() {
            return new AlgorithmRequirements(this);
        }
    }

    @Override
    public String toString() {
        return "M3AlgorithmRequirements["
                + "purpose=" + purpose
                + ", scale=" + scale
                + ", graphWeightLaw=" + graphWeightLaw
                + ", rangeAggregation=" + rangeAggregation
                + ", rangeMutation=" + rangeMutation
                + ", rangeQuery=" + rangeQuery
                + ", connectivityMode=" + connectivityMode
                + ", textMode=" + textMode
                + ", rankMode=" + rankMode
                + ", graphAnalysisMode=" + graphAnalysisMode
                + ", spanningMode=" + spanningMode
                + ", flowMode=" + flowMode
                + ", matchingMode=" + matchingMode
                + ", treeMode=" + treeMode
                + ", windowMode=" + windowMode
                + ", compressionMode=" + compressionMode
                + ", searchMode=" + searchMode
                + ']';
    }
}
