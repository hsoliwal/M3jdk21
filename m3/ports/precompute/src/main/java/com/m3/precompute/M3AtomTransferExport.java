// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexAtomTransferExport}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexAtomTransferExport.java}
 * blob {@code 00b1c74d179180b90462b7574ffa53e6f1408674} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 1a339918180946711027dd44146d3481a26efe18a4e839d9d594172cc73af3d2}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3AtomTransferExport}); projection
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
 *   <li>{@code member build(MIndexAtomTransferIndex, MIndexUniversalLimits, IntConsumer) (parameter source)}</li>
 *   <li>{@code member append(MIndexAtomTransferIndex, MIndexApiPrecomputeTable.Builder, IntConsumer) (parameter source)}</li>
 * </ul>
 */
public final class M3AtomTransferExport {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3AtomTransferExport() {
        throw new M3NotYetReceived("M3AtomTransferExport#<init>/0");
    }
}
