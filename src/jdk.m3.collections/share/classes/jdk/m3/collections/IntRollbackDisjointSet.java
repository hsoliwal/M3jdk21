/* *****************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Rollback-capable union-find using primitive arrays only.
 *
 * <p>parentOrSize[root] is negative component size. Path compression is
 * deliberately omitted so snapshots can be restored exactly; union-by-size
 * keeps find depth logarithmic.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntRollbackDisjointSet.java}
 * blob {@code a3139dfd0fa7b70578e61f22fa5afa006fbb57df} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntRollbackDisjointSet {
    private final int[] parentOrSize;
    private int[] historyChild;
    private int[] historyParentSize;
    private int historySize;
    private int components;

    public IntRollbackDisjointSet(int size) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        parentOrSize = new int[size];
        Arrays.fill(parentOrSize, -1);
        historyChild = new int[Math.max(1, Math.min(16, size))];
        historyParentSize = new int[historyChild.length];
        components = size;
    }

    public int size() { return parentOrSize.length; }
    public int components() { return components; }
    public int historySize() { return historySize; }
    public long payloadBytes() {
        return (long) (parentOrSize.length + historyChild.length + historyParentSize.length)
                * Integer.BYTES;
    }

    public int find(int value) {
        check(value);
        int root = value;
        while (parentOrSize[root] >= 0) root = parentOrSize[root];
        return root;
    }

    public int componentSize(int value) {
        return -parentOrSize[find(value)];
    }

    public boolean connected(int left, int right) {
        return find(left) == find(right);
    }

    public int snapshot() {
        return historySize;
    }

    public boolean union(int left, int right) {
        int a = find(left);
        int b = find(right);
        if (a == b) return false;

        if (parentOrSize[a] > parentOrSize[b]) {
            int swap = a;
            a = b;
            b = swap;
        }

        ensureHistory(historySize + 1);
        historyChild[historySize] = b;
        historyParentSize[historySize] = parentOrSize[a];
        historySize++;

        parentOrSize[a] += parentOrSize[b];
        parentOrSize[b] = a;
        components--;
        return true;
    }

    public void rollback(int snapshot) {
        if (snapshot < 0 || snapshot > historySize) {
            throw new IllegalArgumentException("invalid snapshot " + snapshot);
        }
        while (historySize > snapshot) {
            int historyIndex = --historySize;
            int child = historyChild[historyIndex];
            int parent = parentOrSize[child];
            int combinedSize = parentOrSize[parent];
            int oldParentSize = historyParentSize[historyIndex];
            int oldChildSize = combinedSize - oldParentSize;

            parentOrSize[parent] = oldParentSize;
            parentOrSize[child] = oldChildSize;
            historyChild[historyIndex] = 0;
            historyParentSize[historyIndex] = 0;
            components++;
        }
    }

    public void reset() {
        Arrays.fill(parentOrSize, -1);
        // Popped and unused history slots are always zero; only live entries
        // can contain prior component metadata. Preserve zeroed spare capacity.
        Arrays.fill(historyChild, 0, historySize, 0);
        Arrays.fill(historyParentSize, 0, historySize, 0);
        historySize = 0;
        components = parentOrSize.length;
    }

    private void ensureHistory(int required) {
        if (required <= historyChild.length) return;
        int next = Math.max(required, historyChild.length + (historyChild.length >>> 1) + 1);
        historyChild = Arrays.copyOf(historyChild, next);
        historyParentSize = Arrays.copyOf(historyParentSize, next);
    }

    private void check(int value) {
        if (value < 0 || value >= parentOrSize.length) {
            throw new IndexOutOfBoundsException(value);
        }
    }
}
