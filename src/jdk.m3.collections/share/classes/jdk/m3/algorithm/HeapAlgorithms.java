// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

//FLLDCIM: RELOCATE - donor blob 11689f31eb71 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class HeapAlgorithms {
    private HeapAlgorithms() {
    }

    public static int kthLargest(
            int[] values,
            int k,
            ProgressMonitor monitor) {
        if (k <= 0 || k > values.length) {
            throw new IllegalArgumentException("k outside 1..n");
        }
        AlgorithmSupport.begin(monitor, "kth-largest-heap", values.length);
        try {
            PriorityQueue<Integer> heap = new PriorityQueue<>(k);
            for (int value : values) {
                AlgorithmSupport.tick(monitor);
                heap.add(value);
                if (heap.size() > k) {
                    heap.remove();
                }
            }
            return heap.element();
        } finally {
            monitor.done();
        }
    }

    public static <T> List<T> mergeSorted(
            List<? extends List<T>> lists,
            Comparator<? super T> comparator,
            ProgressMonitor monitor) {
        Objects.requireNonNull(lists, "lists");
        Objects.requireNonNull(comparator, "comparator");
        AlgorithmSupport.begin(monitor, "merge-k-sorted", ProgressMonitor.UNKNOWN);
        try {
            Object[][] runs = new Object[lists.size()][];
            RunHeap<T> heap = new RunHeap<>(runs, comparator);
            for (int list = 0; list < runs.length; list++) {
                List<T> values = lists.get(list);
                if (!values.isEmpty()) {
                    runs[list] = values.toArray();
                    heap.add((long) list << 32);
                }
            }
            List<T> result = new ArrayList<>();
            while (!heap.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                long entry = heap.remove();
                result.add(heap.value(entry));
                if ((int) entry + 1 < runs[(int) (entry >>> 32)].length) heap.add(entry + 1);
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    //FLLDCIM: LEETCODIFY — pattern OFFSET_ARRAY (runs) + MASK (run << 32 | position) + HEAP
    /**
     * A binary heap of packed entries (run in the high 32 bits, position in the low 32) ordered by
     * the comparator on their values, with java.util.PriorityQueue's siftUp and siftDown: the same
     * comparator calls and the same order among equal values.
     */
    private static final class RunHeap<T> {
        private final Object[][] runs;
        private final Comparator<? super T> comparator;
        private final long[] heap;
        private int size;

        RunHeap(Object[][] runs, Comparator<? super T> comparator) {
            this.runs = runs;
            this.comparator = comparator;
            this.heap = new long[Math.max(1, runs.length)];
        }

        boolean isEmpty() {
            return size == 0;
        }

        @SuppressWarnings("unchecked")
        T value(long entry) {
            return (T) runs[(int) (entry >>> 32)][(int) entry];
        }

        void add(long entry) {
            int k = size++;
            while (k > 0) {
                int parent = (k - 1) >>> 1;
                if (comparator.compare(value(entry), value(heap[parent])) >= 0) break;
                heap[k] = heap[parent];
                k = parent;
            }
            heap[k] = entry;
        }

        long remove() {
            long result = heap[0];
            int n = --size;
            if (n > 0) siftDown(heap[n], n);
            return result;
        }

        private void siftDown(long entry, int n) {
            int k = 0;
            for (int half = n >>> 1; k < half; ) {
                int child = (k << 1) + 1;
                if (child + 1 < n
                        && comparator.compare(value(heap[child]), value(heap[child + 1])) > 0) {
                    child++;
                }
                if (comparator.compare(value(entry), value(heap[child])) <= 0) break;
                heap[k] = heap[child];
                k = child;
            }
            heap[k] = entry;
        }
    }
}
