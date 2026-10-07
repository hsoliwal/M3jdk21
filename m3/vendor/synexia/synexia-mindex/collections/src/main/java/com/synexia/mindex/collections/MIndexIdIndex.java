// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntToLongFunction;

/**
 * Immutable inverted occurrence index over an existing canonical primitive lane.
 * IDs sort numerically in their owning space, not by projected Java values.
 * Construction costs O(n log n); no payload, entry or position objects are stored.
 */
public final class MIndexIdIndex {
    private final MIndexCompositeIndex owner;
    private final MIndexSpace<?> space;
    private final int canonicalId;
    private final int offset;
    private final int stride;
    private final int size;
    private final int[] ids;
    private final int[] offsets;
    private final int[] positions;

    MIndexIdIndex(MIndexCompositeIndex owner, int canonicalId, MIndexSpace<?> space,
            int offset, int stride, int size) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.space = Objects.requireNonNull(space, "space");
        this.canonicalId = canonicalId;
        if (offset < 0 || stride <= 0 || size < 0
                || (size > 0 && offset + (long) stride * (size - 1) >= owner.length(canonicalId))) {
            throw new IllegalArgumentException("invalid ID lane");
        }
        this.offset = offset;
        this.stride = stride;
        this.size = size;
        long[] sorted = new long[size];
        for (int position = 0; position < size; position++) {
            int id = idAt(position);
            space.requireId(id);
            sorted[position] = ((long) id << 32) | position;
        }
        Arrays.sort(sorted);
        int distinct = 0;
        for (int i = 0; i < size; i++) {
            if (i == 0 || (sorted[i] >>> 32) != (sorted[i - 1] >>> 32)) {
                distinct++;
            }
        }
        ids = new int[distinct];
        offsets = new int[distinct + 1];
        positions = new int[size];
        int rank = -1;
        for (int i = 0; i < size; i++) {
            int id = (int) (sorted[i] >>> 32);
            if (i == 0 || id != ids[rank]) {
                ids[++rank] = id;
                offsets[rank] = i;
            }
            positions[i] = (int) sorted[i];
        }
        offsets[distinct] = size;
    }

    public MIndexSpace<?> space() { return space; }
    public int size() { return size; }
    public int distinctSize() { return ids.length; }

    /** Original logical position, before grouping by ID. */
    public int idAt(int position) {
        return owner.valueAt(canonicalId, offset + Objects.checkIndex(position, size) * stride);
    }

    public int distinctIdAt(int rank) { return ids[Objects.checkIndex(rank, ids.length)]; }

    /** Number of distinct IDs strictly less than id; also its insertion rank. */
    public int rank(int id) {
        int result = Arrays.binarySearch(ids, id);
        return result < 0 ? -result - 1 : result;
    }

    public boolean containsId(int id) { return findRank(id) >= 0; }

    public int count(int id) {
        int rank = findRank(id);
        return rank < 0 ? 0 : offsets[rank + 1] - offsets[rank];
    }

    /** Zero-based occurrence, in original logical order. */
    public int positionOf(int id, int occurrence) {
        int rank = findRank(id);
        int count = rank < 0 ? 0 : offsets[rank + 1] - offsets[rank];
        Objects.checkIndex(occurrence, count);
        return positions[offsets[rank] + occurrence];
    }

    /** Original position of a sorted occurrence (ID first, position second). */
    public int sortedPositionAt(int ordinal) {
        return positions[Objects.checkIndex(ordinal, size)];
    }

    public int sortedIdAt(int ordinal) { return idAt(sortedPositionAt(ordinal)); }

    public int countInRange(int id, int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        int rank = findRank(id);
        if (rank < 0) { return 0; }
        return lowerPosition(rank, toExclusive) - lowerPosition(rank, fromInclusive);
    }

    /** Cardinality of the exact distinct-ID intersection; domains must match by identity. */
    public int intersectionSize(MIndexIdIndex other) {
        requireSameSpace(other);
        int left = 0;
        int right = 0;
        int count = 0;
        while (left < ids.length && right < other.ids.length) {
            if (ids[left] < other.ids[right]) { left++; }
            else if (ids[left] > other.ids[right]) { right++; }
            else { count++; left++; right++; }
        }
        return count;
    }

    public boolean isSubsetOf(MIndexIdIndex other) {
        return intersectionSize(other) == distinctSize();
    }

    /** Empty versus empty has similarity 1. Repeated occurrences do not change set similarity. */
    public double jaccard(MIndexIdIndex other) {
        int intersection = intersectionSize(other);
        long union = (long) distinctSize() + other.distinctSize() - intersection;
        return union == 0 ? 1.0 : (double) intersection / union;
    }

    /** Explicitly builds a caller-owned positional slice; does not admit another collection. */
    public MIndexIdIndex slice(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        // Empty slices never read their physical offset, including the end of a strided lane.
        int start = fromInclusive == toExclusive ? offset : offset + fromInclusive * stride;
        return new MIndexIdIndex(owner, canonicalId, space, start, stride,
                toExclusive - fromInclusive);
    }

    public MIndexWeights weights(long[] weightsByPosition) {
        return MIndexWeights.of(this, weightsByPosition);
    }

    /** Evaluates the callback once per occurrence during preparation, never during queries. */
    public MIndexWeights weightsById(IntToLongFunction weight) {
        Objects.requireNonNull(weight, "weight");
        long[] values = new long[size];
        for (int position = 0; position < size; position++) {
            values[position] = weight.applyAsLong(idAt(position));
        }
        return weights(values);
    }

    public long primitiveBytes() {
        return Integer.BYTES * ((long) ids.length + offsets.length + positions.length);
    }

    int findRank(int id) { return Arrays.binarySearch(ids, id); }
    int start(int rank) { return offsets[rank]; }
    int end(int rank) { return offsets[rank + 1]; }
    boolean sameSnapshot(MIndexIdIndex other) {
        return owner == other.owner && canonicalId == other.canonicalId;
    }

    private int lowerPosition(int rank, int position) {
        int low = offsets[rank];
        int high = offsets[rank + 1];
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (positions[middle] < position) { low = middle + 1; }
            else { high = middle; }
        }
        return low;
    }

    private void requireSameSpace(MIndexIdIndex other) {
        if (Objects.requireNonNull(other, "other").space != space) {
            throw new IllegalArgumentException("ID indexes must share one MIndexSpace");
        }
    }
}
