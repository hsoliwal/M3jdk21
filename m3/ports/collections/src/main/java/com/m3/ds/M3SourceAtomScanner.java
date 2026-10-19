// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSourceAtomScanner}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSourceAtomScanner.java}
 * blob {@code 06a127f9e3eb52e65365efee0097865c5166c5f2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ddd83c15c7721952d4bf3ad3b90b486e4fc53676abad89146d3e34d3073a4ca3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SourceAtomScanner}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code nested types not received: LongParse, Window}</li>
 *   <li>{@code member scan(MIndexSource, long, long, MIndexAtomGrammar, long, MIndexProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3SourceAtomScanner {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3SourceAtomScanner() {
        throw new M3NotYetReceived("M3SourceAtomScanner#<init>/0");
    }
}
