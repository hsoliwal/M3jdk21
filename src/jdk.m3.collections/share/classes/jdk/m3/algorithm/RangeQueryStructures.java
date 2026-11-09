// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Primitive range-query structures with explicit half-open range semantics. */
//FLLDCIM: RELOCATE - donor blob 346309a81f3b at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class RangeQueryStructures {
    private RangeQueryStructures() {}

    /** Iterative segment tree supporting point-set and range sum. */
    public static final class SegmentTreeSum {
        private final int length;
        private final int base;
        private final long[] tree;

        public SegmentTreeSum(long[] values) {
            Objects.requireNonNull(values, "values");
            length = values.length;
            int power = 1;
            while (power < Math.max(1, length)) power <<= 1;
            base = power;
            tree = new long[base << 1];
            System.arraycopy(values, 0, tree, base, values.length);
            for (int node = base - 1; node > 0; node--) {
                tree[node] = Math.addExact(tree[node << 1], tree[(node << 1) | 1]);
            }
        }

        public int size() { return length; }

        public long get(int index) {
            checkIndex(index, length);
            return tree[base + index];
        }

        public void set(int index, long value) {
            checkIndex(index, length);
            int node = base + index;
            tree[node] = value;
            for (node >>= 1; node > 0; node >>= 1) {
                tree[node] = Math.addExact(tree[node << 1], tree[(node << 1) | 1]);
            }
        }

        /** Sum over [fromInclusive,toExclusive). */
        public long sum(int fromInclusive, int toExclusive) {
            checkRange(fromInclusive, toExclusive, length);
            int left = base + fromInclusive;
            int right = base + toExclusive;
            long leftSum = 0L;
            long rightSum = 0L;
            while (left < right) {
                if ((left & 1) != 0) leftSum = Math.addExact(leftSum, tree[left++]);
                if ((right & 1) != 0) rightSum = Math.addExact(tree[--right], rightSum);
                left >>= 1;
                right >>= 1;
            }
            return Math.addExact(leftSum, rightSum);
        }
    }

    /** Immutable O(1) range-min sparse table after O(n log n) construction. */
    public static final class SparseTableMin {
        private final int length;
        private final int levels;
        private final int[] log2;
        private final int[] table;

        public SparseTableMin(int[] values) {
            Objects.requireNonNull(values, "values");
            if (values.length == 0) throw new IllegalArgumentException("values must not be empty");
            length = values.length;
            log2 = new int[length + 1];
            for (int i = 2; i <= length; i++) log2[i] = log2[i >>> 1] + 1;
            levels = log2[length] + 1;
            table = new int[Math.multiplyExact(levels, length)];
            System.arraycopy(values, 0, table, 0, length);
            for (int level = 1; level < levels; level++) {
                int width = 1 << level;
                int half = width >>> 1;
                int previous = (level - 1) * length;
                int current = level * length;
                int limit = length - width + 1;
                for (int start = 0; start < limit; start++) {
                    table[current + start] = Math.min(
                            table[previous + start],
                            table[previous + start + half]);
                }
            }
        }

        public int size() { return length; }

        /** Minimum over non-empty [fromInclusive,toExclusive). */
        public int min(int fromInclusive, int toExclusive) {
            checkRange(fromInclusive, toExclusive, length);
            int width = toExclusive - fromInclusive;
            if (width == 0) throw new IllegalArgumentException("range must not be empty");
            int level = log2[width];
            int span = 1 << level;
            int offset = level * length;
            return Math.min(
                    table[offset + fromInclusive],
                    table[offset + toExclusive - span]);
        }
    }

    /** Mutable difference-array builder for many range-add operations followed by materialization. */
    public static final class RangeAdder {
        private final long[] difference;

        public RangeAdder(int size) {
            if (size < 0) throw new IllegalArgumentException("size must be non-negative");
            difference = new long[size + 1];
        }

        public int size() { return difference.length - 1; }

        public void add(int fromInclusive, int toExclusive, long delta) {
            checkRange(fromInclusive, toExclusive, size());
            difference[fromInclusive] = Math.addExact(difference[fromInclusive], delta);
            difference[toExclusive] = Math.subtractExact(difference[toExclusive], delta);
        }

        public long[] materialize() {
            long[] output = new long[size()];
            long current = 0L;
            for (int index = 0; index < output.length; index++) {
                current = Math.addExact(current, difference[index]);
                output[index] = current;
            }
            return output;
        }
    }

    private static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index=" + index);
    }

    private static void checkRange(int fromInclusive, int toExclusive, int size) {
        if (fromInclusive < 0 || toExclusive < fromInclusive || toExclusive > size) {
            throw new IndexOutOfBoundsException(
                    "range=[" + fromInclusive + "," + toExclusive + ") size=" + size);
        }
    }
}
