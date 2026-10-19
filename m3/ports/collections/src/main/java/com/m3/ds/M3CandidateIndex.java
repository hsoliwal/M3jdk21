// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexCandidateIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCandidateIndex.java}
 * blob {@code d8a488b4441d6562e3377c2cde9094149357187d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 59bfe65bb667393260cd279a2472daf77fba7e1f4a00741f94a7159559c4da03}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3CandidateIndex}); projection
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
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member contains(CharSequence) (return type)}</li>
 *   <li>{@code member candidateReceipt(CharSequence) (return type)}</li>
 *   <li>{@code member regexCandidates(MIndexRegexTrigramQuery) (return type)}</li>
 *   <li>{@code member prepareRegexCandidates(MIndexRegexTrigramQuery) (return type)}</li>
 *   <li>{@code member prepareRegexCandidates(MIndexRegexTrigramQuery, MIndexPreparedTrigramQuery.Limits, IProgressMonitor) (return type)}</li>
 *   <li>{@code member regexCandidatesPrepared(MIndexPreparedTrigramQuery) (return type)}</li>
 *   <li>{@code member regexCandidatesPrepared(MIndexPreparedTrigramQuery, IProgressMonitor) (return type)}</li>
 *   <li>{@code member bloom(int) (return type)}</li>
 *   <li>{@code member bloomQuery(int, long) (return type)}</li>
 * </ul>
 */
public final class M3CandidateIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code model} of type {@code MIndexQueryModel}. */
    private int[] modelLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code units} of type {@code MIndexLongPostingImage}. */
    private int[] unitsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code bigrams} of type {@code MIndexLongPostingImage}. */
    private int[] bigramsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code trigrams} of type {@code MIndexLongPostingImage}. */
    private int[] trigramsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code unitBloom} of type {@code MIndexBloomFilter}. */
    private int[] unitBloomLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code bigramBloom} of type {@code MIndexBloomFilter}. */
    private int[] bigramBloomLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code trigramBloom} of type {@code MIndexBloomFilter}. */
    private int[] trigramBloomLane;

    private M3CandidateIndex() {
        throw new M3NotYetReceived("M3CandidateIndex#<init>/0");
    }

    public String rootHash() {
        throw new M3NotYetReceived("M3CandidateIndex#rootHash/0");
    }

    public int[] positions(int row, CharSequence needle, boolean overlapping) {
        throw new M3NotYetReceived("M3CandidateIndex#positions/3");
    }
}
