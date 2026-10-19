// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongAtomPostings}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexLongAtomPostings.java}
 * blob {@code edb78ac03bc76abb118c71e4403bdbd89a2173a2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ec0c8e6d2b86543363a25e26d236eacbff1a3c0fb46b83080471ef5f354f46d1}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongAtomPostings}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
final class M3LongAtomPostings {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code offsets} of type {@code long[]}. */
    private long[] offsets;

    /** primitive lane for source field {@code coordinates} of type {@code long[][]}. */
    private long[][] coordinates;

    private M3LongAtomPostings() {
        throw new M3NotYetReceived("M3LongAtomPostings#<init>/0");
    }
}
