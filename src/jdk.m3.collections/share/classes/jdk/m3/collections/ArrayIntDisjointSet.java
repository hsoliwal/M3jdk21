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
package jdk.m3.collections;

/**
 * Union-find over int ordinals using only {@code parent[]} and {@code componentSize[]} lanes: union by size,
 * one-pass path halving in {@link #find(int)}, no node or entry objects. Ordinals outside
 * {@code [0, size())} throw {@link IndexOutOfBoundsException}; a negative size throws
 * {@link IllegalArgumentException}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntDisjointSet.java}
 * blob {@code 93786bf4e4c4b4e308c32a6d1d12b2e95f04f871} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850}
 * by RELOCATE_M3_OWNER v1 (pattern DSU_UNION_BY_SIZE_PATH_HALVING v1, Synexia recipe dataset
 * {@code m3-project-delta-20261021/datasets/dsu-path-halving-20261030}).
 *
 * <p>API_UNION U4 (2026-11-09): this body was the public class jdk.m3.collections.IntDisjointSet; it is now the
 * array implementation of that interface.</p>
 */
final class ArrayIntDisjointSet implements IntDisjointSet {
    /** Number of int lanes per ordinal: {@code parent[]} and {@code componentSize[]}. */
    private static final long PAYLOAD_LANES = 2L;

    private final int[] parent;
    private final int[] componentSize;
    private int components;

    ArrayIntDisjointSet(int size) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        parent = new int[size];
        componentSize = new int[size];
        reset();
    }

    @Override public int elements() { return parent.length; }
    @Override public int size() { return parent.length; }
    public int components() { return components; }
    public long payloadBytes() {
        return (long) parent.length * Integer.BYTES * PAYLOAD_LANES;
    }

    public int find(int value) {
        check(value);
        // One-pass path halving: preserve the original union-by-size root while
        // reducing parent hops without a separate root-discovery walk.
        int next = parent[value];
        while (next != value) {
            int grandparent = parent[next];
            parent[value] = grandparent;
            value = grandparent;
            next = parent[value];
        }
        return value;
    }

    public boolean union(int left, int right) {
        int a = find(left);
        int b = find(right);
        if (a == b) return false;
        if (componentSize[a] < componentSize[b]) {
            int swap = a;
            a = b;
            b = swap;
        }
        parent[b] = a;
        componentSize[a] += componentSize[b];
        components--;
        return true;
    }

    public boolean connected(int left, int right) {
        return find(left) == find(right);
    }

    public int componentSize(int value) {
        return componentSize[find(value)];
    }

    public void reset() {
        components = parent.length;
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
            componentSize[i] = 1;
        }
    }

    private void check(int value) {
        if (value < 0 || value >= parent.length) throw new IndexOutOfBoundsException(value);
    }
}
