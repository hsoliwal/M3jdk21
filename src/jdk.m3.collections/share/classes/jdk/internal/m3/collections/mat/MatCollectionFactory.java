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
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionFactory.java}
 * blob {@code d3d4d5704a6f2e405145320c7a13a4c660122a4e} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatCollectionFactory {
    private MatCollectionFactory() { }

    public static MatPlanned<MatPlannedIntVector> intVector(
            int[] values, MatCollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, MatCollectionRequirements.Shape.VECTOR);
        MatAdaptiveIntVector adaptive = MatAdaptiveIntVector.of(values);
        MatBlockIntVector block = MatBlockIntVector.of(values);
        int mode = mode(values);
        MatSparseIntVector sparse = MatSparseIntVector.fromDense(values, mode);

        long adaptiveBytes = adaptive.primitivePayloadBytes();
        long blockBytes = block.primitivePayloadBytes();
        long sparseBytes = sparse.primitivePayloadBytes();
        long minimum = Math.min(adaptiveBytes, Math.min(blockBytes, sparseBytes));

        MatPlannedIntVector selected;
        if (requirements.policy() == MatCollectionPolicy.MIN_MEMORY) {
            selected = adaptiveBytes == minimum ? MatPlannedIntVector.adaptive(adaptive)
                    : blockBytes == minimum ? MatPlannedIntVector.block(block)
                    : MatPlannedIntVector.sparse(sparse);
        } else if (requirements.policy() == MatCollectionPolicy.FAST_LOOKUP) {
            selected = adaptiveBytes <= blockBytes * 2L
                    ? MatPlannedIntVector.adaptive(adaptive)
                    : MatPlannedIntVector.block(block);
        } else if (requirements.accessPattern() == MatCollectionRequirements.AccessPattern.ITERATION_HEAVY) {
            selected = adaptiveBytes <= blockBytes
                    ? MatPlannedIntVector.adaptive(adaptive)
                    : MatPlannedIntVector.block(block);
        } else {
            long plainBytes = 4L * values.length;
            selected = sparseBytes < Math.min(plainBytes, minimum * 2L)
                    ? MatPlannedIntVector.sparse(sparse)
                    : adaptiveBytes <= blockBytes
                            ? MatPlannedIntVector.adaptive(adaptive)
                            : MatPlannedIntVector.block(block);
        }

        MatCollectionStats stats = MatCollectionStats.of(values);
        String statText = statsText(stats)
                + ";mode=" + mode
                + ";candidates=adaptive:" + adaptiveBytes
                + ",block:" + blockBytes
                + ",sparse:" + sparseBytes;
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntVector/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new MatPlanned<>(selected, receipt);
    }

    public static MatPlanned<MatPlannedIntSet> intSet(
            int[] values, MatCollectionRequirements requirements) {
        Objects.requireNonNull(values, "values");
        requireShape(requirements, MatCollectionRequirements.Shape.SET);
        MatCollectionStats stats = MatCollectionStats.of(values);
        MatRoaringIntSet roaring = MatRoaringIntSet.of(values);
        MatAdaptiveIntSet adaptive = null;
        long adaptiveBytes = Long.MAX_VALUE;
        if (stats.distinct() == 0) {
            adaptive = MatAdaptiveIntSet.of(0);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        } else if (stats.min() >= 0 && stats.max() < 100_000_000) {
            adaptive = MatAdaptiveIntSet.of(stats.max() + 1, values);
            adaptiveBytes = adaptive.primitivePayloadBytes();
        }

        MatPlannedIntSet selected;
        if (requirements.rankSelect() || adaptive == null) {
            selected = MatPlannedIntSet.roaring(roaring);
        } else if (requirements.policy() == MatCollectionPolicy.FAST_LOOKUP
                && adaptive.encoding() == MatAdaptiveIntSet.Encoding.BITMAP) {
            selected = MatPlannedIntSet.adaptive(adaptive);
        } else if (adaptiveBytes <= roaring.primitivePayloadBytes()) {
            selected = MatPlannedIntSet.adaptive(adaptive);
        } else {
            selected = MatPlannedIntSet.roaring(roaring);
        }

        String statText = statsText(stats)
                + ";candidates=adaptive:" + (adaptive == null ? "NA" : adaptiveBytes)
                + ",roaring:" + roaring.primitivePayloadBytes()
                + ";roaringChunks=" + roaring.chunks();
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statText,
                "M3MatPlannedIntSet/" + selected.kind(),
                selected.primitivePayloadBytes(),
                selected.primitivePayloadBytes());
        return new MatPlanned<>(selected, receipt);
    }

    public static MatPlanned<MatAdaptiveIntIntMap> intIntMap(
            int[] keys, int[] values, MatCollectionRequirements requirements) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, MatCollectionRequirements.Shape.MAP);
        if (keys.length != values.length) throw new IllegalArgumentException("length mismatch");
        MatAdaptiveIntIntMap.Builder builder = MatAdaptiveIntIntMap.builder(requirements.policy());
        for (int i = 0; i < keys.length; i++) builder.put(keys[i], values[i]);
        MatAdaptiveIntIntMap map = builder.build();
        MatCollectionStats stats = MatCollectionStats.of(keys);
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statsText(stats),
                "M3MatAdaptiveIntIntMap/" + map.encoding(),
                map.primitivePayloadBytes(),
                map.primitivePayloadBytes());
        return new MatPlanned<>(map, receipt);
    }

    public static MatPlanned<IntPostingIndex> postingIndex(
            int[] keyByRow, MatCollectionRequirements requirements) {
        requireShape(requirements, MatCollectionRequirements.Shape.POSTING_INDEX);
        IntPostingIndex index = IntPostingIndex.ofKeys(keyByRow);
        MatCollectionStats stats = MatCollectionStats.of(keyByRow);
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statsText(stats) + ";distinctKeys=" + index.keyCount()
                        + ";postings=" + index.postingCount(),
                "M3MatIntPostingIndex/CSR",
                index.primitivePayloadBytes(),
                index.primitivePayloadBytes());
        return new MatPlanned<>(index, receipt);
    }

    public static MatPlanned<MatIntRangeSet> rangeSet(
            long[] starts,
            long[] ends,
            MatCollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        requireShape(requirements, MatCollectionRequirements.Shape.RANGE_SET);
        if (starts.length != ends.length) throw new IllegalArgumentException("length mismatch");
        MatIntRangeSet.Builder builder = MatIntRangeSet.builder();
        for (int i = 0; i < starts.length; i++) builder.add(starts[i], ends[i]);
        MatIntRangeSet value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges()
                + ";cardinality=" + value.cardinality();
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeSet/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new MatPlanned<>(value, receipt);
    }

    public static MatPlanned<MatIntRangeMap> rangeMap(
            long[] starts,
            long[] ends,
            int[] values,
            MatCollectionRequirements requirements) {
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        Objects.requireNonNull(values, "values");
        requireShape(requirements, MatCollectionRequirements.Shape.RANGE_MAP);
        if (starts.length != ends.length || starts.length != values.length) {
            throw new IllegalArgumentException("length mismatch");
        }
        MatIntRangeMap.Builder builder = MatIntRangeMap.builder();
        for (int i = 0; i < starts.length; i++) builder.put(starts[i], ends[i], values[i]);
        MatIntRangeMap value = builder.build();
        String statistics = "inputRanges=" + starts.length
                + ";canonicalRanges=" + value.ranges();
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatIntRangeMap/DISJOINT_RANGES",
                value.primitivePayloadBytes(),
                value.primitivePayloadBytes());
        return new MatPlanned<>(value, receipt);
    }

    public static MatPlanned<MatAdaptiveIntGraph> graph(
            int nodes,
            int[] from,
            int[] to,
            MatCollectionRequirements requirements) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        requireShape(requirements, MatCollectionRequirements.Shape.GRAPH);
        MatAdaptiveIntGraph graph = MatAdaptiveIntGraph.of(nodes, from, to, requirements.policy());
        long possibleEdges = (long) nodes * nodes;
        String statistics = "nodes=" + nodes
                + ";inputEdges=" + from.length
                + ";uniqueEdges=" + graph.edges()
                + ";possibleEdges=" + possibleEdges
                + ";encoding=" + graph.encoding();
        MatCollectionPlanReceipt receipt = MatCollectionPlanReceipt.of(
                requirements,
                statistics,
                "M3MatAdaptiveIntGraph/" + graph.encoding(),
                graph.primitivePayloadBytes(),
                graph.primitivePayloadBytes());
        return new MatPlanned<>(graph, receipt);
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

    private static String statsText(MatCollectionStats stats) {
        return "size=" + stats.size()
                + ";distinct=" + stats.distinct()
                + ";min=" + stats.min()
                + ";max=" + stats.max()
                + ";runs=" + stats.runs()
                + ";span=" + stats.span();
    }

    private static void requireShape(
            MatCollectionRequirements requirements, MatCollectionRequirements.Shape expected) {
        Objects.requireNonNull(requirements, "requirements");
        if (requirements.shape() != expected) {
            throw new IllegalArgumentException(
                    "requirements shape " + requirements.shape() + " != " + expected);
        }
        if (requirements.mutationModel() != MatCollectionRequirements.MutationModel.BUILD_THEN_FREEZE) {
            throw new IllegalArgumentException("this factory method publishes immutable collections");
        }
    }
}
