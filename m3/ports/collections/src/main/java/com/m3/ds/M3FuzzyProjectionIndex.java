// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexFuzzyProjectionIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexFuzzyProjectionIndex.java}
 * blob {@code 7dd3d5eb64b381d298a27b9c18c407acc9faa38b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 613c9b578acb594190b3dba424ad0f20a7a9f6b6fc8c6258402f3c03d4878d1a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3FuzzyProjectionIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S13}.
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
 *   <li>{@code nested types not received: Reason, QueryProjection, Candidates}</li>
 * </ul>
 */
final class M3FuzzyProjectionIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code catalog} of type {@code MIndexCollectionCatalog}. */
    private int[] catalogLane;

    /** primitive lane for source field {@code gramKeys} of type {@code long[]}. */
    private long[] gramKeys;

    /** primitive lane for source field {@code gramOffsets} of type {@code int[]}. */
    private int[] gramOffsets;

    /** primitive lane for source field {@code postings} of type {@code int[]}. */
    private int[] postings;

    /** primitive lane for source field {@code rowGramCounts} of type {@code int[]}. */
    private int[] rowGramCounts;

    /** primitive lane for source field {@code primitiveBytes} of type {@code long}. */
    private long primitiveBytes;

    private M3FuzzyProjectionIndex() {
        throw new M3NotYetReceived("M3FuzzyProjectionIndex#<init>/0");
    }
}
