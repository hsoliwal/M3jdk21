// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.ELEMENT;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Standalone allocation gate; execute with -XX:-DoEscapeAnalysis after JUnit passes. */
public final class MIndexPrecomputeAllocationProbe {
    private static volatile long sink;
    private final MIndexCollectionPrecompute cache;
    private final MIndexFrozenList<Integer> source;
    private final MIndexIdIndex index;
    private final MIndexWeights weights;
    private final MIndexWeightTree tree;
    private final MIndexRelations relations;
    private final MIndexWeights edgeWeights;

    private MIndexPrecomputeAllocationProbe() {
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        MIndexSpace<Integer> space = new MIndexPrecomputeTest.NumericSpace();
        cache = new MIndexCollectionPrecompute(store, 4, 1_000_000);
        int[] ids = new int[512];
        long[] amounts = new long[512];
        for (int i = 0; i < ids.length; i++) { ids[i] = i & 63; amounts[i] = i % 5 + 1; }
        source = MIndexFrozenList.ofIds(space, store, ids);
        index = source.precompute(cache).column(ELEMENT);
        weights = index.weights(amounts);
        tree = weights.mutableCopy();
        relations = MIndexRelations.successors(index);
        long[] edges = new long[relations.size()];
        java.util.Arrays.fill(edges, 2);
        edgeWeights = relations.outgoingWeights(edges);
    }

    private void run(int path, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            int id = i & 63;
            switch (path) {
                case 0 -> checksum += cache.prepare(source).size();
                case 1 -> checksum += index.count(id) + index.rank(id) + index.positionOf(id, i & 7);
                case 2 -> checksum += index.countInRange(id, 32, 480) + index.sortedIdAt(i & 511);
                case 3 -> checksum += weights.sum(3, 490) + weights.select(i % weights.total());
                case 4 -> checksum += weights.weightOfId(id) + weights.selectId(id, 0);
                case 5 -> {
                    tree.set(i & 511, i % 9 + 1);
                    checksum += tree.sum(3, 490) + tree.select(i % tree.total());
                }
                case 6 -> checksum += relations.outDegree(id) + relations.inDegree(id)
                        + relations.outgoingTargetAt(id, 0) + relations.incomingSourceAt(id, 0);
                case 7 -> checksum += relations.edgeCount(id, (id + 1) & 63)
                        + relations.selectOutgoingEdge(edgeWeights, id, 0);
                default -> throw new IllegalArgumentException("unknown path");
            }
        }
        sink = checksum;
    }

    public static void main(String[] args) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unavailable");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        MIndexPrecomputeAllocationProbe probe = new MIndexPrecomputeAllocationProbe();
        String[] names = {"cache-hit", "membership-rank-select", "range-count-sort", "weight-range-select",
            "weight-per-id", "fenwick-update-select", "forward-reverse", "weighted-edge-select"};
        System.out.println("path\ttrial\toperations\tallocated_bytes\tchecksum");
        boolean allocated = false;
        for (int path = 0; path < names.length; path++) {
            for (int warmup = 0; warmup < 3; warmup++) { probe.run(path, 1_000_000); }
            // Initialize the counter path before measuring it.
            bean.getThreadAllocatedBytes(thread);
            for (int trial = 0; trial < 3; trial++) {
                long before = bean.getThreadAllocatedBytes(thread);
                probe.run(path, 1_000_000);
                long bytes = bean.getThreadAllocatedBytes(thread) - before;
                System.out.println(names[path] + "\t" + trial + "\t1000000\t" + bytes + "\t" + sink);
                allocated |= bytes != 0;
            }
        }
        if (allocated) { throw new AssertionError("prepared path allocated; inspect every trial above"); }
    }
}
