// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexPmem2Tier}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexPmem2Tier.java}
 * blob {@code 5d50bef0c6b71db2cd1661e4596296036c6a0a62} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d459c4a5615f2179e6e58f75a933f975d7ddc9463d166872f16718b6fc010781}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Pmem2Tier}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
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
 *   <li>{@code nested types not received: Tier, Selection}</li>
 *   <li>{@code member open(Path, Path, MIndexPmem2ShardStager.StageEvidence, boolean, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member open(Path, Path, MIndexPmem2ShardStager.StageEvidence, MIndexProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3Pmem2Tier {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3Pmem2Tier() {
        throw new M3NotYetReceived("M3Pmem2Tier#<init>/0");
    }
}
