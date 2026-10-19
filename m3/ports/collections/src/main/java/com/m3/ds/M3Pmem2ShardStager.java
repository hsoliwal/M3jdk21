// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexPmem2ShardStager}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexPmem2ShardStager.java}
 * blob {@code 9da98e9e633b0c9c8b0462aaf4ff61733448d610} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e7811d51b30087fc2add09ebdbf3215c449dd4a74d3e33c197f28e6720117f24}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Pmem2ShardStager}); projection
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
 *   <li>{@code nested types not received: Receipt, StageEvidence, ByteRange, RegionBytes}</li>
 *   <li>{@code member stage(MIndexSource, MIndexPmem2Region, long, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member stage(MIndexSource, MIndexPmem2Region, long, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member stageWithEvidence(MIndexSource, MIndexPmem2Region, long, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member stageWithEvidence(MIndexSource, MIndexPmem2Region, long, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member verify(MIndexPmem2Region, Receipt, MIndexProgressMonitor) (parameter receipt)}</li>
 *   <li>{@code member verify(MIndexPmem2Region, StageEvidence, MIndexProgressMonitor) (parameter evidence)}</li>
 * </ul>
 */
public final class M3Pmem2ShardStager {
    public static final int DEFAULT_CHUNK_BYTES = 1024 * 1024;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3Pmem2ShardStager() {
        throw new M3NotYetReceived("M3Pmem2ShardStager#<init>/0");
    }
}
