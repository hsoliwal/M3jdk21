// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexStringKeywordPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringKeywordPlan.java}
 * blob {@code e947130c9eb7710aac895bb7d4bffe26901c3e31} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7431ff0211454dac2aaa52df4e54dadb633c5fa77cda7af799da774c4f8d5f2c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3StringKeywordPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
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
 *   <li>{@code member compile(List<? extends CharSequence>, int, IProgressMonitor) (parameter monitor)}</li>
 *   <li>{@code member findFirst(MIndexString) (return type)}</li>
 *   <li>{@code member visitMatches(MIndexString, MIndexLiteralSearchTree.MatchConsumer, IProgressMonitor) (parameter input)}</li>
 *   <li>{@code member visitMatches(MIndexString, MIndexLiteralSearchTree.MatchConsumer) (parameter input)}</li>
 *   <li>{@code member exactPattern(MIndexString) (parameter input)}</li>
 *   <li>{@code member tree() (return type)}</li>
 * </ul>
 */
public final class M3StringKeywordPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code tree} of type {@code MIndexLiteralSearchTree}. */
    private int[] treeLane;

    /** primitive lane for source field {@code patternCount} of type {@code int}. */
    private int patternCount;

    private M3StringKeywordPlan() {
        throw new M3NotYetReceived("M3StringKeywordPlan#<init>/0");
    }

    public int patternCount() {
        throw new M3NotYetReceived("M3StringKeywordPlan#patternCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3StringKeywordPlan#primitivePayloadBytes/0");
    }
}
