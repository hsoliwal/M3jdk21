// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Typed edge builder that freezes to CSR over primitive node/relation IDs.
 */
public final class MIndexGraph<N, R> {
    @FunctionalInterface
    public interface EdgeIdConsumer {
        void accept(int relationId, int targetId);
    }

    private final MIndexSpace<N> nodeSpace;
    private final MIndexSpace<R> relationSpace;
    private int[] from;
    private int[] relations;
    private int[] to;
    private int size;

    public MIndexGraph(MIndexSpace<N> nodeSpace, MIndexSpace<R> relationSpace) {
        this(nodeSpace, relationSpace, 32);
    }

    public MIndexGraph(
            MIndexSpace<N> nodeSpace,
            MIndexSpace<R> relationSpace,
            int expectedEdges) {
        this.nodeSpace = Objects.requireNonNull(nodeSpace, "nodeSpace");
        this.relationSpace = Objects.requireNonNull(relationSpace, "relationSpace");
        if (expectedEdges < 0) {
            throw new IllegalArgumentException("negative expectedEdges");
        }
        int capacity = Math.max(0, expectedEdges);
        from = new int[capacity];
        relations = new int[capacity];
        to = new int[capacity];
    }

    public MIndexSpace<N> nodeSpace() {
        return nodeSpace;
    }

    public MIndexSpace<R> relationSpace() {
        return relationSpace;
    }

    public int edgeCount() {
        return size;
    }

    public void addEdge(N source, R relation, N target) {
        addEdgeIds(nodeSpace.id(source), relationSpace.id(relation), nodeSpace.id(target));
    }

    public void addEdgeIds(int sourceId, int relationId, int targetId) {
        nodeSpace.requireId(sourceId);
        relationSpace.requireId(relationId);
        nodeSpace.requireId(targetId);
        ensureCapacity(size + 1);
        from[size] = sourceId;
        relations[size] = relationId;
        to[size] = targetId;
        size++;
    }

    /** Append a flat slice of source/relation/target triples, retaining builder duplicates. */
    public void addEdgesIds(int[] triples, int offset, int length) {
        Objects.requireNonNull(triples, "triples");
        Objects.checkFromIndexSize(offset, length, triples.length);
        if (length % 3 != 0) throw new IllegalArgumentException("incomplete edge triple");
        for (int index = offset, end = offset + length; index < end; index += 3) {
            nodeSpace.requireId(triples[index]);
            relationSpace.requireId(triples[index + 1]);
            nodeSpace.requireId(triples[index + 2]);
        }
        ensureCapacity(Math.addExact(size, length / 3));
        for (int index = offset, end = offset + length; index < end; index += 3) {
            from[size] = triples[index];
            relations[size] = triples[index + 1];
            to[size] = triples[index + 2];
            size++;
        }
    }

    /** Remove a half-open insertion-order edge range; duplicate occurrences remain distinct. */
    public void removeEdges(int fromEdge, int toEdge) {
        Objects.checkFromToIndex(fromEdge, toEdge, size);
        int removed = toEdge - fromEdge;
        System.arraycopy(from, toEdge, from, fromEdge, size - toEdge);
        System.arraycopy(relations, toEdge, relations, fromEdge, size - toEdge);
        System.arraycopy(to, toEdge, to, fromEdge, size - toEdge);
        Arrays.fill(from, size - removed, size, 0);
        Arrays.fill(relations, size - removed, size, 0);
        Arrays.fill(to, size - removed, size, 0);
        size -= removed;
    }

    public void clear() {
        removeEdges(0, size);
    }

    public void compact() {
        if (from.length == size) return;
        int[] packedFrom = Arrays.copyOf(from, size);
        int[] packedRelations = Arrays.copyOf(relations, size);
        int[] packedTo = Arrays.copyOf(to, size);
        from = packedFrom;
        relations = packedRelations;
        to = packedTo;
    }

    /**
     * Freeze sorted unique edges into CSR. Duplicate triples collapse to one
     * edge; the builder itself remains mutable.
     */
    public MIndexFrozenGraph<N, R> freeze(MIndexCompositeIndex index) {
        Objects.requireNonNull(index, "index");
        int[] lane = new int[size * 3];
        for (int edge = 0; edge < size; edge++) {
            int offset = edge * 3;
            lane[offset] = from[edge];
            lane[offset + 1] = relations[edge];
            lane[offset + 2] = to[edge];
        }
        return MIndexFrozenGraph.ofIds(nodeSpace, relationSpace, index, lane);
    }

    public Frozen<N, R> freeze() {
        int[] sortedFrom = Arrays.copyOf(from, size);
        int[] sortedRelations = Arrays.copyOf(relations, size);
        int[] sortedTo = Arrays.copyOf(to, size);
        sortTriples(sortedFrom, sortedRelations, sortedTo, size);

        int unique = 0;
        for (int index = 0; index < size; index++) {
            if (unique == 0
                    || sortedFrom[index] != sortedFrom[unique - 1]
                    || sortedRelations[index] != sortedRelations[unique - 1]
                    || sortedTo[index] != sortedTo[unique - 1]) {
                sortedFrom[unique] = sortedFrom[index];
                sortedRelations[unique] = sortedRelations[index];
                sortedTo[unique] = sortedTo[index];
                unique++;
            }
        }

        int nodeCount = nodeSpace.size();
        int[] offsets = new int[nodeCount + 1];
        for (int index = 0; index < unique; index++) {
            offsets[sortedFrom[index] + 1]++;
        }
        for (int node = 1; node < offsets.length; node++) {
            offsets[node] += offsets[node - 1];
        }
        return new Frozen<>(
                nodeSpace,
                relationSpace,
                offsets,
                Arrays.copyOf(sortedRelations, unique),
                Arrays.copyOf(sortedTo, unique));
    }

    private void ensureCapacity(int needed) {
        if (needed <= from.length) {
            return;
        }
        int capacity = IdSupport.grown(from.length, needed);
        from = Arrays.copyOf(from, capacity);
        relations = Arrays.copyOf(relations, capacity);
        to = Arrays.copyOf(to, capacity);
    }

    private static void sortTriples(int[] first, int[] second, int[] third, int length) {
        if (length > 1) {
            quickSort(first, second, third, 0, length - 1);
        }
    }

    private static void quickSort(
            int[] first,
            int[] second,
            int[] third,
            int low,
            int high) {
        while (low < high) {
            int pivotIndex = (low + high) >>> 1;
            int p1 = first[pivotIndex];
            int p2 = second[pivotIndex];
            int p3 = third[pivotIndex];
            int left = low;
            int right = high;
            while (left <= right) {
                while (compare(first[left], second[left], third[left], p1, p2, p3) < 0) {
                    left++;
                }
                while (compare(first[right], second[right], third[right], p1, p2, p3) > 0) {
                    right--;
                }
                if (left <= right) {
                    swap(first, left, right);
                    swap(second, left, right);
                    swap(third, left, right);
                    left++;
                    right--;
                }
            }
            if (right - low < high - left) {
                if (low < right) {
                    quickSort(first, second, third, low, right);
                }
                low = left;
            } else {
                if (left < high) {
                    quickSort(first, second, third, left, high);
                }
                high = right;
            }
        }
    }

    private static int compare(int a1, int a2, int a3, int b1, int b2, int b3) {
        int result = Integer.compare(a1, b1);
        if (result == 0) {
            result = Integer.compare(a2, b2);
        }
        if (result == 0) {
            result = Integer.compare(a3, b3);
        }
        return result;
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }

    /** Immutable CSR graph view; all per-edge state is primitive. */
    public static final class Frozen<N, R> {
        private final MIndexSpace<N> nodeSpace;
        private final MIndexSpace<R> relationSpace;
        private final int[] offsets;
        private final int[] relations;
        private final int[] targets;

        private Frozen(
                MIndexSpace<N> nodeSpace,
                MIndexSpace<R> relationSpace,
                int[] offsets,
                int[] relations,
                int[] targets) {
            this.nodeSpace = nodeSpace;
            this.relationSpace = relationSpace;
            this.offsets = offsets;
            this.relations = relations;
            this.targets = targets;
        }

        public MIndexSpace<N> nodeSpace() {
            return nodeSpace;
        }

        public MIndexSpace<R> relationSpace() {
            return relationSpace;
        }

        public int nodeCount() {
            return offsets.length - 1;
        }

        public int edgeCount() {
            return targets.length;
        }

        public int outDegreeId(int nodeId) {
            requireNodeId(nodeId);
            return offsets[nodeId + 1] - offsets[nodeId];
        }

        public int outDegree(N node) {
            int id = nodeSpace.findId(node);
            return id < 0 ? 0 : outDegreeId(id);
        }

        public void forEachOutgoingId(int nodeId, EdgeIdConsumer action) {
            Objects.requireNonNull(action, "action");
            requireNodeId(nodeId);
            int start = offsets[nodeId];
            int end = offsets[nodeId + 1];
            for (int index = start; index < end; index++) {
                action.accept(relations[index], targets[index]);
            }
        }

        public int canonicalEdgeSetId(MIndexCompositeIndex index) {
            return canonical(index).canonicalId();
        }

        public MIndexFrozenGraph<N, R> canonical(MIndexCompositeIndex index) {
            return MIndexFrozenGraph.copyOf(this, Objects.requireNonNull(index, "index"));
        }

        public int[] copyCanonicalEdgeLane() {
            int[] lane = new int[targets.length * 3];
            int cursor = 0;
            for (int source = 0; source + 1 < offsets.length; source++) {
                for (int edge = offsets[source]; edge < offsets[source + 1]; edge++) {
                    lane[cursor++] = source;
                    lane[cursor++] = relations[edge];
                    lane[cursor++] = targets[edge];
                }
            }
            return lane;
        }

        public N node(int id) {
            return nodeSpace.value(id);
        }

        public R relation(int id) {
            return relationSpace.value(id);
        }

        public int[] copyOffsets() {
            return offsets.clone();
        }

        public int[] copyRelationIds() {
            return relations.clone();
        }

        public int[] copyTargetIds() {
            return targets.clone();
        }

        private void requireNodeId(int nodeId) {
            if (nodeId < 0 || nodeId + 1 >= offsets.length) {
                throw new IndexOutOfBoundsException("node id: " + nodeId);
            }
        }
    }
}
