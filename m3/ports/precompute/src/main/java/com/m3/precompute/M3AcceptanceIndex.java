// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexAcceptanceIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexAcceptanceIndex.java}
 * blob {@code a3e3f721b1bccd036f8903691b2f2c8c89fca93a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e79548a629817e286d8e2fc4c86a4d8f7b8f3ced35b2ad6c9910cf4928bbe932}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3AcceptanceIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
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
 *   <li>{@code nested types not received: Builder}</li>
 *   <li>{@code member builder(MIndexFormulaTable) (return type)}</li>
 *   <li>{@code member formulas() (return type)}</li>
 *   <li>{@code member evaluate(long, MIndexInterpretationTable, long) (return type)}</li>
 *   <li>{@code member accepted(long, MIndexInterpretationTable, long) (parameter interpretations)}</li>
 * </ul>
 */
public final class M3AcceptanceIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code formulas} of type {@code MIndexFormulaTable}. */
    private int[] formulasLane;

    /** primitive lane for source field {@code ownerIds} of type {@code long[]}. */
    private long[] ownerIds;

    /** primitive lane for source field {@code formulaIds} of type {@code long[]}. */
    private long[] formulaIds;

    /** primitive lane for source field {@code slots} of type {@code long[]}. */
    private long[] slots;

    private M3AcceptanceIndex() {
        throw new M3NotYetReceived("M3AcceptanceIndex#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3AcceptanceIndex#size/0");
    }

    public boolean contains(long ownerId) {
        throw new M3NotYetReceived("M3AcceptanceIndex#contains/1");
    }

    public long formula(long ownerId) {
        throw new M3NotYetReceived("M3AcceptanceIndex#formula/1");
    }

    public long[] conditionSymbols(long ownerId) {
        throw new M3NotYetReceived("M3AcceptanceIndex#conditionSymbols/1");
    }

    public long[] ownerIds() {
        throw new M3NotYetReceived("M3AcceptanceIndex#ownerIds/0");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3AcceptanceIndex#retainedPayloadBytes/0");
    }
}
