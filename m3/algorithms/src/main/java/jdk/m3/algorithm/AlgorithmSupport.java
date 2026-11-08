// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Objects;
import java.util.Arrays;

//FLLDCIM: RELOCATE - donor blob ee97c7b89da3 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
final class AlgorithmSupport {
    private AlgorithmSupport() {
    }

    static ProgressMonitor begin(
            ProgressMonitor monitor,
            String task,
            long totalWork) {
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        int total = totalWork < 0L
                ? ProgressMonitor.UNKNOWN
                : (int) Math.min(Integer.MAX_VALUE, totalWork);
        monitor.beginTask(task, total);
        return monitor;
    }

    static void tick(ProgressMonitor monitor) {
        monitor.checkCanceled();
        monitor.worked(1);
    }

    static int[] emptyPair() {
        return new int[] {-1, -1};
    }

    //FLLDCIM: FOLD - member PrimitiveAlgorithmSupport.LongIntMinHeap from donor blob ab947fa4cf6f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveAlgorithmSupport.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-support-fold/FOLD_MAP.tsv
    static final class LongIntMinHeap {
        private long[] priorities;
        private int[] values;
        private int size;

        LongIntMinHeap(int capacity) {
            priorities = new long[Math.max(4, capacity)];
            values = new int[priorities.length];
        }

        boolean isEmpty() { return size == 0; }

        void add(long priority, int value) {
            ensure();
            int index = size++;
            priorities[index] = priority;
            values[index] = value;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (priorities[parent] <= priority) break;
                priorities[index] = priorities[parent];
                values[index] = values[parent];
                index = parent;
            }
            priorities[index] = priority;
            values[index] = value;
        }

        long peekPriority() {
            if (size == 0) throw new IllegalStateException("heap empty");
            return priorities[0];
        }

        int peekValue() {
            if (size == 0) throw new IllegalStateException("heap empty");
            return values[0];
        }

        void removeMin() {
            if (size == 0) throw new IllegalStateException("heap empty");
            int last = --size;
            if (last == 0) return;
            long priority = priorities[last];
            int value = values[last];
            int index = 0;
            while (true) {
                int left = index * 2 + 1;
                if (left >= size) break;
                int right = left + 1;
                int child = right < size && priorities[right] < priorities[left] ? right : left;
                if (priorities[child] >= priority) break;
                priorities[index] = priorities[child];
                values[index] = values[child];
                index = child;
            }
            priorities[index] = priority;
            values[index] = value;
        }

        private void ensure() {
            if (size < priorities.length) return;
            priorities = Arrays.copyOf(priorities, priorities.length << 1);
            values = Arrays.copyOf(values, values.length << 1);
        }
    }

    //FLLDCIM: FOLD - member PrimitiveAlgorithmSupport.DisjointSet from donor blob ab947fa4cf6f (m3/algorithms/src/main/java/jdk/m3/algorithm/PrimitiveAlgorithmSupport.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/primitive-support-fold/FOLD_MAP.tsv
    static final class DisjointSet {
        private final int[] parent;
        private final byte[] rank;
        private int components;

        DisjointSet(int n) {
            if (n < 0) throw new IllegalArgumentException("n must be non-negative");
            parent = new int[n];
            rank = new byte[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            components = n;
        }

        int find(int value) {
            int root = value;
            while (parent[root] != root) root = parent[root];
            while (parent[value] != value) {
                int next = parent[value];
                parent[value] = root;
                value = next;
            }
            return root;
        }

        boolean union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra == rb) return false;
            if (rank[ra] < rank[rb]) {
                parent[ra] = rb;
            } else if (rank[ra] > rank[rb]) {
                parent[rb] = ra;
            } else {
                parent[rb] = ra;
                rank[ra]++;
            }
            components--;
            return true;
        }

        int components() { return components; }
    }
}
