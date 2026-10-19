// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexStringSearchTree}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringSearchTree.java}
 * blob {@code 655f149f42d73dc875ff169ec40c3c1f6b284732} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ac8fcbe13a1b9e478cd0c60c11a6127ee76b683f67a8ba7313a502e815da990c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3StringSearchTree}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
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
 *   <li>{@code nested types not received: Range}</li>
 *   <li>{@code member build(MIndexString) (parameter source)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member range(CharSequence) (return type)}</li>
 *   <li>{@code member suffixView(int) (return type)}</li>
 *   <li>{@code member precomputePositions(IntWaveletMatrix.BuildMode, IntWaveletMatrix.BuildLimits, IProgressMonitor) (parameter mode)}</li>
 *   <li>{@code member firstPosition(Range, int, int, IProgressMonitor) (parameter range)}</li>
 *   <li>{@code member lastPosition(Range, int, int, IProgressMonitor) (parameter range)}</li>
 * </ul>
 */
public final class M3StringSearchTree {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexString}. */
    private int[] sourceLane;

    /** primitive lane for source field {@code suffixOrder} of type {@code int[]}. */
    private int[] suffixOrder;

    /** primitive lane for source field {@code suffixRanks} of type {@code int[]}. */
    private int[] suffixRanks;

    /** primitive lane for source field {@code lcp} of type {@code int[]}. */
    private int[] lcp;

    /** int handle lane (offsets into parent-owned storage) for source field {@code positions} of type {@code IntWaveletMatrix}. */
    private int[] positionsLane;

    private M3StringSearchTree() {
        throw new M3NotYetReceived("M3StringSearchTree#<init>/0");
    }

    public int suffixCount() {
        throw new M3NotYetReceived("M3StringSearchTree#suffixCount/0");
    }

    public int suffixStart(int suffixRank) {
        throw new M3NotYetReceived("M3StringSearchTree#suffixStart/1");
    }

    public int suffixRank(int sourceOffset) {
        throw new M3NotYetReceived("M3StringSearchTree#suffixRank/1");
    }

    public int lcpWithPrevious(int suffixRank) {
        throw new M3NotYetReceived("M3StringSearchTree#lcpWithPrevious/1");
    }

    public int longestRepeatedPrefixLength() {
        throw new M3NotYetReceived("M3StringSearchTree#longestRepeatedPrefixLength/0");
    }

    public boolean contains(CharSequence needle) {
        throw new M3NotYetReceived("M3StringSearchTree#contains/1");
    }

    public int count(CharSequence needle) {
        throw new M3NotYetReceived("M3StringSearchTree#count/1");
    }

    public int indexOf(CharSequence needle) {
        throw new M3NotYetReceived("M3StringSearchTree#indexOf/1");
    }

    public int indexOf(CharSequence needle, int fromIndex) {
        throw new M3NotYetReceived("M3StringSearchTree#indexOf/2");
    }

    public int lastIndexOf(CharSequence needle) {
        throw new M3NotYetReceived("M3StringSearchTree#lastIndexOf/1");
    }

    public int lastIndexOf(CharSequence needle, int fromIndex) {
        throw new M3NotYetReceived("M3StringSearchTree#lastIndexOf/2");
    }

    public int[] allMatches(CharSequence needle) {
        throw new M3NotYetReceived("M3StringSearchTree#allMatches/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3StringSearchTree#primitivePayloadBytes/0");
    }

    public boolean hasPrecomputedPositions() {
        throw new M3NotYetReceived("M3StringSearchTree#hasPrecomputedPositions/0");
    }

    public long positionPayloadBytes() {
        throw new M3NotYetReceived("M3StringSearchTree#positionPayloadBytes/0");
    }
}
