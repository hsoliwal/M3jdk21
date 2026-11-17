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
package jdk.internal.m3.collections.mat;

import jdk.m3.arrays.IntPostingIndex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Deterministic fuzzy factory. It chooses from admitted physical implementations using actual
 * primitive-payload measurements plus declared workload requirements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/CollectionFactory.java}
 * blob {@code d3d4d5704a6f2e405145320c7a13a4c660122a4e} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionFactory {
    private CollectionFactory() { }

    public static Planned<PlannedIntVector> intVector(
            int[] values, CollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, CollectionRequirements.Shape.VECTOR);
        AdaptiveIntVector adaptive = AdaptiveIntVector.of(values);
        BlockIntVector block = BlockIntVector.of(values);
        int mode = mode(values);
        SparseIntVector sparse = SparseIntVector.fromDense(values, mode);

        long adaptiveBytes = adaptive.primitivePayloadBytes();
        long blockBytes = block.primitivePayloadBytes();
        long sparseBytes = sparse.primitivePayloadBytes();
        long minimum = Math.min(adaptiveBytes, Math.min(blockBytes, sparseBytes));

        PlannedIntVector selected;
        if (requirements.policy() == CollectionPolicy.MIN_MEMORY) {
            selected = adaptiveBytes == minimum ? PlannedIntVector.adaptive(adaptive)
                    : blockBytes == minimum ? PlannedIntVector.block(block)
                    : PlannedIntVector.sparse(sparse);
        } else if (requirements.policy() == CollectionPolicy.FAST_LOOKUP) {
            selected = adaptiveBytes <= blockBytes * 2L
                    ? PlannedIntVector.adaptive(adaptive)
                    : PlannedIntVector.block(block);
        } else if (requirements.accessPattern() == CollectionRequirements.AccessPattern.ITERATION_HEAVY) {
            selected = adaptiveBytes <= blockBytes
                    ? PlannedIntVector.adaptive(adaptive)
                    : PlannedIntVector.block(block);
        } else {
            long plainBytes = 4L * values.length;
            selected = sparseBytes < Math.min(plainBytes, minimum * 2L)
                    ? PlannedIntVector.sparse(sparse)
                    : adaptiveBytes <= blockBytes
                            ? PlannedIntVector.adaptive(adaptive)
                            : PlannedIntVector.block(block);
        }

        CollectionStats stats = CollectionStats.of(values);
        String statText = statsText(stats)
                + ";mode=" + mode
                + ";candidates=adaptive:" + adaptiveBytes
                + ",block:" + blockBytes
                + ",sparse:" + sparseBytes;
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntVector/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new Planned<>(selected, receipt);
    }

    public static Planned<PlannedIntSet> intSet(
            int[] values, CollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, CollectionRequirements.Shape.SET);
        CollectionStats stats = CollectionStats.of(values);
        RoaringIntSet roaring = RoaringIntSet.of(values);
        AdaptiveIntSet adaptive = null;
        long adaptiveBytes = Long.MAX_VALUE;
        if (stats.distinct() == 0) {
            adaptive = AdaptiveIntSet.of(0);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        } else if (stats.min() >= 0 && stats.max() < 100_000_000) {
            adaptive = AdaptiveIntSet.of(stats.max() + 1, values);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        }

        PlannedIntSet selected;
        if (requirements.rankSelect() || adaptive == null) {
            selected = PlannedIntSet.roaring(roaring);
        } else if (requirements.policy() == CollectionPolicy.FAST_LOOKUP
                && adaptive.encoding() == AdaptiveIntSet.Encoding.BITMAP) {
            selected = PlannedIntSet.adaptive(adaptive);
        } else if (adaptiveBytes <= roaring.primitivePayloadBytes()) {
            selected = PlannedIntSet.adaptive(adaptive);
        } else {
            selected = PlannedIntSet.roaring(roaring);
        }

        String statText = statsText(stats)
                + ";candidates=adaptive:" + (adaptive == null ? "NA" : adaptiveBytes)
                + ",roaring:" + roaring.primitivePayloadBytes()
                + ";roaringChunks=" + roaring.chunks();
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntSet/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new Planned<>(selected, receipt);
    }

    public static Planned<AdaptiveIntIntMap> intIntMap(
            int[] keys, int[] values, CollectionRequirements requirements) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, CollectionRequirements.Shape.MAP);
        if (keys.length != values.length) throw new IllegalArgumentException("length mismatch");
        AdaptiveIntIntMap.Builder builder = AdaptiveIntIntMap.builder(requirements.policy());
        for (int i = 0; i < keys.length; i++) builder.put(keys[i], values[i]);
        AdaptiveIntIntMap map = builder.build();
        CollectionStats stats = CollectionStats.of(keys);
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statsText(stats),
                "M3MatAdaptiveIntIntMap/" + map.encoding(),
                map.primitivePayloadBytes(),
                map.primitivePayloadBytes());
        return new Planned<>(map, receipt);
    }

    public static Planned<IntPostingIndex> postingIndex(
            int[] keyByRow, CollectionRequirements requirements) {
        requireShape(requirements, CollectionRequirements.Shape.POSTING_INDEX);
        IntPostingIndex index = IntPostingIndex.ofKeys(keyByRow);
        CollectionStats stats = CollectionStats.of(keyByRow);
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statsText(stats) + ";distinctKeys=" + index.keyCount()
                        + ";postings=" + index.postingCount(),
                "M3MatIntPostingIndex/CSR",
                index.primitivePayloadBytes(),
                index.primitivePayloadBytes());
        return new Planned<>(index, receipt);
    }

    public static Planned<IntRangeSet> rangeSet(
            long[] starts,
            long[] ends,
            CollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        requireShape(requirements, CollectionRequirements.Shape.RANGE_SET);
        if (starts.length != ends.length) throw new IllegalArgumentException("length mismatch");
        IntRangeSet.Builder builder = IntRangeSet.builder();
        for (int i = 0; i < starts.length; i++) builder.add(starts[i], ends[i]);
        IntRangeSet value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges()
                + ";cardinality=" + value.cardinality();
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeSet/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new Planned<>(value, receipt);
    }

    public static Planned<IntRangeMap> rangeMap(
            long[] starts,
            long[] ends,
            int[] values,
            CollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, CollectionRequirements.Shape.RANGE_MAP);
        if (starts.length != ends.length || starts.length != values.length) {
            throw new IllegalArgumentException("length mismatch");
        }
        IntRangeMap.Builder builder = IntRangeMap.builder();
        for (int i = 0; i < starts.length; i++) builder.put(starts[i], ends[i], values[i]);
        IntRangeMap value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges();
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeMap/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new Planned<>(value, receipt);
    }

    public static Planned<AdaptiveIntGraph> graph(
            int nodes,
            int[] from,
            int[] to,
            CollectionRequirements requirements) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        requireShape(requirements, CollectionRequirements.Shape.GRAPH);
        AdaptiveIntGraph graph = AdaptiveIntGraph.of(nodes, from, to, requirements.policy());
        long possibleEdges = (long) nodes * nodes;
        String statistics = "nodes=" + nodes
                + ";inputEdges=" + from.length
                + ";uniqueEdges=" + graph.edges()
                + ";possibleEdges=" + possibleEdges
                + ";encoding=" + graph.encoding();
        CollectionPlanReceipt receipt = CollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatAdaptiveIntGraph/" + graph.encoding(),
                graph.primitivePayloadBytes(),
                graph.primitivePayloadBytes());
        return new Planned<>(graph, receipt);
    }

    private static int mode(int[] values) {
        if (values.length == 0) return 0;
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        int bestValue = sorted[0];
        int bestCount = 1;
        int currentValue = sorted[0];
        int currentCount = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] == currentValue) {
                currentCount++;
            } else {
                if (currentCount > bestCount) {
                    bestCount = currentCount;
                    bestValue = currentValue;
                }
                currentValue = sorted[i];
                currentCount = 1;
            }
        }
        if (currentCount > bestCount) bestValue = currentValue;
        return bestValue;
    }

    private static String statsText(CollectionStats stats) {
        return "size=" + stats.size()
                + ";distinct=" + stats.distinct()
                + ";min=" + stats.min()
                + ";max=" + stats.max()
                + ";runs=" + stats.runs()
                + ";span=" + stats.span();
    }

    private static void requireShape(
            CollectionRequirements requirements, CollectionRequirements.Shape expected) {
        Objects.requireNonNull(requirements, "requirements");
        if (requirements.shape() != expected) {
            throw new IllegalArgumentException(
                    "requirements shape " + requirements.shape() + " != " + expected);
        }
        if (requirements.mutationModel() != CollectionRequirements.MutationModel.BUILD_THEN_FREEZE) {
            throw new IllegalArgumentException("this factory method publishes immutable collections");
        }
    }
}
