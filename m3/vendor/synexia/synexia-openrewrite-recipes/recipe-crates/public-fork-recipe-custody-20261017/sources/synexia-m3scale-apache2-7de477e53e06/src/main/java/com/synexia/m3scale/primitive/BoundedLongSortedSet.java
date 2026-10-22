/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

import java.util.Arrays;

/**
 * Fixed-capacity sorted long set with admission filtering before mutation.
 *
 * <p>Useful for top-K/bottom-K and marker-style ranking: when full, a value that
 * cannot beat the current boundary is rejected before any array copy.</p>
 */
public final class BoundedLongSortedSet {
    public static final byte KEEP_LOWEST = 0;
    public static final byte KEEP_HIGHEST = 1;

    private final long[] values;
    private final byte policy;
    private final long minAccepted;
    private final long maxAccepted;
    private int size;
    private long filteredByRange;
    private long filteredByBoundary;
    private long duplicates;

    public BoundedLongSortedSet(int limit, byte policy) {
        this(limit, policy, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public BoundedLongSortedSet(int limit, byte policy, long minAccepted, long maxAccepted) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (policy != KEEP_LOWEST && policy != KEEP_HIGHEST) {
            throw new IllegalArgumentException("unknown policy");
        }
        if (minAccepted > maxAccepted) throw new IllegalArgumentException("invalid range");
        this.values = new long[limit];
        this.policy = policy;
        this.minAccepted = minAccepted;
        this.maxAccepted = maxAccepted;
    }

    public int size() { return size; }
    public int limit() { return values.length; }
    public boolean isFull() { return size == values.length; }

    public long min() {
        if (size == 0) throw new IllegalStateException("empty");
        return values[0];
    }

    public long max() {
        if (size == 0) throw new IllegalStateException("empty");
        return values[size - 1];
    }

    public long get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index=" + index);
        return values[index];
    }

    public boolean offer(long value) {
        if (value < minAccepted || value > maxAccepted) {
            filteredByRange++;
            return false;
        }

        int found = Arrays.binarySearch(values, 0, size, value);
        if (found >= 0) {
            duplicates++;
            return false;
        }

        if (size == values.length) {
            if (policy == KEEP_HIGHEST) {
                if (value <= values[0]) {
                    filteredByBoundary++;
                    return false;
                }
                System.arraycopy(values, 1, values, 0, size - 1);
                size--;
            } else {
                if (value >= values[size - 1]) {
                    filteredByBoundary++;
                    return false;
                }
                size--;
            }
        }

        int insertion = -Arrays.binarySearch(values, 0, size, value) - 1;
        int moved = size - insertion;
        if (moved > 0) System.arraycopy(values, insertion, values, insertion + 1, moved);
        values[insertion] = value;
        size++;
        return true;
    }

    public int offerAll(long[] candidates, int from, int to) {
        if (from < 0 || to < from || to > candidates.length) throw new IndexOutOfBoundsException();
        int admitted = 0;
        for (int i = from; i < to; i++) if (offer(candidates[i])) admitted++;
        return admitted;
    }

    public long filteredByRangeCount() { return filteredByRange; }
    public long filteredByBoundaryCount() { return filteredByBoundary; }
    public long duplicateCount() { return duplicates; }

    public long[] toArray() {
        return Arrays.copyOf(values, size);
    }

    public void clear() {
        size = 0;
        filteredByRange = 0L;
        filteredByBoundary = 0L;
        duplicates = 0L;
    }

    public long estimatedPayloadBytes() {
        return (long) values.length * Long.BYTES;
    }
}
