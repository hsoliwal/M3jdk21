// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexAtomPatternPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexAtomPatternPlan.java}
 * blob {@code 8c97e31da46a1c306d847305b0c337367f735c0f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 777821c1da480bcbe8147344d4428020c1ac9da1c04d60160e827a8e35899437}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3AtomPatternPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
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
 *   <li>{@code member compile(MIndexAtomGrammar, CharSequence) (parameter grammar)}</li>
 *   <li>{@code member grammar() (return type)}</li>
 *   <li>{@code member pattern() (return type)}</li>
 *   <li>{@code member matches(MIndexAtomTape, int) (parameter tape)}</li>
 *   <li>{@code member firstMatch(MIndexAtomTape) (parameter tape)}</li>
 *   <li>{@code member firstMatch(MIndexAtomTape, int) (parameter tape)}</li>
 *   <li>{@code member firstKnownMatch(MIndexAtomTape, int) (parameter tape)}</li>
 * </ul>
 */
public final class M3AtomPatternPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code grammar} of type {@code MIndexAtomGrammar}. */
    private int[] grammarLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code pattern} of type {@code MIndexPattern}. */
    private int[] patternLane;

    /** primitive lane for source field {@code accepted} of type {@code long[]}. */
    private long[] accepted;

    /** primitive lane for source field {@code acceptedKnownCount} of type {@code int}. */
    private int acceptedKnownCount;

    private M3AtomPatternPlan() {
        throw new M3NotYetReceived("M3AtomPatternPlan#<init>/0");
    }

    public int acceptedKnownCount() {
        throw new M3NotYetReceived("M3AtomPatternPlan#acceptedKnownCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3AtomPatternPlan#primitivePayloadBytes/0");
    }

    public boolean acceptsKnownId(int atomId) {
        throw new M3NotYetReceived("M3AtomPatternPlan#acceptsKnownId/1");
    }
}
