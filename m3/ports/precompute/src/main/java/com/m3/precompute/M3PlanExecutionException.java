// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.catalog.MIndexPlanExecutionException}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/catalog/MIndexPlanExecutionException.java}
 * blob {@code 53266c89afb301445b72d0dff7fb24b50780fb14} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 3706584701b4b22055cab05ab3ab3a764909491281bc0cb47ecc56f652a8781d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.catalog.M3PlanExecutionException}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: extends RuntimeException}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member receipt() (return type)}</li>
 * </ul>
 */
public final class M3PlanExecutionException {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code receipt} of type {@code MIndexExecutionReceipt}. */
    private int[] receiptLane;

    private M3PlanExecutionException() {
        throw new M3NotYetReceived("M3PlanExecutionException#<init>/0");
    }
}
