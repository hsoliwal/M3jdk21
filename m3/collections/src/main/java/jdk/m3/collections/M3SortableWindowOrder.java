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

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntToLongFunction;
import java.util.function.LongSupplier;

/**
 * Bounded, non-destructive top-K ordering over stable primitive IDs. The sole heap is
 * an {@code int[]} containing positions into the caller's existing ID array; no
 * Entry/Node/Row objects or retained item payloads are constructed.
 *
 * <p>The first {@code @Sortable} signal is used to establish exact order only
 * after a separate caller/compiler proof that different signal values preserve
 * the complete comparison order. Merely annotating a field is not that proof.
 * Unqualified hints can prioritize background work, but cannot change a sorted
 * result. The final exact comparator is mandatory in every mode.</p>
 *
 * <p>Only optional overscan is shed under JVM heap pressure. Mandatory visible
 * results remain requested. The selector explicitly samples Runtime at most once
 * per window; the canonical signal cache can also sample at backing-array growth
 * boundaries, never at ordinary cache hits. Source item residency remains caller-owned.
 * This is not thread safe; an optional generation oracle rejects concurrent edits.
 * An exception never partially overwrites the supplied output.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SortableWindowOrder.java}
 * blob {@code c1562de2a8a3d5c4863b7cfa7043da8df5a15d2b} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SortableWindowOrder {
    public static final int MAX_WINDOW = 4_096;
    private static final long ESTIMATED_ITEM_WORK_BYTES = 256L;
    private static final long SIGNAL_CACHE_MAX_BYTES = 32L << 20;

    public enum SignalAuthority {
        /** The comparator's sign is proven to agree with any unequal primary signal. */
        CERTIFIED_PRIMARY,
        /** Merely a hint; exact sorted output uses the comparator exclusively. */
        UNVERIFIED_HINT
    }

    private final M3SortSignalLane cache;
    private final M3SortSignalLane.Computer signalComputer;
    private final IntToLongFunction revisionOf;
    private final IntBinaryOperator exactOrder;
    private final SignalAuthority authority;
    private final LongSupplier generation;

    /** All supplied callbacks are reusable; no callback is allocated per element. */
    public M3SortableWindowOrder(M3SortSignalLane cache,
            M3SortSignalLane.Computer signalComputer,
            IntToLongFunction revisionOf, IntBinaryOperator exactOrder,
            SignalAuthority authority) {
        this(cache, signalComputer, revisionOf, exactOrder, authority, null);
    }

    /** Optional generation oracle detects changes during a selection. */
    public M3SortableWindowOrder(M3SortSignalLane cache,
            M3SortSignalLane.Computer signalComputer,
            IntToLongFunction revisionOf, IntBinaryOperator exactOrder,
            SignalAuthority authority, LongSupplier generation) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.signalComputer = Objects.requireNonNull(signalComputer, "signalComputer");
        this.revisionOf = Objects.requireNonNull(revisionOf, "revisionOf");
        this.exactOrder = Objects.requireNonNull(exactOrder, "exactOrder");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.generation = generation;
    }

    /**
     * Select the globally smallest visible IDs in exact order. The output slice
     * must fit mandatory visible rows; optional overscan is automatically clipped.
     * A return of zero for visibleRows==0 performs no signal, comparator, heap or
     * Runtime work. Stable ties preserve source occurrence order.
     */
    public int selectSorted(int[] ids, int from, int to, int visibleRows,
            int optionalOverscan, int[] output, int outputOffset) {
        return select(ids, from, to, visibleRows, optionalOverscan, output,
                outputOffset, false, null);
    }

    /**
     * Speculative work priorities from unverified {@code @Sortable} hints.
     * These IDs are NOT necessarily in semantic sorted order and must never be
     * presented as a sorted view or used to discard exact-match candidates.
     */
    public int selectPrefetchHints(int[] ids, int from, int to, int maximum,
            int[] output, int outputOffset) {
        return select(ids, from, to, maximum, 0, output, outputOffset, true, null);
    }

    /** Deterministic pressure input for package-local correctness tests. */
    int select(int[] ids, int from, int to, int visibleRows,
            int optionalOverscan, int[] output, int outputOffset,
            boolean hintsOnly, M3RuntimeHeap.Snapshot observed) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, ids.length);
        if (visibleRows < 0 || optionalOverscan < 0 || visibleRows > MAX_WINDOW
                || optionalOverscan > MAX_WINDOW - visibleRows) {
            throw new IllegalArgumentException("unbounded sorted window");
        }
        int count = to - from;
        int mandatory = Math.min(visibleRows, count);
        Objects.checkFromIndexSize(outputOffset, mandatory, output.length);
        if (mandatory == 0) return 0;
        // A window must not change the caller's original ID sequence, even
        // when the caller accidentally supplies that sequence as the output.
        if (ids == output) throw new IllegalArgumentException("output aliases source IDs");

        int optional = 0;
        boolean needSnapshot = optionalOverscan > 0
                || (hintsOnly || authority == SignalAuthority.CERTIFIED_PRIMARY);
        M3RuntimeHeap.Snapshot heap = observed != null ? observed
                : needSnapshot ? M3RuntimeHeap.current() : null;
        if (optionalOverscan > 0 && mandatory == visibleRows) {
            long reserve = heap.safetyReserve();
            long free = Math.max(0L, heap.freeToMaximum() - reserve);
            if (heap.pressure(0L) == M3RuntimeHeap.Pressure.NORMAL) {
                optional = (int) Math.min((long) optionalOverscan,
                        free / ESTIMATED_ITEM_WORK_BYTES);
            }
        }
        optional = Math.min(optional, output.length - outputOffset - mandatory);
        int wanted = Math.min(count, mandatory + optional);

        // Avoid allocating a large dense signal cache for extremely sparse IDs
        // or when the full new backing is not affordable. Already cached evidence
        // is retained; the exact comparator remains the semantic fallback.
        boolean signals = (hintsOnly || authority == SignalAuthority.CERTIFIED_PRIMARY)
                && (heap == null || heap.pressure(0L) == M3RuntimeHeap.Pressure.NORMAL)
                && affordableSignals(ids, from, to, heap);
        if (hintsOnly && !signals) {
            // Under pressure, retain original source order for *prefetch only*.
            // This does not claim that the result is a sorted window.
            long before = generation == null ? 0L : generation.getAsLong();
            if (generation != null && generation.getAsLong() != before)
                throw new ConcurrentModificationException("M3 source generation changed");
            System.arraycopy(ids, from, output, outputOffset, wanted);
            return wanted;
        }

        long before = generation == null ? 0L : generation.getAsLong();
        int[] heapIds = new int[wanted];
        int occupied = 0;
        for (int position = from; position < to; position++) {
            if (ids[position] < 0 || ids[position] >= Integer.MAX_VALUE - 1)
                throw new IllegalArgumentException("stable ID outside supported range");
            if (occupied < wanted) {
                heapIds[occupied] = position;
                siftUpMax(heapIds, occupied++, ids, signals, hintsOnly);
            } else if (comparePositions(position, heapIds[0], ids, signals, hintsOnly) < 0) {
                heapIds[0] = position;
                siftDownMax(heapIds, 0, occupied, ids, signals, hintsOnly);
            }
        }
        for (int end = occupied - 1; end > 0; end--) {
            int save = heapIds[0];
            heapIds[0] = heapIds[end];
            heapIds[end] = save;
            siftDownMax(heapIds, 0, end, ids, signals, hintsOnly);
        }
        if (generation != null && generation.getAsLong() != before)
            throw new ConcurrentModificationException("M3 source generation changed");
        for (int i = 0; i < occupied; i++) output[outputOffset + i] = ids[heapIds[i]];
        return occupied;
    }

    private boolean affordableSignals(int[] ids, int from, int to,
            M3RuntimeHeap.Snapshot heap) {
        int highest = 0;
        for (int i = from; i < to; i++) {
            int id = ids[i];
            if (id < 0 || id >= Integer.MAX_VALUE - 1)
                throw new IllegalArgumentException("stable ID outside supported range");
            highest = Math.max(highest, id);
        }
        int slots = cache.reservedSlots();
        if (highest < slots) return true; // Never discard already computed evidence.
        long neededSlots = (long) highest + 1L;
        // M3SortSignalLane may grow geometrically several times while filling.
        // Bound the largest possible replacement array, not merely the required
        // index or its first geometric step.
        long largestNewSlots = Math.max(neededSlots + (neededSlots >>> 1) + 2L,
                slots + (slots >>> 1) + 1L);
        long bytes = largestNewSlots * (2L * Long.BYTES + Byte.BYTES);
        long spare = heap == null ? Long.MAX_VALUE
                : Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
        // Strict budget: bounded total lane weight and peak room for two arrays.
        return bytes <= SIGNAL_CACHE_MAX_BYTES && bytes <= spare / 2L
                && heap != null && heap.pressure(bytes) == M3RuntimeHeap.Pressure.NORMAL;
    }

    private int comparePositions(int left, int right, int[] ids,
            boolean signals, boolean hintsOnly) {
        int leftId = ids[left];
        int rightId = ids[right];
        if (signals) {
            long a = cache.resolve(leftId, revisionOf.applyAsLong(leftId), signalComputer);
            long b = cache.resolve(rightId, revisionOf.applyAsLong(rightId), signalComputer);
            int bySignal = Long.compare(a, b);
            if (hintsOnly) return bySignal != 0 ? bySignal : Integer.compare(left, right);
            if (bySignal != 0 && authority == SignalAuthority.CERTIFIED_PRIMARY)
                return bySignal;
        }
        // The exact comparator remains the ordering authority for unverified hints,
        // and for ties of a separately certified order-preserving primary signal.
        int result = exactOrder.applyAsInt(leftId, rightId);
        return result != 0 ? Integer.signum(result) : Integer.compare(left, right);
    }

    private void siftUpMax(int[] heap, int child, int[] ids,
            boolean signals, boolean hintsOnly) {
        while (child > 0) {
            int parent = (child - 1) >>> 1;
            if (comparePositions(heap[parent], heap[child], ids, signals, hintsOnly) >= 0)
                break;
            int saved = heap[parent]; heap[parent] = heap[child]; heap[child] = saved;
            child = parent;
        }
    }

    private void siftDownMax(int[] heap, int parent, int size, int[] ids,
            boolean signals, boolean hintsOnly) {
        while (parent < size / 2) {
            int child = (parent << 1) + 1;
            if (child + 1 < size && comparePositions(heap[child], heap[child + 1],
                    ids, signals, hintsOnly) < 0) child++;
            if (comparePositions(heap[parent], heap[child], ids, signals, hintsOnly) >= 0)
                break;
            int saved = heap[parent]; heap[parent] = heap[child]; heap[child] = saved;
            parent = child;
        }
    }
}
