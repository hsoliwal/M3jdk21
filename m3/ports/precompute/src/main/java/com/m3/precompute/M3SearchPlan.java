// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexSearchPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexSearchPlan.java}
 * blob {@code 5c13dd4cdc239d9fafde4cfc282448fd6e9d4558} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 420df92207ad9952636544a81900b4703803ad4b70fd0e82fa5e25205fec4415}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3SearchPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
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
 *   <li>{@code nested types not received: Statistics, Evaluator}</li>
 *   <li>{@code member query() (return type)}</li>
 *   <li>{@code member result() (return type)}</li>
 *   <li>{@code member statistics() (return type)}</li>
 * </ul>
 */
public final class M3SearchPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code query} of type {@code MIndexSearchQuery}. */
    private int[] queryLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code result} of type {@code MIndexQueryResult}. */
    private int[] resultLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code statistics} of type {@code Statistics}. */
    private int[] statisticsLane;

    private M3SearchPlan() {
        throw new M3NotYetReceived("M3SearchPlan#<init>/0");
    }

    public String explain() {
        throw new M3NotYetReceived("M3SearchPlan#explain/0");
    }
}
