// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.Collection;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexSenseTable}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexSenseTable.java}
 * blob {@code 0df3cbbacdd920ed936f16fcb135a5d04618ab1a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 81c5c4bd91f9347d75da90e546d2dbb342ecab733e2d6bd4d9a27f00d3e31e0e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3SenseTable}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
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
 *   <li>{@code nested types not received: WordClass, Evidence, Sense}</li>
 *   <li>{@code member freeze(long, Collection<Sense>, int) (parameter input)}</li>
 *   <li>{@code member page(long, long, int, int) (return type)}</li>
 * </ul>
 */
public final class M3SenseTable {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code lexiconGeneration} of type {@code long}. */
    private long lexiconGeneration;

    /** primitive lane for source field {@code words} of type {@code long[]}. */
    private long[] words;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** int handle lane (offsets into parent-owned storage) for source field {@code senses} of type {@code Sense[]}. */
    private int[] sensesLane;

    private M3SenseTable() {
        throw new M3NotYetReceived("M3SenseTable#<init>/0");
    }

    public long lexiconGeneration() {
        throw new M3NotYetReceived("M3SenseTable#lexiconGeneration/0");
    }

    public int senseCount() {
        throw new M3NotYetReceived("M3SenseTable#senseCount/0");
    }

    public int wordCount() {
        throw new M3NotYetReceived("M3SenseTable#wordCount/0");
    }
}
