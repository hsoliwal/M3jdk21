// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * First-class immutable canonical graph represented by sorted unique primitive
 * {@code (sourceId, relationId, targetId)} triples.
 *
 * <p>CSR remains a useful physical traversal representation in
 * {@link MIndexGraph.Frozen}; this class is the canonical semantic value.
 */
public final class MIndexFrozenGraph<N, R> implements MIndexCanonicalCollection {
    @FunctionalInterface
    public interface EdgeTripleIdConsumer {
        void accept(int sourceId, int relationId, int targetId);
    }

    private final MIndexSpace<N> nodeSpace;
    private final MIndexSpace<R> relationSpace;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenGraph(
            MIndexSpace<N> nodeSpace,
            MIndexSpace<R> relationSpace,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.nodeSpace = Objects.requireNonNull(nodeSpace, "nodeSpace");
        this.relationSpace = Objects.requireNonNull(relationSpace, "relationSpace");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <N, R> MIndexFrozenGraph<N, R> copyOf(
            MIndexGraph.Frozen<N, R> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return fromNormalizedIds(
                source.nodeSpace(),
                source.relationSpace(),
                index,
                source.copyCanonicalEdgeLane());
    }

    public static <N, R> MIndexFrozenGraph<N, R> ofIds(
            MIndexSpace<N> nodeSpace,
            MIndexSpace<R> relationSpace,
            MIndexCompositeIndex index,
            int[] edgeTriples) {
        Objects.requireNonNull(nodeSpace, "nodeSpace");
        Objects.requireNonNull(relationSpace, "relationSpace");
        Objects.requireNonNull(index, "index");
        int[] lane = Objects.requireNonNull(edgeTriples, "edgeTriples").clone();
        if (lane.length % 3 != 0) {
            throw new IllegalArgumentException(
                    "graph lane must contain source/relation/target triples");
        }

        for (int position = 0; position < lane.length; position += 3) {
            nodeSpace.requireId(lane[position]);
            relationSpace.requireId(lane[position + 1]);
            nodeSpace.requireId(lane[position + 2]);
        }
        sortTriples(lane);

        int uniqueLength = 0;
        for (int position = 0; position < lane.length; position += 3) {
            if (uniqueLength == 0
                    || compare(
                            lane[position],
                            lane[position + 1],
                            lane[position + 2],
                            lane[uniqueLength - 3],
                            lane[uniqueLength - 2],
                            lane[uniqueLength - 1]) != 0) {
                lane[uniqueLength] = lane[position];
                lane[uniqueLength + 1] = lane[position + 1];
                lane[uniqueLength + 2] = lane[position + 2];
                uniqueLength += 3;
            }
        }
        if (uniqueLength != lane.length) {
            lane = Arrays.copyOf(lane, uniqueLength);
        }
        return fromNormalizedIds(nodeSpace, relationSpace, index, lane);
    }

    public MIndexSpace<N> nodeSpace() {
        return nodeSpace;
    }

    public MIndexSpace<R> relationSpace() {
        return relationSpace;
    }

    @Override
    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int canonicalId() {
        return canonicalId;
    }

    @Override
    public int kind() {
        return MIndexCompositeIndex.KIND_GRAPH_EDGE_SET;
    }

    @Override
    public int size() {
        return edgeCount();
    }

    public int edgeCount() {
        return index.length(canonicalId) / 3;
    }

    public boolean isEmpty() {
        return edgeCount() == 0;
    }

    public boolean containsEdge(N source, R relation, N target) {
        int sourceId = nodeSpace.findId(source);
        int relationId = relationSpace.findId(relation);
        int targetId = nodeSpace.findId(target);
        return sourceId >= 0
                && relationId >= 0
                && targetId >= 0
                && containsEdgeIds(sourceId, relationId, targetId);
    }

    public boolean containsEdgeIds(int sourceId, int relationId, int targetId) {
        return findEdge(sourceId, relationId, targetId) >= 0;
    }

    public int outDegree(N node) {
        int nodeId = nodeSpace.findId(node);
        return nodeId < 0 ? 0 : outDegreeId(nodeId);
    }

    public int outDegreeId(int nodeId) {
        nodeSpace.requireId(nodeId);
        return upperBoundSource(nodeId) - lowerBoundSource(nodeId);
    }

    public void forEachEdgeId(EdgeTripleIdConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int edge = 0; edge < edgeCount(); edge++) {
            action.accept(
                    sourceIdAt(edge),
                    relationIdAt(edge),
                    targetIdAt(edge));
        }
    }

    public void forEachOutgoingId(
            int nodeId,
            MIndexGraph.EdgeIdConsumer action) {
        Objects.requireNonNull(action, "action");
        nodeSpace.requireId(nodeId);
        int start = lowerBoundSource(nodeId);
        int end = upperBoundSource(nodeId);
        for (int edge = start; edge < end; edge++) {
            action.accept(relationIdAt(edge), targetIdAt(edge));
        }
    }

    public MIndexFrozenGraph<N, R> withEdge(
            N source,
            R relation,
            N target) {
        return withEdgeIds(
                nodeSpace.id(source),
                relationSpace.id(relation),
                nodeSpace.id(target));
    }

    public MIndexFrozenGraph<N, R> withEdgeIds(
            int sourceId,
            int relationId,
            int targetId) {
        nodeSpace.requireId(sourceId);
        relationSpace.requireId(relationId);
        nodeSpace.requireId(targetId);
        int position = findEdge(sourceId, relationId, targetId);
        if (position >= 0) {
            return this;
        }
        int insertion = -position - 1;
        int[] source = copyEdgeLane();
        int[] target = new int[source.length + 3];
        int offset = insertion * 3;
        System.arraycopy(source, 0, target, 0, offset);
        target[offset] = sourceId;
        target[offset + 1] = relationId;
        target[offset + 2] = targetId;
        System.arraycopy(
                source,
                offset,
                target,
                offset + 3,
                source.length - offset);
        return fromNormalizedIds(nodeSpace, relationSpace, index, target);
    }

    public MIndexFrozenGraph<N, R> withoutEdge(
            N source,
            R relation,
            N target) {
        int sourceId = nodeSpace.findId(source);
        int relationId = relationSpace.findId(relation);
        int targetId = nodeSpace.findId(target);
        if (sourceId < 0 || relationId < 0 || targetId < 0) {
            return this;
        }
        return withoutEdgeIds(sourceId, relationId, targetId);
    }

    public MIndexFrozenGraph<N, R> withoutEdgeIds(
            int sourceId,
            int relationId,
            int targetId) {
        int edge = findEdge(sourceId, relationId, targetId);
        if (edge < 0) {
            return this;
        }
        int[] source = copyEdgeLane();
        int[] target = new int[source.length - 3];
        int offset = edge * 3;
        System.arraycopy(source, 0, target, 0, offset);
        System.arraycopy(
                source,
                offset + 3,
                target,
                offset,
                source.length - offset - 3);
        return fromNormalizedIds(nodeSpace, relationSpace, index, target);
    }

    public N node(int id) {
        return nodeSpace.value(id);
    }

    public R relation(int id) {
        return relationSpace.value(id);
    }

    public int[] copyEdgeLane() {
        return index.copyLane(canonicalId);
    }

    public MIndexGraph<N, R> mutableCopy() {
        MIndexGraph<N, R> graph =
                new MIndexGraph<>(nodeSpace, relationSpace, edgeCount());
        for (int edge = 0; edge < edgeCount(); edge++) {
            graph.addEdgeIds(
                    sourceIdAt(edge),
                    relationIdAt(edge),
                    targetIdAt(edge));
        }
        return graph;
    }

    @Override
    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    @Override
    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexFrozenGraph<?, ?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {
        return "MIndexFrozenGraph[id=" + canonicalId
                + ",edges=" + edgeCount() + ']';
    }

    private int sourceIdAt(int edge) {
        return index.valueAt(canonicalId, Objects.checkIndex(edge, edgeCount()) * 3);
    }

    private int relationIdAt(int edge) {
        return index.valueAt(
                canonicalId,
                Objects.checkIndex(edge, edgeCount()) * 3 + 1);
    }

    private int targetIdAt(int edge) {
        return index.valueAt(
                canonicalId,
                Objects.checkIndex(edge, edgeCount()) * 3 + 2);
    }

    private int findEdge(int sourceId, int relationId, int targetId) {
        int low = 0;
        int high = edgeCount() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int comparison = compare(
                    sourceIdAt(middle),
                    relationIdAt(middle),
                    targetIdAt(middle),
                    sourceId,
                    relationId,
                    targetId);
            if (comparison < 0) {
                low = middle + 1;
            } else if (comparison > 0) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }

    private int lowerBoundSource(int sourceId) {
        int low = 0;
        int high = edgeCount();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sourceIdAt(middle) < sourceId) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private int upperBoundSource(int sourceId) {
        int low = 0;
        int high = edgeCount();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sourceIdAt(middle) <= sourceId) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static <N, R> MIndexFrozenGraph<N, R> fromNormalizedIds(
            MIndexSpace<N> nodeSpace,
            MIndexSpace<R> relationSpace,
            MIndexCompositeIndex index,
            int[] lane) {
        int canonicalId = index.intern(
                MIndexCompositeIndex.KIND_GRAPH_EDGE_SET,
                nodeSpace,
                relationSpace,
                lane);
        return new MIndexFrozenGraph<>(
                nodeSpace,
                relationSpace,
                index,
                canonicalId);
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, nodeSpace, relationSpace)
                || index.length(canonicalId) % 3 != 0) {
            throw new IllegalArgumentException(
                    "canonical ID is not a graph edge set in these MIndex spaces");
        }
    }

    private static void sortTriples(int[] lane) {
        int edges = lane.length / 3;
        if (edges > 1) {
            quickSort(lane, 0, edges - 1);
        }
    }

    private static void quickSort(int[] lane, int low, int high) {
        while (low < high) {
            int pivot = (low + high) >>> 1;
            int offset = pivot * 3;
            int p1 = lane[offset];
            int p2 = lane[offset + 1];
            int p3 = lane[offset + 2];
            int left = low;
            int right = high;
            while (left <= right) {
                while (compareEntry(lane, left, p1, p2, p3) < 0) {
                    left++;
                }
                while (compareEntry(lane, right, p1, p2, p3) > 0) {
                    right--;
                }
                if (left <= right) {
                    swapTriple(lane, left, right);
                    left++;
                    right--;
                }
            }
            if (right - low < high - left) {
                if (low < right) {
                    quickSort(lane, low, right);
                }
                low = left;
            } else {
                if (left < high) {
                    quickSort(lane, left, high);
                }
                high = right;
            }
        }
    }

    private static int compareEntry(
            int[] lane,
            int edge,
            int sourceId,
            int relationId,
            int targetId) {
        int offset = edge * 3;
        return compare(
                lane[offset],
                lane[offset + 1],
                lane[offset + 2],
                sourceId,
                relationId,
                targetId);
    }

    private static int compare(
            int leftSource,
            int leftRelation,
            int leftTarget,
            int rightSource,
            int rightRelation,
            int rightTarget) {
        int comparison = Integer.compare(leftSource, rightSource);
        if (comparison == 0) {
            comparison = Integer.compare(leftRelation, rightRelation);
        }
        if (comparison == 0) {
            comparison = Integer.compare(leftTarget, rightTarget);
        }
        return comparison;
    }

    private static void swapTriple(int[] lane, int left, int right) {
        if (left == right) {
            return;
        }
        int leftOffset = left * 3;
        int rightOffset = right * 3;
        for (int laneOffset = 0; laneOffset < 3; laneOffset++) {
            int value = lane[leftOffset + laneOffset];
            lane[leftOffset + laneOffset] = lane[rightOffset + laneOffset];
            lane[rightOffset + laneOffset] = value;
        }
    }
}
