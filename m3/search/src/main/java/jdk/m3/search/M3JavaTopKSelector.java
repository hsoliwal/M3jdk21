/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia synexia-fastsearch/com/synexia/fastsearch/knowledge/index/JavaTopKSelector.java (blob 895ae23ef198) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import jdk.m3.collections.M3ProgressMonitor;

/** Primitive worst-first heap; O(n log k + k log k) work and O(k) scratch space. */
final class M3JavaTopKSelector implements M3TopKSelector {
    static final M3JavaTopKSelector INSTANCE = new M3JavaTopKSelector();

    private M3JavaTopKSelector() {
    }

    @Override
    public int[] select(int[] documentIds, float[] scores, int limit) {
        return selectInternal(documentIds, scores, limit, null);
    }

    @Override
    public int[] select(int[] documentIds, float[] scores, int limit, M3ProgressMonitor monitor) {
        return M3TopKInputs.monitored(monitor,
                () -> selectInternal(documentIds, scores, limit, monitor));
    }

    private int[] selectInternal(
            int[] documentIds, float[] scores, int limit, M3ProgressMonitor monitor) {
        int size = M3TopKInputs.validate(documentIds, scores, limit, monitor);
        if (monitor != null) monitor.subTask("Select and order Java top-k");
        int[] heap = new int[size];
        if (size == 0) {
            return heap;
        }
        for (int i = 0; i < size; i++) {
            M3TopKInputs.checkpoint(monitor, i);
            heap[i] = i;
        }
        for (int parent = size / 2; parent > 0; parent--) {
            M3TopKInputs.checkpoint(monitor, parent);
            siftDown(heap, parent - 1, size, documentIds, scores);
        }
        for (int candidate = size; candidate < scores.length; candidate++) {
            M3TopKInputs.checkpoint(monitor, candidate);
            if (better(candidate, heap[0], documentIds, scores)) {
                heap[0] = candidate;
                siftDown(heap, 0, size, documentIds, scores);
            }
        }
        // Extracting the worst to the tail yields the required best-first order.
        for (int end = size - 1; end > 0; end--) {
            M3TopKInputs.checkpoint(monitor, end);
            int worst = heap[0];
            heap[0] = heap[end];
            heap[end] = worst;
            siftDown(heap, 0, end, documentIds, scores);
        }
        return heap;
    }

    private static void siftDown(
            int[] heap, int root, int size, int[] documentIds, float[] scores) {
        int value = heap[root];
        // root < size/2 also prevents overflow of 2*root+1.
        while (root < size / 2) {
            int child = root * 2 + 1;
            if (child + 1 < size
                    && better(heap[child], heap[child + 1], documentIds, scores)) {
                child++;
            }
            if (!better(value, heap[child], documentIds, scores)) {
                break;
            }
            heap[root] = heap[child];
            root = child;
        }
        heap[root] = value;
    }

    private static boolean better(int left, int right, int[] documentIds, float[] scores) {
        int scoreOrder = Float.compare(scores[left], scores[right]);
        if (scoreOrder != 0) {
            return scoreOrder > 0;
        }
        int idOrder = Integer.compare(documentIds[left], documentIds[right]);
        return idOrder < 0 || (idOrder == 0 && left < right);
    }
}
