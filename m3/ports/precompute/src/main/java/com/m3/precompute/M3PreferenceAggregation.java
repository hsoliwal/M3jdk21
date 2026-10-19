// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexPreferenceAggregation}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPreferenceAggregation.java}
 * blob {@code d5c2631736ea99a73c0efbd89f7c32cd13b5301f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 39c5897e2cabcac3e777b67b6d52786785aa1543d4a499804f70688560b8a1cf}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3PreferenceAggregation}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
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
 *   <li>{@code nested types not received: BallotDomain, ScoreRow}</li>
 *   <li>{@code member borda(MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member borda(IProgressMonitor, MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member plurality(MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member plurality(IProgressMonitor, MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member veto(MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member veto(IProgressMonitor, MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member rankSum(MIndexRankingTable...) (return type)}</li>
 *   <li>{@code member rankSum(IProgressMonitor, MIndexRankingTable...) (return type)}</li>
 * </ul>
 */
public final class M3PreferenceAggregation {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3PreferenceAggregation() {
        throw new M3NotYetReceived("M3PreferenceAggregation#<init>/0");
    }
}
