// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.framework.MIndexFrameworkPrecomputePlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/framework/src/main/java/com/synexia/mindex/framework/MIndexFrameworkPrecomputePlan.java}
 * blob {@code 5c80b014fce52c48653fb3ffc25a96d0982d388a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7198373183416d21e5f8bf81993ced4669b3d8b5e759c8897e188700bf50ea02}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.framework.M3FrameworkPrecomputePlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
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
 *   <li>{@code nested types not received: Pass}</li>
 *   <li>{@code member of(Pass...) (parameter passes)}</li>
 *   <li>{@code member includes(Pass) (parameter pass)}</li>
 * </ul>
 */
public final class M3FrameworkPrecomputePlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code mask} of type {@code int}. */
    private int mask;

    private M3FrameworkPrecomputePlan() {
        throw new M3NotYetReceived("M3FrameworkPrecomputePlan#<init>/0");
    }

    public static M3FrameworkPrecomputePlan core() {
        throw new M3NotYetReceived("M3FrameworkPrecomputePlan#core/0");
    }

    public static M3FrameworkPrecomputePlan additiveSuperset() {
        throw new M3NotYetReceived("M3FrameworkPrecomputePlan#additiveSuperset/0");
    }

    public int mask() {
        throw new M3NotYetReceived("M3FrameworkPrecomputePlan#mask/0");
    }
}
