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
            record Cursor<T>(int list, int index, T value) {}
            PriorityQueue<Cursor<T>> heap = new PriorityQueue<>(
                    (left, right) -> comparator.compare(left.value(), right.value()));
            for (int list = 0; list < lists.size(); list++) {
                List<T> values = lists.get(list);
                if (!values.isEmpty()) {
                    heap.add(new Cursor<>(list, 0, values.getFirst()));
                }
            }
            List<T> result = new ArrayList<>();
            while (!heap.isEmpty()) {
                AlgorithmSupport.tick(monitor);
                Cursor<T> cursor = heap.remove();
                result.add(cursor.value());
                int next = cursor.index() + 1;
                List<T> source = lists.get(cursor.list());
                if (next < source.size()) {
                    heap.add(new Cursor<>(cursor.list(), next, source.get(next)));
                }
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }
}
