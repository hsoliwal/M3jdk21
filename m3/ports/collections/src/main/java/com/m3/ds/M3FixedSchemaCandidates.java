// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexFixedSchemaCandidates}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexFixedSchemaCandidates.java}
 * blob {@code 444b0bc0a3d7804fe0d91469cd69fc99c3364a49} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 70ba97ee15750a3ab0456ce4cbf1225051ff1a37f623367edd25c2b0db1cb312}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3FixedSchemaCandidates}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Budget, Candidate, Result}</li>
 *   <li>{@code member rank(List<MIndexFixedRecordPlan>, List<byte[]>, Budget, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member rank(List<MIndexFixedRecordPlan>, List<byte[]>, MIndexFixedRecordParser.SpanScanner, Budget, MIndexProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3FixedSchemaCandidates {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3FixedSchemaCandidates() {
        throw new M3NotYetReceived("M3FixedSchemaCandidates#<init>/0");
    }
}
