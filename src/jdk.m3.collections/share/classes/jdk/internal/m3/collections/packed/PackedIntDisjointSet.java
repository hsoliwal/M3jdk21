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

import jdk.m3.collections.IntDisjointSet;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed singleton IDs, union by size, path compression; one int lane, single-threaded.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntDisjointSet.java}
 * blob {@code 877244fe87aeb7fabd8a6a25572eeb60a61007d1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections disjoint-set + segmented lane batch after API_UNION U4 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedIntDisjointSet implements IntDisjointSet {
    private final int[] parents;
    private int components;
    public PackedIntDisjointSet(int elements) {
        if (elements < 0 || elements > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("elements");
        parents = new int[elements]; reset();
    }
    public int elements() { return parents.length; }
    public int components() { return components; }
    public long arrayPayloadBytes() { return 4L * parents.length; }
    public int find(int value) {
        Objects.checkIndex(value, parents.length);
        int root = value;
        while (parents[root] >= 0) root = parents[root];
        while (value != root) { int next = parents[value]; parents[value] = root; value = next; }
        return root;
    }
    public int componentSize(int value) { return -parents[find(value)]; }
    public boolean connected(int left, int right) { return find(left) == find(right); }
    public boolean union(int left, int right) {
        Objects.checkIndex(left, parents.length); Objects.checkIndex(right, parents.length);
        int a = find(left), b = find(right); if (a == b) return false;
        if (parents[a] > parents[b]) { int swap = a; a = b; b = swap; }
        parents[a] += parents[b]; parents[b] = a; components--; return true;
    }
    public void reset() { Arrays.fill(parents, -1); components = parents.length; }
    public long payloadBytes() { return arrayPayloadBytes(); }
}
