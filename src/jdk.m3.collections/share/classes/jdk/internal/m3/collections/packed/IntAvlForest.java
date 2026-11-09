/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Primitive AVL relationships shared by independently rooted trees in one slot arena.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/IntAvlForest.java}
 * blob {@code 8a451cb1193209db2b58092d3cf77b19122c3f89} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class IntAvlForest implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int SHIFT = 6;
    static final int LEAF_SIZE = 1 << SHIFT;
    static final int MASK = LEAF_SIZE - 1;

    private int[][] left = new int[0][];
    private int[][] right = new int[0][];
    private int[][] parent = new int[0][];
    private int[][] count = new int[0][];
    private byte[][] height = new byte[0][];
    private int nextSlot = 1;
    private int freeHead;
    private int liveSlots;
    private int leaves;

    abstract void growExtraDirectory(int length);
    abstract void allocateExtraLeaf(int segment);
    abstract void clearExtraSlot(int slot);
    abstract void clearExtraLeaf(int segment);
    void refreshExtra(int slot) { }

    final int left(int s) { return s == 0 ? 0 : left[s >>> SHIFT][s & MASK]; }
    final int right(int s) { return s == 0 ? 0 : right[s >>> SHIFT][s & MASK]; }
    final int parent(int s) { return s == 0 ? 0 : parent[s >>> SHIFT][s & MASK]; }
    final int count(int s) { return s == 0 ? 0 : count[s >>> SHIFT][s & MASK]; }
    final int height(int s) { return s == 0 ? 0 : height[s >>> SHIFT][s & MASK]; }
    final int liveSlots() { return liveSlots; }
    final int allocatedLeaves() { return leaves; }
    final int issuedSlots() { return nextSlot - 1; }
    private void left(int s, int v) { left[s >>> SHIFT][s & MASK] = v; }
    private void right(int s, int v) { right[s >>> SHIFT][s & MASK] = v; }
    private void parent(int s, int v) { if (s != 0) parent[s >>> SHIFT][s & MASK] = v; }

    final int allocate() {
        int s;
        if (freeHead != 0) {
            s = freeHead;
            freeHead = left(s); // Only dead slots use the left lane as free-next.
        } else {
            if (nextSlot == Integer.MAX_VALUE) throw new IllegalStateException("Slot limit reached");
            s = nextSlot;
            ensureLeaf(s >>> SHIFT);
            nextSlot++;
        }
        left(s, 0);
        right(s, 0);
        parent(s, 0);
        count[s >>> SHIFT][s & MASK] = 1;
        height[s >>> SHIFT][s & MASK] = 1;
        liveSlots++;
        return s;
    }

    private void ensureLeaf(int segment) {
        if (segment >= left.length) {
            int n = Math.max(segment + 1, Math.max(1, left.length * 2));
            left = Arrays.copyOf(left, n);
            right = Arrays.copyOf(right, n);
            parent = Arrays.copyOf(parent, n);
            count = Arrays.copyOf(count, n);
            height = Arrays.copyOf(height, n);
            growExtraDirectory(n);
        }
        if (left[segment] == null) {
            left[segment] = new int[LEAF_SIZE];
            right[segment] = new int[LEAF_SIZE];
            parent[segment] = new int[LEAF_SIZE];
            count[segment] = new int[LEAF_SIZE];
            height[segment] = new byte[LEAF_SIZE];
            allocateExtraLeaf(segment);
            leaves++;
        }
    }

    final void refresh(int s) {
        count[s >>> SHIFT][s & MASK] = 1 + count(left(s)) + count(right(s));
        height[s >>> SHIFT][s & MASK] = (byte) (1 + Math.max(height(left(s)), height(right(s))));
        refreshExtra(s);
    }

    final void refreshToRoot(int s) {
        while (s != 0) {
            refresh(s);
            s = parent(s);
        }
    }

    final int at(int root, int rank) {
        if (rank < 0 || rank >= count(root)) throw new IndexOutOfBoundsException("rank=" + rank);
        int s = root;
        while (s != 0) {
            int smaller = count(left(s));
            if (rank == smaller) return s;
            if (rank < smaller) s = left(s);
            else { rank -= smaller + 1; s = right(s); }
        }
        throw new IllegalStateException("Broken rank metadata");
    }

    final int first(int s) {
        while (left(s) != 0) s = left(s);
        return s;
    }

    final int last(int s) {
        while (right(s) != 0) s = right(s);
        return s;
    }

    final int successor(int s) {
        if (s == 0) return 0;
        if (right(s) != 0) return first(right(s));
        int p = parent(s);
        while (p != 0 && right(p) == s) { s = p; p = parent(p); }
        return p;
    }

    final int predecessor(int s) {
        if (s == 0) return 0;
        if (left(s) != 0) return last(left(s));
        int p = parent(s);
        while (p != 0 && left(p) == s) { s = p; p = parent(p); }
        return p;
    }

    final int insertSlot(int root, int rank, int slot) {
        if (rank < 0 || rank > count(root)) throw new IndexOutOfBoundsException("rank=" + rank);
        if (root == 0) { refresh(slot); return slot; }
        int s = root;
        for (;;) {
            int smaller = count(left(s));
            if (rank <= smaller) {
                if (left(s) == 0) { left(s, slot); break; }
                s = left(s);
            } else {
                rank -= smaller + 1;
                if (right(s) == 0) { right(s, slot); break; }
                s = right(s);
            }
        }
        parent(slot, s);
        refresh(slot);
        return rebalance(s, root);
    }

    /** Removes only the requested slot; surviving payload IDs never change. */
    final int removeSlot(int root, int z) {
        if (z == 0 || height(z) == 0) throw new IllegalArgumentException("Not a live slot");
        int l = left(z), r = right(z), start;
        if (l == 0 || r == 0) {
            start = parent(z);
            root = replace(root, z, l == 0 ? r : l);
        } else {
            int y = first(r);
            if (parent(y) != z) {
                start = parent(y);
                root = replace(root, y, right(y));
                right(y, r);
                parent(r, y);
            } else {
                start = y;
            }
            root = replace(root, z, y);
            left(y, l);
            parent(l, y);
        }
        clearExtraSlot(z);
        parent(z, 0);
        right(z, 0);
        count[z >>> SHIFT][z & MASK] = 0;
        height[z >>> SHIFT][z & MASK] = 0;
        left(z, freeHead);
        freeHead = z;
        liveSlots--;
        return rebalance(start, root);
    }

    private int replace(int root, int old, int replacement) {
        int p = parent(old);
        if (p == 0) root = replacement;
        else if (left(p) == old) left(p, replacement);
        else right(p, replacement);
        parent(replacement, p);
        return root;
    }

    private int rotateLeft(int x) {
        int y = right(x), middle = left(y), p = parent(x);
        if (p != 0) { if (left(p) == x) left(p, y); else right(p, y); }
        parent(y, p);
        left(y, x);
        parent(x, y);
        right(x, middle);
        parent(middle, x);
        refresh(x);
        refresh(y);
        return y;
    }

    private int rotateRight(int x) {
        int y = left(x), middle = right(y), p = parent(x);
        if (p != 0) { if (left(p) == x) left(p, y); else right(p, y); }
        parent(y, p);
        right(y, x);
        parent(x, y);
        left(x, middle);
        parent(middle, x);
        refresh(x);
        refresh(y);
        return y;
    }

    private int balance(int s) { return height(left(s)) - height(right(s)); }

    private int rebalance(int s, int root) {
        while (s != 0) {
            refresh(s);
            if (balance(s) > 1) {
                if (balance(left(s)) < 0) rotateLeft(left(s));
                s = rotateRight(s);
            } else if (balance(s) < -1) {
                if (balance(right(s)) > 0) rotateRight(right(s));
                s = rotateLeft(s);
            }
            if (parent(s) == 0) root = s;
            s = parent(s);
        }
        return root;
    }

    final void clearArena() {
        for (int s = 0; s < leaves; s++) {
            Arrays.fill(left[s], 0);
            Arrays.fill(right[s], 0);
            Arrays.fill(parent[s], 0);
            Arrays.fill(count[s], 0);
            Arrays.fill(height[s], (byte) 0);
            clearExtraLeaf(s);
        }
        nextSlot = 1;
        freeHead = 0;
        liveSlots = 0;
    }
}
