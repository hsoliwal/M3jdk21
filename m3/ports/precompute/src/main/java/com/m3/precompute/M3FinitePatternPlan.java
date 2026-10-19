// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexFinitePatternPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexFinitePatternPlan.java}
 * blob {@code 94ea136d3590d223ff1c2e5bdcb18366f7ec72f2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ce47ae50a5104dd49661361bb23dfc7473985994203bcfb7b609a7fbd75e34cf}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3FinitePatternPlan}); projection
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
 *   <li>{@code nested types not received: Strategy}</li>
 *   <li>{@code member compile(MIndexSearchEngine, CharSequence) (parameter search)}</li>
 *   <li>{@code member compile(MIndexSearchEngine, CharSequence, MIndexRegexPolicy) (parameter search)}</li>
 *   <li>{@code member strategy() (return type)}</li>
 *   <li>{@code member matches() (return type)}</li>
 *   <li>{@code member find() (return type)}</li>
 *   <li>{@code member lookingAt() (return type)}</li>
 * </ul>
 */
public final class M3FinitePatternPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code expression} of type {@code String}. */
    private int[] expressionLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code strategy} of type {@code Strategy}. */
    private int[] strategyLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code matches} of type {@code MIndexQueryResult}. */
    private int[] matchesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code find} of type {@code MIndexQueryResult}. */
    private int[] findLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code lookingAt} of type {@code MIndexQueryResult}. */
    private int[] lookingAtLane;

    /** primitive lane for source field {@code re2ProgramSize} of type {@code int}. */
    private int re2ProgramSize;

    /** primitive lane for source field {@code mindexProgramSize} of type {@code int}. */
    private int mindexProgramSize;

    /** primitive lane for source field {@code primitiveProgramBytes} of type {@code long}. */
    private long primitiveProgramBytes;

    private M3FinitePatternPlan() {
        throw new M3NotYetReceived("M3FinitePatternPlan#<init>/0");
    }

    public String expression() {
        throw new M3NotYetReceived("M3FinitePatternPlan#expression/0");
    }

    public int re2ProgramSize() {
        throw new M3NotYetReceived("M3FinitePatternPlan#re2ProgramSize/0");
    }

    public int mindexProgramSize() {
        throw new M3NotYetReceived("M3FinitePatternPlan#mindexProgramSize/0");
    }

    public long primitiveProgramBytes() {
        throw new M3NotYetReceived("M3FinitePatternPlan#primitiveProgramBytes/0");
    }

    public long bitmapPayloadBytes() {
        throw new M3NotYetReceived("M3FinitePatternPlan#bitmapPayloadBytes/0");
    }

    public boolean usesRegexEngine() {
        throw new M3NotYetReceived("M3FinitePatternPlan#usesRegexEngine/0");
    }

    public boolean usesBacktrackingCompatibility() {
        throw new M3NotYetReceived("M3FinitePatternPlan#usesBacktrackingCompatibility/0");
    }
}
