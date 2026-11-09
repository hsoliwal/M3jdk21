// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Objects;

/**
 * Allocation-bounded fixed-size heap for the nearest SimHash candidates.
 *
 * <p>The hot {@link #nearest(long, long[], int)} path computes each Hamming distance exactly once
 * and feeds it directly into the bounded heap. Retained scratch is O(k), not O(n), while the
 * public distance-then-index ordering remains unchanged. In-place heap extraction
 * replaces the quadratic final insertion sort, giving O(n log k + k log k)
 * time and O(k) scratch for k selected candidates.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-algo/src/main/java/com/synexia/algorithms/agent/TopKByHamming.java blob 21938d5fe371; recipe m3/indexdb/recipe/hamming-topk-20261106/relocate.py v2; MUTANT: negative control of the oracle, SUBST rows of the map applied
public final class M3HammingTopKTieOrderMutant {
    public record Match(int index, int distance, long fingerprint) {}

    private M3HammingTopKTieOrderMutant() {}

    public static Match[] nearest(long query, long[] candidates, int k) {
        Objects.requireNonNull(candidates, "candidates");
        if (k < 0) throw new IllegalArgumentException("k");
        int size = Math.min(k, candidates.length);
        if (size == 0) return new Match[0];

        int[] heapIndexes = new int[size];
        int[] heapDistances = new int[size];
        int used = 0;
        for (int index = 0; index < candidates.length; index++) {
            used = offer(
                    heapIndexes,
                    heapDistances,
                    used,
                    size,
                    index,
                    M3IndexDbChunkSimHash.hammingDistance(query, candidates[index]));
        }
        return materialize(candidates, heapIndexes, heapDistances, used);
    }

    public static Match[] fromDistances(int[] distances, long[] candidates, int k) {
        Objects.requireNonNull(distances, "distances");
        Objects.requireNonNull(candidates, "candidates");
        if (distances.length != candidates.length) {
            throw new IllegalArgumentException("distance/candidate length mismatch");
        }
        if (k < 0) throw new IllegalArgumentException("k");
        int size = Math.min(k, candidates.length);
        if (size == 0) return new Match[0];

        int[] heapIndexes = new int[size];
        int[] heapDistances = new int[size];
        int used = 0;
        for (int index = 0; index < candidates.length; index++) {
            int distance = distances[index];
            if (distance < 0 || distance > Long.SIZE) {
                throw new IllegalArgumentException("distance");
            }
            used = offer(heapIndexes, heapDistances, used, size, index, distance);
        }
        return materialize(candidates, heapIndexes, heapDistances, used);
    }

    private static int offer(
            int[] indexes,
            int[] distances,
            int used,
            int capacity,
            int index,
            int distance) {
        if (used < capacity) {
            indexes[used] = index;
            distances[used] = distance;
            siftUp(indexes, distances, used);
            return used + 1;
        }
        if (better(distance, index, distances[0], indexes[0])) {
            indexes[0] = index;
            distances[0] = distance;
            siftDown(indexes, distances, used, 0);
        }
        return used;
    }

    private static Match[] materialize(
            long[] candidates,
            int[] heapIndexes,
            int[] heapDistances,
            int used) {
        // The selection heap is worst-first. Standard in-place heap extraction
        // moves the worst remaining item to the end, leaving the final columns
        // in exact distance-ascending/index-ascending order without extra scratch.
        for (int remaining = used; remaining > 1; remaining--) {
            swap(heapIndexes, heapDistances, 0, remaining - 1);
            siftDown(heapIndexes, heapDistances, remaining - 1, 0);
        }
        Match[] result = new Match[used];
        for (int index = 0; index < used; index++) {
            int candidate = heapIndexes[index];
            result[index] = new Match(candidate, heapDistances[index], candidates[candidate]);
        }
        return result;
    }

    private static void siftUp(int[] indexes, int[] distances, int child) {
        int current = child;
        while (current > 0) {
            int parent = (current - 1) >>> 1;
            if (!worse(distances[current], indexes[current], distances[parent], indexes[parent])) {
                break;
            }
            swap(indexes, distances, current, parent);
            current = parent;
        }
    }

    private static void siftDown(int[] indexes, int[] distances, int size, int parent) {
        int current = parent;
        while (true) {
            int left = (current << 1) + 1;
            if (left >= size) return;
            int right = left + 1;
            int worseChild = right < size
                            && worse(distances[right], indexes[right], distances[left], indexes[left])
                    ? right
                    : left;
            if (!worse(
                    distances[worseChild],
                    indexes[worseChild],
                    distances[current],
                    indexes[current])) {
                return;
            }
            swap(indexes, distances, current, worseChild);
            current = worseChild;
        }
    }

    private static boolean better(int distance, int index, int otherDistance, int otherIndex) {
        return distance < otherDistance || (distance == otherDistance && index < otherIndex);
    }

    private static boolean worse(int distance, int index, int otherDistance, int otherIndex) {
        return distance > otherDistance || (distance == otherDistance && index < otherIndex);
    }

    private static void swap(int[] indexes, int[] distances, int left, int right) {
        int index = indexes[left];
        indexes[left] = indexes[right];
        indexes[right] = index;
        int distance = distances[left];
        distances[left] = distances[right];
        distances[right] = distance;
    }

}
