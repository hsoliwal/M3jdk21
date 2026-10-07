// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Immutable nonnegative long weights attached to positions of one prepared lane.
 * Contextual weights are separate from canonical collection identity. Two prefix
 * lanes provide encounter-order and ID-grouped sums and selection without entries.
 */
public final class MIndexWeights {
    private final MIndexIdIndex index;
    private final long[] prefix;
    private final long[] groupedPrefix;

    private MIndexWeights(MIndexIdIndex index, long[] weights) {
        this.index = Objects.requireNonNull(index, "index");
        Objects.requireNonNull(weights, "weights");
        if (weights.length != index.size()) {
            throw new IllegalArgumentException("one weight per logical position required");
        }
        int length = Math.addExact(weights.length, 1);
        prefix = new long[length];
        for (int position = 0; position < weights.length; position++) {
            long weight = weights[position];
            if (weight < 0) { throw new IllegalArgumentException("negative weight"); }
            prefix[position + 1] = Math.addExact(prefix[position], weight);
        }
        groupedPrefix = new long[length];
        for (int ordinal = 0; ordinal < weights.length; ordinal++) {
            // Read our frozen prefix, not the caller's mutable array a second time.
            int position = index.sortedPositionAt(ordinal);
            groupedPrefix[ordinal + 1] = groupedPrefix[ordinal] + weightAt(position);
        }
    }

    public static MIndexWeights of(MIndexIdIndex index, long[] weightsByPosition) {
        return new MIndexWeights(index, weightsByPosition);
    }

    public MIndexIdIndex index() { return index; }
    public int size() { return index.size(); }
    public long total() { return prefix[size()]; }

    public long weightAt(int position) {
        Objects.checkIndex(position, size());
        return prefix[position + 1] - prefix[position];
    }

    public long sum(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size());
        return prefix[toExclusive] - prefix[fromInclusive];
    }

    public long weightOfId(int id) {
        int rank = index.findRank(id);
        return rank < 0 ? 0 : groupedPrefix[index.end(rank)] - groupedPrefix[index.start(rank)];
    }

    /** Selects the position containing a zero-based cumulative weight offset. */
    public int select(long weightOffset) {
        requireOffset(weightOffset, total());
        return select(prefix, 0, size(), weightOffset);
    }

    /** Selects within one ID's occurrences, retaining original position order. */
    public int selectId(int id, long weightOffset) {
        int rank = index.findRank(id);
        long total = rank < 0 ? 0
                : groupedPrefix[index.end(rank)] - groupedPrefix[index.start(rank)];
        requireOffset(weightOffset, total);
        int start = index.start(rank);
        int ordinal = select(groupedPrefix, start, index.end(rank),
                groupedPrefix[start] + weightOffset);
        return index.sortedPositionAt(ordinal);
    }

    public MIndexWeightTree mutableCopy() { return new MIndexWeightTree(this); }

    public long primitiveBytes() { return Long.BYTES * ((long) prefix.length + groupedPrefix.length); }

    static void requireOffset(long offset, long total) {
        if (offset < 0 || offset >= total) {
            throw new IndexOutOfBoundsException("weight offset outside [0,total)");
        }
    }

    private static int select(long[] sums, int low, int high, long target) {
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sums[middle + 1] <= target) { low = middle + 1; }
            else { high = middle; }
        }
        return low;
    }
}
