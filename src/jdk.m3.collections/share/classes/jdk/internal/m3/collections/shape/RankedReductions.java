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
package jdk.internal.m3.collections.shape;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Cancellable O(K)-retention ranking with explicit per-visited-occurrence cache cleanup.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RankedReductions.java}
 * blob {@code 22ad2e98122890a609dbb58fb5a0274ce82ee62a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections shape factory batch (v6.2, NOT_HUB): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class RankedReductions {
    private RankedReductions() { }
    private static final class Cancelled extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Cancelled() { super(null, null, false, false); }
    }
    private static void poll(BooleanSupplier canceled) { if (canceled.getAsBoolean()) { throw new Cancelled(); } }
    /** Select using fuzzy admission; source references are never reordered. See the explicit-registry overload. */
    public static <E> RankedSelectionResult<E> select(E[] source, int from, int to, CollectionRequest request,
            Comparator<? super E> comparator, BooleanSupplier canceled, Consumer<? super E> cleanup) {
        return select(CollectionFactory.reductions(), null, source, from, to, request, comparator, canceled, cleanup);
    }
    /**
     * Select [from,to) into a best-first immutable result. Cancellation yields no values.
     * Cleanup is attempted once per visited occurrence, including failure paths; unvisited values are untouched.
     * Cleanup must preserve ranking keys and be idempotent for repeated object identities.
     */
    public static <E> RankedSelectionResult<E> select(CollectionFactory factory, String providerId,
            E[] source, int from, int to, CollectionRequest request, Comparator<? super E> comparator,
            BooleanSupplier canceled, Consumer<? super E> cleanup) {
        Objects.requireNonNull(source, "source"); Objects.requireNonNull(comparator, "comparator");
        Objects.requireNonNull(canceled, "canceled"); Objects.requireNonNull(cleanup, "cleanup");
        if (from < 0 || to < from || to > source.length) { throw new IndexOutOfBoundsException("range"); }
        Comparator<E> checked = (a, b) -> { poll(canceled); return comparator.compare(a, b); };
        var selected = Objects.requireNonNull(factory, "factory").<E>rankedSelection(request, checked, providerId);
        ShapeRankedSelection<E> retained = selected.collection();
        SelectionCleanup<E> releases = new SelectionCleanup<>(cleanup);
        E pending = null; boolean hasPending = false; int visited = 0;
        var status = RankedSelectionResult.Status.COMPLETE; List<E> result = List.of();
        try {
            poll(canceled);
            if (retained.capacity() != 0) {
                for (int i = from; i < to; i++) {
                    poll(canceled); pending = source[i]; hasPending = true; visited++;
                    boolean full = retained.size() == retained.capacity(); E old = full ? retained.worst() : null;
                    boolean accepted = retained.offer(pending); hasPending = false;
                    if (!accepted) { releases.release(pending); }
                    else if (full) { releases.release(old); }
                    releases.throwIfFailed();
                }
            }
            poll(canceled); result = retained.snapshotBestFirst(); poll(canceled);
        } catch (Cancelled stopped) {
            status = RankedSelectionResult.Status.CANCELLED; result = List.of();
        } catch (RuntimeException | Error failure) {
            releases.fail(failure);
        } finally {
            if (hasPending) { releases.release(pending); }
            try { retained.forEachRetained(releases::release); }
            finally { retained.clear(); }
        }
        releases.throwIfFailed();
        return new RankedSelectionResult<>(status, visited, result, selected.selection());
    }
}
