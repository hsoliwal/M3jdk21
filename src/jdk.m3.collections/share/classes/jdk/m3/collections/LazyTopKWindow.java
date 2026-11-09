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

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Derived, bounded top-K row-ID window over a preexisting primitive SoA collection.
 * No per-item Entry/Node/Row object and no payload permutation. The primary lane
 * is an EXACT @Sortable-qualified ordering signal, not an unproved fuzzy score.
 * A secondary lane may refine ties; final ties follow source-row order.
 *
 * A miss takes O(n log k) comparisons and O(k) primitive scratch, k <= 4096.
 * Cached exact prefixes are O(k) to reread and can be discarded under heap
 * pressure without losing source items. Deep pages should use an indexed AVL
 * source rather than repeatedly requesting increasing K.
 *
 * Neither this class nor the JVM can infer arbitrary Java object footprints.
 * SoA primitive payloads already contribute to Runtime.used heap; they are
 * not charged twice. All source mutation must be externally serialized.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LazyTopKWindow.java}
 * blob {@code b35d8f3bc37965fe63fa995d9b86be89fdbd28c7} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LazyTopKWindow {
    public static final int MAX_WINDOW = 4096;
    private static final int[] EMPTY = new int[0];

    private final PrimitiveSoaStore source;
    private final PrimitiveKind firstKind;
    private final PrimitiveKind nextKind;
    private final int firstLane;
    private final int nextLane;
    private final boolean ascending;
    private int[] cached = EMPTY;
    private long cachedRevision = -1L;
    private long builds;
    private long hits;

    public LazyTopKWindow(PrimitiveSoaStore source, int firstLane, boolean ascending) {
        this(source, firstLane, -1, ascending);
    }

    public LazyTopKWindow(PrimitiveSoaStore source, int firstLane,
            int nextLane, boolean ascending) {
        this.source = Objects.requireNonNull(source, "source");
        this.firstKind = source.laneKind(firstLane);
        if (firstLane == nextLane) throw new IllegalArgumentException("duplicate sorting lane");
        this.nextKind = nextLane == -1 ? null : source.laneKind(nextLane);
        this.firstLane = firstLane;
        this.nextLane = nextLane;
        this.ascending = ascending;
    }

    public synchronized int fill(int visibleRows, int overscanRows, int[] output, int offset) {
        return fill(visibleRows, overscanRows, output, offset, null, () -> false);
    }

    public synchronized int fill(int visibleRows, int overscanRows, int[] output, int offset,
            BooleanSupplier cancelled) {
        return fill(visibleRows, overscanRows, output, offset, null, cancelled);
    }

    /** Package-local deterministic heap observation for tests. */
    synchronized int fill(int visibleRows, int overscanRows, int[] output, int offset,
            RuntimeHeap.Snapshot observed, BooleanSupplier cancelled) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(cancelled, "cancelled");
        if (visibleRows < 0 || overscanRows < 0 || visibleRows > MAX_WINDOW
                || overscanRows > MAX_WINDOW - visibleRows)
            throw new IllegalArgumentException("unbounded Top-K window");
        int n = source.size();
        int required = Math.min(n, visibleRows);
        Objects.checkFromIndexSize(offset, required, output.length);
        checkCancelled(cancelled);
        if (required == 0) return 0;
        long revision = source.version();
        int optional = Math.min(overscanRows, n - required);
        optional = Math.min(optional, output.length - offset - required);
        RuntimeHeap.Pressure pressure = RuntimeHeap.Pressure.NORMAL;
        if (optional != 0 || cached.length < required || cachedRevision != revision) {
            RuntimeHeap.Snapshot heap = observed == null ? RuntimeHeap.current() : observed;
            pressure = heap.pressure((required + (long) optional) * Integer.BYTES);
            if (pressure == RuntimeHeap.Pressure.HIGH) optional = 0;
            else if (pressure == RuntimeHeap.Pressure.WARM) optional >>>= 1;
            long free = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
            optional = (int) Math.min((long) optional,
                    Math.max(0L, free / Integer.BYTES - required));
        }
        int target = required + optional;
        if (cachedRevision == revision && cached.length >= target) {
            checkCancelled(cancelled);
            if (source.version() != revision) throw new ConcurrentModificationException();
            System.arraycopy(cached, 0, output, offset, target);
            hits++;
        } else {
            int[] heap = new int[target];
            int count = 0;
            for (int row = 0; row < n; row++) {
                if ((row & 4095) == 0) checkCancelled(cancelled);
                if (count < target) {
                    heap[count] = row;
                    siftUp(heap, count++);
                } else if (compare(row, heap[0]) < 0) {
                    heap[0] = row;
                    siftDown(heap, count, 0);
                }
            }
            for (int end = count - 1; end > 0; end--) {
                int previous = heap[0];
                heap[0] = heap[end];
                heap[end] = previous;
                siftDown(heap, end, 0);
            }
            checkCancelled(cancelled);
            if (source.version() != revision) throw new ConcurrentModificationException();
            System.arraycopy(heap, 0, output, offset, count); // publish last
            builds++;
            if (pressure != RuntimeHeap.Pressure.HIGH) {
                cached = heap;
                cachedRevision = revision;
            }
        }
        if (pressure == RuntimeHeap.Pressure.HIGH) invalidate();
        return target;
    }

    /** Drops only reconstructible ordering state, never elements or their Java identities. */
    public synchronized void invalidate() {
        cached = EMPTY;
        cachedRevision = -1L;
    }

    public synchronized int cachedRows() { return cached.length; }
    public synchronized long cachedPrimitiveBytes() { return Integer.BYTES * (long) cached.length; }
    public synchronized long buildCount() { return builds; }
    public synchronized long cacheHits() { return hits; }
    public long sourcePrimitiveBytes() { return source.payloadBytes(); }

    private int compare(int a, int b) {
        int c = firstKind.compareBits(
                source.getBits(firstLane, a), source.getBits(firstLane, b));
        if (c == 0 && nextKind != null)
            c = nextKind.compareBits(source.getBits(nextLane, a), source.getBits(nextLane, b));
        if (c == 0) return Integer.compare(a, b);
        return ascending ? c : -c;
    }

    private void siftUp(int[] heap, int at) {
        while (at > 0) {
            int parent = (at - 1) >>> 1;
            if (compare(heap[at], heap[parent]) <= 0) return;
            int tmp = heap[at]; heap[at] = heap[parent]; heap[parent] = tmp;
            at = parent;
        }
    }

    private void siftDown(int[] heap, int length, int at) {
        while (at < (length >>> 1)) {
            int child = (at << 1) + 1;
            if (child + 1 < length && compare(heap[child + 1], heap[child]) > 0) child++;
            if (compare(heap[at], heap[child]) >= 0) return;
            int tmp = heap[at]; heap[at] = heap[child]; heap[child] = tmp;
            at = child;
        }
    }

    private static void checkCancelled(BooleanSupplier stop) {
        if (Thread.currentThread().isInterrupted() || stop.getAsBoolean())
            throw new CancellationException("M3 Top-K projection cancelled");
    }
}
