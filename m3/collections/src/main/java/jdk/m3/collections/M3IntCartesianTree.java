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

import java.util.Arrays;
import java.util.Objects;

/**
 * Cartesian tree over int values using parent/left/right integer-index lanes.
 *
 * <p>For MIN mode the root is a minimum; MAX mode mirrors the comparison.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntCartesianTree.java}
 * blob {@code 4e8b415e0b0249aeee49204bc4fc374f9a0d600d} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntCartesianTree {
    public enum Mode { MIN, MAX }

    private final int root;
    private final int[] parent;
    private final int[] left;
    private final int[] right;

    public M3IntCartesianTree(int[] values, Mode mode) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(mode, "mode");
        int n = values.length;
        parent = new int[n];
        left = new int[n];
        right = new int[n];
        Arrays.fill(parent, -1);
        Arrays.fill(left, -1);
        Arrays.fill(right, -1);
        if (n == 0) {
            root = -1;
            return;
        }

        int[] stack = new int[n];
        int size = 0;
        for (int i = 0; i < n; i++) {
            int last = -1;
            while (size != 0 && before(values[i], values[stack[size - 1]], mode)) {
                last = stack[--size];
            }
            if (size != 0) {
                int p = stack[size - 1];
                right[p] = i;
                parent[i] = p;
            }
            if (last >= 0) {
                left[i] = last;
                parent[last] = i;
            }
            stack[size++] = i;
        }
        root = stack[0];
    }

    public int size() { return parent.length; }
    public int root() { return root; }
    public int parent(int node) { return parent[node]; }
    public int left(int node) { return left[node]; }
    public int right(int node) { return right[node]; }
    public long payloadBytes() {
        return (long) (parent.length + left.length + right.length) * Integer.BYTES;
    }

    private static boolean before(int candidate, int retained, Mode mode) {
        return mode == Mode.MIN ? candidate < retained : candidate > retained;
    }
}
