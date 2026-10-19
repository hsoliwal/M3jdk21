// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexSuffixArray}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexSuffixArray.java}
 * blob {@code a615902abd072c382959cfb99ac4e3fc2a42a298} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code cf87ed72cc7cd770132a6c0eb35960b19db3cea58d62c66c64ea84fb9b647177}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3SuffixArray}); projection
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
 *   <li>{@code nested types not received: RankRange, UnitReader}</li>
 *   <li>{@code member analyze(MIndexString) (parameter value)}</li>
 *   <li>{@code member analyze(MIndexString, IProgressMonitor) (parameter value)}</li>
 *   <li>{@code member analyze(MIndexString, long, IProgressMonitor) (parameter value)}</li>
 *   <li>{@code member prefixRange(CharSequence) (return type)}</li>
 *   <li>{@code member requiredAnalysisBytes(MIndexString) (parameter value)}</li>
 * </ul>
 */
public final class M3SuffixArray {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexString}. */
    private int[] sourceLane;

    /** primitive lane for source field {@code suffixOrder} of type {@code int[]}. */
    private int[] suffixOrder;

    /** primitive lane for source field {@code inverseRank} of type {@code int[]}. */
    private int[] inverseRank;

    /** primitive lane for source field {@code lcp} of type {@code int[]}. */
    private int[] lcp;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sourceRootSha256} of type {@code String}. */
    private int[] sourceRootSha256Lane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code rootHash} of type {@code String}. */
    private int[] rootHashLane;

    /** primitive lane for source field {@code longestRepeatedStart} of type {@code int}. */
    private int longestRepeatedStart;

    /** primitive lane for source field {@code longestRepeatedLength} of type {@code int}. */
    private int longestRepeatedLength;

    private M3SuffixArray() {
        throw new M3NotYetReceived("M3SuffixArray#<init>/0");
    }

    public int length() {
        throw new M3NotYetReceived("M3SuffixArray#length/0");
    }

    public int suffixStartAtRank(int rank) {
        throw new M3NotYetReceived("M3SuffixArray#suffixStartAtRank/1");
    }

    public int rankOfSuffix(int suffixStart) {
        throw new M3NotYetReceived("M3SuffixArray#rankOfSuffix/1");
    }

    public int lcpWithPreviousAtRank(int rank) {
        throw new M3NotYetReceived("M3SuffixArray#lcpWithPreviousAtRank/1");
    }

    public int longestRepeatedSubstringStart() {
        throw new M3NotYetReceived("M3SuffixArray#longestRepeatedSubstringStart/0");
    }

    public int longestRepeatedSubstringLength() {
        throw new M3NotYetReceived("M3SuffixArray#longestRepeatedSubstringLength/0");
    }

    public boolean contains(CharSequence needle) {
        throw new M3NotYetReceived("M3SuffixArray#contains/1");
    }

    public String sourceRootSha256() {
        throw new M3NotYetReceived("M3SuffixArray#sourceRootSha256/0");
    }

    public String rootHash() {
        throw new M3NotYetReceived("M3SuffixArray#rootHash/0");
    }

    public long primitiveBytes() {
        throw new M3NotYetReceived("M3SuffixArray#primitiveBytes/0");
    }

    public static long retainedPrimitiveBytes(int utf16Length) {
        throw new M3NotYetReceived("M3SuffixArray#retainedPrimitiveBytes/1");
    }
}
