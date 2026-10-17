// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A short-lived, node-object-free comparator-equivalence index for map batch preflight.
 *
 * <p>Nodes are primitive array coordinates into the caller's alternating key/value
 * array. The first submitted key represents its comparator class, even after later
 * equivalent aliases. AVL balancing bounds search and insertion to O(log n).
 * One instance belongs to one putAll invocation; it is never retained by a view.
 */
final class M3PendingSortedKeys<K> {
    private final Comparator<? super K> order;
    private int[] left = new int[8];
    private int[] right = new int[8];
    private int[] height = new int[8];
    private int[] offsets = new int[8];
    private int root;
    private int nodes;

    M3PendingSortedKeys(Comparator<? super K> order) {
        this.order = order;
    }

    @SuppressWarnings("unchecked")
    private int compare(K a, K b) {
        return order != null ? order.compare(a, b)
                : ((Comparable<? super K>) a).compareTo(b);
    }

    /** Returns the first retained pair's key offset, or -1 when not yet staged. */
    @SuppressWarnings("unchecked")
    int firstOffset(Object[] pairs, K probe) {
        int node = root;
        while (node != 0) {
            int relation = compare(probe, (K) pairs[offsets[node]]);
            if (relation == 0) return offsets[node];
            node = relation < 0 ? left[node] : right[node];
        }
        return -1;
    }

    /** Record only an admitted first-in-class key, after its pair has been appended. */
    void recordFirst(Object[] pairs, int pairOffset) {
        if (pairOffset < 0 || (pairOffset & 1) != 0 || pairOffset + 1 >= pairs.length) {
            throw new IndexOutOfBoundsException("pairOffset");
        }
        if (nodes == Integer.MAX_VALUE - 1) throw new IllegalStateException("batch node capacity");
        int node = ++nodes;
        ensure(node + 1);
        offsets[node] = pairOffset;
        height[node] = 1;
        root = insert(root, node, pairs);
    }

    private void ensure(int required) {
        if (required <= left.length) return;
        int next = left.length;
        while (next < required) {
            int increase = next <= (Integer.MAX_VALUE - 8) / 2 ? next << 1 : Integer.MAX_VALUE - 8;
            if (increase <= next) throw new IllegalStateException("batch node capacity");
            next = increase;
        }
        left = Arrays.copyOf(left, next);
        right = Arrays.copyOf(right, next);
        height = Arrays.copyOf(height, next);
        offsets = Arrays.copyOf(offsets, next);
    }

    @SuppressWarnings("unchecked")
    private int insert(int parent, int node, Object[] pairs) {
        if (parent == 0) return node;
        int relation = compare((K) pairs[offsets[node]], (K) pairs[offsets[parent]]);
        if (relation == 0) throw new IllegalStateException("duplicate pending comparator key");
        if (relation < 0) left[parent] = insert(left[parent], node, pairs);
        else right[parent] = insert(right[parent], node, pairs);
        return balance(parent);
    }

    private int nodeHeight(int node) { return node == 0 ? 0 : height[node]; }

    private void recalc(int node) {
        height[node] = 1 + Math.max(nodeHeight(left[node]), nodeHeight(right[node]));
    }

    private int rotateRight(int y) {
        int x = left[y];
        left[y] = right[x];
        right[x] = y;
        recalc(y);
        recalc(x);
        return x;
    }

    private int rotateLeft(int x) {
        int y = right[x];
        right[x] = left[y];
        left[y] = x;
        recalc(x);
        recalc(y);
        return y;
    }

    private int balance(int node) {
        recalc(node);
        int delta = nodeHeight(left[node]) - nodeHeight(right[node]);
        if (delta > 1) {
            int child = left[node];
            if (nodeHeight(left[child]) < nodeHeight(right[child])) left[node] = rotateLeft(child);
            return rotateRight(node);
        }
        if (delta < -1) {
            int child = right[node];
            if (nodeHeight(right[child]) < nodeHeight(left[child])) right[node] = rotateRight(child);
            return rotateLeft(node);
        }
        return node;
    }
}
