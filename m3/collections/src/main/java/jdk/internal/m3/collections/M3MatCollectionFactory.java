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

import java.util.Arrays;
import java.util.Objects;

/**
 * Deterministic fuzzy factory. It chooses from admitted physical implementations using actual
 * primitive-payload measurements plus declared workload requirements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionFactory.java}
 * blob {@code d3d4d5704a6f2e405145320c7a13a4c660122a4e} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatCollectionFactory {
    private M3MatCollectionFactory() { }

    public static M3MatPlanned<M3MatPlannedIntVector> intVector(
            int[] values, M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, M3MatCollectionRequirements.Shape.VECTOR);
        M3MatAdaptiveIntVector adaptive = M3MatAdaptiveIntVector.of(values);
        M3MatBlockIntVector block = M3MatBlockIntVector.of(values);
        int mode = mode(values);
        M3MatSparseIntVector sparse = M3MatSparseIntVector.fromDense(values, mode);

        long adaptiveBytes = adaptive.primitivePayloadBytes();
        long blockBytes = block.primitivePayloadBytes();
        long sparseBytes = sparse.primitivePayloadBytes();
        long minimum = Math.min(adaptiveBytes, Math.min(blockBytes, sparseBytes));

        M3MatPlannedIntVector selected;
        if (requirements.policy() == M3MatCollectionPolicy.MIN_MEMORY) {
            selected = adaptiveBytes == minimum ? M3MatPlannedIntVector.adaptive(adaptive)
                    : blockBytes == minimum ? M3MatPlannedIntVector.block(block)
                    : M3MatPlannedIntVector.sparse(sparse);
        } else if (requirements.policy() == M3MatCollectionPolicy.FAST_LOOKUP) {
            selected = adaptiveBytes <= blockBytes * 2L
                    ? M3MatPlannedIntVector.adaptive(adaptive)
                    : M3MatPlannedIntVector.block(block);
        } else if (requirements.accessPattern() == M3MatCollectionRequirements.AccessPattern.ITERATION_HEAVY) {
            selected = adaptiveBytes <= blockBytes
                    ? M3MatPlannedIntVector.adaptive(adaptive)
                    : M3MatPlannedIntVector.block(block);
        } else {
            long plainBytes = 4L * values.length;
            selected = sparseBytes < Math.min(plainBytes, minimum * 2L)
                    ? M3MatPlannedIntVector.sparse(sparse)
                    : adaptiveBytes <= blockBytes
                            ? M3MatPlannedIntVector.adaptive(adaptive)
                            : M3MatPlannedIntVector.block(block);
        }

        M3MatCollectionStats stats = M3MatCollectionStats.of(values);
        String statText = statsText(stats)
                + ";mode=" + mode
                + ";candidates=adaptive:" + adaptiveBytes
                + ",block:" + blockBytes
                + ",sparse:" + sparseBytes;
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntVector/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new M3MatPlanned<>(selected, receipt);
    }

    public static M3MatPlanned<M3MatPlannedIntSet> intSet(
            int[] values, M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, M3MatCollectionRequirements.Shape.SET);
        M3MatCollectionStats stats = M3MatCollectionStats.of(values);
        M3MatRoaringIntSet roaring = M3MatRoaringIntSet.of(values);
        M3MatAdaptiveIntSet adaptive = null;
        long adaptiveBytes = Long.MAX_VALUE;
        if (stats.distinct() == 0) {
            adaptive = M3MatAdaptiveIntSet.of(0);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        } else if (stats.min() >= 0 && stats.max() < 100_000_000) {
            adaptive = M3MatAdaptiveIntSet.of(stats.max() + 1, values);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        }

        M3MatPlannedIntSet selected;
        if (requirements.rankSelect() || adaptive == null) {
            selected = M3MatPlannedIntSet.roaring(roaring);
        } else if (requirements.policy() == M3MatCollectionPolicy.FAST_LOOKUP
                && adaptive.encoding() == M3MatAdaptiveIntSet.Encoding.BITMAP) {
            selected = M3MatPlannedIntSet.adaptive(adaptive);
        } else if (adaptiveBytes <= roaring.primitivePayloadBytes()) {
            selected = M3MatPlannedIntSet.adaptive(adaptive);
        } else {
            selected = M3MatPlannedIntSet.roaring(roaring);
        }

        String statText = statsText(stats)
                + ";candidates=adaptive:" + (adaptive == null ? "NA" : adaptiveBytes)
                + ",roaring:" + roaring.primitivePayloadBytes()
                + ";roaringChunks=" + roaring.chunks();
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntSet/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new M3MatPlanned<>(selected, receipt);
    }

    public static M3MatPlanned<M3MatAdaptiveIntIntMap> intIntMap(
            int[] keys, int[] values, M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, M3MatCollectionRequirements.Shape.MAP);
        if (keys.length != values.length) throw new IllegalArgumentException("length mismatch");
        M3MatAdaptiveIntIntMap.Builder builder = M3MatAdaptiveIntIntMap.builder(requirements.policy());
        for (int i = 0; i < keys.length; i++) builder.put(keys[i], values[i]);
        M3MatAdaptiveIntIntMap map = builder.build();
        M3MatCollectionStats stats = M3MatCollectionStats.of(keys);
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statsText(stats),
                "M3MatAdaptiveIntIntMap/" + map.encoding(),
                map.primitivePayloadBytes(),
                map.primitivePayloadBytes());
        return new M3MatPlanned<>(map, receipt);
    }

    public static M3MatPlanned<M3MatIntPostingIndex> postingIndex(
            int[] keyByRow, M3MatCollectionRequirements requirements) {
        requireShape(requirements, M3MatCollectionRequirements.Shape.POSTING_INDEX);
        M3MatIntPostingIndex index = M3MatIntPostingIndex.ofKeys(keyByRow);
        M3MatCollectionStats stats = M3MatCollectionStats.of(keyByRow);
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statsText(stats) + ";distinctKeys=" + index.keyCount()
                        + ";postings=" + index.postingCount(),
                "M3MatIntPostingIndex/CSR",
                index.primitivePayloadBytes(),
                index.primitivePayloadBytes());
        return new M3MatPlanned<>(index, receipt);
    }

    public static M3MatPlanned<M3MatIntRangeSet> rangeSet(
            long[] starts,
            long[] ends,
            M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        requireShape(requirements, M3MatCollectionRequirements.Shape.RANGE_SET);
        if (starts.length != ends.length) throw new IllegalArgumentException("length mismatch");
        M3MatIntRangeSet.Builder builder = M3MatIntRangeSet.builder();
        for (int i = 0; i < starts.length; i++) builder.add(starts[i], ends[i]);
        M3MatIntRangeSet value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges()
                + ";cardinality=" + value.cardinality();
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeSet/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new M3MatPlanned<>(value, receipt);
    }

    public static M3MatPlanned<M3MatIntRangeMap> rangeMap(
            long[] starts,
            long[] ends,
            int[] values,
            M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, M3MatCollectionRequirements.Shape.RANGE_MAP);
        if (starts.length != ends.length || starts.length != values.length) {
            throw new IllegalArgumentException("length mismatch");
        }
        M3MatIntRangeMap.Builder builder = M3MatIntRangeMap.builder();
        for (int i = 0; i < starts.length; i++) builder.put(starts[i], ends[i], values[i]);
        M3MatIntRangeMap value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges();
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeMap/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new M3MatPlanned<>(value, receipt);
    }

    public static M3MatPlanned<M3MatAdaptiveIntGraph> graph(
            int nodes,
            int[] from,
            int[] to,
            M3MatCollectionRequirements requirements) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        requireShape(requirements, M3MatCollectionRequirements.Shape.GRAPH);
        M3MatAdaptiveIntGraph graph = M3MatAdaptiveIntGraph.of(nodes, from, to, requirements.policy());
        long possibleEdges = (long) nodes * nodes;
        String statistics = "nodes=" + nodes
                + ";inputEdges=" + from.length
                + ";uniqueEdges=" + graph.edges()
                + ";possibleEdges=" + possibleEdges
                + ";encoding=" + graph.encoding();
        M3MatCollectionPlanReceipt receipt = M3MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatAdaptiveIntGraph/" + graph.encoding(),
                graph.primitivePayloadBytes(),
                graph.primitivePayloadBytes());
        return new M3MatPlanned<>(graph, receipt);
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

    private static String statsText(M3MatCollectionStats stats) {
        return "size=" + stats.size()
                + ";distinct=" + stats.distinct()
                + ";min=" + stats.min()
                + ";max=" + stats.max()
                + ";runs=" + stats.runs()
                + ";span=" + stats.span();
    }

    private static void requireShape(
            M3MatCollectionRequirements requirements, M3MatCollectionRequirements.Shape expected) {
        Objects.requireNonNull(requirements, "requirements");
        if (requirements.shape() != expected) {
            throw new IllegalArgumentException(
                    "requirements shape " + requirements.shape() + " != " + expected);
        }
        if (requirements.mutationModel() != M3MatCollectionRequirements.MutationModel.BUILD_THEN_FREEZE) {
            throw new IllegalArgumentException("this factory method publishes immutable collections");
        }
    }
}
