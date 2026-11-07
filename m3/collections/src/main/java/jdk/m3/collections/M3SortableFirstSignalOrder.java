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

import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntToLongFunction;

/**
 * First-pass {@link Sortable} signal ordering over stable primitive IDs.
 *
 * <p>The existing {@link M3SortSignalLane} owns cached primitive signals and
 * dependency revision evidence. This adapter never retains Entry, Node, boxed
 * IDs, item objects, or another signal cache. An unverified signal is evaluated
 * first but can only guide provisional work: the exact comparator decides the
 * published order. A certified lexicographic prefix can decide unequal signals;
 * ties still use the exact comparator and stable-ID tie breaker.</p>
 *
 * <p>A certificate is an explicit, qualified assertion by the compiler/recipe
 * admission authority, not a property inferred from an annotation alone.
 * {@code @Sortable} declarations initially have UNVERIFIED proof status.
 * This adapter is not thread safe, as is its existing signal-lane owner.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SortableFirstSignalOrder.java}
 * blob {@code 95e8ae1cba8c4e7be3ef5aa965353c73332baf5c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SortableFirstSignalOrder implements IntBinaryOperator {
    /** Only a qualified primary lexicographic prefix may skip exact comparisons. */
    public enum Proof { UNVERIFIED, CERTIFIED_PREFIX }

    private final M3SortSignalLane signals;
    private final M3SortSignalLane.Computer first;
    private final IntToLongFunction dependencyRevision;
    private final IntBinaryOperator exact;
    private final Proof proof;
    private long exactComparisons;
    private long decisivePrefixes;

    public M3SortableFirstSignalOrder(M3SortSignalLane signals,
            M3SortSignalLane.Computer first,
            IntToLongFunction dependencyRevision,
            IntBinaryOperator exact) {
        this(signals, first, dependencyRevision, exact, Proof.UNVERIFIED);
    }

    /** Caller must possess a separate compiler/recipe receipt for CERTIFIED_PREFIX. */
    public M3SortableFirstSignalOrder(M3SortSignalLane signals,
            M3SortSignalLane.Computer first,
            IntToLongFunction dependencyRevision,
            IntBinaryOperator exact, Proof proof) {
        this.signals = Objects.requireNonNull(signals, "signals");
        this.first = Objects.requireNonNull(first, "first");
        this.dependencyRevision = Objects.requireNonNull(dependencyRevision, "dependencyRevision");
        this.exact = Objects.requireNonNull(exact, "exact");
        this.proof = Objects.requireNonNull(proof, "proof");
    }

    public Proof proof() { return proof; }
    public long exactComparisons() { return exactComparisons; }
    public long decisivePrefixes() { return decisivePrefixes; }
    public M3SortSignalLane signals() { return signals; }

    /** Reuse the prior signal until its dependency revision is invalidated. */
    public long firstSignal(int stableId) {
        if (stableId < 0) throw new IllegalArgumentException("negative stable ID");
        return signals.resolve(stableId, dependencyRevision.applyAsLong(stableId), first);
    }

    /**
     * First signal is always considered before the exact authority. When the
     * signal is unverified it never settles a published comparison, even if
     * values differ. Equal exact values retain stable ascending ID order.
     */
    @Override public int applyAsInt(int leftId, int rightId) {
        return compare(leftId, rightId);
    }

    public int compare(int leftId, int rightId) {
        if (leftId < 0 || rightId < 0) throw new IllegalArgumentException("negative stable ID");
        if (leftId == rightId) return 0;
        long a = firstSignal(leftId);
        long b = firstSignal(rightId);
        if (proof == Proof.CERTIFIED_PREFIX) {
            int order = Long.compare(a, b);
            if (order != 0) {
                decisivePrefixes++;
                return order;
            }
        }
        exactComparisons++;
        int result = exact.applyAsInt(leftId, rightId);
        return result != 0 ? Integer.signum(result) : Integer.compare(leftId, rightId);
    }

    /**
     * Provisional priority only; never use to publish an exact sorted order
     * unless a proof allows the primary prefix to decide the comparison.
     */
    public int compareFirstOnly(int leftId, int rightId) {
        if (leftId < 0 || rightId < 0) throw new IllegalArgumentException("negative stable ID");
        if (leftId == rightId) return 0;
        int signalOrder = Long.compare(firstSignal(leftId), firstSignal(rightId));
        return signalOrder != 0 ? signalOrder : Integer.compare(leftId, rightId);
    }
}
