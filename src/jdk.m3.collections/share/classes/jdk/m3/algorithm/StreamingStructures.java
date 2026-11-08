// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;

/** Stateful primitive structures corresponding to common streaming/design problem shapes. */
//FLLDCIM: RELOCATE - donor blob 8463f9274475 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class StreamingStructures {
    private StreamingStructures() {}

    public static final class IntMinStack {
        //FLLDCIM: RELOCATE - explicit constructor inserted by the INSERT_AFTER row of m3/algorithms/recipe/algorithm-extras-20261106/RELOCATION_MAP.tsv: the donor relies on the implicit public constructor, which -Xlint:all (missing-explicit-ctor) refuses for a public class of the exported package jdk.m3.algorithm; same contract
        public IntMinStack() {
        }

        private int[] values = new int[16];
        private int[] minima = new int[16];
        private int size;

        public int size() { return size; }
        public boolean isEmpty() { return size == 0; }

        public void push(int value) {
            ensure();
            values[size] = value;
            minima[size] = size == 0 ? value : Math.min(value, minima[size - 1]);
            size++;
        }

        public int pop() {
            if (size == 0) throw new IllegalStateException("stack empty");
            return values[--size];
        }

        public int peek() {
            if (size == 0) throw new IllegalStateException("stack empty");
            return values[size - 1];
        }

        public int min() {
            if (size == 0) throw new IllegalStateException("stack empty");
            return minima[size - 1];
        }

        private void ensure() {
            if (size < values.length) return;
            values = Arrays.copyOf(values, values.length << 1);
            minima = Arrays.copyOf(minima, minima.length << 1);
        }
    }

    public static final class MedianTracker {
        //FLLDCIM: RELOCATE - explicit constructor inserted by the INSERT_AFTER row of m3/algorithms/recipe/algorithm-extras-20261106/RELOCATION_MAP.tsv: the donor relies on the implicit public constructor, which -Xlint:all (missing-explicit-ctor) refuses for a public class of the exported package jdk.m3.algorithm; same contract
        public MedianTracker() {
        }

        private final IntHeap lower = new IntHeap(false);
        private final IntHeap upper = new IntHeap(true);

        public int size() { return lower.size() + upper.size(); }
        public boolean isEmpty() { return size() == 0; }

        public void add(int value) {
            if (lower.isEmpty() || value <= lower.peek()) lower.add(value);
            else upper.add(value);

            if (lower.size() > upper.size() + 1) upper.add(lower.removeRoot());
            else if (upper.size() > lower.size()) lower.add(upper.removeRoot());
        }

        public double median() {
            if (isEmpty()) throw new IllegalStateException("tracker empty");
            if (lower.size() == upper.size()) {
                return ((long) lower.peek() + upper.peek()) / 2.0;
            }
            return lower.peek();
        }
    }

    public static final class MovingAverage {
        private final long[] ring;
        private int cursor;
        private int size;
        private long sum;

        public MovingAverage(int window) {
            if (window <= 0) throw new IllegalArgumentException("window must be positive");
            ring = new long[window];
        }

        public int window() { return ring.length; }
        public int size() { return size; }

        public double add(long value) {
            if (size == ring.length) {
                sum = Math.subtractExact(sum, ring[cursor]);
            } else {
                size++;
            }
            ring[cursor] = value;
            sum = Math.addExact(sum, value);
            cursor = (cursor + 1) % ring.length;
            return sum / (double) size;
        }
    }

    public static final class KthLargestStream {
        private final int k;
        private final IntHeap heap = new IntHeap(true);

        public KthLargestStream(int k) {
            if (k <= 0) throw new IllegalArgumentException("k must be positive");
            this.k = k;
        }

        public KthLargestStream(int k, int[] initial) {
            this(k);
            if (initial == null) throw new NullPointerException("initial");
            for (int value : initial) add(value);
        }

        public int observed() { return heap.observed; }

        public void add(int value) {
            heap.observed++;
            if (heap.size() < k) {
                heap.add(value);
            } else if (value > heap.peek()) {
                heap.replaceRoot(value);
            }
        }

        public boolean ready() { return heap.size() == k; }

        public int kthLargest() {
            if (!ready()) throw new IllegalStateException("fewer than k values observed");
            return heap.peek();
        }
    }

    private static final class IntHeap {
        private int[] values = new int[16];
        private int size;
        private int observed;
        private final boolean minHeap;

        IntHeap(boolean minHeap) {
            this.minHeap = minHeap;
        }

        int size() { return size; }
        boolean isEmpty() { return size == 0; }

        int peek() {
            if (size == 0) throw new IllegalStateException("heap empty");
            return values[0];
        }

        void add(int value) {
            ensure();
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (!before(value, values[parent])) break;
                values[index] = values[parent];
                index = parent;
            }
            values[index] = value;
        }

        int removeRoot() {
            int root = peek();
            int last = values[--size];
            if (size > 0) siftDown(0, last);
            return root;
        }

        void replaceRoot(int value) {
            if (size == 0) throw new IllegalStateException("heap empty");
            siftDown(0, value);
        }

        private void siftDown(int index, int value) {
            while (true) {
                int left = index * 2 + 1;
                if (left >= size) break;
                int right = left + 1;
                int child = right < size && before(values[right], values[left]) ? right : left;
                if (!before(values[child], value)) break;
                values[index] = values[child];
                index = child;
            }
            values[index] = value;
        }

        private boolean before(int left, int right) {
            return minHeap ? left < right : left > right;
        }

        private void ensure() {
            if (size < values.length) return;
            values = Arrays.copyOf(values, values.length << 1);
        }
    }
}
