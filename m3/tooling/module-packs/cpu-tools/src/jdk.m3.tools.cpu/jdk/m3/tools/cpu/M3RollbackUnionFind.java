// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Snapshot/rollback disjoint-set union over primitive arrays with no path compression. */
public final class M3RollbackUnionFind {
    private static final int MAX_ITEMS = 1_000_000;
    private final int[] parent;
    private final int[] size;
    private int[] oldRoot;
    private int[] oldChild;
    private int[] oldSize;
    private int changes;
    private int components;

    public M3RollbackUnionFind(int items) {
        if (items < 0 || items > MAX_ITEMS) throw new IllegalArgumentException("item count");
        parent = new int[items];
        size = new int[items];
        for (int i = 0; i < items; i++) { parent[i] = i; size[i] = 1; }
        components = items;
        oldRoot = new int[Math.min(8, items)];
        oldChild = new int[oldRoot.length];
        oldSize = new int[oldRoot.length];
    }

    public int elements() { return parent.length; }
    public int components() { return components; }
    /** Snapshot token is the successful-merge depth, valid until a rollback below it. */
    public int snapshot() { return changes; }

    public int find(int item) {
        Objects.checkIndex(item, parent.length);
        while (parent[item] != item) item = parent[item];
        return item;
    }

    public boolean connected(int a, int b) { return find(a) == find(b); }
    public int componentSize(int item) { return size[find(item)]; }

    /** A redundant union neither allocates history nor changes a snapshot. */
    public boolean union(int a, int b) {
        int rootA = find(a), rootB = find(b);
        if (rootA == rootB) return false;
        if (size[rootA] < size[rootB] || (size[rootA] == size[rootB] && rootA > rootB)) {
            int tmp = rootA; rootA = rootB; rootB = tmp;
        }
        grow();
        oldRoot[changes] = rootA;
        oldChild[changes] = rootB;
        oldSize[changes] = size[rootA];
        changes++;
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
        components--;
        return true;
    }

    /** Restore exactly the state captured by snapshot(), in reverse merge order. */
    public void rollback(int snapshot) {
        if (snapshot < 0 || snapshot > changes) throw new IllegalArgumentException("invalid snapshot");
        while (changes > snapshot) {
            int slot = --changes;
            int root = oldRoot[slot], child = oldChild[slot];
            parent[child] = child;
            size[root] = oldSize[slot];
            components++;
        }
    }

    private void grow() {
        if (changes < oldRoot.length) return;
        int next = Math.min(parent.length, Math.max(8, oldRoot.length * 2));
        if (next <= oldRoot.length) throw new IllegalStateException("union history full");
        oldRoot = Arrays.copyOf(oldRoot, next);
        oldChild = Arrays.copyOf(oldChild, next);
        oldSize = Arrays.copyOf(oldSize, next);
    }
}
