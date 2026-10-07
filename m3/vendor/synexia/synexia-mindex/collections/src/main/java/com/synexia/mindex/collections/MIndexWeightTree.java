// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Caller-owned mutable Fenwick weights over fixed collection positions.
 * Point updates, range sums and weighted selection cost O(log n). No entry objects
 * or policy callbacks occur on these paths. This object is not thread-safe.
 */
public final class MIndexWeightTree {
    private final MIndexIdIndex index;
    private final long[] weights;
    private final long[] tree;
    private long total;

    public MIndexWeightTree(MIndexWeights initial) {
        index = Objects.requireNonNull(initial, "initial").index();
        weights = new long[index.size()];
        tree = new long[Math.addExact(weights.length, 1)];
        total = initial.total();
        for (int position = 0; position < weights.length; position++) {
            weights[position] = initial.weightAt(position);
            tree[position + 1] = weights[position];
        }
        for (int slot = 1; slot < tree.length; slot++) {
            long parent = (long) slot + (slot & -slot);
            if (parent < tree.length) { tree[(int) parent] += tree[slot]; }
        }
    }

    public MIndexIdIndex index() { return index; }
    public int size() { return weights.length; }
    public long total() { return total; }
    public long weightAt(int position) { return weights[Objects.checkIndex(position, size())]; }

    /** Validates before any write; a rejected negative or overflowing update is atomic. */
    public void set(int position, long weight) {
        Objects.checkIndex(position, size());
        if (weight < 0) { throw new IllegalArgumentException("negative weight"); }
        long previous = weights[position];
        long nextTotal = Math.addExact(total - previous, weight);
        long delta = weight - previous;
        weights[position] = weight;
        for (long slot = (long) position + 1; slot < tree.length; slot += slot & -slot) {
            tree[(int) slot] += delta;
        }
        total = nextTotal;
    }

    public long sum(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size());
        return prefix(toExclusive) - prefix(fromInclusive);
    }

    public int select(long weightOffset) {
        MIndexWeights.requireOffset(weightOffset, total);
        int position = 0;
        long remaining = weightOffset;
        for (int bit = Integer.highestOneBit(size()); bit != 0; bit >>>= 1) {
            long next = (long) position + bit;
            if (next < tree.length && tree[(int) next] <= remaining) {
                position = (int) next;
                remaining -= tree[position];
            }
        }
        return position;
    }

    /** Explicitly allocates an immutable snapshot of the current weights. */
    public MIndexWeights snapshot() { return MIndexWeights.of(index, weights); }

    private long prefix(int end) {
        long sum = 0;
        for (int slot = end; slot > 0; slot -= slot & -slot) { sum += tree[slot]; }
        return sum;
    }
}
