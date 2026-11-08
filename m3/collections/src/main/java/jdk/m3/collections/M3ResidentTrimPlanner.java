// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import jdk.internal.m3.collections.LazyElementFlags;

/**
 * Cold-path, index-only LRU victim selection for parent-owned facades.
 * No Entry/Node/Consumer or copied timestamp lanes; public pinned identity wins.
 * Uses a bounded top-k max heap for sparse trimming, index-only min-heap
 * for medium trimming, and an index-plus-priority min heap for full eviction.
 * The last branch improves full traversal locality without eager source copies.
 */
//FLLDCIM: RELOCATE - donor blob 0aa7495771e3 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
final class M3ResidentTrimPlanner {
    private M3ResidentTrimPlanner() { }

    static int[] oldestSlots(short[] flags, long[] lastUse, int count, int requested) {
        if (flags == null || lastUse == null) throw new NullPointerException("facade metadata");
        if (count < 0 || count > flags.length || count > lastUse.length || requested < 0) {
            throw new IllegalArgumentException("invalid residency geometry");
        }
        if (requested == 0 || count == 0) return new int[0];
        int candidates = 0;
        for (int i = 0; i < count; i++) {
            if (!LazyElementFlags.has(flags[i], LazyElementFlags.PINNED)) candidates++;
        }
        if (candidates == 0) return new int[0];
        int wanted = Math.min(requested, candidates);
        // For sparse evictions retain only the k smallest keys, without a full
        // candidates-sized queue or per-victim iteration over all entries.
        if (wanted <= candidates / 4) return sparseOldest(flags, lastUse, count, wanted);
        if (wanted <= candidates - (candidates >>> 2)) {
            return mediumOldest(flags, lastUse, count, candidates, wanted);
        }
        return denseOldest(flags, lastUse, count, candidates, wanted);
    }

    /** O(count log k) comparisons and O(k) primitive scratch, with stable ties. */
    private static int[] sparseOldest(short[] flags, long[] lastUse, int count, int wanted) {
        int[] worstFirst = new int[wanted];
        int size = 0;
        for (int slot = 0; slot < count; slot++) {
            if (LazyElementFlags.has(flags[slot], LazyElementFlags.PINNED)) continue;
            if (size < wanted) {
                int position = size++;
                while (position > 0) {
                    int parent = (position - 1) >>> 1;
                    if (compare(lastUse, slot, worstFirst[parent]) <= 0) break;
                    worstFirst[position] = worstFirst[parent];
                    position = parent;
                }
                worstFirst[position] = slot;
            } else if (compare(lastUse, slot, worstFirst[0]) < 0) {
                maxDown(worstFirst, size, 0, slot, lastUse);
            }
        }
        int[] ordered = new int[wanted];
        for (int at = wanted - 1; at >= 0; at--) {
            ordered[at] = worstFirst[0];
            int last = worstFirst[--size];
            if (size != 0) maxDown(worstFirst, size, 0, last, lastUse);
        }
        return ordered;
    }

    /** O(count + wanted log count) operations and one int[] heap. */
    private static int[] mediumOldest(short[] flags, long[] lastUse,
            int count, int candidates, int wanted) {
        int[] heap = new int[candidates];
        int size = 0;
        for (int slot = 0; slot < count; slot++) {
            if (!LazyElementFlags.has(flags[slot], LazyElementFlags.PINNED)) {
                heap[size++] = slot;
            }
        }
        for (int at = (size >>> 1) - 1; at >= 0; at--) {
            minIndexDown(heap, size, at, heap[at], lastUse);
        }
        int[] ordered = new int[wanted];
        for (int at = 0; at < wanted; at++) {
            ordered[at] = heap[0];
            int last = heap[--size];
            if (size != 0) minIndexDown(heap, size, 0, last, lastUse);
        }
        return ordered;
    }

    /** O(count + wanted log count), two primitive heaps; compact locality for full trims. */
    private static int[] denseOldest(short[] flags, long[] lastUse,
            int count, int candidates, int wanted) {
        int[] bestFirst = new int[candidates];
        long[] epochs = new long[candidates];
        int size = 0;
        for (int slot = 0; slot < count; slot++) {
            if (!LazyElementFlags.has(flags[slot], LazyElementFlags.PINNED)) {
                bestFirst[size] = slot;
                epochs[size++] = lastUse[slot];
            }
        }
        for (int at = (size >>> 1) - 1; at >= 0; at--) {
            minDown(bestFirst, epochs, size, at, bestFirst[at], epochs[at]);
        }
        int[] ordered = new int[wanted];
        for (int at = 0; at < wanted; at++) {
            ordered[at] = bestFirst[0];
            int last = bestFirst[--size];
            long age = epochs[size];
            if (size != 0) minDown(bestFirst, epochs, size, 0, last, age);
        }
        return ordered;
    }

    private static int compare(long[] lastUse, int left, int right) {
        int byEpoch = Long.compare(lastUse[left], lastUse[right]);
        return byEpoch != 0 ? byEpoch : Integer.compare(left, right);
    }

    private static void maxDown(int[] heap, int size, int at, int value, long[] lastUse) {
        int half = size >>> 1;
        while (at < half) {
            int child = (at << 1) + 1;
            int right = child + 1;
            if (right < size && compare(lastUse, heap[right], heap[child]) > 0) child = right;
            if (compare(lastUse, heap[child], value) <= 0) break;
            heap[at] = heap[child];
            at = child;
        }
        heap[at] = value;
    }

    private static void minIndexDown(int[] heap, int size, int at,
            int value, long[] lastUse) {
        int half = size >>> 1;
        while (at < half) {
            int child = (at << 1) + 1;
            int right = child + 1;
            if (right < size && compare(lastUse, heap[right], heap[child]) < 0) child = right;
            if (compare(lastUse, heap[child], value) >= 0) break;
            heap[at] = heap[child];
            at = child;
        }
        heap[at] = value;
    }

    private static void minDown(int[] heap, long[] epochs, int size,
            int at, int slot, long age) {
        int half = size >>> 1;
        while (at < half) {
            int child = (at << 1) + 1;
            int right = child + 1;
            if (right < size && before(epochs[right], heap[right],
                    epochs[child], heap[child])) child = right;
            if (!before(epochs[child], heap[child], age, slot)) break;
            heap[at] = heap[child];
            epochs[at] = epochs[child];
            at = child;
        }
        heap[at] = slot;
        epochs[at] = age;
    }

    private static boolean before(long leftAge, int leftSlot, long rightAge, int rightSlot) {
        int byAge = Long.compare(leftAge, rightAge);
        return byAge != 0 ? byAge < 0 : leftSlot < rightSlot;
    }
}
