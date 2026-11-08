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
import java.util.function.IntConsumer;
import jdk.m3.collections.M3RuntimeHeap;
import jdk.internal.m3.collections.SortSignalLane;
import jdk.internal.m3.collections.SortableFirstSignalOrder;

/**
 * Memory-bounded top-K over stable IDs, intended for marker trees and sorted M3 viewports.
 * The caller's items, their identity and the input ID lane never move. One {@code int[k]}
 * simultaneously serves as selection heap and ascending result; no Entry/Node/row facade
 * is constructed. O(n log(k) + k log(k)) comparisons and O(k) required ID storage.
 *
 * <p>When the comparator is {@link SortableFirstSignalOrder}, the first {@link Sortable}
 * signal is cached by its canonical {@link SortSignalLane}. Unverified signals cannot
 * determine the published ordering: that owner's exact comparator remains authoritative.
 * An arbitrary comparator must describe one stable, transitive order during this call.
 * Ties are broken by stable ID for deterministic results, not by input occurrence.</p>
 *
 * <p>At one selection boundary the JVM's effective Xmx/used heap is observed only if
 * optional cache cleanup was supplied. Under WARM/HIGH pressure the optional callback
 * may release <em>reconstructible secondary payload caches</em> for rejected items.
 * It must never destroy live items, pinned facades, Java references, the input array,
 * source marker semantics or canonical first-signal evidence. Callbacks are synchronous,
 * may be repeated for duplicate IDs, and may throw; failure returns no result.
 * The required k output cannot be silently truncated to satisfy a memory estimate.
 * Heap observations are advisory; storage allocation can still fail.</p>
 *
 * <p>This class and its comparator are not thread safe when underlying item state can
 * mutate. No JNI call occurs in this Java path; the separate direct-buffer native
 * kernel accepts <em>only</em> fully certified signed-long keys with stable-ID ties.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/MarkerTopKIndex.java}
 * blob {@code cfade805dd71f3852c67101ad8e317b9304319ab} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class MarkerTopKIndex {
    private MarkerTopKIndex() { }

    /** Exact-contract selection; never touches caller-owned item payloads. */
    public static int[] smallest(int[] source, int from, int toExclusive, int k,
            IntBinaryOperator exactOrder) {
        return smallest(source, from, toExclusive, k, exactOrder, null);
    }

    /**
     * Selection with an optional pressure-sensitive, secondary-cache release callback.
     * First-signal evidence remains owned and retained by the supplied comparator.
     */
    public static int[] smallest(int[] source, int from, int toExclusive, int k,
            IntBinaryOperator exactOrder, IntConsumer releaseOptionalCache) {
        return smallest(source, from, toExclusive, k, exactOrder, releaseOptionalCache, null);
    }

    /** Deterministic package-private heap-pressure seam for contract oracles. */
    static int[] smallest(int[] source, int from, int toExclusive, int k,
            IntBinaryOperator exactOrder, IntConsumer releaseOptionalCache,
            M3RuntimeHeap.Snapshot observedHeap) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(exactOrder, "exactOrder");
        Objects.checkFromToIndex(from, toExclusive, source.length);
        int count = toExclusive - from;
        if (k < 0 || k > count) throw new IllegalArgumentException("k outside source range");
        if (k == 0) return new int[0]; // no item scan or heap observation
        // Fail before any optional cache release, comparator work or allocated heap.
        for (int i = from; i < toExclusive; i++) {
            if (source[i] < 0) throw new IllegalArgumentException("negative stable ID");
        }
        // Sample after source validation and only for an actually requested top-K.
        M3RuntimeHeap.Snapshot pressureSample = releaseOptionalCache == null ? null
                : observedHeap == null ? M3RuntimeHeap.current() : observedHeap;
        boolean trim = pressureSample != null && pressureSample.pressure(
                (long) k * Integer.BYTES + 16L) != M3RuntimeHeap.Pressure.NORMAL;

        int[] heap = new int[k];
        System.arraycopy(source, from, heap, 0, k);
        for (int i = (k >>> 1) - 1; i >= 0; i--) siftMax(heap, i, k, exactOrder);
        for (int i = from + k; i < toExclusive; i++) {
            int candidate = source[i];
            if (compare(candidate, heap[0], exactOrder) < 0) {
                int former = heap[0];
                heap[0] = candidate;
                siftMax(heap, 0, k, exactOrder);
                if (trim) releaseOptionalCache.accept(former);
            } else if (trim) {
                releaseOptionalCache.accept(candidate);
            }
        }
        // Heap-sort the selected IDs in place; no k-sized scratch/output duplicate.
        for (int end = k - 1; end > 0; end--) {
            int tmp = heap[0];
            heap[0] = heap[end];
            heap[end] = tmp;
            siftMax(heap, 0, end, exactOrder);
        }
        return heap;
    }

    /** Exact published comparator plus stable-ID tie breaking. */
    private static int compare(int a, int b, IntBinaryOperator order) {
        if (a == b) return 0;
        int result = order.applyAsInt(a, b);
        return result == 0 ? Integer.compare(a, b) : Integer.signum(result);
    }

    /** Primitive maximum heap, invariant: the least desirable selected ID is at root. */
    private static void siftMax(int[] heap, int start, int size,
            IntBinaryOperator exactOrder) {
        int id = heap[start];
        int node = start;
        while (node < size / 2) {
            int child = (node << 1) + 1;
            if (child + 1 < size && compare(heap[child], heap[child + 1], exactOrder) < 0)
                child++;
            if (compare(id, heap[child], exactOrder) >= 0) break;
            heap[node] = heap[child];
            node = child;
        }
        heap[node] = id;
    }
}
