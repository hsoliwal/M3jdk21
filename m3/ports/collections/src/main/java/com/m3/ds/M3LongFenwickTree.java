// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-algo/src/main/java/com/synexia/algorithms/collections/PackedLongFenwickTree.java
// Source-Git-blob: b6c820c05dca67fc53a92a4be84f0b0a2c28b411
package com.m3.ds;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive Fenwick tree with exact arithmetic and failure-atomic point mutation.
 *
 * <p>Retained state is two long lanes plus fixed O(log n) scratch. Construction from
 * values is O(n). Prefix/range/add/set are O(log n). No stream, wrapper or per-operation
 * array allocation is required after construction.
 */
public final class M3LongFenwickTree {
    private final long[] points;
    private final long[] tree;
    private final int[] stagedIndexes;
    private final long[] stagedValues;
    private int negativePoints;

    public M3LongFenwickTree(int size) {
        if (size < 0 || size == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("size");
        }
        points = new long[size];
        tree = new long[size + 1];
        int depth = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(1, size)));
        stagedIndexes = new int[depth + 1];
        stagedValues = new long[depth + 1];
    }

    public M3LongFenwickTree(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("size");
        }
        points = values.clone();
        tree = new long[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 0) {
                negativePoints++;
            }
            tree[i + 1] = values[i];
        }
        for (int i = 1; i <= values.length; i++) {
            int parent = i + (i & -i);
            if (parent <= values.length) {
                tree[parent] = Math.addExact(tree[parent], tree[i]);
            }
        }
        int depth = Math.max(1,
                Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(1, values.length)));
        stagedIndexes = new int[depth + 1];
        stagedValues = new long[depth + 1];
    }

    public int size() {
        return points.length;
    }

    public long point(int index) {
        return points[Objects.checkIndex(index, points.length)];
    }

    public long total() {
        return prefixSum(points.length);
    }

    public long payloadBytes() {
        return 8L * (points.length + (long) tree.length + stagedValues.length)
                + 4L * stagedIndexes.length;
    }

    /** Sum of the half-open prefix [0,toExclusive). */
    public long prefixSum(int toExclusive) {
        Objects.checkFromToIndex(0, toExclusive, points.length);
        long sum = 0;
        for (int i = toExclusive; i > 0; i -= i & -i) {
            sum = Math.addExact(sum, tree[i]);
        }
        return sum;
    }

    /** Sum of the half-open range [fromInclusive,toExclusive). */
    public long rangeSum(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, points.length);
        return Math.subtractExact(prefixSum(toExclusive), prefixSum(fromInclusive));
    }

    /**
     * Failure-atomic exact addition. Overflow in the point or any affected Fenwick cell
     * is detected before publishing any value.
     */
    public void add(int index, long delta) {
        Objects.checkIndex(index, points.length);
        long nextPoint = Math.addExact(points[index], delta);
        int count = stagePath(index, delta);
        publish(index, nextPoint, count);
    }

    /** Failure-atomic exact point replacement. */
    public void set(int index, long value) {
        Objects.checkIndex(index, points.length);
        long delta = Math.subtractExact(value, points[index]);
        int count = stagePath(index, delta);
        publish(index, value, count);
    }

    /**
     * Smallest index whose inclusive prefix sum is at least target.
     * Returns size when target exceeds the total. Requires every point to be nonnegative.
     */
    public int lowerBound(long target) {
        if (negativePoints != 0) {
            throw new IllegalStateException("lowerBound requires nonnegative point values");
        }
        if (points.length == 0 || target <= 0) {
            return 0;
        }
        long total = total();
        if (target > total) {
            return points.length;
        }

        int index = 0;
        long accumulated = 0;
        int bit = Integer.highestOneBit(points.length);
        for (; bit != 0; bit >>>= 1) {
            int next = index + bit;
            if (next <= points.length) {
                long candidate = Math.addExact(accumulated, tree[next]);
                if (candidate < target) {
                    index = next;
                    accumulated = candidate;
                }
            }
        }
        return index;
    }

    public void clear() {
        Arrays.fill(points, 0L);
        Arrays.fill(tree, 0L);
        negativePoints = 0;
    }

    private int stagePath(int index, long delta) {
        int count = 0;
        for (int i = index + 1; i <= points.length; i += i & -i) {
            stagedIndexes[count] = i;
            stagedValues[count] = Math.addExact(tree[i], delta);
            count++;
        }
        return count;
    }

    private void publish(int index, long value, int count) {
        long previous = points[index];
        if (previous < 0 && value >= 0) {
            negativePoints--;
        } else if (previous >= 0 && value < 0) {
            negativePoints++;
        }
        points[index] = value;
        for (int i = 0; i < count; i++) {
            tree[stagedIndexes[i]] = stagedValues[i];
        }
    }
}
