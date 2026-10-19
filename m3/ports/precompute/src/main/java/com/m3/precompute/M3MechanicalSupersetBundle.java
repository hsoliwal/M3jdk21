// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexMechanicalSupersetBundle}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexMechanicalSupersetBundle.java}
 * blob {@code 236082f93fa511d9171fbaf5bf52afdb434854f4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code eda97a09ea07d842aa76226a4e767aab08ef06c2abe38d5f74d720737a1ffd14}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3MechanicalSupersetBundle}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S23}.
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
 *   <li>{@code nested types not received: Authority, Lane, Inputs, LaneReceipt, Result}</li>
 *   <li>{@code member compile(String, MIndexPrecomputeScope, Inputs, String, MIndexPrecomputeLimits, MIndexPrecomputeCache<MIndexPrecomputeArtifact<?>>, IProgressMonitor) (return type)}</li>
 *   <li>{@code member compile(String, MIndexPrecomputeScope, Inputs, String) (return type)}</li>
 * </ul>
 */
public final class M3MechanicalSupersetBundle {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3MechanicalSupersetBundle() {
        throw new M3NotYetReceived("M3MechanicalSupersetBundle#<init>/0");
    }
}
