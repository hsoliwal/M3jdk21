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
 * Kruskal minimum spanning forest over primitive edge lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntMinimumSpanningForest.java}
 * blob {@code 30af22031440c0a5ca21796c682c9fffe350d48a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntMinimumSpanningForest {
    public static final class Result {
        private final long totalWeight;
        private final int componentCount;
        private final int[] edgeIndices;

        private Result(long totalWeight, int componentCount, int[] edgeIndices) {
            this.totalWeight = totalWeight;
            this.componentCount = componentCount;
            this.edgeIndices = edgeIndices;
        }

        public long totalWeight() { return totalWeight; }
        public int componentCount() { return componentCount; }
        public int edgeCount() { return edgeIndices.length; }
        public int edgeIndex(int index) { return edgeIndices[index]; }
        public int[] copyEdgeIndices() { return edgeIndices.clone(); }
    }

    private M3IntMinimumSpanningForest() {}

    public static Result kruskal(
            int vertexCount,
            int[] from,
            int[] to,
            long[] weight) {
        if (vertexCount < 0) throw new IllegalArgumentException("vertexCount < 0");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(weight, "weight");
        if (from.length != to.length || from.length != weight.length) {
            throw new IllegalArgumentException("edge lane length mismatch");
        }

        int m = from.length;
        int[] order = new int[m];
        for (int i = 0; i < m; i++) {
            if (from[i] < 0 || from[i] >= vertexCount) throw new IndexOutOfBoundsException("from=" + from[i]);
            if (to[i] < 0 || to[i] >= vertexCount) throw new IndexOutOfBoundsException("to=" + to[i]);
            order[i] = i;
        }
        sort(order, weight, from, to, 0, m - 1);

        M3IntDisjointSet dsu = new M3IntDisjointSet(vertexCount);
        int[] selected = new int[Math.min(m, Math.max(0, vertexCount - 1))];
        int selectedCount = 0;
        long total = 0L;

        for (int edge : order) {
            if (!dsu.union(from[edge], to[edge])) continue;
            selected[selectedCount++] = edge;
            total = Math.addExact(total, weight[edge]);
        }

        return new Result(total, dsu.components(), Arrays.copyOf(selected, selectedCount));
    }

    private static void sort(
            int[] order, long[] weight, int[] from, int[] to,
            int lo, int hi) {
        while (lo < hi) {
            int i = lo;
            int j = hi;
            int pivot = order[(lo + hi) >>> 1];
            while (i <= j) {
                while (compare(order[i], pivot, weight, from, to) < 0) i++;
                while (compare(order[j], pivot, weight, from, to) > 0) j--;
                if (i <= j) {
                    int value = order[i]; order[i] = order[j]; order[j] = value;
                    i++; j--;
                }
            }
            if (j - lo < hi - i) {
                if (lo < j) sort(order, weight, from, to, lo, j);
                lo = i;
            } else {
                if (i < hi) sort(order, weight, from, to, i, hi);
                hi = j;
            }
        }
    }

    private static int compare(
            int left, int right, long[] weight, int[] from, int[] to) {
        int order = Long.compare(weight[left], weight[right]);
        if (order != 0) return order;
        order = Integer.compare(from[left], from[right]);
        if (order != 0) return order;
        order = Integer.compare(to[left], to[right]);
        if (order != 0) return order;
        return Integer.compare(left, right);
    }
}
