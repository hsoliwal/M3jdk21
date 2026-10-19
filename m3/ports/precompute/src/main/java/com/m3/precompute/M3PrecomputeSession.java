// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexPrecomputeSession}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeSession.java}
 * blob {@code 0a1e0352630eb9d837ab8d82441c2a43cd3af009} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 45cee614179b3d97a4ff3dc2fa283f8f385db1e96a336aea72f20b398340ccec}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3PrecomputeSession}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
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
 *   <li>{@code source supertypes not received: implements AutoCloseable}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 * </ul>
 */
final class M3PrecomputeSession {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code limits} of type {@code MIndexPrecomputeLimits}. */
    private int[] limitsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code monitor} of type {@code IProgressMonitor}. */
    private int[] monitorLane;

    /** primitive lane for source field {@code used} of type {@code long}. */
    private long used;

    /** int handle lane (offsets into parent-owned storage) for source field {@code nested} of type {@code IProgressMonitor}. */
    private int[] nestedLane;

    private M3PrecomputeSession() {
        throw new M3NotYetReceived("M3PrecomputeSession#<init>/0");
    }

    public void close() {
        throw new M3NotYetReceived("M3PrecomputeSession#close/0");
    }
}
