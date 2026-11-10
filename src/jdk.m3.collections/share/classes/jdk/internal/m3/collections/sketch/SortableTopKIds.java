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
package jdk.internal.m3.collections.sketch;

import java.util.Objects;
import java.util.function.IntBinaryOperator;
import jdk.internal.m3.collections.SortSignalLane;
import jdk.internal.m3.collections.SortableFirstSignalOrder;

/**
 * Exact top-K sorting over primitive stable IDs without materializing any item or Entry object.
 *
 * <p>The caller's {@code exactOrder} may be {@link SortableFirstSignalOrder}:
 * it evaluates the first {@code Sortable} signal, reuses versioned precompute from
 * {@link SortSignalLane}, and uses exact comparison unless a compiler-qualified
 * lexicographic-prefix proof permits an early decision. This class never treats an
 * unverified first signal as an exact ordering key.</p>
 *
 * <p>Only the K result slots are retained. A worst-first heap is maintained in
 * the caller-owned result lane, then sorted in-place. This avoids O(n) temporary
 * index arrays, reference graphs, and speculative workset caches. Items remain
 * owned by their source collection; this code never swaps or destroys Java objects.
 * The result is stable by ascending numeric ID when exact keys compare equal.
 * Complexity O(n log(k+1) + k log(k+1)) comparisons and O(k) output slots.</p>
 *
 * <p>Input/output geometry is validated before writing; the source and
 * result arrays must be distinct. A zero-K request skips per-element validation. Comparator failure can leave result slots
 * partially updated but never mutates the source or any underlying item.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/SortableTopKIds.java}
 * blob {@code f4ae62eecb6032bf41e5ddc737b12372ee5ac93f} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class SortableTopKIds {
    private static final int[] EMPTY = new int[0];

    private SortableTopKIds() { }

    /** Allocate only the requested number of result IDs; never create per-item wrappers. */
    public static int[] smallest(int[] sourceIds, int from, int toExclusive,
            int maximum, IntBinaryOperator exactOrder) {
        Objects.requireNonNull(sourceIds, "sourceIds");
        Objects.checkFromToIndex(from, toExclusive, sourceIds.length);
        Objects.requireNonNull(exactOrder, "exactOrder");
        if (maximum < 0) throw new IllegalArgumentException("negative maximum");
        int count = Math.min(maximum, toExclusive - from);
        if (count == 0) return EMPTY;
        int[] result = new int[count];
        fillSmallest(sourceIds, from, toExclusive, count, result, 0, exactOrder);
        return result;
    }

    /**
     * Project globally smallest IDs to an already owned destination window.
     * An SWT viewport may reuse that window each frame without allocating more
     * cache nodes or forcing full-list sorting. The output prefix is exact and
     * ascending; the remaining source entries retain their original positions.
     *
     * @return number of IDs written, at most maximum
     */
    public static int fillSmallest(int[] sourceIds, int from, int toExclusive,
            int maximum, int[] output, int offset, IntBinaryOperator exactOrder) {
        Objects.requireNonNull(sourceIds, "sourceIds");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(exactOrder, "exactOrder");
        Objects.checkFromToIndex(from, toExclusive, sourceIds.length);
        if (maximum < 0) throw new IllegalArgumentException("negative maximum");
        int k = Math.min(maximum, toExclusive - from);
        Objects.checkFromIndexSize(offset, k, output.length);
        if (sourceIds == output) {
            throw new IllegalArgumentException("input and output lanes must be distinct");
        }
        if (k == 0) return 0;
        // Validate the entire slice before touching the caller's destination.
        for (int i = from; i < toExclusive; i++) {
            if (sourceIds[i] < 0) throw new IllegalArgumentException("negative stable ID");
        }
        System.arraycopy(sourceIds, from, output, offset, k);
        for (int node = (k >>> 1) - 1; node >= 0; node--) {
            siftDownWorst(output, offset, k, node, output[offset + node], exactOrder);
        }
        for (int i = from + k; i < toExclusive; i++) {
            int candidate = sourceIds[i];
            if (compare(candidate, output[offset], exactOrder) < 0) {
                siftDownWorst(output, offset, k, 0, candidate, exactOrder);
            }
        }
        // Max-heap -> increasing order in exactly the output window.
        for (int remaining = k; remaining > 1; remaining--) {
            int worst = output[offset];
            int replacement = output[offset + remaining - 1];
            output[offset + remaining - 1] = worst;
            siftDownWorst(output, offset, remaining - 1, 0, replacement, exactOrder);
        }
        return k;
    }

    private static int compare(int a, int b, IntBinaryOperator order) {
        int compared = order.applyAsInt(a, b);
        return compared == 0 ? Integer.compare(a, b) : compared;
    }

    private static void siftDownWorst(int[] heap, int base, int size, int start,
            int value, IntBinaryOperator order) {
        int node = start;
        while (node < (size >>> 1)) {
            int child = (node << 1) + 1;
            int candidate = heap[base + child];
            if (child + 1 < size) {
                int right = heap[base + child + 1];
                if (compare(right, candidate, order) > 0) {
                    child++;
                    candidate = right;
                }
            }
            if (compare(value, candidate, order) >= 0) break;
            heap[base + node] = candidate;
            node = child;
        }
        heap[base + node] = value;
    }
}
