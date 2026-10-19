// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexPageableTreeLcaRmqIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPageableTreeLcaRmqIndex.java}
 * blob {@code 9dc6ffa4f061a1f3a59c5b6548df92a970d37af7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 40de5250b1eef315e14e50bb0c7928bdb5012c6121e19108f320d22fc886dbb2}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3PageableTreeLcaRmqIndex}); projection
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
 *   <li>{@code member compile(MIndexPageableTreeQueryIndex) (parameter query)}</li>
 *   <li>{@code member compile(MIndexPageableTreeQueryIndex, long, IProgressMonitor) (parameter query)}</li>
 *   <li>{@code member estimateRetainedPayloadBytes(MIndexPageableTree) (parameter tree)}</li>
 *   <li>{@code member queryIndex() (return type)}</li>
 *   <li>{@code member tree() (return type)}</li>
 * </ul>
 */
public final class M3PageableTreeLcaRmqIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code query} of type {@code MIndexPageableTreeQueryIndex}. */
    private int[] queryLane;

    /** primitive lane for source field {@code firstOccurrence} of type {@code int[]}. */
    private int[] firstOccurrence;

    /** primitive lane for source field {@code eulerPreorder} of type {@code int[]}. */
    private int[] eulerPreorder;

    /** primitive lane for source field {@code log2} of type {@code int[]}. */
    private int[] log2;

    /** primitive lane for source field {@code sparseOffsets} of type {@code int[]}. */
    private int[] sparseOffsets;

    /** primitive lane for source field {@code sparseArgMin} of type {@code int[]}. */
    private int[] sparseArgMin;

    /** primitive lane for source field {@code fingerprint64} of type {@code long}. */
    private long fingerprint64;

    private M3PageableTreeLcaRmqIndex() {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#<init>/0");
    }

    public long fingerprint64() {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#fingerprint64/0");
    }

    public int eulerLength() {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#eulerLength/0");
    }

    public int sparseLevelCount() {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#sparseLevelCount/0");
    }

    public long lowestCommonAncestor(long leftId, long rightId) {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#lowestCommonAncestor/2");
    }

    public int distance(long leftId, long rightId) {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#distance/2");
    }

    public long[] path(long leftId, long rightId) {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#path/2");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3PageableTreeLcaRmqIndex#retainedPayloadBytes/0");
    }
}
