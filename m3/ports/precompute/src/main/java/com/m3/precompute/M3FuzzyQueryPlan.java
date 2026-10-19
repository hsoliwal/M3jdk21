// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexFuzzyQueryPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexFuzzyQueryPlan.java}
 * blob {@code 41390f0a6fec1c628d5759af6345425087dbe50d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 36fd35c3d1ca909a006a14aff70fab3cc1d7e1b8cd17905602ba48748fe7774e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3FuzzyQueryPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S22}.
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
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member operationShape() (return type)}</li>
 * </ul>
 */
public final class M3FuzzyQueryPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code owner} of type {@code MIndexFuzzySearchIndex}. */
    private int[] ownerLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code text} of type {@code String}. */
    private int[] textLane;

    /** primitive lane for source field {@code maxEditDistance} of type {@code int}. */
    private int maxEditDistance;

    /** primitive lane for source field {@code bitSignal} of type {@code long}. */
    private long bitSignal;

    /** primitive lane for source field {@code qGrams} of type {@code long[]}. */
    private long[] qGrams;

    /** primitive lane for source field {@code simHash64} of type {@code long}. */
    private long simHash64;

    /** primitive lane for source field {@code minHashes} of type {@code long[]}. */
    private long[] minHashes;

    /** primitive lane for source field {@code deleteKeys} of type {@code long[]}. */
    private long[] deleteKeys;

    /** primitive lane for source field {@code deleteFallbackAll} of type {@code boolean}. */
    private boolean deleteFallbackAll;

    /** int handle lane (offsets into parent-owned storage) for source field {@code myersPattern} of type {@code MIndexMyersPattern}. */
    private int[] myersPatternLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code histogram} of type {@code MIndexUtf16Histogram}. */
    private int[] histogramLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code rootHash} of type {@code String}. */
    private int[] rootHashLane;

    private M3FuzzyQueryPlan() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#<init>/0");
    }

    public String text() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#text/0");
    }

    public int maxEditDistance() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#maxEditDistance/0");
    }

    public int qGramCount() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#qGramCount/0");
    }

    public long simHash64() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#simHash64/0");
    }

    public String rootHash() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#rootHash/0");
    }

    public boolean deleteFallbackAll() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#deleteFallbackAll/0");
    }

    public int deleteSignatureCount() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#deleteSignatureCount/0");
    }

    public boolean usesBitParallelVerifier() {
        throw new M3NotYetReceived("M3FuzzyQueryPlan#usesBitParallelVerifier/0");
    }
}
